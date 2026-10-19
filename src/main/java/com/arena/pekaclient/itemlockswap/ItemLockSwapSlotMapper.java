package com.arena.pekaclient.itemlockswap;

/**
 * Canonical InventoryPlayer addressing used by locks and pairs. A container slot is accepted only after its
 * {@code Slot.inventory == player.inventory} identity check; its {@code getSlotIndex()} is already this address.
 */
public final class ItemLockSwapSlotMapper {

    public static final int HOTBAR_FIRST = 0;
    public static final int HOTBAR_LAST = 8;
    public static final int MAIN_FIRST = 9;
    public static final int MAIN_LAST = 35;
    public static final int BOOTS = 36;
    public static final int LEGGINGS = 37;
    public static final int CHESTPLATE = 38;
    public static final int HELMET = 39;

    private ItemLockSwapSlotMapper() {
    }

    public static boolean isPlayerInventoryIndex(int index) {
        return index >= HOTBAR_FIRST && index <= HELMET;
    }

    public static boolean isHotbar(int index) {
        return index >= HOTBAR_FIRST && index <= HOTBAR_LAST;
    }

    public static boolean isMainInventory(int index) {
        return index >= MAIN_FIRST && index <= MAIN_LAST;
    }

    public static boolean isArmor(int index) {
        return index >= BOOTS && index <= HELMET;
    }

    /**
     * Maps only a Slot proven to belong to the local player's InventoryPlayer. All other inventories, including
     * chest, crafting-result, merchant and fake slots, deliberately return -1.
     */
    public static int fromPlayerInventorySlot(boolean belongsToPlayerInventory, int slotIndex) {
        return belongsToPlayerInventory && isPlayerInventoryIndex(slotIndex) ? slotIndex : -1;
    }

    /**
     * Vanilla InventoryContainer visual slot id for this InventoryPlayer position, or -1 when outside 0-39.
     * This is useful when an explicit internal swap has to find the destination in the normal player inventory.
     */
    public static int playerInventoryContainerSlot(int inventoryIndex) {
        if (isHotbar(inventoryIndex)) return 36 + inventoryIndex;
        if (isMainInventory(inventoryIndex)) return inventoryIndex;
        switch (inventoryIndex) {
            case BOOTS:
                return 8;
            case LEGGINGS:
                return 7;
            case CHESTPLATE:
                return 6;
            case HELMET:
                return 5;
            default:
                return -1;
        }
    }

    /** Reverse of {@link #playerInventoryContainerSlot(int)} for a vanilla InventoryContainer visual slot id. */
    public static int inventoryIndexFromPlayerContainerSlot(int containerSlot) {
        if (containerSlot >= 36 && containerSlot <= 44) return containerSlot - 36;
        if (containerSlot >= 9 && containerSlot <= 35) return containerSlot;
        switch (containerSlot) {
            case 8:
                return BOOTS;
            case 7:
                return LEGGINGS;
            case 6:
                return CHESTPLATE;
            case 5:
                return HELMET;
            default:
                return -1;
        }
    }
}
