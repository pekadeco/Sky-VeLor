/*
 * This modified version is maintained by ii8we. Review LICENSE for upstream attribution and applicable license terms before use or redistribution. Original NotEnoughFakepixel code and third-party components remain subject to their respective copyrights and license terms.
 */
/* Adapted for Lunaria from code originating in NotEnoughFakepixel, through ii8we's private modifications. See LICENSE. */
package com.arena.pekaclient.dungeon.solver.terminals;
import com.arena.pekaclient.config.AppleSettings;

import com.arena.pekaclient.dungeon.solver.DungeonSolverContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class HideTooltips {

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onTooltipLow(ItemTooltipEvent event) {
        if (!AppleSettings.terminalHideTooltips) return;
        if (!DungeonSolverContext.checkTerminalEssentialsF7()) return;
        if (event.toolTip == null) return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;

        if (player != null && mc.currentScreen instanceof GuiChest && player.openContainer instanceof ContainerChest) {
            ContainerChest chest = (ContainerChest) player.openContainer;
            IInventory inv = chest.getLowerChestInventory();
            String chestName = inv.getDisplayName().getUnformattedText();

            if (chestName.equals("Click in order!") ||
                    chestName.startsWith("Select all the") ||
                    chestName.startsWith("What starts with") ||
                    chestName.contains("Complete the maze!")) {
                event.toolTip.clear();
            }
        }
    }
}
