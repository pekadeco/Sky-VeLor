package com.arena.pekaclient.assist;

import com.arena.pekaclient.PekaClientMod;
import com.arena.pekaclient.config.AssistSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Locale;

/** User-triggered, default-unbound command shortcuts. Commands are never emitted on their own. */
public final class ShortcutController {
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.END) return;
        handle(PekaClientMod.WARDROBE_SHORTCUT_KEY, AssistSettings.wardrobeShortcut, "/wardrobe");
        handle(PekaClientMod.PETS_SHORTCUT_KEY, AssistSettings.petsShortcut, "/pets");
    }

    private static void handle(net.minecraft.client.settings.KeyBinding key, boolean enabled, String command) {
        while (key.isPressed()) {
            Minecraft mc = Minecraft.getMinecraft();
            if (!enabled || mc == null || mc.thePlayer == null || !isFakepixel(mc)) continue;
            mc.thePlayer.sendChatMessage(command);
        }
    }

    private static boolean isFakepixel(Minecraft mc) {
        ServerData data = mc.getCurrentServerData();
        return data != null && data.serverIP != null && data.serverIP.toLowerCase(Locale.ROOT).indexOf("fakepixel") >= 0;
    }
}
