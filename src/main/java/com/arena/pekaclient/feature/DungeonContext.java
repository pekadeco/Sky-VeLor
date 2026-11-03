package com.arena.pekaclient.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.IChatComponent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conservative, shared dungeon-context detector.
 *
 * <p>Fakepixel presents complementary evidence in two visible surfaces: Catacombs/Dungeon Cleared in the sidebar and
 * a public {@code Dungeon:} row in the player tab list. Both are evaluated in the same bounded decision so a valid
 * tab-led dungeon does not get discarded before score/map/alert consumers can see it. It never inspects private tab
 * headers/footers, packets, or server data.</p>
 */
public final class DungeonContext {

    static final int SCAN_EVERY_TICKS = 20;
    private static final Pattern FLOOR_ID = Pattern.compile("(?i)\\b([efm])\\s*([0-7])\\b");
    private static final Pattern FLOOR_TOKEN = Pattern.compile(
            "(?i)(?:\\bmaster\\s*mode\\s*|\\b(?:floor|f|m)\\s*)([1-7])\\b");
    private static final Pattern ROMAN_FLOOR_SEVEN = Pattern.compile(
            "(?i)\\b(?:master\\s*mode|floor)\\s*(?:7|vii)\\b");
    private static final Pattern TAB_DUNGEON = Pattern.compile("(?i)^dungeon\\s*:\\s*(.*)$");
    private static final int METRIC_CLEARED = 1;
    private static final int METRIC_SECRETS = 2;
    private static final int METRIC_CRYPTS = 4;
    private static final int METRIC_DEATHS = 8;
    private static final int METRIC_TIME = 16;

    private static int ticksSinceScan = SCAN_EVERY_TICKS;
    private static volatile Result cached = Result.notInDungeon("not evaluated", "", false, "", "");

    private DungeonContext() {
    }

    /** Immutable diagnostic from the same visible-data detector used by every dungeon feature. */
    public static final class Result {
        public final boolean inDungeon;
        public final String reason;
        /** The visible sidebar or public tab row that made the detector accept the run. */
        public final String matchedScoreboardLine;
        /** Human-readable detector name: Catacombs for direct matches, otherwise Dungeon. */
        public final String dungeon;
        /** Parsed visible floor token such as F5 or M7, or an empty string when visible sources omitted it. */
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

    /** True when the last bounded sidebar/tab scan found corroborated dungeon evidence. */
    public static boolean inDungeon() {
        return cached.inDungeon;
    }

    /**
     * Performs the same conservative visible-data scan immediately for screen/chat decisions that cannot wait for the
     * regular tick cache. Kept as a compatibility entry point for the Chat QoL integration.
     */
    public static boolean isInDungeonNow() {
        return evaluateNow(Minecraft.getMinecraft()).inDungeon;
    }

    /** True only when the cached visible dungeon evidence identifies Floor VII or Master Mode VII. */
    public static boolean inF7OrM7() {
        return cached.inDungeon && cached.f7OrM7;
    }

    /** Last detector outcome, including a human-readable reason suitable for live diagnostics. */
    public static Result cachedResult() {
        return cached;
    }

    /**
     * Runs the visible-data detector immediately and updates the cache. A missing player, world, sidebar, or
     * corroborated dungeon signal is deliberately negative; callers that would replace a vanilla GUI must fail closed.
     */
    public static Result evaluateNow(Minecraft mc) {
        Result result;
        if (mc == null) {
            result = Result.notInDungeon("Minecraft client unavailable", "", false, "", "");
        } else if (mc.theWorld == null || mc.thePlayer == null) {
            result = Result.notInDungeon("world or player unavailable", "", false, "", "");
        } else {
            result = scan(mc.theWorld.getScoreboard(), readPublicTab(mc));
        }
        cached = result;
        return result;
    }

