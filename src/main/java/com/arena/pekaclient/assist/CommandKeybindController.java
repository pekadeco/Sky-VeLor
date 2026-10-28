package com.arena.pekaclient.assist;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/**
 * Sends exactly one saved slash command from a direct physical-key down edge. It intentionally does not use
 * KeyBinding.isPressed(): the custom editor owns the keys and this runtime must never repeat while a key is held.
 */
public final class CommandKeybindController {

    private final boolean[] previousDown = new boolean[CommandKeybindSettings.MAX_KEY_CODE + 1];
    private long lastSentAt;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        boolean keyboardReady = Keyboard.isCreated();
        boolean screenOpen = mc == null || mc.currentScreen != null;
        boolean playerPresent = mc != null && mc.thePlayer != null;
        long now = System.currentTimeMillis();

        // Sample every physical key before the screen guard. Holding a key while closing chat/editor must not create
        // a fresh edge on the first gameplay tick.
        for (int keyCode = 1; keyCode <= CommandKeybindSettings.MAX_KEY_CODE; keyCode++) {
            boolean down = keyboardReady && Keyboard.isKeyDown(keyCode);
            boolean wasDown = previousDown[keyCode];
            previousDown[keyCode] = down;
            CommandKeybindSettings.Entry entry = CommandKeybindSettings.firstExecutableForKey(keyCode);
            if (entry == null) continue;
            if (!CommandKeybindPolicy.shouldSend(CommandKeybindSettings.enabled,
                    CommandKeybindSettings.isExecutable(entry), playerPresent, screenOpen, down, wasDown,
                    now, lastSentAt)) {
                continue;
            }
            // This is a direct player chat command caused by the one physical edge above, not a scheduled action.
            mc.thePlayer.sendChatMessage(entry.command());
            lastSentAt = now;
        }
    }
}
