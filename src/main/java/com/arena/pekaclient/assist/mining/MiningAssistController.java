package com.arena.pekaclient.assist.mining;

import com.arena.pekaclient.config.AssistSettings;
import com.arena.pekaclient.feature.MiningTracker;
import com.arena.pekaclient.render.WorldBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Locale;

/**
 * Client-only mining reminders and the Puzzler marker. Chat parsing happens once per received line; the marker
 * retains only three primitive coordinates and emits one WorldBox pass at render time.
 */
public final class MiningAssistController {
    private static final double MAX_MARKER_DISTANCE_SQ = 256D * 256D;
    private static final int MARKER_COLOR = 0xD05CFF83;

    private MiningAbilityPolicy.Activation activation;
    private long readyAtMillis;
    private boolean readyNoticePending;
    private boolean onSupportedServer;
    private boolean hasPuzzlerTarget;
    private int puzzlerX;
    private int puzzlerY;
    private int puzzlerZ;
    private String targetServer = "";
    private String abilityLine = "Pickaxe ability: Not tracked";
    private String miningLine = "Mining rate: 0.00 blocks/s";
    private String sessionLine = "Session blocks: 0";

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event == null || event.message == null || !isSupportedServer()) return;
        String text = event.message.getUnformattedText();
        if (AssistSettings.miningAbilityAlert) {
            MiningAbilityPolicy.Activation detected = MiningAbilityPolicy.parseActivation(text);
            if (detected != null) {
                activation = detected;
                readyAtMillis = System.currentTimeMillis() + detected.cooldownSeconds * 1000L;
                readyNoticePending = true;
            }
        }
        if (AssistSettings.miningPuzzlerMarker) {
            int[] target = PuzzlerRoute.decode(text);
            if (target != null) {
                puzzlerX = target[0];
                puzzlerY = target[1];
                puzzlerZ = target[2];
                hasPuzzlerTarget = true;
                targetServer = currentServer();
            } else if (PuzzlerRoute.isPuzzlerLine(text)) {
                clearPuzzlerTarget();
            }
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.END) return;
        onSupportedServer = isSupportedServer();
        long now = System.currentTimeMillis();
        if (!onSupportedServer || !AssistSettings.miningAbilityAlert) {
            activation = null;
            readyAtMillis = 0L;
            readyNoticePending = false;
        } else if (readyNoticePending && activation != null && now >= readyAtMillis) {
            readyNoticePending = false;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.ingameGUI != null) {
                mc.ingameGUI.displayTitle(EnumChatFormatting.GOLD + "Mining Ability Ready",
                        EnumChatFormatting.GRAY + activation.name, 2, 55, 2);
            }
        }
        if (!onSupportedServer || !AssistSettings.miningPuzzlerMarker || !targetServer.equals(currentServer())) {
            clearPuzzlerTarget();
        }
        updateHudLines(now);
    }

    @SubscribeEvent
    public void onWorldChange(WorldEvent.Load event) {
        activation = null;
        readyAtMillis = 0L;
        readyNoticePending = false;
        clearPuzzlerTarget();
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (event == null || !AssistSettings.miningPuzzlerMarker || !hasPuzzlerTarget || !WorldBox.ready()) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc == null ? null : mc.thePlayer;
        if (player == null || player.getDistanceSq(puzzlerX + .5D, puzzlerY + .5D, puzzlerZ + .5D) > MAX_MARKER_DISTANCE_SQ) {
            return;
        }
        RenderManager manager = mc.getRenderManager();
        if (manager == null) return;
        WorldBox.begin(manager.viewerPosX, manager.viewerPosY, manager.viewerPosZ, true);
        try {
            WorldBox.outline(puzzlerX + .04D, puzzlerY + .04D, puzzlerZ + .04D,
                    puzzlerX + .96D, puzzlerY + .96D, puzzlerZ + .96D, MARKER_COLOR);
        } finally {
            WorldBox.end();
        }
    }

    public String miningLine() {
        return miningLine;
    }

    public String sessionLine() {
        return sessionLine;
    }

    public String abilityLine() {
        return abilityLine;
    }

    private void updateHudLines(long now) {
        miningLine = "Mining rate: " + Math.round(MiningTracker.rate() * 100F) / 100F + " blocks/s";
        sessionLine = "Session blocks: " + MiningTracker.total();
        abilityLine = "Pickaxe ability: " + MiningAbilityPolicy.status(now, onSupportedServer, activation, readyAtMillis);
    }

    private boolean isSupportedServer() {
        String server = currentServer();
        return server.indexOf("fakepixel") >= 0;
    }

    private String currentServer() {
        Minecraft mc = Minecraft.getMinecraft();
        ServerData data = mc == null ? null : mc.getCurrentServerData();
        return data == null || data.serverIP == null ? "" : data.serverIP.toLowerCase(Locale.ROOT);
    }

    private void clearPuzzlerTarget() {
        hasPuzzlerTarget = false;
        targetServer = "";
    }
}
