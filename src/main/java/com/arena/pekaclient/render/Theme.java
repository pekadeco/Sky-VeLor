package com.arena.pekaclient.render;

import com.arena.pekaclient.config.AppleSettings;

/**
 * Colour palette and motion settings. Static fields are rebuilt by {@link #refresh()}
 * whenever a setting changes, so drawing code never allocates or recomputes palettes.
 */
public final class Theme {

    public static int accent = 0xFF0A84FF;
    public static int textPrimary = 0xFFF5F5F7;
    public static int textSecondary = 0xFFA1A1A8;
    public static int textTertiary = 0xFF707078;
    public static int windowTop = 0xF01E1E21;
    public static int windowBottom = 0xF0101012;
    public static int card = 0x14FFFFFF;
    public static int cardHover = 0x22FFFFFF;
    public static int buttonFill = 0x26FFFFFF;
    public static int buttonHover = 0x3DFFFFFF;
    public static int border = 0x1FFFFFFF;
    public static int divider = 0x18FFFFFF;
    public static int track = 0xFF3A3A3D;
    public static int segSelected = 0xFF5A5A5E;
    public static int popup = 0xF22A2A2E;
    public static int veil = 0x3A000000;

    public static int radius = 12;
    public static boolean animations = true;
    public static float speed = 1f;

    private Theme() {
    }

    public static void refresh() {
        int glass = Math.round(AppleSettings.effectiveGlassOpacity() * 2.55f);
        radius = AppleSettings.effectiveCornerRadius();
        accent = AppleSettings.accentColor();
        animations = AppleSettings.animations;
        speed = AppleSettings.effectiveAnimSpeed() / 100f;

        if (AppleSettings.darkMode) {
            windowTop = (glass << 24) | 0x1E1E21;
            windowBottom = (glass << 24) | 0x101012;
            card = 0x14FFFFFF;
            cardHover = 0x22FFFFFF;
            buttonFill = 0x26FFFFFF;
            buttonHover = 0x3DFFFFFF;
            border = 0x1FFFFFFF;
            divider = 0x18FFFFFF;
            track = 0xFF3A3A3D;
            segSelected = 0xFF5A5A5E;
            popup = 0xF22A2A2E;
            veil = 0x3A000000;
            textPrimary = 0xFFF5F5F7;
            textSecondary = 0xFFA1A1A8;
            textTertiary = 0xFF707078;
        } else {
            // Light: milky, translucent glass. The world shows through each layer:
            //   veil (faint milky wash over the game), window panel (translucent milky white, about 0.5 to 0.8 alpha
            //   from the glass setting), cards (translucent white), dropdowns (nearly opaque for legibility).
            // Buttons keep their own opaque fills, and text, icons and borders are never made transparent.
            int milky = Math.round(AppleSettings.effectiveGlassOpacity() * 2.04f) << 24;
            windowTop = milky | 0xFCFCFE;
            windowBottom = milky | 0xF4F4F8;
            card = 0x8CFFFFFF;
            cardHover = 0xB8FFFFFF;
            buttonFill = 0xFFE4E4EA;
            buttonHover = 0xFFD6D6DE;
            border = 0x38000000;
            divider = 0x26000000;
            track = 0xFFD2D2D8;
            segSelected = 0xFFFFFFFF;
            popup = 0xF5FFFFFF;
            veil = 0x33F4F4F8;
            textPrimary = 0xFF1C1C1E;
            textSecondary = 0xFF4A4A52;
            textTertiary = 0xFF6E6E76;
        }
    }
}
