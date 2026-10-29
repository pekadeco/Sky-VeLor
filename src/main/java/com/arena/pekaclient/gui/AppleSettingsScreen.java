package com.arena.pekaclient.gui;

import com.arena.pekaclient.PekaClientMod;
import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.config.AssistSettings;
import com.arena.pekaclient.config.ChatQolSettings;
import com.arena.pekaclient.config.DungeonEspSettings;
import com.arena.pekaclient.config.FeatureUiPreferences;
import com.arena.pekaclient.dungeon.M7Settings;
import com.arena.pekaclient.chat.ChatMenuPolicy;
import com.arena.pekaclient.feature.ChatChannelTabs;
import com.arena.pekaclient.feature.ChatTabs;
import com.arena.pekaclient.hud.ClientMetrics;
import com.arena.pekaclient.hud.HudElement;
import com.arena.pekaclient.hud.HudManager;
import com.arena.pekaclient.itemlockswap.ItemLockSwapSettings;
import com.arena.pekaclient.render.GuiMotion;
import com.arena.pekaclient.render.GuiStyle;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import com.arena.pekaclient.widget.AppleButton;
import com.arena.pekaclient.widget.AppleDropdown;
import com.arena.pekaclient.widget.AppleKeybindButton;
import com.arena.pekaclient.widget.AppleSlider;
import com.arena.pekaclient.widget.AppleToggle;
import com.arena.pekaclient.widget.SearchField;
import com.arena.pekaclient.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Native Minecraft 1.8.9 restoration of Sky VeLor's Apple-style settings shell.
 *
 * <p>The shipped Beta-0.10.8.9.5.5v window is the primary visual reference and the supplied Apple GUI project is a
 * secondary primitive/motion reference. This screen keeps the current category rail, global search, flat inline
 * disclosures, Home live performance panel, and one clipped scroll pane while every control reads and writes the
 * existing Sky VeLor configuration owners. It introduces no web dependency or second persistence schema.</p>
 */
public class AppleSettingsScreen extends AppleScreen {

    /** Visible labels only; routes and catalog enum identities remain intentionally stable. */
    static final String[] DESTINATION_NAMES = {"Home", "QoL", "Slayer", "Dungeons", "Farming", "Mining", "Fishing", "HUD & Visuals", "Misc"};
    private static final float CHROME_H = SettingsShellLayout.HEADER_HEIGHT;
    /** The title lives in the header; the compact second strip contains only global search. */
    private static final float TOOLBAR_H = 34f;
    /** Restored Apple rail: broad enough for calm icon-plus-label navigation without crowding content. */
    private static final float RAIL_W = 148f;
    /** Shared page rhythm: cards/rows use 8 px gaps and 12 px internal padding at every GUI scale. */
    private static final float GAP = GuiStyle.SPACE_8;
    private static final float CARD_GAP = GuiStyle.SPACE_8;
    private static final float CARD_PAD = GuiStyle.SPACE_12;
    private static final float ROW_PAD_X = GuiStyle.SPACE_12;
    private static final float ROW_PAD_Y = GuiStyle.SPACE_8;
    private static final float CONTROL_GAP = GuiStyle.SPACE_8;
    /** Named editor routes and inline actions share one trailing action column. */
    private static final float EDITOR_BUTTON_TEXT_SCALE = .62f;
    private static final float ACTION_BUTTON_W = SettingsControlLayout.ACTION_BUTTON_WIDTH;
    private static final float ACTION_BUTTON_H = SettingsControlLayout.CONTROL_HEIGHT;
    /** Key labels stay wide enough for NONE and common names such as RSHIFT without truncation. */
    private static final float KEYBIND_BUTTON_W = 82f;
    /** Reserved under every visible key capsule so a persistent red conflict warning never overlaps row copy. */
    private static final float KEYBIND_WARNING_H = 11f;
    private static final float KEYBIND_WARNING_SCALE = .56f;
    /** Every row reserves these fixed right-side columns, even when they are intentionally blank. */
    private static final float CHEVRON_COLUMN_W = SettingsControlLayout.CHEVRON_COLUMN_WIDTH;
    private static final float TOGGLE_COLUMN_W = SettingsControlLayout.TOGGLE_COLUMN_WIDTH;
    private static final float RIGHT_COLUMN_GAP = SettingsControlLayout.COLUMN_GAP;
    private static final float RIGHT_COLUMNS_W = CHEVRON_COLUMN_W + RIGHT_COLUMN_GAP + TOGGLE_COLUMN_W;
    private static final float EXPANDED_BOTTOM_PADDING = 8f;
    private static final float WHEEL_STEP = 84f;
    private static final String[] HOME_QUICK_IDS = {"auto-sprint", "terminal-hints", "chat-qol", "item-animation"};
    private static final String[] HOME_QUICK_LABELS = {"Sprint", "Terminal", "Chat", "Hand"};
    private static final String[] HOME_QUICK_DETAILS = {"Only while forward is held", "Visual helper status", "Attached filter tabs", "First-person animation"};

    private static final int HIT_NAV = 1;
    private static final int HIT_SEARCH_CLEAR = 2;
    private static final int HIT_CONTROL = 3;
    private static final int HIT_HOME_TOGGLE = 6;
    private static final int HIT_HOME_GAMEPLAY = 7;
    private static final int HIT_WINDOW_FULLSCREEN = 8;
    private static final int HIT_WINDOW_CLOSE = 9;
    private static final int HIT_DISCLOSURE = 10;
    private static final int HIT_SEARCH_RESULT = 11;
    private static final int HIT_ACTION = 12;
    /** Derived RGB mask for formatting existing Chat colour values without introducing a render colour token. */
    private static final int RGB_MASK = (1 << 24) - 1;

    private final GuiScreen parent;
    private final SearchField search = new SearchField("Search settings...");
    /** Read-only settings-surface telemetry; this is not a HudElement or a second metrics renderer. */
    private final ClientMetrics.Reader clientMetrics = ClientMetrics.liveReader();
    /** Fixed ring history belongs only to this open settings screen; it never becomes an in-game HUD sampler. */
    private final HomeMetricHistory homeMetricHistory = new HomeMetricHistory();
    private final HomeMetricsLayout.Layout homeMetricsLayout = new HomeMetricsLayout.Layout();
    private final GuiLayoutToolkit.Scroll scroll = new GuiLayoutToolkit.Scroll();
    private final Gesture gesture = new Gesture();
    private final Map<String, Widget> controls = new HashMap<String, Widget>();
    /** A keybind with an explicit master switch owns a second standard toggle, keyed separately from its key button. */
    private final Map<String, AppleToggle> keybindToggles = new HashMap<String, AppleToggle>();
    private final Map<String, Hit> knownHits = new HashMap<String, Hit>();
    private final List<Hit> visibleHits = new ArrayList<Hit>();
    private final Map<String, TextMeasure> textMeasures = new HashMap<String, TextMeasure>();
    /** Per-dropdown progress gives expanded panels a 150 ms height transition without changing saved state. */
    private final Map<String, Float> dropdownProgress = new HashMap<String, Float>();
    /** Inline text controls (currently Chat colour/custom-tab fields) remain in their owning dropdown. */
    private final Map<String, SearchField> inlineTextControls = new HashMap<String, SearchField>();
    private final GuiLayoutToolkit.Rect panel = new GuiLayoutToolkit.Rect();
    private final GuiLayoutToolkit.Rect viewport = new GuiLayoutToolkit.Rect();
    /** Exactly two 24 px header targets and the title-bar drag span share one reusable geometry owner. */
    private final SettingsShellLayout.Header shellHeader = new SettingsShellLayout.Header();
    /** The rail bounds are reused for both drawing and hit testing; no layout refresh allocates entry rectangles. */
    private final SettingsRailLayout.Rail settingsRail = new SettingsRailLayout.Rail(ReferenceSettingsCatalog.TABS.length);

    private ReferenceSettingsCatalog.Tab activeTab;
    /** Per-tab selection fades are screen-owned and allocated once; no animation survives a closed GUI. */
    private final float[] tabSelection = new float[ReferenceSettingsCatalog.TABS.length];
    private boolean tabSelectionInitialized;
    private boolean compactRail;
    /** Persisted compact/fullscreen shell choice; small-window position stays in-memory for a drag round trip. */
    private boolean fullscreenWindow;
    private float smallWindowOffsetX;
    private float smallWindowOffsetY;
    private boolean narrowRows;
    private float sf = 1f;
    private float toolbarH;
    private float railX;
    private float railY;
    private float railW;
    private float railH;
    private float contentX;
    private float contentY;
    private float contentW;
    private float contentH;
    private long lastFrameNs;
    private Widget activeWidget;
    /** Owning catalog id while a visible key field has captured the next physical key. */
    private String keybindCaptureId;
    private Hit pendingHit;
    private boolean gestureActive;
    private boolean sliderEngaged;
    private boolean draggingWindow;
    private float windowOffsetX;
    private float windowOffsetY;
    private float windowGrabX;
    private float windowGrabY;
    private String toast = "";
    private long toastUntilNs;
    /** Header icon actions are immediate, so retain a short paint-only press acknowledgement after the click. */
    private int headerPressedHit;
    private long headerPressedUntilNs;
    private String editingChatTabId = ChatTabs.ALL;
    private int draftChatFilterType = ChatTabs.CONTAINS;
    private boolean draftChatSound;

    /** Opens Home. */
    public AppleSettingsScreen(GuiScreen parent) {
        this(parent, ReferenceSettingsCatalog.Tab.HOME);
    }

    /** Opens the supplied reference destination; null intentionally falls back to Home. */
    public AppleSettingsScreen(GuiScreen parent, ReferenceSettingsCatalog.Tab initialTab) {
        this.parent = parent;
        // A fresh top-level settings visit starts clean. A child editor returns to this existing instance instead.
        FeatureUiPreferences.closeAllDropdowns();
        this.activeTab = initialTab == null ? ReferenceSettingsCatalog.Tab.HOME : initialTab;
        this.fullscreenWindow = AppleSettings.settingsWindowFullscreen;
        refreshReferenceTheme();
    }

    /** Opens the visible QoL category through its stable GAMEPLAY route with Chat QoL disclosed. */
    public static AppleSettingsScreen openChatSettings(GuiScreen parent) {
        AppleSettingsScreen screen = new AppleSettingsScreen(parent, ReferenceSettingsCatalog.Tab.GAMEPLAY);
        // This is an intentional route target, so it opens after the ordinary fresh-screen reset.
        FeatureUiPreferences.openAccordion("chat-qol", ReferenceSettingsCatalog.dropdownIds(ReferenceSettingsCatalog.Tab.GAMEPLAY));
        return screen;
    }

    /** Native replacement destination order, exposed for headless regression coverage. */
    static String[] destinationNames() {
        return DESTINATION_NAMES.clone();
    }

    /** The reference is a rail plus direct inline child rows, never the retired expandable seven-section Hub. */
    static int mainNavigationDepth() {
        return 0;
    }

    static boolean requiredRoutesArePresent() {
        return ReferenceSettingsCatalog.routesAreComplete();
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        HudManager.ensureLoaded();
        // Returning from a HUD editor reuses this same screen instance after its child restored the global UI state.
        refreshReferenceTheme();
    }

    @Override
    public void onGuiClosed() {
        cancelKeybindCapture();
        super.onGuiClosed();
        draggingWindow = false;
        Keyboard.enableRepeatEvents(false);
        commitInlineTextFields();
        AppleSettings.save();
        HudManager.save();
        Theme.refresh();
    }

    /** Rebuilds the fixed Theme after allowed geometry, opacity, or motion preferences change. */
    private void refreshReferenceTheme() {
        Theme.applyReferenceSettingsPalette();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long now = System.nanoTime();
        float dt = lastFrameNs == 0L ? 1f / 60f : Math.min(.1f, (now - lastFrameNs) / 1.0e9f);
        lastFrameNs = now;
        // A child HUD editor deliberately restores generic GUI state as it closes. Reapply this fixed Theme here as
        // an idempotent owner action so repeated editor round-trips cannot tint this screen incorrectly.
        refreshReferenceTheme();
        refreshLayout();
        normalizeAccordion(activeTab);
        updateTabSelection(dt);
        updateDropdownAnimation(dt);
        scroll.update(dt, AppleSettings.smoothScrolling);
        updateSearchAndWidgets(dt);

        int quality = AppleSettings.experimentalPerformance ? 2 : AppleSettings.curveQuality;
        quality = Math.max(0, Math.min(AppleSettings.CURVE_SEGMENTS.length - 1, quality));
        UIRender.segCap = AppleSettings.CURVE_SEGMENTS[quality];
        UIRender.shadows = AppleSettings.shadows && !AppleSettings.experimentalPerformance;

        drawDefaultBackground();
        UIRender.beginFrame();
        try {
            UIRender.rect(0f, 0f, width, height, Theme.veil);
            drawShell(mouseX, mouseY);
        } finally {
            UIRender.endFrame();
        }
    }

    private void refreshLayout() {
        Minecraft mc = Minecraft.getMinecraft();
        sf = new ScaledResolution(mc).getScaleFactor();
        // This single pure layout policy is shared by rendering and hit testing. Fullscreen deliberately owns the
        // safe area, while a dragged small window retains its exact constrained position across a fullscreen round trip.
        ReferenceSettingsWindowLayout.layout(panel, width, height, fullscreenWindow, windowOffsetX, windowOffsetY);
        if (!fullscreenWindow) {
            windowOffsetX = panel.x - ReferenceSettingsWindowLayout.centeredX(width, panel.w);
            windowOffsetY = panel.y - ReferenceSettingsWindowLayout.centeredY(height, panel.h);
            smallWindowOffsetX = windowOffsetX;
            smallWindowOffsetY = windowOffsetY;
        }
        SettingsShellLayout.layoutHeader(shellHeader, panel);

        // Product identity is merged into the header. The second strip stays one compact search row at every width,
        // preserving room for the nine destinations and avoiding a duplicate title/version hierarchy.
        toolbarH = TOOLBAR_H;
        float bodyY = panel.y + CHROME_H + toolbarH;
        float bodyH = Math.max(1f, panel.h - CHROME_H - toolbarH);
        // The rail changes to a two-row layout before its fixed 4 px entry rhythm could turn into large gaps or
        // clipped destinations. SettingsRailLayout owns the exact same rectangles used for painting and input.
        compactRail = SettingsRailLayout.shouldCompact(panel.w, bodyH, ReferenceSettingsCatalog.TABS.length);

        if (compactRail) {
            railX = panel.x + GuiStyle.SPACE_4;
            railY = bodyY + GuiStyle.SPACE_4;
            railW = Math.max(1f, panel.w - GuiStyle.SPACE_8);
            railH = Math.min(bodyH - GuiStyle.SPACE_4, SettingsRailLayout.compactHeight(ReferenceSettingsCatalog.TABS.length));
            railH = Math.max(1f, railH);
            contentX = panel.x + GuiStyle.SPACE_12;
            contentY = railY + railH + GuiStyle.SPACE_4;
            contentW = Math.max(1f, panel.w - GuiStyle.SPACE_12 * 2f);
            contentH = Math.max(1f, panel.bottom() - GuiStyle.SPACE_4 - contentY);
        } else {
            railX = panel.x;
            railY = bodyY;
            railW = Math.min(RAIL_W, Math.max(96f, panel.w * .25f));
            railH = bodyH;
            contentX = railX + railW + GuiStyle.SPACE_12;
            contentY = bodyY + GuiStyle.SPACE_12;
            contentW = Math.max(1f, panel.right() - GuiStyle.SPACE_12 - contentX);
            contentH = Math.max(1f, panel.bottom() - GuiStyle.SPACE_12 - contentY);
        }
        SettingsRailLayout.layout(settingsRail, railX, railY, railW, railH, compactRail);
        // The viewport includes the reserved gutter. Cards, headings and controls use the narrower content column,
        // leaving a permanent clear lane for the thumb at every scroll position.
        viewport.set(contentX, contentY, contentW, contentH);
        contentW = SettingsShellLayout.contentColumnWidth(viewport.w);
        // Narrow pages are intentionally one-column: controls move below their labels rather than fighting them.
        narrowRows = SettingsPageLayout.isNarrow(contentW);
        search.w = Math.max(50f, searchWidth());
        search.h = 23f;
        layoutSearch();
    }

