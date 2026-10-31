/* Adapted for Lunaria from the NotEnoughFakepixel-origin Puzzler route behavior; see LICENSE and NOTICE.md. */
package com.arena.pekaclient.feature.mining;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.feature.SkyblockContext;
import com.arena.pekaclient.render.WorldBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Locale;

/** Highlights the Puzzler target only after a valid clue arrives while on Fakepixel. */
public final class MiningPuzzlerSolver {

    private static final double MAX_RENDER_DISTANCE_SQ = 256.0D * 256.0D;
    private static final int TARGET_COLOR = 0xD05CFF83;
    private BlockPos target;
    private String targetServer;

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (!AppleSettings.miningPuzzlerSolver || event == null || event.message == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null || mc.thePlayer == null
                || !SkyblockContext.isInSkyblock() || !isFakepixelServer(mc)) return;
        String message = event.message.getUnformattedText();
        if (!MiningPuzzlerRoute.isPuzzlerMessage(message)) return;
        int[] parsed = MiningPuzzlerRoute.targetFromMessage(message);
        if (parsed == null) {
            clearTarget();
            return;
        }
        target = new BlockPos(parsed[0], parsed[1], parsed[2]);
        ServerData server = mc.getCurrentServerData();
        targetServer = server == null ? null : server.serverIP;
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!SkyblockContext.isInSkyblock()) {
            clearTarget();
            return;
        }
        if (!AppleSettings.miningPuzzlerSolver) {
            clearTarget();
            return;
        }
        if (target == null || event == null || !WorldBox.ready()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) {
            clearTarget();
            return;
        }
        ServerData current = mc.getCurrentServerData();
        if (current == null || current.serverIP == null || targetServer == null
                || !current.serverIP.equalsIgnoreCase(targetServer)) {
            clearTarget();
            return;
        }
        if (mc.thePlayer.getDistanceSq(target.getX() + 0.5D,
                target.getY() + 0.5D, target.getZ() + 0.5D) > MAX_RENDER_DISTANCE_SQ) return;

        net.minecraft.client.renderer.entity.RenderManager renderManager = mc.getRenderManager();
        WorldBox.begin(renderManager.viewerPosX, renderManager.viewerPosY, renderManager.viewerPosZ, true);
        try {
            WorldBox.outline(target.getX() + 0.04D, target.getY() + 0.04D, target.getZ() + 0.04D,
                    target.getX() + 0.96D, target.getY() + 0.96D, target.getZ() + 0.96D, TARGET_COLOR);
        } finally {
            WorldBox.end();
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        clearTarget();
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || event == null || event.world == mc.theWorld) clearTarget();
    }

    private void clearTarget() {
        target = null;
        targetServer = null;
    }

    private static boolean isFakepixelServer(Minecraft mc) {
        if (mc == null) return false;
        ServerData server = mc.getCurrentServerData();
        return server != null && server.serverIP != null
                && server.serverIP.toLowerCase(Locale.ROOT).contains("fakepixel");
    }
}
