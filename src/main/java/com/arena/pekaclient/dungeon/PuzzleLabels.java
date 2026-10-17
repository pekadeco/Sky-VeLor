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
