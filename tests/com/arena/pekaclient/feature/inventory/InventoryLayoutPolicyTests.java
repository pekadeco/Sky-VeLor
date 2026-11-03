package com.arena.pekaclient.feature.inventory;

public final class InventoryLayoutPolicyTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        int center = (854 - 176) / 2;
        check("inventory centers when no potion effects are active",
                InventoryLayoutPolicy.playerInventoryLeft(854, false, false) == center);
        check("visible potion panel reserves the vanilla 60-pixel offset",
                InventoryLayoutPolicy.playerInventoryLeft(854, true, false) == center + 60);
        check("hidden potion panel recenters inventory",
                InventoryLayoutPolicy.playerInventoryLeft(854, true, true) == center);
        check("hiding option does not shift inventory without effects",
                InventoryLayoutPolicy.playerInventoryLeft(854, false, true) == center);
        check("player inventory keeps vanilla vertical centering",
                InventoryLayoutPolicy.playerInventoryTop(480) == (480 - 166) / 2);
        check("chest layout stays centered across six rows",
                InventoryLayoutPolicy.chestTop(480, 6) == (480 - (114 + 6 * 18)) / 2);
        System.out.println("InventoryLayoutPolicyTests: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void check(String name, boolean condition) {
        if (condition) passed++;
        else { failed++; System.out.println("FAIL: " + name); }
    }
}
