package com.arena.pekaclient.feature;

import com.arena.pekaclient.config.AppleSettings;

/**
 * Checks the live Item Animation settings bridge used by the first-person hook.
 * Pure logic only: Minecraft's bytecode hook itself is verified in the packaged release jar.
 */
public final class ItemAnimationTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        ItemAnimation.resetToDefaults();

        check("size defaults to 100%", AppleSettings.itemAnimationSize == 100);
        check("animation speed defaults to 100%", AppleSettings.itemAnimationSpeed == 100);
        check("size switch defaults on", AppleSettings.itemAnimationSizeOn);
        check("speed switch defaults on", AppleSettings.itemAnimationSpeedOn);

        check("default size multiplier is exactly vanilla", ItemAnimation.getSize() == 1.0f);
        check("default animation multiplier is exactly vanilla", ItemAnimation.getSpeed() == 1.0f);
        check("effective default size matches the constant",
                AppleSettings.effectiveItemAnimationSize() == AppleSettings.DEFAULT_ITEM_ANIMATION_SIZE);
        check("effective default speed matches the constant",
                AppleSettings.effectiveItemAnimationSpeed() == AppleSettings.DEFAULT_ITEM_ANIMATION_SPEED);

        AppleSettings.itemAnimationSize = 150;
        check("size 150% becomes a 1.5x multiplier", ItemAnimation.getSize() == 1.5f);
        AppleSettings.itemAnimationSize = 50;
        check("size 50% becomes a 0.5x multiplier", ItemAnimation.getSize() == 0.5f);
        AppleSettings.itemAnimationSize = 200;
        check("size 200% becomes a 2.0x multiplier", ItemAnimation.getSize() == 2.0f);

        AppleSettings.itemAnimationSize = 130;
        AppleSettings.itemAnimationSizeOn = false;
        check("switch off forces vanilla size despite the slider", ItemAnimation.getSize() == 1.0f);
        check("switch off reports the default effective value", AppleSettings.effectiveItemAnimationSize() == 100);
        AppleSettings.itemAnimationSizeOn = true;
        check("switch back on restores the slider value", ItemAnimation.getSize() == 1.3f);

        AppleSettings.itemAnimationSpeed = 170;
        AppleSettings.itemAnimationSpeedOn = false;
        check("switch off forces vanilla animation speed despite the slider", ItemAnimation.getSpeed() == 1.0f);
        AppleSettings.itemAnimationSpeedOn = true;
        check("switch back on restores the animation slider value", ItemAnimation.getSpeed() == 1.7f);

        check("size clamp leaves an in-range value alone", ItemAnimation.clampSize(120) == 120);
        check("size clamp raises a too-small value to the minimum",
                ItemAnimation.clampSize(0) == ItemAnimation.MIN_SIZE_PERCENT);
        check("size clamp lowers a too-large value to the maximum",
                ItemAnimation.clampSize(999) == ItemAnimation.MAX_SIZE_PERCENT);
        check("size clamp handles negative input", ItemAnimation.clampSize(-500) == ItemAnimation.MIN_SIZE_PERCENT);
        check("size clamp bounds match the GUI slider bounds",
                ItemAnimation.MIN_SIZE_PERCENT == 30 && ItemAnimation.MAX_SIZE_PERCENT == 200);
        check("size clamp bounds mirror the range the loader clamps to",
                ItemAnimation.MIN_SIZE_PERCENT == AppleSettings.ITEM_ANIMATION_SIZE_MIN_PERCENT
                        && ItemAnimation.MAX_SIZE_PERCENT == AppleSettings.ITEM_ANIMATION_SIZE_MAX_PERCENT);
        check("defaults sit inside the clamp bounds",
                ItemAnimation.clampSize(ItemAnimation.DEFAULT_SIZE_PERCENT)
                        == ItemAnimation.DEFAULT_SIZE_PERCENT
                        && ItemAnimation.clampSpeed(ItemAnimation.DEFAULT_SPEED_PERCENT)
                        == ItemAnimation.DEFAULT_SPEED_PERCENT);

        AppleSettings.itemAnimationSize = 200;
        AppleSettings.itemAnimationSpeed = 50;
        AppleSettings.itemAnimationSizeOn = false;
        AppleSettings.itemAnimationSpeedOn = false;
        ItemAnimation.resetToDefaults();
        check("reset restores the size slider", AppleSettings.itemAnimationSize == 100);
        check("reset restores the animation slider", AppleSettings.itemAnimationSpeed == 100);
        check("reset turns the size switch back on", AppleSettings.itemAnimationSizeOn);
        check("reset turns the animation switch back on", AppleSettings.itemAnimationSpeedOn);
        check("reset leaves the renderer at vanilla multipliers",
                ItemAnimation.getSize() == 1.0f && ItemAnimation.getSpeed() == 1.0f);

        System.out.println();
        System.out.println("item animation: passed " + passed + ", failed " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL " + name);
        }
    }
}
