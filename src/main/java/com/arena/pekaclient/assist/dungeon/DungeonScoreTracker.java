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
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Collects only dungeon values already visible to the player, then feeds the native score calculator. Sidebar and
 * public tab-list rows are sampled at most twice per second; chat is observation-only and is never cancelled or sent
 * by this tracker. It retains the exact latest visible rows for the /svdungeon debug paste-back command.
 */
public final class DungeonScoreTracker {
    private static final int SCAN_INTERVAL_TICKS = 10;
    private static final long END_SCORE_GRACE_MILLIS = 15000L;

    public static final DungeonScoreTracker INSTANCE = new DungeonScoreTracker();

    private final DungeonScoreTelemetry telemetry = new DungeonScoreTelemetry();
    private World activeWorld;
    private int scanTicks;
    private long lastDungeonSeenMillis;
    private volatile DebugSnapshot latestDebug = DebugSnapshot.empty();

    private DungeonScoreTracker() {
    }

    public DungeonScoreReadout readout() {
        return telemetry.readout();
    }

    /** Last normal sampling snapshot, useful to commands without provoking an extra scan. */
    public DebugSnapshot latestDebugSnapshot() {
        return latestDebug;
    }

    /**
     * Captures fresh sidebar/tab rows, evaluates the exact same dungeon context, and observes those rows when valid.
     * This is intentionally command-only; it neither fabricates a score nor sends anything to the server.
     */
    public DebugSnapshot captureDebugSnapshot(Minecraft mc) {
        DungeonContext.Result context = DungeonContext.evaluateNow(mc);
        RawSidebar sidebar = readSidebar(mc == null ? null : mc.theWorld);
        List<String> tab = readTab(mc);
        if (context.inDungeon && mc != null && mc.theWorld != null) {
            beginWorld(mc.theWorld);
            observe(sidebar, tab);
            lastDungeonSeenMillis = System.currentTimeMillis();
        }
        DebugSnapshot snapshot = new DebugSnapshot(context, sidebar.title, sidebar.lines, tab, telemetry.readout());
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
        long now = System.currentTimeMillis();
        if (activeWorld != null && activeWorld != mc.theWorld) {
            // A new world cannot receive the prior run's recap. Drop it rather than carrying it into a lobby.
            clear();
        }
        if (++scanTicks < SCAN_INTERVAL_TICKS) return;
        scanTicks = 0;

        // Do not depend on the slower cache alone. This direct, bounded scan is what lets a Fakepixel-formatted
        // sidebar begin score collection even when it arrived just after DungeonContext's 20-tick refresh.
        DungeonContext.Result context = DungeonContext.evaluateNow(mc);
        if (!context.inDungeon) {
            // Keep observations only long enough to parse a final visible recap arriving immediately after a run.
            if (activeWorld != mc.theWorld || now - lastDungeonSeenMillis > END_SCORE_GRACE_MILLIS) clear();
            return;
        }
        beginWorld(mc.theWorld);
        lastDungeonSeenMillis = now;
        RawSidebar sidebar = readSidebar(mc.theWorld);
        List<String> tab = readTab(mc);
        observe(sidebar, tab);
        latestDebug = new DebugSnapshot(context, sidebar.title, sidebar.lines, tab, telemetry.readout());
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (event == null || event.message == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null || mc.thePlayer == null) return;
        long now = System.currentTimeMillis();
        DungeonContext.Result context = DungeonContext.evaluateNow(mc);
        boolean currentDungeon = context.inDungeon;
        boolean finalScoreGrace = activeWorld == mc.theWorld && now - lastDungeonSeenMillis <= END_SCORE_GRACE_MILLIS;
        if (!currentDungeon && !finalScoreGrace) return;
        if (currentDungeon) {
            beginWorld(mc.theWorld);
            lastDungeonSeenMillis = now;
        }
        telemetry.observeChat(event.message.getUnformattedText());
    }

    private void beginWorld(World world) {
        if (activeWorld == world) return;
        activeWorld = world;
        scanTicks = 0;
        telemetry.reset();
    }

    private void clear() {
        if (activeWorld != null || telemetry.readout().hasData()) telemetry.reset();
        activeWorld = null;
        scanTicks = 0;
        lastDungeonSeenMillis = 0L;
        latestDebug = DebugSnapshot.empty();
    }

    private void observe(RawSidebar sidebar, List<String> tab) {
        if (sidebar.title.length() > 0) telemetry.observeVisible(sidebar.title);
        for (String line : sidebar.lines) telemetry.observeVisible(line);
        for (String line : tab) telemetry.observeVisible(line);
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
                // Keep vanilla's visible formatting for /svdungeon debug; telemetry itself strips it safely.
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
        public final DungeonScoreReadout readout;

        private DebugSnapshot(DungeonContext.Result context, String sidebarTitle, List<String> sidebarLines,
                              List<String> tabLines, DungeonScoreReadout readout) {
            this.context = context;
            this.sidebarTitle = sidebarTitle == null ? "" : sidebarTitle;
            this.sidebarLines = Collections.unmodifiableList(new ArrayList<String>(sidebarLines));
            this.tabLines = Collections.unmodifiableList(new ArrayList<String>(tabLines));
            this.readout = readout;
        }

        private static DebugSnapshot empty() {
            return new DebugSnapshot(DungeonContext.cachedResult(), "", Collections.<String>emptyList(),
                    Collections.<String>emptyList(), new DungeonScoreReadout("", -1, -1, -1, -1, -1,
                            0, 0, false, false, -1, ""));
        }
    }
}
