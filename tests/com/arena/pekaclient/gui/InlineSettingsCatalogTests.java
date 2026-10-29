package com.arena.pekaclient.gui;

import com.arena.pekaclient.config.FeatureUiPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

/** Headless contract for the persisted flat inline settings catalog. */
public final class InlineSettingsCatalogTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        exactNineDestinationOrder();
        everyFeatureUsesAFlatInlineOwner();
        requiredOwnersAndMovesArePresent();
        searchOpensOneOwningDropdown();
        dropdownStateIsCollapsedByDefaultAndResetsForFreshGui();
        catalogIdsAreUniqueAndEditorDestinationsRemainIntentional();
        optionalGuiTransparencyKeepsItsExistingOwnerAndKeys();
        done();
    }

    private static void exactNineDestinationOrder() {
        String[] expected = {"Home", "QoL", "Slayer", "Dungeons", "Farming", "Mining", "Fishing", "HUD & Visuals", "Misc"};
        check("sidebar order is Home QoL Slayer Dungeons Farming Mining Fishing HUD & Visuals Misc",
                Arrays.equals(expected, AppleSettingsScreen.destinationNames()));
        check("catalog tabs use the same nine-tab order", ReferenceSettingsCatalog.TABS.length == expected.length
                && ReferenceSettingsCatalog.TABS[0] == ReferenceSettingsCatalog.Tab.HOME
                && ReferenceSettingsCatalog.TABS[1] == ReferenceSettingsCatalog.Tab.GAMEPLAY
                && ReferenceSettingsCatalog.TABS[2] == ReferenceSettingsCatalog.Tab.SLAYER
                && ReferenceSettingsCatalog.TABS[8] == ReferenceSettingsCatalog.Tab.SYSTEM);
        check("Farming remains an explicit empty channel", ReferenceSettingsCatalog.cards(ReferenceSettingsCatalog.Tab.FARMING).isEmpty()
                && ReferenceSettingsCatalog.search("sprout").size() == 1);
    }

    private static void everyFeatureUsesAFlatInlineOwner() {
        for (ReferenceSettingsCatalog.Tab tab : ReferenceSettingsCatalog.TABS) {
            for (ReferenceSettingsCatalog.Card card : ReferenceSettingsCatalog.cards(tab)) {
                for (ReferenceSettingsCatalog.Setting setting : card.settings) {
                    check("top-level row is a supported player row: " + setting.id,
                            setting.control == ReferenceSettingsCatalog.Control.TOGGLE
                                    || setting.control == ReferenceSettingsCatalog.Control.SLIDER
                                    || setting.control == ReferenceSettingsCatalog.Control.DROPDOWN
                                    || setting.control == ReferenceSettingsCatalog.Control.EDITOR
                                    || setting.control == ReferenceSettingsCatalog.Control.KEYBIND
                                    || setting.isResetAction());
                    for (ReferenceSettingsCatalog.Setting child : setting.children) {
                        check("expanded option is never a nested disclosure: " + child.id, !child.isDropdown());
                    }
                }
            }
        }
        check("HUD, Item Animation, and Command Keybind editor routes are explicit", dedicatedEditorActionIds().equals(Arrays.asList(
                "open-command-keybind-editor", "open-hud-editor", "item-animation-open-editor")));
        check("maintenance actions never reopen a feature screen", noRetiredOpenActionLabels());
    }

    private static void requiredOwnersAndMovesArePresent() {
        check("required feature ownership remains complete", ReferenceSettingsCatalog.routesAreComplete());
        String[] preserved = {
                "no-block-particles", "disable-hotbar-scrolling", "hurt-camera-shake", "smooth-aspect-teleport", "auto-sprint",
                "item-lock-swap", "item-lock-key", "chat-qol", "wardrobe-shortcut", "wardrobe-number-keys", "pets-shortcut", "command-keybinds", "slayer-tracker", "dungeon-map", "terminal-assist",
                "spirit-leap", "dungeon-boss-hud", "spirit-bow-locator", "m7-overlay", "mob-esp-enabled", "door-esp-enabled",
                "dungeon-sync-enabled", "mining-bps", "mining-assistant-settings", "catch-counter",
                "fishing-tracker", "hud-editor", "hud-dungeon-run", "item-animation", "system-appearance", "system-performance"
        };
        for (String id : preserved) check("preserved option owner: " + id, ReferenceSettingsCatalog.find(id) != null);
        check("genuine Mining options moved without a new owner", ReferenceSettingsCatalog.ownerOf("mining-bps") == ReferenceSettingsCatalog.Tab.MINING
                && ReferenceSettingsCatalog.ownerOf("mining-assistant-settings") == ReferenceSettingsCatalog.Tab.MINING);
        check("genuine Fishing options moved without a new owner", ReferenceSettingsCatalog.ownerOf("fishing-tracker") == ReferenceSettingsCatalog.Tab.FISHING
                && ReferenceSettingsCatalog.ownerOf("catch-counter") == ReferenceSettingsCatalog.Tab.FISHING
                && ReferenceSettingsCatalog.ownerOf("sea-hp-hud-route") == ReferenceSettingsCatalog.Tab.FISHING);
        check("Slayer owns its existing tracker and HUD/item animation remain in HUD & Visuals",
                ReferenceSettingsCatalog.ownerOf("slayer-tracker") == ReferenceSettingsCatalog.Tab.SLAYER
                        && ReferenceSettingsCatalog.ownerOf("hud-editor") == ReferenceSettingsCatalog.Tab.VISUALS
                        && ReferenceSettingsCatalog.ownerOf("hud-dungeon-run") == ReferenceSettingsCatalog.Tab.VISUALS
                        && ReferenceSettingsCatalog.ownerOf("item-animation") == ReferenceSettingsCatalog.Tab.VISUALS);
        check("new local dungeon readouts use their single intended owners",
                ReferenceSettingsCatalog.ownerOf("spirit-bow-locator") == ReferenceSettingsCatalog.Tab.DUNGEON
                        && ReferenceSettingsCatalog.find("spirit-bow-locator") != null
                        && ReferenceSettingsCatalog.find("hud-dungeon-run") != null);
        check("Terminal Helper is a no-master dropdown", ReferenceSettingsCatalog.find("terminal-assist") != null
                && ReferenceSettingsCatalog.find("terminal-assist").control == ReferenceSettingsCatalog.Control.DROPDOWN);
        check("Starred Mobs and Door ESP keep every flat style control", ReferenceSettingsCatalog.find("esp-starred-color") != null
                && ReferenceSettingsCatalog.find("door-style-color") != null
                && !ReferenceSettingsCatalog.find("esp-starred").isDropdown()
                && !ReferenceSettingsCatalog.find("door-wither-style").isDropdown());
    }

    private static void searchOpensOneOwningDropdown() {
        ReferenceSettingsCatalog.Match chat = matchFor("chat-tab-filter-text");
        check("search finds hidden Chat custom-tab field", chat != null);
        check("search carries only the Chat QoL owner", chat != null && parentIds(chat).equals(
                Arrays.asList("chat-qol")));
        ReferenceSettingsCatalog.Match m7 = matchFor("m7-dragon-flame");
        check("search carries only the M7 owner", m7 != null && parentIds(m7).equals(
                Arrays.asList("m7-overlay")));
        ReferenceSettingsCatalog.Match command = matchFor("command-keybinds");
        ReferenceSettingsCatalog.Card shortcutCard = cardContainingDirectSetting("command-keybinds");
        check("search exposes Command Keybind directly in the standalone Shortcut card", command != null
                && parentIds(command).isEmpty() && shortcutCard != null && "shortcut".equals(shortcutCard.id)
                && "Shortcut".equals(shortcutCard.title));
        ReferenceSettingsCatalog.Setting itemLockKey = ReferenceSettingsCatalog.find("item-lock-key");
        check("Item Lock has an explicit click-to-capture key field", itemLockKey != null
                && itemLockKey.control == ReferenceSettingsCatalog.Control.KEYBIND
                && itemLockKey.description.contains("Backspace clears"));
        ReferenceSettingsCatalog.Setting petsKey = ReferenceSettingsCatalog.find("pets-shortcut");
        ReferenceSettingsCatalog.Setting wardrobeNumbers = ReferenceSettingsCatalog.find("wardrobe-number-keys");
        check("Shortcut exposes Pets capture and the Wardrobe number-key preference", petsKey != null
                && petsKey.control == ReferenceSettingsCatalog.Control.KEYBIND && petsKey.hasMasterToggle()
                && wardrobeNumbers != null && wardrobeNumbers.control == ReferenceSettingsCatalog.Control.TOGGLE);
        Properties p = new Properties();
        FeatureUiPreferences.load(p);
        openParents(chat);
        check("search expansion stores its one owning dropdown", FeatureUiPreferences.dropdownOpen("chat-qol")
                && !FeatureUiPreferences.dropdownOpen("chat-tabs-group"));
        FeatureUiPreferences.save(p);
        check("search expansion serializes presentation keys only", "true".equals(p.getProperty("ui.feature.dropdown.chat-qol"))
                && p.getProperty("ui.feature.dropdown.chat-tabs-group") == null);
    }

    private static void dropdownStateIsCollapsedByDefaultAndResetsForFreshGui() {
        FeatureUiState state = new FeatureUiState();
        check("unknown inline dropdown starts collapsed", !state.dropdownOpen("item-lock-swap"));
        Properties original = new Properties();
        original.setProperty("dungeonMap", "false");
        original.setProperty("slayer.tracker", "false");
        original.setProperty("ui.feature.tree.retired", "ignored");
        original.setProperty("ui.feature.dropdown.item-lock-swap", "true");
        state.load(original);
        check("legacy saved disclosure remains readable before fresh-screen reset", state.dropdownOpen("item-lock-swap"));
        check("retired tree state does not reopen a row", !state.dropdownOpen("retired"));
        state.openAccordion("chat-qol", ReferenceSettingsCatalog.dropdownIds(ReferenceSettingsCatalog.Tab.GAMEPLAY));
        state.setDropdownOpen("dungeon-map", true);
        state.save(original);
        check("existing gameplay and Slayer keys survive presentation-only UI state save",
                "false".equals(original.getProperty("dungeonMap"))
                        && "false".equals(original.getProperty("slayer.tracker")));
        check("accordion save closes peers and writes additive presentation keys",
                "false".equals(original.getProperty("ui.feature.dropdown.item-lock-swap"))
                        && "true".equals(original.getProperty("ui.feature.dropdown.chat-qol")));
        state.closeAllDropdowns();
        check("fresh GUI reset closes loaded and active disclosures", !state.dropdownOpen("item-lock-swap")
                && !state.dropdownOpen("chat-qol") && !state.dropdownOpen("dungeon-map"));
        state.save(original);
        check("fresh GUI reset serializes false instead of resurrecting legacy disclosure state",
                "false".equals(original.getProperty("ui.feature.dropdown.item-lock-swap"))
                        && "false".equals(original.getProperty("ui.feature.dropdown.chat-qol"))
                        && "false".equals(original.getProperty("ui.feature.dropdown.dungeon-map")));
        state.openAccordion("chat-qol", ReferenceSettingsCatalog.dropdownIds(ReferenceSettingsCatalog.Tab.GAMEPLAY));
        check("explicit Chat route can disclose its target after the normal fresh-GUI reset",
                state.dropdownOpen("chat-qol") && !state.dropdownOpen("item-lock-swap"));
    }

    private static void optionalGuiTransparencyKeepsItsExistingOwnerAndKeys() {
        ReferenceSettingsCatalog.Setting toggle = ReferenceSettingsCatalog.find("appearance-opacity-on");
        ReferenceSettingsCatalog.Setting slider = ReferenceSettingsCatalog.find("appearance-opacity");
        check("custom GUI transparency stays an explicit optional toggle in Appearance", toggle != null
                && toggle.control == ReferenceSettingsCatalog.Control.TOGGLE
                && "Custom GUI Transparency".equals(toggle.label)
                && ReferenceSettingsCatalog.ownerOf(toggle.id) == ReferenceSettingsCatalog.Tab.SYSTEM);
        check("GUI transparency slider keeps its established binding id and useful glass range", slider != null
                && slider.control == ReferenceSettingsCatalog.Control.SLIDER && "GUI Transparency".equals(slider.label)
                && slider.min == 45 && slider.max == 100 && "% opacity".equals(slider.suffix));
    }

    private static void catalogIdsAreUniqueAndEditorDestinationsRemainIntentional() {
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

    private static ReferenceSettingsCatalog.Card cardContainingDirectSetting(String id) {
        for (ReferenceSettingsCatalog.Tab tab : ReferenceSettingsCatalog.TABS) {
            for (ReferenceSettingsCatalog.Card card : ReferenceSettingsCatalog.cards(tab)) {
                for (ReferenceSettingsCatalog.Setting setting : card.settings) {
                    if (id.equals(setting.id)) return card;
                }
            }
        }
        return null;
    }

    private static List<String> dedicatedEditorActionIds() {
        List<String> values = new ArrayList<String>();
        for (String id : ReferenceSettingsCatalog.allIds()) {
            ReferenceSettingsCatalog.Setting setting = ReferenceSettingsCatalog.find(id);
            if (setting != null && setting.isEditor() && !values.contains(setting.editorActionId)) values.add(setting.editorActionId);
        }
        return values;
    }

    private static boolean noRetiredOpenActionLabels() {
        for (String id : ReferenceSettingsCatalog.allIds()) {
            ReferenceSettingsCatalog.Setting setting = ReferenceSettingsCatalog.find(id);
            if (setting == null || setting.control != ReferenceSettingsCatalog.Control.ACTION) continue;
            String label = setting.actionLabel.toLowerCase(java.util.Locale.ROOT);
            if (label.startsWith("open") || label.contains("settings") || label.contains("details") || label.contains("controls")) {
                return false;
            }
        }
        return true;
    }

    private static void openParents(ReferenceSettingsCatalog.Match match) {
        if (match == null) return;
        for (ReferenceSettingsCatalog.Setting parent : match.parents) {
            FeatureUiPreferences.openAccordion(parent.id, ReferenceSettingsCatalog.dropdownIds(match.tab));
        }
        if (match.setting.isDropdown()) FeatureUiPreferences.openAccordion(match.setting.id,
                ReferenceSettingsCatalog.dropdownIds(match.tab));
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
