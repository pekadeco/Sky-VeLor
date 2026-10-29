package com.arena.pekaclient.config;

import com.arena.pekaclient.assist.CommandKeybindSettings;

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
    /** A short render-only camera/world glide softens an already-completed AOTE/AOTV view change. */
    public static final boolean DEFAULT_SMOOTH_ASPECT_TELEPORT = true;
    /** Fresh installs enable only these client-side inventory presentation helpers. */
    public static final boolean DEFAULT_ROMAN_NUMERALS = true;
    public static final boolean DEFAULT_TOOLTIP_SCROLL = true;
    public static final boolean DEFAULT_RARITY_HIGHLIGHT = true;
    /** Client-side visual cleanup for dead/dying mob and server-NPC body/nameplate renders. */
    public static final boolean DEFAULT_HIDE_DEAD_MOBS = true;
    /** LWJGL 1.8.9 key codes kept as plain constants so this persistence model stays Minecraft-independent. */
    public static final int DEFAULT_WARDROBE_SHORTCUT_KEY_CODE = 17; // W
    public static final int DEFAULT_PETS_SHORTCUT_KEY_CODE = 25; // P
    public static final boolean DEFAULT_WARDROBE_NUMBER_KEYS = true;

    public static boolean miningAbilityAlert;
    public static boolean miningPuzzlerMarker;

    public static boolean hidePotionEffects;
    public static boolean romanNumerals;
    public static boolean tooltipScroll;
    public static boolean rarityHighlight;
    /** Whole-number percentage of vanilla's local hurt-camera tilt. Zero removes only that tilt. */
    public static int hurtCameraShake;
    /** Render-only Aspect of the End/Void camera/world interpolation. It never changes movement or packets. */
    public static boolean smoothAspectTeleport;

    public static boolean autoSprint;
    public static boolean wardrobeShortcut;
    /** -1 means preserve an established Minecraft Controls binding until this feature is explicitly captured. */
    public static int wardrobeShortcutKeyCode;
    /** Direct number-key selection is opt-in at the Wardrobe GUI only and defaults on to preserve .1.9 behavior. */
    public static boolean wardrobeNumberKeys;
    public static boolean petsShortcut;
    /** -1 has the same preserve-existing-controls meaning as the Wardrobe key. */
    public static int petsShortcutKeyCode;
    /** QoL visual owner for render-only dead mob/server-NPC body and nameplate cleanup. */
    public static boolean hideDeadMobs;

    public static boolean dungeonSync;
    public static boolean dungeonSyncDebug;
    public static String dungeonSyncEndpoint;
    /** Read-only client-world locator for nearby Spirit Bow marker stands. */
    public static boolean spiritBowLocator;

    public static boolean terminalHints;
    public static boolean terminalAutoClick;
    public static boolean terminalHideIncorrect;
    public static boolean terminalPreventWrongClicks;
    public static boolean terminalWaypoints;
    public static int terminalScale;

    private AssistSettings() {
    }

    /** Applies the fresh-install defaults, including only local visual/inventory conveniences. */
    public static void resetDefaults() {
        resetDefaults(true);
    }

    /**
     * Existing configurations must not acquire a new visible inventory behavior merely because an old file lacks a
     * newly-defaulted key. Gameplay, network and input assists remain opt-in in both paths; the separately requested
     * Aspect transition is a bounded render-only effect, so it is safe to default on for either configuration age.
     */
    private static void resetDefaults(boolean freshInstall) {
        miningAbilityAlert = false;
        miningPuzzlerMarker = false;
        hidePotionEffects = false;
        romanNumerals = freshInstall && DEFAULT_ROMAN_NUMERALS;
        tooltipScroll = freshInstall && DEFAULT_TOOLTIP_SCROLL;
        rarityHighlight = freshInstall && DEFAULT_RARITY_HIGHLIGHT;
        hurtCameraShake = DEFAULT_HURT_CAMERA_SHAKE;
        smoothAspectTeleport = DEFAULT_SMOOTH_ASPECT_TELEPORT;
        autoSprint = false;
        wardrobeShortcut = false;
        // Fresh installs and the visible Shortcut reset start with W/P. Established configurations without an explicit
        // shortcut key retain their existing Controls binding instead of being overwritten by this new default.
        wardrobeShortcutKeyCode = freshInstall ? DEFAULT_WARDROBE_SHORTCUT_KEY_CODE : -1;
        wardrobeNumberKeys = DEFAULT_WARDROBE_NUMBER_KEYS;
        petsShortcut = false;
        petsShortcutKeyCode = freshInstall ? DEFAULT_PETS_SHORTCUT_KEY_CODE : -1;
        CommandKeybindSettings.reset();
        // This is a visual cleanup, not combat automation: it is deliberately useful on new and established configs.
        hideDeadMobs = DEFAULT_HIDE_DEAD_MOBS;
        dungeonSync = false;
        dungeonSyncDebug = false;
        dungeonSyncEndpoint = DEFAULT_SYNC_ENDPOINT;
        spiritBowLocator = false;
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
        // A nonempty file is an established player's configuration, even if it predates an individual assist key.
        resetDefaults(p.isEmpty());
        miningAbilityAlert = bool(p, "mining.abilityAlert", "miningAbilityNotifier", miningAbilityAlert);
        miningPuzzlerMarker = bool(p, "mining.puzzlerMarker", "miningPuzzlerSolver", miningPuzzlerMarker);
        // Retired Inventory Search keys are deliberately unread. The other inventory conveniences remain independent.
        hidePotionEffects = bool(p, "inventory.hidePotionEffects", "hideInventoryPotionEffects", hidePotionEffects);
        romanNumerals = bool(p, "inventory.romanNumerals", "romanNumeralsInTooltips", romanNumerals);
        tooltipScroll = bool(p, "inventory.tooltipScroll", "scrollableTooltipsEnabled", tooltipScroll);
        rarityHighlight = bool(p, "inventory.rarityHighlight", "itemRarityBordersEnabled", rarityHighlight);
        hurtCameraShake = hurtCameraShake(p);
        smoothAspectTeleport = bool(p, "visual.smoothAspectTeleport", null, smoothAspectTeleport);
        // Retired Item Protection keys (assist.itemGuard.* and old itemProtection*) are deliberately unread.
        // Shortcut replaces the former visible Keybind Cut group. New keys win, then the predecessor namespace and
        // original aliases preserve established choices on their first load before save rewrites them under shortcut.*.
        autoSprint = shortcutBool(p, "shortcut.autoSprint", "keybindCut.autoSprint", "movement.autoSprint", "autoSprintEnabled", false);
        wardrobeShortcut = shortcutBool(p, "shortcut.wardrobe", "keybindCut.wardrobe", "shortcuts.wardrobe", "wardrobeShortcutEnabled", false);
        int savedWardrobeKey = shortcutKeyCode(p, "shortcut.wardrobeKey", "keybindCut.wardrobeKey");
        if (savedWardrobeKey >= 0) wardrobeShortcutKeyCode = savedWardrobeKey;
        wardrobeNumberKeys = shortcutBool(p, "shortcut.wardrobeNumberKeys", "keybindCut.wardrobeNumberKeys",
                "shortcuts.wardrobeNumberKeys", null, DEFAULT_WARDROBE_NUMBER_KEYS);
        petsShortcut = shortcutBool(p, "shortcut.pets", "keybindCut.pets", "shortcuts.pets", "petsShortcutEnabled", false);
        int savedPetsKey = shortcutKeyCode(p, "shortcut.petsKey", "keybindCut.petsKey");
        if (savedPetsKey >= 0) petsShortcutKeyCode = savedPetsKey;
        CommandKeybindSettings.load(p);
        // The Comfort key wins. The former ESP-local zero-health flag migrates once so an explicit OFF choice is
        // preserved, while absent keys use the new default and include deathTime animation frames.
        hideDeadMobs = hideDeadMobs(p);
        dungeonSync = bool(p, "dungeon.sync.enabled", "dungeonSyncEnabled", false);
        dungeonSyncDebug = bool(p, "dungeon.sync.debug", "dungeonSyncDebug", false);
        dungeonSyncEndpoint = endpoint(value(p, "dungeon.sync.endpoint", "dungeonSyncEndpoint", DEFAULT_SYNC_ENDPOINT));
        spiritBowLocator = bool(p, "dungeon.spiritBowLocator", null, false);
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
        // Shortcut writes one modern namespace after loading either predecessor; never keep stale duplicate keys.
        p.remove(PREFIX + "keybindCut.autoSprint");
        p.remove(PREFIX + "keybindCut.wardrobe");
        p.remove(PREFIX + "keybindCut.wardrobeKey");
        p.remove(PREFIX + "keybindCut.wardrobeNumberKeys");
        p.remove(PREFIX + "keybindCut.pets");
        p.remove(PREFIX + "keybindCut.petsKey");
        p.remove(PREFIX + "movement.autoSprint");
        p.remove(PREFIX + "shortcuts.wardrobe");
        p.remove(PREFIX + "shortcuts.wardrobeNumberKeys");
        p.remove(PREFIX + "shortcuts.pets");
        p.remove(PREFIX + "shortcuts.petsKey");
        p.remove("autoSprintEnabled");
        p.remove("wardrobeShortcutEnabled");
        p.remove("petsShortcutEnabled");
        // The old Dungeon ESP owner is retired. Do not allow an in-place Properties save to revive it.
        p.remove("dungeonEsp.hideZeroHealth");
        p.setProperty(PREFIX + "mining.abilityAlert", Boolean.toString(miningAbilityAlert));
        p.setProperty(PREFIX + "mining.puzzlerMarker", Boolean.toString(miningPuzzlerMarker));
        p.setProperty(PREFIX + "inventory.hidePotionEffects", Boolean.toString(hidePotionEffects));
        p.setProperty(PREFIX + "inventory.romanNumerals", Boolean.toString(romanNumerals));
        p.setProperty(PREFIX + "inventory.tooltipScroll", Boolean.toString(tooltipScroll));
        p.setProperty(PREFIX + "inventory.rarityHighlight", Boolean.toString(rarityHighlight));
        p.setProperty(PREFIX + "visual.hurtCameraShake", Integer.toString(hurtCameraShake));
        p.setProperty(PREFIX + "visual.smoothAspectTeleport", Boolean.toString(smoothAspectTeleport));
        p.setProperty(PREFIX + "shortcut.autoSprint", Boolean.toString(autoSprint));
        p.setProperty(PREFIX + "shortcut.wardrobe", Boolean.toString(wardrobeShortcut));
        saveShortcutKeyCode(p, "shortcut.wardrobeKey", wardrobeShortcutKeyCode);
        p.setProperty(PREFIX + "shortcut.wardrobeNumberKeys", Boolean.toString(wardrobeNumberKeys));
        p.setProperty(PREFIX + "shortcut.pets", Boolean.toString(petsShortcut));
        saveShortcutKeyCode(p, "shortcut.petsKey", petsShortcutKeyCode);
        CommandKeybindSettings.save(p);
        p.setProperty(PREFIX + "comfort.hideDeadMobs", Boolean.toString(hideDeadMobs));
        p.setProperty(PREFIX + "dungeon.sync.enabled", Boolean.toString(dungeonSync));
        p.setProperty(PREFIX + "dungeon.sync.debug", Boolean.toString(dungeonSyncDebug));
        p.setProperty(PREFIX + "dungeon.sync.endpoint", dungeonSyncEndpoint);
        p.setProperty(PREFIX + "dungeon.spiritBowLocator", Boolean.toString(spiritBowLocator));
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
        if (!validShortcutKeyCode(wardrobeShortcutKeyCode)) wardrobeShortcutKeyCode = -1;
        if (!validShortcutKeyCode(petsShortcutKeyCode)) petsShortcutKeyCode = -1;
    }

    /**
     * New Comfort ownership wins over the legacy Dungeon ESP zero-health flag. The legacy key's boolean is preserved
     * exactly, but the new behavior additionally removes an entity while its deathTime animation is running.
     */
    private static boolean hideDeadMobs(Properties p) {
        String current = p.getProperty(PREFIX + "comfort.hideDeadMobs");
        if (current != null) return Boolean.parseBoolean(current.trim());
        String legacy = p.getProperty("dungeonEsp.hideZeroHealth");
        return legacy == null ? DEFAULT_HIDE_DEAD_MOBS : Boolean.parseBoolean(legacy.trim());
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

    /** A captured keyboard code includes zero for an intentional Backspace-cleared binding. */
    public static boolean validShortcutKeyCode(int keyCode) {
        return keyCode >= 0 && keyCode <= 255;
    }

    /** Returns -1 only when no valid persisted value exists, preserving an established Controls binding on older files. */
    private static int shortcutKeyCode(Properties p, String currentPath, String predecessorPath) {
        String value = p.getProperty(PREFIX + currentPath);
        if (value == null && predecessorPath != null) value = p.getProperty(PREFIX + predecessorPath);
        if (value == null) return -1;
        try {
            int code = Integer.parseInt(value.trim());
            return validShortcutKeyCode(code) ? code : -1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static void saveShortcutKeyCode(Properties p, String path, int keyCode) {
        if (validShortcutKeyCode(keyCode)) p.setProperty(PREFIX + path, Integer.toString(keyCode));
        else p.remove(PREFIX + path);
    }

    /** New Shortcut keys take precedence over Keybind Cut, older section paths, and then flat aliases. */
    private static boolean shortcutBool(Properties p, String current, String keybindCutPath, String legacyPath,
                                        String legacyFlat, boolean fallback) {
        String currentValue = p.getProperty(PREFIX + current);
        if (currentValue != null) return Boolean.parseBoolean(currentValue.trim());
        String priorValue = p.getProperty(PREFIX + keybindCutPath);
        if (priorValue != null) return Boolean.parseBoolean(priorValue.trim());
        String oldPath = p.getProperty(PREFIX + legacyPath);
        if (oldPath != null) return Boolean.parseBoolean(oldPath.trim());
        String oldFlat = legacyFlat == null ? null : p.getProperty(legacyFlat);
        return oldFlat == null ? fallback : Boolean.parseBoolean(oldFlat.trim());
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
