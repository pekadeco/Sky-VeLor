package com.arena.pekaclient.config;

import java.util.Properties;

/**
 * Persisted controls for the allocation-conscious dungeon ESP. This owns its own prefixed schema so additions do
 * not enlarge the long-lived general-settings surface. Values are public because Forge render/tick hooks read them
 * on the client thread; {@link #normalize()} keeps direct GUI edits within their documented ranges.
 */
public final class DungeonEspSettings {

    public static final String PREFIX = "dungeonEsp.";

    public static final int MODE_OUTLINE = 0;
    public static final int MODE_FILLED = 1;
    public static final int MODE_BOTH = 2;
    public static final String[] MODE_NAMES = {"Outline", "Filled", "Outline + Filled"};

    public static final int DOORS_NEAREST = 0;
    public static final int DOORS_ALL = 1;
    public static final String[] DOOR_SCOPE_NAMES = {"Nearest closed door", "All closed doors"};

    public static final int MIN_DISTANCE = 8;
    public static final int MAX_DISTANCE = 160;
    public static final int DEFAULT_DISTANCE = 60;
    public static final int MIN_INTERVAL_TICKS = 1;
    public static final int MAX_INTERVAL_TICKS = 20;
    public static final int DEFAULT_INTERVAL_TICKS = 5;
    public static final int MIN_BOXES = 8;
    public static final int MAX_BOXES = 128;
    public static final int DEFAULT_BOXES = 64;
    public static final int MIN_WIDTH = 1;
    public static final int MAX_WIDTH = 6;
    public static final int MIN_OPACITY = 0;
    public static final int MAX_OPACITY = 100;

    /** Previous defaults remain explicit so only matching stored values are migrated, never arbitrary custom colors. */
    public static final int FORMER_WITHER_DOOR_COLOR = 0x32D7FF;
    public static final int FORMER_BLOOD_DOOR_COLOR = 0xFF453A;
    public static final int DEFAULT_WITHER_DOOR_COLOR = 0xFF453A;
    public static final int DEFAULT_BLOOD_DOOR_COLOR = 0x8B1E1E;

    /** Palette shown by the Dungeon ESP color dropdowns. RGB is deliberate; opacity has its own slider. */
    public static final String[] COLOR_NAMES = {
            "Cyan", "Gold", "Purple", "Green", "Orange", "Red", "Dark Red", "Pink", "White", "Blue", "Lime"
    };
    public static final int[] COLORS = {
            0x32D7FF, 0xFFD166, 0xBF5AF2, 0x30D158, 0xFF9F0A,
            0xFF453A, 0x8B1E1E, 0xFF7EB6, 0xF2F2F7, 0x0A84FF, 0xA6E22E
    };

    /** Master switch; star and door cache work both stop immediately when it is false. */
    public static boolean enabled = true;
    public static final Style starred = new Style(true, 0x32D7FF, MODE_BOTH, true, 2, 95, 20);
    public static final Style miniboss = new Style(true, 0xFFD166, MODE_BOTH, true, 3, 100, 24);
    /** Named special starred enemies get a separate, quieter violet category rather than being folded into normal mobs. */
    public static final Style special = new Style(true, 0xBF5AF2, MODE_BOTH, true, 2, 95, 20);
    /** Endermen, including the Dungeon-specific head-marker representation, have an independent visual style. */
    public static final Style enderman = new Style(true, 0xBF5AF2, MODE_BOTH, true, 2, 95, 20);
    /** Skeletor intentionally begins with the ordinary starred-mob visual contract. */
    public static final Style skeletor = new Style(true, 0x32D7FF, MODE_BOTH, true, 2, 95, 20);
    /** Optional room-wide hostile coverage, deliberately inert until its separate force switch is enabled. */
    public static final Style otherMobs = new Style(true, 0x30D158, MODE_BOTH, true, 2, 85, 14);
    /** Off by default: candidates are discovered only on the normal cached tracker scan. */
    public static boolean forceRenderAllMobs = false;
    public static boolean includeBats = false;
    public static boolean includeInvisibleMobs = false;

    public static int maxDistance = DEFAULT_DISTANCE;
    public static int updateIntervalTicks = DEFAULT_INTERVAL_TICKS;
    public static boolean hideZeroHealth = true;
    public static boolean tracers = false;
    public static boolean distanceText = false;
    public static int maxBoxes = DEFAULT_BOXES;

    public static boolean doorsEnabled = true;
    public static int doorScope = DOORS_NEAREST;
    public static boolean doorOnlyWhenKey = false;
    public static boolean showBloodDoors = true;
    public static int bloodDoorColor = DEFAULT_BLOOD_DOOR_COLOR;
    public static final Style doors = new Style(true, DEFAULT_WITHER_DOOR_COLOR, MODE_BOTH, true, 2, 95, 16);
    public static boolean doorDistanceText = false;

