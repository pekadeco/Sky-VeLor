package com.arena.pekaclient.dungeon;

public final class DungeonMapProjectionTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        DungeonMapProjection staticMap = new DungeonMapProjection(false, 37.0F, 79.0F, 90.0F);
        assertPoint("static mode leaves a map point unchanged", staticMap.project(20.0F, 31.0F), 20.0F, 31.0F);
        DungeonMapProjection staticZoomIn = new DungeonMapProjection(false, 64.0F, 64.0F, 0.0F, 2.0F);
        assertPoint("static content zoom-in scales away from the map center", staticZoomIn.project(74.0F, 60.0F), 84.0F, 56.0F);
        DungeonMapProjection staticZoomOut = new DungeonMapProjection(false, 64.0F, 64.0F, 0.0F, 0.5F);
        assertPoint("static content zoom-out moves points toward the center", staticZoomOut.project(0.0F, 0.0F), 32.0F, 32.0F);

        DungeonMapProjection rotated = new DungeonMapProjection(true, 37.0F, 79.0F, 90.0F);
        assertPoint("self pivot stays exactly at map center", rotated.project(37.0F, 79.0F), 64.0F, 64.0F);
        assertTrue("puzzle label glyphs inherit live map rotation", Math.abs(rotated.textRotation() - 90.0F) < 0.001F);
        assertTrue("static map keeps puzzle label glyphs level", Math.abs(staticMap.textRotation()) < 0.001F);
        DungeonMapProjection rotatedZoomIn = new DungeonMapProjection(true, 37.0F, 79.0F, 0.0F, 2.0F);
        assertPoint("rotating content can zoom beyond the fit scale", rotatedZoomIn.project(47.0F, 79.0F), 84.0F, 64.0F);
        assertPoint("rotation keeps a label anchored to its world position", rotated.project(47.0F, 79.0F), 64.0F, 74.0F);
        assertPoint("rotated projection uses both map axes", rotated.project(37.0F, 69.0F), 74.0F, 64.0F);
        float[] reusablePoint = new float[2];
        rotated.project(37.0F, 69.0F, reusablePoint);
        assertPoint("reusable projection output matches the allocation-based path", reusablePoint, 74.0F, 64.0F);

        float centeredFit = DungeonMapProjection.fitZoom(64.0F, 64.0F);
        assertTrue("45-degree rotation zooms out enough to fit the full map", centeredFit < 0.71F && centeredFit > 0.65F);
        DungeonMapProjection fitted = new DungeonMapProjection(true, 64.0F, 64.0F, 45.0F, centeredFit);
        assertInsideInset("top-left map corner remains visible after fit", fitted.project(0.0F, 0.0F));
        assertInsideInset("bottom-right map corner remains visible after fit", fitted.project(128.0F, 128.0F));

        float edgeFit = DungeonMapProjection.fitZoom(0.0F, 0.0F);
        DungeonMapProjection edgeFitted = new DungeonMapProjection(true, 0.0F, 0.0F, 45.0F, edgeFit);
        for (float x : new float[]{0.0F, 128.0F}) {
            for (float y : new float[]{0.0F, 128.0F}) {
                assertInsideInset("edge-pivot corner remains visible " + x + "," + y, edgeFitted.project(x, y));
            }
        }

        float stableZoom = DungeonMapProjection.fitZoom(23.0F, 106.0F);
        for (float angle : new float[]{0.0F, 30.0F, 45.0F, 90.0F, 135.0F, 180.0F, 225.0F, 270.0F, 315.0F}) {
            DungeonMapProjection atAngle = new DungeonMapProjection(true, 23.0F, 106.0F, angle, stableZoom);
            for (float x : new float[]{0.0F, 128.0F}) {
                for (float y : new float[]{0.0F, 128.0F}) {
                    assertInsideInset("fixed zoom keeps corners visible at " + angle + " degrees", atAngle.project(x, y));
                }
            }
        }

        System.out.println("DungeonMapProjectionTests: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void assertInsideInset(String label, float[] actual) {
        boolean ok = actual[0] >= 3.999F && actual[0] <= 124.001F
                && actual[1] >= 3.999F && actual[1] <= 124.001F;
        if (ok) passed++;
        else {
            failed++;
            System.out.println("FAIL " + label + ": got (" + actual[0] + ", " + actual[1] + ")");
        }
    }

    private static void assertTrue(String label, boolean condition) {
        if (condition) passed++;
        else { failed++; System.out.println("FAIL " + label); }
    }

    private static void assertPoint(String label, float[] actual, float expectedX, float expectedY) {
        boolean ok = Math.abs(actual[0] - expectedX) < 0.001F && Math.abs(actual[1] - expectedY) < 0.001F;
        if (ok) passed++;
        else {
            failed++;
            System.out.println("FAIL " + label + ": got (" + actual[0] + ", " + actual[1] + ")");
        }
    }
}
