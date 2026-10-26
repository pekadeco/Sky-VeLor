package com.arena.pekaclient.gui;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.hud.HudElement;
import com.arena.pekaclient.hud.HudManager;
import com.arena.pekaclient.hud.HudPlacement;
import com.arena.pekaclient.render.Anim;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import com.arena.pekaclient.widget.AppleButton;
import com.arena.pekaclient.widget.AppleDropdown;
import com.arena.pekaclient.widget.AppleSlider;
import com.arena.pekaclient.widget.AppleToggle;
import com.arena.pekaclient.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * HUD editor. Click an on-screen HUD element to select it, then drag it to move it; disabled elements remain
 * dimmed, selectable placeholders. The Dungeon Map has its own unclamped placement controls; Item Animation
 * is opened in its separate renderer-only panel. Every change is saved when the editor closes.
 *
 * Works with mouse and touch (PojavLauncher): a tap is a click, a drag moves an element or scrolls
 * the card, and a swipe never activates a control.
 */
public final class HudEditorScreen extends AppleScreen {

    private static final float BAR_H = 30f;
    private static final float BAR_TOP = 8f;
    private static final float CARD_PAD = HudEditorLayout.CARD_PADDING;
    private static final float CONTROL_W = HudSettingsBuilder.CONTROL_W;
    private static final float CONTROL_H = HudSettingsBuilder.CONTROL_H;
    private static final float SNAP = 4f;
    private static final float TOUCH_MIN = 28f;

    /** One inspector row, laid out by the shared fixed-height left-label/right-control grid. */
    private static final class Entry {
        final String label;
        final Widget control;
        final boolean fullWidth;
        final boolean stretchControl;
        final HudEditorLayout.Bounds bounds = new HudEditorLayout.Bounds();

        Entry(String label, Widget control) {
            this(label, control, label == null, control instanceof AppleSlider || control instanceof AppleDropdown
                    || control instanceof HorizontalDragWidget);
        }

        Entry(String label, Widget control, boolean fullWidth, boolean stretchControl) {
            this.label = label;
            this.control = control;
            this.fullWidth = fullWidth;
            this.stretchControl = stretchControl;
        }
    }

    private final GuiScreen parent;
    private final Anim openAnim = new Anim(0f);
    private final List<Entry> entries = new ArrayList<Entry>();
    private final List<Widget> bar = new ArrayList<Widget>();
    private final AppleButton resetAllBtn;
    private final AppleButton doneBtn;
    private final ScrollState scroll = new ScrollState();
    private final Gesture gesture = new Gesture();
    private final HudEditorLayout.Metrics inspectorLayout = new HudEditorLayout.Metrics();
    /** Shared clamp authority for the editor shell; every nested inspector is then measured from this safe rect. */
    private final GuiLayoutToolkit.Rect editorPanel = new GuiLayoutToolkit.Rect();

    private enum EditorTarget { HUD_ELEMENT, DUNGEON_MAP }

    private HudElement selected = HudManager.MINING;
    private EditorTarget target = EditorTarget.HUD_ELEMENT;
    private long lastNs;
    private int scaleKey = -1;
    private float sf = 2f;

    // Layout (GUI pixels, recomputed every frame).
    private float wx;
    private float wy;
    private float wh;
    private float ww;
    private float barX;
    private float barY;
    private float barW;
    private float cardX;
    private float cardY;
    private float cardW;
    private float cardH;
    /** Card offset from its default corner, set by dragging the header. */
    private float cardDx;
    private float cardDy;
    private boolean cardHidden;
    private boolean draggingCard;
    private boolean draggingScrollbar;
    private float scrollbarGrabOffset;
    private boolean draggingDungeonMap;
    private float dungeonMapGrabX;
    private float dungeonMapGrabY;
    private float cardGrabX;
    private float cardGrabY;
    private static final float HEAD_H = HudEditorLayout.HEADER_HEIGHT;
    private float visibleTop;
    private float visibleBottom;
    private float maxScroll;

    // Interaction state.
    private Widget active;
    private boolean gestureActive;
    private boolean sliderEngaged;
    private HudElement dragging;
    private float grabX;
    private float grabY;
    private int guides;
    private boolean dirty;

