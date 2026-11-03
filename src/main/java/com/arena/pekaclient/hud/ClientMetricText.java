package com.arena.pekaclient.hud;

/**
 * Cached client-metric labels for the settings Home surface. Formatting stays Minecraft-free so the live cards update
 * only at their bounded sampling cadence instead of allocating a new string during every rendered frame.
 */
public final class ClientMetricText {
    public static final String UNKNOWN = "--";

    private ClientMetricText() { }

    /** Compatibility label retained for existing read-only callers. */
    public static String fps(int value) {
        return "FPS " + fpsValue(value);
    }

    /** Compatibility label retained for existing read-only callers. */
    public static String ping(int value) {
        return "Ping " + pingValue(value);
    }

    /** Used/max heap in MB. Negative input is retained as a clear unknown value. */
    public static String memory(long usedBytes, long maxBytes) {
        return "Memory " + memoryDetail(usedBytes, maxBytes);
    }

    /** Large Home-card FPS value. */
    public static String fpsValue(int value) {
        return value < 0 ? UNKNOWN : Integer.toString(value);
    }

    /** Large Home-card ping value. Unknown deliberately remains explicit rather than pretending a connection exists. */
    public static String pingValue(int value) {
        return value < 0 ? UNKNOWN + " ms" : Integer.toString(value) + " ms";
    }

    /** Large Home-card memory value. */
    public static String memoryValue(long usedBytes) {
        return usedBytes < 0L ? UNKNOWN + " MB" : megabytes(usedBytes) + " MB";
    }

    /** Compact Home-card supporting text, preserving both used and maximum Java heap values. */
    public static String memoryDetail(long usedBytes, long maxBytes) {
        if (usedBytes < 0L || maxBytes < 0L) return UNKNOWN + " / " + UNKNOWN + " MB";
        return megabytes(usedBytes) + " / " + megabytes(maxBytes) + " MB";
    }

    private static long megabytes(long bytes) {
        return Math.max(0L, bytes) / (1024L * 1024L);
    }
}
