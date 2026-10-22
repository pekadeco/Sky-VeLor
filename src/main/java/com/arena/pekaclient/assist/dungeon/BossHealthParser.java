package com.arena.pekaclient.assist.dungeon;

import java.util.Locale;

/** Bounded text parser for the F7/M7 boss names and optional current/max health text. */
public final class BossHealthParser {

    private static final String[] NAMES = {"Maxor", "Storm", "Goldor", "Necron", "Power Dragon", "Apex Dragon",
            "Soul Dragon", "Ice Dragon", "Flame Dragon"};

    private BossHealthParser() {
    }

    public static final class Entry {
        public final String name;
        public final String current;
        public final String maximum;

        Entry(String name, String current, String maximum) {
            this.name = name;
            this.current = current;
            this.maximum = maximum;
        }

        public boolean hasHealth() {
            return current.length() > 0 && maximum.length() > 0;
        }
    }

    public static Entry parse(String source) {
        String line = clean(source);
        if (line.length() == 0) return null;
        String name = targetName(line);
        if (name == null) return null;
        int slash = line.indexOf('/');
        if (slash < 1 || slash + 1 >= line.length()) return new Entry(name, "", "");
        String current = healthTokenBefore(line, slash - 1);
        String maximum = healthTokenAfter(line, slash + 1);
        return new Entry(name, current, maximum);
    }

    public static String targetName(String source) {
        String text = clean(source).toLowerCase(Locale.ROOT);
        for (String name : NAMES) {
            String candidate = name.toLowerCase(Locale.ROOT);
            int at = text.indexOf(candidate);
            if (at < 0) continue;
            int end = at + candidate.length();
            boolean left = at == 0 || !Character.isLetterOrDigit(text.charAt(at - 1));
            boolean right = end == text.length() || !Character.isLetterOrDigit(text.charAt(end));
            if (left && right) return name;
        }
        return null;
    }

    public static String compact(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0D) return "";
        String[] units = {"", "k", "M", "B", "T"};
        int unit = 0;
        while (value >= 1000D && unit < units.length - 1) { value /= 1000D; unit++; }
        if (value >= 100D || Math.abs(value - Math.rint(value)) < .05D) return Long.toString(Math.round(value)) + units[unit];
        return String.format(Locale.ROOT, "%.1f%s", value, units[unit]);
    }

    private static String healthTokenBefore(String text, int end) {
        int start = end;
        while (start >= 0 && isHealthChar(text.charAt(start))) start--;
        return health(text.substring(start + 1, end + 1));
    }

    private static String healthTokenAfter(String text, int start) {
        int end = start;
        while (end < text.length() && isHealthChar(text.charAt(end))) end++;
        return health(text.substring(start, end));
    }

    private static boolean isHealthChar(char c) {
        return Character.isDigit(c) || c == ',' || c == '.' || c == ' ' || c == 'k' || c == 'K' || c == 'm' || c == 'M'
                || c == 'b' || c == 'B' || c == 't' || c == 'T';
    }

    private static String health(String raw) {
        String value = raw.replace(" ", "").replace(",", "");
        if (value.length() == 0 || value.length() > 16) return "";
        char last = value.charAt(value.length() - 1);
        if (last == 'k' || last == 'K' || last == 'm' || last == 'M' || last == 'b' || last == 'B' || last == 't' || last == 'T') {
            value = value.substring(0, value.length() - 1) + Character.toUpperCase(last);
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!Character.isDigit(c) && c != '.' && !(i == value.length() - 1 && Character.isLetter(c))) return "";
        }
        return value;
    }

    private static String clean(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder(Math.min(value.length(), 256));
        for (int i = 0; i < value.length() && out.length() < 256; i++) {
            char c = value.charAt(i);
            if (c == '\u00a7' && i + 1 < value.length()) { i++; continue; }
            if (c != '\r' && c != '\n') out.append(c);
        }
        return out.toString().trim().replaceAll("\\s+", " ");
    }
}
