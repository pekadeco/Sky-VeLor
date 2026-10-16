package com.arena.pekaclient.gui;

import com.arena.pekaclient.hud.ClickInOrderHud;
import com.arena.pekaclient.hud.HudElement;
import com.arena.pekaclient.hud.HudManager;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import com.arena.pekaclient.widget.AppleButton;
import com.arena.pekaclient.widget.AppleDropdown;
import com.arena.pekaclient.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The Dungeon HUD screen. It shows only the Click in order terminal panel: drag it to move, use the buttons
 * to switch it on or off and make it bigger or smaller. The settings card at the bottom left has the same
 * options as the HUD editor. Done or Esc returns to the screen it came from.
 */
public final class DungeonHudScreen extends GuiScreen {
    private static final float BTN_W = 84f;
    private static final float BTN_H = 22f;
    private static final float BTN_GAP = 6f;
    private static final float SCALE_STEP = 0.1f;
    private static final int SNAP = 6;
    private static final int COLOR_VEIL = 0x88000000;
    private static final int COLOR_TITLE = 0xFFF2F2F4;
    private static final int COLOR_HINT = 0xFF9A9AA2;

    // Settings card, same proportions as the HUD editor's inspector.
    private static final float CARD_W = 248f;
    private static final float CARD_PAD = 10f;
    private static final float ROW_H = 26f;
    private static final float ROW_GAP = 6f;
    private static final float HEAD_H = 26f;
    private static final float HIDE_W = 44f;
    private static final float HIDE_H = 16f;
    private static final float CARD_MARGIN = 10f;
    private static final float CARD_TOP = 70f;   // below the button row and the hint text

    private final GuiScreen parent;
    private final ClickInOrderHud element = HudManager.CLICK_IN_ORDER;
    private final List<AppleButton> buttons = new ArrayList<AppleButton>();
    private final List<Runnable> actions = new ArrayList<Runnable>();
    private final List<HudSettingsBuilder.Item> items = new ArrayList<HudSettingsBuilder.Item>();

    private boolean dragging;
    private int grabX;
    private int grabY;
    private long lastNs;
    private boolean dirty;

    private Widget active;
    private AppleDropdown openDropdown;
    private boolean cardHidden;
    private float scrollY;
    private float maxScroll;
    private float sf = 2f;
    private int lastMx;
    private int lastMy;

    // Card geometry, set in layoutCard() each frame.
    private float cardX;
    private float cardY;
    private float cardW;
    private float cardH;
    private float visibleTop;
    private float visibleBottom;

    public DungeonHudScreen(GuiScreen parent) {
        this.parent = parent;
    }

    /** Builds the button row. Rebuilt after a click so the On/Off label stays correct. */
    private void buildButtons() {
        buttons.clear();
        actions.clear();
        addButton(element.enabled ? "Turn off" : "Turn on", new Runnable() {
            public void run() {
                element.enabled = !element.enabled;
                dirty = true;
            }
        });
        addButton("Smaller", new Runnable() {
            public void run() {
                element.scale = Math.max(HudElement.MIN_SCALE, element.scale - SCALE_STEP);
                dirty = true;
            }
        });
        addButton("Larger", new Runnable() {
            public void run() {
                element.scale = Math.min(HudElement.MAX_SCALE, element.scale + SCALE_STEP);
                dirty = true;
            }
        });
        addButton("Reset", new Runnable() {
            public void run() {
                element.resetPosition();
                element.scale = 1f;
                dirty = true;
            }
        });
        addButton("Done", new Runnable() {
            public void run() {
                close();
            }
        });
        float total = buttons.size() * BTN_W + (buttons.size() - 1) * BTN_GAP;
        float x = (width - total) / 2f;
        for (AppleButton b : buttons) {
            b.x = x;
            b.y = 16f;
            x += BTN_W + BTN_GAP;
        }
    }

    private void addButton(String label, Runnable action) {
        buttons.add(new AppleButton(label, BTN_W, BTN_H, label.equals("Done"), action));
        actions.add(action);
    }

    /** Rebuilds the settings rows from the same builder the HUD editor uses. */
    private void rebuildItems() {
        items.clear();
        items.addAll(HudSettingsBuilder.build(element, null, new Runnable() {
            public void run() {
                dirty = true;
            }
        }, new Runnable() {
            public void run() {
            }
        }, true));
    }

    private void saveIfDirty() {
        if (dirty) {
            dirty = false;
            HudManager.save();
        }
    }

