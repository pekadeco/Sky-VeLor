package com.arena.pekaclient.gui;

/** Pure geometry for the three GUI-only General-category metric cards. */
public final class GeneralMetricsLayout {
    public static final int FPS = 0;
    public static final int PING = 1;
    public static final int MEMORY = 2;
    public static final int COUNT = 3;
    private static final float GAP = 5f;
    private static final float COMPACT_WIDTH = 250f;

    public static final class Layout {
        public final GuiLayoutToolkit.Rect[] cards = {
                new GuiLayoutToolkit.Rect(), new GuiLayoutToolkit.Rect(), new GuiLayoutToolkit.Rect()
        };
        public float height;
        public boolean stacked;
    }

    private GeneralMetricsLayout() { }

    /** Reuses the caller-owned layout/card rectangles so drawing creates no geometry allocations. */
    public static void update(Layout out, float x, float y, float width) {
        if (out == null) return;
        float w = Math.max(1f, width);
        out.stacked = w < COMPACT_WIDTH;
        if (out.stacked) {
            float h = 25f;
            for (int i = 0; i < COUNT; i++) out.cards[i].set(x, y + i * (h + GAP), w, h);
            out.height = COUNT * h + (COUNT - 1) * GAP;
        } else {
            float cardW = Math.max(1f, (w - GAP * (COUNT - 1)) / COUNT);
            for (int i = 0; i < COUNT; i++) out.cards[i].set(x + i * (cardW + GAP), y, cardW, 44f);
            out.height = 44f;
        }
    }
}
