package com.arena.pekaclient.feature;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure text helpers for the Spirit Leap teammate selector. No Minecraft types, so they can be unit-tested.
 *
 * Minecraft text uses section signs (U+00A7) followed by a code character for colour and style. These
 * helpers remove those codes and read a "Class: ..." line from an item's lore when one exists.
 */
public final class LeapText {

    private LeapText() {
    }

    /** Removes colour and style codes: a section sign and the character after it. */
    public static String strip(String s) {
        if (s == null || s.indexOf('\u00a7') < 0) {
            return s == null ? "" : s;
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\u00a7') {
                i++;
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }

    /**
     * The class name from the first lore line that reads "Class: X", with formatting removed.
     * Returns null when no such line exists. Nothing is guessed from other lines.
     */
    public static String classFromLore(List<String> lore) {
        if (lore == null) {
            return null;
        }
        for (String line : lore) {
            String t = strip(line).trim();
            int colon = t.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String key = t.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            if (!"class".equals(key)) {
                continue;
            }
            String value = t.substring(colon + 1).trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        return null;
    }

    /** A bracketed group such as "(Archer XXV)" or "(Mage DEAD)": the first word is the candidate class. */
    private static final Pattern TAB_CLASS = Pattern.compile("\\(([A-Za-z]+)(?:\\s[^()]*)?\\)");

    /**
     * The class from one tab-list entry, such as "Name (Archer XXV)". The entry is used only when it names the
     * same player as the skull's owner as a whole word, so another player's entry can never supply the class.
     * Only an exact class name counts. Returns the class label, or null when the entry does not give one.
     * Pure: the caller passes the entry text it read from the tab list.
     */
    public static String classFromTab(String entryText, String owner) {
        if (entryText == null || owner == null || owner.isEmpty()) {
            return null;
        }
        String t = strip(entryText).trim();
        if (!containsWord(t, owner.trim())) {
            return null;
        }
        Matcher m = TAB_CLASS.matcher(t);
        while (m.find()) {
            LeapClass c = LeapClass.parse(m.group(1));
            if (c != null) {
                return c.label();
            }
        }
        return null;
    }

    /**
     * The class from a scoreboard or tab-list prefix such as "[A] pekadeco" (Archer). The marker must be the
     * bracketed letter that sits directly before the owner's name as a whole word. Ranks in front of the marker,
     * such as "[MVP+] [B] amito", are skipped. Spacing may be absent: "[T]player4" is valid. Formatting codes are
     * removed first. Returns the class, or null when no marker sits in front of this owner. Nothing is guessed from
     * the name itself, and a marker that is not one of A, B, M, T or H is ignored.
     *
     * Kept as the name the rest of the mod already uses; the parsing itself lives in {@link LeapName}, which also
     * strips the marker and the rank tags out of the text it reads.
     */
    public static LeapClass classFromPrefix(String entryText, String owner) {
        return LeapName.classFromText(entryText, owner);
    }

    /**
     * The class from a name the server cut short with "..." (or the single ellipsis character), such as
     * "[B] anuptripah..." for the owner "anuptripah_xyz". The kept part must be at least 3 name characters, shorter
     * than the owner's full name, and the start of it. It must also be the start of exactly one name in
     * {@code knownNames}, and that name must be the owner, so an ambiguous cut never gives a class. Returns null
     * otherwise. Nothing is guessed from the kept text beyond that check.
     */
    public static LeapClass classFromTruncated(String entryText, String owner, Collection<String> knownNames) {
        return LeapName.classFromTruncated(entryText, owner, knownNames);
    }

    /** Name equality for matching a skull owner to a scoreboard or tab-list name: trimmed, case-insensitive. */
    public static boolean sameName(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        String x = strip(a).trim();
        String y = strip(b).trim();
        return !x.isEmpty() && x.equalsIgnoreCase(y);
    }

    /** True when {@code word} appears in {@code text} with no name character on either side. */
    static boolean containsWord(String text, String word) {
        if (word.isEmpty()) {
            return false;
        }
        int from = 0;
        while ((from = text.indexOf(word, from)) >= 0) {
            int end = from + word.length();
            boolean before = from == 0 || !isNameChar(text.charAt(from - 1));
            boolean after = end >= text.length() || !isNameChar(text.charAt(end));
            if (before && after) {
                return true;
            }
            from = end;
        }
        return false;
    }

    private static boolean isNameChar(char c) {
        return c == '_' || Character.isLetterOrDigit(c);
    }

    /** True when a container title, without formatting, is the Spirit Leap menu. */
    public static boolean isLeapTitle(String title) {
        return "Spirit Leap".equals(strip(title).trim());
    }
}
