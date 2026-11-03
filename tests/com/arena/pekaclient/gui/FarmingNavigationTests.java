package com.arena.pekaclient.gui;

import java.util.Arrays;
import java.util.List;

/** Regression proof for the empty Farming channel and honest moves to Mining/Fishing. */
public final class FarmingNavigationTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        exactRailOrderIncludesTheTwoNewChannels();
        emptyFarmingMessageIsSearchableButOwnsNoSetting();
        movedChannelsRetainTheirExistingCatalogIds();
        done();
    }

    private static void exactRailOrderIncludesTheTwoNewChannels() {
        String[] expected = {"Home", "QoL", "Slayer", "Dungeons", "Farming", "Mining", "Fishing", "HUD & Visuals", "Misc"};
        check("Farming precedes Mining and Fishing in the exact requested order",
                Arrays.equals(expected, AppleSettingsScreen.destinationNames()));
    }

    private static void emptyFarmingMessageIsSearchableButOwnsNoSetting() {
        List<ReferenceSettingsCatalog.Match> matches = ReferenceSettingsCatalog.search("sprout");
        check("Farming exposes its empty-channel message through search", matches.size() == 1
                && matches.get(0).tab == ReferenceSettingsCatalog.Tab.FARMING
                && "No Farming Features Yet".equals(matches.get(0).setting.label));
        check("Farming title copy is title cased", "No Farming Features Yet.".equals(
                ReferenceSettingsCatalog.Tab.FARMING.caption));
        check("Farming does not fabricate a persisted feature card", ReferenceSettingsCatalog.cards(
                ReferenceSettingsCatalog.Tab.FARMING).isEmpty() && ReferenceSettingsCatalog.find("farming-empty") == null);
    }

    private static void movedChannelsRetainTheirExistingCatalogIds() {
        String[] mining = {"mining-bps", "mining-assistant-settings", "mining-ability-alert", "mining-puzzler"};
        for (String id : mining) check("Mining owner moved in presentation only: " + id,
                ReferenceSettingsCatalog.find(id) != null && ownerOf(id) == ReferenceSettingsCatalog.Tab.MINING);
        String[] fishing = {"fishing-tracker", "catch-counter", "fishing-festival", "sea-hp-hud-route", "jawbus", "nearby-encounters"};
        for (String id : fishing) check("Fishing owner moved in presentation only: " + id,
                ReferenceSettingsCatalog.find(id) != null && ownerOf(id) == ReferenceSettingsCatalog.Tab.FISHING);
        check("uncertain features stay in their genuine channels", ownerOf("slayer-tracker") == ReferenceSettingsCatalog.Tab.SLAYER
                && ownerOf("terminal-assist") == ReferenceSettingsCatalog.Tab.DUNGEON
                && ownerOf("item-animation") == ReferenceSettingsCatalog.Tab.VISUALS);
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

    private static void done() {
        System.out.println("channel navigation: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
