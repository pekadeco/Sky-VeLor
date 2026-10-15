package com.arena.pekaclient.feature;

import java.util.Collection;
import java.util.Locale;

/**
 * Reads a player's dungeon class and a clean player name out of the text a client can actually see.
 *
 * Fakepixel has no usable public API, so the only sources are the strings the client already holds: the Spirit
 * Leap skull's display name, a scoreboard team prefix/suffix, a sidebar scoreboard line, and a tab-list entry.
 * All of those mix three different things into one string:
 *
 *  - Minecraft formatting codes (section sign + code character),
 *  - rank tags such as {@code [VIP]}, {@code [MVP+]} or {@code [MVP++]},
 *  - the dungeon class marker such as {@code [B]} for Berserk.
 *
 * This class separates them. The class marker is recognised only when a bracket holds exactly one of the letters
 * A, B, M, T or H, so a rank tag can never be mistaken for a class, and a class is never guessed from a name.
 * Everything here is pure: no Minecraft types, so it is unit-tested.
 */
public final class LeapName {

    /** The result of reading one piece of client-visible text. */
    public static final class Parsed {
        /** The class the marker named, or null when this text carries no class marker. */
        public final LeapClass cls;
        /** The player name with the formatting codes, the class marker and the rank tags removed. Never null. */
        public final String name;
        /** The bracket text the class was read from, such as "[B]", or null. */
        public final String marker;

        Parsed(LeapClass cls, String name, String marker) {
            this.cls = cls;
            this.name = name;
            this.marker = marker;
        }

        /** True when this text gave a class. */
        public boolean hasClass() {
            return cls != null;
        }
    }

    private static final Parsed EMPTY = new Parsed(null, "", null);

    private LeapName() {
    }

