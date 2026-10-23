package com.arena.pekaclient.config;

import java.util.Locale;
import java.util.Properties;

/**
 * Isolated, safe-by-default schema for the optional client assists. Keeping this schema separate from the older
 * shared AppleSettings fields means these modules cannot repurpose a historic key or move an existing HUD position.
 * Every value is normalized on both load and save.
 */
public final class AssistSettings {

    public static final String PREFIX = "assist.";
    public static final String DEFAULT_SYNC_ENDPOINT = "wss://skyvelor-dungeon-sync.bombadzalge.workers.dev/ws";
    public static final int MIN_TERMINAL_SCALE = 70;
    public static final int MAX_TERMINAL_SCALE = 250;
    public static final int MIN_HURT_CAMERA_SHAKE = 0;
    public static final int MAX_HURT_CAMERA_SHAKE = 100;
    public static final int DEFAULT_HURT_CAMERA_SHAKE = 100;

    public static boolean miningAbilityAlert;
    public static boolean miningPuzzlerMarker;

    public static boolean hidePotionEffects;
    public static boolean romanNumerals;
    public static boolean tooltipScroll;
    public static boolean rarityHighlight;
    /** Whole-number percentage of vanilla's local hurt-camera tilt. Zero removes only that tilt. */
    public static int hurtCameraShake;

    public static boolean autoSprint;
    public static boolean wardrobeShortcut;
    public static boolean petsShortcut;

    public static boolean dungeonSync;
    public static boolean dungeonSyncDebug;
    public static String dungeonSyncEndpoint;

    public static boolean terminalHints;
    public static boolean terminalAutoClick;
    public static boolean terminalHideIncorrect;
    public static boolean terminalPreventWrongClicks;
    public static boolean terminalWaypoints;
    public static int terminalScale;

    private AssistSettings() {
    }

    public static void resetDefaults() {
        // Gameplay, network and input assists deliberately start opt-in. The hurt camera remains vanilla at 100%.
        miningAbilityAlert = false;
        miningPuzzlerMarker = false;
        hidePotionEffects = false;
        romanNumerals = false;
        tooltipScroll = false;
        rarityHighlight = false;
        hurtCameraShake = DEFAULT_HURT_CAMERA_SHAKE;
        autoSprint = false;
        wardrobeShortcut = false;
        petsShortcut = false;
        dungeonSync = false;
        dungeonSyncDebug = false;
        dungeonSyncEndpoint = DEFAULT_SYNC_ENDPOINT;
        terminalHints = false;
        terminalAutoClick = false;
        terminalHideIncorrect = false;
        terminalPreventWrongClicks = false;
        terminalWaypoints = false;
        terminalScale = 100;
    }

