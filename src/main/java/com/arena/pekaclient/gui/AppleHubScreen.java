package com.arena.pekaclient.gui;

import com.arena.pekaclient.PekaClientMod;
import com.arena.pekaclient.PekaVersion;
import com.arena.pekaclient.assist.guard.ItemGuardController;
import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.config.AssistSettings;
import com.arena.pekaclient.config.ChatQolSettings;
import com.arena.pekaclient.config.DungeonEspSettings;
import com.arena.pekaclient.config.FeatureUiPreferences;
import com.arena.pekaclient.feature.ChatChannelTabs;
import com.arena.pekaclient.dungeon.M7OverlayRenderer;
import com.arena.pekaclient.dungeon.M7Settings;
import com.arena.pekaclient.fishing.FishingStats;
import com.arena.pekaclient.slayer.SlayerTracker;
import com.arena.pekaclient.slayer.SlayerType;
import com.arena.pekaclient.feature.MiningTracker;
import com.arena.pekaclient.hud.HudElement;
import com.arena.pekaclient.hud.HudManager;
import com.arena.pekaclient.itemlockswap.ItemLockSwapKeybind;
import com.arena.pekaclient.itemlockswap.ItemLockSwapSettings;
import com.arena.pekaclient.itemlockswap.ItemLockSwapUiModel;
import com.arena.pekaclient.render.Anim;
import com.arena.pekaclient.render.Icons;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import com.arena.pekaclient.widget.AppleButton;
import com.arena.pekaclient.widget.AppleDropdown;
import com.arena.pekaclient.widget.AppleKeybindButton;
import com.arena.pekaclient.widget.AppleSlider;
import com.arena.pekaclient.widget.AppleToggle;
import com.arena.pekaclient.widget.FeatureRowControl;
import com.arena.pekaclient.widget.SearchField;
import com.arena.pekaclient.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiControls;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenResourcePacks;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

/**
 * The Sky VeLor hub: a macOS-style window with window buttons, a sidebar (or tab bar on narrow
 * screens), a search field and scrollable grouped cards.
 *
 * Tabs, in order: General, Quality of Life, HUD, Farming, Dungeon, Slayer, Fishing. General is the first overview
 * page; Quality of Life owns gameplay plus the collapsible Commands and Chat QoL groups. HUD holds visibility
 * switches and routes every visual customization to the HUD editor. Farming
 * holds Mining BPS. Dungeon holds the Spirit Leap teammate selector. Slayer and Fishing have no modules yet, so
 * they show an empty state rather than placeholder modules.
 *
 * Window buttons: red closes the hub, yellow collapses it to the title bar, green zooms it to the
 * full screen. Input: mouse wheel and touch swipe scroll the list, with the speed set in Mod Settings. Keys scroll by a fixed step. A swipe
 * never activates a control. Keyboard: Ctrl+1 to Ctrl+7 pick a tab, Left and Right step through tabs,
 * Up, Down, Page Up, Page Down, Home and End scroll.
 */
public class AppleHubScreen extends AppleScreen {

    private static final float TITLE_H = 34f;
    private static final float SIDEBAR_W = 136f;
    private static final float PAD = 14f;
    private static final float ROW_GAP = 6f;
    private static final float NAV_H = 26f;
    private static final float NAV_STEP = 30f;
    private static final float TAB_H = 24f;
    /** Narrowest tab width at which the tab name is drawn. Below this, only the icon is drawn. */
    private static final float TAB_TEXT_MIN = 52f;

    static final String[] PAGE_NAMES = {"General", QualityOfLifeUiModel.TAB_LABEL, "HUD", "Farming", "Dungeon", "Slayer", "Fishing"};
    private static final String[] PAGE_SUBS = {
            "Overview, appearance, performance and camera",
            "Gameplay, commands and Default Chat QoL",
            "Show, place and style the on-screen elements",
            "Mining BPS and its settings",
            "Spirit Leap, Dungeon ESP doors and Floor VII (M7) overlays",
            "Drop tracker, boss timer and prices",
            "Sea-creature counter, Common Creature (CC), Festival best, Sea Creature HP and Lord Jawbus"
    };
    /** HUD groups in display order. Each one expands in place to its toggle and settings. */
    private static final HudElement[] HUD_OVERVIEW = {
            HudManager.DOLL, HudManager.INVENTORY, HudManager.MINING, HudManager.DOLLARS, HudManager.HOTBAR,
            HudManager.FISHING, HudManager.SEA_HP, HudManager.SLAYER, HudManager.MINING_ASSIST, HudManager.DUNGEON_BOSS_HP};
    private static final int HUD_MINING = 2; // index into HUD_OVERVIEW
    private static final int HUD_MINING_ASSIST = 8;
    private static final int HUD_DUNGEON_BOSS_HP = 9;
    private static final int[] PAGE_ICONS = {
            Icons.GEAR, Icons.BOLT, Icons.HUD, Icons.LEAF, Icons.TOWER, Icons.SWORD, Icons.FISH
    };
    private static final int PAGE_HUD = 2;
    /** HUD tab categories: 0 is the element list, 1 is Dungeon (Floor VII overlay and terminals). */
    private static final int HUD_CAT_ELEMENTS = 0;
    private static final int HUD_CAT_DUNGEON = 1;
    private int hudCategory = HUD_CAT_ELEMENTS;
    private static final int PAGE_FARMING = 3;
    private static final int PAGE_FISHING = 6;
    private static final int PAGE_DUNGEON = 4;

    // Indexes into statValues.
    private static final int S_FPS = 0;
    private static final int S_MEM = 1;
    private static final int S_FRAME = 2;
    private static final int S_MINE = 3;

    private final GuiScreen parent;
    private final List<List<Row>> pages = new ArrayList<List<Row>>();
    private int pageIndex;

    private final SearchField search = new SearchField("Search settings");
    private final List<Row> searchRows = new ArrayList<Row>();
    private int searchVersion = -1;
    private boolean searching;
    private String query = "";

    /** Element picked in the HUD tab's element settings. */
    /** Titles of the collapsible groups that are currently expanded. Kept while the screen is open. */
    private final Set<String> expanded = new HashSet<String>();
    /** Header under the pointer at press time. Toggles on release if the pointer did not drag. */
    private Row pendingHeader;

    // Window state. Collapsed hides everything but the title bar. Zoomed fills the screen.
    private boolean collapsed;
    private boolean zoomed;

    private final Anim openAnim = new Anim(0f);
    private final Anim fadeAnim = new Anim(1f);
    private final Anim[] navSel = new Anim[PAGE_NAMES.length];
    private final Anim[] navHover = new Anim[PAGE_NAMES.length];
    private final Anim indicator = new Anim(0f);

    // Scrolling state.
    private final ScrollState scroll = new ScrollState();
    private final Gesture gesture = new Gesture();
    private boolean gestureActive;
    private boolean sliderEngaged;
    private Widget active;
    /** The only capture control in this screen; keyboard input remains unavailable to search while it listens. */
    private AppleKeybindButton itemLockSwapKeyButton;

    // Layout (recomputed every frame, in GUI-scaled pixels). The shared clamp keeps the hub reachable at any scale.
    private final GuiLayoutToolkit.Rect hubPanel = new GuiLayoutToolkit.Rect();
    /** Reused row geometry avoids overlapping label/control columns on narrow GUI scales. */
    private final GuiLayoutToolkit.Rect rowLayoutRect = new GuiLayoutToolkit.Rect();
    private final GuiLayoutToolkit.Rect rowLabelRect = new GuiLayoutToolkit.Rect();
    private final GuiLayoutToolkit.Rect rowControlRect = new GuiLayoutToolkit.Rect();
    private float wx, wy, ww, wh;
    private float cx0, cw, headY;
    private float vx, vy, vw, vh;
    private boolean wide;
    private boolean tall;

    // Timing and stats.
    private long lastNs;
    private final float[] frameHistory = new float[90];
    private int histIdx;
    private float dtEma = 1f / 60f;
    private int slowFrames;
    private int fastFrames;
    private boolean autoLow;
    private float statTimer;
    private final String[] statValues = {"--", "--", "--", "--"};
    private int scaleKey = -1;
    private float sf = 2f;

    public AppleHubScreen(GuiScreen parent) {
        this.parent = parent;
        Theme.refresh();
        openAnim.to(1f);
        for (int i = 0; i < PAGE_NAMES.length; i++) {
            navSel[i] = new Anim(i == 0 ? 1f : 0f);
            navHover[i] = new Anim(0f);
        }
        expanded.add("Spirit Leap");
        expanded.add(DungeonEspUiModel.MOB_ESP_TITLE);
        buildPages();
    }

    // ------------------------------------------------------------------ pages

    private void buildPages() {
        pages.clear();
        pages.add(generalPage());
        pages.add(qualityOfLifePage());
        pages.add(hudPage());
        pages.add(farmingPage());
        pages.add(dungeonPage());
        pages.add(slayerPage());
        pages.add(fishingPage());
    }