    public HudEditorScreen(GuiScreen parent) {
        this.parent = parent;
        this.resetAllBtn = new AppleButton("Reset all", 74f, 22f, false, this::resetAll);
        this.doneBtn = new AppleButton("Done", 56f, 22f, true, this::close);
        bar.add(doneBtn);
        bar.add(resetAllBtn);
        openAnim.to(1f);
    }

    /** Compatibility route: Item Animation now owns a standalone panel rather than a HUD-editor target. */
    public static GuiScreen openItemAnimation(GuiScreen parent) {
        return new ItemAnimationEditorScreen(parent);
    }

    /** Opens the standard editor on one ordinary HUD element, keeping its existing saved placement untouched. */
    public static HudEditorScreen openElement(GuiScreen parent, HudElement element) {
        HudEditorScreen editor = new HudEditorScreen(parent);
        if (element != null) {
            editor.selected = element;
        }
        editor.target = EditorTarget.HUD_ELEMENT;
        return editor;
    }

    /** Opens the standard editor on the Dungeon Map's existing position/options panel. */
    public static HudEditorScreen openDungeonMap(GuiScreen parent) {
        HudEditorScreen editor = new HudEditorScreen(parent);
        editor.target = EditorTarget.DUNGEON_MAP;
        return editor;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        HudManager.ensureLoaded();
        rebuild();
    }

