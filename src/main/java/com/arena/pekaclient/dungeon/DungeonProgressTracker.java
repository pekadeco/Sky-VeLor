package com.arena.pekaclient.dungeon;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.feature.DungeonContext;
import com.arena.pekaclient.feature.SkyblockContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.StringUtils;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.lang.reflect.Field;

/**
 * Client-only state for Lunaria's dungeon progress widgets and small dungeon conveniences.
 * It reads visible scoreboard/chat data for the secrets HUD and retained dungeon conveniences.
 */
public final class DungeonProgressTracker {
    private static final String BLOOD_READY = "the watcher: that will be enough for now";

    private final DungeonSecrets secrets = new DungeonSecrets();
    private String floor = "--";
    private long scanTicks;
    private boolean wasInDungeon;
    private boolean bloodReadyShown;
    private final List<String> cachedLines = new ArrayList<String>();

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        boolean inDungeon = DungeonContext.inDungeon() && SkyblockContext.isInSkyblock()
                && mc.thePlayer != null && mc.theWorld != null;
        if (!inDungeon) {
            if (wasInDungeon) resetRun();
            wasInDungeon = false;
            return;
        }
        if (!wasInDungeon) {
            resetRun();
            wasInDungeon = true;
        }
        if (++scanTicks >= 20L) {
            scanTicks = 0L;
            scanScoreboard(mc);
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    public void onChat(ClientChatReceivedEvent event) {
        if (event == null || event.message == null || !SkyblockContext.isInSkyblock() || !DungeonContext.inDungeon()) return;
        String raw = event.message.getUnformattedText();
        String clean = StringUtils.stripControlCodes(raw);
        if (clean == null) clean = raw == null ? "" : raw;
        String lower = clean.toLowerCase(Locale.ROOT);

        if (AppleSettings.dungeonHideUnableToLocateSign && lower.contains("unable to locate sign")) {
            event.setCanceled(true);
            return;
        }
        if (lower.contains(BLOOD_READY) && AppleSettings.dungeonBloodReadyTitle && !bloodReadyShown) {
            bloodReadyShown = true;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.ingameGUI != null) {
                mc.ingameGUI.displayTitle("§cBLOOD READY!", "§bLunaria • Dungeon", 3, 50, 8);
            }
        }
        if (lower.contains("you found a secret") || lower.contains("found a secret!")) secrets.foundSecret();
        secrets.observe(clean);
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event != null && event.world != null && event.world.isRemote) {
            resetRun();
            wasInDungeon = false;
        }
    }

    private void scanScoreboard(Minecraft mc) {
        if (mc == null || mc.theWorld == null) return;
        cachedLines.clear();
        Scoreboard board = mc.theWorld.getScoreboard();
        if (board != null) {
            ScoreObjective sidebar = board.getObjectiveInDisplaySlot(1);
            if (sidebar != null) {
                String title = ScoreboardText.clean(sidebar.getDisplayName());
                if (title != null && !title.isEmpty()) cachedLines.add(title);
                for (Score score : board.getSortedScores(sidebar)) {
                    if (score == null || score.getPlayerName() == null || score.getPlayerName().startsWith("#")) continue;
                    String visible = ScorePlayerTeam.formatPlayerName(board.getPlayersTeam(score.getPlayerName()), score.getPlayerName());
                    String clean = ScoreboardText.clean(visible);
                    if (clean != null && !clean.isEmpty()) cachedLines.add(clean);
                }
            }
        }
        // Fakepixel exposes most dungeon statistics in tab-list rows/header/footer, not on the sidebar.
        if (mc.getNetHandler() != null) {
            for (Object value : mc.getNetHandler().getPlayerInfoMap()) {
                if (!(value instanceof NetworkPlayerInfo)) continue;
                NetworkPlayerInfo info = (NetworkPlayerInfo) value;
                IChatComponent display = info.getDisplayName();
                if (display != null) addLines(display.getUnformattedText());
                if (info.getGameProfile() != null) {
                    String teamLine = ScorePlayerTeam.formatPlayerName(info.getPlayerTeam(), info.getGameProfile().getName());
                    addLines(teamLine);
                }
            }
        }
        if (mc.ingameGUI != null) {
            GuiPlayerTabOverlay tab = mc.ingameGUI.getTabList();
            if (tab != null) {
                for (Field field : tab.getClass().getDeclaredFields()) {
                    if (!IChatComponent.class.isAssignableFrom(field.getType())) continue;
                    try {
                        field.setAccessible(true);
                        Object component = field.get(tab);
                        if (component instanceof IChatComponent) addLines(((IChatComponent) component).getUnformattedText());
                    } catch (Exception ignored) { /* Header/footer are optional and version-dependent. */ }
                }
            }
        }
        for (String line : cachedLines) secrets.observe(line);
        floor = DungeonFloor.detect(cachedLines);
    }

    private void addLines(String raw) {
        if (raw == null || raw.isEmpty()) return;
        String clean = ScoreboardText.clean(raw);
        if (clean.isEmpty()) return;
        String[] split = clean.split("\\r?\\n");
        for (String line : split) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !cachedLines.contains(trimmed)) cachedLines.add(trimmed);
        }
    }

    private void resetRun() {
        secrets.reset();
        floor = "--";
        scanTicks = 0L;
        bloodReadyShown = false;
        cachedLines.clear();
    }

    public String floorText() { return floor; }
    public String secretDisplayText() { return secrets.display(); }

    /** Formatting-code cleanup in a tiny, dependency-free helper to keep scoreboard parsing predictable. */
    private static final class ScoreboardText {
        private static String clean(String text) {
            if (text == null) return "";
            return StringUtils.stripControlCodes(text).replaceAll("\\s+", " ").trim();
        }
    }
}
