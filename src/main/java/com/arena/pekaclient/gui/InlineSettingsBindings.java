package com.arena.pekaclient.gui;

import com.arena.pekaclient.chat.ChatMenuPolicy;
import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.config.AssistSettings;
import com.arena.pekaclient.config.ChatQolSettings;
import com.arena.pekaclient.config.DungeonEspSettings;
import com.arena.pekaclient.dungeon.M7OverlayRenderer;
import com.arena.pekaclient.dungeon.M7Settings;
import com.arena.pekaclient.fishing.FishingStats;
import com.arena.pekaclient.hud.HudManager;
import com.arena.pekaclient.itemlockswap.ItemLockSwapSettings;
import com.arena.pekaclient.slayer.SlayerTracker;
import com.arena.pekaclient.slayer.SlayerType;

/**
 * Existing-owner bridge for inline settings rows.
 *
 * <p>It deliberately contains no persistence of its own: each id reads and writes the same field or method that the
 * retired focused screen used. Keeping this mapping out of the renderer makes old-config and option-preservation
 * tests independent of Minecraft drawing.</p>
 */
final class InlineSettingsBindings {
    private static final StyleClipboard STYLE_CLIPBOARD = new StyleClipboard();

    private InlineSettingsBindings() { }

    static boolean booleanValue(String id) {
        if ("no-block-particles".equals(id)) return AppleSettings.noBlockParticles;
        if ("disable-hotbar-scrolling".equals(id)) return AppleSettings.disableHotbarScrolling;
        if ("auto-sprint".equals(id)) return AssistSettings.autoSprint;

        if ("item-lock-swap".equals(id)) return ItemLockSwapSettings.enabled;
        if ("item-lock-icons".equals(id)) return ItemLockSwapSettings.showLockIcons;
        if ("item-lock-hotbar".equals(id)) return ItemLockSwapSettings.showIconsOnHotbar;
        if ("item-lock-message".equals(id)) return ItemLockSwapSettings.showBlockedMessage;
        if ("hide-potions".equals(id)) return AssistSettings.hidePotionEffects;
        if ("roman-numerals".equals(id)) return AssistSettings.romanNumerals;
        if ("tooltip-scroll".equals(id)) return AssistSettings.tooltipScroll;
        if ("rarity-highlight".equals(id)) return AssistSettings.rarityHighlight;

        if ("chat-qol".equals(id)) return ChatQolSettings.enabled;
        if ("chat-tabs".equals(id)) return ChatQolSettings.tabsEnabled;
        if ("chat-smooth".equals(id)) return ChatQolSettings.smoothMessages;
        if ("chat-compact".equals(id)) return ChatQolSettings.compactMessages;
        if ("chat-hover".equals(id)) return ChatQolSettings.hoverHighlight;
        if ("chat-background-fit-width".equals(id)) return ChatQolSettings.backgroundFitWidth;

        if ("slayer-tracker".equals(id)) return AppleSettings.slayerTracker;
        if ("slayer-boss-timer".equals(id)) return AppleSettings.slayerBossTimer;
        if ("slayer-pricing".equals(id)) return AppleSettings.slayerPricing;

        if ("wardrobe-shortcut".equals(id)) return AssistSettings.wardrobeShortcut;
        if ("pets-shortcut".equals(id)) return AssistSettings.petsShortcut;

        if ("dungeon-map".equals(id)) return AppleSettings.dungeonMap;
        if ("dungeon-map-rotate".equals(id)) return AppleSettings.dungeonMapRotate;
        if ("dungeon-map-player-names".equals(id)) return AppleSettings.dungeonMapShowPlayerNames;
        if ("dungeon-map-puzzle-name".equals(id)) return AppleSettings.dungeonMapShowPuzzleName;
        if ("dungeon-map-scale-on".equals(id)) return AppleSettings.dungeonMapScaleOn;
        if ("party-puzzle-announce".equals(id)) return AppleSettings.partyPuzzleAnnounce;
        if ("terminal-hints".equals(id)) return AssistSettings.terminalHints;
        if ("terminal-auto-click".equals(id)) return AssistSettings.terminalAutoClick;
        if ("terminal-hide-wrong".equals(id)) return AssistSettings.terminalHideIncorrect;
        if ("terminal-prevent-clicks".equals(id)) return AssistSettings.terminalPreventWrongClicks;
        if ("terminal-waypoints".equals(id)) return AssistSettings.terminalWaypoints;
        if ("spirit-leap".equals(id)) return AppleSettings.leapMenu;
        if ("leap-key".equals(id)) return AppleSettings.leapKey;
        if ("dungeon-boss-hud".equals(id)) return HudManager.DUNGEON_BOSS_HP.enabled;
        if ("m7-overlay".equals(id)) return M7Settings.isOverlayEnabled();
        if ("m7-relic-areas".equals(id)) return M7Settings.isStatueZonesEnabled();
        if ("m7-areas-persist".equals(id)) return M7Settings.isAreasPersistAfterDragonSpawn();
        if ("dragon-alerts".equals(id)) return M7Settings.isDragonKillAlertEnabled();
        if ("m7-terminal-solver".equals(id)) return M7Settings.isTerminalSolverEnabled();
        if ("m7-boss-maxor".equals(id)) return M7Settings.isBossHitboxEnabled(M7OverlayRenderer.M7HitboxTarget.MAXOR);
        if ("m7-boss-storm".equals(id)) return M7Settings.isBossHitboxEnabled(M7OverlayRenderer.M7HitboxTarget.STORM);
        if ("m7-boss-goldor".equals(id)) return M7Settings.isBossHitboxEnabled(M7OverlayRenderer.M7HitboxTarget.GOLDOR);
        if ("m7-boss-necron".equals(id)) return M7Settings.isBossHitboxEnabled(M7OverlayRenderer.M7HitboxTarget.NECRON);
        if ("m7-dragon-power".equals(id)) return M7Settings.isDragonHitboxEnabled(M7OverlayRenderer.M7DragonStatue.POWER);
        if ("m7-dragon-apex".equals(id)) return M7Settings.isDragonHitboxEnabled(M7OverlayRenderer.M7DragonStatue.APEX);
        if ("m7-dragon-soul".equals(id)) return M7Settings.isDragonHitboxEnabled(M7OverlayRenderer.M7DragonStatue.SOUL);
        if ("m7-dragon-ice".equals(id)) return M7Settings.isDragonHitboxEnabled(M7OverlayRenderer.M7DragonStatue.ICE);
        if ("m7-dragon-flame".equals(id)) return M7Settings.isDragonHitboxEnabled(M7OverlayRenderer.M7DragonStatue.FLAME);

        if ("mob-esp-enabled".equals(id)) return DungeonEspSettings.enabled;
        if ("door-esp-enabled".equals(id)) return DungeonEspSettings.doorsEnabled;
        if ("esp-force-all".equals(id)) return DungeonEspSettings.forceRenderAllMobs;
        if ("esp-include-bats".equals(id)) return DungeonEspSettings.includeBats;
        if ("esp-include-invisible".equals(id)) return DungeonEspSettings.includeInvisibleMobs;
        if ("esp-hide-zero-health".equals(id)) return DungeonEspSettings.hideZeroHealth;
        if ("esp-tracers".equals(id)) return DungeonEspSettings.tracers;
        if ("esp-mob-labels".equals(id)) return DungeonEspSettings.mobLabels;
        if ("esp-distance-text".equals(id)) return DungeonEspSettings.distanceText;
        if ("door-only-key".equals(id)) return DungeonEspSettings.doorOnlyWhenKey;
        if ("door-distance-text".equals(id)) return DungeonEspSettings.doorDistanceText;
        if ("door-blood".equals(id)) return DungeonEspSettings.showBloodDoors;
        if ("door-wither-style".equals(id)) return DungeonEspSettings.doors.enabled;
        if ("dungeon-sync-enabled".equals(id)) return AssistSettings.dungeonSync;
        if ("dungeon-sync-debug".equals(id)) return AssistSettings.dungeonSyncDebug;

        if ("mining-bps".equals(id)) return HudManager.MINING.enabled;
        if ("mining-ability-alert".equals(id)) return AssistSettings.miningAbilityAlert;
        if ("mining-puzzler".equals(id)) return AssistSettings.miningPuzzlerMarker;
        if ("mining-assistant-hud".equals(id)) return HudManager.MINING_ASSIST.enabled;
        if ("catch-counter".equals(id)) return AppleSettings.fishingCounter;
        if ("fishing-counter".equals(id)) return AppleSettings.fishingCounter;
        if ("fishing-festival".equals(id)) return AppleSettings.fishingFestival;
        if ("sea-hp-hud-route".equals(id)) return HudManager.SEA_HP.enabled;
        if ("jawbus".equals(id)) return AppleSettings.fishingJawbusAlert;
        if ("nearby-encounters".equals(id)) return AppleSettings.fishingNearbyEncounterAlert;

        if ("hud-player-doll".equals(id)) return HudManager.DOLL.enabled;
        if ("hud-inventory".equals(id)) return HudManager.INVENTORY.enabled;
        if ("item-animation".equals(id)) return AppleSettings.itemAnimationEnabled;

        if ("appearance-corners-on".equals(id)) return AppleSettings.cornerRadiusOn;
        if ("appearance-opacity-on".equals(id)) return AppleSettings.glassOpacityOn;
        if ("shadows".equals(id)) return AppleSettings.shadows;
        if ("animations".equals(id)) return AppleSettings.animations;
        if ("animation-speed-on".equals(id)) return AppleSettings.animSpeedOn;
        if ("adaptive-quality".equals(id)) return AppleSettings.adaptive;
        if ("performance-boost".equals(id)) return AppleSettings.experimentalPerformance;
        if ("smooth-scrolling".equals(id)) return AppleSettings.smoothScrolling;
        if ("sv-command".equals(id)) return AppleSettings.svCommand;
        if ("qn-command".equals(id)) return AppleSettings.qnCommand;

        DungeonEspSettings.Style style = styleForToggle(id);
        if (style == null) return false;
        return id != null && id.endsWith("-walls") ? style.throughWalls : style.enabled;
    }

