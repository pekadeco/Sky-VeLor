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

/**
 * Whether the player is in a dungeon. The established signal is the sidebar scoreboard: a visible line that names
 * "Catacombs", assembled from its team prefix, holder name, and suffix. Tick consumers use a bounded cached scan;
 * sensitive GUI interception can call {@link #evaluateNow(Minecraft)} to require a fresh positive result.
 */
public final class DungeonContext {

    static final int SCAN_EVERY_TICKS = 20;
    private static int ticksSinceScan = SCAN_EVERY_TICKS;
    private static volatile Result cached = Result.notInDungeon("not evaluated", "", false);

    private DungeonContext() {
    }

    /** Immutable diagnostic from the same sidebar detector used by every dungeon feature. */
    public static final class Result {
        public final boolean inDungeon;
        public final String reason;
        public final String matchedScoreboardLine;
        private final boolean f7OrM7;

        private Result(boolean inDungeon, String reason, String matchedScoreboardLine, boolean f7OrM7) {
            this.inDungeon = inDungeon;
            this.reason = reason == null ? "" : reason;
            this.matchedScoreboardLine = matchedScoreboardLine == null ? "" : matchedScoreboardLine;
            this.f7OrM7 = f7OrM7;
        }

        private static Result inDungeon(String reason, String matchedScoreboardLine, boolean f7OrM7) {
            return new Result(true, reason, matchedScoreboardLine, f7OrM7);
        }

        private static Result notInDungeon(String reason, String matchedScoreboardLine, boolean f7OrM7) {
            return new Result(false, reason, matchedScoreboardLine, f7OrM7);
        }
    }

    /** True when the last bounded sidebar scan found a dungeon line. */
    public static boolean inDungeon() {
        return cached.inDungeon;
    }

    /** True only when the cached Catacombs sidebar scan identifies Floor VII or Master Mode VII. */
    public static boolean inF7OrM7() {
        return cached.inDungeon && cached.f7OrM7;
    }

    /** Last detector outcome, including a human-readable reason suitable for the command-only self-test. */
    public static Result cachedResult() {
        return cached;
    }

    /**
     * Runs the established sidebar detector immediately and updates the cache. A missing player, world, sidebar, or
     * matching Catacombs signal is deliberately negative; callers that would replace a vanilla GUI must fail closed.
     */
    public static Result evaluateNow(Minecraft mc) {
        Result result;
        if (mc == null) {
            result = Result.notInDungeon("Minecraft client unavailable", "", false);
        } else if (mc.theWorld == null || mc.thePlayer == null) {
            result = Result.notInDungeon("world or player unavailable", "", false);
        } else {
            result = scan(mc.theWorld.getScoreboard());
        }
        cached = result;
        return result;
    }

    /** Called once per client tick. Resets when the world is gone and otherwise updates only at the established rate. */
    public static void tick(Minecraft mc) {
        if (mc == null || mc.theWorld == null || mc.thePlayer == null) {
            cached = Result.notInDungeon("world or player unavailable", "", false);
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
            return Result.notInDungeon("sidebar scoreboard unavailable", "", false);
        }
        ScoreObjective sidebar = board.getObjectiveInDisplaySlot(1);
        if (sidebar == null) {
            return Result.notInDungeon("sidebar objective unavailable", "", false);
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
            return Result.notInDungeon("sidebar scores unavailable", "", false);
        }
        return evaluateSidebar(sidebar.getDisplayName(), lines);
    }

    /**
     * Pure sidebar decision shared with tests. A title match remains a valid positive Catacombs signal, but a normal
     * Hypixel sidebar reports the exact matched visible line so self-test output explains the GUI-open decision.
     */
    static Result evaluateSidebar(String rawTitle, Iterable<String> rawLines) {
        String title = rawTitle == null ? "" : LeapText.strip(rawTitle);
        boolean f7 = isFloorSevenText(title.toLowerCase(Locale.ROOT));
        String matchedLine = "";
        if (rawLines != null) {
            for (String rawLine : rawLines) {
                if (rawLine == null) continue;
                String clean = LeapText.strip(rawLine);
                String lower = clean.toLowerCase(Locale.ROOT);
                if (isFloorSevenText(lower)) f7 = true;
                if (matchedLine.length() == 0 && isCatacombsLine(clean)) {
                    matchedLine = clean;
                }
            }
        }
        if (matchedLine.length() > 0) {
            return Result.inDungeon("matched Catacombs sidebar line", matchedLine, f7);
        }
        if (isCatacombsLine(title)) {
            return Result.inDungeon("matched Catacombs sidebar title", "", f7);
        }
        return Result.notInDungeon("no Catacombs sidebar title or line", "", f7);
    }

    private static boolean isFloorSevenText(String clean) {
        if (clean == null) {
            return false;
        }
        return clean.matches(".*\\bcatacombs\\s*\\(\\s*f7\\s*\\).*")
                || clean.matches(".*\\bmaster\\s*mode\\s*7\\b.*")
                || clean.matches(".*(?:^|[^a-z0-9])m7(?:$|[^a-z0-9]).*")
                || clean.matches(".*\\bfloor\\s*(?:7|vii)\\b.*");
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
