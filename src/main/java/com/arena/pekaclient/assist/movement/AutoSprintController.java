package com.arena.pekaclient.assist.movement;

import com.arena.pekaclient.config.AssistSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.potion.Potion;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Explicit opt-in auto-sprint input assist. It holds only Minecraft's normal sprint key state and deliberately
 * sends no custom movement packet; vanilla decides whether and how to synchronize sprinting with the server.
 */
public final class AutoSprintController {
    private static final int NO_OWNED_KEY = Integer.MIN_VALUE;
    private int ownedSprintKeyCode = NO_OWNED_KEY;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!AssistSettings.autoSprint || mc == null || mc.theWorld == null || mc.thePlayer == null
                || mc.currentScreen != null || mc.gameSettings == null || mc.gameSettings.keyBindSprint == null
                || mc.gameSettings.keyBindForward == null) {
            releaseSprintKey();
            return;
        }

        EntityPlayerSP player = mc.thePlayer;
        if (player.movementInput == null) {
            releaseSprintKey();
            return;
        }
        boolean canSustain = player.capabilities.allowFlying || player.getFoodStats().getFoodLevel() > 6;
        boolean holdSprint = AutoSprintPolicy.shouldHoldSprintKey(mc.gameSettings.keyBindForward.isKeyDown(),
                player.movementInput.moveForward, player.isSneaking(), player.isUsingItem(), player.isRiding(),
                player.isCollidedHorizontally, canSustain, player.isPotionActive(Potion.blindness));
        if (holdSprint) holdSprintKey(mc.gameSettings.keyBindSprint.getKeyCode());
        else releaseSprintKey(); // Includes releasing immediately after forward is no longer held.
    }

    private void holdSprintKey(int keyCode) {
        if (ownedSprintKeyCode != keyCode) releaseSprintKey();
        KeyBinding.setKeyBindState(keyCode, true);
        ownedSprintKeyCode = keyCode;
    }

    private void releaseSprintKey() {
        if (ownedSprintKeyCode == NO_OWNED_KEY) return;
        KeyBinding.setKeyBindState(ownedSprintKeyCode, false);
        ownedSprintKeyCode = NO_OWNED_KEY;
    }
}
