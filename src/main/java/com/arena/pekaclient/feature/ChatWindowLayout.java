package com.arena.pekaclient.feature;

import com.arena.pekaclient.config.ChatQolSettings;

/** Shared, clamped geometry for the two native chat windows and the editor's drag/resize preview. */
public final class ChatWindowLayout {

    public static final int SNAP_PIXELS = 6;
    public static final int RESIZE_GRIP = 9;
    public static final int MIN_PIXELS = 48;

    public static final class Rect {
        public int x;
        public int y;
        public int w;
        public int h;

        public boolean contains(int px, int py) {
            return px >= x && py >= y && px < x + w && py < y + h;
        }

        public boolean inResizeGrip(int px, int py) {
            return px >= x + Math.max(0, w - RESIZE_GRIP) && py >= y + Math.max(0, h - RESIZE_GRIP)
                    && px < x + w && py < y + h;
        }

        public boolean intersects(Rect other) {
            return other != null && x < other.x + other.w && x + w > other.x && y < other.y + other.h
                    && y + h > other.y;
        }
    }

    private ChatWindowLayout() {
    }

    /** Maps a bounded per-GUI-scale percentage geometry to on-screen pixels without letting it leave the viewport. */
    public static void update(Rect out, ChatQolSettings.Window window, int scale, int screenW, int screenH) {
        int sw = Math.max(0, screenW);
        int sh = Math.max(0, screenH);
        int wantedW = percent(window.getW(scale), sw);
        int wantedH = percent(window.getH(scale), sh);
        out.w = Math.min(sw, Math.max(Math.min(MIN_PIXELS, sw), wantedW));
        out.h = Math.min(sh, Math.max(Math.min(MIN_PIXELS, sh), wantedH));
        out.x = clamp(percent(window.getX(scale), sw), 0, Math.max(0, sw - out.w));
        out.y = clamp(percent(window.getY(scale), sh), 0, Math.max(0, sh - out.h));
    }

    /** Saves a moved rectangle back to its current GUI-scale slot, snapping only to safe viewport edges. */
    public static void move(ChatQolSettings.Window window, int scale, int screenW, int screenH,
                            int left, int top, boolean snap) {
        Rect current = new Rect();
        update(current, window, scale, screenW, screenH);
        int maxX = Math.max(0, screenW - current.w);
        int maxY = Math.max(0, screenH - current.h);
        int x = clamp(left, 0, maxX);
        int y = clamp(top, 0, maxY);
        if (snap) {
            if (x <= SNAP_PIXELS) x = 0;
            if (maxX - x <= SNAP_PIXELS) x = maxX;
            if (y <= SNAP_PIXELS) y = 0;
            if (maxY - y <= SNAP_PIXELS) y = maxY;
        }
        window.setGeometry(scale, toPercent(x, screenW), toPercent(y, screenH),
                toPercent(current.w, screenW), toPercent(current.h, screenH));
    }

    /** Saves a resized rectangle; the opposite corner stays fixed and all persisted values remain clamped. */
    public static void resize(ChatQolSettings.Window window, int scale, int screenW, int screenH,
                              int left, int top, int width, int height, boolean snap) {
        int sw = Math.max(0, screenW);
        int sh = Math.max(0, screenH);
        int x = clamp(left, 0, Math.max(0, sw - 1));
        int y = clamp(top, 0, Math.max(0, sh - 1));
        int minW = Math.min(MIN_PIXELS, Math.max(1, sw - x));
        int minH = Math.min(MIN_PIXELS, Math.max(1, sh - y));
        int w = clamp(width, minW, Math.max(minW, sw - x));
        int h = clamp(height, minH, Math.max(minH, sh - y));
        if (snap) {
            if (sw - (x + w) <= SNAP_PIXELS) w = sw - x;
            if (sh - (y + h) <= SNAP_PIXELS) h = sh - y;
        }
        window.setGeometry(scale, toPercent(x, sw), toPercent(y, sh), toPercent(w, sw), toPercent(h, sh));
    }

    private static int percent(int value, int whole) {
        return whole <= 0 ? 0 : Math.round(value * whole / 10000f);
    }

    private static int toPercent(int value, int whole) {
        return whole <= 0 ? 1000 : Math.round(clamp(value, 0, whole) * 10000f / whole);
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : (value > max ? max : value);
    }
}
