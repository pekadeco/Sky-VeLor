package com.arena.pekaclient.render;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.feature.BloodMobMatcher;
import com.arena.pekaclient.feature.DungeonContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Highlights known blood-room minibosses without changing vanilla entity rendering. */
public final class BloodMobHighlight {
    private static final int REFRESH_EVERY_TICKS = 5;
    private static final double RANGE = 64.0D;
    private static final double RANGE_SQ = RANGE * RANGE;
    private static final double TAG_HORIZONTAL_REACH = 1.25D;
    private static final double TAG_HORIZONTAL_REACH_SQ = TAG_HORIZONTAL_REACH * TAG_HORIZONTAL_REACH;
    private static final double TAG_MIN_Y_OFFSET = -0.5D;
    private static final double TAG_MAX_Y_OFFSET = 4.5D;
    private static final int DEFAULT_COLOR = 0xFF00FF00;

    private final List<EntityLivingBase> cachedMobs = new ArrayList<EntityLivingBase>();
    private World cachedWorld;
    private int tickCounter;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (!AppleSettings.bloodMobHighlight || mc == null || mc.theWorld == null
                || mc.thePlayer == null || !isFakepixelDungeon(mc)) {
            clear();
            return;
        }
        if (cachedWorld != mc.theWorld) {
            clear();
            cachedWorld = mc.theWorld;
        }
        if (++tickCounter >= REFRESH_EVERY_TICKS) {
            tickCounter = 0;
            rebuildCache(mc);
        }
    }

    private void rebuildCache(Minecraft mc) {
        cachedMobs.clear();
        World world = mc.theWorld;
        if (world == null || mc.thePlayer == null) {
            return;
        }

        List<Entity> loaded = world.loadedEntityList;
        List<EntityLivingBase> living = new ArrayList<EntityLivingBase>();
        Set<EntityLivingBase> added = Collections.newSetFromMap(
                new IdentityHashMap<EntityLivingBase, Boolean>());

        for (int i = 0; i < loaded.size(); i++) {
            Entity entity = loaded.get(i);
            if (!(entity instanceof EntityLivingBase) || entity instanceof EntityPlayer
                    || entity instanceof EntityArmorStand || entity.isDead
                    || entity.getDistanceSqToEntity(mc.thePlayer) > RANGE_SQ) {
                continue;
            }
            EntityLivingBase mob = (EntityLivingBase) entity;
            living.add(mob);
            if (hasBloodMobName(entity)) {
                addOnce(cachedMobs, mob, added);
            }
        }

        // Some server versions keep the named label on an invisible armour stand rather than on the mob.
        for (int i = 0; i < loaded.size(); i++) {
            Entity label = loaded.get(i);
            if (!(label instanceof EntityArmorStand) || label.isDead
                    || label.getDistanceSqToEntity(mc.thePlayer) > RANGE_SQ
                    || !hasBloodMobName(label)) {
                continue;
            }
            EntityLivingBase body = findBodyForLabel(label, living);
            if (body != null) {
                addOnce(cachedMobs, body, added);
            }
        }
    }

    private static EntityLivingBase findBodyForLabel(Entity label, List<EntityLivingBase> living) {
        EntityLivingBase best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < living.size(); i++) {
            EntityLivingBase candidate = living.get(i);
            if (candidate == label || candidate.isDead) {
                continue;
            }
            double dx = candidate.posX - label.posX;
            double dz = candidate.posZ - label.posZ;
            double horizontalSq = dx * dx + dz * dz;
            if (horizontalSq > TAG_HORIZONTAL_REACH_SQ) {
                continue;
            }
            double vertical = label.posY - candidate.posY;
            if (vertical < TAG_MIN_Y_OFFSET || vertical > TAG_MAX_Y_OFFSET) {
                continue;
            }
            double distance = horizontalSq + vertical * vertical;
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static boolean hasBloodMobName(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (BloodMobMatcher.isBloodMob(entity.getCustomNameTag())
                || BloodMobMatcher.isBloodMob(entity.getName())) {
            return true;
        }
        return entity.getDisplayName() != null
                && BloodMobMatcher.isBloodMob(entity.getDisplayName().getUnformattedText());
    }

    private static void addOnce(List<EntityLivingBase> cache, EntityLivingBase mob,
                                Set<EntityLivingBase> added) {
        if (mob != null && added.add(mob)) {
            cache.add(mob);
        }
    }

    @SubscribeEvent
    public void onWorldRender(RenderWorldLastEvent event) {
        if (event == null || !AppleSettings.bloodMobHighlight || cachedMobs.isEmpty()
                || !WorldBox.ready()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null || mc.thePlayer == null || cachedWorld != mc.theWorld
                || !isFakepixelDungeon(mc)) {
            return;
        }
        RenderManager rm = mc.getRenderManager();
        if (rm == null) {
            return;
        }
        int color = parseColor(AppleSettings.bloodMobHighlightColor);
        WorldBox.begin(rm.viewerPosX, rm.viewerPosY, rm.viewerPosZ, true);
        try {
            for (int i = 0; i < cachedMobs.size(); i++) {
                EntityLivingBase mob = cachedMobs.get(i);
                if (mob == null || mob.isDead || mob.worldObj != mc.theWorld
                        || mob.getDistanceSqToEntity(mc.thePlayer) > RANGE_SQ) {
                    continue;
                }
                double ox = mob.lastTickPosX + (mob.posX - mob.lastTickPosX) * event.partialTicks - mob.posX;
                double oy = mob.lastTickPosY + (mob.posY - mob.lastTickPosY) * event.partialTicks - mob.posY;
                double oz = mob.lastTickPosZ + (mob.posZ - mob.lastTickPosZ) * event.partialTicks - mob.posZ;
                AxisAlignedBB box = mob.getEntityBoundingBox();
                if (box != null) {
                    WorldBox.outline(box.minX + ox, box.minY + oy, box.minZ + oz,
                            box.maxX + ox, box.maxY + oy, box.maxZ + oz, color);
                }
            }
        } finally {
            WorldBox.end();
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event != null && event.world != null && event.world.isRemote) {
            clear();
        }
    }

    private void clear() {
        cachedMobs.clear();
        cachedWorld = null;
        tickCounter = 0;
    }

    private static boolean isFakepixelDungeon(Minecraft mc) {
        if (!DungeonContext.inDungeon()) {
            return false;
        }
        ServerData server = mc.getCurrentServerData();
        return server != null && server.serverIP != null
                && server.serverIP.toLowerCase(Locale.ROOT).contains("fakepixel");
    }

    private static int parseColor(String color) {
        if (color != null) {
            String[] parts = color.trim().split(":");
            if (parts.length == 5) {
                try {
                    int alpha = Integer.parseInt(parts[1]);
                    int red = Integer.parseInt(parts[2]);
                    int green = Integer.parseInt(parts[3]);
                    int blue = Integer.parseInt(parts[4]);
                    if (alpha >= 0 && alpha <= 255 && red >= 0 && red <= 255
                            && green >= 0 && green <= 255 && blue >= 0 && blue <= 255) {
                        return alpha << 24 | red << 16 | green << 8 | blue;
                    }
                } catch (NumberFormatException ignored) {
                    return DEFAULT_COLOR;
                }
            }
        }
        return DEFAULT_COLOR;
    }
}
