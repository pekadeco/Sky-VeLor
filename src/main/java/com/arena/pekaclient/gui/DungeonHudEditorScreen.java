package com.arena.pekaclient.gui;

import com.arena.pekaclient.assist.dungeon.RoomClearAlertController;
import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.hud.HudElement;
import com.arena.pekaclient.hud.HudManager;
import com.arena.pekaclient.hud.HudPlacement;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
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
 * The self-contained editor for dungeon-only overlay elements. It always draws useful Score/Stats, Room Alert, and
 * Dungeon Map samples, including outside a dungeon, so movement and scale never depend on a live run. The responsive
 * layout keeps solid picker/inspector surfaces separate from the preview canvas on desktop and Pojav-sized screens.
 */
public final class DungeonHudEditorScreen extends AppleScreen {
    private static final float SNAP = 4F;
    private static final float TOUCH_MIN = 28F;
    private static final float ROW_H = 25F;
    private static final float OPTION_TOP = 31F;
    private static final float HANDLE_SIZE = 10F;

    private static final int TOGGLE = 0;
    private static final int SLIDER = 1;
    private static final int CHOICE = 2;
    private static final int ACTION = 3;

    private enum Target {
        SCORE("Score / Stats", HudManager.DUNGEON_SCORE),
        ALERT("Room Alert", HudManager.ROOM_CLEAR_ALERT),
        MAP("Dungeon Map", null);

        final String label;
        final HudElement element;

        Target(String label, HudElement element) {
            this.label = label;
            this.element = element;
        }
    }

    private static final class Option {
        final String id;
        final String label;
        final int type;
        final int min;
        final int max;
        final int step;
        final GuiLayoutToolkit.Rect row = new GuiLayoutToolkit.Rect();
        final GuiLayoutToolkit.Rect control = new GuiLayoutToolkit.Rect();

        Option(String id, String label, int type) {
            this(id, label, type, 0, 1, 1);
        }

        Option(String id, String label, int type, int min, int max, int step) {
            this.id = id;
            this.label = label;
            this.type = type;
            this.min = min;
            this.max = max;
            this.step = Math.max(1, step);
        }
    }

    private final GuiScreen parent;
    private final List<Option> options = new ArrayList<Option>();
    private Target selected = Target.SCORE;
    private DungeonHudEditorLayout layout;
    private float optionScroll;
    private float maxOptionScroll;
    private float guiScale = 1F;
    private boolean dragging;
    private boolean resizing;
    private boolean draggingSlider;
    private Target dragTarget;
    private Option sliderOption;
    private float grabX;
    private float grabY;
    private float resizeStartScale;
    private int resizeStartSize;
    private int resizeStartX;
    private int resizeStartY;
    private int guides;
    private boolean dirty;

    public DungeonHudEditorScreen(GuiScreen parent) {
        this.parent = parent;
        rebuildOptions();
    }

    /** Route used by Test Alert: opens on the alert target with a real local preview already armed. */
    public static DungeonHudEditorScreen openRoomAlertPreview(GuiScreen parent) {
        DungeonHudEditorScreen editor = new DungeonHudEditorScreen(parent);
        editor.selected = Target.ALERT;
        editor.rebuildOptions();
        return editor;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        HudManager.ensureLoaded();
        rebuildOptions();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        persist();
        UIRender.restoreVanillaGuiState();
        super.onGuiClosed();
    }

    private void persist() {
        if (!dirty) return;
        HudManager.save();
        AppleSettings.save();
        dirty = false;
    }

    private void changed() {
        dirty = true;
    }

    private void select(Target target) {
        if (target == null || target == selected) return;
        selected = target;
        optionScroll = 0F;
        rebuildOptions();
    }

