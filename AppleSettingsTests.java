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
