/*
 * This modified version is maintained by ii8we. Review LICENSE for upstream attribution and applicable license terms before use or redistribution. Original NotEnoughFakepixel code and third-party components remain subject to their respective copyrights and license terms.
 */
/* Adapted for Lunaria from code originating in NotEnoughFakepixel, through ii8we's private modifications. See LICENSE. */
package com.arena.pekaclient.dungeon.solver.devices;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.dungeon.solver.DungeonSolverColor;
import com.arena.pekaclient.dungeon.solver.DungeonSolverContext;
import com.arena.pekaclient.dungeon.solver.DungeonSolverRender;
import com.arena.pekaclient.events.PacketWriteEvent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockButtonStone;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.*;

public class FirstDeviceSolver {

    private BlockPos[] positionsToSolve;
    private int[] positionsIndexSolved = new int[]{-1, -1, -1, -1, -1};
    private boolean startMemorising;
    private boolean resolving;
    private int positionInRound;
    private int round = 1;

    /* A click can arrive through both Forge interaction and the outbound packet hook. */
    private BlockPos lastHandledPosition;
    private long lastHandledWorldTick = Long.MIN_VALUE;
    private boolean lastHandledClickCancelled;

    @SubscribeEvent
    public void onRenderLast(RenderWorldLastEvent event) {
        if (!AppleSettings.solverFirstDevice) return;
        if (!DungeonSolverContext.checkEssentials()) {
            reset();
            positionsToSolve = null;
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) return;

        if (startMemorising) {
            if (positionsToSolve == null || round < 1 || round > positionsIndexSolved.length) {
                reset();
                return;
            }
            for (int i = 0; i < positionsToSolve.length; i++) {
                BlockPos targetPos = positionsToSolve[i];
                if (targetPos == null || !mc.theWorld.isBlockLoaded(targetPos)) continue;
                Block targetBlock = mc.theWorld.getBlockState(targetPos).getBlock();
                if (targetBlock == Blocks.sea_lantern
                        && positionsIndexSolved[round - 1] == -1
                        && !checkIfAdded(positionsIndexSolved, i)) {
                    positionsIndexSolved[round - 1] = i;
                    resolving = true;
                    startMemorising = false;
                    break;
                }
            }
        }

        if (!resolving) return;
        if (positionsToSolve == null || round < 1 || round > positionsIndexSolved.length
                || positionInRound < 0 || positionInRound >= round) {
            reset();
            return;
        }

        for (int i = 0; i < round; i++) {
            int solvedIndex = positionsIndexSolved[i];
            if (solvedIndex < 0 || solvedIndex >= positionsToSolve.length) {
                reset();
                return;
            }
            Color baseColor = DungeonSolverColor.getColor(AppleSettings.solverAlternativeColor);
            if (i == positionInRound) {
                DungeonSolverRender.highlightBlock(positionsToSolve[solvedIndex],
                        DungeonSolverColor.getColor(AppleSettings.solverCorrectColor), false, true, event.partialTicks);
            } else if (i == positionInRound + 1) {
                DungeonSolverRender.highlightBlock(positionsToSolve[solvedIndex],
                        baseColor, false, true, event.partialTicks);
            } else if (i == positionInRound + 2) {
                DungeonSolverRender.highlightBlock(positionsToSolve[solvedIndex], new Color(
                        baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 150),
                        false, true, event.partialTicks);
            }
        }
    }

