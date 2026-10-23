package com.arena.pekaclient.gui;

import com.arena.pekaclient.PekaVersion;
import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.config.AssistSettings;
import com.arena.pekaclient.hud.ClientMetrics;
import com.arena.pekaclient.hud.HudElement;
import com.arena.pekaclient.hud.HudManager;
import com.arena.pekaclient.render.Anim;
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
 * The Sky VeLor settings hub.
 *
 * <p>This intentionally uses the compact expandable-row interaction from the older Hub rather than a sidebar,
 * segmented tabs, or feature trees. Each top-level category owns direct controls and clearly named editor routes;
 * clicking its header expands or collapses only that category. The result remains one scrollable page at every GUI
 * scale, while every focused editor keeps ownership of its existing configuration keys.</p>
 *
 * <p>Window buttons: red closes the hub, yellow collapses it to the title bar, and green zooms it to the full screen.
 * Mouse wheel, touch swipe, and the scroll keys move the single list. A swipe never activates a control. Ctrl+F
 * focuses search; search temporarily shows matching rows from collapsed categories without changing their state.</p>
 */
public class AppleHubScreen extends AppleScreen {

    private static final float TITLE_H = 34f;
    private static final float PAD = 14f;
    private static final float ROW_GAP = 6f;

    static final String QUALITY_OF_LIFE_SECTION = QualityOfLifeUiModel.SECTION_LABEL;
    static final String HUD_SECTION = "HUD Hub Editor";
    static final String GENERAL_SECTION = "General";
    static final String FARMING_SECTION = "Farming";
    static final String DUNGEON_SECTION = "Dungeon";
    static final String SLAYER_SECTION = "Slayer";
    static final String FISHING_SECTION = "Fishing";

    /** Display order in the single expandable menu. This is not a tab/navigation list. */
    static final String[] SECTION_NAMES = {
            QUALITY_OF_LIFE_SECTION, HUD_SECTION, GENERAL_SECTION, FARMING_SECTION,
            DUNGEON_SECTION, SLAYER_SECTION, FISHING_SECTION
    };

    private final GuiScreen parent;
    private final List<Row> menuRows = new ArrayList<Row>();

    private final SearchField search = new SearchField("Search settings");
    private final List<Row> searchRows = new ArrayList<Row>();
    private int searchVersion = -1;
    private boolean searching;
    private String query = "";

    /** Open historical-style category headers for this Hub session only. */
    private final Set<String> expanded = new HashSet<String>();
    /** Header under the pointer at press time. Toggles on release only if the pointer did not drag. */
    private Row pendingHeader;

    // Window state. Collapsed hides everything but the title bar. Zoomed fills the screen.
    private boolean collapsed;
    private boolean zoomed;

    private final Anim openAnim = new Anim(0f);
    private final Anim fadeAnim = new Anim(1f);
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
    private boolean tall;

    // Timing and GUI-only adaptive quality.
    private long lastNs;
    private int slowFrames;
    private int fastFrames;
    private boolean autoLow;
    /** Read-only GUI telemetry retained inside the General category; it is not another HUD element. */
    private final ClientMetrics.Reader clientMetrics = ClientMetrics.liveReader();
    private final GeneralMetricsLayout.Layout generalMetrics = new GeneralMetricsLayout.Layout();
    private int scaleKey = -1;
    private float sf = 2f;

    public AppleHubScreen(GuiScreen parent) {
        this.parent = parent;
        Theme.refresh();
        openAnim.to(1f);
        buildMenu();
        // Quality of Life is the first, immediately useful section on a fresh opening. All other sections remain
        // one click away without consuming vertical space or introducing another navigation surface.
        expanded.add(defaultExpandedSection());
    }

    /** The initial open category is deliberate, but expansion itself remains session-only presentation state. */
    static String defaultExpandedSection() {
        return QUALITY_OF_LIFE_SECTION;
    }

    /** A clone keeps headless reachability checks from mutating the screen's static display contract. */
    static String[] sectionNames() {
        return SECTION_NAMES.clone();
    }

    /** The Hub intentionally has one category-expansion level and no main sidebar/tab navigation. */
    static int mainExpansionDepth() {
        return 1;
    }

    // ------------------------------------------------------------------ menu construction

