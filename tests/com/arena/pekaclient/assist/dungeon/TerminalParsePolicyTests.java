package com.arena.pekaclient.assist.dungeon;

import java.util.Arrays;
import java.util.List;

/** Headless checks that terminal parsing stays strict before gameplay GUI code can render or click anything. */
public final class TerminalParsePolicyTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        startsWith();
        colors();
        panesAndOrder();
        mazeAndTitles();
        System.out.println("terminal parse policy: passed " + passed + ", failed " + failed);
        if (failed != 0) throw new AssertionError(failed + " terminal parsing checks failed");
    }

    private static void startsWith() {
        TerminalParsePolicy.Solution solution = TerminalParsePolicy.solve("starts-with", "What starts with: A?",
                Arrays.asList(cell(10, 1, 1, false, -1, "\u00a7fApple"),
                        cell(11, 1, 2, false, -1, "\u00a7fBanana")));
        check("starts-with finds only matching word", solution.correct.length == 1 && solution.correct[0] == 10
                && solution.isWrong(11));
    }

    private static void colors() {
        TerminalParsePolicy.Solution solution = TerminalParsePolicy.solve("select-colors", "Select all the RED items!",
                Arrays.asList(cell(10, 1, 1, false, -1, "\u00a7cRuby"),
                        cell(11, 1, 2, false, -1, "\u00a79Sapphire")));
        check("color parser reads Minecraft colour code", solution.correct.length == 1 && solution.correct[0] == 10
                && solution.isWrong(11));
    }

    private static void panesAndOrder() {
        TerminalParsePolicy.Solution panes = TerminalParsePolicy.solve("correct-panes", "Correct all the panes!",
                Arrays.asList(cell(10, 1, 1, true, 14, ""), cell(11, 1, 2, true, 5, "")));
        check("pane parser only marks supported pane metadata", panes.correct.length == 1 && panes.correct[0] == 10);
        TerminalParsePolicy.Solution order = TerminalParsePolicy.solve("click-in-order", "Click in order!",
                Arrays.asList(cell(10, 1, 1, true, 14, "\u00a7c3"), cell(11, 1, 2, true, 14, "\u00a7c1")));
        check("click order chooses lowest explicit number", order.correct.length == 1 && order.correct[0] == 11);
        TerminalParsePolicy.Solution duplicate = TerminalParsePolicy.solve("click-in-order", "Click in order!",
                Arrays.asList(cell(10, 1, 1, true, 14, "1"), cell(11, 1, 2, true, 14, "1")));
        check("duplicate click order is rejected", !duplicate.hasChoices());
    }

    private static void mazeAndTitles() {
        TerminalParsePolicy.Solution maze = TerminalParsePolicy.solve("maze", "Complete the maze!",
                Arrays.asList(cell(10, 1, 1, true, 5, ""), cell(11, 1, 2, true, 0, ""),
                        cell(12, 1, 3, true, 0, "")));
        check("maze marks adjacent white pane", maze.correct.length == 1 && maze.correct[0] == 11);
        check("complete maze title is recognized", "maze".equals(TerminalAssistPolicy.kindForTitle("Complete the maze!")));
    }

    private static TerminalParsePolicy.Cell cell(int slot, int row, int column, boolean pane, int metadata, String name) {
        return new TerminalParsePolicy.Cell(slot, row, column, true, pane, metadata, name);
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++; else { failed++; System.err.println("FAIL " + name); }
    }
}
