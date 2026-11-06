package com.arena.pekaclient.gui;

/** Checks the pixels uploaded by the real LunariaArtwork draw path. */
public final class PixelMoonTests {
    public static void main(String[] args) {
        int[] p = PixelMoon.bake();
        if (p.length != 32 * 32 || p[0] != 0 || p[31] != 0 || p[31 * 32] != 0)
            throw new AssertionError("moon must have a transparent background");
        if ((p[8 * 32 + 25] >>> 24) != 255 || (p[19 * 32 + 23] >>> 24) != 255)
            throw new AssertionError("both sparkles must be visible");
        boolean halo = false, color = false;
        for (int pixel : p) {
            if ((pixel >>> 24) == 65) halo = true;
            if ((pixel >>> 24) == 255 && (pixel & 255) > ((pixel >> 16) & 255)) color = true;
        }
        if (!halo || !color) throw new AssertionError("moon needs blue/violet pixels and a translucent pixel halo");
        System.out.println("pixel moon: transparent background, sparkles and crisp halo passed");
    }
}
