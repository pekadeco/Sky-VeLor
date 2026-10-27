package com.arena.pekaclient.assist.dungeon;

/** Name-matching contract for the render-only Spirit Bow locator. */
public final class SpiritBowLocatorTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        markerNameMatchingIsCaseInsensitiveAndNarrow();
        selfTestBeginsFailClosedUntilRegistration();
        done();
    }

    private static void markerNameMatchingIsCaseInsensitiveAndNarrow() {
        check("plain marker matches", SpiritBowLocator.isSpiritBowMarker("Spirit Bow"));
        check("format codes around marker do not prevent a match", SpiritBowLocator.isSpiritBowMarker("\u00a7d[\u00a7fSpirit Bow\u00a7d]"));
        check("case-insensitive marker matches", SpiritBowLocator.isSpiritBowMarker("spirit bow spawned"));
        check("unrelated armor stand does not match", !SpiritBowLocator.isSpiritBowMarker("Spirit Leap"));
        check("null marker does not match", !SpiritBowLocator.isSpiritBowMarker(null));
    }

    private static void selfTestBeginsFailClosedUntilRegistration() {
        check("no event registration reports unregistered rather than active", "UNREGISTERED".equals(SpiritBowLocator.selfTestStatus()));
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++; else { failed++; System.out.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("spirit bow locator: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
