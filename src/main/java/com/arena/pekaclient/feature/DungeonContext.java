package com.arena.pekaclient.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Whether the player is in a dungeon. A Catacombs label remains the strongest signal, but Fakepixel-style sidebars
 * can use a generic dungeon/floor label instead. Those sidebars are accepted only when a floor or visible dungeon
 * metric corroborates the label, so normal SkyBlock scoreboards still fail closed.
 */
public final class DungeonContext {

    static final int SCAN_EVERY_TICKS = 20;
    private static final Pattern FLOOR_TOKEN = Pattern.compile(
            "(?i)(?:\\bmaster\\s*mode\\s*|\\b(?:floor|f|m)\\s*)([1-7])\\b");
    private static final Pattern ROMAN_FLOOR_SEVEN = Pattern.compile(
            "(?i)\\b(?:master\\s*mode|floor)\\s*(?:7|vii)\\b");
    private static final int METRIC_CLEARED = 1;
    private static final int METRIC_SECRETS = 2;
    private static final int METRIC_CRYPTS = 4;
    private static final int METRIC_DEATHS = 8;
    private static final int METRIC_TIME = 16;

    private static int ticksSinceScan = SCAN_EVERY_TICKS;
    private static volatile Result cached = Result.notInDungeon("not evaluated", "", false, "", "");

    private DungeonContext() {
    }

    /** Immutable diagnostic from the same sidebar detector used by every dungeon feature. */
    public static final class Result {
        public final boolean inDungeon;
        public final String reason;
        /** The visible sidebar title/line that made the detector accept the run. */
        public final String matchedScoreboardLine;
        /** Human-readable detector name: Catacombs for direct matches, otherwise Dungeon. */
        public final String dungeon;
        /** Parsed visible floor token such as F5 or M7, or an empty string when the sidebar omitted it. */
        public final String floor;
        private final boolean f7OrM7;

        private Result(boolean inDungeon, String reason, String matchedScoreboardLine, boolean f7OrM7,
                       String dungeon, String floor) {
            this.inDungeon = inDungeon;
            this.reason = reason == null ? "" : reason;
            this.matchedScoreboardLine = matchedScoreboardLine == null ? "" : matchedScoreboardLine;
            this.dungeon = dungeon == null ? "" : dungeon;
            this.floor = floor == null ? "" : floor;
            this.f7OrM7 = f7OrM7;
        }

        private static Result inDungeon(String reason, String matchedScoreboardLine, boolean f7OrM7,
                                        String dungeon, String floor) {
            return new Result(true, reason, matchedScoreboardLine, f7OrM7, dungeon, floor);
        }

        private static Result notInDungeon(String reason, String matchedScoreboardLine, boolean f7OrM7,
                                           String dungeon, String floor) {
            return new Result(false, reason, matchedScoreboardLine, f7OrM7, dungeon, floor);
        }
    }

    /** True when the last bounded sidebar scan found corroborated dungeon evidence. */
    public static boolean inDungeon() {
        return cached.inDungeon;
    }

    /** True only when the cached dungeon sidebar scan identifies Floor VII or Master Mode VII. */
    public static boolean inF7OrM7() {
        return cached.inDungeon && cached.f7OrM7;
    }

    /** Last detector outcome, including a human-readable reason suitable for live diagnostics. */
    public static Result cachedResult() {
        return cached;
    }

    /**
     * Runs the sidebar detector immediately and updates the cache. A missing player, world, sidebar, or corroborated
     * dungeon signal is deliberately negative; callers that would replace a vanilla GUI must fail closed.
     */
    public static Result evaluateNow(Minecraft mc) {
        Result result;
        if (mc == null) {
            result = Result.notInDungeon("Minecraft client unavailable", "", false, "", "");
        } else if (mc.theWorld == null || mc.thePlayer == null) {
            result = Result.notInDungeon("world or player unavailable", "", false, "", "");
        } else {
            result = scan(mc.theWorld.getScoreboard());
        }
        cached = result;
        return result;
    }

    /** Called once per client tick. Resets when the world is gone and otherwise updates only at the established rate. */
    public static void tick(Minecraft mc) {
        if (mc == null || mc.theWorld == null || mc.thePlayer == null) {
            cached = Result.notInDungeon("world or player unavailable", "", false, "", "");
            ticksSinceScan = SCAN_EVERY_TICKS;
            return;
        }
        if (++ticksSinceScan < SCAN_EVERY_TICKS) {
            return;
        }
        ticksSinceScan = 0;
        evaluateNow(mc);
    }

    private static Result scan(Scoreboard board) {
        if (board == null) {
            return Result.notInDungeon("sidebar scoreboard unavailable", "", false, "", "");
        }
        ScoreObjective sidebar = board.getObjectiveInDisplaySlot(1);
        if (sidebar == null) {
            return Result.notInDungeon("sidebar objective unavailable", "", false, "", "");
        }
        List<String> lines = new ArrayList<String>();
        Collection<Score> scores = board.getSortedScores(sidebar);
        if (scores != null) {
            for (Score score : scores) {
                if (score == null) continue;
                String holder = score.getPlayerName();
                String line = ScorePlayerTeam.formatPlayerName(board.getPlayersTeam(holder), holder);
                if (line != null) lines.add(line);
            }
        }
        if (scores == null) {
            return Result.notInDungeon("sidebar scores unavailable", "", false, "", "");
        }
        return evaluateSidebar(sidebar.getDisplayName(), lines);
    }

