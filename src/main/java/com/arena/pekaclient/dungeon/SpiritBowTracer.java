package com.arena.pekaclient.dungeon;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.feature.DungeonContext;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.WorldBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Teal Lunaria tracer to the closest visible Spirit Bow name-tag stand during a dungeon run. */
public final class SpiritBowTracer {
    private static final double RANGE_SQ = 128.0 * 128.0;
    private long lastEntityScanAt;
    private EntityArmorStand cachedNearest;
    private int cachedWorldIdentity;

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!AppleSettings.dungeonSpiritBowTracer || !DungeonContext.inDungeon() || !WorldBox.ready()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null || mc.thePlayer == null) return;
        long now = System.currentTimeMillis();
        int worldIdentity = System.identityHashCode(mc.theWorld);
        if (now - lastEntityScanAt >= 200L || cachedWorldIdentity != worldIdentity
                || (cachedNearest != null && cachedNearest.isDead)) {
            cachedWorldIdentity = worldIdentity;
            lastEntityScanAt = now;
            cachedNearest = null;
            double best = RANGE_SQ;
            for (Object object : mc.theWorld.loadedEntityList) {
                if (!(object instanceof EntityArmorStand)) continue;
                EntityArmorStand stand = (EntityArmorStand) object;
                String name = stand.getName();
                if (name == null || !name.toLowerCase(java.util.Locale.ROOT).contains("spirit bow")) continue;
                double distance = stand.getDistanceSqToEntity(mc.thePlayer);
                if (distance < best) {
                    best = distance;
                    cachedNearest = stand;
                }
            }
        }
        EntityArmorStand nearest = cachedNearest;
        if (nearest == null || nearest.worldObj != mc.theWorld || nearest.isDead
                || nearest.getDistanceSqToEntity(mc.thePlayer) > RANGE_SQ) return;
        Vec3 eye = mc.thePlayer.getPositionEyes(event.partialTicks);
        double targetX = nearest.lastTickPosX + (nearest.posX - nearest.lastTickPosX) * event.partialTicks;
        double targetY = nearest.lastTickPosY + (nearest.posY - nearest.lastTickPosY) * event.partialTicks + 0.45D;
        double targetZ = nearest.lastTickPosZ + (nearest.posZ - nearest.lastTickPosZ) * event.partialTicks;
        RenderManager rm = mc.getRenderManager();
        boolean open = false;
        try {
            WorldBox.begin(rm.viewerPosX, rm.viewerPosY, rm.viewerPosZ, true);
            open = true;
            WorldBox.line(eye.xCoord, eye.yCoord, eye.zCoord, targetX, targetY, targetZ,
                    Theme.accent | 0xFF000000);
            WorldBox.outline(targetX - 0.18, targetY - 0.28, targetZ - 0.18,
                    targetX + 0.18, targetY + 0.18, targetZ + 0.18, Theme.accent | 0xFF000000);
        } finally {
            if (open) WorldBox.end();
        }
    }
}
