package com.arena.pekaclient.feature;

import com.arena.pekaclient.config.AppleSettings;

/**
 * Live settings bridge for Item Animation.
 *
 * <p>The editor stores whole values for predictable sliders and exact-value entry; the render hook reads the
 * effective float values from this class on every hand render and every equip update. This class deliberately
 * has no world, player, packet, or combat dependency: Item Animation is first-person visual state only.
 */
public final class ItemAnimation {

    /** Size and equip-speed slider lower bound, in percent. */
    public static final int MIN_PERCENT = AppleSettings.ITEM_ANIMATION_MIN_PERCENT;
    /** Size and equip-speed slider upper bound, in percent. */
    public static final int MAX_PERCENT = AppleSettings.ITEM_ANIMATION_MAX_PERCENT;
    /** X/Y/Z renderer-position lower bound, in hundredths of a render unit. */
    public static final int MIN_OFFSET = AppleSettings.ITEM_ANIMATION_OFFSET_MIN;
    /** X/Y/Z renderer-position upper bound, in hundredths of a render unit. */
    public static final int MAX_OFFSET = AppleSettings.ITEM_ANIMATION_OFFSET_MAX;

    public static final int DEFAULT_SIZE_PERCENT = AppleSettings.DEFAULT_ITEM_ANIMATION_SIZE;
    public static final int DEFAULT_SPEED_PERCENT = AppleSettings.DEFAULT_ITEM_ANIMATION_SPEED;

    private ItemAnimation() {
    }

    /** Clamps a size or speed percent to the safe renderer range. */
    public static int clampPercent(int value) {
        return Math.max(MIN_PERCENT, Math.min(MAX_PERCENT, value));
    }

    /** Clamps a renderer offset stored in hundredths of a first-person render unit. */
    public static int clampOffset(int value) {
        return Math.max(MIN_OFFSET, Math.min(MAX_OFFSET, value));
    }

    /** Live X transform. Positive moves the hand/item right; zero is vanilla. */
    public static float getX() {
        return clampOffset(AppleSettings.itemAnimationX) / 100.0F;
    }

    /** Live Y transform. Positive moves the hand/item up; zero is vanilla. */
    public static float getY() {
        return clampOffset(AppleSettings.itemAnimationY) / 100.0F;
    }

    /** Live Z transform. Positive moves the hand/item outward; zero is vanilla. */
    public static float getZ() {
        return clampOffset(AppleSettings.itemAnimationZ) / 100.0F;
    }

    /** Live first-person size multiplier. A disabled switch always yields exact vanilla 1.0. */
    public static float getSize() {
        return AppleSettings.effectiveItemAnimationSize() / 100.0F;
    }

    /** Live first-person equip-transition multiplier. A disabled switch always yields exact vanilla 1.0. */
    public static float getSpeed() {
        return AppleSettings.effectiveItemAnimationSpeed() / 100.0F;
    }

    /** True only when the hand render needs a matrix transform. */
    public static boolean hasVisualTransform() {
        return getX() != 0.0F || getY() != 0.0F || getZ() != 0.0F || getSize() != 1.0F;
    }

    /** Restores every Item Animation setting to the exact vanilla visual state. */
    public static void resetToDefaults() {
        AppleSettings.itemAnimationSizeOn = true;
        AppleSettings.itemAnimationSpeedOn = true;
        AppleSettings.itemAnimationSize = DEFAULT_SIZE_PERCENT;
        AppleSettings.itemAnimationSpeed = DEFAULT_SPEED_PERCENT;
        AppleSettings.itemAnimationX = AppleSettings.DEFAULT_ITEM_ANIMATION_X;
        AppleSettings.itemAnimationY = AppleSettings.DEFAULT_ITEM_ANIMATION_Y;
        AppleSettings.itemAnimationZ = AppleSettings.DEFAULT_ITEM_ANIMATION_Z;
    }
}