    static void setBoolean(String id, boolean value) {
        if ("no-block-particles".equals(id)) AppleSettings.noBlockParticles = value;
        else if ("disable-hotbar-scrolling".equals(id)) AppleSettings.disableHotbarScrolling = value;
        else if ("auto-sprint".equals(id)) AssistSettings.autoSprint = value;

        else if ("item-lock-swap".equals(id)) ItemLockSwapSettings.enabled = value;
        else if ("item-lock-icons".equals(id)) ItemLockSwapSettings.showLockIcons = value;
        else if ("item-lock-hotbar".equals(id)) ItemLockSwapSettings.showIconsOnHotbar = value;
        else if ("item-lock-message".equals(id)) ItemLockSwapSettings.showBlockedMessage = value;
        else if ("hide-potions".equals(id)) AssistSettings.hidePotionEffects = value;
        else if ("roman-numerals".equals(id)) AssistSettings.romanNumerals = value;
        else if ("tooltip-scroll".equals(id)) AssistSettings.tooltipScroll = value;
        else if ("rarity-highlight".equals(id)) AssistSettings.rarityHighlight = value;

        else if ("chat-qol".equals(id)) ChatQolSettings.enabled = value;
        else if ("chat-tabs".equals(id)) ChatQolSettings.tabsEnabled = value;
        else if ("chat-smooth".equals(id)) ChatQolSettings.smoothMessages = value;
        else if ("chat-compact".equals(id)) ChatQolSettings.compactMessages = value;
        else if ("chat-hover".equals(id)) ChatQolSettings.hoverHighlight = value;
        else if ("chat-background-fit-width".equals(id)) ChatQolSettings.backgroundFitWidth = value;

        else if ("slayer-tracker".equals(id)) AppleSettings.slayerTracker = value;
        else if ("slayer-boss-timer".equals(id)) AppleSettings.slayerBossTimer = value;
        else if ("slayer-pricing".equals(id)) AppleSettings.slayerPricing = value;

        else if ("wardrobe-shortcut".equals(id)) AssistSettings.wardrobeShortcut = value;
        else if ("pets-shortcut".equals(id)) AssistSettings.petsShortcut = value;

        else if ("dungeon-map".equals(id)) AppleSettings.dungeonMap = value;
        else if ("dungeon-map-rotate".equals(id)) AppleSettings.dungeonMapRotate = value;
        else if ("dungeon-map-player-names".equals(id)) AppleSettings.dungeonMapShowPlayerNames = value;
        else if ("dungeon-map-puzzle-name".equals(id)) AppleSettings.dungeonMapShowPuzzleName = value;
        else if ("dungeon-map-scale-on".equals(id)) AppleSettings.dungeonMapScaleOn = value;
        else if ("party-puzzle-announce".equals(id)) AppleSettings.partyPuzzleAnnounce = value;
        else if ("terminal-hints".equals(id)) AssistSettings.terminalHints = value;
        else if ("terminal-auto-click".equals(id)) AssistSettings.terminalAutoClick = value;
        else if ("terminal-hide-wrong".equals(id)) AssistSettings.terminalHideIncorrect = value;
        else if ("terminal-prevent-clicks".equals(id)) AssistSettings.terminalPreventWrongClicks = value;
        else if ("terminal-waypoints".equals(id)) AssistSettings.terminalWaypoints = value;
        else if ("spirit-leap".equals(id)) AppleSettings.leapMenu = value;
        else if ("leap-key".equals(id)) AppleSettings.leapKey = value;
        else if ("dungeon-boss-hud".equals(id)) HudManager.DUNGEON_BOSS_HP.enabled = value;
        else if ("m7-overlay".equals(id)) M7Settings.setOverlayEnabled(value);
        else if ("m7-relic-areas".equals(id)) M7Settings.setStatueZonesEnabled(value);
        else if ("m7-areas-persist".equals(id)) M7Settings.setAreasPersistAfterDragonSpawn(value);
        else if ("dragon-alerts".equals(id)) M7Settings.setDragonKillAlertEnabled(value);
        else if ("m7-terminal-solver".equals(id)) M7Settings.setTerminalSolverEnabled(value);
        else if ("m7-boss-maxor".equals(id)) M7Settings.setBossHitboxEnabled(M7OverlayRenderer.M7HitboxTarget.MAXOR, value);
        else if ("m7-boss-storm".equals(id)) M7Settings.setBossHitboxEnabled(M7OverlayRenderer.M7HitboxTarget.STORM, value);
        else if ("m7-boss-goldor".equals(id)) M7Settings.setBossHitboxEnabled(M7OverlayRenderer.M7HitboxTarget.GOLDOR, value);
        else if ("m7-boss-necron".equals(id)) M7Settings.setBossHitboxEnabled(M7OverlayRenderer.M7HitboxTarget.NECRON, value);
        else if ("m7-dragon-power".equals(id)) M7Settings.setDragonHitboxEnabled(M7OverlayRenderer.M7DragonStatue.POWER, value);
        else if ("m7-dragon-apex".equals(id)) M7Settings.setDragonHitboxEnabled(M7OverlayRenderer.M7DragonStatue.APEX, value);
        else if ("m7-dragon-soul".equals(id)) M7Settings.setDragonHitboxEnabled(M7OverlayRenderer.M7DragonStatue.SOUL, value);
        else if ("m7-dragon-ice".equals(id)) M7Settings.setDragonHitboxEnabled(M7OverlayRenderer.M7DragonStatue.ICE, value);
        else if ("m7-dragon-flame".equals(id)) M7Settings.setDragonHitboxEnabled(M7OverlayRenderer.M7DragonStatue.FLAME, value);

        else if ("mob-esp-enabled".equals(id)) DungeonEspSettings.enabled = value;
        else if ("door-esp-enabled".equals(id)) DungeonEspSettings.doorsEnabled = value;
        else if ("esp-force-all".equals(id)) DungeonEspSettings.forceRenderAllMobs = value;
        else if ("esp-include-bats".equals(id)) DungeonEspSettings.includeBats = value;
        else if ("esp-include-invisible".equals(id)) DungeonEspSettings.includeInvisibleMobs = value;
        else if ("esp-hide-zero-health".equals(id)) DungeonEspSettings.hideZeroHealth = value;
        else if ("esp-tracers".equals(id)) DungeonEspSettings.tracers = value;
        else if ("esp-mob-labels".equals(id)) DungeonEspSettings.mobLabels = value;
        else if ("esp-distance-text".equals(id)) DungeonEspSettings.distanceText = value;
        else if ("door-only-key".equals(id)) DungeonEspSettings.doorOnlyWhenKey = value;
        else if ("door-distance-text".equals(id)) DungeonEspSettings.doorDistanceText = value;
        else if ("door-blood".equals(id)) DungeonEspSettings.showBloodDoors = value;
        else if ("door-wither-style".equals(id)) DungeonEspSettings.doors.enabled = value;
        else if ("dungeon-sync-enabled".equals(id)) AssistSettings.dungeonSync = value;
        else if ("dungeon-sync-debug".equals(id)) AssistSettings.dungeonSyncDebug = value;

        else if ("mining-bps".equals(id)) HudManager.MINING.enabled = value;
        else if ("mining-ability-alert".equals(id)) AssistSettings.miningAbilityAlert = value;
        else if ("mining-puzzler".equals(id)) AssistSettings.miningPuzzlerMarker = value;
        else if ("mining-assistant-hud".equals(id)) HudManager.MINING_ASSIST.enabled = value;
        else if ("catch-counter".equals(id) || "fishing-counter".equals(id)) AppleSettings.fishingCounter = value;
        else if ("fishing-festival".equals(id)) AppleSettings.fishingFestival = value;
        else if ("sea-hp-hud-route".equals(id)) HudManager.SEA_HP.enabled = value;
        else if ("jawbus".equals(id)) AppleSettings.fishingJawbusAlert = value;
        else if ("nearby-encounters".equals(id)) AppleSettings.fishingNearbyEncounterAlert = value;

        else if ("hud-player-doll".equals(id)) HudManager.DOLL.enabled = value;
        else if ("hud-inventory".equals(id)) HudManager.INVENTORY.enabled = value;
        else if ("item-animation".equals(id)) AppleSettings.itemAnimationEnabled = value;

        else if ("appearance-corners-on".equals(id)) AppleSettings.cornerRadiusOn = value;
        else if ("appearance-opacity-on".equals(id)) AppleSettings.glassOpacityOn = value;
        else if ("shadows".equals(id)) AppleSettings.shadows = value;
        else if ("animations".equals(id)) AppleSettings.animations = value;
        else if ("animation-speed-on".equals(id)) AppleSettings.animSpeedOn = value;
        else if ("adaptive-quality".equals(id)) AppleSettings.adaptive = value;
        else if ("performance-boost".equals(id)) AppleSettings.experimentalPerformance = value;
        else if ("smooth-scrolling".equals(id)) AppleSettings.smoothScrolling = value;
        else if ("sv-command".equals(id)) AppleSettings.svCommand = value;
        else if ("qn-command".equals(id)) AppleSettings.qnCommand = value;
        else {
            DungeonEspSettings.Style style = styleForToggle(id);
            if (style != null) {
                if (id != null && id.endsWith("-walls")) style.throughWalls = value;
                else style.enabled = value;
            }
        }
    }

