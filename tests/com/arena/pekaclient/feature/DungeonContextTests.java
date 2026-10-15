package com.arena.pekaclient.feature;

/** Checks the sidebar line test used to decide whether the player is in a dungeon. */
public final class DungeonContextTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        catacombsLinesMatch();
        otherLinesDoNot();
        System.out.println();
        System.out.println("dungeon context: passed " + passed + ", failed " + failed);
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

    private static void catacombsLinesMatch() {
        check("plain", DungeonContext.isCatacombsLine("The Catacombs (F5)"), "");
        check("coloured", DungeonContext.isCatacombsLine("\u00a77Catacombs"), "");
        check("lower case", DungeonContext.isCatacombsLine("catacombs"), "");
    }

    private static void otherLinesDoNot() {
        check("null", !DungeonContext.isCatacombsLine(null), "");
        check("empty", !DungeonContext.isCatacombsLine(""), "");
        check("hub", !DungeonContext.isCatacombsLine("Hub"), "");
        check("balance line", !DungeonContext.isCatacombsLine("Purse: 1,000"), "");
    }
}
