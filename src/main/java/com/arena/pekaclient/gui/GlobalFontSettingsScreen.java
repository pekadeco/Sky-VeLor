package com.arena.pekaclient.gui;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.config.ChatQolSettings;
import com.arena.pekaclient.render.GlobalFontManager;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import com.arena.pekaclient.widget.AppleButton;
import com.arena.pekaclient.widget.AppleDropdown;
import com.arena.pekaclient.widget.AppleSlider;
import com.arena.pekaclient.widget.AppleToggle;
import com.arena.pekaclient.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/** Small global font editor reached from Chat QoL; all controls modify Sky VeLor typography, never vanilla by default. */
public final class GlobalFontSettingsScreen extends AppleScreen {

    private final GuiScreen parent;
    private final String[] fontOptions;
    private final AppleDropdown font;
    private final AppleToggle enabled;
    private final AppleToggle allMinecraft;
    private final AppleSlider uiScale;
    private final AppleSlider hudScale;
    private final AppleSlider editorScale;
    private final AppleSlider tabScale;
    private final AppleSlider chatScale;
    private final AppleButton reload;
    private final AppleButton done;
    private Widget active;
    private float x;
    private float y;
    private float w;
    private float h;

    public GlobalFontSettingsScreen(GuiScreen parent) {
        this.parent = parent;
        fontOptions = GlobalFontManager.availableFonts();
        font = new AppleDropdown(fontOptions, null, 176f, 22f, this::fontIndex,
                value -> {
                    if (value >= 0 && value < fontOptions.length) {
                        ChatQolSettings.globalFontName = fontOptions[value];
                        GlobalFontManager.reload();
                    }
                });
        enabled = new AppleToggle(() -> ChatQolSettings.globalFontEnabled, value -> {
            ChatQolSettings.globalFontEnabled = value;
            ChatQolSettings.customFont = value;
            GlobalFontManager.reload();
        });
        allMinecraft = new AppleToggle(() -> ChatQolSettings.applyAllMinecraftText,
                value -> ChatQolSettings.applyAllMinecraftText = value);
        uiScale = slider(() -> ChatQolSettings.fontUiScale, value -> ChatQolSettings.fontUiScale = value);
        hudScale = slider(() -> ChatQolSettings.fontHudScale, value -> ChatQolSettings.fontHudScale = value);
        editorScale = slider(() -> ChatQolSettings.fontEditorScale, value -> ChatQolSettings.fontEditorScale = value);
        tabScale = slider(() -> ChatQolSettings.fontTabScale, value -> ChatQolSettings.fontTabScale = value);
        chatScale = slider(() -> ChatQolSettings.fontChatScale, value -> ChatQolSettings.fontChatScale = value);
        reload = new AppleButton("Reload", 74f, 22f, false, GlobalFontManager::reload);
        done = new AppleButton("Done", 64f, 22f, true, this::close);
    }

    private static AppleSlider slider(java.util.function.IntSupplier getter, java.util.function.IntConsumer setter) {
        return new AppleSlider(50, 200, 5, "%", 145f, getter, setter);
    }

    private int fontIndex() {
        String name = ChatQolSettings.globalFontName;
        for (int i = 0; i < fontOptions.length; i++) if (fontOptions[i].equals(name)) return i;
        return 0;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        AppleSettings.save();
        super.onGuiClosed();
    }

    private void layout() {
        w = Math.max(250f, Math.min(width - 16f, 382f));
        h = Math.max(226f, Math.min(height - 16f, 318f));
        x = (width - w) * 0.5f;
        y = (height - h) * 0.5f;
        float right = x + w - 14f;
        font.x = right - font.w;
        font.y = y + 48f;
        enabled.x = right - enabled.w;
        enabled.y = y + 79f;
        allMinecraft.x = right - allMinecraft.w;
        allMinecraft.y = y + 108f;
        AppleSlider[] scales = {uiScale, hudScale, editorScale, tabScale, chatScale};
        for (int i = 0; i < scales.length; i++) {
            scales[i].x = right - scales[i].w;
            scales[i].y = y + 140f + i * 27f;
        }
        reload.x = x + 14f;
        reload.y = y + h - 31f;
        done.x = x + w - done.w - 14f;
        done.y = y + h - 31f;
    }

