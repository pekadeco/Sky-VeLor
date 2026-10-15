package com.arena.pekaclient.fishing;

/** Checks catch-line detection, the Jawbus and Festival lines, and nameplate parsing for Sea Creature HP. */
public final class FishingTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        catchLinesAreDetected();
        jawbusAndFestivalLines();
        nameplatesAreParsed();
        System.out.println();
        System.out.println("fishing: passed " + passed + ", failed " + failed);
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

    private static void catchLinesAreDetected() {
        check("loch emperor", SeaCreatureParser.detectLegendaryCreature(
                "THE LOCH EMPEROR ARISES FROM THE DEPTHS") == LegendaryCreature.THE_LOCH_EMPEROR, "");
        check("formatted line", SeaCreatureParser.detectLegendaryCreature(
                "\u00a7c\u00a7lHIDE NO LONGER, A GREAT WHITE SHARK HAS TRACKED YOUR SCENT")
                == LegendaryCreature.GREAT_WHITE_SHARK, "");
        check("unrelated chat", SeaCreatureParser.detectLegendaryCreature("Hello there") == null, "");
        check("null", SeaCreatureParser.detectLegendaryCreature(null) == null, "");
    }

    private static void jawbusAndFestivalLines() {
        check("jawbus arrival", SeaCreatureParser.isLordJawbusArrival(
                "YOU HAVE ANGERED A LEGENDARY CREATURE... LORD JAWBUS HAS ARRIVED"), "");
        check("jawbus not arrival", !SeaCreatureParser.isLordJawbusArrival("Lord Jawbus"), "");
        check("festival end", SeaCreatureParser.isFestivalEnd(
                "FISHING FESTIVAL The festival has concluded! Time to dry off and repair your rods!"), "");
        check("festival other", !SeaCreatureParser.isFestivalEnd("FISHING FESTIVAL started"), "");
    }

    private static void nameplatesAreParsed() {
        SeaCreatureParser.Entry e = SeaCreatureParser.parseLabel(
                "\u00a77[Lvl 3] \u00a7bWater Hydra \u00a7a12.5k\u00a7f/\u00a7a20k \u00a7c\u2764", 7, 9.0D);
        check("hydra name", e != null && e.creatureName.equals("Water Hydra"), String.valueOf(e == null ? null : e.creatureName));
        check("hydra level", e != null && e.level == 3, "");
        check("hydra hp", e != null && e.currentHealth.equals("12.5k") && e.maxHealth.equals("20k"), "");
        SeaCreatureParser.Entry noHp = SeaCreatureParser.parseLabel("[Lvl 1] Alligator", 8, 1.0D);
        check("no hp", noHp != null && noHp.creatureName.equals("Alligator") && noHp.currentHealth.isEmpty(), "");
        check("not a sea creature", SeaCreatureParser.parseLabel("[Lvl 5] Zombie 10/20", 9, 1.0D) == null, "");
        check("empty", SeaCreatureParser.parseLabel("", 1, 0.0D) == null, "");
        check("row text", SeaCreatureHud.rowText(e).equals("[Lvl 3] Water Hydra  12.5k/20k"),
                SeaCreatureHud.rowText(e));
    }
}
