package com.arena.pekaclient.hud;

import com.arena.pekaclient.render.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;

/**
 * Bounded-cadence client metric reader for the settings Home surface only. It samples local FPS, Java heap and the
 * player's tab-list response time at most twice per second when that open GUI asks for a snapshot; it registers no
 * HUD element and performs no in-game overlay drawing.
 */
public final class ClientMetrics {
    public static final long REFRESH_MILLIS = 500L;
    /** Blue is the neutral healthy state; warning/error colours are reserved for semantic status only. */
    public static final int COLOR_GOOD = Theme.ACCENT_BLUE;
    public static final int COLOR_WARN = Theme.STATUS_WARNING;
    public static final int COLOR_BAD = Theme.STATUS_CRITICAL;
    public static final int COLOR_UNKNOWN = Theme.STATUS_UNAVAILABLE;

    /** Fixed labels avoid an allocation while a live card is redrawn between two samples. */
    public enum Status {
        NORMAL("NORMAL"), WARNING("WARNING"), CRITICAL("CRITICAL"), UNKNOWN("UNAVAILABLE");

        private final String label;
        Status(String text) { label = text; }
        public String label() { return label; }
    }

    public interface Provider {
        int fps();
        int localTabPingMs();
        long usedMemoryBytes();
        long maxMemoryBytes();
    }

    /** Reused snapshot; callers read it and never retain it as a historical sample. */
    public static final class Snapshot {
        private int fps = -1;
        private int pingMs = -1;
        private long usedBytes = -1L;
        private long maxBytes = -1L;
        private long generation;
        private String fpsText = ClientMetricText.fps(-1);
        private String pingText = ClientMetricText.ping(-1);
        private String memoryText = ClientMetricText.memory(-1L, -1L);
        private String fpsValue = ClientMetricText.fpsValue(-1);
        private String pingValue = ClientMetricText.pingValue(-1);
        private String memoryValue = ClientMetricText.memoryValue(-1L);
        private String memoryDetail = ClientMetricText.memoryDetail(-1L, -1L);

        private void set(int nextFps, int nextPing, long nextUsed, long nextMax) {
            fps = nextFps;
            pingMs = nextPing;
            usedBytes = nextUsed;
            maxBytes = nextMax;
            generation++;
            // The only String formatting occurs on the 500 ms refresh, never while the Home screen repaints.
            fpsText = ClientMetricText.fps(fps);
            pingText = ClientMetricText.ping(pingMs);
            memoryText = ClientMetricText.memory(usedBytes, maxBytes);
            fpsValue = ClientMetricText.fpsValue(fps);
            pingValue = ClientMetricText.pingValue(pingMs);
            memoryValue = ClientMetricText.memoryValue(usedBytes);
            memoryDetail = ClientMetricText.memoryDetail(usedBytes, maxBytes);
        }

        public int fps() { return fps; }
        public int pingMs() { return pingMs; }
        public long usedBytes() { return usedBytes; }
        public long maxBytes() { return maxBytes; }
        public long generation() { return generation; }
        public String fpsText() { return fpsText; }
        public String pingText() { return pingText; }
        public String memoryText() { return memoryText; }
        public String fpsValue() { return fpsValue; }
        public String pingValue() { return pingValue; }
        public String memoryValue() { return memoryValue; }
        public String memoryDetail() { return memoryDetail; }
        public int fpsColor() { return ClientMetrics.fpsColor(fps); }
        public int pingColor() { return ClientMetrics.pingColor(pingMs); }
        public int memoryColor() { return ClientMetrics.memoryColor(usedBytes, maxBytes); }
        public float memoryRatio() { return ClientMetrics.memoryRatio(usedBytes, maxBytes); }
        public Status fpsStatus() { return ClientMetrics.fpsStatus(fps); }
        public Status pingStatus() { return ClientMetrics.pingStatus(pingMs); }
        public Status memoryStatus() { return ClientMetrics.memoryStatus(usedBytes, maxBytes); }
    }

    /** Reader caches data for roughly twice-per-second GUI refreshes with no per-frame snapshot allocation. */
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

    public static Status fpsStatus(int fps) {
        if (fps < 0) return Status.UNKNOWN;
        return fps < 30 ? Status.CRITICAL : fps < 60 ? Status.WARNING : Status.NORMAL;
    }

    public static Status pingStatus(int pingMs) {
        if (pingMs < 0) return Status.UNKNOWN;
        return pingMs > 150 ? Status.CRITICAL : pingMs > 75 ? Status.WARNING : Status.NORMAL;
    }

    public static Status memoryStatus(long usedBytes, long maxBytes) {
        if (maxBytes <= 0L || usedBytes < 0L) return Status.UNKNOWN;
        float ratio = memoryRatio(usedBytes, maxBytes);
        return ratio >= .90f ? Status.CRITICAL : ratio >= .75f ? Status.WARNING : Status.NORMAL;
    }

    public static int fpsColor(int fps) {
        return colorFor(fpsStatus(fps));
    }

    public static int pingColor(int pingMs) {
        return colorFor(pingStatus(pingMs));
    }

    public static float memoryRatio(long usedBytes, long maxBytes) {
        if (usedBytes < 0L || maxBytes <= 0L) return 0f;
        return Math.max(0f, Math.min(1f, usedBytes / (float) maxBytes));
    }

    public static int memoryColor(long usedBytes, long maxBytes) {
        return colorFor(memoryStatus(usedBytes, maxBytes));
    }

    public static int colorFor(Status status) {
        if (status == Status.NORMAL) return COLOR_GOOD;
        if (status == Status.WARNING) return COLOR_WARN;
        if (status == Status.CRITICAL) return COLOR_BAD;
        return COLOR_UNKNOWN;
    }
}
