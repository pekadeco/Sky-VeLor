package com.arena.pekaclient.dungeon.sync;

/** Bounded reconnect timing so an unavailable endpoint cannot trigger a rapid request loop. */
public final class DungeonSyncReconnectPolicy {
    public static final long INITIAL_DELAY_MS = 1500L;
    public static final long MAX_DELAY_MS = 30000L;
    public static final long HEALTHY_CONNECTION_MS = 30000L;

    private DungeonSyncReconnectPolicy() { }

    /** Returns the next delay after a failed or short-lived connection attempt. */
    public static long nextDelay(long currentDelayMs) {
        if (currentDelayMs < INITIAL_DELAY_MS) return INITIAL_DELAY_MS;
        if (currentDelayMs >= MAX_DELAY_MS) return MAX_DELAY_MS;
        return Math.min(MAX_DELAY_MS, currentDelayMs * 2L);
    }

    /** A connection that stayed alive for at least 30 seconds resets the retry schedule. */
    public static long afterDisconnect(long currentDelayMs, long connectionDurationMs) {
        if (connectionDurationMs >= HEALTHY_CONNECTION_MS) return INITIAL_DELAY_MS;
        return nextDelay(currentDelayMs);
    }
}
