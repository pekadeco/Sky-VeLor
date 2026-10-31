package com.arena.pekaclient.config;

import com.arena.pekaclient.dungeon.M7Settings;
import com.arena.pekaclient.hud.HudPlacement;
import com.arena.pekaclient.itemlockswap.ItemLockSwapSettings;
import net.minecraft.client.Minecraft;
import java.io.File;
import java.util.Properties;

/**
 * Lunaria client settings, saved to .minecraft/lunaria.properties.
 *
 * Existing prior-release and earliest legacy settings are read in that order when the Lunaria file does not yet
 * exist. They are never changed in place; the next normal save atomically writes the normalized Lunaria file.
 * Plain static fields keep reads allocation-free on the render path.
 */
public final class AppleSettings {

    public static final String FILE_NAME = "lunaria.properties";
    /** First compatibility fallback; never written after Lunaria has taken over. */
    public static final String PRIOR_RELEASE_LEGACY_FILE_NAME = "skyvelor.properties";
    /** Oldest supported fallback, read only if neither current nor prior-release settings exist. */
    public static final String PEKA_LEGACY_FILE_NAME = "pekaclient.properties";
    /** Schema v4 records additive dungeon HUD preferences without mutating saved choices. */
    static final String CONFIG_VERSION_KEY = "config.version";
    static final int CURRENT_CONFIG_VERSION = 4;

    public static final String[] ACCENT_NAMES = {"Blue", "Purple", "Pink", "Green", "Orange", "Teal"};
    public static final int[] ACCENT_COLORS = {
            0xFF0A84FF, 0xFFBF5AF2, 0xFFFF375F, 0xFF30D158, 0xFFFF9F0A, 0xFF64D2FF
    };
    /** Historical appearance values retained solely so existing settings files and HUD colour choices remain readable. */
    public static final String[] SETTINGS_THEME_NAMES = {"Forest & Cyan", "Black & Blue"};
    public static final int SETTINGS_THEME_FOREST_CYAN = 0;
    public static final int SETTINGS_THEME_BLACK_BLUE = 1;
    public static final int DEFAULT_SETTINGS_THEME = SETTINGS_THEME_FOREST_CYAN;
    public static final String[] CURVE_NAMES = {"Smooth", "Balanced", "Fast"};
    /** Arc subdivisions per 90 degrees for each curve quality; even Fast stays visually rounded. */
    public static final int[] CURVE_SEGMENTS = {16, 12, 8};
    public static final int DEFAULT_CURVE_QUALITY = 0;

    public static int accent = 0;
    /** Persisted legacy palette selection; Theme reads it only for compatibility, never to fork active GUI rendering. */
    public static int settingsTheme = DEFAULT_SETTINGS_THEME;
    /** The settings shell deliberately opens in its compact responsive layout unless the user chose fullscreen. */
    public static final boolean DEFAULT_SETTINGS_WINDOW_FULLSCREEN = false;
    /** Increments only when an old fullscreen selection must be reinterpreted as the compact shell. */
    static final String SETTINGS_WINDOW_LAYOUT_VERSION_KEY = "settings.window.layoutVersion";
    static final int CURRENT_SETTINGS_WINDOW_LAYOUT_VERSION = 2;
    public static boolean settingsWindowFullscreen = DEFAULT_SETTINGS_WINDOW_FULLSCREEN;
    public static boolean darkMode = true;
    public static int cornerRadius = 12;      // 4..18
    /** Switches for the global sliders. Off uses the default value below. */
    public static boolean cornerRadiusOn = true;
    public static boolean glassOpacityOn = true;
    public static boolean animSpeedOn = true;
    public static boolean scrollSpeedOn = true;
    public static final int DEFAULT_CORNER_RADIUS = 12;
    /** A slightly more transparent fresh-install panel setting; saved user values are not reset. */
    public static final int DEFAULT_GLASS_OPACITY = 88;
    /** The optional custom-GUI-transparency slider range. Lower opacity shows more of the game. */
    public static final int MIN_GLASS_OPACITY = 45;
    public static final int MAX_GLASS_OPACITY = 100;
    public static final int DEFAULT_ANIM_SPEED = 100;
    public static final boolean DEFAULT_ADAPTIVE_QUALITY = false;
    public static final boolean DEFAULT_PERFORMANCE_BOOST_BETA = true;
    public static final int DEFAULT_SCROLL_SPEED = 100;
    /** One global animation policy for all Lunaria-owned scroll viewports, including chat. */
    public static final boolean DEFAULT_SMOOTH_SCROLLING = true;
    /** Opt-in gameplay mouse policy; GUI wheel scrolling and numeric hotbar keys are never affected. */
    public static final boolean DEFAULT_DISABLE_HOTBAR_SCROLLING = false;
    public static boolean disableHotbarScrolling = DEFAULT_DISABLE_HOTBAR_SCROLLING;
    /** Default-on QoL particle policy; only vanilla block digging/crack particles are filtered at addEffect. */
    public static final boolean DEFAULT_NO_BLOCK_PARTICLES = true;
    public static boolean noBlockParticles = DEFAULT_NO_BLOCK_PARTICLES;

    // Item Animation. Percent controls use 100% for untouched vanilla; offsets are hundredths of a
    // first-person render unit. Keeping the bounds here makes loading, exact-value entry and sliders agree.
    public static final int DEFAULT_ITEM_ANIMATION_SIZE = 100;
    public static final int DEFAULT_ITEM_ANIMATION_SPEED = 100;
    public static final int ITEM_ANIMATION_SIZE_MIN_PERCENT = 30;
    public static final int ITEM_ANIMATION_SIZE_MAX_PERCENT = 200;
    public static final int ITEM_ANIMATION_SPEED_MIN_PERCENT = 25;
    public static final int ITEM_ANIMATION_SPEED_MAX_PERCENT = 400;
    public static final int ITEM_ANIMATION_OFFSET_MIN = -150;
    public static final int ITEM_ANIMATION_OFFSET_MAX = 150;
    public static final int DEFAULT_ITEM_ANIMATION_X = 0;
    public static final int DEFAULT_ITEM_ANIMATION_Y = 0;
    public static final int DEFAULT_ITEM_ANIMATION_Z = 0;
    public static final int DEFAULT_ITEM_ANIMATION_HIT_SLASH_STYLE = 0;
    /** Smooth Swing is default-on; its curve is visual-only and evaluates from the current partial-tick progress. */
    public static final boolean DEFAULT_ITEM_ANIMATION_SMOOTH_SWING = true;
    public static final int ITEM_ANIMATION_SMOOTHNESS_MIN_PERCENT = 0;
    public static final int ITEM_ANIMATION_SMOOTHNESS_MAX_PERCENT = 100;
    public static final int DEFAULT_ITEM_ANIMATION_SMOOTHNESS = 65;
    // Existing indexes stay fixed so saved 0.10.7.x style values keep their exact appearance; new styles append.
    public static final String[] ITEM_ANIMATION_HIT_SLASH_STYLES = {
            "Vanilla", "Classic Hit", "Slash", "Chop", "Stab"
    };
    /** Master switch for the complete visual-only Item Animation renderer path. Default on. */
    public static boolean itemAnimationEnabled = true;
    /** Legacy-compatible per-value switches: off resolves that one value to its vanilla default. */
    public static boolean itemAnimationSizeOn = true;
    public static boolean itemAnimationSpeedOn = true;
    /** Keeps the custom first-person swing timeline independent of the real Haste potion effect. */
    public static boolean itemAnimationRemoveHasteEffect = true;
    /** Visual first-person hit/slash shape, indexed by {@link #ITEM_ANIMATION_HIT_SLASH_STYLES}. */
    public static int itemAnimationHitSlashStyle = DEFAULT_ITEM_ANIMATION_HIT_SLASH_STYLE;
    /** Local first-person curve only; it never changes swing timing, packets, attacks, or potion effects. */
    public static boolean itemAnimationSmoothSwing = DEFAULT_ITEM_ANIMATION_SMOOTH_SWING;
    /** Blend between linear and smoothstep motion, in percent. */
    public static int itemAnimationSmoothness = DEFAULT_ITEM_ANIMATION_SMOOTHNESS;

    /** Corner radius in use: the slider value when its switch is on, otherwise the default. */
    public static int effectiveCornerRadius() {
        return cornerRadiusOn ? cornerRadius : DEFAULT_CORNER_RADIUS;
    }

    public static int effectiveGlassOpacity() {
        return glassOpacityOn ? glassOpacity : DEFAULT_GLASS_OPACITY;
    }

    public static int effectiveAnimSpeed() {
        return animSpeedOn ? animSpeed : DEFAULT_ANIM_SPEED;
    }

    public static int effectiveScrollSpeed() {
        return scrollSpeedOn ? scrollSpeed : DEFAULT_SCROLL_SPEED;
    }

    /** Shared wheel multiplier; consumers must call their viewport once per real wheel event. */
    public static float guiScrollMultiplier() {
        return effectiveScrollSpeed() / 100.0f;
    }

    /** Effective Item Animation size; an off switch is the exact vanilla value. */
    public static int effectiveItemAnimationSize() {
        return itemAnimationSizeOn ? Math.max(ITEM_ANIMATION_SIZE_MIN_PERCENT,
                Math.min(ITEM_ANIMATION_SIZE_MAX_PERCENT, itemAnimationSize)) : DEFAULT_ITEM_ANIMATION_SIZE;
    }

    /** Effective Item Animation equip-transition speed; it never affects attacks or gameplay. */
    public static int effectiveItemAnimationSpeed() {
        return itemAnimationSpeedOn ? Math.max(ITEM_ANIMATION_SPEED_MIN_PERCENT,
                Math.min(ITEM_ANIMATION_SPEED_MAX_PERCENT, itemAnimationSpeed)) : DEFAULT_ITEM_ANIMATION_SPEED;
    }

    /** Custom GUI glass opacity in percent (45..100). Higher is more solid, lower shows more of the game. */
    public static int glassOpacity = DEFAULT_GLASS_OPACITY;
    public static boolean animations = true;
    public static int animSpeed = 100;        // 40..200 (%)
    public static int curveQuality = DEFAULT_CURVE_QUALITY; // 0 smooth, 1 balanced, 2 fast
    public static boolean shadows = true;
    public static boolean adaptive = DEFAULT_ADAPTIVE_QUALITY; // auto-lower detail when FPS drops
    public static int scrollSpeed = 100;      // 50..300 (%), wheel and swipe speed (keys are fixed)
    public static boolean smoothScrolling = DEFAULT_SMOOTH_SCROLLING;

    // Item Animation renderer settings. They are read live by the first-person render and equip hooks.
    public static int itemAnimationSize = DEFAULT_ITEM_ANIMATION_SIZE;      // 30..200 (%)
    public static int itemAnimationSpeed = DEFAULT_ITEM_ANIMATION_SPEED;    // 25..400 (%)
    /** X/Y/Z visual offsets, in 1/100ths of a first-person render unit (-150..150). */
    public static int itemAnimationX = DEFAULT_ITEM_ANIMATION_X;
    public static int itemAnimationY = DEFAULT_ITEM_ANIMATION_Y;
    public static int itemAnimationZ = DEFAULT_ITEM_ANIMATION_Z;

    /** Dungeon tab: replace the server's Spirit Leap menu with the teammate selector. On by default. */
    public static boolean leapMenu = true;