    private void layoutSearch() {
        search.x = panel.x + GuiStyle.SPACE_12;
        search.y = panel.y + CHROME_H + (toolbarH - search.h) * .5f;
    }

    private float searchWidth() {
        // Header owns the product title; Search owns the deliberately quiet full toolbar width below it.
        return Math.max(52f, panel.w - GuiStyle.SPACE_12 * 2f);
    }

    private void updateSearchAndWidgets(float dt) {
        search.update(dt);
        for (Widget widget : controls.values()) {
            widget.update(dt);
        }
    }

    /** Eases only while this native settings screen is the open Minecraft GUI. */
    private void updateTabSelection(float dt) {
        boolean guiOpen = Minecraft.getMinecraft().currentScreen == this && Theme.animations;
        for (int i = 0; i < tabSelection.length; i++) {
            float target = ReferenceSettingsCatalog.TABS[i] == activeTab ? 1f : 0f;
            if (!tabSelectionInitialized) {
                tabSelection[i] = target;
            } else {
                tabSelection[i] = GuiMotion.ease(tabSelection[i], target, dt, guiOpen,
                        GuiMotion.TAB_SETTLE_MS, Theme.speed);
            }
        }
        tabSelectionInitialized = true;
    }

    /** Normalizes only presentation state so old files cannot render two disclosures open on the same page. */
    private void normalizeAccordion(ReferenceSettingsCatalog.Tab tab) {
        String retained = null;
        for (String id : ReferenceSettingsCatalog.dropdownIds(tab)) {
            if (!FeatureUiPreferences.dropdownOpen(id)) continue;
            if (retained == null) retained = id;
            else FeatureUiPreferences.setDropdownOpen(id, false);
        }
    }

    /** Animates only the paint/layout height; FeatureUiPreferences remains the single persisted disclosure source. */
    private void updateDropdownAnimation(float dt) {
        boolean guiOpen = Minecraft.getMinecraft().currentScreen == this && Theme.animations;
        for (ReferenceSettingsCatalog.Card card : ReferenceSettingsCatalog.cards(activeTab)) {
            for (ReferenceSettingsCatalog.Setting setting : card.settings) updateDropdownAnimation(setting, dt, guiOpen);
        }
    }

    private void updateDropdownAnimation(ReferenceSettingsCatalog.Setting setting, float dt, boolean guiOpen) {
        if (setting == null) return;
        if (setting.isDropdown()) {
            float target = FeatureUiPreferences.dropdownOpen(setting.id) ? 1f : 0f;
            Float prior = dropdownProgress.get(setting.id);
            float current = prior == null ? target : prior.floatValue();
            dropdownProgress.put(setting.id, GuiMotion.ease(current, target, dt, guiOpen,
                    GuiMotion.TOGGLE_SETTLE_MS, Theme.speed));
        }
        for (ReferenceSettingsCatalog.Setting child : setting.children) updateDropdownAnimation(child, dt, guiOpen);
    }

    private float dropdownProgress(ReferenceSettingsCatalog.Setting setting) {
        if (setting == null || !setting.isDropdown()) return 0f;
        Float value = dropdownProgress.get(setting.id);
        return value == null ? (FeatureUiPreferences.dropdownOpen(setting.id) ? 1f : 0f) : value.floatValue();
    }

    private void drawShell(int mouseX, int mouseY) {
        visibleHits.clear();
        // One equal-radius surface owns the entire window. All header, rail, scroll content, popovers and toast
        // rendering happens inside the matching rounded clip below, so no square child can leak through a corner.
        float outerRadius = GuiStyle.radius(GuiStyle.RADIUS_WINDOW, Theme.radius, panel.w, panel.h);
        UIRender.shadow(panel.x, panel.y, panel.w, panel.h, outerRadius);
        UIRender.roundRectBorder(panel.x, panel.y, panel.w, panel.h, outerRadius,
                Theme.windowTop, Theme.windowBottom, Theme.border);
        UIRender.roundedClipBegin(panel.x, panel.y, panel.w, panel.h, outerRadius, sf);
        try {
            drawChrome(mouseX, mouseY);
            drawToolbar(mouseX, mouseY);
            drawHeaderTooltip(mouseX, mouseY);
            drawRail(mouseX, mouseY);
            drawContent(mouseX, mouseY);
            drawToast();
            // Value dropdowns render above clipped rows but remain inside the one settings window.
            drawOpenDropdown(mouseX, mouseY, panel.y + CHROME_H, panel.bottom(), panel.right(), panel.x);
        } finally {
            UIRender.roundedClipEnd();
        }
    }

    /** Header contains exactly the two explicit icon actions; the clear title-bar area remains the drag handle. */
    private void drawChrome(int mouseX, int mouseY) {
        float y = panel.y;
        int chrome = UIRender.mix(Theme.windowTop, Theme.windowBottom, .58f);
        UIRender.rect(panel.x + 1f, y + 1f, panel.w - 2f, Math.max(0f, CHROME_H - 1f), chrome);
        UIRender.line(panel.x + 1f, y + CHROME_H, panel.right() - 1f, y + CHROME_H, 1f, Theme.divider);

        // Keep product identity in the upper-left, entirely inside the existing drag region. The header intentionally
        // still exposes only the Fullscreen and Close hit targets from SettingsShellLayout.
        float wordmarkX = panel.x + GuiStyle.SPACE_12;
        float wordmarkY = y + (CHROME_H - 15f) * .5f;
        UIRender.roundRect(wordmarkX, wordmarkY, 15f, 15f, 4f, Theme.accent, Theme.accent, 0.4f);
        UIRender.text(fr(), "S", wordmarkX + 4.2f, wordmarkY + 3.3f, Theme.OPAQUE_WHITE, Theme.TYPE_BODY, false);
        float titleX = wordmarkX + 20f;
        float titleRight = shellHeader.dragRight - GuiStyle.SPACE_4;
        String wordmark = fit(fr(), "SkyVeLor", Math.max(1f, titleRight - titleX) / Theme.TYPE_TITLE);
        UIRender.text(fr(), wordmark, titleX, centeredTextY(y, CHROME_H, Theme.TYPE_TITLE),
                Theme.textPrimary, Theme.TYPE_TITLE, false);
        float sectionX = titleX + fr().getStringWidth(wordmark) * Theme.TYPE_TITLE + GuiStyle.SPACE_8;
        String section = fit(fr(), "Settings", Math.max(0f, titleRight - sectionX) / Theme.TYPE_BODY);
        if (section.length() > 0) {
            UIRender.text(fr(), section, sectionX, centeredTextY(y, CHROME_H, Theme.TYPE_BODY),
                    Theme.textSecondary, Theme.TYPE_BODY, false);
        }
        drawHeaderIconButton(shellHeader.fullscreen, mouseX, mouseY, fullscreenWindow,
                fullscreenWindow ? "Exit fullscreen" : "Fullscreen", HIT_WINDOW_FULLSCREEN);
        drawHeaderIconButton(shellHeader.close, mouseX, mouseY, false, "Close settings", HIT_WINDOW_CLOSE);
    }

    private void drawHeaderIconButton(GuiLayoutToolkit.Rect bounds, int mouseX, int mouseY, boolean selected,
                                      String tooltip, int hitType) {
        boolean hovered = bounds.contains(mouseX, mouseY);
        boolean pressed = hitType == headerPressedHit && System.nanoTime() < headerPressedUntilNs;
        int fill = selected ? UIRender.withAlpha(Theme.accent, .18f) : (hovered ? Theme.buttonHover : Theme.buttonFill);
        if (pressed) fill = UIRender.mix(fill, Theme.OPAQUE_BLACK, .18f);
        int border = selected ? UIRender.withAlpha(Theme.accent, .72f) : Theme.border;
        UIRender.roundRectBorder(bounds.x, bounds.y, bounds.w, bounds.h,
                GuiStyle.radius(GuiStyle.RADIUS_BUTTON, Theme.radius, bounds.w, bounds.h), fill, fill, border);
        float cx = bounds.x + bounds.w * .5f;
        float cy = bounds.y + bounds.h * .5f;
        if (hitType == HIT_WINDOW_FULLSCREEN) {
            drawFullscreenGlyph(cx, cy, selected ? Theme.accent : Theme.textSecondary, selected);
        } else {
            drawCloseGlyph(cx, cy, hovered ? Theme.textPrimary : Theme.textSecondary);
        }
        registerHit(hitType == HIT_WINDOW_FULLSCREEN ? "window-fullscreen" : "window-close", hitType,
                bounds.x, bounds.y, bounds.w, bounds.h, null, tooltip);
    }

    private void drawFullscreenGlyph(float cx, float cy, int color, boolean active) {
        float outer = 5f;
        float inner = active ? 2f : 4f;
        // Corners point out for fullscreen and in for restore; both forms remain legible at a 24 px hit target.
        if (!active) {
            UIRender.line(cx - outer, cy - outer, cx - inner, cy - outer, 1f, color);
            UIRender.line(cx - outer, cy - outer, cx - outer, cy - inner, 1f, color);
            UIRender.line(cx + outer, cy - outer, cx + inner, cy - outer, 1f, color);
            UIRender.line(cx + outer, cy - outer, cx + outer, cy - inner, 1f, color);
            UIRender.line(cx - outer, cy + outer, cx - inner, cy + outer, 1f, color);
            UIRender.line(cx - outer, cy + outer, cx - outer, cy + inner, 1f, color);
            UIRender.line(cx + outer, cy + outer, cx + inner, cy + outer, 1f, color);
            UIRender.line(cx + outer, cy + outer, cx + outer, cy + inner, 1f, color);
        } else {
            UIRender.line(cx - outer, cy - inner, cx - inner, cy - inner, 1f, color);
            UIRender.line(cx - inner, cy - outer, cx - inner, cy - inner, 1f, color);
            UIRender.line(cx + outer, cy - inner, cx + inner, cy - inner, 1f, color);
            UIRender.line(cx + inner, cy - outer, cx + inner, cy - inner, 1f, color);
            UIRender.line(cx - outer, cy + inner, cx - inner, cy + inner, 1f, color);
            UIRender.line(cx - inner, cy + outer, cx - inner, cy + inner, 1f, color);
            UIRender.line(cx + outer, cy + inner, cx + inner, cy + inner, 1f, color);
            UIRender.line(cx + inner, cy + outer, cx + inner, cy + inner, 1f, color);
        }
    }

    private void drawHeaderTooltip(int mouseX, int mouseY) {
        String label = null;
        GuiLayoutToolkit.Rect anchor = null;
        if (shellHeader.fullscreen.contains(mouseX, mouseY)) {
            label = fullscreenWindow ? "Exit fullscreen" : "Fullscreen";
            anchor = shellHeader.fullscreen;
        } else if (shellHeader.close.contains(mouseX, mouseY)) {
            label = "Close settings";
            anchor = shellHeader.close;
        }
        if (label == null || anchor == null || panel.w < 160f) return;
        float tooltipW = Math.min(panel.w - GuiStyle.SPACE_16, fr().getStringWidth(label) * .62f + 12f);
        float x = Math.max(panel.x + GuiStyle.SPACE_8, Math.min(panel.right() - tooltipW - GuiStyle.SPACE_8,
                anchor.x + anchor.w - tooltipW));
        float y = panel.y + CHROME_H + 3f;
        UIRender.roundRectBorder(x, y, tooltipW, 14f, 4f, Theme.popup, Theme.popup, Theme.border);
        UIRender.text(fr(), label, x + 6f, centeredTextY(y, 14f, .62f), Theme.textPrimary, .62f, false);
    }

    private void drawToolbar(int mouseX, int mouseY) {
        float y = panel.y + CHROME_H;
        UIRender.rect(panel.x + 1f, y, panel.w - 2f, toolbarH, Theme.windowTop);
        UIRender.line(panel.x + 1f, y + toolbarH, panel.right() - 1f, y + toolbarH, 1f, Theme.divider);
        search.draw(mouseX, mouseY);
        if (!search.isEmpty()) {
            registerHit("search-clear", HIT_SEARCH_CLEAR, search.x + search.w - 20f, search.y, 20f, search.h, null, "");
            drawCloseGlyph(search.x + search.w - 11f, search.y + search.h * .5f, Theme.textTertiary);
        } else {
            UIRender.text(fr(), "/", search.x + search.w - 10f, centeredTextY(search.y, search.h, Theme.TYPE_BODY),
                    Theme.textTertiary, Theme.TYPE_BODY, false);
        }
    }

    private void drawRail(int mouseX, int mouseY) {
        int railFill = Theme.windowBottom;
        if (!compactRail) {
            UIRender.rect(railX + 1f, railY, railW, railH, railFill);
            UIRender.line(railX + railW, railY, railX + railW, railY + railH, 1f, Theme.divider);
        } else {
            float railRadius = GuiStyle.radius(GuiStyle.RADIUS_CARD, Theme.radius, railW, railH);
            UIRender.roundRect(railX, railY, railW, railH, railRadius, railFill, railFill, .35f);
            UIRender.roundRectRing(railX, railY, railW, railH, railRadius, GuiStyle.OUTLINE, Theme.border);
        }
        int tabCount = ReferenceSettingsCatalog.TABS.length;
        for (int i = 0; i < tabCount; i++) {
            ReferenceSettingsCatalog.Tab tab = ReferenceSettingsCatalog.TABS[i];
            GuiLayoutToolkit.Rect bounds = settingsRail.entries[i];
            float x = bounds.x;
            float y = bounds.y;
            float w = bounds.w;
            float h = bounds.h;
            boolean active = activeTab == tab;
            float selected = tabSelectionInitialized ? tabSelection[i] : (active ? 1f : 0f);
            boolean hovered = inside(mouseX, mouseY, x, y, w, h);
            if (selected > .001f || hovered) {
                int idle = hovered ? Theme.cardHover : Theme.card;
                int selectedFill = UIRender.withAlpha(Theme.accent, .13f);
                int fill = UIRender.mix(idle, selectedFill, selected);
                UIRender.roundRect(x, y, w, h,
                        GuiStyle.radius(GuiStyle.RADIUS_CARD, Theme.radius, w, h), fill, fill, .25f);
            }
            boolean showLabel = !compactRail || w > 68f;
            String visibleLabel = showLabel ? fit(fr(), tab.label,
                    Math.max(1f, (compactRail ? w - 26f : w - 33f) / .78f)) : "";
            float iconX;
            float labelX = 0f;
            if (compactRail && showLabel) {
                // Keep the icon-and-text pair centered inside a compact cell; the glyph-only fallback is centered too.
                float groupW = 14f + GuiStyle.SPACE_4 + fr().getStringWidth(visibleLabel) * .78f;
                iconX = x + Math.max(3f, (w - groupW) * .5f);
                labelX = iconX + 14f + GuiStyle.SPACE_4;
            } else {
                iconX = compactRail ? x + Math.max(3f, (w - 14f) * .5f) : x + GuiStyle.SPACE_4;
                if (showLabel) labelX = x + 24f;
            }
            float iconY = y + (h - 14f) * .5f;
            drawRailGlyph(tab, iconX, iconY, active ? Theme.accent : Theme.textTertiary);
            if (showLabel) {
                UIRender.text(fr(), visibleLabel, labelX, centeredTextY(y, h, .78f),
                        active ? Theme.textPrimary : Theme.textSecondary, .78f, false);
            }
            if (active && !compactRail) {
                UIRender.circle(x + w - GuiStyle.SPACE_8 + 1f, y + h * .5f, 1.7f, Theme.accent);
            }
            GuiDebugOverlay.recordRow("Settings rail " + tab.label, x, y, w, h);
            registerHit("nav-" + tab.name(), HIT_NAV, x, y, w, h, null, tab.name());
        }

    }

