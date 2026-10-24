package com.arena.pekaclient.render;

/**
 * Shared logical GUI measurements.
 *
 * <p>Values are in Minecraft GUI pixels, rather than framebuffer pixels. Minecraft scales that coordinate system
 * for GUI scales 1 through 4, so one {@link #AA_FRINGE} unit remains one logical GUI pixel at every scale. Keep
 * spacing and corner roles here instead of creating almost-identical magic numbers in every editor.</p>
 */
public final class GuiStyle {
    /** The compact spacing scale used by settings pages, editors, popups and the navigation rail. */
    public static final float SPACE_4 = 4f;
    public static final float SPACE_8 = 8f;
    public static final float SPACE_12 = 12f;
    public static final float SPACE_16 = 16f;

    /** Baseline role radii at the default configured 12 px theme radius. */
    public static final float RADIUS_WINDOW = 12f;
    public static final float RADIUS_CARD = 7f;
    public static final float RADIUS_BUTTON = 8f;
    public static final float RADIUS_FIELD = 7f;
    public static final float RADIUS_POPUP = 7f;
    public static final float RADIUS_SMALL = 4f;
    public static final float RADIUS_PILL = 10f;
    public static final float RADIUS_THEME_BASE = 12f;

    /** One logical GUI pixel: the default outline and anti-alias fringe thickness. */
    public static final float OUTLINE = 1f;
    public static final float AA_FRINGE = 1f;

    private GuiStyle() {
    }

    /**
     * Applies the persisted Corner radius preference to a named role while preserving its relative hierarchy.
     * The final clamp is deliberately local to the actual box, so tiny controls cannot self-intersect.
     */
    public static float radius(float roleRadius, float configuredRadius, float width, float height) {
        float scale = Math.max(0f, configuredRadius) / RADIUS_THEME_BASE;
        float desired = Math.max(0f, roleRadius) * scale;
        return Math.min(Math.max(0f, Math.min(width, height)) * .5f, desired);
    }

    /** A pill always rounds to the available half-height, while respecting the configured corner preference. */
    public static float pillRadius(float configuredRadius, float width, float height) {
        return radius(RADIUS_PILL, configuredRadius, width, height);
    }
}
