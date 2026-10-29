package com.arena.pekaclient.assist.dungeon;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.feature.DungeonContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Optional, title-allow-listed close for normal dungeon secret chests. It waits for the opened GuiChest to remain
 * current, then invokes vanilla's ordinary closeScreen path; no packet or reward/terminal interaction is created.
 */
public final class DungeonSecretChestCloser {
    private int pendingWindowId = -1;
    private int pendingTicks;

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        clearPending();
        if (event == null || !AppleSettings.dungeonAutoChestClose || !(event.gui instanceof GuiChest)) return;
        Minecraft mc = Minecraft.getMinecraft();
        DungeonContext.Result context = DungeonContext.evaluateNow(mc);
        if (!context.inDungeon) return;
        GuiChest chest = (GuiChest) event.gui;
        if (!(chest.inventorySlots instanceof ContainerChest)) return;
        ContainerChest container = (ContainerChest) chest.inventorySlots;
        if (container.getLowerChestInventory() == null || container.getLowerChestInventory().getDisplayName() == null) return;
        String title = container.getLowerChestInventory().getDisplayName().getUnformattedText();
        if (!DungeonSecretChestPolicy.isNormalSecretChestTitle(title)) return;
        pendingWindowId = container.windowId;
        // Let vanilla install the matching screen first; a replacement/reward menu invalidates this pending close.
        pendingTicks = 2;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.END || pendingTicks <= 0) return;
        if (--pendingTicks > 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (mc == null || mc.thePlayer == null || !DungeonContext.inDungeon()) return;
            GuiScreen current = mc.currentScreen;
            if (!(current instanceof GuiChest)) return;
            Container inventory = ((GuiChest) current).inventorySlots;
            if (!(inventory instanceof ContainerChest) || inventory.windowId != pendingWindowId) return;
            ContainerChest chest = (ContainerChest) inventory;
            if (chest.getLowerChestInventory() == null || chest.getLowerChestInventory().getDisplayName() == null) return;
            if (!DungeonSecretChestPolicy.isNormalSecretChestTitle(
                    chest.getLowerChestInventory().getDisplayName().getUnformattedText())) return;
            mc.thePlayer.closeScreen();
        } finally {
            clearPending();
        }
    }

    private void clearPending() {
        pendingWindowId = -1;
        pendingTicks = 0;
    }
}
