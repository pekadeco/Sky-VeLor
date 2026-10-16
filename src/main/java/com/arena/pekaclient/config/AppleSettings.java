package com.arena.pekaclient.config;

import com.arena.pekaclient.dungeon.M7Settings;
import com.arena.pekaclient.hud.HudPlacement;
import net.minecraft.client.Minecraft;
import java.io.File;
import java.util.Properties;

/**
 * Sky VeLor client settings, saved to .minecraft/skyvelor.properties.
 *
 * Older builds used pekaclient.properties. That file is read once when the new file does not exist yet
 * (see {@link ConfigFiles}), and it is never written again.
 * Plain static fields keep reads allocation-free on the render path.
 */
public final class AppleSettings {

    public static final String FILE_NAME = "skyvelor.properties";
    public static final String LEGACY_FILE_NAME = "pekaclient.properties";

    public static final String[] ACCENT_NAMES = {"Blue", "Purple", "Pink", "Green", "Orange", "Teal"};
    public static final int[] ACCENT_COLORS = {
            0xFF0A84FF, 0xFFBF5AF2, 0xFFFF375F, 0xFF30D158, 0xFFFF9F0A, 0xFF64D2FF
    };
    public static final String[] CURVE_NAMES = {"Smooth", "Balanced", "Fast"};
    /** Arc subdivisions per 90 degrees for each curve quality. */
    public static final int[] CURVE_SEGMENTS = {8, 4, 2};

    public static int accent = 0;
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
    public static final int DEFAULT_ANIM_SPEED = 100;
    public static final boolean DEFAULT_ADAPTIVE_QUALITY = false;
    public static final boolean DEFAULT_PERFORMANCE_BOOST_BETA = true;
    public static final int DEFAULT_SCROLL_SPEED = 100;
    
    // First-person held item customization. Both are percentages; 100% is untouched vanilla.
    public static final int DEFAULT_FIRST_PERSON_ITEM_SCALE = 100;  // 100% = normal size
    public static final int DEFAULT_FIRST_PERSON_ANIM_SPEED = 100;  // 100% = normal speed
    /** Shared range for both sliders, so the GUI, the loader and the clamps cannot drift apart. */
    public static final int FIRST_PERSON_MIN = 50;
    public static final int FIRST_PERSON_MAX = 200;
    /** Renderer translation is stored in hundredths of a first-person render unit. */
    public static final int FIRST_PERSON_OFFSET_MIN = -100;
    public static final int FIRST_PERSON_OFFSET_MAX = 100;
    public static final int DEFAULT_FIRST_PERSON_ITEM_X = 0;
    public static final int DEFAULT_FIRST_PERSON_ITEM_Y = 0;
    public static final int DEFAULT_FIRST_PERSON_ITEM_Z = 0;
    public static boolean firstPersonItemScaleOn = true;
    public static boolean firstPersonAnimSpeedOn = true;

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
    
    public static int effectiveFirstPersonItemScale() {
        return firstPersonItemScaleOn ? Math.max(FIRST_PERSON_MIN,
                Math.min(FIRST_PERSON_MAX, firstPersonItemScale)) : DEFAULT_FIRST_PERSON_ITEM_SCALE;
    }

    public static int effectiveFirstPersonAnimSpeed() {
        return firstPersonAnimSpeedOn ? Math.max(FIRST_PERSON_MIN,
                Math.min(FIRST_PERSON_MAX, firstPersonAnimSpeed)) : DEFAULT_FIRST_PERSON_ANIM_SPEED;
    }
    
    /** Panel opacity in percent (60..100). Higher is more solid, lower shows more of the game. */
    public static int glassOpacity = DEFAULT_GLASS_OPACITY;
    public static boolean animations = true;
    public static int animSpeed = 100;        // 40..200 (%)
    public static int curveQuality = 1;       // 0 smooth, 1 balanced, 2 fast
    public static boolean shadows = true;
    public static boolean adaptive = DEFAULT_ADAPTIVE_QUALITY; // auto-lower detail when FPS drops
    public static int scrollSpeed = 100;      // 50..300 (%), wheel and swipe speed (keys are fixed)
    
    // First-person item customization settings (50..200%)
    public static int firstPersonItemScale = DEFAULT_FIRST_PERSON_ITEM_SCALE;  // 50..200 (%)
    public static int firstPersonAnimSpeed = DEFAULT_FIRST_PERSON_ANIM_SPEED;  // 50..200 (%)
    /** X/Y/Z visual offsets, in 1/100ths of a first-person render unit. */
    public static int firstPersonItemX = DEFAULT_FIRST_PERSON_ITEM_X;
    public static int firstPersonItemY = DEFAULT_FIRST_PERSON_ITEM_Y;
    public static int firstPersonItemZ = DEFAULT_FIRST_PERSON_ITEM_Z;
    
