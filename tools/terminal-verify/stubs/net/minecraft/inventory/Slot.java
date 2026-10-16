package net.minecraft.inventory;

import net.minecraft.item.ItemStack;

public class Slot {
    public int xDisplayPosition;
    public int yDisplayPosition;
    public int slotNumber;
    private ItemStack stack;

    public Slot(int slotNumber, int x, int y) {
        this.slotNumber = slotNumber;
        this.xDisplayPosition = x;
        this.yDisplayPosition = y;
    }

    public ItemStack getStack() { return this.stack; }
    public void putStack(ItemStack stack) { this.stack = stack; }
    public boolean getHasStack() { return this.stack != null; }
}
