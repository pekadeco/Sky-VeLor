package com.arena.pekaclient.dungeon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import org.lwjgl.input.Keyboard;

import java.util.Properties;

/**
 * Switches for the Floor VII (M7) dungeon features: the world overlay, boss and dragon hitboxes,
 * statue kill areas, and the dragon kill alert. Saved in the main Sky VeLor settings file, under
 * {@code dungeon.m7.*} keys.
 */
public final class M7Settings {

    private static boolean overlayEnabled = false;
    private static boolean maxorEnabled = false;
    private static boolean stormEnabled = false;
    private static boolean goldorEnabled = false;
    private static boolean necronEnabled = false;
    private static boolean powerDragonEnabled = false;
    private static boolean apexDragonEnabled = false;
    private static boolean soulDragonEnabled = false;
    private static boolean iceDragonEnabled = false;
    private static boolean flameDragonEnabled = false;
    private static boolean statueZonesEnabled = false;
    private static boolean areasPersistAfterDragonSpawn = false;
    private static boolean dragonKillAlertEnabled = false;
    private static boolean terminalSolverEnabled = false;

    private M7Settings() {
    }

    /**
     * Floor VII overlays/solvers are specialized opt-in tools on a fresh install. A populated configuration from an
     * earlier build retains its historic default for keys it never wrote, while every explicit saved value wins.
     */
    public static void load(Properties p) {
        Properties source = p == null ? new Properties() : p;
        boolean legacyFallback = !source.isEmpty();
        overlayEnabled = bool(source, "dungeon.m7.overlay", legacyFallback);
        maxorEnabled = bool(source, "dungeon.m7.boss.maxor", legacyFallback);
        stormEnabled = bool(source, "dungeon.m7.boss.storm", legacyFallback);
        goldorEnabled = bool(source, "dungeon.m7.boss.goldor", legacyFallback);
        necronEnabled = bool(source, "dungeon.m7.boss.necron", legacyFallback);
        powerDragonEnabled = bool(source, "dungeon.m7.dragon.power", legacyFallback);
        apexDragonEnabled = bool(source, "dungeon.m7.dragon.apex", legacyFallback);
        soulDragonEnabled = bool(source, "dungeon.m7.dragon.soul", legacyFallback);
        iceDragonEnabled = bool(source, "dungeon.m7.dragon.ice", legacyFallback);
        flameDragonEnabled = bool(source, "dungeon.m7.dragon.flame", legacyFallback);
        statueZonesEnabled = bool(source, "dungeon.m7.statueZones", legacyFallback);
        areasPersistAfterDragonSpawn = bool(source, "dungeon.m7.areasPersist", legacyFallback);
        dragonKillAlertEnabled = bool(source, "dungeon.m7.dragonKillAlert", legacyFallback);
        terminalSolverEnabled = bool(source, "dungeon.m7.terminalSolver", legacyFallback);
    }

    public static void save(Properties p) {
        p.setProperty("dungeon.m7.overlay", String.valueOf(overlayEnabled));
        p.setProperty("dungeon.m7.boss.maxor", String.valueOf(maxorEnabled));
        p.setProperty("dungeon.m7.boss.storm", String.valueOf(stormEnabled));
        p.setProperty("dungeon.m7.boss.goldor", String.valueOf(goldorEnabled));
        p.setProperty("dungeon.m7.boss.necron", String.valueOf(necronEnabled));
        p.setProperty("dungeon.m7.dragon.power", String.valueOf(powerDragonEnabled));
        p.setProperty("dungeon.m7.dragon.apex", String.valueOf(apexDragonEnabled));
        p.setProperty("dungeon.m7.dragon.soul", String.valueOf(soulDragonEnabled));
        p.setProperty("dungeon.m7.dragon.ice", String.valueOf(iceDragonEnabled));
        p.setProperty("dungeon.m7.dragon.flame", String.valueOf(flameDragonEnabled));
        p.setProperty("dungeon.m7.statueZones", String.valueOf(statueZonesEnabled));
        p.setProperty("dungeon.m7.areasPersist", String.valueOf(areasPersistAfterDragonSpawn));
        p.setProperty("dungeon.m7.dragonKillAlert", String.valueOf(dragonKillAlertEnabled));
        p.setProperty("dungeon.m7.terminalSolver", String.valueOf(terminalSolverEnabled));
    }

