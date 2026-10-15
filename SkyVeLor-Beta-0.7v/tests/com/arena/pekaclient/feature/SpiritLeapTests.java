package com.arena.pekaclient.feature;

/** Checks the Spirit Leap menu decision (title or recent use) and the held-item name check. */
public final class SpiritLeapTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        titleAlwaysCounts();
        fallbackNeedsEveryCondition();
        itemNames();
        System.out.println();
        System.out.println("spirit leap: passed " + passed + ", failed " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void check(String name, boolean ok, String detail) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL " + name + " " + detail);
        }
    }

    private static void titleAlwaysCounts() {
        check("title alone", SpiritLeap.decide(true, false, false, 0), "");
        check("title with no heads", SpiritLeap.decide(true, false, true, 0), "");
    }

    private static void fallbackNeedsEveryCondition() {
        check("no title, no use", !SpiritLeap.decide(false, false, true, 3), "");
        check("use but not dungeon", !SpiritLeap.decide(false, true, false, 3), "");
        check("use, dungeon, no heads", !SpiritLeap.decide(false, true, true, 0), "");
        check("all three", SpiritLeap.decide(false, true, true, 1), "");
    }

    private static void itemNames() {
        check("plain", LeapUse.isLeapItemName("Spirit Leap"), "");
        check("coloured", LeapUse.isLeapItemName("\u00a7bSpirit Leap"), "");
        check("case", LeapUse.isLeapItemName("spirit leap"), "");
        check("other item", !LeapUse.isLeapItemName("Iron Sword"), "");
        check("null", !LeapUse.isLeapItemName(null), "");
        check("partial", !LeapUse.isLeapItemName("Leap"), "");
    }
}
