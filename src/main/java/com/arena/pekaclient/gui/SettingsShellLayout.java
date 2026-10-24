package com.arena.pekaclient.gui;

/**
 * Geometry contract for the one-piece settings shell.
 *
 * <p>The outer rounded panel, its clip, header controls and scroll gutter are deliberately described in one
 * Minecraft-free place. Rendering and hit testing use these values directly, while headless tests exercise the
 * same contract at GUI scales one through four.</p>
 */
public final class SettingsShellLayout {
    /** A 24 px target needs a 30 px header with three px breathing room above and below. */
    public static final float HEADER_HEIGHT = 30f;
    public static final float HEADER_BUTTON_SIZE = 24f;
    public static final float HEADER_INSET = 3f;
    public static final float HEADER_BUTTON_GAP = 4f;
    /** Kept outside the content column, so a thumb can never cover header/card text or controls. */
    public static final float SCROLLBAR_GUTTER = 10f;
    public static final float SCROLL_EDGE_FADE = 8f;
    /** Scroll content always ends with real room below its final card rather than a clipped edge. */
    public static final float FINAL_CARD_PADDING = 12f;

    /** Reused header geometry; no per-frame rectangles are created. */
    public static final class Header {
        public final GuiLayoutToolkit.Rect fullscreen = new GuiLayoutToolkit.Rect();
        public final GuiLayoutToolkit.Rect close = new GuiLayoutToolkit.Rect();
        public float dragLeft;
        public float dragRight;
        public float top;
        public float height;
    }

    private SettingsShellLayout() {
    }

    /** Places exactly Fullscreen and Close targets at the right end of the header. */
    public static void layoutHeader(Header out, GuiLayoutToolkit.Rect panel) {
        if (out == null || panel == null) return;
        float size = Math.min(HEADER_BUTTON_SIZE, Math.max(0f, panel.h - HEADER_INSET * 2f));
        float y = panel.y + Math.max(0f, (HEADER_HEIGHT - size) * .5f);
        float closeX = Math.max(panel.x, panel.right() - HEADER_INSET - size);
        float fullX = Math.max(panel.x, closeX - HEADER_BUTTON_GAP - size);
        out.close.set(closeX, y, size, size);
        out.fullscreen.set(fullX, y, size, size);
        out.dragLeft = panel.x + HEADER_INSET;
        out.dragRight = Math.max(out.dragLeft, fullX - HEADER_BUTTON_GAP);
        out.top = panel.y;
        out.height = Math.min(HEADER_HEIGHT, panel.h);
    }

    /** Returns the card/text width after reserving the permanently separate scrollbar gutter. */
    public static float contentColumnWidth(float viewportWidth) {
        return Math.max(1f, Math.max(0f, viewportWidth) - SCROLLBAR_GUTTER);
    }

    /** The visible thumb lives in the reserved gutter, never over the content column. */
    public static float scrollbarX(GuiLayoutToolkit.Rect viewport) {
        if (viewport == null) return 0f;
        return viewport.right() - SCROLLBAR_GUTTER * .5f - 1f;
    }

    /** The content clip is the outer rounded window exactly: same origin, extent and radius on every corner. */
    public static boolean isSingleRoundedWindow(GuiLayoutToolkit.Rect outer, GuiLayoutToolkit.Rect clip,
                                                 float topLeft, float topRight, float bottomRight, float bottomLeft) {
        return outer != null && clip != null
                && close(outer.x, clip.x) && close(outer.y, clip.y)
                && close(outer.w, clip.w) && close(outer.h, clip.h)
                && close(topLeft, topRight) && close(topLeft, bottomRight) && close(topLeft, bottomLeft);
    }

    /** A control is valid only when it stays inside the header and preserves the promised 24 px hit target. */
    public static boolean isHeaderTargetValid(GuiLayoutToolkit.Rect panel, Header header, GuiLayoutToolkit.Rect target) {
        if (panel == null || header == null || target == null) return false;
        return target.w >= HEADER_BUTTON_SIZE && target.h >= HEADER_BUTTON_SIZE
                && target.x >= panel.x && target.right() <= panel.right()
                && target.y >= header.top && target.bottom() <= header.top + header.height
                && !header.fullscreen.intersects(header.close);
    }

    private static boolean close(float first, float second) {
        return Math.abs(first - second) < .01f;
    }
}
