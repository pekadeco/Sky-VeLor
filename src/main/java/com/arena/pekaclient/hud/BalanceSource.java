package com.arena.pekaclient.hud;

import com.arena.pekaclient.config.AppleSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Where the player's money balance comes from.
 *
 * Auto: reads the "Purse: 1,234,567" line from the sidebar scoreboard. The sidebar is scanned every
 * 10 ticks, not every frame. If the line is not on the sidebar, the value is unknown.
 * Manual: uses a number the player typed in. It is never estimated.
 *
 * An unknown balance is {@link #UNKNOWN}. Callers must show "--" for it, never 0.
 */
public final class BalanceSource {

    public static final long UNKNOWN = -1L;

    private static final Pattern PURSE = Pattern.compile("^\\s*purse\\s*:\\s*([0-9][0-9,.]*)", Pattern.CASE_INSENSITIVE);
    private static final int SCAN_EVERY_TICKS = 10;

    /** True when the balance comes from {@link #manualValue} instead of the scoreboard. */
    public static boolean manualMode;
    public static long manualValue = UNKNOWN;

    private static long detected = UNKNOWN;
    private static int ticksSinceScan;

    private BalanceSource() {
    }

    /** Current balance, or {@link #UNKNOWN}. */
    public static long value() {
        return manualMode ? manualValue : detected;
    }

    /**
     * Called once per client tick. Rescans the scoreboard every 10 ticks (every 20 with the
     * experimental performance mode on).
     */
    public static void tick(Minecraft mc) {
        int every = AppleSettings.experimentalPerformance ? 20 : SCAN_EVERY_TICKS;
        if (++ticksSinceScan < every) {
            return;
        }
        ticksSinceScan = 0;
        detected = scan(mc);
    }

    private static long scan(Minecraft mc) {
        WorldClient world = mc.theWorld;
        if (world == null || mc.thePlayer == null) {
            return UNKNOWN;
        }
        Scoreboard board = world.getScoreboard();
        ScoreObjective sidebar = board.getObjectiveInDisplaySlot(1);
        if (sidebar == null) {
            return UNKNOWN;
        }
        Collection<Score> scores = board.getSortedScores(sidebar);
        for (Score score : scores) {
            ScorePlayerTeam team = board.getPlayersTeam(score.getPlayerName());
            String line = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
            long parsed = parsePurse(line);
            if (parsed != UNKNOWN) {
                return parsed;
            }
        }
        return UNKNOWN;
    }

    /** Parses one sidebar line. Colour codes are removed first. Returns {@link #UNKNOWN} if it is not a purse line. */
    static long parsePurse(String rawLine) {
        if (rawLine == null) {
            return UNKNOWN;
        }
        String line = rawLine.replaceAll("\u00a7.", "");
        Matcher m = PURSE.matcher(line);
        if (!m.find()) {
            return UNKNOWN;
        }
        String digits = m.group(1).replaceAll("[^0-9]", "");
        if (digits.isEmpty() || digits.length() > 15) {
            return UNKNOWN;
        }
        return Long.parseLong(digits);
    }

    /** Accepts a typed character while the manual balance field has focus. Digits and Backspace only. */
    public static boolean acceptsKey(char c, int code) {
        return (c >= '0' && c <= '9') || code == org.lwjgl.input.Keyboard.KEY_BACK;
    }

    /**
     * Switches to manual mode and stores the typed value. Returns false (and changes nothing)
     * when the text has no digits.
     */
    public static boolean applyTyped(String typed) {
        long v = parseManual(typed);
        if (v == UNKNOWN) {
            return false;
        }
        manualValue = v;
        manualMode = true;
        return true;
    }

    /** Parses the typed manual value. Digits only (commas and spaces are ignored). */
    public static long parseManual(String typed) {
        if (typed == null) {
            return UNKNOWN;
        }
        String digits = typed.replaceAll("[^0-9]", "");
        if (digits.isEmpty() || digits.length() > 15) {
            return UNKNOWN;
        }
        return Long.parseLong(digits);
    }
}
