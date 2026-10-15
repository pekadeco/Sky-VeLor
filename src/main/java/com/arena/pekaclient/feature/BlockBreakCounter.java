package com.arena.pekaclient.feature;

/**
 * Rolling count of block-break timestamps, used to turn breaks into blocks per second.
 *
 * Pure Java with no Minecraft types, so it can be unit-tested. Timestamps are in nanoseconds
 * and must be non-decreasing. The buffer is fixed size and never allocates after construction.
 */
public final class BlockBreakCounter {

    private final long[] times;
    private int head;
    private int size;
    private int total;
    private boolean any;
    private long firstNs;

    public BlockBreakCounter(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.times = new long[capacity];
    }

    public void reset() {
        head = 0;
        size = 0;
        total = 0;
        any = false;
        firstNs = 0L;
    }

    /** Records one successful block break at {@code nowNs}. */
    public void record(long nowNs) {
        if (!any) {
            any = true;
            firstNs = nowNs;
        }
        times[head] = nowNs;
        head = (head + 1) % times.length;
        if (size < times.length) {
            size++;
        }
        total++;
    }

    /** Number of breaks in the last {@code windowNs} nanoseconds. Stops early at the first older one. */
    public int countWithin(long nowNs, long windowNs) {
        int n = 0;
        for (int i = 0; i < size; i++) {
            int idx = head - 1 - i;
            if (idx < 0) {
                idx += times.length;
            }
            if (nowNs - times[idx] > windowNs) {
                break;
            }
            n++;
        }
        return n;
    }

    /**
     * Blocks per second over the last {@code windowSeconds}.
     *
     * The divisor is the window, or the time since the first break when the session is younger than
     * the window. This stops a single early break from reading as a low rate for the whole window.
     * Returns 0 when there are no breaks or the window is not positive.
     */
    public float rate(long nowNs, float windowSeconds) {
        if (!any || !(windowSeconds > 0f)) {
            return 0f;
        }
        long windowNs = (long) (windowSeconds * 1.0e9);
        int n = countWithin(nowNs, windowNs);
        double elapsed = Math.max(0L, nowNs - firstNs) / 1.0e9;
        double divisor = Math.min(windowSeconds, Math.max(1.0, elapsed));
        return (float) (n / divisor);
    }

    /** Total breaks recorded since the last reset. */
    public int total() {
        return total;
    }
}
