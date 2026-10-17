package com.arena.pekaclient.fishing;

import java.util.Properties;

/** Checks catch-line detection, counter categories, HUD config compatibility, and Sea Creature HP parsing. */
public final class FishingTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        catchLinesAreDetected();
        allRarityCatchLinesAreDetected();
        counterCategories();
        fishingHudConfigCompatibility();
        scanScheduling();
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

    private static void allRarityCatchLinesAreDetected() {
        check("reindrake", SeaCreatureParser.detectLegendaryCreature(
                "A Reindrake forms from the depths. [COPY]") == LegendaryCreature.REINDRAKE, "");
        check("common squid", SeaCreatureParser.detectLegendaryCreature(
                "A Squid appeared. [COPY]") == LegendaryCreature.SQUID, "");
        check("common sea guardian", SeaCreatureParser.detectLegendaryCreature(
                "You've stumbled upon a Sea Guardian. [COPY]") == LegendaryCreature.SEA_GUARDIAN, "");
        for (LegendaryCreature creature : LegendaryCreature.values()) {
            check("counter announcement: " + creature.displayName,
                    SeaCreatureParser.detectLegendaryCreature(creature.chatPrefixes[0] + " extra") == creature,
                    creature.chatPrefixes[0]);
        }
    }

    private static void counterCategories() {
        check("squid shares Common Creature row", !LegendaryCreature.SQUID.legendary, "");
        check("frozen steve shares Common Creature row", !LegendaryCreature.FROZEN_STEVE.legendary, "");
        check("nurse shark shares Common Creature row", !LegendaryCreature.NURSE_SHARK.legendary, "");
        check("yeti retains its own row", LegendaryCreature.YETI.legendary, "");
        check("reindrake retains its own row", LegendaryCreature.REINDRAKE.legendary, "");
        check("jawbus retains its own row", LegendaryCreature.LORD_JAWBUS.legendary, "");
        int shared = 0;
        int separate = 0;
        for (LegendaryCreature creature : LegendaryCreature.values()) {
            if (creature.legendary) {
                separate++;
            } else {
                shared++;
            }
        }
        check("all Common-through-Epic definitions", shared == 64, "count=" + shared);
        check("all Legendary/Mythic definitions", separate == 14, "count=" + separate);
    }

    private static void fishingHudConfigCompatibility() {
        FishingHud hud = new FishingHud();
        Properties legacy = new Properties();
        legacy.setProperty("fishing.catches", "false");
        legacy.setProperty("fishing.festival", "false");
        hud.load(legacy);
        check("existing fishing HUD configs enable compact CC row", hud.showCommonCreature, "");
        hud.showCommonCreature = false;
        Properties saved = new Properties();
        hud.save(saved);
        check("CC row has its own stable HUD config key",
                "false".equals(saved.getProperty("fishing.commonCreature")),
                saved.getProperty("fishing.commonCreature"));
    }

    private static void scanScheduling() {
        check("first HP scan runs", SeaCreatureHud.shouldScan(1000L, Long.MIN_VALUE), "");
        check("HP scan waits inside interval", !SeaCreatureHud.shouldScan(1200L, 1000L), "");
        check("HP scan repeats at interval", SeaCreatureHud.shouldScan(1500L, 1000L), "");
        check("HP scan survives clock rollback", SeaCreatureHud.shouldScan(900L, 1000L), "");
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
