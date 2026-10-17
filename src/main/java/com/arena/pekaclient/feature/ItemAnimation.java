package com.arena.pekaclient.feature;

import com.arena.pekaclient.config.AppleSettings;

/**
 * Live, visual-only settings bridge for Item Animation.
 *
 * <p>The HUD editor stores integer hundredths/percentages for predictable sliders and exact typed entry. The
 * ItemRenderer callback reads primitive float values from this class every frame; this class has no world, packet,
 * attack, potion, or combat dependency.
 */
public final class ItemAnimation {

    /** Size slider bounds, in percent: 0.30x through 2.00x. */
    public static final int MIN_SIZE_PERCENT = AppleSettings.ITEM_ANIMATION_SIZE_MIN_PERCENT;
    public static final int MAX_SIZE_PERCENT = AppleSettings.ITEM_ANIMATION_SIZE_MAX_PERCENT;
    /** Animation-speed slider bounds, in percent: 0.25x through 4.00x. */
    public static final int MIN_SPEED_PERCENT = AppleSettings.ITEM_ANIMATION_SPEED_MIN_PERCENT;
    public static final int MAX_SPEED_PERCENT = AppleSettings.ITEM_ANIMATION_SPEED_MAX_PERCENT;
    /** X/Y/Z renderer-position lower bound, in hundredths of a render unit (-1.50). */
    public static final int MIN_OFFSET = AppleSettings.ITEM_ANIMATION_OFFSET_MIN;
    /** X/Y/Z renderer-position upper bound, in hundredths of a render unit (+1.50). */
    public static final int MAX_OFFSET = AppleSettings.ITEM_ANIMATION_OFFSET_MAX;

    public static final int DEFAULT_SIZE_PERCENT = AppleSettings.DEFAULT_ITEM_ANIMATION_SIZE;
    public static final int DEFAULT_SPEED_PERCENT = AppleSettings.DEFAULT_ITEM_ANIMATION_SPEED;

    private ItemAnimation() {
    }

    /** True when the default-on master toggle allows visual transforms. */
    public static boolean isEnabled() {
        return AppleSettings.itemAnimationEnabled;
    }

    /** Clamps a size percent to the safe 0.30x–2.00x renderer range. */
    public static int clampSize(int value) {
        return Math.max(MIN_SIZE_PERCENT, Math.min(MAX_SIZE_PERCENT, value));
    }

    /** Clamps an animation-speed percent to the visual-only 0.25x–4.00x range. */
    public static int clampSpeed(int value) {
        return Math.max(MIN_SPEED_PERCENT, Math.min(MAX_SPEED_PERCENT, value));
    }

    /** Clamps a renderer offset stored in hundredths of a first-person render unit. */
    public static int clampOffset(int value) {
        return Math.max(MIN_OFFSET, Math.min(MAX_OFFSET, value));
    }

    /** Stored X value, independent of the master toggle, for editor/debug readouts. */
    public static float getConfiguredX() {
        return clampOffset(AppleSettings.itemAnimationX) / 100.0F;
    }

    /** Stored Y value, independent of the master toggle, for editor/debug readouts. */
    public static float getConfiguredY() {
        return clampOffset(AppleSettings.itemAnimationY) / 100.0F;
    }

    /** Stored Z value, independent of the master toggle, for editor/debug readouts. */
    public static float getConfiguredZ() {
        return clampOffset(AppleSettings.itemAnimationZ) / 100.0F;
    }

    /** Stored size value, respecting the legacy per-value switch but not the master toggle. */
    public static float getConfiguredSize() {
        return AppleSettings.effectiveItemAnimationSize() / 100.0F;
    }

    /** Stored visual speed value, respecting the legacy per-value switch but not the master toggle. */
    public static float getConfiguredSpeed() {
        return AppleSettings.effectiveItemAnimationSpeed() / 100.0F;
    }

    /** Live X transform. Positive moves the held item right; master-off is exact vanilla zero. */
    public static float getX() {
        return isEnabled() ? getConfiguredX() : 0.0F;
    }

    /** Live Y transform. Positive moves the held item up; master-off is exact vanilla zero. */
    public static float getY() {
        return isEnabled() ? getConfiguredY() : 0.0F;
    }

    /** Live Z transform. Positive moves the held item outward; master-off is exact vanilla zero. */
    public static float getZ() {
        return isEnabled() ? getConfiguredZ() : 0.0F;
    }

    /** Live first-person size multiplier. Master-off is exact vanilla 1.0. */
    public static float getSize() {
        return isEnabled() ? getConfiguredSize() : 1.0F;
    }

    /** Live visual swing/equip multiplier. Master-off is exact vanilla 1.0. */
    public static float getSpeed() {
        return isEnabled() ? getConfiguredSpeed() : 1.0F;
    }

    /** True only when the enabled held-item renderer needs a non-vanilla matrix transform. */
    public static boolean hasVisualTransform() {
        return isEnabled() && (getConfiguredX() != 0.0F || getConfiguredY() != 0.0F
                || getConfiguredZ() != 0.0F || getConfiguredSize() != 1.0F);
    }

    /** Restores the default-on master and every visual setting to the exact vanilla state. */
    public static void resetToDefaults() {
        AppleSettings.itemAnimationEnabled = true;
        AppleSettings.itemAnimationSizeOn = true;
        AppleSettings.itemAnimationSpeedOn = true;
        AppleSettings.itemAnimationSize = DEFAULT_SIZE_PERCENT;
        AppleSettings.itemAnimationSpeed = DEFAULT_SPEED_PERCENT;
        AppleSettings.itemAnimationX = AppleSettings.DEFAULT_ITEM_ANIMATION_X;
        AppleSettings.itemAnimationY = AppleSettings.DEFAULT_ITEM_ANIMATION_Y;
        AppleSettings.itemAnimationZ = AppleSettings.DEFAULT_ITEM_ANIMATION_Z;
    }
}
