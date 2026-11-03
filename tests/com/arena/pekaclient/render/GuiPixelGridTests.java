package com.arena.pekaclient.render;

/** Pure regression coverage for crisp native-font scale/origin policy at Minecraft GUI scales. */
public final class GuiPixelGridTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        textScalesResolveToWholePhysicalPixels();
        originsSnapToTheSamePhysicalGrid();
        invalidInputsStaySafe();
        System.out.println("GUI pixel grid: passed " + passed + ", failed " + failed);
        if (failed != 0) System.exit(1);
    }

    private static void textScalesResolveToWholePhysicalPixels() {
        float[] requested = {1.34f, 1.08f, .88f, .72f, .62f, .52f};
        for (int factor = 1; factor <= 4; factor++) {
            for (float scale : requested) {
                float resolved = GuiPixelGrid.textScale(scale, factor);
                float physical = resolved * factor;
                check("scale " + scale + " is integral at GUI factor " + factor,
                        physical >= 1f && Math.abs(physical - Math.round(physical)) < .0001f);
            }
        }
        check("compact body scale rounds to the nearest physical glyph size",
                close(GuiPixelGrid.textScale(.72f, 2), .5f));
        check("heading scale stays readable when a tie rounds upward",
                close(GuiPixelGrid.textScale(.88f, 2), 1f));
    }

    private static void originsSnapToTheSamePhysicalGrid() {
        for (int factor = 1; factor <= 4; factor++) {
            float snapped = GuiPixelGrid.coordinate(17.37f, factor);
            float physical = snapped * factor;
            check("origin is integral at GUI factor " + factor,
                    Math.abs(physical - Math.round(physical)) < .0001f);
        }
        check("factor two keeps a half-logical-pixel origin when it is a whole physical pixel",
                close(GuiPixelGrid.coordinate(9.26f, 2), 9.5f));
    }

    private static void invalidInputsStaySafe() {
        check("zero/negative factors resolve to one", GuiPixelGrid.factor(0) == 1 && GuiPixelGrid.factor(-4) == 1);
        check("zero logical scale remains visible", GuiPixelGrid.textScale(0f, 4) == .25f);
    }

    private static boolean close(float a, float b) {
        return Math.abs(a - b) < .0001f;
    }

    private static void check(String label, boolean okay) {
        if (okay) passed++; else { failed++; System.out.println("FAIL " + label); }
    }
}
