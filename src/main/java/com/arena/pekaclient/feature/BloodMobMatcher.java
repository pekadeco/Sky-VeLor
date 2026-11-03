package com.arena.pekaclient.feature;

import java.util.Locale;
import java.util.regex.Pattern;

/** Pure name matching for the named minibosses highlighted in dungeon blood rooms. */
public final class BloodMobMatcher {
    private static final String[] BLOOD_MOB_NAMES = {
            "angry archaeologist",
            "lost adventurer",
            "frozen adventurer",
            "diamond guy",
            "shadow assassin",
            "king midas"
    };
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern LEVEL_PREFIX = Pattern.compile("^\\[\\s*lv\\s*\\d{1,3}\\s*\\]\\s*");
    private static final Pattern HEALTH_OR_DECORATION_SUFFIX = Pattern.compile(
            "^(?:[0-9][0-9,./ ]*(?:[❤♥])?|[❤♥][0-9,./ ]*|[✯✦★☆]+(?:\\s*[0-9,./]+)?|[•·]+(?:\\s*[0-9,./]+)?)$");

    private BloodMobMatcher() {
    }

    /**
     * Returns the canonical name when the whole mob name (optionally followed by level/health text) matches.
     * Prefix matches are rejected so names such as "Shadow Assassin Clone" are not silently treated as the
     * real blood mob.
     */
    public static String match(String rawName) {
        if (rawName == null || rawName.isEmpty()) {
            return null;
        }
        String normalized = normalize(rawName);
        normalized = LEVEL_PREFIX.matcher(normalized).replaceFirst("").trim();
        if (normalized.isEmpty()) {
            return null;
        }
        for (String known : BLOOD_MOB_NAMES) {
            if (normalized.equals(known)) {
                return known;
            }
            if (normalized.startsWith(known + " ")) {
                String suffix = normalized.substring(known.length()).trim();
                // Accept known UI/decorative suffixes, not arbitrary names such as "Shadow Assassin Clone".
                if ("(blood room)".equals(suffix)
                        || HEALTH_OR_DECORATION_SUFFIX.matcher(suffix).matches()) {
                    return known;
                }
            }
        }
        return null;
    }

    public static boolean isBloodMob(String rawName) {
        return match(rawName) != null;
    }

    private static String normalize(String rawName) {
        return WHITESPACE.matcher(LeapText.normalize(rawName).trim()).replaceAll(" ").toLowerCase(Locale.ROOT);
    }
}
