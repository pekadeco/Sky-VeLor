package com.arena.pekaclient.feature;

/**
 * Stateless, allocation-free first-person swing easing. It works from the current partial-tick progress rather than
 * storing delayed samples, so increasing Smoothness changes only the visual curve and cannot add input lag.
 */
public final class SwingEasing {

    private SwingEasing() {
    }

    /** Blends linear progress toward a continuous smoothstep curve by the requested 0..100 percent. */
    public static float ease(float progress, int smoothnessPercent) {
        if (Float.isNaN(progress) || Float.isInfinite(progress)) {
            return 0.0F;
        }
        float clamped = clamp01(progress);
        int amount = smoothnessPercent < 0 ? 0 : smoothnessPercent > 100 ? 100 : smoothnessPercent;
        if (amount == 0) {
            return clamped;
        }
        float smoothstep = clamped * clamped * (3.0F - 2.0F * clamped);
        return clamped + (smoothstep - clamped) * (amount / 100.0F);
    }

    private static float clamp01(float value) {
        return value < 0.0F ? 0.0F : value > 1.0F ? 1.0F : value;
    }
}
