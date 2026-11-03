package com.arena.pekaclient.itemlockswap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import java.util.List;

/** Runtime inventory inspection for the transformer hook. All callers are on Minecraft's client thread. */
public final class ItemLockSwapProtection {

    private static boolean internalSwap;

    private ItemLockSwapProtection() {
    }

    /** Allows the deliberate pair-swap transaction to pass the same pre-packet guard exactly while it executes. */
    public static void beginInternalSwap() {
        internalSwap = true;
    }

    public static void endInternalSwap() {
        internalSwap = false;
    }

    public static boolean isInternalSwap() {
        return internalSwap;
    }

    public static boolean shouldBlockWindowClick(int windowId, int slotId, int mouseButton, int mode,
                                                  EntityPlayer player) {
        if (!ItemLockSwapSettings.enabled || internalSwap || !ItemLockSwapHookStatus.isWindowClickActive()
                || player == null || player.openContainer == null || windowId != player.openContainer.windowId) {
            return false;
        }
        Container container = player.openContainer;
        int clicked = playerInventoryIndex(container, slotId, player);
        boolean clickedProtected = ItemLockSwapSettings.isProtected(clicked);
        boolean hotbarSourceProtected = mode == ItemLockSwapClickPolicy.SWAP && mouseButton >= 0 && mouseButton <= 8
                && ItemLockSwapSettings.isProtected(mouseButton);
        boolean collectProtected = mode == ItemLockSwapClickPolicy.PICKUP_ALL
                && collectWouldTouchProtected(container, player);
        return ItemLockSwapClickPolicy.shouldBlock(mode, clickedProtected, hotbarSourceProtected, collectProtected);
    }

    public static boolean shouldBlockSelectedHotbarDrop(EntityPlayer player, boolean dropAll) {
        boolean protectedSlot = player != null && player.inventory != null
                && ItemLockSwapSettings.isProtected(player.inventory.currentItem);
        return ItemLockSwapHookStatus.isDropActive()
                && ItemLockSwapDropPolicy.shouldBlock(ItemLockSwapSettings.enabled, protectedSlot, internalSwap, dropAll);
    }

    /** Maps an actual container slot only after strict InventoryPlayer identity verification. */
    public static int playerInventoryIndex(Container container, int containerSlot, EntityPlayer player) {
        if (container == null || player == null || player.inventory == null || containerSlot < 0) return -1;
        try {
            Slot slot = container.getSlot(containerSlot);
            if (slot == null) return -1;
            return ItemLockSwapSlotMapper.fromPlayerInventorySlot(slot.inventory == player.inventory,
                    slot.getSlotIndex());
        } catch (RuntimeException ignored) {
            // A modded container with an invalid slot id must stay wholly vanilla.
            return -1;
        }
    }

    /** True only for a live protected slot that mode-6's cursor stack would actually collect. */
    @SuppressWarnings("unchecked")
    private static boolean collectWouldTouchProtected(Container container, EntityPlayer player) {
        ItemStack cursor = player.inventory.getItemStack();
        if (cursor == null || container == null || container.inventorySlots == null) return false;
        List<Slot> slots = container.inventorySlots;
        for (int i = 0; i < slots.size(); i++) {
            Slot slot = slots.get(i);
            if (slot == null || slot.inventory != player.inventory) continue;
            int index = ItemLockSwapSlotMapper.fromPlayerInventorySlot(true, slot.getSlotIndex());
            if (!ItemLockSwapSettings.isProtected(index) || !slot.getHasStack()) continue;
            ItemStack stack = slot.getStack();
            if (stack != null && ItemStack.areItemsEqual(cursor, stack)
                    && ItemStack.areItemStackTagsEqual(cursor, stack)) {
                return true;
            }
        }
        return false;
    }
}