    // Live dungeon-map overlay imported from the newest Lunaria source. Position uses the established HUD
    // anchors but deliberately remains unclamped, so an editor reset can always recover a map moved off-screen.
    public static boolean dungeonMap = true;
    public static boolean dungeonMapRotate = true;
    public static boolean dungeonMapShowPlayerNames = true;
    /** Dungeon-only skin-face markers replace available local/party map arrows by default. */
    public static boolean dungeonMapPlayerHeads = true;
    public static final int DUNGEON_MAP_PLAYER_HEAD_SIZE_MIN = 4;
    public static final int DUNGEON_MAP_PLAYER_HEAD_SIZE_MAX = 16;
    public static int dungeonMapPlayerHeadSize = 8;
    /** Retains the existing saved key: it controls discovered room/puzzle labels. */
    public static boolean dungeonMapShowPuzzleName = true;
    /** Secrets text is a map-only presentation choice; it never changes dungeon observations. */
    public static boolean dungeonMapShowSecrets = true;
    /** A borderless backing surface is opt-in, preserving the established bare-map appearance. */
    public static boolean dungeonMapBackground = false;
    public static final int DUNGEON_MAP_BACKGROUND_OPACITY_MIN = 10;
    public static final int DUNGEON_MAP_BACKGROUND_OPACITY_MAX = 95;
    public static int dungeonMapBackgroundOpacity = 48;
    public static final String[] DUNGEON_MAP_BACKGROUND_COLOR_NAMES = {"Black", "Navy", "Charcoal", "Purple"};
    public static final int[] DUNGEON_MAP_BACKGROUND_COLORS = {
            0xFF0A0A0A, 0xFF101A35, 0xFF202124, 0xFF21142D
    };
    public static int dungeonMapBackgroundColor;
    public static final String[] DUNGEON_MAP_LABEL_COLOR_NAMES = {"Green", "White", "Aqua", "Gold", "Theme"};
    public static final int[] DUNGEON_MAP_LABEL_COLORS = {
            0xFF55FF55, 0xFFFFFFFF, 0xFF55FFFF, 0xFFFFAA00, 0
    };
    /** Independent readable colors for room labels and the Secrets counter. */
    public static int dungeonMapRoomNameColor;
    public static int dungeonMapSecretsColor = 1;
    /** Opt-in only: map detection never sends party chat unless the player enables this. */
    public static boolean partyPuzzleAnnounce = false;
    public static final int DUNGEON_MAP_SCALE_MIN = 70;
    public static final int DUNGEON_MAP_SCALE_MAX = 250;
    public static int dungeonMapScale = 100;
    public static boolean dungeonMapScaleOn = true;
    public static final int DUNGEON_MAP_ZOOM_MIN = 50;
    public static final int DUNGEON_MAP_ZOOM_MAX = 500;
    public static int dungeonMapZoom = 100;
    /** Legacy percentage coordinates are kept only for migration from map-enabled friend configs. */
    public static int dungeonMapXPercent = 2;
    public static int dungeonMapYPercent = 2;
    public static final HudPlacement dungeonMapPlacement = new HudPlacement(
            HudPlacement.Anchor.START, 12, HudPlacement.Anchor.START, 12);
    private static boolean dungeonMapPlacementInitialized = true;

    // Dungeon HUD settings. The HUD elements themselves persist position/scale/enable state in the prior-release HUD file;
    // these additive keys retain only their visible-line and alert behaviour choices in the established main file.
    public static final boolean DEFAULT_DUNGEON_SCORE_SHOW_SCORE = true;
    public static final boolean DEFAULT_DUNGEON_SCORE_SHOW_CRYPTS = true;
    public static final boolean DEFAULT_DUNGEON_SCORE_SHOW_DEATHS = true;
    public static final boolean DEFAULT_DUNGEON_SCORE_SHOW_RUN_TIME = true;
    public static boolean dungeonScoreShowScore = DEFAULT_DUNGEON_SCORE_SHOW_SCORE;
    public static boolean dungeonScoreShowCrypts = DEFAULT_DUNGEON_SCORE_SHOW_CRYPTS;
    public static boolean dungeonScoreShowDeaths = DEFAULT_DUNGEON_SCORE_SHOW_DEATHS;
    public static boolean dungeonScoreShowRunTime = DEFAULT_DUNGEON_SCORE_SHOW_RUN_TIME;
    public static final boolean DEFAULT_DUNGEON_ROOM_CLEAR_SOUND = false;
    public static boolean dungeonRoomClearSound = DEFAULT_DUNGEON_ROOM_CLEAR_SOUND;
    public static final int DUNGEON_ROOM_CLEAR_DURATION_MIN_MILLIS = 500;
    public static final int DUNGEON_ROOM_CLEAR_DURATION_MAX_MILLIS = 5000;
    public static final int DEFAULT_DUNGEON_ROOM_CLEAR_DURATION_MILLIS = 1800;
    public static int dungeonRoomClearDurationMillis = DEFAULT_DUNGEON_ROOM_CLEAR_DURATION_MILLIS;
    /** Explicitly opt-in: a normal secret chest is never closed unless the player enables this preference. */
    public static final boolean DEFAULT_DUNGEON_AUTO_CHEST_CLOSE = false;
    public static boolean dungeonAutoChestClose = DEFAULT_DUNGEON_AUTO_CHEST_CLOSE;
    /**
     * Performance Boost Beta. Enabled on a fresh install. Selects lower-cost Lunaria GUI/HUD work:
     * fewer curve segments and no shadows in the hub and editor, slower HUD scans, and cached HUD text.
     * It never touches entity rendering, world rendering or JVM flags.
     */
    public static boolean experimentalPerformance = DEFAULT_PERFORMANCE_BOOST_BETA;
    public static boolean slayerTracker = true;        // track Slayer drops from chat
    public static boolean slayerBossTimer = true;      // boss spawn to quest complete timer
    public static boolean slayerPricing = true;        // profit and /setprice
    public static boolean svCommand = true;            // /lunaria opens the hub
    public static boolean qnCommand = true;            // /qn opens quick notes
    public static boolean leapKey = true;              // Spirit Leap key works when bound
    public static boolean fishingCounter = true;       // count sea-creature pulls
    public static boolean fishingFestival = true;      // Festival Great White count and best
    public static boolean fishingJawbusAlert = true;   // title when Lord Jawbus arrives
    /** New nearby-nameplate alerts are opt-in; the established chat-arrival alert remains unchanged. */
    public static final boolean DEFAULT_FISHING_NEARBY_ENCOUNTER_ALERT = false;
    public static boolean fishingNearbyEncounterAlert = DEFAULT_FISHING_NEARBY_ENCOUNTER_ALERT;

    // Retained friend-source feature preferences. These stay in the established main settings file so a
    // prior-release configuration remains readable, while current isolated settings owners retain priority on overlap.
    // The fields are intentionally public because the lightweight render/event hooks read them without allocations.
    public static final int DEFAULT_FIRST_PERSON_ITEM_SCALE = 100;
    public static final int DEFAULT_FIRST_PERSON_ANIM_SPEED = 100;
    public static final int FIRST_PERSON_MIN = 50;
    public static final int FIRST_PERSON_MAX = 200;
    /** Compatibility mirrors for the retired first-person surface; current Item Animation remains authoritative. */
    public static boolean firstPersonItemScaleOn = true;
    public static int firstPersonItemScale = DEFAULT_FIRST_PERSON_ITEM_SCALE;
    public static boolean firstPersonAnimSpeedOn = true;
    public static int firstPersonAnimSpeed = DEFAULT_FIRST_PERSON_ANIM_SPEED;

    public static boolean mobHighlights;
    public static int starredMobDisplayMode;
    public static boolean starredMobHighlights = true;
    public static boolean starredMobEsp = true;
    public static final String DEFAULT_STARRED_MOB_COLOR = "0:92:154:255:255";
    public static final String DEFAULT_STARRED_MOB_STORMY_COLOR = "0:173:216:230:255";
    public static final String DEFAULT_STARRED_MOB_WITHERMANCER_COLOR = "0:169:169:169:255";
    public static final String DEFAULT_STARRED_MOB_ZOMBIE_COMMANDER_COLOR = "0:255:0:0:255";
    public static final String DEFAULT_STARRED_MOB_SKELETON_MASTER_COLOR = "0:255:100:0:255";
    public static String starredMobColor = DEFAULT_STARRED_MOB_COLOR;
    public static String starredMobStormyColor = DEFAULT_STARRED_MOB_STORMY_COLOR;
    public static String starredMobWithermancerColor = DEFAULT_STARRED_MOB_WITHERMANCER_COLOR;
    public static String starredMobZombieCommanderColor = DEFAULT_STARRED_MOB_ZOMBIE_COMMANDER_COLOR;
    public static String starredMobSkeletonMasterColor = DEFAULT_STARRED_MOB_SKELETON_MASTER_COLOR;
    public static boolean felEsp;
    public static String felEspColor = "0:255:0:255:0";
    public static boolean bloodMobHighlight;
    public static String bloodMobHighlightColor = "0:255:0:255:0";

    public static boolean removeIrritatingMessagesEnabled = true;
    public static boolean muteDungeonBossCrowdMessages = true;
    public static boolean dungeonAutoCloseSecretChests = true;
    public static boolean dungeonWitherDoorsHighlight = true;
    public static boolean dungeonHideUnableToLocateSign = true;
    public static boolean dungeonSpiritBowTracer = true;
    public static boolean dungeonSPlusReminder = true;
    public static boolean dungeonSNotifier = true;
    public static boolean dungeonSPlusPartyMessage = true;
    public static String dungeonSPlusCustomMessage = "";
    public static boolean dungeonScoreSimple = true;
    public static boolean dungeonPaulBonus;
    public static boolean dungeonBloodReadyTitle = true;
    public static boolean equipmentOverlayEnabled = true;
    public static boolean showCurrentPet = true;

    public static boolean itemProtectionEnabled = true;
    public static boolean itemProtectionNotifications = true;
    /** Dormant legacy Slot Lock controls: retained for persistence but not registered beside Item Lock & Swap. */
    public static boolean slotLockingEnabled = true;
    public static boolean slotBindingEnabled = true;
    public static boolean slotLockNotifications = true;
    public static boolean inventorySearchEnabled = true;
    public static boolean hideInventoryPotionEffects = true;
    public static boolean autoSprintEnabled = true;
    public static boolean romanNumeralsInTooltips = true;
    public static boolean scrollableTooltipsEnabled = true;
    public static boolean chatDisableWatchdogInfo = true;
    public static boolean chatDisableFriendJoin;
    public static boolean chatDisableZombieRareDrops = true;
    public static boolean chatDisableSellingRanks;
    public static boolean chatDisableServerAnnouncements = true;
    public static boolean chatDisableGameInvites = true;
    public static boolean itemRarityBordersEnabled = true;
    public static boolean chatLengthEnabled = true;
    public static boolean chatCopyEnabled = true;
    public static int chatFilterMode;
    public static boolean chatMessageAnimation = true;
    public static int chatMessageSpeed = 50;
    public static boolean chatBackgroundAnimation = true;
    public static int chatBackgroundDuration = 400;
    public static boolean chatScrollAnimation = true;
    public static int chatScrollDuration = 150;
    public static boolean chatRemoveScrollBar = true;
    public static boolean noHurtCamEnabled = true;
    /** Compatibility mirror for the dedicated particle preference. */
    public static boolean hideBlockBreakingParticles = true;
    /** Compatibility mirror for the dedicated dead-mob preference. */
    public static boolean hideDeadMobs = true;
    public static boolean hideFlamingFists = true;
    public static boolean wardrobeShortcutEnabled;
    public static boolean petsShortcutEnabled;
    public static boolean miningAbilityNotifier;
    public static boolean miningPuzzlerSolver;