    @Override
    public void drawScreen(int mx, int my, float partialTicks) {
        layout();
        Widget[] widgets = widgets();
        for (Widget widget : widgets) widget.update(1f / 60f);
        drawDefaultBackground();
        UIRender.beginFrame();
        UIRender.rect(0f, 0f, width, height, Theme.veil);
        UIRender.shadow(x, y, w, h, Theme.radius);
        UIRender.roundRect(x, y, w, h, Theme.radius, Theme.windowTop, Theme.windowBottom, 0.6f);
        UIRender.roundRectRing(x, y, w, h, Theme.radius, 1f, Theme.border);
        UIRender.text(fr(), "Global Font Settings", x + 14f, y + 13f, Theme.textPrimary, 1f, false,
                GlobalFontManager.Area.EDITOR);
        UIRender.text(fr(), "Applies to Sky VeLor UI, HUD, editors, tabs and custom chat.", x + 14f, y + 27f,
                Theme.textSecondary, 0.7f, false, GlobalFontManager.Area.EDITOR);
        label("Font", font.y + 7f); font.draw(mx, my);
        label("Enable global font", enabled.y + 6f); enabled.draw(mx, my);
        label("Apply to all Minecraft text", allMinecraft.y + 6f); allMinecraft.draw(mx, my);
        String[] names = {"UI scale", "HUD scale", "Editor scale", "Tab scale", "Chat scale"};
        AppleSlider[] scales = {uiScale, hudScale, editorScale, tabScale, chatScale};
        for (int i = 0; i < scales.length; i++) {
            label(names[i], scales[i].y + 7f);
            scales[i].draw(mx, my);
        }
        UIRender.text(fr(), "All-Minecraft is experimental and fails closed if no safe bridge exists.", x + 14f, y + h - 50f,
                Theme.textTertiary, 0.63f, false, GlobalFontManager.Area.EDITOR);
        reload.draw(mx, my);
        done.draw(mx, my);
        if (openDropdown != null) openDropdown.drawPopup(mx, my, y, y + h, x + w, x);
        UIRender.endFrame();
        // Surface the fail-closed notice only after a user explicitly enabled the optional bridge.
        GlobalFontManager.appliesToAllMinecraftText();
    }

    private void label(String value, float yy) {
        UIRender.text(fr(), value, x + 14f, yy, Theme.textSecondary, 0.82f, false, GlobalFontManager.Area.EDITOR);
    }

    private Widget[] widgets() {
        return new Widget[]{font, enabled, allMinecraft, uiScale, hudScale, editorScale, tabScale, chatScale, reload, done};
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        if (button != 0) return;
        if (handleOpenDropdownClick(mx, my)) return;
        for (Widget widget : widgets()) {
            if (!widget.contains(mx, my)) continue;
            active = widget;
            active.onPress(mx, my);
            return;
        }
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long timeSinceLastClick) {
        if (active != null) active.onDrag(mx, my);
    }

    @Override
    protected void mouseReleased(int mx, int my, int button) {
        if (active == null) return;
        Widget released = active;
        active = null;
        boolean activate = released.contains(mx, my);
        released.onRelease(mx, my, activate);
        if (activate && released instanceof AppleDropdown) {
            AppleDropdown dropdown = (AppleDropdown) released;
            openDropdown = openDropdown == dropdown ? null : dropdown;
            if (openDropdown != null) openDropdown.onExpand();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (openDropdown != null) openDropdown = null;
            else close();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private void close() {
        Minecraft.getMinecraft().displayGuiScreen(parent);
    }
}