    /** Builds one compact persisted tree feature row; details remain in a focused editor rather than the tab. */
    private static Row featureRow(String id, String parentId, int depth, boolean hasChildren, boolean initiallyOpen,
                                  String title, String description, java.util.function.Supplier<Boolean> getter,
                                  java.util.function.Consumer<Boolean> setter, Runnable open) {
        return Row.treeFeature(id, parentId, depth, hasChildren, initiallyOpen, title, description,
                getter == null ? new FeatureRowControl(open) : new FeatureRowControl(getter, setter, open));
    }

    private static Row featureRow(String id, String parentId, int depth, boolean hasChildren, boolean initiallyOpen,
                                  String title, String description, Widget inlineControl) {
        return Row.treeFeature(id, parentId, depth, hasChildren, initiallyOpen, title, description, inlineControl);
    }

    private static Row featureBranch(String id, String parentId, int depth, boolean initiallyOpen,
                                     String title, String description) {
        return Row.treeFeature(id, parentId, depth, true, initiallyOpen, title, description, null);
    }

    /**
     * One Quality of Life page with gameplay, optional inventory safety/assist rows, Item Lock and Item Swap,
     * Chat QoL, and commands. The item feature uses ordinary shared rows so it remains scroll-safe at every scale.
     */
    private List<Row> qualityOfLifePage() {
        List<Row> rows = new ArrayList<Row>();
        rows.add(Row.section(QualityOfLifeUiModel.TAB_LABEL));
        rows.add(featureBranch("qol.root", null, 0, true, "Quality of Life Features",
                "Open a feature for its detailed settings."));

        rows.add(featureBranch("qol.gameplay", "qol.root", 1, true, "Gameplay",
                "Two small client-side switches."));
        rows.add(featureRow("qol.hotbar-scroll", "qol.gameplay", 2, false, false, "Disable Hotbar Scrolling",
                QualityOfLifeUiModel.buildToggleModel().get(0).getDescription(),
                new AppleToggle(() -> AppleSettings.disableHotbarScrolling,
                        value -> AppleSettings.disableHotbarScrolling = value)));
        rows.add(featureRow("qol.block-particles", "qol.gameplay", 2, false, false, "No Block Particles",
                QualityOfLifeUiModel.buildToggleModel().get(1).getDescription(),
                new AppleToggle(() -> AppleSettings.noBlockParticles,
                        value -> AppleSettings.noBlockParticles = value)));

        rows.add(featureRow("qol.inventory", "qol.root", 1, false, false, "Inventory Assist",
                "Search, tooltip and inventory visual helpers.", null, null,
                () -> Minecraft.getMinecraft().displayGuiScreen(FeatureSettingsEditors.inventoryAssist(this))));
        rows.add(featureRow("qol.item-protection", "qol.root", 1, false, false, "Item Protection",
                "Protect selected UUID items locally and view status.", () -> AssistSettings.itemGuard,
                value -> AssistSettings.itemGuard = value,
                () -> Minecraft.getMinecraft().displayGuiScreen(FeatureSettingsEditors.itemProtection(this))));
        rows.add(featureRow("qol.movement", "qol.root", 1, false, false, "Movement and Shortcuts",
                "Auto sprint, wardrobe and pets shortcuts.", null, null,
                () -> Minecraft.getMinecraft().displayGuiScreen(FeatureSettingsEditors.movementShortcuts(this))));
        rows.add(featureRow("qol.item-lock-swap", "qol.root", 1, false, false, "Item Lock & Swap",
                "Pair locks, indicators and persistent pair-center lines.", () -> ItemLockSwapSettings.enabled,
                value -> ItemLockSwapSettings.enabled = value,
                () -> Minecraft.getMinecraft().displayGuiScreen(FeatureSettingsEditors.itemLockSwap(this))));
        rows.add(featureRow("qol.chat", "qol.root", 1, false, false, "Chat QoL",
                "Attached tabs, message animation and highlighting.", () -> ChatQolSettings.enabled, value -> {
                    ChatQolSettings.enabled = value;
                    if (!value) ChatChannelTabs.disableAndRestoreAll();
                }, () -> Minecraft.getMinecraft().displayGuiScreen(new ChatEditorScreen(this))));
        rows.add(featureRow("qol.commands", "qol.root", 1, false, false, "Commands",
                "Optional /sv and /qn routes plus diagnostics.", null, null,
                () -> Minecraft.getMinecraft().displayGuiScreen(FeatureSettingsEditors.commands(this))));
        return rows;
    }

    /** General remains the first, non-feature overview page. */
    private List<Row> generalPage() {
        List<Row> rows = new ArrayList<Row>();
        rows.add(Row.section("Overview"));
        rows.add(Row.custom(86f, this::paintStats));
        rows.add(Row.section("Features"));
        rows.add(featureBranch("general.root", null, 0, true, "General Settings", "Theme, global tools and performance."));
        rows.add(featureRow("general.appearance", "general.root", 1, false, false, "Appearance",
                "Dark/light theme, accent, corners and panel opacity.", null, null,
                () -> Minecraft.getMinecraft().displayGuiScreen(FeatureSettingsEditors.appearance(this))));
        rows.add(featureRow("general.mod-settings", "general.root", 1, false, false, "Mod Settings",
                "Global scroll speed and smoothing; Shift + wheel is fine scrolling.", null, null,
                () -> Minecraft.getMinecraft().displayGuiScreen(new ModSettingsScreen(this))));
        rows.add(featureRow("general.fonts", "general.root", 1, false, false, "Fonts",
                "Global font selection, preview, bundled licenses and safe fallback.", null, null,
                () -> Minecraft.getMinecraft().displayGuiScreen(new FontsScreen(this))));
        rows.add(featureRow("general.performance", "general.root", 1, false, false, "Performance",
                "Curve, shadows, animation and adaptive GUI quality.", null, null,
                () -> Minecraft.getMinecraft().displayGuiScreen(FeatureSettingsEditors.performance(this))));
        rows.add(Row.section("Live"));
        rows.add(Row.custom(76f, this::paintGraph));
        rows.add(Row.section("About"));
        rows.add(Row.item("Sky VeLor " + PekaVersion.NAME,
                "Forge client for Minecraft 1.8.9. Settings are saved in skyvelor.properties", null));
        return rows;
    }

    /** The hub opened on the HUD tab, Dungeon category. */
    public static AppleHubScreen dungeonHud(GuiScreen parent) {
        AppleHubScreen hub = new AppleHubScreen(parent);
        hub.hudCategory = HUD_CAT_DUNGEON;
        hub.selectPage(PAGE_HUD);
        return hub;
    }

    /** Switches the HUD tab between its categories and rebuilds that page. */
    private void setHudCategory(int category) {
        hudCategory = category;
        pages.set(PAGE_HUD, hudPage());
        scroll.reset();
        gestureActive = false;
        active = null;
        openDropdown = null;
    }

    private List<Row> hudPage() {
        if (hudCategory == HUD_CAT_DUNGEON) return hudDungeonPage();
        final Minecraft mc = Minecraft.getMinecraft();
        List<Row> rows = new ArrayList<Row>();
        rows.add(Row.section("HUD"));
        rows.add(featureBranch("hud.root", null, 0, true, "HUD Features", "Visibility switches and editor routes."));
        rows.add(featureRow("hud.editor", "hud.root", 1, false, false, "HUD Editor",
                "Drag, resize and style each element on the game screen.", null, null,
                () -> mc.displayGuiScreen(new HudEditorScreen(this))));
        rows.add(featureRow("hud.item-animation", "hud.root", 1, false, false, "Item Animation",
                "Visual first-person transforms only; never changes packets, attacks or gameplay.",
                () -> AppleSettings.itemAnimationEnabled, value -> {
                    AppleSettings.itemAnimationEnabled = value;
                    AppleSettings.save();
                }, () -> mc.displayGuiScreen(HudEditorScreen.openItemAnimation(this))));
        rows.add(featureBranch("hud.elements", "hud.root", 1, false, "HUD Elements",
                "Each element keeps visibility and its single editor route."));
        for (final HudElement element : HUD_OVERVIEW) {
            GuiSettingsAuditModel.Feature feature = GuiSettingsAuditModel.find(element.id);
            final String title = feature == null ? element.name : feature.getTitle();
            final String description = feature == null ? "Shown on screen." : feature.getDescription();
            rows.add(featureRow("hud.element." + element.id, "hud.elements", 2, false, false, title, description,
                    () -> element.enabled, value -> element.enabled = value,
                    () -> mc.displayGuiScreen(HudEditorScreen.openElement(this, element))));
        }
        return rows;
    }