    /**
     * Evaluates a just-captured set of rows and updates the shared cache. The score tracker uses this overload so
     * /lunaria dungeon debug, detection, and parser ingestion all describe exactly the same visible snapshot.
     */
    public static Result evaluateNow(Minecraft mc, String rawSidebarTitle, Iterable<String> rawSidebarLines,
                                     Iterable<String> rawTabLines) {
        Result result;
        if (mc == null) {
            result = Result.notInDungeon("Minecraft client unavailable", "", false, "", "");
        } else if (mc.theWorld == null || mc.thePlayer == null) {
            result = Result.notInDungeon("world or player unavailable", "", false, "", "");
        } else {
            result = evaluateVisible(rawSidebarTitle, rawSidebarLines, rawTabLines);
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
        if (++ticksSinceScan < SCAN_EVERY_TICKS) return;
        ticksSinceScan = 0;
        evaluateNow(mc);
    }

    private static Result scan(Scoreboard board, Iterable<String> rawTabLines) {
        if (board == null) {
            return Result.notInDungeon("sidebar scoreboard unavailable", "", false, "", "");
        }
        ScoreObjective sidebar = board.getObjectiveInDisplaySlot(1);
        if (sidebar == null) {
            return Result.notInDungeon("sidebar objective unavailable", "", false, "", "");
        }
        List<String> lines = new ArrayList<String>();
        Collection<Score> scores = board.getSortedScores(sidebar);
        if (scores == null) {
            return Result.notInDungeon("sidebar scores unavailable", "", false, "", "");
        }
        for (Score score : scores) {
            if (score == null) continue;
            String holder = score.getPlayerName();
            String line = ScorePlayerTeam.formatPlayerName(board.getPlayersTeam(holder), holder);
            if (line != null) lines.add(line);
        }
        return evaluateVisible(sidebar.getDisplayName(), lines, rawTabLines);
    }

    /**
     * Pure decision shared with tests, the tracker, and debug tooling. Direct Catacombs wording wins immediately.
     * A public tab {@code Dungeon:} row is accepted only alongside recognizable SkyBlock/Fakepixel sidebar context;
     * generic sidebars retain the older floor/metric corroboration requirements and fail closed otherwise.
     */
    public static Result evaluateVisible(String rawTitle, Iterable<String> rawSidebarLines,
                                         Iterable<String> rawTabLines) {
        String title = clean(rawTitle);
        List<String> sidebar = cleaned(rawSidebarLines);
        List<String> tab = cleaned(rawTabLines);
        List<String> sidebarEvidence = new ArrayList<String>();
        if (title.length() > 0) sidebarEvidence.add(title);
        sidebarEvidence.addAll(sidebar);

        String catacombsLine = "";
        String genericDungeonLine = "";
        String firstFloorLine = "";
        String sidebarFloor = "";
        int metrics = 0;
        for (String line : sidebarEvidence) {
            String lower = line.toLowerCase(Locale.ROOT);
            String foundFloor = floorIn(lower);
            if (sidebarFloor.length() == 0 && foundFloor.length() > 0) {
                sidebarFloor = foundFloor;
                firstFloorLine = line;
            }
            if (catacombsLine.length() == 0 && isCatacombsLine(line)) catacombsLine = line;
            if (genericDungeonLine.length() == 0 && containsDungeonLabel(lower)) genericDungeonLine = line;
            metrics |= metricBits(lower);
        }

        String tabDungeonLine = "";
        String tabFloor = "";
        for (String line : tab) {
            Matcher dungeon = TAB_DUNGEON.matcher(line);
            if (!dungeon.matches()) continue;
            if (tabDungeonLine.length() == 0) tabDungeonLine = line;
            String foundFloor = floorIn(dungeon.group(1));
            if (tabFloor.length() == 0 && foundFloor.length() > 0) tabFloor = foundFloor;
        }

        String floor = tabFloor.length() > 0 ? tabFloor : sidebarFloor;
        boolean f7 = "F7".equals(floor) || "M7".equals(floor) || containsFloorSeven(sidebarEvidence)
                || containsFloorSeven(tab);
        if (catacombsLine.length() > 0) {
            return Result.inDungeon("matched Catacombs sidebar line or title", catacombsLine, f7,
                    "Catacombs", floor);
        }

        int metricCount = Integer.bitCount(metrics);
        boolean recognizableServer = containsSkyblockContext(title) || containsSkyblockContext(sidebarEvidence);
        if (tabDungeonLine.length() > 0 && (recognizableServer || sidebarFloor.length() > 0 && metricCount >= 1)) {
            return Result.inDungeon("matched Fakepixel public tab Dungeon row with sidebar context", tabDungeonLine,
                    f7, "Dungeon", floor);
        }
        if (genericDungeonLine.length() > 0 && (sidebarFloor.length() > 0 || metricCount >= 1)) {
            return Result.inDungeon("matched corroborated Dungeon sidebar evidence", genericDungeonLine, f7,
                    "Dungeon", floor);
        }
        if (sidebarFloor.length() > 0 && metricCount >= 2) {
            return Result.inDungeon("matched floor plus visible dungeon metrics", firstFloorLine, f7,
                    "Dungeon", floor);
        }
        if (metricCount >= 3) {
            return Result.inDungeon("matched multiple visible dungeon metrics", firstMetricLine(sidebarEvidence), f7,
                    "Dungeon", floor);
        }
        return Result.notInDungeon("no corroborated dungeon sidebar/tab evidence", "", f7, "", floor);
    }

    /** Compatibility pure sidebar decision used by existing integrations/tests. */
    static Result evaluateSidebar(String rawTitle, Iterable<String> rawLines) {
        return evaluateVisible(rawTitle, rawLines, Collections.<String>emptyList());
    }

    private static List<String> readPublicTab(Minecraft mc) {
        if (mc == null || mc.getNetHandler() == null) return Collections.emptyList();
        List<String> result = new ArrayList<String>();
        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            if (info == null) continue;
            IChatComponent display = info.getDisplayName();
            if (display != null) {
                result.add(display.getFormattedText());
                continue;
            }
            if (info.getGameProfile() == null) continue;
            String name = info.getGameProfile().getName();
            if (name == null) continue;
            ScorePlayerTeam team = info.getPlayerTeam();
            result.add(team == null ? name : ScorePlayerTeam.formatPlayerName(team, name));
        }
        return result;
    }