    /**
     * Reads one client-visible string: its class marker, if any, and its player name with the codes, the marker
     * and the rank tags taken out.
     *
     * Examples (all formatting codes removed first):
     * <pre>
     *   "[B] anuptripahti1"        -&gt; Berserk, "anuptripahti1"
     *   "§b[B] §7[VIP] Agent_Zeny" -&gt; Berserk, "Agent_Zeny"
     *   "[H] BlueGamer07"          -&gt; Healer,  "BlueGamer07"
     *   "[MVP+] pekadeco"          -&gt; none,    "pekadeco"
     *   "pekadeco"                 -&gt; none,    "pekadeco"
     *   "§cThis menu is closed"    -&gt; none,    "This menu is closed"
     * </pre>
     * The first single-letter class bracket wins when there is more than one. A bracket that is not a class
     * marker is treated as a rank tag and removed from the name, because no Minecraft user name contains brackets.
     */
    public static Parsed parse(String raw) {
        if (raw == null) {
            return EMPTY;
        }
        String text = LeapText.strip(raw);
        if (text.isEmpty()) {
            return EMPTY;
        }
        StringBuilder name = new StringBuilder(text.length());
        LeapClass cls = null;
        String marker = null;
        int i = 0;
        int n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            if (c == '[') {
                int close = text.indexOf(']', i + 1);
                if (close < 0) {
                    // No closing bracket: treat the rest as plain text rather than throwing it away.
                    name.append(text, i, n);
                    break;
                }
                String inside = text.substring(i + 1, close).trim();
                LeapClass hit = classMarker(inside);
                if (hit != null) {
                    if (cls == null) {
                        cls = hit;
                        marker = text.substring(i, close + 1);
                    }
                }
                // A rank tag or a repeated class marker: dropped from the name either way.
                i = close + 1;
                continue;
            }
            if (Character.isWhitespace(c)) {
                // Collapse runs of whitespace so a removed tag cannot leave a double space behind.
                if (name.length() > 0 && name.charAt(name.length() - 1) != ' ') {
                    name.append(' ');
                }
                i++;
                continue;
            }
            name.append(c);
            i++;
        }
        return new Parsed(cls, name.toString().trim(), marker);
    }

    /**
     * The class for the inside of one bracket, or null. Exactly one upper-case A, B, M, T or H counts, which is
     * what the server sends. "VIP", "MVP+" and "+++" therefore never match, and neither does a lower-case letter.
     */
    public static LeapClass classMarker(String inside) {
        if (inside == null || inside.length() != 1) {
            return null;
        }
        return LeapClass.fromMarker(inside.charAt(0));
    }

    /**
     * The class from a scoreboard team prefix or suffix that holds only the marker, such as {@code "§9[B] "}.
     * The owning name is not part of this text, which is why the marker is simply the first class bracket.
     * Returns null when the text has no class marker.
     */
    public static LeapClass classFromMarkerOnly(String prefixOrSuffix) {
        return parse(prefixOrSuffix).cls;
    }

    /**
     * The class for a name that the server cut short with an ellipsis, such as {@code "[B] anuptripah..."} for the
     * owner {@code anuptripah_xyz}. The kept part must be at least 3 characters, shorter than the full name, the
     * start of it, and the start of exactly one known name - which has to be this owner. Otherwise null, so an
     * ambiguous cut never hands one teammate another teammate's class.
     */
    public static LeapClass classFromTruncated(String raw, String owner, Collection<String> knownNames) {
        if (raw == null || owner == null || knownNames == null) {
            return null;
        }
        String full = owner.trim();
        Parsed parsed = parse(raw);
        String kept = parsed.name;
        if (!isTruncated(kept)) {
            return null;
        }
        kept = kept.substring(0, kept.length() - (kept.endsWith("\u2026") ? 1 : 3)).trim();
        if (kept.length() < 3 || kept.length() >= full.length() || !full.startsWith(kept)) {
            return null;
        }
        if (!isUniqueStart(kept, full, knownNames)) {
            return null;
        }
        return parsed.cls;
    }

    private static boolean isTruncated(String s) {
        return s.endsWith("\u2026") || s.endsWith("...");
    }

    /** True when exactly one name in {@code names} starts with {@code kept}, and that name is {@code owner}. */
    static boolean isUniqueStart(String kept, String owner, Collection<String> names) {
        int hits = 0;
        boolean ownerHit = false;
        for (String n : names) {
            if (n == null) {
                continue;
            }
            String name = n.trim();
            if (name.startsWith(kept)) {
                hits++;
                ownerHit = name.equals(owner);
            }
        }
        return hits == 1 && ownerHit;
    }

    /**
     * The class for text that holds the owner's own name somewhere: a team's prefix + name + suffix, or one
     * sidebar scoreboard line such as {@code "§b[B] §7[VIP] Agent_Zeny"}.
     * The owner has to appear as a whole word. Rank tags in front of the class marker are skipped, and the text
     * may be missing the space after the marker.
     */
    public static LeapClass classFromText(String raw, String owner) {
        if (raw == null || owner == null || owner.trim().isEmpty()) {
            return null;
        }
        String text = LeapText.strip(raw);
        String name = owner.trim();
        int from = 0;
        while (true) {
            int at = text.indexOf(name, from);
            if (at < 0) {
                return null;
            }
            int end = at + name.length();
            boolean wholeWord = (at == 0 || !isNameChar(text.charAt(at - 1)))
                    && (end >= text.length() || !isNameChar(text.charAt(end)));
            if (wholeWord) {
                return classImmediatelyBefore(text, at);
            }
            from = at + 1;
        }
    }

    /**
     * Walks back from the owner's name over rank brackets and spaces and returns the first class bracket it meets.
     * Stops at any other character, so a marker that is not attached to this name is never used.
     */
    static LeapClass classImmediatelyBefore(String text, int nameStart) {
        int end = nameStart;
        while (end > 0 && text.charAt(end - 1) == ' ') {
            end--;
        }
        while (end > 0 && text.charAt(end - 1) == ']') {
            int open = text.lastIndexOf('[', end - 1);
            if (open < 0) {
                return null;
            }
            String inside = text.substring(open + 1, end - 1).trim();
            LeapClass cls = classMarker(inside);
            if (cls != null) {
                return cls;
            }
            end = open;
            while (end > 0 && text.charAt(end - 1) == ' ') {
                end--;
            }
        }
        return null;
    }

    static boolean isNameChar(char c) {
        return c == '_' || Character.isLetterOrDigit(c);
    }

    /** Lower-cased, whitespace-collapsed form of a name, for stable comparisons. */
    public static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return LeapText.strip(s).trim().toLowerCase(Locale.ROOT);
    }
}
