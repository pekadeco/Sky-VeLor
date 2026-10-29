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

import java.util.Collection;

/**
 * Collects only the Catacombs values already visible to the player, then feeds the native score calculator.
 * Sidebar and public tab-list rows are sampled at most twice per second; chat is observation-only and is never
 * cancelled or sent by this tracker.
 */
public final class DungeonScoreTracker {
    private static final int SCAN_INTERVAL_TICKS = 10;
    private static final long END_SCORE_GRACE_MILLIS = 15000L;

    public static final DungeonScoreTracker INSTANCE = new DungeonScoreTracker();

    private final DungeonScoreTelemetry telemetry = new DungeonScoreTelemetry();
    private World activeWorld;
    private int scanTicks;
    private long lastDungeonSeenMillis;

    private DungeonScoreTracker() {
    }

    public DungeonScoreReadout readout() {
        return telemetry.readout();
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
        if (!DungeonContext.inDungeon()) {
            // Keep observations only long enough to parse a final visible recap arriving immediately after a run.
            if (activeWorld != mc.theWorld || now - lastDungeonSeenMillis > END_SCORE_GRACE_MILLIS) clear();
            return;
        }
        lastDungeonSeenMillis = now;
        if (activeWorld != mc.theWorld) {
            activeWorld = mc.theWorld;
            scanTicks = SCAN_INTERVAL_TICKS;
            telemetry.reset();
        }
        if (++scanTicks < SCAN_INTERVAL_TICKS) return;
        scanTicks = 0;
        observeSidebar(mc.theWorld);
        observeTab(mc);
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
            if (activeWorld != mc.theWorld) {
                activeWorld = mc.theWorld;
                telemetry.reset();
            }
            lastDungeonSeenMillis = now;
        }
        telemetry.observeChat(event.message.getUnformattedText());
    }

    private void clear() {
        if (activeWorld != null || telemetry.readout().hasData()) telemetry.reset();
        activeWorld = null;
        scanTicks = 0;
        lastDungeonSeenMillis = 0L;
    }

    private void observeSidebar(World world) {
        if (world == null) return;
        Scoreboard scoreboard = world.getScoreboard();
        if (scoreboard == null) return;
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        if (objective == null) return;
        telemetry.observeVisible(objective.getDisplayName());
        Collection<Score> scores = scoreboard.getSortedScores(objective);
        if (scores == null) return;
        for (Score score : scores) {
            if (score == null || score.getPlayerName() == null || score.getPlayerName().startsWith("#")) continue;
            telemetry.observeVisible(ScorePlayerTeam.formatPlayerName(scoreboard.getPlayersTeam(score.getPlayerName()),
                    score.getPlayerName()));
        }
    }

    /** Reads only public player-info rows; it deliberately does not reflect into tab header/footer internals. */
    private void observeTab(Minecraft mc) {
        if (mc == null || mc.getNetHandler() == null) return;
        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            if (info == null) continue;
            IChatComponent display = info.getDisplayName();
            if (display != null) {
                telemetry.observeVisible(display.getUnformattedText());
                continue;
            }
            if (info.getGameProfile() == null) continue;
            String name = info.getGameProfile().getName();
            if (name == null) continue;
            ScorePlayerTeam team = info.getPlayerTeam();
            telemetry.observeVisible(team == null ? name : ScorePlayerTeam.formatPlayerName(team, name));
        }
    }
}