    private float centeredTextY(float top, float height, float scale) {
        return top + Math.max(0f, (height - fr().FONT_HEIGHT * scale) * .5f);
    }

    /**
     * Layout debug draws after the content scissor has closed. Clip registration to that same viewport so a card that
     * is correctly scrolled out behind the compact rail cannot be reported as a false overlap with a rail target.
     */
    private void recordViewportRow(String name, float x, float y, float w, float h) {
        float left = Math.max(x, viewport.x);
        float top = Math.max(y, viewport.y);
        float right = Math.min(x + w, viewport.right());
        float bottom = Math.min(y + h, viewport.bottom());
        if (right > left && bottom > top) GuiDebugOverlay.recordRow(name, left, top, right - left, bottom - top);
    }

    private void recordViewportControl(String name, float x, float y, float w, float h) {
        float left = Math.max(x, viewport.x);
        float top = Math.max(y, viewport.y);
        float right = Math.min(x + w, viewport.right());
        float bottom = Math.min(y + h, viewport.bottom());
        if (right > left && bottom > top) GuiDebugOverlay.recordControl(name, left, top, right - left, bottom - top);
    }

    private void drawContent(int mouseX, int mouseY) {
        float total = measuredContentHeight();
        scroll.setMaximum(Math.max(0f, total - viewport.h));
        boolean mouseInView = viewport.contains(mouseX, mouseY);
        UIRender.scissorBegin(viewport.x, viewport.y, viewport.w, viewport.h, sf);
        try {
            float y = viewport.y - scroll.value();
            if (!search.isEmpty()) {
                y = drawSearchResults(y, mouseInView ? mouseX : -10000, mouseInView ? mouseY : -10000);
            } else if (activeTab == ReferenceSettingsCatalog.Tab.HOME) {
                y = drawHome(y, mouseInView ? mouseX : -10000, mouseInView ? mouseY : -10000);
            } else if (activeTab == ReferenceSettingsCatalog.Tab.FARMING) {
                y = drawFarmingEmpty(y);
            } else {
                y = drawTab(y, mouseInView ? mouseX : -10000, mouseInView ? mouseY : -10000, activeTab);
            }
            drawScrollEdgeFades(total);
        } finally {
            UIRender.scissorEnd();
        }
        drawScrollBar(total);
    }

    /** A short edge fade makes it clear that clipped cards continue without drawing over the thumb gutter. */
    private void drawScrollEdgeFades(float total) {
        if (total <= viewport.h + .5f) return;
        int transparentSurface = Theme.windowBottom & Theme.RGB_MASK;
        if (scroll.value() > .5f) {
            UIRender.verticalGradient(contentX, viewport.y, contentW, SettingsShellLayout.SCROLL_EDGE_FADE,
                    Theme.windowBottom, transparentSurface);
        }
        if (scroll.value() < scroll.maximum() - .5f) {
            float y = viewport.bottom() - SettingsShellLayout.SCROLL_EDGE_FADE;
            UIRender.verticalGradient(contentX, y, contentW, SettingsShellLayout.SCROLL_EDGE_FADE,
                    transparentSurface, Theme.windowBottom);
        }
    }

    private float measuredContentHeight() {
        if (!search.isEmpty()) return measureSearchResults();
        if (activeTab == ReferenceSettingsCatalog.Tab.HOME) return measureHome();
        if (activeTab == ReferenceSettingsCatalog.Tab.FARMING) return fr().FONT_HEIGHT + 2f + SettingsShellLayout.FINAL_CARD_PADDING;
        return measureTab(activeTab);
    }

    private void drawScrollBar(float total) {
        if (total <= viewport.h + .5f) return;
        float track = Math.max(1f, viewport.h - 8f);
        float thumb = Math.max(22f, track * viewport.h / total);
        float max = Math.max(1f, scroll.maximum());
        float y = viewport.y + 4f + (track - thumb) * (scroll.value() / max);
        UIRender.roundRect(SettingsShellLayout.scrollbarX(viewport), y, 2f, thumb, 1f,
                Theme.textTertiary, Theme.textTertiary, .5f);
    }

    /** This page deliberately has no card, control, placeholder setting or footer until a real farming owner exists. */
    private float drawFarmingEmpty(float y) {
        UIRender.text(fr(), "No Farming Features Yet", contentX, y + 1f, Theme.textSecondary, 1f, false);
        return y + fr().FONT_HEIGHT + 2f + SettingsShellLayout.FINAL_CARD_PADDING;
    }

    private float drawTab(float y, int mouseX, int mouseY, ReferenceSettingsCatalog.Tab tab) {
        UIRender.text(fr(), tab.label, contentX, y + 1f, Theme.textPrimary, 1.26f, false);
        UIRender.text(fr(), fit(fr(), tab.caption, contentW / .78f), contentX, y + 17f, Theme.textSecondary, .78f, false);
        y += 35f;
        List<ReferenceSettingsCatalog.Card> cards = ReferenceSettingsCatalog.cards(tab);
        boolean drew = false;
        for (ReferenceSettingsCatalog.Card card : cards) {
            y = drawCard(y, mouseX, mouseY, tab, card);
            y += CARD_GAP;
            drew = true;
        }
        if (!drew) {
            drawEmpty(y, "No settings are registered in this category.");
            y += 65f;
        }
        // The final card never lands flush with the viewport edge, even when the user scrolls exactly to the end.
        return y + SettingsShellLayout.FINAL_CARD_PADDING;
    }

    private float measureTab(ReferenceSettingsCatalog.Tab tab) {
        float h = 35f;
        int cards = 0;
        for (ReferenceSettingsCatalog.Card card : ReferenceSettingsCatalog.cards(tab)) {
            h += measureCard(card) + CARD_GAP;
            cards++;
        }
        if (cards == 0) h += 65f;
        return h + SettingsShellLayout.FINAL_CARD_PADDING;
    }

    /**
     * Restored Apple card hierarchy: each catalog grouping has its own translucent surface, compact icon well,
     * title/subtitle header, and the existing flat option rows beneath it. The card is presentation only; the
     * catalog, bindings and every child route remain the current implementation.
     */
    private float drawCard(float y, int mouseX, int mouseY, ReferenceSettingsCatalog.Tab tab,
                           ReferenceSettingsCatalog.Card card) {
        final float headerH = 46f;
        float h = measureCard(card);
        recordViewportRow("Settings card " + card.id, contentX, y, contentW, h);
        float radius = GuiStyle.radius(GuiStyle.RADIUS_CARD, Theme.radius, contentW, h);
        UIRender.roundRectBorder(contentX, y, contentW, h, radius, Theme.card, Theme.card, Theme.border);

        float iconX = contentX + CARD_PAD;
        float iconY = y + CARD_PAD;
        UIRender.roundRect(iconX, iconY, 23f, 23f,
                GuiStyle.radius(GuiStyle.RADIUS_SMALL, Theme.radius, 23f, 23f),
                Theme.buttonFill, Theme.buttonFill, .25f);
        drawCardGlyph(card.id, iconX + 6f, iconY + 6f, Theme.accent);

        float titleX = iconX + 31f;
        float textW = Math.max(1f, contentW - (titleX - contentX) - CARD_PAD);
        float titleY = y + 10f;
        UIRender.text(fr(), fit(fr(), card.title, textW / Theme.TYPE_HEADING), titleX, titleY,
                Theme.textPrimary, Theme.TYPE_HEADING, false);
        UIRender.text(fr(), fit(fr(), card.subtitle, textW / Theme.TYPE_BODY), titleX, titleY + 13f,
                Theme.textSecondary, Theme.TYPE_BODY, false);

        float rowY = y + headerH;
        for (ReferenceSettingsCatalog.Setting setting : card.settings) {
            rowY = drawSetting(rowY, mouseX, mouseY, tab, card, setting, false);
        }
        if (!card.notice.isEmpty()) {
            float noticeH = measureNotice(card.notice);
            int noticeFill = UIRender.mix(Theme.windowBottom, Theme.card, .28f);
            UIRender.rect(contentX + 1f, rowY, contentW - 2f, noticeH, noticeFill);
            drawNoticeGlyph(contentX + ROW_PAD_X + 1f, rowY + 10f,
                    card.warningNotice ? Theme.NOTICE_WARNING : Theme.accent, card.warningNotice);
            drawWrapped(card.notice, contentX + ROW_PAD_X + 13f, rowY + 7f,
                    contentW - ROW_PAD_X * 2f - 16f, Theme.textSecondary, Theme.TYPE_BODY, 3);
        }
        return y + h;
    }

    private float measureCard(ReferenceSettingsCatalog.Card card) {
        float h = 46f;
        for (ReferenceSettingsCatalog.Setting setting : card.settings) h += measureSetting(setting, false);
        if (!card.notice.isEmpty()) h += measureNotice(card.notice);
        return h;
    }

    private float measureNotice(String text) {
        return Math.max(30f, 13f + cachedLines("notice:" + text, text,
                Math.max(20f, contentW - ROW_PAD_X * 2f - 16f), .68f, 3) * 8f);
    }

    private float drawSetting(float y, int mouseX, int mouseY, ReferenceSettingsCatalog.Tab tab,
                              ReferenceSettingsCatalog.Card card, ReferenceSettingsCatalog.Setting setting,
                              boolean child) {
        return drawSetting(y, mouseX, mouseY, tab, card, setting, child, false);
    }

    /** Draws a flat settings row. Captions and reset links are paint-only content, never nested controls. */
    private float drawSetting(float y, int mouseX, int mouseY, ReferenceSettingsCatalog.Tab tab,
                              ReferenceSettingsCatalog.Card card, ReferenceSettingsCatalog.Setting setting,
                              boolean child, boolean dimmed) {
        if (setting.isCaption()) return drawGroupCaption(y, setting, dimmed);
        if (child && setting.isResetAction()) return drawResetFooter(y, setting, dimmed);

        boolean disclosure = setting.isDropdown();
        boolean open = disclosure && FeatureUiPreferences.dropdownOpen(setting.id);
        float rowH = measureSetting(setting, child);
        float baseH = baseSettingHeight(setting, child);
        float x = contentX;
        float rowW = contentW;
        recordViewportRow("Settings row " + setting.id, x, y, rowW, rowH);
        boolean rowHot = child && inside(mouseX, mouseY, x + 4f, y + 1f, rowW - 8f, baseH - 2f);
        float rowRadius = GuiStyle.radius(GuiStyle.RADIUS_SMALL, Theme.radius, rowW - 8f, Math.max(1f, baseH - 3f));
        if (disclosure && open) {
            // An open owner gets a quiet blue state surface so its child panel reads as one deliberate group.
            UIRender.roundRect(x + 4f, y + 2f, rowW - 8f, Math.max(1f, baseH - 3f), rowRadius,
                    Theme.expandedHeader, Theme.expandedHeader, .2f);
        } else if (rowHot) {
            UIRender.roundRect(x + 5f, y + 2f, rowW - 10f, Math.max(1f, baseH - 3f), rowRadius,
                    Theme.expandedRowHover, Theme.expandedRowHover, .2f);
        }
        float dividerInset = child ? 8f : 1f;
        UIRender.line(x + dividerInset, y, x + rowW - dividerInset, y, 1f,
                child ? Theme.expandedDivider : Theme.divider);

        float leftX = x + ROW_PAD_X;
        float chevronX = SettingsControlLayout.chevronCenterX(x, rowW, ROW_PAD_X);
        float toggleX = SettingsControlLayout.toggleX(x, rowW, ROW_PAD_X);
        float controlRight = SettingsControlLayout.contentRight(x, rowW, ROW_PAD_X, CONTROL_GAP);
        float valueW = controlWidth(setting);
        boolean inlineControl = setting.control == ReferenceSettingsCatalog.Control.SLIDER
                || setting.control == ReferenceSettingsCatalog.Control.CHOICE
                || setting.control == ReferenceSettingsCatalog.Control.TEXT
                || setting.control == ReferenceSettingsCatalog.Control.ACTION;
        float trailingW = setting.isEditor() ? editorButtonWidth(setting)
                : setting.isKeybind() ? keybindButtonWidth() : valueW;
        float actionX = usesTrailingAction(setting) ? actionButtonX(x, rowW, trailingW) : 0f;
        float labelRight;
        if (setting.control == ReferenceSettingsCatalog.Control.ACTION) {
            labelRight = actionX - CONTROL_GAP;
        } else if (setting.isEditor() || setting.isKeybind()) {
            labelRight = setting.hasMasterToggle() ? editorToggleX(actionX) - CONTROL_GAP : actionX - CONTROL_GAP;
        } else {
            labelRight = inlineControl ? controlRight - valueW - CONTROL_GAP : controlRight;
        }
        float labelW = Math.max(42f, labelRight - leftX);
        // On compact shells, copy takes its own line while every action still holds the shared right edge below it.
        if (narrowRows && (setting.isEditor() || setting.isKeybind() || inlineControl)) {
            labelW = Math.max(42f, rowW - ROW_PAD_X * 2f);
        }
        float labelY = y + ROW_PAD_Y;
        // Keep master-off children visibly muted, not unreadable. They remain intentionally editable so saved choices
        // can be prepared before the owner is enabled.
        int primary = dimmed ? Theme.dependentTextPrimary : Theme.textPrimary;
        int secondary = dimmed ? Theme.dependentTextSecondary : Theme.textSecondary;
        UIRender.text(fr(), fit(fr(), setting.label, labelW / Theme.TYPE_HEADING), leftX, labelY,
                primary, Theme.TYPE_HEADING, false);
        drawWrapped(setting.description, leftX, labelY + 11f, labelW, secondary, Theme.TYPE_BODY, 2);

        if (disclosure) {
            registerHit("dropdown:" + setting.id, HIT_DISCLOSURE, x + 1f, y + 1f, rowW - 2f,
                    baseH - 1f, setting, "");
        }
        float alpha = UIRender.alphaMul;
        if (dimmed) UIRender.alphaMul = alpha * Theme.DEPENDENT_CONTROL_ALPHA;
        try {
            if (setting.isEditor()) {
                float actionY = trailingActionY(y, baseH);
                if (setting.hasMasterToggle()) drawControl(setting, Math.max(leftX, editorToggleX(actionX)), actionY,
                        TOGGLE_COLUMN_W, mouseX, mouseY);
                drawEditorButton(setting, actionX, actionY, trailingW, mouseX, mouseY);
            } else if (setting.isKeybind()) {
                float actionY = keybindControlY(y, baseH);
                // A distinct full-row key keeps its geometry from mutating the compact button hit in knownHits.
                // Register it first so the key capsule and optional toggle retain click priority above it.
                registerHit("keybind-row:" + setting.id, HIT_CONTROL, x + 1f, y + 1f, rowW - 2f, baseH - 1f, setting, "");
                if (setting.hasMasterToggle()) drawKeybindToggle(setting, Math.max(leftX, editorToggleX(actionX)), actionY,
                        mouseX, mouseY);
                drawControl(setting, actionX, actionY, trailingW, mouseX, mouseY);
                drawKeybindConflictWarning(setting, leftX, y + baseH - KEYBIND_WARNING_H + 1f,
                        Math.max(1f, rowW - ROW_PAD_X * 2f));
            } else if (setting.control == ReferenceSettingsCatalog.Control.TOGGLE) {
                drawControl(setting, toggleX, centeredControlY(y, baseH, ACTION_BUTTON_H), TOGGLE_COLUMN_W, mouseX, mouseY);
            } else if (inlineControl) {
                float controlX = inlineControlX(setting, x, rowW, leftX, controlRight, valueW);
                float controlY = inlineControlY(setting, y, baseH);
                drawControl(setting, controlX, controlY, valueW, mouseX, mouseY);
            } else if ("terminal-assist".equals(setting.id)) {
                String status = terminalStatusText();
                UIRender.text(fr(), status, controlRight - fr().getStringWidth(status) * .68f,
                        centeredTextY(y, baseH, .68f), secondary, .68f, false);
            }
        } finally {
            UIRender.alphaMul = alpha;
        }
        if (disclosure) drawDisclosureGlyph(chevronX, y + baseH * .5f, open,
                dimmed ? Theme.dependentTextSecondary : (open ? Theme.accent : Theme.textSecondary));

        float cursor = y + baseH;
        if (inlineControl) {
            Widget widget = controls.get(setting.id);
            if (widget == openDropdown && widget instanceof AppleDropdown) {
                float controlX = inlineControlX(setting, x, rowW, leftX, controlRight, valueW);
                float controlY = inlineControlY(setting, y, baseH);
                cursor += reserveValueDropdown(setting, controlX, controlY, valueW, cursor);
            }
        }
        if (disclosure) {
            float fullChildH = childBlockHeight(setting);
            float progress = dropdownProgress(setting);
            float animatedChildH = fullChildH * progress;
            if (animatedChildH > .01f) {
                // Do not derive the inset from the transparent window: on a low-opacity glass setting that washed
                // feature labels into the world backdrop. This dedicated surface preserves a clear readable group.
                float panelX = x + 5f;
                float panelY = cursor + 3f;
                float panelW = Math.max(1f, rowW - 10f);
                float panelH = Math.max(1f, animatedChildH - 3f);
                float panelRadius = GuiStyle.radius(GuiStyle.RADIUS_CARD, Theme.radius, panelW, panelH);
                UIRender.roundRect(panelX, panelY, panelW, panelH, panelRadius,
                        Theme.expandedPanel, Theme.expandedPanel, .35f);
                UIRender.roundRectRing(panelX, panelY, panelW, panelH, panelRadius, 1f, Theme.expandedBorder);
                UIRender.scissorBegin(panelX, panelY, panelW, panelH, sf);
                float childY = cursor + 7f;
                float childAlpha = UIRender.alphaMul;
                UIRender.alphaMul = childAlpha * Math.max(.32f, progress);
                boolean childrenDimmed = dimmed || (setting.hasMasterToggle() && !InlineSettingsBindings.booleanValue(setting.id));
                try {
                    for (ReferenceSettingsCatalog.Setting nested : setting.children) {
                        childY = drawSetting(childY, mouseX, mouseY, tab, card, nested, true, childrenDimmed);
                    }
                } finally {
                    UIRender.alphaMul = childAlpha;
                    UIRender.scissorEnd();
                }
            }
        }
        return y + rowH;
    }

