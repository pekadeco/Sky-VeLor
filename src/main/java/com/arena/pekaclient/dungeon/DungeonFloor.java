package com.arena.pekaclient.dungeon;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Local floor parsing retained by the secrets/progress tracker after the sync client was removed. */
public final class DungeonFloor {
    private static final Pattern FLOOR = Pattern.compile("(?i)(?:\\b(M[1-7]|F[1-7])\\b|master\\s*mode\\s*([1-7])|catacombs\\s*\\(\\s*F([1-7])\\s*\\))");
    private static final Pattern SIMPLE = Pattern.compile("(?i)\\bfloor\\s*([0-7])\\b|\\bf([0-7])\\b");
    private static final Pattern COLOR = Pattern.compile("(?i)\\u00a7[0-9A-FK-OR]");
    private DungeonFloor() { }
    public static String detect(Iterable<String> lines) {
        if (lines == null) return "--";
        for (String line : lines) {
            if (line == null) continue;
            String clean = COLOR.matcher(line).replaceAll("");
            Matcher m = FLOOR.matcher(clean);
            if (m.find()) {
                if (m.group(1) != null) return m.group(1).toUpperCase(Locale.ROOT);
                if (m.group(2) != null) return "M" + m.group(2);
                return "F" + m.group(3);
            }
            if (clean.toLowerCase(Locale.ROOT).contains("entrance") || clean.matches("(?i).*\\be0\\b.*")) return "E0";
            m = SIMPLE.matcher(clean);
            if (m.find()) return "F" + (m.group(1) != null ? m.group(1) : m.group(2));
        }
        return "--";
    }
}
