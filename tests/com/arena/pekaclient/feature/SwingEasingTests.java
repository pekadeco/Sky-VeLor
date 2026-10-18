package com.arena.pekaclient.feature;

/** Pure curve checks: Smooth Swing must be continuous, bounded, stateless, and linear at zero smoothness. */
public final class SwingEasingTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("zero smoothness is exact linear progress", close(SwingEasing.ease(.37f, 0), .37f));
        check("endpoints remain exact", SwingEasing.ease(0f, 100) == 0f && SwingEasing.ease(1f, 100) == 1f);
        float left = SwingEasing.ease(.499f, 65);
        float middle = SwingEasing.ease(.5f, 65);
        float right = SwingEasing.ease(.501f, 65);
        check("partial-tick curve is continuous and increasing", left < middle && middle < right);
        check("curve has no history or added lag", close(SwingEasing.ease(.42f, 65), SwingEasing.ease(.42f, 65)));
        check("out-of-range and non-finite input fail safely", SwingEasing.ease(-3f, 100) == 0f
                && SwingEasing.ease(3f, 100) == 1f && SwingEasing.ease(Float.NaN, 100) == 0f);

        System.out.println();
        System.out.println("swing easing: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static boolean close(float a, float b) {
        return Math.abs(a - b) < .00001f;
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++; else { failed++; System.out.println("FAIL " + name); }
    }
}
