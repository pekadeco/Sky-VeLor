package com.arena.pekaclient.dungeonesp;

/**
 * Tight, pure geometry contract for the Dungeon Enderman/Fel armor-stand head marker.
 *
 * <p>The marker has no useful vanilla body hitbox, but the old 1.1 by 2.7 proxy was too large for sleeping Fels.
 * This centered 0.76 by 1.85 proxy matches the full Fel/Enderman head-marker representation while avoiding the old
 * oversized 1.1 by 2.7 outline around floor targets.</p>
 */
public final class DungeonEspMarkerBounds {

    public static final double HALF_WIDTH = .38D;
    public static final double MIN_Y_OFFSET = 0D;
    public static final double HEIGHT = 1.85D;

    private DungeonEspMarkerBounds() {
    }

    public static double minX(double x) { return x - HALF_WIDTH; }
    public static double maxX(double x) { return x + HALF_WIDTH; }
    public static double minY(double y) { return y + MIN_Y_OFFSET; }
    public static double maxY(double y) { return minY(y) + HEIGHT; }
    public static double minZ(double z) { return z - HALF_WIDTH; }
    public static double maxZ(double z) { return z + HALF_WIDTH; }
}
