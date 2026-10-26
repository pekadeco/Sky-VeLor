package com.arena.pekaclient.gui;

/**
 * Pure geometry contract for the native settings window. Keeping the size policy free of Minecraft classes lets the
 * screen use exactly the same bounds for drawing and hit testing, and lets the low-GUI-scale behavior be checked
 * without starting a client.
 */
public final class ReferenceSettingsWindowLayout {

    /** Keeps a visible strip of world around both small and fullscreen rounded windows. */
    /** The requested large-layout breathing room around a centered or fullscreen rounded window. */
    public static final float OUTER_MARGIN = 8f;
    public static final float NORMAL_WIDTH_RATIO = .85f;
    public static final float NORMAL_HEIGHT_RATIO = .82f;
    /** Fullscreen occupies the complete safe area while retaining the rounded-window margin. */
    public static final float FULLSCREEN_WIDTH_RATIO = 1f;
    public static final float FULLSCREEN_HEIGHT_RATIO = 1f;
    public static final float NORMAL_MAX_WIDTH = 820f;
    public static final float NORMAL_MAX_HEIGHT = 520f;
    public static final float MIN_WINDOW_WIDTH = 280f;
    public static final float MIN_WINDOW_HEIGHT = 210f;

    private ReferenceSettingsWindowLayout() {
    }

    /** Centers a bounded small/fullscreen window without a user drag offset. */
    public static GuiLayoutToolkit.Rect layout(GuiLayoutToolkit.Rect out, float screenWidth, float screenHeight,
                                               boolean fullscreen) {
        return layout(out, screenWidth, screenHeight, fullscreen, 0f, 0f);
    }

    /**
     * Lays out a bounded small/fullscreen window with a title-bar drag offset from its natural centre. The same
     * constrained bounds drive drawing and hit testing, so a moved window cannot leave stale controls behind. The
     * offset is presentation-only; opening a fresh settings screen still starts centred.
     */
    public static GuiLayoutToolkit.Rect layout(GuiLayoutToolkit.Rect out, float screenWidth, float screenHeight,
                                               boolean fullscreen, float offsetX, float offsetY) {
        GuiLayoutToolkit.Rect result = out == null ? new GuiLayoutToolkit.Rect() : out;
        float sw = Math.max(1f, screenWidth);
        float sh = Math.max(1f, screenHeight);
        float availableW = Math.max(1f, sw - OUTER_MARGIN * 2f);
        float availableH = Math.max(1f, sh - OUTER_MARGIN * 2f);
        float wantedW = fullscreen ? availableW * FULLSCREEN_WIDTH_RATIO
                : Math.max(MIN_WINDOW_WIDTH, Math.min(NORMAL_MAX_WIDTH, availableW * NORMAL_WIDTH_RATIO));
        float wantedH = fullscreen ? availableH * FULLSCREEN_HEIGHT_RATIO
                : Math.max(MIN_WINDOW_HEIGHT, Math.min(NORMAL_MAX_HEIGHT, availableH * NORMAL_HEIGHT_RATIO));
        float w = Math.min(availableW, wantedW);
        float h = Math.min(availableH, wantedH);
        // Fullscreen is intentionally fixed to its safe area. Small-window drags retain their exact offset when the
        // user returns from fullscreen, rather than being overwritten by the temporary fullscreen bounds.
        float x = fullscreen ? centeredX(sw, w) : clampX(centeredX(sw, w) + offsetX, sw, w);
        float y = fullscreen ? centeredY(sh, h) : clampY(centeredY(sh, h) + offsetY, sh, h);
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
}
