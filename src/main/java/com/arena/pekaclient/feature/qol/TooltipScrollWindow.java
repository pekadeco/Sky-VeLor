package com.arena.pekaclient.feature.qol;

/** Pixel-scroll policy for intact tooltips; mirrors Better NEF's wheel and keyboard step sizes. */
public final class TooltipScrollWindow {
    private static final int WHEEL_STEP_PIXELS = 10;
    private static final int KEY_STEP_PIXELS = 5;

    private TooltipScrollWindow() { }

    /** Positive wheel delta is wheel-up in LWJGL 2: shift the tooltip up by ten pixels. */
    public static int offsetAfterWheel(int currentOffset, int wheel) {
        if (wheel > 0) return currentOffset - WHEEL_STEP_PIXELS;
        if (wheel < 0) return currentOffset + WHEEL_STEP_PIXELS;
        return currentOffset;
    }

    /** Better NEF's Up key increments the draw offset; Down key decrements it. */
    public static int offsetAfterKey(int currentOffset, boolean upKey) {
        return currentOffset + (upKey ? KEY_STEP_PIXELS : -KEY_STEP_PIXELS);
    }

    /** The hook changes only the final vertical placement; all tooltip lines remain in GuiUtils' input list. */
    public static int applyVerticalOffset(int tooltipY, int offsetPixels) {
        return tooltipY + offsetPixels;
    }
}
