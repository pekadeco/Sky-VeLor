package com.arena.pekaclient.assist.dungeon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Conservative, Minecraft-free terminal recognition. It emits no candidate unless a title and live grid have an
 * unambiguous supported shape; callers must leave the chest entirely vanilla for an empty solution.
 */
public final class TerminalParsePolicy {

    private TerminalParsePolicy() {
    }

    public static Solution solve(String kind, String title, List<Cell> cells) {
        if (kind == null || cells == null || cells.isEmpty()) return Solution.EMPTY;
        if ("click-in-order".equals(kind)) return clickInOrder(cells);
        if ("starts-with".equals(kind)) return startsWith(title, cells);
        if ("select-colors".equals(kind)) return selectColors(title, cells);
        if ("correct-panes".equals(kind)) return correctPanes(cells);
        if ("maze".equals(kind)) return maze(cells);
        return Solution.EMPTY;
    }

    private static Solution clickInOrder(List<Cell> cells) {
        Cell next = null;
        int nextNumber = Integer.MAX_VALUE;
        ArrayList<Integer> seen = new ArrayList<Integer>();
        for (Cell cell : cells) {
            if (!cell.grid || !cell.pane || cell.metadata == 5) continue;
            int number = standaloneNumber(cell.name);
            if (number < 1 || number > 28) continue;
            Integer boxed = Integer.valueOf(number);
            if (seen.contains(boxed)) return Solution.EMPTY;
            seen.add(boxed);
            if (number < nextNumber) {
                nextNumber = number;
                next = cell;
            }
        }
        return next == null ? Solution.EMPTY : solution(single(next.slotId), wrong(cells, next.slotId));
    }

    private static Solution startsWith(String title, List<Cell> cells) {
        char initial = requestedInitial(title);
        if (initial == 0) return Solution.EMPTY;
        ArrayList<Integer> correct = new ArrayList<Integer>();
        for (Cell cell : cells) {
            if (!cell.grid || cell.name == null) continue;
            String plain = plain(cell.name).trim();
            if (plain.length() > 0 && Character.toLowerCase(plain.charAt(0)) == initial) correct.add(cell.slotId);
        }
        return correct.isEmpty() ? Solution.EMPTY : solution(correct, wrong(cells, correct));
    }

    private static Solution selectColors(String title, List<Cell> cells) {
        String requested = requestedColor(title);
        if (requested == null) return Solution.EMPTY;
        ArrayList<Integer> correct = new ArrayList<Integer>();
        for (Cell cell : cells) {
            if (!cell.grid || cell.name == null || !matchesColor(requested, cell.name)) continue;
            correct.add(cell.slotId);
        }
        return correct.isEmpty() ? Solution.EMPTY : solution(correct, wrong(cells, correct));
    }

    private static Solution correctPanes(List<Cell> cells) {
        ArrayList<Integer> correct = new ArrayList<Integer>();
        for (Cell cell : cells) {
            // The live terminal's actionable red/white panes are 14/0. The grid restriction excludes decoration.
            if (cell.grid && cell.pane && (cell.metadata == 14 || cell.metadata == 0)) correct.add(cell.slotId);
        }
        return correct.isEmpty() ? Solution.EMPTY : solution(correct, wrong(cells, correct));
    }

    private static Solution maze(List<Cell> cells) {
        ArrayList<Integer> correct = new ArrayList<Integer>();
        ArrayList<Integer> alternatives = new ArrayList<Integer>();
        for (Cell source : cells) {
            if (!source.grid || !source.pane || source.metadata != 5) continue;
            for (Cell candidate : cells) {
                if (!candidate.grid || !candidate.pane || candidate.metadata != 0 || !adjacent(source, candidate)) continue;
                if (!correct.contains(candidate.slotId)) correct.add(candidate.slotId);
            }
        }
        if (correct.isEmpty()) return Solution.EMPTY;
        for (Integer primary : correct) {
            Cell source = find(cells, primary.intValue());
            if (source == null) continue;
            for (Cell candidate : cells) {
                if (!candidate.grid || !candidate.pane || candidate.metadata != 0 || !adjacent(source, candidate)
                        || correct.contains(candidate.slotId) || alternatives.contains(candidate.slotId)) continue;
                alternatives.add(candidate.slotId);
            }
        }
        return new Solution(toArray(correct), wrong(cells, correct), toArray(alternatives));
    }

    private static boolean adjacent(Cell first, Cell second) {
        return (Math.abs(first.row - second.row) == 1 && first.column == second.column)
                || (Math.abs(first.column - second.column) == 1 && first.row == second.row);
    }

    private static Cell find(List<Cell> cells, int slotId) {
        for (Cell cell : cells) if (cell.slotId == slotId) return cell;
        return null;
    }

    private static Solution solution(List<Integer> correct, int[] wrong) {
        return new Solution(toArray(correct), wrong, new int[0]);
    }

    private static Solution solution(int[] correct, int[] wrong) {
        return new Solution(correct, wrong, new int[0]);
    }

    private static List<Integer> single(int value) {
        return Collections.singletonList(Integer.valueOf(value));
    }

    private static int[] wrong(List<Cell> cells, int correct) {
        ArrayList<Integer> accepted = new ArrayList<Integer>(1);
        accepted.add(Integer.valueOf(correct));
        return wrong(cells, accepted);
    }

