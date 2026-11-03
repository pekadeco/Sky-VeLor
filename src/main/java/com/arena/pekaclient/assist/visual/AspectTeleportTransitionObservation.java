package com.arena.pekaclient.assist.visual;

/**
 * Small client-side state machine that correlates an observed local Aspect use with a later completed position jump.
 *
 * <p>It never touches an entity, packet, input binding, controller, or camera. Its only output is a retained pair of
 * already-observed coordinates for the render-only camera path.</p>
 */
public final class AspectTeleportTransitionObservation {
    private Object observedWorld;
    private boolean hasLastPosition;
    private double lastX;
    private double lastY;
    private double lastZ;
    private double transitionFromX;
    private double transitionFromY;
    private double transitionFromZ;
    private double transitionToX;
    private double transitionToY;
    private double transitionToZ;
    private int armedTicks;

    /** Arms exactly one short server-response window after an ordinary local Aspect use observation. */
    public void arm() {
        armedTicks = AspectTeleportTransitionPolicy.ARM_TICKS;
    }

    /** Advances the finite response window once per client tick, never once per render sample. */
    public void advanceTick() {
        if (armedTicks > 0) armedTicks--;
    }

    /**
     * Records one completed client position. Returns true at most once per arm window when the position change is a
     * plausible Aspect jump. A world switch always establishes a fresh baseline and never starts a render path.
     */
    public boolean observe(Object world, double x, double y, double z) {
        if (world == null || !finite(x) || !finite(y) || !finite(z)) {
            reset();
            return false;
        }
        if (world != observedWorld) {
            // A first render baseline may happen just after a valid click; preserve that freshly armed window. A true
            // world replacement always has an existing identity and clears the old arm before establishing its baseline.
            boolean firstWorldBaseline = observedWorld == null;
            observedWorld = world;
            hasLastPosition = true;
            lastX = x;
            lastY = y;
            lastZ = z;
            if (!firstWorldBaseline) armedTicks = 0;
            return false;
        }
        boolean transition = hasLastPosition && armedTicks > 0
                && AspectTeleportTransitionPolicy.isLargePositionJump(lastX, lastY, lastZ, x, y, z);
        if (transition) {
            // Preserve the pre-jump and authoritative destination positions before advancing the ordinary baseline.
            transitionFromX = lastX;
            transitionFromY = lastY;
            transitionFromZ = lastZ;
            transitionToX = x;
            transitionToY = y;
            transitionToZ = z;
            // A single use may create only one render transition, even if a server correction settles later.
            armedTicks = 0;
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
        transitionFromX = transitionFromY = transitionFromZ = 0D;
        transitionToX = transitionToY = transitionToZ = 0D;
    }

    /** Package-visible render origin retained only after {@link #observe(Object, double, double, double)} returns true. */
    double transitionFromX() { return transitionFromX; }
    double transitionFromY() { return transitionFromY; }
    double transitionFromZ() { return transitionFromZ; }
    /** Package-visible server-authoritative render destination retained only after a successful observation. */
    double transitionToX() { return transitionToX; }
    double transitionToY() { return transitionToY; }
    double transitionToZ() { return transitionToZ; }

    /** Package-visible only for the headless behavior test; it has no gameplay use. */
    int remainingArmedTicks() {
        return armedTicks;
    }

    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}
