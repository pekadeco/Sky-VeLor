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

Usage:  python3 tools/verify_release_jar.py <jar> [--version Beta-0.10.8.0.1v]
"""

import argparse
import hashlib
import os
import re
import struct
import sys
import zipfile

MOD_PACKAGE = 'com/arena/pekaclient/'

# Classes the release must contain: the core, direct Item Animation hook, read-only terminal overlay,
# and the imported live dungeon-map/puzzle implementation.
REQUIRED_CLASSES = [
    'PekaClientMod',
    'PekaVersion',
    'config/AppleSettings',
    'feature/ItemAnimation',
    'feature/VisualSwingStateMachine',
    'command/ItemDebugCommand',
    'gui/AppleHubScreen',
    'gui/AppleGuiButton',
    'gui/HudEditorScreen',
    'gui/HudEditorLayout',
    'gui/ItemAnimationUiModel',
    'gui/GuiSettingsAuditModel',
    'dungeon/TerminalSolver',
    'dungeon/DungeonMapOverlay',
    'dungeon/PuzzleRoomDatabase',
    'dungeon/PuzzleRoomMatcher',
    'render/itemanimation/ItemAnimationHooks',
    'render/itemanimation/ItemAnimationHookStatus',
    'render/itemanimation/ItemAnimationLoadingPlugin',
    'render/itemanimation/ItemAnimationTransformer',
]

# Old terminal replacements and the unreliable renderer-replacement path must stay absent. The release highlights
# real chest slots without taking over a container, and Item Animation must hook Minecraft directly instead of
# reflectively replacing its final ItemRenderer field.
FORBIDDEN_CLASSES = [
    'feature/FirstPersonItem',
    'render/CustomItemRenderer',
    'render/FirstPersonItemSetup',
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
    'getRenderItem',
    'renderItemAndEffectIntoGUI',
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
    """Return (class_name, member_names, utf8_constants) from a class file, or None.

    member_names holds only names reached through a Fieldref, Methodref or
    InterfaceMethodref constant, i.e. members this class actually touches. UTF8
    constants are returned separately for explicit ASM target-name proof; they
    are never used by the reobfuscation mapping check.
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
    utf8_constants = set(val for tag, val in pool.values() if tag == 1)
    return class_name, members, utf8_constants


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('jar')
    ap.add_argument('--version', help='expected mcmod.info version, e.g. Beta-0.10.8.0.1v')
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

    # Coremod manifest: without both values Forge would package the classes but never wire the real hand hook.
    manifest = zf.read('META-INF/MANIFEST.MF').decode('utf-8', 'replace') if 'META-INF/MANIFEST.MF' in names else ''
    # JAR manifests fold long attributes by starting the continuation line with one space.
    manifest = re.sub(r'\r?\n ', '', manifest)
    r.check('FMLCorePlugin: com.arena.pekaclient.render.itemanimation.ItemAnimationLoadingPlugin' in manifest,
            'manifest registers the Item Animation loading plugin')
    r.check('FMLCorePluginContainsFMLMod: true' in manifest,
            'manifest declares the regular Forge mod alongside the coremod')
    if args.version:
        r.check('Implementation-Version: %s' % args.version in manifest,
                'manifest build metadata version matches the release', args.version)

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

    def parsed_class(cls):
        path = MOD_PACKAGE + cls + '.class'
        return parse_class(zf.read(path)) if path in names else None

    def members_of(cls):
        got = parsed_class(cls)
        return got[1] if got else set()

    def constants_of(cls):
        got = parsed_class(cls)
        return got[2] if got else set()

    def raw_of(cls):
        path = MOD_PACKAGE + cls + '.class'
        return zf.read(path) if path in names else b''

    # --- Item Animation GUI reachability and ownership proof -----------------
    # A standalone Item Animation class is not enough: both real GUI entry points must reference the pure models,
    # the Hub must use its shared collapsible section, and customization may not be duplicated in a GUI tab.
    ui_constants = constants_of('gui/ItemAnimationUiModel')
    r.check(all(value in ui_constants for value in
                ('Item Animation', 'Open the HUD editor', 'Open Item Animation Editor', 'Scale', 'Position',
                 'Animation', 'Options', 'X', 'Y', 'Z', 'Reset Position', 'Remove Haste Effect',
                 'Custom Hit/Slash Style', 'Custom Hit/Slash Speed', 'Preview Swing', 'Reset to Defaults')),
            'ItemAnimationUiModel packages the required compact HUD/editor labels and controls')
    r.check('Open Item Animation Settings' not in ui_constants and 'Auto Fix Hit/Slash' not in ui_constants,
            'ItemAnimationUiModel excludes the removed settings launcher and Auto Fix option')
    model_ref = b'com/arena/pekaclient/gui/ItemAnimationUiModel'
    audit_ref = b'com/arena/pekaclient/gui/GuiSettingsAuditModel'
    layout_ref = b'com/arena/pekaclient/gui/HudEditorLayout'
    hub_raw = raw_of('gui/AppleHubScreen')
    hub_members = members_of('gui/AppleHubScreen')
    r.check(model_ref in hub_raw and audit_ref in hub_raw
                and all(name in hub_members for name in ('buildHudTabModel', 'buildHudItemAnimationDropdownModel')),
            'AppleHubScreen bytecode builds the shared Item Animation dropdown and uses the tab/editor ownership audit')
    r.check(b'HudSettingsBuilder' not in hub_raw,
            'AppleHubScreen keeps HUD customization out of GUI tabs; it routes it to the HUD editor')
    editor_members = members_of('gui/HudEditorScreen')
    editor_raw = raw_of('gui/HudEditorScreen')
    r.check(model_ref in editor_raw and layout_ref in editor_raw
                and all(name in editor_members for name in ('buildHudEditorTargetList', 'panelControlLabel')),
            'HudEditorScreen bytecode keeps Item Animation in the target list and builds its compact shared layout')
    r.check(b'Open Item Animation Settings' not in editor_raw,
            'HudEditorScreen has no redundant Item Animation settings launcher')

    # --- explicit direct ItemRenderer proof --------------------------------
    # These are plain UTF8 constants intentionally: an ASM transformer has to carry the names it will compare at
    # launcher time. Checking all three names and the descriptors proves this is not the old caller-shape guess.
    transformer_constants = constants_of('render/itemanimation/ItemAnimationTransformer')
    r.check(all(value in transformer_constants for value in
                ('net.minecraft.client.renderer.ItemRenderer', 'bfn', 'renderItemInFirstPerson',
                 'func_78440_a', 'a', '(F)V')),
            'transformer carries explicit MCP/SRG/notch ItemRenderer render target names and (F)V')
    r.check(all(value in transformer_constants for value in
                ('updateEquippedItem', 'func_78441_a', '()V')),
            'transformer carries explicit MCP/SRG/notch equip target names and ()V')
    r.check('net.minecraft.client.renderer.EntityRenderer' not in transformer_constants,
            'transformer no longer targets EntityRenderer caller guesses')
    r.check(all(value in transformer_constants for value in
                ('beforeFirstPersonRender', 'afterFirstPersonRender', 'visualSwingProgress',
                 'scaleEquipProgress')),
            'transformer carries balanced matrix, visual swing, and equip hook entry points')

    loading_constants = constants_of('render/itemanimation/ItemAnimationLoadingPlugin')
    r.check('com.arena.pekaclient.render.itemanimation' in loading_constants,
            'loading plugin declares a transformer exclusion for its own coremod package')
    # Coremod setup classes must not link a Minecraft client/world class before LaunchClassLoader installs them.
    for cls in ('render/itemanimation/ItemAnimationLoadingPlugin',
                'render/itemanimation/ItemAnimationTransformer',
                'render/itemanimation/ItemAnimationHookStatus'):
        class_path = MOD_PACKAGE + cls + '.class'
        if class_path in names:
            raw = zf.read(class_path)
            r.check(b'Lnet/minecraft/client/' not in raw and b'Lnet/minecraft/world/' not in raw,
                    'coremod-safe: %s has no eager Minecraft client/world class descriptor' % cls)

    # The feature's direct callbacks must really reach the live settings bridge. The old renderer subclass and
    # reflection installer are forbidden above, so this also proves the packaged path is the direct core hook.
    if MOD_PACKAGE + 'render/itemanimation/ItemAnimationHooks.class' in names:
        hooks = members_of('render/itemanimation/ItemAnimationHooks')
        hook_constants = constants_of('render/itemanimation/ItemAnimationHooks')
        r.check(all(name in hooks for name in ('getConfiguredX', 'getConfiguredY', 'getConfiguredZ',
                                                'getConfiguredSize', 'getConfiguredSpeed', 'isEnabled')),
                'ItemAnimationHooks refreshes live primitive X/Y/Z/Size/Speed/master settings each hand render')
        r.check(all(name in hook_constants for name in ('beforeFirstPersonRender', 'afterFirstPersonRender',
                                                        'visualSwingProgress', 'scaleEquipProgress')),
                'ItemAnimationHooks packages all direct render/swing/equip callbacks')
        hook_raw = raw_of('render/itemanimation/ItemAnimationHooks')
        r.check('advance' in hooks and b'VisualSwingStateMachine' in hook_raw,
                'ItemAnimationHooks uses the allocation-free visual swing state machine')
        r.check(b'AutoFix' not in hook_raw and b'autoFix' not in hook_raw,
                'ItemAnimationHooks keeps the corrected visual swing behavior permanently enabled')
        r.check('func_179094_E' in hooks and 'func_179121_F' in hooks,
                'ItemAnimationHooks contains balanced reobfuscated pushMatrix/popMatrix calls')
        r.check(b'java/lang/reflect' not in zf.read(MOD_PACKAGE
                + 'render/itemanimation/ItemAnimationHooks.class'),
                'ItemAnimationHooks uses no reflection on the frame path')
    if MOD_PACKAGE + 'config/AppleSettings.class' in names:
        settings_constants = constants_of('config/AppleSettings')
        settings_raw = raw_of('config/AppleSettings')
        r.check('itemAnimation.enabled' in settings_constants,
                'default-on Item Animation master toggle is persisted')
        r.check(b'itemAnimationAutoFixHitSlash' not in settings_raw,
                'AppleSettings retains no hidden Auto Fix setting state')
    if MOD_PACKAGE + 'command/ItemDebugCommand.class' in names:
        debug = members_of('command/ItemDebugCommand')
        r.check(all(name in debug for name in ('isEnabled', 'getX', 'getY', 'getZ', 'getSize', 'getSpeed',
                                                'getRenderCallsLastSecond', 'getVisualSwingProgress')),
                '/velor itemdebug reads master, live renderer values, calls/second, and visual swing progress')

    print()
    print('verify release jar: passed %d, failed %d' % (r.passed, r.failed))
    return 1 if r.failed else 0


if __name__ == '__main__':
    sys.exit(main())