    /** Keep the first-person render/tick path live behind the editor so slider changes are immediately visible. */
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
        HudManager.save();
        AppleSettings.save();
        // The editor combines HUD primitives and entity rendering. Always leave a neutral
        // GUI baseline behind; an AppleSettingsScreen parent reapplies its fixed Theme on init/draw.
        UIRender.restoreVanillaGuiState();
    }

    // ------------------------------------------------------------------ inspector

    private void select(HudElement element) {
        if (target == EditorTarget.HUD_ELEMENT && element == selected && !cardHidden) return;
        selected = element;
        target = EditorTarget.HUD_ELEMENT;
        cardHidden = false;
        resetInspector();
    }

    private void selectDungeonMap() {
        if (target == EditorTarget.DUNGEON_MAP && !cardHidden) return;
        target = EditorTarget.DUNGEON_MAP;
        cardHidden = false;
        resetInspector();
    }

    private void resetInspector() {
        openDropdown = null;
        scroll.reset();
        rebuild();
    }

    private void changed() {
        dirty = true;
    }

    /** Rebuilds the inspector for an ordinary HUD element or the map. */
    private void rebuild() {
        entries.clear();
        // The inspector follows the element clicked on the canvas; it deliberately has no target list or sidebar.
        if (target == EditorTarget.DUNGEON_MAP) {
            buildDungeonMapEntries();
            return;
        }
        List<HudSettingsBuilder.Item> items = HudSettingsBuilder.build(selected, this::changed, true);
        for (HudSettingsBuilder.Item item : items) {
            entries.add(new Entry(item.label, item.control));
        }
    }

    private void buildDungeonMapEntries() {
        entries.add(new Entry("Show", new AppleToggle(() -> AppleSettings.dungeonMap, value -> {
            AppleSettings.dungeonMap = value;
            changed();
        })));
        entries.add(new Entry("Rotate map", new AppleToggle(() -> AppleSettings.dungeonMapRotate, value -> {
            AppleSettings.dungeonMapRotate = value;
            changed();
        })));
        entries.add(new Entry("Use scale", new AppleToggle(() -> AppleSettings.dungeonMapScaleOn, value -> {
            AppleSettings.dungeonMapScaleOn = value;
            if (!value) AppleSettings.dungeonMapScale = 100;
            changed();
        })));
        entries.add(new Entry("Scale", new AppleSlider(AppleSettings.DUNGEON_MAP_SCALE_MIN,
                AppleSettings.DUNGEON_MAP_SCALE_MAX, 5, "%", CONTROL_W,
                AppleSettings::effectiveDungeonMapScale, value -> {
                    AppleSettings.dungeonMapScale = value;
                    changed();
                }).gate(() -> AppleSettings.dungeonMapScaleOn)));
        entries.add(new Entry("Map zoom", new AppleSlider(AppleSettings.DUNGEON_MAP_ZOOM_MIN,
                AppleSettings.DUNGEON_MAP_ZOOM_MAX, 5, "%", CONTROL_W,
                AppleSettings::effectiveDungeonMapZoom, value -> {
                    AppleSettings.dungeonMapZoom = value;
                    changed();
                })));
        int xRange = Math.max(128, width);
        int yRange = Math.max(128, height);
        entries.add(new Entry("X position", new AppleSlider(-xRange, xRange * 2, 1, " px", CONTROL_W,
                this::dungeonMapLeft, this::setDungeonMapLeft)));
        entries.add(new Entry("Y position", new AppleSlider(-yRange, yRange * 2, 1, " px", CONTROL_W,
                this::dungeonMapTop, this::setDungeonMapTop)));
        entries.add(new Entry("Player names", new AppleToggle(() -> AppleSettings.dungeonMapShowPlayerNames, value -> {
            AppleSettings.dungeonMapShowPlayerNames = value;
            changed();
        })));
        entries.add(new Entry("Puzzle names", new AppleToggle(() -> AppleSettings.dungeonMapShowPuzzleName, value -> {
            AppleSettings.dungeonMapShowPuzzleName = value;
            changed();
        })));
        entries.add(new Entry("Party announce (chat)", new AppleToggle(() -> AppleSettings.partyPuzzleAnnounce, value -> {
            AppleSettings.partyPuzzleAnnounce = value;
            changed();
        })));
        entries.add(new Entry(null, new AppleButton("Reset map position", CONTROL_W, CONTROL_H, false, () -> {
            AppleSettings.resetDungeonMapPosition();
            changed();
        })));
        entries.add(new Entry(null, new AppleButton("Reset map size", CONTROL_W, CONTROL_H, false, () -> {
            AppleSettings.dungeonMapScaleOn = true;
            AppleSettings.dungeonMapScale = 100;
            changed();
        })));
        entries.add(new Entry(null, new AppleButton("Reset map zoom", CONTROL_W, CONTROL_H, false, () -> {
            AppleSettings.dungeonMapZoom = 100;
            changed();
        })));
    }

    private void resetAll() {
        HudManager.resetAll();
        AppleSettings.resetDungeonMapPosition();
        AppleSettings.dungeonMapScale = 100;
        AppleSettings.dungeonMapScaleOn = true;
        AppleSettings.dungeonMapZoom = 100;
        openDropdown = null;
        rebuild();
        HudManager.save();
        AppleSettings.save();
    }

    private void close() {
        HudManager.save();
        AppleSettings.save();
        Minecraft.getMinecraft().displayGuiScreen(parent);
    }

    // ------------------------------------------------------------------ layout

    private void layout() {
        GuiLayoutToolkit.clampPanel(editorPanel, 6f, 6f, Math.max(1f, width - 12f),
                Math.max(1f, height - 12f), width, height);
        wx = editorPanel.x;
        wy = editorPanel.y;
        ww = editorPanel.w;
        wh = editorPanel.h;

        barW = Math.max(1f, Math.min(ww - 16f, 520f));
        barX = wx + (ww - barW) * 0.5f;
        barY = wy + BAR_TOP;
        doneBtn.x = barX + barW - 8f - doneBtn.w;
        doneBtn.y = barY + (BAR_H - doneBtn.h) * 0.5f;
        resetAllBtn.x = doneBtn.x - 6f - resetAllBtn.w;
        resetAllBtn.y = barY + (BAR_H - resetAllBtn.h) * 0.5f;

        HudEditorLayout.update(inspectorLayout, wx, wy, ww, wh, barY + BAR_H + 8f, entries.size(),
                scroll.value(), cardHidden, cardDx, cardDy);
        scroll.setMax(inspectorLayout.maxScroll);
        // setMax may clamp a prior scroll position after a group collapsed, so use the final value for rows.
        HudEditorLayout.update(inspectorLayout, wx, wy, ww, wh, barY + BAR_H + 8f, entries.size(),
                scroll.value(), cardHidden, cardDx, cardDy);
        cardX = inspectorLayout.cardX;
        cardY = inspectorLayout.cardY;
        cardW = inspectorLayout.cardW;
        cardH = inspectorLayout.cardH;
        visibleTop = inspectorLayout.visibleTop;
        visibleBottom = inspectorLayout.visibleBottom;
        maxScroll = inspectorLayout.maxScroll;

        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            Widget control = entry.control;
            HudEditorLayout.row(inspectorLayout, i, entry.fullWidth, control.w, entry.stretchControl,
                    control.h, entry.bounds);
            control.x = entry.bounds.controlX;
            control.y = entry.bounds.controlY;
            control.w = entry.bounds.controlW;
        }
    }

    private float hideX() {
        return cardX + cardW - CARD_PAD - 44f;
    }

    private float hideY() {
        return cardY + (HEAD_H - 16f) * 0.5f;
    }

    private boolean inScrollbar(float mx, float my) {
        return maxScroll > 0f && inside(mx, my, inspectorLayout.scrollbarX - 4f, visibleTop,
                inspectorLayout.scrollbarW + 8f, inspectorLayout.viewportH);
    }

    private void setScrollbarFromY(float my) {
        float travel = Math.max(0f, inspectorLayout.viewportH - inspectorLayout.scrollbarH);
        float thumbTop = Math.max(visibleTop, Math.min(visibleTop + travel, my - scrollbarGrabOffset));
        float fraction = travel <= 0f ? 0f : (thumbTop - visibleTop) / travel;
        scroll.setImmediate(fraction * maxScroll);
        layout();
    }

    private boolean inCardHeader(float mx, float my) {
        return inside(mx, my, cardX, cardY, cardW, HEAD_H);
    }

    private boolean inHideButton(float mx, float my) {
        return inside(mx, my, hideX(), hideY(), 44f, 16f);
    }

    private void moveCard(int mx, int my) {
        float nx = Math.max(wx, Math.min(wx + ww - cardW, mx - cardGrabX));
        float ny = Math.max(wy, Math.min(wy + wh - cardH, my - cardGrabY));
        cardDx = nx - (wx + 10f);
        cardDy = ny - (wy + wh - 10f - cardH);
        layout();
    }

    /** Uses the feature names shown in the hub where a HUD implementation uses a shorter internal caption. */
    private String selectedSettingsTitle() {
        if (target == EditorTarget.DUNGEON_MAP) return "Dungeon Map Settings";
        if (selected == null) return "HUD Settings";
        if ("doll".equals(selected.id)) return "Player Doll Settings";
        if ("inventory".equals(selected.id)) return "Inventory HUD Settings";
        if ("fishing".equals(selected.id)) return "Fishing HUD Settings";
        if ("slayer".equals(selected.id)) return "Slayer HUD Settings";
        return selected.name + " Settings";
    }

    /** Title on the left, Hide or Show on the right. Drag the header to move the card. */
    private void drawCardHeader(int mx, int my) {
        FontRenderer fr = fr();
        String title = selectedSettingsTitle();
        title = fit(fr, title, Math.max(0f, hideX() - cardX - CARD_PAD - 8f));
        UIRender.text(fr, title, cardX + CARD_PAD, cardY + (HEAD_H - 9f) * 0.5f + 1f,
                Theme.textPrimary, 1f, false);
        boolean hot = inHideButton(mx, my);
        float bx = hideX();
        float by = hideY();
        UIRender.roundRect(bx, by, 44f, 16f, 8f, Theme.buttonFill, Theme.buttonFill, 1f);
        String label = cardHidden ? "Show" : "Hide";
        UIRender.text(fr, label, bx + (44f - fr.getStringWidth(label)) * 0.5f, by + 4f,
                hot ? Theme.accent : Theme.textSecondary, 1f, false);
    }

    private boolean inCardView(float mx, float my) {
        return inside(mx, my, cardX, visibleTop, cardW, visibleBottom - visibleTop);
    }

    private Widget barWidgetAt(int mx, int my) {
        for (Widget button : bar) {
            if (button.contains(mx, my)) return button;
        }
        return null;
    }

    /** The inspector widget under the pointer. HUD elements are selected directly on the canvas instead. */
    private Widget widgetAt(int mx, int my) {
        if (!inCardView(mx, my)) return null;
        for (Entry e : entries) {
            if (e.control.contains(mx, my)) return e.control;
        }
        return null;
    }

    private void beginWidgetGesture(Widget widget, int mx, int my) {
        active = widget;
        sliderEngaged = false;
        gestureActive = true;
        gesture.begin(widget, mx, my, scroll.value());
        if (!(widget instanceof AppleSlider) && !(widget instanceof HorizontalDragWidget)) {
            widget.onPress(mx, my);
        }
    }

    /**
     * Finds an on-screen element, including every disabled dimmed placeholder. Each target is at least TOUCH_MIN px
     * for touch use; repeated clicks cycle overlapping elements so the former list is never needed for reachability.
     */
    private HudElement hitElement(int mx, int my) {
        boolean[] hits = new boolean[HudManager.ELEMENTS.length];
        for (int i = 0; i < HudManager.ELEMENTS.length; i++) {
            HudElement h = HudManager.ELEMENTS[i];
            float w = h.pixelWidth();
            float hh = h.pixelHeight();
            float padX = Math.max(6f, (TOUCH_MIN - w) * 0.5f);
            float padY = Math.max(6f, (TOUCH_MIN - hh) * 0.5f);
            hits[i] = inside(mx, my, h.leftFor(width) - padX, h.topFor(height) - padY,
                    w + padX * 2f, hh + padY * 2f);
        }
        int index = HudEditorSelectionPolicy.nextIndex(selectedElementIndex(), hits);
        return index < 0 ? null : HudManager.ELEMENTS[index];
    }

    private int selectedElementIndex() {
        if (target != EditorTarget.HUD_ELEMENT) return -1;
        for (int i = 0; i < HudManager.ELEMENTS.length; i++) {
            if (HudManager.ELEMENTS[i] == selected) return i;
        }
        return -1;
    }

    /**
     * Moves the dragged element so its grab point stays under the pointer. The placement clamps the
     * position to the screen and snaps it to the centre and edges.
     */
    private void dragTo(int mx, int my) {
        HudElement e = dragging;
        int left = Math.round(mx - grabX);
        int top = Math.round(my - grabY);
        guides = e.place.setFromAbsolute(width, height, e.pixelWidth(), e.pixelHeight(),
                left, top, true, (int) SNAP);
        changed();
    }

    private void updateAnims(float dt) {
        openAnim.update(dt, 10f);
        for (Widget b : bar) {
            b.update(dt);
        }
        for (Entry e : entries) {
            e.control.update(dt);
        }
        scroll.update(dt, gestureActive && gesture.kind == Gesture.SCROLL,
                Theme.animations && AppleSettings.smoothScrolling, Theme.speed);
    }

    private float scrollSpeed() {
        return AppleSettings.guiScrollMultiplier();
    }

    // ------------------------------------------------------------------ drawing

    private void refreshScale(Minecraft mc) {
        int key = mc.displayWidth * 31 + mc.displayHeight * 7 + mc.gameSettings.guiScale;
        if (key != scaleKey) {
            scaleKey = key;
            sf = new ScaledResolution(mc).getScaleFactor();
        }
    }

    @Override
    public void drawScreen(int mx, int my, float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        long now = System.nanoTime();
        float dt = lastNs == 0L ? 1f / 60f : Math.min(0.1f, (now - lastNs) / 1.0e9f);
        lastNs = now;

        refreshScale(mc);
        updateAnims(dt);
        layout();

        int q = AppleSettings.experimentalPerformance ? 2 : AppleSettings.curveQuality;
        UIRender.segCap = AppleSettings.CURVE_SEGMENTS[q];
        UIRender.shadows = AppleSettings.shadows && !AppleSettings.experimentalPerformance;

        drawDefaultBackground();
        float open = openAnim.get();

        // The veil and the elements are separate frames. HudManager.renderEditor manages its own GL state.
        UIRender.beginFrame();
        try {
            UIRender.alphaMul = open;
            UIRender.rect(0f, 0f, width, height, Theme.veil);
        } finally {
            UIRender.endFrame();
        }

        ScaledResolution sr = new ScaledResolution(mc);
        HudManager.renderEditor(sr, mx, my);
        drawDungeonMapPlacementCanvas(mx, my);

        UIRender.beginFrame();
        try {
            UIRender.alphaMul = open;
            drawSelection();
            if ((dragging != null || draggingDungeonMap) && guides != 0) {
                drawGuides();
            }
            drawBar(mx, my);
            drawCard(mx, my);
            UIRender.alphaMul = open;
            drawOpenDropdown(mx, my, 0f, height, width, 0f);
        } finally {
            UIRender.alphaMul = 1f;
            UIRender.endFrame();
        }
    }

    /** Thin rounded ring around the selected HUD target or the dedicated map placement canvas. */
    private void drawSelection() {
        if (target == EditorTarget.DUNGEON_MAP) {
            int[] bounds = new int[3];
            dungeonMapBounds(bounds);
            UIRender.roundRectRing(bounds[0] - 3f, bounds[1] - 3f, bounds[2] + 6f, bounds[2] + 6f,
                    4f, 1f, Theme.accent);
            return;
        }
        HudElement element = selected;
        float x = element.leftFor(width) - 3f;
        float y = element.topFor(height) - 3f;
        float w = element.pixelWidth() + 6f;
        float h = element.pixelHeight() + 6f;
        UIRender.roundRectRing(x, y, w, h, element.radius * element.scale + 3f, 1f, Theme.accent);
        int dot = Theme.accent;
        UIRender.circle(x, y, 2.2f, dot);
        UIRender.circle(x + w, y, 2.2f, dot);
        UIRender.circle(x, y + h, 2.2f, dot);
        UIRender.circle(x + w, y + h, 2.2f, dot);
    }

    private void drawGuides() {
        int c = Theme.accent;
        if ((guides & HudPlacement.GUIDE_CENTER_X) != 0) {
            UIRender.rect(width * 0.5f - 0.5f, 0f, 1f, height, c);
        }
        if ((guides & HudPlacement.GUIDE_CENTER_Y) != 0) {
            UIRender.rect(0f, height * 0.5f - 0.5f, width, 1f, c);
        }
        if ((guides & HudPlacement.GUIDE_LEFT) != 0) {
            UIRender.rect(0f, 0f, 1f, height, c);
        }
        if ((guides & HudPlacement.GUIDE_RIGHT) != 0) {
            UIRender.rect(width - 1f, 0f, 1f, height, c);
        }
        if ((guides & HudPlacement.GUIDE_TOP) != 0) {
            UIRender.rect(0f, 0f, width, 1f, c);
        }
        if ((guides & HudPlacement.GUIDE_BOTTOM) != 0) {
            UIRender.rect(0f, height - 1f, width, 1f, c);
        }
    }

    private void drawBar(int mx, int my) {
        UIRender.shadow(barX, barY, barW, BAR_H, Theme.radius);
        UIRender.roundRectBorder(barX, barY, barW, BAR_H, Theme.radius,
                Theme.windowTop, Theme.windowBottom, Theme.border);

        float cy = barY + BAR_H * 0.5f;
        // The compact editor header exposes one explicit Close icon; no fake collapse/zoom controls are drawn.
        FocusedHeaderCloseButton.draw(barX + 14f, cy, mx, my);

        FontRenderer fr = fr();
        // The editor has no sidebar/list: this single line is the persistent selection instruction.
        String instruction = "Click an on-screen HUD element to select it; drag to move it.";
        float instructionX = barX + 58f;
        float instructionW = Math.max(1f, resetAllBtn.x - instructionX - 8f);
        UIRender.text(fr, fit(fr, instruction, instructionW / .72f), instructionX, cy - 3.2f,
                Theme.textSecondary, .72f, false);
        doneBtn.draw(mx, my);
        resetAllBtn.draw(mx, my);
    }

    private void drawCard(int mx, int my) {
        UIRender.shadow(cardX, cardY, cardW, cardH, Theme.radius);
        UIRender.roundRectBorder(cardX, cardY, cardW, cardH, Theme.radius,
                Theme.windowTop, Theme.windowBottom, Theme.border);
        drawCardHeader(mx, my);
        if (cardHidden) {
            return;
        }

        FontRenderer fr = fr();
        boolean hover = inCardView(mx, my);
        int pmx = hover ? mx : -10000;
        int pmy = hover ? my : -10000;
        float viewH = visibleBottom - visibleTop;

        UIRender.scissorBegin(cardX, visibleTop, cardW, viewH, sf);
        try {
            for (Entry entry : entries) {
                Widget control = entry.control;
                if (control.y + control.h < visibleTop || control.y > visibleBottom) {
                    continue;
                }
                if (entry.label != null && entry.bounds.labelW > 0f) {
                    String label = fit(fr, entry.label, entry.bounds.labelW);
                    UIRender.text(fr, label, entry.bounds.labelX, entry.bounds.labelY,
                            Theme.textSecondary, 0.9f, false);
                }
                control.draw(pmx, pmy);
            }
        } finally {
            UIRender.scissorEnd();
        }

        if (maxScroll > 0f) {
            UIRender.roundRect(inspectorLayout.scrollbarX, inspectorLayout.scrollbarY,
                    inspectorLayout.scrollbarW, inspectorLayout.scrollbarH, inspectorLayout.scrollbarW * 0.5f,
                    Theme.textTertiary, Theme.textTertiary, 1f);
        }
    }

    private void dungeonMapBounds(int[] out) {
        int size = AppleSettings.dungeonMapPixelSize(width, height);
        out[0] = AppleSettings.dungeonMapLeft(width, height, size);
        out[1] = AppleSettings.dungeonMapTop(width, height, size);
        out[2] = size;
    }

    private int dungeonMapLeft() {
        int size = AppleSettings.dungeonMapPixelSize(width, height);
        return AppleSettings.dungeonMapLeft(width, height, size);
    }

    private int dungeonMapTop() {
        int size = AppleSettings.dungeonMapPixelSize(width, height);
        return AppleSettings.dungeonMapTop(width, height, size);
    }

    private void setDungeonMapLeft(int left) {
        int size = AppleSettings.dungeonMapPixelSize(width, height);
        AppleSettings.setDungeonMapLeft(width, height, size, left, false, 0);
        changed();
    }

    private void setDungeonMapTop(int top) {
        int size = AppleSettings.dungeonMapPixelSize(width, height);
        AppleSettings.setDungeonMapTop(width, height, size, top, false, 0);
        changed();
    }

    /** A placement canvas stays available outside a dungeon, where no server map item can be drawn. */
    private void drawDungeonMapPlacementCanvas(int mx, int my) {
        int[] bounds = new int[3];
        dungeonMapBounds(bounds);
        int x = bounds[0], y = bounds[1], size = bounds[2];
        int inner = Math.max(0, size - 4);
        UIRender.beginFrame();
        try {
            boolean hovering = mx >= x && mx <= x + size && my >= y && my <= y + size;
            int border = target == EditorTarget.DUNGEON_MAP || hovering ? Theme.accent : Theme.border;
            int mapFrame = UIRender.withAlpha(Theme.card, .88f);
            int mapSurface = UIRender.mix(Theme.card, Theme.windowBottom, .48f);
            UIRender.roundRectBorder(x - 2, y - 2, size + 4, size + 4, 4f,
                    mapFrame, mapFrame, border);
            UIRender.rect(x + 2, y + 2, inner, inner, mapSurface);
            float zoom = AppleSettings.dungeonMapZoomFactor();
            for (int index = 0; index <= 4; index++) {
                float source = index * 32.0F;
                float mapped = 64.0F + (source - 64.0F) * zoom;
                if (mapped < 0.0F || mapped > 128.0F) continue;
                float gx = x + 2 + mapped / 128.0F * inner;
                float gy = y + 2 + mapped / 128.0F * inner;
                UIRender.line(gx, y + 2, gx, y + size - 2, 1f, UIRender.withAlpha(Theme.textPrimary, .25f));
                UIRender.line(x + 2, gy, x + size - 2, gy, 1f, UIRender.withAlpha(Theme.textPrimary, .25f));
            }
            UIRender.text(fr(), "DUNGEON MAP", x + 7, y + 7, Theme.textPrimary, 0.9f, true);
            UIRender.text(fr(), "Drag | Zoom " + AppleSettings.effectiveDungeonMapZoom() + "%",
                    x + 7, y + size - 14, Theme.textSecondary, 0.8f, false);
        } finally {
            UIRender.endFrame();
        }
    }

    // ------------------------------------------------------------------ input

    @Override
    public void handleMouseInput() throws IOException {
        int wheel = Mouse.getEventDWheel();
        super.handleMouseInput();
        int mx = Mouse.getEventX() * width / Minecraft.getMinecraft().displayWidth;
        int my = height - Mouse.getEventY() * height / Minecraft.getMinecraft().displayHeight - 1;
        if (wheel != 0 && openDropdown == null && inCardView(mx, my)) {
            boolean fine = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
            scroll.wheel(wheel / 120f, fine, scrollSpeed());
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        if (button != 0) {
            return;
        }
        if (handleOpenDropdownClick(mx, my)) {
            return;
        }

        float cy = barY + BAR_H * 0.5f;
        if (FocusedHeaderCloseButton.contains(mx, my, barX + 14f, cy)) {
            close();
            return;
        }
        Widget barWidget = barWidgetAt(mx, my);
        if (barWidget != null) {
            beginWidgetGesture(barWidget, mx, my);
            return;
        }

        // Canvas targets deliberately win before inspector hit testing: no sidebar/list is needed to reach a
        // disabled placeholder or an element stacked underneath another one.
        int[] mapBounds = new int[3];
        dungeonMapBounds(mapBounds);
        if (mx >= mapBounds[0] && mx <= mapBounds[0] + mapBounds[2]
                && my >= mapBounds[1] && my <= mapBounds[1] + mapBounds[2]) {
            selectDungeonMap();
            draggingDungeonMap = true;
            dungeonMapGrabX = mx - mapBounds[0];
            dungeonMapGrabY = my - mapBounds[1];
            guides = 0;
            return;
        }
        HudElement hit = hitElement(mx, my);
        if (hit != null) {
            select(hit);
            dragging = hit;
            grabX = mx - hit.leftFor(width);
            grabY = my - hit.topFor(height);
            dragTo(mx, my);
            return;
        }

        if (inCardHeader(mx, my)) {
                if (inHideButton(mx, my)) {
                cardHidden = !cardHidden;
                openDropdown = null;
                scroll.reset();
                layout();
            } else {
                draggingCard = true;
                cardGrabX = mx - cardX;
                cardGrabY = my - cardY;
            }
            return;
        }
        if (inScrollbar(mx, my)) {
            draggingScrollbar = true;
            boolean thumb = my >= inspectorLayout.scrollbarY
                    && my < inspectorLayout.scrollbarY + inspectorLayout.scrollbarH;
            scrollbarGrabOffset = thumb ? my - inspectorLayout.scrollbarY : inspectorLayout.scrollbarH * 0.5f;
            setScrollbarFromY(my);
            return;
        }

        Widget w = widgetAt(mx, my);
        if (w != null) {
            beginWidgetGesture(w, mx, my);
            return;
        }
        if (inCardView(mx, my)) {
            active = null;
            gestureActive = true;
            sliderEngaged = false;
            gesture.begin(null, mx, my, scroll.value());
        }
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long timeSinceLastClick) {
        if (draggingScrollbar) {
            setScrollbarFromY(my);
            return;
        }
        if (draggingDungeonMap) {
            int size = AppleSettings.dungeonMapPixelSize(width, height);
            guides = AppleSettings.setDungeonMapPosition(width, height, size,
                    Math.round(mx - dungeonMapGrabX), Math.round(my - dungeonMapGrabY), true, (int) SNAP);
            dirty = true;
            return;
        }
        if (draggingCard) {
            moveCard(mx, my);
            return;
        }
        if (dragging != null) {
            dragTo(mx, my);
            return;
        }
        if (!gestureActive) {
            return;
        }
        int kind = gesture.move(mx, my);
        float speed = scrollSpeed();
        if (kind == Gesture.SLIDER) {
            if (!sliderEngaged) {
                sliderEngaged = true;
                active.onPress((int) gesture.startX, (int) gesture.startY);
            }
            active.onDrag(mx, my);
        } else if (kind == Gesture.SCROLL) {
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
        if (draggingScrollbar) {
            draggingScrollbar = false;
            return;
        }
        if (draggingDungeonMap) {
            draggingDungeonMap = false;
            guides = 0;
            AppleSettings.save();
            dirty = false;
            return;
        }
        if (draggingCard) {
            draggingCard = false;
            return;
        }
        if (dragging != null) {
            dragging = null;
            guides = 0;
            if (dirty) {
                dirty = false;
                HudManager.save();
                AppleSettings.save();
            }
            return;
        }
        if (active != null) {
            if ((active instanceof AppleSlider || active instanceof HorizontalDragWidget)
                    && gesture.kind == Gesture.PENDING) {
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
        if (gestureActive && gesture.kind == Gesture.SCROLL) {
            scroll.fling(gesture.flingVelocity() * scrollSpeed());
        }
        gestureActive = false;
        sliderEngaged = false;
        gesture.target = null;
        gesture.kind = Gesture.PENDING;
        if (dirty) {
            dirty = false;
            HudManager.save();
            AppleSettings.save();
        }
    }

    @Override
    protected void keyTyped(char c, int code) throws IOException {
        if (code == Keyboard.KEY_ESCAPE) {
            if (openDropdown != null) {
                openDropdown = null;
            } else {
                close();
            }
            return;
        }
        // Keyboard arrows scroll the inspector card; HUD targets are selected directly on the canvas.
        if (code == Keyboard.KEY_DOWN) {
            scroll.key(ScrollState.KEY_STEP);
        } else if (code == Keyboard.KEY_UP) {
            scroll.key(-ScrollState.KEY_STEP);
        }
    }
}
