package com.arena.pekaclient.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;

/**
 * Shared runtime context for SkyBlock-only client features. The sidebar title is the authority; server address
 * alone is deliberately insufficient because a server can host both SkyBlock and non-SkyBlock modes.
 */
public final class SkyblockContext {
    private static volatile boolean inSkyblock;

    private SkyblockContext() { }

    /** Refresh once per client tick, so render hooks do not rescan the scoreboard for every inventory slot. */
    public static void tick(Minecraft minecraft) {
        inSkyblock = detect(minecraft);
    }

    /** Cached client-tick result; safe to read from render and outbound-packet paths. */
    public static boolean isInSkyblock() {
        return inSkyblock;
    }

    /** Live scoreboard check for client-thread input paths where a stale context could affect item movement. */
    public static boolean isInSkyblockNow() {
        return detect(Minecraft.getMinecraft());
    }

    public static void reset() {
        inSkyblock = false;
    }

    private static boolean detect(Minecraft minecraft) {
        if (minecraft == null || minecraft.thePlayer == null || minecraft.theWorld == null) return false;
        Scoreboard scoreboard = minecraft.theWorld.getScoreboard();
        if (scoreboard == null) return false;
        ScoreObjective sidebar = scoreboard.getObjectiveInDisplaySlot(1);
        return sidebar != null && SkyblockContextPolicy.isSkyblockScoreboardTitle(sidebar.getDisplayName());
    }
}
