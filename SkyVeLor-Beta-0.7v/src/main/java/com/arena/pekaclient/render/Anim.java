package com.arena.pekaclient.render;

/**
 * Frame-rate independent value animation using exponential smoothing.
 * {@code update(dt, rate)} moves the value towards the target; with animations
 * disabled (or on low-end adaptive mode) it snaps immediately.
 */
public final class Anim {

    private float value;
    private float target;

    public Anim(float initial) {
        this.value = initial;
        this.target = initial;
    }

    public void snap(float v) {
        value = v;
        target = v;
    }

    public void to(float t) {
        target = t;
    }

    public float get() {
        return value;
    }

    public void update(float dt, float rate) {
        if (value == target) {
            return;
        }
        if (!Theme.animations) {
            value = target;
            return;
        }
        float k = 1f - (float) Math.exp(-dt * rate * Theme.speed);
        value += (target - value) * k;
        if (Math.abs(target - value) < 0.002f) {
            value = target;
        }
    }
}