    private void buildMenu() {
        menuRows.clear();
        appendGroup(QUALITY_OF_LIFE_SECTION, qualityOfLifeRows());
        appendGroup(HUD_SECTION, hudRows());
        appendGroup(GENERAL_SECTION, generalRows());
        appendGroup(FARMING_SECTION, farmingRows());
        appendGroup(DUNGEON_SECTION, dungeonRows());
        appendGroup(SLAYER_SECTION, slayerRows());
        appendGroup(FISHING_SECTION, fishingRows());
    }

    /**
     * Restores the old simple category grouping rule: one header followed by direct rows until the next header.
     * Page-era visual section labels are deliberately omitted so a category cannot acquire nested navigation.
     */
    private void appendGroup(String title, List<Row> rows) {
        menuRows.add(Row.header(title));
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            if (row.kind != Row.SECTION) menuRows.add(row);
        }
    }

    /** Adds one direct Boolean control. It never opens a second dropdown. */
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

    /** Opens a focused existing settings owner; it does not create a replacement config surface. */
    private static void settingsFeature(List<Row> rows, String title, String description, Runnable open) {
        rows.add(Row.item(title, description, new AppleButton("Open", 102f, 24f, true, open)));
    }

    /** Keeps a HUD element's visibility switch and its single established styling/placement route together. */
    private void hudFeature(List<Row> rows, final HudElement element) {
        GuiSettingsAuditModel.Feature feature = GuiSettingsAuditModel.find(element.id);
        final String title = feature == null ? element.name : feature.getTitle();
        final String description = feature == null ? "Show this HUD element on screen." : feature.getDescription();
        simpleFeature(rows, "Show " + title, description, () -> element.enabled, value -> element.enabled = value);
        settingsFeature(rows, title + " Settings", "Position, scale and style remain in the HUD editor.",
                () -> Minecraft.getMinecraft().displayGuiScreen(HudEditorScreen.openElement(this, element)));
    }

    /** Main expandable Quality of Life section: direct daily controls and the retained focused tools. */
    private List<Row> qualityOfLifeRows() {
        List<Row> rows = new ArrayList<Row>();
        List<QualityOfLifeUiModel.Entry> controls = QualityOfLifeUiModel.buildControlModel();
        simpleFeature(rows, "No Block Particles", controls.get(0).getDescription(),
                () -> AppleSettings.noBlockParticles, value -> AppleSettings.noBlockParticles = value);
        simpleFeature(rows, "No Hotbar Scrolling", controls.get(1).getDescription(),
                () -> AppleSettings.disableHotbarScrolling, value -> AppleSettings.disableHotbarScrolling = value);
        percentSliderFeature(rows, "Hurt Camera Shake", controls.get(2).getDescription(),
                () -> AssistSettings.hurtCameraShake, value -> AssistSettings.hurtCameraShake = value);
        simpleFeature(rows, "Auto Sprint", controls.get(3).getDescription(),
                () -> AssistSettings.autoSprint, value -> AssistSettings.autoSprint = value);

        // Item Animation belongs with the player quality-of-life tools, not behind a separate HUD category. Its editor remains the
        // original non-draggable renderer panel and owns its enable switch, transforms, preview, and persistence.
        List<ItemAnimationUiModel.Entry> itemAnimation = ItemAnimationUiModel.buildQualityOfLifeModel();
        settingsFeature(rows, itemAnimation.get(0).getLabel(), "Open the dedicated first-person renderer editor.",
                () -> Minecraft.getMinecraft().displayGuiScreen(HudEditorScreen.openItemAnimation(this)));
        settingsFeature(rows, "Inventory Assist", "Tooltip, potion-panel and inventory visual helpers.",
                () -> Minecraft.getMinecraft().displayGuiScreen(FeatureSettingsEditors.inventoryAssist(this)));
        settingsFeature(rows, "Movement and Shortcuts", "Wardrobe and pets shortcuts.",
                () -> Minecraft.getMinecraft().displayGuiScreen(FeatureSettingsEditors.movementShortcuts(this)));
        settingsFeature(rows, "Item Lock and Swap", "Pair locks, indicators and persistent pair-center lines.",
                () -> Minecraft.getMinecraft().displayGuiScreen(FeatureSettingsEditors.itemLockSwap(this)));
        settingsFeature(rows, "Chat QoL", "Attached tabs, message animation and highlighting.",
                () -> Minecraft.getMinecraft().displayGuiScreen(new ChatEditorScreen(this)));
        settingsFeature(rows, "Commands", "Optional /sv and /qn routes plus diagnostics.",
                () -> Minecraft.getMinecraft().displayGuiScreen(FeatureSettingsEditors.commands(this)));
        return rows;
    }

    private List<Row> generalRows() {
        List<Row> rows = new ArrayList<Row>();
        // This preserves the existing read-only GUI telemetry without registering a second in-game metric HUD.
        rows.add(Row.custom(85f, (x, y, width, height) -> drawGeneralMetrics(fr(), x, y, width, height)));
        settingsFeature(rows, "Appearance", "Dark/light theme, accent, corners and panel opacity.",
                () -> Minecraft.getMinecraft().displayGuiScreen(FeatureSettingsEditors.appearance(this)));
        settingsFeature(rows, "Mod Settings", "Global scroll speed and smoothing; Shift + wheel is fine scrolling.",
                () -> Minecraft.getMinecraft().displayGuiScreen(FeatureSettingsEditors.modSettings(this)));
        settingsFeature(rows, "Performance", "Curve, shadows, animation and adaptive GUI quality.",
                () -> Minecraft.getMinecraft().displayGuiScreen(FeatureSettingsEditors.performance(this)));
        return rows;
    }

    /**
     * Retained compatibility entry point. The old HUD subcategory is now the normal single-menu Dungeon section.
     */
    public static AppleHubScreen dungeonHud(GuiScreen parent) {
        AppleHubScreen hub = new AppleHubScreen(parent);
        hub.expanded.clear();
        hub.expanded.add(DUNGEON_SECTION);
        return hub;
    }

    /** HUD Hub Editor section: one clear editor entry, plus direct visibility/settings access for the reference HUDs. */
    private List<Row> hudRows() {
        List<Row> rows = new ArrayList<Row>();
        settingsFeature(rows, "HUD Editor", "Click an on-screen HUD element to select it; drag to move it.",
                () -> Minecraft.getMinecraft().displayGuiScreen(new HudEditorScreen(this)));
        hudFeature(rows, HudManager.STATS);
        hudFeature(rows, HudManager.DOLL);
        hudFeature(rows, HudManager.INVENTORY);
        return rows;
    }

    private List<Row> farmingRows() {
        List<Row> rows = new ArrayList<Row>();
        hudFeature(rows, HudManager.MINING);
        settingsFeature(rows, "Mining Assistant", "Alerts, helper controls, and its HUD route.",
                () -> Minecraft.getMinecraft().displayGuiScreen(FeatureSettingsEditors.miningAssistant(this)));
        return rows;
    }

    private List<Row> dungeonRows() {
        final Minecraft mc = Minecraft.getMinecraft();
        List<Row> rows = new ArrayList<Row>();
        settingsFeature(rows, "Door ESP", "Wither and blood door filters, shared style and closed-prism preview.",
                () -> mc.displayGuiScreen(DungeonEspEditorSpecs.doorEditor(this)));
        settingsFeature(rows, "Mob ESP", "Per-mob styles, readable category labels, scan controls and presets.",
                () -> mc.displayGuiScreen(DungeonEspEditorSpecs.mobEditor(this)));
        simpleFeature(rows, "Dungeon Sync", assistDescription("Dungeon Sync"),
                () -> AssistSettings.dungeonSync, value -> AssistSettings.dungeonSync = value);
        settingsFeature(rows, "Floor VII (M7) Overlay", "Areas, alerts, terminal solver, boss and dragon hitboxes.",
                () -> mc.displayGuiScreen(FeatureSettingsEditors.m7Overlay(this)));
        settingsFeature(rows, "Spirit Leap GUI", "Dungeon-only teammate picker, shortcut and safe preview.",
                () -> mc.displayGuiScreen(FeatureSettingsEditors.spiritLeap(this)));
        settingsFeature(rows, "Terminal Assist", "Visual helpers, explicit automation and waypoints.",
                () -> mc.displayGuiScreen(FeatureSettingsEditors.terminalAssist(this)));
        hudFeature(rows, HudManager.DUNGEON_BOSS_HP);
        simpleFeature(rows, "Show Dungeon Map", "Show the existing dungeon map overlay.",
                () -> AppleSettings.dungeonMap, value -> AppleSettings.dungeonMap = value);
        settingsFeature(rows, "Dungeon Map Settings", "Position and map options remain in the HUD editor.",
                () -> mc.displayGuiScreen(HudEditorScreen.openDungeonMap(this)));
        simpleFeature(rows, "Sync Details", assistDescription("Sync Details"),
                () -> AssistSettings.dungeonSyncDebug, value -> AssistSettings.dungeonSyncDebug = value);
        return rows;
    }

    private List<Row> slayerRows() {
        List<Row> rows = new ArrayList<Row>();
        settingsFeature(rows, "Slayer Tracker", "Drop history, timer, prices and clear-history controls.",
                () -> Minecraft.getMinecraft().displayGuiScreen(FeatureSettingsEditors.slayer(this)));
        hudFeature(rows, HudManager.SLAYER);
        return rows;
    }

    private List<Row> fishingRows() {
        List<Row> rows = new ArrayList<Row>();
        simpleFeature(rows, "Lord Jawbus", "Shows a title once when Lord Jawbus arrives.",
                () -> AppleSettings.fishingJawbusAlert, value -> AppleSettings.fishingJawbusAlert = value);
        simpleFeature(rows, "Nearby Encounters", "Optional local title when compatible HP nameplates first appear.",
                () -> AppleSettings.fishingNearbyEncounterAlert, value -> AppleSettings.fishingNearbyEncounterAlert = value);
        settingsFeature(rows, "Catch Counter", "Catch counts, Festival best and counter reset.",
                () -> Minecraft.getMinecraft().displayGuiScreen(FeatureSettingsEditors.catchCounter(this)));
        hudFeature(rows, HudManager.FISHING);
        hudFeature(rows, HudManager.SEA_HP);
        return rows;
    }

    /** Resolves the centrally tested one-line copy for every existing assist control. */
    private static String assistDescription(String name) {
        AssistUiModel.Entry entry = AssistUiModel.find(name);
        return entry == null ? "Optional Sky VeLor client assist." : entry.description;
    }

    private List<Row> currentRows() {
        return searching ? searchRows : menuRows;
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
            if (slowFrames > 30) autoLow = true;
        } else if (dt < 0.03f) {
            fastFrames++;
            slowFrames = 0;
            if (fastFrames > 180) autoLow = false;
        }
    }

    private void syncSearch() {
        if (search.version() == searchVersion) return;
        searchVersion = search.version();
        query = search.text().toLowerCase(Locale.ROOT).trim();
        searchRows.clear();
        if (!query.isEmpty()) {
            for (int i = 0; i < menuRows.size(); i++) {
                Row row = menuRows.get(i);
                if (row.kind != Row.ITEM) continue;
                if (row.label.toLowerCase(Locale.ROOT).contains(query)
                        || (row.desc != null && row.desc.toLowerCase(Locale.ROOT).contains(query))) {
                    searchRows.add(row);
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

        scroll.update(dt, gestureActive && gesture.kind == Gesture.SCROLL,
                Theme.animations && AppleSettings.smoothScrolling, Theme.speed);
        indicator.to((gestureActive || scroll.moving()) ? 1f : 0f);

        List<Row> rows = currentRows();
        for (int i = 0, n = rows.size(); i < n; i++) {
            Widget control = rows.get(i).control;
            if (control != null) control.update(dt);
        }
    }

    // ------------------------------------------------------------------ layout

    private void computeLayout() {
        float availableW = Math.max(1f, width - 8f);
        float availableH = Math.max(1f, height - 8f);
        float desiredW = zoomed ? availableW : Math.min(availableW, 620f);
        float desiredH = zoomed ? availableH : Math.min(availableH, 400f);
        // A preferred minimum never exceeds the actual viewport; this keeps tiny GUI scales reachable.
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
        tall = wh >= 230f;

        cx0 = wx + PAD;
        cw = Math.max(1f, ww - PAD * 2f);
        headY = wy + TITLE_H + 12f;

        if (tall) {
            search.x = cx0;
            search.y = headY + 36f;
            search.w = cw;
            search.h = 22f;
        } else {
            float searchWidth = Math.min(170f, cw * 0.5f);
            search.x = cx0 + cw - searchWidth;
            search.y = headY - 3f;
            search.w = searchWidth;
            search.h = 20f;
        }

        vx = cx0;
        vw = cw;
        float bodyBottom = wy + wh - 12f;
        float candidateViewY = tall ? headY + 68f : headY + 26f;
        vy = Math.min(candidateViewY, Math.max(wy + TITLE_H + 2f, bodyBottom - 20f));
        vh = Math.max(1f, bodyBottom - vy);

        if (!searching) applyGroups(menuRows);
        List<Row> rows = currentRows();
        scroll.setMax(Math.max(0f, contentHeight(rows) + 4f - vh));
        layoutRows(rows, vy - scroll.value());
    }

    /** Total height of a row list, without the top gap. Does not move the rows. */
    private float contentHeight(List<Row> rows) {
        float total = 0f;
        boolean first = true;
        for (int i = 0, n = rows.size(); i < n; i++) {
            Row row = rows.get(i);
            if (!shown(row)) continue;
            total += row.height + (first ? 0f : ROW_GAP);
            first = false;
        }
        return Math.max(0f, total);
    }

    /** Search shows every matching direct row, including one stored in a collapsed category. */
    private boolean shown(Row row) {
        return searching || row.visible;
    }

    /** Restored historical grouping: a header owns all following direct rows until another header or section. */
    private void applyGroups(List<Row> rows) {
        applyGroupVisibility(rows, expanded);
    }

    /**
     * Shared pure grouping rule used by the live menu and headless expansion regression tests. There is no recursive
     * parent lookup: all rows are direct children of the most recent category header, so nested navigation cannot
     * accidentally return.
     */
    static void applyGroupVisibility(List<Row> rows, Set<String> expandedGroups) {
        if (rows == null) return;
        Row header = null;
        for (int i = 0, n = rows.size(); i < n; i++) {
            Row row = rows.get(i);
            if (row.kind == Row.HEADER) {
                header = row;
                row.visible = true;
                row.children = 0;
            } else if (row.kind == Row.SECTION) {
                header = null;
                row.visible = true;
            } else {
                row.group = header == null ? null : header.label;
                row.visible = header == null || (expandedGroups != null && expandedGroups.contains(header.label));
                if (header != null) header.children++;
            }
        }
    }

    /** Closing a category also closes an editor-control popup inside it. */
    private void toggleGroup(String title) {
        if (!expanded.remove(title)) expanded.add(title);
        openDropdown = null;
    }

    private void layoutRows(List<Row> rows, float top) {
        float y = top;
        for (int i = 0, n = rows.size(); i < n; i++) {
            Row row = rows.get(i);
            if (!shown(row)) continue;
            row.y = y;
            Widget control = row.control;
            rowLayoutRect.set(cx0, y, Math.max(1f, cw), row.height);
            float textPad = 12f;
            if (control != null) {
                GuiLayoutToolkit.splitRow(rowLayoutRect, textPad, 8f, control.preferredWidth(), control.h,
                        rowLabelRect, rowControlRect);
                row.labelX = rowLabelRect.x;
                row.labelWidth = rowLabelRect.w;
                control.x = rowControlRect.x;
                control.y = rowControlRect.y;
                control.w = rowControlRect.w;
            } else {
                row.labelX = rowLayoutRect.x + textPad;
                row.labelWidth = Math.max(1f, rowLayoutRect.w - textPad * 2f);
            }
            y = nextRowTop(y, row.height);
        }
    }

    /** Shared row-flow invariant used by the real layout path and headless overlap regression tests. */
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

        FontRenderer font = fr();
        String title = searching ? "Search" : PekaVersion.DISPLAY_NAME;
        UIRender.text(font, title, wx + (ww - font.getStringWidth(title)) * 0.5f, wy + 12.5f,
                Theme.textSecondary, 1f, false);

        String version = PekaVersion.NAME;
        float versionWidth = font.getStringWidth(version) * 0.8f;
        UIRender.text(font, version, wx + ww - 12f - versionWidth, wy + 13.5f, Theme.textTertiary, 0.8f, false);
    }

    private void drawHeader() {
        FontRenderer font = fr();
        String title = searching ? "Search" : "Settings";
        String subtitle = searching ? searchRows.size() + (searchRows.size() == 1 ? " match" : " matches")
                : "Expandable categories and focused editors";
        UIRender.text(font, title, cx0, headY, Theme.textPrimary, 1.5f, false);
        if (tall) UIRender.text(font, subtitle, cx0, headY + 19f, Theme.textSecondary, 1f, false);
    }

    /**
     * Existing General-only readout. It is intentionally inside the General expandable category and never a
     * HudElement, so it cannot duplicate the restored Frame Rate / Mining / Memory in-game display.
     */
    private void drawGeneralMetrics(FontRenderer font, float x, float y, float width, float rowHeight) {
        GeneralMetricsLayout.update(generalMetrics, x, y, width);
        float cardY = y + Math.max(0f, (rowHeight - generalMetrics.height) * 0.5f);
        if (cardY != y) GeneralMetricsLayout.update(generalMetrics, x, cardY, width);
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
        FontRenderer font = fr();
        List<Row> rows = currentRows();
        UIRender.scissorBegin(vx, vy, vw, vh, sf);

        if (searching && rows.isEmpty()) {
            String none = "No matching settings";
            UIRender.text(font, none, vx + (vw - font.getStringWidth(none)) * 0.5f, vy + 16f,
                    Theme.textTertiary, 1f, false);
        }

        float top = vy;
        float bottom = vy + vh;
        for (int i = 0, n = rows.size(); i < n; i++) {
            Row row = rows.get(i);
            if (!shown(row) || row.y + row.height < top || row.y > bottom) continue;
            GuiDebugOverlay.recordRow("Hub: " + row.label, vx, row.y, vw, row.height);
            if (row.kind == Row.SECTION) {
                UIRender.text(font, row.label, vx + 2f, row.y + 4f, Theme.textTertiary, 0.85f, false);
            } else if (row.kind == Row.HEADER) {
                drawGroupHeader(font, row, mx, my);
            } else if (row.kind == Row.ITEM) {
                drawItem(font, row, mx, my);
            } else if (row.painter != null) {
                row.painter.paint(vx, row.y, vw, row.height);
            }
        }
        drawScrollIndicator();
        UIRender.scissorEnd();
    }

    /** Restored header presentation: chevron, title, and direct-row count; no status dot or nested feature menu. */
    private void drawGroupHeader(FontRenderer font, Row row, int mx, int my) {
        float x = vx;
        float y = row.y;
        float w = vw;
        float h = row.height;
        boolean open = expanded.contains(row.label);
        boolean hover = inside(mx, my, x, y, w, h);
        UIRender.roundRectBorder(x, y, w, h, cardRadius(), Theme.card, Theme.card,
                hover ? UIRender.mix(Theme.border, Theme.accent, .45f) : Theme.border);
        int ink = hover ? Theme.accent : Theme.textSecondary;
        float cx = x + 14f;
        float cy = y + h * 0.5f;
        if (open) {
            UIRender.triangle(cx - 4f, cy - 2f, cx + 4f, cy - 2f, cx, cy + 3f, ink);
        } else {
            UIRender.triangle(cx - 2f, cy - 4f, cx - 2f, cy + 4f, cx + 3f, cy, ink);
        }
        float textY = y + (h - 9f) * 0.5f + 1f;
        String count = row.children + (row.children == 1 ? " setting" : " settings");
        float countWidth = font.getStringWidth(count) * 0.9f;
        float titleWidth = Math.max(0f, w - 26f - 12f - countWidth - 8f);
        // At tiny widths, the title is more useful than a clipped count. Never let the two collide.
        boolean showCount = titleWidth >= Math.min(40f, font.getStringWidth(row.label));
        if (!showCount) titleWidth = Math.max(0f, w - 26f - 12f);
        UIRender.text(font, fit(font, row.label, titleWidth), x + 26f, textY, Theme.textPrimary, 1.05f, false);
        if (showCount) {
            UIRender.text(font, count, x + w - 12f - countWidth, textY, Theme.textTertiary, 0.9f, false);
        }
    }

    private void drawItem(FontRenderer font, Row row, int mx, int my) {
        float x = vx;
        float y = row.y;
        float w = vw;
        float h = row.height;
        UIRender.roundRectBorder(x, y, w, h, cardRadius(), Theme.card, Theme.card, Theme.border);

        float labelX = row.labelX > 0f ? row.labelX : x + 12f;
        float maxWidth = row.labelWidth > 0f ? row.labelWidth : Math.max(1f, w - 24f);
        String label = fit(font, row.label, maxWidth);
        if (row.desc != null) {
            UIRender.text(font, label, labelX, y + 7f, Theme.textPrimary, 1f, false);
            UIRender.text(font, fit(font, row.desc, maxWidth), labelX, y + 21f, Theme.textSecondary, 1f, false);
        } else if (!label.isEmpty()) {
            UIRender.text(font, label, labelX, y + (h - 9f) * 0.5f + 1f, Theme.textPrimary, 1f, false);
        }
        if (row.control != null) row.control.draw(mx, my);
    }

    private void drawScrollIndicator() {
        float amount = indicator.get();
        float maxScroll = scroll.max();
        if (amount <= 0.01f || maxScroll <= 0f) return;
        float track = vh - 8f;
        float thumb = Math.max(24f, track * vh / (contentHeight(currentRows()) + 4f));
        float y = vy + 4f + (track - thumb) * (scroll.value() / maxScroll);
        float previous = UIRender.alphaMul;
        UIRender.alphaMul = previous * amount;
        UIRender.roundRect(vx + vw - 3f, y, 3f, thumb, 1.5f, Theme.textSecondary, Theme.textSecondary, 0.5f);
        UIRender.alphaMul = previous;
    }

    private float cardRadius() {
        return Math.max(4f, Theme.radius - 4f);
    }

    // ------------------------------------------------------------------ input

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        if (button != 0) return;
        if (handleOpenDropdownClick(mx, my)) return;
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
        if (collapsed) return;
        if (search.contains(mx, my)) {
            search.setFocused(true);
            return;
        }
        search.setFocused(false);
        if (!inside(mx, my, vx, vy, vw, vh)) return;

        Row header = headerAt(mx, my);
        if (header != null) {
            pendingHeader = header;
            active = null;
            sliderEngaged = false;
            gestureActive = true;
            gesture.begin(null, mx, my, scroll.value());
            return;
        }
        Widget widget = widgetAt(mx, my);
        active = widget;
        sliderEngaged = false;
        gestureActive = true;
        gesture.begin(widget, mx, my, scroll.value());
        if (widget != null && !(widget instanceof AppleSlider)) widget.onPress(mx, my);
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long timeSinceLastClick) {
        if (!gestureActive) return;
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
            Widget current = active;
            active = null;
            current.onRelease(mx, my, activate);
            if (activate && current instanceof AppleDropdown) {
                AppleDropdown dropdown = (AppleDropdown) current;
                openDropdown = (openDropdown == dropdown) ? null : dropdown;
                if (openDropdown != null) openDropdown.onExpand();
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
        if (collapsed && code != Keyboard.KEY_ESCAPE) return;
        if (search.isFocused()) {
            if (code == Keyboard.KEY_ESCAPE) {
                if (!search.isEmpty()) search.clear(); else search.setFocused(false);
                return;
            }
            if (search.keyTyped(c, code)) return;
        }
        if (code == Keyboard.KEY_ESCAPE) {
            if (openDropdown != null) {
                openDropdown = null;
                return;
            }
            close();
            return;
        }
        if (isCtrlKeyDown() && code == Keyboard.KEY_F) {
            search.setFocused(true);
            return;
        }
        if (handleNavigationKey(code)) return;
        if (c >= 32 && c != 127) {
            search.setFocused(true);
            search.keyTyped(c, code);
        }
    }

    /** Keyboard navigation scrolls the one menu; there are deliberately no left/right category tabs. */
    private boolean handleNavigationKey(int code) {
        float page = Math.max(40f, vh * 0.8f);
        switch (code) {
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

    /** The category header under the pointer inside the visible list, or null. */
    private Row headerAt(int mx, int my) {
        if (searching || !inside(mx, my, vx, vy, vw, vh)) return null;
        for (int i = 0, n = menuRows.size(); i < n; i++) {
            Row row = menuRows.get(i);
            if (row.kind == Row.HEADER && my >= row.y && my < row.y + row.height
                    && mx >= vx && mx < vx + vw) return row;
        }
        return null;
    }

    private Widget widgetAt(int mx, int my) {
        List<Row> rows = currentRows();
        for (int i = rows.size() - 1; i >= 0; i--) {
            Row row = rows.get(i);
            Widget control = row.control;
            if (control != null && shown(row) && control.contains(mx, my)) return control;
        }
        return null;
    }
}
