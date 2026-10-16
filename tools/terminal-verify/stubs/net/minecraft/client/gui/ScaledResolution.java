package net.minecraft.client.gui;

public class ScaledResolution {
    private final int scaledWidth;
    private final int scaledHeight;

    public ScaledResolution(int width, int height, int scaleFactor) {
        this.scaledWidth = width / scaleFactor;
        this.scaledHeight = height / scaleFactor;
    }

    public int getScaledWidth() { return this.scaledWidth; }
    public int getScaledHeight() { return this.scaledHeight; }
    public int getScaleFactor() { return 2; }
}
