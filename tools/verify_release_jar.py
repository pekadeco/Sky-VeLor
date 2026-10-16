#!/usr/bin/env python3
"""Verify that a Sky VeLor build output is the real distributable release jar.

Written after a build was accepted whose artifact was not the mod at all: the CI
step globbed for a jar and picked up ``build/tmp/deobfMcMCP/deobfed.jar``, which
is ForgeGradle's deobfuscated Minecraft intermediate (~11 MB). It contains no Sky
VeLor class, so nothing the mod claimed to do could work.

This script answers the questions a filename and a file size cannot:

* is it the mod at all, and not a Minecraft or dependency intermediate?
* is it the *reobfuscated* artifact Forge needs, or the deobfuscated dev jar?
* does the version metadata say the release we intend to ship?
* are the classes for the current feature actually inside it?
* is there an accidental nested archive or unrelated project file?

The mapping check is the decisive one. A release jar must call Minecraft members
by SRG name (``func_78440_a``); a development jar calls them by MCP name
(``renderItemInFirstPerson``).

Member names are taken only from CONSTANT_Fieldref / Methodref /
InterfaceMethodref entries in the constant pool, not from every UTF8 string. That
matters: javac emits debug info by default, so a mod's own local variable named
``entityRenderer`` lands in the constant pool as a plain UTF8 entry and would be
mistaken for an unmapped Minecraft field. Class files are parsed directly, so this
runs with no JDK installed.

Usage:  python3 tools/verify_release_jar.py <jar> [--version Beta-0.10.7.6v]
"""

import argparse
import hashlib
import os
import re
import struct
import sys
import zipfile

MOD_PACKAGE = 'com/arena/pekaclient/'

# Classes the release must contain: the core, first-person renderer configuration, read-only terminal
# overlay, and the imported live dungeon-map/puzzle implementation.
REQUIRED_CLASSES = [
    'PekaClientMod',
    'PekaVersion',
    'config/AppleSettings',
    'feature/FirstPersonItem',
    'gui/AppleHubScreen',
    'gui/AppleGuiButton',
    'gui/HudEditorScreen',
    'dungeon/TerminalSolver',
    'dungeon/DungeonMapOverlay',
    'dungeon/PuzzleRoomDatabase',
    'dungeon/PuzzleRoomMatcher',
    'render/CustomItemRenderer',
    'render/FirstPersonItemSetup',
]

# Old terminal replacements must stay absent: the release highlights real chest slots without taking over
# a container. The renderer proxy was already dead code in the older baseline.
FORBIDDEN_CLASSES = [
    'render/FirstPersonItemRendererProxy',
    'gui/TerminalScreen',
    'gui/DungeonHudScreen',
    'hud/ClickInOrderHud',
    'command/TerminalCommand',
]

# Minecraft members this mod really calls or overrides. Reobfuscation must have
# replaced every one of them with an SRG name; if a name survives as an actual
# member reference, the jar is a development build and will not work in a game.
# Only names that are unambiguously Minecraft's and that this mod really calls or
# overrides. Generic GlStateManager names such as scale, pushMatrix and popMatrix
# are deliberately excluded: Sky VeLor has its own HudElement.scale() and widget
# methods of the same names, and a reobfuscated jar keeps those source names, so
# listing them produces false failures without proving anything.
MCP_MEMBER_NAMES = {
    'renderItemInFirstPerson',
    'updateEquippedItem',
    'resetEquippedProgress',
    'getItemRenderer',
}

# Minecraft-owned types whose names must also have been remapped out of member
# descriptors is not checked; only member names are, which is the part that breaks.
SRG_RE = re.compile(r'^(?:func|field)_\d+_[A-Za-z0-9_]+$')

# Own-project members that legitimately keep their source names.
OWN_PREFIXES = ('get', 'set', 'is', 'has', 'effective', 'clamp', 'reset', 'bind',
                'find', 'write', 'clear', 'give', 'init', 'install')


class Result(object):
    def __init__(self):
        self.passed = 0
        self.failed = 0

    def check(self, ok, label, detail=''):
        if ok:
            self.passed += 1
            print('  ok   %s%s' % (label, (' - ' + detail) if detail else ''))
        else:
            self.failed += 1
            print('  FAIL %s%s' % (label, (' - ' + detail) if detail else ''))
        return ok


