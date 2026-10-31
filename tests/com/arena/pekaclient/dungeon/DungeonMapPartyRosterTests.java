package com.arena.pekaclient.dungeon;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class DungeonMapPartyRosterTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        List<String> tab = Arrays.asList("LocalPlayer", "RealOne", "RealTwo", "NpcMerchant", "DungeonGuide");
        List<String> squareClasses = DungeonMapPartyRoster.build(Arrays.asList(
                "§r[B] [MVP+] RealOne",
                "§r[M] RealTwo",
                "NpcMerchant",
                "Dungeon Guide: DungeonGuide"), tab, "LocalPlayer");
        check("square class markers select party members rather than every tab entry",
                squareClasses.equals(Arrays.asList("LocalPlayer", "RealOne", "RealTwo")));
        check("[A] badge parses as Archer", "ARCHER".equals(DungeonMapPartyRoster.classFromLine("[A] RealOne")));
        check("[B] badge parses as Berserk", "BERSERK".equals(DungeonMapPartyRoster.classFromLine("[B] RealOne")));
        check("[M] badge parses as Mage", "MAGE".equals(DungeonMapPartyRoster.classFromLine("[M] RealOne")));
        check("[H] badge parses as Healer", "HEALER".equals(DungeonMapPartyRoster.classFromLine("[H] RealOne")));
        check("[T] badge parses as Tank", "TANK".equals(DungeonMapPartyRoster.classFromLine("[T] RealOne")));
        check("class tags are read after Minecraft formatting codes", "MAGE".equals(
                DungeonMapPartyRoster.classFromLine("\u00a7b[M] \u00a7aRealOne")));
        List<String> numericNameParty = DungeonMapPartyRoster.build(Arrays.asList("[A] 300405", "[M] RealOne"),
                Arrays.asList("300405", "RealOne", "NpcMerchant"), "300405");
        check("numeric-only Minecraft usernames can be kept in a party roster",
                containsIgnoreCase(numericNameParty, "300405") && containsIgnoreCase(numericNameParty, "RealOne"));
        check("NPC names from tab list are excluded when class rows exist",
                !containsIgnoreCase(squareClasses, "NpcMerchant") && !containsIgnoreCase(squareClasses, "DungeonGuide"));

        List<String> parenClasses = DungeonMapPartyRoster.build(Arrays.asList(
                "RealOne (Archer)",
                "RealTwo (Mage)",
                "NpcMerchant"), tab, "LocalPlayer");
        check("full class names in parentheses are parsed", containsIgnoreCase(parenClasses, "RealOne")
                && containsIgnoreCase(parenClasses, "RealTwo"));
        check("parenthetical party parsing does not fall back to unrelated tab entries",
                !containsIgnoreCase(parenClasses, "NpcMerchant"));

        List<String> scoreboardFallback = DungeonMapPartyRoster.build(Arrays.asList(
                "Party: RealOne + RealTwo",
                "Dungeon status"), tab, "LocalPlayer");
        check("fallback only includes tab names actually present in the scoreboard",
                scoreboardFallback.equals(Arrays.asList("RealOne", "RealTwo", "LocalPlayer"))
                        || scoreboardFallback.equals(Arrays.asList("LocalPlayer", "RealOne", "RealTwo")));
        check("scoreboard fallback excludes tab-only NPCs", !containsIgnoreCase(scoreboardFallback, "NpcMerchant")
                && !containsIgnoreCase(scoreboardFallback, "DungeonGuide"));

        List<String> selfOnly = DungeonMapPartyRoster.build(Collections.<String>emptyList(), tab, "LocalPlayer");
        check("empty scoreboard does not turn the whole tab list into a dungeon roster",
                selfOnly.equals(Collections.singletonList("LocalPlayer")));

        System.out.println("DungeonMapPartyRosterTests: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static boolean containsIgnoreCase(List<String> names, String wanted) {
        for (String name : names) if (name != null && name.equalsIgnoreCase(wanted)) return true;
        return false;
    }

    private static void check(String label, boolean condition) {
        if (condition) passed++;
        else { failed++; System.out.println("FAIL: " + label); }
    }
}
