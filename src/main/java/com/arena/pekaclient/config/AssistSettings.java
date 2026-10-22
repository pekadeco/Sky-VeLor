package com.arena.pekaclient.config;

import java.util.Locale;
import java.util.Properties;

/**
 * Isolated, safe-by-default schema for the Beta-0.10.8.7 optional client assists. Keeping this schema separate
 * from the older shared AppleSettings fields means the new modules cannot repurpose a historic key or move an
 * existing HUD position. Every value is normalized on both load and save.
 */
public final class AssistSettings {

    public static final String PREFIX = "assist.";
    public static final String DEFAULT_SYNC_ENDPOINT = "wss://skyvelor-dungeon-sync.bombadzalge.workers.dev/ws";
    public static final int MIN_TERMINAL_SCALE = 70;
    public static final int MAX_TERMINAL_SCALE = 250;

    public static boolean miningAbilityAlert;
    public static boolean miningPuzzlerMarker;

    public static boolean inventorySearch;
    public static boolean hidePotionEffects;
    public static boolean romanNumerals;
    public static boolean tooltipScroll;
    public static boolean rarityHighlight;
    public static boolean noHurtCamera;

    public static boolean itemGuard;
    public static boolean itemGuardNotifications;
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
        // All new gameplay, network, input and rendering changes deliberately start opt-in.
        miningAbilityAlert = false;
        miningPuzzlerMarker = false;
        inventorySearch = false;
        hidePotionEffects = false;
        romanNumerals = false;
        tooltipScroll = false;
        rarityHighlight = false;
        noHurtCamera = false;
        itemGuard = false;
        itemGuardNotifications = true;
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

    /** Reads only explicit assist keys, with harmless one-time aliases for a compatible older local friend config. */
    public static void load(Properties source) {
        Properties p = source == null ? new Properties() : source;
        resetDefaults();
        miningAbilityAlert = bool(p, "mining.abilityAlert", "miningAbilityNotifier", false);
        miningPuzzlerMarker = bool(p, "mining.puzzlerMarker", "miningPuzzlerSolver", false);
        inventorySearch = bool(p, "inventory.search", "inventorySearchEnabled", false);
        hidePotionEffects = bool(p, "inventory.hidePotionEffects", "hideInventoryPotionEffects", false);
        romanNumerals = bool(p, "inventory.romanNumerals", "romanNumeralsInTooltips", false);
        tooltipScroll = bool(p, "inventory.tooltipScroll", "scrollableTooltipsEnabled", false);
        rarityHighlight = bool(p, "inventory.rarityHighlight", "itemRarityBordersEnabled", false);
        noHurtCamera = bool(p, "visual.noHurtCamera", "noHurtCamEnabled", false);
        itemGuard = bool(p, "itemGuard.enabled", "itemProtectionEnabled", false);
        itemGuardNotifications = bool(p, "itemGuard.notifications", "itemProtectionNotifications", true);
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
        p.setProperty(PREFIX + "mining.abilityAlert", Boolean.toString(miningAbilityAlert));
        p.setProperty(PREFIX + "mining.puzzlerMarker", Boolean.toString(miningPuzzlerMarker));
        p.setProperty(PREFIX + "inventory.search", Boolean.toString(inventorySearch));
        p.setProperty(PREFIX + "inventory.hidePotionEffects", Boolean.toString(hidePotionEffects));
        p.setProperty(PREFIX + "inventory.romanNumerals", Boolean.toString(romanNumerals));
        p.setProperty(PREFIX + "inventory.tooltipScroll", Boolean.toString(tooltipScroll));
        p.setProperty(PREFIX + "inventory.rarityHighlight", Boolean.toString(rarityHighlight));
        p.setProperty(PREFIX + "visual.noHurtCamera", Boolean.toString(noHurtCamera));
        p.setProperty(PREFIX + "itemGuard.enabled", Boolean.toString(itemGuard));
        p.setProperty(PREFIX + "itemGuard.notifications", Boolean.toString(itemGuardNotifications));
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

    /** Auto clicking is never active unless both the terminal assist and its explicit automation switch are on. */
    public static boolean terminalAutoClickActive() {
        return terminalHints && terminalAutoClick;
    }

    public static boolean validSecureEndpoint(String candidate) {
        if (candidate == null || candidate.length() > 240) return false;
        String value = candidate.trim().toLowerCase(Locale.ROOT);
        if (!value.startsWith("wss://") || value.indexOf('@') >= 0 || value.indexOf('#') >= 0) return false;
        int slash = value.indexOf('/', "wss://".length());
        String host = slash < 0 ? value.substring("wss://".length()) : value.substring("wss://".length(), slash);
        return host.length() > 0 && host.indexOf(' ') < 0 && host.indexOf(':') < 0;
    }

    private static void normalize() {
        dungeonSyncEndpoint = endpoint(dungeonSyncEndpoint);
        terminalScale = Math.max(MIN_TERMINAL_SCALE, Math.min(MAX_TERMINAL_SCALE, terminalScale));
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
            return Math.max(min, Math.min(max, (int) Math.round(parsed)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String value(Properties p, String key, String legacy, String fallback) {
        if (p.containsKey(PREFIX + key)) return p.getProperty(PREFIX + key);
        if (legacy != null && p.containsKey(legacy)) return p.getProperty(legacy);
        return fallback;
    }
}
