package com.arena.pekaclient.render;

import com.arena.pekaclient.config.AppleSettings;

/** Checks the hub palettes retain readable controls while making panels translucent and light surfaces softer. */
public final class ThemeTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        AppleSettings.glassOpacityOn = true;
        AppleSettings.glassOpacity = AppleSettings.DEFAULT_GLASS_OPACITY;
        AppleSettings.darkMode = true;
        Theme.refresh();
        check("dark panels use the new transparent default",
                alpha(Theme.windowTop) == Math.round(AppleSettings.DEFAULT_GLASS_OPACITY * 2.55f));
        check("dark cards remain translucent", alpha(Theme.card) < 255 && alpha(Theme.cardHover) < 255);
        check("dark controls retain visible fills", alpha(Theme.buttonFill) > 0 && alpha(Theme.buttonHover) > 0);

        AppleSettings.darkMode = false;
        Theme.refresh();
        check("light panel top is softer than white", rgb(Theme.windowTop) == 0xF1F1F5);
        check("light panel bottom is soft gray", rgb(Theme.windowBottom) == 0xE8E8EF);
        check("light panels reveal the world", alpha(Theme.windowTop) < 255);
        check("light cards are translucent off-white", rgb(Theme.card) == 0xE8E8EE && alpha(Theme.card) < 255);
        check("light hover remains modestly brighter", rgb(Theme.cardHover) == 0xF2F2F6
                && alpha(Theme.cardHover) > alpha(Theme.card));
        check("light buttons stay opaque and readable", alpha(Theme.buttonFill) == 255
                && rgb(Theme.buttonFill) == 0xE5E5EA);
        check("light popup avoids pure white", rgb(Theme.popup) == 0xF1F1F5);
        check("light foreground retains dark contrast", Theme.textPrimary == 0xFF1C1C1E);

        System.out.println();
        System.out.println("theme: passed " + passed + ", failed " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static int alpha(int color) {
        return color >>> 24;
    }

    private static int rgb(int color) {
        return color & 0xFFFFFF;
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
