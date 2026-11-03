package com.arena.pekaclient.assist;

import com.arena.pekaclient.PekaClientMod;
import com.arena.pekaclient.config.AssistSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.Locale;

/**
 * Direct player-triggered Pets shortcut. Wardrobe owns its stricter Wardrobe-GuiChest number-key behavior in its own
 * controller; Pets uses the same physical down-edge and no-screen gate for its one visible {@code /pets} command.
 */
public final class ShortcutController {
    private boolean petsWasDown;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            petsWasDown = false;
            return;
        }
        int configuredKey = PekaClientMod.PETS_SHORTCUT_KEY.getKeyCode();
        boolean petsDown = configuredKey > 0 && Keyboard.isKeyDown(configuredKey);
        boolean petsWasDownBeforeTick = petsWasDown;
        petsWasDown = petsDown;
        // Sample before the screen gate: holding P while closing chat/editor never manufactures a fresh gameplay edge.
        if (!AssistSettings.petsShortcut || mc.thePlayer == null || mc.currentScreen != null || !isFakepixel(mc)) return;
        if (petsDown && !petsWasDownBeforeTick) {
            mc.thePlayer.sendChatMessage("/pets");
        }
    }

    /** Shared host guard for the retained server-specific Pets command; never used as an automation signal. */
    static boolean isFakepixel(Minecraft mc) {
        ServerData data = mc.getCurrentServerData();
        return data != null && data.serverIP != null && data.serverIP.toLowerCase(Locale.ROOT).indexOf("fakepixel") >= 0;
    }
}
