package com.arena.pekaclient.hud;

import com.arena.pekaclient.config.ConfigFiles;
import com.arena.pekaclient.feature.MiningTracker;
import com.arena.pekaclient.fishing.FishingHud;
import com.arena.pekaclient.fishing.SeaCreatureHud;
import com.arena.pekaclient.slayer.SlayerHud;
import com.arena.pekaclient.render.UIRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;

import java.io.File;
import java.util.Properties;

/**
 * Owns the HUD elements. Draws the enabled ones every frame, and saves and loads their settings to
 * {@code skyvelor-hud.properties} in the game directory. Older builds wrote {@code pekaclient-hud.properties};
 * that file is read once when the new one does not exist yet, and it is never written again.
 */
public final class HudManager {

    public static final MiningHud MINING = new MiningHud();
    public static final MiningAssistantHud MINING_ASSIST = new MiningAssistantHud();
    public static final DungeonBossHud DUNGEON_BOSS_HP = new DungeonBossHud();
    public static final InventoryHud INVENTORY = new InventoryHud();
    public static final PlayerDollHud DOLL = new PlayerDollHud();
    public static final FishingHud FISHING = new FishingHud();
    public static final SeaCreatureHud SEA_HP = new SeaCreatureHud();
    public static final SlayerHud SLAYER = new SlayerHud();

    /** Elements shown in the normal HUD editor, in draw order. Metrics deliberately do not register here: FPS,
     * memory and ping exist only in Settings Home. */
    public static final HudElement[] ELEMENTS = {MINING, MINING_ASSIST, DUNGEON_BOSS_HP, INVENTORY, DOLL,
            FISHING, SEA_HP, SLAYER};

    public static final String FILE_NAME = "skyvelor-hud.properties";
    public static final String LEGACY_FILE_NAME = "pekaclient-hud.properties";

    private static boolean loaded;

    private HudManager() {
    }

    private static File dir() {
        return Minecraft.getMinecraft().mcDataDir;
    }

    /** Loads settings once. Missing or unreadable files keep the defaults. */
    public static void ensureLoaded() {
        if (!loaded) load();
    }

    public static void load() {
        Properties p = ConfigFiles.read(ConfigFiles.resolveRead(dir(), FILE_NAME, LEGACY_FILE_NAME));
        // All legacy metric HUD keys, including the former consolidated stats.* group, are intentionally ignored.
        HudConfigCompatibility.ignoreRetiredMetricKeys(p);
        HudConfigCompatibility.ignoreRetiredPlayerDollarsKeys(p);
        for (HudElement e : ELEMENTS) e.load(p);
        MiningTracker.setWindowSeconds(MINING.windowSeconds());
        loaded = true;
    }

    public static void save() {
        Properties p = new Properties();
        for (HudElement e : ELEMENTS) e.save(p);
        ConfigFiles.writeAtomic(new File(dir(), FILE_NAME), p, "Sky VeLor HUD settings");
    }

    /** Called once per client tick, before the world moves. */
    public static void tick(Minecraft mc) {
        ensureLoaded();
        MiningTracker.setWindowSeconds(MINING.windowSeconds());
        MiningTracker.tick(mc);
    }

    /** True when the in-game HUD should draw: a world is loaded, and no GUI is open except chat. */
    public static boolean visibleInGame() {
        Minecraft mc = Minecraft.getMinecraft();
        return !mc.gameSettings.hideGUI && mc.thePlayer != null
                && (mc.currentScreen == null || mc.currentScreen instanceof GuiChat);
    }

    /** Draws the enabled elements in game. A rendering failure must not strand the shared 2D frame. */
    public static void render(ScaledResolution sr) {
        UIRender.beginFrame();
        try {
            for (HudElement e : ELEMENTS) if (e.enabled) e.render(sr.getScaledWidth(), sr.getScaledHeight(), 1f);
        } finally {
            UIRender.endFrame();
        }
    }

    /** Draws every element for the editor. Disabled elements are shown faded. */
    public static void renderEditor(ScaledResolution sr, int mx, int my) {
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();
        HudElement under = null;
        for (HudElement e : ELEMENTS) {
            int x = e.leftFor(sw);
            int y = e.topFor(sh);
            if (mx >= x && mx < x + e.pixelWidth() && my >= y && my < y + e.pixelHeight()) under = e;
        }
        HudElement.backgroundOnHoverOnly = true;
        HudElement.hoveredInEditor = under;
        UIRender.beginFrame();
        try {
            for (HudElement e : ELEMENTS) e.render(sw, sh, e.enabled ? 1f : 0.35f);
        } finally {
            // These static editor hints must not bleed into the normal in-game HUD if a preview renderer throws.
            try {
                UIRender.endFrame();
            } finally {
                HudElement.backgroundOnHoverOnly = false;
                HudElement.hoveredInEditor = null;
            }
        }
    }

    public static void resetPositions() {
        for (HudElement e : ELEMENTS) e.resetPosition();
    }

    public static void resetAll() {
        for (HudElement e : ELEMENTS) e.resetAll();
    }
}
