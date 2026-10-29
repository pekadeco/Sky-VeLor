package com.arena.pekaclient.gui;

import com.arena.pekaclient.assist.dungeon.RoomClearAlertController;
import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.hud.HudElement;
import com.arena.pekaclient.hud.HudManager;
import com.arena.pekaclient.hud.HudPlacement;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import com.arena.pekaclient.widget.AppleButton;
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
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Direct-manipulation editor for dungeon-only overlay elements.
 *
 * <p>It keeps the game visible beneath a light dim layer and renders the actual Score/Stats, Room Alert and Dungeon
 * Map samples on the same full-screen canvas where they are used. Select an element directly, drag it, or pull its
 * lower-right handle; the one compact options dock moves away from that selection instead of reserving a permanent
 * obstructive side panel.</p>
 */
public final class DungeonHudEditorScreen extends AppleScreen {
    private static final float SNAP = 4F;
    private static final float TOUCH_MIN = 28F;
    private static final float HANDLE_SIZE = 10F;
    private static final float OPTION_ROW_H = 27F;
    private static final float GROUP_ROW_H = 17F;
    private static final float PANEL_HEADER_H = 25F;

    private static final int HEADING = 0;
    private static final int TOGGLE = 1;
    private static final int SLIDER = 2;
    private static final int CHOICE = 3;
    private static final int ACTION = 4;

    private enum Target {
        SCORE("Score", HudManager.DUNGEON_SCORE),
        ALERT("Alert", HudManager.ROOM_CLEAR_ALERT),
        MAP("Map", null);

        final String chip;
        final HudElement element;

        Target(String chip, HudElement element) {
            this.chip = chip;
            this.element = element;
        }

        String panelTitle() {
            if (this == SCORE) return "Score / Stats";
            if (this == ALERT) return "Room Alert";
            return "Dungeon Map";
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
        Widget widget;

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
    private final GuiLayoutToolkit.Rect selectedBounds = new GuiLayoutToolkit.Rect();
    private final GuiLayoutToolkit.Rect[] targetChips = {
            new GuiLayoutToolkit.Rect(), new GuiLayoutToolkit.Rect(), new GuiLayoutToolkit.Rect()
    };
    private final GuiLayoutToolkit.Rect optionsChip = new GuiLayoutToolkit.Rect();
    private final GuiLayoutToolkit.Rect resetSelectedChip = new GuiLayoutToolkit.Rect();
    private final GuiLayoutToolkit.Rect resetAllChip = new GuiLayoutToolkit.Rect();
    private final GuiLayoutToolkit.Rect doneChip = new GuiLayoutToolkit.Rect();

    private Target selected = Target.SCORE;
    private DungeonHudEditorLayout layout;
    private float optionScroll;
    private float maxOptionScroll;
    private float guiScale = 1F;
    private boolean inspectorRequested = true;
    private boolean dragging;
    private boolean resizing;
    private Target dragTarget;
    private Widget activeWidget;
    private float grabX;
    private float grabY;
    private float resizeStartScale;
    private int resizeStartSize;
    private int resizeStartX;
    private int resizeStartY;
    private int guides;
    private boolean dirty;
    private long lastDrawNanos;

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
        lastDrawNanos = System.nanoTime();
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
        if (target == null) return;
        boolean changedTarget = target != selected;
        selected = target;
        inspectorRequested = true;
        if (changedTarget) {
            optionScroll = 0F;
            rebuildOptions();
        }
    }

