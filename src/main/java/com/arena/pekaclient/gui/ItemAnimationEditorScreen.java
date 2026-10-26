package com.arena.pekaclient.gui;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import com.arena.pekaclient.feature.ItemAnimation;
import com.arena.pekaclient.render.itemanimation.ItemAnimationHookStatus;
import com.arena.pekaclient.widget.AppleButton;
import com.arena.pekaclient.widget.AppleDropdown;
import com.arena.pekaclient.widget.AppleSlider;
import com.arena.pekaclient.widget.AppleToggle;
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
 * A deliberately small standalone Item Animation panel.  Unlike the HUD editor it never paints a placement canvas,
 * HUD elements, toolbar, world overlay, or full-screen dim layer behind the panel.
 */
public final class ItemAnimationEditorScreen extends AppleScreen {
    static final float PANEL_WIDTH_FRACTION = .28f;
    static final float PANEL_MIN_WIDTH = 208f;
    static final float PANEL_MAX_WIDTH = 360f;
    private static final float PANEL_MARGIN = 8f;
    private static final float HEADER_H = 29f;
    private static final float FOOTER_GAP = 8f;
    private static final float ROW_H = 28f;
    private static final float CAPTION_H = 19f;

    private final GuiScreen parent;
    private final List<Entry> entries = new ArrayList<Entry>();
    private final ScrollState scroll = new ScrollState();
    private Widget activeWidget;
    private float panelX;
    private float panelY;
    private float panelW;
    private float panelH;
    private float contentTop;
    private float contentBottom;
    private float sf = 1f;
    private long lastFrameNanos;

    public ItemAnimationEditorScreen(GuiScreen parent) {
        this.parent = parent;
        buildEntries();
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(false);
        lastFrameNanos = System.nanoTime();
    }

