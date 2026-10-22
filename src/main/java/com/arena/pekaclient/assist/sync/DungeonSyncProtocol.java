package com.arena.pekaclient.assist.sync;

import java.util.Locale;

/**
 * Strict, bounded protocol helpers for the opt-in puzzle worker. The public worker accepts only these canonical
 * dungeon puzzle identifiers and room-aligned coordinates; arbitrary chat text or player data cannot enter a
 * payload through this class.
 */
public final class DungeonSyncProtocol {
    private DungeonSyncProtocol() {
    }

    public static String encodePuzzle(String rawName, int x, int y, int z, double confidence) {
        String name = canonicalPuzzleName(rawName);
        if (name.length() == 0 || confidence < .65D || confidence > 1D || !finite(confidence)
                || Math.abs((long) x) > 30000000L || Math.abs((long) z) > 30000000L || y < 1 || y > 250
                || Math.floorMod(x, 32) != 0 || Math.floorMod(z, 32) != 0) return "";
        return "{\"type\":\"puzzle_discovered\",\"name\":\"" + name + "\",\"x\":" + x
                + ",\"y\":" + y + ",\"z\":" + z + ",\"confidence\":"
                + String.format(Locale.ROOT, "%.2f", confidence) + "}";
    }

    public static boolean validPuzzleName(String name) {
        return canonicalPuzzleName(name).length() > 0;
    }

    /** Converts local map labels to the worker's small fixed allow-list. */
    public static String canonicalPuzzleName(String rawName) {
        if (rawName == null || rawName.length() > 64) return "";
        String value = normalize(rawName);
        if ("silverfish".equals(value)) return "silver fish";
        if ("teleport maze".equals(value)) return "warping";
        if ("three weirdos".equals(value) || "3 weirdos".equals(value)) return "3 wierdos";
        if ("creeper beams".equals(value)) return "creaper beams";
        if ("box".equals(value)) return "boulder";
        if ("water board".equals(value)) return "water";
        return "silver fish".equals(value) || "warping".equals(value) || "3 wierdos".equals(value)
                || "creaper beams".equals(value) || "boulder".equals(value) || "water".equals(value)
                || "tic tac toe".equals(value) ? value : "";
    }

    public static String normalize(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder(Math.min(64, value.length()));
        boolean space = true;
        for (int index = 0; index < value.length() && out.length() < 64; index++) {
            char c = value.charAt(index);
            if (Character.isWhitespace(c)) {
                if (!space) { out.append(' '); space = true; }
            } else if (Character.isLetterOrDigit(c) || c == '-' || c == '_') {
                out.append(Character.toLowerCase(c));
                space = false;
            } else {
                return "";
            }
        }
        if (out.length() > 0 && out.charAt(out.length() - 1) == ' ') out.setLength(out.length() - 1);
        return out.toString();
    }

    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}