    /**
     * Pure sidebar decision shared with tests and debug tooling. Direct Catacombs wording wins immediately. Generic
     * server wording needs corroboration: a Dungeon label plus a floor/metric, a floor plus two metrics, or three
     * independent dungeon metric categories. That makes the detector tolerant without enabling unrelated sidebars.
     */
    static Result evaluateSidebar(String rawTitle, Iterable<String> rawLines) {
        String title = clean(rawTitle);
        List<String> visible = new ArrayList<String>();
        if (title.length() > 0) visible.add(title);
        if (rawLines != null) {
            for (String rawLine : rawLines) {
                String line = clean(rawLine);
                if (line.length() > 0) visible.add(line);
            }
        }

        String catacombsLine = "";
        String genericDungeonLine = "";
        String firstFloorLine = "";
        String floor = "";
        int metrics = 0;
        for (String line : visible) {
            String lower = line.toLowerCase(Locale.ROOT);
            String foundFloor = floorIn(lower);
            if (floor.length() == 0 && foundFloor.length() > 0) {
                floor = foundFloor;
                firstFloorLine = line;
            }
            if (catacombsLine.length() == 0 && isCatacombsLine(line)) catacombsLine = line;
            if (genericDungeonLine.length() == 0 && containsDungeonLabel(lower)) genericDungeonLine = line;
            metrics |= metricBits(lower);
        }
        boolean f7 = "F7".equals(floor) || "M7".equals(floor) || containsFloorSeven(visible);
        if (catacombsLine.length() > 0) {
            return Result.inDungeon("matched Catacombs sidebar line or title", catacombsLine, f7,
                    "Catacombs", floor);
        }
        int metricCount = Integer.bitCount(metrics);
        if (genericDungeonLine.length() > 0 && (floor.length() > 0 || metricCount >= 1)) {
            return Result.inDungeon("matched corroborated Dungeon sidebar evidence", genericDungeonLine, f7,
                    "Dungeon", floor);
        }
        if (floor.length() > 0 && metricCount >= 2) {
            return Result.inDungeon("matched floor plus visible dungeon metrics", firstFloorLine, f7,
                    "Dungeon", floor);
        }
        if (metricCount >= 3) {
            return Result.inDungeon("matched multiple visible dungeon metrics", firstMetricLine(visible), f7,
                    "Dungeon", floor);
        }
        return Result.notInDungeon("no corroborated dungeon sidebar evidence", "", f7, "", floor);
    }

    private static String firstMetricLine(List<String> visible) {
        for (String line : visible) if (metricBits(line.toLowerCase(Locale.ROOT)) != 0) return line;
        return "";
    }

    private static boolean containsFloorSeven(List<String> visible) {
        for (String line : visible) if (ROMAN_FLOOR_SEVEN.matcher(line).find()) return true;
        return false;
    }

    private static String floorIn(String lower) {
        if (lower == null) return "";
        Matcher floor = FLOOR_TOKEN.matcher(lower);
        if (!floor.find()) return "";
        String token = floor.group(0).toLowerCase(Locale.ROOT);
        return (token.indexOf("master") >= 0 || token.matches(".*\\bm\\s*[1-7].*"))
                ? "M" + floor.group(1) : "F" + floor.group(1);
    }

    private static int metricBits(String lower) {
        int bits = 0;
        if (lower.indexOf("cleared") >= 0 || lower.indexOf("rooms") >= 0 && lower.indexOf("%") >= 0) {
            bits |= METRIC_CLEARED;
        }
        if (lower.indexOf("secret") >= 0) bits |= METRIC_SECRETS;
        if (lower.indexOf("crypt") >= 0) bits |= METRIC_CRYPTS;
        if (lower.indexOf("death") >= 0) bits |= METRIC_DEATHS;
        if (lower.indexOf("time elapsed") >= 0 || lower.matches(".*\\b(?:run\\s+)?time\\s*[:=].*")) {
            bits |= METRIC_TIME;
        }
        return bits;
    }

    private static boolean containsDungeonLabel(String lower) {
        return lower.matches(".*\\bdungeons?\\b.*") || lower.indexOf("catacomb") >= 0;
    }

    private static String clean(String raw) {
        return raw == null ? "" : LeapText.strip(raw).trim();
    }

    /** Pure check used by the scan and by tests. Colour codes are removed before matching. */
    public static boolean isCatacombsLine(String rawLine) {
        if (rawLine == null) {
            return false;
        }
        String line = LeapText.strip(rawLine).toLowerCase(Locale.ROOT);
        return line.contains("catacombs");
    }
}
