package com.arena.pekaclient.dungeonesp;

/** Geometry proof that the Fel/Enderman marker proxy is compact but still covers the complete marker representation. */
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
        check("Fel head marker proxy is the reliable 0.76 width", close(width, .76D) && close(depth, .76D));
        check("Fel head marker proxy is the reliable 1.85 height", close(height, 1.85D));
        check("Fel head marker proxy begins at its marker anchor", close(DungeonEspMarkerBounds.minY(y), y));
        check("Fel marker proxy stays smaller than the old 2.7 outline while covering its full marker", height < 2.7D && height >= 1.8D);
        System.out.println("Dungeon ESP marker bounds: passed " + passed + ", failed " + failed);
        if (failed != 0) System.exit(1);
    }

    private static boolean close(double one, double two) { return Math.abs(one - two) < .00001D; }
    private static void check(String label, boolean okay) {
        if (okay) passed++; else { failed++; System.out.println("FAIL " + label); }
    }
}
