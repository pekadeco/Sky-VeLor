package com.arena.pekaclient.assist.sync;

/** Bounded retry timing for the optional transport; a failed endpoint never creates a rapid connection loop. */
public final class DungeonSyncReconnectPolicy {
    public static final long INITIAL_DELAY_MS = 1500L;
    public static final long MAX_DELAY_MS = 30000L;
    private static final long HEALTHY_MS = 30000L;

    private DungeonSyncReconnectPolicy() {
    }

    public static long afterDisconnect(long previousDelay, long connectedForMillis) {
        if (connectedForMillis >= HEALTHY_MS) return INITIAL_DELAY_MS;
        if (previousDelay < INITIAL_DELAY_MS) return INITIAL_DELAY_MS;
        return Math.min(MAX_DELAY_MS, previousDelay * 2L);
    }
}
