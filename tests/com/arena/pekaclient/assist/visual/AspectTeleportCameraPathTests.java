package com.arena.pekaclient.assist.visual;

/** Deterministic interpolation coverage for the independent render-position path. */
public final class AspectTeleportCameraPathTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        progressesFromObservedOriginToAuthoritativeDestination();
        repeatedPathContinuesFromCurrentVisualPosition();
        clearAndInvalidInputFailClosed();
        done();
    }

    private static void progressesFromObservedOriginToAuthoritativeDestination() {
        AspectTeleportCameraPath path = new AspectTeleportCameraPath();
        long start = 1_000L;
        path.begin(1D, 64D, -2D, 9D, 70D, 6D, start);
        check("path is active at its start", path.isActiveAt(start));
        check("x starts at pre-teleport origin", same(path.xAt(start), 1D));
        check("y starts at pre-teleport origin", same(path.yAt(start), 64D));
        check("z starts at pre-teleport origin", same(path.zAt(start), -2D));
        long midpoint = start + AspectTeleportTransitionPolicy.CAMERA_INTERPOLATION_NANOS / 2L;
        check("x uses smooth midpoint", same(path.xAt(midpoint), 5D));
        check("y uses smooth midpoint", same(path.yAt(midpoint), 67D));
        check("z uses smooth midpoint", same(path.zAt(midpoint), 2D));
        long end = start + AspectTeleportTransitionPolicy.CAMERA_INTERPOLATION_NANOS;
        check("path is no longer active once target is reached", !path.isActiveAt(end));
        check("x ends at server destination", same(path.xAt(end), 9D));
        check("y ends at server destination", same(path.yAt(end), 70D));
        check("z ends at server destination", same(path.zAt(end), 6D));
    }

    private static void repeatedPathContinuesFromCurrentVisualPosition() {
        AspectTeleportCameraPath path = new AspectTeleportCameraPath();
        long start = 5_000L;
        path.begin(0D, 0D, 0D, 8D, 0D, 0D, start);
        long midpoint = start + AspectTeleportTransitionPolicy.CAMERA_INTERPOLATION_NANOS / 2L;
        path.begin(999D, 999D, 999D, 12D, 4D, -4D, midpoint);
        check("replacement path uses the current visual x rather than stale caller origin", same(path.xAt(midpoint), 4D));
        check("replacement path uses the current visual y rather than stale caller origin", same(path.yAt(midpoint), 0D));
        check("replacement path uses the current visual z rather than stale caller origin", same(path.zAt(midpoint), 0D));
        long end = midpoint + AspectTeleportTransitionPolicy.CAMERA_INTERPOLATION_NANOS;
        check("replacement path still reaches its new x destination", same(path.xAt(end), 12D));
        check("replacement path still reaches its new y destination", same(path.yAt(end), 4D));
        check("replacement path still reaches its new z destination", same(path.zAt(end), -4D));
    }

    private static void clearAndInvalidInputFailClosed() {
        AspectTeleportCameraPath path = new AspectTeleportCameraPath();
        path.begin(0D, 0D, 0D, 3D, 0D, 0D, 20L);
        path.clear();
        check("clear disables a path immediately", !path.isActiveAt(20L));
        path.begin(Double.NaN, 0D, 0D, 3D, 0D, 0D, 20L);
        check("invalid coordinates do not leave a path active", !path.isActiveAt(20L));
    }

    private static boolean same(double actual, double expected) {
        return Math.abs(actual - expected) < .0000001D;
    }

    private static void check(String label, boolean okay) {
        if (okay) passed++; else { failed++; System.out.println("FAIL " + label); }
    }

    private static void done() {
        System.out.println("Aspect teleport camera path: passed " + passed + ", failed " + failed);
        if (failed != 0) System.exit(1);
    }
}
