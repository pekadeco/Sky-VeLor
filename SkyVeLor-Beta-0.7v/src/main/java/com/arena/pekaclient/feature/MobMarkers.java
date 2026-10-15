package com.arena.pekaclient.feature;

import java.util.regex.Pattern;

/**
 * Reads the name tag of a dungeon mob. Pure text, no Minecraft types.
 *
 * Two markers are used. A level tag such as "[Lv15]" marks a regular dungeon mob. A star (U+272F) marks an
 * elite or mini-boss. Both are read from the formatted name tag after colour codes are removed. These markers
 * are the ones this build expects. They have not been checked against a live server, so a mob with a different
 * name format is simply not highlighted.
 */
public final class MobMarkers {

    /** Highlight class for a mob. */
    public enum Kind {
        NONE, NORMAL, ELITE
    }

    private static final Pattern LEVEL_TAG = Pattern.compile("\\[Lv\\d{1,3}\\]");
    static final char STAR = '\u272F';

    private MobMarkers() {
    }

    /** The highlight kind for a name tag. An elite star wins over a plain level tag. */
    public static Kind classify(String rawName) {
        if (rawName == null || rawName.isEmpty()) {
            return Kind.NONE;
        }
        String name = LeapText.strip(rawName);
        if (name.indexOf(STAR) >= 0) {
            return Kind.ELITE;
        }
        if (LEVEL_TAG.matcher(name).find()) {
            return Kind.NORMAL;
        }
        return Kind.NONE;
    }
}
