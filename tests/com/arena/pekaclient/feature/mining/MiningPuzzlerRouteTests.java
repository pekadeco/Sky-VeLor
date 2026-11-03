package com.arena.pekaclient.feature.mining;

public final class MiningPuzzlerRouteTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        int[] leftUpRight = MiningPuzzlerRoute.targetFromMessage("[NPC] Puzzler: ◀ ▲ ▶");
        check("Puzzler route is parsed", leftUpRight != null);
        check("direction steps reach expected coordinates", leftUpRight != null && leftUpRight[0] == 181
                && leftUpRight[1] == 195 && leftUpRight[2] == 136);
        int[] aliases = MiningPuzzlerRoute.targetFromMessage("[NPC] Puzzler: ←↑↓→");
        check("standard arrow aliases are supported", aliases != null && aliases[0] == 181 && aliases[2] == 135);
        int[] formatted = MiningPuzzlerRoute.targetFromMessage("§e[NPC] Puzzler: §f◀◀");
        check("colored message parses", formatted != null && formatted[0] == 183 && formatted[2] == 135);
        check("non-Puzzler messages are ignored", MiningPuzzlerRoute.targetFromMessage("[NPC] Stonk: ◀▲") == null);
        check("Puzzler message marker is recognized", MiningPuzzlerRoute.isPuzzlerMessage("[NPC] Puzzler: ◀"));
        check("other NPC markers are not recognized", !MiningPuzzlerRoute.isPuzzlerMessage("[NPC] Stonk: ◀"));
        check("missing route is rejected", MiningPuzzlerRoute.targetFromMessage("[NPC] Puzzler:") == null);
        check("invalid route characters are rejected", MiningPuzzlerRoute.targetFromMessage("[NPC] Puzzler: LURD") == null);
        check("oversized routes are rejected", MiningPuzzlerRoute.targetFromMessage("[NPC] Puzzler: " + repeat("◀", 65)) == null);
        check("null lines are ignored", MiningPuzzlerRoute.targetFromMessage(null) == null);
        finish();
    }

    private static String repeat(String value, int count) {
        StringBuilder out = new StringBuilder(value.length() * count);
        for (int i = 0; i < count; i++) out.append(value);
        return out.toString();
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++;
        else { failed++; System.out.println("FAIL " + name); }
    }

    private static void finish() {
        System.out.println("mining puzzler route: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
