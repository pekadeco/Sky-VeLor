package net.minecraft.client.gui;

/** Faithful subset of Gui for Minecraft 1.8.9 (MCP stable_22 names). */
public class Gui {
    /** 1.8.9 sorts the corners, then draws one quad with alpha blending. */
    public static void drawRect(int left, int top, int right, int bottom, int color) {
        if (left < right) { int i = left; left = right; right = i; }
        if (top < bottom) { int j = top; top = bottom; bottom = j; }
        com.arena.pekaclient.verify.Raster.rect(left, top, right, bottom, color);
    }

    protected void drawCenteredString(net.minecraft.client.gui.FontRenderer fr, String text, int x, int y, int color) {
        fr.drawString(text, x - fr.getStringWidth(text) / 2, y, color);
    }

    public void drawString(net.minecraft.client.gui.FontRenderer fr, String text, int x, int y, int color) {
        fr.drawString(text, x, y, color);
    }
}