    private void rebuildOptions() {
        options.clear();
        if (selected == Target.SCORE) {
            options.add(new Option("score-enabled", "Show HUD", TOGGLE));
            options.add(new Option("score-line", "Score and rank", TOGGLE));
            options.add(new Option("score-crypts", "Crypts", TOGGLE));
            options.add(new Option("score-deaths", "Deaths", TOGGLE));
            options.add(new Option("score-time", "Run time", TOGGLE));
            options.add(new Option("element-scale", "Size", SLIDER, 50, 200, 5));
            options.add(new Option("reset-target", "Reset selected", ACTION));
        } else if (selected == Target.ALERT) {
            options.add(new Option("alert-enabled", "Show alert", TOGGLE));
            options.add(new Option("alert-duration", "Alert duration", SLIDER,
                    AppleSettings.DUNGEON_ROOM_CLEAR_DURATION_MIN_MILLIS,
                    AppleSettings.DUNGEON_ROOM_CLEAR_DURATION_MAX_MILLIS, 250));
            options.add(new Option("alert-sound", "Local sound", TOGGLE));
            options.add(new Option("element-scale", "Size", SLIDER, 50, 200, 5));
            options.add(new Option("test-alert", "Test Alert", ACTION));
            options.add(new Option("reset-target", "Reset selected", ACTION));
        } else {
            options.add(new Option("map-enabled", "Show map", TOGGLE));
            options.add(new Option("map-scale-on", "Use map scale", TOGGLE));
            options.add(new Option("map-scale", "Map size", SLIDER,
                    AppleSettings.DUNGEON_MAP_SCALE_MIN, AppleSettings.DUNGEON_MAP_SCALE_MAX, 5));
            options.add(new Option("map-zoom", "Map zoom", SLIDER,
                    AppleSettings.DUNGEON_MAP_ZOOM_MIN, AppleSettings.DUNGEON_MAP_ZOOM_MAX, 5));
            options.add(new Option("map-rotate", "Rotate to facing", TOGGLE));
            options.add(new Option("map-names", "Player names", TOGGLE));
            options.add(new Option("map-heads", "Player heads", TOGGLE));
            options.add(new Option("map-head-size", "Head size", SLIDER,
                    AppleSettings.DUNGEON_MAP_PLAYER_HEAD_SIZE_MIN,
                    AppleSettings.DUNGEON_MAP_PLAYER_HEAD_SIZE_MAX, 1));
            options.add(new Option("map-room-names", "Room / puzzle names", TOGGLE));
            options.add(new Option("map-secrets", "Secrets", TOGGLE));
            options.add(new Option("map-background", "Map background", TOGGLE));
            options.add(new Option("map-background-opacity", "Background opacity", SLIDER,
                    AppleSettings.DUNGEON_MAP_BACKGROUND_OPACITY_MIN,
                    AppleSettings.DUNGEON_MAP_BACKGROUND_OPACITY_MAX, 5));
            options.add(new Option("map-background-color", "Background color", CHOICE));
            options.add(new Option("map-room-color", "Room name color", CHOICE));
            options.add(new Option("map-secrets-color", "Secrets color", CHOICE));
            options.add(new Option("map-announce", "Party puzzle announce", TOGGLE));
            options.add(new Option("reset-map-position", "Reset map position", ACTION));
            options.add(new Option("reset-target", "Reset selected", ACTION));
        }
    }

