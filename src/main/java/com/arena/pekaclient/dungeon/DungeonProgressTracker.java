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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client-only state for Lunaria's dungeon progress widgets and small dungeon conveniences.
 * It reads visible scoreboard/chat data. Better NEF-compatible 270/S+ notifications can send configured /pc text;
 * container closing belongs exclusively to the current guarded DungeonChestCloser.
 */
public final class DungeonProgressTracker {
    private static final Pattern SECRET_FRACTION = Pattern.compile("(?i)secrets?(?:\\s+found)?\\s*[:=]?\\s*(\\d+)\\s*/\\s*(\\d+)");
    private static final Pattern SECRET_PERCENT = Pattern.compile("(?i)secrets? found\\s*[:=]?\\s*(\\d{1,3})\\s*%");
    private static final Pattern TIME = Pattern.compile("(?i)\\btime\\s*[:=]?\\s*(\\d{1,2}):(\\d{2})\\b");
    private static final Pattern CLEARED_PERCENT = Pattern.compile("(?i)\\bcleared\\s*[:=]?\\s*(\\d{1,3})\\s*%");
    private static final Pattern DEATHS = Pattern.compile("(?i)\\bdeaths?\\s*[:=]?\\s*(\\d+)(?:\\s*\\((\\d+)\\))?");
    private static final Pattern CRYPTS = Pattern.compile("(?i)\\bcrypts?\\s*[:=]?\\s*(\\d+)(?:\\s*/\\s*(\\d+))?");
    private static final String BLOOD_READY = "the watcher: that will be enough for now";