    private float drawGroupCaption(float y, ReferenceSettingsCatalog.Setting setting, boolean dimmed) {
        float h = 18f;
        int color = dimmed ? Theme.dependentTextSecondary : Theme.textSecondary;
        UIRender.text(fr(), setting.label.toUpperCase(Locale.ROOT), contentX + ROW_PAD_X, y + 5f, color, .61f, false);
        float textW = fr().getStringWidth(setting.label) * .61f;
        UIRender.line(Math.min(contentX + contentW - ROW_PAD_X, contentX + ROW_PAD_X + textW + 8f), y + 9f,
                contentX + contentW - ROW_PAD_X, y + 9f, 1f, Theme.expandedDivider);
        return y + h;
    }

    /** The only reset presentation is a compact end-of-panel link, not another full settings row. */
    private float drawResetFooter(float y, ReferenceSettingsCatalog.Setting setting, boolean dimmed) {
        float h = 24f;
        float x = contentX + ROW_PAD_X;
        String label = "Reset to defaults";
        int color = dimmed ? Theme.dependentTextSecondary : Theme.accent;
        UIRender.text(fr(), label, x, y + 7f, color, .68f, false);
        float w = fr().getStringWidth(label) * .68f;
        UIRender.line(x, y + 17f, x + w, y + 17f, 1f, color);
        registerHit("reset:" + setting.id, HIT_ACTION, x - 2f, y + 3f, w + 4f, 17f, setting, setting.id);
        return y + h;
    }

    private float measureSetting(ReferenceSettingsCatalog.Setting setting, boolean child) {
        if (setting.isCaption()) return 18f;
        if (child && setting.isResetAction()) return 24f;
        float h = baseSettingHeight(setting, child) + valueDropdownReservation(setting);
        if (setting.isDropdown()) h += childBlockHeight(setting) * dropdownProgress(setting);
        return h;
    }

    private float baseSettingHeight(ReferenceSettingsCatalog.Setting setting, boolean child) {
        float left = ROW_PAD_X;
        float right = ROW_PAD_X + RIGHT_COLUMNS_W + CONTROL_GAP;
        float labelW = Math.max(42f, contentW - left - right);
        if (setting.control == ReferenceSettingsCatalog.Control.ACTION || setting.isEditor() || setting.isKeybind()) {
            float actionW = setting.isEditor() ? editorButtonWidth(setting)
                    : setting.isKeybind() ? keybindButtonWidth() : controlWidth(setting);
            float actionX = actionButtonX(0f, contentW, actionW);
            float labelRight = (setting.isEditor() || setting.isKeybind()) && setting.hasMasterToggle()
                    ? editorToggleX(actionX) - CONTROL_GAP : actionX - CONTROL_GAP;
            labelW = Math.max(42f, labelRight - left);
        } else if (setting.control == ReferenceSettingsCatalog.Control.SLIDER
                || setting.control == ReferenceSettingsCatalog.Control.CHOICE
                || setting.control == ReferenceSettingsCatalog.Control.TEXT) {
            labelW = Math.max(42f, labelW - controlWidth(setting) - CONTROL_GAP);
        }
        boolean stackedAction = setting.isEditor() || setting.isKeybind()
                || setting.control == ReferenceSettingsCatalog.Control.SLIDER
                || setting.control == ReferenceSettingsCatalog.Control.CHOICE
                || setting.control == ReferenceSettingsCatalog.Control.TEXT
                || setting.control == ReferenceSettingsCatalog.Control.ACTION;
        if (narrowRows && stackedAction) labelW = Math.max(42f, contentW - left * 2f);
        int lines = cachedLines(setting.id, setting.description, labelW, Theme.TYPE_BODY, 2);
        float textH = 10f + lines * 8f;
        float base = Math.max(textH, controlHeight(setting)) + ROW_PAD_Y * 2f - 1f;
        if (narrowRows && stackedAction) base += controlHeight(setting) + 5f;
        // Key rows reserve one quiet line even when there is no conflict, preventing late collision text from changing
        // the row flow or overlapping another control.
        if (setting.isKeybind()) base += KEYBIND_WARNING_H;
        return Math.max(31f, base);
    }

    private boolean usesTrailingAction(ReferenceSettingsCatalog.Setting setting) {
        return setting != null && (setting.isEditor() || setting.isKeybind()
                || setting.control == ReferenceSettingsCatalog.Control.ACTION);
    }

    private float keybindButtonWidth() {
        return Math.max(64f, Math.min(KEYBIND_BUTTON_W, contentW - ROW_PAD_X * 2f));
    }

    private float actionButtonWidth() {
        return Math.max(1f, Math.min(ACTION_BUTTON_W, contentW - ROW_PAD_X * 2f));
    }

    private float actionButtonX(float x, float rowW, float actionW) {
        return SettingsControlLayout.actionButtonX(x, rowW, ROW_PAD_X, actionW);
    }

    private float editorToggleX(float actionX) {
        return actionX - CONTROL_GAP - TOGGLE_COLUMN_W;
    }

    private float trailingActionY(float y, float baseH) {
        return narrowRows ? SettingsControlLayout.stackedY(y, baseH, ACTION_BUTTON_H, 7f)
                : centeredControlY(y, baseH, ACTION_BUTTON_H);
    }

    /** Places a key capsule above its always-reserved conflict line on both wide and narrow settings pages. */
    private float keybindControlY(float y, float baseH) {
        float usable = Math.max(ACTION_BUTTON_H, baseH - KEYBIND_WARNING_H);
        return narrowRows ? y + Math.max(0f, baseH - KEYBIND_WARNING_H - ACTION_BUTTON_H - 5f)
                : centeredControlY(y, usable, ACTION_BUTTON_H);
    }

    private float inlineControlX(ReferenceSettingsCatalog.Setting setting, float x, float rowW, float leftX,
                                 float controlRight, float valueW) {
        if (setting.control == ReferenceSettingsCatalog.Control.ACTION) return actionButtonX(x, rowW, valueW);
        return narrowRows ? leftX : controlRight - valueW;
    }

    private float inlineControlY(ReferenceSettingsCatalog.Setting setting, float y, float baseH) {
        if (setting.control == ReferenceSettingsCatalog.Control.ACTION) return trailingActionY(y, baseH);
        return narrowRows ? SettingsControlLayout.stackedY(y, baseH, controlHeight(setting), 7f)
                : centeredControlY(y, baseH, controlHeight(setting));
    }

    private float centeredControlY(float y, float h, float controlH) {
        return Math.max(y + 6f, SettingsControlLayout.centeredY(y, h, controlH));
    }

    /** Reserve a real flow block for an open value selector so it cannot paint across another row. */
    private float valueDropdownReservation(ReferenceSettingsCatalog.Setting setting) {
        Widget widget = controls.get(setting.id);
        return widget == openDropdown && widget instanceof AppleDropdown
                ? ((AppleDropdown) widget).reservedPopupHeight() + 3f : 0f;
    }

    private float reserveValueDropdown(ReferenceSettingsCatalog.Setting setting, float controlX, float controlY,
                                       float controlW, float afterRowY) {
        Widget widget = controls.get(setting.id);
        if (widget != openDropdown || !(widget instanceof AppleDropdown)) return 0f;
        AppleDropdown dropdown = (AppleDropdown) widget;
        float reserve = dropdown.reservedPopupHeight() + 3f;
        dropdown.reservePopup(controlX, afterRowY + 2f, controlW, Math.max(0f, reserve - 3f));
        return reserve;
    }

    private float childBlockHeight(ReferenceSettingsCatalog.Setting parentSetting) {
        float h = 6f;
        for (ReferenceSettingsCatalog.Setting child : parentSetting.children) h += measureSetting(child, true);
        return h + EXPANDED_BOTTOM_PADDING;
    }

    private float controlWidth(ReferenceSettingsCatalog.Setting setting) {
        float available = Math.max(1f, contentW - ROW_PAD_X * 2f - RIGHT_COLUMNS_W);
        if (setting.control == ReferenceSettingsCatalog.Control.TOGGLE || setting.isEditor()) return TOGGLE_COLUMN_W;
        if (setting.isKeybind()) return keybindButtonWidth();
        if (setting.control == ReferenceSettingsCatalog.Control.DROPDOWN || setting.control == ReferenceSettingsCatalog.Control.INFO
                || setting.control == ReferenceSettingsCatalog.Control.CAPTION) return 0f;
        if (setting.control == ReferenceSettingsCatalog.Control.SLIDER) {
            return Math.min(available, narrowRows ? available : Math.min(160f, Math.max(82f, contentW * .34f)));
        }
        if (setting.control == ReferenceSettingsCatalog.Control.TEXT || setting.control == ReferenceSettingsCatalog.Control.CHOICE) {
            return Math.min(available, narrowRows ? available : Math.min(170f, Math.max(104f, contentW * .36f)));
        }
        if (setting.control == ReferenceSettingsCatalog.Control.ACTION) {
            return actionButtonWidth();
        }
        return 0f;
    }

    private float controlHeight(ReferenceSettingsCatalog.Setting setting) {
        if (setting.control == ReferenceSettingsCatalog.Control.SLIDER) return 24f;
        if (setting.control == ReferenceSettingsCatalog.Control.INFO || setting.control == ReferenceSettingsCatalog.Control.DROPDOWN
                || setting.control == ReferenceSettingsCatalog.Control.CAPTION) return 20f;
        return ACTION_BUTTON_H;
    }

    private void drawKeybindToggle(final ReferenceSettingsCatalog.Setting setting, float x, float y,
                                   int mouseX, int mouseY) {
        AppleToggle toggle = keybindToggleFor(setting);
        toggle.x = x;
        toggle.y = y;
        toggle.w = TOGGLE_COLUMN_W;
        toggle.h = ACTION_BUTTON_H;
        toggle.draw(mouseX, mouseY);
        recordViewportControl("Settings keybind toggle " + setting.id, toggle.x, toggle.y, toggle.w, toggle.h);
        registerHit("keybind-toggle:" + setting.id, HIT_CONTROL, toggle.x, toggle.y, toggle.w, toggle.h,
                setting, "keybind-toggle");
    }

    private AppleToggle keybindToggleFor(final ReferenceSettingsCatalog.Setting setting) {
        AppleToggle existing = keybindToggles.get(setting.id);
        if (existing != null) return existing;
        AppleToggle created = new AppleToggle(new Supplier<Boolean>() {
            @Override public Boolean get() { return booleanValue(setting.id); }
        }, new Consumer<Boolean>() {
            @Override public void accept(Boolean value) { setBoolean(setting.id, Boolean.TRUE.equals(value)); }
        });
        keybindToggles.put(setting.id, created);
        return created;
    }

    private void drawControl(ReferenceSettingsCatalog.Setting setting, float x, float y, float w,
                             int mouseX, int mouseY) {
        Widget widget = controlFor(setting);
        if (widget == null) return;
        if (setting.control == ReferenceSettingsCatalog.Control.TOGGLE || setting.isEditor()) {
            widget.x = x;
            widget.y = y;
            widget.w = TOGGLE_COLUMN_W;
            widget.h = ACTION_BUTTON_H;
            widget.draw(mouseX, mouseY);
            recordViewportControl("Settings toggle " + setting.id, widget.x, widget.y, widget.w, widget.h);
            registerHit("control:" + setting.id, HIT_CONTROL, widget.x, widget.y, widget.w, widget.h, setting, "");
            return;
        }
        if (setting.isKeybind()) {
            widget.x = x;
            widget.y = y;
            widget.w = w;
            widget.h = ACTION_BUTTON_H;
            widget.draw(mouseX, mouseY);
            recordViewportControl("Settings keybind " + setting.id, widget.x, widget.y, widget.w, widget.h);
            registerHit("control:" + setting.id, HIT_CONTROL, widget.x, widget.y, widget.w, widget.h, setting, "");
            return;
        }
        widget.x = x;
        widget.y = y;
        widget.w = w;
        widget.h = controlHeight(setting);
        widget.draw(mouseX, mouseY);
        recordViewportControl("Settings control " + setting.id, widget.x, widget.y, widget.w, widget.h);
        registerHit("control:" + setting.id, HIT_CONTROL, x, y, w, widget.h, setting, "");
    }

