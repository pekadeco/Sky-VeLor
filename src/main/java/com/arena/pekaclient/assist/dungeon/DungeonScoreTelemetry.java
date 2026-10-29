package com.arena.pekaclient.assist.dungeon;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stateful parser for dungeon data that Fakepixel already presents in the sidebar, player tab list, and chat.
 *
 * <p>This class is intentionally Minecraft-free so its formats and reset behaviour can be regression-tested without
 * a client. It observes rather than controls: there is no API request, packet, inventory, or player interaction.</p>
 */
public final class DungeonScoreTelemetry {
    private static final Pattern FLOOR_TOKEN = Pattern.compile("(?i)\\b([efm][0-7])\\b");
    private static final Pattern FLOOR_WORD = Pattern.compile("(?i)\\bfloor\\s*([1-7])\\b");
    private static final Pattern MASTER_WORD = Pattern.compile("(?i)\\bmaster\\s+(?:mode\\s*)?(?:catacombs\\s*)?\\(?([1-7])\\)?");
    private static final Pattern SECRETS = Pattern.compile(
            "(?i)\\bsecrets?(?:\\s+found)?\\s*[:=]?\\s*(\\d{1,3})(?:\\.\\d+)?\\s*%");
    private static final Pattern CLEARED = Pattern.compile(
            "(?i)\\b(?:rooms?\\s+)?cleared\\s*[:=]?\\s*(\\d{1,3})\\s*%");
    private static final Pattern TIME = Pattern.compile(
            "(?i)\\b(?:time|run\\s*time)(?:\\s+elapsed)?\\s*[:=]?\\s*(\\d{1,2}):(\\d{2})\\b");
    private static final Pattern DEATHS = Pattern.compile(
            "(?i)\\bdeaths?\\s*[:=]\\s*(?:\\d{1,3}\\s*\\(\\s*)?(\\d{1,3})(?:\\s*\\))?");
    private static final Pattern CRYPTS = Pattern.compile("(?i)\\bcrypts?\\s*[:=]\\s*(\\d{1,2})\\b(?!\\s*/)");
    private static final Pattern TOTAL_SCORE = Pattern.compile("(?i)\\b(?:total\\s+|dungeon\\s+)?score\\s*[:=]\\s*(\\d{1,3})\\b");
    private static final Pattern RANK = Pattern.compile("(?i)\\brank\\s*[:=]\\s*(S\\+|[SABCD])(?=\\s|$)");
    private static final Pattern SKILL = Pattern.compile("(?i)\\bskill(?:\\s+score)?\\s*[:=]\\s*(\\d{1,3})\\b");
    private static final Pattern EXPLORATION = Pattern.compile("(?i)\\bexploration(?:\\s+score)?\\s*[:=]\\s*(\\d{1,3})\\b");
    private static final Pattern SPEED = Pattern.compile("(?i)\\bspeed(?:\\s+score)?\\s*[:=]\\s*(\\d{1,3})\\b");
    private static final Pattern BONUS = Pattern.compile("(?i)\\bbonus(?:\\s+score)?\\s*[:=]\\s*(\\d{1,3})\\b");

    private String floor = "";
    private int clearedPercent = DungeonScoreCalculator.UNKNOWN;
    private int secretsPercent = DungeonScoreCalculator.UNKNOWN;
    private int deaths = DungeonScoreCalculator.UNKNOWN;
    private int crypts = DungeonScoreCalculator.UNKNOWN;
    private int elapsedSeconds = DungeonScoreCalculator.UNKNOWN;
    private int failedPuzzles;
    private int spiritDeathPenaltyReductions;
    private boolean mimicKilled;
    private boolean paulActive;
    private int officialTotal = DungeonScoreCalculator.UNKNOWN;
    private String officialRank = "";
    private int officialSkill = DungeonScoreCalculator.UNKNOWN;
    private int officialExploration = DungeonScoreCalculator.UNKNOWN;
    private int officialSpeed = DungeonScoreCalculator.UNKNOWN;
    private int officialBonus = DungeonScoreCalculator.UNKNOWN;
    /** Small line window after a visible score recap heading; avoids treating ordinary player stats as end scores. */
    private int scoreRecapLines;

