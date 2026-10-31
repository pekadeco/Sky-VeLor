package com.arena.pekaclient.dungeon.solver;

import java.awt.Color;

/** Regression checks for the solver highlight colour format. */
public final class DungeonSolverColorTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("default green keeps opaque alpha and RGB order",
                DungeonSolverColor.getColor("0:255:0:255:0").equals(new Color(0, 255, 0, 255)));
        check("blue tracker color parses correctly",
                DungeonSolverColor.getColor("0:255:0:100:255").equals(new Color(0, 100, 255, 255)));
        check("custom alpha is preserved",
                DungeonSolverColor.getColor("0:150:12:34:56").equals(new Color(12, 34, 56, 150)));
        check("channel values are clamped",
                DungeonSolverColor.getColor("0:256:-1:300:12").equals(new Color(0, 255, 12, 255)));
        check("invalid input falls back to opaque white",
                DungeonSolverColor.getColor("not-a-color").equals(new Color(255, 255, 255, 255)));
        check("null input falls back to opaque white",
                DungeonSolverColor.getColor(null).equals(new Color(255, 255, 255, 255)));

        System.out.println();
        System.out.println("dungeon solver colors: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL " + name);
        }
    }
}
