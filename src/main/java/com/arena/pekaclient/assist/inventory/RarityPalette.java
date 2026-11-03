package com.arena.pekaclient.assist.inventory;

import java.util.List;

/**
 * Local, read-only SkyBlock rarity parsing for the optional inventory tint. It purposefully accepts only explicit
 * rarity words from item display/lore text: unrecognised items receive no overlay rather than a guessed colour.
 */
public final class RarityPalette {

    private RarityPalette() {
    }

    public static int colorFor(String displayName, List<String> lore) {
        if (lore != null) {
            for (int index = lore.size() - 1; index >= 0; index--) {
                int color = colorForText(lore.get(index));
                if (color != 0) return color;
            }
        }
        return colorForText(displayName);
    }

    public static int colorForText(String text) {
        if (text == null || text.length() == 0) return 0;
        String normalized = rarityWords(text);
        // Test the compound phrase before SPECIAL, which it contains.
        if (normalized.indexOf("VERYSPECIAL") >= 0) return 0xFFFF5555;
        if (normalized.indexOf("SUPREME") >= 0) return 0xFFAA0000;
        if (normalized.indexOf("DIVINE") >= 0) return 0xFF55FFFF;
        if (normalized.indexOf("MYTHIC") >= 0) return 0xFFFF55FF;
        if (normalized.indexOf("LEGENDARY") >= 0) return 0xFFFFAA00;
        if (normalized.indexOf("EPIC") >= 0) return 0xFFAA00AA;
        if (normalized.indexOf("RARE") >= 0) return 0xFF5555FF;
        if (normalized.indexOf("UNCOMMON") >= 0) return 0xFF55FF55;
        if (normalized.indexOf("COMMON") >= 0) return 0xFFAAAAAA;
        if (normalized.indexOf("SPECIAL") >= 0) return 0xFFFF5555;
        return 0;
    }

    /**
     * Rarity parsing only needs formatted-text cleanup, kept local so it does not retain an Inventory Search runtime
     * dependency or any filter/query surface. Spaces and punctuation are deliberately omitted for compound words.
     */
    private static String rarityWords(String value) {
        StringBuilder out = new StringBuilder(Math.min(value.length(), 512));
        for (int index = 0; index < value.length() && out.length() < 512; index++) {
            char c = value.charAt(index);
            if (c == '\u00a7' && index + 1 < value.length()) {
                index++;
            } else if (Character.isLetterOrDigit(c)) {
                out.append(Character.toUpperCase(c));
            }
        }
        return out.toString();
    }

    /** Static lower-weighted alpha: never animated, so it has no time-based cost or visual flicker. */
    public static int alphaAtRow(int row) {
        if (row < 0 || row > 15) return 0;
        int lower = row * row;
        return Math.min(92, 8 + lower / 3);
    }
}
