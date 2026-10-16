package net.minecraft.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;

public interface IInventory {
    int getSizeInventory();
    ItemStack getStackInSlot(int index);
    IChatComponent getDisplayName();
    String getName();
    int getInventoryStackLimit();
}
