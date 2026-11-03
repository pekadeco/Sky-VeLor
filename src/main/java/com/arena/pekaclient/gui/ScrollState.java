package com.arena.pekaclient.gui;

/**
 * Compatibility facade for the older hub/editor call sites. Its real position and wheel target are the shared
 * {@link GuiLayoutToolkit.Scroll}, so every Lunaria settings surface now uses the same clamp, Shift fine-scroll
 * and smoothing semantics. Fling remains here because touch dragging is specific to the two legacy screens.
 */
final class ScrollState {

    /** Pixels moved per mouse-wheel notch at 100 % scroll speed. */
    static final float WHEEL_STEP = 96f;
    /** Pixels moved per arrow-key press at 100 % scroll speed. */
    static final float KEY_STEP = 40f;

    private final GuiLayoutToolkit.Scroll shared = new GuiLayoutToolkit.Scroll();
    private float velocity;

    float value() {
        return shared.value();
    }

    float max() {
        return shared.maximum();
    }

    void setMax(float maximum) {
        shared.setMaximum(maximum);
    }

    /** Historical overload: callers that do not know about Shift retain normal wheel behaviour. */
    void wheel(float notches, float speed) {
        wheel(notches, false, speed);
    }

    /**
     * One physical mouse event normally carries +/-1 and changes the shared target once. The magnitude is retained
     * for touchpads/tests that coalesce several notches into one value, matching the legacy public contract without
     * adding a second independent scroll state.
     */
    void wheel(float notches, boolean fine, float speed) {
        if (notches == 0f) return;
        float step = WHEEL_STEP * (fine ? .25f : 1f)
                * GuiLayoutToolkit.clamp(speed, .25f, 6f) * Math.abs(notches);
        shared.nudge(-Math.signum(notches) * step);
        velocity = 0f;
    }

    void key(float pixels) {
        shared.nudge(pixels);
        velocity = 0f;
    }

    void setImmediate(float value) {
        shared.setImmediate(value);
        velocity = 0f;
    }

    void fling(float pixelsPerSecond) {
        velocity = pixelsPerSecond;
    }

    boolean moving() {
        return velocity != 0f || Math.abs(shared.target() - shared.value()) > 0.5f;
    }

    void reset() {
        shared.reset();
        velocity = 0f;
    }

    /** Advances the shared easing state, preserving the legacy touch fling affordance. */
    void update(float dt, boolean dragging, boolean smooth, float speedFactor) {
        if (dragging) {
            return;
        }
        if (velocity != 0f) {
            shared.nudge(velocity * Math.max(0f, dt));
            velocity *= (float) Math.exp(-Math.max(0f, dt) * 5f);
            if (Math.abs(velocity) < 4f) {
                velocity = 0f;
            }
        }
        // Theme.speed remains a visual modifier for the legacy surfaces. The canonical setting decides whether
        // easing is active; GuiLayoutToolkit owns the bounded target/value transition itself.
        float scaledDt = Math.max(0f, Math.min(.1f, dt * Math.max(.1f, speedFactor)));
        shared.update(scaledDt, smooth);
    }
}
