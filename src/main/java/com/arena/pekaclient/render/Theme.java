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
    public static int windowTop = 0xE01E1E21;
    public static int windowBottom = 0xE0101012;
    public static int card = 0x12FFFFFF;
    public static int cardHover = 0x20FFFFFF;
    public static int buttonFill = 0x24FFFFFF;
    public static int buttonHover = 0x38FFFFFF;
    public static int border = 0x1FFFFFFF;
    public static int divider = 0x18FFFFFF;
    public static int track = 0xFF3A3A3D;
    public static int segSelected = 0xFF5A5A5E;
    public static int popup = 0xF22A2A2E;
    public static int veil = 0x28000000;

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
            card = 0x12FFFFFF;
            cardHover = 0x20FFFFFF;
            buttonFill = 0x24FFFFFF;
            buttonHover = 0x38FFFFFF;
            border = 0x1FFFFFFF;
            divider = 0x18FFFFFF;
            track = 0xFF3A3A3D;
            segSelected = 0xFF5A5A5E;
            popup = 0xF22A2A2E;
            veil = 0x28000000;
            textPrimary = 0xFFF5F5F7;
            textSecondary = 0xFFA1A1A8;
            textTertiary = 0xFF707078;
        } else {
            // Softer translucent off-white panels keep the light theme readable without pure-white slabs.
            // Buttons and text stay opaque; only the panel, cards and backdrop reveal the world underneath.
            int milky = Math.round(AppleSettings.effectiveGlassOpacity() * 2.04f) << 24;
            windowTop = milky | 0xF1F1F5;
            windowBottom = milky | 0xE8E8EF;
            card = 0x78E8E8EE;
            cardHover = 0xA8F2F2F6;
            buttonFill = 0xFFE5E5EA;
            buttonHover = 0xFFD8D8E0;
            border = 0x38000000;
            divider = 0x26000000;
            track = 0xFFD1D1D8;
            segSelected = 0xFFF1F1F5;
            popup = 0xF2F1F1F5;
            veil = 0x26F0F0F4;
            textPrimary = 0xFF1C1C1E;
            textSecondary = 0xFF4A4A52;
            textTertiary = 0xFF6E6E76;
        }
    }
}
