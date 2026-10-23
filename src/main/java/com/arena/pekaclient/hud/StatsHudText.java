package com.arena.pekaclient.hud;

import java.util.Locale;

/** Exact, image-backed labels for the three-card Stats Display HUD. */
public final class StatsHudText {
    public static final String UNKNOWN = "--";
    private static final long MEGABYTE = 1024L * 1024L;

    private StatsHudText() {
    }

    /** "115 fps" in the original lower-case unit form. */
    public static String frameRate(int fps) {
        return (fps < 0 ? UNKNOWN : Integer.toString(fps)) + " fps";
    }

    /** "0.00 BPS" with exactly two fractional digits, matching the restored Mining card. */
    public static String mining(float blocksPerSecond) {
        float safe = Float.isNaN(blocksPerSecond) || Float.isInfinite(blocksPerSecond)
                ? 0f : Math.max(0f, blocksPerSecond);
        return String.format(Locale.ROOT, "%.2f BPS", safe);
    }

    /** The restored memory card presents currently used heap only, rather than an invented total bar. */
    public static String memory(long usedBytes) {
        return (usedBytes < 0L ? UNKNOWN : Long.toString(Math.max(0L, usedBytes) / MEGABYTE)) + " MB";
    }
}
