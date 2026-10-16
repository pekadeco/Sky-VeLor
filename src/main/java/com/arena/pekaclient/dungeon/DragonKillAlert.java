package com.arena.pekaclient.dungeon;

import com.arena.pekaclient.dungeon.M7OverlayRenderer.M7DragonStatue;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Calls out the M7 dragon that is close to you and parked at its own statue, as a title in the
 * dragon's colour. Only runs during a Floor VII run. Ported from RNG Carried Bald.
 */
public final class DragonKillAlert {

    static final double PLAYER_RANGE = 30.0D;
    static final double STATUE_RANGE = 25.0D;
    static final double TAG_RANGE = 8.0D;
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
        // Hypixel puts each dragon's name above it on its own entity, e.g. "Power Dragon 48.5M". Those name
        // tags say which dragon is where. A dragon is matched to the nearest tag, and only falls back to the
        // statue it is parked at when no tags are in the world.
        List<DragonTag> tags = new ArrayList<DragonTag>();
        List<Entity> dragons = new ArrayList<Entity>();
        for (Object loaded : minecraft.theWorld.loadedEntityList) {
            if (!(loaded instanceof Entity)) {
                continue;
            }
            Entity entity = (Entity) loaded;
            if (entity instanceof EntityDragon) {
                dragons.add(entity);
                continue;
            }
            if (!(entity instanceof EntityLivingBase)) {
                continue;
            }
            M7DragonStatue tagged = statueForTag(entity.getName());
            if (tagged != null) {
                tags.add(new DragonTag(tagged, entity.posX, entity.posY, entity.posZ));
            }
        }
        List<DragonSighting> sightings = new ArrayList<DragonSighting>();
        for (Entity dragon : dragons) {
            M7DragonStatue statue = tags.isEmpty()
                    ? statueForDragon(null, dragon.posX, dragon.posY, dragon.posZ, true)
                    : statueNearTag(tags, dragon.posX, dragon.posY, dragon.posZ);
            if (statue != null) {
                sightings.add(new DragonSighting(statue, dragon.posX, dragon.posY, dragon.posZ));
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

    /** The dragon named on a name tag, e.g. "Power Dragon 48.5M" gives POWER. Colour codes are ignored. */
    static M7DragonStatue statueForTag(String tagText) {
        if (tagText == null) {
            return null;
        }
        String plain = EnumChatFormatting.getTextWithoutFormattingCodes(tagText);
        if (plain == null) {
            return null;
        }
        String lower = plain.toLowerCase(Locale.ROOT);
        for (M7DragonStatue statue : M7DragonStatue.values()) {
            if (lower.contains(statue.dragonName.toLowerCase(Locale.ROOT))) {
                return statue;
            }
        }
        return null;
    }

    /** The statue of the nearest name tag within a few blocks, or null when no tag is that close. */
    static M7DragonStatue statueNearTag(List<DragonTag> tags, double x, double y, double z) {
        M7DragonStatue best = null;
        double bestDistance = TAG_RANGE * TAG_RANGE;
        for (DragonTag t : tags) {
            double dx = t.x - x;
            double dy = t.y - y;
            double dz = t.z - z;
            double d = dx * dx + dy * dy + dz * dz;
            if (d <= bestDistance) {
                bestDistance = d;
                best = t.statue;
            }
        }
        return best;
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

    static final class DragonTag {
        final M7DragonStatue statue;
        final double x;
        final double y;
        final double z;

        DragonTag(M7DragonStatue statue, double x, double y, double z) {
            this.statue = statue;
            this.x = x;
            this.y = y;
            this.z = z;
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
