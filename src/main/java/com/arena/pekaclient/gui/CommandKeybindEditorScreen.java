package com.arena.pekaclient.gui;

import com.arena.pekaclient.PekaClientMod;
import com.arena.pekaclient.assist.CommandKeybindSettings;
import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import com.arena.pekaclient.widget.AppleButton;
import com.arena.pekaclient.widget.AppleKeybindButton;
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
import java.util.List;
import java.util.function.IntSupplier;

/**
 * Centered, solid Command Keybind window. It deliberately follows the settings-window scale rather than behaving as
 * a tiny corner palette: ordinary screens receive a sixty-percent-wide working surface, while narrow launcher GUI
 * coordinates switch entries to a two-line form before their key, command, or Delete control can overlap.
 */
public final class CommandKeybindEditorScreen extends AppleScreen {

    private static final float PANEL_PAD = 12f;
    private static final float BUTTON_H = 24f;
    private static final float KEY_W = 88f;
    private static final float DELETE_W = 66f;
    private static final float CONTROL_GAP = 8f;
    private static final float WARNING_SCALE = .56f;

    private final GuiScreen parent;
    private final List<BindingRow> rows = new ArrayList<BindingRow>();
    private final ScrollState scroll = new ScrollState();
    private final CommandKeybindEditorLayout.Layout layout = new CommandKeybindEditorLayout.Layout();
    private final AppleButton addButton = new AppleButton("Add", 62f, BUTTON_H, true, new Runnable() {
        public void run() {
            if (CommandKeybindSettings.add()) {
                rebuildRows();
                notice = "New command keybind added";
            } else {
                notice = "Maximum of " + CommandKeybindSettings.MAX_ENTRIES + " command keybinds reached";
            }
        }
    });
    private final AppleButton resetButton = new AppleButton("Reset", 70f, BUTTON_H, false, new Runnable() {
        public void run() {
            CommandKeybindSettings.reset();
            rebuildRows();
            cancelCapture();
            notice = "Command keybinds reset";
        }
    });

    private Widget activeWidget;
    private float sf = 1f;
    private long lastFrameNanos;
    private int captureIndex = -1;
    private int focusedCommandIndex = -1;
    private String notice = "Click a key to capture it. Escape cancels; Backspace clears.";

    public CommandKeybindEditorScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        lastFrameNanos = System.nanoTime();
        rebuildRows();
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
        scroll.update(dt, false, AppleSettings.smoothScrolling, Theme.speed);
        updateWidgets(dt);