    /** Editor rows use the same fixed trailing action size, edge, and 20 px height as every inline action. */
    private void drawEditorButton(ReferenceSettingsCatalog.Setting setting, float x, float y, float w,
                                  int mouseX, int mouseY) {
        boolean hot = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + ACTION_BUTTON_H;
        int fill = hot ? UIRender.mix(Theme.buttonHover, Theme.accent, .16f) : Theme.buttonFill;
        int border = hot ? UIRender.withAlpha(Theme.accent, .76f) : Theme.border;
        UIRender.roundRectBorder(x, y, w, ACTION_BUTTON_H, 5f, fill, fill, border);
        String label = fit(fr(), editorActionLabel(setting), Math.max(1f, w - 18f) / EDITOR_BUTTON_TEXT_SCALE);
        UIRender.text(fr(), label, x + (w - fr().getStringWidth(label) * EDITOR_BUTTON_TEXT_SCALE) * .5f,
                centeredTextY(y, ACTION_BUTTON_H, EDITOR_BUTTON_TEXT_SCALE), hot ? Theme.textPrimary : Theme.textSecondary,
                EDITOR_BUTTON_TEXT_SCALE, false);
        registerHit("editor:" + setting.id, HIT_ACTION, x, y, w, ACTION_BUTTON_H, setting, setting.editorActionId);
    }

    private String editorActionLabel(ReferenceSettingsCatalog.Setting setting) {
        return setting.actionLabel.length() == 0 ? "Open Editor" : setting.actionLabel;
    }

    /** A fixed action width makes named editor routes line up with every other action across tabs. */
    private float editorButtonWidth(ReferenceSettingsCatalog.Setting setting) {
        return actionButtonWidth();
    }

    private String terminalStatusText() {
        String[] ids = {"terminal-hints", "terminal-auto-click", "terminal-hide-wrong", "terminal-prevent-clicks", "terminal-waypoints"};
        int on = 0;
        for (String id : ids) if (InlineSettingsBindings.booleanValue(id)) on++;
        return on + " / " + ids.length + " on";
    }

    private void drawDisclosureGlyph(float x, float y, boolean open, int color) {
        if (open) {
            UIRender.line(x - 3f, y - 1f, x, y + 2f, 1.1f, color);
            UIRender.line(x, y + 2f, x + 3f, y - 1f, 1.1f, color);
        } else {
            UIRender.line(x - 1f, y - 3f, x + 2f, y, 1.1f, color);
            UIRender.line(x + 2f, y, x - 1f, y + 3f, 1.1f, color);
        }
    }

    private Widget controlFor(final ReferenceSettingsCatalog.Setting setting) {
        Widget existing = controls.get(setting.id);
        if (existing != null) return existing;
        Widget created;
        if (setting.isKeybind()) {
            created = new AppleKeybindButton(keybindButtonWidth(), ACTION_BUTTON_H, new IntSupplier() {
                @Override public int getAsInt() { return keybindValue(setting.id); }
            });
        } else if (setting.control == ReferenceSettingsCatalog.Control.TOGGLE
                || (setting.isEditor() && setting.hasMasterToggle())) {
            created = new AppleToggle(new Supplier<Boolean>() {
                @Override public Boolean get() { return booleanValue(setting.id); }
            }, new Consumer<Boolean>() {
                @Override public void accept(Boolean value) { setBoolean(setting.id, Boolean.TRUE.equals(value)); }
            });
        } else if (setting.control == ReferenceSettingsCatalog.Control.SLIDER) {
            created = new AppleSlider(setting.min, setting.max, Math.max(1, setting.step), setting.suffix, 150f,
                    new IntSupplier() {
                        @Override public int getAsInt() { return intValue(setting.id); }
                    }, new IntConsumer() {
                        @Override public void accept(int value) { setInt(setting.id, value); }
                    }).fineStep(1);
        } else if (setting.control == ReferenceSettingsCatalog.Control.CHOICE) {
            created = new AppleDropdown(setting.choices, null, 150f, 22f,
                    new IntSupplier() {
                        @Override public int getAsInt() { return choiceValue(setting.id); }
                    }, new IntConsumer() {
                        @Override public void accept(int value) { setChoice(setting.id, value); }
                    });
        } else if (setting.control == ReferenceSettingsCatalog.Control.TEXT) {
            created = textControl(setting.id, setting.label);
        } else if (setting.control == ReferenceSettingsCatalog.Control.ACTION) {
            created = new AppleButton(setting.actionLabel.length() == 0 ? "Apply" : setting.actionLabel,
                    ACTION_BUTTON_W, ACTION_BUTTON_H, false, new Runnable() {
                        @Override public void run() { performInlineAction(setting.id); }
                    });
        } else {
            return null;
        }
        controls.put(setting.id, created);
        return created;
    }

    private SearchField textControl(String id, String label) {
        SearchField field = inlineTextControls.get(id);
        if (field == null) {
            field = new SearchField(label);
            inlineTextControls.put(id, field);
            syncInlineTextControl(id, field);
        }
        return field;
    }

    private int cachedLines(String id, String text, float maximum, float scale, int cap) {
        TextMeasure cached = textMeasures.get(id);
        if (cached == null) {
            cached = new TextMeasure();
            textMeasures.put(id, cached);
        }
        if (cached.text == text && Math.abs(cached.width - maximum) < .5f && cached.scale == scale && cached.cap == cap) {
            return cached.lines;
        }
        cached.text = text;
        cached.width = maximum;
        cached.scale = scale;
        cached.cap = cap;
        cached.lines = countWrappedLines(text, maximum, scale, cap);
        return cached.lines;
    }

    private int countWrappedLines(String text, float maximum, float scale, int cap) {
        if (text == null || text.length() == 0) return 1;
        float unscaled = Math.max(1f, maximum / Math.max(.1f, scale));
        int start = 0;
        int lines = 0;
        while (start < text.length() && lines < cap) {
            int end = fittingEnd(text, start, unscaled);
            if (end <= start) end = Math.min(text.length(), start + 1);
            start = skipSpaces(text, end);
            lines++;
        }
        return Math.max(1, lines);
    }

    private int drawWrapped(String text, float x, float y, float maximum, int color, float scale, int cap) {
        if (text == null || text.length() == 0) return 0;
        float unscaled = Math.max(1f, maximum / Math.max(.1f, scale));
        int start = 0;
        int lines = 0;
        while (start < text.length() && lines < cap) {
            int end = fittingEnd(text, start, unscaled);
            if (end <= start) end = Math.min(text.length(), start + 1);
            String line = text.substring(start, end).trim();
            if (end < text.length() && lines + 1 == cap) {
                String dots = "...";
                while (line.length() > 0 && fr().getStringWidth(line + dots) > unscaled) {
                    line = line.substring(0, line.length() - 1);
                }
                line += dots;
            }
            UIRender.text(fr(), line, x, y + lines * 8f, color, scale, false);
            start = skipSpaces(text, end);
            lines++;
        }
        return lines;
    }

    private int fittingEnd(String text, int start, float maximum) {
        int end = start;
        int lastSpace = -1;
        while (end < text.length()) {
            char c = text.charAt(end);
            if (Character.isWhitespace(c)) lastSpace = end;
            if (fr().getStringWidth(text.substring(start, end + 1)) > maximum) {
                return lastSpace > start ? lastSpace : end;
            }
            end++;
        }
        return end;
    }

    private int skipSpaces(String text, int value) {
        int next = value;
        while (next < text.length() && Character.isWhitespace(text.charAt(next))) next++;
        return next;
    }

    private void drawEmpty(float y, String message) {
        UIRender.roundRectBorder(contentX, y, contentW, 58f, 7f, Theme.card, Theme.card, Theme.border);
        drawSearchGlyph(contentX + contentW * .5f, y + 17f, Theme.textTertiary);
        UIRender.text(fr(), "No setting matches.", contentX + contentW * .5f - 35f, y + 29f, Theme.textPrimary, .75f, false);
        UIRender.text(fr(), fit(fr(), message, (contentW - 16f) / .63f), contentX + 8f, y + 41f, Theme.textSecondary, .63f, false);
    }

    private static final class TextMeasure {
        String text;
        float width;
        float scale;
        int cap;
        int lines;
    }

    private static final class Hit {
        final GuiLayoutToolkit.Rect rect = new GuiLayoutToolkit.Rect();
        int type;
        ReferenceSettingsCatalog.Setting setting;
        ReferenceSettingsCatalog.Match match;
        String data;
    }

    private void registerHit(String key, int type, float x, float y, float w, float h,
                             ReferenceSettingsCatalog.Setting setting, String data) {
        Hit hit = knownHits.get(key);
        if (hit == null) {
            hit = new Hit();
            knownHits.put(key, hit);
        }
        hit.type = type;
        hit.setting = setting;
        hit.match = null;
        hit.data = data == null ? "" : data;
        hit.rect.set(x, y, w, h);
        visibleHits.add(hit);
    }

    private void registerSearchHit(String key, ReferenceSettingsCatalog.Match match, float x, float y, float w, float h) {
        Hit hit = knownHits.get(key);
        if (hit == null) {
            hit = new Hit();
            knownHits.put(key, hit);
        }
        hit.type = HIT_SEARCH_RESULT;
        hit.setting = match == null ? null : match.setting;
        hit.match = match;
        hit.data = "";
        hit.rect.set(x, y, w, h);
        visibleHits.add(hit);
    }

    private Hit hitAt(int mouseX, int mouseY) {
        for (int i = visibleHits.size() - 1; i >= 0; i--) {
            Hit hit = visibleHits.get(i);
            if (hit.rect.contains(mouseX, mouseY)) return hit;
        }
        return null;
    }


    // ------------------------------------------------------------------ Home and search

    /**
     * Home is an at-a-glance surface, not a setup wizard. Its performance reader and fixed ring are touched only
     * while this open Home page is drawing, so samples remain capped at one per 500 ms with no render-frame growth.
     */
    private float drawHome(float y, int mouseX, int mouseY) {
        UIRender.text(fr(), "Sky VeLor, ready.", contentX, y + 1f, Theme.textPrimary, 1.34f, false);
        UIRender.text(fr(), "Live local performance and quick controls.", contentX, y + 18f,
                Theme.textSecondary, .72f, false);
        y += 37f;

        y = drawHomePerformance(y);
        y += GAP;

        UIRender.text(fr(), "QUICK CONTROLS", contentX, centeredTextY(y, 12f, .61f), Theme.textTertiary, .61f, false);
        String allQol = "all QoL ->";
        float allX = contentX + contentW - fr().getStringWidth(allQol) * .61f;
        UIRender.text(fr(), allQol, allX, centeredTextY(y, 12f, .61f), Theme.accent, .61f, false);
        registerHit("home-gameplay", HIT_HOME_GAMEPLAY, allX - 2f, y - 1f,
                fr().getStringWidth(allQol) * .61f + 4f, 14f, null, "");
        y += 13f;

        float gap = GuiStyle.SPACE_8;
        // These are static shell data, not arrays built in the render loop.
        int quickColumns = SettingsPageLayout.columnsFor(contentW);
        float quickW = SettingsPageLayout.itemWidth(contentW, quickColumns);
        float quickH = narrowRows ? 42f : 39f;
        for (int i = 0; i < HOME_QUICK_IDS.length; i++) {
            int row = i / quickColumns;
            int col = i % quickColumns;
            float x = contentX + col * (quickW + gap);
            float py = y + row * (quickH + gap);
            boolean checked = booleanValue(HOME_QUICK_IDS[i]);
            UIRender.roundRectBorder(x, py, quickW, quickH,
                    GuiStyle.radius(GuiStyle.RADIUS_CARD, Theme.radius, quickW, quickH), Theme.card, Theme.card, Theme.border);
            UIRender.roundRect(x + GuiStyle.SPACE_8 - 1f, py + (quickH - 20f) * .5f, 20f, 20f,
                    GuiStyle.SPACE_4, Theme.buttonFill, Theme.buttonFill, .2f);
            drawQuickGlyph(i, x + GuiStyle.SPACE_12, py + (quickH - 10f) * .5f, checked ? Theme.accent : Theme.textTertiary);
            float labelX = x + 33f;
            float toggleX = x + quickW - 43f;
            float textW = Math.max(1f, toggleX - labelX - GuiStyle.SPACE_4);
            UIRender.text(fr(), fit(fr(), HOME_QUICK_LABELS[i], textW / .75f), labelX, py + 7f, Theme.textPrimary, .75f, false);
            UIRender.text(fr(), fit(fr(), HOME_QUICK_DETAILS[i], textW / .58f), labelX, py + 20f,
                    Theme.textSecondary, .58f, false);
            drawMiniToggle(toggleX, py + (quickH - 17f) * .5f, checked);
            recordViewportControl("Home quick " + HOME_QUICK_IDS[i], x, py, quickW, quickH);
            registerHit("home-toggle:" + HOME_QUICK_IDS[i], HIT_HOME_TOGGLE, x, py, quickW, quickH, null, HOME_QUICK_IDS[i]);
        }
        y += SettingsPageLayout.gridHeight(HOME_QUICK_IDS.length, quickColumns, quickH) + GuiStyle.SPACE_12;

        float noticeH = 30f;
        UIRender.roundRectBorder(contentX, y, contentW, noticeH,
                GuiStyle.radius(GuiStyle.RADIUS_CARD, Theme.radius, contentW, noticeH), Theme.card, Theme.card, Theme.border);
        drawShieldGlyph(contentX + 10f, y + 9f, Theme.accent);
        String safety = "Safe by default — network sync and opt-in assists stay off until you choose them.";
        UIRender.text(fr(), fit(fr(), safety, Math.max(1f, (contentW - 31f) / .68f)), contentX + 26f,
                centeredTextY(y, noticeH, .68f), Theme.textSecondary, .68f, false);
        return y + noticeH + GAP + SettingsShellLayout.FINAL_CARD_PADDING;
    }

    private float drawHomePerformance(float y) {
        // snapshot() is called only by Home while this GuiScreen is painting. The reader refreshes at most twice a
        // second, and record() writes the fixed ring only when that cached generation changes.
        ClientMetrics.Snapshot snapshot = clientMetrics.snapshot(System.currentTimeMillis());
        homeMetricHistory.record(snapshot);
        float h = homePerformanceHeight();
        UIRender.roundRectBorder(contentX, y, contentW, h,
                GuiStyle.radius(GuiStyle.RADIUS_CARD, Theme.radius, contentW, h), Theme.card, Theme.card, Theme.border);
        UIRender.text(fr(), "LIVE PERFORMANCE", contentX + CARD_PAD, y + GuiStyle.SPACE_8, Theme.textTertiary, .61f, false);
        String cadence = "LOCAL · 0.5 s";
        UIRender.text(fr(), cadence, contentX + contentW - fr().getStringWidth(cadence) * .54f - CARD_PAD, y + GuiStyle.SPACE_8,
                Theme.textTertiary, .54f, false);

        float cardsX = contentX + CARD_PAD;
        float cardsY = y + 22f;
        float cardsW = Math.max(1f, contentW - CARD_PAD * 2f);
        HomeMetricsLayout.update(homeMetricsLayout, cardsX, cardsY, cardsW);
        drawHomeMetric(homeMetricsLayout.cards[HomeMetricsLayout.FPS], "FPS", snapshot.fpsValue(), "Frames per second",
                snapshot.fpsStatus(), snapshot.fpsColor(), HomeMetricHistory.FPS, metricRatio(HomeMetricHistory.FPS, snapshot));
        drawHomeMetric(homeMetricsLayout.cards[HomeMetricsLayout.MEMORY], "MEMORY", snapshot.memoryValue(), snapshot.memoryDetail(),
                snapshot.memoryStatus(), snapshot.memoryColor(), HomeMetricHistory.MEMORY, metricRatio(HomeMetricHistory.MEMORY, snapshot));
        drawHomeMetric(homeMetricsLayout.cards[HomeMetricsLayout.PING], "PING", snapshot.pingValue(), "Local tab-list response",
                snapshot.pingStatus(), snapshot.pingColor(), HomeMetricHistory.PING, metricRatio(HomeMetricHistory.PING, snapshot));
        return y + h;
    }