    private void close() {
        HudManager.save();
        Minecraft.getMinecraft().displayGuiScreen(parent);
    }

    @Override
    public void initGui() {
        buildButtons();
        rebuildItems();
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        saveIfDirty();
    }

    // ------------------------------------------------------------------ card layout

    private float contentH() {
        return items.isEmpty() ? 0f : items.size() * (ROW_H + ROW_GAP) - ROW_GAP;
    }

    private void layoutCard() {
        cardW = Math.min(CARD_W, width - CARD_MARGIN * 2f);
        cardX = CARD_MARGIN;
        float available = height - CARD_MARGIN - CARD_TOP - HEAD_H - CARD_PAD * 2f;
        float bodyH = cardHidden ? 0f : Math.max(40f, Math.min(contentH(), available));
        cardH = HEAD_H + (cardHidden ? 0f : bodyH + CARD_PAD * 2f);
        cardY = height - CARD_MARGIN - cardH;
        if (cardHidden) {
            visibleTop = cardY + HEAD_H;
            visibleBottom = visibleTop;
        } else {
            visibleTop = cardY + HEAD_H + CARD_PAD;
            visibleBottom = cardY + cardH - CARD_PAD;
        }
        maxScroll = Math.max(0f, contentH() - (visibleBottom - visibleTop));
        scrollY = Math.max(0f, Math.min(scrollY, maxScroll));

        float innerW = cardW - CARD_PAD * 2f;
        for (int i = 0; i < items.size(); i++) {
            Widget c = items.get(i).control;
            c.x = cardX + cardW - CARD_PAD - c.w;
            c.y = visibleTop + i * (ROW_H + ROW_GAP) - scrollY + (ROW_H - c.h) * 0.5f;
            if (c.w > innerW) {
                c.x = cardX + CARD_PAD;
            }
        }
    }

    private float hideX() {
        return cardX + cardW - CARD_PAD - HIDE_W;
    }

    private float hideY() {
        return cardY + (HEAD_H - HIDE_H) * 0.5f;
    }

