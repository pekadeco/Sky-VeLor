package com.arena.pekaclient.dungeonesp;

/**
 * Pure eligibility policy for the Dungeon Enderman/Fel player-head proxy.
 *
 * <p>A shared skull texture alone is not enough evidence that the marker belongs to a starred target: regular
 * Endermen can use the same representation. The marker must therefore carry a star itself or sit beside a resolved
 * starred/special name tag. Markers at least two blocks above the local player are also rejected; those elevated
 * skulls are the false-positive pattern reported for unstarred Endermen.</p>
 */
public final class DungeonEspEndermanHeadPolicy {

    /** A marker exactly two blocks above the local player's origin is intentionally excluded. */
    public static final double MAX_MARKER_ABOVE_PLAYER = 2.0D;
    /** Keep the association narrower than unrelated nearby dungeon tags. */
    public static final double STAR_TAG_HORIZONTAL_SQ = 2.25D;
    /** Name and star stands can be separated vertically, including sleeping Fel representations. */
    public static final double STAR_TAG_VERTICAL = 3.5D;

    private DungeonEspEndermanHeadPolicy() {
    }

    /** True only while the marker remains below the inclusive false-positive ceiling. */
    public static boolean belowPlayerCeiling(double markerY, double playerY) {
        return finite(markerY) && finite(playerY) && markerY < playerY + MAX_MARKER_ABOVE_PLAYER;
    }

    /**
     * A direct marker can be trusted if its own display line carries a star, or when it has an adjacent resolved
     * star/special tag. Miniboss-only tags are deliberately not accepted: they do not identify an Enderman head.
     */
    public static boolean acceptsAssociatedCategory(DungeonEspCategory category) {
        return category == DungeonEspCategory.STARRED || category == DungeonEspCategory.SPECIAL
                || category == DungeonEspCategory.ENDERMAN;
    }

    /** Bounded geometric association for the independent armor-stand tag lines. */
    public static boolean isNearbyTag(double markerX, double markerY, double markerZ,
                                      double tagX, double tagY, double tagZ) {
        if (!finite(markerX) || !finite(markerY) || !finite(markerZ)
                || !finite(tagX) || !finite(tagY) || !finite(tagZ)) {
            return false;
        }
        double dx = markerX - tagX;
        double dz = markerZ - tagZ;
        return dx * dx + dz * dz <= STAR_TAG_HORIZONTAL_SQ
                && Math.abs(markerY - tagY) <= STAR_TAG_VERTICAL;
    }

    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}
