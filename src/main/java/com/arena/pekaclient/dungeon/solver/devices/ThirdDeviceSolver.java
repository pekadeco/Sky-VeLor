/*
 * This modified version is maintained by ii8we. Review LICENSE for upstream attribution and applicable license terms before use or redistribution. Original NotEnoughFakepixel code and third-party components remain subject to their respective copyrights and license terms.
 */
/* Adapted for Lunaria from code originating in NotEnoughFakepixel, through ii8we's private modifications. See LICENSE. */
package com.arena.pekaclient.dungeon.solver.devices;
import com.arena.pekaclient.config.AppleSettings;

import com.arena.pekaclient.dungeon.solver.DungeonSolverContext;
import com.arena.pekaclient.dungeon.solver.DungeonSolverColor;
import com.arena.pekaclient.dungeon.solver.DungeonSolverRender;
import net.minecraft.block.BlockSeaLantern;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.event.world.WorldEvent;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ThirdDeviceSolver {

    private final Minecraft mc = Minecraft.getMinecraft();

    private static final Map<BlockPos, Integer> itemFramesRotations = new HashMap<>();
    private static final Map<BlockPos, Integer> clientRemainingClicks = new HashMap<>();
    private static final Set<String> processedPackets = new HashSet<>();
    private long lastResyncTime = 0;
    private int tickCounter = 0;

    static {
        itemFramesRotations.put(new BlockPos(6, 122, 85), 3);
        itemFramesRotations.put(new BlockPos(6, 121, 85), 1);
        itemFramesRotations.put(new BlockPos(6, 121, 84), 1);
        itemFramesRotations.put(new BlockPos(6, 121, 83), 7);
        itemFramesRotations.put(new BlockPos(6, 122, 83), 7);
        itemFramesRotations.put(new BlockPos(6, 123, 83), 7);
        itemFramesRotations.put(new BlockPos(6, 124, 83), 7);
        itemFramesRotations.put(new BlockPos(6, 125, 83), 5);
        itemFramesRotations.put(new BlockPos(6, 125, 84), 5);
        itemFramesRotations.put(new BlockPos(6, 125, 85), 5);
        itemFramesRotations.put(new BlockPos(6, 125, 86), 5);
        itemFramesRotations.put(new BlockPos(6, 125, 87), 3);
        itemFramesRotations.put(new BlockPos(6, 124, 87), 3);
        itemFramesRotations.put(new BlockPos(6, 123, 87), 3);
        itemFramesRotations.put(new BlockPos(6, 122, 87), 3);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouse(MouseEvent event) {
        if (!AppleSettings.solverThirdDevice || event.button != 1 || !event.buttonstate) return;
        if (!DungeonSolverContext.checkEssentialsF7()) return;
        if (mc.currentScreen != null || mc.theWorld == null || mc.thePlayer == null) return;

        MovingObjectPosition hit = mc.objectMouseOver;
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY
                || !(hit.entityHit instanceof EntityItemFrame)) return;
        EntityItemFrame frame = (EntityItemFrame) hit.entityHit;
        ItemStack shown = frame.getDisplayedItem();
        if (shown == null || shown.getItem() != Items.arrow) return;

        BlockPos framePos = new BlockPos(frame.getPosition());
        Integer desired = itemFramesRotations.get(framePos);
        if (desired == null) return;

        String packetId = framePos + ":" + tickCounter;
        if (processedPackets.contains(packetId)) {
            event.setCanceled(true);
            return;
        }
        int currentRotation = frame.getRotation();
        int remaining = clientRemainingClicks.containsKey(framePos)
                ? clientRemainingClicks.get(framePos) : (desired - currentRotation + 8) % 8;
        if (remaining <= 0) {
            event.setCanceled(true);
            return;
        }
        processedPackets.add(packetId);
        clientRemainingClicks.put(framePos, remaining - 1);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!AppleSettings.solverThirdDevice) return;
        if (!DungeonSolverContext.checkEssentialsF7()) return;
        if (mc.theWorld == null || mc.thePlayer == null) {
            clientRemainingClicks.clear();
            processedPackets.clear();
            return;
        }

        tickCounter++;
        processedPackets.clear();

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastResyncTime >= 5000) {
            resyncClientRemainingClicks();
            lastResyncTime = currentTime;
        }
    }

    private void resyncClientRemainingClicks() {
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityItemFrame) {
                EntityItemFrame itemFrame = (EntityItemFrame) entity;
                BlockPos posItemFrame = new BlockPos(entity.getPosition());

                if (itemFramesRotations.containsKey(posItemFrame)) {
                    int desiredRotation = itemFramesRotations.get(posItemFrame);
                    int currentRotation = itemFrame.getRotation();
                    int clicksNeeded = (desiredRotation - currentRotation + 8) % 8;
                    clientRemainingClicks.put(posItemFrame, clicksNeeded);
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderLast(RenderWorldLastEvent event) {
        if (!AppleSettings.solverThirdDevice) return;
        if (!DungeonSolverContext.checkEssentialsF7()) return;
        if (mc.theWorld == null || mc.thePlayer == null) return;

        mc.theWorld.loadedEntityList.forEach(entity -> {
            if (entity instanceof EntityItemFrame) {
                EntityItemFrame itemFrame = (EntityItemFrame) entity;
                ItemStack item = itemFrame.getDisplayedItem();
                if (item == null || item.getItem() != Items.arrow) return;
                BlockPos pos = getBlockUnderItemFrame(itemFrame);
                if (pos == null) return;

                Color color = mc.theWorld.getBlockState(pos).getBlock() instanceof BlockSeaLantern
                        ? DungeonSolverColor.getColor(AppleSettings.solverCorrectColor)
                        : DungeonSolverColor.getColor(AppleSettings.solverAlternativeColor);

                DungeonSolverRender.highlightBlock(pos, color, false, event.partialTicks);

                BlockPos posItemFrame = new BlockPos(entity.getPosition());
                if (itemFramesRotations.containsKey(posItemFrame)) {
                    int desiredRotation = itemFramesRotations.get(posItemFrame);
                    int currentRotation = itemFrame.getRotation();
                    int clicksNeeded = clientRemainingClicks.getOrDefault(posItemFrame,
                            (desiredRotation - currentRotation + 8) % 8);

                    double[] renderPos = new double[]{posItemFrame.getX() - 0.7, posItemFrame.getY() - 3.3, posItemFrame.getZ()};

                    Color textColor = (clicksNeeded == 0)
                            ? DungeonSolverColor.getColor(AppleSettings.solverCorrectColor)
                            : DungeonSolverColor.getColor(AppleSettings.solverAlternativeColor);
                    DungeonSolverRender.drawTag(String.valueOf(clicksNeeded), renderPos, textColor, event.partialTicks);
                }
            }
        });
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        clientRemainingClicks.clear();
        processedPackets.clear();
        tickCounter = 0;
        lastResyncTime = 0;
    }

    private static BlockPos getBlockUnderItemFrame(EntityItemFrame itemFrame) {
        switch (itemFrame.facingDirection) {
            case NORTH:
                return new BlockPos(itemFrame.posX, itemFrame.posY, itemFrame.posZ + 1);
            case EAST:
                return new BlockPos(itemFrame.posX - 1, itemFrame.posY, itemFrame.posZ);
            case SOUTH:
                return new BlockPos(itemFrame.posX, itemFrame.posY, itemFrame.posZ - 1);
            case WEST:
                return new BlockPos(itemFrame.posX + 1, itemFrame.posY, itemFrame.posZ);
            default:
                return null;
        }
    }
}
