package com.arena.pekaclient.render;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.feature.DungeonContext;
import com.arena.pekaclient.feature.MobMarkers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Outlines dungeon mobs. A dungeon mob has its name tag on a separate armour stand that floats above it, so the
 * name tag is not the mob. For each name tag that {@link MobMarkers} classifies (a level tag, or the elite star),
 * the mob standing under it is found and its own bounding box is outlined. A living entity that carries the name
 * tag itself is outlined directly.
 *
 * It is drawn in the world overlay pass, after the world, so entity renderers, models, layers and visibility are
 * not touched. It runs only inside a dungeon.
 *
 * Cost: one pass over the loaded entities each frame, a string check only on name tags within {@link #RANGE}, and
 * a short search for the mob under each marked tag.
 */
public final class MobHighlight {

    /** Mobs further away than this are skipped. */
    private static final double RANGE = 64.0;
    private static final double RANGE_SQ = RANGE * RANGE;
    /** Horizontal distance allowed between a name tag and the mob it belongs to. */
    private static final double MOB_REACH = 2.5;
    private static final double MOB_REACH_SQ = MOB_REACH * MOB_REACH;
    /** Vertical range of the mob's feet relative to the name tag: the tag floats above the mob's head. */
    private static final double MOB_BELOW_TAG = 4.0;
    private static final double MOB_ABOVE_TAG = -1.0;
    private static final int NORMAL_COLOR = 0xFF7CFC8A;
    private static final int ELITE_COLOR = 0xFFFFD84D;

    private MobHighlight() {
    }

    /** Colour for a mob class, or 0 when the mob is not highlighted. */
    static int colorFor(MobMarkers.Kind kind) {
        switch (kind) {
            case ELITE:
                return ELITE_COLOR;
            case NORMAL:
                return NORMAL_COLOR;
            default:
                return 0;
        }
    }

    /** Called from RenderWorldLastEvent. */
    public static void draw(float partialTicks) {
        if (!AppleSettings.mobHighlights || !DungeonContext.inDungeon() || !WorldBox.ready()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;
        RenderManager rm = mc.getRenderManager();
        List<Entity> loaded = mc.theWorld.loadedEntityList;
        Set<Entity> drawn = Collections.newSetFromMap(new IdentityHashMap<Entity, Boolean>());
        boolean open = false;
        for (int i = 0, n = loaded.size(); i < n; i++) {
            Entity tag = loaded.get(i);
            if (!(tag instanceof EntityLivingBase) || tag instanceof EntityPlayer || tag == player || tag.isDead) {
                continue;
            }
            if (tag.getDistanceSqToEntity(player) > RANGE_SQ) {
                continue;
            }
            String name = tag.getCustomNameTag();
            if (name == null || name.isEmpty()) {
                continue;
            }
            int colour = colorFor(MobMarkers.classify(name));
            if (colour == 0) {
                continue;
            }
            Entity mob = tag instanceof EntityArmorStand ? findMobUnder(loaded, tag) : tag;
            if (mob == null || !drawn.add(mob)) {
                continue;
            }
            if (!open) {
                // Through walls, so a mob behind a pillar is still marked. The overlay is drawn on top only.
                WorldBox.begin(rm.viewerPosX, rm.viewerPosY, rm.viewerPosZ, true);
                open = true;
            }
            // Follow the entity's interpolated position for this frame, so the outline does not lag.
            double ox = mob.lastTickPosX + (mob.posX - mob.lastTickPosX) * partialTicks - mob.posX;
            double oy = mob.lastTickPosY + (mob.posY - mob.lastTickPosY) * partialTicks - mob.posY;
            double oz = mob.lastTickPosZ + (mob.posZ - mob.lastTickPosZ) * partialTicks - mob.posZ;
            AxisAlignedBB bb = mob.getEntityBoundingBox();
            WorldBox.outline(bb.minX + ox, bb.minY + oy, bb.minZ + oz,
                    bb.maxX + ox, bb.maxY + oy, bb.maxZ + oz, colour);
        }
        if (open) {
            WorldBox.end();
        }
    }

    /**
     * The mob that a floating name tag belongs to: the nearest living, non-player, non-stand entity that is close
     * to the tag horizontally and sits below it. Returns null when none is close enough.
     */
    static Entity findMobUnder(List<Entity> loaded, Entity tag) {
        Entity best = null;
        double bestD = Double.MAX_VALUE;
        for (int i = 0, n = loaded.size(); i < n; i++) {
            Entity m = loaded.get(i);
            if (m == tag || m.isDead || !(m instanceof EntityLivingBase)
                    || m instanceof EntityPlayer || m instanceof EntityArmorStand) {
                continue;
            }
            double dx = m.posX - tag.posX;
            double dz = m.posZ - tag.posZ;
            double horizontal = dx * dx + dz * dz;
            if (horizontal > MOB_REACH_SQ) {
                continue;
            }
            double dy = tag.posY - m.posY;
            if (dy < MOB_ABOVE_TAG || dy > MOB_BELOW_TAG) {
                continue;
            }
            double d = horizontal + dy * dy;
            if (d < bestD) {
                bestD = d;
                best = m;
            }
        }
        return best;
    }
}
