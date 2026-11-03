package com.arena.pekaclient.assist.dungeon;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bounded, Minecraft-free parser for the dungeon values that are already visible in a sidebar.
 *
 * <p>This is deliberately a readout, not a score estimator: it records only explicitly observed floor, time,
 * clear, secrets, and deaths. It does not reflect into tab-list internals, infer score values, send chat, close a
 * screen, or control dungeon gameplay.</p>
 */
public final class DungeonRunReadout {
    private static final Pattern SECRETS_FRACTION = Pattern.compile(
            "(?i)\\bsecrets?(?:\\s+found)?\\s*[:=]?\\s*(\\d{1,4})\\s*/\\s*(\\d{1,4})");
    private static final Pattern SECRETS_PERCENT = Pattern.compile(
            "(?i)\\bsecrets?(?:\\s+found)?\\s*[:=]?\\s*(\\d{1,3})\\s*%");
    private static final Pattern TIME = Pattern.compile(
            "(?i)\\btime(?:\\s+elapsed)?\\s*[:=]?\\s*(\\d{1,2}):(\\d{2})\\b");
    private static final Pattern CLEARED = Pattern.compile(
            "(?i)\\bcleared\\s*[:=]?\\s*(\\d{1,3})\\s*%");
    private static final Pattern DEATHS = Pattern.compile(
            "(?i)\\bdeaths?\\s*[:=]?\\s*(\\d{1,3})\\b");
    private static final Pattern FLOOR_TOKEN = Pattern.compile("(?i)\\b([fm][1-7])\\b");
    private static final Pattern FLOOR_WORD = Pattern.compile("(?i)\\bfloor\\s*([1-7])\\b");
    private static final Pattern MASTER_WORD = Pattern.compile("(?i)\\bmaster\\s+mode\\s*([1-7])\\b");

    private String floor;
    private int elapsedSeconds;
    private int clearedPercent;
    private int secretsFound;
    private int secretsTotal;
    private int secretsPercent;
    private int deaths;

    private String floorLine;
    private String timeLine;
    private String clearedLine;
    private String secretsLine;
    private String deathsLine;

    public DungeonRunReadout() {
        reset();
    }

    /** Drops all run-only observations, for example after leaving a dungeon or loading a different world. */
    public void reset() {
        floor = "--";
        elapsedSeconds = -1;
        clearedPercent = -1;
        secretsFound = -1;
        secretsTotal = -1;
        secretsPercent = -1;
        deaths = -1;
        rebuildLines();
    }

    /** Parses one visible sidebar/title line and returns whether an observed value changed. */
    public boolean observe(String raw) {
        String line = normalize(raw);
        if (line.length() == 0) return false;
        boolean changed = false;

        String observedFloor = floorIn(line);
        if (observedFloor.length() > 0 && !observedFloor.equals(floor)) {
            floor = observedFloor;
            changed = true;
        }

        Matcher fraction = SECRETS_FRACTION.matcher(line);
        if (fraction.find()) {
            int found = bounded(fraction.group(1), -1, 0, 9999);
            int total = bounded(fraction.group(2), -1, 1, 9999);
            if (found != -1 && total != -1 && found <= total
                    && (found != secretsFound || total != secretsTotal)) {
                secretsFound = found;
                secretsTotal = total;
                secretsPercent = -1;
                changed = true;
            }
        }

        Matcher secretPercent = SECRETS_PERCENT.matcher(line);
        if (secretPercent.find()) {
            int value = bounded(secretPercent.group(1), -1, 0, 100);
            if (value != -1 && value != secretsPercent) {
                secretsPercent = value;
                changed = true;
            }
        }

        Matcher time = TIME.matcher(line);
        if (time.find()) {
            int minutes = bounded(time.group(1), -1, 0, 99);
            int seconds = bounded(time.group(2), -1, 0, 59);
            int total = minutes < 0 || seconds < 0 ? -1 : minutes * 60 + seconds;
            if (total >= 0 && total != elapsedSeconds) {
                elapsedSeconds = total;
                changed = true;
            }
        }

        Matcher cleared = CLEARED.matcher(line);
        if (cleared.find()) {
            int value = bounded(cleared.group(1), -1, 0, 100);
            if (value != -1 && value != clearedPercent) {
                clearedPercent = value;
                changed = true;
            }
        }

        Matcher deathMatch = DEATHS.matcher(line);
        if (deathMatch.find()) {
            int value = bounded(deathMatch.group(1), -1, 0, 999);
            if (value != -1 && value != deaths) {
                deaths = value;
                changed = true;
            }
        }

        if (changed) rebuildLines();
        return changed;
    }

    /** True once at least one current-run value has been observed. */
    public boolean hasData() {
        return !"--".equals(floor) || elapsedSeconds >= 0 || clearedPercent >= 0
                || secretsFound >= 0 || secretsPercent >= 0 || deaths >= 0;
    }

    public String floorLine() { return floorLine; }
    public String timeLine() { return timeLine; }
    public String clearedLine() { return clearedLine; }
    public String secretsLine() { return secretsLine; }
    public String deathsLine() { return deathsLine; }

    /** Short diagnostic text used by /lunaria selftest without exposing any player data. */
    public String diagnostic() {
        return hasData() ? floorLine + ", " + secretsLine : "waiting for a visible dungeon sidebar";
    }

    static String normalize(String raw) {
        if (raw == null || raw.length() == 0) return "";
        StringBuilder clean = new StringBuilder(raw.length());
        for (int index = 0; index < raw.length(); index++) {
            char c = raw.charAt(index);
            if (c == '\u00a7' && index + 1 < raw.length()) {
                index++;
            } else if (c >= 32) {
                clean.append(c);
            }
        }
        return clean.toString().trim();
    }

    private void rebuildLines() {
        floorLine = "Floor: " + floor;
        timeLine = elapsedSeconds < 0 ? "Time: --" : "Time: " + timeText(elapsedSeconds);
        clearedLine = clearedPercent < 0 ? "Cleared: --" : "Cleared: " + clearedPercent + "%";
        if (secretsFound >= 0 && secretsTotal >= 0) {
            secretsLine = "Secrets: " + secretsFound + "/" + secretsTotal;
        } else if (secretsPercent >= 0) {
            secretsLine = "Secrets: " + secretsPercent + "%";
        } else {
            secretsLine = "Secrets: --";
        }
        deathsLine = deaths < 0 ? "Deaths: --" : "Deaths: " + deaths;
    }

    private static String floorIn(String line) {
        Matcher master = MASTER_WORD.matcher(line);
        if (master.find()) return "M" + master.group(1);
        Matcher token = FLOOR_TOKEN.matcher(line);
        if (token.find()) return token.group(1).toUpperCase(java.util.Locale.ROOT);
        Matcher floor = FLOOR_WORD.matcher(line);
        return floor.find() ? "F" + floor.group(1) : "";
    }

    private static String timeText(int seconds) {
        int minutes = seconds / 60;
        int remainder = seconds % 60;
        return minutes + ":" + (remainder < 10 ? "0" : "") + remainder;
    }

    private static int bounded(String raw, int fallback, int min, int max) {
        try {
            int value = Integer.parseInt(raw);
            return value < min || value > max ? fallback : value;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
