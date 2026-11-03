package com.arena.pekaclient.feature.qol;

import java.util.Arrays;
import java.util.Collections;

public final class ItemRarityColorsTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("recognizes formatted legendary line", color("§7Some lore", "§6§lLEGENDARY") == 0xFFFFAA00);
        check("recognizes decorated very special", color("✪✪ VERY SPECIAL") == 0xFFFF5555);
        check("recognizes uncommon line", color("§aUNCOMMON") == 0xFF55FF55);
        check("recognizes a rarity word inside a longer uppercase lore line", color("C MYTHIC DUNGEON SWORD C") == 0xFFFF55FF);
        check("recognizes an uppercase rarity word later in the lore line", color("DUNGEON ITEM TYPE: RARE HELMET") == 0xFF5555FF);
        check("recognizes epic item label with descriptor", color("§5EPIC HELMET") == 0xFFAA00AA);
        check("devine follows the requested name", color("§bDEVINE") == 0xFF55FFFF);
        check("recognizes common label", color("§7COMMON") == 0xFFAAAAAA);
        check("formatting and punctuation around a label are ignored", color("✦ §9RARE ✦") == 0xFF5555FF);
        check("formatting codes do not hide an uppercase label", color("§kC §dMYTHIC DUNGEON SWORD §kC") == 0xFFFF55FF);
        check("decorative punctuation between letters is accepted", color("M§5Y§5T§5H§5I§5C HELMET") == 0xFFFF55FF);

        check("very special wins over special", color("SPECIAL", "VERY SPECIAL") == 0xFFFF5555);
        check("uncommon does not match the common suffix", color("UNCOMMON") == 0xFF55FF55);
        check("lowercase rarity word is ignored", color("This item is rare") == 0);
        check("mixed-case rarity word is ignored", color("Mythic DUNGEON SWORD") == 0);
        check("rarity phrase without a rarity token is ignored", color("RARITY UPGRADED") == 0);
        check("rarely mentioned text does not create a rarity", color("This item is RARELY used") == 0);
        check("rarefied is not treated as rare", color("RAREFIED HELMET") == 0);
        check("rarity is not treated as rare", color("RARITY HELMET") == 0);
        check("display name alone does not set rarity", ItemRarityColors.colorForItem("RARE HELMET", Collections.<String>emptyList()) == 0);
        check("display name does not override a lore with no uppercase rarity", ItemRarityColors.colorForItem("MYTHIC HELMET", Arrays.asList("ordinary vault description")) == 0);
        check("uppercase rarity in lore is used despite unrelated display name", ItemRarityColors.colorForItem("Ordinary item", Arrays.asList("C MYTHIC DUNGEON SWORD C")) == 0xFFFF55FF);

        check("very special accessory is recognized", color("§dVERY SPECIAL ACCESSORY") == 0xFFFF5555);
        check("special accessory is recognized", color("§cSPECIAL ACCESSORY") == 0xFFFF5555);
        check("devine accessory is recognized", color("§bDEVINE ACCESSORY") == 0xFF55FFFF);
        check("mythic accessory is recognized", color("§dMYTHIC ACCESSORY") == 0xFFFF55FF);
        check("legendary accessory is recognized", color("§6LEGENDARY ACCESSORY") == 0xFFFFAA00);
        check("epic accessory is recognized", color("§5EPIC ACCESSORY") == 0xFFAA00AA);
        check("rare accessory is recognized", color("§9RARE ACCESSORY") == 0xFF5555FF);
        check("uncommon accessory is recognized", color("§aUNCOMMON ACCESSORY") == 0xFF55FF55);
        check("common accessory is recognized", color("§7COMMON ACCESSORY") == 0xFFAAAAAA);
        check("accessory label later in the line is recognized", color("VAULT ITEM TYPE: MYTHIC ACCESSORY") == 0xFFFF55FF);
        check("accessory rarity beats a regular rarity elsewhere in lore", color("MYTHIC DUNGEON SWORD", "RARE ACCESSORY") == 0xFF5555FF);
        check("highest accessory rarity wins when accessory labels conflict", color("COMMON ACCESSORY", "MYTHIC ACCESSORY") == 0xFFFF55FF);
        check("lowercase accessory mention is ignored", color("This item is a rare accessory") == 0);

        check("empty lore returns no border", ItemRarityColors.colorForLoreLines(Collections.<String>emptyList()) == 0);
        check("null lore returns no border", ItemRarityColors.colorForLoreLines(null) == 0);
        check("static highlight alpha is low at top", ItemRarityColors.highlightAlphaForRow(0) > 0
                && ItemRarityColors.highlightAlphaForRow(0) < 16);
        check("highlight is strongest near lower middle", ItemRarityColors.highlightAlphaForRow(11)
                > ItemRarityColors.highlightAlphaForRow(0));
        check("gradient fades before the bottom edge", ItemRarityColors.highlightAlphaForRow(11)
                > ItemRarityColors.highlightAlphaForRow(15));
        check("highlight is visibly stronger", ItemRarityColors.highlightAlphaForRow(11) >= 96);
        check("highlight alpha is bounded", ItemRarityColors.highlightAlphaForRow(11) <= 120);
        check("highlight rejects rows outside slot", ItemRarityColors.highlightAlphaForRow(16) == 0);
        check("negative row has no alpha", ItemRarityColors.highlightAlphaForRow(-1) == 0);
        System.out.println("item rarity colors: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static int color(String... lines) {
        return ItemRarityColors.colorForLoreLines(Arrays.asList(lines));
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++;
        else { failed++; System.out.println("FAIL " + name); }
    }
}
