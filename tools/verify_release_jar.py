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

Usage:  python3 tools/verify_release_jar.py <jar> [--version Beta-0.10.8.9.7v] [--mcmod-version 0.10.8.9.7]
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
    'gui/ItemAnimationUiModel',
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
    'assist/inventory/InventoryAssistController',
    'assist/inventory/RomanNumeralPolicy',
    'assist/inventory/TooltipWindow',
    'assist/movement/AutoSprintController',
    'assist/movement/AutoSprintPolicy',
    'assist/mining/MiningAssistController',
    'assist/mining/MiningAbilityPolicy',
    'assist/mining/PuzzlerRoute',
    'assist/dungeon/DungeonBossTracker',
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
    'hud/MiningAssistantHud',
    'hud/DungeonBossHud',
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
    # Stage 2 folds every feature-specific screen into AppleSettingsScreen. HUD and Item Animation stay in
    # HudEditorScreen, so no generic, Chat, ESP, or Mod Settings editor class may ship.
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
    ap.add_argument('--version', help='expected display/manifest version, e.g. Beta-0.10.8.9.7v')
    ap.add_argument('--mcmod-version', help='expected numeric mcmod.info version, e.g. 0.10.8.9.7')
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

    r.check(all(MOD_PACKAGE + value + '.class' not in names for value in
                ('gui/ChatEditorPreview', 'gui/DungeonEspPreviewRenderer')),
            'retired editor sample-panel classes are absent from the release jar')

    # --- Stage 2 inline settings / retained runtime proof ----------------------
    # The catalog owns presentation only. It must expose every established owner through persisted collapsed
    # dropdowns, while the old focused-screen framework and all preview surfaces are genuinely absent.
    settings_screen_raw = raw_of('gui/AppleSettingsScreen')
    reference_catalog_raw = raw_of('gui/ReferenceSettingsCatalog') + raw_of('gui/ReferenceSettingsCatalog$Tab')
    bindings_raw = raw_of('gui/InlineSettingsBindings')
    feature_state_raw = raw_of('gui/FeatureUiState')
    preferences_raw = raw_of('config/FeatureUiPreferences')
    assist_settings_raw = raw_of('config/AssistSettings')
    apple_settings_raw = raw_of('config/AppleSettings')
    client_raw = raw_of('PekaClientMod')
    command_actions_raw = raw_of('command/ClientActions')
    theme_raw = raw_of('render/Theme')
    rail_layout_raw = raw_of('gui/SettingsRailLayout')
    page_layout_raw = raw_of('gui/SettingsPageLayout')
    ui_render_raw = raw_of('render/UIRender')
    hud_manager_raw = raw_of('hud/HudManager')
    hud_element_raw = raw_of('hud/HudElement')
    item_slots_raw = raw_of('hud/ItemSlots')

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
                 b'FishingStats', b'StyleClipboard')) and b'setProperty' not in bindings_raw,
            'InlineSettingsBindings writes existing feature owners and keeps the ESP clipboard session-only')
    r.check(all(value in feature_state_raw for value in (b'dropdowns', b'ui.feature.dropdown.', b'MAX_ENTRIES'))
                and b'dropdownOpen' in preferences_raw and b'setDropdownOpen' in preferences_raw,
            'collapsed inline dropdown state defaults separately from feature configuration and persists under ui.feature.dropdown')
    r.check(all(value in settings_screen_raw for value in
                (b'HIT_DISCLOSURE', b'FeatureUiPreferences', b'childBlockHeight', b'reserveValueDropdown',
                 b'expandSearchResult', b'drawFullscreenGlyph', b'open-hud-editor', b'item-animation-open-editor'))
                and b'AppleToggle' in settings_screen_raw and b'AppleDropdown' in settings_screen_raw
                and b'AppleSegmented' not in settings_screen_raw,
            'AppleSettingsScreen uses persisted recursive inline dropdowns, parent-expanding search, standard toggle pills and flow-reserved value dropdowns')
    r.check(all(value in reference_catalog_raw for value in
                (b'Home', b'Gameplay', b'Dungeon', b'Farming', b'Mining', b'Fishing', b'Visuals', b'System',
                 b'No farming features yet', b'Mining Assistant', b'Fishing Tracker', b'Slayer Tracker')),
            'catalog packages the requested eight-tab sidebar, empty Farming message, and genuine Mining/Fishing moves')
    r.check(all(value in reference_catalog_raw for value in
                (b'Dungeon Map', b'Terminal Helper', b'Spirit Leap Picker', b'Boss Health HUD', b'Floor VII Overlay',
                 b'Mob ESP', b'Door ESP', b'Maxor', b'Storm', b'Goldor', b'Necron', b'Power', b'Apex', b'Soul',
                 b'Ice', b'Flame', b'Item Lock and Swap')),
            'inline Dungeon groups retain map, no-master Terminal Helper, Spirit Leap, boss HUD, M7 and compact Mob/Door controls')
    r.check(all(value in reference_catalog_raw for value in
                (b'Background', b'Messages', b'Tabs', b'Menu and Highlight', b'Reset', b'Add custom tab',
                 b'Custom accent colour', b'Filter type', b'Message sound', b'Open chat background'))
                and b'ChatEditorScreen' not in settings_screen_raw and b'openChatSettings' in command_actions_raw,
            'Chat settings and custom-tab editing live inline in the five requested groups; commands route to that owner')
    r.check(all(value in reference_catalog_raw for value in
                (b'HUD Editor', b'Item Animation Editor', b'Preview Swing'))
                and b'HudEditorScreen' in settings_screen_raw,
            'only HUD Editor and the real Item Animation hand editor remain reachable')
    r.check(b'CHAT_PREVIEW_' not in theme_raw and b'ChatEditorPreview' not in settings_screen_raw
                and b'DungeonEspPreviewRenderer' not in settings_screen_raw,
            'no retired live-preview color surface or focused-editor preview route remains')
    r.check(all(value in settings_screen_raw for value in
                (b'SettingsRailLayout', b'SettingsPageLayout', b'Settings rail', b'Settings card', b'Settings row'))
                and all(value in rail_layout_raw for value in (b'ENTRY_HEIGHT', b'ENTRY_GAP', b'COMPACT_COLUMNS'))
                and b'NARROW_CONTENT_WIDTH' in page_layout_raw,
            'settings shell retains shared scale-1–4 spacing, compact rail reflow and debug bounds')
    r.check(all(value in ui_render_raw for value in (b'restoreVanillaGuiState', b'restoreFrameState', b'frameDepth'))
                and all(value in hud_manager_raw for value in (b'backgroundOnHoverOnly', b'hoveredInEditor'))
                and b'alphaMul' in hud_element_raw and b'restoreFrameState' in item_slots_raw,
            'HUD/editor rendering packages exception-safe state cleanup and a neutral GUI restoration baseline')
    r.check(b'applyNeutralDark' in theme_raw and b'ACCENT_BLUE' in theme_raw
                and b'appearance-theme' not in reference_catalog_raw and b'appearance-accent' not in reference_catalog_raw,
            'one fixed neutral-dark blue Theme remains while legacy palette keys stay out of the visible shell')

    # Retained Assist, ESP, Chat, Item Lock/Swap, Fishing and HUD runtime paths continue to be checked independently
    # of their presentation location.
    sync_client_raw = raw_of('assist/sync/DungeonSyncClient')
    sync_messages_raw = raw_of('assist/sync/DungeonSyncMessages')
    sync_manager_raw = raw_of('assist/sync/DungeonSyncManager')
    terminal_raw = raw_of('assist/dungeon/DungeonTerminalAssistController')
    mining_raw = raw_of('assist/mining/MiningAssistController')
    r.check(all(value in assist_settings_raw for value in
                (b'assist.visual.hurtCameraShake', b'assist.movement.autoSprint', b'assist.dungeon.sync.enabled',
                 b'assist.dungeon.sync.endpoint', b'assist.dungeon.terminal.autoClick', b'hurtCameraShakeMultiplier'))
                and b'itemGuard' not in assist_settings_raw,
            'AssistSettings retains bounded Hurt Camera Shake, inventory, movement, terminal and sync configuration owners')
    r.check(all(value in sync_client_raw for value in (b'wss', b'Sec-WebSocket-Key', b'sec-websocket-accept', b'SHA-1'))
                and all(value in sync_messages_raw for value in (b'welcome', b'puzzle_discovered'))
                and b'DungeonSyncEnvironment' in sync_manager_raw and b'rememberSyncedPuzzle' in sync_manager_raw,
            'Dungeon Sync packages WSS validation, allow-listed events, local environment hashing and map memory')
    r.check(b'lastAutoFingerprint' in terminal_raw and b'lastAutoSlot' in terminal_raw
                and b'TerminalParsePolicy' in terminal_raw and b'sendChatMessage' not in mining_raw,
            'terminal automation remains fingerprint-guarded and the Puzzler helper remains read-only')

    esp_settings_constants = constants_of('config/DungeonEspSettings')
    esp_controller_raw = raw_of('dungeonesp/DungeonEspController')
    esp_tracker_raw = raw_of('dungeonesp/DungeonEspTracker')
    esp_batch_raw = raw_of('dungeonesp/DungeonEspRenderBatch')
    esp_display_raw = raw_of('dungeonesp/DungeonEspMobDisplay')
    esp_command_raw = raw_of('command/ItemDebugCommand')
    r.check(all(value in esp_settings_constants for value in
                ('dungeonEsp.', 'Outline', 'Filled', 'Outline + Filled', 'Nearest closed door', 'All closed doors',
                 'Cyan', 'Gold', 'Purple')),
            'DungeonEspSettings packages the persisted modes, door scope and color palette')
    r.check(all(value in reference_catalog_raw for value in
                (b'Force Render All Mobs in Room', b'Wither Door Style', b'Blood Door Color', b'Apply Copied Style',
                 b'Apply This Style to All', b'Mob Category Labels', b'Maximum Mob Labels'))
                and b'drawStyledBox' in esp_batch_raw,
            'inline ESP tables retain every cached Mob/Door control without a hitbox or door preview surface')
    r.check(all(value in esp_batch_raw for value in
                (b'DungeonEspMobDisplay', b'layoutMobLabels', b'drawLabelBackdrop', b'restoreWorldRenderState'))
                and all(value in esp_display_raw for value in (b'MINIBOSS', b'SPECIAL', b'ENDERMAN', b'HARD_LABEL_CAP')),
            'Mob ESP keeps bounded hierarchy labels, collision-aware layout and world-render state restoration')
    r.check(all(value in esp_controller_raw for value in
                (b'DungeonEspTracker', b'WitherDoorCache', b'DungeonEspRenderBatch', b'DungeonEspMatcher',
                 b'forceRenderAllMobs', b'includeBats', b'includeInvisibleMobs', b'SkullOwner', b'espDump'))
                and b'RenderWorldLastEvent' in esp_controller_raw and b'espdebug' in esp_command_raw
                and b'espdump' in esp_command_raw and b'MAX_CANDIDATES' in esp_tracker_raw,
            'Dungeon ESP runtime retains its timer cache, filtering, head support and diagnostics')

    item_ui_constants = constants_of('gui/ItemAnimationUiModel')
    hud_editor_raw = raw_of('gui/HudEditorScreen')
    r.check(all(value in item_ui_constants for value in
                ('Item Animation', 'item-animation-open-editor', 'Scale', 'Position', 'Preview Swing',
                 'Loop Preview Swing', 'Reset to Defaults'))
                and b'previewSwing' in hud_editor_raw and b'ItemAnimation' in hud_editor_raw,
            'Item Animation retains the real hand renderer controls and renderer-only Preview Swing')
    r.check(b'LIVE HELD-ITEM PREVIEW' not in hud_editor_raw and b'drawItemAnimationEditorPreview' not in hud_editor_raw,
            'HUD Editor contains no Item Animation sample-panel surface')

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
    r.check('com.arena.pekaclient.render.itemanimation' in loading_constants,
            'loading plugin declares a transformer exclusion for its own coremod package')
    # Coremod setup classes must not link a Minecraft client/world class before LaunchClassLoader installs them.
    for cls in ('render/itemanimation/ItemAnimationLoadingPlugin',
                'render/itemanimation/ItemAnimationTransformer',
                'render/itemanimation/ItemAnimationHookStatus',
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
