package net.minecraftforge.client.event;

import net.minecraft.client.gui.GuiScreen;

public class GuiOpenEvent {
    public GuiScreen gui;

    public GuiOpenEvent(GuiScreen gui) { this.gui = gui; }
}
