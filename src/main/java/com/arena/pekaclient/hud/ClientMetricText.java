package com.arena.pekaclient.hud;

/**
 * Cached client-metric labels. Formatting is deliberately small and Minecraft-free so the HUD updates values at a
 * bounded cadence instead of allocating a new string on every render frame.
 */
public final class ClientMetricText {
    public static final String UNKNOWN = "--";

    private ClientMetricText() { }

    public static String fps(int value) {
        return "FPS " + (value < 0 ? UNKNOWN : Integer.toString(value));
    }

    public static String ping(int value) {
        return "Ping " + (value < 0 ? UNKNOWN : Integer.toString(value)) + " ms";
    }

    /** Used/max heap in MB. Negative input is retained as a clear unknown value. */
    public static String memory(long usedBytes, long maxBytes) {
        if (usedBytes < 0L || maxBytes < 0L) return "Memory " + UNKNOWN + " / " + UNKNOWN + " MB";
        return "Memory " + megabytes(usedBytes) + " / " + megabytes(maxBytes) + " MB";
    }

    private static long megabytes(long bytes) {
        return Math.max(0L, bytes) / (1024L * 1024L);
    }
}
