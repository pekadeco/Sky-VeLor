package com.arena.pekaclient.feature;

/**
 * Shared, Minecraft-free interaction contract for every Chat QoL surface.  The runtime supplies actual component
 * objects, while this class owns the parts that must be identical for Default Chat, Chat 1 and Chat 2: bounds,
 * hover easing, menu geometry, labels and a single wheel-to-scroll calculation.
 *
 * Keeping this model free of Minecraft classes makes the coordinate and input rules deterministic in the standalone
 * suite and prevents a native-window-only implementation from quietly diverging from vanilla-layout chat.
 */
public final class ChatInteractionModel {

    public static final int DEFAULT_CHAT = 0;
    public static final int CHAT_1 = 1;
    public static final int CHAT_2 = 2;
    public static final int SURFACE_COUNT = 3;

    /** The only two context-menu labels. Keep these literals central so alternate copy paths cannot reappear. */
    public static final String COPY_TO_CLIPBOARD = "Copy to clipboard";
    public static final String COPY_TO_CHAT_BOX = "Copy to chat box";
    public static final String[] MENU_LABELS = {COPY_TO_CLIPBOARD, COPY_TO_CHAT_BOX};

    private ChatInteractionModel() {
    }

    /** A render-time message row. token is owned by the caller (normally a component or a history message). */
    public static class Bounds {
        public int surface;
        public float x;
        public float y;
        public float w;
        public float h;
        public Object token;

        public Bounds set(int surface, float x, float y, float w, float h, Object token) {
            this.surface = clampSurface(surface);
            this.x = x;
            this.y = y;
            this.w = Math.max(0f, w);
            this.h = Math.max(0f, h);
            this.token = token;
            return this;
        }

        public boolean contains(float mouseX, float mouseY) {
            return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        }
    }

    /** Reusable result holder. No per-frame hit-test allocation is needed. */
    public static final class Hit {
        public int surface = -1;
        public Object token;
        public float x;
        public float y;
        public float w;
        public float h;

        public boolean found() {
            return surface >= 0 && token != null;
        }

        public void clear() {
            surface = -1;
            token = null;
            x = y = w = h = 0f;
        }

        public void copy(Bounds source) {
            if (source == null) {
                clear();
                return;
            }
            surface = source.surface;
            token = source.token;
            x = source.x;
            y = source.y;
            w = source.w;
            h = source.h;
        }
    }

    /**
     * Finds the topmost matching row on one requested surface. The caller controls draw ordering by the order of the
     * supplied fixed array; this method walks backwards so overlapping wrapped rows behave like the renderer.
     */
    public static void hitTest(Bounds[] rows, int count, int surface, float mouseX, float mouseY, Hit out) {
        if (out == null) return;
        out.clear();
        if (rows == null) return;
        int safe = Math.min(Math.max(0, count), rows.length);
        for (int i = safe - 1; i >= 0; i--) {
            Bounds row = rows[i];
            if (row != null && row.surface == surface && row.token != null && row.contains(mouseX, mouseY)) {
                out.copy(row);
                return;
            }
        }
    }

    /**
     * Snappy 100 ms ease-out hover for each chat surface. The token is a persistent message-group token rather than
     * a wrapped row, so moving across a long message cannot flash the whole-message highlight.
     */
    public static final class Hover {
        private static final float DURATION_SECONDS = .10f;
        private final Object[] token = new Object[SURFACE_COUNT];
        private final float[] alpha = new float[SURFACE_COUNT];
        private final float[] start = new float[SURFACE_COUNT];
        private final float[] target = new float[SURFACE_COUNT];
        private final float[] elapsed = new float[SURFACE_COUNT];

        public void update(int surface, Object next, float seconds) {
            int slot = clampSurface(surface);
            float wanted = next == null ? 0f : 1f;
            if (next != token[slot] || wanted != target[slot]) {
                token[slot] = next;
                start[slot] = alpha[slot];
                target[slot] = wanted;
                elapsed[slot] = 0f;
            }
            elapsed[slot] = Math.min(DURATION_SECONDS, elapsed[slot] + clamp(seconds, 0f, 0.1f));
            float progress = elapsed[slot] / DURATION_SECONDS;
            alpha[slot] = start[slot] + (target[slot] - start[slot]) * easeOut(progress);
            if (target[slot] == 0f && progress >= 1f) token[slot] = null;
        }

        public float alpha(int surface) {
            return alpha[clampSurface(surface)];
        }

        public Object token(int surface) {
            return token[clampSurface(surface)];
        }

        public void clear() {
            for (int i = 0; i < SURFACE_COUNT; i++) {
                token[i] = null;
                alpha[i] = start[i] = target[i] = elapsed[i] = 0f;
            }
        }
    }

