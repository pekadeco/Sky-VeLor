package com.arena.pekaclient.dungeon;

import java.util.List;

/** Pure layout helpers that keep map labels inside the viewport and avoid overlapping higher-priority labels. */
final class DungeonMapLabelLayout {
    private static final float[][] PROJECTED_OFFSETS = {
            {0.0F, 0.0F}, {0.0F, -8.0F}, {0.0F, -13.0F}, {-8.0F, -8.0F}, {8.0F, -8.0F},
            {-12.0F, 0.0F}, {12.0F, 0.0F},
            {-8.0F, 8.0F}, {8.0F, 8.0F}, {0.0F, 13.0F},
            {-16.0F, -8.0F}, {16.0F, -8.0F}, {-16.0F, 8.0F}, {16.0F, 8.0F}
    };
    private static final float[][] SCREEN_OFFSETS = {
            // The player marker is roughly 8 map pixels tall. A 7-pixel upward offset keeps the
            // name close to its marker without painting the text across the marker itself.
            {0.0F, -7.0F}, {0.0F, -11.0F}, {-7.0F, -7.0F}, {7.0F, -7.0F},
            {-7.0F, 0.0F}, {7.0F, 0.0F}, {0.0F, 0.0F},
            {-7.0F, 7.0F}, {7.0F, 7.0F}, {0.0F, 11.0F},
            {-12.0F, -7.0F}, {12.0F, -7.0F}
    };
    private static final float EDGE_PADDING = 2.0F;
    private static final float OVERLAP_PADDING = 1.5F;

    private DungeonMapLabelLayout() { }

    /**
     * Places a puzzle label around its map-space anchor. Candidate offsets are in map coordinates,
     * so the chosen location rotates with the map instead of sliding in screen space.
     */
    static Placement findProjected(DungeonMapProjection projection, float anchorMapX, float anchorMapY,
                                   float textWidth, float textHeight, List<Rect> occupied) {
        return findProjected(projection, anchorMapX, anchorMapY, textWidth, textHeight, occupied, new float[2]);
    }

    static Placement findProjected(DungeonMapProjection projection, float anchorMapX, float anchorMapY,
                                   float textWidth, float textHeight, List<Rect> occupied, float[] point) {
        if (projection == null || !validSize(textWidth, textHeight) || point == null || point.length < 2) return null;
        double radians = Math.toRadians(projection.textRotation());
        float boundWidth = (float) (Math.abs(Math.cos(radians)) * textWidth
                + Math.abs(Math.sin(radians)) * textHeight);
        float boundHeight = (float) (Math.abs(Math.sin(radians)) * textWidth
                + Math.abs(Math.cos(radians)) * textHeight);
        for (float[] offset : PROJECTED_OFFSETS) {
            projection.project(anchorMapX + offset[0], anchorMapY + offset[1], point);
            Rect bounds = centered(point[0], point[1], boundWidth, boundHeight);
            if (inside(bounds) && clear(bounds, occupied)) {
                return new Placement(point[0], point[1], bounds);
            }
        }
        return null;
    }

    /**
     * Keeps a puzzle label exactly centered on its matched room tile. Puzzle labels are higher-priority
     * semantic markers than player names, so they must never be displaced by an occupied-label collision.
     * Glyphs remain horizontal; only the anchor follows map rotation.
     */
    static Placement findCenteredProjected(DungeonMapProjection projection, float anchorMapX, float anchorMapY,
                                           float textWidth, float textHeight, float[] point) {
        if (projection == null || !validSize(textWidth, textHeight) || point == null || point.length < 2) return null;
        projection.project(anchorMapX, anchorMapY, point);
        Rect bounds = centered(point[0], point[1], textWidth, textHeight);
        return inside(bounds) ? new Placement(point[0], point[1], bounds) : null;
    }

    /** Places a player name at one fixed screen-space offset so rotation cannot make it jump sides. */
    static Placement findStableScreen(float anchorX, float anchorY, float textWidth, float textHeight) {
        if (!validSize(textWidth, textHeight) || Float.isNaN(anchorX) || Float.isInfinite(anchorX)
                || Float.isNaN(anchorY) || Float.isInfinite(anchorY)) return null;
        float centerX = anchorX;
        float centerY = anchorY + SCREEN_OFFSETS[0][1];
        Rect bounds = centered(centerX, centerY, textWidth, textHeight);
        return inside(bounds) ? new Placement(centerX, centerY, bounds) : null;
    }

    /** Places a horizontal player-name label above/around a projected player marker. */
    static Placement findScreen(float anchorX, float anchorY, float textWidth, float textHeight,
                                List<Rect> occupied) {
        if (!validSize(textWidth, textHeight)) return null;
        for (float[] offset : SCREEN_OFFSETS) {
            float centerX = anchorX + offset[0];
            float centerY = anchorY + offset[1];
            Rect bounds = centered(centerX, centerY, textWidth, textHeight);
            if (inside(bounds) && clear(bounds, occupied)) {
                return new Placement(centerX, centerY, bounds);
            }
        }
        return null;
    }

    static boolean overlaps(Rect first, Rect second) {
        if (first == null || second == null) return false;
        return first.left < second.right + OVERLAP_PADDING
                && first.right + OVERLAP_PADDING > second.left
                && first.top < second.bottom + OVERLAP_PADDING
                && first.bottom + OVERLAP_PADDING > second.top;
    }

    private static boolean validSize(float width, float height) {
        return width > 0.0F && height > 0.0F && !Float.isNaN(width) && !Float.isNaN(height)
                && !Float.isInfinite(width) && !Float.isInfinite(height);
    }

    private static Rect centered(float x, float y, float width, float height) {
        return new Rect(x - width * 0.5F, y - height * 0.5F, x + width * 0.5F, y + height * 0.5F);
    }

    private static boolean inside(Rect bounds) {
        return bounds.left >= EDGE_PADDING && bounds.top >= EDGE_PADDING
                && bounds.right <= 128.0F - EDGE_PADDING && bounds.bottom <= 128.0F - EDGE_PADDING;
    }

    private static boolean clear(Rect candidate, List<Rect> occupied) {
        if (occupied == null || occupied.isEmpty()) return true;
        for (Rect current : occupied) if (overlaps(candidate, current)) return false;
        return true;
    }

    static final class Rect {
        final float left;
        final float top;
        final float right;
        final float bottom;

        Rect(float left, float top, float right, float bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }

    static final class Placement {
        final float centerX;
        final float centerY;
        final Rect bounds;

        Placement(float centerX, float centerY, Rect bounds) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.bounds = bounds;
        }
    }
}
