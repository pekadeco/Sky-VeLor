package com.arena.pekaclient.itemlockswap;

/** Mock-clock checks for short tap lock gesture versus held/slot-click pair gesture. */
public final class ItemLockSwapInputStateTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        ItemLockSwapInputState input = new ItemLockSwapInputState();
        check("press arms gesture", input.press(100L) && input.isDown());
        check("short clean tap locks", input.releaseIsTap(399L) && !input.isDown());

        check("hold arms gesture", input.press(1000L));
        check("300ms exact boundary is not tap", !input.releaseIsTap(1300L));

        check("press for selection", input.press(2000L));
        input.markSlotClick();
        check("slot click makes release a hold", !input.releaseIsTap(2010L));

        check("first key-down begins a hold", input.press(3000L));
        check("repeated key-down does not restart its clock", !input.press(3200L));
        check("repeat cannot make an old hold tap", !input.releaseIsTap(3400L));

        check("cancel releases stuck input", input.press(4000L));
        input.cancel();
        check("cancelled gesture cannot tap", !input.releaseIsTap(4010L));

        System.out.println("item lock/swap input state: passed " + passed + ", failed " + failed);
        if (failed > 0) throw new AssertionError("item lock/swap input failures: " + failed);
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++;
        else {
            failed++;
            System.err.println("FAIL " + name);
        }
    }
}