    private void rebuildOptions() {
        options.clear();
        if (selected == Target.SCORE) {
            heading("Display");
            toggle("score-enabled", "Show HUD");
            toggle("score-line", "Score and rank");
            toggle("score-crypts", "Crypts");
            toggle("score-deaths", "Deaths");
            toggle("score-time", "Run time");
            heading("Placement");
            slider("element-scale", "Size", 50, 200, 5);
        } else if (selected == Target.ALERT) {
            heading("Display");
            toggle("alert-enabled", "Show alert");
            slider("element-scale", "Size", 50, 200, 5);
            heading("Alert");
            slider("alert-duration", "Duration", AppleSettings.DUNGEON_ROOM_CLEAR_DURATION_MIN_MILLIS,
                    AppleSettings.DUNGEON_ROOM_CLEAR_DURATION_MAX_MILLIS, 250);
            toggle("alert-sound", "Local sound");
            action("test-alert", "Test Alert");
        } else {
            // Keep all Map controls in the requested compact semantic groups rather than spreading them across a
            // giant settings wall.
            heading("Display");
            toggle("map-enabled", "Show map");
            toggle("map-scale-on", "Use map size");
            slider("map-scale", "Map size", AppleSettings.DUNGEON_MAP_SCALE_MIN,
                    AppleSettings.DUNGEON_MAP_SCALE_MAX, 5);
            slider("map-zoom", "Map zoom", AppleSettings.DUNGEON_MAP_ZOOM_MIN,
                    AppleSettings.DUNGEON_MAP_ZOOM_MAX, 5);
            toggle("map-rotate", "Rotate to facing");
            toggle("map-room-names", "Room / puzzle names");
            toggle("map-secrets", "Secrets");
            toggle("map-announce", "Party puzzle announce");
            heading("Players");
            toggle("map-names", "Player names");
            toggle("map-heads", "Player heads");
            slider("map-head-size", "Head size", AppleSettings.DUNGEON_MAP_PLAYER_HEAD_SIZE_MIN,
                    AppleSettings.DUNGEON_MAP_PLAYER_HEAD_SIZE_MAX, 1);
            heading("Colors");
            choice("map-room-color", "Room / puzzle text");
            choice("map-secrets-color", "Secrets text");
            heading("Background");
            toggle("map-background", "Map background");
            slider("map-background-opacity", "Opacity", AppleSettings.DUNGEON_MAP_BACKGROUND_OPACITY_MIN,
                    AppleSettings.DUNGEON_MAP_BACKGROUND_OPACITY_MAX, 5);
            choice("map-background-color", "Color");
        }
    }

    private void heading(String label) {
        options.add(new Option("heading-" + options.size(), label, HEADING));
    }

