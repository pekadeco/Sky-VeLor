package com.arena.pekaclient.itemlockswap;

/** Endpoint regression checks for the exact shared geometry used by persistent pair and live cursor lines. */
public final class ItemLockSwapLineGeometryTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("first endpoint is slot center", ItemLockSwapLineGeometry.slotCenterX(11, 20) == 39f
                && ItemLockSwapLineGeometry.slotCenterY(27, 42) == 77f);
        check("second endpoint keeps independent GUI origin", ItemLockSwapLineGeometry.slotCenterX(100, 3) == 111f
                && ItemLockSwapLineGeometry.slotCenterY(4, 130) == 142f);
        check("live endpoint uses current scaled cursor", ItemLockSwapLineGeometry.cursorX(319) == 319f
                && ItemLockSwapLineGeometry.cursorY(201) == 201f);
        System.out.println("item lock/swap line geometry: passed " + passed + ", failed " + failed);
        if (failed > 0) throw new AssertionError("item lock/swap line geometry failures: " + failed);
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++;
        else {
            failed++;
            System.err.println("FAIL " + name);
        }
    }
}
