package com.arena.pekaclient.feature;

import java.util.Arrays;
import java.util.Collections;

/** Checks the corroborated sidebar/tab test used to decide whether the player is in a dungeon. */
public final class DungeonContextTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        catacombsLinesMatch();
        fakepixelDungeonEvidenceMatches();
        publicTabDungeonEvidenceMatches();
        otherLinesDoNot();
        freshSidebarResultsExplainDecision();
        System.out.println();
        System.out.println("dungeon context: passed " + passed + ", failed " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void freshSidebarResultsExplainDecision() {
        java.util.List<String> lines = Arrays.asList("§7Purse: 1,000", "§cThe Catacombs (F7)");
        DungeonContext.Result hit = DungeonContext.evaluateSidebar("§bSKYBLOCK", lines);
        check("fresh sidebar finds a positive dungeon result", hit.inDungeon, hit.reason);
        check("fresh sidebar preserves matched visible line", "The Catacombs (F7)".equals(hit.matchedScoreboardLine),
                hit.matchedScoreboardLine);
        check("fresh sidebar reports matching reason", hit.reason.contains("Catacombs"), hit.reason);
        check("fresh sidebar parses floor", "F7".equals(hit.floor), hit.floor);

        DungeonContext.Result uncertain = DungeonContext.evaluateSidebar("§bSKYBLOCK",
                Arrays.asList("§7Purse: 1,000", "§aVillage"));
        check("unmatched sidebar fails closed", !uncertain.inDungeon, uncertain.reason);
        check("unmatched sidebar reports why", uncertain.reason.contains("no corroborated"), uncertain.reason);
    }

    private static void fakepixelDungeonEvidenceMatches() {
        DungeonContext.Result labeled = DungeonContext.evaluateSidebar("§6FAKEPIXEL",
                Arrays.asList("§eDungeon: F6", "§aCleared: 42%"));
        check("generic Dungeon label plus floor matches", labeled.inDungeon, labeled.reason);
        check("generic Dungeon label identifies floor", "F6".equals(labeled.floor), labeled.floor);
        check("generic Dungeon label identifies dungeon", "Dungeon".equals(labeled.dungeon), labeled.dungeon);

        DungeonContext.Result metrics = DungeonContext.evaluateSidebar("§6FAKEPIXEL",
                Arrays.asList("§7Floor: M3", "§aCleared: 30%", "§bSecrets: 50%", "§eDeaths: 0"));
        check("floor plus two visible dungeon metrics matches", metrics.inDungeon, metrics.reason);
        check("master floor is parsed", "M3".equals(metrics.floor), metrics.floor);

        DungeonContext.Result weak = DungeonContext.evaluateSidebar("§6FAKEPIXEL",
                Arrays.asList("§eDungeon Hub", "§7Purse: 1,000"));
        check("generic dungeon wording without corroboration fails closed", !weak.inDungeon, weak.reason);
    }

    private static void publicTabDungeonEvidenceMatches() {
        DungeonContext.Result tabLed = DungeonContext.evaluateVisible("§bSKYBLOCK",
                Arrays.asList("§7Dungeon Cleared: 42%", "§7Purse: 1,000"),
                Arrays.asList("§eDungeon: The Catacombs (F6)", "§bSecrets Found: 53%", "§eTime: 4:20"));
        check("SkyBlock sidebar plus Fakepixel public Dungeon tab row matches", tabLed.inDungeon, tabLed.reason);
        check("tab-led dungeon floor is preserved", "F6".equals(tabLed.floor), tabLed.floor);
        check("tab-led reason identifies the public Dungeon source", tabLed.reason.contains("public tab Dungeon"),
                tabLed.reason);

        DungeonContext.Result loneTab = DungeonContext.evaluateVisible("§7Lobby", Collections.<String>emptyList(),
                Collections.singletonList("§eDungeon: The Catacombs (F6)"));
        check("a Dungeon-looking tab row without sidebar server context fails closed", !loneTab.inDungeon,
                loneTab.reason);
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