    /** HUD tab, Dungeon category: the dungeon screen features, with the same switches as the Dungeon tab. */
    private List<Row> hudDungeonPage() {
        List<Row> rows = new ArrayList<Row>();
        rows.add(Row.section("Dungeon"));
        rows.add(Row.item("Back to Elements", "Return to the HUD element list.",
                new AppleButton("Back", 84f, 26f, false, () -> setHudCategory(HUD_CAT_ELEMENTS))));
        rows.add(featureBranch("hud.dungeon.root", null, 0, true, "Dungeon HUD Features", "Floor VII overlay controls."));
        rows.add(featureRow("hud.dungeon.m7", "hud.dungeon.root", 1, false, false, "Floor VII (M7) Overlay",
                "Areas, alert, terminal solver, boss and dragon hitboxes.", M7Settings::isOverlayEnabled,
                M7Settings::setOverlayEnabled,
                () -> Minecraft.getMinecraft().displayGuiScreen(FeatureSettingsEditors.m7Overlay(this))));
        return rows;
    }

    /** Opens one HUD element at its only customization surface: the HUD editor. */
    private void openHud(int index) {
        if (index < 0 || index >= HUD_OVERVIEW.length) {
            return;
        }
        Minecraft.getMinecraft().displayGuiScreen(HudEditorScreen.openElement(this, HUD_OVERVIEW[index]));
    }

    private List<Row> farmingPage() {
        List<Row> rows = new ArrayList<Row>();
        GuiSettingsAuditModel.Feature mining = GuiSettingsAuditModel.find("mining");
        GuiSettingsAuditModel.Feature assistant = GuiSettingsAuditModel.find("mining-assistant");
        rows.add(Row.section("Mining"));
        rows.add(featureBranch("farming.root", null, 0, true, "Mining Features", "HUD and optional mining helpers."));
        rows.add(featureRow("farming.mining-bps", "farming.root", 1, false, false, mining.getTitle(),
                mining.getDescription(), () -> HudManager.MINING.enabled, value -> HudManager.MINING.enabled = value,
                () -> openHud(HUD_MINING)));
        rows.add(featureRow("farming.mining-assistant", "farming.root", 1, false, false, assistant.getTitle(),
                assistant.getDescription(), () -> HudManager.MINING_ASSIST.enabled,
                value -> HudManager.MINING_ASSIST.enabled = value,
                () -> Minecraft.getMinecraft().displayGuiScreen(FeatureSettingsEditors.miningAssistant(this))));
        return rows;
    }

    private List<Row> dungeonPage() {
        final Minecraft mc = Minecraft.getMinecraft();
        List<Row> rows = new ArrayList<Row>();
        GuiSettingsAuditModel.Feature dungeonMap = GuiSettingsAuditModel.find("dungeon-map");
        GuiSettingsAuditModel.Feature bossHud = GuiSettingsAuditModel.find("dungeon-boss-hp");
        rows.add(Row.section("Dungeon"));
        rows.add(featureBranch("dungeon.root", null, 0, true, "Dungeon Features", "Open a feature for its detailed settings."));
        rows.add(featureRow("dungeon.spirit-leap", "dungeon.root", 1, false, false, "Spirit Leap",
                "Menu replacement, key shortcut and isolated preview.", () -> AppleSettings.leapMenu,
                value -> AppleSettings.leapMenu = value,
                () -> mc.displayGuiScreen(FeatureSettingsEditors.spiritLeap(this))));
        rows.add(featureRow("dungeon.map", "dungeon.root", 1, false, false, dungeonMap.getTitle(),
                dungeonMap.getDescription(), () -> AppleSettings.dungeonMap, value -> AppleSettings.dungeonMap = value,
                () -> mc.displayGuiScreen(HudEditorScreen.openDungeonMap(this))));
        rows.add(featureRow("dungeon.boss-hp", "dungeon.root", 1, false, false, bossHud.getTitle(),
                assistDescription("Boss HP HUD"), () -> HudManager.DUNGEON_BOSS_HP.enabled,
                value -> HudManager.DUNGEON_BOSS_HP.enabled = value, () -> openHud(HUD_DUNGEON_BOSS_HP)));
        rows.add(featureBranch("dungeon.sync", "dungeon.root", 1, true, "Dungeon Sync",
                "Two small sync switches."));
        rows.add(featureRow("dungeon.sync.enabled", "dungeon.sync", 2, false, false, "Enable Dungeon Sync",
                assistDescription("Enable Dungeon Sync"), new AppleToggle(() -> AssistSettings.dungeonSync,
                        value -> AssistSettings.dungeonSync = value)));
        rows.add(featureRow("dungeon.sync.debug", "dungeon.sync", 2, false, false, "Dungeon Sync Debug",
                assistDescription("Dungeon Sync Debug"), new AppleToggle(() -> AssistSettings.dungeonSyncDebug,
                        value -> AssistSettings.dungeonSyncDebug = value)));
        rows.add(featureRow("dungeon.terminals", "dungeon.root", 1, false, false, "Terminal Assist",
                "Visual helpers, explicit automation and waypoints.", () -> AssistSettings.terminalHints,
                value -> AssistSettings.terminalHints = value,
                () -> mc.displayGuiScreen(FeatureSettingsEditors.terminalAssist(this))));

        rows.add(featureBranch("dungeon.esp", "dungeon.root", 1, true, "Dungeon ESP",
                "Cached dungeon mob and closed-door hitboxes."));
        rows.add(featureRow("dungeon.mob-esp", "dungeon.esp", 2, true, false, "Mob ESP",
                "Per-category styles, live fallback preview, scan controls and presets.",
                () -> DungeonEspSettings.enabled, value -> DungeonEspSettings.enabled = value,
                () -> mc.displayGuiScreen(DungeonEspEditorSpecs.mobEditor(this))));
        addMobEspTreeRows(rows, "dungeon.mob-esp", mc);
        rows.add(featureRow("dungeon.door-esp", "dungeon.esp", 2, true, false, "Door ESP",
                "Wither and blood door filters, shared style and closed-prism preview.",
                () -> DungeonEspSettings.doorsEnabled, value -> DungeonEspSettings.doorsEnabled = value,
                () -> mc.displayGuiScreen(DungeonEspEditorSpecs.doorEditor(this))));
        rows.add(featureRow("dungeon.door-esp.wither", "dungeon.door-esp", 3, false, false, "Wither Doors",
                "Shared cached door style and wither door color.", () -> DungeonEspSettings.doors.enabled,
                value -> DungeonEspSettings.doors.enabled = value,
                () -> mc.displayGuiScreen(DungeonEspEditorSpecs.doorEditor(this, "wither"))));
        rows.add(featureRow("dungeon.door-esp.blood", "dungeon.door-esp", 3, false, false, "Blood Doors",
                "Blood door filter, color and existing shared door style.", () -> DungeonEspSettings.showBloodDoors,
                value -> DungeonEspSettings.showBloodDoors = value,
                () -> mc.displayGuiScreen(DungeonEspEditorSpecs.doorEditor(this, "blood"))));
        rows.add(featureRow("dungeon.m7", "dungeon.root", 1, false, false, "Floor VII (M7) Overlay",
                "Areas, alerts, terminal solver, boss and dragon hitboxes.", M7Settings::isOverlayEnabled,
                M7Settings::setOverlayEnabled,
                () -> mc.displayGuiScreen(FeatureSettingsEditors.m7Overlay(this))));
        return rows;
    }

    /** Adds child feature cards that route to the exact selected Mob ESP editor category. */
    private void addMobEspTreeRows(List<Row> rows, String parentId, final Minecraft mc) {
        addMobEspTreeRow(rows, parentId, "starred", "Starred Mob", "Standard starred dungeon mobs",
                DungeonEspSettings.starred, mc);
        addMobEspTreeRow(rows, parentId, "miniboss", "Miniboss", "Dungeon miniboss hitboxes",
                DungeonEspSettings.miniboss, mc);
        addMobEspTreeRow(rows, parentId, "special", "Special Starred Mob", "Named special starred enemies",
                DungeonEspSettings.special, mc);
        addMobEspTreeRow(rows, parentId, "enderman", "Enderman", "Real and head-marker Endermen",
                DungeonEspSettings.enderman, mc);
        addMobEspTreeRow(rows, parentId, "skeletor", "Skeletor", "Starred Skeletors",
                DungeonEspSettings.skeletor, mc);
        addMobEspTreeRow(rows, parentId, "other", "Other Mobs", "Optional room-wide hostile coverage",
                DungeonEspSettings.otherMobs, mc);
    }

    private void addMobEspTreeRow(List<Row> rows, String parentId, String category, String title, String description,
                                  final DungeonEspSettings.Style style, final Minecraft mc) {
        rows.add(Row.treeFeature("dungeon.mob-esp." + category, parentId, 3, false, false, title, description,
                new FeatureRowControl(() -> style.enabled, value -> style.enabled = value,
                        () -> mc.displayGuiScreen(DungeonEspEditorSpecs.mobEditor(this, category)))));
    }