    static int intValue(String id) {
        if ("hurt-camera-shake".equals(id)) return AssistSettings.hurtCameraShake;
        if ("item-lock-cooldown".equals(id)) return ItemLockSwapSettings.cooldownTenths;
        if ("item-lock-badge-opacity".equals(id)) return ItemLockSwapSettings.iconOpacity;
        if ("item-lock-line-opacity".equals(id)) return ItemLockSwapSettings.swapLineOpacity;
        if ("item-lock-line-width".equals(id)) return ItemLockSwapSettings.swapLineWidth;
        if ("chat-background-open-opacity".equals(id)) return ChatQolSettings.backgroundOpacityOpen;
        if ("chat-background-closed-opacity".equals(id)) return ChatQolSettings.backgroundOpacityClosed;
        if ("chat-background-radius".equals(id)) return ChatQolSettings.backgroundRadius;
        if ("chat-message-animation".equals(id)) return ChatQolSettings.messageAnimationMillis;
        if ("chat-fade-seconds".equals(id)) return ChatQolSettings.fadeSeconds;
        if ("chat-tab-opacity".equals(id)) return ChatQolSettings.tabOpacity;
        if ("chat-tab-active-opacity".equals(id)) return ChatQolSettings.tabActiveOpacity;
        if ("chat-hover-opacity".equals(id)) return ChatQolSettings.hoverOpacity;
        if ("dungeon-map-scale".equals(id)) return AppleSettings.dungeonMapScale;
        if ("dungeon-map-zoom".equals(id)) return AppleSettings.dungeonMapZoom;
        if ("terminal-scale".equals(id)) return AssistSettings.terminalScale;
        if ("esp-max-distance".equals(id)) return DungeonEspSettings.maxDistance;
        if ("esp-update-interval".equals(id)) return DungeonEspSettings.updateIntervalTicks;
        if ("esp-max-mob-labels".equals(id)) return DungeonEspSettings.maxMobLabels;
        if ("esp-max-boxes".equals(id)) return DungeonEspSettings.maxBoxes;
        if ("appearance-corners".equals(id)) return AppleSettings.cornerRadius;
        if ("appearance-opacity".equals(id)) return AppleSettings.glassOpacity;
        if ("animation-speed".equals(id)) return AppleSettings.animSpeed;
        if ("global-scroll".equals(id)) return AppleSettings.effectiveScrollSpeed();
        DungeonEspSettings.Style style = styleForValue(id);
        if (style != null) {
            if (id.endsWith("-width")) return style.outlineWidth;
            if (id.endsWith("-outline-opacity")) return style.outlineOpacity;
            if (id.endsWith("-fill-opacity")) return style.fillOpacity;
        }
        return 0;
    }

