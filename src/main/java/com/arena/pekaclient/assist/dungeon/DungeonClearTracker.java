package com.arena.pekaclient.assist.dungeon;

import com.arena.pekaclient.feature.DungeonContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Samples visible dungeon sidebar and public tab rows twice per second. The raw cleared percentage feeds
 * Room Clear Alert, while the captured rows support local parser diagnostics.
 */
public final class DungeonClearTracker {
    private static final int SCAN_INTERVAL_TICKS = 10;
    

    public static final DungeonClearTracker INSTANCE = new DungeonClearTracker();

    private final DungeonClearParser parser = new DungeonClearParser();
    private World activeWorld;
    private int scanTicks;
    private volatile DebugSnapshot latestDebug = DebugSnapshot.empty();

    private DungeonClearTracker() {
    }

    public int clearedPercent() {
        return parser.percent();
    }

    /** Last normal sampling snapshot, useful to commands without provoking an extra scan. */
    public DebugSnapshot latestDebugSnapshot() {
        return latestDebug;
    }

    /**
     * Captures fresh sidebar/tab rows, evaluates the exact same dungeon context, and observes those rows when valid.
     * This is intentionally command-only and sends nothing to the server.
     */
    public DebugSnapshot captureDebugSnapshot(Minecraft mc) {
        RawSidebar sidebar = readSidebar(mc == null ? null : mc.theWorld);
        List<String> tab = readTab(mc);
        DungeonContext.Result context = DungeonContext.evaluateNow(mc, sidebar.title, sidebar.lines, tab);
        if (context.inDungeon && mc != null && mc.theWorld != null) {
            beginWorld(mc.theWorld);
            observe(sidebar, tab);
        }
        DebugSnapshot snapshot = new DebugSnapshot(context, sidebar.title, sidebar.lines, tab, parser.percent());
        latestDebug = snapshot;
        return snapshot;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null || mc.thePlayer == null) {
            clear();
            return;
        }
        if (activeWorld != null && activeWorld != mc.theWorld) {
            // A new world must not inherit the prior run.
            clear();
        }
        if (++scanTicks < SCAN_INTERVAL_TICKS) return;
        scanTicks = 0;

        // Capture both surfaces before deciding. Fakepixel's decisive live Dungeon: marker is in the public tab list,
        // while Dungeon Cleared is in the sidebar, so either source must not be discarded by a sidebar-only gate.
        RawSidebar sidebar = readSidebar(mc.theWorld);
        List<String> tab = readTab(mc);
        DungeonContext.Result context = DungeonContext.evaluateNow(mc, sidebar.title, sidebar.lines, tab);
        if (!context.inDungeon) {
            clear();
            latestDebug = new DebugSnapshot(context, sidebar.title, sidebar.lines, tab, parser.percent());
            return;
        }
        beginWorld(mc.theWorld);
        observe(sidebar, tab);
        latestDebug = new DebugSnapshot(context, sidebar.title, sidebar.lines, tab, parser.percent());
    }

    private void beginWorld(World world) {
        if (activeWorld == world) return;
        activeWorld = world;
        scanTicks = 0;
        parser.reset();
    }

    private void clear() {
        if (activeWorld != null || parser.percent() >= 0) parser.reset();
        activeWorld = null;
        scanTicks = 0;
        latestDebug = DebugSnapshot.empty();
    }

    private void observe(RawSidebar sidebar, List<String> tab) {
        if (sidebar.title.length() > 0) parser.observeSidebar(sidebar.title);
        for (String line : sidebar.lines) parser.observeSidebar(line);
    }

    private static RawSidebar readSidebar(World world) {
        if (world == null) return RawSidebar.EMPTY;
        Scoreboard scoreboard = world.getScoreboard();
        if (scoreboard == null) return RawSidebar.EMPTY;
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        if (objective == null) return RawSidebar.EMPTY;
        List<String> lines = new ArrayList<String>();
        Collection<Score> scores = scoreboard.getSortedScores(objective);
        if (scores != null) {
            for (Score score : scores) {
                if (score == null || score.getPlayerName() == null || score.getPlayerName().startsWith("#")) continue;
                String line = ScorePlayerTeam.formatPlayerName(scoreboard.getPlayersTeam(score.getPlayerName()),
                        score.getPlayerName());
                if (line != null) lines.add(line);
            }
        }
        return new RawSidebar(objective.getDisplayName(), lines);
    }

    /** Reads only public player-info rows; it deliberately does not reflect into tab header/footer internals. */
    private static List<String> readTab(Minecraft mc) {
        if (mc == null || mc.getNetHandler() == null) return Collections.emptyList();
        List<String> result = new ArrayList<String>();
        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            if (info == null) continue;
            IChatComponent display = info.getDisplayName();
            if (display != null) {
                // Keep vanilla's visible formatting for /lunaria dungeon debug; the clear parser strips it safely.
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

    private static final class RawSidebar {
        static final RawSidebar EMPTY = new RawSidebar("", Collections.<String>emptyList());
        final String title;
        final List<String> lines;

        RawSidebar(String title, List<String> lines) {
            this.title = title == null ? "" : title;
            this.lines = lines == null ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(lines));
        }
    }

    /** Immutable command-facing raw capture and parsed readout. */
    public static final class DebugSnapshot {
        public final DungeonContext.Result context;
        public final String sidebarTitle;
        public final List<String> sidebarLines;
        public final List<String> tabLines;
        public final int clearedPercent;

        private DebugSnapshot(DungeonContext.Result context, String sidebarTitle, List<String> sidebarLines,
                              List<String> tabLines, int clearedPercent) {
            this.context = context;
            this.sidebarTitle = sidebarTitle == null ? "" : sidebarTitle;
            this.sidebarLines = Collections.unmodifiableList(new ArrayList<String>(sidebarLines));
            this.tabLines = Collections.unmodifiableList(new ArrayList<String>(tabLines));
            this.clearedPercent = clearedPercent;
        }

        private static DebugSnapshot empty() {
            return new DebugSnapshot(DungeonContext.cachedResult(), "", Collections.<String>emptyList(),
                    Collections.<String>emptyList(), -1);
        }
    }
}
