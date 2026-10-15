package com.arena.pekaclient.dungeon;

import com.arena.pekaclient.dungeon.M7OverlayRenderer.M7DragonStatue;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Calls out the M7 dragon that is close to you and parked at its own statue, as a title in the
 * dragon's colour. Only runs during a Floor VII run. Ported from RNG Carried Bald.
 */
public final class DragonKillAlert {

    static final double PLAYER_RANGE = 20.0D;
    static final double STATUE_RANGE = 15.0D;
    static final long HOLD_MS = 900L;
    static final long RETITLE_MS = 1200L;

    private final M7OverlayRenderer overlay;
    private long alertUntil;
    private long titleAt;
    private M7DragonStatue target;

    public DragonKillAlert(M7OverlayRenderer overlay) {
        this.overlay = overlay;
    }

    /** Call once per client tick. */
    public void tick(long now) {
        if (!M7Settings.isDragonKillAlertEnabled() || !overlay.isFloorSevenScoreboardDetected()) {
            target = null;
            alertUntil = 0L;
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.theWorld == null || minecraft.thePlayer == null) {
            return;
        }
        List<DragonSighting> sightings = new ArrayList<DragonSighting>();
        for (Object loaded : minecraft.theWorld.loadedEntityList) {
            if (!(loaded instanceof Entity)) {
                continue;
            }
            Entity entity = (Entity) loaded;
            String name = entity instanceof EntityLivingBase ? entity.getName() : null;
            M7DragonStatue statue = statueForDragon(name, entity.posX, entity.posY, entity.posZ,
                    entity instanceof EntityDragon);
            if (statue != null) {
                sightings.add(new DragonSighting(statue, entity.posX, entity.posY, entity.posZ));
            }
        }
        M7DragonStatue picked = pickTarget(minecraft.thePlayer.posX, minecraft.thePlayer.posY,
                minecraft.thePlayer.posZ, sightings);
        if (picked == null) {
            if (now > alertUntil) {
                target = null;
            }
            return;
        }
        alertUntil = now + HOLD_MS;
        if (picked != target || now - titleAt >= RETITLE_MS) {
            showTitle(picked);
            target = picked;
            titleAt = now;
        }
    }

    private static void showTitle(M7DragonStatue statue) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.ingameGUI == null) {
            return;
        }
        String name = statue.dragonName.replace(" Dragon", "").toUpperCase(Locale.ROOT);
        minecraft.ingameGUI.displayTitle(colorCode(statue) + "\u00A7lKILL " + name + " DRAGON",
                "", 2, 18, 4);
    }

    /**
     * The dragon to call out: one that is both close to the player and sitting at its own statue.
     * When several qualify the closest to the player wins.
     */
    static M7DragonStatue pickTarget(double px, double py, double pz, List<DragonSighting> sightings) {
        M7DragonStatue best = null;
        double bestDistance = PLAYER_RANGE * PLAYER_RANGE;
        for (DragonSighting s : sightings) {
            double dx = s.x - px;
            double dy = s.y - py;
            double dz = s.z - pz;
            double toPlayer = dx * dx + dy * dy + dz * dz;
            if (toPlayer > PLAYER_RANGE * PLAYER_RANGE) {
                continue;
            }
            if (distanceSqToAnchor(s.statue, s.x, s.y, s.z) > STATUE_RANGE * STATUE_RANGE) {
                continue;
            }
            if (toPlayer <= bestDistance) {
                bestDistance = toPlayer;
                best = s.statue;
            }
        }
        return best;
    }

    /**
     * Which statue a dragon belongs to. A name that matches one of the five M7 dragons wins. A dragon
     * entity with no readable name falls back to the nearest statue anchor it is parked at.
     */
    static M7DragonStatue statueForDragon(String entityName, double x, double y, double z,
                                          boolean dragonEntity) {
        if (entityName != null) {
            for (M7DragonStatue statue : M7DragonStatue.values()) {
                if (entityName.equalsIgnoreCase(statue.dragonName)) {
                    return statue;
                }
            }
        }
        if (!dragonEntity) {
            return null;
        }
        M7DragonStatue nearest = null;
        double bestDistance = STATUE_RANGE * STATUE_RANGE;
        for (M7DragonStatue statue : M7DragonStatue.values()) {
            double distance = distanceSqToAnchor(statue, x, y, z);
            if (distance <= bestDistance) {
                bestDistance = distance;
                nearest = statue;
            }
        }
        return nearest;
    }

    private static double distanceSqToAnchor(M7DragonStatue statue, double x, double y, double z) {
        double dx = statue.anchor.getX() + 0.5D - x;
        double dy = statue.anchor.getY() - y;
        double dz = statue.anchor.getZ() + 0.5D - z;
        return dx * dx + dy * dy + dz * dz;
    }

    static String colorCode(M7DragonStatue statue) {
        switch (statue) {
            case POWER: return "\u00A7c";
            case APEX: return "\u00A7a";
            case SOUL: return "\u00A7d";
            case ICE: return "\u00A7b";
            case FLAME: return "\u00A76";
            default: return "\u00A7f";
        }
    }

    static final class DragonSighting {
        final M7DragonStatue statue;
        final double x;
        final double y;
        final double z;

        DragonSighting(M7DragonStatue statue, double x, double y, double z) {
            this.statue = statue;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
