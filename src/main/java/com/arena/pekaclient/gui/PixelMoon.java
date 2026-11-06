package com.arena.pekaclient.gui;

import com.arena.pekaclient.render.Theme;

/** A transparent 32-pixel Lunaria crescent. The CPU pixels are generated only when the texture is created. */
public final class PixelMoon {
    public static final int SIZE = 32;

    private PixelMoon() { }

    private static boolean solid(int x, int y) {
        int dx = x - 13, dy = y - 16;
        int ix = x - 18, iy = y - 11;
        boolean crescent = dx * dx + dy * dy <= 120 && ix * ix + iy * iy < 85;
        crescent = dx * dx + dy * dy <= 120 && !crescent;
        // Two deliberately square-pixel sparkles, in the open side of the crescent.
        boolean star1 = (Math.abs(x - 25) <= 1 && y == 8) || (x == 25 && Math.abs(y - 8) <= 2);
        boolean star2 = (Math.abs(x - 23) <= 1 && y == 14) || (x == 23 && Math.abs(y - 14) <= 1);
        return crescent || star1 || star2;
    }

    /** Returns pixels for one upload; the halo is a crisp, translucent pixel outline, not a blur. */
    public static int[] bake() {
        int[] pixels = new int[SIZE * SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (solid(x, y)) {
                    float t = y / (float) (SIZE - 1);
                    int a = Theme.MOON_BLUE, b = Theme.MOON_VIOLET;
                    int r = Math.round(((a >> 16) & 255) * (1f - t) + ((b >> 16) & 255) * t);
                    int g = Math.round(((a >> 8) & 255) * (1f - t) + ((b >> 8) & 255) * t);
                    int blue = Math.round((a & 255) * (1f - t) + (b & 255) * t);
                    pixels[y * SIZE + x] = (255 << 24) | (r << 16) | (g << 8) | blue;
                } else {
                    boolean near = false;
                    for (int oy = -1; oy <= 1 && !near; oy++) {
                        for (int ox = -1; ox <= 1; ox++) {
                            if (solid(x + ox, y + oy)) { near = true; break; }
                        }
                    }
                    if (near) pixels[y * SIZE + x] = (65 << 24) | (Theme.MOON_BLUE & Theme.RGB_MASK);
                }
            }
        }
        return pixels;
    }
}
