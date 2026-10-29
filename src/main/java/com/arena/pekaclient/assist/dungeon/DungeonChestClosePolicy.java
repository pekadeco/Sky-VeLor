package com.arena.pekaclient.assist.dungeon;

/** Strict allow-list for the optional dungeon chest close path. */
public final class DungeonChestClosePolicy {
    private DungeonChestClosePolicy() {
    }

    /**
     * Returns true only for Fakepixel's plain normal secret chest titles. Reward chests, terminals, and every other
     * menu intentionally return false, including titles that merely contain the word "Chest".
     */
    public static boolean isNormalSecretChestTitle(String rawTitle) {
        String title = DungeonRunReadout.normalize(rawTitle);
        return "chest".equalsIgnoreCase(title) || "large chest".equalsIgnoreCase(title);
    }
}
