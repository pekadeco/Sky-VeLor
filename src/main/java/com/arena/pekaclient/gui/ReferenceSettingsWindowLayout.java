package com.arena.pekaclient.gui;

/**
 * Pure geometry contract for the native reference settings window. Keeping the size policy free of Minecraft
 * classes lets the screen use exactly the same bounds for drawing and hit testing, and lets the low-GUI-scale
 * behavior be checked without starting a client.
 */
public final class ReferenceSettingsWindowLayout {

    /** Keeps a visible strip of world around the desktop window whenever the GUI is large enough. */
    public static final float OUTER_MARGIN = 12f;
    public static final float NORMAL_WIDTH_RATIO = .85f;
    public static final float NORMAL_HEIGHT_RATIO = .82f;
    public static final float EXPANDED_WIDTH_RATIO = .96f;
    public static final float EXPANDED_HEIGHT_RATIO = .96f;
    public static final float NORMAL_MAX_WIDTH = 820f;
    public static final float NORMAL_MAX_HEIGHT = 520f;
    public static final float EXPANDED_MAX_WIDTH = 920f;
    public static final float EXPANDED_MAX_HEIGHT = 640f;
    public static final float MIN_WINDOW_WIDTH = 280f;
    public static final float MIN_WINDOW_HEIGHT = 210f;
    public static final float MINIMIZED_MAX_WIDTH = 360f;
    public static final float MINIMIZED_HEIGHT = 46f;

    private ReferenceSettingsWindowLayout() {
    }

    /** Centers a bounded desktop window without a user drag offset. */
    public static GuiLayoutToolkit.Rect layout(GuiLayoutToolkit.Rect out, float screenWidth, float screenHeight,
                                               boolean expanded, boolean minimized) {
        return layout(out, screenWidth, screenHeight, expanded, minimized, 0f, 0f);
    }

    /**
     * Lays out a bounded desktop window with a title-bar drag offset from its natural centre. The same constrained
     * bounds drive drawing and hit testing, so a moved, expanded, or minimized window cannot leave stale controls
     * behind. The offset is intentionally presentation-only; opening a fresh settings screen still starts centred.
     */
    public static GuiLayoutToolkit.Rect layout(GuiLayoutToolkit.Rect out, float screenWidth, float screenHeight,
                                               boolean expanded, boolean minimized, float offsetX, float offsetY) {
        GuiLayoutToolkit.Rect result = out == null ? new GuiLayoutToolkit.Rect() : out;
        float sw = Math.max(1f, screenWidth);
        float sh = Math.max(1f, screenHeight);
        float availableW = Math.max(1f, sw - OUTER_MARGIN * 2f);
        float availableH = Math.max(1f, sh - OUTER_MARGIN * 2f);

        float wantedW;
        float wantedH;
        if (minimized) {
            wantedW = Math.min(MINIMIZED_MAX_WIDTH, Math.max(220f, availableW * .58f));
            wantedH = MINIMIZED_HEIGHT;
        } else {
            float ratioW = expanded ? EXPANDED_WIDTH_RATIO : NORMAL_WIDTH_RATIO;
            float ratioH = expanded ? EXPANDED_HEIGHT_RATIO : NORMAL_HEIGHT_RATIO;
            float maxW = expanded ? EXPANDED_MAX_WIDTH : NORMAL_MAX_WIDTH;
            float maxH = expanded ? EXPANDED_MAX_HEIGHT : NORMAL_MAX_HEIGHT;
            wantedW = Math.max(MIN_WINDOW_WIDTH, Math.min(maxW, availableW * ratioW));
            wantedH = Math.max(MIN_WINDOW_HEIGHT, Math.min(maxH, availableH * ratioH));
        }

        float w = Math.min(availableW, wantedW);
        float h = Math.min(availableH, wantedH);
        float x = clampX(centeredX(sw, w) + offsetX, sw, w);
        float y = clampY(centeredY(sh, h) + offsetY, sh, h);
        result.set(x, y, w, h);
        return result;
    }

    /** Natural centered X position for a known panel width. */
    public static float centeredX(float screenWidth, float panelWidth) {
        return (Math.max(1f, screenWidth) - Math.max(0f, panelWidth)) * .5f;
    }

    /** Natural centered Y position for a known panel height. */
    public static float centeredY(float screenHeight, float panelHeight) {
        return (Math.max(1f, screenHeight) - Math.max(0f, panelHeight)) * .5f;
    }

    /** Constrains an X coordinate so dragging never loses the window beyond the visible world border. */
    public static float clampX(float x, float screenWidth, float panelWidth) {
        return clampCoordinate(x, screenWidth, panelWidth);
    }

    /** Constrains a Y coordinate so dragging never loses the window beyond the visible world border. */
    public static float clampY(float y, float screenHeight, float panelHeight) {
        return clampCoordinate(y, screenHeight, panelHeight);
    }

    private static float clampCoordinate(float position, float screen, float extent) {
        float usableScreen = Math.max(1f, screen);
        float usableExtent = Math.max(0f, Math.min(usableScreen, extent));
        // Tiny GUI scales may consume the whole screen; in that case no artificial margin can exist.
        float margin = Math.min(OUTER_MARGIN, Math.max(0f, (usableScreen - usableExtent) * .5f));
        float min = margin;
        float max = Math.max(min, usableScreen - usableExtent - margin);
        return Math.max(min, Math.min(max, position));
    }

    /** A minimized chrome strip must still expose all three small desktop controls. */
    public static boolean canShowDesktopControls(GuiLayoutToolkit.Rect bounds) {
        return bounds != null && bounds.w >= 42f && bounds.h >= 14f;
    }
}
