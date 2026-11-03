package com.arena.pekaclient.assist.visual;

/**
 * Allocation-free mathematical path for the temporary rendered camera origin.
 *
 * <p>It owns only doubles and a timestamp. It has no Minecraft references, cannot move a player, and can therefore
 * be tested without a client. A replacement transition starts at the current visual position so repeated legitimate
 * Aspect uses never introduce a second visual snap.</p>
 */
final class AspectTeleportCameraPath {
    private long startedNanos = Long.MIN_VALUE;
    private double fromX;
    private double fromY;
    private double fromZ;
    private double toX;
    private double toY;
    private double toZ;

    void begin(double nextFromX, double nextFromY, double nextFromZ,
               double nextToX, double nextToY, double nextToZ, long nowNanos) {
        if (!finite(nextFromX) || !finite(nextFromY) || !finite(nextFromZ)
                || !finite(nextToX) || !finite(nextToY) || !finite(nextToZ)) {
            clear();
            return;
        }
        if (isActiveAt(nowNanos)) {
            // Continue from what was actually rendered at this instant rather than popping back to an old target.
            nextFromX = xAt(nowNanos);
            nextFromY = yAt(nowNanos);
            nextFromZ = zAt(nowNanos);
        }
        fromX = nextFromX;
        fromY = nextFromY;
        fromZ = nextFromZ;
        toX = nextToX;
        toY = nextToY;
        toZ = nextToZ;
        startedNanos = nowNanos;
    }

    void clear() {
        startedNanos = Long.MIN_VALUE;
        fromX = fromY = fromZ = 0D;
        toX = toY = toZ = 0D;
    }

    boolean isActiveAt(long nowNanos) {
        return startedNanos != Long.MIN_VALUE && nowNanos >= startedNanos
                && nowNanos - startedNanos < AspectTeleportTransitionPolicy.CAMERA_INTERPOLATION_NANOS;
    }

    double xAt(long nowNanos) { return interpolate(fromX, toX, nowNanos); }
    double yAt(long nowNanos) { return interpolate(fromY, toY, nowNanos); }
    double zAt(long nowNanos) { return interpolate(fromZ, toZ, nowNanos); }

    private double interpolate(double from, double to, long nowNanos) {
        if (startedNanos == Long.MIN_VALUE) return to;
        return from + (to - from) * AspectTeleportTransitionPolicy.cameraProgress(nowNanos - startedNanos);
    }

    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}