    public static boolean solverThreeWeirdos = true;
    public static boolean solverCreeper = true;
    public static boolean solverBoulder = true;
    public static boolean solverSilverfish = true;
    public static boolean solverWater = true;
    public static boolean solverTeleportMaze = true;
    public static boolean terminalTracker = true;
    public static float terminalTrackerScale = 1.0F;
    public static String terminalTrackerColor = "0:255:0:100:255";
    public static int terminalTrackerX = 10;
    public static int terminalTrackerY = 10;
    public static boolean terminalCustomClickIn = true;
    public static boolean terminalCustomColors = true;
    public static boolean terminalCustomMaze = true;
    public static boolean terminalCustomStartsWith = true;
    public static boolean terminalCustomPanes = true;
    public static float terminalGuiScale = 3.0F;
    public static boolean terminalStartsWithSolver = true;
    public static boolean terminalColorsSolver = true;
    public static boolean terminalClickInOrderSolver = true;
    public static boolean terminalMazeSolver = true;
    public static boolean terminalCorrectPanesSolver = true;
    public static boolean terminalHideIncorrect = true;
    public static boolean terminalPreventMissclicks = true;
    public static boolean terminalHideTooltips = true;
    public static boolean solverFirstDevice = true;
    public static boolean solverThirdDevice = true;
    public static String solverCorrectColor = "0:255:0:255:0";
    public static String solverAlternativeColor = "0:255:255:255:0";
    public static boolean terminalWaypoints = true;

    // Chat QoL has its own compact, Minecraft-free settings model. It is loaded/saved through this established
    // config file so old HUD positions and all other Lunaria settings remain untouched.

    /** Set while a legacy firstPerson* key was read; load immediately writes its Item Animation replacement. */
    private static boolean itemAnimationMigrationPending;
    private static final String[] RETIRED_ITEM_ANIMATION_BACKGROUND_KEYS = {
            "itemAnimation.backgroundTransparency", "itemAnimation.backgroundOpacity", "itemAnimation.backgroundAlpha",
            "itemAnimation.transparent", "firstPersonItemBackgroundTransparency", "firstPersonItemBackgroundOpacity",
            "firstPersonItemBackgroundAlpha"
    };
    /** Blur duplicates require unsafe translucent re-rendering, so historic draft keys are harmless and never saved. */
    private static final String[] RETIRED_ITEM_ANIMATION_BLUR_KEYS = {
            "itemAnimation.swingBlur", "itemAnimation.swingBlurIntensity", "itemAnimation.blur",
            "itemAnimation.blurIntensity", "firstPersonSwingBlur", "firstPersonSwingBlurIntensity"
    };
    /** Set when target-local Chat QoL scrolling is promoted into the one global viewport policy. */
    private static boolean globalScrollMigrationPending;
    /** Set while an older schema marker needs to be rewritten without changing loaded values. */
    private static boolean configVersionMigrationPending;
    /** Set only when a pre-compact-layout fullscreen flag is reset once, before later explicit fullscreen choices. */
    private static boolean settingsWindowLayoutMigrationPending;

    private AppleSettings() {
    }

    public static int accentColor() {
        return ACCENT_COLORS[Math.max(0, Math.min(ACCENT_COLORS.length - 1, accent))];
    }

    /** Historical compatibility query; the fixed active Theme deliberately ignores this legacy palette choice. */
    public static boolean isBlackBlueTheme() {
        return settingsTheme == SETTINGS_THEME_BLACK_BLUE;
    }

    /** Reads the additive theme key without mutating any existing appearance choice. */
    static void loadSettingsTheme(Properties settings) {
        Properties p = settings == null ? new Properties() : settings;
        settingsTheme = readInt(p, "appearance.settingsTheme", DEFAULT_SETTINGS_THEME,
                SETTINGS_THEME_FOREST_CYAN, SETTINGS_THEME_BLACK_BLUE);
    }

    /** Writes the one stable theme-selection key alongside the established appearance keys. */
    static void saveSettingsTheme(Properties settings) {
        if (settings != null) {
            settings.setProperty("appearance.settingsTheme", Integer.toString(Math.max(SETTINGS_THEME_FOREST_CYAN,
                    Math.min(SETTINGS_THEME_BLACK_BLUE, settingsTheme))));
        }
    }

    /**
     * Reads the small/fullscreen shell choice. The previous wide-shell fullscreen flag is migrated once to the new
     * compact default, while the version marker means a later deliberate Fullscreen click remains fully persistent.
     */
    static void loadSettingsWindowLayout(Properties settings) {
        Properties p = settings == null ? new Properties() : settings;
        settingsWindowLayoutMigrationPending = false;
        int layoutVersion = readInt(p, SETTINGS_WINDOW_LAYOUT_VERSION_KEY, 0, 0,
                CURRENT_SETTINGS_WINDOW_LAYOUT_VERSION);
        boolean requestedFullscreen = readBool(p, "settings.window.fullscreen", DEFAULT_SETTINGS_WINDOW_FULLSCREEN);
        if (layoutVersion < CURRENT_SETTINGS_WINDOW_LAYOUT_VERSION && requestedFullscreen) {
            settingsWindowFullscreen = DEFAULT_SETTINGS_WINDOW_FULLSCREEN;
            settingsWindowLayoutMigrationPending = true;
        } else {
            settingsWindowFullscreen = requestedFullscreen;
        }
    }

    /** Persists only the selected shell size mode/version, never a screen coordinate or a gameplay setting. */
    static void saveSettingsWindowLayout(Properties settings) {
        if (settings != null) {
            settings.setProperty("settings.window.fullscreen", Boolean.toString(settingsWindowFullscreen));
            settings.setProperty(SETTINGS_WINDOW_LAYOUT_VERSION_KEY,
                    Integer.toString(CURRENT_SETTINGS_WINDOW_LAYOUT_VERSION));
        }
    }

    /**
     * Loads the established appearance-owner keys without renaming them. Package visibility keeps this legacy
     * compatibility boundary directly testable without requiring a Minecraft configuration directory.
     */
    static void loadAppearanceSettings(Properties settings) {
        Properties p = settings == null ? new Properties() : settings;
        accent = readInt(p, "accent", accent, 0, ACCENT_COLORS.length - 1);
        darkMode = readBool(p, "darkMode", darkMode);
        cornerRadius = readInt(p, "cornerRadius", cornerRadius, 4, 18);
        cornerRadiusOn = readBool(p, "cornerRadiusOn", cornerRadiusOn);
        glassOpacityOn = readBool(p, "glassOpacityOn", glassOpacityOn);
        animSpeedOn = readBool(p, "animSpeedOn", animSpeedOn);
        scrollSpeedOn = readBool(p, "scrollSpeedOn", scrollSpeedOn);
        // Keep the legacy glassOpacity key; older values remain valid while the refreshed slider permits 45..100.
        glassOpacity = readInt(p, "glassOpacity", glassOpacity, MIN_GLASS_OPACITY, MAX_GLASS_OPACITY);
        animations = readBool(p, "animations", animations);
        animSpeed = readInt(p, "animSpeed", animSpeed, 40, 200);
        curveQuality = readInt(p, "curveQuality", DEFAULT_CURVE_QUALITY, 0, 2);
        shadows = readBool(p, "shadows", shadows);
    }

    /** Writes the same established appearance-owner keys used by older configurations. */
    static void saveAppearanceSettings(Properties p) {
        if (p == null) return;
        p.setProperty("accent", Integer.toString(accent));
        p.setProperty("darkMode", Boolean.toString(darkMode));
        p.setProperty("cornerRadius", Integer.toString(cornerRadius));
        p.setProperty("cornerRadiusOn", Boolean.toString(cornerRadiusOn));
        p.setProperty("glassOpacityOn", Boolean.toString(glassOpacityOn));
        p.setProperty("animSpeedOn", Boolean.toString(animSpeedOn));
        p.setProperty("scrollSpeedOn", Boolean.toString(scrollSpeedOn));
        p.setProperty("glassOpacity", Integer.toString(glassOpacity));
        p.setProperty("animations", Boolean.toString(animations));
        p.setProperty("animSpeed", Integer.toString(animSpeed));
        p.setProperty("curveQuality", Integer.toString(curveQuality));
        p.setProperty("shadows", Boolean.toString(shadows));
    }

    public static void load() {
        itemAnimationMigrationPending = false;
        globalScrollMigrationPending = false;
        configVersionMigrationPending = false;
        settingsWindowLayoutMigrationPending = false;
        Properties p = ConfigFiles.read(readFile());
        loadSettingsTheme(p);
        loadSettingsWindowLayout(p);
        loadAppearanceSettings(p);
        loadPerformanceSettings(p);
        loadGlobalScrollSettings(p);
        loadMouseSettings(p);
        loadParticleSettings(p);

        loadItemAnimationSettings(p);
        // Own categories keep newer gameplay state isolated from the legacy shared settings surface.
        ItemLockSwapSettings.load(p);
        AssistSettings.load(p);

        leapMenu = readBool(p, "leapMenu", leapMenu);
        // Dungeon ESP owns its isolated prefixed schema and consumes the retired mobHighlights toggle once.
        DungeonEspSettings.load(p);
        dungeonMap = readBool(p, "dungeonMap", dungeonMap);
        dungeonMapRotate = readBool(p, "dungeonMapRotate", dungeonMapRotate);
        dungeonMapShowPlayerNames = readBool(p, "dungeonMapShowPlayerNames", dungeonMapShowPlayerNames);
        dungeonMapPlayerHeads = readBool(p, "dungeonMapPlayerHeads", dungeonMapPlayerHeads);
        dungeonMapPlayerHeadSize = readInt(p, "dungeonMapPlayerHeadSize", dungeonMapPlayerHeadSize,
                DUNGEON_MAP_PLAYER_HEAD_SIZE_MIN, DUNGEON_MAP_PLAYER_HEAD_SIZE_MAX);
        dungeonMapShowPuzzleName = readBool(p, "dungeonMapShowPuzzleName", dungeonMapShowPuzzleName);
        dungeonMapShowSecrets = readBool(p, "dungeonMapShowSecrets", dungeonMapShowSecrets);
        dungeonMapBackground = readBool(p, "dungeonMapBackground", dungeonMapBackground);
        dungeonMapBackgroundOpacity = readInt(p, "dungeonMapBackgroundOpacity", dungeonMapBackgroundOpacity,
                DUNGEON_MAP_BACKGROUND_OPACITY_MIN, DUNGEON_MAP_BACKGROUND_OPACITY_MAX);
        dungeonMapBackgroundColor = readInt(p, "dungeonMapBackgroundColor", dungeonMapBackgroundColor, 0,
                DUNGEON_MAP_BACKGROUND_COLORS.length - 1);
        dungeonMapRoomNameColor = readInt(p, "dungeonMapRoomNameColor", dungeonMapRoomNameColor, 0,
                DUNGEON_MAP_LABEL_COLORS.length - 1);
        dungeonMapSecretsColor = readInt(p, "dungeonMapSecretsColor", dungeonMapSecretsColor, 0,
                DUNGEON_MAP_LABEL_COLORS.length - 1);
        partyPuzzleAnnounce = readBool(p, "partyPuzzleAnnounce", partyPuzzleAnnounce);
        dungeonMapScale = readInt(p, "dungeonMapScale", dungeonMapScale,
                DUNGEON_MAP_SCALE_MIN, DUNGEON_MAP_SCALE_MAX);
        dungeonMapScaleOn = readBool(p, "dungeonMapScaleOn", dungeonMapScaleOn);
        dungeonMapZoom = readInt(p, "dungeonMapZoom", dungeonMapZoom,
                DUNGEON_MAP_ZOOM_MIN, DUNGEON_MAP_ZOOM_MAX);
        dungeonMapXPercent = readInt(p, "dungeonMapXPercent", dungeonMapXPercent, 0, 100);
        dungeonMapYPercent = readInt(p, "dungeonMapYPercent", dungeonMapYPercent, 0, 100);
        if (p.containsKey("dungeonMapAx") && p.containsKey("dungeonMapOx")
                && p.containsKey("dungeonMapAy") && p.containsKey("dungeonMapOy")) {
            dungeonMapPlacement.reset(
                    readAnchor(p, "dungeonMapAx", HudPlacement.Anchor.START),
                    readInt(p, "dungeonMapOx", 12, -HudPlacement.MAX_OFFSET, HudPlacement.MAX_OFFSET),
                    readAnchor(p, "dungeonMapAy", HudPlacement.Anchor.START),
                    readInt(p, "dungeonMapOy", 12, -HudPlacement.MAX_OFFSET, HudPlacement.MAX_OFFSET));
            dungeonMapPlacementInitialized = true;
        } else {
            dungeonMapPlacement.reset(HudPlacement.Anchor.START, 12, HudPlacement.Anchor.START, 12);
            dungeonMapPlacementInitialized = false;
        }
        loadDungeonHudSettings(p);
        leapKey = readBool(p, "leapKey", leapKey);
        svCommand = readBool(p, "cmd.sv", svCommand);
        slayerTracker = readBool(p, "slayer.tracker", slayerTracker);
        slayerBossTimer = readBool(p, "slayer.bossTimer", slayerBossTimer);
        slayerPricing = readBool(p, "slayer.pricing", slayerPricing);
        qnCommand = readBool(p, "cmd.qn", qnCommand);
        loadFishingSettings(p);
        loadChatSettings(p);
        FontSettings.load(p);
        // Friend-source keys are read after current owners so their historic values can only fill a missing current key.
        loadFriendFeatureSettings(p);
        // Editor navigation is presentation state only; it never changes existing feature schemas or defaults.
        FeatureUiPreferences.load(p);
        applyConfigurationVersionUpgrade(p);
        // The supported fonts.* schema is loaded above; FontSettings consumes its retired chat.qol.*font* inputs
        // without changing source files, and the next save writes the one current font schema.
        M7Settings.load(p);
        // A legacy config is not merely read: persist the clean Item Animation category immediately. The
        // atomic writer preserves a valid file even if the client is interrupted while migrating.
        if (itemAnimationMigrationPending || globalScrollMigrationPending || configVersionMigrationPending
                || settingsWindowLayoutMigrationPending || ChatQolSettings.isMigrationPending()
                || FontSettings.isMigrationPending()) {
            save();
        }
    }

