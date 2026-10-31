package com.arena.pekaclient.feature;

import com.arena.pekaclient.config.AppleSettings;

/** Pure settings checks for the first-person item rendering options merged from the source branch. */
public final class FirstPersonItemTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("vanilla size at default", Math.abs(FirstPersonItem.getItemScale() - 1.0F) < 0.001F);
        check("vanilla animation at default", Math.abs(FirstPersonItem.getAnimationSpeed() - 1.0F) < 0.001F);
        check("clamps minimum", FirstPersonItem.clamp(-10) == AppleSettings.FIRST_PERSON_MIN);
        check("clamps maximum", FirstPersonItem.clamp(999) == AppleSettings.FIRST_PERSON_MAX);
        AppleSettings.firstPersonItemScale = 150;
        check("scale converts percent to multiplier", Math.abs(FirstPersonItem.getItemScale() - 1.5F) < 0.001F);
        AppleSettings.firstPersonItemScaleOn = false;
        check("disabled scale stays vanilla", Math.abs(FirstPersonItem.getItemScale() - 1.0F) < 0.001F);
        AppleSettings.firstPersonItemScaleOn = true;
        AppleSettings.firstPersonAnimSpeed = 70;
        check("animation percent converts to multiplier", Math.abs(FirstPersonItem.getAnimationSpeed() - 0.7F) < 0.001F);
        AppleSettings.firstPersonAnimSpeedOn = false;
        check("disabled animation stays vanilla", Math.abs(FirstPersonItem.getAnimationSpeed() - 1.0F) < 0.001F);
        FirstPersonItem.resetToDefaults();
        check("reset restores defaults and enables both options", AppleSettings.firstPersonItemScale == 100
                && AppleSettings.firstPersonAnimSpeed == 100 && AppleSettings.firstPersonItemScaleOn
                && AppleSettings.firstPersonAnimSpeedOn);
        System.out.println("first-person item: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++;
        else {
            failed++;
            System.out.println("FAIL " + name);
        }
    }
}
