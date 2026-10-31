package com.arena.pekaclient.gui;

/** Regression checks for title-bar control positions, hit testing and disabled-control hints. */
public final class WindowControlsTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        positionsRemainSpacedAndStable();
        enabledControlsRemainClickable();
        disabledControlsDoNotStealClicks();
        disabledHintsRemainSpecific();
        System.out.println("window controls: passed " + passed + ", failed " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void positionsRemainSpacedAndStable() {
        check("first control position", WindowControls.centreX(100f, 0) == 100f);
        check("second control position", WindowControls.centreX(100f, 1) == 114f);
        check("third control position", WindowControls.centreX(100f, 2) == 128f);
    }

    private static void enabledControlsRemainClickable() {
        check("close control hit", WindowControls.hit(100, 30, 100f, 30f, 0b111) == WindowControls.CLOSE);
        check("collapse control hit", WindowControls.hit(114, 30, 100f, 30f, 0b111) == WindowControls.COLLAPSE);
        check("zoom control hit", WindowControls.hit(128, 30, 100f, 30f, 0b111) == WindowControls.ZOOM);
        check("pointer away from controls", WindowControls.hit(150, 30, 100f, 30f, 0b111) == WindowControls.NONE);
        check("pointer vertically away from controls", WindowControls.hit(100, 40, 100f, 30f, 0b111) == WindowControls.NONE);
    }

    private static void disabledControlsDoNotStealClicks() {
        check("only close enabled", WindowControls.hit(100, 30, 100f, 30f, 0b001) == WindowControls.CLOSE);
        check("disabled collapse identified", WindowControls.hit(114, 30, 100f, 30f, 0b001) == WindowControls.NONE);
        check("disabled collapse hover identified", WindowControls.disabledAt(114, 30, 100f, 30f, 0b001) == WindowControls.COLLAPSE);
        check("disabled zoom hover identified", WindowControls.disabledAt(128, 30, 100f, 30f, 0b001) == WindowControls.ZOOM);
        check("enabled control is not reported as disabled", WindowControls.disabledAt(100, 30, 100f, 30f, 0b001) == WindowControls.NONE);
    }

    private static void disabledHintsRemainSpecific() {
        check("collapse hint", "Not used".equals(WindowControls.hintFor(WindowControls.COLLAPSE, "Not used", "No zoom")));
        check("zoom hint", "No zoom".equals(WindowControls.hintFor(WindowControls.ZOOM, "Not used", "No zoom")));
        check("close has no disabled hint", WindowControls.hintFor(WindowControls.CLOSE, "Not used", "No zoom") == null);
        check("outside control has no hint", WindowControls.hintFor(WindowControls.NONE, "Not used", "No zoom") == null);
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