    /** Reads only explicit assist keys, with harmless one-time aliases for a compatible older local config. */
    public static void load(Properties source) {
        Properties p = source == null ? new Properties() : source;
        resetDefaults();
        miningAbilityAlert = bool(p, "mining.abilityAlert", "miningAbilityNotifier", false);
        miningPuzzlerMarker = bool(p, "mining.puzzlerMarker", "miningPuzzlerSolver", false);
        // Retired Inventory Search keys are deliberately unread. The other inventory conveniences remain independent.
        hidePotionEffects = bool(p, "inventory.hidePotionEffects", "hideInventoryPotionEffects", false);
        romanNumerals = bool(p, "inventory.romanNumerals", "romanNumeralsInTooltips", false);
        tooltipScroll = bool(p, "inventory.tooltipScroll", "scrollableTooltipsEnabled", false);
        rarityHighlight = bool(p, "inventory.rarityHighlight", "itemRarityBordersEnabled", false);
        hurtCameraShake = hurtCameraShake(p);
        // Retired Item Protection keys (assist.itemGuard.* and old itemProtection*) are deliberately unread.
        autoSprint = bool(p, "movement.autoSprint", "autoSprintEnabled", false);
        wardrobeShortcut = bool(p, "shortcuts.wardrobe", "wardrobeShortcutEnabled", false);
        petsShortcut = bool(p, "shortcuts.pets", "petsShortcutEnabled", false);
        dungeonSync = bool(p, "dungeon.sync.enabled", "dungeonSyncEnabled", false);
        dungeonSyncDebug = bool(p, "dungeon.sync.debug", "dungeonSyncDebug", false);
        dungeonSyncEndpoint = endpoint(value(p, "dungeon.sync.endpoint", "dungeonSyncEndpoint", DEFAULT_SYNC_ENDPOINT));
        terminalHints = bool(p, "dungeon.terminal.hints", "terminalVisualHints", false);
        terminalAutoClick = bool(p, "dungeon.terminal.autoClick", "terminalAutoClick", false);
        terminalHideIncorrect = bool(p, "dungeon.terminal.hideIncorrect", "terminalHideIncorrect", false);
        terminalPreventWrongClicks = bool(p, "dungeon.terminal.preventWrongClicks", "terminalPreventMissclicks", false);
        terminalWaypoints = bool(p, "dungeon.terminal.waypoints", "terminalWaypoints", false);
        terminalScale = integer(p, "dungeon.terminal.scale", "terminalGuiScale", 100,
                MIN_TERMINAL_SCALE, MAX_TERMINAL_SCALE, true);
        normalize();
    }

    public static void save(Properties p) {
        if (p == null) return;
        normalize();
        // Do not carry retired controls forward when callers save into a pre-existing Properties object.
        p.remove(PREFIX + "inventory.search");
        p.remove("inventorySearchEnabled");
        p.remove(PREFIX + "visual.noHurtCamera");
        p.remove("noHurtCamEnabled");
        p.setProperty(PREFIX + "mining.abilityAlert", Boolean.toString(miningAbilityAlert));
        p.setProperty(PREFIX + "mining.puzzlerMarker", Boolean.toString(miningPuzzlerMarker));
        p.setProperty(PREFIX + "inventory.hidePotionEffects", Boolean.toString(hidePotionEffects));
        p.setProperty(PREFIX + "inventory.romanNumerals", Boolean.toString(romanNumerals));
        p.setProperty(PREFIX + "inventory.tooltipScroll", Boolean.toString(tooltipScroll));
        p.setProperty(PREFIX + "inventory.rarityHighlight", Boolean.toString(rarityHighlight));
        p.setProperty(PREFIX + "visual.hurtCameraShake", Integer.toString(hurtCameraShake));
        p.setProperty(PREFIX + "movement.autoSprint", Boolean.toString(autoSprint));
        p.setProperty(PREFIX + "shortcuts.wardrobe", Boolean.toString(wardrobeShortcut));
        p.setProperty(PREFIX + "shortcuts.pets", Boolean.toString(petsShortcut));
        p.setProperty(PREFIX + "dungeon.sync.enabled", Boolean.toString(dungeonSync));
        p.setProperty(PREFIX + "dungeon.sync.debug", Boolean.toString(dungeonSyncDebug));
        p.setProperty(PREFIX + "dungeon.sync.endpoint", dungeonSyncEndpoint);
        p.setProperty(PREFIX + "dungeon.terminal.hints", Boolean.toString(terminalHints));
        p.setProperty(PREFIX + "dungeon.terminal.autoClick", Boolean.toString(terminalAutoClick));
        p.setProperty(PREFIX + "dungeon.terminal.hideIncorrect", Boolean.toString(terminalHideIncorrect));
        p.setProperty(PREFIX + "dungeon.terminal.preventWrongClicks", Boolean.toString(terminalPreventWrongClicks));
        p.setProperty(PREFIX + "dungeon.terminal.waypoints", Boolean.toString(terminalWaypoints));
        p.setProperty(PREFIX + "dungeon.terminal.scale", Integer.toString(terminalScale));
    }

