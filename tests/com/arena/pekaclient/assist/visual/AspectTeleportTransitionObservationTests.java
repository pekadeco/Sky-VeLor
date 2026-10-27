package com.arena.pekaclient.assist.visual;

/** Headless regression coverage for explicit-use arming and completed-jump observation. */
public final class AspectTeleportTransitionObservationTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        requiresAnObservedUseBeforePainting();
        oneUseCreatesOnlyOneTransition();
        armWindowSurvivesExpectedServerDelayButExpires();
        worldChangesAndInvalidCoordinatesNeverCreateAFlash();
        done();
    }

    private static void requiresAnObservedUseBeforePainting() {
        AspectTeleportTransitionObservation state = new AspectTeleportTransitionObservation();
        Object world = new Object();
        check("first world position is only a baseline", !state.observe(world, 0D, 64D, 0D));
        check("unarmed position jump stays quiet", !state.observe(world, 3D, 64D, 0D));
        state.arm();
        check("armed short Aspect jump starts the visual transition", state.observe(world, 5D, 64D, 0D));
    }

    private static void oneUseCreatesOnlyOneTransition() {
        AspectTeleportTransitionObservation state = new AspectTeleportTransitionObservation();
        Object world = new Object();
        state.observe(world, 0D, 64D, 0D);
        state.arm();
        check("first eligible jump consumes the arm", state.observe(world, 4D, 64D, 0D)
                && state.remainingArmedTicks() == 0);
        check("later correction without another use cannot retrigger", !state.observe(world, 8D, 64D, 0D));
    }

    private static void armWindowSurvivesExpectedServerDelayButExpires() {
        AspectTeleportTransitionObservation state = new AspectTeleportTransitionObservation();
        Object world = new Object();
        state.observe(world, 0D, 64D, 0D);
        state.arm();
        for (int i = 0; i < AspectTeleportTransitionPolicy.ARM_TICKS - 2; i++) {
            check("ordinary armed movement remains quiet " + i, !state.observe(world, .20D * i, 64D, 0D));
        }
        check("late server response inside arm window still transitions", state.observe(world, 3D, 64D, 0D));

        state.reset();
        state.observe(world, 0D, 64D, 0D);
        state.arm();
        for (int i = 0; i < AspectTeleportTransitionPolicy.ARM_TICKS; i++) {
            state.observe(world, .05D * i, 64D, 0D);
        }
        check("expired use window cannot flash on a delayed unrelated jump", !state.observe(world, 4D, 64D, 0D));
    }

    private static void worldChangesAndInvalidCoordinatesNeverCreateAFlash() {
        AspectTeleportTransitionObservation state = new AspectTeleportTransitionObservation();
        Object first = new Object();
        Object second = new Object();
        state.observe(first, 0D, 64D, 0D);
        state.arm();
        check("world replacement establishes a quiet new baseline", !state.observe(second, 10D, 70D, 10D));
        state.arm();
        check("invalid observation clears rather than paints", !state.observe(second, Double.NaN, 64D, 0D)
                && state.remainingArmedTicks() == 0);
    }

    private static void check(String label, boolean okay) {
        if (okay) passed++; else { failed++; System.out.println("FAIL " + label); }
    }

    private static void done() {
        System.out.println("Aspect teleport observation: passed " + passed + ", failed " + failed);
        if (failed != 0) System.exit(1);
    }
}
