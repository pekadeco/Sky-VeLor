package com.arena.pekaclient.gui;

/**
 * Scroll position for a settings list: a current value that eases towards a target, plus an optional
 * fling velocity.
 *
 * Every change is clamped to 0..max, so the list never over-scrolls. Scrolling is measured in pixels,
 * not rows, so no entry is skipped when the list moves.
 */
final class ScrollState {

    /** Pixels moved per mouse-wheel notch at 100 % scroll speed. Larger than the key step, so the wheel is quicker. */
    static final float WHEEL_STEP = 96f;
    /** Pixels moved per arrow-key press at 100 % scroll speed. */
    static final float KEY_STEP = 40f;

    private float pos;
    private float target;
    private float velocity;
    private float max;

    float value() {
        return pos;
    }

    float max() {
        return max;
    }

    /** Sets the largest allowed scroll value and clamps the current position into range. */
    void setMax(float m) {
        max = Math.max(0f, m);
        target = clamp(target);
        pos = clamp(pos);
    }

    /** Mouse wheel. Positive notches scroll up. Speed is the user's scroll-speed setting (1.0 = 100 %). */
    void wheel(float notches, float speed) {
        target = clamp(target - notches * WHEEL_STEP * speed);
        velocity = 0f;
    }

    /** Keyboard scroll by a fixed step (positive is down). */
    void key(float px) {
        target = clamp(target + px);
        velocity = 0f;
    }

    /** Sets the position directly, for touch drags that must follow the finger exactly. */
    void setImmediate(float v) {
        pos = clamp(v);
        target = pos;
        velocity = 0f;
    }

    /** Starts a fling. Positive pixels per second scrolls down. */
    void fling(float pxPerSecond) {
        velocity = pxPerSecond;
    }

    /** True while the list is still moving (a fling or an unfinished ease). */
    boolean moving() {
        return velocity != 0f || Math.abs(target - pos) > 0.5f;
    }

    void reset() {
        pos = 0f;
        target = 0f;
        velocity = 0f;
    }

    /**
     * Advances one frame. While a drag is in progress the position is left to the drag. Otherwise the
     * fling decays, the target is clamped, and the position eases towards it (or jumps when animations
     * are off).
     */
    void update(float dt, boolean dragging, boolean animate, float speedFactor) {
        if (dragging) {
            return;
        }
        if (velocity != 0f) {
            target += velocity * dt;
            velocity *= (float) Math.exp(-dt * 5f);
            if (Math.abs(velocity) < 4f) {
                velocity = 0f;
            }
        }
        target = clamp(target);
        if (target <= 0f || target >= max) {
            velocity = 0f;
        }
        if (animate) {
            pos += (target - pos) * (1f - (float) Math.exp(-dt * 16f * speedFactor));
        } else {
            pos = target;
        }
        if (Math.abs(target - pos) < 0.05f) {
            pos = target;
        }
        pos = clamp(pos);
    }

    private float clamp(float v) {
        if (v < 0f) {
            return 0f;
        }
        return v > max ? max : v;
    }
}
