package com.arena.pekaclient.render;

/**
 * Pure pixel-grid policy for Minecraft's bitmap {@code FontRenderer}.
 *
 * <p>Font glyphs become soft when an arbitrary logical scale or origin lands between physical GUI pixels. The
 * renderer resolves the active {@code ScaledResolution} once per frame and routes every text draw through these
 * methods: glyph scale occupies a whole number of physical pixels, while its origin is snapped to the same grid.
 * This preserves the compact type hierarchy without adding a custom font subsystem.</p>
 */
public final class GuiPixelGrid {

    private GuiPixelGrid() {
    }

    /** Returns a positive integral GUI scale factor even if a caller is initializing before Minecraft is ready. */
    public static int factor(int requested) {
        return Math.max(1, requested);
    }

    /**
     * Maps a desired logical bitmap-font scale to its nearest whole physical-pixel scale. Ties round upward so small
     * headings remain legible; values below one physical pixel never disappear.
     */
    public static float textScale(float requestedScale, int guiScaleFactor) {
        int factor = factor(guiScaleFactor);
        float desired = Math.max(0f, requestedScale) * factor;
        int physicalPixels = Math.max(1, Math.round(desired));
        return physicalPixels / (float) factor;
    }

    /** Snaps a logical GUI coordinate to the active physical pixel grid. */
    public static float coordinate(float logicalCoordinate, int guiScaleFactor) {
        int factor = factor(guiScaleFactor);
        return Math.round(logicalCoordinate * factor) / (float) factor;
    }
}