    /**
     * Loads additive Dungeon HUD presentation preferences. Missing keys retain safe current defaults and do not
     * rewrite an existing player's map, HUD placement, or feature enable choices.
     */
    static void loadDungeonHudSettings(Properties settings) {
        Properties p = settings == null ? new Properties() : settings;
        dungeonScoreShowScore = readBool(p, "dungeonHud.score", DEFAULT_DUNGEON_SCORE_SHOW_SCORE);
        dungeonScoreShowCrypts = readBool(p, "dungeonHud.crypts", DEFAULT_DUNGEON_SCORE_SHOW_CRYPTS);
        dungeonScoreShowDeaths = readBool(p, "dungeonHud.deaths", DEFAULT_DUNGEON_SCORE_SHOW_DEATHS);
        dungeonScoreShowRunTime = readBool(p, "dungeonHud.runTime", DEFAULT_DUNGEON_SCORE_SHOW_RUN_TIME);
        dungeonRoomClearSound = readBool(p, "dungeonHud.roomClearSound", DEFAULT_DUNGEON_ROOM_CLEAR_SOUND);
        dungeonRoomClearDurationMillis = readInt(p, "dungeonHud.roomClearDurationMillis",
                DEFAULT_DUNGEON_ROOM_CLEAR_DURATION_MILLIS, DUNGEON_ROOM_CLEAR_DURATION_MIN_MILLIS,
                DUNGEON_ROOM_CLEAR_DURATION_MAX_MILLIS);
        dungeonAutoChestClose = readBool(p, "dungeonHud.autoChestClose", DEFAULT_DUNGEON_AUTO_CHEST_CLOSE);
    }

    /** Writes just the Dungeon HUD presentation keys; HUD positions and enable states remain HudManager-owned. */
    static void saveDungeonHudSettings(Properties p) {
        if (p == null) return;
        dungeonRoomClearDurationMillis = Math.max(DUNGEON_ROOM_CLEAR_DURATION_MIN_MILLIS,
                Math.min(DUNGEON_ROOM_CLEAR_DURATION_MAX_MILLIS, dungeonRoomClearDurationMillis));
        p.setProperty("dungeonHud.score", Boolean.toString(dungeonScoreShowScore));
        p.setProperty("dungeonHud.crypts", Boolean.toString(dungeonScoreShowCrypts));
        p.setProperty("dungeonHud.deaths", Boolean.toString(dungeonScoreShowDeaths));
        p.setProperty("dungeonHud.runTime", Boolean.toString(dungeonScoreShowRunTime));
        p.setProperty("dungeonHud.roomClearSound", Boolean.toString(dungeonRoomClearSound));
        p.setProperty("dungeonHud.roomClearDurationMillis", Integer.toString(dungeonRoomClearDurationMillis));
        p.setProperty("dungeonHud.autoChestClose", Boolean.toString(dungeonAutoChestClose));
    }

    /** Clamps a direct/programmatic duration read, matching load and save semantics. */
    public static int effectiveDungeonRoomClearDurationMillis() {
        return Math.max(DUNGEON_ROOM_CLEAR_DURATION_MIN_MILLIS,
                Math.min(DUNGEON_ROOM_CLEAR_DURATION_MAX_MILLIS, dungeonRoomClearDurationMillis));
    }

    /**
     * Reads the one global scroll policy. Retired split-chat target keys are deliberately ignored: they must never
     * recreate behavior or silently alter the modern global choice.
     */
    static void loadGlobalScrollSettings(Properties settings) {
        Properties p = settings == null ? new Properties() : settings;
        scrollSpeed = readInt(p, "scrollSpeed", DEFAULT_SCROLL_SPEED, 50, 300);
        smoothScrolling = readBool(p, "smoothScrolling", DEFAULT_SMOOTH_SCROLLING);
        globalScrollMigrationPending = false;
    }

    /** Reads the Mouse section independently so old files simply retain vanilla hotbar-wheel selection. */
    static void loadMouseSettings(Properties settings) {
        Properties p = settings == null ? new Properties() : settings;
        disableHotbarScrolling = readBool(p, "mouse.disableHotbarScrolling", DEFAULT_DISABLE_HOTBAR_SCROLLING);
    }

    static void saveMouseSettings(Properties p) {
        if (p != null) {
            p.setProperty("mouse.disableHotbarScrolling", Boolean.toString(disableHotbarScrolling));
        }
    }

    static void loadParticleSettings(Properties settings) {
        Properties p = settings == null ? new Properties() : settings;
        // A truly new config receives the local visual QoL default. A populated legacy file with no key keeps
        // vanilla particles instead of silently changing an established player's appearance.
        boolean fallback = p.isEmpty() ? DEFAULT_NO_BLOCK_PARTICLES : false;
        noBlockParticles = readBool(p, "particles.noBlockParticles", fallback);
    }

    static void saveParticleSettings(Properties p) {
        if (p != null) {
            p.setProperty("particles.noBlockParticles", Boolean.toString(noBlockParticles));
        }
    }

    /**
     * Records a schema upgrade after values load, but never rewrites a saved choice. Fresh-install defaults are
     * applied only while a configuration is empty; an older player file is normalized and marked, not promoted.
     */
    static boolean applyConfigurationVersionUpgrade(Properties settings) {
        Properties p = settings == null ? new Properties() : settings;
        int version = readInt(p, CONFIG_VERSION_KEY, 0, 0, CURRENT_CONFIG_VERSION);
        configVersionMigrationPending = version < CURRENT_CONFIG_VERSION;
        return configVersionMigrationPending;
    }

    /** Writes the schema marker used by the non-destructive fresh-default audit. */
    static void saveConfigurationVersion(Properties p) {
        if (p != null) {
            p.setProperty(CONFIG_VERSION_KEY, Integer.toString(CURRENT_CONFIG_VERSION));
        }
    }

    /**
     * Loads only the Item Animation portion of a properties file. Package-visible for the compatibility suite;
     * the game calls this through {@link #load()}. Unknown historic keys are intentionally harmless.
     */
    static void loadItemAnimationSettings(Properties settings) {
        Properties p = settings == null ? new Properties() : settings;
        itemAnimationMigrationPending = hasRetiredItemAnimationBackgroundKey(p) || hasRetiredItemAnimationBlurKey(p);
        // New category names win when both names exist; old firstPerson* keys are read once and then rewritten.
        // Beta-0.10.8v's itemAnimation.autoFixHitSlash key is intentionally ignored: the corrected visual timeline
        // is permanent and no replacement setting is retained.
        itemAnimationEnabled = readItemAnimationBool(p, "itemAnimation.enabled", true, "firstPersonItemAnimation");
        // The dotted prior-release names predate the camel-case aliases; both are read without overriding a modern key.
        itemAnimationSizeOn = readItemAnimationBool(p, "itemAnimation.sizeOn", itemAnimationSizeOn,
                "firstPerson.itemScaleOn", "firstPersonItemScaleOn");
        itemAnimationSpeedOn = readItemAnimationBool(p, "itemAnimation.speedOn", itemAnimationSpeedOn,
                "firstPerson.animationSpeedOn", "firstPersonAnimSpeedOn");
        itemAnimationRemoveHasteEffect = readBool(p, "itemAnimation.removeHasteEffect",
                itemAnimationRemoveHasteEffect);
        itemAnimationHitSlashStyle = readInt(p, "itemAnimation.hitSlashStyle", itemAnimationHitSlashStyle, 0,
                ITEM_ANIMATION_HIT_SLASH_STYLES.length - 1);
        itemAnimationSmoothSwing = readBool(p, "itemAnimation.smoothSwing", DEFAULT_ITEM_ANIMATION_SMOOTH_SWING);
        itemAnimationSmoothness = readInt(p, "itemAnimation.smoothness", DEFAULT_ITEM_ANIMATION_SMOOTHNESS,
                ITEM_ANIMATION_SMOOTHNESS_MIN_PERCENT, ITEM_ANIMATION_SMOOTHNESS_MAX_PERCENT);
        itemAnimationSize = readItemAnimationInt(p, "itemAnimation.size", itemAnimationSize,
                ITEM_ANIMATION_SIZE_MIN_PERCENT, ITEM_ANIMATION_SIZE_MAX_PERCENT,
                "firstPerson.itemScale", "firstPersonItemScale");
        itemAnimationSpeed = readItemAnimationInt(p, "itemAnimation.speed", itemAnimationSpeed,
                ITEM_ANIMATION_SPEED_MIN_PERCENT, ITEM_ANIMATION_SPEED_MAX_PERCENT,
                "firstPerson.animationSpeed", "firstPersonAnimSpeed");
        itemAnimationX = readItemAnimationInt(p, "itemAnimation.x", itemAnimationX,
                ITEM_ANIMATION_OFFSET_MIN, ITEM_ANIMATION_OFFSET_MAX, "firstPersonItemX");
        itemAnimationY = readItemAnimationInt(p, "itemAnimation.y", itemAnimationY,
                ITEM_ANIMATION_OFFSET_MIN, ITEM_ANIMATION_OFFSET_MAX, "firstPersonItemY");
        itemAnimationZ = readItemAnimationInt(p, "itemAnimation.z", itemAnimationZ,
                ITEM_ANIMATION_OFFSET_MIN, ITEM_ANIMATION_OFFSET_MAX, "firstPersonItemZ");
    }

