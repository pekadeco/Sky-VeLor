package com.arena.pekaclient.dungeon.solver;

import java.awt.Color;

/** Parses the Better NEF colour format: chroma speed, alpha, red, green, blue. */
public final class DungeonSolverColor {
    private static final Color FALLBACK = new Color(255, 255, 255, 255);

    private DungeonSolverColor() { }

    public static Color getColor(String value) {
        if (value == null) return FALLBACK;
        String[] parts = value.trim().split(":");
        if (parts.length != 5) return FALLBACK;
        try {
            int alpha = clamp(Integer.parseInt(parts[1]));
            int red = clamp(Integer.parseInt(parts[2]));
            int green = clamp(Integer.parseInt(parts[3]));
            int blue = clamp(Integer.parseInt(parts[4]));
            return new Color(red, green, blue, alpha);
        } catch (NumberFormatException ignored) {
            return FALLBACK;
        }
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
