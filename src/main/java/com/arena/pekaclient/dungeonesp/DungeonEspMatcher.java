package com.arena.pekaclient.dungeonesp;

/**
 * Allocation-free recognition for Hypixel dungeon tags and Enderman marker data. Ordinary level tags deliberately do
 * not become ESP targets; a yellow-star signal is still required for non-miniboss bodies, including when the star
 * and the target name live on separate armor stands.
 */
public final class DungeonEspMatcher {

    /*
     * These are the two server NBT representations of the public Dungeon Enderman-head texture (http/https URL).
     * Comparing the raw property value is cheaper and safer than decoding player-head NBT on each cached scan.
     */
    private static final String ENDERMAN_HEAD_TEXTURE_HTTP =
            "e3RleHR1cmVzOntTS0lOOnt1cmw6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2E1OWJiMGE3YTMyOTY1YjNkOTBkOGVhZmE4OTlkMTgzNWY0MjQ1MDllYWRkNGU2YjcwOWFkYTUwYjljZiJ9fX0=";
    private static final String ENDERMAN_HEAD_TEXTURE_HTTPS =
            "e3RleHR1cmVzOntTS0lOOnt1cmw6Imh0dHBzOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzdhNTliYjBhN2EzMjk2NWIzZDkwZDhlYWZhODk5ZDE4MzVmNDI0NTA5ZWFkZDRlNmI3MDlhZGE1MGI5Y2YifX19";

    private DungeonEspMatcher() {
    }

    /** Classifies one complete tag line. A non-starred named miniboss is retained while tag text settles. */
    public static DungeonEspCategory categoryForTag(String name) {
        if (name == null || name.length() == 0) return DungeonEspCategory.NONE;
        DungeonEspCategory named = categoryForMobName(name);
        if (named == DungeonEspCategory.MINIBOSS) return named;
        if (!hasStar(name)) return DungeonEspCategory.NONE;
        return named == DungeonEspCategory.NONE ? DungeonEspCategory.STARRED : named;
    }

    /** Identifies a mob name independently from the star so split armor-stand lines can be joined by the tracker. */
    public static DungeonEspCategory categoryForMobName(String name) {
        if (name == null || name.length() == 0) return DungeonEspCategory.NONE;
        if (containsToken(name, "shadow assassin") || containsToken(name, "lost adventurer")
                || containsToken(name, "angry archaeologist") || containsToken(name, "frozen adventurer")
                || containsToken(name, "king midas")) {
            return DungeonEspCategory.MINIBOSS;
        }
        if (containsToken(name, "skeletor")) {
            return DungeonEspCategory.SKELETOR;
        }
        if (containsToken(name, "withermancer") || containsToken(name, "skeleton master")
                || containsToken(name, "zombie commander") || containsToken(name, "crypt dreadlord")
                || containsToken(name, "crypt souleater") || containsToken(name, "fels")
                || containsToken(name, "dreadlord")) {
            return DungeonEspCategory.SPECIAL;
        }
        return DungeonEspCategory.NONE;
    }

    /** Relevant tag text is a star-bearing line or a recognized identity that may be paired with a separate star. */
    public static boolean isTagCandidate(String name) {
        return hasStar(name) || categoryForMobName(name) != DungeonEspCategory.NONE;
    }

    /** Recognizes the two raw skull-property forms currently used for the Dungeon Enderman/Fel marker. */
    public static boolean isKnownEndermanHeadTexture(String texture) {
        return ENDERMAN_HEAD_TEXTURE_HTTP.equals(texture) || ENDERMAN_HEAD_TEXTURE_HTTPS.equals(texture);
    }

    /** A named head marker is also accepted when server text explicitly identifies the Enderman/Fel representation. */
    public static boolean isEndermanMarkerName(String name) {
        return containsToken(name, "enderman") || containsToken(name, "fel");
    }

    /** Supports the common Hypixel star glyphs plus visually equivalent tags emitted by resource packs. */
    public static boolean hasStar(String name) {
        if (name == null) return false;
        for (int i = 0, n = name.length(); i < n; i++) {
            char c = name.charAt(i);
            if (c == '\u272e' || c == '\u272f' || c == '\u2726' || c == '\u2727'
                    || c == '\u2736' || c == '\u2605') return true;
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