    private DungeonEspSettings() {
    }

    /** A per-category visual contract. It is one pre-created object and is never rebuilt while scanning or drawing. */
    public static final class Style {
        public boolean enabled;
        /** 0xRRGGBB; alpha is applied by the renderer from outline/fill opacity. */
        public int color;
        public int mode;
        public boolean throughWalls;
        public int outlineWidth;
        public int outlineOpacity;
        public int fillOpacity;

        private Style(boolean enabled, int color, int mode, boolean throughWalls,
                      int outlineWidth, int outlineOpacity, int fillOpacity) {
            this.enabled = enabled;
            this.color = color;
            this.mode = mode;
            this.throughWalls = throughWalls;
            this.outlineWidth = outlineWidth;
            this.outlineOpacity = outlineOpacity;
            this.fillOpacity = fillOpacity;
        }

        private void reset(boolean newEnabled, int newColor, int newMode, boolean newThroughWalls,
                           int newOutlineWidth, int newOutlineOpacity, int newFillOpacity) {
            enabled = newEnabled;
            color = newColor;
            mode = newMode;
            throughWalls = newThroughWalls;
            outlineWidth = newOutlineWidth;
            outlineOpacity = newOutlineOpacity;
            fillOpacity = newFillOpacity;
        }
    }

    /** Restores the full, high-visibility preset documented in the UI. */
    public static void applyDefaultPreset() {
        enabled = true;
        starred.reset(true, 0x32D7FF, MODE_BOTH, true, 2, 95, 20);
        miniboss.reset(true, 0xFFD166, MODE_BOTH, true, 3, 100, 24);
        special.reset(true, 0xBF5AF2, MODE_BOTH, true, 2, 95, 20);
        enderman.reset(true, 0xBF5AF2, MODE_BOTH, true, 2, 95, 20);
        skeletor.reset(true, 0x32D7FF, MODE_BOTH, true, 2, 95, 20);
        otherMobs.reset(true, 0x30D158, MODE_BOTH, true, 2, 85, 14);
        forceRenderAllMobs = false;
        includeBats = false;
        includeInvisibleMobs = false;
        maxDistance = DEFAULT_DISTANCE;
        updateIntervalTicks = DEFAULT_INTERVAL_TICKS;
        hideZeroHealth = true;
        tracers = false;
        distanceText = false;
        maxBoxes = DEFAULT_BOXES;
        doorsEnabled = true;
        doorScope = DOORS_NEAREST;
        doorOnlyWhenKey = false;
        showBloodDoors = true;
        bloodDoorColor = DEFAULT_BLOOD_DOOR_COLOR;
        doors.reset(true, DEFAULT_WITHER_DOOR_COLOR, MODE_BOTH, true, 2, 95, 16);
        doorDistanceText = false;
    }

    /** A lower-noise preset for players who prefer only thin, depth-tested edges. */
    public static void applySubtlePreset() {
        enabled = true;
        starred.reset(true, 0x32D7FF, MODE_OUTLINE, false, 1, 72, 0);
        miniboss.reset(true, 0xFFD166, MODE_OUTLINE, false, 2, 84, 0);
        special.reset(true, 0xBF5AF2, MODE_OUTLINE, false, 1, 72, 0);
        enderman.reset(true, 0xBF5AF2, MODE_OUTLINE, false, 1, 72, 0);
        skeletor.reset(true, 0x32D7FF, MODE_OUTLINE, false, 1, 72, 0);
        otherMobs.reset(true, 0x30D158, MODE_OUTLINE, false, 1, 70, 0);
        forceRenderAllMobs = false;
        includeBats = false;
        includeInvisibleMobs = false;
        maxDistance = 48;
        updateIntervalTicks = DEFAULT_INTERVAL_TICKS;
        hideZeroHealth = true;
        tracers = false;
        distanceText = false;
        maxBoxes = DEFAULT_BOXES;
        doorsEnabled = true;
        doorScope = DOORS_NEAREST;
        doorOnlyWhenKey = false;
        showBloodDoors = true;
        bloodDoorColor = DEFAULT_BLOOD_DOOR_COLOR;
        doors.reset(true, DEFAULT_WITHER_DOOR_COLOR, MODE_OUTLINE, false, 1, 72, 0);
        doorDistanceText = false;
    }

    /** Reset is intentionally equivalent to Default, not an undocumented third visual profile. */
    public static void resetDefaults() {
        applyDefaultPreset();
    }

