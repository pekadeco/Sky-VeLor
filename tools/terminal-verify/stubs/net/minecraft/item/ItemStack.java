package net.minecraft.item;

import net.minecraft.nbt.NBTTagCompound;

public class ItemStack {
    public int stackSize;
    private final Item item;
    private final int metadata;
    private String displayName;
    private NBTTagCompound tag;

    public ItemStack(Item item, int stackSize, int metadata) {
        this(item, stackSize, metadata, null);
    }

    public ItemStack(Item item, int stackSize, int metadata, String displayName) {
        this.item = item;
        this.stackSize = stackSize;
        this.metadata = metadata;
        this.displayName = displayName;
    }

    public Item getItem() { return this.item; }
    public int getMetadata() { return this.metadata; }
    public String getDisplayName() { return this.displayName == null ? "" : this.displayName; }
    public void setStackDisplayName(String name) { this.displayName = name; }
    public NBTTagCompound getTagCompound() { return this.tag; }
    public void setTagCompound(NBTTagCompound tag) { this.tag = tag; }
    public int getMaxStackSize() { return 64; }
    public ItemStack copy() { return new ItemStack(this.item, this.stackSize, this.metadata, this.displayName); }
}
