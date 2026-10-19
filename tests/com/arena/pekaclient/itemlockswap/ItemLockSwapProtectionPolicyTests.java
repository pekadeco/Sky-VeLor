package com.arena.pekaclient.itemlockswap;

/** Exhaustive mode decision tests; Minecraft objects are intentionally not needed for this packet policy. */
public final class ItemLockSwapProtectionPolicyTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("mode 0 pickup blocks protected target", ItemLockSwapClickPolicy.shouldBlock(0, true, false, false));
        check("mode 1 quick move blocks protected target", ItemLockSwapClickPolicy.shouldBlock(1, true, false, false));
        check("mode 2 number swap blocks protected destination", ItemLockSwapClickPolicy.shouldBlock(2, true, false, false));
        check("mode 2 number swap blocks protected source", ItemLockSwapClickPolicy.shouldBlock(2, false, true, false));
        check("mode 3 clone blocks protected target", ItemLockSwapClickPolicy.shouldBlock(3, true, false, false));
        check("mode 4 throw blocks protected target", ItemLockSwapClickPolicy.shouldBlock(4, true, false, false));
        check("mode 5 drag blocks protected target", ItemLockSwapClickPolicy.shouldBlock(5, true, false, false));
        check("mode 6 collect blocks protected target", ItemLockSwapClickPolicy.shouldBlock(6, true, false, false));
        check("mode 6 collect blocks matching protected candidate", ItemLockSwapClickPolicy.shouldBlock(6, false, false, true));
        check("normal unprotected modes pass", !ItemLockSwapClickPolicy.shouldBlock(0, false, false, false)
                && !ItemLockSwapClickPolicy.shouldBlock(1, false, false, false)
                && !ItemLockSwapClickPolicy.shouldBlock(2, false, false, false)
                && !ItemLockSwapClickPolicy.shouldBlock(3, false, false, false)
                && !ItemLockSwapClickPolicy.shouldBlock(4, false, false, false)
                && !ItemLockSwapClickPolicy.shouldBlock(5, false, false, false)
                && !ItemLockSwapClickPolicy.shouldBlock(6, false, false, false));
        check("unknown mode stays vanilla", !ItemLockSwapClickPolicy.shouldBlock(7, true, true, true));
        check("Q drops selected protected hotbar position", ItemLockSwapDropPolicy.shouldBlock(true, true, false, false));
        check("Ctrl Q uses the same selected-position drop guard", ItemLockSwapDropPolicy.shouldBlock(true, true, false, true));
        check("disabled feature and internal swap never block drop", !ItemLockSwapDropPolicy.shouldBlock(false, true, false, false)
                && !ItemLockSwapDropPolicy.shouldBlock(true, true, true, true));
        System.out.println("item lock/swap protection policy: passed " + passed + ", failed " + failed);
        if (failed > 0) throw new AssertionError("item lock/swap protection failures: " + failed);
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++;
        else {
            failed++;
            System.err.println("FAIL " + name);
        }
    }
}
