package com.arena.pekaclient.dungeon;

import com.arena.pekaclient.feature.LeapText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Builds the dungeon-party roster from scoreboard party rows, not from the whole tab list. */
final class DungeonMapPartyRoster {
    private static final Pattern CLASS_TOKEN = Pattern.compile(
            "\\[(B|A|H|M|T|Archer|Berserk|Healer|Mage|Tank)]", Pattern.CASE_INSENSITIVE);
    private static final Pattern ROLE_SUFFIX = Pattern.compile(
            "\\((Archer|Berserk|Healer|Mage|Tank)\\b(?:\\s+[^()]*)?\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FIRST_NAME = Pattern.compile(
            "^\\s*(?:\\[[^\\]]*]\\s*)*([A-Za-z0-9_]{1,16})");
    private static final Pattern LAST_WORD = Pattern.compile("([A-Za-z0-9_]{1,16})[^A-Za-z0-9_]*$");
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{3,16}");

    private DungeonMapPartyRoster() {
    }

    static List<String> build(List<String> scoreboardLines, List<String> tabNames, String selfName) {
        List<String> knownNames = new ArrayList<String>();
        if (tabNames != null) {
            for (String name : tabNames) addUsername(knownNames, name);
        }
        addUsername(knownNames, selfName);

        List<String> party = new ArrayList<String>();
        boolean foundClassRow = false;
        if (scoreboardLines != null) {
            for (String raw : scoreboardLines) {
                String line = visible(raw);
                if (line.isEmpty()) continue;

                String candidate = null;
                Matcher classToken = CLASS_TOKEN.matcher(line);
                if (classToken.find()) {
                    String afterClass = line.substring(classToken.end());
                    Matcher first = FIRST_NAME.matcher(afterClass);
                    if (first.find()) candidate = resolveName(first.group(1), knownNames);
                } else {
                    Matcher roleSuffix = ROLE_SUFFIX.matcher(line);
                    if (roleSuffix.find()) {
                        Matcher last = LAST_WORD.matcher(line.substring(0, roleSuffix.start()));
                        if (last.find()) candidate = resolveName(last.group(1), knownNames);
                    }
                }

                if (candidate != null) {
                    foundClassRow = true;
                    addUnique(party, candidate);
                }
            }
        }

        // Mirror Better NEF's fallback: use tab names only when they actually appear in scoreboard lines.
        // Never use the entire tab roster as a substitute for the dungeon party; that admits NPCs and lobby users.
        if (!foundClassRow && scoreboardLines != null) {
            for (String raw : scoreboardLines) {
                String line = visible(raw);
                if (line.isEmpty()) continue;
                for (String name : knownNames) {
                    if (containsUsername(line, name)) addUnique(party, name);
                }
            }
        }

        String canonicalSelf = canonicalUsername(selfName);
        if (canonicalSelf != null && !containsIgnoreCase(party, canonicalSelf)) party.add(0, canonicalSelf);
        return Collections.unmodifiableList(party);
    }

    private static String resolveName(String token, List<String> knownNames) {
        String candidate = canonicalUsername(token);
        if (candidate == null) return null;
        for (String known : knownNames) {
            if (known.equalsIgnoreCase(candidate)) return known;
        }

        // Better NEF resolves a truncated scoreboard username when it matches one tab-list identity uniquely.
        if (candidate.length() >= 10) {
            String match = null;
            int matches = 0;
            for (String known : knownNames) {
                if (known.length() > candidate.length()
                        && known.regionMatches(true, 0, candidate, 0, candidate.length())) {
                    match = known;
                    matches++;
                }
            }
            if (matches == 1) return match;
        }
        // A class-tagged party row is stronger evidence than an arbitrary tab entry.
        return candidate;
    }

    private static String visible(String raw) {
        return LeapText.normalize(raw == null ? "" : raw);
    }

    private static String canonicalUsername(String value) {
        String normalized = visible(value);
        if (!USERNAME.matcher(normalized).matches()) return null;
        boolean hasLetter = false;
        for (int i = 0; i < normalized.length(); i++) {
            if (Character.isLetter(normalized.charAt(i))) {
                hasLetter = true;
                break;
            }
        }
        return hasLetter ? normalized : null;
    }

    private static void addUsername(List<String> destination, String value) {
        String name = canonicalUsername(value);
        if (name != null) addUnique(destination, name);
    }

    private static void addUnique(List<String> destination, String value) {
        if (!containsIgnoreCase(destination, value)) destination.add(value);
    }

    private static boolean containsIgnoreCase(List<String> values, String value) {
        for (String existing : values) {
            if (existing != null && value != null && existing.equalsIgnoreCase(value)) return true;
        }
        return false;
    }

    private static boolean containsUsername(String text, String username) {
        String target = username.toLowerCase(Locale.ROOT);
        String lowered = text.toLowerCase(Locale.ROOT);
        int from = 0;
        while ((from = lowered.indexOf(target, from)) >= 0) {
            int end = from + target.length();
            boolean before = from == 0 || !isUsernameCharacter(lowered.charAt(from - 1));
            boolean after = end == lowered.length() || !isUsernameCharacter(lowered.charAt(end));
            if (before && after) return true;
            from = end;
        }
        return false;
    }

    private static boolean isUsernameCharacter(char c) {
        return c == '_' || Character.isLetterOrDigit(c);
    }
}