    /** Equal metric cards retain status text, a value bar and a history line even when the row stacks responsively. */
    private void drawHomeMetric(GuiLayoutToolkit.Rect bounds, String label, String value, String detail,
                                ClientMetrics.Status status, int valueColor, int historyMetric, float ratio) {
        float x = bounds.x;
        float y = bounds.y;
        float w = bounds.w;
        float h = bounds.h;
        UIRender.roundRectBorder(x, y, w, h,
                GuiStyle.radius(GuiStyle.RADIUS_SMALL, Theme.radius, w, h), Theme.buttonFill, Theme.buttonFill, Theme.border);
        UIRender.text(fr(), label, x + 7f, y + 6f, Theme.textTertiary, .52f, false);
        String statusLabel = status.label();
        float statusW = fr().getStringWidth(statusLabel) * .50f;
        UIRender.text(fr(), statusLabel, x + Math.max(7f, w - statusW - 7f), y + 6f, valueColor, .50f, false);
        float valueScale = w < 92f ? .78f : 1.08f;
        UIRender.text(fr(), value, x + 7f, y + 18f, valueColor, valueScale, false);
        UIRender.text(fr(), detail, x + 7f, y + 33f, Theme.textSecondary, .52f, false);
        float graphX = x + 7f;
        float graphW = Math.max(1f, w - 14f);
        drawHomeSparkline(graphX, y + h - 22f, graphW, 12f, historyMetric, valueColor);
        UIRender.roundRect(graphX, y + h - 6f, graphW, 2f, 1f, Theme.track, Theme.track, 0f);
        if (ratio >= 0f) UIRender.roundRect(graphX, y + h - 6f, Math.max(1f, graphW * ratio), 2f, 1f, valueColor, valueColor, 0f);
    }

    /** Draws directly from the fixed oldest-to-newest ring; no sample list or per-frame point array is made. */
    private void drawHomeSparkline(float x, float y, float w, float h, int metric, int color) {
        int count = homeMetricHistory.size();
        if (count < 2) return;
        float last = homeMetricHistory.normalizedAt(metric, 0);
        for (int i = 1; i < count; i++) {
            float next = homeMetricHistory.normalizedAt(metric, i);
            if (last >= 0f && next >= 0f) {
                float fromX = x + w * (i - 1) / (count - 1f);
                float toX = x + w * i / (count - 1f);
                UIRender.line(fromX, y + h * (1f - last), toX, y + h * (1f - next), 1f, color);
            }
            last = next;
        }
    }

    private float metricRatio(int metric, ClientMetrics.Snapshot snapshot) {
        if (metric == HomeMetricHistory.FPS) return snapshot.fps() < 0 ? -1f : Math.min(1f, snapshot.fps() / 60f);
        if (metric == HomeMetricHistory.MEMORY) return snapshot.maxBytes() <= 0L ? -1f : snapshot.memoryRatio();
        return snapshot.pingMs() < 0 ? -1f : Math.max(0f, 1f - Math.min(1f, snapshot.pingMs() / 200f));
    }

    private float homePerformanceHeight() {
        return 22f + HomeMetricsLayout.heightFor(Math.max(1f, contentW - CARD_PAD * 2f)) + CARD_PAD;
    }

    private float measureHome() {
        float quickH = narrowRows ? 42f : 39f;
        int quickColumns = SettingsPageLayout.columnsFor(contentW);
        return 37f + homePerformanceHeight() + GAP + 13f + SettingsPageLayout.gridHeight(HOME_QUICK_IDS.length, quickColumns, quickH)
                + GuiStyle.SPACE_12 + 30f + GAP + SettingsShellLayout.FINAL_CARD_PADDING;
    }

    private float drawSearchResults(float y, int mouseX, int mouseY) {
        List<ReferenceSettingsCatalog.Match> matches = ReferenceSettingsCatalog.search(search.text());
        UIRender.text(fr(), "Search", contentX, y + 1f, Theme.textPrimary, 1.26f, false);
        String subtitle = matches.size() + (matches.size() == 1 ? " result for \"" : " results for \"") + search.text() + "\"";
        UIRender.text(fr(), fit(fr(), subtitle, contentW / .68f), contentX, y + 17f, Theme.textSecondary, .68f, false);
        String clear = "Clear";
        float clearX = contentX + contentW - fr().getStringWidth(clear) * .7f;
        UIRender.text(fr(), clear, clearX, y + 12f, Theme.accent, .7f, false);
        registerHit("search-clear-content", HIT_SEARCH_CLEAR, clearX - 2f, y + 5f,
                fr().getStringWidth(clear) * .7f + 4f, 13f, null, "");
        y += 33f;
        if (matches.isEmpty()) {
            drawEmpty(y, "Try \"lock\", \"leap\", \"particles\", \"map\" or \"terminal\".");
            return y + 66f + SettingsShellLayout.FINAL_CARD_PADDING;
        }
        for (ReferenceSettingsCatalog.Match match : matches) {
            float h = measureSearchMatch(match);
            UIRender.roundRectBorder(contentX, y, contentW, h,
                    GuiStyle.radius(GuiStyle.RADIUS_CARD, Theme.radius, contentW, h), Theme.card, Theme.card, Theme.border);
            String owner = match.tab.label + "  >  " + match.card.title + searchParentSuffix(match);
            UIRender.text(fr(), fit(fr(), owner, (contentW - 20f) / .61f), contentX + 10f, y + 7f, Theme.textTertiary, .61f, false);
            UIRender.text(fr(), fit(fr(), match.setting.label, (contentW - 20f) / .86f), contentX + 10f, y + 20f,
                    Theme.textPrimary, .86f, false);
            UIRender.text(fr(), fit(fr(), match.setting.description, (contentW - 20f) / .61f), contentX + 10f, y + 32f,
                    Theme.textSecondary, .61f, false);
            registerSearchHit("search-result:" + match.setting.id + ":" + y, match, contentX, y, contentW, h);
            y += h + CARD_GAP;
        }
        return y + SettingsShellLayout.FINAL_CARD_PADDING;
    }

    private String searchParentSuffix(ReferenceSettingsCatalog.Match match) {
        if (match == null || match.parents.isEmpty()) return "";
        ReferenceSettingsCatalog.Setting parent = match.parents.get(match.parents.size() - 1);
        return "  >  " + parent.label;
    }

    private float measureSearchResults() {
        List<ReferenceSettingsCatalog.Match> matches = ReferenceSettingsCatalog.search(search.text());
        if (matches.isEmpty()) return 33f + 66f + SettingsShellLayout.FINAL_CARD_PADDING;
        float h = 33f;
        for (ReferenceSettingsCatalog.Match match : matches) h += measureSearchMatch(match) + CARD_GAP;
        return h + SettingsShellLayout.FINAL_CARD_PADDING;
    }

    private float measureSearchMatch(ReferenceSettingsCatalog.Match match) {
        return 49f;
    }

    // ------------------------------------------------------------------ Input and accessibility

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        if (button != 0) return;
        if (handleOpenDropdownClick(mouseX, mouseY)) {
            clearInlineTextFocus();
            afterSettingChanged();
            return;
        }
        Hit hit = hitAt(mouseX, mouseY);

