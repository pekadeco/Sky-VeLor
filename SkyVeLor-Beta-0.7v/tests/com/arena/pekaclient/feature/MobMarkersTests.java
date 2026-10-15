package com.arena.pekaclient.feature;

/** Checks the name-tag classification for dungeon mob highlights. */
public final class MobMarkersTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        levelTagIsNormal();
        eliteStarIsElite();
        othersAreNone();
        System.out.println();
        System.out.println("mob markers: passed " + passed + ", failed " + failed);
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

    private static void levelTagIsNormal() {
        check("level tag", MobMarkers.classify("\u00a78[\u00a77Lv15\u00a78] \u00a7aZombie") == MobMarkers.Kind.NORMAL, "");
        check("two digit", MobMarkers.classify("[Lv100] Skeleton") == MobMarkers.Kind.NORMAL, "");
    }

    private static void eliteStarIsElite() {
        check("star", MobMarkers.classify("[Lv50] \u272F Mage") == MobMarkers.Kind.ELITE, "");
    }

    private static void othersAreNone() {
        check("null", MobMarkers.classify(null) == MobMarkers.Kind.NONE, "");
        check("empty", MobMarkers.classify("") == MobMarkers.Kind.NONE, "");
        check("player name", MobMarkers.classify("PlayerName") == MobMarkers.Kind.NONE, "");
        check("no tag", MobMarkers.classify("Lv15 Zombie") == MobMarkers.Kind.NONE, "");
        check("too many digits", MobMarkers.classify("[Lv1000] Zombie") == MobMarkers.Kind.NONE, "");
    }
}
