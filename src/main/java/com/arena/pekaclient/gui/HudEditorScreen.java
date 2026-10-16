package com.arena.pekaclient.gui;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.feature.FirstPersonItem;
import com.arena.pekaclient.hud.BalanceSource;
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
import java.util.List;

/**
 * HUD editor. Drag ordinary HUD elements to move them. The Dungeon Map has its own unclamped placement
 * controls, and First-Person Item is a clearly labelled visual renderer configuration target. Every change
 * is saved when the editor closes.
 *
 * Works with mouse and touch (PojavLauncher): a tap is a click, a drag moves an element or scrolls
 * the card, and a swipe never activates a control.
 */
public final class HudEditorScreen extends AppleScreen {

    private static final float BAR_H = 30f;
    private static final float BAR_TOP = 8f;
    private static final float CARD_W = 248f;
    private static final float CARD_PAD = 10f;
    private static final float ROW_H = 26f;
    private static final float ROW_GAP = 6f;
    private static final float CONTROL_W = HudSettingsBuilder.CONTROL_W;
    private static final float CONTROL_H = HudSettingsBuilder.CONTROL_H;
    private static final float SNAP = 4f;
    private static final float TOUCH_MIN = 28f;

    /** Element names plus dedicated map and renderer configuration targets. */
    private static String[] chooserNames() {
        String[] out = new String[HudManager.ELEMENTS.length + 2];
        for (int i = 0; i < HudManager.ELEMENTS.length; i++) {
            out[i] = HudManager.ELEMENTS[i].name;
        }
        out[HudManager.ELEMENTS.length] = "Dungeon Map";
        out[HudManager.ELEMENTS.length + 1] = "First-Person Item";
        return out;
    }

    /** One inspector row: an optional label and the control on the right. */
    private static final class Entry {
        final String label;
        final Widget control;
        float cy;

        Entry(String label, Widget control) {
            this.label = label;
            this.control = control;
        }
    }

    private final GuiScreen parent;
    private final Anim openAnim = new Anim(0f);
    private final List<Entry> entries = new ArrayList<Entry>();
    private final List<Widget> bar = new ArrayList<Widget>();
    private final SearchField manualField = new SearchField("Enter amount");
    private final AppleDropdown chooser;
    private final AppleButton resetAllBtn;
    private final AppleButton doneBtn;
    private final ScrollState scroll = new ScrollState();
    private final Gesture gesture = new Gesture();

