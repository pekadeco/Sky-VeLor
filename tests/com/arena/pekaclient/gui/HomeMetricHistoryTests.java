package com.arena.pekaclient.gui;

import com.arena.pekaclient.hud.ClientMetricText;
import com.arena.pekaclient.hud.ClientMetrics;

/** Mock-value regression coverage for Home-only metrics, bounded sampling and explicit unknown ping formatting. */
public final class HomeMetricHistoryTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        samplesOnlyWhenTheBoundedReaderRefreshes();
        ringRemainsFixedAtCapacity();
        mockFormattingAndThresholdsStayHonest();
        System.out.println("home metric history: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void samplesOnlyWhenTheBoundedReaderRefreshes() {
        MutableProvider provider = new MutableProvider(144, 42, 128L, 512L);
        ClientMetrics.Reader reader = new ClientMetrics.Reader(provider);
        HomeMetricHistory history = new HomeMetricHistory();
        ClientMetrics.Snapshot first = reader.snapshot(0L);
        check("first GUI Home sample records", history.record(first));
        check("same painted frame does not append", !history.record(first) && history.size() == 1);
        // 461 / 512 is just above the documented 90% critical memory boundary.
        provider.set(31, 151, 461L, 512L);
        ClientMetrics.Snapshot beforeBoundary = reader.snapshot(499L);
        check("reader remains cached before 500 ms", beforeBoundary == first && !history.record(beforeBoundary));
        ClientMetrics.Snapshot refreshed = reader.snapshot(500L);
        check("second sample arrives only at cadence boundary", history.record(refreshed) && history.size() == 2);
        check("new mock values update status", refreshed.fpsStatus() == ClientMetrics.Status.WARNING
                && refreshed.pingStatus() == ClientMetrics.Status.CRITICAL
                && refreshed.memoryStatus() == ClientMetrics.Status.CRITICAL);
    }

    private static void ringRemainsFixedAtCapacity() {
        MutableProvider provider = new MutableProvider(60, 25, 1L, 4L);
        ClientMetrics.Reader reader = new ClientMetrics.Reader(provider);
        HomeMetricHistory history = new HomeMetricHistory();
        for (int i = 0; i < HomeMetricHistory.CAPACITY + 8; i++) {
            provider.set(30 + i, 20 + i, i, 100L);
            history.record(reader.snapshot(i * ClientMetrics.REFRESH_MILLIS));
        }
        check("fixed ring never grows beyond capacity", history.size() == HomeMetricHistory.CAPACITY
                && history.capacity() == HomeMetricHistory.CAPACITY);
        check("oldest retained FPS point reflects wrap", history.normalizedAt(HomeMetricHistory.FPS, 0) >= .5f);
        check("newest retained ping point is normalized", history.normalizedAt(HomeMetricHistory.PING, history.size() - 1) >= 0f);
    }

    private static void mockFormattingAndThresholdsStayHonest() {
        long mb = 1024L * 1024L;
        check("mock large FPS format", "144".equals(ClientMetricText.fpsValue(144)));
        check("mock memory format", "256 MB".equals(ClientMetricText.memoryValue(256L * mb)));
        check("unknown ping stays explicit", "-- ms".equals(ClientMetricText.pingValue(-1)));
        check("normal warning critical FPS thresholds", ClientMetrics.fpsStatus(60) == ClientMetrics.Status.NORMAL
                && ClientMetrics.fpsStatus(30) == ClientMetrics.Status.WARNING
                && ClientMetrics.fpsStatus(29) == ClientMetrics.Status.CRITICAL);
        check("normal warning critical ping thresholds", ClientMetrics.pingStatus(75) == ClientMetrics.Status.NORMAL
                && ClientMetrics.pingStatus(76) == ClientMetrics.Status.WARNING
                && ClientMetrics.pingStatus(151) == ClientMetrics.Status.CRITICAL);
    }

    private static final class MutableProvider implements ClientMetrics.Provider {
        private int fps;
        private int ping;
        private long used;
        private long max;

        MutableProvider(int fpsValue, int pingValue, long usedValue, long maxValue) {
            set(fpsValue, pingValue, usedValue, maxValue);
        }

        void set(int fpsValue, int pingValue, long usedValue, long maxValue) {
            fps = fpsValue;
            ping = pingValue;
            used = usedValue;
            max = maxValue;
        }

        @Override public int fps() { return fps; }
        @Override public int localTabPingMs() { return ping; }
        @Override public long usedMemoryBytes() { return used; }
        @Override public long maxMemoryBytes() { return max; }
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++; else { failed++; System.err.println("FAIL " + name); }
    }
}
