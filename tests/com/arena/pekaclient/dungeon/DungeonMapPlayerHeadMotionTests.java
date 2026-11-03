package com.arena.pekaclient.dungeon;

/** Pure contract for smooth, bounded dungeon-map player-head placement. */
public final class DungeonMapPlayerHeadMotionTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        newMarkersStartWithoutAFlickerJump();
        markerUpdatesUsePartialTickInterpolation();
        facesStayInsideTheMapViewport();
        fiveHundredPercentProjectionRemainsReachable();
        System.out.println("dungeon map player heads: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void newMarkersStartWithoutAFlickerJump() {
        DungeonMapPlayerHeadMotion motion = new DungeonMapPlayerHeadMotion();
        motion.update(24.0F, 80.0F);
        check("first marker starts exactly at its first position", close(motion.x(.0F), 24.0F)
                && close(motion.y(.75F), 80.0F));
    }

    private static void markerUpdatesUsePartialTickInterpolation() {
        DungeonMapPlayerHeadMotion motion = new DungeonMapPlayerHeadMotion();
        motion.update(10.0F, 20.0F);
        motion.advanceTick();
        motion.update(30.0F, 60.0F);
        check("new marker update begins at previous tick coordinate", close(motion.x(0.0F), 10.0F)
                && close(motion.y(0.0F), 20.0F));
        check("partial tick samples the midpoint", close(motion.x(.5F), 20.0F)
                && close(motion.y(.5F), 40.0F));
        check("partial tick one reaches the new marker coordinate", close(motion.x(1.0F), 30.0F)
                && close(motion.y(1.0F), 60.0F));
        motion.advanceTick();
        check("completed interpolation remains stable on the next tick", close(motion.x(0.0F), 30.0F)
                && close(motion.y(.4F), 60.0F));
    }

    private static void facesStayInsideTheMapViewport() {
        check("left/top head center is clamped by half size", close(DungeonMapPlayerHeadMotion.clampCenter(-90.0F, 8.0F), 4.0F));
        check("right/bottom head center is clamped by half size", close(DungeonMapPlayerHeadMotion.clampCenter(300.0F, 8.0F), 124.0F));
        check("in-bounds player head remains untouched", close(DungeonMapPlayerHeadMotion.clampCenter(64.0F, 16.0F), 64.0F));
    }

    private static void fiveHundredPercentProjectionRemainsReachable() {
        DungeonMapProjection projection = new DungeonMapProjection(false, 64.0F, 64.0F, 0.0F, 5.0F);
        DungeonMapProjection tooHigh = new DungeonMapProjection(false, 64.0F, 64.0F, 0.0F, 6.0F);
        check("five hundred percent zoom is not cut back to the former 300 percent ceiling", close(projection.zoom(), 5.0F));
        check("invalid projection zoom still has a safe maximum", close(tooHigh.zoom(), 5.0F));
    }

    private static boolean close(float left, float right) {
        return Math.abs(left - right) < .01F;
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++; else { failed++; System.out.println("FAIL " + name); }
    }
}