    private List<Row> fishingPage() {
        List<Row> rows = new ArrayList<Row>();
        GuiSettingsAuditModel.Feature fishingHud = GuiSettingsAuditModel.find("fishing");
        GuiSettingsAuditModel.Feature seaHpHud = GuiSettingsAuditModel.find("seahp");
        rows.add(Row.section("Fishing"));
        rows.add(featureBranch("fishing.root", null, 0, true, "Fishing Features", "Counters, alerts and HUD routes."));
        rows.add(featureRow("fishing.catch-counter", "fishing.root", 1, false, false, "Catch Counter",
                "Catch counts, Festival best and counter reset.", () -> AppleSettings.fishingCounter,
                value -> AppleSettings.fishingCounter = value,
                () -> Minecraft.getMinecraft().displayGuiScreen(FeatureSettingsEditors.catchCounter(this))));
        rows.add(featureBranch("fishing.alerts", "fishing.root", 1, true, "Simple Alerts",
                "Two independent one-switch alerts."));
        rows.add(featureRow("fishing.jawbus", "fishing.alerts", 2, false, false, "Lord Jawbus",
                "Shows a title once when Lord Jawbus arrives.", new AppleToggle(() -> AppleSettings.fishingJawbusAlert,
                        value -> AppleSettings.fishingJawbusAlert = value)));
        rows.add(featureRow("fishing.nearby", "fishing.alerts", 2, false, false, "Nearby Encounters",
                "Optional local title when compatible HP nameplates first appear.",
                new AppleToggle(() -> AppleSettings.fishingNearbyEncounterAlert,
                        value -> AppleSettings.fishingNearbyEncounterAlert = value)));
        rows.add(featureRow("fishing.hud", "fishing.root", 1, false, false, fishingHud.getTitle(),
                fishingHud.getDescription(), () -> HudManager.FISHING.enabled, value -> HudManager.FISHING.enabled = value,
                () -> Minecraft.getMinecraft().displayGuiScreen(HudEditorScreen.openElement(this, HudManager.FISHING))));
        rows.add(featureRow("fishing.sea-hp", "fishing.root", 1, false, false, seaHpHud.getTitle(),
                seaHpHud.getDescription(), () -> HudManager.SEA_HP.enabled, value -> HudManager.SEA_HP.enabled = value,
                () -> Minecraft.getMinecraft().displayGuiScreen(HudEditorScreen.openElement(this, HudManager.SEA_HP))));
        return rows;
    }

    /** Resolves the centrally tested one-line copy for every newly added control. */
    private static String assistDescription(String name) {
        AssistUiModel.Entry entry = AssistUiModel.find(name);
        return entry == null ? "Optional Sky VeLor client assist" : entry.description;
    }

    private static String bossName(M7OverlayRenderer.M7HitboxTarget t) {
        String lower = t.name().toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private List<Row> slayerPage() {
        List<Row> rows = new ArrayList<Row>();
        GuiSettingsAuditModel.Feature slayerHud = GuiSettingsAuditModel.find("slayer");
        rows.add(Row.section("Slayer"));
        rows.add(featureBranch("slayer.root", null, 0, true, "Slayer Features", "Tracker, HUD and drop controls."));
        rows.add(featureRow("slayer.tracker", "slayer.root", 1, false, false, "Track Slayer Drops",
                "Records rare drops from chat until you clear their histories.", () -> AppleSettings.slayerTracker,
                value -> AppleSettings.slayerTracker = value,
                () -> Minecraft.getMinecraft().displayGuiScreen(FeatureSettingsEditors.slayer(this))));
        rows.add(featureRow("slayer.hud", "slayer.root", 1, false, false, slayerHud.getTitle(),
                slayerHud.getDescription(), () -> HudManager.SLAYER.enabled, value -> HudManager.SLAYER.enabled = value,
                () -> Minecraft.getMinecraft().displayGuiScreen(HudEditorScreen.openElement(this, HudManager.SLAYER))));
        rows.add(featureRow("slayer.settings", "slayer.root", 1, false, false, "Boss Timer, Prices and History",
                "Timer, price drops, /setprice help and per-Slayer clear actions.", null, null,
                () -> Minecraft.getMinecraft().displayGuiScreen(FeatureSettingsEditors.slayer(this))));
        return rows;
    }

    /** Empty state for a category with no modules. Honest: no fake settings. */
    private List<Row> emptyPage(String name, final int icon) {
        List<Row> rows = new ArrayList<Row>();
        rows.add(Row.section("Modules"));
        rows.add(Row.custom(64f, (x, y, w, h) -> paintEmpty(x, y, w, h, icon)));
        return rows;
    }

    private List<Row> currentRows() {
        return searching ? searchRows : pages.get(pageIndex);
    }

    private void selectPage(int index) {
        if (index == pageIndex && !searching) {
            return;
        }
        pageIndex = index;
        if (index == PAGE_HUD) {
            pages.set(PAGE_HUD, hudPage());
        } else if (index == PAGE_FARMING) {
            pages.set(PAGE_FARMING, farmingPage());
        } else if (index == PAGE_DUNGEON) {
            pages.set(PAGE_DUNGEON, dungeonPage());
        } else if (index == PAGE_FISHING) {
            pages.set(PAGE_FISHING, fishingPage());
        }
        scroll.reset();
        gestureActive = false;
        active = null;
        openDropdown = null;
        search.clear();
        search.setFocused(false);
        fadeAnim.snap(0f);
        fadeAnim.to(1f);
    }

    // ------------------------------------------------------------------ frame loop

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        HudManager.ensureLoaded();
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
        AppleSettings.save();
        HudManager.save();
    }

    @Override
    public void drawScreen(int mx, int my, float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        long now = System.nanoTime();
        float dt = lastNs == 0L ? 1f / 60f : Math.min(0.1f, (now - lastNs) / 1.0e9f);
        lastNs = now;

        refreshScale(mc);
        trackFrame(dt);
        syncSearch();
        updateStats(dt);
        updateAnims(dt);
        computeLayout();

        // Experimental performance mode lowers only GUI work: fewer curve segments and no shadows.
        boolean perf = AppleSettings.experimentalPerformance;
        int q = (autoLow || perf) ? 2 : AppleSettings.curveQuality;
        UIRender.segCap = AppleSettings.CURVE_SEGMENTS[q];
        UIRender.shadows = AppleSettings.shadows && !autoLow && !perf;

        drawDefaultBackground();

        UIRender.beginFrame();
        float open = openAnim.get();
        float fade = fadeAnim.get();

        // Both themes: the vanilla backdrop shows through a translucent veil (dark or milky white).
        UIRender.alphaMul = open;
        UIRender.rect(0f, 0f, width, height, Theme.veil);
        drawWindowChrome(mx, my);

        if (!collapsed) {
            UIRender.alphaMul = open * fade;
            drawHeader();
            search.draw(mx, my);
            boolean inView = inside(mx, my, vx, vy, vw, vh);
            drawContent(inView ? mx : -10000, inView ? my : -10000);
        }

        UIRender.alphaMul = open;
        if (openDropdown != null && !collapsed) {
            openDropdown.drawPopup(mx, my, wy, wy + wh, wx + ww, wx);
        }
        UIRender.alphaMul = 1f;
        UIRender.endFrame();
    }

    private void refreshScale(Minecraft mc) {
        int key = mc.displayWidth * 31 + mc.displayHeight * 7 + mc.gameSettings.guiScale;
        if (key != scaleKey) {
            scaleKey = key;
            sf = new ScaledResolution(mc).getScaleFactor();
        }
    }

    /** Tracks frame times and drops detail when the client is struggling (adaptive quality). */
    private void trackFrame(float dt) {
        frameHistory[histIdx] = dt * 1000f;
        histIdx = (histIdx + 1) % frameHistory.length;
        dtEma += (dt - dtEma) * 0.1f;

        if (!AppleSettings.adaptive) {
            autoLow = false;
            slowFrames = 0;
            fastFrames = 0;
            return;
        }
        if (dt > 0.05f) {
            slowFrames++;
            fastFrames = 0;
            if (slowFrames > 30) {
                autoLow = true;
            }
        } else if (dt < 0.03f) {
            fastFrames++;
            slowFrames = 0;
            if (fastFrames > 180) {
                autoLow = false;
            }
        }
    }

    private void syncSearch() {
        if (search.version() == searchVersion) {
            return;
        }
        searchVersion = search.version();
        query = search.text().toLowerCase(Locale.ROOT).trim();
        searchRows.clear();
        if (!query.isEmpty()) {
            for (List<Row> page : pages) {
                for (Row r : page) {
                    if (r.kind != Row.ITEM && r.kind != Row.TREE) {
                        continue;
                    }
                    if (r.label.toLowerCase(Locale.ROOT).contains(query)
                            || (r.desc != null && r.desc.toLowerCase(Locale.ROOT).contains(query))) {
                        searchRows.add(r);
                    }
                }
            }
        }
        searching = !query.isEmpty();
        scroll.reset();
        openDropdown = null;
    }

