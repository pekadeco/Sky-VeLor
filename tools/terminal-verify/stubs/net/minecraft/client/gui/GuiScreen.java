package net.minecraft.client.gui;

import java.io.IOException;

/** Faithful subset of GuiScreen for Minecraft 1.8.9. */
public class GuiScreen extends Gui {
    public int width;
    public int height;
    public FontRenderer fontRendererObj = new FontRenderer();
    public net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            this.mc.displayGuiScreen((GuiScreen) null);
            if (this.mc.currentScreen == null) {
                this.mc.setIngameFocus();
            }
        }
    }

    public void initGui() { }
    public void onGuiClosed() { }
    public boolean doesGuiPauseGame() { return true; }
    public void updateScreen() { }
}
