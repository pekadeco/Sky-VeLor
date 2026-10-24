package com.arena.pekaclient.gui;

import com.arena.pekaclient.config.FeatureUiPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/** Headless contract for the persisted inline settings catalog. */
public final class InlineSettingsCatalogTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        exactEightDestinationOrder();
        everyFeatureUsesAnInlineOwner();
        requiredOwnersAndMovesArePresent();
        searchCarriesAndOpensItsParentPath();
        dropdownStateIsCollapsedByDefaultAndPersistsSeparately();
        catalogIdsAreUniqueAndNoFocusedEditorsRemain();
        done();
    }

    private static void exactEightDestinationOrder() {
        String[] expected = {"Home", "Gameplay", "Dungeon", "Farming", "Mining", "Fishing", "Visuals", "System"};
        check("sidebar order is Home Gameplay Dungeon Farming Mining Fishing Visuals System",
                Arrays.equals(expected, AppleSettingsScreen.destinationNames()));
        check("catalog tabs use the same eight-tab order", ReferenceSettingsCatalog.TABS.length == expected.length
                && ReferenceSettingsCatalog.TABS[0] == ReferenceSettingsCatalog.Tab.HOME
                && ReferenceSettingsCatalog.TABS[1] == ReferenceSettingsCatalog.Tab.GAMEPLAY
                && ReferenceSettingsCatalog.TABS[2] == ReferenceSettingsCatalog.Tab.DUNGEON
                && ReferenceSettingsCatalog.TABS[3] == ReferenceSettingsCatalog.Tab.FARMING
                && ReferenceSettingsCatalog.TABS[4] == ReferenceSettingsCatalog.Tab.MINING
                && ReferenceSettingsCatalog.TABS[5] == ReferenceSettingsCatalog.Tab.FISHING
                && ReferenceSettingsCatalog.TABS[6] == ReferenceSettingsCatalog.Tab.VISUALS
                && ReferenceSettingsCatalog.TABS[7] == ReferenceSettingsCatalog.Tab.SYSTEM);
        check("Farming remains an explicit empty channel", ReferenceSettingsCatalog.cards(ReferenceSettingsCatalog.Tab.FARMING).isEmpty()
                && ReferenceSettingsCatalog.search("sprout").size() == 1);
    }

    private static void everyFeatureUsesAnInlineOwner() {
        for (ReferenceSettingsCatalog.Tab tab : ReferenceSettingsCatalog.TABS) {
            for (ReferenceSettingsCatalog.Card card : ReferenceSettingsCatalog.cards(tab)) {
                for (ReferenceSettingsCatalog.Setting setting : card.settings) {
                    check("top-level row is toggle or dropdown: " + setting.id,
                            setting.control == ReferenceSettingsCatalog.Control.TOGGLE
                                    || setting.control == ReferenceSettingsCatalog.Control.DROPDOWN);
                    assertInlineChildren(setting);
                }
            }
        }
        check("only allowed dedicated editor actions remain", dedicatedEditorActionIds().equals(Arrays.asList(
                "open-hud-editor", "item-animation-open-editor")));
        check("retained maintenance actions never reopen a focused feature screen", noRetiredOpenActionLabels());
    }

    private static void assertInlineChildren(ReferenceSettingsCatalog.Setting setting) {
        if (setting.hasChildren()) {
            check("owner has a disclosure row: " + setting.id, setting.isDropdown());
            for (ReferenceSettingsCatalog.Setting child : setting.children) assertInlineChildren(child);
        }
    }

    private static void requiredOwnersAndMovesArePresent() {
        check("required feature ownership remains complete", ReferenceSettingsCatalog.routesAreComplete());
        String[] preserved = {
                "no-block-particles", "disable-hotbar-scrolling", "hurt-camera-shake", "auto-sprint",
                "item-lock-swap", "chat-qol", "slayer-tracker", "dungeon-map", "terminal-assist",
                "spirit-leap", "dungeon-boss-hud", "m7-overlay", "mob-esp-enabled", "door-esp-enabled",
                "dungeon-sync-enabled", "mining-bps", "mining-assistant-settings", "catch-counter",
                "fishing-tracker", "hud-editor", "item-animation", "system-appearance", "system-performance"
        };
        for (String id : preserved) check("preserved option owner: " + id, ReferenceSettingsCatalog.find(id) != null);
        check("genuine Mining options moved without a new owner", ownerOf("mining-bps") == ReferenceSettingsCatalog.Tab.MINING
                && ownerOf("mining-assistant-settings") == ReferenceSettingsCatalog.Tab.MINING);
        check("genuine Fishing options moved without a new owner", ownerOf("fishing-tracker") == ReferenceSettingsCatalog.Tab.FISHING
                && ownerOf("catch-counter") == ReferenceSettingsCatalog.Tab.FISHING
                && ownerOf("sea-hp-hud-route") == ReferenceSettingsCatalog.Tab.FISHING);
        check("Slayer stays in Gameplay and HUD/item animation remain in Visuals",
                ownerOf("slayer-tracker") == ReferenceSettingsCatalog.Tab.GAMEPLAY
                        && ownerOf("hud-editor") == ReferenceSettingsCatalog.Tab.VISUALS
                        && ownerOf("item-animation") == ReferenceSettingsCatalog.Tab.VISUALS);
        check("Terminal Helper is a no-master status dropdown", ReferenceSettingsCatalog.find("terminal-assist") != null
                && ReferenceSettingsCatalog.find("terminal-assist").control == ReferenceSettingsCatalog.Control.DROPDOWN);
        check("Mob and Door ESP remain compact nested tables", ReferenceSettingsCatalog.find("esp-starred-color") != null
                && ReferenceSettingsCatalog.find("door-style-color") != null
                && ReferenceSettingsCatalog.find("esp-starred").isDropdown()
                && ReferenceSettingsCatalog.find("door-wither-style").isDropdown());
    }

    private static void searchCarriesAndOpensItsParentPath() {
        ReferenceSettingsCatalog.Match chat = matchFor("chat-tab-filter-text");
        check("search finds hidden Chat custom-tab field", chat != null);
        check("search carries Chat QoL and Tabs disclosure parents", chat != null && parentIds(chat).equals(
                Arrays.asList("chat-qol", "chat-tabs-group")));
        ReferenceSettingsCatalog.Match m7 = matchFor("m7-dragon-flame");
        check("search carries nested M7 disclosure parents", m7 != null && parentIds(m7).equals(
                Arrays.asList("m7-overlay", "m7-dragons")));
        Properties p = new Properties();
        FeatureUiPreferences.load(p);
        openParents(chat);
        check("search expansion stores every Chat parent", FeatureUiPreferences.dropdownOpen("chat-qol")
                && FeatureUiPreferences.dropdownOpen("chat-tabs-group"));
        FeatureUiPreferences.save(p);
        check("search expansion serializes presentation keys only", "true".equals(p.getProperty("ui.feature.dropdown.chat-qol"))
                && "true".equals(p.getProperty("ui.feature.dropdown.chat-tabs-group")));
    }

    private static void dropdownStateIsCollapsedByDefaultAndPersistsSeparately() {
        FeatureUiState state = new FeatureUiState();
        check("unknown inline dropdown starts collapsed", !state.dropdownOpen("item-lock-swap"));
        Properties original = new Properties();
        original.setProperty("dungeonMap", "false");
        original.setProperty("ui.feature.tree.retired", "ignored");
        original.setProperty("ui.feature.dropdown.item-lock-swap", "true");
        state.load(original);
        check("saved disclosure reopens the same row", state.dropdownOpen("item-lock-swap"));
        check("retired tree state does not reopen a row", !state.dropdownOpen("retired"));
        state.setDropdownOpen("item-lock-swap", false);
        state.setDropdownOpen("chat-qol", true);
        state.save(original);
        check("existing gameplay key survives UI-state save", "false".equals(original.getProperty("dungeonMap")));
        check("state save writes only additive ui.feature.dropdown keys",
                "false".equals(original.getProperty("ui.feature.dropdown.item-lock-swap"))
                        && "true".equals(original.getProperty("ui.feature.dropdown.chat-qol")));
    }

    private static void catalogIdsAreUniqueAndNoFocusedEditorsRemain() {
        List<String> ids = ReferenceSettingsCatalog.allIds();
        check("catalog option ids are unique", ids.size() == new HashSet<String>(ids).size());
        String[] retired = {
                "com.arena.pekaclient.gui.FeatureSettingsEditorScreen",
                "com.arena.pekaclient.gui.FeatureSettingsEditors",
                "com.arena.pekaclient.gui.DungeonEspEditorSpecs",
                "com.arena.pekaclient.gui.ChatEditorScreen",
                "com.arena.pekaclient.gui.ModSettingsScreen"
        };
        for (String name : retired) check("retired focused screen is absent: " + name, !classExists(name));
    }

    private static List<String> dedicatedEditorActionIds() {
        List<String> values = new ArrayList<String>();
        for (String id : ReferenceSettingsCatalog.allIds()) {
            ReferenceSettingsCatalog.Setting setting = ReferenceSettingsCatalog.find(id);
            if (setting != null && setting.control == ReferenceSettingsCatalog.Control.ACTION
                    && setting.actionLabel.endsWith("Editor")) values.add(id);
        }
        return values;
    }

    private static boolean noRetiredOpenActionLabels() {
        for (String id : ReferenceSettingsCatalog.allIds()) {
            ReferenceSettingsCatalog.Setting setting = ReferenceSettingsCatalog.find(id);
            if (setting == null || setting.control != ReferenceSettingsCatalog.Control.ACTION) continue;
            if ("open-hud-editor".equals(id) || "item-animation-open-editor".equals(id)) continue;
            String label = setting.actionLabel.toLowerCase(java.util.Locale.ROOT);
            if (label.startsWith("open") || label.contains("settings") || label.contains("details") || label.contains("controls")) {
                return false;
            }
        }
        return true;
    }

    private static void openParents(ReferenceSettingsCatalog.Match match) {
        if (match == null) return;
        for (ReferenceSettingsCatalog.Setting parent : match.parents) FeatureUiPreferences.setDropdownOpen(parent.id, true);
        if (match.setting.isDropdown()) FeatureUiPreferences.setDropdownOpen(match.setting.id, true);
    }

    private static ReferenceSettingsCatalog.Match matchFor(String id) {
        for (ReferenceSettingsCatalog.Match match : ReferenceSettingsCatalog.search(id.replace('-', ' '))) {
            if (id.equals(match.setting.id)) return match;
        }
        return null;
    }

    private static List<String> parentIds(ReferenceSettingsCatalog.Match match) {
        List<String> values = new ArrayList<String>();
        if (match != null) for (ReferenceSettingsCatalog.Setting parent : match.parents) values.add(parent.id);
        return values;
    }

    private static ReferenceSettingsCatalog.Tab ownerOf(String id) {
        for (ReferenceSettingsCatalog.Tab tab : ReferenceSettingsCatalog.TABS) {
            for (ReferenceSettingsCatalog.Card card : ReferenceSettingsCatalog.cards(tab)) {
                if (contains(card.settings, id)) return tab;
            }
        }
        return null;
    }

    private static boolean contains(List<ReferenceSettingsCatalog.Setting> values, String id) {
        for (ReferenceSettingsCatalog.Setting value : values) {
            if (id.equals(value.id) || contains(value.children, id)) return true;
        }
        return false;
    }

    private static boolean classExists(String binaryName) {
        return InlineSettingsCatalogTests.class.getClassLoader()
                .getResource(binaryName.replace('.', '/') + ".class") != null;
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++; else { failed++; System.out.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("inline settings catalog: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
