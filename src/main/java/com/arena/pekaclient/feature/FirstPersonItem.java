package com.arena.pekaclient.feature;

import com.arena.pekaclient.config.AppleSettings;

/**
 * Settings bridge for the first-person held-item customization.
 *
 * <p>The GUI and the properties file store whole percentages; the renderer needs float multipliers.
 * Keeping both conversions here means there is exactly one place that decides what 100% means, and the
 * default always resolves to a {@code 1.0f} multiplier, i.e. untouched vanilla rendering.
 *
 * <p>Everything in here is purely visual. No potion effect is applied and no gameplay timer, swing
 * progress or attack cooldown is touched, so servers and combat behaviour are unaffected.
 */
public final class FirstPersonItem {

    /** Lower bound of both sliders, in percent. Mirrors {@link AppleSettings#FIRST_PERSON_MIN}. */
    public static final int MIN_PERCENT = AppleSettings.FIRST_PERSON_MIN;
    /** Upper bound of both sliders, in percent. Mirrors {@link AppleSettings#FIRST_PERSON_MAX}. */
    public static final int MAX_PERCENT = AppleSettings.FIRST_PERSON_MAX;
    /** Lower bound for each renderer-position control, in hundredths of a render unit. */
    public static final int MIN_OFFSET = AppleSettings.FIRST_PERSON_OFFSET_MIN;
    /** Upper bound for each renderer-position control, in hundredths of a render unit. */
    public static final int MAX_OFFSET = AppleSettings.FIRST_PERSON_OFFSET_MAX;

    /** 100% keeps the held item at its vanilla size. */
    public static final int DEFAULT_SCALE_PERCENT = AppleSettings.DEFAULT_FIRST_PERSON_ITEM_SCALE;
    /** 100% keeps the equip animation at its vanilla tempo. */
    public static final int DEFAULT_ANIM_SPEED_PERCENT = AppleSettings.DEFAULT_FIRST_PERSON_ANIM_SPEED;

    private FirstPersonItem() {
    }

    /** Clamps a percent value into the supported slider range. */
    public static int clamp(int value) {
        return Math.max(MIN_PERCENT, Math.min(MAX_PERCENT, value));
    }

    /** Clamps a saved X/Y/Z renderer offset in hundredths of a render unit. */
    public static int clampOffset(int value) {
        return Math.max(MIN_OFFSET, Math.min(MAX_OFFSET, value));
    }

    /** X renderer translation. Zero is vanilla; positive values move the held model to the right. */
    public static float getItemX() {
        return clampOffset(AppleSettings.firstPersonItemX) / 100.0f;
    }

    /** Y renderer translation. Zero is vanilla; positive values move the held model up. */
    public static float getItemY() {
        return clampOffset(AppleSettings.firstPersonItemY) / 100.0f;
    }

    /** Z renderer translation. Zero is vanilla; positive values move the held model outward. */
    public static float getItemZ() {
        return clampOffset(AppleSettings.firstPersonItemZ) / 100.0f;
    }

    /** True when position or scale requires a non-vanilla render transform. */
    public static boolean hasVisualTransform() {
        return getItemScale() != 1.0f || getItemX() != 0.0f || getItemY() != 0.0f || getItemZ() != 0.0f;
    }

    /**
     * The held-item size multiplier in use, or {@code 1.0f} when the switch is off. A result of
     * {@code 1.0f} means the renderer must not add any transform at all.
     */
    public static float getItemScale() {
        return AppleSettings.effectiveFirstPersonItemScale() / 100.0f;
    }

    /**
     * The equip-animation tempo multiplier in use, or {@code 1.0f} when the switch is off. Values
     * above 1 finish the equip transition sooner; values below 1 stretch it out.
     */
    public static float getAnimationSpeed() {
        return AppleSettings.effectiveFirstPersonAnimSpeed() / 100.0f;
    }

    /**
     * Restores size, X/Y/Z renderer position, animation speed, and both switches to their defaults.
     *
     * <p>Deliberately does not write the properties file: the settings screens already call
     * {@code AppleSettings.save()} after a change, so saving here too
     * would be a second, redundant write. The renderer reads the effective values every frame, so the
     * reset is visible immediately without any persistence step.
     */
    public static void resetToDefaults() {
        AppleSettings.firstPersonItemScale = DEFAULT_SCALE_PERCENT;
        AppleSettings.firstPersonAnimSpeed = DEFAULT_ANIM_SPEED_PERCENT;
        AppleSettings.firstPersonItemX = AppleSettings.DEFAULT_FIRST_PERSON_ITEM_X;
        AppleSettings.firstPersonItemY = AppleSettings.DEFAULT_FIRST_PERSON_ITEM_Y;
        AppleSettings.firstPersonItemZ = AppleSettings.DEFAULT_FIRST_PERSON_ITEM_Z;
        AppleSettings.firstPersonItemScaleOn = true;
        AppleSettings.firstPersonAnimSpeedOn = true;
    }
}
