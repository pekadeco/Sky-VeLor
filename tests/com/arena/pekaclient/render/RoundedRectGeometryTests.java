package com.arena.pekaclient.render;

/** Headless proof for the shared cached rounded-rectangle perimeter used by every GUI surface. */
public final class RoundedRectGeometryTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        cacheReusesTheSameGeometryWithoutFrameAllocation();
        eachCornerIsClampedIndependentlyAndEdgesJoin();
        scaledGuiGeometryRemainsContinuousFromScaleOneToFour();
        roundedCornerPolicyKeepsEveryQualityVisiblyCurved();
        namedMotionSettlesInsideTheRequestedGuiWindow();
        System.out.println("rounded geometry: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void cacheReusesTheSameGeometryWithoutFrameAllocation() {
        RoundedRectGeometry.clearCacheForTests();
        RoundedRectGeometry.Geometry first = RoundedRectGeometry.get(240f, 64f, 12f, 8f, 7f, 9f, 6);
        RoundedRectGeometry.Geometry second = RoundedRectGeometry.get(240f, 64f, 12f, 8f, 7f, 9f, 6);
        check("same dimensions and corner radii reuse a cached perimeter", first == second);
        check("cache records a hit instead of another geometry allocation", RoundedRectGeometry.cacheSize() == 1
                && RoundedRectGeometry.cacheMisses() == 1 && RoundedRectGeometry.cacheHits() == 1);

        for (int i = 0; i < RoundedRectGeometry.CACHE_CAPACITY + 20; i++) {
            RoundedRectGeometry.get(50f + i, 31f, 6f, 5f, 4f, 3f, 4);
        }
        check("bounded cache never grows per frame without limit",
                RoundedRectGeometry.cacheSize() == RoundedRectGeometry.CACHE_CAPACITY);
    }

    private static void eachCornerIsClampedIndependentlyAndEdgesJoin() {
        RoundedRectGeometry.Geometry geometry = RoundedRectGeometry.get(20f, 10f, 99f, -3f, 6f, 8f, 8);
        check("top-left radius clamps to half of the shortest side", close(geometry.topLeft, 5f));
        check("negative radius clamps to zero", close(geometry.topRight, 0f));
        check("each remaining radius independently clamps", close(geometry.bottomRight, 5f)
                && close(geometry.bottomLeft, 5f));
        check("all cached perimeter points stay within the surface", pointsStayInBounds(geometry));
        check("corner arcs meet the four straight edges without a gap", edgesJoin(geometry));
    }

    private static void scaledGuiGeometryRemainsContinuousFromScaleOneToFour() {
        for (int scale = 1; scale <= 4; scale++) {
            RoundedRectGeometry.Geometry geometry = RoundedRectGeometry.get(132f * scale, 37f * scale,
                    9f * scale, 7f * scale, 8f * scale, 6f * scale, 8);
            check("scale " + scale + " keeps every corner inside its GUI surface", pointsStayInBounds(geometry));
            check("scale " + scale + " keeps the cached perimeter joined", edgesJoin(geometry));
            check("scale " + scale + " preserves an approximately one GUI pixel anti-alias unit",
                    close(GuiStyle.AA_FRINGE * scale, (float) scale));
        }
    }

    private static void roundedCornerPolicyKeepsEveryQualityVisiblyCurved() {
        check("shared geometry permits smooth 16-segment quarter arcs", RoundedRectGeometry.MAX_SEGMENTS >= 16);
        check("12 px window corner uses the selected smooth quality", RoundedRectGeometry.segmentsFor(12f, 16) == 12);
        check("Balanced no longer caps a 12 px corner at four facets", RoundedRectGeometry.segmentsFor(12f, 12) >= 12);
        check("Fast remains rounded rather than using the old two-facet corner", RoundedRectGeometry.segmentsFor(12f, 8) == 8);
    }

    private static void namedMotionSettlesInsideTheRequestedGuiWindow() {
        check("hover/toggle/tab durations are the requested compact range",
                GuiMotion.HOVER_SETTLE_MS >= 120 && GuiMotion.HOVER_SETTLE_MS <= 180
                        && GuiMotion.TOGGLE_SETTLE_MS >= 120 && GuiMotion.TOGGLE_SETTLE_MS <= 180
                        && GuiMotion.TAB_SETTLE_MS >= 120 && GuiMotion.TAB_SETTLE_MS <= 180);
        float value = 0f;
        for (int frame = 0; frame < 10; frame++) {
            value = GuiMotion.ease(value, 1f, .016f, true, GuiMotion.TOGGLE_SETTLE_MS, 1f);
        }
        check("toggle easing reaches its target smoothly instead of jumping", value > .94f && value < 1f);
        check("closed GUI immediately stops easing and rests", GuiMotion.ease(value, 0f, .016f, false,
                GuiMotion.TOGGLE_SETTLE_MS, 1f) == 0f);
    }

    private static boolean pointsStayInBounds(RoundedRectGeometry.Geometry geometry) {
        for (int i = 0; i < geometry.pointCount; i++) {
            if (geometry.x(i) < -.001f || geometry.x(i) > geometry.width + .001f
                    || geometry.y(i) < -.001f || geometry.y(i) > geometry.height + .001f) return false;
        }
        return true;
    }

    private static boolean edgesJoin(RoundedRectGeometry.Geometry geometry) {
        int step = geometry.segments + 1;
        int topLeftEnd = step - 1;
        int topRightStart = step;
        int topRightEnd = step * 2 - 1;
        int bottomRightStart = step * 2;
        int bottomRightEnd = step * 3 - 1;
        int bottomLeftStart = step * 3;
        int bottomLeftEnd = geometry.pointCount - 1;
        return close(geometry.y(topLeftEnd), 0f) && close(geometry.y(topRightStart), 0f)
                && geometry.x(topLeftEnd) <= geometry.x(topRightStart) + .001f
                && close(geometry.x(topRightEnd), geometry.width) && close(geometry.x(bottomRightStart), geometry.width)
                && geometry.y(topRightEnd) <= geometry.y(bottomRightStart) + .001f
                && close(geometry.y(bottomRightEnd), geometry.height) && close(geometry.y(bottomLeftStart), geometry.height)
                && geometry.x(bottomLeftStart) <= geometry.x(bottomRightEnd) + .001f
                && close(geometry.x(bottomLeftEnd), 0f) && close(geometry.x(0), 0f)
                && geometry.y(0) <= geometry.y(bottomLeftEnd) + .001f;
    }

    private static boolean close(float one, float two) {
        return Math.abs(one - two) < .01f;
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++; else { failed++; System.out.println("FAIL " + name); }
    }
}
