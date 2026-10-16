package com.arena.pekaclient.startup;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

public final class StartupTasks {
    private static final class Task {
        final String label;
        final Runnable work;
        long nanos = -1L;

        Task(String label, Runnable work) {
            this.label = label;
            this.work = work;
        }
    }

    private static final List<Task> TASKS = new ArrayList<Task>();
    private static int completed;
    private static boolean failed;
    private static String failureMessage = "";
    private static long startedNs;
    private static long wallNanos = -1L;
    private static long uptimeAtDoneMs = -1L;

    private StartupTasks() {
    }

    public static synchronized void add(String label, Runnable work) {
        if (work == null || failed) {
            return;
        }
        if (startedNs == 0L) {
            startedNs = System.nanoTime();
        }
        if (completed == TASKS.size()) {
            uptimeAtDoneMs = -1L;
            wallNanos = -1L;
        }
        TASKS.add(new Task(label == null || label.trim().isEmpty() ? "Startup task" : label, work));
    }

    public static synchronized boolean hasWork() {
        return !failed && completed < TASKS.size();
    }

    public static synchronized int total() {
        return TASKS.size();
    }

    public static synchronized int completed() {
        return completed;
    }

    public static synchronized String currentLabel() {
        return hasWork() ? TASKS.get(completed).label : "";
    }

    public static synchronized boolean failed() {
        return failed;
    }

    public static synchronized String failureMessage() {
        return failureMessage;
    }

    public static synchronized long taskNanos(int index) {
        if (index < 0 || index >= TASKS.size()) {
            return -1L;
        }
        return TASKS.get(index).nanos;
    }

    public static synchronized long totalTaskNanos() {
        long total = 0L;
        for (Task task : TASKS) {
            if (task.nanos > 0L) {
                total += task.nanos;
            }
        }
        return total;
    }

    public static synchronized long wallNanos() {
        return wallNanos;
    }

    public static synchronized long jvmUptimeAtDoneMs() {
        return uptimeAtDoneMs;
    }

    public static boolean runNext() {
        final Task task;
        final int index;
        synchronized (StartupTasks.class) {
            if (!hasWork()) {
                return false;
            }
            index = completed;
            task = TASKS.get(index);
        }

        long before = System.nanoTime();
        try {
            task.work.run();
        } catch (Throwable error) {
            if (error instanceof ThreadDeath) {
                throw (ThreadDeath) error;
            }
            if (error instanceof VirtualMachineError) {
                throw (VirtualMachineError) error;
            }
            synchronized (StartupTasks.class) {
                failed = true;
                String detail = error.getMessage();
                String type = error.getClass().getSimpleName();
                failureMessage = "Startup failed: " + (type == null || type.isEmpty() ? "error" : type)
                        + (detail == null || detail.trim().isEmpty() ? "" : " — " + detail.trim());
                if (failureMessage.length() > 150) {
                    failureMessage = failureMessage.substring(0, 147) + "...";
                }
            }
            return true;
        }

        long elapsed = Math.max(0L, System.nanoTime() - before);
        synchronized (StartupTasks.class) {
            task.nanos = elapsed;
            if (completed == index) {
                completed++;
            }
            if (completed >= TASKS.size()) {
                long now = System.nanoTime();
                wallNanos = startedNs == 0L ? 0L : Math.max(0L, now - startedNs);
                uptimeAtDoneMs = ManagementFactory.getRuntimeMXBean().getUptime();
            }
        }
        return true;
    }
}
