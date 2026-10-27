package com.arena.pekaclient.assist.visual;

/**
 * Small, mapping-free observation state for a visual-only Aspect teleport transition.
 *
 * <p>It retains only the last observed local position and a short window armed by a local item-use observation.
 * It has no Minecraft, input, packet, controller, camera, or movement dependency, so it cannot initiate or steer a
 * teleport. The renderer supplies already-observed values and decides how to paint a successful observation.</p>
 */
public final class AspectTeleportTransitionObservation {

    private Object observedWorld;
    private boolean hasLastPosition;
    private double lastX;
    private double lastY;
    private double lastZ;
    private int armedTicks;

    /** Opens a short window in which one completed position discontinuity may start a visual transition. */
    public void arm() {
        armedTicks = AspectTeleportTransitionPolicy.ARM_TICKS;
    }

    /**
     * Records one completed client position. Returns true at most once per arm window when the position change is a
     * plausible Aspect jump. A world switch always establishes a fresh baseline and never paints a transition.
     */
    public boolean observe(Object world, double x, double y, double z) {
        if (world == null || !finite(x) || !finite(y) || !finite(z)) {
            reset();
            return false;
        }
        if (world != observedWorld) {
            observedWorld = world;
            hasLastPosition = true;
            lastX = x;
            lastY = y;
            lastZ = z;
            armedTicks = 0;
            return false;
        }
        boolean transition = hasLastPosition && armedTicks > 0
                && AspectTeleportTransitionPolicy.isLargePositionJump(lastX, lastY, lastZ, x, y, z);
        if (transition) {
            // A single use may create only one veil, even if a server correction settles across later ticks.
            armedTicks = 0;
        } else if (armedTicks > 0) {
            armedTicks--;
        }
        lastX = x;
        lastY = y;
        lastZ = z;
        hasLastPosition = true;
        return transition;
    }

    /** Clears all observations on disconnect, world replacement, or when the visual setting is disabled. */
    public void reset() {
        observedWorld = null;
        hasLastPosition = false;
        armedTicks = 0;
        lastX = lastY = lastZ = 0D;
    }

    /** Package-visible only for the headless behavior test; it has no gameplay use. */
    int remainingArmedTicks() {
        return armedTicks;
    }

    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}
