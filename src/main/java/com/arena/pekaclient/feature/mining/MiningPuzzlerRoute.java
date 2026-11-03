package com.arena.pekaclient.feature.mining;

import java.util.Locale;
import java.util.regex.Pattern;

/** Pure route parser for the Dwarven Mines Puzzler's direction clue. */
public final class MiningPuzzlerRoute {

    public static final int ORIGIN_X = 181;
    public static final int ORIGIN_Y = 195;
    public static final int ORIGIN_Z = 135;
    private static final int MAX_DIRECTIONS = 64;
    private static final Pattern FORMAT_CODE = Pattern.compile("\u00a7[0-9a-fk-or]", Pattern.CASE_INSENSITIVE);

    private MiningPuzzlerRoute() {
    }

    public static boolean isPuzzlerMessage(String message) {
        if (message == null || message.isEmpty()) return false;
        String clean = FORMAT_CODE.matcher(message).replaceAll("").trim();
        return clean.toLowerCase(Locale.ROOT).startsWith("[npc] puzzler:");
    }

    /** Returns {x,y,z} when the line is a valid Puzzler clue, otherwise null. */
    public static int[] targetFromMessage(String message) {
        if (!isPuzzlerMessage(message)) return null;
        String clean = FORMAT_CODE.matcher(message).replaceAll("").trim();
        int colon = clean.indexOf(':');
        if (colon < 0 || colon == clean.length() - 1) return null;

        int x = ORIGIN_X;
        int y = ORIGIN_Y;
        int z = ORIGIN_Z;
        int steps = 0;
        String route = clean.substring(colon + 1).trim();
        for (int i = 0; i < route.length(); i++) {
            char direction = route.charAt(i);
            if (Character.isWhitespace(direction)) continue;
            switch (direction) {
                case '\u25c0': // left triangle: east in the puzzle's instruction notation
                case '\u25c4':
                case '\u2190':
                    x++;
                    break;
                case '\u25b2':
                case '\u2191':
                    z++;
                    break;
                case '\u25bc':
                case '\u2193':
                    z--;
                    break;
                case '\u25b6':
                case '\u25ba':
                case '\u2192':
                    x--;
                    break;
                default:
                    return null;
            }
            if (++steps > MAX_DIRECTIONS) return null;
        }
        return steps == 0 ? null : new int[]{x, y, z};
    }
}
