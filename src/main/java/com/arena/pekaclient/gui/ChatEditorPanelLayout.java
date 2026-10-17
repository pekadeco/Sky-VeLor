package com.arena.pekaclient.gui;

/**
 * Persistable geometry for Chat Editor setting panels. Coordinates are stored as hundredths of a percent so layouts
 * remain independent per Minecraft GUI scale, but all mutation is performed in pixels against the current viewport.
 *
 * ChatEditorScreen uses this for its title card and section cards; keeping drag/clamp/reset logic here gives both the
 * in-game screen and headless tests one authoritative implementation.
 */
public final class ChatEditorPanelLayout {

    public static final int SCALE_MIN = 1;
    public static final int SCALE_MAX = 4;
    public static final int POSITION_MIN = 0;
    public static final int POSITION_MAX = 10000;

    public static final class Rect {
        public float x;
        public float y;
        public float w;
        public float h;

        public boolean contains(float mouseX, float mouseY) {
            return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        }
    }

    /** A panel's scale-specific position and default, plus a compact collapse state. */
    public static final class State {
        private final int defaultX;
        private final int defaultY;
        private final int[] x = new int[SCALE_MAX + 1];
        private final int[] y = new int[SCALE_MAX + 1];
        private final boolean[] collapsed = new boolean[SCALE_MAX + 1];

        public State(int defaultXPercent, int defaultYPercent) {
            defaultX = clampPercent(defaultXPercent);
            defaultY = clampPercent(defaultYPercent);
            resetAll();
        }

        public int x(int scale) {
            return x[index(scale)];
        }

        public int y(int scale) {
            return y[index(scale)];
        }

        public boolean collapsed(int scale) {
            return collapsed[index(scale)];
        }

        public void set(int scale, int nextX, int nextY, boolean nextCollapsed) {
            int slot = index(scale);
            x[slot] = clampPercent(nextX);
            y[slot] = clampPercent(nextY);
            collapsed[slot] = nextCollapsed;
        }

        public void move(int scale, float leftPx, float topPx, float panelW, float panelH,
                         float viewportW, float viewportH) {
            float maxX = Math.max(0f, viewportW - Math.max(1f, panelW));
            float maxY = Math.max(0f, viewportH - Math.max(1f, panelH));
            int slot = index(scale);
            x[slot] = clampPercent(Math.round(clamp(leftPx, 0f, maxX) * 10000f / Math.max(1f, viewportW)));
            y[slot] = clampPercent(Math.round(clamp(topPx, 0f, maxY) * 10000f / Math.max(1f, viewportH)));
        }

        public void toggleCollapsed(int scale) {
            int slot = index(scale);
            collapsed[slot] = !collapsed[slot];
        }

        /** Double-clicking the title restores just this GUI scale and expands the panel. */
        public void resetScale(int scale) {
            int slot = index(scale);
            x[slot] = defaultX;
            y[slot] = defaultY;
            collapsed[slot] = false;
        }

        public void resetAll() {
            for (int scale = SCALE_MIN; scale <= SCALE_MAX; scale++) resetScale(scale);
        }
    }

    private ChatEditorPanelLayout() {
    }

    /** Resolves stored percentage coordinates into an on-screen, clamped pixel rect. */
    public static void resolve(Rect out, State state, int scale, float panelW, float panelH,
                               float viewportW, float viewportH) {
        if (out == null || state == null) return;
        float safeW = Math.max(1f, viewportW);
        float safeH = Math.max(1f, viewportH);
        out.w = Math.min(Math.max(1f, panelW), safeW);
        out.h = Math.min(Math.max(1f, panelH), safeH);
        out.x = clamp(state.x(scale) * safeW / 10000f, 0f, Math.max(0f, safeW - out.w));
        out.y = clamp(state.y(scale) * safeH / 10000f, 0f, Math.max(0f, safeH - out.h));
    }

    private static int index(int scale) {
        return scale < SCALE_MIN ? SCALE_MIN : (scale > SCALE_MAX ? SCALE_MAX : scale);
    }

    private static int clampPercent(int value) {
        return value < POSITION_MIN ? POSITION_MIN : (value > POSITION_MAX ? POSITION_MAX : value);
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : (value > max ? max : value);
    }
}