    private static List<String> cleaned(Iterable<String> rows) {
        List<String> result = new ArrayList<String>();
        if (rows == null) return result;
        for (String row : rows) {
            String line = clean(row);
            if (line.length() > 0) result.add(line);
        }
        return result;
    }

    private static boolean containsSkyblockContext(String title) {
        String lower = title == null ? "" : title.toLowerCase(Locale.ROOT);
        return lower.indexOf("skyblock") >= 0 || lower.indexOf("fakepixel") >= 0;
    }

    private static boolean containsSkyblockContext(List<String> lines) {
        for (String line : lines) if (containsSkyblockContext(line)) return true;
        return false;
    }

    private static String firstMetricLine(List<String> visible) {
        for (String line : visible) if (metricBits(line.toLowerCase(Locale.ROOT)) != 0) return line;
        return "";
    }

    private static boolean containsFloorSeven(List<String> visible) {
        for (String line : visible) if (ROMAN_FLOOR_SEVEN.matcher(line).find()) return true;
        return false;
    }

    private static String floorIn(String raw) {
        if (raw == null) return "";
        Matcher explicit = FLOOR_ID.matcher(raw);
        if (explicit.find()) {
            String floor = explicit.group(1).toUpperCase(Locale.ROOT) + explicit.group(2);
            // E0/F0 are valid calculator keys; no unrelated single-letter digits are accepted.
            if ("E0".equals(floor) || "F0".equals(floor) || (floor.charAt(1) >= '1' && floor.charAt(1) <= '7')) {
                return floor;
            }
        }
        Matcher floor = FLOOR_TOKEN.matcher(raw);
        if (!floor.find()) return "";
        String token = floor.group(0).toLowerCase(Locale.ROOT);
        return token.indexOf("master") >= 0 || token.matches(".*\\bm\\s*[1-7].*")
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
        if (rawLine == null) return false;
        String line = LeapText.strip(rawLine).toLowerCase(Locale.ROOT);
        return line.contains("catacombs");
    }
}
