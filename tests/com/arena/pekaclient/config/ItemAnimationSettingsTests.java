package com.arena.pekaclient.config;

import java.util.Properties;

/** Checks modern Item Animation persistence and compatibility with earlier first-person setting keys. */
public final class ItemAnimationSettingsTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        AppleSettings.loadItemAnimationSettings(new Properties());
        check("default master toggle is on", AppleSettings.itemAnimationEnabled);
        check("default size and speed are vanilla", AppleSettings.itemAnimationSize == 100
                && AppleSettings.itemAnimationSpeed == 100);
        check("default swing curve is smooth", AppleSettings.itemAnimationSmoothSwing
                && AppleSettings.itemAnimationSmoothness == 65);
        Properties legacyRepeatKeys = new Properties();
        legacyRepeatKeys.setProperty("combat.holdAttackRepeat", "true");
        legacyRepeatKeys.setProperty("itemAnimation.visualRepeatWhileHeld", "true");
        AppleSettings.loadItemAnimationSettings(legacyRepeatKeys);
        check("retired held-repeat keys cannot alter the standard item animation settings",
                AppleSettings.itemAnimationEnabled && AppleSettings.itemAnimationSize == 100
                        && AppleSettings.itemAnimationSpeed == 100);

        Properties oldBase = new Properties();
        oldBase.setProperty("firstPerson.itemScaleOn", "false");
        oldBase.setProperty("firstPerson.itemScale", "175");
        oldBase.setProperty("firstPerson.animationSpeedOn", "true");
        oldBase.setProperty("firstPerson.animationSpeed", "135");
        AppleSettings.firstPersonItemScaleOn = false;
        AppleSettings.firstPersonItemScale = 175;
        AppleSettings.firstPersonAnimSpeedOn = true;
        AppleSettings.firstPersonAnimSpeed = 135;
        AppleSettings.loadItemAnimationSettings(oldBase);
        check("legacy dotted scale settings migrate", !AppleSettings.itemAnimationSizeOn
                && AppleSettings.itemAnimationSize == 175);
        check("legacy dotted speed settings migrate", AppleSettings.itemAnimationSpeedOn
                && AppleSettings.itemAnimationSpeed == 135);

        Properties legacyR5 = new Properties();
        legacyR5.setProperty("firstPersonItemAnimation", "false");
        legacyR5.setProperty("firstPersonItemScaleOn", "true");
        legacyR5.setProperty("firstPersonItemScale", "190");
        legacyR5.setProperty("firstPersonAnimSpeed", "380");
        AppleSettings.loadItemAnimationSettings(legacyR5);
        check("r5 legacy master and scale keys migrate", !AppleSettings.itemAnimationEnabled
                && AppleSettings.itemAnimationSize == 190);
        check("r5 speed aliases support values through 400%", AppleSettings.itemAnimationSpeed == 380);

        Properties modernWins = new Properties();
        modernWins.setProperty("itemAnimation.enabled", "true");
        modernWins.setProperty("itemAnimation.size", "80");
        modernWins.setProperty("itemAnimation.speed", "250");
        modernWins.setProperty("firstPerson.itemScale", "190");
        AppleSettings.loadItemAnimationSettings(modernWins);
        check("modern keys override legacy keys", AppleSettings.itemAnimationEnabled
                && AppleSettings.itemAnimationSize == 80 && AppleSettings.itemAnimationSpeed == 250);

        Properties clamped = new Properties();
        clamped.setProperty("itemAnimation.size", "-100");
        clamped.setProperty("itemAnimation.speed", "999");
        clamped.setProperty("itemAnimation.x", "999");
        AppleSettings.loadItemAnimationSettings(clamped);
        check("malformed settings are clamped to safe ranges", AppleSettings.itemAnimationSize == 30
                && AppleSettings.itemAnimationSpeed == 400 && AppleSettings.itemAnimationX == 150);

        System.out.println("item animation settings: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++;
        else { failed++; System.out.println("FAIL " + name); }
    }
}
