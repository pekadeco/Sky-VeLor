package com.arena.pekaclient.dungeon;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.feature.DungeonContext;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.WorldBox;
import net.minecraft.block.Block;
import net.minecraft.block.BlockColored;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.BlockPos;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Locally scans nearby dungeon rooms for the 3x4x3 coal/red-clay Wither Door structure and draws
 * Lunaria wireframes. Scan work is throttled; this does not change blocks or send packets.
 */
public final class WitherDoorHighlight {
    private static final int DOOR_Y = 69;
    private static final int RANGE = 40;
    private static final long SCAN_INTERVAL_MS = 1250L;
    private final Set<BlockPos> doors = new HashSet<BlockPos>();
    private long lastScanAt;
    private boolean prompted;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!AppleSettings.dungeonWitherDoorsHighlight || !DungeonContext.inDungeon()
                || mc.thePlayer == null || mc.theWorld == null) {
            doors.clear();
            prompted = false;
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastScanAt < SCAN_INTERVAL_MS) return;
        lastScanAt = now;
        Set<BlockPos> found = new HashSet<BlockPos>();
        int px = (int) Math.floor(mc.thePlayer.posX);
        int pz = (int) Math.floor(mc.thePlayer.posZ);
        for (int x = px - RANGE; x <= px + RANGE; x++) {
            for (int z = pz - RANGE; z <= pz + RANGE; z++) {
                BlockPos base = new BlockPos(x, DOOR_Y, z);
                if (!isDoorMaterial(mc.theWorld.getBlockState(base))) continue;
                // Door orientations can begin up to two blocks before the material block being scanned.
                if (isDoorStructure(base)) found.add(base);
                BlockPos westBase = base.add(-2, 0, 0);
                if (isDoorStructure(westBase)) found.add(westBase);
                BlockPos northBase = base.add(0, 0, -2);
                if (isDoorStructure(northBase)) found.add(northBase);
            }
        }
        doors.clear();
        doors.addAll(found);
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!AppleSettings.dungeonWitherDoorsHighlight || event == null || event.message == null
                || !DungeonContext.inDungeon()) return;
        String text = StringUtils.stripControlCodes(event.message.getUnformattedText());
        if (text == null) return;
        String lower = text.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("right click on the wither door") || lower.contains("right click on the blood door")) {
            prompted = true;
        } else if (lower.contains("opened a wither door") || lower.contains("blood door has been opened")) {
            prompted = false;
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event != null && event.world != null && event.world.isRemote) {
            doors.clear();
            prompted = false;
            lastScanAt = 0L;
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!AppleSettings.dungeonWitherDoorsHighlight || !DungeonContext.inDungeon()
                || doors.isEmpty() || !WorldBox.ready()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;
        RenderManager rm = mc.getRenderManager();
        boolean open = false;
        try {
            WorldBox.begin(rm.viewerPosX, rm.viewerPosY, rm.viewerPosZ, true);
            open = true;
            int color = prompted ? 0xFFFF9F0A : (Theme.accent | 0xFF000000);
            for (BlockPos pos : doors) {
                double x = pos.getX();
                double y = pos.getY();
                double z = pos.getZ();
                WorldBox.outline(x, y, z, x + 3.0, y + 4.0, z + 3.0, color);
            }
        } finally {
            if (open) WorldBox.end();
        }
    }

    private boolean isDoorStructure(BlockPos base) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return false;
        for (int dx = 0; dx < 3; dx++) {
            for (int dy = 0; dy < 4; dy++) {
                for (int dz = 0; dz < 3; dz++) {
                    if (!isDoorMaterial(mc.theWorld.getBlockState(base.add(dx, dy, dz)))) return false;
                }
            }
        }
        return true;
    }

    private static boolean isDoorMaterial(IBlockState state) {
        if (state == null) return false;
        Block block = state.getBlock();
        return block == Blocks.coal_block || (block == Blocks.stained_hardened_clay
                && state.getValue(BlockColored.COLOR) == EnumDyeColor.RED);
    }
}
