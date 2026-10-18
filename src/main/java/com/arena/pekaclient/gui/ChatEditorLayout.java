package com.arena.pekaclient.gui;

/**
 * Minecraft-free stacked geometry for the Chat Editor's six compact dropdown sections. It has one vertical flow and
 * one clipped viewport, making header/body overlap impossible at every GUI scale.
 */
public final class ChatEditorLayout {

    public static final int WINDOWS = 0;
    public static final int BACKGROUND = 1;
    public static final int MESSAGES = 2;
    public static final int FONT = 3;
    public static final int TABS = 4;
    public static final int HOVER_MENU = 5;
    public static final int SECTION_COUNT = 6;
    public static final float HEADER_H = 26f;
    public static final float ROW_H = 28f;
    public static final float GAP = 5f;
    public static final float PAD = 10f;

    public static final class Rect {
        public float x;
        public float y;
        public float w;
        public float h;

        public boolean intersects(Rect other) {
            return other != null && x < other.x + other.w && x + w > other.x && y < other.y + other.h
                    && y + h > other.y;
        }
    }

    public static final class Layout {
        public final Rect viewport = new Rect();
        public final Rect[] headers = new Rect[SECTION_COUNT];
        public final Rect[] bodies = new Rect[SECTION_COUNT];
        /** The screen's compact Rect facade mirrors this single shared geometry authority. */
        private final GuiLayoutToolkit.Sections shared = new GuiLayoutToolkit.Sections(SECTION_COUNT);
        public float contentH;
        public float maxScroll;
        public float scroll;

        public Layout() {
            for (int i = 0; i < SECTION_COUNT; i++) {
                headers[i] = new Rect();
                bodies[i] = new Rect();
            }
        }
    }

    private ChatEditorLayout() {
    }

    /**
     * Places section headers and their expanded bodies in one flow. rows contains body row counts for each section.
     * Hidden/collapsed bodies receive height zero and cannot intercept input.
     */
    public static void update(Layout out, float x, float y, float width, float height, boolean[] expanded,
                              int[] rows, float requestedScroll) {
        if (out == null) return;
        float[] bodyHeights = new float[SECTION_COUNT];
        float[] openness = new float[SECTION_COUNT];
        for (int i = 0; i < SECTION_COUNT; i++) {
            int count = rows == null || i >= rows.length ? 0 : Math.max(0, rows[i]);
            bodyHeights[i] = count * ROW_H;
            openness[i] = expanded != null && i < expanded.length && expanded[i] ? 1f : 0f;
        }
        // GuiLayoutToolkit owns the measured flow, collapse height, clipping viewport and scroll clamp. This
        // compatibility facade exists only because ChatEditorScreen keeps its historic Rect type for drag persistence.
        out.shared.layout(x, y, width, height, HEADER_H, GAP, bodyHeights, openness, requestedScroll);
        out.viewport.x = out.shared.viewport.x;
        out.viewport.y = out.shared.viewport.y;
        out.viewport.w = out.shared.viewport.w;
        out.viewport.h = out.shared.viewport.h;
        out.contentH = out.shared.contentHeight;
        out.maxScroll = out.shared.maxScroll;
        out.scroll = out.shared.scroll;
        for (int i = 0; i < SECTION_COUNT; i++) {
            GuiLayoutToolkit.Rect sourceHeader = out.shared.headers[i];
            GuiLayoutToolkit.Rect sourceBody = out.shared.bodies[i];
            out.headers[i].x = sourceHeader.x;
            out.headers[i].y = sourceHeader.y;
            out.headers[i].w = sourceHeader.w;
            out.headers[i].h = sourceHeader.h;
            out.bodies[i].x = sourceBody.x;
            out.bodies[i].y = sourceBody.y;
            out.bodies[i].w = sourceBody.w;
            out.bodies[i].h = sourceBody.h;
        }
    }

}
