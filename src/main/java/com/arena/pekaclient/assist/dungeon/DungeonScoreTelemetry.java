package com.arena.pekaclient.assist.dungeon;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stateful parser for dungeon data that Fakepixel already presents in the sidebar, public player tab list, and chat.
 *
 * <p>The three entry points intentionally retain their source boundaries: sidebar data supplies Catacombs/floor and
 * Dungeon Cleared, tab rows supply Dungeon/Secrets Found/Time/Deaths/Crypts, and chat supplies run events and the
 * final score recap. This keeps a player-chat line that merely resembles a statistic from manufacturing a live score.
 * The class is Minecraft-free so formats and reset behaviour can be regression-tested without a client.</p>
 */
public final class DungeonScoreTelemetry {
    private static final Pattern FLOOR_TOKEN = Pattern.compile("(?i)\\b([efm][0-7])\\b");
    private static final Pattern FLOOR_WORD = Pattern.compile("(?i)\\bfloor\\s*([1-7])\\b");
    private static final Pattern MASTER_WORD = Pattern.compile(
            "(?i)\\bmaster\\s+(?:mode\\s*)?(?:catacombs\\s*)?\\(?([1-7])\\)?");

    // Fakepixel's sidebar uses Dungeon Cleared. The optional prefix retains compatibility with older visible layouts.
    private static final Pattern SIDEBAR_CLEARED = Pattern.compile(
            "(?i)^(?:dungeon\\s+)?cleared\\s*[:=]\\s*(\\d{1,3})(?:\\.\\d+)?\\s*%\\s*$");
    private static final Pattern TAB_DUNGEON = Pattern.compile("(?i)^dungeon\\s*:\\s*(.+)$");
    private static final Pattern TAB_SECRETS = Pattern.compile(
            "(?i)^secrets\\s+found\\s*:\\s*(\\d{1,3})(?:\\.\\d+)?\\s*%\\s*$");
    private static final Pattern TAB_TIME = Pattern.compile(
            "(?i)^time(?:\\s+elapsed)?\\s*:\\s*(\\d{1,2}):(\\d{2})\\s*$");
    private static final Pattern TAB_DEATHS = Pattern.compile(
            "(?i)^deaths?\\s*:\\s*(?:\\d{1,3}\\s*\\(\\s*)?(\\d{1,3})(?:\\s*\\))?\\s*$");
    private static final Pattern TAB_CRYPTS = Pattern.compile("(?i)^crypts?\\s*:\\s*(.+)$");
    private static final Pattern FIRST_NUMBER = Pattern.compile("^(\\d{1,2})\\b");