        UIRender.beginFrame();
        try {
            drawPanel(mouseX, mouseY);
        } finally {
            UIRender.endFrame();
        }
    }

    private void layoutPanel() {
        CommandKeybindEditorLayout.layout(layout, width, height);
        scroll.setMax(Math.max(0f, rows.size() * layout.rowH - layout.contentHeight()));
    }

    private void updateWidgets(float dt) {
        addButton.update(dt);
        resetButton.update(dt);
        for (int i = 0; i < rows.size(); i++) rows.get(i).update(dt);
    }

    private void drawPanel(int mouseX, int mouseY) {
        // The outer panel is intentionally opaque even if the parent settings window has optional glass enabled.
        UIRender.shadow(layout.panelX, layout.panelY, layout.panelW, layout.panelH, 8f);
        UIRender.roundRectBorder(layout.panelX, layout.panelY, layout.panelW, layout.panelH, 8f,
                Theme.ITEM_ANIMATION_PANEL, Theme.ITEM_ANIMATION_PANEL, Theme.ITEM_ANIMATION_PANEL_BORDER);
        drawTitleBar(mouseX, mouseY);
        drawIntro();

        UIRender.scissorBegin(layout.panelX + 3f, layout.contentTop, layout.panelW - 6f, layout.contentHeight(), sf);
        try {
            for (int i = 0; i < rows.size(); i++) {
                drawRow(rows.get(i), layout.contentTop + i * layout.rowH - scroll.value(), mouseX, mouseY);
            }
            if (rows.isEmpty()) {
                UIRender.text(fr(), "No command keybinds yet. Use Add to create one.", layout.panelX + PANEL_PAD,
                        layout.contentTop + 12f, Theme.textSecondary, Theme.TYPE_BODY, false);
            }
        } finally {
            UIRender.scissorEnd();
        }
        drawScrollBar();
        drawFooter(mouseX, mouseY);
    }

    private void drawTitleBar(int mouseX, int mouseY) {
        float titleY = layout.panelY + Math.max(6f, (layout.headerH - fr().FONT_HEIGHT * Theme.TYPE_TITLE) * .5f);
        UIRender.text(fr(), "Command Keybind", layout.panelX + PANEL_PAD, titleY, Theme.textPrimary, Theme.TYPE_TITLE, false);
        float closeSize = Math.min(22f, Math.max(14f, layout.headerH - 10f));
        float closeX = layout.panelX + layout.panelW - PANEL_PAD - closeSize;
        float closeY = layout.panelY + (layout.headerH - closeSize) * .5f;
        boolean closeHover = inside(mouseX, mouseY, closeX, closeY, closeSize, closeSize);
        int closeFill = closeHover ? Theme.buttonHover : Theme.buttonFill;
        UIRender.roundRectBorder(closeX, closeY, closeSize, closeSize, 5f, closeFill, closeFill, Theme.border);
        UIRender.text(fr(), "×", closeX + (closeSize - fr().getStringWidth("×") * .92f) * .5f,
                closeY + Math.max(2f, (closeSize - fr().FONT_HEIGHT * .92f) * .5f),
                closeHover ? Theme.textPrimary : Theme.textSecondary, .92f, false);
        UIRender.line(layout.panelX + PANEL_PAD, layout.panelY + layout.headerH,
                layout.panelX + layout.panelW - PANEL_PAD, layout.panelY + layout.headerH, 1f, Theme.divider);
    }

    private void drawIntro() {
        String message = "A command is active when it has both a key and a slash command.";
        float max = Math.max(20f, layout.panelW - PANEL_PAD * 2f);
        float y = layout.panelY + layout.headerH + Math.max(5f, (layout.introH - fr().FONT_HEIGHT * Theme.TYPE_BODY) * .5f);
        UIRender.text(fr(), fit(fr(), message, max / Theme.TYPE_BODY), layout.panelX + PANEL_PAD, y,
                Theme.textSecondary, Theme.TYPE_BODY, false);
        UIRender.line(layout.panelX + PANEL_PAD, layout.contentTop - 1f,
                layout.panelX + layout.panelW - PANEL_PAD, layout.contentTop - 1f, 1f, Theme.divider);
    }

    /** Draws a readable entry with a small red collision message in that same key row when needed. */
    private void drawRow(BindingRow row, float y, int mouseX, int mouseY) {
        CommandKeybindSettings.Entry entry = CommandKeybindSettings.entryAt(row.index);
        if (entry == null) return;
        float x = layout.panelX + PANEL_PAD;
        float rowW = Math.max(1f, layout.panelW - PANEL_PAD * 2f);
        UIRender.roundRectBorder(x, y + 2f, rowW, Math.max(1f, layout.rowH - 4f), 5f,
                Theme.buttonFill, Theme.buttonFill, Theme.border);

        if (layout.compactRows) {
            layoutCompactRow(row, entry, x, y, rowW, mouseX, mouseY);
        } else {
            layoutWideRow(row, entry, x, y, rowW, mouseX, mouseY);
        }
        String warning = collisionWarning(row.index, entry);
        if (warning.length() > 0) {
            float warningY = layout.compactRows ? y + layout.rowH - 12f : y + layout.rowH - 12f;
            UIRender.text(fr(), fit(fr(), warning, Math.max(16f, rowW - 16f) / WARNING_SCALE), x + 8f, warningY,
                    Theme.STATUS_CRITICAL, WARNING_SCALE, false);
        }
    }

    /** Wide desktop form: Key, command, and Delete remain on one normal-sized control line. */
    private void layoutWideRow(BindingRow row, CommandKeybindSettings.Entry entry, float x, float y, float rowW,
                               int mouseX, int mouseY) {
        float innerX = x + 8f;
        float innerW = Math.max(1f, rowW - 16f);
        float deleteW = Math.min(DELETE_W, Math.max(38f, innerW * .24f));
        float keyW = Math.min(KEY_W, Math.max(42f, innerW * .31f));
        float commandW = Math.max(1f, innerW - keyW - deleteW - CONTROL_GAP * 2f);
        float controlY = y + 5f;
        configureKey(row, innerX, controlY, keyW, mouseX, mouseY);
        row.deleteButton.x = innerX + innerW - deleteW;
        row.deleteButton.y = controlY;
        row.deleteButton.w = deleteW;
        row.deleteButton.h = BUTTON_H;
        row.deleteButton.draw(mouseX, mouseY);
        configureCommand(row, entry, innerX + keyW + CONTROL_GAP, controlY, commandW, mouseX, mouseY);
    }

    /** Narrow Android/launcher form: only the command field moves below the key/Delete pair, never outside the card. */
    private void layoutCompactRow(BindingRow row, CommandKeybindSettings.Entry entry, float x, float y, float rowW,
                                  int mouseX, int mouseY) {
        float innerX = x + 8f;
        float innerW = Math.max(1f, rowW - 16f);
        float deleteW = Math.min(DELETE_W, Math.max(28f, innerW * .30f));
        float keyW = Math.min(KEY_W, Math.max(1f, innerW - deleteW - CONTROL_GAP));
        float controlY = y + 5f;
        configureKey(row, innerX, controlY, keyW, mouseX, mouseY);
        row.deleteButton.x = innerX + innerW - deleteW;
        row.deleteButton.y = controlY;
        row.deleteButton.w = deleteW;
        row.deleteButton.h = BUTTON_H;
        row.deleteButton.draw(mouseX, mouseY);
        configureCommand(row, entry, innerX, y + BUTTON_H + 10f, innerW, mouseX, mouseY);
    }

    private void configureKey(BindingRow row, float x, float y, float w, int mouseX, int mouseY) {
        row.keyButton.x = x;
        row.keyButton.y = y;
        row.keyButton.w = Math.max(1f, w);
        row.keyButton.h = BUTTON_H;
        row.keyButton.draw(mouseX, mouseY);
    }

    private void configureCommand(BindingRow row, CommandKeybindSettings.Entry entry, float x, float y, float w,
                                  int mouseX, int mouseY) {
        row.commandField.x = x;
        row.commandField.y = y;
        row.commandField.w = Math.max(1f, w);
        row.commandField.h = BUTTON_H;
        if (focusedCommandIndex != row.index && !row.commandField.text().equals(entry.command())) {
            row.commandField.setText(entry.command());
        }
        row.commandField.draw(mouseX, mouseY);
    }

    private void drawFooter(int mouseX, int mouseY) {
        UIRender.line(layout.panelX + PANEL_PAD, layout.contentBottom, layout.panelX + layout.panelW - PANEL_PAD,
                layout.contentBottom, 1f, Theme.divider);
        float footerY = layout.contentBottom + Math.max(2f, (layout.footerH - BUTTON_H) * .5f);
        float left = layout.panelX + PANEL_PAD;
        addButton.x = left;
        addButton.y = footerY;
        addButton.w = 62f;
        addButton.h = BUTTON_H;
        addButton.draw(mouseX, mouseY);
        resetButton.x = addButton.x + addButton.w + 7f;
        resetButton.y = footerY;
        resetButton.w = 70f;
        resetButton.h = BUTTON_H;
        resetButton.draw(mouseX, mouseY);
        float noticeX = resetButton.x + resetButton.w + 9f;
        float noticeW = Math.max(0f, layout.panelX + layout.panelW - PANEL_PAD - noticeX);
        if (noticeW >= 28f) {
            UIRender.text(fr(), fit(fr(), notice, noticeW / Theme.TYPE_META), noticeX,
                    footerY + Math.max(2f, (BUTTON_H - fr().FONT_HEIGHT * Theme.TYPE_META) * .5f),
                    Theme.textTertiary, Theme.TYPE_META, false);
        }
    }

    private void drawScrollBar() {
        if (scroll.max() <= .5f) return;
        float view = layout.contentHeight();
        float total = view + scroll.max();
        float thumbH = Math.max(18f, view * view / total);
        float thumbY = layout.contentTop + (view - thumbH) * (scroll.value() / Math.max(1f, scroll.max()));
        UIRender.roundRect(layout.panelX + layout.panelW - 5f, thumbY, 2f, thumbH, 1f,
                Theme.textTertiary, Theme.textTertiary, 1f);
    }

    @Override
    public void handleMouseInput() throws IOException {
        int wheel = Mouse.getEventDWheel();
        super.handleMouseInput();
        int mouseX = Mouse.getEventX() * width / Math.max(1, Minecraft.getMinecraft().displayWidth);
        int mouseY = height - Mouse.getEventY() * height / Math.max(1, Minecraft.getMinecraft().displayHeight) - 1;
        if (wheel != 0 && inside(mouseX, mouseY, layout.panelX, layout.contentTop, layout.panelW, layout.contentHeight())) {
            boolean fine = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
            scroll.wheel(wheel / 120f, fine, 1f);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        if (button != 0) return;
        float closeSize = Math.min(22f, Math.max(14f, layout.headerH - 10f));
        float closeX = layout.panelX + layout.panelW - PANEL_PAD - closeSize;
        float closeY = layout.panelY + (layout.headerH - closeSize) * .5f;
        if (inside(mouseX, mouseY, closeX, closeY, closeSize, closeSize)) {
            close();
            return;
        }
        Widget widget = widgetAt(mouseX, mouseY);
        if (widget == null) {
            focusCommand(-1);
            return;
        }
        BindingRow row = rowFor(widget);
        if (row != null && widget == row.commandField) {
            cancelCapture();
            focusCommand(row.index);
        } else {
            // Leaving a field keeps its complete draft; persistence trims only outer whitespace during settings save.
            focusCommand(-1);
            if (row != null && widget == row.keyButton) cancelCapture();
        }
        activeWidget = widget;
        activeWidget.onPress(mouseX, mouseY);
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
        BindingRow row = rowFor(released);
        if (row != null && released == row.keyButton && row.keyButton.isListening()) {
            captureIndex = row.index;
            notice = "Press a key for command keybind " + (row.index + 1) + ". Escape cancels; Backspace clears.";
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (captureIndex >= 0) {
            handleCapturedKey(keyCode);
            return;
        }
        if (focusedCommandIndex >= 0) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                focusCommand(-1);
                notice = "Command edit canceled";
                return;
            }
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                focusCommand(-1);
                notice = "Command saved";
                return;
            }
            BindingRow row = rowAt(focusedCommandIndex);
            if (row == null) {
                focusCommand(-1);
                return;
            }
            // Minecraft can report a zero event character for KEY_SPACE on some 1.8.9 GUI paths. Supply the ordinary
            // printable character ourselves and always consume it while this command field owns focus. All normal
            // spaces remain in the draft; AppleSettings.save performs the deliberate outer-only persistence trim.
            char acceptedChar = keyCode == Keyboard.KEY_SPACE && typedChar == 0 ? ' ' : typedChar;
            String before = row.commandField.text();
            boolean consumed = row.commandField.keyTyped(acceptedChar, keyCode);
            if (keyCode == Keyboard.KEY_SPACE || consumed || !before.equals(row.commandField.text())) return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            close();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private void handleCapturedKey(int keyCode) {
        BindingRow row = rowAt(captureIndex);
        if (row == null) {
            cancelCapture();
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            cancelCapture();
            notice = "Key capture canceled";
            return;
        }
        if (keyCode == Keyboard.KEY_BACK) {
            CommandKeybindSettings.setKeyCode(row.index, 0);
            row.keyButton.finishCapture();
            captureIndex = -1;
            notice = "Key cleared";
            return;
        }
        if (!CommandKeybindSettings.validKeyCode(keyCode)) return;
        CommandKeybindSettings.setKeyCode(row.index, keyCode);
        row.keyButton.finishCapture();
        captureIndex = -1;
        CommandKeybindSettings.Entry entry = CommandKeybindSettings.entryAt(row.index);
        String warning = entry == null ? "" : collisionWarning(row.index, entry);
        notice = warning.length() == 0 ? "Key captured" : warning;
    }

    private Widget widgetAt(int mouseX, int mouseY) {
        if (inside(mouseX, mouseY, addButton.x, addButton.y, addButton.w, addButton.h)) return addButton;
        if (inside(mouseX, mouseY, resetButton.x, resetButton.y, resetButton.w, resetButton.h)) return resetButton;
        if (!inside(mouseX, mouseY, layout.panelX, layout.contentTop, layout.panelW, layout.contentHeight())) return null;
        for (int i = 0; i < rows.size(); i++) {
            BindingRow row = rows.get(i);
            if (row.keyButton.contains(mouseX, mouseY)) return row.keyButton;
            if (row.commandField.contains(mouseX, mouseY)) return row.commandField;
            if (row.deleteButton.contains(mouseX, mouseY)) return row.deleteButton;
        }
        return null;
    }

    private BindingRow rowFor(Widget widget) {
        if (widget == null) return null;
        for (int i = 0; i < rows.size(); i++) if (rows.get(i).owns(widget)) return rows.get(i);
        return null;
    }

    private BindingRow rowAt(int index) {
        return index >= 0 && index < rows.size() ? rows.get(index) : null;
    }

    private void focusCommand(int index) {
        if (focusedCommandIndex >= 0 && focusedCommandIndex != index) commitCommand(focusedCommandIndex);
        focusedCommandIndex = index;
        for (int i = 0; i < rows.size(); i++) rows.get(i).commandField.setFocused(rows.get(i).index == index);
    }

    /** Commits a complete draft only when editing ends; command serialization owns the later outer-only trim. */
    private void commitCommand(int index) {
        BindingRow row = rowAt(index);
        if (row == null) return;
        CommandKeybindSettings.setCommand(row.index, row.commandField.text());
        CommandKeybindSettings.Entry entry = CommandKeybindSettings.entryAt(row.index);
        if (entry != null) row.commandField.setText(entry.command());
    }

    private void cancelCapture() {
        if (captureIndex >= 0) {
            BindingRow row = rowAt(captureIndex);
            if (row != null) row.keyButton.cancelCapture();
        }
        captureIndex = -1;
    }

    private void rebuildRows() {
        rows.clear();
        for (int i = 0; i < CommandKeybindSettings.count(); i++) rows.add(new BindingRow(i));
        focusedCommandIndex = -1;
        captureIndex = -1;
        scroll.reset();
    }

    private void deleteRow(int index) {
        focusCommand(-1);
        if (CommandKeybindSettings.delete(index)) {
            rebuildRows();
            notice = "Command keybind deleted";
        }
    }

    /** Collision warnings are deliberately visible inside the affected entry instead of only in a transient toast. */
    private String collisionWarning(int index, CommandKeybindSettings.Entry entry) {
        if (entry == null || !CommandKeybindSettings.validKeyCode(entry.keyCode())) return "";
        int keyCode = entry.keyCode();
        if (CommandKeybindSettings.hasOtherEntryWithKey(index, keyCode)) {
            return "Warning: another Command Keybind uses this key";
        }
        String skyVelor = skyVelorKeyName(keyCode);
        if (skyVelor != null) return "Warning: conflicts with Sky VeLor " + skyVelor;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.gameSettings != null && mc.gameSettings.keyBindings != null) {
            KeyBinding[] bindings = mc.gameSettings.keyBindings;
            for (int i = 0; i < bindings.length; i++) {
                KeyBinding binding = bindings[i];
                if (binding != null && binding.getKeyCode() == keyCode) {
                    return "Warning: conflicts with Minecraft " + binding.getKeyDescription();
                }
            }
        }
        return "";
    }

    private static String skyVelorKeyName(int keyCode) {
        if (keyCode == PekaClientMod.OPEN_KEY.getKeyCode()) return "Open Menu";
        if (keyCode == PekaClientMod.ITEM_ANIMATION_KEY.getKeyCode()) return "Item Animation";
        if (keyCode == PekaClientMod.LEAP_KEY.getKeyCode()) return "Spirit Leap";
        if (keyCode == PekaClientMod.CHAT_TAB_KEY.getKeyCode()) return "Chat Tab";
        if (keyCode == PekaClientMod.CHAT_EDITOR_KEY.getKeyCode()) return "Chat Settings";
        if (keyCode == PekaClientMod.ITEM_LOCK_SWAP_KEY.getKeyCode()) return "Item Lock And Swap";
        if (keyCode == PekaClientMod.WARDROBE_SHORTCUT_KEY.getKeyCode()) return "Wardrobe Shortcut";
        if (keyCode == PekaClientMod.PETS_SHORTCUT_KEY.getKeyCode()) return "Pets Shortcut";
        return null;
    }

    private void close() {
        cancelCapture();
        focusCommand(-1);
        AppleSettings.save();
        Minecraft.getMinecraft().displayGuiScreen(parent);
    }

    @Override
    public void onGuiClosed() {
        cancelCapture();
        focusCommand(-1);
        Keyboard.enableRepeatEvents(false);
        AppleSettings.save();
        UIRender.restoreVanillaGuiState();
        super.onGuiClosed();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private final class BindingRow {
        final int index;
        final AppleKeybindButton keyButton;
        final SearchField commandField;
        final AppleButton deleteButton;

        BindingRow(final int index) {
            this.index = index;
            keyButton = new AppleKeybindButton(KEY_W, BUTTON_H, new IntSupplier() {
                public int getAsInt() {
                    CommandKeybindSettings.Entry entry = CommandKeybindSettings.entryAt(index);
                    return entry == null ? 0 : entry.keyCode();
                }
            });
            commandField = new SearchField("/command");
            CommandKeybindSettings.Entry entry = CommandKeybindSettings.entryAt(index);
            commandField.setText(entry == null ? "/" : entry.command());
            deleteButton = new AppleButton("Delete", DELETE_W, BUTTON_H, false, new Runnable() {
                public void run() {
                    deleteRow(index);
                }
            });
        }

        void update(float dt) {
            keyButton.update(dt);
            commandField.update(dt);
            deleteButton.update(dt);
        }

        boolean owns(Widget widget) {
            return widget == keyButton || widget == commandField || widget == deleteButton;
        }
    }
}