    /** Supports both button input paths; the packet hook remains the final guard for wrong clicks. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (!AppleSettings.solverFirstDevice || event == null
                || !DungeonSolverContext.checkEssentials()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null || mc.thePlayer != event.entityPlayer) return;
        if (event.action != PlayerInteractEvent.Action.LEFT_CLICK_BLOCK
                && event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;

        if (handleButtonClick(event.pos)) {
            event.setCanceled(true);
        }
    }

    /** Checks the outgoing left-click packet so a wrong sequence press never reaches the server. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onDiggingPacket(PacketWriteEvent event) {
        if (!AppleSettings.solverFirstDevice || event == null || event.packet == null
                || !DungeonSolverContext.checkEssentials()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;
        Packet packet = event.packet;
        if (!(packet instanceof C07PacketPlayerDigging)) return;

        C07PacketPlayerDigging digging = (C07PacketPlayerDigging) packet;
        if (digging.getStatus() != C07PacketPlayerDigging.Action.START_DESTROY_BLOCK) return;
        if (handleButtonClick(digging.getPosition())) {
            event.setCanceled(true);
        }
    }

    /** @return true when this click must be cancelled. */
    private boolean handleButtonClick(BlockPos buttonPos) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || buttonPos == null || !mc.theWorld.isBlockLoaded(buttonPos)) return false;

        long worldTick = mc.theWorld.getTotalWorldTime();
        if (buttonPos.equals(lastHandledPosition) && worldTick == lastHandledWorldTick) {
            return lastHandledClickCancelled;
        }

        net.minecraft.block.state.IBlockState buttonState = mc.theWorld.getBlockState(buttonPos);
        if (!(buttonState.getBlock() instanceof BlockButtonStone)) return false;

        EnumFacing facing = buttonState.getValue(BlockButton.FACING);
        BlockPos supportPos = getBlockUnderButton(buttonPos, facing);
        if (supportPos == null || !mc.theWorld.isBlockLoaded(supportPos)) return false;

        Block supportBlock = mc.theWorld.getBlockState(supportPos).getBlock();
        if (supportBlock == Blocks.emerald_block) {
            // Do not restart the sequence while waiting for the next lamp to light up.
            if (!startMemorising) {
                BlockPos[] targets = getSurroundingBlocks(supportPos, facing);
                reset();
                positionsToSolve = targets;
                startMemorising = true;
            }
            rememberClick(buttonPos, worldTick, false);
            return false;
        }

        if (supportBlock != Blocks.obsidian || !resolving) return false;

        if (positionsToSolve == null || positionInRound < 0 || positionInRound >= round
                || positionInRound >= positionsIndexSolved.length) {
            rememberClick(buttonPos, worldTick, true);
            return true;
        }

        int solvedIndex = positionsIndexSolved[positionInRound];
        if (solvedIndex < 0 || solvedIndex >= positionsToSolve.length
                || !supportPos.equals(positionsToSolve[solvedIndex])) {
            rememberClick(buttonPos, worldTick, true);
            return true;
        }

        positionInRound++;
        if (positionInRound == round) {
            positionInRound = 0;
            round++;
            startMemorising = true;
            resolving = false;
        }
        if (round == 6) {
            reset();
        }
        rememberClick(buttonPos, worldTick, false);
        return false;
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        reset();
        positionsToSolve = null;
    }

    private void rememberClick(BlockPos pos, long tick, boolean cancelled) {
        lastHandledPosition = pos;
        lastHandledWorldTick = tick;
        lastHandledClickCancelled = cancelled;
    }

    private static BlockPos getBlockUnderButton(BlockPos pos, EnumFacing facing) {
        if (pos == null || facing == null) return null;
        switch (facing) {
            case NORTH:
                return new BlockPos(pos.getX(), pos.getY(), pos.getZ() + 1);
            case EAST:
                return new BlockPos(pos.getX() - 1, pos.getY(), pos.getZ());
            case SOUTH:
                return new BlockPos(pos.getX(), pos.getY(), pos.getZ() - 1);
            case WEST:
                return new BlockPos(pos.getX() + 1, pos.getY(), pos.getZ());
            default:
                return null;
        }
    }

    public static BlockPos[] getSurroundingBlocks(BlockPos startPos, EnumFacing facing) {
        BlockPos[] surrounding = new BlockPos[16];
        int index = 0;
        for (int yOffset = -1; yOffset <= 2; yOffset++) {
            for (int horizontalOffset = 1; horizontalOffset <= 4; horizontalOffset++) {
                int dx = 0;
                int dz = 0;
                switch (facing) {
                    case NORTH:
                        dx = -horizontalOffset;
                        break;
                    case SOUTH:
                        dx = horizontalOffset;
                        break;
                    case WEST:
                        dz = horizontalOffset;
                        break;
                    case EAST:
                        dz = -horizontalOffset;
                        break;
                    default:
                        break;
                }
                surrounding[index++] = new BlockPos(startPos.getX() + dx,
                        startPos.getY() + yOffset, startPos.getZ() + dz);
            }
        }
        return surrounding;
    }

    private boolean checkIfAdded(int[] array, int index) {
        for (int value : array) {
            if (value == index) return true;
        }
        return false;
    }

    private void reset() {
        positionsIndexSolved = new int[]{-1, -1, -1, -1, -1};
        startMemorising = false;
        resolving = false;
        positionInRound = 0;
        round = 1;
        lastHandledPosition = null;
        lastHandledWorldTick = Long.MIN_VALUE;
        lastHandledClickCancelled = false;
    }
}
