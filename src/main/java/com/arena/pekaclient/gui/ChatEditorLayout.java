package com.arena.pekaclient.gui;

/**
 * Minecraft-free measured geometry for the Chat Editor's compact dropdown sections. Every visible row is a real
 * rectangle derived from active-font/control metrics rather than a hard-coded baseline; the screen renderer,
 * scissor/input path and headless wide/tall-font tests all consume this one authority.
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
        /** Measured logical rows; widgets and labels are placed from these instead of `row * 28`. */
        public final Rect[][] rows = new Rect[SECTION_COUNT][];
        /** The screen's compact Rect facade mirrors this single shared geometry authority. */
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

    /** Compatibility/default layout using the historic safe 26/28 pixel metrics. */
    public static void update(Layout out, float x, float y, float width, float height, boolean[] expanded,
                              int[] rows, float requestedScroll) {
        float[][] measured = new float[SECTION_COUNT][];
        for (int i = 0; i < SECTION_COUNT; i++) {
            int count = rows == null || i >= rows.length ? 0 : Math.max(0, rows[i]);
            measured[i] = new float[count];
            for (int row = 0; row < count; row++) measured[i][row] = ROW_H;
        }
        update(out, x, y, width, height, expanded, HEADER_H, measured, null, requestedScroll);
    }

    /**
     * Places headers and individually measured rows in one clipped flow. `extraAfter[section][row]` reserves space
     * for an expanded dropdown immediately after that row, so later controls never live underneath its popup.
     */
    public static void update(Layout out, float x, float y, float width, float height, boolean[] expanded,
                              float headerHeight, float[][] rowHeights, float[][] extraAfter,
                              float requestedScroll) {
        if (out == null) return;
        float[] bodyHeights = new float[SECTION_COUNT];
        float[] openness = new float[SECTION_COUNT];
        for (int section = 0; section < SECTION_COUNT; section++) {
            float total = 0f;
            float[] rows = rowHeights == null || section >= rowHeights.length ? null : rowHeights[section];
            for (int row = 0; rows != null && row < rows.length; row++) {
                total += Math.max(1f, rows[row]);
                total += valueAt(extraAfter, section, row);
            }
            bodyHeights[section] = total;
            openness[section] = expanded != null && section < expanded.length && expanded[section] ? 1f : 0f;
        }
        out.shared.layout(x, y, width, height, Math.max(1f, headerHeight), GAP, bodyHeights, openness,
                requestedScroll);
        out.viewport.set(out.shared.viewport.x, out.shared.viewport.y, out.shared.viewport.w, out.shared.viewport.h);
        out.contentH = out.shared.contentHeight;
        out.maxScroll = out.shared.maxScroll;
        out.scroll = out.shared.scroll;
        for (int section = 0; section < SECTION_COUNT; section++) {
            GuiLayoutToolkit.Rect sourceHeader = out.shared.headers[section];
            GuiLayoutToolkit.Rect sourceBody = out.shared.bodies[section];
            out.headers[section].set(sourceHeader.x, sourceHeader.y, sourceHeader.w, sourceHeader.h);
            out.bodies[section].set(sourceBody.x, sourceBody.y, sourceBody.w, sourceBody.h);
            float[] sourceRows = rowHeights == null || section >= rowHeights.length ? null : rowHeights[section];
            ensureRows(out, section, sourceRows == null ? 0 : sourceRows.length);
            float cursor = sourceBody.y;
            boolean visible = openness[section] > 0f;
            for (int row = 0; row < out.rows[section].length; row++) {
                float rowH = Math.max(1f, sourceRows[row]);
                out.rows[section][row].set(sourceBody.x, cursor, sourceBody.w, visible ? rowH : 0f);
                cursor += rowH + valueAt(extraAfter, section, row);
            }
        }
    }

    /** Shifts one section's header/body/rows together after a persisted panel position is resolved. */
    public static void moveSection(Layout layout, int section, float dx, float dy) {
        if (layout == null || section < 0 || section >= SECTION_COUNT) return;
        move(layout.headers[section], dx, dy);
        move(layout.bodies[section], dx, dy);
        for (Rect row : layout.rows[section]) move(row, dx, dy);
    }

    /** True only if two real visible rectangles have a nonzero intersection. Useful to headless layout proofs. */
    public static boolean overlaps(Rect left, Rect right) {
        return left != null && right != null && left.w > 0f && left.h > 0f && right.w > 0f && right.h > 0f
                && left.intersects(right);
    }

    private static void ensureRows(Layout layout, int section, int count) {
        if (layout.rows[section].length == count) return;
        layout.rows[section] = new Rect[count];
        for (int i = 0; i < count; i++) layout.rows[section][i] = new Rect();
    }

    private static float valueAt(float[][] values, int section, int row) {
        if (values == null || section < 0 || section >= values.length || values[section] == null
                || row < 0 || row >= values[section].length) return 0f;
        return Math.max(0f, values[section][row]);
    }

    private static void move(Rect rect, float dx, float dy) {
        if (rect == null) return;
        rect.x += dx;
        rect.y += dy;
    }
}
