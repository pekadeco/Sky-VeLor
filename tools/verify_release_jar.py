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

Usage:  python3 tools/verify_release_jar.py <jar> [--version Beta-0.10.9.2v] [--mcmod-version 0.10.9.2]
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
    'config/ChatQolSettings',
    'hud/ClientMetrics',
    'hud/HudConfigCompatibility',
    'config/DungeonEspSettings',
    'feature/ItemAnimation',
    'feature/VisualSwingStateMachine',
    'feature/SwingEasing',
    'feature/HotbarScrollPolicy',
    'feature/BlockParticlePolicy',
    'feature/DungeonContext',
    'feature/SpiritLeap',
    'feature/ChatTabs',
    'feature/ChatTabLayout',
    'feature/ChatInteractionModel',
    'feature/ChatChannelTabs',
    'feature/ChatTabClosePolicy',
    'feature/ChatSurfaceRenderer',
    'command/ItemDebugCommand',
    'command/SvCommand',
    'gui/AppleSettingsScreen',
    'gui/ReferenceSettingsCatalog',
    'gui/SettingsRailLayout',
    'gui/SettingsPageLayout',
    'gui/SettingsShellLayout',
    'gui/HomeMetricHistory',
    'gui/HomeMetricsLayout',
    'render/UIRender',
    'render/GuiPixelGrid',
    'render/RoundedRectGeometry',
    'render/GuiStyle',
    'render/GuiMotion',
    'render/Theme',
    'hud/HudElement',
    'hud/ItemSlots',
    # Retained only as a binary-compatible alias; it must not be the visible entry point.
    'gui/AppleHubScreen',
    'gui/FeatureUiState',
    'gui/Row',
    'gui/InlineSettingsBindings',
    'config/FeatureUiPreferences',
    'gui/GuiLayoutToolkit',
    'notes/QuickNoteLayout',
    'chat/CapturedChatMenu',
    'chat/ChatMenuState',
    'chat/ChatClipboard',
    'chat/ChatContextMenu',
    'chat/ChatInputInsertion',
    'chat/ChatMenuPolicy',
    'chat/ChatPlayerName',
    'chat/ChatRenderGeometry',
    'chat/ChatCompactMessages',
    'chat/ChatMessageHistory',
    'chat/ChatMenuInputRouter',
    'chat/ChatVisibilityAudit',
    'gui/AppleGuiButton',
    'gui/HudEditorScreen',
    'gui/HudEditorLayout',
    'gui/HudEditorSelectionPolicy',
    'gui/ItemAnimationEditorScreen',
    'gui/CommandKeybindEditorScreen',
    'gui/CommandKeybindEditorLayout',
    'gui/ItemAnimationPanelModel',
    'dungeon/TerminalSolver',
    'dungeon/DungeonMapOverlay',
    'dungeon/PuzzleRoomDatabase',
    'dungeon/PuzzleRoomMatcher',
    'dungeonesp/DungeonEspController',
    'dungeonesp/DungeonEspTracker',
    'dungeonesp/DungeonEspMatcher',
    'dungeonesp/DungeonEspMatchReason',
    'dungeonesp/WitherDoorCache',
    'dungeonesp/DungeonEspRenderBatch',
    'dungeonesp/DungeonEspMarkerBounds',
    'dungeonesp/DungeonEspMobDisplay',
    'render/itemanimation/ItemAnimationHooks',
    'render/itemanimation/ItemAnimationHookStatus',
    'render/itemanimation/ItemAnimationLoadingPlugin',
    'render/itemanimation/ItemAnimationTransformer',
    'render/blockparticles/BlockParticleHookStatus',
    'render/blockparticles/BlockParticleHooks',
    'render/blockparticles/BlockParticleTransformer',
    'itemlockswap/ItemLockSwapSettings',
    'itemlockswap/ItemLockSwapSlotMapper',
    'itemlockswap/ItemLockSwapKeybind',
    'itemlockswap/ItemLockSwapUiModel',
    'itemlockswap/ItemLockSwapInputState',
    'itemlockswap/ItemLockSwapGesturePolicy',
    'itemlockswap/ItemLockSwapLineGeometry',
    'itemlockswap/ItemLockSwapClickPolicy',
    'itemlockswap/ItemLockSwapDropPolicy',
    'itemlockswap/ItemLockSwapSwapPolicy',
    'itemlockswap/ItemLockSwapGuiAccess',
    'itemlockswap/ItemLockSwapOverlay',
    'itemlockswap/ItemLockSwapProtection',
    'itemlockswap/ItemLockSwapHooks',
    'itemlockswap/ItemLockSwapHookStatus',
    'itemlockswap/ItemLockSwapTransformer',
    'itemlockswap/ItemLockSwapController',
    'widget/AppleKeybindButton',
    # Assist runtime: requiring these keeps a release from claiming
    # the feature catalogue while accidentally packaging only its settings/UI surface.
    'config/AssistSettings',
    'assist/ShortcutController',
    'assist/CommandKeybindSettings',
    'assist/CommandKeybindPolicy',
    'assist/CommandKeybindController',
    'assist/inventory/InventoryAssistController',
    'assist/inventory/RomanNumeralPolicy',
    'assist/inventory/TooltipWindow',
    'assist/movement/AutoSprintController',
    'assist/movement/AutoSprintPolicy',
    'assist/mining/MiningAssistController',
    'assist/mining/MiningAbilityPolicy',
    'assist/mining/PuzzlerRoute',
    'assist/dungeon/DungeonBossTracker',
    'assist/dungeon/DungeonRunReadout',
    'assist/dungeon/DungeonRunTracker',
    'assist/dungeon/SpiritBowLocator',
    'assist/dungeon/DungeonTerminalAssistController',
    'assist/dungeon/DungeonWaypointController',
    'assist/dungeon/TerminalParsePolicy',
    'assist/sync/DungeonSyncManager',
    'assist/sync/DungeonSyncClient',
    'assist/sync/DungeonSyncEnvironment',
    'assist/sync/DungeonSyncProtocol',
    'assist/sync/DungeonSyncMessages',
    'assist/sync/DungeonSyncReconnectPolicy',
    'assist/visual/VisualAssistHookStatus',
    'assist/visual/VisualAssistHooks',
    'assist/visual/VisualAssistTransformer',
    'assist/visual/AspectTeleportTransitionPolicy',
    'assist/visual/AspectTeleportTransitionObservation',
    'assist/visual/AspectTeleportTransitionRenderer',
    'assist/visual/DeadMobRenderPolicy',
    'assist/visual/DeadMobRenderFilter',
    'assist/visual/AspectTeleportCameraPath',
    'assist/visual/AspectTeleportCameraHooks',
    'assist/visual/AspectTeleportCameraHookStatus',
    'assist/visual/AspectTeleportCameraTransformer',
    'hud/MiningAssistantHud',
    'hud/DungeonBossHud',
    'hud/DungeonRunHud',
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
    # The per-frame level-tag renderer was replaced by the cached yellow-star Dungeon ESP.
    'render/MobHighlight',
    'feature/MobMarkers',
    # The one-default-chat implementation retains no split-chat classes.
    'feature/ChatFilterRules',
    'feature/ChatPatternRules',
    'feature/ChatQolHistory',
    'feature/ChatQolMode',
    'feature/ChatQolRouting',
    'feature/ChatWindowLayout',
    'feature/ChattingAnimationBridge',
    'gui/ChatEditorPanelLayout',
    'chat/ChatPipeline',
    # The optional font system was removed entirely; all Sky VeLor text uses Minecraft's native FontRenderer.
    'config/FontSettings',
    'feature/ChatFontPolicy',
    'gui/FontsScreen',
    'render/FontAtlasGenerator',
    'render/GlobalFontManager',
    # Item Protection was retired; Item Lock / Swap remains in its separate package.
    'assist/guard/ItemGuardController',
    'assist/guard/ItemGuardPolicy',
    'assist/guard/ItemGuardStore',
    'assist/guard/ItemIdentity',
    # The established cleanup retires only the Player Dollars and Inventory Search implementations.
    'hud/DollarsHud',
    'hud/BalanceSource',
    'assist/inventory/InventorySearchPolicy',
    # FPS, memory and ping must remain Home-only settings telemetry, never a registered HUD.
    'hud/StatsHud',
    'hud/StatsHudText',
    # Settings features stay inline. HUD keeps its editor while Item Animation has one purpose-built standalone
    # panel; no generic, Chat, ESP, or Mod Settings editor class may ship.
    'gui/FeatureSettingsEditorScreen',
    'gui/FeatureSettingsEditors',
    'gui/FeatureSettingsUiModel',
    'gui/DungeonEspEditorSpecs',
    'gui/DungeonEspUiModel',
    'gui/ChatEditorLayout',
    'gui/ChatEditorScreen',
    'gui/ChatUiModel',
    'gui/GuiLayoutScreen',
    'gui/GuiSettingsAuditModel',
    'gui/ModSettingsScreen',
    'gui/ModSettingsUiModel',
    'gui/QualityOfLifeUiModel',
    'gui/FishingUiModel',
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
    ap.add_argument('--version', help='expected display/manifest version, e.g. Beta-0.10.9.2v')
    ap.add_argument('--mcmod-version', help='expected numeric mcmod.info version, e.g. 0.10.9.2')
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
    r.check(MOD_PACKAGE + 'dungeonesp/DungeonEspEndermanHeadPolicy.class' not in names,
            'absent (removed): dungeonesp/DungeonEspEndermanHeadPolicy.class')

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
            r.check(args.version in info, 'mcmod.info description carries the display release', args.version)
        expected_mcmod = args.mcmod_version or args.version
        if expected_mcmod:
            r.check(found == expected_mcmod, 'mcmod.info version matches the numeric mod metadata release',
                    'expected %s' % expected_mcmod)
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
    for resource in ('META-INF/LICENSE-CHATTING-GPL-3.0.txt',
                     'META-INF/LICENSE-SKYTILS-AGPL-3.0.txt',
                     'META-INF/THIRD_PARTY_NOTICES-CHAT.txt'):
        r.check(resource in names, 'packaged legal resource: %s' % resource)
    r.check(not any(name.startswith('assets/skyvelor/fonts/') for name in names),
            'no bundled custom-font asset or license remains')
    r.check('THIRD_PARTY_LICENSES.md' not in names and 'THIRD_PARTY_NOTICES.md' not in names,
            'no removed-font third-party notice remains')

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

    def raw_family_of(cls):
        """Outer plus synthetic/named inner classes, where Java places Runnable widget callbacks."""
        prefix = MOD_PACKAGE + cls
        return b''.join(zf.read(name) for name in names
                        if name == prefix + '.class' or (name.startswith(prefix + '$') and name.endswith('.class')))

    def missing_raw_values(raw, values):
        """A compact verifier detail that stays readable in GitHub Actions annotations."""
        return ', '.join(value.decode('ascii', 'replace') for value in values if value not in raw)

    r.check(all(MOD_PACKAGE + value + '.class' not in names for value in
                ('gui/ChatEditorPreview', 'gui/DungeonEspPreviewRenderer')),
            'retired editor sample-panel classes are absent from the release jar')

    # --- Stage 2 inline settings / retained runtime proof ----------------------
    # The catalog owns presentation only. It must expose every established owner through persisted collapsed
    # dropdowns, while the old focused-screen framework and all preview surfaces are genuinely absent.
    settings_screen_raw = raw_of('gui/AppleSettingsScreen')
    reference_catalog_raw = (raw_of('gui/ReferenceSettingsCatalog') + raw_of('gui/ReferenceSettingsCatalog$Tab')
                             + raw_of('gui/ReferenceSettingsCatalog$Control'))
    bindings_raw = raw_of('gui/InlineSettingsBindings')
    feature_state_raw = raw_of('gui/FeatureUiState')
    preferences_raw = raw_of('config/FeatureUiPreferences')
    assist_settings_raw = raw_of('config/AssistSettings')
    apple_settings_raw = raw_of('config/AppleSettings')
    client_raw = raw_of('PekaClientMod')
    command_actions_raw = raw_of('command/ClientActions')
    theme_raw = raw_of('render/Theme')
    dropdown_raw = raw_of('widget/AppleDropdown')
    rail_layout_raw = raw_of('gui/SettingsRailLayout')
    page_layout_raw = raw_of('gui/SettingsPageLayout')
    ui_render_raw = raw_of('render/UIRender')
    pixel_grid_raw = raw_of('render/GuiPixelGrid')
    hud_manager_raw = raw_of('hud/HudManager')
    hud_element_raw = raw_of('hud/HudElement')
    item_slots_raw = raw_of('hud/ItemSlots')
    item_animation_editor_raw = raw_family_of('gui/ItemAnimationEditorScreen')
    command_keybind_editor_raw = raw_family_of('gui/CommandKeybindEditorScreen')
    command_keybind_editor_layout_raw = raw_of('gui/CommandKeybindEditorLayout')
    command_keybind_settings_raw = raw_of('assist/CommandKeybindSettings')
    command_keybind_policy_raw = raw_of('assist/CommandKeybindPolicy')
    command_keybind_controller_raw = raw_of('assist/CommandKeybindController')
    shortcut_controller_raw = raw_of('assist/ShortcutController')
    wardrobe_shortcut_policy_raw = raw_of('assist/WardrobeShortcutPolicy')
    wardrobe_shortcut_controller_raw = raw_of('assist/WardrobeShortcutController')
    item_animation_panel_raw = raw_of('gui/ItemAnimationPanelModel')
    hook_status_raw = raw_of('render/itemanimation/ItemAnimationHookStatus')

    retired_inline_classes = (
        'gui/FeatureSettingsEditorScreen', 'gui/FeatureSettingsEditors', 'gui/FeatureSettingsUiModel',
        'gui/DungeonEspEditorSpecs', 'gui/DungeonEspUiModel', 'gui/ChatEditorLayout', 'gui/ChatEditorScreen',
        'gui/ChatUiModel', 'gui/GuiLayoutScreen', 'gui/GuiSettingsAuditModel', 'gui/ModSettingsScreen',
        'gui/ModSettingsUiModel', 'gui/QualityOfLifeUiModel', 'gui/FishingUiModel')
    r.check(all(MOD_PACKAGE + value + '.class' not in names for value in retired_inline_classes),
            'only HUD Editor and its Item Animation mode remain; all retired focused feature editor classes are absent')
    r.check(all(MOD_PACKAGE + value + '.class' not in names for value in
                ('gui/ChatEditorPreview', 'gui/DungeonEspPreviewRenderer', 'gui/LeapPreviewData')),
            'retired Chat, ESP, Door, inventory/swap and leap preview-only classes are absent from the release jar')
    r.check(len(bindings_raw) > 0 and all(value in bindings_raw for value in
                (b'ItemLockSwapSettings', b'ChatQolSettings', b'DungeonEspSettings', b'M7Settings',
                 b'FishingStats')) and b'StyleClipboard' not in bindings_raw and b'setProperty' not in bindings_raw,
            'InlineSettingsBindings writes existing feature owners without the retired ESP style clipboard')
    r.check(all(value in feature_state_raw for value in (b'dropdowns', b'ui.feature.dropdown.', b'MAX_ENTRIES', b'openAccordion'))
                and all(value in preferences_raw for value in (b'dropdownOpen', b'setDropdownOpen', b'openAccordion')),
            'collapsed inline dropdown state persists separately and provides one-open accordion behavior')
    r.check(all(value in settings_screen_raw for value in
                (b'HIT_DISCLOSURE', b'FeatureUiPreferences', b'childBlockHeight', b'reserveValueDropdown',
                 b'expandSearchResult', b'drawFullscreenGlyph', b'open-hud-editor', b'item-animation-open-editor',
                 b'open-command-keybind-editor', b'drawGroupCaption', b'drawResetFooter', b'EXPANDED_BOTTOM_PADDING', b'GuiMotion',
                 b'expandedPanel', b'expandedBorder', b'expandedHeader', b'expandedRowHover', b'expandedDivider',
                 b'dependentTextPrimary', b'dependentTextSecondary', b'actionButtonX', b'actionButtonWidth',
                 b'editorToggleX', b'ACTION_BUTTON_H'))
                and b'AppleToggle' in settings_screen_raw and b'AppleDropdown' in settings_screen_raw
                and b'AppleSegmented' not in settings_screen_raw,
            'AppleSettingsScreen uses one-level persisted dropdowns, readable inset accordions, search-driven opening, fixed controls and reset-link footers')
    r.check(all(value in reference_catalog_raw for value in
                (b'CAPTION', b'EDITOR', b'maxDropdownDepth', b'everyDropdownHasAtLeastTwoOptions',
                 b'everyChoiceHasAtLeastTwoOptions', b'dropdownIds', b'playerTextViolations')),
            'catalog packages flat captions, editor rows, depth/count guards and player-copy validation')
    r.check(all(value in reference_catalog_raw for value in
                (b'Home', b'QoL', b'Slayer', b'Dungeons', b'Farming', b'Mining', b'Fishing', b'HUD & Visuals', b'Misc',
                 b'SLAYER', b'No Farming Features Yet', b'Mining Assistant', b'Fishing Tracker', b'Slayer Tracker')),
            'catalog packages the requested feature-oriented nine-tab sidebar, QoL relabel, Slayer move, empty Farming message, and genuine Mining/Fishing moves')
    r.check(all(value in reference_catalog_raw for value in
                (b'Dungeon Map', b'Terminal Helper', b'Spirit Leap Picker', b'Boss Health HUD', b'Spirit Bow Locator',
                 b'spirit-bow-locator', b'Floor VII Overlay', b'Starred Mobs', b'Door ESP', b'Maxor', b'Storm',
                 b'Goldor', b'Necron', b'Power', b'Apex', b'Soul', b'Ice', b'Flame', b'Item Lock And Swap')),
            'flat Dungeons category retains map, terminals, leap, boss HUD, Spirit Bow locator, M7 and ESP controls')
    r.check(all(value in reference_catalog_raw for value in
                (b'Background', b'Messages', b'Tabs', b'Click Menu', b'Reset to defaults', b'Add Custom Tab',
                 b'Accent Color', b'Filter Type', b'Message Sound', b'Open Chat Background'))
                and b'ChatEditorScreen' not in settings_screen_raw and b'openChatSettings' in command_actions_raw,
            'Chat settings and custom-tab editing live in one flat owning dropdown; commands route to that owner')
    r.check(all(value in reference_catalog_raw for value in
                (b'HUD Editor', b'Dungeon Run HUD', b'hud-dungeon-run', b'Item Animation', b'Open HUD Editor',
                 b'Open Item Animation Editor'))
                and b'HudEditorScreen' in settings_screen_raw and b'ItemAnimationEditorScreen' in settings_screen_raw
                and b'item-animation-open-editor' in settings_screen_raw and b'editorButtonWidth' in settings_screen_raw
                and b'[Edit]' not in settings_screen_raw,
            'HUD Editor, movable Dungeon Run HUD and standalone Item Animation editor remain clearly reachable')
    r.check(b'CHAT_PREVIEW_' not in theme_raw and b'ChatEditorPreview' not in settings_screen_raw
                and b'DungeonEspPreviewRenderer' not in settings_screen_raw,
            'no retired live-preview color surface or focused-editor preview route remains')
    r.check(all(value in settings_screen_raw for value in
                (b'SettingsRailLayout', b'SettingsPageLayout', b'Settings rail', b'Settings card', b'Settings row'))
                and all(value in rail_layout_raw for value in (b'ENTRY_HEIGHT', b'ENTRY_GAP', b'COMPACT_COLUMNS'))
                and b'PROFILE_HEIGHT' not in rail_layout_raw and b'NARROW_CONTENT_WIDTH' in page_layout_raw,
            'settings shell retains shared scale-1–4 spacing, compact rail reflow and restored Apple-card debug bounds')
    r.check(all(value not in settings_screen_raw for value in (b'MOUSE', b'MOUSE/GUI', b'GUI AREA')),
            'settings shell packages no launcher-owned MOUSE/GUI indicator labels')
    r.check(all(value in settings_screen_raw for value in
                (b'HIT_WINDOW_FULLSCREEN', b'HIT_WINDOW_CLOSE', b'drawFullscreenGlyph', b'headerPressedHit', b'SkyVeLor'))
                and all(value not in settings_screen_raw for value in
                        (b'HIT_PROFILE', b'window-red', b'window-yellow', b'window-green')),
            'settings header packages the SkyVeLor upper-left wordmark and only Fullscreen/Close icon actions')
    r.check(all(value in dropdown_raw for value in
                (b'dropdownTrigger', b'dropdownPopup', b'dropdownBorder', b'dropdownRowHover',
                 b'dropdownRowSelected', b'dropdownDivider', b'drawSelectedCheck', b'reservePopup')),
            'shared one-level AppleDropdown packages high-contrast trigger/popup, hover, selected-check, divider, and flow reservation')
    r.check(all(value in apple_settings_raw for value in
                (b'glassOpacityOn', b'glassOpacity', b'MIN_GLASS_OPACITY', b'MAX_GLASS_OPACITY',
                 b'loadAppearanceSettings', b'saveAppearanceSettings'))
                and all(value in reference_catalog_raw for value in
                        (b'Custom GUI Transparency', b'GUI Transparency', b'appearance-opacity-on', b'appearance-opacity')),
            'optional GUI-transparency toggle and slider retain their established config keys and owner')
    r.check(all(value in ui_render_raw for value in (b'restoreVanillaGuiState', b'restoreFrameState', b'frameDepth',
                                                        b'GuiPixelGrid', b'guiScaleFactor'))
                and all(value in pixel_grid_raw for value in (b'textScale', b'coordinate'))
                and all(value in hud_manager_raw for value in (b'backgroundOnHoverOnly', b'hoveredInEditor'))
                and b'alphaMul' in hud_element_raw and b'restoreFrameState' in item_slots_raw,
            'HUD/editor rendering packages pixel-snapped native text, exception-safe state cleanup and an Apple-glass baseline')
    r.check(all(value in theme_raw for value in
                (b'applyAppleGlass', b'ACCENT_BLUE', b'SURFACE_CARD', b'EXPANDED_PANEL', b'EXPANDED_BORDER',
                 b'EXPANDED_HEADER', b'EXPANDED_ROW_HOVER', b'EXPANDED_DIVIDER', b'DEPENDENT_TEXT_PRIMARY',
                 b'DEPENDENT_TEXT_SECONDARY', b'DEPENDENT_CONTROL_ALPHA'))
                and b'appearance-theme' not in reference_catalog_raw and b'appearance-accent' not in reference_catalog_raw,
            'one fixed Apple-glass Theme keeps expanded disclosures readable while legacy palette keys stay out of the visible shell')

    # Retained Assist, ESP, Chat, Item Lock/Swap, Fishing and HUD runtime paths continue to be checked independently
    # of their presentation location.
    sync_client_raw = raw_of('assist/sync/DungeonSyncClient')
    sync_messages_raw = raw_of('assist/sync/DungeonSyncMessages')
    sync_manager_raw = raw_of('assist/sync/DungeonSyncManager')
    terminal_raw = raw_of('assist/dungeon/DungeonTerminalAssistController')
    mining_raw = raw_of('assist/mining/MiningAssistController')
    spirit_bow_locator_raw = raw_of('assist/dungeon/SpiritBowLocator')
    dungeon_run_readout_raw = raw_of('assist/dungeon/DungeonRunReadout')
    dungeon_run_tracker_raw = raw_of('assist/dungeon/DungeonRunTracker')
    dungeon_run_hud_raw = raw_of('hud/DungeonRunHud')
    selftest_command_raw = raw_of('command/ItemDebugCommand')
    aspect_policy_raw = raw_of('assist/visual/AspectTeleportTransitionPolicy')
    aspect_observation_raw = raw_of('assist/visual/AspectTeleportTransitionObservation')
    aspect_renderer_raw = raw_of('assist/visual/AspectTeleportTransitionRenderer')
    dead_filter_raw = raw_of('assist/visual/DeadMobRenderFilter')
    dead_policy_raw = raw_of('assist/visual/DeadMobRenderPolicy')
    aspect_path_raw = raw_of('assist/visual/AspectTeleportCameraPath')
    aspect_hooks_raw = raw_of('assist/visual/AspectTeleportCameraHooks')
    aspect_status_raw = raw_of('assist/visual/AspectTeleportCameraHookStatus')
    # The ASM visitors that contain injected descriptors live in nested classes, so inspect the whole transformer family.
    aspect_transformer_raw = raw_family_of('assist/visual/AspectTeleportCameraTransformer')
    r.check(all(value in assist_settings_raw for value in
                (b'assist.visual.hurtCameraShake', b'assist.visual.smoothAspectTeleport', b'assist.shortcut.autoSprint',
                 b'assist.shortcut.wardrobe', b'assist.shortcut.wardrobeKey', b'assist.shortcut.wardrobeNumberKeys',
                 b'assist.shortcut.pets', b'assist.shortcut.petsKey', b'DEFAULT_WARDROBE_SHORTCUT_KEY_CODE',
                 b'DEFAULT_PETS_SHORTCUT_KEY_CODE', b'assist.keybindCut.autoSprint', b'assist.keybindCut.wardrobe',
                 b'movement.autoSprint', b'assist.comfort.hideDeadMobs', b'assist.dungeon.sync.enabled',
                 b'assist.dungeon.sync.endpoint', b'assist.dungeon.spiritBowLocator', b'spiritBowLocator',
                 b'assist.dungeon.terminal.autoClick', b'hurtCameraShakeMultiplier')) and b'itemGuard' not in assist_settings_raw,
            'AssistSettings retains bounded visual, comfort, W/P Shortcut migration, Wardrobe-number preference, terminal, sync and opt-in Spirit Bow locator owners')
    # Raw bytes include declaration names; parse_class.members_of intentionally only reports referenced members.
    aspect_policy_values = (b'isAspectTeleportItem', b'isLargePositionJump', b'cameraProgress', b'aspect of the end',
                            b'aspect of the void', b'CAMERA_INTERPOLATION_NANOS')
    aspect_observation_values = (b'arm', b'observe', b'transitionFromX', b'transitionToX', b'remainingArmedTicks',
                                  b'AspectTeleportTransitionPolicy')
    aspect_path_values = (b'begin', b'isActiveAt', b'xAt', b'yAt', b'zAt', b'cameraProgress')
    aspect_renderer_values = (b'tick', b'renderTick', b'observeUse', b'AspectTeleportTransitionObservation',
                              b'AspectTeleportCameraHooks', b'advanceTick', b'AssistSettings')
    dead_policy_values = (b'isDeadOrDying', b'shouldSuppress', b'hasNpcMarker', b'Locale')
    # getPlayerInfo(UUID) is reobfuscated to its stable 1.8.9 SRG member in the shipped jar.
    dead_filter_values = (b'RenderLivingEvent$Pre', b'setCanceled', b'AssistSettings', b'NetworkPlayerInfo',
                          b'func_175102_a', b'DeadMobRenderPolicy')
    aspect_hook_values = (b'orientCameraX', b'orientCameraY', b'orientCameraZ', b'renderCameraX', b'renderCameraY',
                          b'renderCameraZ', b'dispatchRenderLast', b'ForgeHooksClient', b'AspectTeleportCameraPath')
    # The verifier runs on reobfuscated bytecode, so require the stable SRG orient method rather than its MCP spelling.
    aspect_transformer_values = (b'EntityRenderer', b'RenderGlobal', b'func_78467_g', b'(IFJ)V', b'orientCameraX',
                                 b'(DF)D', b'renderCameraX', b'dispatchRenderLast', b'(Ljava/lang/Object;F)V')
    aspect_status_values = (b'entityRendererInjected', b'renderGlobalInjected', b'isReady')
    r.check(all(value in aspect_policy_raw for value in aspect_policy_values)
                and all(value in aspect_observation_raw for value in aspect_observation_values)
                and all(value in aspect_path_raw for value in aspect_path_values),
            'Smooth Aspect policy, observation, and finite camera path are packaged',
            'missing policy=%s observation=%s path=%s' % (missing_raw_values(aspect_policy_raw, aspect_policy_values),
                    missing_raw_values(aspect_observation_raw, aspect_observation_values),
                    missing_raw_values(aspect_path_raw, aspect_path_values)))
    r.check(all(value in aspect_renderer_raw for value in aspect_renderer_values)
                and b'UIRender' not in aspect_renderer_raw and b'veilAlpha' not in aspect_renderer_raw
                and all(value in aspect_hooks_raw for value in aspect_hook_values),
            'Smooth Aspect runtime uses camera/world hooks rather than the retired HUD veil',
            'missing renderer=%s hooks=%s' % (missing_raw_values(aspect_renderer_raw, aspect_renderer_values),
                    missing_raw_values(aspect_hooks_raw, aspect_hook_values)))
    r.check(all(value in dead_policy_raw for value in dead_policy_values)
                and all(value in dead_filter_raw for value in dead_filter_values),
            'Hide Dead Mobs packages dedicated dead-state policy plus cancellable body/nameplate render filtering',
            'missing policy=%s filter=%s' % (missing_raw_values(dead_policy_raw, dead_policy_values),
                    missing_raw_values(dead_filter_raw, dead_filter_values)))
    dead_filter_prohibited = (b'sendPacket', b'sendQueue', b'setPosition', b'setLocationAndAngles', b'setHealth',
                              b'setDead', b'removeEntity', b'playerController')
    r.check(all(value not in dead_filter_raw for value in dead_filter_prohibited),
            'Hide Dead Mobs filter changes no entity, packet, movement, controller, or server state',
            'forbidden: ' + ', '.join(value.decode('ascii') for value in dead_filter_prohibited
                                       if value in dead_filter_raw))
    r.check(all(value in aspect_status_raw for value in aspect_status_values)
                and all(value in aspect_transformer_raw for value in aspect_transformer_values),
            'Smooth Aspect transformer verifies orient, world-pass, RenderGlobal, and mapping-neutral overlay hooks',
            'missing status=%s transformer=%s' % (missing_raw_values(aspect_status_raw, aspect_status_values),
                    missing_raw_values(aspect_transformer_raw, aspect_transformer_values)))
    # Minecraft member names are reobfuscated in a real release, so do not require MCP item/input spellings here.
    # Network/controller/position-writing owner paths must remain absent in every mapping.
    aspect_prohibited_values = (b'net/minecraft/network', b'NetworkManager', b'sendPacket', b'sendQueue',
                                b'playerController', b'setPosition', b'setLocationAndAngles', b'NetHandler')
    aspect_raw = aspect_renderer_raw + aspect_hooks_raw + aspect_transformer_raw
    r.check(all(value not in aspect_raw for value in aspect_prohibited_values),
            'Smooth Aspect camera glide has no movement, packet, controller, or position-write path',
            'forbidden: ' + ', '.join(value.decode('ascii') for value in aspect_prohibited_values if value in aspect_raw))
    r.check(all(value in client_raw for value in
                (b'AspectTeleportTransitionRenderer', b'aspectTeleportTransition', b'observeUse', b'RenderTickEvent',
                 b'renderTick', b'DeadMobRenderFilter', b'deadMobRenderFilter', b'MouseEvent')),
            'client packages render-time Aspect observation and cancellable dead-mob body/nameplate filtering')
    r.check(all(value in sync_client_raw for value in (b'wss', b'Sec-WebSocket-Key', b'sec-websocket-accept', b'SHA-1'))
                and all(value in sync_messages_raw for value in (b'welcome', b'puzzle_discovered'))
                and b'DungeonSyncEnvironment' in sync_manager_raw and b'rememberSyncedPuzzle' in sync_manager_raw,
            'Dungeon Sync packages WSS validation, allow-listed events, local environment hashing and map memory')
    r.check(b'lastAutoFingerprint' in terminal_raw and b'lastAutoSlot' in terminal_raw
                and b'TerminalParsePolicy' in terminal_raw and b'sendChatMessage' not in mining_raw,
            'terminal automation remains fingerprint-guarded and the Puzzler helper remains read-only')

    dungeon_readout_raw = spirit_bow_locator_raw + dungeon_run_readout_raw + dungeon_run_tracker_raw + dungeon_run_hud_raw
    r.check(all(value in spirit_bow_locator_raw for value in
                (b'Spirit Bow', b'AssistSettings', b'DungeonContext', b'RenderWorldLastEvent', b'WorldBox'))
                and b'SpiritBowLocator' in client_raw,
            'Spirit Bow Locator is packaged as an opt-in Forge world-render observer')
    r.check(all(value in dungeon_run_readout_raw for value in
                (b'Floor: ', b'Time: ', b'Cleared: ', b'Secrets: ', b'Deaths: '))
                and all(value in dungeon_run_tracker_raw for value in
                        (b'DungeonContext', b'DungeonRunReadout', b'ClientTickEvent', b'Scoreboard'))
                and all(value in dungeon_run_hud_raw for value in
                        (b'dungeonRun', b'DUNGEON RUN', b'DungeonRunTracker'))
                and b'DungeonRunTracker' in client_raw,
            'Dungeon Run HUD retains only bounded observed sidebar fields and its movable HUD owner')
    r.check(all(value in selftest_command_raw for value in
                (b'Dungeon Run HUD hook:', b'Spirit Bow Locator hook:')),
            '/velor selftest reports both new local runtime hooks')
    dungeon_readout_forbidden = (b'net/minecraft/network', b'sendPacket', b'sendQueue', b'sendChatMessage',
                                 b'playerController', b'windowClick', b'clickMouse', b'KeyBinding', b'MouseEvent',
                                 b'setPosition', b'setLocationAndAngles', b'java/lang/reflect')
    r.check(all(value not in dungeon_readout_raw for value in dungeon_readout_forbidden),
            'new dungeon readouts have no packet, chat, input, controller, position-write, or reflection path',
            'forbidden: ' + ', '.join(value.decode('ascii') for value in dungeon_readout_forbidden
                                       if value in dungeon_readout_raw))

    esp_settings_constants = constants_of('config/DungeonEspSettings')
    esp_controller_raw = raw_of('dungeonesp/DungeonEspController')
    esp_controller_family_raw = raw_family_of('dungeonesp/DungeonEspController')
    esp_tracker_raw = raw_of('dungeonesp/DungeonEspTracker')
    esp_batch_raw = raw_of('dungeonesp/DungeonEspRenderBatch')
    esp_marker_bounds_raw = raw_of('dungeonesp/DungeonEspMarkerBounds')
    esp_display_raw = raw_of('dungeonesp/DungeonEspMobDisplay')
    esp_command_raw = raw_of('command/ItemDebugCommand')
    r.check(all(value in esp_settings_constants for value in
                ('dungeonEsp.', 'Outline', 'Filled', 'Outline + Filled', 'Nearest closed door', 'All closed doors',
                 'Cyan', 'Gold', 'Purple')),
            'DungeonEspSettings packages the persisted modes, door scope and color palette')
    r.check(all(value in reference_catalog_raw for value in
                (b'Starred Mobs', b'Wither Door Style', b'Blood Door Color', b'Starred Mob Labels', b'Maximum Labels',
                 b'Hide Dead Mobs', b'Smooth Aspect Teleport', b'smooth-aspect-teleport'))
                and all(value not in reference_catalog_raw for value in
                        (b'Other Mobs', b'Force All Room Mobs', b'Include Bats', b'Include Invisible Mobs',
                         b'Copy Starred Mob Style', b'Apply Copied Style', b'Apply Style To All', b'esp-skeletor',
                         b'esp-tracers', b'esp-hide-zero-health', b'esp-default-preset', b'esp-subtle-preset'))
                and b'drawStyledBox' in esp_batch_raw,
            'flat ESP tables retain Starred Mobs and Door controls while old categories, tracers, mob presets and room-wide controls stay absent')
    r.check(all(value in esp_batch_raw for value in
                (b'DungeonEspMobDisplay', b'layoutMobLabels', b'drawLabelBackdrop', b'restoreWorldRenderState',
                 b'AssistSettings', b'hideDeadMobs', b'DungeonEspMarkerBounds', b'isLocalPlayerEntity'))
                and all(value in esp_marker_bounds_raw for value in (b'HALF_WIDTH', b'MIN_Y_OFFSET', b'HEIGHT'))
                and all(value in esp_display_raw for value in (b'MINIBOSS', b'SPECIAL', b'ENDERMAN', b'HARD_LABEL_CAP'))
                and b'SKELETOR' not in esp_display_raw and b'drawTracers' not in esp_batch_raw
                and b'HOSTILE' not in esp_display_raw,
            'Starred Mobs keeps bounded labels, local-player live exclusion, reliable direct Fel markers and no tracer draw path')
    r.check(all(value in esp_controller_raw for value in
                (b'DungeonEspTracker', b'WitherDoorCache', b'DungeonEspRenderBatch', b'DungeonEspMatcher',
                 b'AssistSettings', b'hideDeadMobs', b'SkullOwner', b'espDump'))
                # localPlayer lives in the controller's private EntitySource inner class, so inspect its class family.
                and b'localPlayer' in esp_controller_family_raw
                and all(value in esp_tracker_raw for value in
                        (b'localPlayer', b'directEndermanMarker', b'ENDERMAN_HEAD'))
                and all(value not in esp_tracker_raw for value in
                        (b'belowPlayerCeiling', b'hasStarredEndermanAssociation'))
                and all(value not in esp_controller_raw for value in
                        (b'forceRenderAllMobs', b'includeBats', b'includeInvisibleMobs'))
                and b'FORCE_ROOM_MOB' not in esp_tracker_raw
                and b'RenderWorldLastEvent' in esp_controller_raw and b'espdebug' in esp_command_raw
                and b'espdump' in esp_command_raw and b'MAX_CANDIDATES' in esp_tracker_raw,
            'Starred Mobs runtime retains timer caching, local-player/NPC separation, direct Fel head support and diagnostics without a generic room fallback')

    item_panel_constants = constants_of('gui/ItemAnimationPanelModel')
    hud_editor_raw = raw_of('gui/HudEditorScreen')
    r.check(all(value in item_panel_constants for value in
                ('item-animation-panel', 'Hook', 'Enable', 'Scale', 'Position', 'Animation', 'Options',
                 'Custom Hit/Slash Style', 'Custom Hit/Slash Speed', 'Smooth Swing', 'Smoothness',
                 'Remove Haste Effect', 'Loop Preview Swing', 'Preview Swing', 'Reset to Defaults'))
                and b'previewSwing' in item_animation_editor_raw and b'ItemAnimation' in item_animation_editor_raw,
            'standalone Item Animation panel retains only the requested real hand-renderer controls and Preview Swing')
    # Theme int constants are inlined into the editor by javac, so prove the named opaque palette in Theme and its
    # roundRectBorder consumer separately rather than requiring a field reference to survive reobfuscation.
    r.check(all(value not in item_panel_constants for value in ('Transparency', 'Opacity', 'Background'))
                and b'roundRectBorder' in item_animation_editor_raw and b'ITEM_ANIMATION_PANEL' in theme_raw
                and b'ITEM_ANIMATION_PANEL_BORDER' in theme_raw and b'drawDefaultBackground' not in item_animation_editor_raw
                and b'HudManager' not in item_animation_editor_raw
                and b'Hook: active' in hook_status_raw and b'Hook: failed (' in hook_status_raw
                and b'ItemAnimationHookStatus' in item_animation_editor_raw
                and b'isActive' in item_animation_editor_raw and b'isFailed' in item_animation_editor_raw,
            'Item Animation is an undimmed, opaque standalone panel with no HUD/world surface or retired background controls')
    r.check(b'LIVE HELD-ITEM PREVIEW' not in hud_editor_raw and b'drawItemAnimationEditorPreview' not in hud_editor_raw
                and b'ItemAnimationUiModel' not in hud_editor_raw and b'ITEM_ANIMATION' not in hud_editor_raw
                and b'ItemAnimationHookStatus' not in hud_editor_raw,
            'HUD Editor contains no Item Animation target, sample-panel surface, or renderer controls')
    r.check(all(value in reference_catalog_raw for value in
                (b'Shortcut', b'Wardrobe Shortcut', b'Wardrobe Number Keys', b'Pets Shortcut', b'item-lock-key',
                 b'Command Keybind', b'Open Command Keybind Editor'))
                and all(value in bindings_raw for value in (b'wardrobe-number-keys', b'pets-shortcut'))
                and b'open-command-keybind-editor' in settings_screen_raw and b'AppleKeybindButton' in settings_screen_raw
                and b'beginKeybindCapture' in settings_screen_raw and b'PETS_SHORTCUT_KEY' in client_raw
                and b'rebindPetsShortcutKey' in client_raw and b'resetShortcutKeysToDefaults' in client_raw,
            'Shortcut is a standalone card with W/P capture controls, Wardrobe Number Keys, explicit defaults and Command Keybind route')
    r.check(all(value in command_keybind_settings_raw for value in
                (b'assist.shortcut.commandKeybinds', b'assist.keybindCut.commandKeybinds', b'.entries', b'MAX_ENTRIES', b'normalizeCommand',
                 b'firstExecutableForKey'))
                and all(value in command_keybind_policy_raw for value in (b'COOLDOWN_MILLIS', b'shouldSend'))
                and all(value in command_keybind_controller_raw for value in
                        (b'Keyboard', b'CommandKeybindSettings', b'CommandKeybindPolicy'))
                and b'CommandKeybindController' in client_raw,
            'Command Keybind persistence, physical-edge cooldown policy and registered direct-user runtime are packaged')
    r.check(all(value in command_keybind_editor_raw for value in
                (b'Command Keybind', b'AppleKeybindButton', b'Key capture canceled', b'Key cleared',
                 b'Warning: conflicts with Sky VeLor ', b'Warning: conflicts with Minecraft ', b'AppleSettings',
                 b'CommandKeybindEditorLayout', b'drawTitleBar', b'drawScrollBar'))
                and all(value in command_keybind_editor_layout_raw for value in
                        (b'PANEL_WIDTH_RATIO', b'PANEL_HEIGHT_RATIO', b'COMPACT_ROW_BREAKPOINT', b'panelX', b'panelY'))
                and b'AppleToggle' not in command_keybind_editor_raw and b'drawDefaultBackground' not in command_keybind_editor_raw,
            'Command Keybind editor is an opaque centered responsive add/delete/capture/reset window with no per-entry toggle, focused space entry and visible collision warnings')
    r.check(all(value in shortcut_controller_raw for value in
                (b'PETS_SHORTCUT_KEY', b'Keyboard', b'/pets', b'petsWasDown', b'isFakepixel'))
                and all(value not in shortcut_controller_raw for value in
                        (b'isPressed', b'sendPacket', b'setPosition', b'clickMouse')),
            'Pets Shortcut remains one physical key-down edge with player/screen/server guards and no automation path')
    wardrobe_policy_values = (b'shouldOpenWardrobe', b'shouldSelectSlot', b'FIRST_WARDROBE_SLOT',
                              b'LAST_WARDROBE_SLOT', b'isWardrobeTitle')
    # Minecraft GUI/container type names and PlayerControllerMP.windowClick are reobfuscated in the shipped jar.
    # Require the stable Forge input event, LWJGL key source, local guard policy, command literal, and the 1.8.9
    # SRG window-click member here; source tests separately audit the readable GuiChest/ContainerChest gates.
    wardrobe_controller_values = (b'Keyboard', b'KeyboardInputEvent$Pre', b'WardrobeShortcutPolicy', b'/wardrobe',
                                  b'wardrobeNumberKeys', b'func_78753_a')
    wardrobe_forbidden_values = (b'sendPacket', b'setPosition', b'setLocationAndAngles', b'clickMouse', b'Thread.sleep')
    r.check(all(value in wardrobe_shortcut_policy_raw for value in wardrobe_policy_values)
                and all(value in wardrobe_shortcut_controller_raw for value in wardrobe_controller_values)
                and all(value not in wardrobe_shortcut_controller_raw for value in wardrobe_forbidden_values),
            'Wardrobe Shortcut remains a physical-edge, recognized-GuiChest direct command/slot helper without movement, packet, or repeated-click automation',
            'missing policy=%s controller=%s forbidden=%s' % (
                missing_raw_values(wardrobe_shortcut_policy_raw, wardrobe_policy_values),
                missing_raw_values(wardrobe_shortcut_controller_raw, wardrobe_controller_values),
                [value.decode('ascii') for value in wardrobe_forbidden_values if value in wardrobe_shortcut_controller_raw]))

    chat_settings_raw = raw_of('config/ChatQolSettings')
    tabs_raw = raw_of('feature/ChatChannelTabs')
    chat_surface_raw = raw_of('feature/ChatSurfaceRenderer')
    compact_raw = raw_of('chat/ChatCompactMessages')
    r.check(all(value in chat_settings_raw for value in
                (b'backgroundOpenFullArea', b'backgroundFitWidth', b'tabInactiveColor', b'tabBackgroundColor',
                 b'compactCounterColor', b'hoverColor'))
                and b'ChatSurfaceRenderer' in tabs_raw and b'ChatSurfaceRenderer' in chat_surface_raw
                and b'GROUP_ID_BASE' in compact_raw and b'COUNTER_LINE_ID' not in compact_raw,
            'Chat QoL runtime retains one default chat renderer, custom tabs and exact compact-message replacement')

    lock_settings_constants = constants_of('itemlockswap/ItemLockSwapSettings')
    lock_overlay_raw = raw_of('itemlockswap/ItemLockSwapOverlay')
    lock_controller_raw = raw_of('itemlockswap/ItemLockSwapController')
    r.check(all(value in lock_settings_constants for value in
                ('itemLockSwap.', 'itemLockSwap.locks', 'itemLockSwap.pairs', 'itemLockSwap.cooldownTenths',
                 'itemLockSwap.showIconsOnHotbar', 'itemLockSwap.swapLineMode', 'itemLockSwap.swapLineColor'))
                and all(value in lock_overlay_raw for value in (b'drawContainerForeground', b'drawBadge', b'drawRoundLine'))
                and b'beginInternalSwap' in lock_controller_raw,
            'Item Lock/Swap runtime rendering and safe pair logic remain while its focused settings screen is removed')
    r.check(all(value in raw_of('itemlockswap/ItemLockSwapSettings') for value in
                (b'isLegalPair', b'ILLEGAL', b'pairCountForSlot', b'partnerForSwap', b'advanceSwapPartner', b'removePair'))
                and all(value in lock_overlay_raw for value in (b'drawPersistentPairLines', b'pairCountForSlot'))
                and all(value in lock_controller_raw for value in
                        (b'partnerForSwap', b'advanceSwapPartner', b'Cannot pair: one slot must be in the hotbar')),
            'Item Lock/Swap accepts only hotbar-ended pairs, preserves every legal line and cycles shared endpoints after successful swaps')
    fishing_constants = constants_of('fishing/FishingStats')
    fishing_hud_raw = raw_of('fishing/FishingHud')
    sea_constants = constants_of('fishing/SeaCreatureHud')
    r.check(all(name in fishing_constants for name in ('commonCreatureCount', 'legendaryCreatureCount'))
                and b'commonCreature' in fishing_hud_raw and 'shouldScan' in sea_constants,
            'Fishing counters and Sea Creature HP runtime remain after their controls move to Fishing')

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
    r.check('com.arena.pekaclient.render.itemanimation' in loading_constants
                and 'com/arena/pekaclient/assist/visual/AspectTeleportCameraTransformer' in loading_constants,
            'loading plugin declares its coremod exclusion and registers the fail-closed Aspect render transformer')
    # Coremod setup classes must not link a Minecraft client/world class before LaunchClassLoader installs them.
    for cls in ('render/itemanimation/ItemAnimationLoadingPlugin',
                'render/itemanimation/ItemAnimationTransformer',
                'render/itemanimation/ItemAnimationHookStatus',
                'assist/visual/AspectTeleportCameraTransformer',
                'assist/visual/AspectTeleportCameraHookStatus',
                'itemlockswap/ItemLockSwapTransformer',
                'itemlockswap/ItemLockSwapHookStatus'):
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
    smooth_hook_raw = raw_of('render/itemanimation/ItemAnimationHooks')
    r.check(b'SwingEasing' in smooth_hook_raw and 'ease' in members_of('render/itemanimation/ItemAnimationHooks')
                and 'isSmoothSwingEnabled' in members_of('render/itemanimation/ItemAnimationHooks')
                and 'getSmoothness' in members_of('render/itemanimation/ItemAnimationHooks'),
            'Smooth Swing uses the stateless first-person easing bridge before optional hit/slash styling')
    if MOD_PACKAGE + 'config/AppleSettings.class' in names:
        settings_constants = constants_of('config/AppleSettings')
        settings_raw = raw_of('config/AppleSettings')
        r.check('itemAnimation.enabled' in settings_constants,
                'default-on Item Animation master toggle is persisted')
        r.check('itemAnimation.smoothSwing' in settings_constants and 'itemAnimation.smoothness' in settings_constants
                    and b'itemAnimationSmoothSwing' in settings_raw and b'itemAnimationSmoothness' in settings_raw,
                'default-on Smooth Swing and bounded smoothness persistence are packaged')
        r.check(b'itemAnimationAutoFixHitSlash' not in settings_raw,
                'AppleSettings retains no hidden Auto Fix setting state')
        r.check('chat.qol.' in constants_of('config/ChatQolSettings')
                    and b'ChatQolSettings' in settings_raw,
                'AppleSettings delegates native chat.qol persistence and migration to the bounded Chat QoL settings model')
        r.check('config.version' in settings_constants and 'particles.noBlockParticles' in settings_constants
                    and 'chat.qol.messages.compact' in constants_of('config/ChatQolSettings')
                    and 'applyConfigurationVersionUpgrade' in members_of('config/AppleSettings')
                    and 'saveConfigurationVersion' in members_of('config/AppleSettings'),
                'AppleSettings ships the one-time schema-v2 Compact Messages and No block particles migration')
        r.check('fishing.nearbyEncounterAlert' in settings_constants,
                'AppleSettings persists the safe-default nearby encounter title toggle')
        r.check('scrollSpeed' in settings_constants and 'smoothScrolling' in settings_constants
                    and b'target.default.scroll.speed' not in settings_raw,
                'AppleSettings keeps one global scroll policy and ignores retired split-chat target values')
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
