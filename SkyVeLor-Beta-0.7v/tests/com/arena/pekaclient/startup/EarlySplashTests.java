package com.arena.pekaclient.startup;

import java.util.Properties;

/**
 * Checks the rule that decides what the loading screen looks like: Sky VeLor brands a fresh install, and never
 * overwrites a value the player changed. Also checks that {@code enabled} is left to Forge, which is what keeps
 * macOS and OptiFine behaviour untouched.
 */
public final class EarlySplashTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        brandsAFreshConfig();
        keepsPlayerChoices();
        replacesForgeDefaults();
        neverTouchesEnabled();
        keepsOtherKeys();
        ignoresMissingValues();

        System.out.println();
        System.out.println("early splash: passed " + passed + ", failed " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void check(String name, boolean ok, String detail) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL " + name + " " + detail);
        }
    }

    private static void brandsAFreshConfig() {
        Properties out = EarlySplash.merge(new Properties());
        check("background", "0x0B2350".equals(out.getProperty("background")), out.getProperty("background"));
        check("bar", "0x0A84FF".equals(out.getProperty("bar")), out.getProperty("bar"));
        check("font", "0xFFFFFF".equals(out.getProperty("font")), out.getProperty("font"));
        check("logo", EarlySplash.LOGO_LOCATION.equals(out.getProperty("logoTexture")),
                out.getProperty("logoTexture"));
        check("rotate off", "false".equals(out.getProperty("rotate")), out.getProperty("rotate"));
    }

    private static void keepsPlayerChoices() {
        Properties in = new Properties();
        in.setProperty("background", "0x123456");
        in.setProperty("bar", "0xFF00FF");
        Properties out = EarlySplash.merge(in);
        check("player background kept", "0x123456".equals(out.getProperty("background")), out.getProperty("background"));
        check("player bar kept", "0xFF00FF".equals(out.getProperty("bar")), out.getProperty("bar"));
        // The keys the player did not touch are still branded.
        check("untouched key branded", EarlySplash.LOGO_LOCATION.equals(out.getProperty("logoTexture")),
                out.getProperty("logoTexture"));
    }

    private static void replacesForgeDefaults() {
        Properties in = new Properties();
        for (String key : new String[]{"background", "font", "bar", "barBorder", "barBackground", "logoTexture", "rotate"}) {
            in.setProperty(key, EarlySplash.STOCK.get(key));
        }
        Properties out = EarlySplash.merge(in);
        int branded = 0;
        for (String[] pair : EarlySplash.BRANDING) {
            if (pair[1].equals(out.getProperty(pair[0]))) {
                branded++;
            }
        }
        check("stock values replaced", branded == EarlySplash.BRANDING.length, "branded " + branded);
    }

    private static void neverTouchesEnabled() {
        Properties in = new Properties();
        in.setProperty("enabled", "false");
        Properties out = EarlySplash.merge(in);
        check("disabled stays disabled", "false".equals(out.getProperty("enabled")), out.getProperty("enabled"));

        Properties empty = EarlySplash.merge(new Properties());
        check("enabled not added", empty.getProperty("enabled") == null, empty.getProperty("enabled"));
    }

    private static void keepsOtherKeys() {
        Properties in = new Properties();
        in.setProperty("resourcePackPath", "resources");
        in.setProperty("someOtherKey", "keep me");
        Properties out = EarlySplash.merge(in);
        check("resourcePackPath kept", "resources".equals(out.getProperty("resourcePackPath")), "");
        check("unknown key kept", "keep me".equals(out.getProperty("someOtherKey")), "");
    }

    private static void ignoresMissingValues() {
        Properties in = new Properties();
        in.setProperty("background", "  ");
        Properties out = EarlySplash.merge(in);
        check("blank value branded", "0x0B2350".equals(out.getProperty("background")), out.getProperty("background"));
        check("null input", EarlySplash.merge(null).getProperty("background") != null, "");
        check("install with no dir", !EarlySplash.install(null), "");
    }
}