    /** Reads only dungeonEsp.* keys. The removed mobHighlights key is consumed once as the master migration. */
    public static void load(Properties p) {
        Properties source = p == null ? new Properties() : p;
        applyDefaultPreset();
        boolean migratedLegacyMobToggle = !source.containsKey(PREFIX + "enabled") && source.containsKey("mobHighlights");
        enabled = migratedLegacyMobToggle ? readBool(source, "mobHighlights", enabled)
                : readBool(source, PREFIX + "enabled", enabled);
        loadStyle(source, "starred", starred);
        loadStyle(source, "miniboss", miniboss);
        loadStyle(source, "special", special);
        loadStyle(source, "enderman", enderman);
        loadStyle(source, "skeletor", skeletor);
        loadStyle(source, "otherMobs", otherMobs);
        forceRenderAllMobs = readBool(source, PREFIX + "forceRenderAllMobs", forceRenderAllMobs);
        includeBats = readBool(source, PREFIX + "includeBats", includeBats);
        includeInvisibleMobs = readBool(source, PREFIX + "includeInvisibleMobs", includeInvisibleMobs);
        maxDistance = readInt(source, PREFIX + "maxDistance", maxDistance, MIN_DISTANCE, MAX_DISTANCE);
        updateIntervalTicks = readInt(source, PREFIX + "updateIntervalTicks", updateIntervalTicks,
                MIN_INTERVAL_TICKS, MAX_INTERVAL_TICKS);
        hideZeroHealth = readBool(source, PREFIX + "hideZeroHealth", hideZeroHealth);
        tracers = readBool(source, PREFIX + "tracers", tracers);
        distanceText = readBool(source, PREFIX + "distanceText", distanceText);
        maxBoxes = readInt(source, PREFIX + "maxBoxes", maxBoxes, MIN_BOXES, MAX_BOXES);

        doorsEnabled = readBool(source, PREFIX + "doors.enabled", doorsEnabled);
        doorScope = readInt(source, PREFIX + "doors.scope", doorScope, DOORS_NEAREST, DOORS_ALL);
        doorOnlyWhenKey = readBool(source, PREFIX + "doors.onlyWhenKey", doorOnlyWhenKey);
        showBloodDoors = readBool(source, PREFIX + "doors.showBlood", showBloodDoors);
        bloodDoorColor = readColor(source, PREFIX + "doors.bloodColor", bloodDoorColor);
        loadStyle(source, "doors.style", doors);
        doorDistanceText = readBool(source, PREFIX + "doors.distanceText", doorDistanceText);
        migrateFormerDoorDefaults(source);
        normalize();
    }

    /** Saves only this self-contained namespace and drops the superseded old mob highlight switch. */
    public static void save(Properties p) {
        if (p == null) return;
        normalize();
        p.remove("mobHighlights");
        p.setProperty(PREFIX + "enabled", Boolean.toString(enabled));
        saveStyle(p, "starred", starred);
        saveStyle(p, "miniboss", miniboss);
        saveStyle(p, "special", special);
        saveStyle(p, "enderman", enderman);
        saveStyle(p, "skeletor", skeletor);
        saveStyle(p, "otherMobs", otherMobs);
        p.setProperty(PREFIX + "forceRenderAllMobs", Boolean.toString(forceRenderAllMobs));
        p.setProperty(PREFIX + "includeBats", Boolean.toString(includeBats));
        p.setProperty(PREFIX + "includeInvisibleMobs", Boolean.toString(includeInvisibleMobs));
        p.setProperty(PREFIX + "maxDistance", Integer.toString(maxDistance));
        p.setProperty(PREFIX + "updateIntervalTicks", Integer.toString(updateIntervalTicks));
        p.setProperty(PREFIX + "hideZeroHealth", Boolean.toString(hideZeroHealth));
        p.setProperty(PREFIX + "tracers", Boolean.toString(tracers));
        p.setProperty(PREFIX + "distanceText", Boolean.toString(distanceText));
        p.setProperty(PREFIX + "maxBoxes", Integer.toString(maxBoxes));

        p.setProperty(PREFIX + "doors.enabled", Boolean.toString(doorsEnabled));
        p.setProperty(PREFIX + "doors.scope", Integer.toString(doorScope));
        p.setProperty(PREFIX + "doors.onlyWhenKey", Boolean.toString(doorOnlyWhenKey));
        p.setProperty(PREFIX + "doors.showBlood", Boolean.toString(showBloodDoors));
        p.setProperty(PREFIX + "doors.bloodColor", Integer.toString(bloodDoorColor));
        saveStyle(p, "doors.style", doors);
        p.setProperty(PREFIX + "doors.distanceText", Boolean.toString(doorDistanceText));
    }

