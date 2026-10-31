package com.arena.pekaclient.feature;

import com.arena.pekaclient.config.AppleSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.potion.Potion;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Opt-in forward auto-sprint that follows the normal movement restrictions. */
public final class AutoSprint {
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.END || !AppleSettings.autoSprintEnabled) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;
        if (player == null || mc.theWorld == null || mc.currentScreen != null || player.movementInput == null) return;

        boolean hasFoodOrFlight = player.capabilities.allowFlying || player.getFoodStats().getFoodLevel() > 6;
        boolean blinded = player.isPotionActive(Potion.blindness);
        if (AutoSprintPolicy.shouldStart(player.movementInput.moveForward, player.isSneaking(), player.isUsingItem(),
                player.isRiding(), player.isCollidedHorizontally, hasFoodOrFlight, blinded)) {
            player.setSprinting(true);
        }
    }
}
