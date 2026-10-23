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

Usage:  python3 tools/verify_release_jar.py <jar> [--version Beta-0.10.8.9.5v] [--mcmod-version 0.10.8.9.5]
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
    'render/UIRender',
    'render/Theme',
    'hud/HudElement',
    'hud/ItemSlots',
    # Retained only as a binary-compatible alias; it must not be the visible entry point.
    'gui/AppleHubScreen',
    'gui/FeatureUiState',
    'gui/Row',
    'gui/FeatureSettingsUiModel',
    'gui/FeatureSettingsEditorScreen',
    'gui/FeatureSettingsEditors',
    'gui/DungeonEspEditorSpecs',
    'gui/DungeonEspPreviewRenderer',
    'config/FeatureUiPreferences',
    'gui/DungeonEspUiModel',
    'gui/QualityOfLifeUiModel',
    'gui/ModSettingsUiModel',
    'gui/ChatUiModel',
    'gui/ChatEditorLayout',
    'gui/ChatEditorScreen',
    'gui/ChatEditorPreview',
    'gui/GuiLayoutToolkit',
    'gui/GeneralMetricsLayout',
    'gui/ModSettingsScreen',
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
    'gui/FishingUiModel',
    'gui/AppleGuiButton',
    'gui/HudEditorScreen',
    'gui/HudEditorLayout',
    'gui/HudEditorSelectionPolicy',
    'gui/ItemAnimationUiModel',
    'gui/GuiSettingsAuditModel',
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
    'hud/StatsHud',
    'hud/StatsHudText',
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
    ap.add_argument('--version', help='expected display/manifest version, e.g. Beta-0.10.8.9.5v')
    ap.add_argument('--mcmod-version', help='expected numeric mcmod.info version, e.g. 0.10.8.9.5')
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

    # --- Assist and retained migration proof --------------------------------
    # Presence alone is not enough: these checks tie persisted safe-default controls, reference-shell labels,
    # lifecycle, guarded terminal path, and privacy-constrained sync transport together in the jar.
    assist_settings_raw = raw_of('config/AssistSettings')
    settings_screen_raw = raw_of('gui/AppleSettingsScreen')
    # Human rail labels live on the nested enum class; include it with the outer catalog for ownership checks.
    reference_catalog_raw = raw_of('gui/ReferenceSettingsCatalog') + raw_of('gui/ReferenceSettingsCatalog$Tab')
    # Labels/data live in the immutable catalog while interaction owners live in the native screen.
    settings_surface_raw = settings_screen_raw + reference_catalog_raw
    feature_editors_raw = raw_of('gui/FeatureSettingsEditors')
    sync_client_raw = raw_of('assist/sync/DungeonSyncClient')
    sync_messages_raw = raw_of('assist/sync/DungeonSyncMessages')
    sync_manager_raw = raw_of('assist/sync/DungeonSyncManager')
    terminal_raw = raw_of('assist/dungeon/DungeonTerminalAssistController')
    mining_raw = raw_of('assist/mining/MiningAssistController')
    r.check(all(value in assist_settings_raw for value in
                (b'assist.visual.hurtCameraShake', b'assist.movement.autoSprint', b'assist.dungeon.sync.enabled',
                 b'assist.dungeon.sync.endpoint', b'assist.dungeon.terminal.autoClick', b'hurtCameraShakeMultiplier'))
                and b'itemGuard' not in assist_settings_raw,
            'AssistSettings packages bounded Hurt Camera Shake plus retained inventory, movement, terminal, and sync controls')
    r.check(b'FeatureSettingsEditors' in settings_screen_raw and b'Dungeon Sync' in settings_surface_raw
                and b'Hurt Camera Shake' in settings_surface_raw and b'Sprint while holding forward' in settings_surface_raw
                and b'Auto Sprint' not in feature_editors_raw
                and b'Item Protection' not in settings_surface_raw and b'Item Protection' not in feature_editors_raw
                and all(value in feature_editors_raw for value in
                        (b'Pickaxe Ability Ready Alert', b'Terminal Auto Click', b'Terminal Waypoints')),
            'reference settings shell exposes direct shake/sprint controls and has no retired Item Protection route')
    r.check(all(value in sync_client_raw for value in
                (b'wss', b'Sec-WebSocket-Key', b'sec-websocket-accept', b'SHA-1'))
                and all(value in sync_messages_raw for value in (b'welcome', b'puzzle_discovered'))
                and b'DungeonSyncEnvironment' in sync_manager_raw and b'rememberSyncedPuzzle' in sync_manager_raw,
            'Dungeon Sync packages WSS handshake validation, allow-listed events, local environment hashing, and map memory')
    r.check(b'lastAutoFingerprint' in terminal_raw and b'lastAutoSlot' in terminal_raw
                and b'TerminalParsePolicy' in terminal_raw and b'sendChatMessage' not in mining_raw,
            'terminal automation remains fingerprint-guarded and the Puzzler helper remains read-only')
    item_lock_hooks_raw = raw_of('itemlockswap/ItemLockSwapHooks')
    lang_raw = zf.read('assets/pekaclient/lang/en_US.lang') if 'assets/pekaclient/lang/en_US.lang' in names else b''
    r.check(b'ItemGuard' not in item_lock_hooks_raw and b'ITEM_GUARD_KEY' not in raw_of('PekaClientMod')
                and b'itemGuard' not in lang_raw and b'ItemLockSwapProtection' in item_lock_hooks_raw
                and b'ItemLockSwapController' in item_lock_hooks_raw,
            'retired Item Protection has no runtime/keybind route while independent Item Lock / Swap remains active')
    visual_hooks_raw = raw_of('assist/visual/VisualAssistHooks')
    visual_transformer_raw = raw_of('assist/visual/VisualAssistTransformer')
    visual_status_raw = raw_of('assist/visual/VisualAssistHookStatus')
    client_mod_raw = raw_of('PekaClientMod')
    selftest_raw = raw_of('command/SvCommand')
    # The narrow transformer carries the explicit EntityRenderer aliases/float descriptor and calls one owned
    # amplitude helper. The persisted percentage stays in AssistSettings; no player hurt-time fallback is allowed.
    shake_ui = (b'assist.visual.hurtCameraShake' in assist_settings_raw and b'Hurt Camera Shake' in settings_surface_raw
                and b'No Hurt Cam' not in settings_surface_raw and b'No Hurt Camera' not in feature_editors_raw)
    shake_hook_tokens = tuple(value in visual_transformer_raw for value in
                              (b'bfk', b'(F)V', b'VisualAssistHooks'))
    shake_hook = all(shake_hook_tokens) and all(value in visual_hooks_raw for value in
                                                (b'scaleHurtCameraTilt', b'hurtCameraShakeMultiplier'))
    shake_no_fallback = (b'zeroFallbackVisualTilt' not in visual_hooks_raw
                         and b'zeroFallbackVisualTilt' not in client_mod_raw
                         and b'shouldUseVisualFallback' not in visual_hooks_raw)
    shake_status = (b'hurt camera shake' in selftest_raw and b'hurtCameraInjected' in visual_status_raw)
    r.check(shake_ui and shake_hook and shake_no_fallback and shake_status,
            'Hurt Camera Shake persists 0-100% and scales only the narrow hurt-camera amplitude without a fallback',
            'ui=%s hook=%s tokens=%s noFallback=%s status=%s' %
            (shake_ui, shake_hook, shake_hook_tokens, shake_no_fallback, shake_status))
    auto_sprint_raw = raw_of('assist/movement/AutoSprintController')
    auto_sprint_policy_raw = raw_of('assist/movement/AutoSprintPolicy')
    r.check(b'assist.movement.autoSprint' in assist_settings_raw and b'Sprint while holding forward' in settings_surface_raw
                and b'Sprints automatically while you hold forward.' in raw_of('gui/QualityOfLifeUiModel')
                and b'Auto Sprint' not in feature_editors_raw
                # Minecraft members are SRG-renamed by reobfJar. The static KeyBinding setter is func_74510_a;
                # the policy and controller ownership names remain stable in the packed mod classes.
                and all(value in auto_sprint_raw for value in
                        (b'AutoSprintPolicy', b'holdSprintKey', b'releaseSprintKey', b'ownedSprintKeyCode', b'func_74510_a'))
                and b'setSprinting' not in auto_sprint_raw and b'C0BPacketEntityAction' not in auto_sprint_raw
                and all(value in auto_sprint_policy_raw for value in (b'shouldHoldSprintKey', b'mayStart')),
            'Auto Sprint owns only vanilla sprint-key state, releases on forward loss, and sends no custom packets')

    # --- Cached Dungeon ESP proof --------------------------------------------
    # This release must not silently fall back to the retired per-frame MobHighlight path. The packaged controller,
    # fixed tracker/cache, settings schema and hub groups together prove the new ownership route is present.
    esp_settings_constants = constants_of('config/DungeonEspSettings')
    esp_hub_constants = constants_of('gui/DungeonEspUiModel')
    esp_controller_raw = raw_of('dungeonesp/DungeonEspController')
    esp_tracker_raw = raw_of('dungeonesp/DungeonEspTracker')
    esp_command_raw = raw_of('command/ItemDebugCommand')
    esp_editor_raw = raw_of('gui/DungeonEspEditorSpecs')
    esp_preview_raw = raw_of('gui/DungeonEspPreviewRenderer')
    esp_batch_raw = raw_of('dungeonesp/DungeonEspRenderBatch')
    esp_display_raw = raw_of('dungeonesp/DungeonEspMobDisplay')
    esp_editor_screen_raw = raw_of('gui/FeatureSettingsEditorScreen')
    layout_toolkit_raw = raw_of('gui/GuiLayoutToolkit')
    ui_render_raw = raw_of('render/UIRender')
    theme_raw = raw_of('render/Theme')
    apple_settings_raw = raw_of('config/AppleSettings')
    hud_element_raw = raw_of('hud/HudElement')
    hud_manager_raw = raw_of('hud/HudManager')
    item_slots_raw = raw_of('hud/ItemSlots')
    category_shell_tokens = (b'Quality of Life', b'Mining', b'Slayer', b'Fishing', b'HUD Hub', b'Settings theme',
                             b'Forest & Cyan', b'Black & Blue')
    category_screen_tokens = (b'appearance-theme', b'refreshReferenceTheme', b'windowOffsetX', b'DRAG TITLE BAR')
    category_settings_tokens = (b'appearance.settingsTheme', b'SETTINGS_THEME_BLACK_BLUE', b'SETTINGS_THEME_FOREST_CYAN')
    missing_catalog = [value.decode('ascii') for value in category_shell_tokens if value not in reference_catalog_raw]
    missing_screen = [value.decode('ascii') for value in category_screen_tokens if value not in settings_screen_raw]
    missing_settings = [value.decode('ascii') for value in category_settings_tokens if value not in apple_settings_raw]
    direct_category_shell = (not missing_catalog and not missing_screen and not missing_settings
                             and b'HIT_ADVANCED' not in settings_screen_raw
                             and b'Advanced settings shown' not in settings_screen_raw
                             and b'applyBlackBluePalette' in theme_raw)
    r.check(direct_category_shell,
            'direct category shell removes the visible Advanced gate and persists Forest & Cyan or Black & Blue',
            'missing catalog=%s screen=%s settings=%s legacyGate=%s themeHelper=%s' %
            (missing_catalog, missing_screen, missing_settings,
             b'HIT_ADVANCED' in settings_screen_raw or b'Advanced settings shown' in settings_screen_raw,
             b'applyBlackBluePalette' in theme_raw))
    r.check(all(value in ui_render_raw for value in (b'restoreVanillaGuiState', b'restoreFrameState', b'frameDepth'))
                and all(value in hud_manager_raw for value in (b'backgroundOnHoverOnly', b'hoveredInEditor'))
                and b'alphaMul' in hud_element_raw and b'restoreFrameState' in item_slots_raw,
            'HUD/editor rendering packages exception-safe state cleanup and a neutral GUI restoration baseline')
    r.check(all(value in esp_settings_constants for value in
                ('dungeonEsp.', 'Outline', 'Filled', 'Outline + Filled', 'Nearest closed door',
                 'All closed doors', 'Cyan', 'Gold', 'Purple')),
            'DungeonEspSettings packages the persisted modes, door scope and color palette')
    r.check(all(value in esp_hub_constants for value in
                ('Mob ESP', 'Door ESP', 'Performance', 'Presets', 'Force Render All Mobs in Room', 'Enderman',
                 'Skeletor', 'Other Mobs')),
            'DungeonEspUiModel retains the pre-flat option inventory for reachability coverage')
    r.check(all(value in settings_surface_raw for value in (b'Mob highlights', b'Door highlights'))
                and all(value in esp_editor_raw for value in
                        (b'Enable Dungeon ESP', b'Force Render All Mobs in Room', b'Wither Door',
                         b'Blood Door Color', b'Default Preset', b'Subtle Preset', b'Apply Copied Style',
                         b'Apply This Style to All'))
                and all(value in esp_preview_raw for value in (b'drawStyledBox', b'drawMobThumbnail', b'drawDoorThumbnail'))
                and b'drawStyledBox' in esp_batch_raw,
            'flat Dungeon rows route every ESP control through shared Mob/Door editors and shared live-style previews')
    r.check(all(value in esp_editor_raw for value in (b'Mob Category Labels', b'Maximum Mob Labels'))
                and all(value in esp_hub_constants for value in ('Mob Category Labels', 'Maximum Mob Labels'))
                and all(value in esp_batch_raw for value in
                        (b'DungeonEspMobDisplay', b'layoutMobLabels', b'drawLabelBackdrop', b'restoreWorldRenderState'))
                and all(value in esp_display_raw for value in (b'MINIBOSS', b'SPECIAL', b'ENDERMAN', b'HARD_LABEL_CAP')),
            'Mob ESP packages bounded hierarchy labels, collision-aware layout and world-render state restoration')
    r.check(all(value in esp_editor_screen_raw for value in
                (b'splitResponsiveLivePreview', b'previewVisible', b'restoreFrameState'))
                and b'splitResponsiveLivePreview' in layout_toolkit_raw
                and b'restoreFrameState' in ui_render_raw,
            'Dungeon ESP editor preserves usable full-width controls on compact GUIs and restores 2D state after preview rendering')
    # ReobfJar remaps Minecraft class names, so inspect durable Sky VeLor fields/strings rather than MCP class names.
    r.check(all(value in esp_controller_raw for value in
                (b'DungeonEspTracker', b'WitherDoorCache', b'DungeonEspRenderBatch', b'DungeonEspMatcher',
                 b'forceRenderAllMobs', b'includeBats', b'includeInvisibleMobs', b'SkullOwner', b'espDump'))
                and b'RenderWorldLastEvent' in esp_controller_raw and b'espdebug' in esp_command_raw
                and b'espdump' in esp_command_raw,
            'DungeonEspController packages timer cache, hostile filtering, Enderman-head support, and espdump telemetry')
    r.check(b'MAX_CANDIDATES' in esp_tracker_raw and b'IdentityHashMap' not in esp_tracker_raw
                and b'SPLIT_TAG' in esp_tracker_raw and b'ENDERMAN_HEAD' in esp_tracker_raw
                and b'SKELETOR' in esp_tracker_raw and b'FORCE_ROOM_MOB' in esp_tracker_raw,
            'DungeonEspTracker packages fixed storage, split tags, Skeletor, force filtering, and no identity sets')

    # --- Item Animation GUI reachability and ownership proof -----------------
    # A standalone Item Animation class is not enough: the direct HUD Hub route and renderer panel must
    # both reference the same model, without recreating renderer controls in the main category shell.
    ui_constants = constants_of('gui/ItemAnimationUiModel')
    r.check(all(value in ui_constants for value in
                ('Item Animation', 'item-animation-open-editor', 'Scale', 'Position',
                 'Animation', 'Options', 'X', 'Y', 'Z', 'Reset Position', 'Remove Haste Effect',
                 'Custom Hit/Slash Style', 'Custom Hit/Slash Speed', 'Smooth swing', 'Smoothness',
                 'Swing blur unavailable (no safe translucent ghosts)', 'Loop Preview Swing', 'Preview Swing', 'Reset to Defaults')),
            'ItemAnimationUiModel packages the direct HUD Hub route and required compact editor controls')
    forbidden_item_animation_terms = ('transparency', 'opacity', 'background')
    r.check('On/Off' not in ui_constants and 'Auto Fix Hit/Slash' not in ui_constants
                and 'Swing blur' not in ui_constants and 'Blur intensity' not in ui_constants
                and not any(term in value.lower() for value in ui_constants
                            for term in forbidden_item_animation_terms),
            'ItemAnimationUiModel excludes duplicate Hub switches, background controls, and unsafe blur controls')
    model_ref = b'com/arena/pekaclient/gui/ItemAnimationUiModel'
    audit_ref = b'com/arena/pekaclient/gui/GuiSettingsAuditModel'
    layout_ref = b'com/arena/pekaclient/gui/HudEditorLayout'
    flat_model_raw = raw_of('gui/FeatureSettingsUiModel')
    r.check(b'HudEditorScreen' in settings_screen_raw and b'AppleToggle' in settings_screen_raw
                and b'AppleButton' in settings_screen_raw and b'AppleSlider' in settings_screen_raw
                and b'ReferenceSettingsCatalog' in settings_screen_raw
                and all(value in flat_model_raw for value in
                        (b'simple', b'slider', b'editor-route', b'editor-button'))
                and all(value in settings_surface_raw for value in
                        (b'Home', b'Quality of Life', b'Mining', b'Slayer', b'Fishing', b'Dungeon', b'HUD Hub', b'System',
                         b'HUD Editor', b'Stats Display'))
                and b'Item Animation' in raw_of('gui/ItemAnimationUiModel')
                and b'Stats Display' in raw_of('gui/GuiSettingsAuditModel')
                and all(value not in settings_screen_raw for value in
                        (b'PAGE_NAMES', b'drawSidebar', b'drawTabs', b'FeatureRowControl', b'FeatureTreeLayout')),
            'reference rail keeps direct controls, inline Item Animation details, and distinct HUD editor access')
    r.check(b'HudSettingsBuilder' not in settings_screen_raw,
            'reference shell keeps HUD customization in the HUD editor rather than duplicating it')
    feature_editor_titles_raw = raw_of('gui/FeatureSettingsEditors')
    hud_editor_titles_raw = raw_of('gui/HudEditorScreen')
    chat_editor_titles_raw = raw_of('gui/ChatEditorScreen')
    esp_editor_titles_raw = raw_of('gui/DungeonEspEditorSpecs')
    mod_settings_titles_raw = raw_of('gui/ModSettingsScreen')
    r.check(all(value in feature_editor_titles_raw for value in
                (b'Appearance Settings', b'Performance Settings', b'Inventory Assist Settings',
                 b'Movement and Shortcuts Settings', b'Item Lock and Swap Settings', b'Commands Settings',
                 b'Mining Assistant Settings', b'Spirit Leap GUI Settings', b'Terminal Assist Settings',
                 b'Floor VII (M7) Overlay Settings', b'Catch Counter Settings', b'Slayer Tracker Settings'))
                and all(value in esp_editor_titles_raw for value in (b'Mob ESP Settings', b'Door ESP Settings'))
                and b'Chat Settings' in chat_editor_titles_raw and b'Chat Editor' not in chat_editor_titles_raw
                and all(value in hud_editor_titles_raw for value in
                        (b'Item Animation Settings', b'Dungeon Map Settings', b'Player Doll Settings',
                         b'Inventory HUD Settings', b'Fishing HUD Settings', b'Slayer HUD Settings'))
                and b'Item Animation \xe2\x80\x94 Renderer' not in hud_editor_titles_raw
                and b'Mod Settings' in mod_settings_titles_raw and b'Mod Settings' in feature_editor_titles_raw,
            'every focused editor uses its Feature Settings title and has no retired Editor title')
    metric_raw = raw_of('hud/ClientMetrics')
    metric_compat_raw = raw_of('hud/HudConfigCompatibility')
    metric_layout_raw = raw_of('gui/GeneralMetricsLayout')
    stats_hud_raw = raw_of('hud/StatsHud')
    stats_text_raw = raw_of('hud/StatsHudText')
    r.check(b'ClientMetrics' in settings_screen_raw and b'Client metrics' in reference_catalog_raw
                and len(metric_raw) > 0 and len(metric_compat_raw) > 0 and len(metric_layout_raw) > 0
                and MOD_PACKAGE + 'hud/ClientMetricHud.class' not in names
                and b'ClientMetricHud' not in hud_manager_raw and b'FPS_DISPLAY' not in hud_manager_raw
                and b'PING_DISPLAY' not in hud_manager_raw and b'MEMORY_DISPLAY' not in hud_manager_raw
                and b'StatsHud' in hud_manager_raw and b'migrateVisibleRetiredMetricDisplay' in metric_compat_raw
                and b'migrateVisibleRetiredMetricDisplay' in hud_manager_raw
                and all(value in stats_hud_raw for value in
                        (b'FRAME RATE', b'MINING', b'MEMORY', b'bevelRect', b'ClientMetrics', b'MiningTracker',
                         b'CARD_WIDTH', b'CARD_HEIGHT', b'CARD_GAP', b'CARD_CUT',
                         b'CARD_SHADOW_OFFSET_X', b'CARD_SHADOW_OFFSET_Y'))
                and all(value in stats_text_raw for value in (b' fps', b' BPS', b' MB')),
            'System Diagnostics keeps GUI-only telemetry while only restored Frame Rate/Mining/Memory Stats Display renders in-game metrics and migrates legacy visibility')
    r.check(MOD_PACKAGE + 'hud/DollarsHud.class' not in names and MOD_PACKAGE + 'hud/BalanceSource.class' not in names
                and b'DollarsHud' not in hud_manager_raw and b'BalanceSource' not in hud_manager_raw,
            'Player Dollars HUD and manual balance source have no packaged runtime route')
    r.check(MOD_PACKAGE + 'hud/HotbarHud.class' not in names and b'HotbarHud' not in hud_manager_raw
                and b'HOTBAR' not in settings_surface_raw and b'hud-hotbar' not in flat_model_raw
                and b'hotbar' not in raw_of('gui/GuiSettingsAuditModel'),
            'retired Hotbar HUD customization, slot highlight and panel background have no packaged route')
    editor_members = members_of('gui/HudEditorScreen')
    editor_raw = raw_of('gui/HudEditorScreen')
    selection_raw = raw_of('gui/HudEditorSelectionPolicy')
    r.check(model_ref in editor_raw and layout_ref in editor_raw
                and 'panelControlLabel' in editor_members
                and b'HudEditorSelectionPolicy' in editor_raw
                and b'Click an on-screen HUD element to select it; drag to move it.' in editor_raw
                and b'Choose HUD target' not in editor_raw and b'buildHudEditorTargetList' not in editor_raw
                and b'nextIndex' in selection_raw,
            'HudEditorScreen uses direct canvas selection with overlap cycling and no target sidebar/list')
    r.check(b'Open Item Animation Settings' not in editor_raw,
            'HudEditorScreen has no redundant Item Animation settings launcher')

    # --- Chat QoL ownership and one-default-chat implementation proof --------
    # Beta-0.10.8.6.4.1 deliberately has no alternate windows/classifier/routing bridge. The Hub exposes only feature
    # switches and the editor route; all visual/tab details remain editor-owned.
    chat_ui_constants = constants_of('gui/ChatUiModel')
    chat_editor_raw = raw_of('gui/ChatEditorScreen')
    r.check(all(value in chat_ui_constants for value in ('On/Off', 'Settings'))
                and all(value not in chat_ui_constants for value in ('Chat Tabs', 'Smooth Messages', 'Hover Highlight',
                                                                      'Separate Chats', 'Global Font', 'Open Font Settings',
                                                                      'Open Chat Editor')),
            'ChatUiModel keeps exactly its Hub On/Off toggle and Settings route')
    chat_editor_layout_raw = raw_of('gui/ChatEditorLayout')
    r.check(all(value in chat_editor_raw for value in
                (b'Background', b'Messages', b'Tabs', b'Menu and Highlight', b'Reset', b'Invalid regex',
                 b'Open chat background', b'Fit width to text', b'Unselected tab colour', b'Selected tab colour',
                 b'Active tab opacity'))
                and all(value not in chat_editor_raw for value in
                        (b'Windows', b'Link all', b'Copy from', b'Chat 1', b'Chat 2', b'Show tabs', b'Menu animation',
                         b'Tab base colour'))
                and b'com/arena/pekaclient/gui/ChatEditorLayout' in chat_editor_raw
                and b'com/arena/pekaclient/gui/GuiLayoutToolkit' in chat_editor_raw
                and len(chat_editor_layout_raw) > 0,
            'ChatEditorScreen packages five sections plus adjustable black-inactive and blue-selected tab controls')
    menu_constants = constants_of('chat/ChatContextMenu')
    menu_members = members_of('chat/ChatContextMenu')
    player_constants = constants_of('chat/ChatPlayerName')
    menu_state_raw = raw_of('chat/ChatMenuState')
    menu_state_members = members_of('chat/ChatMenuState')
    tabs_raw = raw_of('feature/ChatChannelTabs')
    chat_surface_raw = raw_of('feature/ChatSurfaceRenderer')
    chat_preview_raw = raw_of('gui/ChatEditorPreview')
    copy_menu_raw = raw_of('feature/ChatChannelTabs$CopyMenu')
    tabs_model_raw = raw_of('feature/ChatTabs')
    geometry_raw = raw_of('chat/ChatRenderGeometry')
    settings_raw_chat = raw_of('config/ChatQolSettings')
    chat_runtime_raw = tabs_raw + copy_menu_raw
    history_raw = raw_of('chat/ChatMessageHistory')
    history_entry_raw = raw_of('chat/ChatMessageHistory$Entry')
    menu_router_raw = raw_of('chat/ChatMenuInputRouter')
    visibility_reason_raw = raw_of('chat/ChatVisibilityAudit$Reason')
    tabs_members = members_of('feature/ChatChannelTabs')
    chat_close_policy_raw = raw_of('feature/ChatTabClosePolicy')
    r.check(b'guiChatWasOpen' in tabs_raw and b'shouldResetToAll' in tabs_raw
                and b'resetSelectedTabAfterChatClose' in tabs_raw
                and b'shouldResetToAll' in chat_close_policy_raw
                and b'wasGuiChatOpen' in chat_close_policy_raw,
            'Chat QoL resets the selected tab only on a GuiChat open-to-closed transition')
    r.check(b'ChatMessageHistory' in tabs_raw and b'GuiUtilRenderComponents' in tabs_raw
                and all(name in tabs_members for name in
                        ('arrivalOrder', 'newestArrivalFirst', 'originalForCompactGroup', 'sequence',
                         'chatWheelNotches', 'wheelLines'))
                and b'nextArrivalSequence' in history_raw and b'sequence' in history_entry_raw
                and b'displayOrder=top-to-bottom' in tabs_raw and b'CHAT_DEBUG_HISTORY_LIMIT' in tabs_raw
                and b'ChatMenuInputRouter' in copy_menu_raw and b'DISMISS' in menu_router_raw
                and b'CLOSED_ACTIVE_TAB' in visibility_reason_raw and len(history_raw) > 0,
            'Chat QoL packages sequence-ordered bounded rich-message history, selected closed filtering, chat-only wheel direction, and one menu router')
    r.check(all(value in menu_constants for value in ('Copy to clipboard', 'Copy to chat box', 'Message player'))
                and all(not value.startswith('Message player' + ':') for value in player_constants)
                and 'extract' in menu_members and b'ChatMenuState' in copy_menu_raw
                and b'ChatInteractionModel' in tabs_raw and b'CapturedChatMenu' in chat_runtime_raw
                and b'ChatRenderGeometry' in tabs_raw and b'ChatTabs' in tabs_raw
                and b'ChatContextMenu' in menu_state_raw and b'ChatClipboard' in chat_runtime_raw
                and b'ChatPlayerName' in menu_state_raw and b'ChatPipeline' not in chat_runtime_raw,
            'Chat QoL packages three fixed menu labels and a captured MenuState with recipient parsing')
    r.check(all(name in menu_state_members for name in ('insert', 'messagePlayer'))
                and 'insertInto' in members_of('feature/ChatChannelTabs$CopyMenu')
                and b'Nothing selected' not in chat_runtime_raw,
            'production Copy to chat box and Message player use MenuState after mouse release without stale-selection text')
    interaction_members = members_of('feature/ChatInteractionModel$Menu')
    r.check('setAnimationMillis' not in interaction_members and 'openAmount' not in interaction_members,
            'context menu opens and closes instantly while retaining only row-hover easing')
    chat_visual_raw = tabs_raw + chat_surface_raw + chat_preview_raw
    r.check(all(value in chat_visual_raw for value in (b'backgroundOpenFullArea', b'backgroundFitWidth',
                                                        b'tabInactiveColor', b'tabBackgroundColor', b'tabOpacity',
                                                        b'tabActiveOpacity'))
                and b'ChatSurfaceRenderer' in tabs_raw and b'ChatSurfaceRenderer' in chat_preview_raw
                and b'BackgroundTransition' in geometry_raw and b'messageBackground' in geometry_raw
                and b'tabs.always' in settings_raw_chat and b'menu.animation' in settings_raw_chat,
            'message-only background, tabs, hover and menu share the live Chat surface renderer with the editor preview')
    r.check(all(value in tabs_model_raw for value in
                (b'custom-', b'Invalid regex', b'Contains text', b'Starts with', b'Regex', b'partyCommandLine',
                 b'joined the party', b'party was disbanded', b'party members'))
                and b'MAX_TABS' in tabs_model_raw,
            'ChatTabs packages Party chat plus party command results, bounded custom filtering, and safe regex handling')
    r.check(b'drawInputChannelLabel' not in tabs_raw and b'selectedLabel' not in tabs_raw,
            'ChatChannelTabs leaves the chat field unshifted and has no in-box channel/mode label')
    r.check(b'Copy clean' not in chat_runtime_raw and b'Copy colours' not in chat_runtime_raw
                and b'Copy to box' not in chat_runtime_raw,
            'Chat QoL excludes retired alternate copy actions from the runtime menu')
    r.check(all(value not in chat_runtime_raw for value in
                (b'ChatQolRouting', b'ChatFilterRules', b'ChatQolHistory', b'ChattingAnimationBridge')),
            'one Default Chat runtime carries no split-window classifier/router bridge')

    # Compact Messages is default-on in the one cache/renderer: exact formatted-text matching decides the
    # duplicate at event time, then only the immediately newest unique group is deleted and reprinted with a gray
    # suffix. This deliberately rejects the old global fixed-counter id that could erase unrelated chat rows.
    compact_raw = raw_of('chat/ChatCompactMessages')
    compact_constants = constants_of('chat/ChatCompactMessages')
    visibility_raw = raw_of('chat/ChatVisibilityAudit')
    r.check(b'ChatCompactMessages' in tabs_raw
                and b'ChatVisibilityAudit' in tabs_raw and len(visibility_raw) > 0
                and (b'printChatMessageWithOptionalDeletion' in tabs_raw or b'func_146234_a' in tabs_raw)
                and (b'deleteChatLine' in tabs_raw or b'func_146242_c' in tabs_raw)
                and b'Compact messages' in chat_editor_raw and b'Compact counter colour' in chat_editor_raw
                and b'chat.qol.messages.compact' in settings_raw_chat
                and b'chat.qol.messages.compactCounterColor' in settings_raw_chat
                and b'GROUP_ID_BASE' in compact_raw and b'NO_GROUP_ID' in compact_raw
                and b'COUNTER_LINE_ID' not in compact_raw
                and any('(x' in value for value in compact_constants),
            'Compact Messages packages exact unique-group replacement, closed/open visibility audit, and editor colour')
    r.check(all(value not in settings_surface_raw and value not in chat_editor_raw for value in
                (b'FontsScreen', b'FontSettings', b'GlobalFontManager', b'custom font')),
            'Hub and Chat Settings have no custom-font route or renderer hook')
    r.check(b'com/arena/pekaclient/gui/ChatEditorScreen' in settings_screen_raw
                and b'Chat QoL' in reference_catalog_raw and b'Enable Chat QoL' in chat_editor_raw
                and b'Use chat tabs' in chat_editor_raw,
            'reference Quality of Life card routes Chat QoL through Chat Settings including its feature-level switches')
    r.check(all(value not in settings_surface_raw for value in
                (b'Chatting animation bridge', b'Message animation speed', b'Hide vanilla scroll bar')),
            'reference settings shell has no retired external animation or duplicate chat customization controls')
    r.check(all(value in settings_screen_raw for value in
                (b'GuiLayoutToolkit', b'FeatureSettingsEditors')) and b'modSettings' in feature_editors_raw,
            'reference shell uses shared clamped geometry and routes Mod Settings through the shared editor framework')
    r.check(all(b'GuiLayoutToolkit' in raw_of(cls) for cls in
                ('gui/AppleSettingsScreen', 'gui/HudEditorScreen', 'gui/ChatEditorScreen',
                 'gui/LoadingScreen', 'notes/QuickNoteScreen')),
            'every production Sky VeLor screen packages the shared layout toolkit')
    client_raw = raw_of('PekaClientMod')
    r.check(b'com/arena/pekaclient/feature/ChatChannelTabs' in client_raw
                and b'key.pekaclient.chatTab' in client_raw and b'key.pekaclient.chatEditor' in client_raw,
            'PekaClientMod registers native Chat QoL and its optional cycle/editor keybindings')

    # The gameplay wheel guard is deliberately one narrow MouseEvent predicate. Its default-off control and the
    # default-on particle filter lives in the reference Quality of Life / Comfort card; scroll controls remain in System.
    mod_settings_raw = raw_of('gui/ModSettingsScreen')
    quality_ui_constants = constants_of('gui/QualityOfLifeUiModel')
    quality_ui_raw = raw_of('gui/QualityOfLifeUiModel')
    mod_ui_constants = constants_of('gui/ModSettingsUiModel')
    apple_constants = constants_of('config/AppleSettings')
    r.check(b'HotbarScrollPolicy' in client_raw and 'shouldCancel' in members_of('PekaClientMod')
                and all(value in reference_catalog_raw for value in
                        (b'Comfort', b'Lock hotbar to keys', b'Quiet block particles'))
                and 'mouse.disableHotbarScrolling' in apple_constants,
            'Quality of Life Comfort owns the default-off gameplay hotbar wheel guard and default-on block-particle filter')
    r.check(all(value in mod_ui_constants for value in ('Scroll speed', 'Smooth scrolling'))
                and 'Item Animation' not in mod_ui_constants and 'Chat QoL master' not in mod_ui_constants
                and b'com/arena/pekaclient/gui/ModSettingsUiModel' in mod_settings_raw
                and b'ChatQolSettings' not in mod_settings_raw and b'itemAnimationEnabled' not in mod_settings_raw,
            'focused Mod Settings remains scroll-only and carries neither duplicate Item Animation nor Chat QoL master control')
    r.check(b'No Hotbar Scrolling' in quality_ui_raw and b'Disable Hotbar Scrolling' not in quality_ui_raw
                and b'Hurt Camera Shake' in reference_catalog_raw and b'No Hurt Cam' not in reference_catalog_raw
                and b'No block particles' not in mod_settings_raw
                and b'Item Animation' in reference_catalog_raw
                and b'Item Animation' in raw_of('gui/ItemAnimationUiModel'),
            'Quality of Life keeps direct toggle/percentage rows and HUD Hub keeps a named non-duplicated Item Animation route')

    # Spirit Leap's sensitive GUI interception reruns the established sidebar detector at the actual open event;
    # the self-test carries the same result, reason, and matched score line to make a fail-closed decision visible.
    sv_raw = raw_of('command/SvCommand')
    r.check(b'DungeonContext' in client_raw and 'evaluateNow' in members_of('PekaClientMod')
                and b'inDungeon' in client_raw and b'SpiritLeap' in client_raw
                and b'inDungeon' in sv_raw and b'matchedScoreboardLine' in sv_raw and b'reason' in sv_raw,
            'Spirit Leap requires a fresh positive dungeon result and selftest reports the detector evidence')

    # Focused transformer tests exercise the exact EffectRenderer.addEffect(EntityFX) mappings. The release audit
    # verifies that the narrowly scoped transformer, predicate bridge, status bridge, and loading registration ship.
    block_transformer_raw = raw_of('render/blockparticles/BlockParticleTransformer')
    block_hooks_raw = raw_of('render/blockparticles/BlockParticleHooks')
    loading_raw = raw_of('render/itemanimation/ItemAnimationLoadingPlugin')
    r.check(b'BlockParticleHooks' in block_transformer_raw and b'BlockParticleHookStatus' in block_transformer_raw,
            'No block particles packages a transformer with only its predicate and fail-closed status bridge')
    r.check(b'BlockParticlePolicy' in block_hooks_raw and b'AppleSettings' in block_hooks_raw,
            'No block particles hook routes the runtime object through the default-on block-digging policy')
    r.check(b'BlockParticleTransformer' in loading_raw,
            'Item Animation loading plugin registers the narrow No block particles transformer')

    # Item Lock / Item Swap is real production inventory protection, not a GUI-only setting: require the complete
    # persisted position model, packet/drop transformer bridge, controller, shape overlay, and plugin registration.
    lock_settings_constants = constants_of('itemlockswap/ItemLockSwapSettings')
    lock_transformer_constants = constants_of('itemlockswap/ItemLockSwapTransformer')
    lock_transformer_raw = raw_of('itemlockswap/ItemLockSwapTransformer')
    lock_hooks_raw = raw_of('itemlockswap/ItemLockSwapHooks')
    lock_controller_raw = raw_of('itemlockswap/ItemLockSwapController')
    lock_input_raw = raw_of('itemlockswap/ItemLockSwapInputState')
    lock_gesture_raw = raw_of('itemlockswap/ItemLockSwapGesturePolicy')
    lock_gesture_decision_raw = raw_of('itemlockswap/ItemLockSwapGesturePolicy$Decision')
    lock_geometry_raw = raw_of('itemlockswap/ItemLockSwapLineGeometry')
    lock_overlay_raw = raw_of('itemlockswap/ItemLockSwapOverlay')
    lock_ui_raw = raw_of('itemlockswap/ItemLockSwapUiModel')
    lock_key_resource = zf.read('assets/pekaclient/lang/en_US.lang') if 'assets/pekaclient/lang/en_US.lang' in names else b''
    r.check(all(value in lock_settings_constants for value in
                ('itemLockSwap.', 'itemLockSwap.locks', 'itemLockSwap.pairs', 'itemLockSwap.cooldownTenths',
                 'itemLockSwap.showIconsOnHotbar', 'itemLockSwap.swapLineMode', 'itemLockSwap.swapLineColor',
                 'itemLockSwap.swapLineOpacity', 'itemLockSwap.swapLineWidth', 'itemLockSwap.key')),
            'Item Lock/Swap packages persistent 40-position locks, pairs, cooldown, hotbar icon, line and key settings')
    r.check(all(value in lock_transformer_constants for value in
                ('net.minecraft.client.multiplayer.PlayerControllerMP', 'bda', 'windowClick', 'func_78753_a',
                 'net.minecraft.client.entity.EntityPlayerSP', 'bew', 'dropOneItem', 'func_71040_bB'))
                and b'shouldBlockWindowClick' in raw_of('itemlockswap/ItemLockSwapTransformer$WindowClickVisitor')
                and b'shouldBlockHotbarDrop' in raw_of('itemlockswap/ItemLockSwapTransformer$DropVisitor'),
            'Item Lock/Swap transformer carries explicit MCP/SRG/notch window-click and Q/Ctrl+Q targets')
    r.check(b'ItemLockSwapHooks' in lock_transformer_raw and b'ItemLockSwapHookStatus' in lock_transformer_raw
                and b'ItemLockSwapProtection' in lock_hooks_raw and b'ItemLockSwapController' in lock_hooks_raw,
            'Item Lock/Swap core bridge injects hook predicates and production blocked-action feedback')
    r.check(b'ItemLockSwapClickPolicy' in raw_of('itemlockswap/ItemLockSwapProtection')
                and b'ItemLockSwapDropPolicy' in raw_of('itemlockswap/ItemLockSwapProtection')
                and b'ItemLockSwapSwapPolicy' in lock_controller_raw
                and b'beginInternalSwap' in lock_controller_raw and b'Cannot swap: item does not fit' in lock_controller_raw,
            'Item Lock/Swap production path covers click modes, selected-hotbar drops, internal pair bypass and armor-fit feedback')
    r.check(b'GuiScreenEvent$DrawScreenEvent$Post' in lock_controller_raw and b'isKeyDown' in lock_controller_raw
                and b'onGuiDrawPost' in lock_controller_raw and b'ItemLockSwapInputState' in lock_controller_raw
                and b'ItemLockSwapGesturePolicy' in lock_controller_raw and b'onClientTick' not in lock_controller_raw
                and all(value in lock_input_raw for value in (b'HELD', b'STARTED', b'RELEASED', b'poll'))
                and b'Slot is already paired' in lock_gesture_decision_raw,
            'Item Lock/Swap polls the physical key in DrawScreen Post and uses one HELD drag/release state machine')
    r.check(b'removePairContaining' in lock_gesture_raw and b'PAIR_REMOVED' in lock_gesture_decision_raw
                and b'Swap removed' in lock_controller_raw and b'AppleSettings' in lock_controller_raw,
            'Item Lock/Swap ships persisted paired-endpoint tap removal with the concise Swap removed feedback')
    r.check(b'drawContainerForeground' in lock_overlay_raw and b'drawEditorPreview' in lock_overlay_raw
                and b'drawBadge' in lock_overlay_raw and b'drawRoundLine' in lock_overlay_raw
                and b'ItemLockSwapLineGeometry' in lock_overlay_raw and b'ItemLockSwapGesturePolicy' in lock_overlay_raw
                and b'Release L on another slot to create Item Swap' in lock_overlay_raw
                and b'ItemLockSwapOverlay' in lock_transformer_raw
                and 'net.minecraft.client.gui.inventory.GuiContainer' in lock_transformer_constants
                and all(value in lock_geometry_raw for value in (b'slotCenterX', b'slotCenterY', b'cursorX', b'cursorY')),
            'Item Lock/Swap injects rounded pair and live-drag lines above items and below tooltips')
    r.check(all(value in lock_ui_raw for value in
                (b'Item Lock and Item Swap', b'item-lock-swap-master', b'item-lock-swap-lines',
                 b'item-lock-swap-line-color', b'item-lock-swap-line-opacity', b'item-lock-swap-line-width',
                 b'item-lock-swap-clear-locks', b'item-lock-swap-clear-pairs'))
                and b'ItemLockSwapOverlay' in client_raw and b'ItemLockSwapController' in client_raw
                and b'key.pekaclient.itemLockSwap' in client_raw and b'key.pekaclient.itemLockSwap' in lock_key_resource
                and b'lockdebug' in raw_of('command/ItemDebugCommand'),
            'Item Lock/Swap ships the Quality of Life editor route, L keybind, overlay/controller registration and diagnostics')
    r.check(b'ItemLockSwapTransformer' in loading_raw and b'com.arena.pekaclient.itemlockswap' in loading_raw,
            'loading plugin registers and excludes the Item Lock/Swap transformer package')
    fishing_constants = constants_of('fishing/FishingStats')
    r.check(all(name in fishing_constants for name in ('commonCreatureCount', 'legendaryCreatureCount')),
            'FishingStats packages separate Common Creature and preserved Legendary/Mythic totals')
    fishing_hud_raw = raw_of('fishing/FishingHud')
    r.check(b'commonCreature' in fishing_hud_raw,
            'Fishing HUD persists its compact Common Creature row separately from Festival visibility')
    sea_constants = constants_of('fishing/SeaCreatureHud')
    r.check('shouldScan' in sea_constants,
            'Sea Creature HP packages the overflow-safe scan scheduler')
    r.check(all(value in sea_constants for value in
                ('keepPreviousDetection', 'betterEntry', 'EncounterState')),
            'Sea Creature HP packages bounded nameplate grace and health-bearing fallback selection')
    parser_constants = constants_of('fishing/SeaCreatureParser')
    r.check('Water Hydra' not in parser_constants and 'Reindrake' not in parser_constants
                and all(value in parser_constants for value in ('Yeti', 'Lord Jawbus')),
            'CorruptedHP profile excludes Water Hydra/Reindrake while retaining Yeti and Lord Jawbus labels')
    fishing_ui_constants = constants_of('gui/FishingUiModel')
    r.check('Nearby encounter titles' in fishing_ui_constants
                and b'Nearby encounters' in reference_catalog_raw and b'Catch Counter Settings' in feature_editors_raw,
            'Fishing keeps nearby encounter alerts and routes counters to their focused editor')

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