def parse_class(data):
    """Return (class_name, member_names) from a class file, or None.

    member_names holds only names reached through a Fieldref, Methodref or
    InterfaceMethodref constant, i.e. members this class actually touches.
    """
    if len(data) < 10 or data[:4] != b'\xca\xfe\xba\xbe':
        return None
    count = struct.unpack('>H', data[8:10])[0]
    pool = {}
    i, n = 10, 1
    while n < count and i < len(data):
        tag = data[i]
        if tag == 1:                                        # Utf8
            ln = struct.unpack('>H', data[i + 1:i + 3])[0]
            pool[n] = (tag, data[i + 3:i + 3 + ln].decode('utf-8', 'replace'))
            i += 3 + ln
        elif tag in (5, 6):                                 # Long, Double: two slots
            pool[n] = (tag, data[i + 1:i + 9])
            i += 9
            n += 1
        elif tag in (3, 4):                                 # Integer, Float
            pool[n] = (tag, data[i + 1:i + 5])
            i += 5
        elif tag in (7, 8, 16):                             # Class, String, MethodType
            pool[n] = (tag, struct.unpack('>H', data[i + 1:i + 3])[0])
            i += 3
        elif tag == 15:                                     # MethodHandle
            pool[n] = (tag, data[i + 1:i + 4])
            i += 4
        elif tag in (9, 10, 11):                            # Field/Method/InterfaceMethod ref
            pool[n] = (tag, struct.unpack('>HH', data[i + 1:i + 5]))
            i += 5
        elif tag in (12, 17, 18):                           # NameAndType, Dynamic, InvokeDynamic
            pool[n] = (tag, struct.unpack('>HH', data[i + 1:i + 5]))
            i += 5
        elif tag in (19, 20):                               # Module, Package
            pool[n] = (tag, struct.unpack('>H', data[i + 1:i + 3])[0])
            i += 3
        else:
            return None
        n += 1

    def utf8(idx):
        e = pool.get(idx)
        return e[1] if e and e[0] == 1 else None

    this_idx = struct.unpack('>H', data[i + 2:i + 4])[0] if i + 4 <= len(data) else 0
    class_entry = pool.get(this_idx)
    class_name = utf8(class_entry[1]) if class_entry and class_entry[0] == 7 else None

    def nat_name(nat_idx):
        """NameAndType stores (name_index, descriptor_index); we want the name."""
        nat = pool.get(nat_idx)
        if nat and nat[0] == 12:
            return utf8(nat[1][0])
        return None

    members = set()
    for tag, val in pool.values():
        if tag in (9, 10, 11):                              # field / method references
            name = nat_name(val[1])
            if name:
                members.add(name)
        elif tag == 18:                                     # invokedynamic (lambdas)
            name = nat_name(val[1])
            if name:
                members.add(name)
    return class_name, members


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('jar')
    ap.add_argument('--version', help='expected mcmod.info version, e.g. Beta-0.10.7.6v')
    args = ap.parse_args()

    path = args.jar
    r = Result()
    print('=== %s ===' % path)

    if not r.check(os.path.isfile(path), 'file exists'):
        return 1
    size = os.path.getsize(path)
    digest = hashlib.sha256(open(path, 'rb').read()).hexdigest()
    print('  size   : %d bytes (%.1f KB)' % (size, size / 1024.0))
    print('  sha256 : %s' % digest)

    r.check(size > 10000, 'nonzero, plausible size', '%d bytes' % size)
    # The mod is ~280 KB. Anything megabytes in size is a Minecraft/dependency jar.
    r.check(size < 2 * 1024 * 1024, 'not a Minecraft or dependency intermediate',
            'a deobfuscated Minecraft jar is ~11 MB')
    r.check('deobfed' not in os.path.basename(path),
            'not named like a ForgeGradle intermediate')

    try:
        zf = zipfile.ZipFile(path)
    except zipfile.BadZipFile:
        r.check(False, 'is a valid zip/jar')
        return 1
    names = zf.namelist()

    mod_classes = [n for n in names if n.startswith(MOD_PACKAGE) and n.endswith('.class')]
    r.check(len(mod_classes) > 0, 'contains compiled Sky VeLor classes',
            '%d class files' % len(mod_classes))
    r.check(not any(n.startswith('net/minecraft/') for n in names),
            'does not contain Minecraft classes', 'a Minecraft intermediate would')

    for c in REQUIRED_CLASSES:
        r.check(MOD_PACKAGE + c + '.class' in names, 'present: %s.class' % c)
    for c in FORBIDDEN_CLASSES:
        r.check(MOD_PACKAGE + c + '.class' not in names, 'absent (removed): %s.class' % c)

    r.check(not any(n.endswith('.jar') or n.endswith('.zip') for n in names),
            'no nested archive', 'a shaded or double-packed jar would show here')
    r.check(not any(n.startswith(('src/', 'tests/', 'gradle/', '.github/', 'ci-evidence/'))
                    for n in names),
            'no unrelated project files packaged')

    # --- version metadata -------------------------------------------------
    if 'mcmod.info' in names:
        info = zf.read('mcmod.info').decode('utf-8', 'replace')
        m = re.search(r'"version"\s*:\s*"([^"]+)"', info)
        found = m.group(1) if m else None
        print('  mcmod.info version : %s' % found)
        r.check(found is not None, 'mcmod.info carries a version')
        if args.version:
            r.check(found == args.version, 'mcmod.info version matches the release',
                    'expected %s' % args.version)
        mc = re.search(r'"mcversion"\s*:\s*"([^"]+)"', info)
        r.check(bool(mc) and mc.group(1) == '1.8.9', 'mcmod.info targets 1.8.9',
                mc.group(1) if mc else 'missing')
        r.check('"modid": "pekaclient"' in info, 'mcmod.info modid is pekaclient')
    else:
        r.check(False, 'mcmod.info present')

    # Resources: keybind labels would fall back to raw translation keys without these.
    r.check('assets/pekaclient/lang/en_US.lang' in names,
            'English keybind labels packaged')
    r.check('assets/pekaclient/dungeons/rooms.json' in names,
            'dungeon puzzle-room templates packaged')

    # --- the decisive mapping check --------------------------------------
    srg, mcp_hits, parsed, unparsed = set(), [], 0, []
    for n in mod_classes:
        got = parse_class(zf.read(n))
        if got is None:
            unparsed.append(n)
            continue
        parsed += 1
        for member in got[1]:
            if SRG_RE.match(member):
                srg.add(member)
            elif member in MCP_MEMBER_NAMES:
                mcp_hits.append((n, member))

    print('  class files parsed          : %d' % parsed)
    print('  distinct SRG members called : %d' % len(srg))
    for s in sorted(srg):
        print('      %s' % s)
    r.check(not unparsed, 'every mod class parsed as valid bytecode',
            ', '.join(unparsed[:5]))
    r.check(len(srg) > 0, 'calls Minecraft members by SRG name',
            'proves reobfJar ran; a deobfuscated dev jar has none')
    r.check(not mcp_hits, 'no Minecraft development (MCP) member calls left',
            '; '.join(sorted(set('%s -> %s' % h for h in mcp_hits[:6]))) or 'clean')

    # The feature's own entry points must really reach Minecraft and the settings.
    def members_of(cls):
        got = parse_class(zf.read(MOD_PACKAGE + cls + '.class'))
        return got[1] if got else set()

    if MOD_PACKAGE + 'render/CustomItemRenderer.class' in names:
        cir = members_of('render/CustomItemRenderer')
        r.check(any(SRG_RE.match(m) for m in cir),
                'CustomItemRenderer calls remapped Minecraft members',
                'the override must bind in the obfuscated runtime')
        r.check('getItemScale' in cir,
                'CustomItemRenderer reads the Sky VeLor size setting',
                'the size slider must reach the renderer')
        r.check(all(name in cir for name in ('getItemX', 'getItemY', 'getItemZ')),
                'CustomItemRenderer reads the X/Y/Z renderer position settings',
                'all three HUD-editor position controls must reach the renderer')
        r.check('getAnimationSpeed' in cir,
                'CustomItemRenderer reads the Sky VeLor animation-speed setting',
                'the visual-only animation-speed slider must reach the renderer')
    if MOD_PACKAGE + 'render/FirstPersonItemSetup.class' in names:
        fps = members_of('render/FirstPersonItemSetup')
        r.check('<init>' in fps, 'FirstPersonItemSetup constructs the custom renderer')
        # The install mechanism: find the renderer field by type, then write it.
        r.check('getDeclaredFields' in fps,
                'FirstPersonItemSetup resolves the renderer field by declared type',
                'name-based lookup cannot survive reobfuscation')
        r.check('setAccessible' in fps and 'set' in fps,
                'FirstPersonItemSetup writes the renderer field')

    print()
    print('verify release jar: passed %d, failed %d' % (r.passed, r.failed))
    return 1 if r.failed else 0


if __name__ == '__main__':
    sys.exit(main())
