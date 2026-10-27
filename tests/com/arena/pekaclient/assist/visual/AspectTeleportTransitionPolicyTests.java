package com.arena.pekaclient.assist.visual;

/** Headless proof that the Aspect transition can only recognize an observed jump and draw a bounded visual veil. */
public final class AspectTeleportTransitionPolicyTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("formatted AOTE name arms visual transition", AspectTeleportTransitionPolicy.isAspectTeleportItem(
                "\u00a76Aspect of the End"));
        check("case-insensitive AOTV name arms visual transition", AspectTeleportTransitionPolicy.isAspectTeleportItem(
                "ASPECT OF THE VOID"));
        check("other held item cannot arm transition", !AspectTeleportTransitionPolicy.isAspectTeleportItem("Hyperion"));
        check("four-block completed position jump qualifies", AspectTeleportTransitionPolicy.isLargePositionJump(
                0D, 64D, 0D, 4D, 64D, 0D));
        check("small normal movement cannot qualify", !AspectTeleportTransitionPolicy.isLargePositionJump(
                0D, 64D, 0D, .7D, 64D, .2D));
        check("invalid observed coordinates cannot qualify", !AspectTeleportTransitionPolicy.isLargePositionJump(
                Double.NaN, 64D, 0D, 8D, 64D, 0D));
        check("veil starts at zero for a smooth fade in", AspectTeleportTransitionPolicy.veilAlpha(0L) == 0F);
        check("veil reaches a bounded positive alpha after fade in",
                AspectTeleportTransitionPolicy.veilAlpha(AspectTeleportTransitionPolicy.FADE_IN_NANOS) > 0F
                        && AspectTeleportTransitionPolicy.veilAlpha(AspectTeleportTransitionPolicy.FADE_IN_NANOS)
                        <= AspectTeleportTransitionPolicy.MAX_VEIL_ALPHA);
        check("veil ends deterministically after fixed duration",
                AspectTeleportTransitionPolicy.veilAlpha(AspectTeleportTransitionPolicy.DURATION_NANOS) == 0F
                        && AspectTeleportTransitionPolicy.veilAlpha(AspectTeleportTransitionPolicy.DURATION_NANOS + 1L) == 0F);
        check("negative elapsed time never draws", AspectTeleportTransitionPolicy.veilAlpha(-1L) == 0F);
        System.out.println("Aspect teleport transition policy: passed " + passed + ", failed " + failed);
        if (failed != 0) System.exit(1);
    }

    private static void check(String label, boolean ok) {
        if (ok) passed++; else { failed++; System.out.println("FAIL " + label); }
    }
}
