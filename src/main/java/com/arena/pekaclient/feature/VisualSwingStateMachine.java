package com.arena.pekaclient.feature;

/**
 * Allocation-free visual swing timeline used only by the first-person ItemRenderer hook. It reads vanilla/input
 * state supplied by the hook but never mutates player swing fields, attack timing, packets, or hit registration.
 */
public final class VisualSwingStateMachine {

    public static final float SWING_TICKS = 6.0F;
    /** Returned when the hook should keep vanilla's local swing value for this frame. */
    public static final float USE_VANILLA = -1.0F;

    private boolean active;
    private boolean sawVanillaSwing;
    private boolean sawAttackHeld;
    private float startedAt;
    private float lastClock;
    private float lastRaw;
    private long starts;

    /**
     * Advances the renderer-only timeline. A held attack/mine key starts another visual cycle only after the prior
     * cycle completes; rising vanilla swings still work for single clicks. Extra clicks while active are ignored,
     * so spam clicking cannot restart an animation midway.
     */
    public float advance(float vanillaProgress, float clockTicks, boolean vanillaSwinging, boolean attackHeld,
                         float speed, boolean autoFix) {
        if (!(speed > 0.0F) || Float.isNaN(speed) || Float.isInfinite(speed)) {
            reset();
            return USE_VANILLA;
        }
        if (clockTicks < lastClock) {
            resetTimeline();
        }
        lastClock = clockTicks;

        boolean vanillaStarted = vanillaSwinging && !sawVanillaSwing;
        boolean attackStarted = attackHeld && !sawAttackHeld;
        sawVanillaSwing = vanillaSwinging;
        sawAttackHeld = attackHeld;

        if (!active) {
            if (!vanillaStarted && !attackStarted) {
                return USE_VANILLA;
            }
            begin(clockTicks);
        }

        // The persisted UI range is 25..400%; retaining that clamp here makes malformed external config harmless.
        float visualSpeed = Math.max(0.25F, Math.min(4.0F, speed));
        float duration = SWING_TICKS / visualSpeed;
        float raw = (clockTicks - startedAt) / duration;
        if (raw >= 1.0F) {
            if (!attackHeld) {
                active = false;
                lastRaw = 1.0F;
                return 1.0F;
            }
            // Preserve any partial-tick overshoot so repeat cycles remain smooth even on a slow frame.
            do {
                startedAt += duration;
                starts++;
                lastRaw = 0.0F;
                raw = (clockTicks - startedAt) / duration;
            } while (raw >= 1.0F);
        }
        if (raw <= 0.0F) {
            lastRaw = 0.0F;
            return 0.0F;
        }
        if (autoFix && raw < lastRaw) {
            raw = lastRaw;
        }
        lastRaw = raw;
        return raw;
    }

    public void reset() {
        resetTimeline();
        sawVanillaSwing = false;
        sawAttackHeld = false;
        lastClock = 0.0F;
    }

    public boolean isActive() {
        return active;
    }

    /** Number of timeline starts, exposed only for headless state-machine verification. */
    public long getStarts() {
        return starts;
    }

    private void begin(float clockTicks) {
        active = true;
        startedAt = clockTicks;
        lastRaw = 0.0F;
        starts++;
    }

    private void resetTimeline() {
        active = false;
        startedAt = 0.0F;
        lastRaw = 0.0F;
    }
}