    /** Clears one run's visible observations. */
    public synchronized void reset() {
        floor = "";
        clearedPercent = DungeonScoreCalculator.UNKNOWN;
        secretsPercent = DungeonScoreCalculator.UNKNOWN;
        deaths = DungeonScoreCalculator.UNKNOWN;
        crypts = DungeonScoreCalculator.UNKNOWN;
        elapsedSeconds = DungeonScoreCalculator.UNKNOWN;
        failedPuzzles = 0;
        spiritDeathPenaltyReductions = 0;
        mimicKilled = false;
        paulActive = false;
        officialTotal = DungeonScoreCalculator.UNKNOWN;
        officialRank = "";
        officialSkill = DungeonScoreCalculator.UNKNOWN;
        officialExploration = DungeonScoreCalculator.UNKNOWN;
        officialSpeed = DungeonScoreCalculator.UNKNOWN;
        officialBonus = DungeonScoreCalculator.UNKNOWN;
        scoreRecapLines = 0;
    }

    /** Observes one sidebar or tab-list line. */
    public synchronized void observeVisible(String raw) {
        observe(raw);
    }

    /** Observes one received chat line while the caller has already confirmed a dungeon context. */
    public synchronized void observeChat(String raw) {
        String line = clean(raw);
        if (line.length() == 0) return;
        String lower = line.toLowerCase(Locale.ROOT);
        if (lower.indexOf("puzzle fail!") >= 0) {
            failedPuzzles = Math.min(99, failedPuzzles + 1);
        }
        if (lower.indexOf("spirit pet reduced your death score penalty") >= 0) {
            // The visible message guarantees one reduction. Repeating it cannot increase the same death's reduction.
            spiritDeathPenaltyReductions = Math.max(spiritDeathPenaltyReductions, 1);
        }
        if (isMimicKill(lower)) {
            mimicKilled = true;
        }
        if (looksLikePaul(lower)) {
            paulActive = true;
        }
        observe(line);
    }

    /** Snapshot used by the dungeon HUD. Individual visible inputs remain available for diagnostics and tests. */
    public synchronized DungeonScoreReadout readout() {
        return new DungeonScoreReadout(floor, clearedPercent, secretsPercent, deaths, crypts, elapsedSeconds,
                failedPuzzles, spiritDeathPenaltyReductions, mimicKilled, paulActive, officialTotal, officialRank);
    }

    public synchronized int officialSkill() { return officialSkill; }
    public synchronized int officialExploration() { return officialExploration; }
    public synchronized int officialSpeed() { return officialSpeed; }
    public synchronized int officialBonus() { return officialBonus; }

