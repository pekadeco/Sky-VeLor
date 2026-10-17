package com.arena.pekaclient.render.itemanimation;

import com.arena.pekaclient.feature.ItemAnimation;
import net.minecraft.client.renderer.GlStateManager;

/**
 * The two tiny callbacks woven into Minecraft's real 1.8.9 first-person renderer.
 *
 * <p>They intentionally read {@link ItemAnimation} on every invocation instead of caching a renderer or a
 * setting snapshot. A slider drag or exact-value entry therefore changes the very next hand/equip frame.
 * No world, player, packet, attack, or third-person method is touched.
 */
public final class ItemAnimationHooks {

    private static final int INITIAL_RENDER_STACK = 4;
    private static int renderDepth;
    private static boolean[] pushedMatrix = new boolean[INITIAL_RENDER_STACK];

    private static volatile boolean renderHookReached;
    private static volatile boolean equipHookReached;
    private static volatile long renderCalls;
    private static volatile long equipCalls;
    private static volatile long lastRenderNanos;
    private static volatile long lastEquipNanos;
    private static volatile float lastX;
    private static volatile float lastY;
    private static volatile float lastZ;
    private static volatile float lastSize = 1.0F;
    private static volatile float lastSpeed = 1.0F;

    private ItemAnimationHooks() {
    }

    /**
     * Called immediately before Minecraft invokes its actual ItemRenderer first-person draw method.
     * The object and partial-tick arguments preserve the original bytecode stack without tying this core hook to
     * an MCP/obfuscated ItemRenderer descriptor.
     */
    public static void beforeFirstPersonRender(Object ignoredRenderer, float partialTicks) {
        float x = ItemAnimation.getX();
        float y = ItemAnimation.getY();
        float z = ItemAnimation.getZ();
        float size = ItemAnimation.getSize();
        boolean apply = x != 0.0F || y != 0.0F || z != 0.0F || size != 1.0F;

        renderHookReached = true;
        renderCalls++;
        lastRenderNanos = System.nanoTime();
        lastX = x;
        lastY = y;
        lastZ = z;
        lastSize = size;

        int slot = renderDepth++;
        if (slot >= pushedMatrix.length) {
            boolean[] expanded = new boolean[pushedMatrix.length * 2];
            System.arraycopy(pushedMatrix, 0, expanded, 0, pushedMatrix.length);
            pushedMatrix = expanded;
        }
        pushedMatrix[slot] = apply;
        if (apply) {
            // Translation first keeps the familiar X/Y/Z controls anchored at the vanilla hand position;
            // scaling then enlarges/shrinks the item around the normal first-person origin.
            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, z);
            GlStateManager.scale(size, size, size);
        }
    }

    /** Called immediately after the real first-person draw; balances only a matrix this hook pushed. */
    public static void afterFirstPersonRender() {
        if (renderDepth <= 0) {
            // A malformed third-party transformer must not underflow our state or pop a Minecraft matrix.
            return;
        }
        int slot = --renderDepth;
        boolean apply = pushedMatrix[slot];
        pushedMatrix[slot] = false;
        if (apply) {
            GlStateManager.popMatrix();
        }
    }

    /**
     * Called after vanilla computes its per-tick equipped-progress change. It adjusts only that visual
     * interpolation: attack cooldown, swing progress, item use and all gameplay timing remain untouched.
     */
    public static float scaleEquipProgress(float previous, float vanillaCurrent) {
        float speed = ItemAnimation.getSpeed();
        equipHookReached = true;
        equipCalls++;
        lastEquipNanos = System.nanoTime();
        lastSpeed = speed;

        // The exact 1.0 path avoids needless floating-point drift at vanilla defaults.
        if (speed == 1.0F || Float.isNaN(speed) || Float.isInfinite(speed)) {
            return vanillaCurrent;
        }
        float adjusted = previous + (vanillaCurrent - previous) * speed;
        // Vanilla equipped progress is [0, 1]. Clamping prevents a high multiplier from overshooting its target
        // and bouncing on the next tick, which would otherwise look like a hand jitter/stutter.
        return Math.max(0.0F, Math.min(1.0F, adjusted));
    }

    public static boolean hasRenderHookRun() {
        return renderHookReached;
    }

    public static boolean hasEquipHookRun() {
        return equipHookReached;
    }

    public static long getRenderCalls() {
        return renderCalls;
    }

    public static long getEquipCalls() {
        return equipCalls;
    }

    public static long getLastRenderNanos() {
        return lastRenderNanos;
    }

    public static long getLastEquipNanos() {
        return lastEquipNanos;
    }

    public static float getLastX() {
        return lastX;
    }

    public static float getLastY() {
        return lastY;
    }

    public static float getLastZ() {
        return lastZ;
    }

    public static float getLastSize() {
        return lastSize;
    }

    public static float getLastSpeed() {
        return lastSpeed;
    }
}
