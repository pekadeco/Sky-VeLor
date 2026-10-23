package com.arena.pekaclient.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;

/**
 * Bounded-cadence client metrics for the General settings tab. This is deliberately not a HudElement:
 * FPS, local tab-list ping, and heap use are informative controls inside the GUI only.
 */
public final class ClientMetrics {
    public static final long REFRESH_MILLIS = 500L;
    public static final int COLOR_GOOD = 0xFF30D158;
    public static final int COLOR_WARN = 0xFFFFB347;
    public static final int COLOR_BAD = 0xFFFF453A;
    public static final int COLOR_UNKNOWN = 0xFF9AA6B5;

    public interface Provider {
        int fps();
        int localTabPingMs();
        long usedMemoryBytes();
        long maxMemoryBytes();
    }

    /** Reused snapshot; callers must read it, not retain it as a historical sample. */
    public static final class Snapshot {
        private int fps = -1;
        private int pingMs = -1;
        private long usedBytes = -1L;
        private long maxBytes = -1L;
        private String fpsText = ClientMetricText.fps(-1);
        private String pingText = ClientMetricText.ping(-1);
        private String memoryText = ClientMetricText.memory(-1L, -1L);

        private void set(int nextFps, int nextPing, long nextUsed, long nextMax) {
            fps = nextFps;
            pingMs = nextPing;
            usedBytes = nextUsed;
            maxBytes = nextMax;
            // Labels are allocated only on the 500 ms data refresh, never every GUI frame.
            fpsText = ClientMetricText.fps(fps);
            pingText = ClientMetricText.ping(pingMs);
            memoryText = ClientMetricText.memory(usedBytes, maxBytes);
        }

        public int fps() { return fps; }
        public int pingMs() { return pingMs; }
        public long usedBytes() { return usedBytes; }
        public long maxBytes() { return maxBytes; }
        public String fpsText() { return fpsText; }
        public String pingText() { return pingText; }
        public String memoryText() { return memoryText; }
        public int fpsColor() { return ClientMetrics.fpsColor(fps); }
        public int pingColor() { return ClientMetrics.pingColor(pingMs); }
        public int memoryColor() { return ClientMetrics.memoryColor(usedBytes, maxBytes); }
        public float memoryRatio() { return ClientMetrics.memoryRatio(usedBytes, maxBytes); }
    }

    /** Reader caches data for roughly twice-per-second General-tab refreshes with no per-frame snapshot allocation. */
    public static final class Reader {
        private final Provider provider;
        private final Snapshot snapshot = new Snapshot();
        private long nextRefreshAt;

        public Reader(Provider source) {
            provider = source == null ? UNKNOWN_PROVIDER : source;
        }

        public Snapshot snapshot(long nowMillis) {
            if (nowMillis >= nextRefreshAt) {
                try {
                    snapshot.set(provider.fps(), provider.localTabPingMs(), provider.usedMemoryBytes(), provider.maxMemoryBytes());
                } catch (RuntimeException ignored) {
                    snapshot.set(-1, -1, -1L, -1L);
                }
                nextRefreshAt = nowMillis + REFRESH_MILLIS;
            }
            return snapshot;
        }
    }

    private static final Provider UNKNOWN_PROVIDER = new Provider() {
        @Override public int fps() { return -1; }
        @Override public int localTabPingMs() { return -1; }
        @Override public long usedMemoryBytes() { return -1L; }
        @Override public long maxMemoryBytes() { return -1L; }
    };

    private static final Provider MINECRAFT_PROVIDER = new Provider() {
        @Override public int fps() { return Minecraft.getDebugFPS(); }
        @Override public int localTabPingMs() { return ownTabPingMs(); }
        @Override public long usedMemoryBytes() {
            Runtime runtime = Runtime.getRuntime();
            return runtime.totalMemory() - runtime.freeMemory();
        }
        @Override public long maxMemoryBytes() { return Runtime.getRuntime().maxMemory(); }
    };

    private ClientMetrics() { }

    public static Reader liveReader() { return new Reader(MINECRAFT_PROVIDER); }

    /** Player-only tab-list response time; this is never a server TPS estimate. */
    public static int ownTabPingMs() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return -1;
        NetHandlerPlayClient handler = mc.getNetHandler();
        if (handler == null) return -1;
        NetworkPlayerInfo info = handler.getPlayerInfo(mc.thePlayer.getName());
        return info == null || info.getResponseTime() < 0 ? -1 : info.getResponseTime();
    }

    public static int fpsColor(int fps) {
        if (fps < 0) return COLOR_UNKNOWN;
        return fps < 30 ? COLOR_BAD : fps < 60 ? COLOR_WARN : COLOR_GOOD;
    }

    public static int pingColor(int pingMs) {
        if (pingMs < 0) return COLOR_UNKNOWN;
        return pingMs > 150 ? COLOR_BAD : pingMs > 75 ? COLOR_WARN : COLOR_GOOD;
    }

    public static float memoryRatio(long usedBytes, long maxBytes) {
        if (usedBytes < 0L || maxBytes <= 0L) return 0f;
        return Math.max(0f, Math.min(1f, usedBytes / (float) maxBytes));
    }

    public static int memoryColor(long usedBytes, long maxBytes) {
        float ratio = memoryRatio(usedBytes, maxBytes);
        if (maxBytes <= 0L || usedBytes < 0L) return COLOR_UNKNOWN;
        return ratio >= .90f ? COLOR_BAD : ratio >= .75f ? COLOR_WARN : COLOR_GOOD;
    }
}
