package com.arena.pekaclient.hud;

import com.arena.pekaclient.config.ConfigFiles;
import com.arena.pekaclient.feature.MiningTracker;
import com.arena.pekaclient.render.UIRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

/**
 * Owns the HUD elements. Draws the enabled ones every frame, and saves and loads their settings to
 * {@code skyvelor-hud.properties} in the game directory. Older builds wrote {@code pekaclient-hud.properties};
 * that file is read once when the new one does not exist yet, and it is never written again.
 *
 * The file is read once. A corrupt file is logged and the defaults are used, so a bad save cannot stop
 * the game from starting. Save writes to a temporary file and then renames it, so an interrupted save
 * keeps the previous file.
 */
public final class HudManager {

    public static final MiningHud MINING = new MiningHud();
    public static final DollarsHud DOLLARS = new DollarsHud();
    public static final InventoryHud INVENTORY = new InventoryHud();
    public static final HotbarHud HOTBAR = new HotbarHud();
    public static final PlayerDollHud DOLL = new PlayerDollHud();
    public static final StatsHud STATS = new StatsHud();

    /** Every element, in draw order. */
    public static final HudElement[] ELEMENTS = {MINING, DOLLARS, INVENTORY, HOTBAR, DOLL, STATS};

    public static final String FILE_NAME = "skyvelor-hud.properties";
    public static final String LEGACY_FILE_NAME = "pekaclient-hud.properties";

    private static final Logger LOG = LogManager.getLogger("SkyVeLor");
    private static boolean loaded;

    private HudManager() {
    }

    private static File dir() {
        return Minecraft.getMinecraft().mcDataDir;
    }

    /** Loads settings once. Missing or unreadable files keep the defaults. */
    public static void ensureLoaded() {
        if (!loaded) {
            load();
        }
    }

    public static void load() {
        Properties p = ConfigFiles.read(ConfigFiles.resolveRead(dir(), FILE_NAME, LEGACY_FILE_NAME));
        for (HudElement e : ELEMENTS) {
            e.load(p);
        }
        BalanceSource.manualMode = "manual".equalsIgnoreCase(p.getProperty("balance.source", "auto").trim());
        BalanceSource.manualValue = BalanceSource.parseManual(p.getProperty("balance.manual"));
        MiningTracker.setWindowSeconds(MINING.windowSeconds());
        loaded = true;
    }

    public static void save() {
        Properties p = new Properties();
        for (HudElement e : ELEMENTS) {
            e.save(p);
        }
        p.setProperty("balance.source", BalanceSource.manualMode ? "manual" : "auto");
        if (BalanceSource.manualValue != BalanceSource.UNKNOWN) {
            p.setProperty("balance.manual", Long.toString(BalanceSource.manualValue));
        }
        File f = new File(dir(), FILE_NAME);
        File tmp = new File(f.getParentFile(), f.getName() + ".tmp");
        try (OutputStream out = new FileOutputStream(tmp)) {
            p.store(out, "Sky VeLor HUD settings");
        } catch (IOException e) {
            LOG.warn("Could not write " + tmp, e);
            return;
        }
        if (f.exists() && !f.delete()) {
            LOG.warn("Could not replace " + f + "; the new HUD settings are in " + tmp);
            return;
        }
        if (!tmp.renameTo(f)) {
            LOG.warn("Could not rename " + tmp + " to " + f);
        }
    }

    /** Called once per client tick, before the world moves. */
    public static void tick(Minecraft mc) {
        ensureLoaded();
        MiningTracker.setWindowSeconds(MINING.windowSeconds());
        MiningTracker.tick(mc);
        BalanceSource.tick(mc);
    }

    /** True when the in-game HUD should draw: a world is loaded, and no GUI is open except chat. */
    public static boolean visibleInGame() {
        Minecraft mc = Minecraft.getMinecraft();
        return !mc.gameSettings.hideGUI && mc.thePlayer != null
                && (mc.currentScreen == null || mc.currentScreen instanceof GuiChat);
    }

    /** Draws the enabled elements in game. */
    public static void render(ScaledResolution sr) {
        UIRender.beginFrame();
        for (HudElement e : ELEMENTS) {
            if (e.enabled) {
                e.render(sr.getScaledWidth(), sr.getScaledHeight(), 1f);
            }
        }
        UIRender.endFrame();
    }

    /** Draws every element for the editor. Disabled elements are shown faded. */
    public static void renderEditor(ScaledResolution sr) {
        UIRender.beginFrame();
        for (HudElement e : ELEMENTS) {
            e.render(sr.getScaledWidth(), sr.getScaledHeight(), e.enabled ? 1f : 0.35f);
        }
        UIRender.endFrame();
    }

    public static void resetPositions() {
        for (HudElement e : ELEMENTS) {
            e.resetPosition();
        }
    }

    public static void resetAll() {
        for (HudElement e : ELEMENTS) {
            e.resetAll();
        }
        BalanceSource.manualMode = false;
        BalanceSource.manualValue = BalanceSource.UNKNOWN;
    }
}
