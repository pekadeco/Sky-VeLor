package net.minecraft.client.gui;

/** Faithful subset of FontRenderer. drawString keeps the 1.8.9 ARGB colour handling. */
public class FontRenderer {
    public int FONT_HEIGHT = 9;

    public int drawString(String text, int x, int y, int color) {
        return this.drawString(text, (float) x, (float) y, color, false);
    }

    public int drawString(String text, float x, float y, int color, boolean dropShadow) {
        if (text == null) { text = "null"; }
        com.arena.pekaclient.verify.Raster.text(text, x, y, color, dropShadow);
        return (int) x + this.getStringWidth(text);
    }

    public int getStringWidth(String text) {
        return text == null ? 0 : text.length() * 6 - 1;
    }
}
