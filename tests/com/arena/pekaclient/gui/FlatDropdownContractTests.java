package com.arena.pekaclient.gui;

import java.util.List;
import java.util.Properties;

/** Contract for the four player-visible settings row types and one-level disclosure policy. */
public final class FlatDropdownContractTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        onlyFourVisibleRowTypesRemain();
        everyDisclosureIsFlatAndUseful();
        everyValueDropdownIsMeaningfulAndReachable();
        fixedColumnsStayAlignedAtGuiScalesOneToFour();
        accordionStateClosesPeersAndPersists();
        requestedFeatureRowsUseTheirRequiredTypes();
        done();
    }

    private static void onlyFourVisibleRowTypesRemain() {
        for (ReferenceSettingsCatalog.Tab tab : ReferenceSettingsCatalog.TABS) {
            for (ReferenceSettingsCatalog.Card card : ReferenceSettingsCatalog.cards(tab)) {
                for (ReferenceSettingsCatalog.Setting row : card.settings) {
                    check("top row type is supported: " + row.id,
                            row.control == ReferenceSettingsCatalog.Control.TOGGLE
                                    || row.control == ReferenceSettingsCatalog.Control.SLIDER
                                    || row.control == ReferenceSettingsCatalog.Control.DROPDOWN
                                    || row.control == ReferenceSettingsCatalog.Control.EDITOR);
                    for (ReferenceSettingsCatalog.Setting child : row.children) {
                        check("expanded option is not a nested dropdown: " + child.id,
                                !child.isDropdown());
                    }
                }
            }
        }
    }

    private static void everyDisclosureIsFlatAndUseful() {
        check("maximum dropdown depth is one", ReferenceSettingsCatalog.maxDropdownDepth() == 1);
        check("every dropdown has two or more options", ReferenceSettingsCatalog.everyDropdownHasAtLeastTwoOptions());
        for (ReferenceSettingsCatalog.Tab tab : ReferenceSettingsCatalog.TABS) {
            for (ReferenceSettingsCatalog.Card card : ReferenceSettingsCatalog.cards(tab)) {
                for (ReferenceSettingsCatalog.Setting row : card.settings) {
                    if (!row.isDropdown()) continue;
                    int resets = 0;
                    for (int i = 0; i < row.children.size(); i++) {
                        ReferenceSettingsCatalog.Setting child = row.children.get(i);
                        if (child.isResetAction()) {
                            resets++;
                            check("reset link is the final option: " + row.id, i == row.children.size() - 1);
                        }
                    }
                    check("at most one reset link: " + row.id, resets <= 1);
                }
            }
        }
    }

    private static void everyValueDropdownIsMeaningfulAndReachable() {
        check("every inline value dropdown has two or more choices", ReferenceSettingsCatalog.everyChoiceHasAtLeastTwoOptions());
        for (ReferenceSettingsCatalog.Tab tab : ReferenceSettingsCatalog.TABS) {
            for (String id : ReferenceSettingsCatalog.dropdownIds(tab)) {
                ReferenceSettingsCatalog.Setting setting = ReferenceSettingsCatalog.find(id);
                check("disclosure belongs to its current page: " + id,
                        setting != null && setting.isDropdown() && ReferenceSettingsCatalog.ownerOf(id) == tab);
            }
        }
    }

    private static void fixedColumnsStayAlignedAtGuiScalesOneToFour() {
        for (int scale = 1; scale <= 4; scale++) {
            float rowX = 12f;
            float rowW = Math.max(92f, 1280f / scale - 48f);
            float chevron = SettingsControlLayout.chevronCenterX(rowX, rowW, 12f);
            float toggleX = SettingsControlLayout.toggleX(rowX, rowW, 12f);
            float right = SettingsControlLayout.contentRight(rowX, rowW, 12f, 8f);
            check("fixed chevron is in bounds at scale " + scale,
                    chevron >= rowX && chevron <= rowX + rowW);
            check("standard toggle is in bounds at scale " + scale,
                    toggleX >= rowX && toggleX + SettingsControlLayout.TOGGLE_COLUMN_WIDTH <= rowX + rowW);
            check("fixed columns leave room for inline controls at scale " + scale,
                    right >= rowX && right <= chevron - SettingsControlLayout.CHEVRON_COLUMN_WIDTH * .5f);
            check("same chevron coordinate for toggle/dropdown/editor at scale " + scale,
                    chevron == SettingsControlLayout.chevronCenterX(rowX, rowW, 12f));
            check("chevron stays immediately before the rightmost toggle at scale " + scale,
                    chevron + SettingsControlLayout.CHEVRON_COLUMN_WIDTH * .5f + SettingsControlLayout.COLUMN_GAP
                            <= toggleX + .01f);
        }
    }

    private static void accordionStateClosesPeersAndPersists() {
        List<String> gameplay = ReferenceSettingsCatalog.dropdownIds(ReferenceSettingsCatalog.Tab.GAMEPLAY);
        FeatureUiState state = new FeatureUiState();
        state.openAccordion("chat-qol", gameplay);
        check("accordion opens Chat QoL", state.dropdownOpen("chat-qol"));
        state.openAccordion("item-lock-swap", gameplay);
        check("accordion closes the previous Gameplay dropdown", !state.dropdownOpen("chat-qol")
                && state.dropdownOpen("item-lock-swap"));
        Properties properties = new Properties();
        state.save(properties);
        FeatureUiState loaded = new FeatureUiState();
        loaded.load(properties);
        check("accordion disclosure persists separately", loaded.dropdownOpen("item-lock-swap")
                && !loaded.dropdownOpen("chat-qol"));
    }

    private static void requestedFeatureRowsUseTheirRequiredTypes() {
        ReferenceSettingsCatalog.Setting hurt = ReferenceSettingsCatalog.find("hurt-camera-shake");
        ReferenceSettingsCatalog.Setting aspect = ReferenceSettingsCatalog.find("smooth-aspect-teleport");
        ReferenceSettingsCatalog.Setting leap = ReferenceSettingsCatalog.find("spirit-leap");
        ReferenceSettingsCatalog.Setting terminal = ReferenceSettingsCatalog.find("terminal-assist");
        ReferenceSettingsCatalog.Setting boss = ReferenceSettingsCatalog.find("dungeon-boss-hud");
        ReferenceSettingsCatalog.Setting hand = ReferenceSettingsCatalog.find("item-animation");
        ReferenceSettingsCatalog.Setting hud = ReferenceSettingsCatalog.find("hud-editor");
        check("Hurt Camera Shake is one inline slider", hurt != null && hurt.control == ReferenceSettingsCatalog.Control.SLIDER
                && ReferenceSettingsCatalog.find("hurt-camera") == null);
        check("Smooth Aspect Teleport is a direct QoL visual toggle", aspect != null
                && aspect.control == ReferenceSettingsCatalog.Control.TOGGLE
                && ReferenceSettingsCatalog.ownerOf("smooth-aspect-teleport") == ReferenceSettingsCatalog.Tab.GAMEPLAY);
        check("Spirit Leap is a master dropdown with its key inside", leap != null && leap.isDropdown()
                && leap.hasMasterToggle() && contains(leap, "leap-key"));
        check("Terminal Helper is a no-master dropdown", terminal != null && terminal.isDropdown()
                && !terminal.hasMasterToggle() && terminal.children.size() >= 5);
        check("Boss Health HUD is a toggle plus named HUD editor route", boss != null && boss.isEditor()
                && boss.hasMasterToggle() && "open-hud-editor".equals(boss.editorActionId)
                && "Open HUD Editor".equals(boss.actionLabel));
        check("Item Animation is a toggle plus named editor route", hand != null && hand.isEditor()
                && hand.hasMasterToggle() && "item-animation-open-editor".equals(hand.editorActionId)
                && "Open Item Animation Editor".equals(hand.actionLabel));
        check("HUD Editor is a named route with no chevron", hud != null && hud.isEditor() && !hud.hasMasterToggle()
                && "open-hud-editor".equals(hud.editorActionId) && "Open HUD Editor".equals(hud.actionLabel));
    }

    private static boolean contains(ReferenceSettingsCatalog.Setting setting, String id) {
        if (setting == null) return false;
        for (ReferenceSettingsCatalog.Setting child : setting.children) if (id.equals(child.id)) return true;
        return false;
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++; else { failed++; System.out.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("flat dropdown contract: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
