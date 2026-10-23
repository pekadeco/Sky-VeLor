package com.arena.pekaclient.hud;

/** Pure checks for the three GUI-only General-category metric labels. */
public final class StatsTextTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        formatsMetrics();
        unknownValuesStayExplicit();
        memoryUsesUsedAndMaximumMb();

        System.out.println();
        System.out.println("client metric text: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void check(String name, boolean ok, String detail) {
        if (ok) passed++;
        else {
            failed++;
            System.out.println("FAIL " + name + " " + detail);
        }
    }

    private static void formatsMetrics() {
        check("fps", "FPS 144".equals(ClientMetricText.fps(144)), ClientMetricText.fps(144));
        check("ping", "Ping 42 ms".equals(ClientMetricText.ping(42)), ClientMetricText.ping(42));
        check("zero ping is real", "Ping 0 ms".equals(ClientMetricText.ping(0)), ClientMetricText.ping(0));
    }

    private static void unknownValuesStayExplicit() {
        check("unknown fps", "FPS --".equals(ClientMetricText.fps(-1)), ClientMetricText.fps(-1));
        check("unknown ping", "Ping -- ms".equals(ClientMetricText.ping(-1)), ClientMetricText.ping(-1));
        check("unknown memory", "Memory -- / -- MB".equals(ClientMetricText.memory(-1L, -1L)),
                ClientMetricText.memory(-1L, -1L));
    }

    private static void memoryUsesUsedAndMaximumMb() {
        long mb = 1024L * 1024L;
        String label = ClientMetricText.memory(123L * mb + 999L, 768L * mb + 17L);
        check("used/max", "Memory 123 / 768 MB".equals(label), label);
    }
}