        if (!panel.contains(mouseX, mouseY)) {
            close();
            return;
        }
        if (hit != null && (hit.type == HIT_WINDOW_FULLSCREEN || hit.type == HIT_WINDOW_CLOSE)) {
            headerPressedHit = hit.type;
            headerPressedUntilNs = System.nanoTime() + 120_000_000L;
            activateWindowControl(hit.type);
            return;
        }
        if (isTitleBar(mouseX, mouseY)) {
            clearInlineTextFocus();
            // Keep the pointer's grab point rather than snapping the window origin under it. The layout policy
            // performs the visible-world clamp, and every child coordinate will be rebuilt on the next draw.
            draggingWindow = true;
            windowGrabX = mouseX - panel.x;
            windowGrabY = mouseY - panel.y;
            search.setFocused(false);
            openDropdown = null;
            return;
        }
        if (search.contains(mouseX, mouseY)) {
            clearInlineTextFocus();
            search.setFocused(true);
            return;
        }
        if (hit != null && hit.type == HIT_SEARCH_CLEAR) {
            clearInlineTextFocus();
            search.clear();
            search.setFocused(true);
            return;
        }
        if (hit != null && hit.type == HIT_NAV) {
            clearInlineTextFocus();
            selectTab(hit.data);
            return;
        }
        search.setFocused(false);
        clearInlineTextFocus();
        if (!viewport.contains(mouseX, mouseY)) return;
        if (hit != null && hit.type == HIT_CONTROL && hit.setting != null && hit.setting.isKeybind()
                && !"keybind-toggle".equals(hit.data)) {
            beginKeybindCapture(hit.setting.id);
            return;
        }
        pendingHit = hit;
        activeWidget = hit != null && hit.type == HIT_CONTROL && hit.setting != null
                ? ("keybind-toggle".equals(hit.data) ? keybindToggleFor(hit.setting) : controlFor(hit.setting)) : null;
        if (activeWidget instanceof SearchField) {
            ((SearchField) activeWidget).setFocused(true);
            activeWidget = null;
            pendingHit = null;
            return;
        }
        gestureActive = true;
        sliderEngaged = false;
        gesture.begin(activeWidget, mouseX, mouseY, scroll.value());
        if (activeWidget != null && !(activeWidget instanceof AppleSlider)) {
            activeWidget.onPress(mouseX, mouseY);
        }
    }

    /** The clear part of the two-control header drags only the small window; fullscreen preserves that position. */
    private boolean isTitleBar(int mouseX, int mouseY) {
        return !fullscreenWindow && mouseX >= shellHeader.dragLeft && mouseX < shellHeader.dragRight
                && mouseY >= shellHeader.top && mouseY < shellHeader.top + shellHeader.height;
    }

    private void activateWindowControl(int type) {
        if (type == HIT_WINDOW_CLOSE) {
            close();
            return;
        }
        if (type == HIT_WINDOW_FULLSCREEN) {
            if (fullscreenWindow) {
                fullscreenWindow = false;
                windowOffsetX = smallWindowOffsetX;
                windowOffsetY = smallWindowOffsetY;
                showToast("Small layout");
            } else {
                // Keep the actual constrained small-window offset, not a raw drag request that could sit off-screen.
                smallWindowOffsetX = windowOffsetX;
                smallWindowOffsetY = windowOffsetY;
                fullscreenWindow = true;
                showToast("Fullscreen");
            }
            AppleSettings.settingsWindowFullscreen = fullscreenWindow;
            search.setFocused(false);
            openDropdown = null;
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long timeSinceLastClick) {
        if (draggingWindow && button == 0 && !fullscreenWindow) {
            windowOffsetX = mouseX - windowGrabX - ReferenceSettingsWindowLayout.centeredX(width, panel.w);
            windowOffsetY = mouseY - windowGrabY - ReferenceSettingsWindowLayout.centeredY(height, panel.h);
            refreshLayout();
            smallWindowOffsetX = windowOffsetX;
            smallWindowOffsetY = windowOffsetY;
            return;
        }
        if (!gestureActive) return;
        float speed = AppleSettings.guiScrollMultiplier();
        int kind = gesture.move(mouseX, mouseY);
        if (kind == Gesture.SLIDER && activeWidget != null) {
            if (!sliderEngaged) {
                sliderEngaged = true;
                activeWidget.onPress((int) gesture.startX, (int) gesture.startY);
            }
            activeWidget.onDrag(mouseX, mouseY);
        } else if (kind == Gesture.SCROLL) {
            if (activeWidget != null) {
                activeWidget.onRelease(mouseX, mouseY, false);
                activeWidget = null;
            }
            scroll.setImmediate(gesture.scrollAt(mouseY, speed));
            gesture.track(mouseY, speed);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int button) {
        if (draggingWindow) {
            draggingWindow = false;
            return;
        }
        if (!gestureActive) return;
        boolean tap = gesture.kind == Gesture.PENDING;
        if (activeWidget != null) {
            Widget widget = activeWidget;
            activeWidget = null;
            if (widget instanceof AppleSlider && tap) {
                widget.onPress((int) gesture.startX, (int) gesture.startY);
            }
            widget.onRelease(mouseX, mouseY, tap && widget.contains(mouseX, mouseY));
            if (tap && widget instanceof AppleDropdown) {
                AppleDropdown dropdown = (AppleDropdown) widget;
                openDropdown = openDropdown == dropdown ? null : dropdown;
                if (openDropdown != null) dropdown.onExpand();
            }
            if (tap) afterSettingChanged();
        } else if (tap && pendingHit != null && pendingHit.rect.contains(mouseX, mouseY)) {
            activateHit(pendingHit);
        }
        pendingHit = null;
        gestureActive = false;
        sliderEngaged = false;
        gesture.target = null;
        gesture.kind = Gesture.PENDING;
    }

    @Override
    public void handleMouseInput() throws IOException {
        int wheel = Mouse.getEventDWheel();
        super.handleMouseInput();
        int mouseX = Mouse.getEventX() * width / Math.max(1, Minecraft.getMinecraft().displayWidth);
        int mouseY = height - Mouse.getEventY() * height / Math.max(1, Minecraft.getMinecraft().displayHeight) - 1;
        if (wheel != 0 && openDropdown == null && viewport.contains(mouseX, mouseY)) {
            boolean fine = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
            scroll.wheel(wheel / 120f, fine, AppleSettings.guiScrollMultiplier(), WHEEL_STEP);
        }
    }

    private int keybindValue(String id) {
        if ("item-lock-key".equals(id)) return PekaClientMod.ITEM_LOCK_SWAP_KEY.getKeyCode();
        if ("wardrobe-shortcut".equals(id)) return PekaClientMod.WARDROBE_SHORTCUT_KEY.getKeyCode();
        if ("pets-shortcut".equals(id)) return PekaClientMod.PETS_SHORTCUT_KEY.getKeyCode();
        return 0;
    }

    /** Returns a compact persistent warning for a key that overlaps vanilla Controls or another Sky VeLor binding. */
    private String keybindCollisionWarning(String id) {
        int keyCode = keybindValue(id);
        if (keyCode <= 0) return "";
        KeyBinding own = keybindForId(id);
        KeyBinding[] skyVelor = {PekaClientMod.OPEN_KEY, PekaClientMod.ITEM_ANIMATION_KEY, PekaClientMod.LEAP_KEY,
                PekaClientMod.CHAT_TAB_KEY, PekaClientMod.CHAT_EDITOR_KEY, PekaClientMod.ITEM_LOCK_SWAP_KEY,
                PekaClientMod.WARDROBE_SHORTCUT_KEY, PekaClientMod.PETS_SHORTCUT_KEY};
        String[] skyVelorNames = {"Open Menu", "Item Animation", "Spirit Leap", "Chat Tab", "Chat Settings",
                "Item Lock And Swap", "Wardrobe Shortcut", "Pets Shortcut"};
        for (int i = 0; i < skyVelor.length; i++) {
            if (skyVelor[i] != null && skyVelor[i] != own && skyVelor[i].getKeyCode() == keyCode) {
                return "Key conflict: Sky VeLor " + skyVelorNames[i];
            }
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.gameSettings != null && mc.gameSettings.keyBindings != null) {
            for (int i = 0; i < mc.gameSettings.keyBindings.length; i++) {
                KeyBinding binding = mc.gameSettings.keyBindings[i];
                if (binding != null && binding != own && binding.getKeyCode() == keyCode) {
                    return "Key conflict: Minecraft " + binding.getKeyDescription();
                }
            }
        }
        return "";
    }

    private void drawKeybindConflictWarning(ReferenceSettingsCatalog.Setting setting, float x, float y, float w) {
        String warning = keybindCollisionWarning(setting == null ? null : setting.id);
        if (warning.length() == 0) return;
        UIRender.text(fr(), fit(fr(), warning, Math.max(12f, w) / KEYBIND_WARNING_SCALE), x, y,
                Theme.STATUS_CRITICAL, KEYBIND_WARNING_SCALE, false);
    }

    private static KeyBinding keybindForId(String id) {
        if ("item-lock-key".equals(id)) return PekaClientMod.ITEM_LOCK_SWAP_KEY;
        if ("wardrobe-shortcut".equals(id)) return PekaClientMod.WARDROBE_SHORTCUT_KEY;
        if ("pets-shortcut".equals(id)) return PekaClientMod.PETS_SHORTCUT_KEY;
        return null;
    }

    private void beginKeybindCapture(String id) {
        if (id == null) return;
        cancelKeybindCapture();
        Widget widget = controls.get(id);
        if (!(widget instanceof AppleKeybindButton)) widget = controlFor(ReferenceSettingsCatalog.find(id));
        if (!(widget instanceof AppleKeybindButton)) return;
        keybindCaptureId = id;
        ((AppleKeybindButton) widget).beginCapture();
        showToast("Press a key. Esc cancels; Backspace clears.");
    }

    private void cancelKeybindCapture() {
        if (keybindCaptureId != null) {
            Widget widget = controls.get(keybindCaptureId);
            if (widget instanceof AppleKeybindButton) ((AppleKeybindButton) widget).cancelCapture();
        }
        keybindCaptureId = null;
    }

    private void handleKeybindCapture(int keyCode) {
        String id = keybindCaptureId;
        if (keyCode == Keyboard.KEY_ESCAPE) {
            cancelKeybindCapture();
            showToast("Key capture canceled");
            return;
        }
        if (keyCode == Keyboard.KEY_BACK) {
            applyKeybindCapture(id, 0);
            return;
        }
        if (keyCode <= 0 || keyCode > 255) return;
        applyKeybindCapture(id, keyCode);
    }

    private void applyKeybindCapture(String id, int keyCode) {
        String conflict = null;
        if ("item-lock-key".equals(id)) conflict = PekaClientMod.rebindItemLockSwapKey(keyCode);
        else if ("wardrobe-shortcut".equals(id)) conflict = PekaClientMod.rebindWardrobeShortcutKey(keyCode);
        else if ("pets-shortcut".equals(id)) conflict = PekaClientMod.rebindPetsShortcutKey(keyCode);
        else return;
        Widget widget = controls.get(id);
        if (widget instanceof AppleKeybindButton) ((AppleKeybindButton) widget).finishCapture();
        keybindCaptureId = null;
        AppleSettings.save();
        if (keyCode == 0) {
            showToast("Key cleared");
        } else if (conflict != null) {
            showToast("Key set; also bound to " + conflict);
        } else {
            showToast("Key set");
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keybindCaptureId != null) {
            handleKeybindCapture(keyCode);
            return;
        }
        SearchField inlineFocused = focusedInlineText();
        if (inlineFocused != null) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                inlineFocused.setFocused(false);
                return;
            }
            if (inlineFocused.keyTyped(typedChar, keyCode)) return;
        }
        if (search.isFocused()) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                if (!search.isEmpty()) search.clear();
                else search.setFocused(false);
                return;
            }
            if (search.keyTyped(typedChar, keyCode)) {
                scroll.reset();
                return;
            }
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (openDropdown != null) {
                openDropdown = null;
                return;
            }
            close();
            return;
        }
        if (isCtrlKeyDown() && keyCode == Keyboard.KEY_F) {
            search.setFocused(true);
            return;
        }
        if (keyCode == Keyboard.KEY_SLASH) {
            search.setFocused(true);
            return;
        }
        if (handleScrollKey(keyCode)) return;
        if (keyCode == Keyboard.KEY_LEFT) {
            moveTab(-1);
            return;
        }
        if (keyCode == Keyboard.KEY_RIGHT) {
            moveTab(1);
            return;
        }
        if (typedChar >= 32 && typedChar != 127) {
            search.setFocused(true);
            search.keyTyped(typedChar, keyCode);
            scroll.reset();
        }
    }

    private boolean handleScrollKey(int code) {
        float page = Math.max(36f, viewport.h * .8f);
        if (code == Keyboard.KEY_DOWN) {
            scroll.nudge(40f);
            return true;
        }
        if (code == Keyboard.KEY_UP) {
            scroll.nudge(-40f);
            return true;
        }
        if (code == Keyboard.KEY_NEXT) {
            scroll.nudge(page);
            return true;
        }
        if (code == Keyboard.KEY_PRIOR) {
            scroll.nudge(-page);
            return true;
        }
        if (code == Keyboard.KEY_HOME) {
            scroll.setImmediate(0f);
            return true;
        }
        if (code == Keyboard.KEY_END) {
            scroll.setImmediate(scroll.maximum());
            return true;
        }
        return false;
    }

    private void activateHit(Hit hit) {
        if (hit == null) return;
        if (hit.type == HIT_DISCLOSURE && hit.setting != null) {
            boolean open = !FeatureUiPreferences.dropdownOpen(hit.setting.id);
            setAccordionOpen(activeTab, hit.setting.id, open);
            if (open) {
                revealDropdownHeader(hit.setting.id);
                showToast(hit.setting.label + " expanded");
            }
            return;
        }
        if (hit.type == HIT_SEARCH_RESULT && hit.match != null) {
            expandSearchResult(hit.match);
            return;
        }
        if (hit.type == HIT_ACTION) {
            String id = hit.data == null || hit.data.length() == 0
                    ? (hit.setting == null ? "" : hit.setting.id) : hit.data;
            if (id.length() > 0) performInlineAction(id);
            afterSettingChanged();
            return;
        }
        if (hit.type == HIT_HOME_TOGGLE) {
            setBoolean(hit.data, !booleanValue(hit.data));
            afterSettingChanged();
            return;
        }
        if (hit.type == HIT_HOME_GAMEPLAY) {
            activeTab = ReferenceSettingsCatalog.Tab.GAMEPLAY;
            scroll.reset();
        }
    }

    /** Opens one disclosure per page and keeps the persisted state as presentation-only data. */
    private void setAccordionOpen(ReferenceSettingsCatalog.Tab tab, String id, boolean open) {
        if (tab == null || id == null) return;
        if (open) FeatureUiPreferences.openAccordion(id, ReferenceSettingsCatalog.dropdownIds(tab));
        else FeatureUiPreferences.setDropdownOpen(id, false);
    }

    /** Uses the last rendered header bounds so an opened row never sits outside the scroll viewport. */
    private void revealDropdownHeader(String id) {
        Hit hit = knownHits.get("dropdown:" + id);
        if (hit == null) return;
        float delta = 0f;
        if (hit.rect.y < viewport.y) delta = hit.rect.y - viewport.y;
        else if (hit.rect.bottom() > viewport.bottom()) delta = hit.rect.bottom() - viewport.bottom();
        if (Math.abs(delta) > .5f) scroll.nudge(delta);
    }

    private void expandSearchResult(ReferenceSettingsCatalog.Match match) {
        if (match == null) return;
        activeTab = match.tab;
        for (ReferenceSettingsCatalog.Setting parent : match.parents) {
            if (parent.isDropdown()) setAccordionOpen(match.tab, parent.id, true);
        }
        if (match.setting.isDropdown()) setAccordionOpen(match.tab, match.setting.id, true);
        search.clear();
        search.setFocused(false);
        scroll.reset();
        revealDropdownHeader(match.parents.isEmpty() ? match.setting.id : match.parents.get(0).id);
        showToast(match.setting.label + " opened");
    }

    private void clearInlineTextFocus() {
        boolean changedFocus = false;
        for (SearchField field : inlineTextControls.values()) {
            changedFocus |= field.isFocused();
            field.setFocused(false);
        }
        // Palette text fields write their existing owners as soon as the user leaves the field; malformed hex stays
        // visibly editable and leaves the last valid configuration value intact.
        if (changedFocus) commitInlineTextFields();
    }

    private SearchField focusedInlineText() {
        for (SearchField field : inlineTextControls.values()) if (field.isFocused()) return field;
        return null;
    }

    private void selectTab(String tabName) {
        try {
            activeTab = ReferenceSettingsCatalog.Tab.valueOf(tabName);
            search.clear();
            search.setFocused(false);
            scroll.reset();
        } catch (IllegalArgumentException ignored) {
            activeTab = ReferenceSettingsCatalog.Tab.HOME;
        }
    }

    private void moveTab(int direction) {
        ReferenceSettingsCatalog.Tab[] values = ReferenceSettingsCatalog.TABS;
        int index = activeTab.ordinal();
        index = Math.max(0, Math.min(values.length - 1, index + direction));
        if (values[index] != activeTab) {
            activeTab = values[index];
            search.clear();
            scroll.reset();
        }
    }

    private void close() {
        Minecraft.getMinecraft().displayGuiScreen(parent);
    }

    private void afterSettingChanged() {
        refreshReferenceTheme();
    }

    private void showToast(String value) {
        toast = value == null ? "" : value;
        toastUntilNs = System.nanoTime() + 2_200_000_000L;
    }

    private void drawToast() {
        if (toast.length() == 0 || System.nanoTime() >= toastUntilNs) return;
        float w = Math.min(panel.w - 24f, Math.max(110f, fr().getStringWidth(toast) * .72f + 24f));
        float x = panel.x + (panel.w - w) * .5f;
        float y = panel.bottom() - 28f;
        UIRender.shadow(x, y, w, 19f, 5f);
        UIRender.roundRectBorder(x, y, w, 19f, 5f, Theme.popup, Theme.popup, Theme.border);
        drawCheckGlyph(x + 9f, y + 10f, Theme.accent);
        UIRender.text(fr(), fit(fr(), toast, (w - 24f) / .72f), x + 16f, centeredTextY(y, 19f, .72f),
                Theme.textPrimary, .72f, false);
    }

    // ------------------------------------------------------------------ Existing setting owners

    private boolean booleanValue(String id) {
        if ("chat-tab-visible".equals(id)) {
            ChatTabs.Tab tab = currentChatTab();
            return tab != null && tab.enabled();
        }
        if ("chat-tab-sound".equals(id)) return draftChatSound;
        return InlineSettingsBindings.booleanValue(id);
    }

    private void setBoolean(String id, boolean value) {
        if ("chat-tab-visible".equals(id)) {
            ChatTabs.Tab tab = currentChatTab();
            if (tab != null) ChatQolSettings.setTabVisible(tab.id(), value);
            return;
        }
        if ("chat-tab-sound".equals(id)) {
            draftChatSound = value;
            return;
        }
        InlineSettingsBindings.setBoolean(id, value);
        if (("chat-qol".equals(id) || "chat-tabs".equals(id)) && !value) ChatChannelTabs.disableAndRestoreAll();
    }

    private int intValue(String id) {
        return InlineSettingsBindings.intValue(id);
    }

    private void setInt(String id, int value) {
        InlineSettingsBindings.setInt(id, value);
        refreshReferenceTheme();
    }

    private int choiceValue(String id) {
        if ("chat-tab-filter-type".equals(id)) return draftChatFilterType;
        return InlineSettingsBindings.choiceValue(id);
    }

    private void setChoice(String id, int value) {
        if ("chat-tab-filter-type".equals(id)) {
            draftChatFilterType = ChatTabs.normalizeFilterType(value);
            return;
        }
        InlineSettingsBindings.setChoice(id, value);
        refreshReferenceTheme();
    }

    /** Executes a retained value action; named editor destinations are the only actions that open a child screen. */
    private void performInlineAction(String id) {
        if ("open-hud-editor".equals(id)) {
            Minecraft.getMinecraft().displayGuiScreen(new HudEditorScreen(this));
            return;
        }
        if ("item-animation-open-editor".equals(id)) {
            Minecraft.getMinecraft().displayGuiScreen(new ItemAnimationEditorScreen(this));
            return;
        }
        if ("open-command-keybind-editor".equals(id)) {
            Minecraft.getMinecraft().displayGuiScreen(new CommandKeybindEditorScreen(this));
            return;
        }
        if (handleChatAction(id)) return;
        if (InlineSettingsBindings.runAction(id)) {
            // Shortcut reset deliberately restores W/P only after this explicit player action, never while loading an
            // established config that merely lacks the newer key fields.
            if ("shortcut-reset".equals(id)) PekaClientMod.resetShortcutKeysToDefaults();
            showToast("Applied " + actionToastLabel(id));
            return;
        }
        showToast("This option is informational");
    }

    private String actionToastLabel(String id) {
        ReferenceSettingsCatalog.Setting setting = ReferenceSettingsCatalog.find(id);
        return setting == null ? "setting" : setting.label;
    }

    private ChatTabs.Tab currentChatTab() {
        ChatTabs.Tab tab = ChatQolSettings.tab(editingChatTabId);
        if (tab == null) {
            editingChatTabId = ChatTabs.ALL;
            tab = ChatQolSettings.tab(editingChatTabId);
        }
        return tab;
    }

    private boolean handleChatAction(String id) {
        if (id == null || !id.startsWith("chat-")) return false;
        if ("chat-tab-previous".equals(id)) {
            cycleChatTab(-1);
            return true;
        }
        if ("chat-tab-next".equals(id)) {
            cycleChatTab(1);
            return true;
        }
        if ("chat-tab-add".equals(id)) {
            ChatQolSettings.Result result = ChatQolSettings.addCustomTab("Custom tab");
            if (result.success) editingChatTabId = result.id;
            syncChatTextControls();
            showToast(result.message);
            return true;
        }
        if ("chat-tab-save".equals(id)) {
            saveChatTab();
            return true;
        }
        if ("chat-tab-left".equals(id)) {
            ChatQolSettings.moveTab(editingChatTabId, -1);
            syncChatTextControls();
            showToast("Tab moved left");
            return true;
        }
        if ("chat-tab-right".equals(id)) {
            ChatQolSettings.moveTab(editingChatTabId, 1);
            syncChatTextControls();
            showToast("Tab moved right");
            return true;
        }
        if ("chat-tab-delete".equals(id)) {
            ChatTabs.Tab tab = currentChatTab();
            if (tab == null || tab.builtIn()) {
                showToast("Built-in tabs cannot be deleted");
            } else {
                ChatQolSettings.deleteCustomTab(tab.id());
                editingChatTabId = ChatTabs.ALL;
                syncChatTextControls();
                showToast("Custom tab deleted");
            }
            return true;
        }
        if ("chat-reset-all".equals(id)) {
            ChatQolSettings.resetDefaults();
            editingChatTabId = ChatTabs.ALL;
            syncChatTextControls();
            showToast("Chat QoL reset");
            return true;
        }
        if ("chat-reset-background".equals(id)) {
            ChatQolSettings.resetBackground();
            syncChatTextControls();
            showToast("Chat background reset");
            return true;
        }
        if ("chat-reset-messages".equals(id)) {
            ChatQolSettings.resetMessages();
            syncChatTextControls();
            showToast("Chat messages reset");
            return true;
        }
        if ("chat-reset-tabs".equals(id)) {
            ChatQolSettings.resetTabs();
            editingChatTabId = ChatTabs.ALL;
            syncChatTextControls();
            showToast("Chat tabs reset");
            return true;
        }
        if ("chat-reset-menu".equals(id)) {
            ChatQolSettings.resetMenuHighlight();
            syncChatTextControls();
            showToast("Chat menu reset");
            return true;
        }
        return false;
    }

    private void cycleChatTab(int direction) {
        java.util.List<ChatTabs.Tab> tabs = ChatQolSettings.tabs();
        if (tabs.isEmpty()) return;
        int at = 0;
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).id().equals(editingChatTabId)) { at = i; break; }
        }
        int next = (at + direction) % tabs.size();
        if (next < 0) next += tabs.size();
        editingChatTabId = tabs.get(next).id();
        syncChatTextControls();
        showToast("Editing " + tabs.get(next).name());
    }

    private void saveChatTab() {
        commitInlineTextFields();
        ChatTabs.Tab tab = currentChatTab();
        if (tab == null) return;
        if (tab.builtIn()) {
            ChatQolSettings.setTabVisible(tab.id(), booleanValue("chat-tab-visible"));
            showToast(tab.name() + " visibility saved");
            return;
        }
        Integer colour = parseColour(textValue("chat-tab-accent-color"));
        if (colour == null) {
            showToast("Enter a valid tab colour");
            return;
        }
        ChatQolSettings.Result result = ChatQolSettings.updateCustomTab(tab.id(), textValue("chat-tab-name"),
                draftChatFilterType, textValue("chat-tab-filter-text"), textValue("chat-tab-send-prefix"),
                colour.intValue(), draftChatSound, booleanValue("chat-tab-visible"));
        if (result.success) syncChatTextControls();
        showToast(result.message);
    }

    private String textValue(String id) {
        SearchField field = inlineTextControls.get(id);
        return field == null ? "" : field.text();
    }

    private void syncInlineTextControl(String id, SearchField field) {
        if (field == null || field.isFocused()) return;
        if ("chat-background-color".equals(id)) field.setText(colourText(ChatQolSettings.backgroundColor));
        else if ("chat-tab-inactive-color".equals(id)) field.setText(colourText(ChatQolSettings.tabInactiveColor));
        else if ("chat-tab-selected-color".equals(id)) field.setText(colourText(ChatQolSettings.tabBackgroundColor));
        else if ("chat-compact-counter-color".equals(id)) field.setText(colourText(ChatQolSettings.compactCounterColor));
        else if ("chat-hover-color".equals(id)) field.setText(colourText(ChatQolSettings.hoverColor));
        else {
            ChatTabs.Tab tab = currentChatTab();
            if (tab == null) return;
            if ("chat-tab-name".equals(id)) field.setText(tab.name());
            else if ("chat-tab-filter-text".equals(id)) field.setText(tab.filterText());
            else if ("chat-tab-send-prefix".equals(id)) field.setText(tab.sendPrefix());
            else if ("chat-tab-accent-color".equals(id)) field.setText(colourText(tab.colour()));
        }
    }

    private void syncChatTextControls() {
        ChatTabs.Tab tab = currentChatTab();
        if (tab != null) {
            draftChatFilterType = tab.filterType();
            draftChatSound = tab.sound();
        }
        for (java.util.Map.Entry<String, SearchField> entry : inlineTextControls.entrySet()) {
            syncInlineTextControl(entry.getKey(), entry.getValue());
        }
    }

    private void commitInlineTextFields() {
        commitColour("chat-background-color", new java.util.function.IntConsumer() {
            @Override public void accept(int value) { ChatQolSettings.backgroundColor = value; }
        });
        commitColour("chat-tab-inactive-color", new java.util.function.IntConsumer() {
            @Override public void accept(int value) { ChatQolSettings.tabInactiveColor = value; }
        });
        commitColour("chat-tab-selected-color", new java.util.function.IntConsumer() {
            @Override public void accept(int value) { ChatQolSettings.tabBackgroundColor = value; }
        });
        commitColour("chat-compact-counter-color", new java.util.function.IntConsumer() {
            @Override public void accept(int value) { ChatQolSettings.compactCounterColor = value; }
        });
        commitColour("chat-hover-color", new java.util.function.IntConsumer() {
            @Override public void accept(int value) { ChatQolSettings.hoverColor = value; }
        });
    }

    private void commitColour(String id, java.util.function.IntConsumer setter) {
        SearchField field = inlineTextControls.get(id);
        if (field == null || setter == null) return;
        Integer color = parseColour(field.text());
        if (color != null) setter.accept(color.intValue());
    }

    private static String colourText(int color) {
        return String.format(java.util.Locale.ROOT, "#%06X", color & RGB_MASK);
    }

    private static Integer parseColour(String value) {
        if (value == null) return null;
        String raw = value.trim();
        if (raw.startsWith("#")) raw = raw.substring(1);
        if (raw.length() != 6) return null;
        try {
            return Integer.valueOf(Integer.parseInt(raw, 16));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    // ------------------------------------------------------------------ Reference line-art vocabulary

    private void drawRailGlyph(ReferenceSettingsCatalog.Tab tab, float x, float y, int color) {
        if (tab == ReferenceSettingsCatalog.Tab.HOME) {
            UIRender.line(x, y + 6f, x + 6f, y + 1f, 1.2f, color);
            UIRender.line(x + 6f, y + 1f, x + 12f, y + 6f, 1.2f, color);
            UIRender.rect(x + 2f, y + 6f, 8f, 6f, color);
        } else if (tab == ReferenceSettingsCatalog.Tab.GAMEPLAY) {
            UIRender.ring(x + 6f, y + 6f, 5f, 3.6f, color);
            UIRender.line(x + 6f, y + 6f, x + 10f, y + 3f, 1.2f, color);
        } else if (tab == ReferenceSettingsCatalog.Tab.SLAYER) {
            // Compact sword silhouette: a distinct channel glyph without a texture allocation.
            UIRender.line(x + 2f, y + 10f, x + 10f, y + 2f, 1.4f, color);
            UIRender.line(x + 6f, y + 3f, x + 10f, y + 7f, 1.1f, color);
            UIRender.line(x + 1f, y + 11f, x + 4f, y + 8f, 1.2f, color);
        } else if (tab == ReferenceSettingsCatalog.Tab.DUNGEON) {
            UIRender.rect(x + 1f, y + 2f, 10f, 8f, UIRender.withAlpha(color, .38f));
            UIRender.roundRectRing(x + 1f, y + 2f, 10f, 8f, 1f, GuiStyle.OUTLINE, color);
            UIRender.line(x + 3f, y + 11f, x + 9f, y + 11f, 1f, color);
        } else if (tab == ReferenceSettingsCatalog.Tab.FARMING) {
            // Wheat/sprout: a stem plus two opposite leaves, recognizable without allocating a texture.
            UIRender.line(x + 6f, y + 12f, x + 6f, y + 4f, 1.2f, color);
            UIRender.line(x + 6f, y + 7f, x + 2f, y + 4f, 1.2f, color);
            UIRender.line(x + 6f, y + 5f, x + 10f, y + 2f, 1.2f, color);
            UIRender.circle(x + 2f, y + 4f, 1.7f, UIRender.withAlpha(color, .75f));
            UIRender.circle(x + 10f, y + 2f, 1.7f, UIRender.withAlpha(color, .75f));
        } else if (tab == ReferenceSettingsCatalog.Tab.MINING) {
            UIRender.line(x + 2f, y + 2f, x + 10f, y + 10f, 1.3f, color);
            UIRender.line(x + 8f, y + 2f, x + 11f, y + 5f, 1.3f, color);
            UIRender.line(x + 2f, y + 8f, x + 5f, y + 11f, 1.3f, color);
        } else if (tab == ReferenceSettingsCatalog.Tab.FISHING) {
            UIRender.line(x + 6f, y + 1f, x + 6f, y + 9f, 1.1f, color);
            UIRender.line(x + 6f, y + 2f, x + 10f, y + 4f, 1.1f, color);
            UIRender.ring(x + 6f, y + 10f, 2.7f, 1.7f, color);
        } else if (tab == ReferenceSettingsCatalog.Tab.VISUALS) {
            UIRender.roundRectRing(x, y + 1f, 12f, 9f, 1f, GuiStyle.OUTLINE, color);
            UIRender.line(x + 4f, y + 12f, x + 8f, y + 12f, 1f, color);
        } else {
            UIRender.circle(x + 6f, y + 6f, 2f, color);
            UIRender.ring(x + 6f, y + 6f, 5f, 3.7f, color);
            UIRender.line(x + 6f, y, x + 6f, y + 2f, 1f, color);
            UIRender.line(x + 6f, y + 10f, x + 6f, y + 12f, 1f, color);
        }
    }

    private void drawCardGlyph(String id, float x, float y, int color) {
        if (id.indexOf("inventory") >= 0 || id.indexOf("hud") >= 0) {
            UIRender.roundRectRing(x, y, 11f, 10f, 1f, 1f, color);
            UIRender.line(x + 4f, y, x + 4f, y + 10f, 1f, color);
            UIRender.line(x + 7f, y, x + 7f, y + 10f, 1f, color);
            UIRender.line(x, y + 5f, x + 11f, y + 5f, 1f, color);
        } else if (id.indexOf("dungeon") >= 0 || id.indexOf("floor") >= 0 || id.indexOf("run") >= 0) {
            UIRender.roundRectRing(x + 1f, y + 1f, 9f, 9f, 1f, 1f, color);
            UIRender.line(x + 5.5f, y, x + 5.5f, y + 12f, 1f, color);
            UIRender.line(x, y + 5.5f, x + 12f, y + 5.5f, 1f, color);
        } else if (id.indexOf("performance") >= 0 || id.indexOf("skills") >= 0) {
            UIRender.ring(x + 6f, y + 6f, 5f, 3.8f, color);
            UIRender.line(x + 6f, y + 6f, x + 10f, y + 4f, 1.2f, color);
        } else if (id.indexOf("appearance") >= 0) {
            UIRender.circle(x + 6f, y + 6f, 4f, UIRender.withAlpha(color, .4f));
            UIRender.ring(x + 6f, y + 6f, 5.5f, 4.2f, color);
        } else if (id.indexOf("chat") >= 0) {
            UIRender.roundRectRing(x, y + 1f, 12f, 8f, 2f, 1f, color);
            UIRender.triangle(x + 3f, y + 9f, x + 5f, y + 9f, x + 3f, y + 11f, color);
        } else if (id.indexOf("item") >= 0) {
            UIRender.line(x + 2f, y + 10f, x + 10f, y + 2f, 2f, color);
            UIRender.line(x + 4f, y + 2f, x + 10f, y + 8f, 1f, color);
        } else if (id.indexOf("highlight") >= 0) {
            UIRender.roundRectRing(x + 1f, y + 1f, 10f, 10f, 1f, 1f, color);
            UIRender.line(x + 3f, y + 3f, x + 9f, y + 9f, 1f, color);
        } else {
            UIRender.ring(x + 6f, y + 6f, 5f, 3.8f, color);
            UIRender.circle(x + 6f, y + 6f, 1.4f, color);
        }
    }

    private void drawQuickGlyph(int index, float x, float y, int color) {
        if (index == 0) {
            UIRender.line(x + 4f, y, x + 1f, y + 5f, 1.3f, color);
            UIRender.line(x + 1f, y + 5f, x + 5f, y + 5f, 1.3f, color);
            UIRender.line(x + 5f, y + 5f, x + 2f, y + 10f, 1.3f, color);
        } else if (index == 1) {
            UIRender.line(x, y + 8f, x + 8f, y, 1.5f, color);
            UIRender.line(x + 3f, y, x + 8f, y + 5f, 1f, color);
        } else if (index == 2) {
            UIRender.roundRectRing(x, y + 1f, 10f, 7f, 2f, 1f, color);
            UIRender.triangle(x + 2f, y + 8f, x + 4f, y + 8f, x + 2f, y + 10f, color);
        } else {
            UIRender.circle(x + 5f, y + 5f, 3f, UIRender.withAlpha(color, .3f));
            UIRender.ring(x + 5f, y + 5f, 4.5f, 3.1f, color);
        }
    }

    private void drawMiniToggle(float x, float y, boolean on) {
        int fill = on ? Theme.accent : Theme.track;
        float knobX = x + (on ? 23f : 9f);
        UIRender.roundRect(x, y, 32f, 17f, 8.5f, fill, fill, .3f);
        UIRender.circle(knobX, y + 9.5f, 6f, UIRender.withAlpha(Theme.SHADOW, .70f));
        UIRender.circle(knobX, y + 8.5f, 6f, Theme.OPAQUE_WHITE);
    }

    private void drawShieldGlyph(float x, float y, int color) {
        UIRender.line(x + 5f, y, x + 10f, y + 2f, 1.2f, color);
        UIRender.line(x + 10f, y + 2f, x + 9f, y + 8f, 1.2f, color);
        UIRender.line(x + 9f, y + 8f, x + 5f, y + 11f, 1.2f, color);
        UIRender.line(x + 5f, y + 11f, x + 1f, y + 8f, 1.2f, color);
        UIRender.line(x + 1f, y + 8f, x, y + 2f, 1.2f, color);
        UIRender.line(x, y + 2f, x + 5f, y, 1.2f, color);
    }

    private void drawSearchGlyph(float x, float y, int color) {
        UIRender.ring(x - 2f, y - 2f, 4.5f, 3.1f, color);
        UIRender.line(x + 1f, y + 1f, x + 5f, y + 5f, 1.2f, color);
    }

    private void drawCloseGlyph(float x, float y, int color) {
        UIRender.line(x - 3f, y - 3f, x + 3f, y + 3f, 1.1f, color);
        UIRender.line(x + 3f, y - 3f, x - 3f, y + 3f, 1.1f, color);
    }

    private void drawCheckGlyph(float x, float y, int color) {
        UIRender.line(x - 4f, y, x - 1f, y + 3f, 1.4f, color);
        UIRender.line(x - 1f, y + 3f, x + 5f, y - 4f, 1.4f, color);
    }

    private void drawNoticeGlyph(float x, float y, int color, boolean warning) {
        if (warning) {
            UIRender.triangle(x, y + 6f, x + 5f, y - 3f, x + 10f, y + 6f, color);
            UIRender.line(x + 5f, y, x + 5f, y + 3f, 1f, Theme.windowBottom);
        } else {
            UIRender.ring(x + 5f, y + 2f, 5f, 3.7f, color);
            UIRender.line(x + 5f, y + 1f, x + 5f, y + 5f, 1f, color);
        }
    }
}
