package com.arena.pekaclient.assist;

import com.arena.pekaclient.assist.dungeon.BossHealthParser;
import com.arena.pekaclient.assist.dungeon.TerminalAssistPolicy;
import com.arena.pekaclient.assist.inventory.InventorySearchPolicy;
import com.arena.pekaclient.assist.inventory.RarityPalette;
import com.arena.pekaclient.assist.inventory.RomanNumeralPolicy;
import com.arena.pekaclient.assist.inventory.TooltipWindow;
import com.arena.pekaclient.assist.mining.MiningAbilityPolicy;
import com.arena.pekaclient.assist.mining.PuzzlerRoute;
import com.arena.pekaclient.assist.movement.AutoSprintPolicy;
import com.arena.pekaclient.assist.sync.DungeonSyncEnvironment;
import com.arena.pekaclient.assist.sync.DungeonSyncIdentity;
import com.arena.pekaclient.assist.sync.DungeonSyncMessages;
import com.arena.pekaclient.assist.sync.DungeonSyncProtocol;
import com.arena.pekaclient.assist.sync.DungeonSyncReconnectPolicy;

import java.util.Arrays;

/** Headless behavioral regression suite for all clean-room assist policies. */
public final class AssistFeatureTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        miningPolicies();
        inventoryPolicies();
        bossAndTerminalPolicies();
        movementPolicies();
        syncPolicies();
        done();
    }

    private static void miningPolicies() {
        MiningAbilityPolicy.Activation pick = MiningAbilityPolicy.parseActivation("\u00a7aYou used your Pickobulus Pickaxe Ability!");
        check("parse Pickobulus", pick != null && pick.cooldownSeconds == 60);
        check("unknown ability ignored", MiningAbilityPolicy.parseActivation("You used your Unknown Pickaxe Ability!") == null);
        check("remaining seconds round up", MiningAbilityPolicy.remainingSeconds(2501L, 1501L) == 1L);
        int[] route = PuzzlerRoute.decode("[NPC] Puzzler: \u25c0\u25b2\u25ba");
        check("puzzler route decodes", route != null && route[0] == PuzzlerRoute.ORIGIN_X && route[1] == PuzzlerRoute.ORIGIN_Y
                && route[2] == PuzzlerRoute.ORIGIN_Z + 1);
        check("puzzler rejects unknown glyph", PuzzlerRoute.decode("[NPC] Puzzler: X") == null);
    }

    private static void inventoryPolicies() {
        check("search strips formatting and matches all terms", InventorySearchPolicy.matches("\u00a76Aspect of the Dragons", "aspect dragons"));
        check("search requires every term", !InventorySearchPolicy.matches("Aspect of the Dragons", "aspect void"));
        check("normalized search path agrees", InventorySearchPolicy.matchesNormalized("aspect of the dragons", "aspect dragons")
                && !InventorySearchPolicy.matchesNormalized("aspect of the dragons", "aspect void"));
        check("rarity parser rejects unknown and reads explicit lore", RarityPalette.colorForText("plain item") == 0
                && RarityPalette.colorForText("\u00a76LEGENDARY SWORD") == 0xFFFFAA00);
        check("roman canonical parse", RomanNumeralPolicy.parse("XIV") == 14 && RomanNumeralPolicy.parse("IIX") < 0);
        check("roman replaces bounded token", "Sharpness 5".equals(RomanNumeralPolicy.replaceLevels("Sharpness V")));
        check("tooltip window clamps", TooltipWindow.clampOffset(99, 15, 12) == 3
                && TooltipWindow.visible(Arrays.asList("1", "2", "3"), 1, 2).size() == 2);
    }

    private static void bossAndTerminalPolicies() {
        BossHealthParser.Entry entry = BossHealthParser.parse("\u00a7c[BOSS] Storm 400M/400M\u2764");
        check("boss parser recognizes faction boss", entry != null && "Storm".equals(entry.name) && entry.hasHealth());
        check("boss parser rejects unrelated text", BossHealthParser.parse("A healthy zombie") == null);
        check("terminal title is explicit", "select-colors".equals(TerminalAssistPolicy.kindForTitle("Select all the RED items!")));
        check("terminal automation needs both switches", !TerminalAssistPolicy.allowAutoClick(true, false, "maze", 500L, 0L)
                && TerminalAssistPolicy.allowAutoClick(true, true, "maze", 500L, 0L));
    }

    private static void movementPolicies() {
        check("auto sprint requires vanilla-safe movement", AutoSprintPolicy.mayStart(1F, false, false, false, false, true, false)
                && !AutoSprintPolicy.mayStart(.5F, false, false, false, false, true, false));
        check("auto sprint releases when forward is not held", AutoSprintPolicy.shouldHoldSprintKey(true, 1F,
                false, false, false, false, true, false)
                && !AutoSprintPolicy.shouldHoldSprintKey(false, 1F, false, false, false, false, true, false));
    }

    private static void syncPolicies() {
        String one = DungeonSyncIdentity.sessionId("sbdung-3", "M7", Arrays.asList("Alice_1", "Bob"));
        String two = DungeonSyncIdentity.sessionId("sbdung-3", "m7", Arrays.asList("bob", "ALICE_1"));
        check("sync identity is order/case stable", one.length() == 64 && one.equals(two));
        check("sync rejects solo and invalid identity", DungeonSyncIdentity.sessionId("sbdung-3", "F7", Arrays.asList("Solo")).length() == 0
                && DungeonSyncIdentity.sessionId("bad id", "F7", Arrays.asList("Alice", "Bob")).length() == 0);
        String payload = DungeonSyncProtocol.encodePuzzle("Water Board", 0, 70, 32, .9D);
        check("sync protocol bounds puzzle payload", payload.contains("puzzle_discovered") && payload.contains("\"water\"")
                && DungeonSyncProtocol.encodePuzzle("bad\"name", 0, 70, 32, .9D).length() == 0
                && DungeonSyncProtocol.encodePuzzle("water", 1, 70, 32, .9D).length() == 0);
        check("sync inbound parser rejects arbitrary text", DungeonSyncMessages.puzzleEvent(
                "{\"type\":\"puzzle_discovered\",\"name\":\"water\",\"x\":0,\"y\":70,\"z\":32,\"confidence\":0.9}") != null
                && DungeonSyncMessages.puzzleEvent("{\"type\":\"chat\",\"name\":\"water\"}") == null);
        check("sync environment extracts only strict identifiers", "mini42".equals(DungeonSyncEnvironment.findServerId(
                Arrays.asList("Server: mini42"))) && "M7".equals(DungeonSyncEnvironment.findFloor(
                Arrays.asList("Master Mode 7"))));
        check("sync reconnect backs off and caps", DungeonSyncReconnectPolicy.afterDisconnect(1500L, 0L) == 3000L
                && DungeonSyncReconnectPolicy.afterDisconnect(30000L, 0L) == 30000L
                && DungeonSyncReconnectPolicy.afterDisconnect(30000L, 30000L) == 1500L);
    }

    private static void check(String name, boolean ok) {
        if (ok) { passed++; System.out.println("ok   " + name); }
        else { failed++; System.err.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("assist features: passed " + passed + ", failed " + failed);
        if (failed != 0) throw new AssertionError(failed + " assist feature checks failed");
    }
}
