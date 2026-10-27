package com.arena.pekaclient.assist.dungeon;

import com.arena.pekaclient.feature.DungeonContext;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Collection;

/**
 * Supplies the optional Dungeon Run HUD from the already-visible sidebar only.
 *
 * <p>It scans at most twice a second while the client is in a confirmed dungeon. No packets, tab-list reflection,
 * chat cancellation, inventory access, controller call, or player/entity mutation is used.</p>
 */
public final class DungeonRunTracker {
    private static final int SCAN_INTERVAL_TICKS = 10;

    public static final DungeonRunTracker INSTANCE = new DungeonRunTracker();

    private static volatile boolean registered;

    private final DungeonRunReadout readout = new DungeonRunReadout();
    private World activeWorld;
    private int scanTicks;

    private DungeonRunTracker() {
    }

    /** Records normal Forge registration for the opt-in /velor self-test report. */
    public static void markRegistered() {
        registered = true;
    }

    public static String selfTestStatus() {
        return registered ? "ACTIVE (Forge sidebar reader; " + INSTANCE.readout.diagnostic() + ")" : "UNREGISTERED";
    }

    public DungeonRunReadout readout() {
        return readout;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null || mc.thePlayer == null || !DungeonContext.inDungeon()) {
            clearForWorldChange();
            return;
        }
        if (activeWorld != mc.theWorld) {
            activeWorld = mc.theWorld;
            scanTicks = SCAN_INTERVAL_TICKS;
            readout.reset();
        }
        if (++scanTicks < SCAN_INTERVAL_TICKS) return;
        scanTicks = 0;
        observeSidebar(mc.theWorld);
    }

    private void clearForWorldChange() {
        if (activeWorld != null || readout.hasData()) readout.reset();
        activeWorld = null;
        scanTicks = 0;
    }

    private void observeSidebar(World world) {
        if (world == null) return;
        Scoreboard scoreboard = world.getScoreboard();
        if (scoreboard == null) return;
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        if (objective == null) return;
        readout.observe(objective.getDisplayName());
        Collection<Score> scores = scoreboard.getSortedScores(objective);
        if (scores == null) return;
        for (Score score : scores) {
            if (score == null || score.getPlayerName() == null || score.getPlayerName().startsWith("#")) continue;
            String text = ScorePlayerTeam.formatPlayerName(scoreboard.getPlayersTeam(score.getPlayerName()), score.getPlayerName());
            readout.observe(text);
        }
    }
}
