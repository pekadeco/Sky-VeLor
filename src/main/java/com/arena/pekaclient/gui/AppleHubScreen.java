package com.arena.pekaclient.gui;

import com.arena.pekaclient.PekaVersion;
import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.config.AssistSettings;
import com.arena.pekaclient.config.ChatQolSettings;
import com.arena.pekaclient.config.DungeonEspSettings;
import com.arena.pekaclient.feature.ChatChannelTabs;
import com.arena.pekaclient.dungeon.M7Settings;
import com.arena.pekaclient.hud.ClientMetrics;
import com.arena.pekaclient.hud.HudElement;
import com.arena.pekaclient.hud.HudManager;
import com.arena.pekaclient.itemlockswap.ItemLockSwapSettings;
import com.arena.pekaclient.render.Anim;
import com.arena.pekaclient.render.Icons;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import com.arena.pekaclient.widget.AppleButton;
import com.arena.pekaclient.widget.AppleDropdown;
import com.arena.pekaclient.widget.AppleSlider;
import com.arena.pekaclient.widget.AppleToggle;
import com.arena.pekaclient.widget.SearchField;
import com.arena.pekaclient.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The Sky VeLor hub: a macOS-style window with window buttons, a sidebar (or tab bar on narrow
 * screens), a search field and scrollable grouped cards.
 *
 * Tabs, in order: General, Quality of Life, HUD, Farming, Dungeon, Slayer, Fishing. General is the first overview
 * page; every tab uses direct controls or flat one-level editor dropdowns. HUD routes visual customization to the
 * HUD editor, while focused editors own rich feature settings without nested tab controls.
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
            "Appearance, global settings and performance",
            "Simple gameplay toggles and focused feature editors",
            "General HUD elements and first-person renderer settings",
            "Mining BPS and Mining Assistant editors",
            "Sync, Spirit Leap, cached ESP and Floor VII overlay",
            "Drop tracker, boss timer, prices and Slayer HUD",
            "Alerts, catch counters, Fishing HUD and Sea Creature HP"
    };
    /** HUD targets in editor order; contextual tabs route their own specialised HUD features. */
    private static final HudElement[] HUD_OVERVIEW = {
            HudManager.DOLL, HudManager.INVENTORY, HudManager.STATS, HudManager.MINING,
            HudManager.FISHING, HudManager.SEA_HP, HudManager.SLAYER, HudManager.MINING_ASSIST, HudManager.DUNGEON_BOSS_HP};
    /** The HUD tab keeps only general-purpose elements; specialised HUDs live in their feature tab. */
    private static final HudElement[] HUD_TAB_ELEMENTS = {
            HudManager.STATS, HudManager.DOLL, HudManager.INVENTORY};
    private static final int HUD_MINING = 3; // index into HUD_OVERVIEW
    private static final int HUD_MINING_ASSIST = 7;
    private static final int HUD_DUNGEON_BOSS_HP = 8;
    private static final int[] PAGE_ICONS = {
            Icons.GEAR, Icons.BOLT, Icons.HUD, Icons.LEAF, Icons.TOWER, Icons.SWORD, Icons.FISH
    };
    private static final int PAGE_HUD = 2;
    private static final int PAGE_FARMING = 3;
    private static final int PAGE_FISHING = 6;
    private static final int PAGE_DUNGEON = 4;

    private final GuiScreen parent;
    private final List<List<Row>> pages = new ArrayList<List<Row>>();
    private int pageIndex;

    private final SearchField search = new SearchField("Search settings");
    private final List<Row> searchRows = new ArrayList<Row>();
    private int searchVersion = -1;
    private boolean searching;
    private String query = "";

    /** Titles of the one-level dropdowns currently expanded while the Hub is open. */
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
    private int slowFrames;
    private int fastFrames;
    private boolean autoLow;
    /** GUI-only telemetry; this is deliberately not a HUD element or a persisted feature toggle. */
    private final ClientMetrics.Reader clientMetrics = ClientMetrics.liveReader();
    private final GeneralMetricsLayout.Layout generalMetrics = new GeneralMetricsLayout.Layout();
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

    /** Adds one direct Boolean control. It never opens a dropdown. */
    private static void simpleFeature(List<Row> rows, String title, String description,
                                      java.util.function.Supplier<Boolean> getter,
                                      java.util.function.Consumer<Boolean> setter) {
        rows.add(Row.item(title, description, new AppleToggle(getter, setter)));
    }

    /** Adds one direct whole-percent camera-tilt control without recreating the retired binary switch. */
    private static void percentSliderFeature(List<Row> rows, String title, String description,
                                             java.util.function.IntSupplier getter,
                                             java.util.function.IntConsumer setter) {
        rows.add(Row.item(title, description, new AppleSlider(0, 100, 1, "%", 172f, getter, setter)
                .valueLabel(value -> value + "%")));
    }

    /** Opens a detailed surface that has no meaningful all-or-nothing master toggle. */
    private static void settingsFeature(List<Row> rows, String title, String description, Runnable open) {
        rows.add(Row.item(title, description, new AppleButton("Settings", 172f, 24f, true, open)));
    }

    /**
     * Adds one flat, one-level dropdown. Its two and only two body rows are On/Off and Settings; the header status
     * dot reads the same toggle, so it cannot drift from the control below it.
     */
    private static void editorFeature(List<Row> rows, String title, String description,
                                      java.util.function.Supplier<Boolean> getter,
                                      java.util.function.Consumer<Boolean> setter, Runnable open) {
        rows.add(Row.dropdown(title, description, getter));
        rows.add(Row.item("On/Off", "Turn this feature on or off.", new AppleToggle(getter, setter)));
        rows.add(Row.item("Settings", "Open " + title + " settings.",
                new AppleButton("Settings", 172f, 24f, true, open)));
    }

    /** One flat Quality of Life page: simple switches first, then one-level editor dropdowns. */
    private List<Row> qualityOfLifePage() {
        List<Row> rows = new ArrayList<Row>();
        rows.add(Row.section("Simple features"));
        List<QualityOfLifeUiModel.Entry> controls = QualityOfLifeUiModel.buildControlModel();
        simpleFeature(rows, "No Block Particles", controls.get(0).getDescription(),
                () -> AppleSettings.noBlockParticles, value -> AppleSettings.noBlockParticles = value);
        simpleFeature(rows, "No Hotbar Scrolling", controls.get(1).getDescription(),
                () -> AppleSettings.disableHotbarScrolling, value -> AppleSettings.disableHotbarScrolling = value);
        percentSliderFeature(rows, "Hurt Camera Shake", controls.get(2).getDescription(),
                () -> AssistSettings.hurtCameraShake, value -> AssistSettings.hurtCameraShake = value);
        simpleFeature(rows, "Auto Sprint", controls.get(3).getDescription(),
                () -> AssistSettings.autoSprint, value -> AssistSettings.autoSprint = value);

        rows.add(Row.section("Feature editors"));
        settingsFeature(rows, "Inventory Assist", "Tooltip, potion-panel and inventory visual helpers.",
                () -> Minecraft.getMinecraft().displayGuiScreen(FeatureSettingsEditors.inventoryAssist(this)));
        editorFeature(rows, "Movement and Shortcuts", "Wardrobe and pets shortcuts.", () -> AssistSettings.wardrobeShortcut,
                value -> AssistSettings.wardrobeShortcut = value,
                () -> Minecraft.getMinecraft().displayGuiScreen(FeatureSettingsEditors.movementShortcuts(this)));
        editorFeature(rows, "Item Lock and Swap", "Pair locks, indicators and persistent pair-center lines.",
                () -> ItemLockSwapSettings.enabled, value -> ItemLockSwapSettings.enabled = value, () -> Minecraft.getMinecraft().displayGuiScreen(FeatureSettingsEditors.itemLockSwap(this)));
        editorFeature(rows, "Chat QoL", "Attached tabs, message animation and highlighting.", () -> ChatQolSettings.enabled, value -> {
                    ChatQolSettings.enabled = value;
                    if (!value) ChatChannelTabs.disableAndRestoreAll();
                }, () -> Minecraft.getMinecraft().displayGuiScreen(new ChatEditorScreen(this)));
        editorFeature(rows, "Commands", "Optional /sv and /qn routes plus diagnostics.", () -> AppleSettings.svCommand,
                value -> AppleSettings.svCommand = value,
                () -> Minecraft.getMinecraft().displayGuiScreen(FeatureSettingsEditors.commands(this)));
        return rows;
    }

    /** General begins with GUI-only live telemetry and routes multi-option controls to focused editors. */
    private List<Row> generalPage() {
        List<Row> rows = new ArrayList<Row>();
        rows.add(Row.section("Feature editors"));
        editorFeature(rows, "Appearance", "Dark/light theme, accent, corners and panel opacity.", () -> AppleSettings.darkMode,
                value -> { AppleSettings.darkMode = value; Theme.refresh(); },
                () -> Minecraft.getMinecraft().displayGuiScreen(FeatureSettingsEditors.appearance(this)));
        editorFeature(rows, "Mod Settings", "Global scroll speed and smoothing; Shift + wheel is fine scrolling.", () -> AppleSettings.smoothScrolling,
                value -> AppleSettings.smoothScrolling = value,
                () -> Minecraft.getMinecraft().displayGuiScreen(FeatureSettingsEditors.modSettings(this)));
        editorFeature(rows, "Performance", "Curve, shadows, animation and adaptive GUI quality.", () -> AppleSettings.experimentalPerformance,
                value -> AppleSettings.experimentalPerformance = value,
                () -> Minecraft.getMinecraft().displayGuiScreen(FeatureSettingsEditors.performance(this)));
        return rows;
    }

    /** Retained entry point: the former HUD subcategory now opens the flat Dungeon tab directly. */
    public static AppleHubScreen dungeonHud(GuiScreen parent) {
        AppleHubScreen hub = new AppleHubScreen(parent);
        hub.selectPage(PAGE_DUNGEON);
        return hub;
    }

    private List<Row> hudPage() {
        final Minecraft mc = Minecraft.getMinecraft();
        List<Row> rows = new ArrayList<Row>();
        rows.add(Row.section("Feature editors"));
        editorFeature(rows, "Item Animation", "Visual first-person transforms only; never changes packets, attacks or gameplay.", () -> AppleSettings.itemAnimationEnabled, value -> {
                    AppleSettings.itemAnimationEnabled = value;
                    AppleSettings.save();
                }, () -> mc.displayGuiScreen(HudEditorScreen.openItemAnimation(this)));
        for (final HudElement element : HUD_TAB_ELEMENTS) {
            GuiSettingsAuditModel.Feature feature = GuiSettingsAuditModel.find(element.id);
            final String title = feature == null ? element.name : feature.getTitle();
            final String description = feature == null ? "Shown on screen." : feature.getDescription();
            editorFeature(rows, title, description,
                    () -> element.enabled, value -> element.enabled = value,
                    () -> mc.displayGuiScreen(HudEditorScreen.openElement(this, element)));
        }
        return rows;
    }

    /** Opens one HUD element at its only customization surface: the HUD editor. */
    private void openHud(int index) {
        if (index < 0 || index >= HUD_OVERVIEW.length) return;
        Minecraft.getMinecraft().displayGuiScreen(HudEditorScreen.openElement(this, HUD_OVERVIEW[index]));
    }

    private List<Row> farmingPage() {
        List<Row> rows = new ArrayList<Row>();
        GuiSettingsAuditModel.Feature mining = GuiSettingsAuditModel.find("mining");
        GuiSettingsAuditModel.Feature assistant = GuiSettingsAuditModel.find("mining-assistant");
        rows.add(Row.section("Feature editors"));
        editorFeature(rows, mining.getTitle(), mining.getDescription(), () -> HudManager.MINING.enabled,
                value -> HudManager.MINING.enabled = value, () -> openHud(HUD_MINING));
        editorFeature(rows, assistant.getTitle(), assistant.getDescription(), () -> HudManager.MINING_ASSIST.enabled,
                value -> HudManager.MINING_ASSIST.enabled = value,
                () -> Minecraft.getMinecraft().displayGuiScreen(FeatureSettingsEditors.miningAssistant(this)));
        return rows;
    }

    private List<Row> dungeonPage() {
        final Minecraft mc = Minecraft.getMinecraft();
        List<Row> rows = new ArrayList<Row>();
        GuiSettingsAuditModel.Feature dungeonMap = GuiSettingsAuditModel.find("dungeon-map");
        GuiSettingsAuditModel.Feature bossHud = GuiSettingsAuditModel.find("dungeon-boss-hp");

        // Flat usage order: ESP, solvers/helpers, then information/other. No group exceeds eight rows.
        editorFeature(rows, "Door ESP", "Wither and blood door filters, shared style and closed-prism preview.", () -> DungeonEspSettings.doorsEnabled,
                value -> DungeonEspSettings.doorsEnabled = value,
                () -> mc.displayGuiScreen(DungeonEspEditorSpecs.doorEditor(this)));
        editorFeature(rows, "Mob ESP", "Per-mob styles, live hitbox preview, scan controls and presets.", () -> DungeonEspSettings.enabled,
                value -> DungeonEspSettings.enabled = value,
                () -> mc.displayGuiScreen(DungeonEspEditorSpecs.mobEditor(this)));

        simpleFeature(rows, "Dungeon Sync", assistDescription("Dungeon Sync"),
                () -> AssistSettings.dungeonSync, value -> AssistSettings.dungeonSync = value);
        editorFeature(rows, "Floor VII (M7) Overlay", "Areas, alerts, terminal solver, boss and dragon hitboxes.", M7Settings::isOverlayEnabled,
                M7Settings::setOverlayEnabled,
                () -> mc.displayGuiScreen(FeatureSettingsEditors.m7Overlay(this)));
        editorFeature(rows, "Spirit Leap GUI", "Dungeon-only teammate picker, shortcut and safe preview.",
                () -> AppleSettings.leapMenu, value -> AppleSettings.leapMenu = value,
                () -> mc.displayGuiScreen(FeatureSettingsEditors.spiritLeap(this)));
        editorFeature(rows, "Terminal Assist", "Visual helpers, explicit automation and waypoints.", () -> AssistSettings.terminalHints,
                value -> AssistSettings.terminalHints = value,
                () -> mc.displayGuiScreen(FeatureSettingsEditors.terminalAssist(this)));

        editorFeature(rows, bossHud.getTitle(), assistDescription("Boss HP HUD"), () -> HudManager.DUNGEON_BOSS_HP.enabled,
                value -> HudManager.DUNGEON_BOSS_HP.enabled = value,
                () -> openHud(HUD_DUNGEON_BOSS_HP));
        editorFeature(rows, dungeonMap.getTitle(), dungeonMap.getDescription(), () -> AppleSettings.dungeonMap,
                value -> AppleSettings.dungeonMap = value,
                () -> mc.displayGuiScreen(HudEditorScreen.openDungeonMap(this)));
        simpleFeature(rows, "Sync Details", assistDescription("Sync Details"),
                () -> AssistSettings.dungeonSyncDebug, value -> AssistSettings.dungeonSyncDebug = value);
        return rows;
    }

    private List<Row> fishingPage() {
        List<Row> rows = new ArrayList<Row>();
        GuiSettingsAuditModel.Feature fishingHud = GuiSettingsAuditModel.find("fishing");
        GuiSettingsAuditModel.Feature seaHpHud = GuiSettingsAuditModel.find("seahp");
        rows.add(Row.section("Simple features"));
        simpleFeature(rows, "Lord Jawbus", "Shows a title once when Lord Jawbus arrives.",
                () -> AppleSettings.fishingJawbusAlert, value -> AppleSettings.fishingJawbusAlert = value);
        simpleFeature(rows, "Nearby Encounters", "Optional local title when compatible HP nameplates first appear.",
                () -> AppleSettings.fishingNearbyEncounterAlert, value -> AppleSettings.fishingNearbyEncounterAlert = value);

        rows.add(Row.section("Feature editors"));
        editorFeature(rows, "Catch Counter", "Catch counts, Festival best and counter reset.", () -> AppleSettings.fishingCounter,
                value -> AppleSettings.fishingCounter = value,
                () -> Minecraft.getMinecraft().displayGuiScreen(FeatureSettingsEditors.catchCounter(this)));
        editorFeature(rows, fishingHud.getTitle(), fishingHud.getDescription(), () -> HudManager.FISHING.enabled,
                value -> HudManager.FISHING.enabled = value,
                () -> Minecraft.getMinecraft().displayGuiScreen(HudEditorScreen.openElement(this, HudManager.FISHING)));
        editorFeature(rows, seaHpHud.getTitle(), seaHpHud.getDescription(), () -> HudManager.SEA_HP.enabled,
                value -> HudManager.SEA_HP.enabled = value,
                () -> Minecraft.getMinecraft().displayGuiScreen(HudEditorScreen.openElement(this, HudManager.SEA_HP)));
        return rows;
    }

    /** Resolves the centrally tested one-line copy for every existing assist control. */
    private static String assistDescription(String name) {
        AssistUiModel.Entry entry = AssistUiModel.find(name);
        return entry == null ? "Optional Sky VeLor client assist" : entry.description;
    }

    private List<Row> slayerPage() {
        List<Row> rows = new ArrayList<Row>();
        GuiSettingsAuditModel.Feature slayerHud = GuiSettingsAuditModel.find("slayer");
        rows.add(Row.section("Feature editors"));
        editorFeature(rows, "Slayer Tracker", "Drop history, timer, prices and clear-history controls.",
                () -> AppleSettings.slayerTracker, value -> AppleSettings.slayerTracker = value,
                () -> Minecraft.getMinecraft().displayGuiScreen(FeatureSettingsEditors.slayer(this)));
        editorFeature(rows, slayerHud.getTitle(), slayerHud.getDescription(), () -> HudManager.SLAYER.enabled,
                value -> HudManager.SLAYER.enabled = value,
                () -> Minecraft.getMinecraft().displayGuiScreen(HudEditorScreen.openElement(this, HudManager.SLAYER)));
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
                    if (r.kind != Row.ITEM) {
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
        if (showsGeneralMetrics()) {
            GeneralMetricsLayout.update(generalMetrics, vx, vy, vw);
            if (generalMetrics.height + ROW_GAP + 1f <= vh) {
                float settingsTop = Math.min(bodyBottom - 1f, generalMetrics.cards[0].y + generalMetrics.height + ROW_GAP);
                vy = Math.max(vy, settingsTop);
                vh = Math.max(1f, bodyBottom - vy);
            } else {
                // At an extreme GUI scale, clip the optional dashboard rather than overlap editor rows.
                generalMetrics.height = 0f;
            }
        } else {
            generalMetrics.height = 0f;
        }

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

    /** Applies one-level dropdown visibility. A header owns rows only until the next section/header. */
    private void applyGroups(List<Row> rows) {
        Row header = null;
        for (int i = 0, n = rows.size(); i < n; i++) {
            Row row = rows.get(i);
            if (row.kind == Row.HEADER) {
                header = row;
                row.visible = true;
                row.bodyRows = 0;
            } else if (row.kind == Row.SECTION) {
                header = null;
                row.visible = true;
            } else {
                row.visible = header == null || expanded.contains(header.label);
                if (header != null) header.bodyRows++;
            }
        }
    }

    /** Toggles one flat feature dropdown. Closing it also closes an editor-control popup inside it. */
    private void toggleGroup(String title) {
        if (!expanded.remove(title)) expanded.add(title);
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
            rowLayoutRect.set(cx0, y, Math.max(1f, cw), r.height);
            float textPad = 12f;
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

    private boolean showsGeneralMetrics() {
        return !searching && pageIndex == 0;
    }

    /** Top-of-General GUI telemetry: refreshed by ClientMetrics at a bounded 500 ms cadence. */
    private void drawGeneralMetrics(FontRenderer font) {
        ClientMetrics.Snapshot snapshot = clientMetrics.snapshot(System.currentTimeMillis());
        drawMetricCard(font, generalMetrics.cards[GeneralMetricsLayout.FPS], "FPS", snapshot.fpsText(),
                snapshot.fpsColor(), 1f);
        drawMetricCard(font, generalMetrics.cards[GeneralMetricsLayout.PING], "PING", snapshot.pingText(),
                snapshot.pingColor(), 1f);
        drawMetricCard(font, generalMetrics.cards[GeneralMetricsLayout.MEMORY], "MEMORY", snapshot.memoryText(),
                snapshot.memoryColor(), snapshot.memoryRatio());
    }

    private void drawMetricCard(FontRenderer font, GuiLayoutToolkit.Rect card, String title, String value,
                                int color, float barProgress) {
        if (card == null || card.w <= 0f || card.h <= 0f) return;
        UIRender.roundRectBorder(card.x, card.y, card.w, card.h, Math.max(3f, Theme.radius - 5f),
                Theme.card, Theme.card, Theme.border);
        if (generalMetrics.stacked) {
            UIRender.text(font, title, card.x + 7f, card.y + 8f, Theme.textTertiary, .70f, false);
            UIRender.text(font, fit(font, value, Math.max(1f, card.w - 54f)), card.x + 48f, card.y + 7f,
                    color, .78f, false);
        } else {
            UIRender.text(font, title, card.x + 7f, card.y + 7f, Theme.textTertiary, .68f, false);
            UIRender.text(font, fit(font, value, Math.max(1f, card.w - 14f)), card.x + 7f, card.y + 19f,
                    color, .82f, false);
        }
        float barX = card.x + 6f;
        float barW = Math.max(1f, card.w - 12f);
        float barY = card.bottom() - 5f;
        UIRender.roundRect(barX, barY, barW, 2f, 1f, Theme.track, Theme.track, 0f);
        float amount = "MEMORY".equals(title) ? barProgress : 1f;
        UIRender.roundRect(barX, barY, Math.max(1f, barW * Math.max(0f, Math.min(1f, amount))), 2f,
                1f, color, color, 0f);
    }

    private void drawContent(int mx, int my) {
        FontRenderer fr = fr();
        if (generalMetrics.height > 0f) drawGeneralMetrics(fr);
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
            }
        }
        drawScrollIndicator();
        UIRender.scissorEnd();
    }

    /** One-level dropdown header with a small red/green status dot from its exact body toggle. */
    private void drawGroupHeader(FontRenderer fr, Row r, int mx, int my) {
        float x = vx;
        float y = r.y;
        float w = vw;
        float h = r.height;
        boolean open = expanded.contains(r.label);
        boolean hover = inside(mx, my, x, y, w, h);
        UIRender.roundRectBorder(x, y, w, h, cardRadius(), Theme.card, Theme.card,
                hover ? UIRender.mix(Theme.border, Theme.accent, .45f) : Theme.border);
        int ink = hover ? Theme.accent : Theme.textSecondary;
        float cx = x + 14f;
        float cy = y + h * .5f;
        if (open) {
            UIRender.triangle(cx - 4f, cy - 2f, cx + 4f, cy - 2f, cx, cy + 3f, ink);
        } else {
            UIRender.triangle(cx - 2f, cy - 4f, cx - 2f, cy + 4f, cx + 3f, cy, ink);
        }
        boolean enabled = r.status != null && Boolean.TRUE.equals(r.status.get());
        int dot = enabled ? FeatureSettingsUiModel.STATUS_DOT_ON : FeatureSettingsUiModel.STATUS_DOT_OFF;
        float dotX = x + w - 15f;
        UIRender.circle(dotX, cy, 4f, dot);
        float titleX = x + 26f;
        float titleW = Math.max(1f, dotX - titleX - 10f);
        UIRender.text(fr, fit(fr, r.label, titleW), titleX, y + 7f, Theme.textPrimary, 1f, false);
        if (r.desc != null && r.desc.length() > 0) {
            UIRender.text(fr, fit(fr, r.desc, titleW), titleX, y + 22f, Theme.textSecondary, .88f, false);
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
            UIRender.text(fr, label, labelX, y + (h - 9f) * .5f + 1f, Theme.textPrimary, 1f, false);
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
                toggleGroup(header.label);
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