    /** Writes exactly the supported Item Animation keys; obsolete keys are never carried forward. */
    static void saveItemAnimationSettings(Properties p) {
        if (p == null) {
            return;
        }
        removeRetiredItemAnimationKeys(p);
        p.setProperty("itemAnimation.enabled", Boolean.toString(itemAnimationEnabled));
        p.setProperty("itemAnimation.sizeOn", Boolean.toString(itemAnimationSizeOn));
        p.setProperty("itemAnimation.speedOn", Boolean.toString(itemAnimationSpeedOn));
        p.setProperty("itemAnimation.removeHasteEffect", Boolean.toString(itemAnimationRemoveHasteEffect));
        p.setProperty("itemAnimation.hitSlashStyle", Integer.toString(itemAnimationHitSlashStyle));
        p.setProperty("itemAnimation.smoothSwing", Boolean.toString(itemAnimationSmoothSwing));
        p.setProperty("itemAnimation.smoothness", Integer.toString(itemAnimationSmoothness));
        p.setProperty("itemAnimation.size", Integer.toString(itemAnimationSize));
        p.setProperty("itemAnimation.speed", Integer.toString(itemAnimationSpeed));
        p.setProperty("itemAnimation.x", Integer.toString(itemAnimationX));
        p.setProperty("itemAnimation.y", Integer.toString(itemAnimationY));
        p.setProperty("itemAnimation.z", Integer.toString(itemAnimationZ));
    }

    /**
     * Loads fishing toggles while making the new proximity alert safe for existing configurations: no key means off.
     * Package-visible so the headless compatibility suite can prove an old properties file remains harmless.
     */
    static void loadFishingSettings(Properties settings) {
        Properties p = settings == null ? new Properties() : settings;
        fishingCounter = readBool(p, "fishing.counter", fishingCounter);
        fishingFestival = readBool(p, "fishing.festival", fishingFestival);
        fishingJawbusAlert = readBool(p, "fishing.jawbusAlert", fishingJawbusAlert);
        fishingNearbyEncounterAlert = readBool(p, "fishing.nearbyEncounterAlert",
                DEFAULT_FISHING_NEARBY_ENCOUNTER_ALERT);
    }

    /** Writes all supported fishing keys without modifying any HUD element placement or style keys. */
    static void saveFishingSettings(Properties p) {
        if (p == null) {
            return;
        }
        p.setProperty("fishing.counter", Boolean.toString(fishingCounter));
        p.setProperty("fishing.festival", Boolean.toString(fishingFestival));
        p.setProperty("fishing.jawbusAlert", Boolean.toString(fishingJawbusAlert));
        p.setProperty("fishing.nearbyEncounterAlert", Boolean.toString(fishingNearbyEncounterAlert));
    }

    /**
     * Reads retained friend-source feature keys. Newer isolated owners load first; this method only maps a historic
     * value into such an owner when that owner's current key is absent, so a deliberate modern choice always wins.
     */
    static void loadFriendFeatureSettings(Properties settings) {
        Properties p = settings == null ? new Properties() : settings;

        // Current Item Animation keys win; dotted prior-release keys are a read-only compatibility input.
        if (!p.containsKey("itemAnimation.sizeOn") && !p.containsKey("firstPersonItemScaleOn")) {
            if (p.containsKey("firstPerson.itemScaleOn")) itemAnimationMigrationPending = true;
            itemAnimationSizeOn = readBool(p, "firstPerson.itemScaleOn", itemAnimationSizeOn);
        }
        if (!p.containsKey("itemAnimation.speedOn") && !p.containsKey("firstPersonAnimSpeedOn")) {
            if (p.containsKey("firstPerson.animationSpeedOn")) itemAnimationMigrationPending = true;
            itemAnimationSpeedOn = readBool(p, "firstPerson.animationSpeedOn", itemAnimationSpeedOn);
        }
        if (!p.containsKey("itemAnimation.size") && !p.containsKey("firstPersonItemScale")) {
            if (p.containsKey("firstPerson.itemScale")) itemAnimationMigrationPending = true;
            itemAnimationSize = readInt(p, "firstPerson.itemScale", itemAnimationSize,
                    ITEM_ANIMATION_SIZE_MIN_PERCENT, ITEM_ANIMATION_SIZE_MAX_PERCENT);
        }
        if (!p.containsKey("itemAnimation.speed") && !p.containsKey("firstPersonAnimSpeed")) {
            if (p.containsKey("firstPerson.animationSpeed")) itemAnimationMigrationPending = true;
            itemAnimationSpeed = readInt(p, "firstPerson.animationSpeed", itemAnimationSpeed,
                    ITEM_ANIMATION_SPEED_MIN_PERCENT, ITEM_ANIMATION_SPEED_MAX_PERCENT);
        }
        firstPersonItemScaleOn = itemAnimationSizeOn;
        firstPersonItemScale = Math.max(FIRST_PERSON_MIN, Math.min(FIRST_PERSON_MAX, itemAnimationSize));
        firstPersonAnimSpeedOn = itemAnimationSpeedOn;
        firstPersonAnimSpeed = Math.max(FIRST_PERSON_MIN, Math.min(FIRST_PERSON_MAX, itemAnimationSpeed));

        mobHighlights = readBool(p, "mobHighlights", mobHighlights);
        boolean legacyStarredEnabled = readBool(p, "starredMobHighlights", starredMobHighlights);
        starredMobDisplayMode = readInt(p, "dungeon.starredMobDisplayMode",
                legacyStarredEnabled ? 0 : 2, 0, 2);
        starredMobHighlights = starredMobDisplayMode != 2;
        starredMobEsp = readBool(p, "dungeon.starredMobEsp", starredMobEsp);
        starredMobColor = readColor(p.getProperty("dungeon.starredMobColor"), DEFAULT_STARRED_MOB_COLOR);
        starredMobStormyColor = readColor(p.getProperty("dungeon.starredMobStormyColor"), DEFAULT_STARRED_MOB_STORMY_COLOR);
        starredMobWithermancerColor = readColor(p.getProperty("dungeon.starredMobWithermancerColor"),
                DEFAULT_STARRED_MOB_WITHERMANCER_COLOR);
        starredMobZombieCommanderColor = readColor(p.getProperty("dungeon.starredMobZombieCommanderColor"),
                DEFAULT_STARRED_MOB_ZOMBIE_COMMANDER_COLOR);
        starredMobSkeletonMasterColor = readColor(p.getProperty("dungeon.starredMobSkeletonMasterColor"),
                DEFAULT_STARRED_MOB_SKELETON_MASTER_COLOR);
        felEsp = readBool(p, "dungeon.felEsp", felEsp);
        felEspColor = readColor(p.getProperty("dungeon.felEspColor"), felEspColor);
        bloodMobHighlight = readBool(p, "dungeon.bloodMobHighlight", bloodMobHighlight);
        bloodMobHighlightColor = readColor(p.getProperty("dungeon.bloodMobHighlightColor"), bloodMobHighlightColor);

        removeIrritatingMessagesEnabled = readBool(p, "qol.removeIrritatingMessages", removeIrritatingMessagesEnabled);
        muteDungeonBossCrowdMessages = readBool(p, "dungeon.muteBossCrowdMessages", muteDungeonBossCrowdMessages);
        dungeonAutoCloseSecretChests = readBool(p, "dungeon.autoCloseSecretChests", dungeonAutoCloseSecretChests);
        if (!p.containsKey("dungeonHud.autoChestClose") && p.containsKey("dungeon.autoCloseSecretChests")) {
            dungeonAutoChestClose = dungeonAutoCloseSecretChests;
        }
        dungeonWitherDoorsHighlight = readBool(p, "dungeon.witherDoorsHighlight", dungeonWitherDoorsHighlight);
        dungeonHideUnableToLocateSign = readBool(p, "dungeon.hideUnableToLocateSign", dungeonHideUnableToLocateSign);
        dungeonSpiritBowTracer = readBool(p, "dungeon.spiritBowTracer", dungeonSpiritBowTracer);
        dungeonSPlusReminder = readBool(p, "dungeon.sPlusReminder", dungeonSPlusReminder);
        dungeonSNotifier = readBool(p, "dungeon.sNotifier", dungeonSNotifier);
        dungeonSPlusPartyMessage = readBool(p, "dungeon.sPlusPartyMessage", dungeonSPlusPartyMessage);
        dungeonSPlusCustomMessage = text(p, "dungeon.sPlusCustomMessage", dungeonSPlusCustomMessage, 100);
        dungeonScoreSimple = readBool(p, "dungeon.scoreSimple", dungeonScoreSimple);
        dungeonPaulBonus = readBool(p, "dungeon.paulBonus", dungeonPaulBonus);
        dungeonBloodReadyTitle = readBool(p, "dungeon.bloodReadyTitle", dungeonBloodReadyTitle);
        equipmentOverlayEnabled = readBool(p, "qol.equipmentOverlay", equipmentOverlayEnabled);
        showCurrentPet = readBool(p, "qol.showCurrentPet", showCurrentPet);

        itemProtectionEnabled = readBool(p, "itemProtection.enabled", itemProtectionEnabled);
        itemProtectionNotifications = readBool(p, "itemProtection.notifications", itemProtectionNotifications);
        slotLockingEnabled = readBool(p, "slotLocking.enabled", slotLockingEnabled);
        slotBindingEnabled = readBool(p, "slotLocking.binding", slotBindingEnabled);
        slotLockNotifications = readBool(p, "slotLocking.notifications", slotLockNotifications);
        inventorySearchEnabled = readBool(p, "qol.inventorySearch", inventorySearchEnabled);
        hideInventoryPotionEffects = readBool(p, "qol.hideInventoryPotionEffects", hideInventoryPotionEffects);
        autoSprintEnabled = readBool(p, "movement.autoSprint", autoSprintEnabled);
        romanNumeralsInTooltips = readBool(p, "qol.romanNumerals", romanNumeralsInTooltips);
        scrollableTooltipsEnabled = readBool(p, "qol.scrollableTooltips", scrollableTooltipsEnabled);
        chatDisableWatchdogInfo = readBool(p, "qol.chatDisableWatchdogInfo", chatDisableWatchdogInfo);
        chatDisableFriendJoin = readBool(p, "qol.chatDisableFriendJoin", chatDisableFriendJoin);
        chatDisableZombieRareDrops = readBool(p, "qol.chatDisableZombieRareDrops", chatDisableZombieRareDrops);
        chatDisableSellingRanks = readBool(p, "qol.chatDisableSellingRanks", chatDisableSellingRanks);
        chatDisableServerAnnouncements = readBool(p, "qol.chatDisableServerAnnouncements", chatDisableServerAnnouncements);
        chatDisableGameInvites = readBool(p, "qol.chatDisableGameInvites", chatDisableGameInvites);
        itemRarityBordersEnabled = readBool(p, "qol.itemRarityBorders", itemRarityBordersEnabled);
        chatLengthEnabled = readBool(p, "chat.lengthEnabled", chatLengthEnabled);
        chatCopyEnabled = readBool(p, "qol.chatCopy", chatCopyEnabled);
        chatFilterMode = readChatTabMode(p);
        chatMessageAnimation = readBool(p, "chat.animation.messages", chatMessageAnimation);
        chatMessageSpeed = readInt(p, "chat.animation.messageSpeed", chatMessageSpeed, 0, 100);
        chatBackgroundAnimation = readBool(p, "chat.animation.background", chatBackgroundAnimation);
        chatBackgroundDuration = readInt(p, "chat.animation.backgroundDuration", chatBackgroundDuration, 50, 1000);
        chatScrollAnimation = readBool(p, "chat.animation.scrolling", chatScrollAnimation);
        chatScrollDuration = readInt(p, "chat.animation.scrollDuration", chatScrollDuration, 0, 1000);
        chatRemoveScrollBar = readBool(p, "chat.animation.removeScrollBar", chatRemoveScrollBar);
        noHurtCamEnabled = readBool(p, "qol.noHurtCam", noHurtCamEnabled);
        if (!p.containsKey(AssistSettings.PREFIX + "visual.hurtCameraShake")
                && !p.containsKey("noHurtCamEnabled") && p.containsKey("qol.noHurtCam")) {
            AssistSettings.hurtCameraShake = noHurtCamEnabled ? 0 : 100;
        } else {
            noHurtCamEnabled = AssistSettings.hurtCameraShake == 0;
        }
        hideBlockBreakingParticles = readBool(p, "qol.hideBlockBreakingParticles", hideBlockBreakingParticles);
        if (!p.containsKey("particles.noBlockParticles") && p.containsKey("qol.hideBlockBreakingParticles")) {
            noBlockParticles = hideBlockBreakingParticles;
        } else {
            hideBlockBreakingParticles = noBlockParticles;
        }
        hideDeadMobs = readBool(p, "qol.hideDeadMobs", hideDeadMobs);
        if (!p.containsKey(AssistSettings.PREFIX + "comfort.hideDeadMobs") && p.containsKey("qol.hideDeadMobs")) {
            AssistSettings.hideDeadMobs = hideDeadMobs;
        } else {
            hideDeadMobs = AssistSettings.hideDeadMobs;
        }
        hideFlamingFists = readBool(p, "qol.hideFlamingFists", hideFlamingFists);
        wardrobeShortcutEnabled = readBool(p, "qol.wardrobeShortcut", wardrobeShortcutEnabled);
        petsShortcutEnabled = readBool(p, "qol.petsShortcut", petsShortcutEnabled);
        miningAbilityNotifier = readBool(p, "mining.abilityNotifier", miningAbilityNotifier);
        miningPuzzlerSolver = readBool(p, "mining.puzzlerSolver", miningPuzzlerSolver);

        solverThreeWeirdos = readBool(p, "dungeon.solver.threeWeirdos", solverThreeWeirdos);
        solverCreeper = readBool(p, "dungeon.solver.creeper", solverCreeper);
        solverBoulder = readBool(p, "dungeon.solver.boulder", solverBoulder);
        solverSilverfish = readBool(p, "dungeon.solver.silverfish", solverSilverfish);
        solverWater = readBool(p, "dungeon.solver.water", solverWater);
        solverTeleportMaze = readBool(p, "dungeon.solver.teleportMaze", solverTeleportMaze);
        terminalTracker = readBool(p, "dungeon.terminal.tracker", terminalTracker);
        terminalTrackerScale = readFloat(p, "dungeon.terminal.trackerScale", terminalTrackerScale, 0.5F, 3.0F);
        terminalTrackerColor = readColor(p.getProperty("dungeon.terminal.trackerColor"), terminalTrackerColor);
        terminalTrackerX = readInt(p, "dungeon.terminal.trackerX", terminalTrackerX, -1000, 4000);
        terminalTrackerY = readInt(p, "dungeon.terminal.trackerY", terminalTrackerY, -1000, 4000);
        terminalCustomClickIn = readBool(p, "dungeon.terminal.customClickIn", terminalCustomClickIn);
        terminalCustomColors = readBool(p, "dungeon.terminal.customColors", terminalCustomColors);
        terminalCustomMaze = readBool(p, "dungeon.terminal.customMaze", terminalCustomMaze);
        terminalCustomStartsWith = readBool(p, "dungeon.terminal.customStartsWith", terminalCustomStartsWith);
        terminalCustomPanes = readBool(p, "dungeon.terminal.customPanes", terminalCustomPanes);
        terminalGuiScale = readFloat(p, "dungeon.terminal.guiScale", terminalGuiScale, 1.0F, 5.0F);
        terminalStartsWithSolver = readBool(p, "dungeon.terminal.startsWith", terminalStartsWithSolver);
        terminalColorsSolver = readBool(p, "dungeon.terminal.colors", terminalColorsSolver);
        terminalClickInOrderSolver = readBool(p, "dungeon.terminal.clickInOrder", terminalClickInOrderSolver);
        terminalMazeSolver = readBool(p, "dungeon.terminal.maze", terminalMazeSolver);
        terminalCorrectPanesSolver = readBool(p, "dungeon.terminal.correctPanes", terminalCorrectPanesSolver);
        terminalHideIncorrect = readBool(p, "dungeon.terminal.hideIncorrect", terminalHideIncorrect);
        terminalPreventMissclicks = readBool(p, "dungeon.terminal.preventMissclicks", terminalPreventMissclicks);
        terminalHideTooltips = readBool(p, "dungeon.terminal.hideTooltips", terminalHideTooltips);
        solverFirstDevice = readBool(p, "dungeon.device.first", solverFirstDevice);
        solverThirdDevice = readBool(p, "dungeon.device.third", solverThirdDevice);
        solverCorrectColor = readColor(p.getProperty("dungeon.solver.correctColor"), solverCorrectColor);
        solverAlternativeColor = readColor(p.getProperty("dungeon.solver.alternativeColor"), solverAlternativeColor);
        terminalWaypoints = readBool(p, "dungeon.terminal.waypoints", terminalWaypoints);
    }

