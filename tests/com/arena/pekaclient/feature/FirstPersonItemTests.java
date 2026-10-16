package com.arena.pekaclient.feature;

import com.arena.pekaclient.config.AppleSettings;

/**
 * Checks the settings bridge that feeds the first-person item renderer.
 *
 * <p>These are the values that actually reach {@code CustomItemRenderer} every frame, so the
 * assertions concentrate on the two properties the feature depends on: the default must resolve to an
 * exact {@code 1.0f} multiplier (the renderer only skips its matrix work on an exact match), and a
 * switch turned off must force the vanilla value no matter where the slider sits.
 *
 * <p>Pure logic only. Nothing here touches the file system or Minecraft, so it runs headless.
 */
public final class FirstPersonItemTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        FirstPersonItem.resetToDefaults();

        check("scale defaults to 100%", AppleSettings.firstPersonItemScale == 100);
        check("animation speed defaults to 100%", AppleSettings.firstPersonAnimSpeed == 100);
        check("scale switch defaults on", AppleSettings.firstPersonItemScaleOn);
        check("animation switch defaults on", AppleSettings.firstPersonAnimSpeedOn);

        check("default scale multiplier is exactly vanilla", FirstPersonItem.getItemScale() == 1.0f);
        check("default animation multiplier is exactly vanilla",
                FirstPersonItem.getAnimationSpeed() == 1.0f);
        check("effective default scale matches the constant",
                AppleSettings.effectiveFirstPersonItemScale()
                        == AppleSettings.DEFAULT_FIRST_PERSON_ITEM_SCALE);
        check("effective default speed matches the constant",
                AppleSettings.effectiveFirstPersonAnimSpeed()
                        == AppleSettings.DEFAULT_FIRST_PERSON_ANIM_SPEED);

        AppleSettings.firstPersonItemScale = 150;
        check("scale 150% becomes a 1.5x multiplier", FirstPersonItem.getItemScale() == 1.5f);
        AppleSettings.firstPersonItemScale = 50;
        check("scale 50% becomes a 0.5x multiplier", FirstPersonItem.getItemScale() == 0.5f);
        AppleSettings.firstPersonItemScale = 200;
        check("scale 200% becomes a 2.0x multiplier", FirstPersonItem.getItemScale() == 2.0f);

        AppleSettings.firstPersonItemScale = 130;
        AppleSettings.firstPersonItemScaleOn = false;
        check("switch off forces vanilla scale despite the slider",
                FirstPersonItem.getItemScale() == 1.0f);
        check("switch off reports the default effective value",
                AppleSettings.effectiveFirstPersonItemScale() == 100);
        AppleSettings.firstPersonItemScaleOn = true;
        check("switch back on restores the slider value", FirstPersonItem.getItemScale() == 1.3f);

        AppleSettings.firstPersonAnimSpeed = 170;
        AppleSettings.firstPersonAnimSpeedOn = false;
        check("switch off forces vanilla animation speed despite the slider",
                FirstPersonItem.getAnimationSpeed() == 1.0f);
        AppleSettings.firstPersonAnimSpeedOn = true;
        check("switch back on restores the animation slider value",
                FirstPersonItem.getAnimationSpeed() == 1.7f);

        check("clamp leaves an in-range value alone", FirstPersonItem.clamp(120) == 120);
        check("clamp raises a too-small value to the minimum",
                FirstPersonItem.clamp(0) == FirstPersonItem.MIN_PERCENT);
        check("clamp lowers a too-large value to the maximum",
                FirstPersonItem.clamp(999) == FirstPersonItem.MAX_PERCENT);
        check("clamp handles negative input", FirstPersonItem.clamp(-500) == FirstPersonItem.MIN_PERCENT);
        check("clamp bounds match the GUI slider bounds",
                FirstPersonItem.MIN_PERCENT == 50 && FirstPersonItem.MAX_PERCENT == 200);
        check("clamp bounds mirror the range the loader clamps to",
                FirstPersonItem.MIN_PERCENT == AppleSettings.FIRST_PERSON_MIN
                        && FirstPersonItem.MAX_PERCENT == AppleSettings.FIRST_PERSON_MAX);
        check("defaults sit inside the clamp bounds",
                FirstPersonItem.clamp(FirstPersonItem.DEFAULT_SCALE_PERCENT)
                        == FirstPersonItem.DEFAULT_SCALE_PERCENT
                        && FirstPersonItem.clamp(FirstPersonItem.DEFAULT_ANIM_SPEED_PERCENT)
                        == FirstPersonItem.DEFAULT_ANIM_SPEED_PERCENT);

        AppleSettings.firstPersonItemScale = 200;
        AppleSettings.firstPersonAnimSpeed = 50;
        AppleSettings.firstPersonItemScaleOn = false;
        AppleSettings.firstPersonAnimSpeedOn = false;
        FirstPersonItem.resetToDefaults();
        check("reset restores the scale slider", AppleSettings.firstPersonItemScale == 100);
        check("reset restores the animation slider", AppleSettings.firstPersonAnimSpeed == 100);
        check("reset turns the scale switch back on", AppleSettings.firstPersonItemScaleOn);
        check("reset turns the animation switch back on", AppleSettings.firstPersonAnimSpeedOn);
        check("reset leaves the renderers at vanilla multipliers",
                FirstPersonItem.getItemScale() == 1.0f && FirstPersonItem.getAnimationSpeed() == 1.0f);

        System.out.println();
        System.out.println("first person item: passed " + passed + ", failed " + failed);
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