    // ----------------------------------------------------------------- accessors
    public static boolean isOverlayEnabled() { return overlayEnabled; }
    public static void setOverlayEnabled(boolean v) { overlayEnabled = v; }
    public static boolean isStatueZonesEnabled() { return statueZonesEnabled; }
    public static void setStatueZonesEnabled(boolean v) { statueZonesEnabled = v; }
    public static boolean isAreasPersistAfterDragonSpawn() { return areasPersistAfterDragonSpawn; }
    public static void setAreasPersistAfterDragonSpawn(boolean v) { areasPersistAfterDragonSpawn = v; }
    public static boolean isDragonKillAlertEnabled() { return dragonKillAlertEnabled; }
    public static void setDragonKillAlertEnabled(boolean v) { dragonKillAlertEnabled = v; }
    public static boolean isTerminalSolverEnabled() { return terminalSolverEnabled; }
    public static void setTerminalSolverEnabled(boolean v) { terminalSolverEnabled = v; }

    public static boolean isBossHitboxEnabled(M7OverlayRenderer.M7HitboxTarget target) {
        if (target == null) {
            return false;
        }
        switch (target) {
            case MAXOR: return maxorEnabled;
            case STORM: return stormEnabled;
            case GOLDOR: return goldorEnabled;
            case NECRON: return necronEnabled;
            default: return false;
        }
    }

    public static void setBossHitboxEnabled(M7OverlayRenderer.M7HitboxTarget target, boolean v) {
        if (target == null) {
            return;
        }
        switch (target) {
            case MAXOR: maxorEnabled = v; break;
            case STORM: stormEnabled = v; break;
            case GOLDOR: goldorEnabled = v; break;
            case NECRON: necronEnabled = v; break;
            default: break;
        }
    }

    public static boolean isDragonHitboxEnabled(M7OverlayRenderer.M7DragonStatue statue) {
        if (statue == null) {
            return false;
        }
        switch (statue) {
            case POWER: return powerDragonEnabled;
            case APEX: return apexDragonEnabled;
            case SOUL: return soulDragonEnabled;
            case ICE: return iceDragonEnabled;
            case FLAME: return flameDragonEnabled;
            default: return false;
        }
    }

    public static void setDragonHitboxEnabled(M7OverlayRenderer.M7DragonStatue statue, boolean v) {
        if (statue == null) {
            return;
        }
        switch (statue) {
            case POWER: powerDragonEnabled = v; break;
            case APEX: apexDragonEnabled = v; break;
            case SOUL: soulDragonEnabled = v; break;
            case ICE: iceDragonEnabled = v; break;
            case FLAME: flameDragonEnabled = v; break;
            default: break;
        }
    }

    /** True when the overlay should draw: enabled, and no chat or tab list covering the view. */
    public static boolean shouldRenderWorldOverlay() {
        if (!overlayEnabled) {
            return false;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.gameSettings == null) {
            return false;
        }
        if (minecraft.currentScreen instanceof GuiChat) {
            return false;
        }
        return !isPlayerListKeyDown(minecraft);
    }

    private static boolean isPlayerListKeyDown(Minecraft minecraft) {
        if (minecraft.gameSettings.keyBindPlayerList == null) {
            return false;
        }
        if (minecraft.gameSettings.keyBindPlayerList.isKeyDown()) {
            return true;
        }
        int keyCode = minecraft.gameSettings.keyBindPlayerList.getKeyCode();
        return keyCode > 0 && Keyboard.isKeyDown(keyCode);
    }

    private static boolean bool(Properties p, String key, boolean def) {
        String v = p.getProperty(key);
        return v == null ? def : Boolean.parseBoolean(v.trim());
    }
}
