package com.arena.pekaclient.render;

/**
 * Frame-rate-independent GUI-only easing shared by controls and settings navigation.
 *
 * <p>Each duration means "within five percent of the target". The durations intentionally stay between
 * 120 and 180 milliseconds so hover, toggle and tab feedback feels responsive without continuing to animate after
 * a screen is closed. Callers pass whether their GUI is currently open; a closed GUI snaps to its resting value.</p>
 */
public final class GuiMotion {
    public static final int HOVER_SETTLE_MS = 140;
    public static final int PRESS_SETTLE_MS = 120;
    public static final int TOGGLE_SETTLE_MS = 150;
    public static final int TAB_SETTLE_MS = 160;
    public static final int SLIDER_SETTLE_MS = 150;
    public static final int PULSE_SETTLE_MS = 150;

    private static final float LOG_TWENTY = 2.9957323f;

    private GuiMotion() {
    }

    /** Moves {@code value} toward {@code target}; no timer, collection or per-frame allocation is needed. */
    public static float ease(float value, float target, float seconds, boolean guiOpen, int settleMillis,
                             float speedMultiplier) {
        if (!guiOpen || seconds <= 0f) {
            return target;
        }
        float duration = Math.max(.001f, settleMillis / 1000f);
        float speed = Math.max(.1f, speedMultiplier);
        float clampedSeconds = Math.max(0f, Math.min(.1f, seconds));
        float fraction = 1f - (float) Math.exp(-clampedSeconds * LOG_TWENTY * speed / duration);
        float result = value + (target - value) * fraction;
        return Math.abs(target - result) < .0015f ? target : result;
    }

    /** Converts an established exponential rate to the same allocation-free GUI-open policy. */
    public static float easeRate(float value, float target, float seconds, boolean guiOpen, float rate) {
        if (!guiOpen || seconds <= 0f) {
            return target;
        }
        float clampedSeconds = Math.max(0f, Math.min(.1f, seconds));
        float fraction = 1f - (float) Math.exp(-clampedSeconds * Math.max(.1f, rate));
        float result = value + (target - value) * fraction;
        return Math.abs(target - result) < .0015f ? target : result;
    }
}
