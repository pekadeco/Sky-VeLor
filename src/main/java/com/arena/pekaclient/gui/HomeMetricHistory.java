package com.arena.pekaclient.gui;

import com.arena.pekaclient.hud.ClientMetrics;

/**
 * Fixed-size history for Home's three live cards. Arrays are allocated once with the settings screen and samples are
 * recorded only when {@link ClientMetrics.Snapshot#generation()} advances, so repeated frames allocate nothing and
 * cannot grow the buffer.
 */
public final class HomeMetricHistory {
    public static final int CAPACITY = 24;
    public static final int FPS = 0;
    public static final int MEMORY = 1;
    public static final int PING = 2;

    private final int[] fps = new int[CAPACITY];
    private final int[] ping = new int[CAPACITY];
    private final float[] memory = new float[CAPACITY];
    private int next;
    private int size;
    private long lastGeneration = Long.MIN_VALUE;

    /** Returns true only for a new bounded-cadence metric snapshot. */
    public boolean record(ClientMetrics.Snapshot snapshot) {
        if (snapshot == null || snapshot.generation() == lastGeneration) return false;
        lastGeneration = snapshot.generation();
        fps[next] = snapshot.fps();
        ping[next] = snapshot.pingMs();
        memory[next] = snapshot.usedBytes() < 0L || snapshot.maxBytes() <= 0L
                ? -1f : snapshot.memoryRatio();
        next = (next + 1) % CAPACITY;
        if (size < CAPACITY) size++;
        return true;
    }

    public int size() { return size; }
    public int capacity() { return CAPACITY; }

    /** Values are addressed oldest-to-newest, avoiding a copied/allocated display array. */
    public float normalizedAt(int metric, int index) {
        if (index < 0 || index >= size) return -1f;
        int slot = (next - size + index + CAPACITY) % CAPACITY;
        if (metric == FPS) return fps[slot] < 0 ? -1f : Math.min(1f, fps[slot] / 60f);
        if (metric == MEMORY) return memory[slot];
        if (metric == PING) return ping[slot] < 0 ? -1f : Math.max(0f, 1f - Math.min(1f, ping[slot] / 200f));
        return -1f;
    }
}
