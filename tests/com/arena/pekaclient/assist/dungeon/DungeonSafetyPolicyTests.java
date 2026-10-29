package com.arena.pekaclient.assist.dungeon;

/** Strict false-positive regression checks for the room-alert and chest-close allow-lists. */
public final class DungeonSafetyPolicyTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        onlyExactNormalChestTitlesAreAllowed();
        roomAlertRejectsSpecialAndRepeatedRooms();
        roomAlertAcceptsVisibleSidebarProgressWithoutChat();
        done();
    }

    private static void onlyExactNormalChestTitlesAreAllowed() {
        check("plain Chest is allowed", DungeonChestClosePolicy.isNormalSecretChestTitle("Chest"));
        check("formatted Large Chest is allowed", DungeonChestClosePolicy.isNormalSecretChestTitle("\u00a76Large Chest"));
        check("reward chest is rejected", !DungeonChestClosePolicy.isNormalSecretChestTitle("Reward Chest"));
        check("end reward menu is rejected", !DungeonChestClosePolicy.isNormalSecretChestTitle("Dungeon Reward Chest"));
        check("terminal title is rejected", !DungeonChestClosePolicy.isNormalSecretChestTitle("Click in order!"));
        check("substring title is rejected", !DungeonChestClosePolicy.isNormalSecretChestTitle("Large Chest Rewards"));
    }

    private static void roomAlertRejectsSpecialAndRepeatedRooms() {
        RoomClearAlertPolicy policy = new RoomClearAlertPolicy();
        check("normal clear in a confirmed dungeon is allowed", policy.shouldAlert(true,
                "You have cleared this room!", "12:18", 1000L));
        check("same room cannot alert twice", !policy.shouldAlert(true,
                "You have cleared this room!", "12:18", 2000L));
        check("blood clear text is rejected", !policy.shouldAlert(true,
                "You have cleared the Blood room!", "13:18", 3000L));
        check("fairy clear text is rejected", !policy.shouldAlert(true,
                "You have cleared the Fairy room!", "14:18", 4000L));
        check("entrance clear text is rejected", !policy.shouldAlert(true,
                "You have cleared the Entrance room!", "15:18", 5000L));
        check("already-cleared message is rejected", !policy.shouldAlert(true,
                "You have already cleared this room!", "16:18", 6000L));
        check("outside a dungeon is rejected", !policy.shouldAlert(false,
                "You have cleared this room!", "17:18", 7000L));
        policy.reset();
        check("reset permits the same coordinate in a new run", policy.shouldAlert(true,
                "Room Cleared!", "12:18", 8000L));
    }

    private static void roomAlertAcceptsVisibleSidebarProgressWithoutChat() {
        RoomClearAlertPolicy policy = new RoomClearAlertPolicy();
        check("first Cleared sidebar value is only a baseline", !policy.shouldAlertClearedProgress(true,
                -1, 12, "1:1", 1000L));
        check("Cleared sidebar increase alerts without chat", policy.shouldAlertClearedProgress(true,
                12, 17, "1:1", 3000L));
        check("nearby chat/sidebar confirmations de-duplicate", !policy.shouldAlert(true,
                "Room Cleared!", "5:1", 3500L));
        check("same Cleared value cannot alert twice", !policy.shouldAlertClearedProgress(true,
                12, 17, "2:1", 6000L));
        check("impossible stale progress jump is rejected", !policy.shouldAlertClearedProgress(true,
                17, 60, "3:1", 9000L));
        check("sidebar progress outside a dungeon is rejected", !policy.shouldAlertClearedProgress(false,
                17, 20, "4:1", 12000L));
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++;
        else { failed++; System.out.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("dungeon safety policies: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