    static void setInt(String id, int value) {
        if ("hurt-camera-shake".equals(id)) AssistSettings.hurtCameraShake = clamp(value, 0, 100);
        else if ("item-lock-cooldown".equals(id)) ItemLockSwapSettings.cooldownTenths = clamp(value,
                ItemLockSwapSettings.MIN_COOLDOWN_TENTHS, ItemLockSwapSettings.MAX_COOLDOWN_TENTHS);
        else if ("item-lock-badge-opacity".equals(id)) ItemLockSwapSettings.iconOpacity = clamp(value,
                ItemLockSwapSettings.MIN_ICON_OPACITY, ItemLockSwapSettings.MAX_ICON_OPACITY);
        else if ("item-lock-line-opacity".equals(id)) ItemLockSwapSettings.swapLineOpacity = clamp(value,
                ItemLockSwapSettings.MIN_LINE_OPACITY, ItemLockSwapSettings.MAX_LINE_OPACITY);
        else if ("item-lock-line-width".equals(id)) ItemLockSwapSettings.swapLineWidth = clamp(value,
                ItemLockSwapSettings.MIN_LINE_WIDTH, ItemLockSwapSettings.MAX_LINE_WIDTH);
        else if ("chat-background-open-opacity".equals(id)) ChatQolSettings.backgroundOpacityOpen = clamp(value, 0, 100);
        else if ("chat-background-closed-opacity".equals(id)) ChatQolSettings.backgroundOpacityClosed = clamp(value, 0, 100);
        else if ("chat-background-radius".equals(id)) ChatQolSettings.backgroundRadius = clamp(value, 0, 12);
        else if ("chat-message-animation".equals(id)) ChatQolSettings.messageAnimationMillis = clamp(value, 0, 600);
        else if ("chat-fade-seconds".equals(id)) ChatQolSettings.fadeSeconds = clamp(value, 0, 60);
        else if ("chat-tab-opacity".equals(id)) ChatQolSettings.tabOpacity = clamp(value, 0, 100);
        else if ("chat-tab-active-opacity".equals(id)) ChatQolSettings.tabActiveOpacity = clamp(value, 0, 100);
        else if ("chat-hover-opacity".equals(id)) ChatQolSettings.hoverOpacity = clamp(value, 0, 100);
        else if ("dungeon-map-scale".equals(id)) AppleSettings.dungeonMapScale = clamp(value,
                AppleSettings.DUNGEON_MAP_SCALE_MIN, AppleSettings.DUNGEON_MAP_SCALE_MAX);
        else if ("dungeon-map-zoom".equals(id)) AppleSettings.dungeonMapZoom = clamp(value,
                AppleSettings.DUNGEON_MAP_ZOOM_MIN, AppleSettings.DUNGEON_MAP_ZOOM_MAX);
        else if ("terminal-scale".equals(id)) AssistSettings.terminalScale = clamp(value,
                AssistSettings.MIN_TERMINAL_SCALE, AssistSettings.MAX_TERMINAL_SCALE);
        else if ("esp-max-distance".equals(id)) DungeonEspSettings.maxDistance = clamp(value,
                DungeonEspSettings.MIN_DISTANCE, DungeonEspSettings.MAX_DISTANCE);
        else if ("esp-update-interval".equals(id)) DungeonEspSettings.updateIntervalTicks = clamp(value,
                DungeonEspSettings.MIN_INTERVAL_TICKS, DungeonEspSettings.MAX_INTERVAL_TICKS);
        else if ("esp-max-mob-labels".equals(id)) DungeonEspSettings.maxMobLabels = clamp(value,
                DungeonEspSettings.MIN_MOB_LABELS, DungeonEspSettings.MAX_MOB_LABELS);
        else if ("esp-max-boxes".equals(id)) DungeonEspSettings.maxBoxes = clamp(value,
                DungeonEspSettings.MIN_BOXES, DungeonEspSettings.MAX_BOXES);
        else if ("appearance-corners".equals(id)) AppleSettings.cornerRadius = clamp(value, 4, 18);
        else if ("appearance-opacity".equals(id)) AppleSettings.glassOpacity = clamp(value, 60, 100);
        else if ("animation-speed".equals(id)) AppleSettings.animSpeed = clamp(value, 40, 200);
        else if ("global-scroll".equals(id)) {
            AppleSettings.scrollSpeedOn = true;
            AppleSettings.scrollSpeed = clamp(value, 50, 300);
        } else {
            DungeonEspSettings.Style style = styleForValue(id);
            if (style != null) {
                if (id.endsWith("-width")) style.outlineWidth = clamp(value, DungeonEspSettings.MIN_WIDTH, DungeonEspSettings.MAX_WIDTH);
                else if (id.endsWith("-outline-opacity")) style.outlineOpacity = clamp(value, DungeonEspSettings.MIN_OPACITY, DungeonEspSettings.MAX_OPACITY);
                else if (id.endsWith("-fill-opacity")) style.fillOpacity = clamp(value, DungeonEspSettings.MIN_OPACITY, DungeonEspSettings.MAX_OPACITY);
            }
        }
    }

