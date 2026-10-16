package com.arena.pekaclient.feature;

import com.arena.pekaclient.config.AppleSettings;

/**
 * Utility class for first-person held item customization.
 * Provides helper methods for accessing and manipulating first-person item settings.
 */
public class FirstPersonItem {

    /** Default scale factor (1.0 = normal size) */
    public static final float DEFAULT_SCALE = 1.0f;
    
    /** Default animation speed (1.0 = normal speed) */
    public static final float DEFAULT_ANIM_SPEED = 1.0f;

    /**
     * Gets the effective item scale from settings.
     * Converts from percentage (stored in settings) to float multiplier.
     */
    public static float getItemScale() {
        return AppleSettings.effectiveFirstPersonItemScale() / 100.0f;
    }

    /**
     * Gets the effective animation speed from settings.
     * Converts from percentage (stored in settings) to float multiplier.
     */
    public static float getAnimationSpeed() {
        return AppleSettings.effectiveFirstPersonAnimSpeed() / 100.0f;
    }

    /**
     * Clamps a scale value to valid range and converts to percentage.
     */
    public static int clampScale(int value) {
        return Math.max(50, Math.min(200, value));
    }

    /**
     * Clamps an animation speed value to valid range and converts to percentage.
     */
    public static int clampAnimSpeed(int value) {
        return Math.max(50, Math.min(200, value));
    }
}
