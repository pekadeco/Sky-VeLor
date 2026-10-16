package net.minecraft.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

/** A simple chest inventory, used by the verification harness. */
public class InventoryBasic implements IInventory {
    private final String title;
    private final ItemStack[] items;

    public InventoryBasic(String title, int size) {
        this.title = title;
        this.items = new ItemStack[size];
    }

    public int getSizeInventory() { return this.items.length; }
    public ItemStack getStackInSlot(int index) { return this.items[index]; }
    public IChatComponent getDisplayName() { return new ChatComponentText(this.title); }
    public String getName() { return this.title; }
    public int getInventoryStackLimit() { return 64; }

    public void set(int index, ItemStack stack) { this.items[index] = stack; }
}
