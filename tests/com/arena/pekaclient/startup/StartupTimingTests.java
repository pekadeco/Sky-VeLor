package com.arena.pekaclient.startup;

/**
 * Checks the startup timing: a finished task has a measured duration, a failed task has none and keeps the failure,
 * and the uptime is recorded only once every task has finished.
 */
public final class StartupTimingTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        measuresFinishedTasks();
        failureIsNotTimedAsFinished();

        System.out.println();
        System.out.println("startup timing: passed " + passed + ", failed " + failed);
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

    private static void measuresFinishedTasks() throws Exception {
        StartupTasks.add("sleeps", new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(20L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        check("uptime not set before done", StartupTasks.jvmUptimeAtDoneMs() == -1L, "");
        check("task runs", StartupTasks.runNext(), "");
        check("one finished", StartupTasks.completed() == 1, "completed=" + StartupTasks.completed());
        check("measured at least 15 ms", StartupTasks.taskNanos(0) >= 15000000L, "ns=" + StartupTasks.taskNanos(0));
        check("total matches", StartupTasks.totalTaskNanos() == StartupTasks.taskNanos(0), "");
        check("wall time recorded", StartupTasks.wallNanos() >= StartupTasks.taskNanos(0), "");
        check("uptime recorded at done", StartupTasks.jvmUptimeAtDoneMs() >= 0L, "");
    }

    private static void failureIsNotTimedAsFinished() {
        StartupTasks.add("breaks", new Runnable() {
            @Override
            public void run() {
                throw new IllegalStateException("test failure");
            }
        });
        check("failing task runs", StartupTasks.runNext(), "");
        check("failure kept", StartupTasks.failed(), "");
        check("failed task not counted", StartupTasks.completed() == 1, "completed=" + StartupTasks.completed());
        check("failed task has no time", StartupTasks.taskNanos(1) == -1L, "");
        check("no more work after failure", !StartupTasks.hasWork(), "");
    }
}
