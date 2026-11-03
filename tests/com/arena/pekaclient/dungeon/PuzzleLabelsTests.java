package com.arena.pekaclient.dungeon;

public final class PuzzleLabelsTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        checkLines("Water Board", "Water", "Board");
        checkLines("Teleport Maze", "Teleport", "Maze");
        checkLines("Creeper Beams", "Creeper", "Beams");
        checkLines("Three Weirdos", "Three", "Weirdos");
        checkLines("Tic Tac Toe", "Tic Tac", "Toe");
        checkLines("Silverfish", "Silverfish");
        checkLines("Boulder", "Boulder");
        check("water identifier has the correct display name", "Water Board".equals(PuzzleLabels.of("water").full));
        check("known puzzle names have distinct colors", PuzzleLabels.of("water").color != PuzzleLabels.of("boulder").color);
        System.out.println("PuzzleLabelsTests: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void checkLines(String source, String... expected) {
        String[] actual = PuzzleLabels.wrap(source);
        boolean matches = actual.length == expected.length;
        if (matches) {
            for (int i = 0; i < expected.length; i++) {
                if (!expected[i].equals(actual[i])) matches = false;
            }
        }
        check("wraps '" + source + "'", matches);
    }

    private static void check(String name, boolean condition) {
        if (condition) passed++;
        else { failed++; System.out.println("FAIL: " + name); }
    }
}