    /**
     * One normalized wheel event. Vanilla 1.8.9 limits an event to +/-1 then uses seven lines normally and one line
     * with Shift held; speed is deliberately applied exactly once by this function.
     */
    public static float wheelLines(int wheel, boolean shiftFine, float speed) {
        if (wheel == 0) return 0f;
        float direction = wheel > 0 ? 1f : -1f;
        float vanillaLines = shiftFine ? 1f : 7f;
        return direction * vanillaLines * clamp(speed, 0.25f, 4f);
    }

    /** Bounded scroll target; no caller should add its own wheel delta after this result. */
    public static float boundedScroll(float current, float delta, float maximum) {
        return clamp(current + delta, 0f, Math.max(0f, maximum));
    }

    /** Context menu position, 80–120 ms ease-out opening, and a matching snappy row-hover fade. */
    public static final class Menu {
        public float x;
        public float y;
        public float w = 142f;
        public float h = 42f;
        public boolean visible;
        private float open;
        private float openElapsed;
        private int animationMillis = 100;
        private final float[] rowHover = new float[MENU_LABELS.length];
        private final float[] rowHoverStart = new float[MENU_LABELS.length];
        private final float[] rowHoverTarget = new float[MENU_LABELS.length];
        private final float[] rowHoverElapsed = new float[MENU_LABELS.length];

        public void setAnimationMillis(int value) {
            animationMillis = value < 0 ? 0 : value > 240 ? 240 : value;
        }

        public int animationMillis() {
            return animationMillis;
        }

        public void show(float mouseX, float mouseY, float screenW, float screenH) {
            w = Math.max(112f, Math.min(176f, screenW - 4f));
            h = 42f;
            x = clamp(mouseX, 2f, Math.max(2f, screenW - w - 2f));
            y = clamp(mouseY, 2f, Math.max(2f, screenH - h - 2f));
            visible = true;
            openElapsed = 0f;
            open = animationMillis == 0 ? 1f : 0f;
            clearRowHover();
        }

        public void hide() {
            visible = false;
            open = 0f;
            openElapsed = 0f;
            clearRowHover();
        }

        public void update(float seconds) {
            if (!visible) return;
            float dt = clamp(seconds, 0f, 0.1f);
            if (animationMillis == 0) {
                open = 1f;
            } else {
                float duration = animationMillis / 1000f;
                openElapsed = Math.min(duration, openElapsed + dt);
                open = openElapsed >= duration ? 1f : easeOut(openElapsed / duration);
            }
        }

        /** Call once per frame with the current pointer so menu-row feedback completes a real 100 ms ease-out. */
        public void updateHover(float seconds, int hoveredRow) {
            float dt = clamp(seconds, 0f, .1f);
            for (int i = 0; i < rowHover.length; i++) {
                float target = i == hoveredRow ? 1f : 0f;
                if (target != rowHoverTarget[i]) {
                    rowHoverStart[i] = rowHover[i];
                    rowHoverTarget[i] = target;
                    rowHoverElapsed[i] = 0f;
                }
                rowHoverElapsed[i] = Math.min(.1f, rowHoverElapsed[i] + dt);
                float progress = rowHoverElapsed[i] / .1f;
                rowHover[i] = rowHoverElapsed[i] >= .1f ? rowHoverTarget[i]
                        : rowHoverStart[i] + (rowHoverTarget[i] - rowHoverStart[i]) * easeOut(progress);
            }
        }

        private void clearRowHover() {
            for (int i = 0; i < rowHover.length; i++) {
                rowHover[i] = rowHoverStart[i] = rowHoverTarget[i] = rowHoverElapsed[i] = 0f;
            }
        }

        public float hoverAmount(int row) {
            return row < 0 || row >= rowHover.length ? 0f : rowHover[row];
        }

        public float openAmount() {
            return open;
        }

        public int rowAt(float mouseX, float mouseY) {
            if (!visible || mouseX < x || mouseX >= x + w || mouseY < y || mouseY >= y + h) return -1;
            int row = (int) ((mouseY - y) / 20f);
            return row >= 0 && row < MENU_LABELS.length ? row : -1;
        }
    }

    /** Maps an on-screen native row to local font coordinates after a caller's text scale has been applied. */
    public static float localTextX(float mouseX, float rowX, float textScale) {
        return (mouseX - rowX) / Math.max(0.01f, textScale);
    }

    /** Quadratic ease-out reaches a settled value at the requested duration without an abrupt first frame. */
    private static float easeOut(float value) {
        float t = clamp(value, 0f, 1f);
        return 1f - (1f - t) * (1f - t);
    }

    private static int clampSurface(int surface) {
        return surface < DEFAULT_CHAT ? DEFAULT_CHAT : (surface > CHAT_2 ? CHAT_2 : surface);
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : (value > max ? max : value);
    }
}
