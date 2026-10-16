package net.minecraft.client.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.util.IChatComponent;

public class EntityPlayerSP extends EntityPlayer {
    public void closeScreen() {
        Minecraft.getMinecraft().displayGuiScreen((net.minecraft.client.gui.GuiScreen) null);
    }

    public void addChatMessage(IChatComponent component) { }
}
