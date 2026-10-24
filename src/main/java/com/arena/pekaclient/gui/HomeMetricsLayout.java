package com.arena.pekaclient.gui;

import com.arena.pekaclient.render.GuiStyle;

/** Equal-card Home performance geometry, shared by drawing and the scale-1–4 regression checks. */
public final class HomeMetricsLayout {
    public static final int FPS = 0;
    public static final int MEMORY = 1;
    public static final int PING = 2;
    public static final int COUNT = 3;
    public static final float GAP = GuiStyle.SPACE_8;
    public static final float WIDE_CARD_HEIGHT = 74f;
    public static final float STACKED_CARD_HEIGHT = 66f;
    public static final float STACK_AT_WIDTH = 300f;

    /** Reused rectangles keep layout and paint allocation-free after opening the screen. */
    public static final class Layout {
        public final GuiLayoutToolkit.Rect[] cards = {
                new GuiLayoutToolkit.Rect(), new GuiLayoutToolkit.Rect(), new GuiLayoutToolkit.Rect()
        };
        public float height;
        public boolean stacked;
    }

    private HomeMetricsLayout() { }

    public static void update(Layout out, float x, float y, float width) {
        if (out == null) return;
        float usable = Math.max(1f, width);
        out.stacked = usable < STACK_AT_WIDTH;
        if (out.stacked) {
            for (int i = 0; i < COUNT; i++) out.cards[i].set(x, y + i * (STACKED_CARD_HEIGHT + GAP), usable, STACKED_CARD_HEIGHT);
            out.height = COUNT * STACKED_CARD_HEIGHT + (COUNT - 1) * GAP;
        } else {
            float cardW = Math.max(1f, (usable - GAP * (COUNT - 1)) / COUNT);
            for (int i = 0; i < COUNT; i++) out.cards[i].set(x + i * (cardW + GAP), y, cardW, WIDE_CARD_HEIGHT);
            out.height = WIDE_CARD_HEIGHT;
        }
    }

    public static float heightFor(float width) {
        return Math.max(1f, width) < STACK_AT_WIDTH ? COUNT * STACKED_CARD_HEIGHT + (COUNT - 1) * GAP : WIDE_CARD_HEIGHT;
    }
}