    /** Dungeon tab: replace the server's Spirit Leap menu with the teammate selector. On by default. */
    public static boolean leapMenu = true;
    /** Dungeon tab: outline dungeon mobs (named mobs with a level tag). On by default. Overlay only. */
    public static boolean mobHighlights = true;

    // Live dungeon-map overlay imported from the newest SkyVeLor source. Position uses the established HUD
    // anchors but deliberately remains unclamped, so an editor reset can always recover a map moved off-screen.
    public static boolean dungeonMap = true;
    public static boolean dungeonMapRotate = true;
    public static boolean dungeonMapShowPlayerNames = true;
    public static boolean dungeonMapShowPuzzleName = true;
    /** Opt-in only: map detection never sends party chat unless the player enables this. */
    public static boolean partyPuzzleAnnounce = false;
    public static final int DUNGEON_MAP_SCALE_MIN = 70;
    public static final int DUNGEON_MAP_SCALE_MAX = 250;
    public static int dungeonMapScale = 100;
    public static boolean dungeonMapScaleOn = true;
    public static final int DUNGEON_MAP_ZOOM_MIN = 50;
    public static final int DUNGEON_MAP_ZOOM_MAX = 200;
    public static int dungeonMapZoom = 100;
    /** Legacy percentage coordinates are kept only for migration from map-enabled friend configs. */
    public static int dungeonMapXPercent = 2;
    public static int dungeonMapYPercent = 2;
    public static final HudPlacement dungeonMapPlacement = new HudPlacement(
            HudPlacement.Anchor.START, 12, HudPlacement.Anchor.START, 12);
    private static boolean dungeonMapPlacementInitialized = true;
    /**
     * Performance Boost Beta. Enabled on a fresh install. Selects lower-cost Sky VeLor GUI/HUD work:
     * fewer curve segments and no shadows in the hub and editor, slower HUD scans, and cached HUD text.
     * It never touches entity rendering, world rendering or JVM flags.
     */
    public static boolean experimentalPerformance = DEFAULT_PERFORMANCE_BOOST_BETA;
    public static boolean slayerTracker = true;        // track Slayer drops from chat
    public static boolean slayerBossTimer = true;      // boss spawn to quest complete timer
    public static boolean slayerPricing = true;        // profit and /setprice
    public static boolean svCommand = true;            // /sv opens the hub
    public static boolean qnCommand = true;            // /qn opens quick notes
    public static boolean leapKey = true;              // Spirit Leap key works when bound
    public static boolean fishingCounter = true;       // count legendary catches
    public static boolean fishingFestival = true;      // Festival Great White count and best
    public static boolean fishingJawbusAlert = true;   // title when Lord Jawbus arrives

    private AppleSettings() {
    }

    public static int accentColor() {
        return ACCENT_COLORS[Math.max(0, Math.min(ACCENT_COLORS.length - 1, accent))];
    }

