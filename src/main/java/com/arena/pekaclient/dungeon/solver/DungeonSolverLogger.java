package com.arena.pekaclient.dungeon.solver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class DungeonSolverLogger {
    private static final Logger LOG = LogManager.getLogger("Lunaria");
    private DungeonSolverLogger() { }
    public static void log(String message) { LOG.warn(message); }
}
