package com.arena.pekaclient.dungeon;

import com.arena.pekaclient.dungeon.sync.DungeonSyncIdentity;
import com.arena.pekaclient.dungeon.sync.DungeonSyncClassResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StringUtils;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Derives a temporary sync identity from the tab-list server ID and exact dungeon-party names.
 * Reads usernames only; it does not require Mojang authentication or use player UUIDs, which
 * keeps the roster compatible with offline-mode servers when their Tab List exposes usernames.
 */
public final class DungeonPuzzleSyncEnvironment {
    private static String lastCaptureFailure = "scoreboard data is not ready";

    private DungeonPuzzleSyncEnvironment() { }

    public static Snapshot capture(Minecraft mc) {
        if (mc == null || mc.theWorld == null || mc.thePlayer == null || mc.getNetHandler() == null)
            return fail("client world, player, or network handler is not ready");
        Scoreboard board = mc.theWorld.getScoreboard();
        if (board == null) return fail("scoreboard is unavailable");

        List<String> sidebar = sidebarLines(board);
        List<String> scoreboardSources = new ArrayList<String>(sidebar);
        ScoreObjective sidebarObjective = board.getObjectiveInDisplaySlot(1);
        if (sidebarObjective != null) scoreboardSources.add(sidebarObjective.getDisplayName());

        List<NetworkPlayerInfo> tabInfo = new ArrayList<NetworkPlayerInfo>(mc.getNetHandler().getPlayerInfoMap());
        List<String> tabNames = new ArrayList<String>();
        List<String> tabRows = new ArrayList<String>();
        List<String> visiblePlayerNames = new ArrayList<String>();
        for (NetworkPlayerInfo info : tabInfo) {
            if (info == null || info.getGameProfile() == null || info.getGameProfile().getName() == null) continue;
            String profileName = info.getGameProfile().getName();
            tabNames.add(profileName);
            IChatComponent display = info.getDisplayName();
            if (display != null) {
                String visibleRow = display.getFormattedText();
                tabRows.add(StringUtils.stripControlCodes(visibleRow));
                if (DungeonSyncIdentity.isVisibleProfileRow(visibleRow, profileName)) visiblePlayerNames.add(profileName);
            } else {
                ScorePlayerTeam infoTeam = info.getPlayerTeam();
                if (infoTeam != null) {
                    String visibleRow = ScorePlayerTeam.formatPlayerName(infoTeam, profileName);
                    tabRows.add(StringUtils.stripControlCodes(visibleRow));
                    if (DungeonSyncIdentity.isVisibleProfileRow(visibleRow, profileName)
                            && !profileName.equals(StringUtils.stripControlCodes(visibleRow).trim())) {
                        visiblePlayerNames.add(profileName);
                    }
                }
            }
            ScorePlayerTeam infoTeam = info.getPlayerTeam();
            if (infoTeam != null) tabRows.add(StringUtils.stripControlCodes(ScorePlayerTeam.formatPlayerName(infoTeam, profileName)));
            ScorePlayerTeam boardTeam = board.getPlayersTeam(profileName);
            if (boardTeam != null) tabRows.add(StringUtils.stripControlCodes(ScorePlayerTeam.formatPlayerName(boardTeam, profileName)));
        }

        // The Tab List supplies the server instance ID and profile usernames; the scoreboard supplies the floor.
        List<String> identitySources = tabListSources(mc, tabInfo);
        String serverId = DungeonSyncIdentity.findServerId(identitySources);
        if (serverId == null) return fail("server ID was not found in the tab list");
        String floor = DungeonSyncIdentity.findFloor(scoreboardSources);
        if (floor == null) return fail("dungeon floor was not found on the scoreboard");

        // Fakepixel's Tab List is a custom multi-section table. Its profile collection can contain
        // synthetic rows, so identify actual player entries by checking whether each profile's own
        // display row contains that exact username. Never infer usernames from ranks or class text.
        List<String> classMarkedNames = new ArrayList<String>();
        for (String profileName : tabNames) {
            if (!DungeonSyncIdentity.isValidUsername(profileName)) continue;
            String resolvedClass = DungeonSyncClassResolver.resolve(scoreboardSources, tabRows, profileName, tabNames);
            if (!"UNKNOWN".equalsIgnoreCase(resolvedClass)) classMarkedNames.add(profileName);
        }
        List<String> rosterSources = new ArrayList<String>(identitySources);
        rosterSources.addAll(tabRows);
        int expectedPartySize = DungeonSyncIdentity.findPartySize(rosterSources);
        if (expectedPartySize < 2 || expectedPartySize > 5) {
            return fail(expectedPartySize == 1
                    ? "the scoreboard/Tab List declares Party (1); shared Dungeon Sync requires at least two players"
                    : "a consistent Party (2)–Party (5) size was not found in the scoreboard/Tab List");
        }
        List<String> partyNames = DungeonSyncIdentity.buildTabListRoster(tabNames,
                visiblePlayerNames, classMarkedNames, mc.thePlayer.getName(), expectedPartySize);
        if (partyNames.isEmpty()) {
            boolean localPresent = false;
            for (String name : tabNames) if (name != null && name.equalsIgnoreCase(mc.thePlayer.getName())) localPresent = true;
            boolean localClassMarked = false;
            for (String name : classMarkedNames) if (name != null && name.equalsIgnoreCase(mc.thePlayer.getName())) localClassMarked = true;
            int validProfiles = DungeonSyncIdentity.countValidTabNames(tabNames);
            return fail("Tab List roster rejected (profiles=" + validProfiles
                    + ", visible-player-rows=" + visiblePlayerNames.size()
                    + ", declared-party-size=" + expectedPartySize
                    + ", class-marked=" + classMarkedNames.size()
                    + ", local-player-present=" + localPresent
                    + ", local-class-marked=" + localClassMarked
                    + "); need the exact declared party roster from Tab List profile rows. No raw usernames were sent");
        }

        String sessionId = DungeonSyncIdentity.createNamedSessionId(serverId, floor, partyNames);
        if (sessionId == null) return fail("session identity could not be built from the selected profile usernames");
        String rosterSource = selectedRosterSource(partyNames, visiblePlayerNames, classMarkedNames,
                mc.thePlayer.getName(), expectedPartySize);
        lastCaptureFailure = null;
        return new Snapshot(sessionId, serverId, floor, partyNames.size(), expectedPartySize, rosterSource,
                tabNames.size(), DungeonSyncIdentity.countValidTabNames(tabNames),
                visiblePlayerNames.size(), classMarkedNames.size());
    }

