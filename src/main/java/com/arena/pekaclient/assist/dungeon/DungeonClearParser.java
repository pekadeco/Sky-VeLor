package com.arena.pekaclient.assist.dungeon;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses only the raw visible sidebar percentage used by Room Clear Alert. */
public final class DungeonClearParser {
    private static final Pattern CLEARED = Pattern.compile(
            "(?i)^(?:dungeon\\s+)?cleared\\s*[:=]\\s*(\\d{1,3})(?:\\.\\d+)?\\s*%\\s*$");
    private int percent = -1;

    public void reset() { percent = -1; }
    public int percent() { return percent; }
    public void observeSidebar(String raw) {
        String line = DungeonText.normalize(raw);
        Matcher m = CLEARED.matcher(line);
        if (!m.matches()) return;
        int value = Integer.parseInt(m.group(1));
        if (value <= 100) percent = value;
    }
}
