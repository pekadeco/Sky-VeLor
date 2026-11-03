package com.arena.pekaclient.feature.qol;

import java.util.List;
import java.util.regex.Pattern;

/** Deterministic rarity palette and static 16x16 slot-gradient opacity. */
public final class ItemRarityColors {
    private static final String[] ACCESSORY_RARITIES = {
            "VERY SPECIAL", "SPECIAL", "DEVINE", "MYTHIC", "LEGENDARY",
            "EPIC", "RARE", "UNCOMMON", "COMMON"
    };

    private static final String[] ITEM_RARITIES = {
            "VERY SPECIAL", "SPECIAL", "DEVINE", "MYTHIC", "LEGENDARY",
            "EPIC", "RARE", "UNCOMMON", "COMMON"
    };

    private static final Pattern[] ACCESSORY_LABELS = compileAccessoryLabels();
    private static final Pattern[] ITEM_LABELS = compileLabels(ITEM_RARITIES);

    private ItemRarityColors() {
    }

    public static int colorForLoreLines(List<String> lore) {
        if (lore == null || lore.isEmpty()) return 0;

        String[] lines = new String[lore.size()];
        for (int i = 0; i < lore.size(); i++) {
            lines[i] = stripFormattingPreservingCase(lore.get(i));
        }

        // Accessory labels always win, even if a regular rarity appears earlier in the lore.
        for (int rarityIndex = 0; rarityIndex < ACCESSORY_LABELS.length; rarityIndex++) {
            for (String line : lines) {
                if (containsUppercaseLabel(line, ACCESSORY_LABELS[rarityIndex])) {
                    return colorForRarity(ACCESSORY_RARITIES[rarityIndex]);
                }
            }
        }

        // Rarity words can appear inside a longer all-caps item classification line, such as
        // "C MYTHIC DUNGEON SWORD C". Match a complete uppercase word, not just an exact line.
        // The case-sensitive matcher leaves ordinary lowercase vault descriptions unclassified.
        for (int rarityIndex = 0; rarityIndex < ITEM_LABELS.length; rarityIndex++) {
            for (int lineIndex = lines.length - 1; lineIndex >= 0; lineIndex--) {
                if (containsUppercaseLabel(lines[lineIndex], ITEM_LABELS[rarityIndex])) {
                    return colorForRarity(ITEM_RARITIES[rarityIndex]);
                }
            }
        }
        return 0;
    }

    /** Rarity highlight reads the lore only; item names and unrelated display text are ignored. */
    public static int colorForItem(String displayName, List<String> lore) {
        return colorForLoreLines(lore);
    }

    private static boolean containsUppercaseLabel(String text, Pattern label) {
        return text != null && label.matcher(text).find();
    }

    private static Pattern[] compileAccessoryLabels() {
        Pattern[] patterns = new Pattern[ACCESSORY_RARITIES.length];
        for (int i = 0; i < ACCESSORY_RARITIES.length; i++) {
            patterns[i] = compileLabelPattern(ACCESSORY_RARITIES[i] + " ACCESSORY");
        }
        return patterns;
    }

    private static Pattern[] compileLabels(String[] labels) {
        Pattern[] patterns = new Pattern[labels.length];
        for (int i = 0; i < labels.length; i++) patterns[i] = compileLabelPattern(labels[i]);
        return patterns;
    }

    /**
     * Formatting codes are removed before matching. Labels remain case-sensitive and need word
     * boundaries, so an all-caps descriptor can contain a rarity word without matching "RARELY"
     * or "RARITY" as though either contained the actual rarity word "RARE".
     */
    private static Pattern compileLabelPattern(String label) {
        StringBuilder regex = new StringBuilder("(?<![A-Za-z0-9])");
        String[] words = label.split(" ");
        for (int i = 0; i < words.length; i++) {
            if (i > 0) regex.append("[^A-Za-z0-9]+");
            regex.append(Pattern.quote(words[i]));
        }
        regex.append("(?![A-Za-z0-9])");
        return Pattern.compile(regex.toString());
    }

    /** Removes Minecraft formatting codes without changing the case of the actual text. */
    private static String stripFormattingPreservingCase(String text) {
        if (text == null || text.isEmpty()) return "";
        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (current == '\u00a7' && i + 1 < text.length()) {
                i++;
                continue;
            }
            result.append(current);
        }
        return result.toString();
    }

    private static int colorForRarity(String rarity) {
        switch (rarity) {
            case "COMMON": return 0xFFAAAAAA;
            case "UNCOMMON": return 0xFF55FF55;
            case "RARE": return 0xFF5555FF;
            case "EPIC": return 0xFFAA00AA;
            case "LEGENDARY": return 0xFFFFAA00;
            case "MYTHIC": return 0xFFFF55FF;
            case "DEVINE": return 0xFF55FFFF;
            case "SPECIAL": return 0xFFFF5555;
            case "VERY SPECIAL": return 0xFFFF5555;
            default: return 0;
        }
    }

    /**
     * Returns a fixed opacity for one row of the 16x16 slot background.
     * The tint is almost transparent at the top and strongest in the lower-middle;
     * no time value is used, so the shape never animates.
     */
    public static int highlightAlphaForRow(int row) {
        if (row < 0 || row >= 16) return 0;
        float position = row / 15.0F;
        float lowerGradient = position * position;
        float distance = Math.abs(row - 11.0F);
        float lowerMiddlePlume = Math.max(0.0F, 1.0F - distance / 5.5F);
        float opacity = 0.04F + lowerGradient * 0.16F + lowerMiddlePlume * 0.30F;
        return Math.min(120, Math.max(0, Math.round(opacity * 255.0F)));
    }
}
