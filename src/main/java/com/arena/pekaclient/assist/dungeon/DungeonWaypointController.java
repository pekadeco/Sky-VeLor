package com.arena.pekaclient.assist.dungeon;

import com.arena.pekaclient.config.AssistSettings;
import com.arena.pekaclient.feature.DungeonContext;
import com.arena.pekaclient.render.WorldBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Read-only local waypoint cache for explicitly named inactive F7/M7 terminal/device entities. It scans at a fixed
 * low rate and caps its result set; unrelated entities, unnamed stands, and all interaction remain vanilla.
 */
public final class DungeonWaypointController {
    private static final int SCAN_INTERVAL_TICKS = 10;
    private static final int MAX_WAYPOINTS = 32;
    private static final double MAX_DISTANCE_SQ = 22500D;
    private static final int TERMINAL_COLOR = 0xD05CA8FF;
    private static final int DEVICE_COLOR = 0xD0FFD166;

    private List<Waypoint> cached = Collections.emptyList();
    private int ticksUntilScan;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.START) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!AssistSettings.terminalWaypoints || !DungeonContext.inF7OrM7() || mc == null || mc.theWorld == null
                || mc.thePlayer == null) {
            cached = Collections.emptyList();
            ticksUntilScan = 0;
            return;
        }
        if (++ticksUntilScan < SCAN_INTERVAL_TICKS) return;
        ticksUntilScan = 0;
        ArrayList<Waypoint> found = new ArrayList<Waypoint>();
        for (Object object : mc.theWorld.loadedEntityList) {
            if (!(object instanceof Entity) || found.size() >= MAX_WAYPOINTS) continue;
            Entity entity = (Entity) object;
            if (!(entity instanceof EntityArmorStand) && !(entity instanceof EntityItemFrame)) continue;
            if (mc.thePlayer.getDistanceSqToEntity(entity) > MAX_DISTANCE_SQ) continue;
            String label = label(entity);
            boolean terminal = isInactiveTerminal(label);
            boolean device = isInactiveDevice(label);
            if (!terminal && !device) continue;
            found.add(new Waypoint(entity.posX, entity.posY, entity.posZ, terminal ? TERMINAL_COLOR : DEVICE_COLOR));
        }
        cached = found.isEmpty() ? Collections.<Waypoint>emptyList() : found;
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        if (event == null || !AssistSettings.terminalWaypoints || cached.isEmpty() || !WorldBox.ready()) return;
        Minecraft mc = Minecraft.getMinecraft();
        RenderManager manager = mc == null ? null : mc.getRenderManager();
        if (manager == null) return;
        WorldBox.begin(manager.viewerPosX, manager.viewerPosY, manager.viewerPosZ, true);
        try {
            for (Waypoint waypoint : cached) {
                WorldBox.outline(waypoint.x - .32D, waypoint.y, waypoint.z - .32D,
                        waypoint.x + .32D, waypoint.y + 1.12D, waypoint.z + .32D, waypoint.color);
            }
        } finally {
            WorldBox.end();
        }
    }

    private static String label(Entity entity) {
        String value = entity.hasCustomName() ? entity.getCustomNameTag() : "";
        if ((value == null || value.length() == 0) && entity instanceof EntityItemFrame) {
            ItemStack item = ((EntityItemFrame) entity).getDisplayedItem();
            value = item == null ? "" : item.getDisplayName();
        }
        return strip(value).toLowerCase(Locale.ROOT);
    }

    private static boolean isInactiveTerminal(String label) {
        return label.indexOf("terminal") >= 0 && (label.indexOf("inactive") >= 0 || label.indexOf("not started") >= 0);
    }

    private static boolean isInactiveDevice(String label) {
        return label.indexOf("device") >= 0 && (label.indexOf("inactive") >= 0 || label.indexOf("not started") >= 0);
    }

    private static String strip(String value) {
        if (value == null) return "";
        StringBuilder result = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            if (value.charAt(index) == '\u00a7' && index + 1 < value.length()) index++;
            else result.append(value.charAt(index));
        }
        return result.toString();
    }

    private static final class Waypoint {
        final double x;
        final double y;
        final double z;
        final int color;

        Waypoint(double x, double y, double z, int color) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.color = color;
        }
    }
}
