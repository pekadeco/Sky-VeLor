package com.arena.pekaclient.itemlockswap;

/** Covers the exact 1.8.9 InventoryPlayer position convention, including armor ordering. */
public final class ItemLockSwapSlotMapperTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("hotbar bounds", ItemLockSwapSlotMapper.isHotbar(0) && ItemLockSwapSlotMapper.isHotbar(8)
                && !ItemLockSwapSlotMapper.isHotbar(9));
        check("main bounds", ItemLockSwapSlotMapper.isMainInventory(9)
                && ItemLockSwapSlotMapper.isMainInventory(35) && !ItemLockSwapSlotMapper.isMainInventory(36));
        check("armor order", ItemLockSwapSlotMapper.isArmor(ItemLockSwapSlotMapper.BOOTS)
                && ItemLockSwapSlotMapper.BOOTS == 36 && ItemLockSwapSlotMapper.LEGGINGS == 37
                && ItemLockSwapSlotMapper.CHESTPLATE == 38 && ItemLockSwapSlotMapper.HELMET == 39);
        check("only local player inventory slot maps", ItemLockSwapSlotMapper.fromPlayerInventorySlot(true, 39) == 39
                && ItemLockSwapSlotMapper.fromPlayerInventorySlot(false, 0) == -1
                && ItemLockSwapSlotMapper.fromPlayerInventorySlot(true, 40) == -1);
        check("hotbar visual mapping", ItemLockSwapSlotMapper.playerInventoryContainerSlot(0) == 36
                && ItemLockSwapSlotMapper.playerInventoryContainerSlot(8) == 44);
        check("main visual mapping", ItemLockSwapSlotMapper.playerInventoryContainerSlot(9) == 9
                && ItemLockSwapSlotMapper.playerInventoryContainerSlot(35) == 35);
        check("armor visual mapping", ItemLockSwapSlotMapper.playerInventoryContainerSlot(36) == 8
                && ItemLockSwapSlotMapper.playerInventoryContainerSlot(37) == 7
                && ItemLockSwapSlotMapper.playerInventoryContainerSlot(38) == 6
                && ItemLockSwapSlotMapper.playerInventoryContainerSlot(39) == 5);
        check("inverse inventory mapping", ItemLockSwapSlotMapper.inventoryIndexFromPlayerContainerSlot(36) == 0
                && ItemLockSwapSlotMapper.inventoryIndexFromPlayerContainerSlot(44) == 8
                && ItemLockSwapSlotMapper.inventoryIndexFromPlayerContainerSlot(5) == 39
                && ItemLockSwapSlotMapper.inventoryIndexFromPlayerContainerSlot(8) == 36
                && ItemLockSwapSlotMapper.inventoryIndexFromPlayerContainerSlot(0) == -1);
        System.out.println("item lock/swap slot mapper: passed " + passed + ", failed " + failed);
        if (failed > 0) throw new AssertionError("item lock/swap mapper failures: " + failed);
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++;
        else {
            failed++;
            System.err.println("FAIL " + name);
        }
    }
}
