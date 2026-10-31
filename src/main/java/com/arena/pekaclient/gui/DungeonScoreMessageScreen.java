package com.arena.pekaclient.gui;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.render.GlobalFontManager;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import com.arena.pekaclient.widget.AppleButton;
import com.arena.pekaclient.widget.SearchField;
import com.arena.pekaclient.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/** Small Lunaria-themed editor for the optional S+ party message. */
public final class DungeonScoreMessageScreen extends AppleScreen {
    private final GuiScreen parent;
    private final SearchField message = new SearchField("300 Score! (default)");
    private final AppleButton saveButton = new AppleButton("Save", 82f, 24f, true, this::saveAndClose);
    private final AppleButton clearButton = new AppleButton("Use default", 96f, 24f, false, () -> message.clear());
    private Widget active;
    private float x, y, w, h;

    public DungeonScoreMessageScreen(GuiScreen parent) {
        this.parent = parent;
        message.setText(AppleSettings.dungeonSPlusCustomMessage);
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        layout();
    }

    private void layout() {
        w = Math.min(440f, Math.max(220f, width - 16f));
        h = 154f;
        x = (width - w) * 0.5f;
        y = (height - h) * 0.5f;
        message.x = x + 14f;
        message.y = y + 62f;
        message.w = w - 28f;
        message.h = 25f;
        clearButton.x = x + 14f;
        clearButton.y = y + h - 39f;
        saveButton.x = x + w - 96f;
        saveButton.y = y + h - 39f;
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    @Override
    public void drawScreen(int mx, int my, float partialTicks) {
        layout();
        drawDefaultBackground();
        UIRender.beginFrame();
        UIRender.rect(0f, 0f, width, height, Theme.veil);
        UIRender.shadow(x, y, w, h, Math.min(Theme.radius, 12f));
        UIRender.roundRect(x, y, w, h, Theme.radius, Theme.windowTop, Theme.windowBottom, .7f);
        UIRender.roundRectRing(x, y, w, h, Theme.radius, 1f, Theme.border);
        UIRender.text(fr(), "S+ Party Message", x + 14f, y + 12f, Theme.textPrimary, 1f, false,
                GlobalFontManager.Area.EDITOR);
        UIRender.text(fr(), "Sent as /pc <message> when virtual score reaches 300.", x + 14f, y + 31f,
                Theme.textSecondary, .78f, false, GlobalFontManager.Area.EDITOR);
        UIRender.text(fr(), "Empty uses the default text: 300 Score!", x + 14f, y + 43f,
                Theme.textTertiary, .72f, false, GlobalFontManager.Area.EDITOR);
        message.update(1f / 60f);
        clearButton.update(1f / 60f);
        saveButton.update(1f / 60f);
        message.draw(mx, my);
        clearButton.draw(mx, my);
        saveButton.draw(mx, my);
        UIRender.endFrame();
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        if (button != 0) return;
        if (message.contains(mx, my)) {
            active = null;
            message.setFocused(true);
            return;
        }
        message.setFocused(false);
        if (saveButton.contains(mx, my)) active = saveButton;
        else if (clearButton.contains(mx, my)) active = clearButton;
        else active = null;
        if (active != null) active.onPress(mx, my);
    }

    @Override
    protected void mouseReleased(int mx, int my, int button) {
        if (button != 0) { active = null; return; }
        if (active == null) return;
        Widget released = active;
        active = null;
        released.onRelease(mx, my, released.contains(mx, my));
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (message.isFocused() && message.keyTyped(typedChar, keyCode)) return;
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            saveAndClose();
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            Minecraft.getMinecraft().displayGuiScreen(parent);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private void saveAndClose() {
        String value = message.text() == null ? "" : message.text().replace('\r', ' ').replace('\n', ' ').trim();
        if (value.length() > 100) value = value.substring(0, 100);
        AppleSettings.dungeonSPlusCustomMessage = value;
        AppleSettings.save();
        Minecraft.getMinecraft().displayGuiScreen(parent);
    }
}
