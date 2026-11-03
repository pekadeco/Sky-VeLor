package com.arena.pekaclient.assist.visual;

/** Headless regression coverage for the render-only dead-mob eligibility boundary. */
public final class DeadMobRenderPolicyTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("disabled cleanup never suppresses", !DeadMobRenderPolicy.shouldSuppress(false, false, false, false,
                true, 0, 20f));
        check("dead ordinary mob is suppressed", DeadMobRenderPolicy.shouldSuppress(true, false, false, false,
                true, 0, 20f));
        check("death animation mob is suppressed", DeadMobRenderPolicy.shouldSuppress(true, false, false, false,
                false, 1, 20f));
        check("zero-health custom mob is suppressed", DeadMobRenderPolicy.shouldSuppress(true, false, false, false,
                false, 0, 0f));
        check("alive mob remains visible", !DeadMobRenderPolicy.shouldSuppress(true, false, false, false,
                false, 0, 20f));
        check("local player never disappears", !DeadMobRenderPolicy.shouldSuppress(true, true, true, false,
                true, 10, 0f));
        check("ordinary remote player never disappears", !DeadMobRenderPolicy.shouldSuppress(true, false, true, false,
                true, 10, 0f));
        check("dead fakeplayer NPC is suppressed", DeadMobRenderPolicy.shouldSuppress(true, false, true, true,
                true, 10, 0f));
        check("formatted bracketed NPC marker is recognized", DeadMobRenderPolicy.hasNpcMarker("\u00a7e[NPC] \u00a7fWatcher"));
        check("plain NPC marker is recognized", DeadMobRenderPolicy.hasNpcMarker("Helpful NPC"));
        check("arbitrary player-like name is not an NPC marker", !DeadMobRenderPolicy.hasNpcMarker("NpcFanatic"));
        done();
    }

    private static void check(String label, boolean okay) {
        if (okay) passed++; else { failed++; System.out.println("FAIL " + label); }
    }

    private static void done() {
        System.out.println("Dead mob render policy: passed " + passed + ", failed " + failed);
        if (failed != 0) System.exit(1);
    }
}
