package com.arena.pekaclient.gui;

import com.arena.pekaclient.config.AppleSettings;
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
 * HUD editor. Drag an element to move it. It snaps to the screen centre and edges, and it can never be
 * dragged off screen. Pick an element with a tap or the chooser, then adjust it in the inspector card.
 * Every change is saved when the editor closes.
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
    private static final float SNAP = 4f;
    private static final float TOUCH_MIN = 28f;

    /** Element names for the chooser, taken from the element list so a new element shows up automatically. */
    private static String[] chooserNames() {
        String[] out = new String[HudManager.ELEMENTS.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = HudManager.ELEMENTS[i].name;
        }
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
    private final AppleButton dungeonBtn;
    private final ScrollState scroll = new ScrollState();
    private final Gesture gesture = new Gesture();

    private HudElement selected = HudManager.MINING;
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

    /** Opens the Dungeon HUD screen, which shows only the Click in order panel. Done returns here. */
    private void openDungeonHud() {
        Minecraft.getMinecraft().displayGuiScreen(new DungeonHudScreen(this));
    }

    public HudEditorScreen(GuiScreen parent) {
        this.parent = parent;
        this.chooser = new AppleDropdown(chooserNames(), null, CONTROL_W, 22f,
                () -> indexOf(selected), v -> select(HudManager.ELEMENTS[v]));
        this.resetAllBtn = new AppleButton("Reset all", 74f, 22f, false, this::resetAll);
        this.doneBtn = new AppleButton("Done", 56f, 22f, true, this::close);
        this.dungeonBtn = new AppleButton("Dungeon HUD", 92f, 22f, false, this::openDungeonHud);
        this.manualField.w = CONTROL_W;
        this.manualField.h = 22f;
        bar.add(doneBtn);
        bar.add(resetAllBtn);
        bar.add(dungeonBtn);
        openAnim.to(1f);
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
    }

    // ------------------------------------------------------------------ inspector

    private void select(HudElement e) {
        if (e == selected) {
            return;
        }
        selected = e;
        openDropdown = null;
        manualField.setFocused(false);
        scroll.reset();
        rebuild();
    }

    private void changed() {
        dirty = true;
    }

    /** Rebuilds the inspector rows for the selected element, from the same builder the hub uses. */
    private void rebuild() {
        entries.clear();
        entries.add(new Entry(null, chooser));
        List<HudSettingsBuilder.Item> items = HudSettingsBuilder.build(
                selected, manualField, this::changed, this::applyManual, true);
        for (HudSettingsBuilder.Item it : items) {
            entries.add(new Entry(it.label, it.control));
        }
    }

    private void applyManual() {
        if (!BalanceSource.applyTyped(manualField.text())) {
            return;
        }
        manualField.clear();
        manualField.setFocused(false);
        changed();
        HudManager.save();
    }

    private void resetAll() {
        HudManager.resetAll();
        openDropdown = null;
        rebuild();
        HudManager.save();
    }

    private void close() {
        HudManager.save();
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
        dungeonBtn.x = resetAllBtn.x - 6f - dungeonBtn.w;
        dungeonBtn.y = barY + (BAR_H - dungeonBtn.h) * 0.5f;

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
        String title = selected == null ? "Settings" : selected.name;
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

        UIRender.beginFrame();
        UIRender.alphaMul = open;
        drawSelection();
        if (dragging != null && guides != 0) {
            drawGuides();
        }
        drawBar(mx, my);
        drawCard(mx, my);
        UIRender.alphaMul = open;
        drawOpenDropdown(mx, my, 0f, height, width, 0f);
        UIRender.alphaMul = 1f;
        UIRender.endFrame();
    }

    /** Thin rounded ring around the selected element, with corner handles. */
    private void drawSelection() {
        HudElement e = selected;
        float x = e.leftFor(width) - 3f;
        float y = e.topFor(height) - 3f;
        float w = e.pixelWidth() + 6f;
        float h = e.pixelHeight() + 6f;
        UIRender.roundRectRing(x, y, w, h, e.radius * e.scale + 3f, 1f, Theme.accent);
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
        dungeonBtn.draw(mx, my);
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
            int next = (indexOf(selected) + 1) % HudManager.ELEMENTS.length;
            select(HudManager.ELEMENTS[next]);
        }
    }
}
