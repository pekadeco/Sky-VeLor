package com.arena.pekaclient.startup;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * The real startup work for Sky VeLor, run one task at a time.
 *
 * The loading screen asks this class for the next task on every game tick and shows the real count of
 * finished tasks. The progress is never guessed. If a task throws, the error is logged and shown on the
 * loading screen, and the screen stays up until the player continues. A failure is never shown as a
 * completed startup.
 */
public final class StartupTasks {

    private static final Logger LOG = LogManager.getLogger("SkyVeLor");

    /** One named piece of startup work. */
    private static final class Task {
        final String label;
        final Runnable work;

        Task(String label, Runnable work) {
            this.label = label;
            this.work = work;
        }
    }

    private static final List<Task> TASKS = new ArrayList<Task>();
    private static int next;
    private static String failure;

    /** Measured work time of each finished task, in nanoseconds, in the order the tasks ran. */
    private static final List<Long> TASK_NANOS = new ArrayList<Long>();
    /** Wall time from the first task start to the last task end, or 0 before the first task ends. */
    private static long firstStartNs;
    private static long lastEndNs;
    /** JVM uptime in ms when the last task finished, or -1 before that. Includes Mojang and Forge startup. */
    private static long jvmUptimeAtDoneMs = -1L;

    private StartupTasks() {
    }

    /** Queues a task. Tasks run in the order they were added. */
    public static synchronized void add(String label, Runnable work) {
        TASKS.add(new Task(label, work));
    }

    /** True while a task is still waiting to run, and nothing has failed. */
    public static synchronized boolean hasWork() {
        return failure == null && next < TASKS.size();
    }

    /** True once a task has thrown. The loading screen keeps showing this until the player continues. */
    public static synchronized boolean failed() {
        return failure != null;
    }

    /** The failure message, or null. */
    public static synchronized String failureMessage() {
        return failure;
    }

    /** Number of tasks that have finished. */
    public static synchronized int completed() {
        return next;
    }

    public static synchronized int total() {
        return TASKS.size();
    }

    /** Label of the task that is running or about to run, or an empty string when there is none. */
    public static synchronized String currentLabel() {
        return next < TASKS.size() ? TASKS.get(next).label : "";
    }

    /**
     * Runs the next task. Returns true when a task ran (successfully or not).
     * A task that throws is logged, and the failure is kept. The task is not counted as finished.
     */
    public static boolean runNext() {
        Task t;
        synchronized (StartupTasks.class) {
            if (!hasWork()) {
                return false;
            }
            t = TASKS.get(next);
            if (next == 0) {
                firstStartNs = System.nanoTime();
            }
        }
        long t0 = System.nanoTime();
        try {
            t.work.run();
        } catch (RuntimeException e) {
            synchronized (StartupTasks.class) {
                failure = t.label + " failed: " + e;
            }
            LOG.error("Sky VeLor startup task failed: " + t.label, e);
            return true;
        }
        long t1 = System.nanoTime();
        boolean last;
        synchronized (StartupTasks.class) {
            TASK_NANOS.add(t1 - t0);
            next++;
            lastEndNs = t1;
            last = next == TASKS.size();
            if (last) {
                jvmUptimeAtDoneMs = uptimeMs();
            }
        }
        LOG.info(String.format(java.util.Locale.ROOT, "Sky VeLor startup task \"%s\" took %.1f ms",
                t.label, (t1 - t0) / 1e6));
        if (last) {
            LOG.info(String.format(java.util.Locale.ROOT,
                    "Sky VeLor startup done: %d tasks, %.1f ms of task work, %.1f ms wall, JVM uptime %d ms",
                    TASK_NANOS.size(), totalTaskNanos() / 1e6, wallNanos() / 1e6, jvmUptimeAtDoneMs));
        }
        return true;
    }

    private static long uptimeMs() {
        try {
            return ManagementFactory.getRuntimeMXBean().getUptime();
        } catch (RuntimeException e) {
            return -1L;
        }
    }

    /** Sum of the measured work time of the finished tasks, in nanoseconds. */
    public static synchronized long totalTaskNanos() {
        long sum = 0L;
        for (Long n : TASK_NANOS) {
            sum += n;
        }
        return sum;
    }

    /** Wall time from the first task start to the last task end, in nanoseconds. 0 when nothing finished. */
    public static synchronized long wallNanos() {
        return lastEndNs == 0L || firstStartNs == 0L ? 0L : lastEndNs - firstStartNs;
    }

    /** Measured work time of finished task {@code i} in nanoseconds, or -1 if that task has not finished. */
    public static synchronized long taskNanos(int i) {
        return i >= 0 && i < TASK_NANOS.size() ? TASK_NANOS.get(i) : -1L;
    }

    /** JVM uptime in ms when the last task finished, or -1 before that. */
    public static synchronized long jvmUptimeAtDoneMs() {
        return jvmUptimeAtDoneMs;
    }
}
