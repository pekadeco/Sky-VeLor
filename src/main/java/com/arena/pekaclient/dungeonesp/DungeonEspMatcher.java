package com.arena.pekaclient.dungeonesp;

/**
 * Allocation-free name-tag recognition for Hypixel dungeon mobs. The matcher intentionally requires a yellow-star
 * marker for ordinary mobs: a plain [Lv] stand is not an ESP target. Known dungeon minibosses are accepted even
 * while their tag's star/health text is in flight, which lets a delayed tag/body pair settle on the next tick scan.
 */
public final class DungeonEspMatcher {

    private DungeonEspMatcher() {
    }

    public static DungeonEspCategory categoryForTag(String name) {
        if (name == null || name.length() == 0) return DungeonEspCategory.NONE;
        if (containsToken(name, "shadow assassin") || containsToken(name, "lost adventurer")
                || containsToken(name, "angry archaeologist") || containsToken(name, "frozen adventurer")
                || containsToken(name, "king midas")) {
            return DungeonEspCategory.MINIBOSS;
        }
        if (!hasStar(name)) return DungeonEspCategory.NONE;
        if (containsToken(name, "withermancer") || containsToken(name, "skeleton master")
                || containsToken(name, "zombie commander") || containsToken(name, "crypt dreadlord")
                || containsToken(name, "fels") || containsToken(name, "dreadlord")) {
            return DungeonEspCategory.SPECIAL;
        }
        return DungeonEspCategory.STARRED;
    }

    /** Supports the two common Hypixel star glyphs plus visually equivalent tags emitted by resource packs. */
    public static boolean hasStar(String name) {
        if (name == null) return false;
        for (int i = 0, n = name.length(); i < n; i++) {
            char c = name.charAt(i);
            if (c == '\u272e' || c == '\u272f' || c == '\u2726' || c == '\u2605') return true;
        }
        return false;
    }

    /**
     * Case-insensitive text search which ignores Minecraft formatting pairs. It does not create stripped strings,
     * regex matchers, or lowercase copies on the configured tick scan path.
     */
    static boolean containsToken(String source, String token) {
        if (source == null || token == null || token.length() == 0) return false;
        int matched = 0;
        for (int i = 0, n = source.length(); i < n; i++) {
            char c = source.charAt(i);
            if (c == '\u00a7' && i + 1 < n) {
                i++;
                continue;
            }
            char expected = token.charAt(matched);
            if (equalsIgnoreAscii(c, expected)) {
                matched++;
                if (matched == token.length()) return true;
            } else {
                // Restart at this character when it could be the first letter of the token.
                matched = equalsIgnoreAscii(c, token.charAt(0)) ? 1 : 0;
            }
        }
        return false;
    }

    private static boolean equalsIgnoreAscii(char left, char right) {
        if (left == right) return true;
        if (left >= 'A' && left <= 'Z') left = (char) (left + ('a' - 'A'));
        return left == right;
    }
}
