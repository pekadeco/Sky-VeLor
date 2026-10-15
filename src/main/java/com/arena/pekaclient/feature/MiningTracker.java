package com.arena.pekaclient.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;

/**
 * Counts blocks the local player successfully mines, and reports blocks per second.
 *
 * Detection, once per client tick, uses only client-visible state:
 *  - A block counts when a solid block the player was aiming at, or attacking, turns to air.
 *    That is the client-side result of a completed break.
 *  - Attacking means the attack key is held, or the controller is hitting a block. The block is
 *    remembered from the previous tick, so an instant break is caught on the next tick. A tap so short
 *    that the key is released before the tick is not guaranteed to be seen.
 *  - A break only counts once. The pending target is cleared when it is counted, and a target that is
 *    already air is never armed again, so the same break is not counted twice.
 *  - Failed attempts, placing blocks, walking and progress updates do not count.
 *
 * Limits: a block that another player or a falling block removes while you are attacking the same
 * spot may be counted. Server-side rejections are not visible to the client. The counter resets on
 * world change, respawn and disconnect.
 */
public final class MiningTracker {

    /** Ticks after an attack during which a block turning to air still counts as your break. */
    private static final int GRACE_TICKS = 6;
    private static final int CAPACITY = 4096;

    private static final BlockBreakCounter COUNTER = new BlockBreakCounter(CAPACITY);
    private static float windowSeconds = 5f;
    private static float rate;

    private static WorldClient sessionWorld;
    private static EntityPlayerSP sessionPlayer;
    private static long ticks;

    /** Solid block the crosshair was on at the end of the previous tick. */
    private static BlockPos watchPos;
    /** Block the player attacked recently and may have just broken. */
    private static BlockPos pendingPos;
    private static long pendingDeadline;

    private MiningTracker() {
    }

    /** Sets the rolling window in seconds. Accepted values are 1, 5 and 10 (anything positive works). */
    public static void setWindowSeconds(float seconds) {
        windowSeconds = seconds;
    }

    /** Current mining rate in blocks per second. Updated once per tick. */
    public static float rate() {
        return rate;
    }

    /** Blocks mined since the last reset. */
    public static int total() {
        return COUNTER.total();
    }

    /** Clears all state. Called on world change, respawn and disconnect. */
    public static void reset() {
        COUNTER.reset();
        rate = 0f;
        watchPos = null;
        pendingPos = null;
        pendingDeadline = 0L;
    }

    /** Called from the client tick, before the world moves. */
    public static void tick(Minecraft mc) {
        WorldClient world = mc.theWorld;
        EntityPlayerSP player = mc.thePlayer;
        if (world != sessionWorld || player != sessionPlayer) {
            reset();
            sessionWorld = world;
            sessionPlayer = player;
        }
        if (world == null || player == null || mc.playerController == null || mc.gameSettings == null) {
            return;
        }
        ticks++;
        long now = System.nanoTime();

        // 1. Resolve a pending break: the target has gone to air, so the player broke it.
        if (pendingPos != null) {
            if (world.isAirBlock(pendingPos)) {
                COUNTER.record(now);
                pendingPos = null;
            } else if (ticks > pendingDeadline) {
                pendingPos = null;
            }
        }

        // 2. Attack intent this tick. The block is the one watched at the end of the last tick.
        // A watched block that is already air was just broken and counted in step 1. Re-arming it here
        // would count the same break a second time, so it is skipped.
        boolean attacking = mc.playerController.getIsHittingBlock() || mc.gameSettings.keyBindAttack.isKeyDown();
        if (attacking && watchPos != null && !world.isAirBlock(watchPos)) {
            pendingPos = watchPos;
            pendingDeadline = ticks + GRACE_TICKS;
        }

        // 3. Watch the solid block under the crosshair for the next tick.
        watchPos = null;
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            BlockPos p = mop.getBlockPos();
            if (p != null && !world.isAirBlock(p)) {
                watchPos = p;
            }
        }

        rate = COUNTER.rate(now, windowSeconds);
    }
}
