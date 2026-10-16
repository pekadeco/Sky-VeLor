package net.minecraft.block;

import net.minecraft.item.Item;

public class Block {
    public final Item item;
    private final String name;

    public Block(String name, Item item) {
        this.name = name;
        this.item = item;
    }

    public String getUnlocalizedName() { return this.name; }
    public String toString() { return this.name; }
}
