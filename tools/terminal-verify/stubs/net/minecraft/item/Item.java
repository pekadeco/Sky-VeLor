package net.minecraft.item;

import net.minecraft.block.Block;

public class Item {
    private final String name;

    public Item(String name) { this.name = name; }

    public String getUnlocalizedName() { return this.name; }
    public String toString() { return this.name; }

    public static Item getItemFromBlock(Block block) { return block == null ? null : block.item; }
}
