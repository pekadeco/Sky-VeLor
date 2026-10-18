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
    /** One global animation policy for all Sky VeLor-owned scroll viewports, including chat. */
    public static final boolean DEFAULT_SMOOTH_SCROLLING = true;
    
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
    
    /** Panel opacity in percent (60..100). Higher is more solid, lower shows more of the game. */
    public static int glassOpacity = DEFAULT_GLASS_OPACITY;
    public static boolean animations = true;
    public static int animSpeed = 100;        // 40..200 (%)
    public static int curveQuality = 1;       // 0 smooth, 1 balanced, 2 fast
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
    public static boolean fishingCounter = true;       // count sea-creature pulls
    public static boolean fishingFestival = true;      // Festival Great White count and best
    public static boolean fishingJawbusAlert = true;   // title when Lord Jawbus arrives
    /** New nearby-nameplate alerts are opt-in; the established chat-arrival alert remains unchanged. */
    public static final boolean DEFAULT_FISHING_NEARBY_ENCOUNTER_ALERT = false;
    public static boolean fishingNearbyEncounterAlert = DEFAULT_FISHING_NEARBY_ENCOUNTER_ALERT;

    // Chat QoL has its own compact, Minecraft-free settings model. It is loaded/saved through this established
    // config file so old HUD positions and all other Sky VeLor settings remain untouched.

    /** Set while a legacy firstPerson* key was read; load immediately writes its Item Animation replacement. */
    private static boolean itemAnimationMigrationPending;
    /** Set when target-local Chat QoL scrolling is promoted into the one global viewport policy. */
    private static boolean globalScrollMigrationPending;

    private AppleSettings() {
    }

    public static int accentColor() {
        return ACCENT_COLORS[Math.max(0, Math.min(ACCENT_COLORS.length - 1, accent))];
    }

    public static void load() {
        itemAnimationMigrationPending = false;
        globalScrollMigrationPending = false;
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
        loadGlobalScrollSettings(p);

        loadItemAnimationSettings(p);

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
        loadFishingSettings(p);
        loadChatSettings(p);
        FontSettings.load(p);
        M7Settings.load(p);
        // A legacy config is not merely read: persist the clean Item Animation category immediately. The
        // atomic writer preserves a valid file even if the client is interrupted while migrating.
        if (itemAnimationMigrationPending || globalScrollMigrationPending || ChatQolSettings.isMigrationPending()
                || FontSettings.isMigrationPending()) {
            save();
        }
    }

    /**
     * Promotes retired per-chat scroll settings into the single global policy. A native global value always wins;
     * otherwise Default Chat is the historical source and Chat 1 is a safe fallback. This method has no Minecraft
     * dependency so compatibility tests can prove old files do not silently lose their scroll preference.
     */
    static void loadGlobalScrollSettings(Properties settings) {
        Properties p = settings == null ? new Properties() : settings;
        boolean hasSpeed = p.containsKey("scrollSpeed");
        boolean hasSmooth = p.containsKey("smoothScrolling");
        String defaultSpeed = ChatQolSettings.PREFIX + "target.default.scroll.speed";
        String chatOneSpeed = ChatQolSettings.PREFIX + "target.chat1.scroll.speed";
        String defaultSmooth = ChatQolSettings.PREFIX + "target.default.scroll.smooth";
        String chatOneSmooth = ChatQolSettings.PREFIX + "target.chat1.scroll.smooth";
        String speedSource = p.containsKey(defaultSpeed) ? defaultSpeed : chatOneSpeed;
        String smoothSource = p.containsKey(defaultSmooth) ? defaultSmooth : chatOneSmooth;
        scrollSpeed = hasSpeed ? readInt(p, "scrollSpeed", scrollSpeed, 50, 300)
                : readInt(p, speedSource, DEFAULT_SCROLL_SPEED, 50, 300);
        smoothScrolling = hasSmooth ? readBool(p, "smoothScrolling", DEFAULT_SMOOTH_SCROLLING)
                : readBool(p, smoothSource, DEFAULT_SMOOTH_SCROLLING);
        globalScrollMigrationPending = (!hasSpeed && p.containsKey(speedSource))
                || (!hasSmooth && p.containsKey(smoothSource));
    }

    /**
     * Loads only the Item Animation portion of a properties file. Package-visible for the compatibility suite;
     * the game calls this through {@link #load()}. Unknown historic keys are intentionally harmless.
     */
    static void loadItemAnimationSettings(Properties settings) {
        Properties p = settings == null ? new Properties() : settings;
        itemAnimationMigrationPending = false;
        // New category names win when both names exist; old firstPerson* keys are read once and then rewritten.
        // Beta-0.10.8v's itemAnimation.autoFixHitSlash key is intentionally ignored: the corrected visual timeline
        // is permanent and no replacement setting is retained.
        itemAnimationEnabled = readItemAnimationBool(p, "itemAnimation.enabled", "firstPersonItemAnimation", true);
        itemAnimationSizeOn = readItemAnimationBool(p, "itemAnimation.sizeOn", "firstPersonItemScaleOn",
                itemAnimationSizeOn);
        itemAnimationSpeedOn = readItemAnimationBool(p, "itemAnimation.speedOn", "firstPersonAnimSpeedOn",
                itemAnimationSpeedOn);
        itemAnimationRemoveHasteEffect = readBool(p, "itemAnimation.removeHasteEffect",
                itemAnimationRemoveHasteEffect);
        itemAnimationHitSlashStyle = readInt(p, "itemAnimation.hitSlashStyle", itemAnimationHitSlashStyle, 0,
                ITEM_ANIMATION_HIT_SLASH_STYLES.length - 1);
        itemAnimationSize = readItemAnimationInt(p, "itemAnimation.size", "firstPersonItemScale",
                itemAnimationSize, ITEM_ANIMATION_SIZE_MIN_PERCENT, ITEM_ANIMATION_SIZE_MAX_PERCENT);
        itemAnimationSpeed = readItemAnimationInt(p, "itemAnimation.speed", "firstPersonAnimSpeed",
                itemAnimationSpeed, ITEM_ANIMATION_SPEED_MIN_PERCENT, ITEM_ANIMATION_SPEED_MAX_PERCENT);
        itemAnimationX = readItemAnimationInt(p, "itemAnimation.x", "firstPersonItemX",
                itemAnimationX, ITEM_ANIMATION_OFFSET_MIN, ITEM_ANIMATION_OFFSET_MAX);
        itemAnimationY = readItemAnimationInt(p, "itemAnimation.y", "firstPersonItemY",
                itemAnimationY, ITEM_ANIMATION_OFFSET_MIN, ITEM_ANIMATION_OFFSET_MAX);
        itemAnimationZ = readItemAnimationInt(p, "itemAnimation.z", "firstPersonItemZ",
                itemAnimationZ, ITEM_ANIMATION_OFFSET_MIN, ITEM_ANIMATION_OFFSET_MAX);
    }

    /** Writes exactly the supported Item Animation keys; obsolete keys are never carried forward. */
    static void saveItemAnimationSettings(Properties p) {
        if (p == null) {
            return;
        }
        p.setProperty("itemAnimation.enabled", Boolean.toString(itemAnimationEnabled));
        p.setProperty("itemAnimation.sizeOn", Boolean.toString(itemAnimationSizeOn));
        p.setProperty("itemAnimation.speedOn", Boolean.toString(itemAnimationSpeedOn));
        p.setProperty("itemAnimation.removeHasteEffect", Boolean.toString(itemAnimationRemoveHasteEffect));
        p.setProperty("itemAnimation.hitSlashStyle", Integer.toString(itemAnimationHitSlashStyle));
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
        p.setProperty("smoothScrolling", Boolean.toString(smoothScrolling));
        
        // Item Animation. Save only the current supported category; old and obsolete keys are deliberately retired.
        saveItemAnimationSettings(p);

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
        saveFishingSettings(p);
        saveChatSettings(p);
        FontSettings.save(p);
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

    private static boolean readItemAnimationBool(Properties p, String key, String legacyKey, boolean def) {
        if (p.containsKey(legacyKey)) {
            itemAnimationMigrationPending = true;
        }
        return p.containsKey(key) ? readBool(p, key, def) : readBool(p, legacyKey, def);
    }

    private static int readItemAnimationInt(Properties p, String key, String legacyKey,
                                            int def, int min, int max) {
        if (p.containsKey(legacyKey)) {
            itemAnimationMigrationPending = true;
        }
        return p.containsKey(key) ? readInt(p, key, def, min, max)
                : readInt(p, legacyKey, def, min, max);
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
