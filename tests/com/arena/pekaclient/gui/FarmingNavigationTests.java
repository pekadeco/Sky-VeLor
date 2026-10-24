package com.arena.pekaclient.gui;

import java.util.Arrays;
import java.util.List;

/**
 * Regression proof for the Farming navigation contract: this is presentation-only and must never fabricate a farm
 * setting, migrate a config key, or repurpose mining/fishing state as crop automation.
 */
public final class FarmingNavigationTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        exactRailOrderAndEmptyState();
        searchFindsTheEmptyFarmingDestination();
        movedPresentationKeepsEveryRetainedSettingId();
        System.out.println("farming navigation: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void exactRailOrderAndEmptyState() {
        String[] expected = {"Home", "Gameplay", "Dungeon", "Farming", "Visuals", "System"};
        check("Farming follows Dungeon in the exact requested rail order",
                Arrays.equals(expected, AppleSettingsScreen.destinationNames()));
        check("Farming has no fabricated card or config setting", ReferenceSettingsCatalog.cards(
                ReferenceSettingsCatalog.Tab.FARMING).isEmpty()
                && ReferenceSettingsCatalog.find("farming-empty") == null);
    }

    private static void searchFindsTheEmptyFarmingDestination() {
        List<ReferenceSettingsCatalog.Match> matches = ReferenceSettingsCatalog.search("sprout");
        check("Farming empty state is searchable by its navigation keywords", matches.size() == 1
                && matches.get(0).tab == ReferenceSettingsCatalog.Tab.FARMING
                && "No farming features yet".equals(matches.get(0).setting.label)
                && matches.get(0).setting.control == ReferenceSettingsCatalog.Control.INFO);
    }

    private static void movedPresentationKeepsEveryRetainedSettingId() {
        String[] gameplayIds = {"auto-sprint", "mining-bps", "mining-assistant-settings", "slayer-tracker",
                "slayer-hud-route", "fishing-hud-route", "catch-counter", "sea-hp-hud-route", "jawbus"};
        for (String id : gameplayIds) {
            check("existing Gameplay configuration id remains reachable: " + id,
                    ReferenceSettingsCatalog.find(id) != null && ownerOf(id) == ReferenceSettingsCatalog.Tab.GAMEPLAY);
        }
        check("Visuals keeps HUD and Item Animation config routes",
                ownerOf("hud-editor") == ReferenceSettingsCatalog.Tab.VISUALS
                        && ownerOf("item-animation") == ReferenceSettingsCatalog.Tab.VISUALS);
        check("Dungeon and System keys retain their existing destinations",
                ownerOf("terminal-assist") == ReferenceSettingsCatalog.Tab.DUNGEON
                        && ownerOf("appearance-theme") == ReferenceSettingsCatalog.Tab.SYSTEM);
    }

    private static ReferenceSettingsCatalog.Tab ownerOf(String id) {
        for (ReferenceSettingsCatalog.Tab tab : ReferenceSettingsCatalog.TABS) {
            for (ReferenceSettingsCatalog.Card card : ReferenceSettingsCatalog.cards(tab)) {
                if (contains(card.settings, id)) return tab;
            }
        }
        return null;
    }

    private static boolean contains(List<ReferenceSettingsCatalog.Setting> settings, String id) {
        for (ReferenceSettingsCatalog.Setting setting : settings) {
            if (id.equals(setting.id) || contains(setting.children, id)) return true;
        }
        return false;
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++; else { failed++; System.out.println("FAIL " + name); }
    }
}