    private void observe(String raw) {
        String line = clean(raw);
        if (line.length() == 0) return;
        String lower = line.toLowerCase(Locale.ROOT);
        String foundFloor = floorIn(line);
        if (foundFloor.length() > 0 && !foundFloor.equals(floor)) {
            if (floor.length() > 0) resetForNewRun(foundFloor);
            else floor = foundFloor;
        }

        int value = percent(line, CLEARED);
        if (value >= 0) clearedPercent = value;
        value = percent(line, SECRETS);
        if (value >= 0) secretsPercent = value;
        value = seconds(line);
        if (value >= 0) {
            if (elapsedSeconds >= 0 && value + 15 < elapsedSeconds) {
                // A timer moving backwards is a new attempt even when its floor text has not refreshed yet.
                resetForNewRun(floor);
            }
            elapsedSeconds = value;
        }
        value = number(line, DEATHS, 0, 999);
        if (value >= 0) deaths = value;
        value = number(line, CRYPTS, 0, 99);
        if (value >= 0) crypts = value;
        if (looksLikePaul(lower)) paulActive = true;
        if (isMimicKill(lower)) mimicKilled = true;

        // A party/player chat line can imitate a score label, so only the server's multi-line recap heading opens the
        // confirmation window. This conservatism is what prevents an unverified S+ label.
        boolean recapHeading = lower.indexOf("extra stats") >= 0 || lower.indexOf("score breakdown") >= 0;
        if (recapHeading) scoreRecapLines = 12;
        boolean recapLine = recapHeading || scoreRecapLines > 0;
        if (recapLine) {
            officialSkill = readScoreComponent(line, SKILL, officialSkill);
            officialExploration = readScoreComponent(line, EXPLORATION, officialExploration);
            officialSpeed = readScoreComponent(line, SPEED, officialSpeed);
            officialBonus = readScoreComponent(line, BONUS, officialBonus);
            Matcher rank = RANK.matcher(line);
            if (rank.find()) officialRank = rank.group(1).toUpperCase(Locale.ROOT);
        }

        Matcher total = TOTAL_SCORE.matcher(line);
        if (total.find() && recapLine) {
            int parsed = bounded(total.group(1), 0, 999);
            if (parsed >= 0) officialTotal = parsed;
        }
        if (scoreRecapLines > 0 && !recapHeading) scoreRecapLines--;
    }

    private void resetForNewRun(String carriedFloor) {
        String retained = DungeonScoreCalculator.canonicalFloor(carriedFloor);
        reset();
        floor = retained;
    }

    private static String clean(String raw) {
        return DungeonRunReadout.normalize(raw);
    }

    private static String floorIn(String line) {
        Matcher master = MASTER_WORD.matcher(line);
        if (master.find()) return "M" + master.group(1);
        Matcher token = FLOOR_TOKEN.matcher(line);
        if (token.find()) return DungeonScoreCalculator.canonicalFloor(token.group(1));
        Matcher floor = FLOOR_WORD.matcher(line);
        return floor.find() ? "F" + floor.group(1) : "";
    }

    private static int percent(String line, Pattern pattern) {
        Matcher match = pattern.matcher(line);
        return match.find() ? bounded(match.group(1), 0, 100) : DungeonScoreCalculator.UNKNOWN;
    }

    private static int seconds(String line) {
        Matcher match = TIME.matcher(line);
        if (!match.find()) return DungeonScoreCalculator.UNKNOWN;
        int minutes = bounded(match.group(1), 0, 99);
        int seconds = bounded(match.group(2), 0, 59);
        return minutes < 0 || seconds < 0 ? DungeonScoreCalculator.UNKNOWN : minutes * 60 + seconds;
    }

    private static int number(String line, Pattern pattern, int minimum, int maximum) {
        Matcher match = pattern.matcher(line);
        return match.find() ? bounded(match.group(1), minimum, maximum) : DungeonScoreCalculator.UNKNOWN;
    }

    private static int readScoreComponent(String line, Pattern pattern, int previous) {
        Matcher match = pattern.matcher(line);
        if (!match.find()) return previous;
        int value = bounded(match.group(1), 0, 200);
        return value < 0 ? previous : value;
    }

    private static int bounded(String raw, int minimum, int maximum) {
        try {
            int value = Integer.parseInt(raw);
            return value < minimum || value > maximum ? DungeonScoreCalculator.UNKNOWN : value;
        } catch (RuntimeException ignored) {
            return DungeonScoreCalculator.UNKNOWN;
        }
    }

    private static boolean isMimicKill(String lower) {
        return lower.indexOf("mimic") >= 0 && (lower.indexOf("killed") >= 0 || lower.indexOf("defeated") >= 0
                || lower.indexOf("slain") >= 0);
    }

    private static boolean looksLikePaul(String lower) {
        return lower.indexOf("paul") >= 0 && (lower.indexOf("mayor") >= 0 || lower.indexOf("ezpz") >= 0
                || lower.indexOf("dungeon") >= 0);
    }
}