    public static void load() {
        Properties p = ConfigFiles.read(readFile());
        accent = readInt(p, "accent", accent, 0, ACCENT_COLORS.length - 1);
        darkMode = readBool(p, "darkMode", darkMode);
        cornerRadius = readInt(p, "cornerRadius", cornerRadius, 4, 18);
        cornerRadiusOn = readBool(p, "cornerRadiusOn", cornerRadiusOn);
        glassOpacityOn = readBool(p, "glassOpacityOn", glassOpacityOn);
        animSpeedOn = readBool(p, "animSpeedOn", animSpeedOn);
        scrollSpeedOn = readBool(p, "scrollSpeedOn", scrollSpeedOn);
        glassOpacity = readInt(p, "glassOpacity", glassOpacity, 60, 100);
        animations = readBool(p, "animations", animations);
        animSpeed = readInt(p, "animSpeed", animSpeed, 40, 200);
        curveQuality = readInt(p, "curveQuality", curveQuality, 0, 2);
        shadows = readBool(p, "shadows", shadows);
        loadPerformanceSettings(p);
        scrollSpeed = readInt(p, "scrollSpeed", scrollSpeed, 50, 300);
        
        // First-person item customization
        firstPersonItemScaleOn = readBool(p, "firstPersonItemScaleOn", firstPersonItemScaleOn);
        firstPersonAnimSpeedOn = readBool(p, "firstPersonAnimSpeedOn", firstPersonAnimSpeedOn);
        firstPersonItemScale = readInt(p, "firstPersonItemScale", firstPersonItemScale,
                FIRST_PERSON_MIN, FIRST_PERSON_MAX);
        firstPersonAnimSpeed = readInt(p, "firstPersonAnimSpeed", firstPersonAnimSpeed,
                FIRST_PERSON_MIN, FIRST_PERSON_MAX);
        firstPersonItemX = readInt(p, "firstPersonItemX", firstPersonItemX,
                FIRST_PERSON_OFFSET_MIN, FIRST_PERSON_OFFSET_MAX);
        firstPersonItemY = readInt(p, "firstPersonItemY", firstPersonItemY,
                FIRST_PERSON_OFFSET_MIN, FIRST_PERSON_OFFSET_MAX);
        firstPersonItemZ = readInt(p, "firstPersonItemZ", firstPersonItemZ,
                FIRST_PERSON_OFFSET_MIN, FIRST_PERSON_OFFSET_MAX);

        leapMenu = readBool(p, "leapMenu", leapMenu);
        mobHighlights = readBool(p, "mobHighlights", mobHighlights);
        dungeonMap = readBool(p, "dungeonMap", dungeonMap);
        dungeonMapRotate = readBool(p, "dungeonMapRotate", dungeonMapRotate);
        dungeonMapShowPlayerNames = readBool(p, "dungeonMapShowPlayerNames", dungeonMapShowPlayerNames);
        dungeonMapShowPuzzleName = readBool(p, "dungeonMapShowPuzzleName", dungeonMapShowPuzzleName);
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
        leapKey = readBool(p, "leapKey", leapKey);
        svCommand = readBool(p, "cmd.sv", svCommand);
        slayerTracker = readBool(p, "slayer.tracker", slayerTracker);
        slayerBossTimer = readBool(p, "slayer.bossTimer", slayerBossTimer);
        slayerPricing = readBool(p, "slayer.pricing", slayerPricing);
        qnCommand = readBool(p, "cmd.qn", qnCommand);
        fishingCounter = readBool(p, "fishing.counter", fishingCounter);
        fishingFestival = readBool(p, "fishing.festival", fishingFestival);
        fishingJawbusAlert = readBool(p, "fishing.jawbusAlert", fishingJawbusAlert);
        M7Settings.load(p);
    }

    public static void save() {
        Properties p = new Properties();
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
        p.setProperty("adaptive", Boolean.toString(adaptive));
        p.setProperty("scrollSpeed", Integer.toString(scrollSpeed));
        
        // First-person item customization
        p.setProperty("firstPersonItemScaleOn", Boolean.toString(firstPersonItemScaleOn));
        p.setProperty("firstPersonAnimSpeedOn", Boolean.toString(firstPersonAnimSpeedOn));
        p.setProperty("firstPersonItemScale", Integer.toString(firstPersonItemScale));
        p.setProperty("firstPersonAnimSpeed", Integer.toString(firstPersonAnimSpeed));
        p.setProperty("firstPersonItemX", Integer.toString(firstPersonItemX));
        p.setProperty("firstPersonItemY", Integer.toString(firstPersonItemY));
        p.setProperty("firstPersonItemZ", Integer.toString(firstPersonItemZ));

        p.setProperty("leapMenu", Boolean.toString(leapMenu));
        p.setProperty("mobHighlights", Boolean.toString(mobHighlights));
        p.setProperty("dungeonMap", Boolean.toString(dungeonMap));
        p.setProperty("dungeonMapRotate", Boolean.toString(dungeonMapRotate));
        p.setProperty("dungeonMapShowPlayerNames", Boolean.toString(dungeonMapShowPlayerNames));
        p.setProperty("dungeonMapShowPuzzleName", Boolean.toString(dungeonMapShowPuzzleName));
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
        p.setProperty("experimentalPerformance", Boolean.toString(experimentalPerformance));
        p.setProperty("leapKey", Boolean.toString(leapKey));
        p.setProperty("cmd.sv", Boolean.toString(svCommand));
        p.setProperty("slayer.tracker", Boolean.toString(slayerTracker));
        p.setProperty("slayer.bossTimer", Boolean.toString(slayerBossTimer));
        p.setProperty("slayer.pricing", Boolean.toString(slayerPricing));
        p.setProperty("cmd.qn", Boolean.toString(qnCommand));
        p.setProperty("fishing.counter", Boolean.toString(fishingCounter));
        p.setProperty("fishing.festival", Boolean.toString(fishingFestival));
        p.setProperty("fishing.jawbusAlert", Boolean.toString(fishingJawbusAlert));
        M7Settings.save(p);
        ConfigFiles.writeAtomic(new File(dir(), FILE_NAME), p, "Sky VeLor settings");
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
        return ConfigFiles.resolveRead(dir(), FILE_NAME, LEGACY_FILE_NAME);
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
        return Boolean.parseBoolean(p.getProperty(key, Boolean.toString(def)));
    }
}