    /** 0.0 through 1.0 multiplier used exactly once at vanilla's hurt-camera tilt amplitude. */
    public static float hurtCameraShakeMultiplier() {
        return clamp(hurtCameraShake, MIN_HURT_CAMERA_SHAKE, MAX_HURT_CAMERA_SHAKE) / 100f;
    }

    /** Auto clicking is never active unless both the terminal assist and its explicit automation switch are on. */
    public static boolean terminalAutoClickActive() {
        return terminalHints && terminalAutoClick;
    }

    public static boolean validSecureEndpoint(String candidate) {
        if (candidate == null || candidate.length() > 240) return false;
        String value = candidate.trim().toLowerCase(Locale.ROOT);
        if (!value.startsWith("wss://") || value.indexOf('@') >= 0 || value.indexOf('#') >= 0 || value.indexOf('?') >= 0) return false;
        int slash = value.indexOf('/', "wss://".length());
        String host = slash < 0 ? value.substring("wss://".length()) : value.substring("wss://".length(), slash);
        String path = slash < 0 ? "" : value.substring(slash).replaceAll("/+$", "");
        // The transport appends only a local SHA-256 session path below the Worker websocket route.
        return host.length() > 0 && host.indexOf(' ') < 0 && host.indexOf(':') < 0 && "/ws".equals(path);
    }

    private static void normalize() {
        dungeonSyncEndpoint = endpoint(dungeonSyncEndpoint);
        terminalScale = clamp(terminalScale, MIN_TERMINAL_SCALE, MAX_TERMINAL_SCALE);
        hurtCameraShake = clamp(hurtCameraShake, MIN_HURT_CAMERA_SHAKE, MAX_HURT_CAMERA_SHAKE);
    }

    /** New integer key wins; the retired binary key maps true to 0% and false to vanilla 100%. */
    private static int hurtCameraShake(Properties p) {
        String current = p.getProperty(PREFIX + "visual.hurtCameraShake");
        if (current != null) return wholePercent(current, DEFAULT_HURT_CAMERA_SHAKE);
        String legacy = p.containsKey(PREFIX + "visual.noHurtCamera")
                ? p.getProperty(PREFIX + "visual.noHurtCamera")
                : p.getProperty("noHurtCamEnabled");
        if (legacy == null) return DEFAULT_HURT_CAMERA_SHAKE;
        return Boolean.parseBoolean(legacy.trim()) ? MIN_HURT_CAMERA_SHAKE : MAX_HURT_CAMERA_SHAKE;
    }

    private static int wholePercent(String raw, int fallback) {
        if (raw == null) return fallback;
        try {
            return clamp(Integer.parseInt(raw.trim()), MIN_HURT_CAMERA_SHAKE, MAX_HURT_CAMERA_SHAKE);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String endpoint(String candidate) {
        return validSecureEndpoint(candidate) ? candidate.trim() : DEFAULT_SYNC_ENDPOINT;
    }

    private static boolean bool(Properties p, String key, String legacy, boolean fallback) {
        String value = value(p, key, legacy, null);
        return value == null ? fallback : Boolean.parseBoolean(value.trim());
    }

    private static int integer(Properties p, String key, String legacy, int fallback, int min, int max,
                               boolean legacyPercentOrFloat) {
        String raw = value(p, key, legacy, null);
        if (raw == null) return fallback;
        try {
            // The old terminal source persisted a multiplier (for example 1.5); its new schema is an integer percent.
            double parsed = Double.parseDouble(raw.trim());
            if (Double.isNaN(parsed) || Double.isInfinite(parsed)) return fallback;
            if (legacyPercentOrFloat && parsed > 0D && parsed <= 5D && raw.indexOf('.') >= 0) parsed *= 100D;
            return clamp((int) Math.round(parsed), min, max);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String value(Properties p, String key, String legacy, String fallback) {
        if (p.containsKey(PREFIX + key)) return p.getProperty(PREFIX + key);
        if (legacy != null && p.containsKey(legacy)) return p.getProperty(legacy);
        return fallback;
    }
}
