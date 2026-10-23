package com.arena.pekaclient.gui;

import com.arena.pekaclient.PekaVersion;
import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.config.AssistSettings;
import com.arena.pekaclient.config.ChatQolSettings;
import com.arena.pekaclient.config.DungeonEspSettings;
import com.arena.pekaclient.dungeon.M7Settings;
import com.arena.pekaclient.hud.ClientMetrics;
import com.arena.pekaclient.hud.HudElement;
import com.arena.pekaclient.hud.HudManager;
import com.arena.pekaclient.itemlockswap.ItemLockSwapSettings;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import com.arena.pekaclient.widget.AppleButton;
import com.arena.pekaclient.widget.AppleSlider;
import com.arena.pekaclient.widget.AppleToggle;
import com.arena.pekaclient.widget.SearchField;
import com.arena.pekaclient.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Native Minecraft 1.8.9 port of the supplied DeepSeek settings reference.
 *
 * <p>The reference is a React/Vite design proposal, not runnable Forge code.  This screen deliberately keeps its
 * five-destination rail, global search, Advanced gate, card hierarchy, inline details, Home live performance panel
 * and the one clipped scroll pane, while every control reads and writes the existing Sky VeLor configuration owners. It does
 * not create a web dependency or a second persistence schema.</p>
 */
public class AppleSettingsScreen extends AppleScreen {

    static final String[] DESTINATION_NAMES = {"Home", "Gameplay", "Dungeon", "Visuals", "System"};
    static final String PROFILE_CREATOR = "pekadeco";
    static final String[] PROFILE_CONTRIBUTORS = {"ii88w", "whiteShadow"};
    private static final float CHROME_H = 17f;
    private static final float NORMAL_TOOLBAR_H = 43f;
    private static final float NARROW_TOOLBAR_H = 65f;
    private static final float RAIL_W = 158f;
    private static final float GAP = 8f;
    private static final float CARD_GAP = 9f;
    private static final float CARD_PAD = 13f;
    private static final float ROW_PAD_X = 13f;
    private static final float ROW_PAD_Y = 9f;
    private static final float CONTROL_GAP = 8f;
    private static final float WHEEL_STEP = 84f;

    private static final int HIT_NAV = 1;
    private static final int HIT_ADVANCED = 2;
    private static final int HIT_SEARCH_CLEAR = 3;
    private static final int HIT_CONTROL = 4;
    private static final int HIT_DISCLOSURE = 5;
    private static final int HIT_ROUTE = 6;
    private static final int HIT_CHOICE = 7;
    private static final int HIT_HOME_TOGGLE = 8;
    private static final int HIT_HOME_GAMEPLAY = 9;
    private static final int HIT_WINDOW_GREEN = 10;
    private static final int HIT_WINDOW_YELLOW = 11;
    private static final int HIT_WINDOW_RED = 12;
    private static final int HIT_PROFILE = 13;
    private static final int HIT_PROFILE_CLOSE = 14;

    private final GuiScreen parent;
    private final SearchField search = new SearchField("Search settings...");
    /** Read-only settings-surface telemetry; this is not a HudElement or a second metrics renderer. */
    private final ClientMetrics.Reader clientMetrics = ClientMetrics.liveReader();
    private final GuiLayoutToolkit.Scroll scroll = new GuiLayoutToolkit.Scroll();
    private final Gesture gesture = new Gesture();
    private final Map<String, Widget> controls = new HashMap<String, Widget>();
    private final Map<String, Hit> knownHits = new HashMap<String, Hit>();
    private final List<Hit> visibleHits = new ArrayList<Hit>();
    private final Set<String> expandedRows = new HashSet<String>();
    private final Map<String, TextMeasure> textMeasures = new HashMap<String, TextMeasure>();
    private final GuiLayoutToolkit.Rect panel = new GuiLayoutToolkit.Rect();
    private final GuiLayoutToolkit.Rect viewport = new GuiLayoutToolkit.Rect();
    private final GuiLayoutToolkit.Rect profilePopup = new GuiLayoutToolkit.Rect();
    private final GuiLayoutToolkit.Rect profileClose = new GuiLayoutToolkit.Rect();

    private ReferenceSettingsCatalog.Tab activeTab;
    private boolean showAdvanced;
    private boolean compactRail;
    private boolean expandedWindow;
    private boolean minimizedWindow;
    private boolean profileOpen;
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
    private Hit pendingHit;
    private boolean gestureActive;
    private boolean sliderEngaged;
    private String toast = "";
    private long toastUntilNs;

    /** Opens Home. */
    public AppleSettingsScreen(GuiScreen parent) {
        this(parent, ReferenceSettingsCatalog.Tab.HOME);
    }

    /** Opens the supplied reference destination; null intentionally falls back to Home. */
    public AppleSettingsScreen(GuiScreen parent, ReferenceSettingsCatalog.Tab initialTab) {
        this.parent = parent;
        this.activeTab = initialTab == null ? ReferenceSettingsCatalog.Tab.HOME : initialTab;
        refreshReferenceTheme();
    }

    /** Native replacement destination order, exposed for headless regression coverage. */
    static String[] destinationNames() {
        return DESTINATION_NAMES.clone();
    }