    static int choiceValue(String id) {
        if ("item-lock-badge-color".equals(id)) return ItemLockSwapSettings.iconColor;
        if ("item-lock-line-mode".equals(id)) return ItemLockSwapSettings.swapLineMode;
        if ("item-lock-line-color".equals(id)) return ItemLockSwapSettings.swapLineColor;
        if ("chat-background-open-area".equals(id)) return ChatQolSettings.backgroundOpenFullArea ? 1 : 0;
        if ("chat-menu-mode".equals(id)) return ChatQolSettings.menuOpenMode == ChatMenuPolicy.LEFT ? 1
                : ChatQolSettings.menuOpenMode == ChatMenuPolicy.BOTH ? 2 : 0;
        if ("door-scope".equals(id)) return DungeonEspSettings.doorScope;
        if ("door-blood-color".equals(id)) return DungeonEspSettings.colorIndex(DungeonEspSettings.bloodDoorColor);
        if ("curve-quality".equals(id)) return AppleSettings.curveQuality;
        DungeonEspSettings.Style style = styleForValue(id);
        if (style != null) {
            if (id.endsWith("-color")) return DungeonEspSettings.colorIndex(style.color);
            if (id.endsWith("-mode")) return style.mode;
        }
        return 0;
    }

    static void setChoice(String id, int value) {
        if ("item-lock-badge-color".equals(id)) ItemLockSwapSettings.iconColor = clamp(value, 0, ItemLockSwapSettings.ICON_COLOR_NAMES.length - 1);
        else if ("item-lock-line-mode".equals(id)) ItemLockSwapSettings.swapLineMode = clamp(value, 0, ItemLockSwapSettings.SWAP_LINE_MODE_NAMES.length - 1);
        else if ("item-lock-line-color".equals(id)) ItemLockSwapSettings.swapLineColor = clamp(value, 0, ItemLockSwapSettings.ICON_COLOR_NAMES.length - 1);
        else if ("chat-background-open-area".equals(id)) ChatQolSettings.backgroundOpenFullArea = value == 1;
        else if ("chat-menu-mode".equals(id)) ChatQolSettings.setMenuOpenMode(value == 1 ? ChatMenuPolicy.LEFT
                : value == 2 ? ChatMenuPolicy.BOTH : ChatMenuPolicy.RIGHT);
        else if ("door-scope".equals(id)) DungeonEspSettings.doorScope = clamp(value, 0, DungeonEspSettings.DOOR_SCOPE_NAMES.length - 1);
        else if ("door-blood-color".equals(id)) DungeonEspSettings.bloodDoorColor = DungeonEspSettings.colorForIndex(value);
        else if ("curve-quality".equals(id)) AppleSettings.curveQuality = clamp(value, 0, AppleSettings.CURVE_NAMES.length - 1);
        else {
            DungeonEspSettings.Style style = styleForValue(id);
            if (style != null) {
                if (id.endsWith("-color")) style.color = DungeonEspSettings.colorForIndex(value);
                else if (id.endsWith("-mode")) style.mode = clamp(value, 0, DungeonEspSettings.MODE_NAMES.length - 1);
            }
        }
    }

