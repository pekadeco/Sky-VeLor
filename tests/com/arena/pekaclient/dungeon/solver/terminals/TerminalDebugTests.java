package com.arena.pekaclient.dungeon.solver.terminals;

/** Regression tests for title classification and the bounded debug buffer. */
public final class TerminalDebugTests {
    private TerminalDebugTests() { }

    public static void main(String[] args) {
        testDisabledByDefault();
        testTitleClassification();
        testBufferBound();
        System.out.println("TerminalDebugTests: 9 assertions passed.");
    }

    private static void testDisabledByDefault() {
        if (TerminalDebug.isEnabled()) throw new AssertionError("Debug capture must be disabled by default");
        int before = TerminalDebug.getEntryCount();
        TerminalDebug.record("TEST", "this entry must not be captured");
        if (TerminalDebug.getEntryCount() != before) {
            throw new AssertionError("Recording while disabled must not add entries");
        }
    }

    private static void testTitleClassification() {
        expect("Click in Order", TerminalDebug.classifyTitle("Click in Order (1/7)"));
        expect("Select Colors", TerminalDebug.classifyTitle("Select all the RED items"));
        expect("Correct Panes", TerminalDebug.classifyTitle("Correct all the panes!"));
        expect("Starts With", TerminalDebug.classifyTitle("What starts with: A?"));
        expect("Maze", TerminalDebug.classifyTitle("Complete the maze!"));
        expect("Unclassified chest", TerminalDebug.classifyTitle("Random Chest"));
    }

    private static void testBufferBound() {
        TerminalDebug.clear();
        TerminalDebug.setEnabled(true);
        for (int i = 0; i < 550; i++) TerminalDebug.record("TEST", "entry-" + i);
        if (TerminalDebug.getEntryCount() != 500) {
            throw new AssertionError("Debug buffer should remain capped at 500 entries, got "
                    + TerminalDebug.getEntryCount());
        }
        TerminalDebug.setEnabled(false);
        TerminalDebug.clear();
    }

    private static void expect(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected '" + expected + "', got '" + actual + "'");
        }
    }
}