    private static boolean inside(float px, float py, float x, float y, float w, float h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    private boolean inHideButton(float mx, float my) {
        return inside(mx, my, hideX(), hideY(), HIDE_W, HIDE_H);
    }

    private boolean inCard(float mx, float my) {
        return inside(mx, my, cardX, cardY, cardW, cardH);
    }

    private boolean inCardBody(float mx, float my) {
        return !cardHidden && inside(mx, my, cardX, visibleTop, cardW, visibleBottom - visibleTop);
    }

    /** The control under the pointer. Only rows inside the visible body can be hit. */
    private Widget controlAt(int mx, int my) {
        if (!inCardBody(mx, my)) {
            return null;
        }
        for (HudSettingsBuilder.Item it : items) {
            if (it.control.contains(mx, my)) {
                return it.control;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void drawScreen(int mx, int my, float partialTicks) {
        lastMx = mx;
        lastMy = my;
        long now = System.nanoTime();
        float dt = lastNs == 0L ? 1f / 60f : Math.min(0.1f, (now - lastNs) / 1.0e9f);
        lastNs = now;

        sf = new ScaledResolution(Minecraft.getMinecraft()).getScaleFactor();
        layoutCard();

        drawRect(0, 0, width, height, COLOR_VEIL);
        drawCenteredString(fontRendererObj, "Dungeon HUD: Click in order panel", width / 2, 48, COLOR_TITLE);
        drawCenteredString(fontRendererObj, "Drag the panel to move it", width / 2, 60, COLOR_HINT);

        // The panel is drawn alone. Other HUD elements are not drawn on this screen.
        UIRender.beginFrame();
        element.render(width, height, element.enabled ? 1f : 0.35f);
        UIRender.endFrame();

        UIRender.beginFrame();
        for (AppleButton b : buttons) {
            b.update(dt);
            b.draw(mx, my);
        }
        drawCard(mx, my, dt);
        UIRender.endFrame();
    }

    private void drawCard(int mx, int my, float dt) {
        UIRender.roundRect(cardX, cardY, cardW, cardH, Theme.radius, Theme.windowTop, Theme.windowBottom, 1f);

        UIRender.text(fontRendererObj, element.name, cardX + CARD_PAD, cardY + (HEAD_H - 9f) * 0.5f + 1f,
                Theme.textPrimary, 1f, false);
        float hx = hideX();
        float hy = hideY();
        boolean hot = inHideButton(mx, my);
        UIRender.roundRect(hx, hy, HIDE_W, HIDE_H, 8f, Theme.buttonFill, Theme.buttonFill, 1f);
        String label = cardHidden ? "Show" : "Hide";
        UIRender.text(fontRendererObj, label, hx + (HIDE_W - fontRendererObj.getStringWidth(label)) * 0.5f,
                hy + 4f, hot ? Theme.accent : Theme.textSecondary, 1f, false);

        if (cardHidden) {
            return;
        }
        int pmx = inCardBody(mx, my) ? mx : -10000;
        int pmy = inCardBody(mx, my) ? my : -10000;
        UIRender.scissorBegin(cardX, visibleTop, cardW, visibleBottom - visibleTop, sf);
        for (HudSettingsBuilder.Item it : items) {
            Widget c = it.control;
            c.update(dt);
            if (c.y + c.h < visibleTop || c.y > visibleBottom) {
                continue;
            }
            if (it.label != null) {
                UIRender.text(fontRendererObj, it.label, cardX + CARD_PAD, c.y + (c.h - 9f) * 0.5f + 1f,
                        Theme.textSecondary, 1f, false);
            }
            c.draw(pmx, pmy);
        }
        UIRender.scissorEnd();

        if (maxScroll > 0f) {
            float track = visibleBottom - visibleTop;
            float thumbH = Math.max(12f, track * track / (track + maxScroll));
            float thumbY = visibleTop + (scrollY / maxScroll) * (track - thumbH);
            UIRender.roundRect(cardX + cardW - 4f, thumbY, 2f, thumbH, 1f, Theme.textTertiary,
                    Theme.textTertiary, 0.6f);
        }

        // A dropdown popup is drawn last so it sits above the rows below it.
        if (openDropdown != null) {
            openDropdown.drawPopup(mx, my, 0f, height, width, 0f);
        }
    }

    // ------------------------------------------------------------------ input

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && inCardBody(lastMx, lastMy) && maxScroll > 0f) {
            float step = ROW_H + ROW_GAP;
            scrollY = Math.max(0f, Math.min(maxScroll, scrollY - Integer.signum(wheel) * step));
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        if (button != 0) {
            return;
        }
        for (int i = 0; i < buttons.size(); i++) {
            if (buttons.get(i).contains(mx, my)) {
                actions.get(i).run();
                // The On/Off label and the scale change, so rebuild the row.
                buildButtons();
                saveIfDirty();
                return;
            }
        }

        // An open dropdown takes the click first: a choice, or a click outside closes it.
        if (openDropdown != null) {
            int index = openDropdown.optionAt(mx, my);
            if (index >= 0) {
                openDropdown.choose(index);
                openDropdown = null;
                dirty = true;
                saveIfDirty();
                return;
            }
            if (!openDropdown.contains(mx, my)) {
                openDropdown = null;
            } else {
                return;
            }
        }

        if (inHideButton(mx, my)) {
            cardHidden = !cardHidden;
            openDropdown = null;
            scrollY = 0f;
            return;
        }
        if (inCard(mx, my)) {
            Widget w = controlAt(mx, my);
            if (w instanceof AppleDropdown) {
                AppleDropdown d = (AppleDropdown) w;
                if (openDropdown == d) {
                    openDropdown = null;
                } else {
                    openDropdown = d;
                    d.onExpand();
                }
            } else if (w != null) {
                active = w;
                w.onPress(mx, my);
            }
            return;
        }

        int left = element.leftFor(width);
        int top = element.topFor(height);
        if (mx >= left && mx < left + element.pixelWidth() && my >= top && my < top + element.pixelHeight()) {
            dragging = true;
            grabX = mx - left;
            grabY = my - top;
        }
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long timeSinceLastClick) {
        if (active != null) {
            active.onDrag(mx, my);
            dirty = true;
        }
        if (dragging) {
            element.place.setFromAbsolute(width, height, element.pixelWidth(), element.pixelHeight(),
                    mx - grabX, my - grabY, true, SNAP);
        }
    }

    @Override
    protected void mouseReleased(int mx, int my, int button) {
        if (active != null) {
            active.onRelease(mx, my, active.contains(mx, my));
            active = null;
            dirty = true;
        }
        if (dragging) {
            dragging = false;
            dirty = true;
        }
        saveIfDirty();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (openDropdown != null) {
                openDropdown = null;
            } else {
                close();
            }
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }
}