    /** The reference is a rail plus inline child disclosure, never the retired expandable seven-section Hub. */
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
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
        AppleSettings.save();
        HudManager.save();
        Theme.refresh();
    }

    /** Rebuilds the reference palette from the established appearance settings without adding new persisted state. */
    private void refreshReferenceTheme() {
        Theme.applyReferenceSettingsPalette();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long now = System.nanoTime();
        float dt = lastFrameNs == 0L ? 1f / 60f : Math.min(.1f, (now - lastFrameNs) / 1.0e9f);
        lastFrameNs = now;
        refreshLayout();
        scroll.update(dt, AppleSettings.smoothScrolling);
        updateSearchAndWidgets(dt);

        int quality = AppleSettings.experimentalPerformance ? 2 : AppleSettings.curveQuality;
        quality = Math.max(0, Math.min(AppleSettings.CURVE_SEGMENTS.length - 1, quality));
        UIRender.segCap = AppleSettings.CURVE_SEGMENTS[quality];
        UIRender.shadows = AppleSettings.shadows && !AppleSettings.experimentalPerformance;

        drawDefaultBackground();
        UIRender.beginFrame();
        UIRender.rect(0f, 0f, width, height, Theme.veil);
        drawShell(mouseX, mouseY);
        UIRender.endFrame();
    }

    private void refreshLayout() {
        Minecraft mc = Minecraft.getMinecraft();
        sf = new ScaledResolution(mc).getScaleFactor();
        // This single pure layout policy is shared by rendering and hit testing, so larger/minimized controls never
        // leave stale click rectangles behind at a different GUI scale.
        ReferenceSettingsWindowLayout.layout(panel, width, height, expandedWindow, minimizedWindow);
        if (minimizedWindow) {
            compactRail = false;
            toolbarH = 0f;
            railX = railY = railW = railH = 0f;
            contentX = contentY = contentW = contentH = 0f;
            viewport.set(panel.x, panel.y, 0f, 0f);
            narrowRows = true;
            return;
        }

        compactRail = panel.w < 540f;
        toolbarH = panel.w < 520f ? NARROW_TOOLBAR_H : NORMAL_TOOLBAR_H;
        float bodyY = panel.y + CHROME_H + toolbarH;
        float bodyH = Math.max(1f, panel.h - CHROME_H - toolbarH);

        if (compactRail) {
            railX = panel.x + 7f;
            railY = bodyY + 5f;
            railW = Math.max(1f, panel.w - 14f);
            // A second compact row keeps the profile control reachable instead of overloading tiny tab labels.
            railH = Math.min(57f, Math.max(48f, bodyH * .30f));
            contentX = panel.x + 8f;
            contentY = railY + railH + 7f;
            contentW = Math.max(1f, panel.w - 16f);
        } else {
            railX = panel.x;
            railY = bodyY;
            railW = Math.min(RAIL_W, Math.max(92f, panel.w * .25f));
            railH = bodyH;
            contentX = railX + railW + 10f;
            contentY = bodyY + 9f;
            contentW = Math.max(1f, panel.right() - 9f - contentX);
        }
        contentH = Math.max(1f, panel.bottom() - 9f - contentY);
        viewport.set(contentX, contentY, contentW, contentH);
        narrowRows = contentW < 330f;
        search.w = Math.max(50f, searchWidth());
        search.h = 23f;
        layoutSearch();
    }

    private void layoutSearch() {
        if (toolbarH > NORMAL_TOOLBAR_H) {
            search.x = panel.x + 9f;
            search.y = panel.y + CHROME_H + 34f;
        } else {
            search.x = panel.x + 116f;
            search.y = panel.y + CHROME_H + (toolbarH - search.h) * .5f;
        }
    }

    private float searchWidth() {
        float advancedW = panel.w < 360f ? 33f : 69f;
        if (toolbarH > NORMAL_TOOLBAR_H) return Math.max(50f, panel.w - 18f);
        // Removing the confirmation button lets Search use the reclaimed toolbar width; this exact right edge also places Advanced.
        return Math.max(52f, panel.right() - 9f - advancedW - 6f - (panel.x + 116f));
    }

    private void updateSearchAndWidgets(float dt) {
        search.update(dt);
        for (Widget widget : controls.values()) {
            widget.update(dt);
        }
    }

    private void drawShell(int mouseX, int mouseY) {
        visibleHits.clear();
        float outerRadius = Math.max(7f, Math.min(12f, Theme.radius));
        UIRender.shadow(panel.x, panel.y, panel.w, panel.h, outerRadius);
        UIRender.roundRectBorder(panel.x, panel.y, panel.w, panel.h, outerRadius,
                Theme.windowBottom, Theme.windowBottom, Theme.border);
        drawChrome();
        if (minimizedWindow) {
            drawMinimizedShell();
            return;
        }
        drawToolbar(mouseX, mouseY);
        drawRail(mouseX, mouseY);
        drawContent(mouseX, mouseY);
        drawToast();
        drawProfilePopup();
    }

    /** The three desktop circles are real controls: red closes, yellow minimizes/restores, green toggles size. */
    private void drawChrome() {
        float y = panel.y;
        UIRender.rect(panel.x + 1f, y + 1f, panel.w - 2f, CHROME_H, 0xCC0A110D);
        UIRender.line(panel.x + 1f, y + CHROME_H, panel.right() - 1f, y + CHROME_H, 1f, 0xFF233129);
        float dotY = y + CHROME_H * .5f;
        drawWindowCircle(panel.x + 12f, dotY, 0xFFFF6B6B, "window-red", HIT_WINDOW_RED);
        drawWindowCircle(panel.x + 22f, dotY, 0xFFF0B45E, "window-yellow", HIT_WINDOW_YELLOW);
        drawWindowCircle(panel.x + 32f, dotY, 0xFF5EDC87, "window-green", HIT_WINDOW_GREEN);
        if (panel.w > 170f) {
            UIRender.text(fr(), "MINECRAFT 1.8.9  ·  SKY VE LOR", panel.x + 44f,
                    centeredTextY(y, CHROME_H, .68f), 0xFF8DA697, .68f, false);
        }
        if (panel.w > 250f) {
            String hint = minimizedWindow ? "YELLOW RESTORES" : "ESC CLOSES";
            UIRender.text(fr(), hint, panel.right() - fr().getStringWidth(hint) * .62f - 9f,
                    centeredTextY(y, CHROME_H, .62f), 0xFF8DA697, .62f, false);
        }
    }

    private void drawWindowCircle(float x, float y, int color, String key, int hitType) {
        UIRender.circle(x, y, 3.2f, color);
        UIRender.ring(x, y, 3.7f, 2.8f, UIRender.mix(color, 0xFF07100A, .35f));
        registerHit(key, hitType, x - 5f, y - 5f, 10f, 10f, null, "");
    }

    private void drawMinimizedShell() {
        float y = panel.y + CHROME_H;
        float h = Math.max(1f, panel.bottom() - y - 1f);
        UIRender.rect(panel.x + 1f, y, panel.w - 2f, h, Theme.windowTop);
        String title = "Sky VeLor minimized";
        UIRender.text(fr(), title, panel.x + 10f, centeredTextY(y, h, .76f), Theme.textPrimary, .76f, false);
        if (panel.w > 225f) {
            String restore = "click yellow to restore";
            UIRender.text(fr(), restore, panel.right() - fr().getStringWidth(restore) * .59f - 10f,
                    centeredTextY(y, h, .59f), Theme.textSecondary, .59f, false);
        }
    }

    private void drawToolbar(int mouseX, int mouseY) {
        float y = panel.y + CHROME_H;
        UIRender.rect(panel.x + 1f, y, panel.w - 2f, toolbarH, Theme.windowTop);
        UIRender.line(panel.x + 1f, y + toolbarH, panel.right() - 1f, y + toolbarH, 1f, Theme.divider);

        if (toolbarH > NORMAL_TOOLBAR_H && panel.w < 360f) {
            drawCube(panel.x + 12f, y + 8f, 17f, Theme.accent);
            UIRender.text(fr(), "SV", panel.x + 34f, centeredTextY(y + 4f, 21f, .9f), Theme.textPrimary, .9f, false);
        } else {
            float titleBlockH = fr().FONT_HEIGHT * .94f + 2f + fr().FONT_HEIGHT * .58f;
            float titleY = y + (toolbarH - titleBlockH) * .5f;
            drawCube(panel.x + 12f, y + (toolbarH - 17f) * .5f, 17f, Theme.accent);
            UIRender.text(fr(), PekaVersion.DISPLAY_NAME, panel.x + 35f, titleY, Theme.textPrimary, .94f, false);
            UIRender.text(fr(), "SETTINGS · " + compactVersion(), panel.x + 35f,
                    titleY + fr().FONT_HEIGHT * .94f + 2f, Theme.textSecondary, .58f, false);
        }

        search.draw(mouseX, mouseY);
        if (!search.isEmpty()) {
            registerHit("search-clear", HIT_SEARCH_CLEAR, search.x + search.w - 20f, search.y, 20f, search.h, null, "");
            drawCloseGlyph(search.x + search.w - 11f, search.y + search.h * .5f, Theme.textTertiary);
        } else {
            UIRender.text(fr(), "/", search.x + search.w - 10f, centeredTextY(search.y, search.h, .75f),
                    Theme.textTertiary, .75f, false);
        }

        float buttonY = toolbarH > NORMAL_TOOLBAR_H ? y + 8f : y + (toolbarH - 23f) * .5f;
        float advancedW = panel.w < 360f ? 33f : 69f;
        float advancedX = panel.right() - advancedW - 9f;
        int advanceFill = showAdvanced ? UIRender.withAlpha(Theme.accent, .15f) : Theme.card;
        int advanceBorder = showAdvanced ? UIRender.withAlpha(Theme.accent, .65f) : Theme.border;
        UIRender.roundRectBorder(advancedX, buttonY, advancedW, 23f, 5f, advanceFill, advanceFill, advanceBorder);
        drawFilterGlyph(advancedX + 7f, buttonY + 11.5f, showAdvanced ? Theme.accent : Theme.textSecondary);
        if (advancedW > 40f) {
            UIRender.text(fr(), showAdvanced ? "Advanced on" : "Advanced", advancedX + 19f,
                    centeredTextY(buttonY, 23f, .72f), showAdvanced ? Theme.accent : Theme.textSecondary, .72f, false);
        }
        registerHit("advanced", HIT_ADVANCED, advancedX, buttonY, advancedW, 23f, null, "");
    }

    private void drawRail(int mouseX, int mouseY) {
        int railFill = Theme.windowBottom;
        if (!compactRail) {
            UIRender.rect(railX + 1f, railY, railW, railH, railFill);
            UIRender.line(railX + railW, railY, railX + railW, railY + railH, 1f, Theme.divider);
        } else {
            UIRender.roundRect(railX, railY, railW, railH, 5f, railFill, railFill, .35f);
            UIRender.roundRectRing(railX, railY, railW, railH, 5f, 1f, Theme.border);
        }
        int tabCount = ReferenceSettingsCatalog.TABS.length;
        float footerReserve = compactRail ? 21f : 58f;
        float navTop = railY + (compactRail ? 3f : 7f);
        float navBottom = railY + railH - footerReserve;
        float slotH = compactRail ? Math.max(20f, navBottom - navTop)
                : Math.max(19f, (navBottom - navTop) / Math.max(1, tabCount));
        for (int i = 0; i < tabCount; i++) {
            ReferenceSettingsCatalog.Tab tab = ReferenceSettingsCatalog.TABS[i];
            float x;
            float y;
            float w;
            float h;
            if (compactRail) {
                w = railW / tabCount - 2f;
                h = Math.min(25f, slotH);
                x = railX + i * railW / tabCount + 1f;
                y = navTop;
            } else {
                x = railX + 8f;
                y = navTop + i * slotH;
                w = railW - 16f;
                h = Math.max(18f, Math.min(26f, slotH - 3f));
            }
            boolean active = activeTab == tab;
            int fill = active ? UIRender.withAlpha(Theme.accent, .13f) : 0x00000000;
            if (active || inside(mouseX, mouseY, x, y, w, h)) {
                UIRender.roundRect(x, y, w, h, 5f, fill == 0 ? Theme.card : fill, fill == 0 ? Theme.card : fill, .25f);
            }
            boolean showLabel = !compactRail || w > 68f;
            float iconX = compactRail && showLabel ? x + 5f
                    : compactRail ? x + Math.max(3f, (w - 14f) * .5f) : x + 5f;
            float iconY = y + (h - 14f) * .5f;
            drawRailGlyph(i, iconX, iconY, active ? Theme.accent : Theme.textTertiary);
            if (showLabel) {
                float labelX = compactRail ? iconX + 16f : x + 24f;
                float labelW = compactRail ? Math.max(1f, x + w - labelX - 3f) : w - 33f;
                UIRender.text(fr(), fit(fr(), tab.label, labelW / .78f), labelX, centeredTextY(y, h, .78f),
                        active ? Theme.textPrimary : Theme.textSecondary, .78f, false);
            }
            if (active && !compactRail) {
                UIRender.circle(x + w - 7f, y + h * .5f, 1.7f, Theme.accent);
            }
            registerHit("nav-" + tab.name(), HIT_NAV, x, y, w, h, null, tab.name());
        }

        if (compactRail) {
            drawCompactProfileControl(mouseX, mouseY);
        } else {
            drawSidebarProfileControl(mouseX, mouseY);
        }
    }

    private void drawSidebarProfileControl(int mouseX, int mouseY) {
        float h = 36f;
        float y = railY + railH - h - 8f;
        float x = railX + 8f;
        float w = railW - 16f;
        UIRender.line(railX + 9f, y - 7f, railX + railW - 9f, y - 7f, 1f, Theme.divider);
        boolean hovered = inside(mouseX, mouseY, x, y, w, h);
        UIRender.roundRectBorder(x, y, w, h, 5f, hovered ? Theme.cardHover : Theme.card,
                hovered ? Theme.cardHover : Theme.card, profileOpen ? Theme.accent : Theme.border);
        drawProfileGlyph(x + 8f, y + 11f, profileOpen ? Theme.accent : Theme.textSecondary);
        UIRender.text(fr(), "PROFILE", x + 25f, y + 7f, Theme.textTertiary, .58f, false);
        UIRender.text(fr(), "About Sky VeLor", x + 25f, y + 18f, Theme.textPrimary, .70f, false);
        registerHit("profile", HIT_PROFILE, x, y, w, h, null, "");
    }

    private void drawCompactProfileControl(int mouseX, int mouseY) {
        float x = railX + 4f;
        float y = railY + railH - 19f;
        float w = railW - 8f;
        float h = 15f;
        boolean hovered = inside(mouseX, mouseY, x, y, w, h);
        UIRender.roundRect(x, y, w, h, 4f, hovered ? Theme.cardHover : Theme.buttonFill,
                hovered ? Theme.cardHover : Theme.buttonFill, .2f);
        drawProfileGlyph(x + 7f, y + 3f, profileOpen ? Theme.accent : Theme.textTertiary);
        UIRender.text(fr(), "PROFILE", x + 18f, centeredTextY(y, h, .57f),
                profileOpen ? Theme.accent : Theme.textSecondary, .57f, false);
        registerHit("profile", HIT_PROFILE, x, y, w, h, null, "");
    }

    private float centeredTextY(float top, float height, float scale) {
        return top + Math.max(0f, (height - fr().FONT_HEIGHT * scale) * .5f);
    }

    private void drawContent(int mouseX, int mouseY) {
        float total = measuredContentHeight();
        scroll.setMaximum(Math.max(0f, total - viewport.h));
        boolean mouseInView = viewport.contains(mouseX, mouseY);
        UIRender.scissorBegin(viewport.x, viewport.y, viewport.w, viewport.h, sf);
        float y = viewport.y - scroll.value();
        if (!search.isEmpty()) {
            y = drawSearchResults(y, mouseInView ? mouseX : -10000, mouseInView ? mouseY : -10000);
        } else if (activeTab == ReferenceSettingsCatalog.Tab.HOME) {
            y = drawHome(y, mouseInView ? mouseX : -10000, mouseInView ? mouseY : -10000);
        } else {
            y = drawTab(y, mouseInView ? mouseX : -10000, mouseInView ? mouseY : -10000, activeTab);
        }
        UIRender.scissorEnd();
        drawScrollBar(total);
    }

    private float measuredContentHeight() {
        if (!search.isEmpty()) return measureSearchResults();
        if (activeTab == ReferenceSettingsCatalog.Tab.HOME) return measureHome();
        return measureTab(activeTab);
    }

    private void drawScrollBar(float total) {
        if (total <= viewport.h + .5f) return;
        float track = Math.max(1f, viewport.h - 8f);
        float thumb = Math.max(22f, track * viewport.h / total);
        float max = Math.max(1f, scroll.maximum());
        float y = viewport.y + 4f + (track - thumb) * (scroll.value() / max);
        UIRender.roundRect(viewport.right() - 3f, y, 2f, thumb, 1f, Theme.textTertiary, Theme.textTertiary, .5f);
    }

    private float drawTab(float y, int mouseX, int mouseY, ReferenceSettingsCatalog.Tab tab) {
        UIRender.text(fr(), tab.label, contentX, y + 1f, Theme.textPrimary, 1.26f, false);
        UIRender.text(fr(), fit(fr(), tab.caption, contentW / .78f), contentX, y + 17f, Theme.textSecondary, .78f, false);
        int everyday = everydayToggleCount(tab);
        String meta = everyday + " everyday switches  ·  " + visibleCardCount(tab) + " cards";
        float metaW = fr().getStringWidth(meta) * .63f;
        UIRender.text(fr(), meta, contentX + Math.max(0f, contentW - metaW), y + 17f, Theme.textTertiary, .63f, false);
        y += 35f;
        List<ReferenceSettingsCatalog.Card> cards = ReferenceSettingsCatalog.cards(tab);
        boolean drew = false;
        for (ReferenceSettingsCatalog.Card card : cards) {
            if (card.advanced && !showAdvanced) continue;
            y = drawCard(y, mouseX, mouseY, tab, card);
            y += CARD_GAP;
            drew = true;
        }
        if (!drew) {
            drawEmpty(y, "Everything in this section is advanced. Turn on Advanced to see it.");
            y += 65f;
        }
        y += 4f;
        UIRender.text(fr(), "changes save when you close settings", contentX + 2f, y, Theme.textTertiary, .63f, false);
        String advanced = showAdvanced ? "hide advanced" : "show advanced";
        UIRender.text(fr(), advanced, contentX + contentW - fr().getStringWidth(advanced) * .63f - 2f, y, Theme.textTertiary, .63f, false);
        return y + 16f;
    }

    private float measureTab(ReferenceSettingsCatalog.Tab tab) {
        float h = 35f;
        int cards = 0;
        for (ReferenceSettingsCatalog.Card card : ReferenceSettingsCatalog.cards(tab)) {
            if (card.advanced && !showAdvanced) continue;
            h += measureCard(card) + CARD_GAP;
            cards++;
        }
        if (cards == 0) h += 65f;
        return h + 20f;
    }

    private int everydayToggleCount(ReferenceSettingsCatalog.Tab tab) {
        int count = 0;
        for (ReferenceSettingsCatalog.Card card : ReferenceSettingsCatalog.cards(tab)) {
            if (card.advanced && !showAdvanced) continue;
            for (ReferenceSettingsCatalog.Setting setting : card.settings) {
                if (!setting.advanced && setting.control == ReferenceSettingsCatalog.Control.TOGGLE) count++;
            }
        }
        return count;
    }

    private int visibleCardCount(ReferenceSettingsCatalog.Tab tab) {
        int count = 0;
        for (ReferenceSettingsCatalog.Card card : ReferenceSettingsCatalog.cards(tab)) {
            if (showAdvanced || !card.advanced) count++;
        }
        return count;
    }

    private float drawCard(float y, int mouseX, int mouseY, ReferenceSettingsCatalog.Tab tab,
                           ReferenceSettingsCatalog.Card card) {
        float h = measureCard(card);
        float radius = Math.max(5f, Math.min(10f, Theme.radius * .72f));
        UIRender.roundRectBorder(contentX, y, contentW, h, radius, Theme.card, Theme.card, Theme.border);
        float iconX = contentX + CARD_PAD;
        float iconY = y + CARD_PAD;
        UIRender.roundRect(iconX, iconY, 23f, 23f, 5f, Theme.buttonFill, Theme.buttonFill, .25f);
        drawCardGlyph(card.id, iconX + 6f, iconY + 6f, Theme.accent);
        float cardTextBlockH = fr().FONT_HEIGHT * .88f + 3f + fr().FONT_HEIGHT * .72f;
        float cardTextY = y + (46f - cardTextBlockH) * .5f;
        UIRender.text(fr(), card.title, iconX + 31f, cardTextY, Theme.textPrimary, .88f, false);
        if (card.advanced) {
            String tag = "ADVANCED";
            float tagX = Math.min(contentX + contentW - 54f, iconX + 31f + fr().getStringWidth(card.title) * .88f + 4f);
            UIRender.roundRectRing(tagX, cardTextY - 1f, 49f, 10f, 2f, 1f, Theme.border);
            UIRender.text(fr(), tag, tagX + 3f, centeredTextY(cardTextY - 1f, 10f, .51f), Theme.textTertiary, .51f, false);
        }
        UIRender.text(fr(), fit(fr(), card.subtitle, (contentW - 54f) / .72f), iconX + 31f,
                cardTextY + fr().FONT_HEIGHT * .88f + 3f, Theme.textSecondary, .72f, false);
        float rowY = y + 46f;
        List<ReferenceSettingsCatalog.Setting> settings = card.settings;
        for (ReferenceSettingsCatalog.Setting setting : settings) {
            if (setting.advanced && !showAdvanced) continue;
            rowY = drawSetting(rowY, mouseX, mouseY, tab, card, setting, false);
        }
        if (!card.notice.isEmpty()) {
            float noticeY = rowY;
            float noticeH = measureNotice(card.notice);
            UIRender.rect(contentX + 1f, noticeY, contentW - 2f, noticeH, Theme.windowBottom);
            drawNoticeGlyph(contentX + ROW_PAD_X + 1f, noticeY + 10f,
                    card.warningNotice ? 0xFFF0B45E : Theme.accent, card.warningNotice);
            drawWrapped(card.notice, contentX + ROW_PAD_X + 13f, noticeY + 7f,
                    contentW - ROW_PAD_X * 2f - 16f, Theme.textSecondary, .68f, 3);
            rowY += noticeH;
        }
        return y + h;
    }

    private float measureCard(ReferenceSettingsCatalog.Card card) {
        float h = 46f;
        for (ReferenceSettingsCatalog.Setting setting : card.settings) {
            if (showAdvanced || !setting.advanced) h += measureSetting(setting, false);
        }
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
        float rowH = measureSetting(setting, child);
        float x = contentX;
        float rowW = contentW;
        int divider = Theme.divider;
        UIRender.line(x + 1f, y, x + rowW - 1f, y, 1f, divider);
        float padX = child ? ROW_PAD_X + 14f : ROW_PAD_X;
        float leftX = x + padX;
        float controlW = controlWidth(setting);
        float labelW = narrowRows ? rowW - padX * 2f : Math.max(42f, rowW - padX - ROW_PAD_X - controlW - CONTROL_GAP);
        float labelY = y + ROW_PAD_Y;
        UIRender.text(fr(), fit(fr(), setting.label, labelW / .84f), leftX, labelY, Theme.textPrimary, .84f, false);
        drawStateChip(setting, leftX, labelY, labelW);
        float descriptionY = labelY + 11f;
        int descriptionLines = drawWrapped(setting.description, leftX, descriptionY, labelW, Theme.textSecondary, .68f, 2);

        boolean hasChildren = setting.hasChildren(showAdvanced);
        float disclosureY = descriptionY + descriptionLines * 8f + 3f;
        if (hasChildren) {
            boolean expanded = expandedRows.contains(setting.id);
            drawChevron(leftX + 2f, disclosureY + 4f, expanded ? Theme.accent : Theme.textTertiary, expanded);
            String detail = setting.visibleChildCount(showAdvanced) + (setting.visibleChildCount(showAdvanced) == 1 ? " option" : " options");
            UIRender.text(fr(), detail, leftX + 9f, disclosureY, expanded ? Theme.accent : Theme.textTertiary, .62f, false);
            registerHit("disclose:" + setting.id, HIT_DISCLOSURE, leftX, disclosureY - 2f,
                    Math.min(labelW, fr().getStringWidth(detail) * .62f + 16f), 12f, setting, "");
        }

        float controlX;
        float controlY;
        if (narrowRows) {
            controlX = leftX;
            controlY = y + rowH - controlHeight(setting) - 7f;
        } else {
            controlX = x + rowW - ROW_PAD_X - controlW;
            controlY = y + Math.max(6f, (baseSettingHeight(setting) - controlHeight(setting)) * .5f);
        }
        drawControl(setting, controlX, controlY, controlW, mouseX, mouseY);

        float cursor = y + baseSettingHeight(setting);
        if (hasChildren && expandedRows.contains(setting.id)) {
            float areaY = cursor;
            float childBlockH = childBlockHeight(setting);
            UIRender.rect(x + 1f, areaY, rowW - 2f, childBlockH, Theme.windowBottom);
            UIRender.line(leftX + 2f, areaY + 5f, leftX + 2f, areaY + childBlockH - 6f, 1f, Theme.divider);
            for (ReferenceSettingsCatalog.Setting nested : setting.children) {
                if (nested.advanced && !showAdvanced) continue;
                cursor = drawSetting(cursor, mouseX, mouseY, tab, card, nested, true);
            }
        }
        return y + rowH;
    }

    private float measureSetting(ReferenceSettingsCatalog.Setting setting, boolean child) {
        float h = baseSettingHeight(setting);
        if (setting.hasChildren(showAdvanced) && expandedRows.contains(setting.id)) h += childBlockHeight(setting);
        return h;
    }

    private float baseSettingHeight(ReferenceSettingsCatalog.Setting setting) {
        float controlW = controlWidth(setting);
        float labelW = narrowRows ? contentW - ROW_PAD_X * 2f
                : Math.max(42f, contentW - ROW_PAD_X * 2f - controlW - CONTROL_GAP);
        int lines = cachedLines(setting.id, setting.description, labelW, .68f, 2);
        float textH = 10f + lines * 8f + (setting.hasChildren(showAdvanced) ? 12f : 0f);
        float base = Math.max(textH, controlHeight(setting)) + ROW_PAD_Y * 2f - 1f;
        if (narrowRows) base += controlHeight(setting) + 5f;
        return Math.max(31f, base);
    }

    private float childBlockHeight(ReferenceSettingsCatalog.Setting parentSetting) {
        float h = 6f;
        for (ReferenceSettingsCatalog.Setting child : parentSetting.children) {
            if (showAdvanced || !child.advanced) h += measureSetting(child, true);
        }
        return h + 5f;
    }

    private float controlWidth(ReferenceSettingsCatalog.Setting setting) {
        float available = Math.max(1f, contentW - ROW_PAD_X * 2f);
        switch (setting.control) {
            case TOGGLE:
                return Math.min(available, setting.actionLabel.length() == 0 ? 38f : (narrowRows ? 104f : 112f));
            case SLIDER:
                return Math.min(available, narrowRows ? Math.max(80f, available) : Math.min(160f, Math.max(82f, contentW * .34f)));
            case ACTION:
                return Math.min(available, Math.max(42f, Math.min(116f, fr().getStringWidth(setting.actionLabel) + 19f)));
            case CHOICE:
                return choiceWidth(setting);
            default:
                return Math.min(available, 12f);
        }
    }

    private float choiceWidth(ReferenceSettingsCatalog.Setting setting) {
        float width = 5f;
        for (String choice : setting.choices) width += fr().getStringWidth(choice) * .67f + 11f;
        return Math.min(narrowRows ? contentW - ROW_PAD_X * 2f : Math.max(52f, contentW * .44f), Math.max(52f, width));
    }

    private float controlHeight(ReferenceSettingsCatalog.Setting setting) {
        if (setting.control == ReferenceSettingsCatalog.Control.SLIDER) return 24f;
        if (setting.control == ReferenceSettingsCatalog.Control.CHOICE) return 21f;
        return 21f;
    }

    private void drawStateChip(ReferenceSettingsCatalog.Setting setting, float x, float labelY, float labelW) {
        String chip = setting.advanced ? "ADVANCED" : "EVERYDAY";
        float titleW = fr().getStringWidth(setting.label) * .84f;
        float chipX = x + titleW + 5f;
        float chipW = fr().getStringWidth(chip) * .51f + 5f;
        if (chipX + chipW > x + labelW) return;
        if (setting.advanced) UIRender.roundRect(chipX, labelY - 1f, chipW, 9f, 2f, Theme.buttonFill, Theme.buttonFill, .1f);
        else UIRender.roundRectRing(chipX, labelY - 1f, chipW, 9f, 2f, .7f, Theme.divider);
        UIRender.text(fr(), chip, chipX + 2f, labelY + 1f, Theme.textTertiary, .51f, false);
    }

    private void drawControl(ReferenceSettingsCatalog.Setting setting, float x, float y, float w,
                             int mouseX, int mouseY) {
        if (setting.control == ReferenceSettingsCatalog.Control.INFO) {
            if ("gui-metrics".equals(setting.id)) {
                ClientMetrics.Snapshot snapshot = clientMetrics.snapshot(System.currentTimeMillis());
                String values = snapshot.fpsText() + "  ·  " + snapshot.pingText() + "  ·  " + snapshot.memoryText();
                UIRender.text(fr(), fit(fr(), values, Math.max(1f, w / .62f)), x,
                        centeredTextY(y, controlHeight(setting), .62f), Theme.textTertiary, .62f, false);
            } else {
                UIRender.text(fr(), "—", x, centeredTextY(y, controlHeight(setting), .9f), Theme.textTertiary, .9f, false);
            }
            return;
        }
        if (setting.control == ReferenceSettingsCatalog.Control.CHOICE) {
            drawChoiceControl(setting, x, y, w);
            return;
        }
        Widget widget = controlFor(setting);
        if (widget == null) return;
        if (setting.control == ReferenceSettingsCatalog.Control.TOGGLE && setting.actionLabel.length() > 0 && w >= 84f) {
            float toggleW = Math.min(38f, w);
            float toggleX = x + w - toggleW;
            float routeW = Math.max(1f, w - toggleW - 6f);
            drawRouteButton(setting, x, y, routeW, 20f);
            widget.x = toggleX;
            widget.y = y;
            widget.w = toggleW;
            widget.h = 20f;
            widget.draw(mouseX, mouseY);
            registerHit("route:" + setting.id, HIT_ROUTE, x, y, routeW, 20f, setting, "");
            registerHit("control:" + setting.id, HIT_CONTROL, toggleX, y, toggleW, 20f, setting, "");
        } else {
            widget.x = x;
            widget.y = y;
            widget.w = w;
            widget.h = controlHeight(setting);
            widget.draw(mouseX, mouseY);
            registerHit("control:" + setting.id, HIT_CONTROL, x, y, w, widget.h, setting, "");
        }
    }

    private void drawRouteButton(ReferenceSettingsCatalog.Setting setting, float x, float y, float w, float h) {
        UIRender.roundRectBorder(x, y, w, h, 4f, Theme.buttonFill, Theme.buttonFill, Theme.border);
        String label = fit(fr(), setting.actionLabel, Math.max(1f, (w - 8f) / .68f));
        UIRender.text(fr(), label, x + 5f, centeredTextY(y, h, .68f), Theme.textSecondary, .68f, false);
    }

    private void drawChoiceControl(ReferenceSettingsCatalog.Setting setting, float x, float y, float w) {
        UIRender.roundRectBorder(x, y, w, 21f, 5f, Theme.buttonFill, Theme.buttonFill, Theme.border);
        float total = 0f;
        for (String value : setting.choices) total += fr().getStringWidth(value) * .67f + 11f;
        float cursor = x + Math.max(2f, (w - total) * .5f);
        int selected = choiceValue(setting.id);
        for (int i = 0; i < setting.choices.length; i++) {
            String choice = setting.choices[i];
            float optionW = fr().getStringWidth(choice) * .67f + 11f;
            if (i == selected) {
                UIRender.roundRect(cursor, y + 2f, optionW - 1f, 17f, 3f, Theme.cardHover, Theme.cardHover, .2f);
            }
            UIRender.text(fr(), choice, cursor + 5f, centeredTextY(y, 21f, .67f),
                    i == selected ? Theme.textPrimary : Theme.textSecondary, .67f, false);
            registerHit("choice:" + setting.id + ":" + i, HIT_CHOICE, cursor, y, optionW, 21f, setting, Integer.toString(i));
            cursor += optionW;
        }
    }

    private Widget controlFor(final ReferenceSettingsCatalog.Setting setting) {
        Widget existing = controls.get(setting.id);
        if (existing != null) return existing;
        Widget created;
        if (setting.control == ReferenceSettingsCatalog.Control.TOGGLE) {
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
        } else if (setting.control == ReferenceSettingsCatalog.Control.ACTION) {
            created = new AppleButton(setting.actionLabel.length() == 0 ? "Open" : setting.actionLabel,
                    Math.max(58f, Math.min(116f, fr().getStringWidth(setting.actionLabel) + 19f)), 21f, false,
                    new Runnable() {
                        @Override public void run() { openRoute(setting.id); }
                    });
        } else {
            return null;
        }
        controls.put(setting.id, created);
        return created;
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
        hit.data = data == null ? "" : data;
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

    private String compactVersion() {
        String version = PekaVersion.NAME;
        return version.startsWith("Beta-") ? version.substring(5) : version;
    }

    // ------------------------------------------------------------------ Home and search

    /**
     * Home is an at-a-glance surface, not a setup wizard. The reused reader refreshes its strings at most twice a
     * second, so drawing the panel does not poll networking or format new values every frame.
     */
    private float drawHome(float y, int mouseX, int mouseY) {
        UIRender.text(fr(), "Sky VeLor, ready.", contentX, y + 1f, Theme.textPrimary, 1.34f, false);
        UIRender.text(fr(), "Live local performance and quick controls. Changes save when this window closes.",
                contentX, y + 18f, Theme.textSecondary, .72f, false);
        String saved = "SAVES ON CLOSE";
        float pillW = fr().getStringWidth(saved) * .58f + 12f;
        float pillX = contentX + Math.max(0f, contentW - pillW);
        UIRender.roundRect(pillX, y + 3f, pillW, 15f, 7f,
                UIRender.withAlpha(Theme.accent, .12f), UIRender.withAlpha(Theme.accent, .12f), .2f);
        UIRender.text(fr(), saved, pillX + 6f, centeredTextY(y + 3f, 15f, .58f), Theme.accent, .58f, false);
        y += 37f;

        y = drawHomePerformance(y);
        y += 10f;

        UIRender.text(fr(), "QUICK CONTROLS", contentX, centeredTextY(y, 12f, .61f), Theme.textTertiary, .61f, false);
        String allGameplay = "all gameplay ->";
        float allX = contentX + contentW - fr().getStringWidth(allGameplay) * .61f;
        UIRender.text(fr(), allGameplay, allX, centeredTextY(y, 12f, .61f), Theme.accent, .61f, false);
        registerHit("home-gameplay", HIT_HOME_GAMEPLAY, allX - 2f, y - 1f,
                fr().getStringWidth(allGameplay) * .61f + 4f, 14f, null, "");
        y += 13f;

        float gap = 6f;
        String[] ids = {"auto-sprint", "terminal-assist", "chat-qol", "item-animation"};
        String[] labels = {"Sprint", "Terminal", "Chat", "Hand"};
        String[] details = {"Only while forward is held", "Passive slot outline", "Attached filter tabs", "First-person animation"};
        float quickW = (contentW - gap) * .5f;
        float quickH = narrowRows ? 42f : 39f;
        for (int i = 0; i < ids.length; i++) {
            int row = i / 2;
            int col = i % 2;
            float x = contentX + col * (quickW + gap);
            float py = y + row * (quickH + gap);
            boolean checked = booleanValue(ids[i]);
            UIRender.roundRectBorder(x, py, quickW, quickH, 6f, Theme.card, Theme.card, Theme.border);
            UIRender.roundRect(x + 7f, py + (quickH - 20f) * .5f, 20f, 20f, 4f,
                    Theme.buttonFill, Theme.buttonFill, .2f);
            drawQuickGlyph(i, x + 12f, py + (quickH - 10f) * .5f, checked ? Theme.accent : Theme.textTertiary);
            float labelX = x + 33f;
            float toggleX = x + quickW - 43f;
            UIRender.text(fr(), fit(fr(), labels[i], Math.max(1f, (toggleX - labelX - 3f) / .75f)), labelX,
                    py + 7f, Theme.textPrimary, .75f, false);
            UIRender.text(fr(), fit(fr(), details[i], Math.max(1f, (quickW - 40f) / .58f)), labelX, py + 20f,
                    Theme.textSecondary, .58f, false);
            drawMiniToggle(toggleX, py + (quickH - 17f) * .5f, checked);
            registerHit("home-toggle:" + ids[i], HIT_HOME_TOGGLE, x, py, quickW, quickH, null, ids[i]);
        }
        y += quickH * 2f + gap + 12f;

        float noticeH = 48f;
        UIRender.roundRectBorder(contentX, y, contentW, noticeH, 6f, Theme.card, Theme.card, Theme.border);
        drawShieldGlyph(contentX + 10f, y + 11f, Theme.accent);
        UIRender.text(fr(), "Safe by default", contentX + 27f, y + 9f, Theme.textPrimary, .76f, false);
        drawWrapped("Network sync, automated clicks, and opt-in assists stay off until you explicitly choose them.",
                contentX + 10f, y + 22f, contentW - 20f, Theme.textSecondary, .63f, 2);
        return y + noticeH + 10f;
    }

    private float drawHomePerformance(float y) {
        ClientMetrics.Snapshot snapshot = clientMetrics.snapshot(System.currentTimeMillis());
        float h = homePerformanceHeight();
        UIRender.roundRectBorder(contentX, y, contentW, h, 7f, Theme.card, Theme.card, Theme.border);
        UIRender.text(fr(), "LIVE PERFORMANCE", contentX + 10f, y + 8f, Theme.textTertiary, .61f, false);
        String cadence = "LOCAL · 0.5 s";
        UIRender.text(fr(), cadence, contentX + contentW - fr().getStringWidth(cadence) * .54f - 10f, y + 8f,
                Theme.textTertiary, .54f, false);

        if (contentW < 405f) {
            float metricH = 28f;
            float metricX = contentX + 7f;
            float metricW = Math.max(1f, contentW - 14f);
            float firstY = y + 22f;
            drawHomeMetric(metricX, firstY, metricW, metricH, "FRAME RATE", snapshot.fpsText(), snapshot.fpsColor());
            drawHomeMetric(metricX, firstY + 31f, metricW, metricH, "JAVA MEMORY", snapshot.memoryText(), snapshot.memoryColor());
            drawHomeMetric(metricX, firstY + 62f, metricW, metricH, "SERVER PING", snapshot.pingText(), snapshot.pingColor());
        } else {
            float gap = 5f;
            float metricX = contentX + 7f;
            float metricY = y + 22f;
            float metricW = Math.max(1f, (contentW - 14f - gap * 2f) / 3f);
            drawHomeMetric(metricX, metricY, metricW, 48f, "FRAME RATE", snapshot.fpsText(), snapshot.fpsColor());
            drawHomeMetric(metricX + metricW + gap, metricY, metricW, 48f, "JAVA MEMORY", snapshot.memoryText(), snapshot.memoryColor());
            drawHomeMetric(metricX + (metricW + gap) * 2f, metricY, metricW, 48f, "SERVER PING", snapshot.pingText(), snapshot.pingColor());
        }
        return y + h;
    }

    /** Uses the cached strings from ClientMetrics.Snapshot; unknown/disconnected ping remains the honest -- ms. */
    private void drawHomeMetric(float x, float y, float w, float h, String label, String value, int valueColor) {
        UIRender.roundRect(x, y, w, h, 4f, Theme.buttonFill, Theme.buttonFill, .22f);
        UIRender.text(fr(), label, x + 7f, y + 5f, Theme.textTertiary, .52f, false);
        float valueScale = h < 34f ? .68f : .74f;
        UIRender.text(fr(), value, x + 7f, y + h - fr().FONT_HEIGHT * valueScale - 4f, valueColor, valueScale, false);
    }

    private float homePerformanceHeight() {
        return contentW < 405f ? 116f : 78f;
    }

    private float measureHome() {
        float quickH = narrowRows ? 42f : 39f;
        return 37f + homePerformanceHeight() + 10f + 13f + quickH * 2f + 6f + 12f + 48f + 10f;
    }

    private float drawSearchResults(float y, int mouseX, int mouseY) {
        List<ReferenceSettingsCatalog.Match> matches = ReferenceSettingsCatalog.search(search.text(), showAdvanced);
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
            drawEmpty(y, "Try \"lock\", \"leap\", \"particles\", \"theme\" or \"terminal\".");
            return y + 66f;
        }
        for (ReferenceSettingsCatalog.Match match : matches) {
            float h = measureSearchMatch(match);
            UIRender.roundRectBorder(contentX, y, contentW, h, 6f, Theme.card, Theme.card, Theme.border);
            String owner = match.tab.label + "  >  " + match.card.title;
            UIRender.text(fr(), fit(fr(), owner, (contentW - 20f) / .61f), contentX + 10f, y + 7f, Theme.textTertiary, .61f, false);
            UIRender.line(contentX + 1f, y + 18f, contentX + contentW - 1f, y + 18f, 1f, Theme.divider);
            drawSetting(y + 18f, mouseX, mouseY, match.tab, match.card, match.setting, false);
            y += h + CARD_GAP;
        }
        return y;
    }

    private float measureSearchResults() {
        List<ReferenceSettingsCatalog.Match> matches = ReferenceSettingsCatalog.search(search.text(), showAdvanced);
        if (matches.isEmpty()) return 33f + 66f;
        float h = 33f;
        for (ReferenceSettingsCatalog.Match match : matches) h += measureSearchMatch(match) + CARD_GAP;
        return h;
    }

    private float measureSearchMatch(ReferenceSettingsCatalog.Match match) {
        return 18f + measureSetting(match.setting, false);
    }

    // ------------------------------------------------------------------ Input and accessibility

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        if (button != 0) return;
        if (handleOpenDropdownClick(mouseX, mouseY)) return;
        Hit hit = hitAt(mouseX, mouseY);

        // The compact profile popover owns its entire z-layer: its X and an outside click are the two clear exits.
        if (profileOpen) {
            if (hit != null && hit.type == HIT_PROFILE_CLOSE) {
                profileOpen = false;
                return;
            }
            if (!profilePopup.contains(mouseX, mouseY)) profileOpen = false;
            return;
        }
        if (!panel.contains(mouseX, mouseY)) {
            // A minimized strip deliberately ignores outside clicks so yellow remains a reliable restore action.
            if (!minimizedWindow) close();
            return;
        }
        if (hit != null && (hit.type == HIT_WINDOW_RED || hit.type == HIT_WINDOW_YELLOW
                || hit.type == HIT_WINDOW_GREEN)) {
            activateWindowControl(hit.type);
            return;
        }
        if (minimizedWindow) return;
        if (search.contains(mouseX, mouseY)) {
            search.setFocused(true);
            return;
        }
        if (hit != null && hit.type == HIT_SEARCH_CLEAR) {
            search.clear();
            search.setFocused(true);
            return;
        }
        if (hit != null && hit.type == HIT_PROFILE) {
            profileOpen = true;
            search.setFocused(false);
            return;
        }
        if (hit != null && hit.type == HIT_NAV) {
            selectTab(hit.data);
            return;
        }
        if (hit != null && hit.type == HIT_ADVANCED) {
            showAdvanced = !showAdvanced;
            scroll.reset();
            showToast(showAdvanced ? "Advanced settings shown" : "Advanced settings hidden");
            return;
        }
        search.setFocused(false);
        if (!viewport.contains(mouseX, mouseY)) return;
        pendingHit = hit;
        activeWidget = hit != null && hit.type == HIT_CONTROL && hit.setting != null ? controlFor(hit.setting) : null;
        gestureActive = true;
        sliderEngaged = false;
        gesture.begin(activeWidget, mouseX, mouseY, scroll.value());
        if (activeWidget != null && !(activeWidget instanceof AppleSlider)) {
            activeWidget.onPress(mouseX, mouseY);
        }
    }

    private void activateWindowControl(int type) {
        if (type == HIT_WINDOW_RED) {
            close();
            return;
        }
        if (type == HIT_WINDOW_YELLOW) {
            minimizedWindow = !minimizedWindow;
            profileOpen = false;
            search.setFocused(false);
            openDropdown = null;
            if (!minimizedWindow) showToast("Settings restored");
            return;
        }
        if (type == HIT_WINDOW_GREEN) {
            // Green is useful from either state: clicking it while minimized restores directly into the larger view.
            if (minimizedWindow) {
                minimizedWindow = false;
                expandedWindow = true;
                showToast("Expanded layout restored");
            } else {
                expandedWindow = !expandedWindow;
                showToast(expandedWindow ? "Expanded layout" : "Standard layout");
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long timeSinceLastClick) {
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
        if (!gestureActive) return;
        boolean tap = gesture.kind == Gesture.PENDING;
        if (activeWidget != null) {
            Widget widget = activeWidget;
            activeWidget = null;
            if (widget instanceof AppleSlider && tap) {
                widget.onPress((int) gesture.startX, (int) gesture.startY);
            }
            widget.onRelease(mouseX, mouseY, tap && widget.contains(mouseX, mouseY));
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

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (profileOpen && keyCode == Keyboard.KEY_ESCAPE) {
            profileOpen = false;
            return;
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
        if (minimizedWindow) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER || keyCode == Keyboard.KEY_SPACE) {
                minimizedWindow = false;
                showToast("Settings restored");
            }
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
        if (hit.type == HIT_DISCLOSURE && hit.setting != null) {
            if (expandedRows.contains(hit.setting.id)) expandedRows.remove(hit.setting.id);
            else expandedRows.add(hit.setting.id);
            scroll.setMaximum(Math.max(0f, measuredContentHeight() - viewport.h));
            return;
        }
        if (hit.type == HIT_ROUTE && hit.setting != null) {
            openRoute(hit.setting.id);
            return;
        }
        if (hit.type == HIT_CHOICE && hit.setting != null) {
            try {
                setChoice(hit.setting.id, Integer.parseInt(hit.data));
                afterSettingChanged();
            } catch (NumberFormatException ignored) {
                // The click target is screen-owned; a malformed stale target safely does nothing.
            }
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

    /** Compact profile information popover; it is intentionally not a sixth navigation destination. */
    private void drawProfilePopup() {
        if (!profileOpen || minimizedWindow) return;
        float popupW = Math.min(278f, Math.max(210f, panel.w - 24f));
        float popupH = 84f;
        float popupX = panel.x + (panel.w - popupW) * .5f;
        float popupY = panel.y + Math.max(CHROME_H + 12f, (panel.h - popupH) * .5f);
        profilePopup.set(popupX, popupY, popupW, popupH);
        UIRender.shadow(popupX, popupY, popupW, popupH, 7f);
        UIRender.roundRectBorder(popupX, popupY, popupW, popupH, 7f, Theme.popup, Theme.popup,
                UIRender.withAlpha(Theme.accent, .60f));
        UIRender.text(fr(), "SKY VELOR PROFILE", popupX + 12f, popupY + 9f, Theme.textTertiary, .61f, false);
        profileClose.set(popupX + popupW - 24f, popupY + 5f, 19f, 17f);
        drawCloseGlyph(profileClose.x + profileClose.w * .5f, profileClose.y + profileClose.h * .5f, Theme.textSecondary);
        registerHit("profile-close", HIT_PROFILE_CLOSE, profileClose.x, profileClose.y, profileClose.w, profileClose.h, null, "");
        UIRender.line(popupX + 10f, popupY + 25f, popupX + popupW - 10f, popupY + 25f, 1f, Theme.divider);
        UIRender.text(fr(), "CREATOR", popupX + 13f, popupY + 35f, Theme.textTertiary, .53f, false);
        UIRender.text(fr(), PROFILE_CREATOR, popupX + 13f, popupY + 46f, Theme.textPrimary, .76f, false);
        float contributorsX = popupX + popupW * .48f;
        UIRender.text(fr(), "CONTRIBUTORS", contributorsX, popupY + 35f, Theme.textTertiary, .53f, false);
        String contributors = PROFILE_CONTRIBUTORS[0] + "  ·  " + PROFILE_CONTRIBUTORS[1];
        UIRender.text(fr(), contributors, contributorsX, popupY + 46f, Theme.textPrimary, .68f, false);
        UIRender.text(fr(), "Click outside to close", popupX + 13f, popupY + 66f, Theme.textTertiary, .52f, false);
    }

    // ------------------------------------------------------------------ Existing setting owners

    private boolean booleanValue(String id) {
        if ("no-block-particles".equals(id)) return AppleSettings.noBlockParticles;
        if ("disable-hotbar-scrolling".equals(id)) return AppleSettings.disableHotbarScrolling;
        if ("auto-sprint".equals(id)) return AssistSettings.autoSprint;
        if ("item-lock-swap".equals(id)) return ItemLockSwapSettings.enabled;
        if ("item-lock-icons".equals(id)) return ItemLockSwapSettings.showLockIcons;
        if ("item-lock-hotbar".equals(id)) return ItemLockSwapSettings.showIconsOnHotbar;
        if ("hide-potions".equals(id)) return AssistSettings.hidePotionEffects;
        if ("roman-numerals".equals(id)) return AssistSettings.romanNumerals;
        if ("tooltip-scroll".equals(id)) return AssistSettings.tooltipScroll;
        if ("rarity-highlight".equals(id)) return AssistSettings.rarityHighlight;
        if ("chat-qol".equals(id)) return ChatQolSettings.enabled;
        if ("chat-tabs".equals(id)) return ChatQolSettings.tabsEnabled;
        if ("chat-smooth".equals(id)) return ChatQolSettings.smoothMessages;
        if ("chat-hover".equals(id)) return ChatQolSettings.hoverHighlight;
        if ("mining-bps".equals(id)) return HudManager.MINING.enabled;
        if ("mining-ability-alert".equals(id)) return AssistSettings.miningAbilityAlert;
        if ("mining-puzzler".equals(id)) return AssistSettings.miningPuzzlerMarker;
        if ("mining-assistant-hud".equals(id)) return HudManager.MINING_ASSIST.enabled;
        if ("slayer-tracker".equals(id)) return AppleSettings.slayerTracker;
        if ("slayer-hud-route".equals(id)) return HudManager.SLAYER.enabled;
        if ("slayer-boss-timer".equals(id)) return AppleSettings.slayerBossTimer;
        if ("slayer-pricing".equals(id)) return AppleSettings.slayerPricing;
        if ("sea-hp-hud-route".equals(id)) return HudManager.SEA_HP.enabled;
        if ("jawbus".equals(id)) return AppleSettings.fishingJawbusAlert;
        if ("nearby-encounters".equals(id)) return AppleSettings.fishingNearbyEncounterAlert;
        if ("wardrobe-shortcut".equals(id)) return AssistSettings.wardrobeShortcut;
        if ("pets-shortcut".equals(id)) return AssistSettings.petsShortcut;
        if ("dungeon-map".equals(id)) return AppleSettings.dungeonMap;
        if ("dungeon-map-rotate".equals(id)) return AppleSettings.dungeonMapRotate;
        if ("terminal-assist".equals(id)) return AssistSettings.terminalHints;
        if ("terminal-hints".equals(id)) return AssistSettings.terminalHints;
        if ("terminal-hide-wrong".equals(id)) return AssistSettings.terminalHideIncorrect;
        if ("terminal-prevent-clicks".equals(id)) return AssistSettings.terminalPreventWrongClicks;
        if ("terminal-auto-click".equals(id)) return AssistSettings.terminalAutoClick;
        if ("terminal-waypoints".equals(id)) return AssistSettings.terminalWaypoints;
        if ("spirit-leap".equals(id)) return AppleSettings.leapMenu;
        if ("leap-key".equals(id)) return AppleSettings.leapKey;
        if ("dungeon-boss-hud".equals(id)) return HudManager.DUNGEON_BOSS_HP.enabled;
        if ("m7-overlay".equals(id)) return M7Settings.isOverlayEnabled();
        if ("dragon-alerts".equals(id)) return M7Settings.isDragonKillAlertEnabled();
        if ("m7-areas".equals(id)) return M7Settings.isStatueZonesEnabled();
        if ("dungeon-sync-enabled".equals(id)) return AssistSettings.dungeonSync;
        if ("dungeon-sync-debug".equals(id)) return AssistSettings.dungeonSyncDebug;
        if ("hud-stats".equals(id)) return HudManager.STATS.enabled;
        if ("hud-player-doll".equals(id)) return HudManager.DOLL.enabled;
        if ("hud-inventory".equals(id)) return HudManager.INVENTORY.enabled;
        if ("item-animation".equals(id)) return AppleSettings.itemAnimationEnabled;
        if ("performance-boost".equals(id)) return AppleSettings.experimentalPerformance;
        if ("animations".equals(id)) return AppleSettings.animations;
        if ("adaptive-quality".equals(id)) return AppleSettings.adaptive;
        if ("shadows".equals(id)) return AppleSettings.shadows;
        if ("smooth-scrolling".equals(id)) return AppleSettings.smoothScrolling;
        if ("mob-esp-enabled".equals(id)) return DungeonEspSettings.enabled;
        if ("door-esp-enabled".equals(id)) return DungeonEspSettings.doorsEnabled;
        return false;
    }

    private void setBoolean(String id, boolean value) {
        if ("no-block-particles".equals(id)) AppleSettings.noBlockParticles = value;
        else if ("disable-hotbar-scrolling".equals(id)) AppleSettings.disableHotbarScrolling = value;
        else if ("auto-sprint".equals(id)) AssistSettings.autoSprint = value;
        else if ("item-lock-swap".equals(id)) ItemLockSwapSettings.enabled = value;
        else if ("item-lock-icons".equals(id)) ItemLockSwapSettings.showLockIcons = value;
        else if ("item-lock-hotbar".equals(id)) ItemLockSwapSettings.showIconsOnHotbar = value;
        else if ("hide-potions".equals(id)) AssistSettings.hidePotionEffects = value;
        else if ("roman-numerals".equals(id)) AssistSettings.romanNumerals = value;
        else if ("tooltip-scroll".equals(id)) AssistSettings.tooltipScroll = value;
        else if ("rarity-highlight".equals(id)) AssistSettings.rarityHighlight = value;
        else if ("chat-qol".equals(id)) ChatQolSettings.enabled = value;
        else if ("chat-tabs".equals(id)) ChatQolSettings.tabsEnabled = value;
        else if ("chat-smooth".equals(id)) ChatQolSettings.smoothMessages = value;
        else if ("chat-hover".equals(id)) ChatQolSettings.hoverHighlight = value;
        else if ("mining-bps".equals(id)) HudManager.MINING.enabled = value;
        else if ("mining-ability-alert".equals(id)) AssistSettings.miningAbilityAlert = value;
        else if ("mining-puzzler".equals(id)) AssistSettings.miningPuzzlerMarker = value;
        else if ("mining-assistant-hud".equals(id)) HudManager.MINING_ASSIST.enabled = value;
        else if ("slayer-tracker".equals(id)) AppleSettings.slayerTracker = value;
        else if ("slayer-hud-route".equals(id)) HudManager.SLAYER.enabled = value;
        else if ("slayer-boss-timer".equals(id)) AppleSettings.slayerBossTimer = value;
        else if ("slayer-pricing".equals(id)) AppleSettings.slayerPricing = value;
        else if ("sea-hp-hud-route".equals(id)) HudManager.SEA_HP.enabled = value;
        else if ("jawbus".equals(id)) AppleSettings.fishingJawbusAlert = value;
        else if ("nearby-encounters".equals(id)) AppleSettings.fishingNearbyEncounterAlert = value;
        else if ("wardrobe-shortcut".equals(id)) AssistSettings.wardrobeShortcut = value;
        else if ("pets-shortcut".equals(id)) AssistSettings.petsShortcut = value;
        else if ("dungeon-map".equals(id)) AppleSettings.dungeonMap = value;
        else if ("dungeon-map-rotate".equals(id)) AppleSettings.dungeonMapRotate = value;
        else if ("terminal-assist".equals(id) || "terminal-hints".equals(id)) AssistSettings.terminalHints = value;
        else if ("terminal-hide-wrong".equals(id)) AssistSettings.terminalHideIncorrect = value;
        else if ("terminal-prevent-clicks".equals(id)) AssistSettings.terminalPreventWrongClicks = value;
        else if ("terminal-auto-click".equals(id)) AssistSettings.terminalAutoClick = value;
        else if ("terminal-waypoints".equals(id)) AssistSettings.terminalWaypoints = value;
        else if ("spirit-leap".equals(id)) AppleSettings.leapMenu = value;
        else if ("leap-key".equals(id)) AppleSettings.leapKey = value;
        else if ("dungeon-boss-hud".equals(id)) HudManager.DUNGEON_BOSS_HP.enabled = value;
        else if ("m7-overlay".equals(id)) M7Settings.setOverlayEnabled(value);
        else if ("dragon-alerts".equals(id)) M7Settings.setDragonKillAlertEnabled(value);
        else if ("m7-areas".equals(id)) M7Settings.setStatueZonesEnabled(value);
        else if ("dungeon-sync-enabled".equals(id)) AssistSettings.dungeonSync = value;
        else if ("dungeon-sync-debug".equals(id)) AssistSettings.dungeonSyncDebug = value;
        else if ("hud-stats".equals(id)) HudManager.STATS.enabled = value;
        else if ("hud-player-doll".equals(id)) HudManager.DOLL.enabled = value;
        else if ("hud-inventory".equals(id)) HudManager.INVENTORY.enabled = value;
        else if ("item-animation".equals(id)) AppleSettings.itemAnimationEnabled = value;
        else if ("performance-boost".equals(id)) AppleSettings.experimentalPerformance = value;
        else if ("animations".equals(id)) AppleSettings.animations = value;
        else if ("adaptive-quality".equals(id)) AppleSettings.adaptive = value;
        else if ("shadows".equals(id)) AppleSettings.shadows = value;
        else if ("smooth-scrolling".equals(id)) AppleSettings.smoothScrolling = value;
        else if ("mob-esp-enabled".equals(id)) DungeonEspSettings.enabled = value;
        else if ("door-esp-enabled".equals(id)) DungeonEspSettings.doorsEnabled = value;
    }

    private int intValue(String id) {
        if ("hurt-camera-shake".equals(id)) return AssistSettings.hurtCameraShake;
        if ("item-animation-size".equals(id)) return AppleSettings.itemAnimationSize;
        if ("item-animation-speed".equals(id)) return AppleSettings.itemAnimationSpeed;
        if ("item-animation-x".equals(id)) return AppleSettings.itemAnimationX;
        if ("item-animation-y".equals(id)) return AppleSettings.itemAnimationY;
        if ("item-animation-z".equals(id)) return AppleSettings.itemAnimationZ;
        if ("appearance-corners".equals(id)) return AppleSettings.cornerRadius;
        if ("appearance-opacity".equals(id)) return AppleSettings.glassOpacity;
        if ("global-scroll".equals(id)) return AppleSettings.scrollSpeed;
        return 0;
    }

    private void setInt(String id, int value) {
        if ("hurt-camera-shake".equals(id)) AssistSettings.hurtCameraShake = clamp(value, 0, 100);
        else if ("item-animation-size".equals(id)) AppleSettings.itemAnimationSize = clamp(value,
                AppleSettings.ITEM_ANIMATION_SIZE_MIN_PERCENT, AppleSettings.ITEM_ANIMATION_SIZE_MAX_PERCENT);
        else if ("item-animation-speed".equals(id)) AppleSettings.itemAnimationSpeed = clamp(value,
                AppleSettings.ITEM_ANIMATION_SPEED_MIN_PERCENT, AppleSettings.ITEM_ANIMATION_SPEED_MAX_PERCENT);
        else if ("item-animation-x".equals(id)) AppleSettings.itemAnimationX = clamp(value,
                AppleSettings.ITEM_ANIMATION_OFFSET_MIN, AppleSettings.ITEM_ANIMATION_OFFSET_MAX);
        else if ("item-animation-y".equals(id)) AppleSettings.itemAnimationY = clamp(value,
                AppleSettings.ITEM_ANIMATION_OFFSET_MIN, AppleSettings.ITEM_ANIMATION_OFFSET_MAX);
        else if ("item-animation-z".equals(id)) AppleSettings.itemAnimationZ = clamp(value,
                AppleSettings.ITEM_ANIMATION_OFFSET_MIN, AppleSettings.ITEM_ANIMATION_OFFSET_MAX);
        else if ("appearance-corners".equals(id)) AppleSettings.cornerRadius = clamp(value, 4, 18);
        else if ("appearance-opacity".equals(id)) AppleSettings.glassOpacity = clamp(value, 60, 100);
        else if ("global-scroll".equals(id)) AppleSettings.scrollSpeed = clamp(value, 50, 300);
        refreshReferenceTheme();
    }

    private int choiceValue(String id) {
        if ("appearance".equals(id)) return AppleSettings.darkMode ? 0 : 1;
        if ("appearance-accent".equals(id)) return clamp(AppleSettings.accent, 0, AppleSettings.ACCENT_NAMES.length - 1);
        return 0;
    }

    private void setChoice(String id, int value) {
        if ("appearance".equals(id)) AppleSettings.darkMode = value == 0;
        else if ("appearance-accent".equals(id)) AppleSettings.accent = clamp(value, 0, AppleSettings.ACCENT_NAMES.length - 1);
        refreshReferenceTheme();
    }

    private void openRoute(String id) {
        if ("item-animation-reset".equals(id)) {
            AppleSettings.itemAnimationSize = AppleSettings.DEFAULT_ITEM_ANIMATION_SIZE;
            AppleSettings.itemAnimationSpeed = AppleSettings.DEFAULT_ITEM_ANIMATION_SPEED;
            AppleSettings.itemAnimationX = AppleSettings.DEFAULT_ITEM_ANIMATION_X;
            AppleSettings.itemAnimationY = AppleSettings.DEFAULT_ITEM_ANIMATION_Y;
            AppleSettings.itemAnimationZ = AppleSettings.DEFAULT_ITEM_ANIMATION_Z;
            showToast("Item animation reset to vanilla");
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if ("hud-editor".equals(id)) {
            minecraft.displayGuiScreen(new HudEditorScreen(this));
        } else if ("hud-stats".equals(id)) {
            minecraft.displayGuiScreen(HudEditorScreen.openElement(this, HudManager.STATS));
        } else if ("hud-player-doll".equals(id)) {
            minecraft.displayGuiScreen(HudEditorScreen.openElement(this, HudManager.DOLL));
        } else if ("hud-inventory".equals(id)) {
            minecraft.displayGuiScreen(HudEditorScreen.openElement(this, HudManager.INVENTORY));
        } else if ("item-animation".equals(id)) {
            minecraft.displayGuiScreen(HudEditorScreen.openItemAnimation(this));
        } else if ("item-lock-swap".equals(id) || "item-swap-lines".equals(id)) {
            minecraft.displayGuiScreen(FeatureSettingsEditors.itemLockSwap(this));
        } else if ("inventory-assist".equals(id)) {
            minecraft.displayGuiScreen(FeatureSettingsEditors.inventoryAssist(this));
        } else if ("chat-qol".equals(id)) {
            minecraft.displayGuiScreen(new ChatEditorScreen(this));
        } else if ("mining-bps".equals(id)) {
            minecraft.displayGuiScreen(HudEditorScreen.openElement(this, HudManager.MINING));
        } else if ("mining-assistant-settings".equals(id)) {
            minecraft.displayGuiScreen(FeatureSettingsEditors.miningAssistant(this));
        } else if ("slayer-tracker".equals(id)) {
            minecraft.displayGuiScreen(FeatureSettingsEditors.slayer(this));
        } else if ("slayer-hud-route".equals(id)) {
            minecraft.displayGuiScreen(HudEditorScreen.openElement(this, HudManager.SLAYER));
        } else if ("fishing-hud-route".equals(id)) {
            minecraft.displayGuiScreen(HudEditorScreen.openElement(this, HudManager.FISHING));
        } else if ("catch-counter".equals(id)) {
            minecraft.displayGuiScreen(FeatureSettingsEditors.catchCounter(this));
        } else if ("sea-hp-hud-route".equals(id)) {
            minecraft.displayGuiScreen(HudEditorScreen.openElement(this, HudManager.SEA_HP));
        } else if ("movement-shortcuts".equals(id)) {
            minecraft.displayGuiScreen(FeatureSettingsEditors.movementShortcuts(this));
        } else if ("dungeon-map".equals(id)) {
            minecraft.displayGuiScreen(HudEditorScreen.openDungeonMap(this));
        } else if ("terminal-assist".equals(id)) {
            minecraft.displayGuiScreen(FeatureSettingsEditors.terminalAssist(this));
        } else if ("spirit-leap".equals(id)) {
            minecraft.displayGuiScreen(FeatureSettingsEditors.spiritLeap(this));
        } else if ("dungeon-boss-hud".equals(id)) {
            minecraft.displayGuiScreen(HudEditorScreen.openElement(this, HudManager.DUNGEON_BOSS_HP));
        } else if ("m7-overlay".equals(id)) {
            minecraft.displayGuiScreen(FeatureSettingsEditors.m7Overlay(this));
        } else if ("door-esp".equals(id)) {
            minecraft.displayGuiScreen(DungeonEspEditorSpecs.doorEditor(this));
        } else if ("mob-esp".equals(id)) {
            minecraft.displayGuiScreen(DungeonEspEditorSpecs.mobEditor(this));
        } else if ("appearance".equals(id)) {
            minecraft.displayGuiScreen(FeatureSettingsEditors.appearance(this));
        } else if ("performance".equals(id)) {
            minecraft.displayGuiScreen(FeatureSettingsEditors.performance(this));
        } else if ("mod-settings".equals(id)) {
            minecraft.displayGuiScreen(FeatureSettingsEditors.modSettings(this));
        } else if ("commands".equals(id) || "sync-diagnostics".equals(id) || "dungeon-sync-debug".equals(id)) {
            minecraft.displayGuiScreen(FeatureSettingsEditors.commands(this));
        } else {
            showToast("This setting is already available on this screen");
        }
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    // ------------------------------------------------------------------ Reference line-art vocabulary

    private void drawCube(float x, float y, float size, int color) {
        float s = size * .44f;
        UIRender.roundRect(x, y + s, s, s, 1f, color, color, .1f);
        UIRender.roundRect(x + s + 1f, y, s, s, 1f, UIRender.mix(color, 0xFFFFFFFF, .16f),
                UIRender.mix(color, 0xFFFFFFFF, .16f), .1f);
        UIRender.roundRect(x + s + 1f, y + s + 1f, s, s, 1f, UIRender.mix(color, 0xFF000000, .22f),
                UIRender.mix(color, 0xFF000000, .22f), .1f);
        UIRender.line(x + s, y + s, x + s + 1f, y + s + 1f, 1f, 0xCCFFFFFF);
    }

    private void drawRailGlyph(int index, float x, float y, int color) {
        if (index == 0) {
            UIRender.line(x, y + 6f, x + 6f, y + 1f, 1.2f, color);
            UIRender.line(x + 6f, y + 1f, x + 12f, y + 6f, 1.2f, color);
            UIRender.rect(x + 2f, y + 6f, 8f, 6f, color);
        } else if (index == 1) {
            UIRender.ring(x + 6f, y + 6f, 5f, 3.6f, color);
            UIRender.line(x + 6f, y + 6f, x + 10f, y + 3f, 1.2f, color);
        } else if (index == 2) {
            UIRender.rect(x + 1f, y + 2f, 10f, 8f, UIRender.withAlpha(color, .38f));
            UIRender.roundRectRing(x + 1f, y + 2f, 10f, 8f, 1f, 1f, color);
            UIRender.line(x + 3f, y + 11f, x + 9f, y + 11f, 1f, color);
        } else if (index == 3) {
            UIRender.circle(x + 6f, y + 5f, 4.5f, UIRender.withAlpha(color, .35f));
            UIRender.ring(x + 6f, y + 5f, 5f, 3.6f, color);
            UIRender.line(x + 2f, y + 12f, x + 10f, y + 12f, 1f, color);
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

    private void drawProfileGlyph(float x, float y, int color) {
        UIRender.ring(x + 5f, y + 3f, 2.5f, 1.7f, color);
        UIRender.roundRectRing(x + 1f, y + 7f, 8f, 5f, 2f, 1f, color);
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
        UIRender.roundRect(x, y, 32f, 17f, 8.5f, fill, fill, .3f);
        UIRender.circle(x + (on ? 23f : 9f), y + 8.5f, 6f, 0xFFFFFFFF);
    }

    private void drawShieldGlyph(float x, float y, int color) {
        UIRender.line(x + 5f, y, x + 10f, y + 2f, 1.2f, color);
        UIRender.line(x + 10f, y + 2f, x + 9f, y + 8f, 1.2f, color);
        UIRender.line(x + 9f, y + 8f, x + 5f, y + 11f, 1.2f, color);
        UIRender.line(x + 5f, y + 11f, x + 1f, y + 8f, 1.2f, color);
        UIRender.line(x + 1f, y + 8f, x, y + 2f, 1.2f, color);
        UIRender.line(x, y + 2f, x + 5f, y, 1.2f, color);
    }

    private void drawFilterGlyph(float x, float y, int color) {
        UIRender.line(x, y - 4f, x + 8f, y - 4f, 1.2f, color);
        UIRender.line(x + 1f, y - 1f, x + 7f, y - 1f, 1.2f, color);
        UIRender.line(x + 3f, y + 2f, x + 5f, y + 2f, 1.2f, color);
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

    private void drawChevron(float x, float y, int color, boolean down) {
        if (down) {
            UIRender.line(x - 2f, y - 1f, x + 1f, y + 2f, 1f, color);
            UIRender.line(x + 1f, y + 2f, x + 4f, y - 1f, 1f, color);
        } else {
            UIRender.line(x, y - 3f, x + 3f, y, 1f, color);
            UIRender.line(x + 3f, y, x, y + 3f, 1f, color);
        }
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