    private int totalScore = -1;
    private int skillScore = -1;
    private int explorationScore = -1;
    private int speedScore = -1;
    private int bonusScore = -1;
    private int explorationClearScore = -1;
    private int explorationSecretsScore = -1;
    private int secretTargetPercent = -1;
    private int requiredSecretsPercent = -1;
    private int secretsFound = 0;
    private int secretsRequired = 0;
    private int secretsPercent = 0;
    private boolean secretPercentSeen;
    private int timeSeconds = -1;
    private int clearPercent = -1;
    private int deaths = -1;
    private int failedPuzzles;
    private int crypts = -1;
    private boolean paulMayor;
    private boolean bloodDoorOpen;
    private boolean finalStage;
    private int reducedDeathPenalty;
    private boolean sNotifierShown;
    private boolean sPlusPartyMessageSent;
    private boolean unreachableMessageSent;
    private String floor = "--";
    private long scanTicks;
    private boolean wasInDungeon;
    private boolean bloodReadyShown;
    private boolean sPlusShown;
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
        updateScoreAlerts();
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
        if (lower.contains("> extra stats <")) finalStage = true;
        if (lower.contains("puzzle fail!")) failedPuzzles++;
        if (lower.contains("your spirit pet reduced your death score penalty to 1")) reducedDeathPenalty = 1;
        if (lower.contains("the blood door has been opened")) bloodDoorOpen = true;
        if (lower.contains(BLOOD_READY) && AppleSettings.dungeonBloodReadyTitle && !bloodReadyShown) {
            bloodReadyShown = true;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.ingameGUI != null) {
                mc.ingameGUI.displayTitle("§cBLOOD READY!", "§bLunaria • Dungeon", 3, 50, 8);
            }
        }
        if (lower.contains("you found a secret") || lower.contains("found a secret!")) {
            if (secretsFound < 0) secretsFound = 0;
            secretsFound++;
        }
        parseScoreLine(clean);
        calculateScoreFromVisibleStats();
        updateScoreAlerts();
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
        for (String line : cachedLines) parseScoreLine(line);
        if (!secretPercentSeen && secretsRequired > 0) {
            secretsPercent = clamp((int) ((long) secretsFound * 100L / secretsRequired), 0, 100);
        }
        String tabFloor = DungeonPuzzleSyncEnvironment.currentFloor(mc);
        floor = tabFloor != null && !tabFloor.isEmpty() ? tabFloor : detectFloor(cachedLines);
        paulMayor = AppleSettings.dungeonPaulBonus;
        calculateScoreFromVisibleStats();
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

    private void parseScoreLine(String text) {
        if (text == null) return;
        Matcher m = SECRET_FRACTION.matcher(text);
        if (m.find()) {
            secretsFound = parseInt(m.group(1), secretsFound);
            secretsRequired = parseInt(m.group(2), secretsRequired);
        }
        m = SECRET_PERCENT.matcher(text);
        if (m.find()) {
            secretsPercent = clamp(parseInt(m.group(1), secretsPercent), 0, 100);
            secretPercentSeen = true;
        }
        m = TIME.matcher(text);
        if (m.find()) timeSeconds = parseInt(m.group(1), 0) * 60 + parseInt(m.group(2), 0);
        m = CLEARED_PERCENT.matcher(text);
        if (m.find()) clearPercent = clamp(parseInt(m.group(1), clearPercent), 0, 100);
        m = DEATHS.matcher(text);
        if (m.find()) deaths = parseInt(m.group(2) != null ? m.group(2) : m.group(1), deaths);
        m = CRYPTS.matcher(text);
        if (m.find()) crypts = m.group(2) != null ? 0 : parseInt(m.group(1), crypts);
    }

    /** Better NEF 1.2.5-compatible score estimate using the same score components and run modifiers. */
    private void calculateScoreFromVisibleStats() {
        paulMayor = AppleSettings.dungeonPaulBonus;
        int effectiveSecrets = Math.max(0, secretsPercent);
        int effectiveClear = clearPercent; // Preserve NEF's pre-clamp behavior when the clear percentage is still unknown.
        int effectiveDeaths = Math.max(0, deaths);
        int effectiveCrypts = Math.max(0, crypts);
        DungeonScoreMath.Result result = DungeonScoreMath.calculate(floor, timeSeconds, effectiveSecrets,
                effectiveClear, effectiveDeaths, Math.max(0, failedPuzzles), effectiveCrypts, paulMayor,
                bloodDoorOpen, reducedDeathPenalty);
        if (result == null) {
            totalScore = skillScore = explorationScore = speedScore = bonusScore = -1;
            explorationClearScore = explorationSecretsScore = secretTargetPercent = requiredSecretsPercent = -1;
            return;
        }
        skillScore = result.skill;
        explorationScore = result.exploration;
        explorationClearScore = result.explorationClear;
        explorationSecretsScore = result.explorationSecrets;
        speedScore = result.speed;
        bonusScore = result.bonus;
        secretTargetPercent = result.secretTarget;
        totalScore = result.total;
        requiredSecretsPercent = DungeonScoreMath.requiredSecretPercent(skillScore, speedScore, bonusScore, floor);
    }

    /** Score notifier semantics follow Better NEF: 270 target, virtual 300/S+ target and optional party message. */
    private void updateScoreAlerts() {
        if (totalScore < 0 || !DungeonContext.inDungeon() || !SkyblockContext.isInSkyblock()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (AppleSettings.dungeonSNotifier && !sNotifierShown && totalScore >= 270) {
            sNotifierShown = true;
            if (mc != null && mc.thePlayer != null) {
                mc.thePlayer.playSound("note.pling", 2.0F, 2.0F);
                mc.thePlayer.sendChatMessage("/pc 270 Score!");
            }
            if (mc != null && mc.ingameGUI != null) {
                mc.ingameGUI.displayTitle("§c270 Score!", "§bLunaria • S target reached", 3, 50, 8);
            }
        }

        if (totalScore >= 300) {
            if (AppleSettings.dungeonSPlusReminder && !sPlusShown) {
                sPlusShown = true;
                if (mc != null && mc.thePlayer != null) mc.thePlayer.playSound("note.pling", 2.0F, 2.0F);
                if (mc != null && mc.ingameGUI != null) {
                    mc.ingameGUI.displayTitle("§a300 Score!", "§bLunaria • S+ target reached", 3, 50, 8);
                }
            }
            if (AppleSettings.dungeonSPlusPartyMessage && !sPlusPartyMessageSent && mc != null && mc.thePlayer != null) {
                String message = AppleSettings.dungeonSPlusCustomMessage == null
                        ? "" : AppleSettings.dungeonSPlusCustomMessage.trim();
                if (message.isEmpty()) message = "300 Score!";
                message = message.replace('\r', ' ').replace('\n', ' ').trim();
                if (message.regionMatches(true, 0, "/pc ", 0, 4)) message = message.substring(4).trim();
                if (!message.isEmpty()) mc.thePlayer.sendChatMessage("/pc " + message);
                sPlusPartyMessageSent = true;
            }
        }

        if (requiredSecretsPercent == -1 && secretTargetPercent > 0
                && AppleSettings.dungeonSPlusPartyMessage && !unreachableMessageSent
                && (AppleSettings.dungeonSPlusReminder || AppleSettings.dungeonSPlusPartyMessage)
                && mc != null && mc.thePlayer != null) {
            mc.thePlayer.sendChatMessage("/pc [Lunaria] S+ may not be reachable by secrets alone; do crypts or restart.");
            unreachableMessageSent = true;
        }
    }

    /** Score rank thresholds are identical to Better NEF's ScoreOverlay. */
    public String rankText() {
        if (totalScore < 0) return "--";
        if (finalStage && explorationClearScore != 60) return "UNKNOWN SCORE";
        if (totalScore < 100) return "D";
        if (totalScore < 160) return "C";
        if (totalScore < 230) return "B";
        if (totalScore < 270) return "A";
        if (totalScore < 300) return "S";
        return "S+";
    }

    public String totalScoreText() {
        if (totalScore < 0) return "Total: waiting for server data";
        if (finalStage && explorationClearScore != 60) return "Total score: UNKNOWN SCORE";
        return "Total score: " + totalScore + " (" + rankText() + ")";
    }

    public String secretDisplayText() {
        if (secretTargetPercent <= 0 || totalScore < 0) return "Secrets: waiting for dungeon data";
        int actual = Math.max(0, secretsPercent);
        if (requiredSecretsPercent < 0) {
            return "Secrets: " + actual + "% / S+ unreachable / " + secretTargetPercent + "%";
        }
        return "Secrets: " + actual + "% / " + requiredSecretsPercent + "% / " + secretTargetPercent + "%";
    }

    private static String detectFloor(List<String> lines) {
        for (String line : lines) {
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.contains("master mode 7") || lower.matches(".*\\bm7\\b.*")) return "M7";
            if (lower.contains("entrance") || lower.matches(".*\\be0\\b.*")) return "E0";
            Matcher m = Pattern.compile("(?i)\\bfloor\\s*([0-7])\\b|\\bf([0-7])\\b").matcher(lower);
            if (m.find()) return "F" + (m.group(1) != null ? m.group(1) : m.group(2));
            if (lower.contains("catacombs") && lower.contains("(f7)")) return "F7";
        }
        return "--";
    }

    private static int parseInt(String text, int fallback) {
        try { return Integer.parseInt(text); } catch (RuntimeException ignored) { return fallback; }
    }

    /** Clamp parsed scoreboard percentages to the valid inclusive range. */
    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void resetRun() {
        totalScore = skillScore = explorationScore = speedScore = bonusScore = -1;
        failedPuzzles = 0;
        paulMayor = AppleSettings.dungeonPaulBonus;
        bloodDoorOpen = false;
        finalStage = false;
        reducedDeathPenalty = 0;
        floor = "--";
        scanTicks = 0L;
        bloodReadyShown = false;
        sPlusShown = false;
        sNotifierShown = false;
        sPlusPartyMessageSent = false;
        unreachableMessageSent = false;
        secretPercentSeen = false;
        explorationClearScore = explorationSecretsScore = secretTargetPercent = requiredSecretsPercent = -1;
        secretsFound = secretsRequired = secretsPercent = 0;
        timeSeconds = -1;
        clearPercent = -1;
        deaths = 0;
        crypts = 0;
        cachedLines.clear();
    }

    public String floorText() { return floor; }
    public int totalScore() { return totalScore; }
    public int skillScore() { return skillScore; }
    public int explorationScore() { return explorationScore; }
    public int speedScore() { return speedScore; }
    public int bonusScore() { return bonusScore; }
    public int secretsFound() { return secretsFound; }
    public int secretsRequired() { return secretsRequired; }
    public int secretsPercent() { return secretsPercent; }
    public int explorationClearScore() { return explorationClearScore; }
    public int explorationSecretsScore() { return explorationSecretsScore; }
    public int secretTargetPercent() { return secretTargetPercent; }
    public int requiredSecretsPercent() { return requiredSecretsPercent; }
    public int reducedDeathPenalty() { return reducedDeathPenalty; }
    public boolean finalStageIncomplete() { return finalStage && explorationClearScore != 60; }

    /** Formatting-code cleanup in a tiny, dependency-free helper to keep scoreboard parsing predictable. */
    private static final class ScoreboardText {
        private static String clean(String text) {
            if (text == null) return "";
            return StringUtils.stripControlCodes(text).replaceAll("\\s+", " ").trim();
        }
    }
}