    private void updateLayout() {
        layout = DungeonHudEditorLayout.create(width, height);
        guiScale = new ScaledResolution(Minecraft.getMinecraft()).getScaleFactor();
        float viewportH = Math.max(1F, layout.inspector.h - OPTION_TOP - 5F);
        maxOptionScroll = Math.max(0F, options.size() * ROW_H + 4F - viewportH);
        optionScroll = clamp(optionScroll, 0F, maxOptionScroll);
        float y = layout.inspector.y + OPTION_TOP - optionScroll;
        for (Option option : options) {
            option.row.set(layout.inspector.x + 5F, y, Math.max(1F, layout.inspector.w - 10F), ROW_H - 2F);
            float controlW = Math.max(54F, option.row.w * .48F);
            option.control.set(option.row.right() - controlW, option.row.y + 3F, controlW, ROW_H - 8F);
            if (option.type == ACTION) option.control.set(option.row.x, option.row.y + 2F, option.row.w, ROW_H - 6F);
            y += ROW_H;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        updateLayout();
        drawDefaultBackground();
        UIRender.beginFrame();
        try {
            UIRender.rect(0F, 0F, width, height, Theme.veil);
        } finally {
            UIRender.endFrame();
        }

        // The real HUD classes render their editor-safe samples even when no world/sidebar values exist.
        HudManager.renderDungeonEditor(new ScaledResolution(Minecraft.getMinecraft()), mouseX, mouseY);
        drawDungeonMapPreview();

        UIRender.beginFrame();
        try {
            drawSelection();
            drawGuides();
            drawHeader(mouseX, mouseY);
            drawPicker(mouseX, mouseY);
            drawInspector(mouseX, mouseY);
        } finally {
            UIRender.endFrame();
        }
    }

    private void drawHeader(int mouseX, int mouseY) {
        UIRender.rect(layout.header.x, layout.header.y, layout.header.w, layout.header.h, Theme.ITEM_ANIMATION_PANEL);
        UIRender.line(0F, layout.header.bottom() - 1F, width, layout.header.bottom() - 1F, 1F,
                Theme.ITEM_ANIMATION_PANEL_BORDER);
        FontRenderer font = fr();
        float doneW = layout.compact ? 40F : 50F;
        float allW = layout.compact ? 40F : 68F;
        float selectedW = layout.compact ? 46F : 92F;
        float x = width - 5F - doneW;
        drawButton(x, 4F, doneW, 20F, "Done", true, mouseX, mouseY);
        x -= 4F + allW;
        drawButton(x, 4F, allW, 20F, layout.compact ? "All" : "Reset all", false, mouseX, mouseY);
        x -= 4F + selectedW;
        drawButton(x, 4F, selectedW, 20F, layout.compact ? "Target" : "Reset target", false, mouseX, mouseY);
        String title = fit(font, "Dungeon HUD Editor", Math.max(45F, x - 10F));
        UIRender.text(font, title, 8F, 10F, Theme.textPrimary, .92F, false);
    }

    private void drawPicker(int mouseX, int mouseY) {
        drawSolidPanel(layout.picker);
        FontRenderer font = fr();
        if (!layout.compact) {
            UIRender.text(font, "ELEMENTS", layout.picker.x + 9F, layout.picker.y + 10F, Theme.textSecondary, .68F, false);
        }
        for (int i = 0; i < Target.values().length; i++) {
            Target target = Target.values()[i];
            GuiLayoutToolkit.Rect row = pickerRow(i);
            drawButton(row.x, row.y, row.w, row.h, target.label, target == selected, mouseX, mouseY);
        }
        if (!layout.compact) {
            UIRender.text(font, "Tap an element or choose it here.", layout.picker.x + 9F,
                    Math.min(layout.picker.bottom() - 14F, layout.picker.y + 119F), Theme.textSecondary, .62F, false);
            UIRender.text(font, "Drag to move. Pull the corner", layout.picker.x + 9F,
                    Math.min(layout.picker.bottom() - 5F, layout.picker.y + 129F), Theme.textSecondary, .62F, false);
        }
    }

    private GuiLayoutToolkit.Rect pickerRow(int index) {
        GuiLayoutToolkit.Rect row = new GuiLayoutToolkit.Rect();
        if (layout.compact) {
            float gap = 3F;
            float w = (layout.picker.w - gap * 2F) / 3F;
            row.set(layout.picker.x + index * (w + gap), layout.picker.y + 4F, w, layout.picker.h - 8F);
        } else {
            row.set(layout.picker.x + 7F, layout.picker.y + 25F + index * 28F,
                    Math.max(1F, layout.picker.w - 14F), 23F);
        }
        return row;
    }

    private void drawInspector(int mouseX, int mouseY) {
        drawSolidPanel(layout.inspector);
        FontRenderer font = fr();
        UIRender.text(font, fit(font, selected.label + " options", layout.inspector.w - 18F),
                layout.inspector.x + 8F, layout.inspector.y + 10F, Theme.textPrimary, .78F, false);
        UIRender.line(layout.inspector.x + 6F, layout.inspector.y + 24F, layout.inspector.right() - 6F,
                layout.inspector.y + 24F, 1F, Theme.expandedDivider);
        float clipY = layout.inspector.y + OPTION_TOP;
        float clipH = Math.max(1F, layout.inspector.h - OPTION_TOP - 5F);
        UIRender.scissorBegin(layout.inspector.x + 2F, clipY, Math.max(1F, layout.inspector.w - 4F), clipH, guiScale);
        try {
            for (Option option : options) drawOption(option, mouseX, mouseY);
        } finally {
            UIRender.scissorEnd();
        }
        if (maxOptionScroll > 0F) {
            float thumbH = Math.max(16F, clipH * clipH / (clipH + maxOptionScroll));
            float travel = Math.max(0F, clipH - thumbH);
            float thumbY = clipY + travel * optionScroll / maxOptionScroll;
            UIRender.roundRect(layout.inspector.right() - 4F, thumbY, 2F, thumbH, 1F,
                    Theme.textTertiary, Theme.textTertiary, 1F);
        }
    }

    private void drawOption(Option option, int mouseX, int mouseY) {
        boolean hot = option.control.contains(mouseX, mouseY);
        FontRenderer font = fr();
        if (option.type == ACTION) {
            drawButton(option.control.x, option.control.y, option.control.w, option.control.h,
                    option.label, false, mouseX, mouseY);
            return;
        }
        UIRender.text(font, fit(font, option.label, Math.max(1F, option.control.x - option.row.x - 5F)),
                option.row.x, option.row.y + 8F, Theme.textSecondary, .66F, false);
        if (option.type == TOGGLE) {
            boolean on = boolValue(option.id);
            drawButton(option.control.x, option.control.y, option.control.w, option.control.h,
                    on ? "On" : "Off", on, mouseX, mouseY);
        } else if (option.type == CHOICE) {
            drawButton(option.control.x, option.control.y, option.control.w, option.control.h,
                    fit(font, choiceName(option.id), option.control.w - 8F), false, mouseX, mouseY);
        } else {
            drawSlider(option, hot, mouseX);
        }
    }

    private void drawSlider(Option option, boolean hot, int mouseX) {
        int value = intValue(option.id);
        float fraction = (clamp(value, option.min, option.max) - option.min) / (float) Math.max(1, option.max - option.min);
        float barY = option.control.y + option.control.h * .5F - 1F;
        UIRender.roundRect(option.control.x, barY, option.control.w, 2F, 1F,
                Theme.buttonFill, Theme.buttonFill, 1F);
        UIRender.roundRect(option.control.x, barY, option.control.w * fraction, 2F, 1F,
                Theme.accent, Theme.accent, 1F);
        float thumbX = option.control.x + option.control.w * fraction;
        UIRender.circle(thumbX, barY + 1F, hot || sliderOption == option ? 4F : 3F, Theme.accent);
        String text = sliderText(option, value);
        UIRender.text(fr(), text, option.control.x + option.control.w - fr().getStringWidth(text) * .55F,
                option.control.y - 6F, Theme.textPrimary, .55F, false);
    }

    private String sliderText(Option option, int value) {
        if ("alert-duration".equals(option.id)) return value + " ms";
        if ("map-head-size".equals(option.id)) return value + " px";
        return value + "%";
    }

    private void drawDungeonMapPreview() {
        int size = mapSize();
        int left = mapLeft(size);
        int top = mapTop(size);
        UIRender.beginFrame();
        try {
            int background = AppleSettings.dungeonMapBackground
                    ? withPercentAlpha(AppleSettings.effectiveDungeonMapBackgroundColor(),
                    AppleSettings.effectiveDungeonMapBackgroundOpacity()) : UIRender.withAlpha(Theme.card, .78F);
            // There is deliberately no decorative frame or border around the live-map equivalent.
            UIRender.rect(left, top, size, size, background);
            float cell = size / 4F;
            int roomA = UIRender.mix(Theme.windowBottom, Theme.accent, .32F);
            int roomB = UIRender.mix(Theme.card, Theme.textSecondary, .16F);
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    float x = left + cell * (col + .28F);
                    float y = top + cell * (row + .28F);
                    UIRender.rect(x, y, cell * .64F, cell * .64F, (row + col) % 2 == 0 ? roomA : roomB);
                }
            }
            UIRender.line(left + cell, top + cell * 1.5F, left + cell * 3F, top + cell * 1.5F, Math.max(1F, cell * .12F),
                    UIRender.withAlpha(Theme.textSecondary, .55F));
            UIRender.line(left + cell * 2F, top + cell * .5F, left + cell * 2F, top + cell * 2.5F, Math.max(1F, cell * .12F),
                    UIRender.withAlpha(Theme.textSecondary, .55F));
            if (AppleSettings.dungeonMapShowPuzzleName && size >= 58) {
                UIRender.text(fr(), "Puzzle", left + cell * 1.22F, top + cell * .83F,
                        AppleSettings.effectiveDungeonMapRoomNameColor(), .55F, true);
            }
            if (AppleSettings.dungeonMapShowSecrets && size >= 48) {
                UIRender.text(fr(), "Secrets: 5", left + 3F, top + size - 10F,
                        AppleSettings.effectiveDungeonMapSecretsColor(), .55F, true);
            }
            if (AppleSettings.dungeonMapPlayerHeads) {
                float markerX = left + size * .58F;
                float markerY = top + size * .56F;
                float radius = Math.max(2F, AppleSettings.effectiveDungeonMapPlayerHeadSize() * size / 128F);
                UIRender.circle(markerX, markerY, radius, Theme.accent);
                UIRender.circle(left + size * .33F, top + size * .38F, radius, Theme.STATUS_SUCCESS);
                if (AppleSettings.dungeonMapShowPlayerNames && size >= 72) {
                    UIRender.text(fr(), "You", markerX + radius + 2F, markerY - 3F, Theme.textPrimary, .48F, true);
                    UIRender.text(fr(), "Mage", left + size * .33F + radius + 2F, top + size * .38F - 3F,
                            Theme.textPrimary, .48F, true);
                }
            }
            if (size >= 60) UIRender.text(fr(), "Zoom " + AppleSettings.effectiveDungeonMapZoom() + "%",
                    left + 4F, top + 4F, Theme.textPrimary, .5F, true);
        } finally {
            UIRender.endFrame();
        }
    }

    private void drawSelection() {
        int left = targetLeft(selected);
        int top = targetTop(selected);
        int targetWidth = targetWidth(selected);
        int targetHeight = targetHeight(selected);
        UIRender.roundRectRing(left - 3F, top - 3F, targetWidth + 6F, targetHeight + 6F, 3F, 1F, Theme.accent);
        UIRender.rect(left + targetWidth - HANDLE_SIZE * .5F, top + targetHeight - HANDLE_SIZE * .5F,
                HANDLE_SIZE, HANDLE_SIZE, Theme.accent);
    }

    private void drawGuides() {
        if (guides == 0) return;
        if ((guides & HudPlacement.GUIDE_CENTER_X) != 0) UIRender.rect(width * .5F - .5F, 0F, 1F, height, Theme.accent);
        if ((guides & HudPlacement.GUIDE_CENTER_Y) != 0) UIRender.rect(0F, height * .5F - .5F, width, 1F, Theme.accent);
        if ((guides & HudPlacement.GUIDE_LEFT) != 0) UIRender.rect(0F, 0F, 1F, height, Theme.accent);
        if ((guides & HudPlacement.GUIDE_RIGHT) != 0) UIRender.rect(width - 1F, 0F, 1F, height, Theme.accent);
        if ((guides & HudPlacement.GUIDE_TOP) != 0) UIRender.rect(0F, 0F, width, 1F, Theme.accent);
        if ((guides & HudPlacement.GUIDE_BOTTOM) != 0) UIRender.rect(0F, height - 1F, width, 1F, Theme.accent);
    }

    private void drawSolidPanel(GuiLayoutToolkit.Rect panel) {
        UIRender.roundRectBorder(panel.x, panel.y, panel.w, panel.h, 5F, Theme.ITEM_ANIMATION_PANEL,
                Theme.ITEM_ANIMATION_PANEL, Theme.ITEM_ANIMATION_PANEL_BORDER);
    }

    private void drawButton(float x, float y, float w, float h, String label, boolean active, int mouseX, int mouseY) {
        boolean hot = inside(mouseX, mouseY, x, y, w, h);
        int fill = active ? Theme.accent : Theme.buttonFill;
        int text = active ? Theme.windowTop : (hot ? Theme.textPrimary : Theme.textSecondary);
        UIRender.roundRectBorder(x, y, w, h, Math.min(5F, h * .5F), fill, fill,
                active ? Theme.accent : Theme.border);
        float scale = .62F;
        float textW = fr().getStringWidth(label) * scale;
        UIRender.text(fr(), label, x + Math.max(3F, (w - textW) * .5F), y + Math.max(3F, (h - 8F) * .5F),
                text, scale, false);
    }

    @Override
    public void handleMouseInput() throws IOException {
        int wheel = Mouse.getEventDWheel();
        super.handleMouseInput();
        int mouseX = Mouse.getEventX() * width / Math.max(1, Minecraft.getMinecraft().displayWidth);
        int mouseY = height - Mouse.getEventY() * height / Math.max(1, Minecraft.getMinecraft().displayHeight) - 1;
        if (wheel != 0 && layout != null && layout.inspector.contains(mouseX, mouseY)) {
            optionScroll = clamp(optionScroll - Math.signum(wheel) * 38F, 0F, maxOptionScroll);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        if (button != 0) return;
        updateLayout();
        if (headerClick(mouseX, mouseY)) return;
        for (int i = 0; i < Target.values().length; i++) {
            if (pickerRow(i).contains(mouseX, mouseY)) {
                select(Target.values()[i]);
                return;
            }
        }
        Option option = optionAt(mouseX, mouseY);
        if (option != null) {
            if (option.type == ACTION) runAction(option.id);
            else if (option.type == TOGGLE) {
                setBool(option.id, !boolValue(option.id));
                changed();
            } else if (option.type == CHOICE) {
                cycleChoice(option.id);
                changed();
            } else {
                sliderOption = option;
                draggingSlider = true;
                setSliderFromMouse(option, mouseX);
            }
            return;
        }
        Target target = targetAt(mouseX, mouseY);
        if (target == null) return;
        select(target);
        dragTarget = target;
        int left = targetLeft(target);
        int top = targetTop(target);
        int targetWidth = targetWidth(target);
        int targetHeight = targetHeight(target);
        if (inside(mouseX, mouseY, left + targetWidth - HANDLE_SIZE * 1.5F,
                top + targetHeight - HANDLE_SIZE * 1.5F, HANDLE_SIZE * 2F, HANDLE_SIZE * 2F)) {
            resizing = true;
            resizeStartScale = target == Target.MAP ? AppleSettings.effectiveDungeonMapScale() / 100F
                    : target.element.scale;
            resizeStartSize = Math.max(1, Math.max(targetWidth, targetHeight));
            resizeStartX = mouseX;
            resizeStartY = mouseY;
        } else {
            dragging = true;
            grabX = mouseX - left;
            grabY = mouseY - top;
        }
        guides = 0;
    }

    private boolean headerClick(int mouseX, int mouseY) {
        float doneW = layout.compact ? 40F : 50F;
        float allW = layout.compact ? 40F : 68F;
        float selectedW = layout.compact ? 46F : 92F;
        float x = width - 5F - doneW;
        if (inside(mouseX, mouseY, x, 4F, doneW, 20F)) {
            close();
            return true;
        }
        x -= 4F + allW;
        if (inside(mouseX, mouseY, x, 4F, allW, 20F)) {
            resetAll();
            return true;
        }
        x -= 4F + selectedW;
        if (inside(mouseX, mouseY, x, 4F, selectedW, 20F)) {
            resetSelected();
            return true;
        }
        return false;
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long timeSinceLastClick) {
        if (draggingSlider && sliderOption != null) {
            setSliderFromMouse(sliderOption, mouseX);
            return;
        }
        if (dragTarget == null) return;
        if (resizing) {
            resizeTarget(mouseX, mouseY);
            return;
        }
        if (dragging) moveTarget(mouseX, mouseY);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int button) {
        dragging = false;
        resizing = false;
        draggingSlider = false;
        sliderOption = null;
        dragTarget = null;
        guides = 0;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) close();
    }

    private Option optionAt(int mouseX, int mouseY) {
        if (layout == null || !layout.inspector.contains(mouseX, mouseY)) return null;
        for (Option option : options) if (option.control.contains(mouseX, mouseY)) return option;
        return null;
    }

    private Target targetAt(int mouseX, int mouseY) {
        // Prefer the current target at overlaps; an explicit picker remains available for every other target.
        if (targetContains(selected, mouseX, mouseY)) return selected;
        for (Target target : Target.values()) if (target != selected && targetContains(target, mouseX, mouseY)) return target;
        return null;
    }

    private boolean targetContains(Target target, int mouseX, int mouseY) {
        int targetWidth = targetWidth(target);
        int targetHeight = targetHeight(target);
        float padX = Math.max(4F, (TOUCH_MIN - targetWidth) * .5F);
        float padY = Math.max(4F, (TOUCH_MIN - targetHeight) * .5F);
        return inside(mouseX, mouseY, targetLeft(target) - padX, targetTop(target) - padY,
                targetWidth + padX * 2F, targetHeight + padY * 2F);
    }

    private void moveTarget(int mouseX, int mouseY) {
        if (dragTarget == Target.MAP) {
            int size = mapSize();
            guides = AppleSettings.setDungeonMapPosition(width, height, size, Math.round(mouseX - grabX),
                    Math.round(mouseY - grabY), true, (int) SNAP);
        } else {
            HudElement element = dragTarget.element;
            guides = element.place.setFromAbsolute(width, height, element.pixelWidth(), element.pixelHeight(),
                    Math.round(mouseX - grabX), Math.round(mouseY - grabY), true, (int) SNAP);
        }
        changed();
    }

    private void resizeTarget(int mouseX, int mouseY) {
        float delta = Math.max(mouseX - resizeStartX, mouseY - resizeStartY);
        float factor = Math.max(.5F, Math.min(2F, (resizeStartSize + delta) / Math.max(1F, resizeStartSize)));
        if (dragTarget == Target.MAP) {
            AppleSettings.dungeonMapScaleOn = true;
            AppleSettings.dungeonMapScale = clamp(Math.round(resizeStartScale * factor * 100F),
                    AppleSettings.DUNGEON_MAP_SCALE_MIN, AppleSettings.DUNGEON_MAP_SCALE_MAX);
        } else {
            dragTarget.element.scale = clamp(resizeStartScale * factor, HudElement.MIN_SCALE, HudElement.MAX_SCALE);
            dragTarget.element.scaleOn = true;
        }
        changed();
    }

    private int targetLeft(Target target) {
        return target == Target.MAP ? mapLeft(mapSize()) : target.element.leftFor(width);
    }

    private int targetTop(Target target) {
        return target == Target.MAP ? mapTop(mapSize()) : target.element.topFor(height);
    }

    private int targetWidth(Target target) {
        return target == Target.MAP ? mapSize() : target.element.pixelWidth();
    }

    private int targetHeight(Target target) {
        return target == Target.MAP ? mapSize() : target.element.pixelHeight();
    }

    private int mapSize() {
        return AppleSettings.dungeonMapPixelSize(width, height);
    }

    private int mapLeft(int size) {
        return AppleSettings.dungeonMapLeft(width, height, size);
    }

    private int mapTop(int size) {
        return AppleSettings.dungeonMapTop(width, height, size);
    }

    private void setSliderFromMouse(Option option, int mouseX) {
        float fraction = clamp((mouseX - option.control.x) / Math.max(1F, option.control.w), 0F, 1F);
        int raw = option.min + Math.round((option.max - option.min) * fraction);
        int value = option.min + Math.round((raw - option.min) / (float) option.step) * option.step;
        setInt(option.id, clamp(value, option.min, option.max));
        changed();
    }

    private boolean boolValue(String id) {
        if ("score-enabled".equals(id)) return HudManager.DUNGEON_SCORE.enabled;
        if ("score-line".equals(id)) return AppleSettings.dungeonScoreShowScore;
        if ("score-crypts".equals(id)) return AppleSettings.dungeonScoreShowCrypts;
        if ("score-deaths".equals(id)) return AppleSettings.dungeonScoreShowDeaths;
        if ("score-time".equals(id)) return AppleSettings.dungeonScoreShowRunTime;
        if ("alert-enabled".equals(id)) return HudManager.ROOM_CLEAR_ALERT.enabled;
        if ("alert-sound".equals(id)) return AppleSettings.dungeonRoomClearSound;
        if ("map-enabled".equals(id)) return AppleSettings.dungeonMap;
        if ("map-scale-on".equals(id)) return AppleSettings.dungeonMapScaleOn;
        if ("map-rotate".equals(id)) return AppleSettings.dungeonMapRotate;
        if ("map-names".equals(id)) return AppleSettings.dungeonMapShowPlayerNames;
        if ("map-heads".equals(id)) return AppleSettings.dungeonMapPlayerHeads;
        if ("map-room-names".equals(id)) return AppleSettings.dungeonMapShowPuzzleName;
        if ("map-secrets".equals(id)) return AppleSettings.dungeonMapShowSecrets;
        if ("map-background".equals(id)) return AppleSettings.dungeonMapBackground;
        if ("map-announce".equals(id)) return AppleSettings.partyPuzzleAnnounce;
        return false;
    }

    private void setBool(String id, boolean value) {
        if ("score-enabled".equals(id)) HudManager.DUNGEON_SCORE.enabled = value;
        else if ("score-line".equals(id)) AppleSettings.dungeonScoreShowScore = value;
        else if ("score-crypts".equals(id)) AppleSettings.dungeonScoreShowCrypts = value;
        else if ("score-deaths".equals(id)) AppleSettings.dungeonScoreShowDeaths = value;
        else if ("score-time".equals(id)) AppleSettings.dungeonScoreShowRunTime = value;
        else if ("alert-enabled".equals(id)) HudManager.ROOM_CLEAR_ALERT.enabled = value;
        else if ("alert-sound".equals(id)) AppleSettings.dungeonRoomClearSound = value;
        else if ("map-enabled".equals(id)) AppleSettings.dungeonMap = value;
        else if ("map-scale-on".equals(id)) AppleSettings.dungeonMapScaleOn = value;
        else if ("map-rotate".equals(id)) AppleSettings.dungeonMapRotate = value;
        else if ("map-names".equals(id)) AppleSettings.dungeonMapShowPlayerNames = value;
        else if ("map-heads".equals(id)) AppleSettings.dungeonMapPlayerHeads = value;
        else if ("map-room-names".equals(id)) AppleSettings.dungeonMapShowPuzzleName = value;
        else if ("map-secrets".equals(id)) AppleSettings.dungeonMapShowSecrets = value;
        else if ("map-background".equals(id)) AppleSettings.dungeonMapBackground = value;
        else if ("map-announce".equals(id)) AppleSettings.partyPuzzleAnnounce = value;
    }

    private int intValue(String id) {
        if ("element-scale".equals(id)) return Math.round(selected.element.scale * 100F);
        if ("alert-duration".equals(id)) return AppleSettings.effectiveDungeonRoomClearDurationMillis();
        if ("map-scale".equals(id)) return AppleSettings.effectiveDungeonMapScale();
        if ("map-zoom".equals(id)) return AppleSettings.effectiveDungeonMapZoom();
        if ("map-head-size".equals(id)) return AppleSettings.effectiveDungeonMapPlayerHeadSize();
        if ("map-background-opacity".equals(id)) return AppleSettings.effectiveDungeonMapBackgroundOpacity();
        return 0;
    }

    private void setInt(String id, int value) {
        if ("element-scale".equals(id) && selected.element != null) {
            selected.element.scale = clamp(value / 100F, HudElement.MIN_SCALE, HudElement.MAX_SCALE);
            selected.element.scaleOn = true;
        } else if ("alert-duration".equals(id)) AppleSettings.dungeonRoomClearDurationMillis = value;
        else if ("map-scale".equals(id)) {
            AppleSettings.dungeonMapScaleOn = true;
            AppleSettings.dungeonMapScale = value;
        } else if ("map-zoom".equals(id)) AppleSettings.dungeonMapZoom = value;
        else if ("map-head-size".equals(id)) AppleSettings.dungeonMapPlayerHeadSize = value;
        else if ("map-background-opacity".equals(id)) AppleSettings.dungeonMapBackgroundOpacity = value;
    }

    private String choiceName(String id) {
        if ("map-background-color".equals(id)) {
            return AppleSettings.DUNGEON_MAP_BACKGROUND_COLOR_NAMES[clamp(AppleSettings.dungeonMapBackgroundColor,
                    0, AppleSettings.DUNGEON_MAP_BACKGROUND_COLOR_NAMES.length - 1)];
        }
        int choice = "map-room-color".equals(id) ? AppleSettings.dungeonMapRoomNameColor
                : AppleSettings.dungeonMapSecretsColor;
        return AppleSettings.DUNGEON_MAP_LABEL_COLOR_NAMES[clamp(choice, 0,
                AppleSettings.DUNGEON_MAP_LABEL_COLOR_NAMES.length - 1)];
    }

    private void cycleChoice(String id) {
        if ("map-background-color".equals(id)) {
            AppleSettings.dungeonMapBackgroundColor = (AppleSettings.dungeonMapBackgroundColor + 1)
                    % AppleSettings.DUNGEON_MAP_BACKGROUND_COLOR_NAMES.length;
        } else if ("map-room-color".equals(id)) {
            AppleSettings.dungeonMapRoomNameColor = (AppleSettings.dungeonMapRoomNameColor + 1)
                    % AppleSettings.DUNGEON_MAP_LABEL_COLOR_NAMES.length;
        } else if ("map-secrets-color".equals(id)) {
            AppleSettings.dungeonMapSecretsColor = (AppleSettings.dungeonMapSecretsColor + 1)
                    % AppleSettings.DUNGEON_MAP_LABEL_COLOR_NAMES.length;
        }
    }

    private void runAction(String id) {
        if ("test-alert".equals(id)) {
            RoomClearAlertController.triggerTestAlert();
            select(Target.ALERT);
        } else if ("reset-map-position".equals(id)) {
            AppleSettings.resetDungeonMapPosition();
            changed();
        } else if ("reset-target".equals(id)) {
            resetSelected();
        }
    }

    private void resetSelected() {
        if (selected == Target.MAP) resetMap();
        else selected.element.resetAll();
        rebuildOptions();
        changed();
    }

    private void resetAll() {
        HudManager.DUNGEON_SCORE.resetAll();
        HudManager.ROOM_CLEAR_ALERT.resetAll();
        resetMap();
        rebuildOptions();
        changed();
    }

    private void resetMap() {
        AppleSettings.dungeonMap = true;
        AppleSettings.dungeonMapRotate = true;
        AppleSettings.dungeonMapShowPlayerNames = true;
        AppleSettings.dungeonMapPlayerHeads = true;
        AppleSettings.dungeonMapPlayerHeadSize = 8;
        AppleSettings.dungeonMapShowPuzzleName = true;
        AppleSettings.dungeonMapShowSecrets = true;
        AppleSettings.dungeonMapBackground = false;
        AppleSettings.dungeonMapBackgroundOpacity = 48;
        AppleSettings.dungeonMapBackgroundColor = 0;
        AppleSettings.dungeonMapRoomNameColor = 0;
        AppleSettings.dungeonMapSecretsColor = 1;
        AppleSettings.partyPuzzleAnnounce = false;
        AppleSettings.dungeonMapScaleOn = true;
        AppleSettings.dungeonMapScale = 100;
        AppleSettings.dungeonMapZoom = 100;
        AppleSettings.resetDungeonMapPosition();
    }

    private void close() {
        persist();
        Minecraft.getMinecraft().displayGuiScreen(parent);
    }

    private static int withPercentAlpha(int color, int percent) {
        int alpha = Math.max(0, Math.min(255, Math.round(percent * 2.55F)));
        return (alpha << 24) | (color & Theme.RGB_MASK);
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
