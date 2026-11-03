package com.arena.pekaclient.feature;

/** Checks the name-tag classification for dungeon mob highlights. */
public final class MobMarkersTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        levelTagIsNormal();
        eliteStarIsElite();
        betterNefStarIsElite();
        similarStarsAreNotEliteMarkers();
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
        check("level tag spacing", MobMarkers.classify("[Lv 15] Zombie") == MobMarkers.Kind.NORMAL, "");
        check("uppercase level tag", MobMarkers.classify("[LV15] Zombie") == MobMarkers.Kind.NORMAL, "");
    }

    private static void eliteStarIsElite() {
        check("exact Better NEF star", MobMarkers.classify("[Lv50] \u272E Mage") == MobMarkers.Kind.ELITE, "");
        check("star survives colour codes", MobMarkers.classify("[Lv50] \u00a7e\u272E \u00a7aSkeleton Master") == MobMarkers.Kind.ELITE, "");
        check("starred name without level tag", MobMarkers.classify("\u272E Skeleton Master") == MobMarkers.Kind.ELITE, "");
    }

    private static void betterNefStarIsElite() {
        check("Better NEF exact marker U+272E", MobMarkers.classify("[Lv50] \u272E Skeleton Master") == MobMarkers.Kind.ELITE, "");
    }

    private static void similarStarsAreNotEliteMarkers() {
        check("similar star U+272F is not elite", MobMarkers.classify("\u272F Mage") == MobMarkers.Kind.NONE, "");
        check("U+272F with level remains a normal mob", MobMarkers.classify("[Lv50] \u272F Mage") == MobMarkers.Kind.NORMAL, "");
        check("black star is not elite marker", MobMarkers.classify("\u2605 Skeleton") == MobMarkers.Kind.NONE, "");
    }

    private static void othersAreNone() {
        check("null", MobMarkers.classify(null) == MobMarkers.Kind.NONE, "");
        check("empty", MobMarkers.classify("") == MobMarkers.Kind.NONE, "");
        check("player name", MobMarkers.classify("PlayerName") == MobMarkers.Kind.NONE, "");
        check("no tag", MobMarkers.classify("Lv15 Zombie") == MobMarkers.Kind.NONE, "");
        check("tag elsewhere in name is not accepted", MobMarkers.classify("Zombie [Lv15]") == MobMarkers.Kind.NONE, "");
        check("tag in ordinary text is not accepted", MobMarkers.classify("Player note [Lv15] Zombie") == MobMarkers.Kind.NONE, "");
        check("too many digits", MobMarkers.classify("[Lv1000] Zombie") == MobMarkers.Kind.NONE, "");
    }
}
