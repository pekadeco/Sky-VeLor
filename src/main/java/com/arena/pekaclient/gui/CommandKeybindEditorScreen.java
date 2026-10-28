package com.arena.pekaclient.gui;

import com.arena.pekaclient.PekaClientMod;
import com.arena.pekaclient.assist.CommandKeybindSettings;
import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import com.arena.pekaclient.widget.AppleButton;
import com.arena.pekaclient.widget.AppleKeybindButton;
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
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Solid, undimmed Shortcut command editor. It follows the Item Animation editor's opaque surface while deliberately
 * sizing itself to its short, one-line key/command/toggle/delete entries instead of occupying the whole screen.
 */
public final class CommandKeybindEditorScreen extends AppleScreen {

    private static final float PANEL_MARGIN = 8f;
    private static final float PANEL_WIDTH_FRACTION = .34f;
    private static final float PANEL_MIN_WIDTH = 360f;
    private static final float PANEL_MAX_WIDTH = 520f;
    private static final float HEADER_H = 29f;
    private static final float INTRO_H = 22f;
    private static final float FOOTER_H = 35f;
    private static final float ROW_H = 30f;
    private static final float BUTTON_H = 20f;
    /** Seven direct entries remain compact; only an unusually long list earns a scroll surface. */
    private static final int MAX_VISIBLE_ROWS = 7;
    private static final float KEY_W = 72f;
    private static final float DELETE_W = 56f;

    private final GuiScreen parent;
    private final List<BindingRow> rows = new ArrayList<BindingRow>();
    private final ScrollState scroll = new ScrollState();
    private final AppleButton addButton = new AppleButton("Add", 58f, BUTTON_H, true, new Runnable() {
        public void run() {
            if (CommandKeybindSettings.add()) {
                rebuildRows();
                notice = "New command keybind added";
            } else {
                notice = "Maximum of " + CommandKeybindSettings.MAX_ENTRIES + " command keybinds reached";
            }
        }
    });
    private final AppleButton resetButton = new AppleButton("Reset", 62f, BUTTON_H, false, new Runnable() {
        public void run() {
            CommandKeybindSettings.reset();
            rebuildRows();
            cancelCapture();
            notice = "Command keybinds reset";
        }
    });