    /** Writes retained friend-only settings and compatibility inputs without replacing modern feature owner keys. */
    static void saveFriendFeatureSettings(Properties p) {
        if (p == null) return;
        starredMobDisplayMode = Math.max(0, Math.min(2, starredMobDisplayMode));
        starredMobHighlights = starredMobDisplayMode != 2;
        terminalTrackerScale = clampFloat(terminalTrackerScale, 0.5F, 3.0F);
        terminalGuiScale = clampFloat(terminalGuiScale, 1.0F, 5.0F);
        terminalTrackerX = Math.max(-1000, Math.min(4000, terminalTrackerX));
        terminalTrackerY = Math.max(-1000, Math.min(4000, terminalTrackerY));
        dungeonSPlusCustomMessage = text(dungeonSPlusCustomMessage, "", 100);
        // Current owners are canonical on the three intentional overlap paths.
        dungeonAutoCloseSecretChests = dungeonAutoChestClose;
        hideBlockBreakingParticles = noBlockParticles;
        hideDeadMobs = AssistSettings.hideDeadMobs;
        noHurtCamEnabled = AssistSettings.hurtCameraShake == 0;

        p.setProperty("mobHighlights", Boolean.toString(mobHighlights));
        p.setProperty("starredMobHighlights", Boolean.toString(starredMobHighlights));
        p.setProperty("dungeon.starredMobDisplayMode", Integer.toString(starredMobDisplayMode));
        p.setProperty("dungeon.starredMobEsp", Boolean.toString(starredMobEsp));
        p.setProperty("dungeon.starredMobColor", starredMobColor);
        p.setProperty("dungeon.starredMobStormyColor", starredMobStormyColor);
        p.setProperty("dungeon.starredMobWithermancerColor", starredMobWithermancerColor);
        p.setProperty("dungeon.starredMobZombieCommanderColor", starredMobZombieCommanderColor);
        p.setProperty("dungeon.starredMobSkeletonMasterColor", starredMobSkeletonMasterColor);
        p.setProperty("dungeon.felEsp", Boolean.toString(felEsp));
        p.setProperty("dungeon.felEspColor", felEspColor);
        p.setProperty("dungeon.bloodMobHighlight", Boolean.toString(bloodMobHighlight));
        p.setProperty("dungeon.bloodMobHighlightColor", bloodMobHighlightColor);
        p.setProperty("qol.removeIrritatingMessages", Boolean.toString(removeIrritatingMessagesEnabled));
        p.setProperty("dungeon.muteBossCrowdMessages", Boolean.toString(muteDungeonBossCrowdMessages));
        p.setProperty("dungeon.witherDoorsHighlight", Boolean.toString(dungeonWitherDoorsHighlight));
        p.setProperty("dungeon.hideUnableToLocateSign", Boolean.toString(dungeonHideUnableToLocateSign));
        p.setProperty("dungeon.spiritBowTracer", Boolean.toString(dungeonSpiritBowTracer));
        p.setProperty("dungeon.sPlusReminder", Boolean.toString(dungeonSPlusReminder));
        p.setProperty("dungeon.sNotifier", Boolean.toString(dungeonSNotifier));
        p.setProperty("dungeon.sPlusPartyMessage", Boolean.toString(dungeonSPlusPartyMessage));
        p.setProperty("dungeon.sPlusCustomMessage", dungeonSPlusCustomMessage);
        p.setProperty("dungeon.scoreSimple", Boolean.toString(dungeonScoreSimple));
        p.setProperty("dungeon.paulBonus", Boolean.toString(dungeonPaulBonus));
        p.setProperty("dungeon.bloodReadyTitle", Boolean.toString(dungeonBloodReadyTitle));
        p.setProperty("qol.equipmentOverlay", Boolean.toString(equipmentOverlayEnabled));
        p.setProperty("qol.showCurrentPet", Boolean.toString(showCurrentPet));
        p.setProperty("itemProtection.enabled", Boolean.toString(itemProtectionEnabled));
        p.setProperty("itemProtection.notifications", Boolean.toString(itemProtectionNotifications));
        p.setProperty("slotLocking.enabled", Boolean.toString(slotLockingEnabled));
        p.setProperty("slotLocking.binding", Boolean.toString(slotBindingEnabled));
        p.setProperty("slotLocking.notifications", Boolean.toString(slotLockNotifications));
        p.setProperty("qol.inventorySearch", Boolean.toString(inventorySearchEnabled));
        p.setProperty("qol.hideInventoryPotionEffects", Boolean.toString(hideInventoryPotionEffects));
        p.setProperty("movement.autoSprint", Boolean.toString(autoSprintEnabled));
        p.setProperty("qol.romanNumerals", Boolean.toString(romanNumeralsInTooltips));
        p.setProperty("qol.scrollableTooltips", Boolean.toString(scrollableTooltipsEnabled));
        p.setProperty("qol.chatDisableWatchdogInfo", Boolean.toString(chatDisableWatchdogInfo));
        p.setProperty("qol.chatDisableFriendJoin", Boolean.toString(chatDisableFriendJoin));
        p.setProperty("qol.chatDisableZombieRareDrops", Boolean.toString(chatDisableZombieRareDrops));
        p.setProperty("qol.chatDisableSellingRanks", Boolean.toString(chatDisableSellingRanks));
        p.setProperty("qol.chatDisableServerAnnouncements", Boolean.toString(chatDisableServerAnnouncements));
        p.setProperty("qol.chatDisableGameInvites", Boolean.toString(chatDisableGameInvites));
        p.setProperty("qol.itemRarityBorders", Boolean.toString(itemRarityBordersEnabled));
        p.setProperty("chat.lengthEnabled", Boolean.toString(chatLengthEnabled));
        p.setProperty("qol.chatCopy", Boolean.toString(chatCopyEnabled));
        p.setProperty("chat.tabMode", Integer.toString(Math.max(0, Math.min(4, chatFilterMode))));
        p.setProperty("chat.animation.messages", Boolean.toString(chatMessageAnimation));
        p.setProperty("chat.animation.messageSpeed", Integer.toString(Math.max(0, Math.min(100, chatMessageSpeed))));
        p.setProperty("chat.animation.background", Boolean.toString(chatBackgroundAnimation));
        p.setProperty("chat.animation.backgroundDuration", Integer.toString(Math.max(50, Math.min(1000, chatBackgroundDuration))));
        p.setProperty("chat.animation.scrolling", Boolean.toString(chatScrollAnimation));
        p.setProperty("chat.animation.scrollDuration", Integer.toString(Math.max(0, Math.min(1000, chatScrollDuration))));
        p.setProperty("chat.animation.removeScrollBar", Boolean.toString(chatRemoveScrollBar));
        p.setProperty("qol.hideFlamingFists", Boolean.toString(hideFlamingFists));
        p.setProperty("qol.wardrobeShortcut", Boolean.toString(wardrobeShortcutEnabled));
        p.setProperty("qol.petsShortcut", Boolean.toString(petsShortcutEnabled));
        p.setProperty("mining.abilityNotifier", Boolean.toString(miningAbilityNotifier));
        p.setProperty("mining.puzzlerSolver", Boolean.toString(miningPuzzlerSolver));
        p.setProperty("dungeon.solver.threeWeirdos", Boolean.toString(solverThreeWeirdos));
        p.setProperty("dungeon.solver.creeper", Boolean.toString(solverCreeper));
        p.setProperty("dungeon.solver.boulder", Boolean.toString(solverBoulder));
        p.setProperty("dungeon.solver.silverfish", Boolean.toString(solverSilverfish));
        p.setProperty("dungeon.solver.water", Boolean.toString(solverWater));
        p.setProperty("dungeon.solver.teleportMaze", Boolean.toString(solverTeleportMaze));
        p.setProperty("dungeon.terminal.tracker", Boolean.toString(terminalTracker));
        p.setProperty("dungeon.terminal.trackerScale", Float.toString(terminalTrackerScale));
        p.setProperty("dungeon.terminal.trackerColor", terminalTrackerColor);
        p.setProperty("dungeon.terminal.trackerX", Integer.toString(terminalTrackerX));
        p.setProperty("dungeon.terminal.trackerY", Integer.toString(terminalTrackerY));
        p.setProperty("dungeon.terminal.customClickIn", Boolean.toString(terminalCustomClickIn));
        p.setProperty("dungeon.terminal.customColors", Boolean.toString(terminalCustomColors));
        p.setProperty("dungeon.terminal.customMaze", Boolean.toString(terminalCustomMaze));
        p.setProperty("dungeon.terminal.customStartsWith", Boolean.toString(terminalCustomStartsWith));
        p.setProperty("dungeon.terminal.customPanes", Boolean.toString(terminalCustomPanes));
        p.setProperty("dungeon.terminal.guiScale", Float.toString(terminalGuiScale));
        p.setProperty("dungeon.terminal.startsWith", Boolean.toString(terminalStartsWithSolver));
        p.setProperty("dungeon.terminal.colors", Boolean.toString(terminalColorsSolver));
        p.setProperty("dungeon.terminal.clickInOrder", Boolean.toString(terminalClickInOrderSolver));
        p.setProperty("dungeon.terminal.maze", Boolean.toString(terminalMazeSolver));
        p.setProperty("dungeon.terminal.correctPanes", Boolean.toString(terminalCorrectPanesSolver));
        p.setProperty("dungeon.terminal.hideIncorrect", Boolean.toString(terminalHideIncorrect));
        p.setProperty("dungeon.terminal.preventMissclicks", Boolean.toString(terminalPreventMissclicks));
        p.setProperty("dungeon.terminal.hideTooltips", Boolean.toString(terminalHideTooltips));
        p.setProperty("dungeon.device.first", Boolean.toString(solverFirstDevice));
        p.setProperty("dungeon.device.third", Boolean.toString(solverThirdDevice));
        p.setProperty("dungeon.solver.correctColor", solverCorrectColor);
        p.setProperty("dungeon.solver.alternativeColor", solverAlternativeColor);
        p.setProperty("dungeon.terminal.waypoints", Boolean.toString(terminalWaypoints));
    }

