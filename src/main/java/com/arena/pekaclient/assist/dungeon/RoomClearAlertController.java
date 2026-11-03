package com.arena.pekaclient.assist.dungeon;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.feature.DungeonContext;
import com.arena.pekaclient.hud.HudManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Local room-clear alert controller. It observes two real visible signals--server chat and sidebar Cleared-percent
 * progress--and never changes chat, packets, player state, or server gameplay.
 */
public final class RoomClearAlertController {
    private final RoomClearAlertPolicy policy = new RoomClearAlertPolicy();
    private World activeWorld;
    private int lastClearedPercent = -1;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null || mc.thePlayer == null || !DungeonContext.inDungeon()) {
            clear();
            return;
        }
        if (activeWorld != mc.theWorld) {
            activeWorld = mc.theWorld;
            lastClearedPercent = -1;
            policy.reset();
            HudManager.ROOM_CLEAR_ALERT.clear();
        }
        observeSidebarProgress(mc, System.currentTimeMillis());
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (event == null || event.message == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null || mc.thePlayer == null) return;
        DungeonContext.Result context = DungeonContext.evaluateNow(mc);
        if (!context.inDungeon) return;
        if (activeWorld != mc.theWorld) {
            activeWorld = mc.theWorld;
            lastClearedPercent = -1;
            policy.reset();
        }
        long now = System.currentTimeMillis();
        if (!policy.shouldAlert(true, event.message.getUnformattedText(), roomKey(mc), now)) return;
        showAlert(mc, now);
    }

    /**
     * Lets the settings UI demonstrate the exact movable alert outside a dungeon. It is a local visual/sound preview;
     * it does not change the user's enabled toggle or send a chat/packet action.
     */
    public static void triggerTestAlert() {
        HudManager.ensureLoaded();
        Minecraft mc = Minecraft.getMinecraft();
        long now = System.currentTimeMillis();
        HudManager.ROOM_CLEAR_ALERT.triggerTest(now);
        if (AppleSettings.dungeonRoomClearSound && mc != null && mc.thePlayer != null) {
            mc.thePlayer.playSound("random.orb", 0.70F, 1.35F);
        }
    }

    private void observeSidebarProgress(Minecraft mc, long now) {
        DungeonScoreReadout readout = DungeonScoreTracker.INSTANCE.readout();
        // Use the raw sidebar Dungeon Cleared percentage, never the calculator's Blood Door-adjusted clear input.
        // The latter is score-only and must not create a false room-clear alert when Blood opens.
        int current = readout.clearedPercent;
        if (current < 0) return;
        int previous = lastClearedPercent;
        lastClearedPercent = current;
        // The first sidebar value establishes the run baseline. It must never create a delayed alert on entry.
        if (previous < 0 || !HudManager.ROOM_CLEAR_ALERT.enabled) return;
        if (!policy.shouldAlertClearedProgress(true, previous, current, roomKey(mc), now)) return;
        showAlert(mc, now);
    }

    private static void showAlert(Minecraft mc, long now) {
        HudManager.ensureLoaded();
        if (!HudManager.ROOM_CLEAR_ALERT.enabled) return;
        HudManager.ROOM_CLEAR_ALERT.trigger(now);
        if (AppleSettings.dungeonRoomClearSound && mc != null && mc.thePlayer != null) {
            // Local client sound only; the event, chat, packets, and player state are otherwise untouched.
            mc.thePlayer.playSound("random.orb", 0.70F, 1.35F);
        }
    }

    private void clear() {
        if (activeWorld != null) {
            policy.reset();
            HudManager.ROOM_CLEAR_ALERT.clear();
        }
        activeWorld = null;
        lastClearedPercent = -1;
    }

    /** Coarse 32-block dungeon cell key is sufficient to prevent repeat alerts after revisiting a cleared room. */
    private static String roomKey(Minecraft mc) {
        if (mc == null || mc.thePlayer == null) return "";
        int x = (int) Math.floor(mc.thePlayer.posX / 32.0D);
        int z = (int) Math.floor(mc.thePlayer.posZ / 32.0D);
        return x + ":" + z;
    }
}
