package com.arena.pekaclient.gui;

import java.util.HashMap;
import java.util.Map;

/** Catalog ownership and search use the actual model consumed by the settings screen. */
public final class CategoryPlacementTests {
    private static void check(boolean value, String detail) { if (!value) throw new AssertionError(detail); }
    public static void main(String[] args) {
        String[][] expected = {
            {"GAMEPLAY", "no-block-particles", "disable-hotbar-scrolling", "hurt-camera-shake", "smooth-aspect-teleport", "hide-dead-mobs", "auto-sprint", "item-lock-swap", "inventory-assist", "chat-qol", "wardrobe-shortcut", "wardrobe-number-keys", "pets-shortcut", "command-keybinds"},
            {"DUNGEON", "dungeon-map", "terminal-assist", "spirit-leap", "dungeon-boss-hud", "spirit-bow-locator", "dungeon-score-hud", "room-clear-alert", "dungeon-auto-chest-close", "m7-overlay", "mob-esp-enabled", "door-esp-enabled", "dungeon-sync-enabled"},
            {"MINING", "mining-bps", "mining-assistant-settings"}, {"FISHING", "fishing-tracker"},
            {"SLAYER", "slayer-tracker"}, {"VISUALS", "hud-editor", "hud-player-doll", "hud-inventory", "item-animation"},
            {"SYSTEM", "system-appearance", "system-performance", "font-settings", "chat-editor", "system-general", "system-commands", "open-menu-key"}
        };
        Map<String, Integer> counts = new HashMap<String, Integer>();
        for (ReferenceSettingsCatalog.Tab tab : ReferenceSettingsCatalog.TABS)
            for (ReferenceSettingsCatalog.Card card : ReferenceSettingsCatalog.cards(tab))
                for (ReferenceSettingsCatalog.Setting setting : card.settings)
                    counts.put(setting.id, counts.containsKey(setting.id) ? counts.get(setting.id) + 1 : 1);
        for (String[] row : expected) for (int i = 1; i < row.length; i++) {
            String id = row[i];
            ReferenceSettingsCatalog.Tab tab = ReferenceSettingsCatalog.Tab.valueOf(row[0]);
            check(counts.containsKey(id) && counts.get(id) == 1, "missing or duplicate " + id);
            check(ReferenceSettingsCatalog.ownerOf(id) == tab, "wrong owner " + id);
            boolean found = false;
            for (ReferenceSettingsCatalog.Match match : ReferenceSettingsCatalog.search(id))
                if (match.setting.id.equals(id) && match.tab == tab) found = true;
            check(found, "search cannot find " + id);
        }
        check(ReferenceSettingsCatalog.cards(ReferenceSettingsCatalog.Tab.FARMING).isEmpty(), "farming must stay empty");
        System.out.println("CategoryPlacementTests passed");
    }
}
