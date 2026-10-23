package com.arena.pekaclient.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Headless contract for the native port of the supplied five-destination settings reference. */
public final class FeatureSettingsUiModelTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        referenceRailIsExact();
        cardsFollowReferenceHierarchy();
        allCurrentRoutesRemainReachable();
        searchUsesLabelsDescriptionsAndInlineChildren();
        advancedGateHidesOnlyAdvancedPresentation();
        catalogHasNoDuplicateSettingIds();
        legacyPrototypeOnlyEntriesStayAbsent();
        responsiveGeometryStaysBounded();
        done();
    }

    private static void referenceRailIsExact() {
        String[] expected = {"Home", "Gameplay", "Dungeon", "Visuals", "System"};
        check("reference rail has the exact five destinations",
                Arrays.equals(expected, AppleSettingsScreen.destinationNames()));
        check("catalog tabs are in reference order", ReferenceSettingsCatalog.TABS.length == 5
                && ReferenceSettingsCatalog.TABS[0] == ReferenceSettingsCatalog.Tab.HOME
                && ReferenceSettingsCatalog.TABS[4] == ReferenceSettingsCatalog.Tab.SYSTEM);
        check("main shell is a rail, not the retired expandable Hub", AppleSettingsScreen.mainNavigationDepth() == 0);
    }

    private static void cardsFollowReferenceHierarchy() {
        check("Gameplay has the reference Comfort/Inventory/Chat/Skills/Shortcuts cards", cardNames(ReferenceSettingsCatalog.Tab.GAMEPLAY)
                .equals(Arrays.asList("Comfort", "Inventory", "Chat", "Skills", "Shortcuts")));
        check("Dungeon has Run tools/Floor VII/Highlights/Party sync cards", cardNames(ReferenceSettingsCatalog.Tab.DUNGEON)
                .equals(Arrays.asList("Run tools", "Floor VII", "Highlights", "Party sync")));
        check("Visuals has HUD and Item Animation cards", cardNames(ReferenceSettingsCatalog.Tab.VISUALS)
                .equals(Arrays.asList("HUD", "Item Animation")));
        check("System has Appearance through About cards", cardNames(ReferenceSettingsCatalog.Tab.SYSTEM)
                .equals(Arrays.asList("Appearance", "Performance", "General", "Diagnostics", "About")));
        ReferenceSettingsCatalog.Setting itemAnimation = ReferenceSettingsCatalog.find("item-animation");
        check("Item Animation retains inline sliders and reset", itemAnimation != null && itemAnimation.children.size() == 6
                && "item-animation-size".equals(itemAnimation.children.get(0).id)
                && "item-animation-reset".equals(itemAnimation.children.get(5).id));
        ReferenceSettingsCatalog.Setting terminal = ReferenceSettingsCatalog.find("terminal-assist");
        check("Terminal safeguards remain inline and Advanced auto click is not deleted", terminal != null
                && terminal.children.size() == 5 && terminal.children.get(3).advanced);
    }

    private static void allCurrentRoutesRemainReachable() {
        check("required current route inventory is complete", ReferenceSettingsCatalog.routesAreComplete());
        for (String id : ReferenceSettingsCatalog.REQUIRED_ROUTE_IDS) {
            ReferenceSettingsCatalog.Setting setting = ReferenceSettingsCatalog.find(id);
            check("route is retained: " + id, setting != null);
        }
        check("HUD Editor remains distinct", ReferenceSettingsCatalog.find("hud-editor") != null);
        check("Stats HUD route is retained", ReferenceSettingsCatalog.find("hud-stats") != null);
        check("Dungeon ESP style routes are retained", ReferenceSettingsCatalog.find("door-esp") != null
                && ReferenceSettingsCatalog.find("mob-esp") != null);
        check("focused chat route is retained", ReferenceSettingsCatalog.find("chat-qol") != null);
    }

    private static void searchUsesLabelsDescriptionsAndInlineChildren() {
        List<ReferenceSettingsCatalog.Match> lock = ReferenceSettingsCatalog.search("pairing", false);
        check("search finds label/description intent", contains(lock, "item-lock-swap"));
        List<ReferenceSettingsCatalog.Match> terminal = ReferenceSettingsCatalog.search("wrong clicks", false);
        check("search indexes inline child descriptions", contains(terminal, "terminal-assist"));
        List<ReferenceSettingsCatalog.Match> stats = ReferenceSettingsCatalog.search("frame rate mining memory", false);
        check("search finds restored Stats surface", contains(stats, "hud-stats"));
        check("empty search does not manufacture results", ReferenceSettingsCatalog.search("", false).isEmpty());
    }

    private static void advancedGateHidesOnlyAdvancedPresentation() {
        List<ReferenceSettingsCatalog.Card> gameplay = ReferenceSettingsCatalog.cards(ReferenceSettingsCatalog.Tab.GAMEPLAY);
        ReferenceSettingsCatalog.Card shortcuts = gameplay.get(gameplay.size() - 1);
        check("Shortcuts is intentionally advanced", shortcuts.advanced);
        ReferenceSettingsCatalog.Setting pricing = ReferenceSettingsCatalog.find("slayer-pricing");
        check("advanced inline child is explicitly marked", pricing != null && pricing.advanced);
        check("advanced search hides advanced-only card without the gate",
                !contains(ReferenceSettingsCatalog.search("wardrobe", false), "movement-shortcuts"));
        check("advanced search reveals the same route with the gate",
                contains(ReferenceSettingsCatalog.search("wardrobe", true), "movement-shortcuts"));
    }

    private static void catalogHasNoDuplicateSettingIds() {
        List<String> ids = new ArrayList<String>();
        for (ReferenceSettingsCatalog.Tab tab : ReferenceSettingsCatalog.TABS) {
            for (ReferenceSettingsCatalog.Card card : ReferenceSettingsCatalog.cards(tab)) collect(card.settings, ids);
        }
        check("every native setting id is unique", ids.size() == new HashSet<String>(ids).size());
    }

    private static void legacyPrototypeOnlyEntriesStayAbsent() {
        check("retired binary No Hurt Cam is not reintroduced", ReferenceSettingsCatalog.find("no-hurt-cam") == null);
        check("retired Player Dollars is not reintroduced", ReferenceSettingsCatalog.find("hud-dollars") == null);
        check("prototype Inventory Search is not reintroduced", ReferenceSettingsCatalog.find("inventory-search") == null);
        check("current Hurt Camera Shake is retained", ReferenceSettingsCatalog.find("hurt-camera-shake") != null);
    }

    private static void responsiveGeometryStaysBounded() {
        float[] widths = {1f, 80f, 160f, 320f, 640f, 1280f};
        for (float width : widths) {
            GuiLayoutToolkit.Rect row = new GuiLayoutToolkit.Rect().set(0f, 0f, width, 42f);
            GuiLayoutToolkit.Rect label = new GuiLayoutToolkit.Rect();
            GuiLayoutToolkit.Rect control = new GuiLayoutToolkit.Rect();
            GuiLayoutToolkit.splitRow(row, 12f, 8f, 172f, 24f, label, control);
            check("reference control geometry stays in bounds at " + width,
                    control.x >= row.x && control.right() <= row.right() && !label.intersects(control));
        }
    }

    private static List<String> cardNames(ReferenceSettingsCatalog.Tab tab) {
        List<String> names = new ArrayList<String>();
        for (ReferenceSettingsCatalog.Card card : ReferenceSettingsCatalog.cards(tab)) names.add(card.title);
        return names;
    }

    private static void collect(List<ReferenceSettingsCatalog.Setting> settings, List<String> ids) {
        for (ReferenceSettingsCatalog.Setting setting : settings) {
            ids.add(setting.id);
            collect(setting.children, ids);
        }
    }

    private static boolean contains(List<ReferenceSettingsCatalog.Match> values, String id) {
        for (ReferenceSettingsCatalog.Match match : values) if (id.equals(match.setting.id)) return true;
        return false;
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++; else { failed++; System.out.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("reference settings catalog: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
