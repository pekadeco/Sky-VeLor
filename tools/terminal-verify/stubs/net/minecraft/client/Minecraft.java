package net.minecraft.client;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.PlayerControllerMP;

public class Minecraft {
    private static Minecraft instance;

    public PlayerControllerMP playerController;
    public EntityPlayerSP thePlayer;
    public GuiScreen currentScreen;
    public boolean inGameHasFocus;

    public static Minecraft getMinecraft() {
        if (instance == null) { instance = new Minecraft(); }
        return instance;
    }

    public void displayGuiScreen(GuiScreen screen) { this.currentScreen = screen; }

    public void setIngameFocus() { this.inGameHasFocus = true; }
}