    private static final Pattern TOTAL_SCORE = Pattern.compile(
            "(?i)\\b(?:total\\s+|dungeon\\s+)?score\\s*[:=]\\s*(\\d{1,3})\\b");
    private static final Pattern RANK = Pattern.compile("(?i)\\brank\\s*[:=]\\s*(S\\+|[SABCD])(?=\\s|$)");
    private static final Pattern SKILL = Pattern.compile("(?i)\\bskill(?:\\s+score)?\\s*[:=]\\s*(\\d{1,3})\\b");
    private static final Pattern EXPLORATION = Pattern.compile(
            "(?i)\\bexploration(?:\\s+score)?\\s*[:=]\\s*(\\d{1,3})\\b");
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
    /** Retained for /svdungeon diagnostics; Fakepixel does not award it a score bonus. */
    private boolean mimicKilled;
    private boolean bloodDoorOpened;
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
        bloodDoorOpened = false;
        paulActive = false;
        officialTotal = DungeonScoreCalculator.UNKNOWN;
        officialRank = "";
        officialSkill = DungeonScoreCalculator.UNKNOWN;
        officialExploration = DungeonScoreCalculator.UNKNOWN;
        officialSpeed = DungeonScoreCalculator.UNKNOWN;
        officialBonus = DungeonScoreCalculator.UNKNOWN;
        scoreRecapLines = 0;
    }

    /** Observes a raw sidebar title/row. Only sidebar-compatible floor and Dungeon Cleared values are accepted. */
    public synchronized void observeSidebar(String raw) {
        String line = clean(raw);
        if (line.length() == 0) return;
        observeFloor(line);
        int value = percent(line, SIDEBAR_CLEARED);
        if (value >= 0) clearedPercent = value;
        if (looksLikePaul(line.toLowerCase(Locale.ROOT))) paulActive = true;
    }

    /** Observes a raw public player-tab-list row using the labels displayed by Fakepixel. */
    public synchronized void observeTab(String raw) {
        String line = clean(raw);
        if (line.length() == 0) return;
        String lower = line.toLowerCase(Locale.ROOT);

        Matcher dungeon = TAB_DUNGEON.matcher(line);
        if (dungeon.matches()) {
            observeFloor(dungeon.group(1));
            // A few Fakepixel layouts include the floor in a decorated Dungeon row. Do not need a second source to use it.
            if (floor.length() == 0) observeFloor(line);
        }

        int value = percent(line, TAB_SECRETS);
        if (value >= 0) secretsPercent = value;

        value = seconds(line, TAB_TIME);
        if (value >= 0) observeTime(value);

        value = number(line, TAB_DEATHS, 0, 999);
        if (value >= 0) deaths = value;

        Matcher crypt = TAB_CRYPTS.matcher(line);
        if (crypt.matches()) {
            String rest = crypt.group(1).trim();
            // The reference treats a fraction-style Crypts row as no completed crypts, rather than pretending it is
            // the first numerator. A normal integer row remains the only source of a positive crypt bonus.
            if (rest.indexOf('/') >= 0) {
                crypts = 0;
            } else {
                Matcher number = FIRST_NUMBER.matcher(rest);
                if (number.find()) {
                    int parsed = bounded(number.group(1), 0, 99);
                    if (parsed >= 0) crypts = parsed;
                }
            }
        }
        if (looksLikePaul(lower)) paulActive = true;
    }

    /**
     * Compatibility entry point for callers/tests that only have an unlabeled visible line. New runtime collection
     * uses {@link #observeSidebar(String)} and {@link #observeTab(String)} explicitly.
     */
    public synchronized void observeVisible(String raw) {
        observeSidebar(raw);
        observeTab(raw);
    }

    /** Observes one received chat line while the caller has already confirmed a dungeon context. */
    public synchronized void observeChat(String raw) {
        String line = clean(raw);
        if (line.length() == 0) return;
        String lower = line.toLowerCase(Locale.ROOT);
        if (lower.indexOf("puzzle fail!") >= 0) {
            failedPuzzles = Math.min(99, failedPuzzles + 1);
        }
        if ("your spirit pet reduced your death score penalty to 1!".equals(lower)) {
            // The visible message guarantees one reduction. Repeating it cannot increase the same death's reduction.
            spiritDeathPenaltyReductions = Math.max(spiritDeathPenaltyReductions, 1);
        }
        if (lower.endsWith("the blood door has been opened!")) {
            bloodDoorOpened = true;
        }
        if (isMimicKill(lower)) mimicKilled = true;
        if (looksLikePaul(lower)) paulActive = true;
        observeOfficialRecap(line, lower);
    }

    /** Snapshot used by the dungeon HUD. Individual visible inputs remain available for diagnostics and tests. */
    public synchronized DungeonScoreReadout readout() {
        return new DungeonScoreReadout(floor, clearedPercent, secretsPercent, deaths, crypts, elapsedSeconds,
                failedPuzzles, spiritDeathPenaltyReductions, mimicKilled, bloodDoorOpened, paulActive,
                officialTotal, officialRank, officialSkill, officialExploration, officialSpeed, officialBonus);
    }

    public synchronized int officialSkill() { return officialSkill; }
    public synchronized int officialExploration() { return officialExploration; }
    public synchronized int officialSpeed() { return officialSpeed; }
    public synchronized int officialBonus() { return officialBonus; }

    private void observeOfficialRecap(String line, String lower) {
        // A party/player chat line can imitate a score label, so only the server's multi-line recap heading opens the
        // confirmation window. This conservatism is what prevents an unverified S+ label.
        boolean recapHeading = lower.indexOf("extra stats") >= 0 || lower.indexOf("score breakdown") >= 0;
        if (recapHeading) {
            scoreRecapLines = 14;
            return;
        }
        if (scoreRecapLines <= 0) return;

        officialSkill = readScoreComponent(line, SKILL, officialSkill);
        officialExploration = readScoreComponent(line, EXPLORATION, officialExploration);
        officialSpeed = readScoreComponent(line, SPEED, officialSpeed);
        officialBonus = readScoreComponent(line, BONUS, officialBonus);
        Matcher rank = RANK.matcher(line);
        if (rank.find()) officialRank = rank.group(1).toUpperCase(Locale.ROOT);
        Matcher total = TOTAL_SCORE.matcher(line);
        if (total.find()) {
            int parsed = bounded(total.group(1), 0, 999);
            if (parsed >= 0) officialTotal = parsed;
        }
        scoreRecapLines--;
    }

    private void observeFloor(String line) {
        String foundFloor = floorIn(line);
        if (foundFloor.length() == 0 || foundFloor.equals(floor)) return;
        if (floor.length() > 0) resetForNewRun(foundFloor);
        else floor = foundFloor;
    }

    private void observeTime(int value) {
        if (elapsedSeconds >= 0 && value + 15 < elapsedSeconds) {
            // A tab timer moving backwards is a new attempt even when its Dungeon row has not refreshed yet.
            resetForNewRun(floor);
        }
        elapsedSeconds = value;
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
        return match.matches() ? bounded(match.group(1), 0, 100) : DungeonScoreCalculator.UNKNOWN;
    }

    private static int seconds(String line, Pattern pattern) {
        Matcher match = pattern.matcher(line);
        if (!match.matches()) return DungeonScoreCalculator.UNKNOWN;
        int minutes = bounded(match.group(1), 0, 99);
        int seconds = bounded(match.group(2), 0, 59);
        return minutes < 0 || seconds < 0 ? DungeonScoreCalculator.UNKNOWN : minutes * 60 + seconds;
    }

    private static int number(String line, Pattern pattern, int minimum, int maximum) {
        Matcher match = pattern.matcher(line);
        return match.matches() ? bounded(match.group(1), minimum, maximum) : DungeonScoreCalculator.UNKNOWN;
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