    /** Current Item Animation values are the only source for the old visual bridge. */
    public static int effectiveFirstPersonItemScale() {
        return firstPersonItemScaleOn ? Math.max(FIRST_PERSON_MIN, Math.min(FIRST_PERSON_MAX, firstPersonItemScale))
                : DEFAULT_FIRST_PERSON_ITEM_SCALE;
    }

    /** Current Item Animation values are the only source for the old visual bridge. */
    public static int effectiveFirstPersonAnimSpeed() {
        return firstPersonAnimSpeedOn ? Math.max(FIRST_PERSON_MIN, Math.min(FIRST_PERSON_MAX, firstPersonAnimSpeed))
                : DEFAULT_FIRST_PERSON_ANIM_SPEED;
    }

    public static void setStarredMobDisplayMode(int mode) {
        starredMobDisplayMode = Math.max(0, Math.min(2, mode));
        starredMobHighlights = starredMobDisplayMode != 2;
        save();
    }

    static int readChatTabMode(Properties p) {
        if (p != null && p.containsKey("chat.tabMode")) return readInt(p, "chat.tabMode", 0, 0, 4);
        int legacy = readInt(p == null ? new Properties() : p, "chat.filterMode", 0, 0, 2);
        return legacy == 2 ? 4 : legacy;
    }

    private static float readFloat(Properties p, String key, float def, float min, float max) {
        try {
            float value = Float.parseFloat(p.getProperty(key, Float.toString(def)).trim());
            return Float.isNaN(value) || Float.isInfinite(value) ? def : clampFloat(value, min, max);
        } catch (RuntimeException ignored) {
            return def;
        }
    }

    private static float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /** Validates the saved chroma-speed:alpha:red:green:blue form before a render hook consumes it. */
    private static String readColor(String value, String def) {
        if (value == null) return def;
        String[] parts = value.trim().split(":");
        if (parts.length != 5) return def;
        try {
            Integer.parseInt(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                int channel = Integer.parseInt(parts[i]);
                if (channel < 0 || channel > 255) return def;
            }
            return value.trim();
        } catch (RuntimeException ignored) {
            return def;
        }
    }

    private static String text(Properties p, String key, String fallback, int maxLength) {
        return text(p == null ? fallback : p.getProperty(key, fallback), fallback, maxLength);
    }

    private static String text(String value, String fallback, int maxLength) {
        String safe = value == null ? (fallback == null ? "" : fallback) : value;
        return safe.length() > maxLength ? safe.substring(0, maxLength) : safe;
    }

    /**
     * Delegates native Chat QoL loading and legacy migration to its Minecraft-free settings model. This preserves
     * the existing public hook used by the compatibility suite while retiring the external animation bridge keys.
     */
    static void loadChatSettings(Properties settings) {
        ChatQolSettings.load(settings);
    }

    /** Writes only native chat.qol.* keys; a save intentionally drops retired animation-bridge keys. */
    static void saveChatSettings(Properties p) {
        ChatQolSettings.save(p);
    }

    public static void save() {
        // Keep programmatic edits as safe as slider/loader edits before the atomic writer persists them.
        itemAnimationSize = Math.max(ITEM_ANIMATION_SIZE_MIN_PERCENT,
                Math.min(ITEM_ANIMATION_SIZE_MAX_PERCENT, itemAnimationSize));
        itemAnimationSpeed = Math.max(ITEM_ANIMATION_SPEED_MIN_PERCENT,
                Math.min(ITEM_ANIMATION_SPEED_MAX_PERCENT, itemAnimationSpeed));
        itemAnimationX = Math.max(ITEM_ANIMATION_OFFSET_MIN,
                Math.min(ITEM_ANIMATION_OFFSET_MAX, itemAnimationX));
        itemAnimationY = Math.max(ITEM_ANIMATION_OFFSET_MIN,
                Math.min(ITEM_ANIMATION_OFFSET_MAX, itemAnimationY));
        itemAnimationZ = Math.max(ITEM_ANIMATION_OFFSET_MIN,
                Math.min(ITEM_ANIMATION_OFFSET_MAX, itemAnimationZ));
        itemAnimationHitSlashStyle = Math.max(0, Math.min(ITEM_ANIMATION_HIT_SLASH_STYLES.length - 1,
                itemAnimationHitSlashStyle));
        itemAnimationSmoothness = Math.max(ITEM_ANIMATION_SMOOTHNESS_MIN_PERCENT,
                Math.min(ITEM_ANIMATION_SMOOTHNESS_MAX_PERCENT, itemAnimationSmoothness));
        dungeonMapBackgroundOpacity = Math.max(DUNGEON_MAP_BACKGROUND_OPACITY_MIN,
                Math.min(DUNGEON_MAP_BACKGROUND_OPACITY_MAX, dungeonMapBackgroundOpacity));
        dungeonMapBackgroundColor = Math.max(0, Math.min(DUNGEON_MAP_BACKGROUND_COLORS.length - 1,
                dungeonMapBackgroundColor));
        dungeonMapRoomNameColor = Math.max(0, Math.min(DUNGEON_MAP_LABEL_COLORS.length - 1,
                dungeonMapRoomNameColor));
        dungeonMapSecretsColor = Math.max(0, Math.min(DUNGEON_MAP_LABEL_COLORS.length - 1,
                dungeonMapSecretsColor));
        Properties p = new Properties();
        saveConfigurationVersion(p);
        saveSettingsTheme(p);
        saveSettingsWindowLayout(p);
        saveAppearanceSettings(p);
        p.setProperty("adaptive", Boolean.toString(adaptive));
        p.setProperty("scrollSpeed", Integer.toString(scrollSpeed));
        p.setProperty("smoothScrolling", Boolean.toString(smoothScrolling));
        saveMouseSettings(p);
        saveParticleSettings(p);

        // Item Animation. Save only the current supported category; old and obsolete keys are deliberately retired.
        saveItemAnimationSettings(p);
        ItemLockSwapSettings.save(p);
        AssistSettings.save(p);

        p.setProperty("leapMenu", Boolean.toString(leapMenu));
        DungeonEspSettings.save(p);
        p.setProperty("dungeonMap", Boolean.toString(dungeonMap));
        p.setProperty("dungeonMapRotate", Boolean.toString(dungeonMapRotate));
        p.setProperty("dungeonMapShowPlayerNames", Boolean.toString(dungeonMapShowPlayerNames));
        p.setProperty("dungeonMapPlayerHeads", Boolean.toString(dungeonMapPlayerHeads));
        p.setProperty("dungeonMapPlayerHeadSize", Integer.toString(dungeonMapPlayerHeadSize));
        p.setProperty("dungeonMapShowPuzzleName", Boolean.toString(dungeonMapShowPuzzleName));
        p.setProperty("dungeonMapShowSecrets", Boolean.toString(dungeonMapShowSecrets));
        p.setProperty("dungeonMapBackground", Boolean.toString(dungeonMapBackground));
        p.setProperty("dungeonMapBackgroundOpacity", Integer.toString(dungeonMapBackgroundOpacity));
        p.setProperty("dungeonMapBackgroundColor", Integer.toString(dungeonMapBackgroundColor));
        p.setProperty("dungeonMapRoomNameColor", Integer.toString(dungeonMapRoomNameColor));
        p.setProperty("dungeonMapSecretsColor", Integer.toString(dungeonMapSecretsColor));
        p.setProperty("partyPuzzleAnnounce", Boolean.toString(partyPuzzleAnnounce));
        p.setProperty("dungeonMapScale", Integer.toString(dungeonMapScale));
        p.setProperty("dungeonMapScaleOn", Boolean.toString(dungeonMapScaleOn));
        p.setProperty("dungeonMapZoom", Integer.toString(dungeonMapZoom));
        p.setProperty("dungeonMapXPercent", Integer.toString(dungeonMapXPercent));
        p.setProperty("dungeonMapYPercent", Integer.toString(dungeonMapYPercent));
        if (dungeonMapPlacementInitialized) {
            p.setProperty("dungeonMapAx", dungeonMapPlacement.anchorX.name());
            p.setProperty("dungeonMapOx", Integer.toString(dungeonMapPlacement.offsetX));
            p.setProperty("dungeonMapAy", dungeonMapPlacement.anchorY.name());
            p.setProperty("dungeonMapOy", Integer.toString(dungeonMapPlacement.offsetY));
        }
        saveDungeonHudSettings(p);
        p.setProperty("experimentalPerformance", Boolean.toString(experimentalPerformance));
        p.setProperty("leapKey", Boolean.toString(leapKey));
        p.setProperty("cmd.sv", Boolean.toString(svCommand));
        p.setProperty("slayer.tracker", Boolean.toString(slayerTracker));
        p.setProperty("slayer.bossTimer", Boolean.toString(slayerBossTimer));
        p.setProperty("slayer.pricing", Boolean.toString(slayerPricing));
        p.setProperty("cmd.qn", Boolean.toString(qnCommand));
        saveFishingSettings(p);
        saveChatSettings(p);
        FontSettings.save(p);
        saveFriendFeatureSettings(p);
        M7Settings.save(p);
        // Add only ui.feature.* presentation keys; every gameplay/config key above stays owned by its prior schema.
        FeatureUiPreferences.save(p);
        ConfigFiles.writeAtomic(new File(dir(), FILE_NAME), p, "Lunaria settings");
    }

