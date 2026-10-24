package com.arena.pekaclient.gui;

/** Headless shell geometry proof: one rounded clip, separated gutter and reachable header at GUI scales one through four. */
public final class SettingsShellLayoutTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        roundedClipMatchesOuterWindowAtEveryGuiScale();
        headerHasExactlyTwoValidTwentyFourPixelTargetsAtEveryGuiScale();
        scrollbarGutterCannotOverlapTheContentColumn();
        System.out.println("settings shell layout: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void roundedClipMatchesOuterWindowAtEveryGuiScale() {
        for (int scale = 1; scale <= 4; scale++) {
            float sw = 1280f / scale;
            float sh = 720f / scale;
            GuiLayoutToolkit.Rect panel = ReferenceSettingsWindowLayout.layout(new GuiLayoutToolkit.Rect(), sw, sh, false);
            GuiLayoutToolkit.Rect clip = new GuiLayoutToolkit.Rect().set(panel.x, panel.y, panel.w, panel.h);
            check("single equal-radius shell at scale " + scale,
                    SettingsShellLayout.isSingleRoundedWindow(panel, clip, 12f, 12f, 12f, 12f));
            check("final card padding is 12 px at scale " + scale,
                    SettingsShellLayout.FINAL_CARD_PADDING == 12f);
        }
    }

    private static void headerHasExactlyTwoValidTwentyFourPixelTargetsAtEveryGuiScale() {
        for (int scale = 1; scale <= 4; scale++) {
            GuiLayoutToolkit.Rect panel = ReferenceSettingsWindowLayout.layout(new GuiLayoutToolkit.Rect(),
                    1280f / scale, 720f / scale, false);
            SettingsShellLayout.Header header = new SettingsShellLayout.Header();
            SettingsShellLayout.layoutHeader(header, panel);
            check("fullscreen target is 24 px and inside header at scale " + scale,
                    SettingsShellLayout.isHeaderTargetValid(panel, header, header.fullscreen));
            check("close target is 24 px and inside header at scale " + scale,
                    SettingsShellLayout.isHeaderTargetValid(panel, header, header.close));
            check("drag span stops before controls at scale " + scale,
                    header.dragRight <= header.fullscreen.x && header.dragRight >= header.dragLeft);
        }
    }

    private static void scrollbarGutterCannotOverlapTheContentColumn() {
        GuiLayoutToolkit.Rect viewport = new GuiLayoutToolkit.Rect().set(40f, 50f, 380f, 250f);
        float contentW = SettingsShellLayout.contentColumnWidth(viewport.w);
        float thumbX = SettingsShellLayout.scrollbarX(viewport);
        check("content column excludes the permanent 10 px gutter", contentW == 370f);
        check("thumb begins after content column", thumbX >= viewport.x + contentW);
        check("thumb stays in viewport", thumbX + 2f <= viewport.right());
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++; else { failed++; System.err.println("FAIL " + name); }
    }
}
