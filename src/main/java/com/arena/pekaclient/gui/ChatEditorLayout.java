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
        out.viewport.x = x;
        out.viewport.y = y;
        out.viewport.w = Math.max(1f, width);
        out.viewport.h = Math.max(1f, height);
        float total = 0f;
        for (int i = 0; i < SECTION_COUNT; i++) {
            total += HEADER_H;
            boolean open = expanded != null && i < expanded.length && expanded[i];
            int count = rows == null || i >= rows.length ? 0 : Math.max(0, rows[i]);
            if (open && count > 0) total += GAP + count * ROW_H;
            if (i + 1 < SECTION_COUNT) total += GAP;
        }
        out.contentH = total;
        out.maxScroll = Math.max(0f, total - out.viewport.h);
        out.scroll = clamp(requestedScroll, 0f, out.maxScroll);

        float cursor = y - out.scroll;
        for (int i = 0; i < SECTION_COUNT; i++) {
            Rect header = out.headers[i];
            header.x = x;
            header.y = cursor;
            header.w = out.viewport.w;
            header.h = HEADER_H;
            cursor += HEADER_H;
            Rect body = out.bodies[i];
            boolean open = expanded != null && i < expanded.length && expanded[i];
            int count = rows == null || i >= rows.length ? 0 : Math.max(0, rows[i]);
            if (open && count > 0) {
                cursor += GAP;
                body.x = x + PAD;
                body.y = cursor;
                body.w = Math.max(1f, out.viewport.w - PAD * 2f);
                body.h = count * ROW_H;
                cursor += body.h;
            } else {
                body.x = x + PAD;
                body.y = cursor;
                body.w = Math.max(1f, out.viewport.w - PAD * 2f);
                body.h = 0f;
            }
            if (i + 1 < SECTION_COUNT) cursor += GAP;
        }
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : (value > max ? max : value);
    }
}