    private static String selectedRosterSource(List<String> selected, List<String> visiblePlayers,
                                               List<String> classMarkedNames, String localName,
                                               int expectedPartySize) {
        if (sameRoster(selected, visiblePlayers)) return "tab-display-rows";
        if (sameRoster(selected, classMarkedNames)) return "class-marked-rows";
        if (selected != null && localName != null && expectedPartySize == selected.size()
                && selected.size() >= 2 && containsName(selected, localName)
                && !containsName(classMarkedNames, localName)
                && classMarkedNames != null && classMarkedNames.size() == selected.size() - 1
                && containsAllSelected(classMarkedNames, selected)) {
            return "local-profile+class-marked-others";
        }
        return "profile-names";
    }

    private static boolean sameRoster(List<String> selected, List<String> candidates) {
        return selected != null && candidates != null && selected.size() == candidates.size()
                && containsAllSelected(selected, candidates);
    }

    private static boolean containsName(List<String> names, String wanted) {
        if (names == null || wanted == null) return false;
        for (String name : names) if (name != null && name.equalsIgnoreCase(wanted)) return true;
        return false;
    }

    private static boolean containsAllSelected(List<String> selected, List<String> candidates) {
        if (selected == null || candidates == null || selected.isEmpty()) return false;
        for (String name : selected) {
            if (!containsName(candidates, name)) return false;
        }
        return true;
    }

    public static String getLastCaptureFailure() {
        return lastCaptureFailure == null ? "scoreboard identity is temporarily unavailable" : lastCaptureFailure;
    }

    private static Snapshot fail(String reason) {
        lastCaptureFailure = reason;
        return null;
    }

