package com.arena.pekaclient.dungeon.sync;

/**
 * Rate-limits the optional Dungeon Sync reminder to once per dungeon run. A short
 * world/scoreboard transition does not count as leaving the run; the reminder resets
 * only after the player has been in a confirmed non-dungeon SkyBlock area for a grace period.
 */
public final class DungeonSyncReminderPolicy {
    static final int OUTSIDE_DUNGEON_RESET_TICKS = 40;

    private boolean shownForCurrentRun;
    private int confirmedOutsideTicks;

    /** A confirmed new countdown can also start a fresh reminder cycle after reconnecting. */
    public void onDungeonStartCountdown() {
        shownForCurrentRun = false;
        confirmedOutsideTicks = 0;
    }

    /** Returns true once when a player is in a dungeon with sync disabled. */
    public boolean shouldShowReminder(boolean inDungeon, boolean confirmedOutsideDungeon, boolean syncEnabled) {
        if (inDungeon) {
            confirmedOutsideTicks = 0;
            if (syncEnabled || shownForCurrentRun) return false;
            shownForCurrentRun = true;
            return true;
        }

        // Unknown context (disconnect/loading/scoreboard transition) must not reset a run.
        if (!confirmedOutsideDungeon) {
            confirmedOutsideTicks = 0;
            return false;
        }
        if (confirmedOutsideTicks < OUTSIDE_DUNGEON_RESET_TICKS) {
            confirmedOutsideTicks++;
        }
        if (confirmedOutsideTicks >= OUTSIDE_DUNGEON_RESET_TICKS) {
            shownForCurrentRun = false;
        }
        return false;
    }
}
