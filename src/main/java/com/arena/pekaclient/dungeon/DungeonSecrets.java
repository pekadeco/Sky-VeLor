package com.arena.pekaclient.dungeon;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Observed secrets only; no inferred targets or dungeon rating. */
public final class DungeonSecrets {
    private static final Pattern FRACTION = Pattern.compile("(?i)secrets?(?:\\s+found)?\\s*[:=]?\\s*(\\d+)\\s*/\\s*(\\d+)");
    private static final Pattern PERCENT = Pattern.compile("(?i)secrets? found\\s*[:=]?\\s*(\\d{1,3})\\s*%");
    private int found = -1, total = -1, percent = -1;

    public void reset() { found = total = percent = -1; }
    public void observe(String line) {
        if (line == null) return;
        Matcher m = FRACTION.matcher(line);
        if (m.find()) { found = number(m.group(1), found); total = number(m.group(2), total); }
        m = PERCENT.matcher(line);
        if (m.find()) percent = Math.max(0, Math.min(100, number(m.group(1), 0)));
    }
    public void foundSecret() { found = Math.max(0, found) + 1; }
    public String display() {
        if (percent >= 0) return "Secrets: " + percent + "%";
        if (found >= 0 && total > 0) return "Secrets: " + found + "/" + total;
        if (found >= 0) return "Secrets: " + found + " found";
        return "Secrets: waiting for dungeon data";
    }
    private static int number(String text, int fallback) {
        try { return Integer.parseInt(text); } catch (RuntimeException ignored) { return fallback; }
    }
}
