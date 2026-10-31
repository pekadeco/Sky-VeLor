package com.arena.pekaclient.assist.dungeon;

import com.arena.pekaclient.config.AssistSettings;
import com.arena.pekaclient.feature.DungeonContext;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.WorldBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Read-only dungeon-world locator for the nearest visible Spirit Bow marker.
 *
 * <p>The locator observes loaded armor stands on a short timer and draws a client-side line/outline only. It never
 * changes player state, clicks, sends a packet, uses a controller, or alters an entity.</p>
 */
public final class SpiritBowLocator {
    private static final long RESCAN_INTERVAL_MS = 200L;
    private static final double MAX_RANGE = 128D;
    private static final double MAX_RANGE_SQ = MAX_RANGE * MAX_RANGE;
    private static final String MARKER_TEXT = "Spirit Bow";

    private static volatile boolean registered;

    private EntityArmorStand nearest;
    private Object scannedWorld;
    private long lastScanAt;

    /** Records normal Forge registration for the opt-in /lunaria self-test report. */
    public static void markRegistered() {
        registered = true;
    }

    /** The hook is a normal Forge render subscriber; there is no transformer or networking surface to validate. */
    public static String selfTestStatus() {
        return registered ? "ACTIVE (Forge render hook; client-side visual only)" : "UNREGISTERED";
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!AssistSettings.spiritBowLocator || !DungeonContext.inDungeon() || !WorldBox.ready()) {
            clear();
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null || mc.thePlayer == null || mc.getRenderManager() == null) {
            clear();
            return;
        }

        refreshNearest(mc, System.currentTimeMillis());
        EntityArmorStand marker = nearest;
        if (!isUsable(marker, mc)) return;

        float partialTicks = event == null ? 0F : event.partialTicks;
        EntityPlayerSP player = mc.thePlayer;
        double eyeX = interpolate(player.lastTickPosX, player.posX, partialTicks);
        double eyeY = interpolate(player.lastTickPosY, player.posY, partialTicks) + player.getEyeHeight();
        double eyeZ = interpolate(player.lastTickPosZ, player.posZ, partialTicks);
        double targetX = interpolate(marker.lastTickPosX, marker.posX, partialTicks);
        double targetY = interpolate(marker.lastTickPosY, marker.posY, partialTicks) + .45D;
        double targetZ = interpolate(marker.lastTickPosZ, marker.posZ, partialTicks);
        RenderManager renderManager = mc.getRenderManager();
        WorldBox.begin(renderManager.viewerPosX, renderManager.viewerPosY, renderManager.viewerPosZ, true);
        try {
            int color = Theme.accent | 0xFF000000;
            WorldBox.line(eyeX, eyeY, eyeZ, targetX, targetY, targetZ, color);
            WorldBox.outline(targetX - .18D, targetY - .28D, targetZ - .18D,
                    targetX + .18D, targetY + .18D, targetZ + .18D, color);
        } finally {
            WorldBox.end();
        }
    }

    private void refreshNearest(Minecraft mc, long now) {
        if (scannedWorld != mc.theWorld) {
            scannedWorld = mc.theWorld;
            nearest = null;
            lastScanAt = 0L;
        }
        if (now - lastScanAt < RESCAN_INTERVAL_MS && isUsable(nearest, mc)) return;
        lastScanAt = now;
        nearest = null;
        double bestDistance = MAX_RANGE_SQ;
        for (int index = 0; index < mc.theWorld.loadedEntityList.size(); index++) {
            Object candidate = mc.theWorld.loadedEntityList.get(index);
            if (!(candidate instanceof EntityArmorStand)) continue;
            EntityArmorStand stand = (EntityArmorStand) candidate;
            if (stand.isDead || !isSpiritBowMarker(stand.getName())) continue;
            double distance = stand.getDistanceSqToEntity(mc.thePlayer);
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = stand;
            }
        }
    }

    private boolean isUsable(EntityArmorStand stand, Minecraft mc) {
        return stand != null && !stand.isDead && stand.worldObj == mc.theWorld
                && stand.getDistanceSqToEntity(mc.thePlayer) <= MAX_RANGE_SQ;
    }

    private void clear() {
        nearest = null;
        scannedWorld = null;
        lastScanAt = 0L;
    }

    static boolean isSpiritBowMarker(String value) {
        if (value == null || value.length() < MARKER_TEXT.length()) return false;
        for (int index = 0; index <= value.length() - MARKER_TEXT.length(); index++) {
            if (value.regionMatches(true, index, MARKER_TEXT, 0, MARKER_TEXT.length())) return true;
        }
        return false;
    }

    private static double interpolate(double before, double after, float partialTicks) {
        return before + (after - before) * partialTicks;
    }
}
