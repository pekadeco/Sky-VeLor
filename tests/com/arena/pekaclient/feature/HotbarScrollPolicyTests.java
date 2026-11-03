package com.arena.pekaclient.feature;

/** Verifies the mouse policy never consumes GUI scrolling, zero-delta events, or keyboard hotbar selection. */
public final class HotbarScrollPolicyTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("enabled gameplay wheel up cancels", HotbarScrollPolicy.shouldCancel(true, 120, false));
        check("enabled gameplay wheel down cancels", HotbarScrollPolicy.shouldCancel(true, -120, false));
        check("disabled gameplay wheel remains vanilla", !HotbarScrollPolicy.shouldCancel(false, 120, false));
        check("zero wheel event remains vanilla", !HotbarScrollPolicy.shouldCancel(true, 0, false));
        check("chat or inventory GUI wheel remains vanilla", !HotbarScrollPolicy.shouldCancel(true, -120, true));

        System.out.println();
        System.out.println("hotbar scroll policy: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++; else { failed++; System.out.println("FAIL " + name); }
    }
}
