package com.arena.pekaclient.config;

import java.util.Properties;

/** Checks fresh-install performance defaults and explicit saved choices. */
public final class AppleSettingsTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("fresh adaptive quality defaults off", !AppleSettings.adaptive);
        check("fresh Performance Boost Beta defaults on", AppleSettings.experimentalPerformance);
        check("fresh panel opacity is slightly transparent", AppleSettings.DEFAULT_GLASS_OPACITY == 88);

        AppleSettings.adaptive = AppleSettings.DEFAULT_ADAPTIVE_QUALITY;
        AppleSettings.experimentalPerformance = AppleSettings.DEFAULT_PERFORMANCE_BOOST_BETA;
        AppleSettings.loadPerformanceSettings(new Properties());
        check("empty fresh config keeps adaptive disabled", !AppleSettings.adaptive);
        check("empty fresh config keeps Performance Boost Beta enabled",
                AppleSettings.experimentalPerformance);

        Properties saved = new Properties();
        saved.setProperty("adaptive", "true");
        saved.setProperty("experimentalPerformance", "false");
        AppleSettings.loadPerformanceSettings(saved);
        check("saved adaptive choice is preserved", AppleSettings.adaptive);
        check("saved Performance Boost Beta choice is preserved", !AppleSettings.experimentalPerformance);

        AppleSettings.loadPerformanceSettings(new Properties());
        check("missing keys do not overwrite effective adaptive choice", AppleSettings.adaptive);
        check("missing keys do not overwrite effective performance choice",
                !AppleSettings.experimentalPerformance);

        Properties beta0108 = new Properties();
        beta0108.setProperty("itemAnimation.enabled", "false");
        beta0108.setProperty("itemAnimation.size", "145");
        beta0108.setProperty("itemAnimation.speed", "175");
        beta0108.setProperty("itemAnimation.x", "-75");
        beta0108.setProperty("itemAnimation.y", "25");
        beta0108.setProperty("itemAnimation.z", "50");
        beta0108.setProperty("itemAnimation.autoFixHitSlash", "false");
        AppleSettings.loadItemAnimationSettings(beta0108);
        check("0.10.8v config with obsolete Auto Fix key loads cleanly",
                !AppleSettings.itemAnimationEnabled && AppleSettings.itemAnimationSize == 145
                        && AppleSettings.itemAnimationSpeed == 175 && AppleSettings.itemAnimationX == -75
                        && AppleSettings.itemAnimationY == 25 && AppleSettings.itemAnimationZ == 50);
        Properties migrated = new Properties();
        AppleSettings.saveItemAnimationSettings(migrated);
        check("obsolete Auto Fix key is not written back", !migrated.containsKey("itemAnimation.autoFixHitSlash"));

        System.out.println();
        System.out.println("apple settings: passed " + passed + ", failed " + failed);
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
