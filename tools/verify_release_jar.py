#!/usr/bin/env python3
"""Verify that a Sky VeLor build output is the real distributable release jar.

Written after a build was accepted whose artifact was not the mod at all: the CI
step globbed for a jar and picked up ``build/tmp/deobfMcMCP/deobfed.jar``, which
is ForgeGradle's deobfuscated Minecraft intermediate (~11 MB). It contains no Sky
VeLor class, so nothing the mod claimed to do could work.

This script answers the questions that a filename and a file size cannot:

* is it the mod at all, and not a Minecraft/dependency intermediate?
* is it the *reobfuscated* artifact Forge needs, or the deobfuscated dev jar?
* does the version metadata say the release we intend to ship?
* are the classes for the current feature actually inside it?
* is there an accidental nested archive or unrelated project file?

The mapping check is the decisive one. A release jar must reference Minecraft
members by SRG name (``func_78440_a``); a development jar references them by MCP
name (``renderItemInFirstPerson``). Class files are parsed directly, so this runs
on a machine with no JDK at all.

Usage:  python3 tools/verify_release_jar.py <jar> [--version Beta-0.10.7v]
"""

import argparse
import hashlib
import os
import re
import struct
import sys
import zipfile

MOD_PACKAGE = 'com/arena/pekaclient/'

# Classes the Beta-0.10.7v release must contain.
REQUIRED_CLASSES = [
    'PekaClientMod',
    'PekaVersion',
    'config/AppleSettings',
    'feature/FirstPersonItem',
    'gui/AppleHubScreen',
    'gui/AppleGuiButton',
    'render/CustomItemRenderer',
    'render/FirstPersonItemSetup',
]

# Removed in Beta-0.10.7v: dead code whose own comments admitted it could not work.
FORBIDDEN_CLASSES = ['render/FirstPersonItemRendererProxy']

# Minecraft members the mod touches. If any of these development names survives
# into the jar, it was not reobfuscated and will not work in a real game.
MCP_NAMES = [
    'renderItemInFirstPerson',
    'updateEquippedItem',
    'equippedProgress',
    'prevEquippedProgress',
    'resetEquippedProgress',
    'itemRenderer',
    'entityRenderer',
]

SRG_RE = re.compile(r'^(?:func|field)_\d+_[A-Za-z0-9_]+$')


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


def constant_pool_utf8(data):
    """Every CONSTANT_Utf8 string in a class file, or None if it is not one."""
    if len(data) < 10 or data[:4] != b'\xca\xfe\xba\xbe':
        return None
    count = struct.unpack('>H', data[8:10])[0]
    out = []
    i, n = 10, 1
    while n < count and i < len(data):
        tag = data[i]
        if tag == 1:                                   # Utf8
            ln = struct.unpack('>H', data[i + 1:i + 3])[0]
            out.append(data[i + 3:i + 3 + ln].decode('utf-8', 'replace'))
            i += 3 + ln
        elif tag in (7, 8, 16, 19, 20):                # Class, String, MethodType, Module, Package
            i += 3
        elif tag == 15:                                # MethodHandle
            i += 4
        elif tag in (5, 6):                            # Long, Double take two slots
            i += 9
            n += 1
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):     # Int, Float, refs, NameAndType, Dynamic
            i += 5
        else:
            return None
        n += 1
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('jar')
    ap.add_argument('--version', help='expected mcmod.info version, e.g. Beta-0.10.7v')
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

    mod_classes = [n for n in names
                   if n.startswith(MOD_PACKAGE) and n.endswith('.class')]
    r.check(len(mod_classes) > 0, 'contains compiled Sky VeLor classes',
            '%d class files' % len(mod_classes))
    # An intermediate Minecraft jar has net/minecraft, and no mod package at all.
    r.check(not any(n.startswith('net/minecraft/') for n in names),
            'does not contain Minecraft classes',
            'a Minecraft intermediate would')

    for c in REQUIRED_CLASSES:
        r.check(MOD_PACKAGE + c + '.class' in names, 'present: %s.class' % c)
    for c in FORBIDDEN_CLASSES:
        r.check(MOD_PACKAGE + c + '.class' not in names, 'absent (removed): %s.class' % c)

    r.check(not any(n.endswith('.jar') or n.endswith('.zip') for n in names),
            'no nested archive', 'a shaded or double-packed jar would show here')
    r.check(not any(n.startswith(('src/', 'tests/', 'gradle/', '.github/'))
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

    # --- the decisive mapping check --------------------------------------
    srg, mcp_hits, parsed = set(), [], 0
    for n in mod_classes:
        utf8 = constant_pool_utf8(zf.read(n))
        if utf8 is None:
            continue
        parsed += 1
        for s in utf8:
            if SRG_RE.match(s):
                srg.add(s)
            elif s in MCP_NAMES:
                mcp_hits.append((n, s))
    print('  class files parsed : %d' % parsed)
    print('  distinct SRG member names referenced : %d' % len(srg))
    for s in sorted(srg):
        print('      %s' % s)
    r.check(parsed == len(mod_classes), 'every mod class parsed as valid bytecode')
    r.check(len(srg) > 0, 'references Minecraft members by SRG name',
            'proves reobfJar ran; a deobfuscated dev jar has none')
    r.check(not mcp_hits, 'no Minecraft development (MCP) member names left',
            ', '.join(sorted(set('%s:%s' % h for h in mcp_hits[:6]))) or 'clean')

    # The feature's own entry point must really call into Minecraft, reobfuscated.
    cir = MOD_PACKAGE + 'render/CustomItemRenderer.class'
    if cir in names:
        utf8 = constant_pool_utf8(zf.read(cir)) or []
        r.check(any(SRG_RE.match(s) for s in utf8),
                'CustomItemRenderer is remapped into the obfuscated runtime')
        r.check('getItemScale' in utf8,
                'CustomItemRenderer still reads the Sky VeLor scale setting',
                'the slider must reach the renderer')

    print()
    print('verify release jar: passed %d, failed %d' % (r.passed, r.failed))
    return 1 if r.failed else 0


if __name__ == '__main__':
    sys.exit(main())
