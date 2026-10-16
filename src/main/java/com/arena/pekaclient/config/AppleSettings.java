package com.arena.pekaclient.config;

import com.arena.pekaclient.dungeon.M7Settings;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

/**
 * Sky VeLor client settings, saved to .minecraft/skyvelor.properties.
 *
 * Older builds used pekaclient.properties. That file is read once when the new file does not exist yet
 * (see {@link ConfigFiles}), and it is never written again.
 * Plain static fields keep reads allocation-free on the render path.
 */
public final class AppleSettings {

    private static final Logger LOG = LogManager.getLogger("SkyVeLor");
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
    /** Panel opacity in percent (60..100). Higher is more solid, lower shows more of the game. */
    public static int glassOpacity = DEFAULT_GLASS_OPACITY;
    public static boolean animations = true;
    public static int animSpeed = 100;        // 40..200 (%)
    public static int curveQuality = 1;       // 0 smooth, 1 balanced, 2 fast
    public static boolean shadows = true;
    public static boolean adaptive = DEFAULT_ADAPTIVE_QUALITY; // auto-lower detail when FPS drops
    public static int scrollSpeed = 100;      // 50..300 (%), wheel and swipe speed (keys are fixed)
    /** Dungeon tab: replace the server's Spirit Leap menu with the teammate selector. On by default. */
    public static boolean leapMenu = true;
    /** Dungeon tab: outline dungeon mobs (named mobs with a level tag). On by default. Overlay only. */
    public static boolean mobHighlights = true;
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
        leapMenu = readBool(p, "leapMenu", leapMenu);
        mobHighlights = readBool(p, "mobHighlights", mobHighlights);
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
        p.setProperty("leapMenu", Boolean.toString(leapMenu));
        p.setProperty("mobHighlights", Boolean.toString(mobHighlights));
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
        try (OutputStream out = new FileOutputStream(new File(dir(), FILE_NAME))) {
            p.store(out, "Sky VeLor settings");
        } catch (IOException e) {
            LOG.warn("Could not save Sky VeLor settings", e);
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