    private void updateStats(float dt) {
        statTimer -= dt;
        if (statTimer > 0f) {
            return;
        }
        // The experimental performance mode refreshes these stats half as often.
        statTimer = AppleSettings.experimentalPerformance ? 1.0f : 0.5f;
        Runtime rt = Runtime.getRuntime();
        statValues[S_FPS] = Minecraft.getDebugFPS() + " fps";
        statValues[S_MEM] = ((rt.totalMemory() - rt.freeMemory()) >> 20) + " MB";
        statValues[S_FRAME] = String.format(Locale.ROOT, "%.1f ms", dtEma * 1000f);
        statValues[S_MINE] = String.format(Locale.ROOT, "%.2f BPS", MiningTracker.rate());
    }

    private void updateAnims(float dt) {
        openAnim.update(dt, 10f);
        fadeAnim.update(dt, 14f);
        indicator.update(dt, 10f);
        search.update(dt);
        for (int i = 0; i < navSel.length; i++) {
            navSel[i].to(!searching && i == pageIndex ? 1f : 0f);
            navSel[i].update(dt, 14f);
            navHover[i].update(dt, 16f);
        }

        scroll.update(dt, gestureActive && gesture.kind == Gesture.SCROLL,
                Theme.animations && AppleSettings.smoothScrolling, Theme.speed);
        boolean moving = gestureActive || scroll.moving();
        indicator.to(moving ? 1f : 0f);

        List<Row> rows = currentRows();
        for (int i = 0, n = rows.size(); i < n; i++) {
            Widget c = rows.get(i).control;
            if (c != null) {
                c.update(dt);
            }
        }
    }

    // ------------------------------------------------------------------ layout

    private void computeLayout() {
        float availableW = Math.max(1f, width - 8f);
        float availableH = Math.max(1f, height - 8f);
        float desiredW = zoomed ? availableW : Math.min(availableW, 620f);
        float desiredH = zoomed ? availableH : Math.min(availableH, 400f);
        // A preferred minimum never exceeds the actual viewport; this is what makes 854x480 and small GUI scales
        // safe without a second hardcoded layout branch.
        desiredW = Math.max(Math.min(160f, availableW), desiredW);
        desiredH = Math.max(Math.min(120f, availableH), desiredH);
        if (collapsed) desiredH = Math.min(desiredH, TITLE_H + 1f);
        float wantedX = zoomed ? 4f : (width - desiredW) * 0.5f;
        float wantedY = zoomed ? 4f : (height - desiredH) * 0.5f + (1f - openAnim.get()) * 12f;
        GuiLayoutToolkit.clampPanel(hubPanel, wantedX, wantedY, desiredW, desiredH, width, height);
        wx = hubPanel.x;
        wy = hubPanel.y;
        ww = hubPanel.w;
        wh = hubPanel.h;
        // A sidebar must fit every navigation row; short windows fall back to the existing compact tab strip.
        wide = ww >= 460f && wh >= TITLE_H + 10f + (PAGE_NAMES.length - 1) * NAV_STEP + NAV_H + 12f;
        tall = wh >= 230f;

        float side = wide ? SIDEBAR_W : 0f;
        float inset = wide ? 16f : PAD;
        cx0 = wx + side + inset;
        cw = Math.max(1f, ww - side - inset - PAD);
        headY = wy + TITLE_H + (wide ? 12f : 40f);

        if (tall) {
            search.x = cx0;
            search.y = headY + 36f;
            search.w = cw;
            search.h = 22f;
        } else {
            float sw = Math.min(170f, cw * 0.5f);
            search.x = cx0 + cw - sw;
            search.y = headY - 3f;
            search.w = sw;
            search.h = 20f;
        }

        vx = cx0;
        vw = cw;
        float bodyBottom = wy + wh - 12f;
        float candidateViewY = tall ? headY + 68f : headY + 26f;
        vy = Math.min(candidateViewY, Math.max(wy + TITLE_H + 2f, bodyBottom - 20f));
        vh = Math.max(1f, bodyBottom - vy);

        if (!searching) {
            applyGroups(pages.get(pageIndex));
        }
        List<Row> rows = currentRows();
        scroll.setMax(Math.max(0f, contentHeight(rows) + 4f - vh));
        layoutRows(rows, vy - scroll.value());
    }

    /** Total height of a row list, without the top gap. Does not move the rows. */
    private float contentHeight(List<Row> rows) {
        float total = 0f;
        boolean first = true;
        for (int i = 0, n = rows.size(); i < n; i++) {
            Row r = rows.get(i);
            if (!shown(r)) {
                continue;
            }
            total += r.height + (first ? 0f : ROW_GAP);
            first = false;
        }
        return Math.max(0f, total);
    }

    /** Whether a row is drawn and hit-tested. Search shows every match, even inside a collapsed group. */
    private boolean shown(Row r) {
        return searching || r.visible;
    }

    /** Sets row visibility from legacy headers and persistent compact-tree ancestors. */
    private void applyGroups(List<Row> rows) {
        Map<String, Row> treeRows = new HashMap<String, Row>();
        for (Row row : rows) {
            if (row.kind == Row.TREE && row.treeId != null && row.treeId.length() > 0) {
                treeRows.put(row.treeId, row);
            }
        }
        Row header = null;
        for (int i = 0, n = rows.size(); i < n; i++) {
            Row r = rows.get(i);
            if (r.kind == Row.HEADER) {
                header = r;
                r.visible = true;
                r.children = 0;
            } else if (r.kind == Row.SECTION) {
                header = null;
                r.visible = true;
            } else {
                boolean groupVisible = header == null || expanded.contains(header.label);
                r.visible = groupVisible && (r.kind != Row.TREE || treeVisible(r, treeRows));
                if (header != null) header.children++;
            }
        }
    }

    /** A closed tree branch has no body geometry; a malformed parent fails closed instead of exposing a stray child. */
    private boolean treeVisible(Row row, Map<String, Row> trees) {
        String parentId = row.treeParentId;
        int safety = 0;
        while (parentId != null && safety++ < 64) {
            Row parentRow = trees.get(parentId);
            if (parentRow == null) return false;
            if (!FeatureUiPreferences.isExpanded(parentRow.treeId, parentRow.treeInitiallyOpen)) return false;
            parentId = parentRow.treeParentId;
        }
        return safety < 64;
    }

    /** Toggles a legacy group. Closing it also closes any dropdown that lives inside it. */
    private void toggleGroup(String title) {
        if (!expanded.remove(title)) expanded.add(title);
        openDropdown = null;
    }

    /** Persists compact-tree state independently from gameplay settings. */
    private void toggleTree(Row row) {
        if (row == null || !row.treeHasChildren || row.treeId == null) return;
        FeatureUiPreferences.toggleExpanded(row.treeId, row.treeInitiallyOpen);
        openDropdown = null;
    }

    private void layoutRows(List<Row> rows, float top) {
        float y = top;
        for (int i = 0, n = rows.size(); i < n; i++) {
            Row r = rows.get(i);
            if (!shown(r)) {
                continue;
            }
            r.y = y;
            Widget c = r.control;
            float indent = r.kind == Row.TREE ? r.treeDepth * FeatureTreeLayout.INDENT : 0f;
            rowLayoutRect.set(cx0 + indent, y, Math.max(1f, cw - indent), r.height);
            float textPad = r.kind == Row.TREE ? (r.treeHasChildren ? 26f : 13f) : 12f;
            if (c != null) {
                GuiLayoutToolkit.splitRow(rowLayoutRect, textPad, 8f, c.preferredWidth(), c.h,
                        rowLabelRect, rowControlRect);
                r.labelX = rowLabelRect.x;
                r.labelWidth = rowLabelRect.w;
                c.x = rowControlRect.x;
                c.y = rowControlRect.y;
                c.w = rowControlRect.w;
            } else {
                r.labelX = rowLayoutRect.x + textPad;
                r.labelWidth = Math.max(1f, rowLayoutRect.w - textPad * 2f);
            }
            y = nextRowTop(y, r.height);
        }
    }

    /** Shared row-flow invariant used by the real layout path and headless dropdown overlap regression tests. */
    static float nextRowTop(float rowTop, float rowHeight) {
        return rowTop + Math.max(0f, rowHeight) + ROW_GAP;
    }

    // ------------------------------------------------------------------ drawing