    /** Returns true when a non-screen action was handled. Screen-owned actions are handled by AppleSettingsScreen. */
    static boolean runAction(String id) {
        if ("appearance-reset".equals(id)) {
            AppleSettings.settingsTheme = AppleSettings.DEFAULT_SETTINGS_THEME;
            AppleSettings.darkMode = true;
            AppleSettings.accent = 0;
            AppleSettings.cornerRadius = AppleSettings.DEFAULT_CORNER_RADIUS;
            AppleSettings.cornerRadiusOn = true;
            AppleSettings.glassOpacity = AppleSettings.DEFAULT_GLASS_OPACITY;
            AppleSettings.glassOpacityOn = true;
            return true;
        }
        if ("performance-reset".equals(id)) {
            AppleSettings.curveQuality = 1;
            AppleSettings.shadows = true;
            AppleSettings.animations = true;
            AppleSettings.animSpeedOn = true;
            AppleSettings.animSpeed = AppleSettings.DEFAULT_ANIM_SPEED;
            AppleSettings.adaptive = AppleSettings.DEFAULT_ADAPTIVE_QUALITY;
            AppleSettings.experimentalPerformance = AppleSettings.DEFAULT_PERFORMANCE_BOOST_BETA;
            return true;
        }
        if ("system-general-reset".equals(id)) {
            AppleSettings.scrollSpeedOn = true;
            AppleSettings.scrollSpeed = AppleSettings.DEFAULT_SCROLL_SPEED;
            AppleSettings.smoothScrolling = AppleSettings.DEFAULT_SMOOTH_SCROLLING;
            return true;
        }
        if ("inventory-assist-reset".equals(id)) {
            AssistSettings.hidePotionEffects = false;
            AssistSettings.romanNumerals = false;
            AssistSettings.tooltipScroll = false;
            AssistSettings.rarityHighlight = false;
            return true;
        }
        if ("movement-shortcuts-reset".equals(id)) {
            AssistSettings.wardrobeShortcut = false;
            AssistSettings.petsShortcut = false;
            return true;
        }
        if ("item-lock-reset".equals(id)) {
            ItemLockSwapSettings.enabled = true;
            ItemLockSwapSettings.keyCode = ItemLockSwapSettings.DEFAULT_KEY_CODE;
            ItemLockSwapSettings.cooldownTenths = ItemLockSwapSettings.DEFAULT_COOLDOWN_TENTHS;
            ItemLockSwapSettings.showLockIcons = true;
            ItemLockSwapSettings.showIconsOnHotbar = true;
            ItemLockSwapSettings.showBlockedMessage = true;
            ItemLockSwapSettings.iconColor = 0;
            ItemLockSwapSettings.iconOpacity = 100;
            ItemLockSwapSettings.swapLineMode = ItemLockSwapSettings.SWAP_LINES_ALWAYS;
            ItemLockSwapSettings.swapLineColor = 0;
            ItemLockSwapSettings.swapLineOpacity = 100;
            ItemLockSwapSettings.swapLineWidth = ItemLockSwapSettings.DEFAULT_LINE_WIDTH;
            ItemLockSwapSettings.clearLocks();
            ItemLockSwapSettings.clearPairs();
            return true;
        }
        if ("commands-reset".equals(id)) {
            AppleSettings.svCommand = true;
            AppleSettings.qnCommand = true;
            return true;
        }
        if ("mining-assistant-reset".equals(id)) {
            AssistSettings.miningAbilityAlert = false;
            AssistSettings.miningPuzzlerMarker = false;
            return true;
        }
        if ("spirit-leap-reset".equals(id)) {
            AppleSettings.leapMenu = true;
            AppleSettings.leapKey = true;
            return true;
        }
        if ("terminal-reset".equals(id)) {
            AssistSettings.terminalHints = false;
            AssistSettings.terminalAutoClick = false;
            AssistSettings.terminalHideIncorrect = false;
            AssistSettings.terminalPreventWrongClicks = false;
            AssistSettings.terminalWaypoints = false;
            AssistSettings.terminalScale = 100;
            return true;
        }
        if ("m7-reset".equals(id)) {
            M7Settings.setOverlayEnabled(true);
            M7Settings.setStatueZonesEnabled(true);
            M7Settings.setAreasPersistAfterDragonSpawn(true);
            M7Settings.setDragonKillAlertEnabled(true);
            M7Settings.setTerminalSolverEnabled(true);
            for (M7OverlayRenderer.M7HitboxTarget target : M7OverlayRenderer.M7HitboxTarget.values()) {
                M7Settings.setBossHitboxEnabled(target, true);
            }
            for (M7OverlayRenderer.M7DragonStatue dragon : M7OverlayRenderer.M7DragonStatue.values()) {
                M7Settings.setDragonHitboxEnabled(dragon, true);
            }
            return true;
        }
        if ("slayer-reset".equals(id)) {
            AppleSettings.slayerBossTimer = true;
            AppleSettings.slayerPricing = true;
            return true;
        }
        if ("item-lock-clear-locks".equals(id)) { ItemLockSwapSettings.clearLocks(); return true; }
        if ("item-lock-clear-pairs".equals(id)) { ItemLockSwapSettings.clearPairs(); return true; }
        if ("fishing-reset".equals(id)) { FishingStats.reset(); return true; }
        if ("slayer-clear-spider".equals(id)) { SlayerTracker.clearHistory(SlayerType.SPIDER); return true; }
        if ("slayer-clear-revenant".equals(id)) { SlayerTracker.clearHistory(SlayerType.REVENANT); return true; }
        if ("slayer-clear-blaze".equals(id)) { SlayerTracker.clearHistory(SlayerType.BLAZE); return true; }
        if ("slayer-clear-wolf".equals(id)) { SlayerTracker.clearHistory(SlayerType.WOLF); return true; }
        if ("slayer-clear-enderman".equals(id)) { SlayerTracker.clearHistory(SlayerType.ENDERMAN); return true; }
        if ("esp-default-preset".equals(id) || "door-default-preset".equals(id)) { DungeonEspSettings.applyDefaultPreset(); return true; }
        if ("esp-subtle-preset".equals(id) || "door-subtle-preset".equals(id)) { DungeonEspSettings.applySubtlePreset(); return true; }
        if ("esp-reset".equals(id) || "door-reset".equals(id)) { DungeonEspSettings.resetDefaults(); return true; }
        if (id != null && id.endsWith("-copy")) { STYLE_CLIPBOARD.copy(styleForAction(id)); return true; }
        if (id != null && id.endsWith("-apply")) { STYLE_CLIPBOARD.apply(styleForAction(id)); return true; }
        if (id != null && id.endsWith("-apply-all")) {
            DungeonEspSettings.Style source = styleForAction(id);
            if (source != null) applyStyleToAllMobs(source);
            return true;
        }
        return false;
    }

