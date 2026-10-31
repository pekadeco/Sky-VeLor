package com.arena.pekaclient.feature;

import java.util.regex.Pattern;

/**
 * Reads the name tag of a dungeon mob. Pure text, no Minecraft types.
 *
 * A level tag such as "[Lv15]" marks a regular dungeon mob. The exact U+272E star marks an elite or mini-boss.
 * Starred mobs primarily use the floating armor-stand label, with a direct-name fallback for server variants
 * that do not spawn a separate label. Similar-looking star glyphs are intentionally not treated as the marker.
 */
public final class MobMarkers {

    /** Highlight class for a mob. */
    public enum Kind {
        NONE, NORMAL, ELITE
    }

    private static final Pattern LEVEL_TAG = Pattern.compile("^\\s*\\[Lv\\s*\\d{1,3}\\](?:\\s|$)", Pattern.CASE_INSENSITIVE);
    /** Exact Better NEF starred-mob marker: ✮ (U+272E). Do not broaden to similar-looking glyphs. */
    static final char STAR = '\u272E';

    private MobMarkers() {
    }

    /** The highlight kind for a name tag. The exact ✮ marker wins over a plain level tag. */
    public static Kind classify(String rawName) {
        if (rawName == null || rawName.isEmpty()) {
            return Kind.NONE;
        }
        String name = LeapText.strip(rawName).trim();
        if (name.indexOf(STAR) >= 0) {
            return Kind.ELITE;
        }
        if (LEVEL_TAG.matcher(name).find()) {
            return Kind.NORMAL;
        }
        return Kind.NONE;
    }
}
