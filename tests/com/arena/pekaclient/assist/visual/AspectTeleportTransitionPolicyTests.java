package com.arena.pekaclient.assist.visual;

/** Headless proof that Aspect smoothing recognizes only observed jumps and exposes a bounded render path. */
public final class AspectTeleportTransitionPolicyTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("formatted AOTE name arms visual transition", AspectTeleportTransitionPolicy.isAspectTeleportItem(
                "\u00a76Aspect of the End"));
        check("case-insensitive AOTV name arms visual transition", AspectTeleportTransitionPolicy.isAspectTeleportItem(
                "ASPECT OF THE VOID"));
        check("other held item cannot arm transition", !AspectTeleportTransitionPolicy.isAspectTeleportItem("Hyperion"));
        check("short completed Aspect position jump qualifies", AspectTeleportTransitionPolicy.isLargePositionJump(
                0D, 64D, 0D, 2D, 64D, 0D));
        check("small normal movement cannot qualify", !AspectTeleportTransitionPolicy.isLargePositionJump(
                0D, 64D, 0D, .7D, 64D, .2D));
        check("sub-threshold drift cannot qualify", !AspectTeleportTransitionPolicy.isLargePositionJump(
                0D, 64D, 0D, 1.49D, 64D, 0D));
        check("server response window tolerates normal latency", AspectTeleportTransitionPolicy.ARM_TICKS >= 10);
        check("invalid observed coordinates cannot qualify", !AspectTeleportTransitionPolicy.isLargePositionJump(
                Double.NaN, 64D, 0D, 8D, 64D, 0D));
        check("camera path starts exactly at the pre-teleport origin",
                AspectTeleportTransitionPolicy.cameraProgress(0L) == 0D);
        check("camera path reaches a smooth midpoint", Math.abs(AspectTeleportTransitionPolicy.cameraProgress(
                AspectTeleportTransitionPolicy.CAMERA_INTERPOLATION_NANOS / 2L) - .5D) < .000001D);
        check("camera path ends deterministically at the authoritative target",
                AspectTeleportTransitionPolicy.cameraProgress(AspectTeleportTransitionPolicy.CAMERA_INTERPOLATION_NANOS) == 1D
                        && AspectTeleportTransitionPolicy.cameraProgress(
                        AspectTeleportTransitionPolicy.CAMERA_INTERPOLATION_NANOS + 1L) == 1D);
        check("negative elapsed time never backs up the visual camera",
                AspectTeleportTransitionPolicy.cameraProgress(-1L) == 0D);
        check("glide stays brief enough to preserve the ordinary server teleport",
                AspectTeleportTransitionPolicy.CAMERA_INTERPOLATION_NANOS > 0L
                        && AspectTeleportTransitionPolicy.CAMERA_INTERPOLATION_NANOS <= 200_000_000L);
        System.out.println("Aspect teleport transition policy: passed " + passed + ", failed " + failed);
        if (failed != 0) System.exit(1);
    }

    private static void check(String label, boolean ok) {
        if (ok) passed++; else { failed++; System.out.println("FAIL " + label); }
    }
}
