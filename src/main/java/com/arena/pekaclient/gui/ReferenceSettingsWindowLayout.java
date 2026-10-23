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

    /**
     * Centers a bounded desktop window. On a normal-size screen it uses the requested 85% by 82% footprint; on a
     * tiny GUI scale it gives priority to usable controls and the surrounding screen margin rather than shrinking
     * text below readability. The caller continues to scroll content inside the returned bounds.
     */
    public static GuiLayoutToolkit.Rect layout(GuiLayoutToolkit.Rect out, float screenWidth, float screenHeight,
                                               boolean expanded, boolean minimized) {
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
        result.set((sw - w) * .5f, (sh - h) * .5f, w, h);
        return result;
    }

    /** A minimized chrome strip must still expose all three small desktop controls. */
    public static boolean canShowDesktopControls(GuiLayoutToolkit.Rect bounds) {
        return bounds != null && bounds.w >= 42f && bounds.h >= 14f;
    }
}
