package com.arena.pekaclient.render;

/**
 * Layout helper facade for the named tokens in {@link Theme}.
 *
 * <p>Existing screens use this class for local radius calculations; concrete spacing and radius values intentionally
 * live only in Theme so the whole GUI shares one visual system.</p>
 */
public final class GuiStyle {
    public static final float SPACE_4 = Theme.SPACE_4;
    public static final float SPACE_8 = Theme.SPACE_8;
    public static final float SPACE_12 = Theme.SPACE_12;
    public static final float SPACE_16 = Theme.SPACE_16;

    public static final float RADIUS_WINDOW = Theme.RADIUS_WINDOW;
    public static final float RADIUS_CARD = Theme.RADIUS_CARD;
    public static final float RADIUS_BUTTON = Theme.RADIUS_BUTTON;
    public static final float RADIUS_FIELD = Theme.RADIUS_FIELD;
    public static final float RADIUS_POPUP = Theme.RADIUS_POPUP;
    public static final float RADIUS_SMALL = Theme.RADIUS_SMALL;
    public static final float RADIUS_PILL = Theme.RADIUS_PILL;
    public static final float RADIUS_THEME_BASE = Theme.RADIUS_THEME_BASE;
    public static final float OUTLINE = Theme.OUTLINE;
    public static final float AA_FRINGE = Theme.AA_FRINGE;

    private GuiStyle() {
    }

    /** Applies the persisted Corner radius preference while preserving each role's relative hierarchy. */
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
