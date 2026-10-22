package com.arena.pekaclient.assist.sync;

import com.arena.pekaclient.feature.DungeonContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.IChatComponent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Derives a short-lived hashed dungeon session locally. Raw profile names are used only as inputs to
 * {@link DungeonSyncIdentity#sessionId(String, String, Collection)} and are never returned in a Snapshot or placed
 * in any transport payload.
 */
public final class DungeonSyncEnvironment {
    private static final Pattern SERVER = Pattern.compile("(?i)\\b(?:server|instance)\\s*:?\\s*([a-z0-9_-]{3,48})\\b");
    private static final Pattern FLOOR = Pattern.compile("(?i)\\b(?:catacombs\\s*\\(\\s*|floor\\s*|master\\s*mode\\s*)([fm]?[1-7]|vii)\\b");
    private static final Pattern PARTY = Pattern.compile("(?i)\\bparty\\s*\\(\\s*([1-5])\\s*\\)");

    private DungeonSyncEnvironment() {
    }

    public static Snapshot capture(Minecraft mc) {
        if (mc == null || mc.theWorld == null || mc.thePlayer == null || mc.getNetHandler() == null
                || !DungeonContext.inDungeon()) return null;
        List<String> tabText = tabText(mc);
        String server = findServerId(tabText);
        String floor = findFloor(scoreboardText(mc.theWorld.getScoreboard()));
        if (server.length() == 0 || floor.length() == 0) return null;

        ArrayList<String> profileNames = new ArrayList<String>();
        ArrayList<String> classMarked = new ArrayList<String>();
        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            if (info == null || info.getGameProfile() == null) continue;
            String name = info.getGameProfile().getName();
            if (!DungeonSyncIdentity.validName(name) || containsIgnoreCase(profileNames, name)) continue;
            profileNames.add(name);
            String row = visibleRow(info, name);
            if (looksLikeDungeonClassRow(row)) classMarked.add(name);
        }
        int partySize = declaredPartySize(tabText);
        List<String> roster = selectRoster(profileNames, classMarked, mc.thePlayer.getName(), partySize);
        String session = DungeonSyncIdentity.sessionId(server, floor, roster);
        return session.length() == 64 ? new Snapshot(session, server, floor, roster.size()) : null;
    }

    /** Strict server extraction exposed for headless regression tests. */
    public static String findServerId(Iterable<String> rows) {
        if (rows == null) return "";
        for (String raw : rows) {
            String text = plain(raw);
            Matcher match = SERVER.matcher(text);
            if (match.find()) {
                String id = match.group(1).toLowerCase(Locale.ROOT);
                if (DungeonSyncIdentity.validServerId(id)) return id;
            }
        }
        return "";
    }

    /** Returns F1–F7/M1–M7 only when a score/tab line explicitly names a dungeon floor. */
    public static String findFloor(Iterable<String> rows) {
        if (rows == null) return "";
        for (String raw : rows) {
            String text = plain(raw);
            Matcher match = FLOOR.matcher(text);
            if (!match.find()) continue;
            String token = match.group(1).toUpperCase(Locale.ROOT);
            boolean master = text.toLowerCase(Locale.ROOT).indexOf("master mode") >= 0;
            if ("VII".equals(token)) return master ? "M7" : "F7";
            if (token.length() == 1 && token.charAt(0) >= '1' && token.charAt(0) <= '7') {
                return (master ? "M" : "F") + token;
            }
            if (DungeonSyncIdentity.validFloor(token)) return token;
        }
        return "";
    }

    private static List<String> selectRoster(List<String> profiles, List<String> marked, String localName, int declared) {
        if (declared >= 2 && declared <= 5) {
            if (profiles.size() == declared && containsIgnoreCase(profiles, localName)) return profiles;
            if (marked.size() == declared && containsIgnoreCase(marked, localName)) return marked;
            // A local row can omit its class badge while every teammate is class-marked. Add only the exact local
            // profile in that narrow declared-size case; never fill a roster with unrelated tab entries.
            if (marked.size() == declared - 1 && DungeonSyncIdentity.validName(localName)
                    && containsIgnoreCase(profiles, localName) && !containsIgnoreCase(marked, localName)) {
                ArrayList<String> withLocal = new ArrayList<String>(marked);
                withLocal.add(localName);
                return withLocal;
            }
        }
        // Some Fakepixel tab layouts expose exactly the five party profile rows without an explicit Party marker.
        if (profiles.size() >= 2 && profiles.size() <= 5 && containsIgnoreCase(profiles, localName)) return profiles;
        if (marked.size() >= 2 && marked.size() <= 5 && containsIgnoreCase(marked, localName)) return marked;
        return java.util.Collections.emptyList();
    }

    private static int declaredPartySize(List<String> rows) {
        for (String raw : rows) {
            Matcher match = PARTY.matcher(plain(raw));
            if (match.find()) return Integer.parseInt(match.group(1));
        }
        return 0;
    }

    private static List<String> tabText(Minecraft mc) {
        ArrayList<String> result = new ArrayList<String>();
        if (mc != null && mc.getNetHandler() != null) {
            for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                if (info == null || info.getGameProfile() == null) continue;
                String name = info.getGameProfile().getName();
                result.add(visibleRow(info, name));
            }
        }
        // Header/footer names differ under MCP/SRG/notch. Reading only IChatComponent fields is bounded and optional.
        try {
            Object tab = mc == null || mc.ingameGUI == null ? null : mc.ingameGUI.getTabList();
            if (tab != null) {
                for (Field field : tab.getClass().getDeclaredFields()) {
                    if (!IChatComponent.class.isAssignableFrom(field.getType())) continue;
                    field.setAccessible(true);
                    Object component = field.get(tab);
                    if (component instanceof IChatComponent) result.add(((IChatComponent) component).getUnformattedText());
                }
            }
        } catch (RuntimeException | IllegalAccessException ignored) {
            // Sync remains disabled for this run if no public tab row exposes an instance ID.
        }
        return result;
    }

    private static String visibleRow(NetworkPlayerInfo info, String fallback) {
        if (info == null) return fallback == null ? "" : fallback;
        IChatComponent display = info.getDisplayName();
        if (display != null) return display.getUnformattedText();
        ScorePlayerTeam team = info.getPlayerTeam();
        return team == null ? (fallback == null ? "" : fallback) : ScorePlayerTeam.formatPlayerName(team, fallback);
    }

    private static List<String> scoreboardText(Scoreboard board) {
        ArrayList<String> result = new ArrayList<String>();
        if (board == null) return result;
        ScoreObjective objective = board.getObjectiveInDisplaySlot(1);
        if (objective == null) return result;
        result.add(objective.getDisplayName());
        for (Score score : board.getSortedScores(objective)) {
            if (score == null) continue;
            String holder = score.getPlayerName();
            if (holder == null || holder.startsWith("#")) continue;
            result.add(ScorePlayerTeam.formatPlayerName(board.getPlayersTeam(holder), holder));
        }
        return result;
    }

    private static boolean looksLikeDungeonClassRow(String row) {
        String value = plain(row).toLowerCase(Locale.ROOT);
        return value.indexOf("mage") >= 0 || value.indexOf("healer") >= 0 || value.indexOf("archer") >= 0
                || value.indexOf("berserk") >= 0 || value.indexOf("tank") >= 0;
    }

    private static boolean containsIgnoreCase(List<String> values, String wanted) {
        if (wanted == null) return false;
        for (String value : values) if (value != null && value.equalsIgnoreCase(wanted)) return true;
        return false;
    }

    private static String plain(String raw) {
        if (raw == null) return "";
        StringBuilder result = new StringBuilder(raw.length());
        for (int index = 0; index < raw.length(); index++) {
            if (raw.charAt(index) == '\u00a7' && index + 1 < raw.length()) index++;
            else result.append(raw.charAt(index));
        }
        return result.toString().trim();
    }

    public static final class Snapshot {
        public final String sessionId;
        public final String serverId;
        public final String floor;
        public final int partySize;

        Snapshot(String sessionId, String serverId, String floor, int partySize) {
            this.sessionId = sessionId;
            this.serverId = serverId;
            this.floor = floor;
            this.partySize = partySize;
        }
    }
}
