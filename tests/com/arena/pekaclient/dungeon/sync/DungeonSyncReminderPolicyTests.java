package com.arena.pekaclient.dungeon.sync;

public final class DungeonSyncReminderPolicyTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        DungeonSyncReminderPolicy policy = new DungeonSyncReminderPolicy();
        check("no reminder outside a dungeon", !policy.shouldShowReminder(false, true, false));
        check("disabled sync prompts on first dungeon tick", policy.shouldShowReminder(true, false, false));
        check("reminder is not repeated every tick", !policy.shouldShowReminder(true, false, false));
        check("reminder is not repeated after sync becomes enabled", !policy.shouldShowReminder(true, false, true));

        for (int i = 0; i < DungeonSyncReminderPolicy.OUTSIDE_DUNGEON_RESET_TICKS - 1; i++) {
            policy.shouldShowReminder(false, true, false);
        }
        check("brief outside period does not repeat the reminder", !policy.shouldShowReminder(true, false, false));
        for (int i = 0; i < DungeonSyncReminderPolicy.OUTSIDE_DUNGEON_RESET_TICKS; i++) {
            policy.shouldShowReminder(false, true, false);
        }
        check("a new dungeon run can show the reminder again", policy.shouldShowReminder(true, false, false));
        policy.onDungeonStartCountdown();
        check("a new start countdown resets the reminder cycle", policy.shouldShowReminder(true, false, false));

        DungeonSyncReminderPolicy transition = new DungeonSyncReminderPolicy();
        check("first run prompts", transition.shouldShowReminder(true, false, false));
        for (int i = 0; i < DungeonSyncReminderPolicy.OUTSIDE_DUNGEON_RESET_TICKS + 5; i++) {
            transition.shouldShowReminder(false, false, false);
        }
        check("unknown world transitions cannot reset the current-run reminder", !transition.shouldShowReminder(true, false, false));

        DungeonSyncReminderPolicy enabledFirst = new DungeonSyncReminderPolicy();
        check("enabled sync never prompts at entry", !enabledFirst.shouldShowReminder(true, false, true));
        check("if sync is disabled mid-run, it can prompt once", enabledFirst.shouldShowReminder(true, false, false));

        System.out.println("DungeonSyncReminderPolicyTests: " + passed + " passed, " + failed + " failed");
        if (failed != 0) throw new AssertionError("Dungeon sync reminder policy test failures: " + failed);
    }

    private static void check(String label, boolean okay) {
        if (okay) passed++;
        else { failed++; System.out.println("FAIL: " + label); }
    }
}