    /** Keeps settings safe when a config was hand edited or a widget writes a value directly. */
    public static void normalize() {
        maxDistance = clamp(maxDistance, MIN_DISTANCE, MAX_DISTANCE);
        updateIntervalTicks = clamp(updateIntervalTicks, MIN_INTERVAL_TICKS, MAX_INTERVAL_TICKS);
        maxBoxes = clamp(maxBoxes, MIN_BOXES, MAX_BOXES);
        doorScope = clamp(doorScope, DOORS_NEAREST, DOORS_ALL);
        bloodDoorColor = sanitizeColor(bloodDoorColor, DEFAULT_BLOOD_DOOR_COLOR);
        normalizeStyle(starred, 0x32D7FF);
        normalizeStyle(miniboss, 0xFFD166);
        normalizeStyle(special, 0xBF5AF2);
        normalizeStyle(enderman, 0xBF5AF2);
        normalizeStyle(skeletor, 0x32D7FF);
        normalizeStyle(otherMobs, 0x30D158);
        normalizeStyle(doors, DEFAULT_WITHER_DOOR_COLOR);
    }

    public static int colorIndex(int color) {
        int rgb = color & 0xFFFFFF;
        for (int i = 0; i < COLORS.length; i++) {
            if (COLORS[i] == rgb) return i;
        }
        return 0;
    }

    public static int colorForIndex(int index) {
        return COLORS[clamp(index, 0, COLORS.length - 1)];
    }

    /** Converts only explicit former defaults; absent and hand-picked values retain their respective meaning. */
    private static void migrateFormerDoorDefaults(Properties source) {
        if (storedColorEquals(source, PREFIX + "doors.style.color", FORMER_WITHER_DOOR_COLOR)) {
            doors.color = DEFAULT_WITHER_DOOR_COLOR;
        }
        if (storedColorEquals(source, PREFIX + "doors.bloodColor", FORMER_BLOOD_DOOR_COLOR)) {
            bloodDoorColor = DEFAULT_BLOOD_DOOR_COLOR;
        }
    }

    private static boolean storedColorEquals(Properties source, String key, int expected) {
        if (source == null || !source.containsKey(key)) return false;
        try {
            return Integer.parseInt(source.getProperty(key).trim()) == expected;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static void normalizeStyle(Style style, int defaultColor) {
        style.color = sanitizeColor(style.color, defaultColor);
        style.mode = clamp(style.mode, MODE_OUTLINE, MODE_BOTH);
        style.outlineWidth = clamp(style.outlineWidth, MIN_WIDTH, MAX_WIDTH);
        style.outlineOpacity = clamp(style.outlineOpacity, MIN_OPACITY, MAX_OPACITY);
        style.fillOpacity = clamp(style.fillOpacity, MIN_OPACITY, MAX_OPACITY);
    }

    private static void loadStyle(Properties p, String id, Style style) {
        String key = PREFIX + id + ".";
        style.enabled = readBool(p, key + "enabled", style.enabled);
        style.color = readColor(p, key + "color", style.color);
        style.mode = readInt(p, key + "mode", style.mode, MODE_OUTLINE, MODE_BOTH);
        style.throughWalls = readBool(p, key + "throughWalls", style.throughWalls);
        style.outlineWidth = readInt(p, key + "outlineWidth", style.outlineWidth, MIN_WIDTH, MAX_WIDTH);
        style.outlineOpacity = readInt(p, key + "outlineOpacity", style.outlineOpacity, MIN_OPACITY, MAX_OPACITY);
        style.fillOpacity = readInt(p, key + "fillOpacity", style.fillOpacity, MIN_OPACITY, MAX_OPACITY);
    }

    private static void saveStyle(Properties p, String id, Style style) {
        String key = PREFIX + id + ".";
        p.setProperty(key + "enabled", Boolean.toString(style.enabled));
        p.setProperty(key + "color", Integer.toString(style.color & 0xFFFFFF));
        p.setProperty(key + "mode", Integer.toString(style.mode));
        p.setProperty(key + "throughWalls", Boolean.toString(style.throughWalls));
        p.setProperty(key + "outlineWidth", Integer.toString(style.outlineWidth));
        p.setProperty(key + "outlineOpacity", Integer.toString(style.outlineOpacity));
        p.setProperty(key + "fillOpacity", Integer.toString(style.fillOpacity));
    }

    private static boolean readBool(Properties p, String key, boolean fallback) {
        String raw = p.getProperty(key);
        if (raw == null) return fallback;
        return "true".equalsIgnoreCase(raw.trim()) ? true
                : "false".equalsIgnoreCase(raw.trim()) ? false : fallback;
    }

    private static int readInt(Properties p, String key, int fallback, int min, int max) {
        try {
            return clamp(Integer.parseInt(p.getProperty(key, Integer.toString(fallback)).trim()), min, max);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int readColor(Properties p, String key, int fallback) {
        try {
            return sanitizeColor(Integer.parseInt(p.getProperty(key, Integer.toString(fallback)).trim()), fallback);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int sanitizeColor(int color, int fallback) {
        return color >= 0 && color <= 0xFFFFFF ? color : fallback;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
