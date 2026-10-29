package com.arena.pekaclient.assist.dungeon;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.feature.DungeonContext;
import com.arena.pekaclient.hud.HudManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Local, conservative room-clear alert controller; it observes chat and never changes server/gameplay state. */
public final class RoomClearAlertController {
    private final RoomClearAlertPolicy policy = new RoomClearAlertPolicy();
    private World activeWorld;

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
            policy.reset();
            HudManager.ROOM_CLEAR_ALERT.clear();
        }
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
            policy.reset();
        }
        HudManager.ensureLoaded();
        if (!HudManager.ROOM_CLEAR_ALERT.enabled) return;
        long now = System.currentTimeMillis();
        if (!policy.shouldAlert(true, event.message.getUnformattedText(), roomKey(mc), now)) return;
        HudManager.ROOM_CLEAR_ALERT.trigger(now);
        if (AppleSettings.dungeonRoomClearSound) {
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
    }

    /** Coarse 32-block dungeon cell key is sufficient to prevent repeat alerts after revisiting a cleared room. */
    private static String roomKey(Minecraft mc) {
        if (mc == null || mc.thePlayer == null) return "";
        int x = (int) Math.floor(mc.thePlayer.posX / 32.0D);
        int z = (int) Math.floor(mc.thePlayer.posZ / 32.0D);
        return x + ":" + z;
    }
}
