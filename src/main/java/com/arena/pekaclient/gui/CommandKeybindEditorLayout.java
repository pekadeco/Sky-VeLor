package com.arena.pekaclient.gui;

/**
 * Responsive, pure geometry for the standalone Command Keybind editor.
 *
 * <p>The editor is intentionally a centered working window rather than a small corner palette. On ordinary
 * desktop-sized GUI coordinates it occupies sixty percent of the screen width; very narrow Android/launcher
 * coordinates safely use the available width and switch each binding to a taller two-line row instead of clipping
 * controls.</p>
 */
public final class CommandKeybindEditorLayout {

    public static final float OUTER_MARGIN = 8f;
    public static final float PANEL_WIDTH_RATIO = .60f;
    public static final float PANEL_HEIGHT_RATIO = .72f;
    public static final float PANEL_MIN_WIDTH = 300f;
    /** High cap retains the requested 55–65% working width even on large desktop GUI scales. */
    public static final float PANEL_MAX_WIDTH = 4096f;
    public static final float PANEL_MIN_HEIGHT = 230f;
    public static final float PANEL_MAX_HEIGHT = 760f;
    public static final float HEADER_HEIGHT = 36f;
    public static final float INTRO_HEIGHT = 34f;
    public static final float FOOTER_HEIGHT = 42f;
    public static final float WIDE_ROW_HEIGHT = 46f;
    public static final float COMPACT_ROW_HEIGHT = 76f;
    /** Below this width the command field moves below the key/Delete pair rather than being squeezed or cut off. */
    public static final float COMPACT_ROW_BREAKPOINT = 420f;

    private CommandKeybindEditorLayout() {
    }

    public static final class Layout {
        public float panelX;
        public float panelY;
        public float panelW;
        public float panelH;
        public float headerH;
        public float introH;
        public float footerH;
        public float contentTop;
        public float contentBottom;
        public float rowH;
        public boolean compactRows;

        public float contentHeight() {
            return Math.max(1f, contentBottom - contentTop);
        }
    }

    /**
     * Centers a contained editor rectangle and reserves non-overlapping title, content, and footer areas. The output
     * is valid even at unusually small scaled resolutions, where the compact row form is preferable to overlap.
     */
    public static Layout layout(Layout out, float screenWidth, float screenHeight) {
        Layout result = out == null ? new Layout() : out;
        float sw = Math.max(1f, screenWidth);
        float sh = Math.max(1f, screenHeight);
        float availableW = Math.max(1f, sw - OUTER_MARGIN * 2f);
        float availableH = Math.max(1f, sh - OUTER_MARGIN * 2f);
        float wantedW = clamp(availableW * PANEL_WIDTH_RATIO, PANEL_MIN_WIDTH, PANEL_MAX_WIDTH);
        float wantedH = clamp(availableH * PANEL_HEIGHT_RATIO, PANEL_MIN_HEIGHT, PANEL_MAX_HEIGHT);
        result.panelW = Math.min(availableW, wantedW);
        result.panelH = Math.min(availableH, wantedH);
        result.panelX = (sw - result.panelW) * .5f;
        result.panelY = (sh - result.panelH) * .5f;

        // Do not let fixed desktop chrome consume a tiny launcher viewport. These proportional caps retain a positive
        // clipped content strip so rows can still scroll rather than being painted through the footer.
        result.headerH = Math.min(HEADER_HEIGHT, result.panelH * .34f);
        result.introH = Math.min(INTRO_HEIGHT, result.panelH * .25f);
        result.footerH = Math.min(FOOTER_HEIGHT, result.panelH * .34f);
        result.contentTop = result.panelY + result.headerH + result.introH;
        result.contentBottom = result.panelY + result.panelH - result.footerH;
        // The proportional chrome caps above leave room in every practical GUI size. Keep an epsilon content strip as
        // a final fail-safe for an impossibly tiny coordinate space so scroll math never receives a negative height.
        if (result.contentBottom <= result.contentTop) result.contentBottom = result.contentTop + .01f;
        result.compactRows = result.panelW < COMPACT_ROW_BREAKPOINT;
        result.rowH = result.compactRows ? COMPACT_ROW_HEIGHT : WIDE_ROW_HEIGHT;
        return result;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