    private static DungeonEspSettings.Style styleForToggle(String id) {
        if ("esp-starred".equals(id)) return DungeonEspSettings.starred;
        if ("esp-miniboss".equals(id)) return DungeonEspSettings.miniboss;
        if ("esp-special".equals(id)) return DungeonEspSettings.special;
        if ("esp-enderman".equals(id)) return DungeonEspSettings.enderman;
        if ("esp-skeletor".equals(id)) return DungeonEspSettings.skeletor;
        if ("esp-other".equals(id)) return DungeonEspSettings.otherMobs;
        if (id != null && id.endsWith("-walls")) return styleForValue(id);
        return null;
    }

    private static DungeonEspSettings.Style styleForValue(String id) {
        if (id == null) return null;
        if (id.startsWith("esp-starred-")) return DungeonEspSettings.starred;
        if (id.startsWith("esp-miniboss-")) return DungeonEspSettings.miniboss;
        if (id.startsWith("esp-special-")) return DungeonEspSettings.special;
        if (id.startsWith("esp-enderman-")) return DungeonEspSettings.enderman;
        if (id.startsWith("esp-skeletor-")) return DungeonEspSettings.skeletor;
        if (id.startsWith("esp-other-")) return DungeonEspSettings.otherMobs;
        if (id.startsWith("door-style-")) return DungeonEspSettings.doors;
        return null;
    }

