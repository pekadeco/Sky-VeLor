package com.arena.pekaclient.dungeonesp;

/** Geometry proof that the sleeping Fel/Enderman marker proxy is visibly smaller than the retired oversized box. */
public final class DungeonEspMarkerBoundsTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        double x = 10D, y = 64D, z = -2D;
        double width = DungeonEspMarkerBounds.maxX(x) - DungeonEspMarkerBounds.minX(x);
        double height = DungeonEspMarkerBounds.maxY(y) - DungeonEspMarkerBounds.minY(y);
        double depth = DungeonEspMarkerBounds.maxZ(z) - DungeonEspMarkerBounds.minZ(z);
        check("marker remains centered horizontally", close(DungeonEspMarkerBounds.minX(x) + width * .5D, x)
                && close(DungeonEspMarkerBounds.minZ(z) + depth * .5D, z));
        check("head marker proxy is the refined 0.68 width", close(width, .68D) && close(depth, .68D));
        check("head marker proxy is the refined 1.60 height", close(height, 1.60D));
        check("head marker proxy begins just below its skull anchor", close(DungeonEspMarkerBounds.minY(y), y - .10D));
        check("sleeping Fel marker proxy stays shorter than the old 2.7 outline", height < 2.7D && height >= 1.5D);
        System.out.println("Dungeon ESP marker bounds: passed " + passed + ", failed " + failed);
        if (failed != 0) System.exit(1);
    }

    private static boolean close(double one, double two) { return Math.abs(one - two) < .00001D; }
    private static void check(String label, boolean okay) {
        if (okay) passed++; else { failed++; System.out.println("FAIL " + label); }
    }
}
