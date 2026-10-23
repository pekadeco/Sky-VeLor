package com.arena.pekaclient.gui;

/** Headless proof that canvas selection has no hidden enabled-only or sidebar-only reachability path. */
public final class HudEditorSelectionPolicyTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        topmostCandidateWinsInitially();
        repeatedClicksCycleOverlap();
        disabledPlaceholderUsesTheSameHitPath();
        noCandidateIsSafe();
        System.out.println("HUD editor selection: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void topmostCandidateWinsInitially() {
        check("topmost canvas element selected", HudEditorSelectionPolicy.nextIndex(-1,
                new boolean[]{true, false, true, false}) == 2);
    }

    private static void repeatedClicksCycleOverlap() {
        boolean[] overlapping = {true, true, true};
        check("from top cycles to next covered element", HudEditorSelectionPolicy.nextIndex(2, overlapping) == 1);
        check("middle cycles to bottom element", HudEditorSelectionPolicy.nextIndex(1, overlapping) == 0);
        check("bottom wraps to top element", HudEditorSelectionPolicy.nextIndex(0, overlapping) == 2);
    }

    private static void disabledPlaceholderUsesTheSameHitPath() {
        // The policy deliberately receives only canvas-hit geometry, not an enabled flag.
        check("dimmed placeholder remains selectable", HudEditorSelectionPolicy.nextIndex(-1,
                new boolean[]{false, true, false}) == 1);
    }

    private static void noCandidateIsSafe() {
        check("no hit remains unselected", HudEditorSelectionPolicy.nextIndex(1,
                new boolean[]{false, false}) == -1);
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++;
        else {
            failed++;
            System.out.println("FAIL " + name);
        }
    }
}
