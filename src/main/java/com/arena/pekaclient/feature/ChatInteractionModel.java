package com.arena.pekaclient.feature;

/**
 * Minecraft-free interaction contract for the one Default Chat replacement. It owns deterministic grouped hit bounds,
 * eased hover feedback, context-menu geometry, and one application of a mouse-wheel delta.
 */
public final class ChatInteractionModel {

    /** Copy labels are exact and stable; the third row is conditionally shown from the captured player name. */
    public static final String COPY_TO_CLIPBOARD = "Copy to clipboard";
    public static final String COPY_TO_CHAT_BOX = "Copy to chat box";
    public static final String MESSAGE_PLAYER = "Message player";
    public static final String[] MENU_LABELS = {COPY_TO_CLIPBOARD, COPY_TO_CHAT_BOX, MESSAGE_PLAYER};

    private ChatInteractionModel() {
    }

    /** A render-time full-width wrapped-message row. token is its persistent message-group owner. */
    public static class Bounds {
        public float x;
        public float y;
        public float w;
        public float h;
        public Object token;

        public Bounds set(float x, float y, float w, float h, Object token) {
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
        public Object token;
        public float x;
        public float y;
        public float w;
        public float h;

        public boolean found() { return token != null; }
        public void clear() { token = null; x = y = w = h = 0f; }
        public void copy(Bounds source) {
            if (source == null) { clear(); return; }
            token = source.token;
            x = source.x;
            y = source.y;
            w = source.w;
            h = source.h;
        }
    }

    /** Finds the topmost matching full-width row. The caller controls draw ordering with the supplied array order. */
    public static void hitTest(Bounds[] rows, int count, float mouseX, float mouseY, Hit out) {
        if (out == null) return;
        out.clear();
        if (rows == null) return;
        int safe = Math.min(Math.max(0, count), rows.length);
        for (int i = safe - 1; i >= 0; i--) {
            Bounds row = rows[i];
            if (row != null && row.token != null && row.contains(mouseX, mouseY)) {
                out.copy(row);
                return;
            }
        }
    }

    /** Snappy 100 ms ease-out hover keyed to a message group rather than a wrapped row. */
    public static final class Hover {
        private static final float DURATION_SECONDS = .10f;
        private Object token;
        private float alpha;
        private float start;
        private float target;
        private float elapsed;

        public void update(Object next, float seconds) {
            float wanted = next == null ? 0f : 1f;
            if (next != token || wanted != target) {
                token = next;
                start = alpha;
                target = wanted;
                elapsed = 0f;
            }
            elapsed = Math.min(DURATION_SECONDS, elapsed + clamp(seconds, 0f, 0.1f));
            float progress = elapsed / DURATION_SECONDS;
            alpha = start + (target - start) * easeOut(progress);
            if (target == 0f && progress >= 1f) token = null;
        }

        public float alpha() { return alpha; }
        public Object token() { return token; }
        public void clear() { token = null; alpha = start = target = elapsed = 0f; }
    }

    /** One normalized wheel event. Vanilla 1.8.9 uses seven lines normally and one line with Shift held. */
    public static float wheelLines(int wheel, boolean shiftFine, float speed) {
        if (wheel == 0) return 0f;
        float direction = wheel > 0 ? 1f : -1f;
        return direction * (shiftFine ? 1f : 7f) * clamp(speed, 0.25f, 4f);
    }

    /** Bounded scroll target; no caller should add its own wheel delta after this result. */
    public static float boundedScroll(float current, float delta, float maximum) {
        return clamp(current + delta, 0f, Math.max(0f, maximum));
    }

    /**
     * Context-menu geometry. Open/close is deliberately immediate; only the individual row hover remains eased.
     * The caller supplies whether the stored message exposed the third, recipient-specific action.
     */
    public static final class Menu {
        public static final float ROW_HEIGHT = 20f;
        public float x;
        public float y;
        public float w = 142f;
        public float h = ROW_HEIGHT * 2f + 2f;
        public boolean visible;
        private int rows = 2;
        private final float[] rowHover = new float[MENU_LABELS.length];
        private final float[] rowHoverStart = new float[MENU_LABELS.length];
        private final float[] rowHoverTarget = new float[MENU_LABELS.length];
        private final float[] rowHoverElapsed = new float[MENU_LABELS.length];

        public void show(float mouseX, float mouseY, float screenW, float screenH, int requestedRows) {
            rows = Math.max(2, Math.min(MENU_LABELS.length, requestedRows));
            w = Math.max(112f, Math.min(176f, screenW - 4f));
            h = rows * ROW_HEIGHT + 2f;
            x = clamp(mouseX, 2f, Math.max(2f, screenW - w - 2f));
            y = clamp(mouseY, 2f, Math.max(2f, screenH - h - 2f));
            visible = true;
            clearRowHover();
        }

        public void hide() { visible = false; clearRowHover(); }
        public int optionCount() { return rows; }

        public void updateHover(float seconds, int hoveredRow) {
            float dt = clamp(seconds, 0f, .1f);
            for (int i = 0; i < rowHover.length; i++) {
                float next = i < rows && i == hoveredRow ? 1f : 0f;
                if (next != rowHoverTarget[i]) {
                    rowHoverStart[i] = rowHover[i];
                    rowHoverTarget[i] = next;
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

        public float hoverAmount(int row) { return row < 0 || row >= rowHover.length ? 0f : rowHover[row]; }
        public int rowAt(float mouseX, float mouseY) {
            if (!visible || mouseX < x || mouseX >= x + w || mouseY < y || mouseY >= y + h) return -1;
            int row = (int) ((mouseY - y) / ROW_HEIGHT);
            return row >= 0 && row < rows ? row : -1;
        }
    }

    /** Maps an on-screen native row to local font coordinates after text scale. */
    public static float localTextX(float mouseX, float rowX, float textScale) {
        return (mouseX - rowX) / Math.max(0.01f, textScale);
    }

    private static float easeOut(float value) {
        float t = clamp(value, 0f, 1f);
        return 1f - (1f - t) * (1f - t);
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : (value > max ? max : value);
    }
}