    private void drawWindowChrome(int mx, int my) {
        UIRender.shadow(wx, wy, ww, wh, Theme.radius);
        UIRender.roundRectBorder(wx, wy, ww, wh, Theme.radius, Theme.windowTop, Theme.windowBottom, Theme.border);
        if (!AppleSettings.darkMode) {
            // Light mode frosting: a soft sheen over the top of the milky panel. No real blur is used.
            float sheenH = Math.min(wh * 0.35f, 90f);
            UIRender.roundRect(wx + 1f, wy + 1f, ww - 2f, sheenH, Math.max(0f, Theme.radius - 1f),
                    0x59FFFFFF, 0x00FFFFFF, 1f);
        }
        UIRender.rect(wx + 1f, wy + TITLE_H, ww - 2f, 1f, Theme.divider);

        // Close (red), collapse (yellow), zoom (green). All three are live in the hub.
        WindowDots.draw(wx + 16f, wy + 17f, mx, my, 0b111, zoomed);

        FontRenderer fr = fr();
        String title = searching ? "Search" : PekaVersion.DISPLAY_NAME;
        UIRender.text(fr, title, wx + (ww - fr.getStringWidth(title)) * 0.5f, wy + 12.5f,
                Theme.textSecondary, 1f, false);

        // The version is small and sits in the title bar, out of the way of the content.
        String ver = PekaVersion.NAME;
        float verW = fr.getStringWidth(ver) * 0.8f;
        UIRender.text(fr, ver, wx + ww - 12f - verW, wy + 13.5f, Theme.textTertiary, 0.8f, false);

        if (collapsed) {
            return;
        }
        if (wide) {
            UIRender.rect(wx + SIDEBAR_W, wy + TITLE_H + 1f, 1f, wh - TITLE_H - 2f, Theme.divider);
            drawSidebar(mx, my);
        } else {
            drawTabs();
        }
    }

    private void drawSidebar(int mx, int my) {
        FontRenderer fr = fr();
        for (int i = 0; i < PAGE_NAMES.length; i++) {
            float x = wx + 8f;
            float w = SIDEBAR_W - 16f;
            float y = wy + TITLE_H + 10f + i * NAV_STEP;
            boolean hov = inside(mx, my, x, y, w, NAV_H);
            navHover[i].to(hov ? 1f : 0f);
            float sel = navSel[i].get();
            float hv = navHover[i].get();

            if (hv > 0.01f) {
                UIRender.roundRect(x, y, w, NAV_H, 8f, UIRender.withAlpha(Theme.cardHover, hv), UIRender.withAlpha(Theme.cardHover, hv), 1f);
            }
            if (sel > 0.01f) {
                int pill = UIRender.withAlpha(Theme.accent, 0.20f * sel);
                UIRender.roundRect(x, y, w, NAV_H, 8f, pill, pill, 1f);
            }
            int iconCol = UIRender.mix(Theme.textSecondary, Theme.accent, sel);
            Icons.draw(PAGE_ICONS[i], x + 13f, y + NAV_H * 0.5f, 12f, iconCol);
            UIRender.text(fr, PAGE_NAMES[i], x + 28f, y + (NAV_H - 9f) * 0.5f + 1f,
                    UIRender.mix(Theme.textSecondary, Theme.textPrimary, sel), 1f, false);
        }

        // Version footer, only when the sidebar is tall enough to show it without overlapping the tabs.
        if (wh >= TITLE_H + 10f + PAGE_NAMES.length * NAV_STEP + 34f && !collapsed) {
            float fy = wy + wh - 34f;
            UIRender.rect(wx + 14f, fy - 6f, SIDEBAR_W - 28f, 1f, Theme.divider);
            UIRender.text(fr, "VERSION", wx + 14f, fy, Theme.textTertiary, 0.75f, false);
            UIRender.text(fr, PekaVersion.NAME, wx + 14f, fy + 11f, Theme.textSecondary, 0.9f, false);
        }
    }

    /** Narrow layout: a segmented tab bar. Names are shown when there is room, otherwise icons only. */
    private void drawTabs() {
        FontRenderer fr = fr();
        float x = wx + PAD;
        float y = wy + TITLE_H + 6f;
        float w = ww - PAD * 2f;
        float h = TAB_H;
        UIRender.roundRect(x, y, w, h, h * 0.5f, Theme.track, Theme.track, 1f);
        float seg = w / PAGE_NAMES.length;
        float widestTabName = 0f;
        for (String pageName : PAGE_NAMES) widestTabName = Math.max(widestTabName, fr.getStringWidth(pageName));
        boolean names = seg >= Math.max(TAB_TEXT_MIN, widestTabName + 12f);
        for (int i = 0; i < PAGE_NAMES.length; i++) {
            float sx = x + i * seg;
            float sel = navSel[i].get();
            if (sel > 0.01f) {
                float inner = h - 4f;
                int pill = UIRender.withAlpha(Theme.segSelected, sel);
                UIRender.roundRect(sx + 2f, y + 2f, seg - 4f, inner, inner * 0.5f, pill, pill, 1f);
            }
            int col = UIRender.mix(Theme.textSecondary, Theme.textPrimary, sel);
            if (names) {
                float tw = fr.getStringWidth(PAGE_NAMES[i]);
                UIRender.text(fr, PAGE_NAMES[i], sx + (seg - tw) * 0.5f, y + (h - 9f) * 0.5f + 1f, col, 1f, false);
            } else {
                Icons.draw(PAGE_ICONS[i], sx + seg * 0.5f, y + h * 0.5f, 11f, col);
            }
        }
    }

    private void drawHeader() {
        FontRenderer fr = fr();
        String title = searching ? "Search" : PAGE_NAMES[pageIndex];
        String sub = searching ? searchRows.size() + (searchRows.size() == 1 ? " match" : " matches")
                : PAGE_SUBS[pageIndex];
        UIRender.text(fr, title, cx0, headY, Theme.textPrimary, 1.5f, false);
        if (tall) {
            UIRender.text(fr, sub, cx0, headY + 19f, Theme.textSecondary, 1f, false);
        }
    }

    private void drawContent(int mx, int my) {
        FontRenderer fr = fr();
        List<Row> rows = currentRows();
        UIRender.scissorBegin(vx, vy, vw, vh, sf);

        if (searching && rows.isEmpty()) {
            String none = "No matching settings";
            UIRender.text(fr, none, vx + (vw - fr.getStringWidth(none)) * 0.5f, vy + 16f,
                    Theme.textTertiary, 1f, false);
        }

        float top = vy;
        float bottom = vy + vh;
        for (int i = 0, n = rows.size(); i < n; i++) {
            Row r = rows.get(i);
            if (!shown(r) || r.y + r.height < top || r.y > bottom) {
                continue;
            }
            GuiDebugOverlay.recordRow("Hub: " + r.label, vx, r.y, vw, r.height);
            if (r.kind == Row.SECTION) {
                UIRender.text(fr, r.label, vx + 2f, r.y + 4f, Theme.textTertiary, 0.85f, false);
            } else if (r.kind == Row.HEADER) {
                drawGroupHeader(fr, r, mx, my);
            } else if (r.kind == Row.ITEM) {
                drawItem(fr, r, mx, my);
            } else if (r.kind == Row.TREE) {
                drawTreeFeature(fr, r, mx, my);
            } else {
                r.painter.paint(vx, r.y, vw, r.height);
            }
        }
        drawScrollIndicator();
        UIRender.scissorEnd();
    }

    /** Collapsible category: a chevron (right when closed, down when open), the title, and the row count. */
    private void drawGroupHeader(FontRenderer fr, Row r, int mx, int my) {
        float x = vx;
        float y = r.y;
        float w = vw;
        float h = r.height;
        boolean open = expanded.contains(r.label);
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + h;
        UIRender.roundRectBorder(x, y, w, h, cardRadius(), Theme.card, Theme.card, Theme.border);
        int ink = hov ? Theme.accent : Theme.textSecondary;
        float cx = x + 14f;
        float cy = y + h * 0.5f;
        if (open) {
            UIRender.triangle(cx - 4f, cy - 2f, cx + 4f, cy - 2f, cx, cy + 3f, ink);
        } else {
            UIRender.triangle(cx - 2f, cy - 4f, cx - 2f, cy + 4f, cx + 3f, cy, ink);
        }
        float ty = y + (h - 9f) * 0.5f + 1f;
        String count = r.children + (r.children == 1 ? " setting" : " settings");
        float cwid = fr.getStringWidth(count) * 0.9f;
        float titleW = Math.max(0f, w - 26f - 12f - cwid - 8f);
        // At tiny GUI widths the title is more useful than a clipped count; never let the two text runs collide.
        boolean showCount = titleW >= Math.min(40f, fr.getStringWidth(r.label));
        if (!showCount) titleW = Math.max(0f, w - 26f - 12f);
        UIRender.text(fr, fit(fr, r.label, titleW), x + 26f, ty, Theme.textPrimary, 1.05f, false);
        if (showCount) {
            UIRender.text(fr, count, x + w - 12f - cwid, ty, Theme.textTertiary, 0.9f, false);
        }
    }

