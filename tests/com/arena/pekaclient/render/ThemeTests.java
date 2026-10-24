package com.arena.pekaclient.render;

import com.arena.pekaclient.config.AppleSettings;

/** Headless contract for the one fixed neutral-dark/blue GUI theme and its compatibility boundary. */
public final class ThemeTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        fixedThemeIgnoresLegacyPaletteChoices();
        geometryAndMotionAreCentralTokens();
        semanticStatusColorsRemainPurposeBound();
        System.out.println();
        System.out.println("theme: passed " + passed + ", failed " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void fixedThemeIgnoresLegacyPaletteChoices() {
        AppleSettings.glassOpacityOn = true;
        AppleSettings.glassOpacity = 82;
        AppleSettings.settingsTheme = AppleSettings.SETTINGS_THEME_FOREST_CYAN;
        AppleSettings.darkMode = false;
        AppleSettings.accent = 4;
        Theme.refresh();
        int top = Theme.windowTop;
        int bottom = Theme.windowBottom;
        int card = Theme.card;
        int control = Theme.buttonFill;

        AppleSettings.settingsTheme = AppleSettings.SETTINGS_THEME_BLACK_BLUE;
        AppleSettings.darkMode = true;
        AppleSettings.accent = 2;
        Theme.refresh();

        check("legacy palette key does not fork the active window surface", Theme.windowTop == top
                && Theme.windowBottom == bottom);
        check("legacy palette key does not fork cards or controls", Theme.card == card && Theme.buttonFill == control);
        check("one fixed positive accent is blue", Theme.accent == Theme.ACCENT_BLUE
                && Theme.ACCENT_BLUE == 0xFF2F7DFF);
        check("neutral dark token values drive resolved surfaces", rgb(Theme.windowTop) == Theme.SURFACE_TOP_RGB
                && rgb(Theme.windowBottom) == Theme.SURFACE_BOTTOM_RGB && Theme.card == Theme.SURFACE_CARD
                && Theme.buttonHover == Theme.SURFACE_CONTROL_HOVER);
        check("allowed opacity preference still changes only window alpha", alpha(Theme.windowTop) == Math.round(82f * 2.55f));

        // Leave static compatibility state at its normal defaults for independently-run suites.
        AppleSettings.settingsTheme = AppleSettings.DEFAULT_SETTINGS_THEME;
        AppleSettings.darkMode = true;
        AppleSettings.accent = 0;
        AppleSettings.glassOpacity = AppleSettings.DEFAULT_GLASS_OPACITY;
        AppleSettings.glassOpacityOn = false;
        Theme.refresh();
    }

    private static void geometryAndMotionAreCentralTokens() {
        check("GuiStyle spacing is a Theme facade", GuiStyle.SPACE_4 == Theme.SPACE_4
                && GuiStyle.SPACE_8 == Theme.SPACE_8 && GuiStyle.SPACE_12 == Theme.SPACE_12
                && GuiStyle.SPACE_16 == Theme.SPACE_16);
        check("GuiStyle radii are a Theme facade", GuiStyle.RADIUS_WINDOW == Theme.RADIUS_WINDOW
                && GuiStyle.RADIUS_CARD == Theme.RADIUS_CARD && GuiStyle.RADIUS_BUTTON == Theme.RADIUS_BUTTON);
        check("all visible motion durations stay in the subtle 120 to 180 ms range",
                inRange(Theme.MOTION_HOVER_MS) && inRange(Theme.MOTION_PRESS_MS) && inRange(Theme.MOTION_TOGGLE_MS)
                        && inRange(Theme.MOTION_TAB_MS) && inRange(Theme.MOTION_SLIDER_MS)
                        && inRange(Theme.MOTION_PULSE_MS));
        check("GuiMotion is a Theme timing facade", GuiMotion.HOVER_SETTLE_MS == Theme.MOTION_HOVER_MS
                && GuiMotion.PRESS_SETTLE_MS == Theme.MOTION_PRESS_MS
                && GuiMotion.TOGGLE_SETTLE_MS == Theme.MOTION_TOGGLE_MS
                && GuiMotion.TAB_SETTLE_MS == Theme.MOTION_TAB_MS
                && GuiMotion.SLIDER_SETTLE_MS == Theme.MOTION_SLIDER_MS
                && GuiMotion.PULSE_SETTLE_MS == Theme.MOTION_PULSE_MS);
    }

    private static void semanticStatusColorsRemainPurposeBound() {
        check("warnings and critical errors retain dedicated semantic tokens",
                Theme.STATUS_WARNING != Theme.ACCENT_BLUE && Theme.STATUS_CRITICAL != Theme.ACCENT_BLUE
                        && Theme.STATUS_UNAVAILABLE != Theme.ACCENT_BLUE);
        check("shared utility surfaces are tokenized", Theme.NOTE_PANEL != 0 && Theme.LEAP_CARD != 0
                && Theme.CHAT_PREVIEW_SURFACE != 0 && Theme.LOADING_BLUE != 0 && Theme.DEBUG_HOT != 0);
    }

    private static boolean inRange(int millis) {
        return millis >= 120 && millis <= 180;
    }

    private static int alpha(int color) {
        return color >>> 24;
    }

    private static int rgb(int color) {
        return color & Theme.RGB_MASK;
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