    private void buildEntries() {
        entries.clear();
        entries.add(Entry.status("Hook", new HookStatusWidget()));
        entries.add(Entry.control("Enable", new AppleToggle(new java.util.function.Supplier<Boolean>() {
            public Boolean get() { return AppleSettings.itemAnimationEnabled; }
        }, new java.util.function.Consumer<Boolean>() {
            public void accept(Boolean value) { AppleSettings.itemAnimationEnabled = value; }
        })));
        entries.add(Entry.control("Scale", slider(ItemAnimation.MIN_SIZE_PERCENT, ItemAnimation.MAX_SIZE_PERCENT, 1, new IntValue() {
            public int get() { return AppleSettings.effectiveItemAnimationSize(); }
            public void set(int value) {
                AppleSettings.itemAnimationSizeOn = true;
                AppleSettings.itemAnimationSize = ItemAnimation.clampSize(value);
            }
        }, "%")));
        entries.add(Entry.caption("Position"));
        entries.add(Entry.control("X", slider(ItemAnimation.MIN_OFFSET, ItemAnimation.MAX_OFFSET, 1, new IntValue() {
            public int get() { return AppleSettings.itemAnimationX; }
            public void set(int value) { AppleSettings.itemAnimationX = ItemAnimation.clampOffset(value); }
        }, "")));
        entries.add(Entry.control("Y", slider(ItemAnimation.MIN_OFFSET, ItemAnimation.MAX_OFFSET, 1, new IntValue() {
            public int get() { return AppleSettings.itemAnimationY; }
            public void set(int value) { AppleSettings.itemAnimationY = ItemAnimation.clampOffset(value); }
        }, "")));
        entries.add(Entry.control("Z", slider(ItemAnimation.MIN_OFFSET, ItemAnimation.MAX_OFFSET, 1, new IntValue() {
            public int get() { return AppleSettings.itemAnimationZ; }
            public void set(int value) { AppleSettings.itemAnimationZ = ItemAnimation.clampOffset(value); }
        }, "")));
        entries.add(Entry.caption("Animation"));
        entries.add(Entry.control("Custom Hit/Slash Style", new AppleDropdown(AppleSettings.ITEM_ANIMATION_HIT_SLASH_STYLES,
                null, 1f, 22f, new java.util.function.IntSupplier() {
                    public int getAsInt() { return AppleSettings.itemAnimationHitSlashStyle; }
                }, new java.util.function.IntConsumer() {
                    public void accept(int value) { AppleSettings.itemAnimationHitSlashStyle = ItemAnimation.clampHitSlashStyle(value); }
                })));
        entries.add(Entry.control("Custom Hit/Slash Speed", slider(ItemAnimation.MIN_SPEED_PERCENT, ItemAnimation.MAX_SPEED_PERCENT,
                1, new IntValue() {
                    public int get() { return AppleSettings.effectiveItemAnimationSpeed(); }
                    public void set(int value) {
                        AppleSettings.itemAnimationSpeedOn = true;
                        AppleSettings.itemAnimationSpeed = ItemAnimation.clampSpeed(value);
                    }
                }, "%")));
        entries.add(Entry.control("Smooth Swing", new AppleToggle(new java.util.function.Supplier<Boolean>() {
            public Boolean get() { return AppleSettings.itemAnimationSmoothSwing; }
        }, new java.util.function.Consumer<Boolean>() {
            public void accept(Boolean value) { AppleSettings.itemAnimationSmoothSwing = value; }
        })));
        entries.add(Entry.control("Smoothness", slider(ItemAnimation.MIN_SMOOTHNESS_PERCENT,
                ItemAnimation.MAX_SMOOTHNESS_PERCENT, 1, new IntValue() {
                    public int get() { return AppleSettings.itemAnimationSmoothness; }
                    public void set(int value) { AppleSettings.itemAnimationSmoothness = ItemAnimation.clampSmoothness(value); }
                }, "%")));
        entries.add(Entry.caption("Options"));
        entries.add(Entry.control("Remove Haste Effect", new AppleToggle(new java.util.function.Supplier<Boolean>() {
            public Boolean get() { return AppleSettings.itemAnimationRemoveHasteEffect; }
        }, new java.util.function.Consumer<Boolean>() {
            public void accept(Boolean value) { AppleSettings.itemAnimationRemoveHasteEffect = value; }
        })));
        entries.add(Entry.control("Loop Preview Swing", new AppleToggle(new java.util.function.Supplier<Boolean>() {
            public Boolean get() { return ItemAnimation.isPreviewSwingLoop(); }
        }, new java.util.function.Consumer<Boolean>() {
            public void accept(Boolean value) { ItemAnimation.setPreviewSwingLoop(value); }
        })));
        entries.add(Entry.control("Preview Swing", new AppleButton("Preview", 72f, 22f, true, new Runnable() {
            public void run() { ItemAnimation.previewSwing(); }
        })));
        entries.add(Entry.control("Reset to Defaults", new AppleButton("Reset to Defaults", 98f, 22f, false, new Runnable() {
            public void run() {
                ItemAnimation.resetToDefaults();
                AppleSettings.save();
            }
        })));
    }

    private AppleSlider slider(int min, int max, int step, final IntValue value, String suffix) {
        return new AppleSlider(min, max, step, suffix, 1f, new java.util.function.IntSupplier() {
            public int getAsInt() { return value.get(); }
        }, new java.util.function.IntConsumer() {
            public void accept(int next) { value.set(next); }
        }).fineStep(step);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        Theme.applyReferenceSettingsPalette();
        long now = System.nanoTime();
        float dt = lastFrameNanos == 0L ? 1f / 60f : Math.min(.1f, (now - lastFrameNanos) / 1000000000f);
        lastFrameNanos = now;
        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
        sf = Math.max(1f, resolution.getScaleFactor());
        layoutPanel();
        scroll.update(dt, activeWidget instanceof AppleSlider, AppleSettings.smoothScrolling, Theme.speed);
        for (Entry entry : entries) if (entry.widget != null) entry.widget.update(dt);

        UIRender.beginFrame();
        try {
            drawPanel(mouseX, mouseY);
            drawOpenDropdown(mouseX, mouseY, contentTop, contentBottom, panelX + panelW - 6f, panelX + 6f);
        } finally {
            UIRender.endFrame();
        }
    }

