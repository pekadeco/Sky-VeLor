package com.arena.pekaclient.dungeonesp;

/** Pure threshold and association regression coverage for the conservative Fel/Enderman head marker gate. */
public final class DungeonEspEndermanHeadPolicyTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("marker below two-block ceiling remains eligible",
                DungeonEspEndermanHeadPolicy.belowPlayerCeiling(65.999D, 64D));
        check("marker exactly two blocks above player is suppressed",
                !DungeonEspEndermanHeadPolicy.belowPlayerCeiling(66D, 64D));
        check("marker above two-block ceiling is suppressed",
                !DungeonEspEndermanHeadPolicy.belowPlayerCeiling(66.01D, 64D));
        check("starred association is accepted",
                DungeonEspEndermanHeadPolicy.acceptsAssociatedCategory(DungeonEspCategory.STARRED));
        check("special Fel association is accepted",
                DungeonEspEndermanHeadPolicy.acceptsAssociatedCategory(DungeonEspCategory.SPECIAL));
        check("miniboss-only tag is not enough evidence for a head marker",
                !DungeonEspEndermanHeadPolicy.acceptsAssociatedCategory(DungeonEspCategory.MINIBOSS));
        check("near tag within horizontal and vertical window associates",
                DungeonEspEndermanHeadPolicy.isNearbyTag(5D, 65D, 5D, 6.4D, 67.9D, 5D));
        check("far horizontal tag cannot associate",
                !DungeonEspEndermanHeadPolicy.isNearbyTag(5D, 65D, 5D, 6.51D, 65D, 5D));
        check("far vertical tag cannot associate",
                !DungeonEspEndermanHeadPolicy.isNearbyTag(5D, 65D, 5D, 5D, 68.51D, 5D));
        System.out.println("Dungeon ESP Enderman head policy: passed " + passed + ", failed " + failed);
        if (failed != 0) System.exit(1);
    }

    private static void check(String label, boolean ok) {
        if (ok) passed++; else { failed++; System.out.println("FAIL " + label); }
    }
}
