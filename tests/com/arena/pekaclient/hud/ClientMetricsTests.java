package com.arena.pekaclient.hud;

/** Headless coverage for the General-tab client metric reader, colour coding, and 500 ms refresh boundary. */
public final class ClientMetricsTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        readerRefreshesAtTwicePerSecond();
        memoryAndNetworkPresentationUsesThresholds();
        done();
    }

    private static void readerRefreshesAtTwicePerSecond() {
        MutableProvider provider = new MutableProvider(144, 42, 128L, 512L);
        ClientMetrics.Reader reader = new ClientMetrics.Reader(provider);
        ClientMetrics.Snapshot initial = reader.snapshot(0L);
        check("initial FPS read", initial.fps() == 144);
        check("initial local tab ping read", initial.pingMs() == 42);
        check("initial used/max heap read", initial.usedBytes() == 128L && initial.maxBytes() == 512L);
        provider.set(37, 180, 460L, 512L);
        ClientMetrics.Snapshot beforeBoundary = reader.snapshot(499L);
        check("cached before 500 ms boundary", beforeBoundary.fps() == 144 && beforeBoundary.pingMs() == 42);
        ClientMetrics.Snapshot refreshed = reader.snapshot(500L);
        check("same snapshot object reused", refreshed == initial);
        check("refresh at 500 ms boundary", refreshed.fps() == 37 && refreshed.pingMs() == 180
                && refreshed.usedBytes() == 460L && refreshed.maxBytes() == 512L);
        check("labels refresh with values", "FPS 37".equals(refreshed.fpsText())
                && "Ping 180 ms".equals(refreshed.pingText()) && "Memory 0 / 0 MB".equals(refreshed.memoryText()));
    }

    private static void memoryAndNetworkPresentationUsesThresholds() {
        check("high FPS green", ClientMetrics.fpsColor(60) == ClientMetrics.COLOR_GOOD);
        check("low FPS red", ClientMetrics.fpsColor(29) == ClientMetrics.COLOR_BAD);
        check("high local ping red", ClientMetrics.pingColor(151) == ClientMetrics.COLOR_BAD);
        check("unknown ping neutral", ClientMetrics.pingColor(-1) == ClientMetrics.COLOR_UNKNOWN);
        check("memory ratio", Math.abs(ClientMetrics.memoryRatio(3L, 4L) - .75f) < .0001f);
        check("75 percent memory warns", ClientMetrics.memoryColor(3L, 4L) == ClientMetrics.COLOR_WARN);
        check("90 percent memory red", ClientMetrics.memoryColor(9L, 10L) == ClientMetrics.COLOR_BAD);
    }

    private static final class MutableProvider implements ClientMetrics.Provider {
        private int fps;
        private int ping;
        private long used;
        private long max;

        MutableProvider(int fps, int ping, long used, long max) { set(fps, ping, used, max); }
        void set(int nextFps, int nextPing, long nextUsed, long nextMax) {
            fps = nextFps; ping = nextPing; used = nextUsed; max = nextMax;
        }
        @Override public int fps() { return fps; }
        @Override public int localTabPingMs() { return ping; }
        @Override public long usedMemoryBytes() { return used; }
        @Override public long maxMemoryBytes() { return max; }
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++; else { failed++; System.err.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("client metrics: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
