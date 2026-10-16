package net.minecraft.init;

import net.minecraft.block.Block;
import net.minecraft.item.Item;

/** The glass family used by the Click in order terminal. */
public class Blocks {
    public static final Block stained_glass_pane = new Block("stained_glass_pane", new Item("item.stained_glass_pane"));
    public static final Block stained_glass = new Block("stained_glass", new Item("item.stained_glass"));
    public static final Block glass_pane = new Block("glass_pane", new Item("item.glass_pane"));
    public static final Block chest = new Block("chest", new Item("item.chest"));

    public static void register() { }
}
