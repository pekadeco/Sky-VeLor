package com.arena.pekaclient.gui;

/**
 * Shared, Minecraft-free geometry for the HUD editor inspector. Every panel uses the same fixed row height,
 * left-label/right-control columns, clipping viewport, and scrollbar gutter; tests can exercise it at each GUI
 * scale without opening a game window.
 */
public final class HudEditorLayout {

    public static final float ROW_HEIGHT = 30f;
    public static final float ROW_GAP = 4f;
    public static final float CARD_PADDING = 10f;
    public static final float HEADER_HEIGHT = 26f;
    private static final float CARD_MAX_WIDTH = 312f;
    private static final float LABEL_GAP = 8f;
    private static final float SCROLLBAR_GUTTER = 6f;

    private HudEditorLayout() {
    }

    /** Mutable per-screen metrics, reused by the live editor to avoid allocations while drawing. */
    public static final class Metrics {
        public float cardX;
        public float cardY;
        public float cardW;
        public float cardH;
        public float visibleTop;
        public float visibleBottom;
        public float viewportH;
        public float contentH;
        public float maxScroll;
        public float scroll;
        public float innerX;
        public float innerW;
        public float controlColumnW;
        public float scrollbarX;
        public float scrollbarY;
        public float scrollbarW;
        public float scrollbarH;
    }

    /** Mutable row bounds. The live screen stores one in each entry; tests use the same math. */
    public static final class Bounds {
        public float rowX;
        public float rowY;
        public float rowW;
        public float rowH;
        public float labelX;
        public float labelY;
        public float labelW;
        public float controlX;
        public float controlY;
        public float controlW;
        public float controlH;
    }

    /**
     * Recomputes an inspector card inside the available editor window. The supplied top is the first safe pixel
     * below the editor bar; content is always clipped to the viewport even when the card becomes short.
     */
    public static void update(Metrics out, float windowX, float windowY, float windowW, float windowH,
                              float top, int rowCount, float requestedScroll, boolean hidden,
                              float cardOffsetX, float cardOffsetY) {
        int count = Math.max(0, rowCount);
        float maxCardW = Math.max(96f, windowW - 8f);
        out.cardW = Math.min(CARD_MAX_WIDTH, maxCardW);
        out.contentH = count == 0 ? 0f : count * ROW_HEIGHT + (count - 1) * ROW_GAP;

        float bottom = windowY + windowH - 8f;
        float available = Math.max(0f, bottom - top);
        float bodyAvailable = Math.max(32f, available - HEADER_HEIGHT);
        float wantedBody = out.contentH + CARD_PADDING * 2f;
        float bodyH = hidden ? 0f : Math.max(32f, Math.min(bodyAvailable, wantedBody));
        out.cardH = HEADER_HEIGHT + bodyH;
        if (out.cardH > windowH) {
            out.cardH = Math.max(HEADER_HEIGHT, windowH);
        }

        float baseX = windowX + 10f;
        float baseY = bottom - out.cardH;
        out.cardX = clamp(baseX + cardOffsetX, windowX, windowX + windowW - out.cardW);
        out.cardY = clamp(baseY + cardOffsetY, windowY, windowY + windowH - out.cardH);
        if (hidden) {
            out.visibleTop = out.cardY + HEADER_HEIGHT;
            out.visibleBottom = out.visibleTop;
        } else {
            out.visibleTop = out.cardY + HEADER_HEIGHT + CARD_PADDING;
            out.visibleBottom = Math.max(out.visibleTop, out.cardY + out.cardH - CARD_PADDING);
        }
        out.viewportH = Math.max(0f, out.visibleBottom - out.visibleTop);
        out.maxScroll = Math.max(0f, out.contentH - out.viewportH);
        out.scroll = clamp(requestedScroll, 0f, out.maxScroll);

        boolean hasScrollbar = out.maxScroll > 0f;
        out.innerX = out.cardX + CARD_PADDING;
        out.innerW = Math.max(1f, out.cardW - CARD_PADDING * 2f - (hasScrollbar ? SCROLLBAR_GUTTER : 0f));
        float maximumControl = Math.max(48f, out.innerW - LABEL_GAP - 36f);
        out.controlColumnW = Math.min(maximumControl, Math.max(70f, Math.min(150f, out.innerW * 0.54f)));

        out.scrollbarW = hasScrollbar ? 2.5f : 0f;
        out.scrollbarH = hasScrollbar
                ? Math.max(16f, out.viewportH * out.viewportH / Math.max(1f, out.contentH)) : 0f;
        out.scrollbarX = out.cardX + out.cardW - 4f;
        out.scrollbarY = hasScrollbar
                ? out.visibleTop + (out.scroll / Math.max(0.001f, out.maxScroll)) * (out.viewportH - out.scrollbarH)
                : out.visibleTop;
    }

    /** Lays one fixed-height row into the shared left-label/right-control grid. */
    public static void row(Metrics metrics, int index, boolean fullWidth, float naturalControlW,
                           boolean stretchControl, float controlH, Bounds out) {
        float rowY = metrics.visibleTop + index * (ROW_HEIGHT + ROW_GAP) - metrics.scroll;
        out.rowX = metrics.innerX;
        out.rowY = rowY;
        out.rowW = metrics.innerW;
        out.rowH = ROW_HEIGHT;
        out.controlH = Math.min(Math.max(1f, controlH), ROW_HEIGHT);
        out.controlY = rowY + (ROW_HEIGHT - out.controlH) * 0.5f;
        if (fullWidth) {
            out.labelX = metrics.innerX;
            out.labelY = rowY;
            out.labelW = 0f;
            out.controlX = metrics.innerX;
            out.controlW = metrics.innerW;
            return;
        }

        float wanted = stretchControl ? metrics.controlColumnW : Math.min(naturalControlW, metrics.controlColumnW);
        out.controlW = Math.max(1f, wanted);
        out.controlX = metrics.innerX + metrics.innerW - out.controlW;
        out.labelX = metrics.innerX;
        out.labelY = rowY + (ROW_HEIGHT - 9f) * 0.5f + 1f;
        out.labelW = Math.max(0f, out.controlX - LABEL_GAP - out.labelX);
    }

    public static boolean intersects(Bounds a, Bounds b) {
        return a.rowX < b.rowX + b.rowW && a.rowX + a.rowW > b.rowX
                && a.rowY < b.rowY + b.rowH && a.rowY + a.rowH > b.rowY;
    }

    private static float clamp(float value, float min, float max) {
        if (max < min) return min;
        return value < min ? min : (value > max ? max : value);
    }
}
