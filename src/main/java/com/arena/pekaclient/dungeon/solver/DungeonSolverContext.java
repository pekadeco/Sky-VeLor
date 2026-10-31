package com.arena.pekaclient.dungeon.solver;

import com.arena.pekaclient.feature.DungeonContext;
import com.arena.pekaclient.config.AssistSettings;

/** Shared context gates for the Better NEF dungeon features adapted to Lunaria. */
public final class DungeonSolverContext {
    private DungeonSolverContext() { }

    public static boolean checkEssentials() {
        return DungeonContext.inDungeon();
    }

    public static boolean checkEssentialsF7() {
        return DungeonContext.inF7OrM7();
    }

    /**
     * Legacy terminal solvers must stand down when the current Safe Terminal Assist is enabled.
     * That controller is the single owner of terminal highlighting and wrong-click interception.
     */
    public static boolean checkTerminalEssentialsF7() {
        return !AssistSettings.terminalHints && checkEssentialsF7();
    }
}
