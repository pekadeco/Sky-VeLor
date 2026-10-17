package com.arena.pekaclient.feature;

/** Pure visual-timeline tests: no Minecraft input, player, packet, or world object is created. */
public final class VisualSwingStateMachineTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        heldAttackRepeats();
        singleClickPlaysOnce();
        spamDoesNotRestartMidway();

        System.out.println();
        System.out.println("visual swing state machine: passed " + passed + ", failed " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void heldAttackRepeats() {
        VisualSwingStateMachine swing = new VisualSwingStateMachine();
        float result = VisualSwingStateMachine.USE_VANILLA;
        for (int tick = 0; tick <= 30; tick++) {
            // One rising attack starts the first cycle; held input is what starts each completed repeat.
            result = swing.advance(0f, tick, tick == 0, true, 1f, true);
        }
        check("held attack keeps a visual swing active", result != VisualSwingStateMachine.USE_VANILLA);
        check("held attack starts six cycles in thirty ticks", swing.getStarts() == 6L);
    }

    private static void singleClickPlaysOnce() {
        VisualSwingStateMachine swing = new VisualSwingStateMachine();
        swing.advance(0f, 0f, true, true, 1f, true);
        for (int tick = 1; tick <= 12; tick++) {
            swing.advance(0f, tick, false, false, 1f, true);
        }
        check("single click starts exactly one visual cycle", swing.getStarts() == 1L);
        check("single click finishes instead of repeating", !swing.isActive());
    }

    private static void spamDoesNotRestartMidway() {
        VisualSwingStateMachine swing = new VisualSwingStateMachine();
        swing.advance(0f, 0f, true, true, 1f, true);
        swing.advance(0f, 1f, false, false, 1f, true);
        // A second real click before the first timeline ends must not reset its visual progress.
        float before = swing.advance(0f, 2f, true, true, 1f, true);
        float after = swing.advance(0f, 3f, true, true, 1f, true);
        check("spam click does not add a midway restart", swing.getStarts() == 1L);
        check("spam click keeps progress moving forward", after > before);
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL " + name);
        }
    }
}
