package com.arena.pekaclient.assist.mining;

/** Bounded, read-only decoder for the Dwarven Mines Puzzler direction line. It never clicks or sends packets. */
public final class PuzzlerRoute {

    public static final int ORIGIN_X = 181;
    public static final int ORIGIN_Y = 195;
    public static final int ORIGIN_Z = 135;
    private static final int MAX_STEPS = 64;

    private PuzzlerRoute() {
    }

    public static boolean isPuzzlerLine(String message) {
        String text = MiningAbilityPolicy.plain(message).toLowerCase(java.util.Locale.ROOT);
        return text.startsWith("[npc] puzzler:");
    }

    /** Returns x/y/z for a complete recognized direction path, or null for malformed/oversized input. */
    public static int[] decode(String message) {
        String text = MiningAbilityPolicy.plain(message);
        if (!isPuzzlerLine(text)) return null;
        int colon = text.indexOf(':');
        if (colon < 0 || colon + 1 >= text.length()) return null;
        int x = ORIGIN_X;
        int z = ORIGIN_Z;
        int steps = 0;
        for (int index = colon + 1; index < text.length(); index++) {
            char direction = text.charAt(index);
            if (Character.isWhitespace(direction)) continue;
            if (direction == '\u25c0' || direction == '\u25c4' || direction == '\u2190') {
                x++;
            } else if (direction == '\u25b6' || direction == '\u25ba' || direction == '\u2192') {
                x--;
            } else if (direction == '\u25b2' || direction == '\u2191') {
                z++;
            } else if (direction == '\u25bc' || direction == '\u2193') {
                z--;
            } else {
                return null;
            }
            if (++steps > MAX_STEPS) return null;
        }
        return steps == 0 ? null : new int[]{x, ORIGIN_Y, z};
    }
}