    private Widget activeWidget;
    private float panelX;
    private float panelY;
    private float panelW;
    private float panelH;
    private float contentTop;
    private float contentBottom;
    private float sf = 1f;
    private long lastFrameNanos;
    private int captureIndex = -1;
    private int focusedCommandIndex = -1;
    private String notice = "Click a key to capture it. Escape cancels; Backspace clears it.";

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
        panelX = PANEL_MARGIN;
        panelY = PANEL_MARGIN;
        float usableWidth = Math.max(1f, width - PANEL_MARGIN * 2f);
        panelW = Math.min(usableWidth, Math.max(PANEL_MIN_WIDTH,
                Math.min(PANEL_MAX_WIDTH, width * PANEL_WIDTH_FRACTION)));
        int visibleRows = Math.min(MAX_VISIBLE_ROWS, Math.max(1, rows.size()));
        float rowArea = rows.isEmpty() ? 31f : visibleRows * ROW_H;
        float desiredH = HEADER_H + INTRO_H + rowArea + FOOTER_H;
        float availableH = Math.max(1f, height - PANEL_MARGIN * 2f);
        panelH = Math.min(availableH, Math.max(HEADER_H + INTRO_H + FOOTER_H + 28f, desiredH));
        contentTop = panelY + HEADER_H + INTRO_H;
        contentBottom = panelY + panelH - FOOTER_H;
        scroll.setMax(Math.max(0f, rows.size() * ROW_H - Math.max(1f, contentBottom - contentTop)));
    }

    private void updateWidgets(float dt) {
        addButton.update(dt);
        resetButton.update(dt);
        for (int i = 0; i < rows.size(); i++) rows.get(i).update(dt);
    }

    private void drawPanel(int mouseX, int mouseY) {
        // Like Item Animation, this editor has one opaque surface and deliberately does not dim the game behind it.
        UIRender.roundRectBorder(panelX, panelY, panelW, panelH, 7f,
                Theme.ITEM_ANIMATION_PANEL, Theme.ITEM_ANIMATION_PANEL, Theme.ITEM_ANIMATION_PANEL_BORDER);
        UIRender.text(fr(), "Command Keybind", panelX + 10f, panelY + 10f, Theme.textPrimary, .88f, false);
        float closeX = panelX + panelW - 20f;
        boolean closeHover = inside(mouseX, mouseY, closeX, panelY + 5f, 15f, 16f);
        int closeFill = closeHover ? Theme.buttonHover : Theme.buttonFill;
        UIRender.roundRect(closeX, panelY + 5f, 15f, 16f, 4f, closeFill, closeFill, 1f);
        UIRender.text(fr(), "×", closeX + 4.5f, panelY + 8f,
                closeHover ? Theme.textPrimary : Theme.textSecondary, .86f, false);
        UIRender.line(panelX + 7f, panelY + HEADER_H, panelX + panelW - 7f, panelY + HEADER_H, 1f, Theme.divider);
        UIRender.text(fr(), fit(fr(), "Commands run only from a physical key while no screen is open.", panelW - 20f),
                panelX + 10f, panelY + HEADER_H + 6f, Theme.textTertiary, .58f, false);

        UIRender.scissorBegin(panelX + 1f, contentTop, panelW - 2f, contentBottom - contentTop, sf);
        try {
            for (int i = 0; i < rows.size(); i++) {
                drawRow(rows.get(i), contentTop + i * ROW_H - scroll.value(), mouseX, mouseY);
            }
            if (rows.isEmpty()) {
                UIRender.text(fr(), "No command keybinds yet. Use Add to create one.", panelX + 10f, contentTop + 9f,
                        Theme.textSecondary, .62f, false);
            }
        } finally {
            UIRender.scissorEnd();
        }
        drawScrollBar();
        drawFooter(mouseX, mouseY);
    }

    /** One entry stays on one readable line: key, editable command, on/off toggle, then a fully sized Delete button. */
    private void drawRow(BindingRow row, float y, int mouseX, int mouseY) {
        CommandKeybindSettings.Entry entry = CommandKeybindSettings.entryAt(row.index);
        if (entry == null) return;
        float x = panelX + 6f;
        float rowW = panelW - 12f;
        UIRender.roundRectBorder(x, y + 2f, rowW, ROW_H - 4f, 4f,
                Theme.buttonFill, Theme.buttonFill, Theme.border);

        row.keyButton.x = x + 6f;
        row.keyButton.y = y + 5f;
        row.keyButton.w = KEY_W;
        row.keyButton.h = BUTTON_H;
        row.keyButton.draw(mouseX, mouseY);

        row.deleteButton.x = x + rowW - DELETE_W - 6f;
        row.deleteButton.y = y + 5f;
        row.deleteButton.w = DELETE_W;
        row.deleteButton.h = BUTTON_H;
        row.deleteButton.draw(mouseX, mouseY);

        row.enabledToggle.x = row.deleteButton.x - 45f;
        row.enabledToggle.y = y + 5f;
        row.enabledToggle.w = 38f;
        row.enabledToggle.h = BUTTON_H;
        row.enabledToggle.draw(mouseX, mouseY);
        UIRender.text(fr(), entry.enabled() ? "On" : "Off", row.enabledToggle.x - 15f, y + 10f,
                entry.enabled() ? Theme.STATUS_SUCCESS : Theme.textTertiary, .52f, false);

        row.commandField.x = row.keyButton.x + row.keyButton.w + 6f;
        row.commandField.y = y + 5f;
        // Never let a minimum field width cross into the On/toggle/delete controls on a small scaled display.
        row.commandField.w = Math.max(1f, row.enabledToggle.x - 19f - row.commandField.x);
        row.commandField.h = BUTTON_H;
        if (focusedCommandIndex != row.index && !row.commandField.text().equals(entry.command())) {
            row.commandField.setText(entry.command());
        }
        row.commandField.draw(mouseX, mouseY);
    }

    private void drawFooter(int mouseX, int mouseY) {
        UIRender.line(panelX + 7f, contentBottom, panelX + panelW - 7f, contentBottom, 1f, Theme.divider);
        addButton.x = panelX + 10f;
        addButton.y = contentBottom + 7f;
        addButton.w = 58f;
        addButton.h = BUTTON_H;
        addButton.draw(mouseX, mouseY);
        resetButton.x = addButton.x + addButton.w + 5f;
        resetButton.y = addButton.y;
        resetButton.w = 62f;
        resetButton.h = BUTTON_H;
        resetButton.draw(mouseX, mouseY);
        String footer = footerNotice();
        UIRender.text(fr(), fit(fr(), footer, Math.max(30f, panelW - 155f)), resetButton.x + resetButton.w + 8f,
                contentBottom + 13f, Theme.textTertiary, .54f, false);
    }

    /** A collision remains visible without making every entry a tall, two-line card. */
    private String footerNotice() {
        for (int i = 0; i < rows.size(); i++) {
            CommandKeybindSettings.Entry entry = CommandKeybindSettings.entryAt(rows.get(i).index);
            String warning = collisionWarning(rows.get(i).index, entry);
            if (warning.length() > 0) return warning;
        }
        return notice;
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
        if (wheel != 0 && inside(mouseX, mouseY, panelX, contentTop, panelW, contentBottom - contentTop)) {
            boolean fine = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
            scroll.wheel(wheel / 120f, fine, 1f);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        if (button != 0) return;
        if (inside(mouseX, mouseY, panelX + panelW - 20f, panelY + 5f, 15f, 16f)) {
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
            // Leaving a field commits its outer-trimmed value, preserving every inner space the player just typed.
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
            // printable character ourselves and always consume it while this command field owns focus.
            char acceptedChar = keyCode == Keyboard.KEY_SPACE && typedChar == 0 ? ' ' : typedChar;
            String before = row.commandField.text();
            boolean consumed = row.commandField.keyTyped(acceptedChar, keyCode);
            if (keyCode == Keyboard.KEY_SPACE || consumed || !before.equals(row.commandField.text())) {
                return;
            }
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
        if (!inside(mouseX, mouseY, panelX, contentTop, panelW, contentBottom - contentTop)) return null;
        for (int i = 0; i < rows.size(); i++) {
            BindingRow row = rows.get(i);
            if (row.keyButton.contains(mouseX, mouseY)) return row.keyButton;
            if (row.commandField.contains(mouseX, mouseY)) return row.commandField;
            if (row.enabledToggle.contains(mouseX, mouseY)) return row.enabledToggle;
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

    /** Commits only when editing ends, so a typed trailing space is not stripped before its next normal character. */
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

    /** Shows all three collision categories before a user leaves the editor. Collisions remain explicit, not hidden. */
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
        final AppleToggle enabledToggle;
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
            enabledToggle = new AppleToggle(new Supplier<Boolean>() {
                public Boolean get() {
                    CommandKeybindSettings.Entry entry = CommandKeybindSettings.entryAt(index);
                    return entry != null && entry.enabled();
                }
            }, new Consumer<Boolean>() {
                public void accept(Boolean value) {
                    CommandKeybindSettings.setEntryEnabled(index, Boolean.TRUE.equals(value));
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
            enabledToggle.update(dt);
            commandField.update(dt);
            deleteButton.update(dt);
        }

        boolean owns(Widget widget) {
            return widget == keyButton || widget == enabledToggle || widget == commandField || widget == deleteButton;
        }
    }
}