    private void toggle(final String id, String label) {
        Option option = new Option(id, label, TOGGLE);
        option.widget = new AppleToggle(new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return Boolean.valueOf(boolValue(id));
            }
        }, new Consumer<Boolean>() {
            @Override
            public void accept(Boolean value) {
                setBool(id, Boolean.TRUE.equals(value));
                changed();
            }
        });
        options.add(option);
    }

    private void slider(final String id, String label, final int minimum, final int maximum, final int step) {
        Option option = new Option(id, label, SLIDER, minimum, maximum, step);
        AppleSlider slider = new AppleSlider(minimum, maximum, step, "", 92F, new IntSupplier() {
            @Override
            public int getAsInt() {
                return intValue(id);
            }
        }, new IntConsumer() {
            @Override
            public void accept(int value) {
                setInt(id, value);
                changed();
            }
        });
        slider.valueLabel(new IntFunction<String>() {
            @Override
            public String apply(int value) {
                return sliderText(id, value);
            }
        });
        option.widget = slider;
        options.add(option);
    }

    private void choice(String id, String label) {
        options.add(new Option(id, label, CHOICE));
    }

    private void action(final String id, String label) {
        Option option = new Option(id, label, ACTION);
        option.widget = new AppleButton(label, 70F, 20F, false, new Runnable() {
            @Override
            public void run() {
                runAction(id);
            }
        });
        options.add(option);
    }

    private void updateLayout() {
        layout = DungeonHudEditorLayout.create(width, height);
        guiScale = new ScaledResolution(Minecraft.getMinecraft()).getScaleFactor();
        setSelectedBounds(selectedBounds, selected);
        layout.dockInspector(selectedBounds, optionContentHeight() + PANEL_HEADER_H + 8F, inspectorRequested);
        layoutTopControls();

        if (!layout.inspectorVisible) {
            optionScroll = 0F;
            maxOptionScroll = 0F;
            return;
        }
        float viewportH = Math.max(1F, layout.inspector.h - PANEL_HEADER_H - 5F);
        maxOptionScroll = Math.max(0F, optionContentHeight() + 3F - viewportH);
        optionScroll = clamp(optionScroll, 0F, maxOptionScroll);
        float y = layout.inspector.y + PANEL_HEADER_H - optionScroll;
        for (Option option : options) {
            float rowH = option.type == HEADING ? GROUP_ROW_H : OPTION_ROW_H;
            option.row.set(layout.inspector.x + 6F, y, Math.max(1F, layout.inspector.w - 12F), rowH);
            if (option.type != HEADING) layoutOptionControl(option);
            y += rowH;
        }
    }

    private float optionContentHeight() {
        float total = 0F;
        for (Option option : options) total += option.type == HEADING ? GROUP_ROW_H : OPTION_ROW_H;
        return total;
    }

    private void layoutTopControls() {
        float h = Math.max(18F, layout.topBar.h - 4F);
        float y = layout.topBar.y + (layout.topBar.h - h) * .5F;
        boolean compact = layout.compact;
        float chipW = compact ? 31F : 48F;
        float optionsW = compact ? 27F : 52F;
        float selectedW = compact ? 24F : 78F;
        float allW = compact ? 24F : 58F;
        float doneW = compact ? 32F : 44F;
        float gap = 3F;
        float total = chipW * Target.values().length + optionsW + selectedW + allW + doneW + gap * 6F;
        // Preserve all controls at very low GUI scales without letting the strip enter the launcher reserve.
        if (total > layout.topBar.w - 4F) {
            float scale = Math.max(.55F, (layout.topBar.w - 4F) / total);
            chipW *= scale;
            optionsW *= scale;
            selectedW *= scale;
            allW *= scale;
            doneW *= scale;
            gap = Math.max(1F, gap * scale);
        }
        float x = layout.topBar.x + 2F;
        Target[] targets = Target.values();
        for (int i = 0; i < targets.length; i++) {
            targetChips[i].set(x, y, chipW, h);
            x += chipW + gap;
        }
        optionsChip.set(x, y, optionsW, h);
        x += optionsW + gap;
        resetSelectedChip.set(x, y, selectedW, h);
        x += selectedW + gap;
        resetAllChip.set(x, y, allW, h);
        x += allW + gap;
        doneChip.set(x, y, doneW, h);
    }

    private void layoutOptionControl(Option option) {
        float available = Math.max(1F, option.row.w);
        if (option.type == TOGGLE) {
            option.control.set(option.row.right() - 38F, option.row.y + (option.row.h - 20F) * .5F, 38F, 20F);
        } else if (option.type == SLIDER) {
            float w = Math.max(58F, Math.min(104F, available * .50F));
            option.control.set(option.row.right() - w, option.row.y + 2F, w, 22F);
        } else if (option.type == CHOICE) {
            float w = Math.max(58F, Math.min(108F, available * .52F));
            option.control.set(option.row.right() - w, option.row.y + 3F, w, 20F);
        } else {
            option.control.set(option.row.right() - Math.min(78F, available), option.row.y + 3F,
                    Math.min(78F, available), 20F);
        }
        if (option.widget != null) {
            option.widget.x = option.control.x;
            option.widget.y = option.control.y;
            option.widget.w = option.control.w;
            option.widget.h = option.control.h;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        updateLayout();
        float delta = frameSeconds();
        UIRender.beginFrame();
        try {
            // Do not call drawDefaultBackground: the lightly dimmed in-world canvas is the editor itself.
            UIRender.rect(0F, 0F, width, height, Theme.HUD_EDITOR_DIM);
        } finally {
            UIRender.endFrame();
        }

        // The real HUD classes render editor-safe samples even when no world/sidebar values exist.
        HudManager.renderDungeonEditor(new ScaledResolution(Minecraft.getMinecraft()), mouseX, mouseY);
        drawDungeonMapPreview();

        UIRender.beginFrame();
        try {
            drawSelection();
            drawGuides();
            drawTopControls(mouseX, mouseY);
            drawInspector(mouseX, mouseY, delta);
        } finally {
            UIRender.endFrame();
        }
    }

    private float frameSeconds() {
        long now = System.nanoTime();
        float seconds = lastDrawNanos == 0L ? 1F / 60F : (now - lastDrawNanos) / 1000000000F;
        lastDrawNanos = now;
        return clamp(seconds, 0F, .10F);
    }

    private void drawTopControls(int mouseX, int mouseY) {
        int fill = UIRender.withAlpha(Theme.popup, .88F);
        UIRender.roundRectBorder(layout.topBar.x, layout.topBar.y, layout.topBar.w, layout.topBar.h,
                Math.min(7F, layout.topBar.h * .5F), fill, fill, Theme.border);
        Target[] targets = Target.values();
        for (int i = 0; i < targets.length; i++) {
            drawChip(targetChips[i], targets[i].chip, targets[i] == selected, false, mouseX, mouseY);
        }
        drawChip(optionsChip, layout.compact ? "Opt" : "Options", inspectorRequested, false, mouseX, mouseY);
        drawChip(resetSelectedChip, layout.compact ? "Sel" : "Reset selected", false, false, mouseX, mouseY);
        drawChip(resetAllChip, layout.compact ? "All" : "Reset all", false, false, mouseX, mouseY);
        drawChip(doneChip, "Done", false, true, mouseX, mouseY);
    }

    private void drawChip(GuiLayoutToolkit.Rect rect, String label, boolean active, boolean primary,
                          int mouseX, int mouseY) {
        boolean hot = rect.contains(mouseX, mouseY);
        int fill = primary || active ? Theme.accent : Theme.buttonFill;
        if (hot && !(primary || active)) fill = Theme.buttonHover;
        UIRender.roundRectBorder(rect.x, rect.y, rect.w, rect.h, Math.min(5F, rect.h * .5F), fill, fill,
                primary || active ? Theme.accent : Theme.border);
        float scale = layout.compact ? .50F : .58F;
        String shown = fit(fr(), label, Math.max(1F, rect.w / scale - 6F));
        float tx = rect.x + Math.max(2F, (rect.w - fr().getStringWidth(shown) * scale) * .5F);
        float ty = rect.y + Math.max(2F, (rect.h - 8F * scale) * .5F);
        UIRender.text(fr(), shown, tx, ty, primary || active ? Theme.OPAQUE_WHITE : Theme.textPrimary, scale, false);
    }

    private void drawInspector(int mouseX, int mouseY, float delta) {
        if (!layout.inspectorVisible) return;
        int panel = UIRender.withAlpha(Theme.popup, .94F);
        UIRender.roundRectBorder(layout.inspector.x, layout.inspector.y, layout.inspector.w, layout.inspector.h,
                6F, panel, panel, Theme.border);
        String title = selected.panelTitle() + " options";
        UIRender.text(fr(), fit(fr(), title, Math.max(1F, layout.inspector.w - 43F)),
                layout.inspector.x + 7F, layout.inspector.y + 8F, Theme.textPrimary, .67F, false);
        drawChip(new GuiLayoutToolkit.Rect().set(layout.inspector.right() - 34F, layout.inspector.y + 4F, 28F, 17F),
                "Hide", false, false, mouseX, mouseY);
        UIRender.line(layout.inspector.x + 5F, layout.inspector.y + PANEL_HEADER_H - 1F,
                layout.inspector.right() - 5F, layout.inspector.y + PANEL_HEADER_H - 1F, 1F, Theme.divider);

        float clipY = layout.inspector.y + PANEL_HEADER_H;
        float clipH = Math.max(1F, layout.inspector.h - PANEL_HEADER_H - 4F);
        UIRender.scissorBegin(layout.inspector.x + 2F, clipY, Math.max(1F, layout.inspector.w - 4F), clipH, guiScale);
        try {
            for (Option option : options) drawOption(option, mouseX, mouseY, delta);
        } finally {
            UIRender.scissorEnd();
        }
        if (maxOptionScroll > 0F) {
            float thumbH = Math.max(15F, clipH * clipH / (clipH + maxOptionScroll));
            float travel = Math.max(0F, clipH - thumbH);
            float thumbY = clipY + travel * optionScroll / maxOptionScroll;
            UIRender.roundRect(layout.inspector.right() - 4F, thumbY, 2F, thumbH, 1F,
                    Theme.textTertiary, Theme.textTertiary, 1F);
        }
    }

    private void drawOption(Option option, int mouseX, int mouseY, float delta) {
        if (option.type == HEADING) {
            UIRender.text(fr(), option.label.toUpperCase(java.util.Locale.ROOT), option.row.x,
                    option.row.y + 5F, Theme.textTertiary, .52F, false);
            UIRender.line(option.row.x + 1F, option.row.bottom() - 2F, option.row.right(), option.row.bottom() - 2F,
                    1F, Theme.divider);
            return;
        }
        float labelWidth = Math.max(1F, option.control.x - option.row.x - 5F);
        UIRender.text(fr(), fit(fr(), option.label, labelWidth / .60F), option.row.x, option.row.y + 9F,
                Theme.textSecondary, .60F, false);
        if (option.type == CHOICE) {
            drawChoice(option, mouseX, mouseY);
            return;
        }
        if (option.widget != null) {
            option.widget.update(delta);
            option.widget.draw(mouseX, mouseY);
        }
    }

    private void drawChoice(Option option, int mouseX, int mouseY) {
        boolean hot = option.control.contains(mouseX, mouseY);
        int fill = hot ? Theme.buttonHover : Theme.buttonFill;
        UIRender.roundRectBorder(option.control.x, option.control.y, option.control.w, option.control.h,
                4F, fill, fill, Theme.border);
        String name = fit(fr(), choiceName(option.id), Math.max(1F, option.control.w / .55F - 15F));
        UIRender.text(fr(), name, option.control.x + 4F, option.control.y + 6F, Theme.textPrimary, .55F, false);
        UIRender.text(fr(), ">", option.control.right() - 8F, option.control.y + 6F, Theme.textSecondary, .55F, false);
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
            UIRender.line(left + cell, top + cell * 1.5F, left + cell * 3F, top + cell * 1.5F,
                    Math.max(1F, cell * .12F), UIRender.withAlpha(Theme.textSecondary, .55F));
            UIRender.line(left + cell * 2F, top + cell * .5F, left + cell * 2F, top + cell * 2.5F,
                    Math.max(1F, cell * .12F), UIRender.withAlpha(Theme.textSecondary, .55F));
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
        setSelectedBounds(selectedBounds, selected);
        UIRender.roundRectRing(selectedBounds.x - 3F, selectedBounds.y - 3F, selectedBounds.w + 6F,
                selectedBounds.h + 6F, 3F, 1F, Theme.accent);
        UIRender.rect(selectedBounds.right() - HANDLE_SIZE * .5F, selectedBounds.bottom() - HANDLE_SIZE * .5F,
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

    @Override
    public void handleMouseInput() throws IOException {
        int wheel = Mouse.getEventDWheel();
        super.handleMouseInput();
        int mouseX = Mouse.getEventX() * width / Math.max(1, Minecraft.getMinecraft().displayWidth);
        int mouseY = height - Mouse.getEventY() * height / Math.max(1, Minecraft.getMinecraft().displayHeight) - 1;
        if (wheel != 0 && layout != null && layout.inspectorVisible && layout.inspector.contains(mouseX, mouseY)) {
            optionScroll = clamp(optionScroll - Math.signum(wheel) * 34F, 0F, maxOptionScroll);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        if (button != 0) return;
        updateLayout();
        if (topClick(mouseX, mouseY)) return;
        if (layout.inspectorVisible && inside(mouseX, mouseY, layout.inspector.x, layout.inspector.y,
                layout.inspector.w, PANEL_HEADER_H)) {
            inspectorRequested = false;
            activeWidget = null;
            return;
        }
        Option option = optionAt(mouseX, mouseY);
        if (option != null) {
            if (option.type == CHOICE) {
                cycleChoice(option.id);
                changed();
            } else if (option.widget != null) {
                activeWidget = option.widget;
                activeWidget.onPress(mouseX, mouseY);
            }
            return;
        }
        Target target = targetAt(mouseX, mouseY);
        if (target == null) return;
        select(target);
        dragTarget = target;
        setSelectedBounds(selectedBounds, target);
        if (inside(mouseX, mouseY, selectedBounds.right() - HANDLE_SIZE * 1.5F,
                selectedBounds.bottom() - HANDLE_SIZE * 1.5F, HANDLE_SIZE * 2F, HANDLE_SIZE * 2F)) {
            resizing = true;
            resizeStartScale = target == Target.MAP ? AppleSettings.effectiveDungeonMapScale() / 100F
                    : target.element.scale;
            resizeStartSize = Math.max(1, Math.max(Math.round(selectedBounds.w), Math.round(selectedBounds.h)));
            resizeStartX = mouseX;
            resizeStartY = mouseY;
        } else {
            dragging = true;
            grabX = mouseX - selectedBounds.x;
            grabY = mouseY - selectedBounds.y;
        }
        guides = 0;
    }

    private boolean topClick(int mouseX, int mouseY) {
        Target[] targets = Target.values();
        for (int i = 0; i < targets.length; i++) {
            if (targetChips[i].contains(mouseX, mouseY)) {
                select(targets[i]);
                return true;
            }
        }
        if (optionsChip.contains(mouseX, mouseY)) {
            inspectorRequested = !inspectorRequested;
            return true;
        }
        if (resetSelectedChip.contains(mouseX, mouseY)) {
            resetSelected();
            return true;
        }
        if (resetAllChip.contains(mouseX, mouseY)) {
            resetAll();
            return true;
        }
        if (doneChip.contains(mouseX, mouseY)) {
            close();
            return true;
        }
        return false;
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long timeSinceLastClick) {
        if (activeWidget != null) {
            activeWidget.onDrag(mouseX, mouseY);
            changed();
            return;
        }
        if (dragTarget == null) return;
        if (resizing) resizeTarget(mouseX, mouseY);
        else if (dragging) moveTarget(mouseX, mouseY);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int button) {
        if (activeWidget != null) {
            activeWidget.onRelease(mouseX, mouseY, button == 0);
            activeWidget = null;
        }
        dragging = false;
        resizing = false;
        dragTarget = null;
        guides = 0;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) close();
    }

    private Option optionAt(int mouseX, int mouseY) {
        if (layout == null || !layout.inspectorVisible || !layout.inspector.contains(mouseX, mouseY)) return null;
        for (Option option : options) {
            if (option.type != HEADING && option.control.contains(mouseX, mouseY)) return option;
        }
        return null;
    }

    private Target targetAt(int mouseX, int mouseY) {
        // Prefer the already-selected target at overlaps; slim chips still select every other target explicitly.
        if (targetContains(selected, mouseX, mouseY)) return selected;
        for (Target target : Target.values()) {
            if (target != selected && targetContains(target, mouseX, mouseY)) return target;
        }
        return null;
    }

    private boolean targetContains(Target target, int mouseX, int mouseY) {
        setSelectedBounds(selectedBounds, target);
        float padX = Math.max(4F, (TOUCH_MIN - selectedBounds.w) * .5F);
        float padY = Math.max(4F, (TOUCH_MIN - selectedBounds.h) * .5F);
        return inside(mouseX, mouseY, selectedBounds.x - padX, selectedBounds.y - padY,
                selectedBounds.w + padX * 2F, selectedBounds.h + padY * 2F);
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

    private void setSelectedBounds(GuiLayoutToolkit.Rect out, Target target) {
        int targetWidth = targetWidth(target);
        int targetHeight = targetHeight(target);
        out.set(targetLeft(target), targetTop(target), targetWidth, targetHeight);
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

    private String sliderText(String id, int value) {
        if ("alert-duration".equals(id)) return value + " ms";
        if ("map-head-size".equals(id)) return value + " px";
        return value + "%";
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
            changed();
        }
    }

    private void resetSelected() {
        if (selected == Target.MAP) resetMap();
        else if (selected == Target.SCORE) resetScore();
        else resetAlert();
        optionScroll = 0F;
        rebuildOptions();
        changed();
    }

    private void resetAll() {
        resetScore();
        resetAlert();
        resetMap();
        optionScroll = 0F;
        rebuildOptions();
        changed();
    }

    private void resetScore() {
        HudManager.DUNGEON_SCORE.resetAll();
        AppleSettings.dungeonScoreShowScore = AppleSettings.DEFAULT_DUNGEON_SCORE_SHOW_SCORE;
        AppleSettings.dungeonScoreShowCrypts = AppleSettings.DEFAULT_DUNGEON_SCORE_SHOW_CRYPTS;
        AppleSettings.dungeonScoreShowDeaths = AppleSettings.DEFAULT_DUNGEON_SCORE_SHOW_DEATHS;
        AppleSettings.dungeonScoreShowRunTime = AppleSettings.DEFAULT_DUNGEON_SCORE_SHOW_RUN_TIME;
    }

    private void resetAlert() {
        HudManager.ROOM_CLEAR_ALERT.resetAll();
        AppleSettings.dungeonRoomClearSound = AppleSettings.DEFAULT_DUNGEON_ROOM_CLEAR_SOUND;
        AppleSettings.dungeonRoomClearDurationMillis = AppleSettings.DEFAULT_DUNGEON_ROOM_CLEAR_DURATION_MILLIS;
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