    private static List<String> sidebarLines(Scoreboard board) {
        List<String> lines = new ArrayList<String>();
        ScoreObjective objective = board.getObjectiveInDisplaySlot(1);
        if (objective == null) return lines;
        for (Score score : board.getSortedScores(objective)) {
            if (score == null || score.getPlayerName() == null || score.getPlayerName().startsWith("#")) continue;
            String raw = ScorePlayerTeam.formatPlayerName(board.getPlayersTeam(score.getPlayerName()), score.getPlayerName());
            lines.add(StringUtils.stripControlCodes(raw));
        }
        return lines;
    }

    private static List<String> tabListSources(Minecraft mc, List<NetworkPlayerInfo> tabInfo) {
        List<String> sources = new ArrayList<String>();
        if (tabInfo != null) {
            for (NetworkPlayerInfo info : tabInfo) {
                if (info == null || info.getGameProfile() == null) continue;
                String name = info.getGameProfile().getName();
                if (name == null) continue;
                IChatComponent display = info.getDisplayName();
                sources.add(display != null ? display.getFormattedText() : name);
                String teamDisplay = ScorePlayerTeam.formatPlayerName(info.getPlayerTeam(), name);
                if (teamDisplay != null) sources.add(teamDisplay);
            }
        }

        // In Minecraft 1.8.9 the tab header/footer are private fields on GuiPlayerTabOverlay.
        // Read IChatComponent fields by type so reobfuscation does not depend on field names.
        if (mc != null && mc.ingameGUI != null && mc.ingameGUI.getTabList() != null) {
            Object overlay = mc.ingameGUI.getTabList();
            for (Field field : overlay.getClass().getDeclaredFields()) {
                if (!IChatComponent.class.isAssignableFrom(field.getType())) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(overlay);
                    if (value instanceof IChatComponent) {
                        String text = ((IChatComponent) value).getUnformattedText();
                        if (text != null && !text.trim().isEmpty()) sources.add(text);
                    }
                } catch (Exception ignored) {
                    // Header/footer are optional; a missing field should not stop local map use.
                }
            }
        }
        return sources;
    }

    public static String currentFloor(Minecraft mc) {
        if (mc == null || mc.theWorld == null) return null;
        Scoreboard board = mc.theWorld.getScoreboard();
        if (board == null) return null;
        List<String> sources = new ArrayList<String>(sidebarLines(board));
        ScoreObjective objective = board.getObjectiveInDisplaySlot(1);
        if (objective != null) sources.add(objective.getDisplayName());
        return DungeonSyncIdentity.findFloor(sources);
    }

    public static String currentServerId(Minecraft mc) {
        if (mc == null || mc.thePlayer == null || mc.getNetHandler() == null) return null;
        List<NetworkPlayerInfo> tabInfo = new ArrayList<NetworkPlayerInfo>(mc.getNetHandler().getPlayerInfoMap());
        return DungeonSyncIdentity.findServerId(tabListSources(mc, tabInfo));
    }

    public static final class Snapshot {
        public final String sessionId;
        public final String serverId;
        public final String floor;
        public final int partySize;
        public final int declaredPartySize;
        public final String rosterSource;
        public final int tabProfileCount;
        public final int validProfileCount;
        public final int visiblePlayerCount;
        public final int classMarkedCount;

        Snapshot(String sessionId, String serverId, String floor, int partySize, int declaredPartySize,
                 String rosterSource, int tabProfileCount, int validProfileCount,
                 int visiblePlayerCount, int classMarkedCount) {
            this.sessionId = sessionId;
            this.serverId = serverId;
            this.floor = floor;
            this.partySize = partySize;
            this.declaredPartySize = declaredPartySize;
            this.rosterSource = rosterSource;
            this.tabProfileCount = tabProfileCount;
            this.validProfileCount = validProfileCount;
            this.visiblePlayerCount = visiblePlayerCount;
            this.classMarkedCount = classMarkedCount;
        }

        public boolean sameAs(Snapshot other) {
            return other != null && sessionId.equals(other.sessionId)
                    && serverId.equals(other.serverId) && floor.equals(other.floor)
                    && partySize == other.partySize;
        }
    }
}
