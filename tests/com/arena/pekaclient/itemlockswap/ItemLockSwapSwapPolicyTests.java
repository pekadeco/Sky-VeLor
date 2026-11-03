package com.arena.pekaclient.itemlockswap;

/** Headless transaction policy coverage for hotbar/main/armor, empty positions, invalid fits and cooldown. */
public final class ItemLockSwapSwapPolicyTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("main inventory to hotbar allows two items", result(true, true, true, true, true, true, true, 5000L, 0L)
                == ItemLockSwapSwapPolicy.Result.ALLOW);
        check("empty hotbar destination allows item", result(true, true, true, true, true, false, true, 5000L, 0L)
                == ItemLockSwapSwapPolicy.Result.ALLOW);
        check("empty source accepts armor item", result(true, true, true, false, true, true, true, 5000L, 0L)
                == ItemLockSwapSwapPolicy.Result.ALLOW);
        check("main item rejected by armor target reports fit", result(true, true, true, true, false, true, true, 5000L, 0L)
                == ItemLockSwapSwapPolicy.Result.ITEM_DOES_NOT_FIT);
        check("armor item rejected by destination reports fit", result(true, true, true, true, true, true, false, 5000L, 0L)
                == ItemLockSwapSwapPolicy.Result.ITEM_DOES_NOT_FIT);
        check("cursor stack prevents transaction", result(false, true, true, true, true, true, true, 5000L, 0L)
                == ItemLockSwapSwapPolicy.Result.CURSOR_NOT_EMPTY);
        check("unavailable armor slot prevents transaction", result(true, false, true, true, true, true, true, 5000L, 0L)
                == ItemLockSwapSwapPolicy.Result.SLOT_UNAVAILABLE);
        check("cooldown blocks early swap", result(true, true, true, true, true, true, true, 6000L, 5000L)
                == ItemLockSwapSwapPolicy.Result.COOLDOWN);
        check("cooldown expires exactly at boundary", result(true, true, true, true, true, true, true, 7000L, 5000L)
                == ItemLockSwapSwapPolicy.Result.ALLOW);
        check("remaining time uses mock clock", ItemLockSwapSwapPolicy.remainingMillis(5500L, 5000L, 2000) == 1500L
                && ItemLockSwapSwapPolicy.remainingMillis(7000L, 5000L, 2000) == 0L);
        System.out.println("item lock/swap swap policy: passed " + passed + ", failed " + failed);
        if (failed > 0) throw new AssertionError("item lock/swap swap policy failures: " + failed);
    }

    private static ItemLockSwapSwapPolicy.Result result(boolean cursorEmpty, boolean firstCanTake, boolean secondCanTake,
                                                         boolean firstHas, boolean secondAcceptsFirst,
                                                         boolean secondHas, boolean firstAcceptsSecond,
                                                         long now, long lastSwap) {
        return ItemLockSwapSwapPolicy.evaluate(cursorEmpty, firstCanTake, secondCanTake, firstHas,
                secondAcceptsFirst, secondHas, firstAcceptsSecond, now, lastSwap, 2000);
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++;
        else {
            failed++;
            System.err.println("FAIL " + name);
        }
    }
}
