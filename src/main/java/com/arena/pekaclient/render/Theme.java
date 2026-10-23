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

    /**
     * Applies the reference-shell surface palette. It reuses the persisted Sky VeLor appearance controls, including
     * the additive Settings Theme selection, rather than introducing a separate configuration file. Call
     * {@link #refresh()} after leaving that shell so focused legacy editors retain their own palette.
     */
    public static void applyReferenceSettingsPalette() {
        int glass = Math.round(AppleSettings.effectiveGlassOpacity() * 2.55f);
        radius = AppleSettings.effectiveCornerRadius();
        accent = AppleSettings.accentColor();
        animations = AppleSettings.animations;
        speed = AppleSettings.effectiveAnimSpeed() / 100f;

        if (AppleSettings.isBlackBlueTheme()) {
            applyBlackBluePalette(glass);
            return;
        }

        if (AppleSettings.darkMode) {
            // DeepSeek reference Graphite: forest-black chrome, graphite cards and low-contrast green lines.
            windowTop = (glass << 24) | 0x0D1410;
            windowBottom = (glass << 24) | 0x0C120E;
            card = 0xFF131B16;
            cardHover = 0xFF1A2620;
            buttonFill = 0xFF1A2620;
            buttonHover = 0xFF233129;
            border = 0xFF233129;
            divider = 0xFF233129;
            track = 0xFF2C3A32;
            segSelected = 0xFF1A2620;
            popup = 0xFF131B16;
            veil = 0x50000000;
            textPrimary = 0xFFE9F2EC;
            textSecondary = 0xFF8BA095;
            textTertiary = 0xFF6F8979;
        } else {
            // DeepSeek reference Frost: quiet neutral surfaces rather than pure-white slabs.
            windowTop = (glass << 24) | 0xF6F7F8;
            windowBottom = (glass << 24) | 0xECEEF0;
            card = 0xFFFFFFFF;
            cardHover = 0xFFF2F4F5;
            buttonFill = 0xFFE9ECEE;
            buttonHover = 0xFFDDE2E4;
            border = 0xFFDDE3E6;
            divider = 0xFFDDE3E6;
            track = 0xFFD6DADC;
            segSelected = 0xFFF2F4F5;
            popup = 0xFFFFFFFF;
            veil = 0x26000000;
            textPrimary = 0xFF1B1F1D;
            textSecondary = 0xFF6B7370;
            textTertiary = 0xFF6B7370;
        }
    }

    /**
     * A deliberately fixed black/blue palette. It uses a cyan-blue accent rather than the optional per-accent
     * picker so the selected theme reads consistently across the shell, focused editors, dropdowns and popovers.
     */
    private static void applyBlackBluePalette(int glass) {
        accent = 0xFF28B9FF;
        windowTop = (glass << 24) | 0x090E16;
        windowBottom = (glass << 24) | 0x05080D;
        card = 0xFF0D1622;
        cardHover = 0xFF142238;
        buttonFill = 0xFF101C2B;
        buttonHover = 0xFF19304A;
        border = 0xFF234C70;
        divider = 0xFF193953;
        track = 0xFF29445E;
        segSelected = 0xFF17304A;
        popup = 0xFF0A121D;
        veil = 0x62000000;
        textPrimary = 0xFFF2F7FF;
        textSecondary = 0xFFB4C6D9;
        textTertiary = 0xFF7894B0;
    }

    public static void refresh() {
        int glass = Math.round(AppleSettings.effectiveGlassOpacity() * 2.55f);
        radius = AppleSettings.effectiveCornerRadius();
        accent = AppleSettings.accentColor();
        animations = AppleSettings.animations;
        speed = AppleSettings.effectiveAnimSpeed() / 100f;

        if (AppleSettings.isBlackBlueTheme()) {
            applyBlackBluePalette(glass);
            return;
        }

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