    private void layoutPanel() {
        panelX = PANEL_MARGIN;
        panelY = PANEL_MARGIN;
        float usableWidth = Math.max(1f, width - PANEL_MARGIN * 2f);
        panelW = Math.min(usableWidth,
                Math.max(PANEL_MIN_WIDTH, Math.min(PANEL_MAX_WIDTH, width * PANEL_WIDTH_FRACTION)));
        panelH = Math.max(1f, height - PANEL_MARGIN * 2f);
        contentTop = panelY + HEADER_H + 4f;
        contentBottom = panelY + panelH - FOOTER_GAP;
        float total = 0f;
        for (Entry entry : entries) total += entry.height();
        scroll.setMax(Math.max(0f, total - Math.max(1f, contentBottom - contentTop)));
    }

    private void drawPanel(int mouseX, int mouseY) {
        UIRender.roundRectBorder(panelX, panelY, panelW, panelH, 7f,
                Theme.card, Theme.card, Theme.border);
        UIRender.text(fr(), "Item Animation", panelX + 10f, panelY + 10f, Theme.textPrimary, .88f, false);
        float closeX = panelX + panelW - 20f;
        boolean closeHover = inside(mouseX, mouseY, closeX, panelY + 5f, 15f, 16f);
        int closeFill = closeHover ? Theme.buttonHover : Theme.buttonFill;
        UIRender.roundRect(closeX, panelY + 5f, 15f, 16f, 4f, closeFill, closeFill, 1f);
        UIRender.text(fr(), "×", closeX + 4.5f, panelY + 8f, closeHover ? Theme.textPrimary : Theme.textSecondary, .86f, false);
        UIRender.line(panelX + 7f, panelY + HEADER_H, panelX + panelW - 7f, panelY + HEADER_H, 1f, Theme.divider);

        UIRender.scissorBegin(panelX + 1f, contentTop, panelW - 2f, contentBottom - contentTop, sf);
        try {
            float y = contentTop - scroll.value();
            for (Entry entry : entries) {
                drawEntry(entry, y, mouseX, mouseY);
                y += entry.height();
            }
        } finally {
            UIRender.scissorEnd();
        }
        drawScrollBar();
    }

    private void drawEntry(Entry entry, float y, int mouseX, int mouseY) {
        if (entry.caption) {
            UIRender.text(fr(), entry.label, panelX + 10f, y + 7f, Theme.textTertiary, .62f, false);
            return;
        }
        float rowY = y + 2f;
        float labelX = panelX + 10f;
        float controlX = panelX + Math.min(100f, panelW * .42f);
        float controlW = panelX + panelW - 10f - controlX;
        if (entry.status) {
            entry.widget.x = labelX;
            entry.widget.y = rowY;
            entry.widget.w = panelW - 20f;
            entry.widget.h = ROW_H - 4f;
            entry.widget.draw(mouseX, mouseY);
            return;
        }
        UIRender.text(fr(), fit(fr(), entry.label, Math.max(30f, controlX - labelX - 6f)), labelX,
                rowY + 8f, Theme.textSecondary, .66f, false);
        entry.widget.x = controlX;
        entry.widget.y = rowY + 2f;
        entry.widget.w = controlW;
        entry.widget.h = ROW_H - 8f;
        entry.widget.draw(mouseX, mouseY);
    }

    private void drawScrollBar() {
        if (scroll.max() <= .5f) return;
        float view = contentBottom - contentTop;
        float total = view + scroll.max();
        float thumbH = Math.max(18f, view * view / total);
        float thumbY = contentTop + (view - thumbH) * (scroll.value() / Math.max(1f, scroll.max()));
        UIRender.roundRect(panelX + panelW - 4f, thumbY, 2f, thumbH, 1f,
                Theme.textTertiary, Theme.textTertiary, 1f);
    }

