package com.arena.pekaclient.hud;

/** Checks the FPS and ping text: unknown values show "--", and the parts are joined only when both are shown. */
public final class StatsTextTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        formatsValues();
        unknownIsNeverZero();
        joinsOnlyChosenParts();

        System.out.println();
        System.out.println("stats text: passed " + passed + ", failed " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void check(String name, boolean ok, String detail) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL " + name + " " + detail);
        }
    }

    private static void formatsValues() {
        check("fps", "144 FPS".equals(StatsText.fps(144)), StatsText.fps(144));
        check("ping", "42 ms".equals(StatsText.ping(42)), StatsText.ping(42));
        check("zero ping is real", "0 ms".equals(StatsText.ping(0)), StatsText.ping(0));
    }

    private static void unknownIsNeverZero() {
        check("unknown fps", "-- FPS".equals(StatsText.fps(-1)), StatsText.fps(-1));
        check("unknown ping", "-- ms".equals(StatsText.ping(-1)), StatsText.ping(-1));
    }

    private static void joinsOnlyChosenParts() {
        check("both", ("144 FPS" + StatsText.SEPARATOR + "42 ms").equals(StatsText.line(true, 144, true, 42)),
                StatsText.line(true, 144, true, 42));
        check("fps only", "144 FPS".equals(StatsText.line(true, 144, false, 42)), StatsText.line(true, 144, false, 42));
        check("ping only", "42 ms".equals(StatsText.line(false, 144, true, 42)), StatsText.line(false, 144, true, 42));
        check("none", "".equals(StatsText.line(false, 144, false, 42)), StatsText.line(false, 144, false, 42));
        check("ping unknown", ("144 FPS" + StatsText.SEPARATOR + "-- ms").equals(
                StatsText.line(true, 144, true, -1)), StatsText.line(true, 144, true, -1));
    }
}
