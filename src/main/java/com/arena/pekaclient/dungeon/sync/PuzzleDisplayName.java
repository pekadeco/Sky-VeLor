package com.arena.pekaclient.dungeon.sync;

import java.util.Locale;

/** Formats allow-listed puzzle identifiers for local chat without displaying protocol internals. */
public final class PuzzleDisplayName {
    private PuzzleDisplayName() { }

    public static String of(String name) {
        if (name == null) return "Puzzle";
        String normalized = name.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        if ("silver fish".equals(normalized)) return "Silverfish";
        if ("3 wierdos".equals(normalized)) return "Three Weirdos";
        if ("creaper beams".equals(normalized)) return "Creeper Beams";
        if ("tic tac toe".equals(normalized)) return "Tic Tac Toe";
        if ("warping".equals(normalized)) return "Teleport Maze";
        if ("boulder".equals(normalized)) return "Boulder";
        if ("water".equals(normalized)) return "Water Board";
        return "Puzzle";
    }
}