    @Override
    public void handleMouseInput() throws IOException {
        int wheel = Mouse.getEventDWheel();
        super.handleMouseInput();
        int mouseX = Mouse.getEventX() * width / Math.max(1, Minecraft.getMinecraft().displayWidth);
        int mouseY = height - Mouse.getEventY() * height / Math.max(1, Minecraft.getMinecraft().displayHeight) - 1;
        if (wheel != 0 && openDropdown == null && inside(mouseX, mouseY, panelX, contentTop, panelW, contentBottom - contentTop)) {
            boolean fine = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
            scroll.wheel(wheel / 120f, fine, 1f);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        if (button != 0) return;
        if (handleOpenDropdownClick(mouseX, mouseY)) return;
        if (inside(mouseX, mouseY, panelX + panelW - 20f, panelY + 5f, 15f, 16f)) {
            close();
            return;
        }
        Widget widget = widgetAt(mouseX, mouseY);
        if (widget != null) {
            activeWidget = widget;
            activeWidget.onPress(mouseX, mouseY);
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long elapsed) {
        if (button == 0 && activeWidget != null) activeWidget.onDrag(mouseX, mouseY);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (state != 0 || activeWidget == null) return;
        Widget released = activeWidget;
        activeWidget = null;
        released.onRelease(mouseX, mouseY, released.contains(mouseX, mouseY));
        if (released instanceof AppleDropdown && released.contains(mouseX, mouseY)) {
            AppleDropdown dropdown = (AppleDropdown) released;
            if (openDropdown == dropdown) {
                openDropdown = null;
            } else {
                openDropdown = dropdown;
                dropdown.onExpand();
            }
        }
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

    private Widget widgetAt(int mouseX, int mouseY) {
        if (!inside(mouseX, mouseY, panelX, contentTop, panelW, contentBottom - contentTop)) return null;
        for (Entry entry : entries) {
            if (entry.widget != null && !entry.caption && entry.widget.contains(mouseX, mouseY)) return entry.widget;
        }
        return null;
    }

    private void close() {
        ItemAnimation.stopPreviewSwingLoop();
        AppleSettings.save();
        Minecraft.getMinecraft().displayGuiScreen(parent);
    }

    @Override
    public void onGuiClosed() {
        ItemAnimation.stopPreviewSwingLoop();
        AppleSettings.save();
        UIRender.restoreVanillaGuiState();
        super.onGuiClosed();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private interface IntValue {
        int get();
        void set(int value);
    }

    private static final class Entry {
        final String label;
        final Widget widget;
        final boolean caption;
        final boolean status;

        private Entry(String label, Widget widget, boolean caption, boolean status) {
            this.label = label;
            this.widget = widget;
            this.caption = caption;
            this.status = status;
        }

        static Entry caption(String label) { return new Entry(label, null, true, false); }
        static Entry control(String label, Widget widget) { return new Entry(label, widget, false, false); }
        static Entry status(String label, Widget widget) { return new Entry(label, widget, false, true); }
        float height() { return caption ? CAPTION_H : ROW_H; }
    }

    private static final class HookStatusWidget extends Widget {
        HookStatusWidget() { super(1f, ROW_H - 4f); }

        @Override
        public void draw(int mouseX, int mouseY) {
            boolean active = ItemAnimationHookStatus.isActive();
            int state = active ? Theme.STATUS_SUCCESS
                    : (ItemAnimationHookStatus.isFailed() ? Theme.STATUS_CRITICAL : Theme.STATUS_WARNING);
            UIRender.roundRectBorder(x, y, w, h, 4f, Theme.buttonFill, Theme.buttonFill, Theme.border);
            UIRender.circle(x + 8f, y + h * .5f, 3f, state);
            int text = active ? Theme.STATUS_SUCCESS
                    : (ItemAnimationHookStatus.isFailed() ? Theme.STATUS_CRITICAL : Theme.textSecondary);
            UIRender.text(font(), fitText(ItemAnimationHookStatus.getDisplay(), Math.max(20f, w - 22f)),
                    x + 16f, y + Math.max(4f, (h - font().FONT_HEIGHT * .66f) * .5f), text, .66f, false);
        }
    }
}