    private void drawItem(FontRenderer fr, Row r, int mx, int my) {
        float x = vx;
        float y = r.y;
        float w = vw;
        float h = r.height;
        UIRender.roundRectBorder(x, y, w, h, cardRadius(), Theme.card, Theme.card, Theme.border);

        float labelX = r.labelX > 0f ? r.labelX : x + 12f;
        float maxW = r.labelWidth > 0f ? r.labelWidth : Math.max(1f, w - 24f);
        String label = fit(fr, r.label, maxW);
        if (r.desc != null) {
            UIRender.text(fr, label, labelX, y + 7f, Theme.textPrimary, 1f, false);
            UIRender.text(fr, fit(fr, r.desc, maxW), labelX, y + 21f, Theme.textSecondary, 1f, false);
        } else if (!label.isEmpty()) {
            UIRender.text(fr, label, labelX, y + (h - 9f) * 0.5f + 1f, Theme.textPrimary, 1f, false);
        }
        if (r.control != null) {
            r.control.draw(mx, my);
        }
    }

    /** Compact feature tree card with a persisted arrow, indentation, vertical connector and child branch tick. */
    private void drawTreeFeature(FontRenderer fr, Row r, int mx, int my) {
        float indent = r.treeDepth * FeatureTreeLayout.INDENT;
        float x = vx + indent;
        float y = r.y;
        float w = Math.max(1f, vw - indent);
        float h = r.height;
        boolean hover = inside(mx, my, x, y, w, h);
        UIRender.roundRectBorder(x, y, w, h, cardRadius(), Theme.card, Theme.card,
                hover ? UIRender.mix(Theme.border, Theme.accent, .45f) : Theme.border);

        // Parent stems are intentionally drawn in the shared indentation rhythm. They sit outside nested cards,
        // making every child branch traceable even when an action strip is at the right edge.
        if (r.treeDepth > 0) {
            for (int level = 0; level < r.treeDepth; level++) {
                float lineX = vx + 8f + level * FeatureTreeLayout.INDENT;
                UIRender.rect(lineX, y - ROW_GAP * .5f, 1f, h * .5f + ROW_GAP * .5f, Theme.divider);
            }
            float tickX = vx + 8f + (r.treeDepth - 1) * FeatureTreeLayout.INDENT;
            UIRender.rect(tickX, y + h * .5f, FeatureTreeLayout.CONNECTOR_TICK, 1f, Theme.divider);
        }
        if (r.treeHasChildren) {
            boolean open = FeatureUiPreferences.isExpanded(r.treeId, r.treeInitiallyOpen);
            float cx = x + 12f;
            float cy = y + h * .5f;
            int ink = hover ? Theme.accent : Theme.textSecondary;
            if (open) UIRender.triangle(cx - 4f, cy - 2f, cx + 4f, cy - 2f, cx, cy + 3f, ink);
            else UIRender.triangle(cx - 2f, cy - 4f, cx - 2f, cy + 4f, cx + 3f, cy, ink);
        }
        float labelX = r.labelX > 0f ? r.labelX : x + (r.treeHasChildren ? 26f : 12f);
        float maxW = r.labelWidth > 0f ? r.labelWidth : Math.max(1f, w - 24f);
        UIRender.text(fr, fit(fr, r.label, maxW), labelX, y + 7f, Theme.textPrimary, 1f, false);
        if (r.desc != null && r.desc.length() > 0) {
            UIRender.text(fr, fit(fr, r.desc, maxW), labelX, y + 22f, Theme.textSecondary, .92f, false);
        }
        if (r.control != null) r.control.draw(mx, my);
    }

    private void drawScrollIndicator() {
        float ind = indicator.get();
        float maxS = scroll.max();
        if (ind <= 0.01f || maxS <= 0f) {
            return;
        }
        float track = vh - 8f;
        float thumb = Math.max(24f, track * vh / (contentHeight(currentRows()) + 4f));
        float ty = vy + 4f + (track - thumb) * (scroll.value() / maxS);
        float prev = UIRender.alphaMul;
        UIRender.alphaMul = prev * ind;
        UIRender.roundRect(vx + vw - 3f, ty, 3f, thumb, 1.5f, Theme.textSecondary, Theme.textSecondary, 0.5f);
        UIRender.alphaMul = prev;
    }

    /** Stat cards for the QoL overview. Painter, no widgets. */
    private void paintStats(float x, float y, float w, float h) {
        FontRenderer fr = fr();
        final int[] order = {S_FPS, S_MINE, S_MEM};
        final String[] labels = {"FRAME RATE", "MINING", "MEMORY"};
        float gap = 8f;
        float cardW = (w - gap * 2f) / 3f;
        for (int i = 0; i < order.length; i++) {
            float cx = x + i * (cardW + gap);
            UIRender.roundRectBorder(cx, y, cardW, h, cardRadius(), Theme.card, Theme.card, Theme.border);
            UIRender.text(fr, labels[i], cx + 10f, y + 11f, Theme.textTertiary, 0.8f, false);
            int valueCol = i == 0 || i == 1 ? Theme.accent : Theme.textPrimary;
            UIRender.text(fr, statValues[order[i]], cx + 10f, y + h - 26f, valueCol, 1.4f, false);
        }
    }

    /** Frame-time bar chart for the QoL page. Batched into one draw call. */
    private void paintGraph(float x, float y, float w, float h) {
        FontRenderer fr = fr();
        UIRender.roundRectBorder(x, y, w, h, cardRadius(), Theme.card, Theme.card, Theme.border);
        UIRender.text(fr, "FRAME TIME", x + 12f, y + 9f, Theme.textTertiary, 0.8f, false);
        String v = statValues[S_FRAME];
        UIRender.text(fr, v, x + w - 12f - fr.getStringWidth(v), y + 8f, Theme.textPrimary, 1f, false);

        float px = x + 12f;
        float pw = w - 24f;
        float py = y + 26f;
        float ph = h - 36f;
        UIRender.rect(px, py + ph * 0.5f, pw, 1f, Theme.divider);

        int n = frameHistory.length;
        float bw = pw / n;
        UIRender.beginBatch();
        for (int i = 0; i < n; i++) {
            float ms = frameHistory[(histIdx + i) % n];
            float bh = Math.max(1f, Math.min(1f, ms / 33.3f) * ph);
            int c = ms > 33.3f ? 0xFFFF9F0A : Theme.accent;
            UIRender.rect(px + i * bw, py + ph - bh, Math.max(1f, bw - 1f), bh, c);
        }
        UIRender.endBatch();
    }

    /** Empty state for a category with no modules. Painter, no widgets. */
    private void paintEmpty(float x, float y, float w, float h, int icon) {
        FontRenderer fr = fr();
        UIRender.roundRectBorder(x, y, w, h, cardRadius(), Theme.card, Theme.card, Theme.border);
        Icons.draw(icon, x + 26f, y + h * 0.5f, 22f, Theme.textTertiary);
        UIRender.text(fr, "No modules yet", x + 52f, y + h * 0.5f - 13f, Theme.textPrimary, 1f, false);
        String note = "Nothing is available in this category yet.";
        UIRender.text(fr, fit(fr, note, w - 64f), x + 52f, y + h * 0.5f + 3f, Theme.textSecondary, 1f, false);
    }

    private float cardRadius() {
        return Math.max(4f, Theme.radius - 4f);
    }

    // ------------------------------------------------------------------ input

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        if (button != 0) {
            return;
        }
        if (handleOpenDropdownClick(mx, my)) {
            return;
        }
        if (!inside(mx, my, wx, wy, ww, wh)) {
            close();
            return;
        }
        int dot = WindowDots.hit(mx, my, wx + 16f, wy + 17f, 0b111);
        if (dot == WindowDots.CLOSE) {
            close();
            return;
        }
        if (dot == WindowDots.COLLAPSE) {
            toggleCollapsed();
            return;
        }
        if (dot == WindowDots.ZOOM) {
            toggleZoom();
            return;
        }
        if (collapsed) {
            return;
        }
        int nav = navIndexAt(mx, my);
        if (nav >= 0) {
            selectPage(nav);
            return;
        }
        if (search.contains(mx, my)) {
            search.setFocused(true);
            return;
        }
        search.setFocused(false);
        if (!inside(mx, my, vx, vy, vw, vh)) {
            return;
        }

