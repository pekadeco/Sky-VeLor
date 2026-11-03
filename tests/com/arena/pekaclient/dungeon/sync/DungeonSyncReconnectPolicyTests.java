package com.arena.pekaclient.dungeon.sync;

public final class DungeonSyncReconnectPolicyTests {
    private static int passed;

    public static void main(String[] args) {
        testBoundedExponentialBackoff();
        testHealthyConnectionResetsBackoff();
        testShortConnectionContinuesBackoff();
        testInvalidCurrentDelayUsesMinimum();
        System.out.println("DungeonSyncReconnectPolicyTests: " + passed + " passed, 0 failed");
    }

    private static void testBoundedExponentialBackoff() {
        long delay = DungeonSyncReconnectPolicy.INITIAL_DELAY_MS;
        long[] expected = {3000L, 6000L, 12000L, 24000L, 30000L, 30000L};
        for (long value : expected) {
            delay = DungeonSyncReconnectPolicy.nextDelay(delay);
            equal(value, delay, "backoff progression");
        }
    }

    private static void testHealthyConnectionResetsBackoff() {
        equal(DungeonSyncReconnectPolicy.INITIAL_DELAY_MS,
                DungeonSyncReconnectPolicy.afterDisconnect(24000L, 30000L), "healthy connection resets backoff");
        equal(DungeonSyncReconnectPolicy.INITIAL_DELAY_MS,
                DungeonSyncReconnectPolicy.afterDisconnect(30000L, 120000L), "long connection resets backoff");
    }

    private static void testShortConnectionContinuesBackoff() {
        equal(6000L, DungeonSyncReconnectPolicy.afterDisconnect(3000L, 29999L), "short connection continues backoff");
        equal(30000L, DungeonSyncReconnectPolicy.afterDisconnect(24000L, 200L), "short connection respects maximum");
    }

    private static void testInvalidCurrentDelayUsesMinimum() {
        equal(DungeonSyncReconnectPolicy.INITIAL_DELAY_MS,
                DungeonSyncReconnectPolicy.nextDelay(0L), "zero delay normalizes to minimum");
        equal(DungeonSyncReconnectPolicy.INITIAL_DELAY_MS,
                DungeonSyncReconnectPolicy.nextDelay(-1L), "negative delay normalizes to minimum");
    }

    private static void equal(long expected, long actual, String label) {
        if (expected != actual) throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        passed++;
    }
}