    private static int[] wrong(List<Cell> cells, List<Integer> accepted) {
        ArrayList<Integer> wrong = new ArrayList<Integer>();
        for (Cell cell : cells) {
            if (cell.grid && !accepted.contains(Integer.valueOf(cell.slotId))) wrong.add(Integer.valueOf(cell.slotId));
        }
        return toArray(wrong);
    }

    private static int[] toArray(List<Integer> values) {
        int[] result = new int[values.size()];
        for (int index = 0; index < values.size(); index++) result[index] = values.get(index).intValue();
        java.util.Arrays.sort(result);
        return result;
    }

    private static char requestedInitial(String title) {
        String clean = plain(title).toLowerCase(Locale.ROOT);
        int token = clean.indexOf("starts with");
        if (token < 0) return 0;
        for (int index = token + "starts with".length(); index < clean.length(); index++) {
            char value = clean.charAt(index);
            if (value >= 'a' && value <= 'z') return value;
        }
        return 0;
    }

    private static String requestedColor(String title) {
        String clean = plain(title).toLowerCase(Locale.ROOT);
        String[] colors = {"light blue", "light purple", "dark blue", "dark green", "dark aqua", "dark red",
                "dark purple", "dark gray", "black", "blue", "green", "aqua", "red", "purple", "orange",
                "yellow", "white", "gray"};
        for (String color : colors) if (clean.indexOf(color) >= 0) return color;
        return null;
    }

    private static boolean matchesColor(String requested, String text) {
        char code = firstColorCode(text);
        if (code == 0) return false;
        if ("red".equals(requested)) return code == 'c' || code == '4';
        if ("green".equals(requested)) return code == 'a' || code == '2';
        if ("blue".equals(requested)) return code == '9' || code == '1';
        if ("aqua".equals(requested)) return code == 'b' || code == '3';
        if ("purple".equals(requested)) return code == 'd' || code == '5';
        if ("orange".equals(requested)) return code == '6';
        if ("yellow".equals(requested)) return code == 'e';
        if ("white".equals(requested)) return code == 'f';
        if ("gray".equals(requested)) return code == '7' || code == '8';
        if ("black".equals(requested)) return code == '0';
        if ("light blue".equals(requested)) return code == 'b';
        if ("light purple".equals(requested)) return code == 'd';
        if ("dark blue".equals(requested)) return code == '1';
        if ("dark green".equals(requested)) return code == '2';
        if ("dark aqua".equals(requested)) return code == '3';
        if ("dark red".equals(requested)) return code == '4';
        if ("dark purple".equals(requested)) return code == '5';
        if ("dark gray".equals(requested)) return code == '8';
        return false;
    }

    private static char firstColorCode(String text) {
        if (text == null) return 0;
        for (int index = 0; index + 1 < text.length(); index++) {
            if (text.charAt(index) == '\u00a7') {
                char code = Character.toLowerCase(text.charAt(index + 1));
                if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) return code;
                index++;
            }
        }
        return 0;
    }

    private static int standaloneNumber(String text) {
        String clean = plain(text);
        if (clean == null) return -1;
        int start = -1;
        for (int index = 0; index <= clean.length(); index++) {
            boolean digit = index < clean.length() && Character.isDigit(clean.charAt(index));
            if (digit && start < 0) start = index;
            if (!digit && start >= 0) {
                boolean boundedBefore = start == 0 || !Character.isLetterOrDigit(clean.charAt(start - 1));
                boolean boundedAfter = index == clean.length() || !Character.isLetterOrDigit(clean.charAt(index));
                if (boundedBefore && boundedAfter && index - start <= 2) {
                    try { return Integer.parseInt(clean.substring(start, index)); }
                    catch (NumberFormatException ignored) { return -1; }
                }
                start = -1;
            }
        }
        return -1;
    }

    private static String plain(String text) {
        if (text == null) return "";
        StringBuilder result = new StringBuilder(text.length());
        for (int index = 0; index < text.length(); index++) {
            char value = text.charAt(index);
            if (value == '\u00a7' && index + 1 < text.length()) {
                index++;
            } else {
                result.append(value);
            }
        }
        return result.toString();
    }

    public static final class Cell {
        public final int slotId;
        public final int row;
        public final int column;
        public final boolean grid;
        public final boolean pane;
        public final int metadata;
        public final String name;

        public Cell(int slotId, int row, int column, boolean grid, boolean pane, int metadata, String name) {
            this.slotId = slotId;
            this.row = row;
            this.column = column;
            this.grid = grid;
            this.pane = pane;
            this.metadata = metadata;
            this.name = name == null ? "" : name;
        }
    }

    public static final class Solution {
        public static final Solution EMPTY = new Solution(new int[0], new int[0], new int[0]);
        public final int[] correct;
        public final int[] wrong;
        public final int[] alternatives;

        private Solution(int[] correct, int[] wrong, int[] alternatives) {
            this.correct = correct;
            this.wrong = wrong;
            this.alternatives = alternatives;
        }

        public boolean hasChoices() {
            return correct.length > 0;
        }

        public boolean isCorrect(int slotId) {
            return java.util.Arrays.binarySearch(correct, slotId) >= 0;
        }

        public boolean isWrong(int slotId) {
            return java.util.Arrays.binarySearch(wrong, slotId) >= 0;
        }
    }
}
