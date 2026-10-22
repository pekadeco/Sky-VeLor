package com.arena.pekaclient.assist.movement;

import com.arena.pekaclient.config.AssistSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.potion.Potion;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Explicit opt-in auto-sprint event handler. It changes only local sprint state and sends no custom packets. */
public final class AutoSprintController {
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.END || !AssistSettings.autoSprint) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc == null ? null : mc.thePlayer;
        if (player == null || mc.theWorld == null || mc.currentScreen != null || player.movementInput == null) return;
        boolean canSustain = player.capabilities.allowFlying || player.getFoodStats().getFoodLevel() > 6;
        if (AutoSprintPolicy.mayStart(player.movementInput.moveForward, player.isSneaking(), player.isUsingItem(),
                player.isRiding(), player.isCollidedHorizontally, canSustain, player.isPotionActive(Potion.blindness))) {
            player.setSprinting(true);
        }
    }
}
