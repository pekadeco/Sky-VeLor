package com.arena.pekaclient.dungeon;

import java.util.Locale;

/**
 * Display text and accent colour for each puzzle room drawn on the dungeon map. The names in rooms.json are
 * lookup keys (some have typos), so the map shows a cleaned-up name and a short form for narrow tiles.
 */
final class PuzzleLabels {

    static final int DEFAULT_COLOR = 0xFFFFE59A;

    /** What to draw for one puzzle. */
    static final class Style {
        final String full;
        final String shortName;
        final int color;

        Style(String full, String shortName, int color) {
            this.full = full;
            this.shortName = shortName;
            this.color = color;
        }
    }

    private PuzzleLabels() {
    }

    static Style of(String rawName) {
        String key = rawName == null ? "" : rawName.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        switch (key) {
            case "silver fish":
            case "silverfish":
                return new Style("Silverfish", "Silver", 0xFFB7C9DA);
            case "warping":
            case "teleport maze":
                return new Style("Teleport Maze", "TP Maze", 0xFFC08BFF);
            case "3 wierdos":
            case "3 weirdos":
            case "three weirdos":
                return new Style("Three Weirdos", "3 Weirdos", 0xFF86E58A);
            case "creaper beams":
            case "creeper beams":
                return new Style("Creeper Beams", "Beams", 0xFF55E6D6);
            case "box":
            case "boulder":
                return new Style("Boulder", "Boulder", 0xFFFFB05A);
            case "water":
            case "water board":
                return new Style("Water Board", "Water", 0xFF5BAEFF);
            case "tic tac toe":
                return new Style("Tic Tac Toe", "TTT", 0xFFFF86B4);
            default:
                String t = titleCase(key);
                return new Style(t, t.length() > 8 ? t.substring(0, 8) : t, DEFAULT_COLOR);
        }
    }

    /** Splits multi-word map labels into two balanced lines without breaking words. */
    static String[] wrap(String value) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) return new String[] { "Puzzle" };
        String[] words = normalized.split(" ");
        if (words.length < 2) return new String[] { normalized };

        int bestSplit = 1;
        int bestDifference = Integer.MAX_VALUE;
        for (int split = 1; split < words.length; split++) {
            int firstLength = joinedLength(words, 0, split);
            int secondLength = joinedLength(words, split, words.length);
            int difference = Math.abs(firstLength - secondLength);
            // Prefer the later split on ties so three-word names keep the first two words together.
            if (difference <= bestDifference) {
                bestDifference = difference;
                bestSplit = split;
            }
        }
        return new String[] { join(words, 0, bestSplit), join(words, bestSplit, words.length) };
    }

    private static int joinedLength(String[] words, int start, int end) {
        int length = Math.max(0, end - start - 1);
        for (int i = start; i < end; i++) length += words[i].length();
        return length;
    }

    private static String join(String[] words, int start, int end) {
        StringBuilder value = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (value.length() > 0) value.append(' ');
            value.append(words[i]);
        }
        return value.toString();
    }

    private static String titleCase(String value) {
        StringBuilder out = new StringBuilder();
        for (String word : value.split(" ")) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) out.append(word.substring(1));
        }
        return out.toString();
    }
}