    private enum EditorTarget { HUD_ELEMENT, DUNGEON_MAP, FIRST_PERSON_ITEM }

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
    private boolean draggingDungeonMap;
    private float dungeonMapGrabX;
    private float dungeonMapGrabY;
    private float cardGrabX;
    private float cardGrabY;
    private static final float HEAD_H = 26f;
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
        this.chooser = new AppleDropdown(chooserNames(), null, CONTROL_W, 22f,
                this::chooserIndex, value -> {
                    if (value < HudManager.ELEMENTS.length) {
                        select(HudManager.ELEMENTS[value]);
                    } else if (value == HudManager.ELEMENTS.length) {
                        selectDungeonMap();
                    } else {
                        selectFirstPersonItem();
                    }
                });
        this.resetAllBtn = new AppleButton("Reset all", 74f, 22f, false, this::resetAll);
        this.doneBtn = new AppleButton("Done", 56f, 22f, true, this::close);
        this.manualField.w = CONTROL_W;
        this.manualField.h = 22f;
        bar.add(doneBtn);
        bar.add(resetAllBtn);
        openAnim.to(1f);
    }

    private int chooserIndex() {
        if (target == EditorTarget.DUNGEON_MAP) return HudManager.ELEMENTS.length;
        if (target == EditorTarget.FIRST_PERSON_ITEM) return HudManager.ELEMENTS.length + 1;
        return indexOf(selected);
    }

    private static int indexOf(HudElement e) {
        for (int i = 0; i < HudManager.ELEMENTS.length; i++) {
            if (HudManager.ELEMENTS[i] == e) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        HudManager.ensureLoaded();
        rebuild();
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
        HudManager.save();
        AppleSettings.save();
    }

    // ------------------------------------------------------------------ inspector

    private void select(HudElement element) {
        if (target == EditorTarget.HUD_ELEMENT && element == selected) {
            return;
        }
        selected = element;
        target = EditorTarget.HUD_ELEMENT;
        resetInspector();
    }

    private void selectDungeonMap() {
        if (target == EditorTarget.DUNGEON_MAP) return;
        target = EditorTarget.DUNGEON_MAP;
        resetInspector();
    }

    private void selectFirstPersonItem() {
        if (target == EditorTarget.FIRST_PERSON_ITEM) return;
        target = EditorTarget.FIRST_PERSON_ITEM;
        resetInspector();
    }

    private void resetInspector() {
        openDropdown = null;
        manualField.setFocused(false);
        scroll.reset();
        rebuild();
    }

    private void changed() {
        dirty = true;
    }

    /** Rebuilds the inspector for an ordinary HUD element, the map, or the first-person renderer. */
    private void rebuild() {
        entries.clear();
        entries.add(new Entry(null, chooser));
        if (target == EditorTarget.DUNGEON_MAP) {
            buildDungeonMapEntries();
            return;
        }
        if (target == EditorTarget.FIRST_PERSON_ITEM) {
            buildFirstPersonEntries();
            return;
        }
        List<HudSettingsBuilder.Item> items = HudSettingsBuilder.build(
                selected, manualField, this::changed, this::applyManual, true);
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

    private void buildFirstPersonEntries() {
        entries.add(new Entry("Use size", new AppleToggle(() -> AppleSettings.firstPersonItemScaleOn, value -> {
            AppleSettings.firstPersonItemScaleOn = value;
            changed();
        })));
        entries.add(new Entry("Size", new AppleSlider(FirstPersonItem.MIN_PERCENT, FirstPersonItem.MAX_PERCENT,
                5, "%", CONTROL_W, AppleSettings::effectiveFirstPersonItemScale, value -> {
                    AppleSettings.firstPersonItemScale = FirstPersonItem.clamp(value);
                    changed();
                }).gate(() -> AppleSettings.firstPersonItemScaleOn)));
        entries.add(new Entry("X position", new AppleSlider(FirstPersonItem.MIN_OFFSET, FirstPersonItem.MAX_OFFSET,
                5, "%", CONTROL_W, () -> AppleSettings.firstPersonItemX, value -> {
                    AppleSettings.firstPersonItemX = FirstPersonItem.clampOffset(value);
                    changed();
                })));
        entries.add(new Entry("Y position", new AppleSlider(FirstPersonItem.MIN_OFFSET, FirstPersonItem.MAX_OFFSET,
                5, "%", CONTROL_W, () -> AppleSettings.firstPersonItemY, value -> {
                    AppleSettings.firstPersonItemY = FirstPersonItem.clampOffset(value);
                    changed();
                })));
        entries.add(new Entry("Z position", new AppleSlider(FirstPersonItem.MIN_OFFSET, FirstPersonItem.MAX_OFFSET,
                5, "%", CONTROL_W, () -> AppleSettings.firstPersonItemZ, value -> {
                    AppleSettings.firstPersonItemZ = FirstPersonItem.clampOffset(value);
                    changed();
                })));
        entries.add(new Entry("Use animation", new AppleToggle(() -> AppleSettings.firstPersonAnimSpeedOn, value -> {
            AppleSettings.firstPersonAnimSpeedOn = value;
            changed();
        })));
        entries.add(new Entry("Animation multiplier", new AppleSlider(FirstPersonItem.MIN_PERCENT,
                FirstPersonItem.MAX_PERCENT, 5, "%", CONTROL_W, AppleSettings::effectiveFirstPersonAnimSpeed,
                value -> {
                    AppleSettings.firstPersonAnimSpeed = FirstPersonItem.clamp(value);
                    changed();
                }).gate(() -> AppleSettings.firstPersonAnimSpeedOn)));
        entries.add(new Entry(null, new AppleButton("Reset First-Person Item", CONTROL_W, CONTROL_H, false, () -> {
            FirstPersonItem.resetToDefaults();
            changed();
        })));
    }

    private void applyManual() {
        if (!BalanceSource.applyTyped(manualField.text())) {
            return;
        }
        manualField.clear();
        manualField.setFocused(false);
        changed();
        HudManager.save();
        AppleSettings.save();
    }

    private void resetAll() {
        HudManager.resetAll();
        AppleSettings.resetDungeonMapPosition();
        AppleSettings.dungeonMapScale = 100;
        AppleSettings.dungeonMapScaleOn = true;
        AppleSettings.dungeonMapZoom = 100;
        FirstPersonItem.resetToDefaults();
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
        wx = 6f;
        wy = 6f;
        ww = Math.max(120f, width - 12f);
        wh = Math.max(120f, height - 12f);

        barW = Math.min(ww - 16f, 380f);
        barX = wx + (ww - barW) * 0.5f;
        barY = wy + BAR_TOP;
        doneBtn.x = barX + barW - 8f - doneBtn.w;
        doneBtn.y = barY + (BAR_H - doneBtn.h) * 0.5f;
        resetAllBtn.x = doneBtn.x - 6f - resetAllBtn.w;
        resetAllBtn.y = barY + (BAR_H - resetAllBtn.h) * 0.5f;
        // The inspector card sits bottom-left, so the default HUD corners stay clear of it.
        cardW = Math.min(CARD_W, ww - 20f);
        float contentH = entries.isEmpty() ? 0f : entries.size() * (ROW_H + ROW_GAP) - ROW_GAP;
        float top = barY + BAR_H + 10f;
        float available = wy + wh - 10f - top;
        float bodyH = cardHidden ? 0f : Math.max(60f, Math.min(available - HEAD_H, contentH + CARD_PAD * 2f));
        cardH = HEAD_H + bodyH;
        float baseX = wx + 10f;
        float baseY = wy + wh - 10f - cardH;
        cardX = Math.max(wx, Math.min(wx + ww - cardW, baseX + cardDx));
        cardY = Math.max(wy, Math.min(wy + wh - cardH, baseY + cardDy));
        if (cardHidden) {
            visibleTop = cardY + HEAD_H;
            visibleBottom = visibleTop;
        } else {
            visibleTop = cardY + HEAD_H + CARD_PAD;
            visibleBottom = cardY + cardH - CARD_PAD;
        }

        float viewH = visibleBottom - visibleTop;
        maxScroll = Math.max(0f, contentH - viewH);
        scroll.setMax(maxScroll);

        float innerW = cardW - CARD_PAD * 2f;
        float offset = scroll.value();
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            e.cy = i * (ROW_H + ROW_GAP);
            Widget c = e.control;
            if (c == chooser) {
                c.w = innerW;
            }
            c.x = cardX + cardW - CARD_PAD - c.w;
            c.y = visibleTop + e.cy - offset + (ROW_H - c.h) * 0.5f;
        }
    }

    private float hideX() {
        return cardX + cardW - CARD_PAD - 44f;
    }

    private float hideY() {
        return cardY + (HEAD_H - 16f) * 0.5f;
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

    /** Title on the left, Hide or Show on the right. Drag the header to move the card. */
    private void drawCardHeader(int mx, int my) {
        FontRenderer fr = fr();
        String title = target == EditorTarget.DUNGEON_MAP ? "Dungeon Map"
                : target == EditorTarget.FIRST_PERSON_ITEM ? "First-Person Item — Renderer" : selected.name;
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

    /** The widget under the pointer. Bar buttons first, then visible inspector controls. */
    private Widget widgetAt(int mx, int my) {
        for (Widget b : bar) {
            if (b.contains(mx, my)) {
                return b;
            }
        }
        if (!inCardView(mx, my)) {
            return null;
        }
        for (Entry e : entries) {
            if (e.control.contains(mx, my)) {
                return e.control;
            }
        }
        return null;
    }

    /** The element under the pointer. Each target is at least TOUCH_MIN px so it is easy to hit by touch. */
    private HudElement hitElement(int mx, int my) {
        for (int i = HudManager.ELEMENTS.length - 1; i >= 0; i--) {
            HudElement h = HudManager.ELEMENTS[i];
            float w = h.pixelWidth();
            float hh = h.pixelHeight();
            float padX = Math.max(6f, (TOUCH_MIN - w) * 0.5f);
            float padY = Math.max(6f, (TOUCH_MIN - hh) * 0.5f);
            if (inside(mx, my, h.leftFor(width) - padX, h.topFor(height) - padY, w + padX * 2f, hh + padY * 2f)) {
                return h;
            }
        }
        return null;
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
                Theme.animations, Theme.speed);
    }

    private float scrollSpeed() {
        return AppleSettings.scrollSpeed / 100f;
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
        UIRender.alphaMul = open;
        UIRender.rect(0f, 0f, width, height, Theme.veil);
        UIRender.endFrame();

        ScaledResolution sr = new ScaledResolution(mc);
        HudManager.renderEditor(sr, mx, my);
        drawDungeonMapEditorPreview(mx, my);

        UIRender.beginFrame();
        UIRender.alphaMul = open;
        drawSelection();
        if ((dragging != null || draggingDungeonMap) && guides != 0) {
            drawGuides();
        }
        drawBar(mx, my);
        drawCard(mx, my);
        UIRender.alphaMul = open;
        drawOpenDropdown(mx, my, 0f, height, width, 0f);
        UIRender.alphaMul = 1f;
        UIRender.endFrame();
    }

    /** Thin rounded ring around the selected HUD target or the dedicated map preview. */
    private void drawSelection() {
        if (target == EditorTarget.FIRST_PERSON_ITEM) {
            return;
        }
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
        // Only close is live here. Collapse and zoom have no meaning for the editor, so they are drawn dimmed and
        // hovering them says why.
        WindowDots.draw(barX + 14f, cy, mx, my, 0b001, false);

        FontRenderer fr = fr();
        String title = "HUD Editor";
        if (barW - 180f > fr.getStringWidth(title) + 58f) {
            UIRender.text(fr, title, barX + 58f, cy - 4.5f, Theme.textSecondary, 1f, false);
        }
        doneBtn.draw(mx, my);
        WindowDots.drawHint(fr, mx, my, barX + 14f, cy, 0b001,
                "Collapse is not used in the HUD editor", "Zoom is not used in the HUD editor", width);
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
        for (Entry e : entries) {
            Widget c = e.control;
            if (c.y + c.h < visibleTop || c.y > visibleBottom) {
                continue;
            }
            if (e.label != null) {
                UIRender.text(fr, e.label, cardX + CARD_PAD, c.y + (c.h - 9f) * 0.5f + 1f,
                        Theme.textSecondary, 1f, false);
            }
            c.draw(pmx, pmy);
        }
        UIRender.scissorEnd();

        if (maxScroll > 0f) {
            float contentH = maxScroll + viewH;
            float th = Math.max(16f, viewH * viewH / contentH);
            float ty = visibleTop + (scroll.value() / maxScroll) * (viewH - th);
            UIRender.roundRect(cardX + cardW - 4f, ty, 2.5f, th, 1.25f,
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

    /** A controllable preview is available even outside a dungeon, where the live map has no server item to draw. */
    private void drawDungeonMapEditorPreview(int mx, int my) {
        int[] bounds = new int[3];
        dungeonMapBounds(bounds);
        int x = bounds[0], y = bounds[1], size = bounds[2];
        int inner = Math.max(0, size - 4);
        UIRender.beginFrame();
        try {
            boolean hovering = mx >= x && mx <= x + size && my >= y && my <= y + size;
            int border = target == EditorTarget.DUNGEON_MAP || hovering ? Theme.accent : Theme.border;
            UIRender.roundRectBorder(x - 2, y - 2, size + 4, size + 4, 4f,
                    0xB8181C24, 0xB8181C24, border);
            UIRender.rect(x + 2, y + 2, inner, inner, 0xAA292D34);
            float zoom = AppleSettings.dungeonMapZoomFactor();
            for (int index = 0; index <= 4; index++) {
                float source = index * 32.0F;
                float mapped = 64.0F + (source - 64.0F) * zoom;
                if (mapped < 0.0F || mapped > 128.0F) continue;
                float gx = x + 2 + mapped / 128.0F * inner;
                float gy = y + 2 + mapped / 128.0F * inner;
                UIRender.line(gx, y + 2, gx, y + size - 2, 1f, 0x553FFFFF);
                UIRender.line(x + 2, gy, x + size - 2, gy, 1f, 0x553FFFFF);
            }
            UIRender.text(fr(), "DUNGEON MAP", x + 7, y + 7, 0xFFFFFFFF, 0.9f, true);
            UIRender.text(fr(), "Drag | Zoom " + AppleSettings.effectiveDungeonMapZoom() + "%",
                    x + 7, y + size - 14, 0xFFD0D4DC, 0.8f, false);
        } finally {
            UIRender.endFrame();
        }
    }

    // ------------------------------------------------------------------ input

    @Override
    public void handleMouseInput() throws IOException {
        int wheel = Mouse.getEventDWheel();
        super.handleMouseInput();
        if (wheel != 0) {
            scroll.wheel(wheel / 120f, scrollSpeed());
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

        if (manualField.contains(mx, my) && inCardView(mx, my)) {
            manualField.setFocused(true);
            return;
        }
        manualField.setFocused(false);

        float cy = barY + BAR_H * 0.5f;
        if (WindowDots.hit(mx, my, barX + 14f, cy, 0b001) == WindowDots.CLOSE) {
            close();
            return;
        }

        Widget w = widgetAt(mx, my);
        if (w != null) {
            active = w;
            sliderEngaged = false;
            gestureActive = true;
            gesture.begin(w, mx, my, scroll.value());
            if (!(w instanceof AppleSlider)) {
                w.onPress(mx, my);
            }
            return;
        }
        if (inCardView(mx, my)) {
            active = null;
            gestureActive = true;
            sliderEngaged = false;
            gesture.begin(null, mx, my, scroll.value());
            return;
        }

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

        HudElement h = hitElement(mx, my);
        if (h != null) {
            select(h);
            dragging = h;
            grabX = mx - h.leftFor(width);
            grabY = my - h.topFor(height);
            dragTo(mx, my);
        }
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long timeSinceLastClick) {
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
            } else if (manualField.isFocused()) {
                manualField.setFocused(false);
            } else {
                close();
            }
            return;
        }
        if (manualField.isFocused()) {
            if (code == Keyboard.KEY_RETURN || code == Keyboard.KEY_NUMPADENTER) {
                applyManual();
                return;
            }
            // Digits only.
            if (BalanceSource.acceptsKey(c, code)) {
                manualField.keyTyped(c, code);
            }
            return;
        }
        // Keyboard: arrows scroll the inspector card, and Tab cycles the element.
        if (code == Keyboard.KEY_DOWN) {
            scroll.key(ScrollState.KEY_STEP);
        } else if (code == Keyboard.KEY_UP) {
            scroll.key(-ScrollState.KEY_STEP);
        } else if (code == Keyboard.KEY_TAB) {
            int next = (chooserIndex() + 1) % (HudManager.ELEMENTS.length + 2);
            if (next < HudManager.ELEMENTS.length) select(HudManager.ELEMENTS[next]);
            else if (next == HudManager.ELEMENTS.length) selectDungeonMap();
            else selectFirstPersonItem();
        }
    }
}