    private static DungeonEspSettings.Style styleForAction(String id) {
        if (id == null) return null;
        String base = id;
        if (base.endsWith("-apply-all")) base = base.substring(0, base.length() - "-apply-all".length());
        else if (base.endsWith("-copy")) base = base.substring(0, base.length() - "-copy".length());
        else if (base.endsWith("-apply")) base = base.substring(0, base.length() - "-apply".length());
        return styleForToggle(base) != null ? styleForToggle(base) : ("door-style".equals(base) ? DungeonEspSettings.doors : null);
    }

    private static void applyStyleToAllMobs(DungeonEspSettings.Style source) {
        STYLE_CLIPBOARD.copy(source);
        STYLE_CLIPBOARD.apply(DungeonEspSettings.starred);
        STYLE_CLIPBOARD.apply(DungeonEspSettings.miniboss);
        STYLE_CLIPBOARD.apply(DungeonEspSettings.special);
        STYLE_CLIPBOARD.apply(DungeonEspSettings.enderman);
        STYLE_CLIPBOARD.apply(DungeonEspSettings.skeletor);
        STYLE_CLIPBOARD.apply(DungeonEspSettings.otherMobs);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    /** Session-only style clipboard; it does not add any persisted key. */
    private static final class StyleClipboard {
        private boolean hasValue;
        private boolean enabled;
        private int color;
        private int mode;
        private boolean throughWalls;
        private int outlineWidth;
        private int outlineOpacity;
        private int fillOpacity;

        void copy(DungeonEspSettings.Style style) {
            if (style == null) return;
            hasValue = true;
            enabled = style.enabled;
            color = style.color;
            mode = style.mode;
            throughWalls = style.throughWalls;
            outlineWidth = style.outlineWidth;
            outlineOpacity = style.outlineOpacity;
            fillOpacity = style.fillOpacity;
        }

        void apply(DungeonEspSettings.Style style) {
            if (!hasValue || style == null) return;
            style.enabled = enabled;
            style.color = color;
            style.mode = mode;
            style.throughWalls = throughWalls;
            style.outlineWidth = outlineWidth;
            style.outlineOpacity = outlineOpacity;
            style.fillOpacity = fillOpacity;
            DungeonEspSettings.normalize();
        }
    }
}
