package com.arena.pekaclient.hud;

/** Pure fit calculation for an oversized alert without changing its saved size. */
public final class RoomAlertViewport {
    private RoomAlertViewport() { }
    public static float fit(float contentWidth, float contentHeight, float scale, int screenWidth, int screenHeight) {
        if (contentWidth <= 0F || contentHeight <= 0F || scale <= 0F) return 1F;
        float x = Math.max(1, screenWidth - 2) / (contentWidth * scale);
        float y = Math.max(1, screenHeight - 2) / (contentHeight * scale);
        return Math.min(1F, Math.min(x, y));
    }
}