        Row header = headerAt(mx, my);
        if (header == null) header = treeAt(mx, my);
        if (header != null) {
            pendingHeader = header;
            active = null;
            sliderEngaged = false;
            gestureActive = true;
            gesture.begin(null, mx, my, scroll.value());
            return;
        }
        Widget w = widgetAt(mx, my);
        active = w;
        sliderEngaged = false;
        gestureActive = true;
        gesture.begin(w, mx, my, scroll.value());
        if (w != null && !(w instanceof AppleSlider)) {
            w.onPress(mx, my);
        }
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long timeSinceLastClick) {
        if (!gestureActive) {
            return;
        }
        float speed = scrollSpeed();
        int kind = gesture.move(mx, my);
        if (kind == Gesture.SLIDER) {
            if (!sliderEngaged) {
                sliderEngaged = true;
                active.onPress((int) gesture.startX, (int) gesture.startY);
            }
            active.onDrag(mx, my);
        } else if (kind == Gesture.SCROLL) {
            // The swipe cancels any press. The control is not activated on release.
            if (active != null) {
                active.onRelease(mx, my, false);
                active = null;
            }
            scroll.setImmediate(gesture.scrollAt(my, speed));
            gesture.track(my, speed);
        }
    }

    @Override
    protected void mouseReleased(int mx, int my, int button) {
        if (active != null) {
            if (active instanceof AppleSlider && gesture.kind == Gesture.PENDING) {
                // A tap on a slider sets it where the finger lifted.
                active.onPress((int) gesture.startX, (int) gesture.startY);
            }
            boolean activate = gesture.kind == Gesture.PENDING && active.contains(mx, my);
            Widget a = active;
            active = null;
            a.onRelease(mx, my, activate);
            if (activate && a instanceof AppleDropdown) {
                AppleDropdown d = (AppleDropdown) a;
                openDropdown = (openDropdown == d) ? null : d;
                if (openDropdown != null) {
                    openDropdown.onExpand();
                }
            }
        }
        if (pendingHeader != null) {
            Row header = pendingHeader;
            pendingHeader = null;
            if (gesture.kind == Gesture.PENDING && mx >= vx && mx < vx + vw
                    && my >= header.y && my < header.y + header.height) {
                if (header.kind == Row.TREE) toggleTree(header); else toggleGroup(header.label);
            }
        }
        if (gestureActive && gesture.kind == Gesture.SCROLL) {
            scroll.fling(gesture.flingVelocity() * scrollSpeed());
        }
        gestureActive = false;
        sliderEngaged = false;
        gesture.target = null;
        gesture.kind = Gesture.PENDING;
    }

    /** Mouse wheel. 1.8.9 has no wheel callback, so read the event before the default handling. */
    @Override
    public void handleMouseInput() throws IOException {
        int wheel = Mouse.getEventDWheel();
        super.handleMouseInput();
        int mx = Mouse.getEventX() * width / Minecraft.getMinecraft().displayWidth;
        int my = height - Mouse.getEventY() * height / Minecraft.getMinecraft().displayHeight - 1;
        if (wheel != 0 && openDropdown == null && inside(mx, my, vx, vy, vw, vh)) {
            boolean fine = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
            scroll.wheel(wheel / 120f, fine, scrollSpeed());
        }
    }

    private float scrollSpeed() {
        return AppleSettings.guiScrollMultiplier();
    }

    @Override
    protected void keyTyped(char c, int code) throws IOException {
        if (itemLockSwapKeyButton != null && itemLockSwapKeyButton.isListening()) {
            if (code == Keyboard.KEY_ESCAPE) {
                itemLockSwapKeyButton.cancelCapture();
                return;
            }
            if (ItemLockSwapKeybind.canBind(code)) {
                String conflict = PekaClientMod.rebindItemLockSwapKey(code);
                itemLockSwapKeyButton.finishCapture();
                if (conflict != null && Minecraft.getMinecraft().thePlayer != null) {
                    String key = Keyboard.getKeyName(code);
                    Minecraft.getMinecraft().thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
                            "\u00a7e[Sky VeLor] \u00a7f" + key + " is also used by " + conflict));
                }
            }
            return;
        }
        if (collapsed && code != Keyboard.KEY_ESCAPE) {
            return;
        }
        if (search.isFocused()) {
            if (code == Keyboard.KEY_ESCAPE) {
                if (!search.isEmpty()) {
                    search.clear();
                } else {
                    search.setFocused(false);
                }
                return;
            }
            if (search.keyTyped(c, code)) {
                return;
            }
        }
        if (code == Keyboard.KEY_ESCAPE) {
            if (openDropdown != null) {
                openDropdown = null;
                return;
            }
            close();
            return;
        }
        if (isCtrlKeyDown()) {
            if (code == Keyboard.KEY_F) {
                search.setFocused(true);
                return;
            }
            if (code >= Keyboard.KEY_1 && code - Keyboard.KEY_1 < PAGE_NAMES.length) {
                selectPage(code - Keyboard.KEY_1);
            }
            return;
        }
        if (handleNavigationKey(code)) {
            return;
        }
        if (c >= 32 && c != 127) {
            search.setFocused(true);
            search.keyTyped(c, code);
        }
    }

    /** Keyboard navigation: tabs with Left and Right, the list with the scroll keys. */
    private boolean handleNavigationKey(int code) {
        float page = Math.max(40f, vh * 0.8f);
        switch (code) {
            case Keyboard.KEY_LEFT:
                selectPage(Math.max(0, pageIndex - 1));
                return true;
            case Keyboard.KEY_RIGHT:
                selectPage(Math.min(PAGE_NAMES.length - 1, pageIndex + 1));
                return true;
            case Keyboard.KEY_DOWN:
                scroll.key(ScrollState.KEY_STEP);
                return true;
            case Keyboard.KEY_UP:
                scroll.key(-ScrollState.KEY_STEP);
                return true;
            case Keyboard.KEY_NEXT:
                scroll.key(page);
                return true;
            case Keyboard.KEY_PRIOR:
                scroll.key(-page);
                return true;
            case Keyboard.KEY_HOME:
                scroll.key(-1.0e7f);
                return true;
            case Keyboard.KEY_END:
                scroll.key(1.0e7f);
                return true;
            default:
                return false;
        }
    }

    private void close() {
        Minecraft.getMinecraft().displayGuiScreen(parent);
    }

    /** Yellow: hide the body and keep the title bar. Clears any open popup and focus first. */
    private void toggleCollapsed() {
        collapsed = !collapsed;
        resetTransient();
    }

    /** Green: fill the screen, or go back to the normal size. Expands the window if it was collapsed. */
    private void toggleZoom() {
        zoomed = !zoomed;
        collapsed = false;
        resetTransient();
    }

    private void resetTransient() {
        openDropdown = null;
        active = null;
        if (itemLockSwapKeyButton != null) itemLockSwapKeyButton.cancelCapture();
        gestureActive = false;
        sliderEngaged = false;
        search.setFocused(false);
    }

    private int navIndexAt(int mx, int my) {
        if (wide) {
            float x = wx + 8f;
            float w = SIDEBAR_W - 16f;
            for (int i = 0; i < PAGE_NAMES.length; i++) {
                float y = wy + TITLE_H + 10f + i * NAV_STEP;
                if (inside(mx, my, x, y, w, NAV_H)) {
                    return i;
                }
            }
            return -1;
        }
        float y = wy + TITLE_H + 6f;
        if (my < y || my >= y + TAB_H) {
            return -1;
        }
        float x0 = wx + PAD;
        float seg = (ww - PAD * 2f) / PAGE_NAMES.length;
        if (mx < x0) {
            return -1;
        }
        int i = (int) ((mx - x0) / seg);
        return (i >= 0 && i < PAGE_NAMES.length) ? i : -1;
    }

    /** The group header under the pointer inside the visible list, or null. */
    private Row headerAt(int mx, int my) {
        if (searching || !inside(mx, my, vx, vy, vw, vh)) {
            return null;
        }
        List<Row> rows = pages.get(pageIndex);
        for (int i = 0, n = rows.size(); i < n; i++) {
            Row r = rows.get(i);
            if (r.kind == Row.HEADER && my >= r.y && my < r.y + r.height && mx >= vx && mx < vx + vw) {
                return r;
            }
        }
        return null;
    }

    /** Only the arrow/leading label portion toggles a compact branch; action strips retain their own click targets. */
    private Row treeAt(int mx, int my) {
        if (searching || !inside(mx, my, vx, vy, vw, vh)) return null;
        List<Row> rows = pages.get(pageIndex);
        for (Row row : rows) {
            if (row.kind != Row.TREE || !row.treeHasChildren || !shown(row)) continue;
            float left = vx + row.treeDepth * FeatureTreeLayout.INDENT;
            float hitWidth = Math.min(Math.max(32f, row.labelWidth + 10f), Math.max(32f, vw - row.treeDepth * FeatureTreeLayout.INDENT));
            if (mx >= left && mx < left + hitWidth && my >= row.y && my < row.y + row.height) return row;
        }
        return null;
    }

    private Widget widgetAt(int mx, int my) {
        List<Row> rows = currentRows();
        for (int i = rows.size() - 1; i >= 0; i--) {
            Row row = rows.get(i);
            Widget c = row.control;
            if (c != null && shown(row) && c.contains(mx, my)) {
                return c;
            }
        }
        return null;
    }
}
