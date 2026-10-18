package com.arena.pekaclient.gui;

/**
 * Minecraft-free measured geometry for the deliberately smaller Default Chat editor. The real screen supplies active
 * font/control measurements and this class keeps every visible header and row in one clipped, non-overlapping flow.
 */
public final class ChatEditorLayout {
    public static final int BACKGROUND = 0;
    public static final int MESSAGES = 1;
    public static final int TABS = 2;
    public static final int MENU_HIGHLIGHT = 3;
    public static final int RESET = 4;
    public static final int SECTION_COUNT = 5;
    public static final float HEADER_H = 26f;
    public static final float ROW_H = 28f;
    public static final float GAP = 5f;
    public static final float PAD = 10f;

    public static final class Rect {
        public float x;
        public float y;
        public float w;
        public float h;

        public Rect set(float nextX, float nextY, float nextW, float nextH) {
            x = nextX;
            y = nextY;
            w = Math.max(0f, nextW);
            h = Math.max(0f, nextH);
            return this;
        }

        public boolean intersects(Rect other) {
            return other != null && x < other.x + other.w && x + w > other.x && y < other.y + other.h
                    && y + h > other.y;
        }
    }

    public static final class Layout {
        public final Rect viewport = new Rect();
        public final Rect[] headers = new Rect[SECTION_COUNT];
        public final Rect[] bodies = new Rect[SECTION_COUNT];
        public final Rect[][] rows = new Rect[SECTION_COUNT][];
        private final GuiLayoutToolkit.Sections shared = new GuiLayoutToolkit.Sections(SECTION_COUNT);
        public float contentH;
        public float maxScroll;
        public float scroll;

        public Layout() {
            for (int i = 0; i < SECTION_COUNT; i++) {
                headers[i] = new Rect();
                bodies[i] = new Rect();
                rows[i] = new Rect[0];
            }
        }
    }

    private ChatEditorLayout() {
    }

    public static void update(Layout out, float x, float y, float width, float height, boolean[] expanded,
                              float headerHeight, float[][] rowHeights, float[][] extraAfter, float requestedScroll) {
        if (out == null) return;
        float[] bodyHeights = new float[SECTION_COUNT];
        float[] openness = new float[SECTION_COUNT];
        for (int section = 0; section < SECTION_COUNT; section++) {
            float total = 0f;
            float[] rows = rowHeights == null || section >= rowHeights.length ? null : rowHeights[section];
            for (int row = 0; rows != null && row < rows.length; row++) {
                total += Math.max(1f, rows[row]) + at(extraAfter, section, row);
            }
            bodyHeights[section] = total;
            openness[section] = expanded != null && section < expanded.length && expanded[section] ? 1f : 0f;
        }
        out.shared.layout(x, y, width, height, Math.max(1f, headerHeight), GAP, bodyHeights, openness, requestedScroll);
        out.viewport.set(out.shared.viewport.x, out.shared.viewport.y, out.shared.viewport.w, out.shared.viewport.h);
        out.contentH = out.shared.contentHeight;
        out.maxScroll = out.shared.maxScroll;
        out.scroll = out.shared.scroll;
        for (int section = 0; section < SECTION_COUNT; section++) {
            GuiLayoutToolkit.Rect h = out.shared.headers[section];
            GuiLayoutToolkit.Rect b = out.shared.bodies[section];
            out.headers[section].set(h.x, h.y, h.w, h.h);
            out.bodies[section].set(b.x, b.y, b.w, b.h);
            float[] source = rowHeights == null || section >= rowHeights.length ? null : rowHeights[section];
            ensureRows(out, section, source == null ? 0 : source.length);
            float cursor = b.y;
            for (int row = 0; row < out.rows[section].length; row++) {
                float rowH = Math.max(1f, source[row]);
                out.rows[section][row].set(b.x, cursor, b.w, openness[section] > 0f ? rowH : 0f);
                cursor += rowH + at(extraAfter, section, row);
            }
        }
    }

    public static boolean overlaps(Rect left, Rect right) {
        return left != null && right != null && left.w > 0f && left.h > 0f && right.w > 0f && right.h > 0f
                && left.intersects(right);
    }

    private static void ensureRows(Layout layout, int section, int count) {
        if (layout.rows[section].length == count) return;
        layout.rows[section] = new Rect[count];
        for (int i = 0; i < count; i++) layout.rows[section][i] = new Rect();
    }

    private static float at(float[][] values, int section, int row) {
        return values == null || section < 0 || section >= values.length || values[section] == null
                || row < 0 || row >= values[section].length ? 0f : Math.max(0f, values[section][row]);
    }
}
