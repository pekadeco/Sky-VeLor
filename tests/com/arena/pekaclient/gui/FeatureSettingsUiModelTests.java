package com.arena.pekaclient.gui;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Headless contract for the polished, direct-category settings surface. */
public final class FeatureSettingsUiModelTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        categoryRailContainsOnlyBackedDestinations();
        categoryCardsKeepExistingControlsInTheirRealAreas();
        allCurrentRoutesRemainReachable();
        searchAndChildrenAreNeverAdvancedGated();
        catalogHasNoDuplicateSettingIds();
        legacyPrototypeOnlyEntriesStayAbsent();
        homeIsLiveAndTheWindowUsesThePolishedShellContract();
        responsiveGeometryStaysBounded();
        done();
    }

    private static void categoryRailContainsOnlyBackedDestinations() {
        String[] expected = {"Home", "Gameplay", "Dungeon", "Farming", "Visuals", "System"};
        check("settings rail has the exact six requested destinations", Arrays.equals(expected,
                AppleSettingsScreen.destinationNames()));
        check("catalog tabs have the same exact destination order", ReferenceSettingsCatalog.TABS.length == expected.length
                && ReferenceSettingsCatalog.TABS[0] == ReferenceSettingsCatalog.Tab.HOME
                && ReferenceSettingsCatalog.TABS[1] == ReferenceSettingsCatalog.Tab.GAMEPLAY
                && ReferenceSettingsCatalog.TABS[2] == ReferenceSettingsCatalog.Tab.DUNGEON
                && ReferenceSettingsCatalog.TABS[3] == ReferenceSettingsCatalog.Tab.FARMING
                && ReferenceSettingsCatalog.TABS[4] == ReferenceSettingsCatalog.Tab.VISUALS
                && ReferenceSettingsCatalog.TABS[5] == ReferenceSettingsCatalog.Tab.SYSTEM);
        check("Home catalog describes live performance rather than presets", ReferenceSettingsCatalog.Tab.HOME.caption
                .contains("Live performance") && !ReferenceSettingsCatalog.Tab.HOME.caption.contains("Preset"));
        check("main shell is a rail, not retired expandable navigation", AppleSettingsScreen.mainNavigationDepth() == 0);
    }

    private static void categoryCardsKeepExistingControlsInTheirRealAreas() {
        check("Gameplay groups existing Comfort, Inventory, Chat, Shortcuts, Mining, Slayer and Fishing cards",
                cardNames(ReferenceSettingsCatalog.Tab.GAMEPLAY).equals(Arrays.asList(
                        "Comfort", "Inventory", "Chat", "Shortcuts", "Mining", "Slayer", "Fishing")));
        check("Gameplay preserves existing mining, Slayer and fishing setting owners",
                ownerOf("mining-assistant-settings") == ReferenceSettingsCatalog.Tab.GAMEPLAY
                        && ownerOf("slayer-hud-route") == ReferenceSettingsCatalog.Tab.GAMEPLAY
                        && ownerOf("fishing-hud-route") == ReferenceSettingsCatalog.Tab.GAMEPLAY);
        check("Farming is a deliberate empty page, not a fake configuration surface",
                ReferenceSettingsCatalog.cards(ReferenceSettingsCatalog.Tab.FARMING).isEmpty()
                        && ReferenceSettingsCatalog.find("farming-empty") == null);
        List<ReferenceSettingsCatalog.Match> farming = ReferenceSettingsCatalog.search("farming");
        check("Farming remains discoverable from settings search", farming.size() == 1
                && farming.get(0).tab == ReferenceSettingsCatalog.Tab.FARMING
                && "No farming features yet".equals(farming.get(0).setting.label));
        check("Visuals owns the retained HUD and Item Animation cards", cardNames(ReferenceSettingsCatalog.Tab.VISUALS)
                .equals(Arrays.asList("HUD Hub", "Item Animation"))
                && ownerOf("hud-editor") == ReferenceSettingsCatalog.Tab.VISUALS
                && ownerOf("item-animation") == ReferenceSettingsCatalog.Tab.VISUALS);
        check("Dungeon has Run tools/Floor VII/Highlights/Party sync cards", cardNames(ReferenceSettingsCatalog.Tab.DUNGEON)
                .equals(Arrays.asList("Run tools", "Floor VII", "Highlights", "Party sync")));
        check("System has Appearance through About cards", cardNames(ReferenceSettingsCatalog.Tab.SYSTEM)
                .equals(Arrays.asList("Appearance", "Performance", "General", "Diagnostics", "About")));
        ReferenceSettingsCatalog.Setting itemAnimation = ReferenceSettingsCatalog.find("item-animation");
        check("Item Animation retains inline sliders and reset", itemAnimation != null && itemAnimation.children.size() == 6
                && "item-animation-size".equals(itemAnimation.children.get(0).id)
                && "item-animation-reset".equals(itemAnimation.children.get(5).id));
        ReferenceSettingsCatalog.Setting settingsTheme = ReferenceSettingsCatalog.find("appearance-theme");
        check("Settings Theme exposes both persisted palette choices", settingsTheme != null
                && settingsTheme.control == ReferenceSettingsCatalog.Control.CHOICE
                && Arrays.equals(new String[]{"Forest & Cyan", "Black & Blue"}, settingsTheme.choices));
    }

    private static void allCurrentRoutesRemainReachable() {
        check("required current route inventory is complete", ReferenceSettingsCatalog.routesAreComplete());
        for (String id : ReferenceSettingsCatalog.REQUIRED_ROUTE_IDS) {
            ReferenceSettingsCatalog.Setting setting = ReferenceSettingsCatalog.find(id);
            check("route is retained: " + id, setting != null);
        }
        check("HUD Editor remains distinct", ReferenceSettingsCatalog.find("hud-editor") != null);
        check("retired metric HUD route is absent", ReferenceSettingsCatalog.find("hud-stats") == null);
        check("Dungeon ESP style routes are retained", ReferenceSettingsCatalog.find("door-esp") != null
                && ReferenceSettingsCatalog.find("mob-esp") != null);
        check("focused chat route is retained", ReferenceSettingsCatalog.find("chat-qol") != null);
    }

    private static void searchAndChildrenAreNeverAdvancedGated() {
        List<ReferenceSettingsCatalog.Match> lock = ReferenceSettingsCatalog.search("pairing");
        check("search finds label/description intent", contains(lock, "item-lock-swap"));
        List<ReferenceSettingsCatalog.Match> terminal = ReferenceSettingsCatalog.search("wrong clicks");
        check("search indexes inline child descriptions", contains(terminal, "terminal-assist"));
        List<ReferenceSettingsCatalog.Match> wardrobe = ReferenceSettingsCatalog.search("wardrobe");
        check("formerly Advanced shortcut is directly searchable", contains(wardrobe, "movement-shortcuts"));
        check("compatibility search overload no longer changes visible results",
                sameIds(wardrobe, ReferenceSettingsCatalog.search("wardrobe", false))
                        && sameIds(wardrobe, ReferenceSettingsCatalog.search("wardrobe", true)));
        ReferenceSettingsCatalog.Setting terminalAssist = ReferenceSettingsCatalog.find("terminal-assist");
        check("formerly Advanced inline child remains visible through normal child API", terminalAssist != null
                && terminalAssist.visibleChildCount() == terminalAssist.visibleChildCount(false)
                && terminalAssist.hasChildren() && terminalAssist.hasChildren(false));
        check("empty search does not manufacture results", ReferenceSettingsCatalog.search("").isEmpty());
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

    private static void homeIsLiveAndTheWindowUsesThePolishedShellContract() {
        String screen = classText("com.arena.pekaclient.gui.AppleSettingsScreen");
        check("Home uses its cached live performance reader and fixed history", screen.contains("LIVE PERFORMANCE")
                && screen.contains("ClientMetrics$Reader") && screen.contains("HomeMetricHistory")
                && screen.contains("drawHomeSparkline"));
        check("screen has global search with no Advanced-button behavior", screen.contains("Search settings")
                && !screen.contains("Advanced settings shown") && !screen.contains("Advanced settings hidden"));
        check("title bar drag retains the small window across fullscreen", screen.contains("smallWindowOffsetX")
                && screen.contains("fullscreenWindow") && screen.contains("refreshReferenceTheme"));
        check("header packages exactly fullscreen and close controls without traffic-light hints", screen.contains("window-fullscreen")
                && screen.contains("window-close") && !screen.contains("window-green") && !screen.contains("window-yellow")
                && !screen.contains("window-red") && !screen.contains("DRAG TITLE BAR")
                && "pekadeco".equals(AppleSettingsScreen.PROFILE_CREATOR)
                && Arrays.equals(new String[]{"ii88w", "whiteShadow"}, AppleSettingsScreen.PROFILE_CONTRIBUTORS));
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

    private static ReferenceSettingsCatalog.Tab ownerOf(String id) {
        for (ReferenceSettingsCatalog.Tab tab : ReferenceSettingsCatalog.TABS) {
            for (ReferenceSettingsCatalog.Card card : ReferenceSettingsCatalog.cards(tab)) {
                if (containsSetting(card.settings, id)) return tab;
            }
        }
        return null;
    }

    private static boolean containsSetting(List<ReferenceSettingsCatalog.Setting> settings, String id) {
        for (ReferenceSettingsCatalog.Setting setting : settings) {
            if (id.equals(setting.id) || containsSetting(setting.children, id)) return true;
        }
        return false;
    }

    private static boolean hasTabNamed(String name) {
        try {
            ReferenceSettingsCatalog.Tab.valueOf(name);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
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

    private static boolean sameIds(List<ReferenceSettingsCatalog.Match> one, List<ReferenceSettingsCatalog.Match> two) {
        if (one.size() != two.size()) return false;
        for (int i = 0; i < one.size(); i++) if (!one.get(i).setting.id.equals(two.get(i).setting.id)) return false;
        return true;
    }

    private static String classText(String binaryName) {
        InputStream input = FeatureSettingsUiModelTests.class.getClassLoader()
                .getResourceAsStream(binaryName.replace('.', '/') + ".class");
        if (input == null) return "";
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = input.read(buffer)) != -1) out.write(buffer, 0, read);
            return new String(out.toByteArray(), "ISO-8859-1");
        } catch (IOException ignored) {
            return "";
        } finally {
            try { input.close(); } catch (IOException ignored) { }
        }
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++; else { failed++; System.out.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("feature settings UI model: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