    /** Scale value in use, matching the regular HUD scale switch. */
    public static int effectiveDungeonMapScale() {
        return dungeonMapScaleOn ? Math.max(DUNGEON_MAP_SCALE_MIN,
                Math.min(DUNGEON_MAP_SCALE_MAX, dungeonMapScale)) : 100;
    }

    /** Content magnification is independent from the on-screen map widget size. */
    public static int effectiveDungeonMapZoom() {
        return Math.max(DUNGEON_MAP_ZOOM_MIN, Math.min(DUNGEON_MAP_ZOOM_MAX, dungeonMapZoom));
    }

    public static float dungeonMapZoomFactor() {
        return effectiveDungeonMapZoom() / 100.0F;
    }

    /** Native map-coordinate size for a skin face, clamped even if a caller changed the raw field directly. */
    public static int effectiveDungeonMapPlayerHeadSize() {
        return Math.max(DUNGEON_MAP_PLAYER_HEAD_SIZE_MIN,
                Math.min(DUNGEON_MAP_PLAYER_HEAD_SIZE_MAX, dungeonMapPlayerHeadSize));
    }

    /** Safe visible alpha for the optional borderless map backing surface. */
    public static int effectiveDungeonMapBackgroundOpacity() {
        return Math.max(DUNGEON_MAP_BACKGROUND_OPACITY_MIN,
                Math.min(DUNGEON_MAP_BACKGROUND_OPACITY_MAX, dungeonMapBackgroundOpacity));
    }

    public static int effectiveDungeonMapBackgroundColor() {
        return DUNGEON_MAP_BACKGROUND_COLORS[Math.max(0, Math.min(DUNGEON_MAP_BACKGROUND_COLORS.length - 1,
                dungeonMapBackgroundColor))];
    }

    /** Theme is intentionally one label-palette choice, rather than a mutable map/render tint. */
    public static int effectiveDungeonMapLabelColor(int choice) {
        int index = Math.max(0, Math.min(DUNGEON_MAP_LABEL_COLORS.length - 1, choice));
        int color = DUNGEON_MAP_LABEL_COLORS[index];
        return color == 0 ? accentColor() : color;
    }

    public static int effectiveDungeonMapRoomNameColor() {
        return effectiveDungeonMapLabelColor(dungeonMapRoomNameColor);
    }

    public static int effectiveDungeonMapSecretsColor() {
        return effectiveDungeonMapLabelColor(dungeonMapSecretsColor);
    }

    /** Shared effective map size in GUI pixels. */
    public static int dungeonMapPixelSize(int screenW, int screenH) {
        float requested = effectiveDungeonMapScale() / 100.0F;
        float fit = Math.min(screenW, screenH) / 128.0F;
        float scale = Math.min(requested, Math.max(0.10F, fit));
        return Math.max(1, Math.round(128.0F * scale));
    }

    /** Resolves legacy percentage coordinates once the viewport dimensions are known. */
    public static void ensureDungeonMapPlacement(int screenW, int screenH, int size) {
        if (dungeonMapPlacementInitialized) return;
        int maxX = Math.max(0, screenW - size);
        int maxY = Math.max(0, screenH - size);
        int x = Math.round(maxX * clampPercent(dungeonMapXPercent) / 100.0F);
        int y = Math.round(maxY * clampPercent(dungeonMapYPercent) / 100.0F);
        dungeonMapPlacement.reset(HudPlacement.Anchor.START, x, HudPlacement.Anchor.START, y);
        dungeonMapPlacementInitialized = true;
    }

    public static int dungeonMapLeft(int screenW, int screenH, int size) {
        ensureDungeonMapPlacement(screenW, screenH, size);
        return dungeonMapPlacement.leftUnclamped(screenW, size);
    }

    public static int dungeonMapTop(int screenW, int screenH, int size) {
        ensureDungeonMapPlacement(screenW, screenH, size);
        return dungeonMapPlacement.topUnclamped(screenH, size);
    }

    public static int setDungeonMapPosition(int screenW, int screenH, int size,
                                            int left, int top, boolean snap, int threshold) {
        ensureDungeonMapPlacement(screenW, screenH, size);
        dungeonMapPlacementInitialized = true;
        return dungeonMapPlacement.setFromAbsoluteUnclamped(screenW, screenH, size, size,
                left, top, snap, threshold);
    }

    public static int setDungeonMapLeft(int screenW, int screenH, int size,
                                        int left, boolean snap, int threshold) {
        ensureDungeonMapPlacement(screenW, screenH, size);
        dungeonMapPlacementInitialized = true;
        return dungeonMapPlacement.setLeftFromAbsoluteUnclamped(screenW, size, left, snap, threshold);
    }

    public static int setDungeonMapTop(int screenW, int screenH, int size,
                                       int top, boolean snap, int threshold) {
        ensureDungeonMapPlacement(screenW, screenH, size);
        dungeonMapPlacementInitialized = true;
        return dungeonMapPlacement.setTopFromAbsoluteUnclamped(screenH, size, top, snap, threshold);
    }

    public static void resetDungeonMapPosition() {
        dungeonMapPlacement.reset(HudPlacement.Anchor.START, 12, HudPlacement.Anchor.START, 12);
        dungeonMapPlacementInitialized = true;
        dungeonMapXPercent = 2;
        dungeonMapYPercent = 2;
    }

    private static int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static HudPlacement.Anchor readAnchor(Properties p, String key, HudPlacement.Anchor fallback) {
        try {
            return HudPlacement.Anchor.valueOf(p.getProperty(key, fallback.name()).trim()
                    .toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static File dir() {
        return Minecraft.getMinecraft().mcDataDir;
    }

    private static File readFile() {
        return ConfigFiles.resolveRead(dir(), FILE_NAME, PRIOR_RELEASE_LEGACY_FILE_NAME, PEKA_LEGACY_FILE_NAME);
    }

    /** Legacy Item Animation background controls never affected the renderer and are now actively rewritten away. */
    private static boolean hasRetiredItemAnimationBackgroundKey(Properties p) {
        if (p == null) return false;
        for (String key : RETIRED_ITEM_ANIMATION_BACKGROUND_KEYS) if (p.containsKey(key)) return true;
        return false;
    }

    private static boolean hasRetiredItemAnimationBlurKey(Properties p) {
        if (p == null) return false;
        for (String key : RETIRED_ITEM_ANIMATION_BLUR_KEYS) if (p.containsKey(key)) return true;
        return false;
    }

    private static void removeRetiredItemAnimationKeys(Properties p) {
        if (p == null) return;
        for (String key : RETIRED_ITEM_ANIMATION_BACKGROUND_KEYS) p.remove(key);
        for (String key : RETIRED_ITEM_ANIMATION_BLUR_KEYS) p.remove(key);
        // Beta-0.10.8's corrected swing is permanent as well; never retain the obsolete toggle on an in-place save.
        p.remove("itemAnimation.autoFixHitSlash");
    }

    /** Reads a modern Item Animation key first, then the ordered historic aliases without changing their values. */
    private static boolean readItemAnimationBool(Properties p, String key, boolean def, String... legacyKeys) {
        if (p.containsKey(key)) return readBool(p, key, def);
        if (legacyKeys != null) {
            for (String legacyKey : legacyKeys) {
                if (legacyKey != null && p.containsKey(legacyKey)) {
                    itemAnimationMigrationPending = true;
                    return readBool(p, legacyKey, def);
                }
            }
        }
        return def;
    }

    /** Reads a modern Item Animation number first, then the ordered historic aliases with the same safe bounds. */
    private static int readItemAnimationInt(Properties p, String key, int def, int min, int max,
                                            String... legacyKeys) {
        if (p.containsKey(key)) return readInt(p, key, def, min, max);
        if (legacyKeys != null) {
            for (String legacyKey : legacyKeys) {
                if (legacyKey != null && p.containsKey(legacyKey)) {
                    itemAnimationMigrationPending = true;
                    return readInt(p, legacyKey, def, min, max);
                }
            }
        }
        return Math.max(min, Math.min(max, def));
    }

    private static int readInt(Properties p, String key, int def, int min, int max) {
        try {
            int v = Integer.parseInt(p.getProperty(key, Integer.toString(def)).trim());
            return Math.max(min, Math.min(max, v));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** Loads the two performance defaults while honoring explicit values saved by the user. */
    static void loadPerformanceSettings(Properties p) {
        adaptive = readBool(p, "adaptive", adaptive);
        experimentalPerformance = readBool(p, "experimentalPerformance", experimentalPerformance);
    }

    static boolean readBool(Properties p, String key, boolean def) {
        String raw = p.getProperty(key);
        if (raw == null) {
            return def;
        }
        raw = raw.trim();
        if ("true".equalsIgnoreCase(raw)) {
            return true;
        }
        if ("false".equalsIgnoreCase(raw)) {
            return false;
        }
        return def;
    }
}
