package com.arena.pekaclient.gui;

/** Headless geometry checks for the centered, launcher-safe Command Keybind editor window. */
public final class CommandKeybindEditorLayoutTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        desktopWindowUsesCenteredSixtyPercentWorkingArea();
        narrowLauncherWindowStaysContainedAndUsesCompactRows();
        chromeContentAndFooterNeverOverlap();
        done();
    }

    private static void desktopWindowUsesCenteredSixtyPercentWorkingArea() {
        CommandKeybindEditorLayout.Layout layout = CommandKeybindEditorLayout.layout(null, 1000f, 700f);
        float ratio = layout.panelW / 1000f;
        check("desktop width stays in the requested 55 to 65 percent range", ratio >= .55f && ratio <= .65f);
        check("desktop panel is centered", nearly(layout.panelX, (1000f - layout.panelW) * .5f)
                && nearly(layout.panelY, (700f - layout.panelH) * .5f));
        check("desktop uses normal one-line rows", !layout.compactRows && layout.rowH == CommandKeybindEditorLayout.WIDE_ROW_HEIGHT);
        CommandKeybindEditorLayout.Layout large = CommandKeybindEditorLayout.layout(null, 1920f, 1080f);
        float largeRatio = large.panelW / 1920f;
        check("large desktop retains the requested 55 to 65 percent width", largeRatio >= .55f && largeRatio <= .65f);
    }

    private static void narrowLauncherWindowStaysContainedAndUsesCompactRows() {
        CommandKeybindEditorLayout.Layout layout = CommandKeybindEditorLayout.layout(null, 320f, 180f);
        check("narrow panel stays inside the safe viewport", layout.panelX >= CommandKeybindEditorLayout.OUTER_MARGIN - .01f
                && layout.panelY >= CommandKeybindEditorLayout.OUTER_MARGIN - .01f
                && layout.panelX + layout.panelW <= 320.01f - CommandKeybindEditorLayout.OUTER_MARGIN
                && layout.panelY + layout.panelH <= 180.01f - CommandKeybindEditorLayout.OUTER_MARGIN);
        check("narrow launcher switches to two-line rows instead of clipping controls", layout.compactRows
                && layout.rowH == CommandKeybindEditorLayout.COMPACT_ROW_HEIGHT);
        check("narrow panel remains centered", nearly(layout.panelX, (320f - layout.panelW) * .5f)
                && nearly(layout.panelY, (180f - layout.panelH) * .5f));
    }

    private static void chromeContentAndFooterNeverOverlap() {
        float[][] screens = {{160f, 120f}, {480f, 270f}, {854f, 480f}, {1920f, 1080f}};
        for (int i = 0; i < screens.length; i++) {
            CommandKeybindEditorLayout.Layout layout = CommandKeybindEditorLayout.layout(null, screens[i][0], screens[i][1]);
            check("positive clipped content height for screen " + i, layout.contentHeight() > 0f);
            check("content follows title and intro for screen " + i,
                    layout.contentTop >= layout.panelY + layout.headerH + layout.introH - .01f);
            check("footer begins at or after the content for screen " + i,
                    layout.contentBottom >= layout.contentTop && layout.contentBottom <= layout.panelY + layout.panelH + .02f);
        }
    }

    private static boolean nearly(float a, float b) {
        return Math.abs(a - b) < .02f;
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++;
        else { failed++; System.err.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("command keybind editor layout: passed " + passed + ", failed " + failed);
        if (failed > 0) throw new AssertionError("command keybind editor layout failures: " + failed);
    }
}
