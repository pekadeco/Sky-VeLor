package com.arena.pekaclient.gui;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.feature.ItemAnimation;
import com.arena.pekaclient.hud.BalanceSource;
import com.arena.pekaclient.hud.HudElement;
import com.arena.pekaclient.hud.HudManager;
import com.arena.pekaclient.hud.HudPlacement;
import com.arena.pekaclient.render.Anim;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import com.arena.pekaclient.render.itemanimation.ItemAnimationHookStatus;
import com.arena.pekaclient.widget.AppleButton;
import com.arena.pekaclient.widget.AppleDropdown;
import com.arena.pekaclient.widget.AppleSlider;
import com.arena.pekaclient.widget.AppleToggle;
import com.arena.pekaclient.widget.SearchField;
import com.arena.pekaclient.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * HUD editor. Drag ordinary HUD elements to move them. The Dungeon Map has its own unclamped placement
 * controls, and Item Animation is a clearly labelled visual renderer configuration target. Every change
 * is saved when the editor closes.
 *
 * Works with mouse and touch (PojavLauncher): a tap is a click, a drag moves an element or scrolls
 * the card, and a swipe never activates a control.
 */
public final class HudEditorScreen extends AppleScreen {

    private static final float BAR_H = 30f;
    private static final float BAR_TOP = 8f;
    private static final float CARD_PAD = HudEditorLayout.CARD_PADDING;
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
        out[HudManager.ELEMENTS.length + 1] = ItemAnimationUiModel.buildHudEditorTargetList().get(0).getLabel();
        return out;
    }

    /** One inspector row, laid out by the shared fixed-height left-label/right-control grid. */
    private static final class Entry {
        final String label;
        final Widget control;
        final boolean fullWidth;
        final boolean stretchControl;
        final HudEditorLayout.Bounds bounds = new HudEditorLayout.Bounds();

        Entry(String label, Widget control) {
            this(label, control, label == null, control instanceof AppleSlider || control instanceof AppleDropdown
                    || control instanceof SearchField || control instanceof HorizontalDragWidget);
        }

        Entry(String label, Widget control, boolean fullWidth, boolean stretchControl) {
            this.label = label;
            this.control = control;
            this.fullWidth = fullWidth;
            this.stretchControl = stretchControl;
        }
    }

    /** A non-clickable launcher status strip. Inactive is intentionally red inside the renderer panel. */
    private static final class ItemAnimationHookWidget extends Widget {
        ItemAnimationHookWidget(float width) {
            super(width, 22f);
        }

        @Override
        public void draw(int mouseX, int mouseY) {
            boolean active = ItemAnimationHookStatus.isActive();
            int accent = active ? 0xFF30D158 : 0xFFFF453A;
            int fill = active ? 0x2830D158 : 0x38FF453A;
            UIRender.roundRectBorder(x, y, w, h, 5f, fill, fill, accent);
            String status = fit(font(), ItemAnimationHookStatus.getDisplay(), Math.max(0f, w - 14f));
            UIRender.text(font(), status, x + 7f, y + 7f,
                    active ? 0xFFD7FFE4 : 0xFFFFD7D1, 0.72f, false);
        }

        @Override
        public void onPress(int mouseX, int mouseY) {
            // Status is informational. It must not look like a toggle or hide an injection failure.
        }
    }

    /** One compact slider plus exact-number box. It is still one fixed inspector row, not two overlapping rows. */
    private final class ExactSliderWidget extends Widget implements HorizontalDragWidget {
        private final ItemAnimationValue value;
        private final AppleSlider slider;
        private final SearchField field;
        private int lastDisplayed = Integer.MIN_VALUE;

        ExactSliderWidget(ItemAnimationValue value, AppleSlider slider, SearchField field) {
            super(136f, CONTROL_H);
            this.value = value;
            this.slider = slider;
            this.field = field;
        }

        private void arrange() {
            // Keep the compact components disjoint even at the narrowest supported GUI scale.
            float fieldW = Math.min(62f, Math.max(24f, w * 0.40f));
            field.w = fieldW;
            field.h = 20f;
            field.x = x + w - fieldW;
            field.y = y + (h - field.h) * 0.5f;
            slider.w = Math.max(0f, field.x - x - 3f);
            slider.x = x;
            slider.y = y + (h - slider.h) * 0.5f;
        }

        private void syncNumberBox() {
            int raw = configuredItemAnimationValue(value);
            if (!field.isFocused() && (raw != lastDisplayed || field.isEmpty())) {
                field.setText(formatExactValue(value, raw));
                lastDisplayed = raw;
            }
        }

        @Override
        protected void updateWidget(float dt) {
            arrange();
            slider.update(dt);
            field.update(dt);
        }

        @Override
        public void draw(int mouseX, int mouseY) {
            arrange();
            syncNumberBox();
            slider.draw(mouseX, mouseY);
            field.draw(mouseX, mouseY);
        }

        @Override
        public void onPress(int mouseX, int mouseY) {
            super.onPress(mouseX, mouseY);
            arrange();
            if (pressed && slider.contains(mouseX, mouseY)) {
                slider.onPress(mouseX, mouseY);
            }
        }

        @Override
        public void onDrag(int mouseX, int mouseY) {
            slider.onDrag(mouseX, mouseY);
        }

        @Override
        public void onRelease(int mouseX, int mouseY, boolean activate) {
            slider.onRelease(mouseX, mouseY, activate && slider.contains(mouseX, mouseY));
            super.onRelease(mouseX, mouseY, activate);
        }
    }

    /** Compact accordion row used for Position, Animation, and Options. Position also exposes its small reset. */
    private final class GroupWidget extends Widget {
        private final String title;
        private final String summary;
        private final boolean expanded;
        private final Runnable toggle;
        private final Runnable reset;
        private boolean resetPressed;

        GroupWidget(String title, String summary, boolean expanded, Runnable toggle, Runnable reset) {
            super(160f, CONTROL_H);
            this.title = title;
            this.summary = summary == null ? "" : summary;
            this.expanded = expanded;
            this.toggle = toggle;
            this.reset = reset;
        }

        private float resetX() {
            return x + w - 49f;
        }

        @Override
        public void draw(int mouseX, int mouseY) {
            beginAnimatedDraw(0.012f, 0.018f, 0.008f);
            int fill = UIRender.mix(Theme.buttonFill, Theme.buttonHover, hoverAmount * 0.45f);
            float radius = Math.min(Theme.radius * 0.6f, h * 0.5f);
            UIRender.roundRect(x, y, w, h, radius, fill, fill, 0.4f);
            drawBorder(radius, Theme.border);
            String prefix = expanded ? "▾ " : "▸ ";
            float reserve = reset == null ? 8f : 56f;
            String line = prefix + title + (summary.length() == 0 ? "" : "  " + summary);
            UIRender.text(font(), fit(font(), line, Math.max(0f, w - reserve)), x + 8f,
                    y + (h - 9f) * 0.5f + 1f, Theme.textPrimary, 0.9f, false);
            if (reset != null) {
                float bx = resetX();
                boolean hot = mouseX >= bx && mouseX < bx + 41f && mouseY >= y + 4f && mouseY < y + h - 4f;
                UIRender.roundRect(bx, y + 4f, 41f, h - 8f, 7f, Theme.track, Theme.track, 0.3f);
                UIRender.text(font(), "Reset", bx + 6f, y + (h - 8f - 8f) * 0.5f + 5f,
                        hot ? Theme.accent : Theme.textSecondary, 0.75f, false);
            }
            endAnimatedDraw();
        }

        @Override
        public void onPress(int mouseX, int mouseY) {
            super.onPress(mouseX, mouseY);
            resetPressed = reset != null && mouseX >= resetX() && mouseX < resetX() + 41f;
        }

        @Override
        public void onRelease(int mouseX, int mouseY, boolean activate) {
            boolean fire = pressed && activate && contains(mouseX, mouseY);
            super.onRelease(mouseX, mouseY, activate);
            if (!fire) return;
            if (resetPressed && reset != null) {
                reset.run();
            } else if (toggle != null) {
                toggle.run();
            }
        }
    }

    private final GuiScreen parent;
    private final Anim openAnim = new Anim(0f);
    private final List<Entry> entries = new ArrayList<Entry>();
    private final List<Widget> bar = new ArrayList<Widget>();
    private final SearchField manualField = new SearchField("Enter amount");
    // Exact-value fields deliberately live in the same inspector card as their sliders. Clicking one starts a
    // fresh numeric entry, so touch users do not need cursor-selection support to replace a value exactly.
    private final SearchField itemAnimationXField = new SearchField("Type -150 to 150");
    private final SearchField itemAnimationYField = new SearchField("Type -150 to 150");
    private final SearchField itemAnimationZField = new SearchField("Type -150 to 150");
    private final SearchField itemAnimationSizeField = new SearchField("Type 30 to 200");
    private final SearchField itemAnimationSpeedField = new SearchField("Type 25 to 400");
    private final ItemAnimationUiModel.PanelState itemAnimationPanel = new ItemAnimationUiModel.PanelState();
    private final ExactSliderWidget itemAnimationXControl;
    private final ExactSliderWidget itemAnimationYControl;
    private final ExactSliderWidget itemAnimationZControl;
    private final ExactSliderWidget itemAnimationSizeControl;
    private final ExactSliderWidget itemAnimationSpeedControl;
    private final AppleDropdown chooser;
    private final AppleButton resetAllBtn;
    private final AppleButton doneBtn;
    private final ScrollState scroll = new ScrollState();
    private final Gesture gesture = new Gesture();
    private final HudEditorLayout.Metrics inspectorLayout = new HudEditorLayout.Metrics();
    /** Shared clamp authority for the editor shell; every nested inspector is then measured from this safe rect. */
    private final GuiLayoutToolkit.Rect editorPanel = new GuiLayoutToolkit.Rect();

    private enum EditorTarget { HUD_ELEMENT, DUNGEON_MAP, ITEM_ANIMATION }
    private enum ItemAnimationValue { X, Y, Z, SIZE, SPEED }

    private HudElement selected = HudManager.MINING;
    private EditorTarget target = EditorTarget.HUD_ELEMENT;
    private long lastNs;
    private int scaleKey = -1;
    private float sf = 2f;
    private int previewTextX = Integer.MIN_VALUE;
    private int previewTextY = Integer.MIN_VALUE;
    private int previewTextZ = Integer.MIN_VALUE;
    private String previewPositionText = "";

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
    private boolean draggingScrollbar;
    private float scrollbarGrabOffset;
    private boolean draggingDungeonMap;
    private float dungeonMapGrabX;
    private float dungeonMapGrabY;
    private float cardGrabX;
    private float cardGrabY;
    private static final float HEAD_H = HudEditorLayout.HEADER_HEIGHT;
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
                        selectItemAnimation();
                    }
                });
        this.resetAllBtn = new AppleButton("Reset all", 74f, 22f, false, this::resetAll);
        this.doneBtn = new AppleButton("Done", 56f, 22f, true, this::close);
        this.manualField.w = CONTROL_W;
        this.manualField.h = 22f;
        configureItemAnimationExactField(itemAnimationXField);
        configureItemAnimationExactField(itemAnimationYField);
        configureItemAnimationExactField(itemAnimationZField);
        configureItemAnimationExactField(itemAnimationSizeField);
        configureItemAnimationExactField(itemAnimationSpeedField);
        itemAnimationXControl = compactSlider(ItemAnimationValue.X, itemAnimationXField,
                new AppleSlider(ItemAnimation.MIN_OFFSET, ItemAnimation.MAX_OFFSET, 5, "", CONTROL_W,
                        () -> AppleSettings.itemAnimationX, value -> {
                            AppleSettings.itemAnimationX = ItemAnimation.clampOffset(value);
                            changed();
                        }).fineStep(1).onDoubleClick(() -> {
                            AppleSettings.itemAnimationX = AppleSettings.DEFAULT_ITEM_ANIMATION_X;
                            changed();
                        }).hideValue());
        itemAnimationYControl = compactSlider(ItemAnimationValue.Y, itemAnimationYField,
                new AppleSlider(ItemAnimation.MIN_OFFSET, ItemAnimation.MAX_OFFSET, 5, "", CONTROL_W,
                        () -> AppleSettings.itemAnimationY, value -> {
                            AppleSettings.itemAnimationY = ItemAnimation.clampOffset(value);
                            changed();
                        }).fineStep(1).onDoubleClick(() -> {
                            AppleSettings.itemAnimationY = AppleSettings.DEFAULT_ITEM_ANIMATION_Y;
                            changed();
                        }).hideValue());
        itemAnimationZControl = compactSlider(ItemAnimationValue.Z, itemAnimationZField,
                new AppleSlider(ItemAnimation.MIN_OFFSET, ItemAnimation.MAX_OFFSET, 5, "", CONTROL_W,
                        () -> AppleSettings.itemAnimationZ, value -> {
                            AppleSettings.itemAnimationZ = ItemAnimation.clampOffset(value);
                            changed();
                        }).fineStep(1).onDoubleClick(() -> {
                            AppleSettings.itemAnimationZ = AppleSettings.DEFAULT_ITEM_ANIMATION_Z;
                            changed();
                        }).hideValue());
        itemAnimationSizeControl = compactSlider(ItemAnimationValue.SIZE, itemAnimationSizeField,
                new AppleSlider(ItemAnimation.MIN_SIZE_PERCENT, ItemAnimation.MAX_SIZE_PERCENT, 5, "%", CONTROL_W,
                        AppleSettings::effectiveItemAnimationSize, value -> {
                            AppleSettings.itemAnimationSizeOn = true;
                            AppleSettings.itemAnimationSize = ItemAnimation.clampSize(value);
                            changed();
                        }).fineStep(1).onDoubleClick(() -> {
                            AppleSettings.itemAnimationSizeOn = true;
                            AppleSettings.itemAnimationSize = ItemAnimation.DEFAULT_SIZE_PERCENT;
                            changed();
                        }).hideValue());
        itemAnimationSpeedControl = compactSlider(ItemAnimationValue.SPEED, itemAnimationSpeedField,
                new AppleSlider(ItemAnimation.MIN_SPEED_PERCENT, ItemAnimation.MAX_SPEED_PERCENT, 5, "%", CONTROL_W,
                        AppleSettings::effectiveItemAnimationSpeed, value -> {
                            AppleSettings.itemAnimationSpeedOn = true;
                            AppleSettings.itemAnimationSpeed = ItemAnimation.clampSpeed(value);
                            changed();
                        }).fineStep(1).onDoubleClick(() -> {
                            AppleSettings.itemAnimationSpeedOn = true;
                            AppleSettings.itemAnimationSpeed = ItemAnimation.DEFAULT_SPEED_PERCENT;
                            changed();
                        }).hideValue());
        bar.add(doneBtn);
        bar.add(resetAllBtn);
        openAnim.to(1f);
    }

    /** Opens the renderer inspector directly instead of hiding it behind the generic target dropdown. */
    public static HudEditorScreen openItemAnimation(GuiScreen parent) {
        HudEditorScreen editor = new HudEditorScreen(parent);
        editor.target = EditorTarget.ITEM_ANIMATION;
        return editor;
    }

    /** Opens the standard editor on one ordinary HUD element, keeping its existing saved placement untouched. */
    public static HudEditorScreen openElement(GuiScreen parent, HudElement element) {
        HudEditorScreen editor = new HudEditorScreen(parent);
        if (element != null) {
            editor.selected = element;
        }
        editor.target = EditorTarget.HUD_ELEMENT;
        return editor;
    }

    /** Opens the standard editor on the Dungeon Map's existing position/options panel. */
    public static HudEditorScreen openDungeonMap(GuiScreen parent) {
        HudEditorScreen editor = new HudEditorScreen(parent);
        editor.target = EditorTarget.DUNGEON_MAP;
        return editor;
    }

    private int chooserIndex() {
        if (target == EditorTarget.DUNGEON_MAP) return HudManager.ELEMENTS.length;
        if (target == EditorTarget.ITEM_ANIMATION) return HudManager.ELEMENTS.length + 1;
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

    /** Keep the first-person render/tick path live behind the editor so slider changes are immediately visible. */
    @Override
    public boolean doesGuiPauseGame() {
        return false;
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

    private void selectItemAnimation() {
        if (target == EditorTarget.ITEM_ANIMATION) return;
        target = EditorTarget.ITEM_ANIMATION;
        resetInspector();
    }

    private void resetInspector() {
        openDropdown = null;
        manualField.setFocused(false);
        clearItemAnimationExactFields();
        scroll.reset();
        rebuild();
    }

    private void changed() {
        dirty = true;
    }

    private void configureItemAnimationExactField(SearchField field) {
        field.w = CONTROL_W;
        field.h = 22f;
    }

    private ExactSliderWidget compactSlider(ItemAnimationValue value, SearchField field, AppleSlider slider) {
        return new ExactSliderWidget(value, slider, field);
    }

    /** Rebuilds the inspector for an ordinary HUD element, the map, or the Item Animation renderer. */
    private void rebuild() {
        entries.clear();
        // Item Animation is a normal choice in this element list. A focused Item Animation editor shows its
        // controls immediately; no second in-editor launcher is needed.
        entries.add(new Entry("Choose HUD target", chooser));
        if (target == EditorTarget.DUNGEON_MAP) {
            buildDungeonMapEntries();
            return;
        }
        if (target == EditorTarget.ITEM_ANIMATION) {
            buildItemAnimationEntries();
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

    /**
     * Item Animation is deliberately an inspector-only renderer panel: it cannot be dragged like a HUD element.
     * Compact slider-and-exact-field rows stay in one fixed inspector row; Enter applies a clamped value and
     * persists it right away.
     */
    private void buildItemAnimationEntries() {
        // Item Animation is a first-person renderer target, never a draggable HUD element or combat setting.
        entries.add(new Entry(null, new ItemAnimationHookWidget(CONTROL_W)));
        entries.add(new Entry(ItemAnimationUiModel.panelControlLabel("enable", itemAnimationPanel),
                new AppleToggle(ItemAnimation::isEnabled, value -> {
                    AppleSettings.itemAnimationEnabled = value;
                    changed();
                })));
        entries.add(new Entry(ItemAnimationUiModel.panelControlLabel("scale", itemAnimationPanel),
                itemAnimationSizeControl));

        entries.add(new Entry(null, new GroupWidget(ItemAnimationUiModel.panelControlLabel("position", itemAnimationPanel),
                positionSummary(), itemAnimationPanel.isPositionExpanded(), this::togglePositionGroup,
                this::resetItemAnimationPosition)));
        if (itemAnimationPanel.isPositionExpanded()) {
            entries.add(new Entry(ItemAnimationUiModel.panelControlLabel("x", itemAnimationPanel), itemAnimationXControl));
            entries.add(new Entry(ItemAnimationUiModel.panelControlLabel("y", itemAnimationPanel), itemAnimationYControl));
            entries.add(new Entry(ItemAnimationUiModel.panelControlLabel("z", itemAnimationPanel), itemAnimationZControl));
        }

        entries.add(new Entry(null, new GroupWidget(ItemAnimationUiModel.panelControlLabel("animation", itemAnimationPanel),
                animationSummary(), itemAnimationPanel.isAnimationExpanded(), this::toggleAnimationGroup, null)));
        if (itemAnimationPanel.isAnimationExpanded()) {
            entries.add(new Entry(ItemAnimationUiModel.panelControlLabel("hit-slash-style", itemAnimationPanel),
                    new AppleDropdown(AppleSettings.ITEM_ANIMATION_HIT_SLASH_STYLES, null, CONTROL_W, CONTROL_H,
                            () -> AppleSettings.itemAnimationHitSlashStyle, value -> {
                                AppleSettings.itemAnimationHitSlashStyle = ItemAnimation.clampHitSlashStyle(value);
                                changed();
                            })));
            entries.add(new Entry(ItemAnimationUiModel.panelControlLabel("hit-slash-speed", itemAnimationPanel),
                    itemAnimationSpeedControl));
        }

        entries.add(new Entry(null, new GroupWidget(ItemAnimationUiModel.panelControlLabel("options", itemAnimationPanel),
                optionsSummary(), itemAnimationPanel.isOptionsExpanded(), this::toggleOptionsGroup, null)));
        if (itemAnimationPanel.isOptionsExpanded()) {
            entries.add(new Entry(ItemAnimationUiModel.panelControlLabel("remove-haste", itemAnimationPanel), new AppleToggle(
                    ItemAnimation::isHasteEffectRemoved, value -> {
                        AppleSettings.itemAnimationRemoveHasteEffect = value;
                        changed();
                    })));
        }
        entries.add(new Entry(ItemAnimationUiModel.panelControlLabel("preview-swing", itemAnimationPanel),
                new AppleButton("Preview", 74f, CONTROL_H, true, ItemAnimation::previewSwing)));
        entries.add(new Entry(null, new AppleButton(ItemAnimationUiModel.panelControlLabel("reset-defaults", itemAnimationPanel),
                CONTROL_W, CONTROL_H, false, () -> {
                    ItemAnimation.resetToDefaults();
                    clearItemAnimationExactFields();
                    changed();
                    AppleSettings.save();
                })));
    }

    private void togglePositionGroup() {
        itemAnimationPanel.togglePosition();
        openDropdown = null;
        rebuild();
    }

    private void toggleAnimationGroup() {
        itemAnimationPanel.toggleAnimation();
        openDropdown = null;
        rebuild();
    }

    private void toggleOptionsGroup() {
        itemAnimationPanel.toggleOptions();
        openDropdown = null;
        rebuild();
    }

    private void resetItemAnimationPosition() {
        AppleSettings.itemAnimationX = AppleSettings.DEFAULT_ITEM_ANIMATION_X;
        AppleSettings.itemAnimationY = AppleSettings.DEFAULT_ITEM_ANIMATION_Y;
        AppleSettings.itemAnimationZ = AppleSettings.DEFAULT_ITEM_ANIMATION_Z;
        clearItemAnimationPositionFields();
        changed();
    }

    private String positionSummary() {
        return "X " + formatOffset(AppleSettings.itemAnimationX) + "  Y "
                + formatOffset(AppleSettings.itemAnimationY) + "  Z " + formatOffset(AppleSettings.itemAnimationZ);
    }

    private String animationSummary() {
        int style = ItemAnimation.clampHitSlashStyle(AppleSettings.itemAnimationHitSlashStyle);
        return AppleSettings.ITEM_ANIMATION_HIT_SLASH_STYLES[style] + "  "
                + AppleSettings.effectiveItemAnimationSpeed() + "%";
    }

    private String optionsSummary() {
        return "Haste " + (AppleSettings.itemAnimationRemoveHasteEffect ? "off" : "on");
    }

    private int configuredItemAnimationValue(ItemAnimationValue value) {
        switch (value) {
            case X: return ItemAnimation.clampOffset(AppleSettings.itemAnimationX);
            case Y: return ItemAnimation.clampOffset(AppleSettings.itemAnimationY);
            case Z: return ItemAnimation.clampOffset(AppleSettings.itemAnimationZ);
            case SIZE: return ItemAnimation.clampSize(AppleSettings.effectiveItemAnimationSize());
            case SPEED: return ItemAnimation.clampSpeed(AppleSettings.effectiveItemAnimationSpeed());
            default: return 0;
        }
    }

    private static String formatOffset(int value) {
        int clamped = ItemAnimation.clampOffset(value);
        int absolute = Math.abs(clamped);
        String hundredths = absolute % 100 < 10 ? "0" + (absolute % 100) : Integer.toString(absolute % 100);
        return (clamped < 0 ? "-" : "") + (absolute / 100) + "." + hundredths;
    }

    private static String formatExactValue(ItemAnimationValue value, int raw) {
        return value == ItemAnimationValue.X || value == ItemAnimationValue.Y || value == ItemAnimationValue.Z
                ? formatOffset(raw) : Integer.toString(raw);
    }

    private String itemAnimationPreviewPositionText() {
        int x = Math.round(ItemAnimation.getX() * 100f);
        int y = Math.round(ItemAnimation.getY() * 100f);
        int z = Math.round(ItemAnimation.getZ() * 100f);
        if (x != previewTextX || y != previewTextY || z != previewTextZ) {
            previewTextX = x;
            previewTextY = y;
            previewTextZ = z;
            previewPositionText = "X " + signedOffset(x) + "  Y " + signedOffset(y) + "  Z " + signedOffset(z);
        }
        return previewPositionText;
    }

    private static String signedOffset(int value) {
        String text = formatOffset(value);
        return value > 0 ? "+" + text : text;
    }

    private void clearItemAnimationPositionFields() {
        itemAnimationXField.clear();
        itemAnimationYField.clear();
        itemAnimationZField.clear();
        itemAnimationXField.setFocused(false);
        itemAnimationYField.setFocused(false);
        itemAnimationZField.setFocused(false);
    }

    private SearchField itemAnimationField(ItemAnimationValue value) {
        switch (value) {
            case X: return itemAnimationXField;
            case Y: return itemAnimationYField;
            case Z: return itemAnimationZField;
            case SIZE: return itemAnimationSizeField;
            case SPEED: return itemAnimationSpeedField;
            default: return null;
        }
    }

    private ItemAnimationValue itemAnimationValue(SearchField field) {
        if (field == itemAnimationXField) return ItemAnimationValue.X;
        if (field == itemAnimationYField) return ItemAnimationValue.Y;
        if (field == itemAnimationZField) return ItemAnimationValue.Z;
        if (field == itemAnimationSizeField) return ItemAnimationValue.SIZE;
        if (field == itemAnimationSpeedField) return ItemAnimationValue.SPEED;
        return null;
    }

    private SearchField focusedItemAnimationField() {
        for (ItemAnimationValue value : ItemAnimationValue.values()) {
            SearchField field = itemAnimationField(value);
            if (field.isFocused()) return field;
        }
        return null;
    }

    private void clearItemAnimationExactFields() {
        for (ItemAnimationValue value : ItemAnimationValue.values()) {
            SearchField field = itemAnimationField(value);
            field.clear();
            field.setFocused(false);
        }
    }

    private void focusItemAnimationExactField(SearchField field) {
        if (field.isFocused()) {
            return;
        }
        SearchField previous = focusedItemAnimationField();
        if (previous != null) {
            applyItemAnimationExact(previous);
        }
        manualField.setFocused(false);
        field.clear();
        field.setFocused(true);
    }

    private boolean isItemAnimationValueVisible(ItemAnimationValue value) {
        if (value == ItemAnimationValue.X || value == ItemAnimationValue.Y || value == ItemAnimationValue.Z) {
            return itemAnimationPanel.isPositionExpanded();
        }
        return value != ItemAnimationValue.SPEED || itemAnimationPanel.isAnimationExpanded();
    }

    private SearchField itemAnimationExactAt(int mx, int my) {
        if (target != EditorTarget.ITEM_ANIMATION || !inCardView(mx, my)) {
            return null;
        }
        for (ItemAnimationValue value : ItemAnimationValue.values()) {
            if (!isItemAnimationValueVisible(value)) {
                continue;
            }
            SearchField field = itemAnimationField(value);
            if (field.contains(mx, my)) {
                return field;
            }
        }
        return null;
    }

    /** Applies a single exact typed Item Animation value, clamps it, and atomically saves the result. */
    private boolean applyItemAnimationExact(SearchField field) {
        ItemAnimationValue value = itemAnimationValue(field);
        if (value == null) return false;
        String typed = field.text().trim();
        if (typed.length() == 0 || "-".equals(typed)) {
            field.setFocused(false);
            return false;
        }
        final int requested;
        try {
            if ((value == ItemAnimationValue.X || value == ItemAnimationValue.Y || value == ItemAnimationValue.Z)
                    && typed.indexOf('.') >= 0) {
                double units = Double.parseDouble(typed);
                if (Double.isNaN(units) || Double.isInfinite(units)) {
                    throw new NumberFormatException("non-finite offset");
                }
                requested = Math.round((float) units * 100.0F);
            } else {
                // Keep existing saved/editor integer semantics: 50 still means +0.50 for X/Y/Z.
                requested = Integer.parseInt(typed);
            }
        } catch (NumberFormatException ignored) {
            field.clear();
            field.setFocused(false);
            return false;
        }
        int applied;
        switch (value) {
            case X:
                applied = ItemAnimation.clampOffset(requested);
                AppleSettings.itemAnimationX = applied;
                break;
            case Y:
                applied = ItemAnimation.clampOffset(requested);
                AppleSettings.itemAnimationY = applied;
                break;
            case Z:
                applied = ItemAnimation.clampOffset(requested);
                AppleSettings.itemAnimationZ = applied;
                break;
            case SIZE:
                applied = ItemAnimation.clampSize(requested);
                AppleSettings.itemAnimationSizeOn = true;
                AppleSettings.itemAnimationSize = applied;
                break;
            case SPEED:
                applied = ItemAnimation.clampSpeed(requested);
                AppleSettings.itemAnimationSpeedOn = true;
                AppleSettings.itemAnimationSpeed = applied;
                break;
            default:
                return false;
        }
        field.setText(formatExactValue(value, applied));
        field.setFocused(false);
        changed();
        AppleSettings.save();
        return true;
    }

    private void applyFocusedItemAnimationExact() {
        SearchField field = focusedItemAnimationField();
        if (field != null) {
            applyItemAnimationExact(field);
        }
    }

    private boolean acceptsItemAnimationKey(SearchField field, char typed, int keyCode) {
        if (keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_DELETE || keyCode == Keyboard.KEY_HOME) {
            return true;
        }
        if (Character.isDigit(typed) || (typed == '-' && field.text().length() == 0)) {
            return true;
        }
        ItemAnimationValue value = itemAnimationValue(field);
        return typed == '.' && (value == ItemAnimationValue.X || value == ItemAnimationValue.Y
                || value == ItemAnimationValue.Z) && field.text().indexOf('.') < 0;
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
        ItemAnimation.resetToDefaults();
        clearItemAnimationExactFields();
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
        GuiLayoutToolkit.clampPanel(editorPanel, 6f, 6f, Math.max(1f, width - 12f),
                Math.max(1f, height - 12f), width, height);
        wx = editorPanel.x;
        wy = editorPanel.y;
        ww = editorPanel.w;
        wh = editorPanel.h;

        barW = Math.max(1f, Math.min(ww - 16f, 380f));
        barX = wx + (ww - barW) * 0.5f;
        barY = wy + BAR_TOP;
        doneBtn.x = barX + barW - 8f - doneBtn.w;
        doneBtn.y = barY + (BAR_H - doneBtn.h) * 0.5f;
        resetAllBtn.x = doneBtn.x - 6f - resetAllBtn.w;
        resetAllBtn.y = barY + (BAR_H - resetAllBtn.h) * 0.5f;

        HudEditorLayout.update(inspectorLayout, wx, wy, ww, wh, barY + BAR_H + 8f, entries.size(),
                scroll.value(), cardHidden, cardDx, cardDy);
        scroll.setMax(inspectorLayout.maxScroll);
        // setMax may clamp a prior scroll position after a group collapsed, so use the final value for rows.
        HudEditorLayout.update(inspectorLayout, wx, wy, ww, wh, barY + BAR_H + 8f, entries.size(),
                scroll.value(), cardHidden, cardDx, cardDy);
        cardX = inspectorLayout.cardX;
        cardY = inspectorLayout.cardY;
        cardW = inspectorLayout.cardW;
        cardH = inspectorLayout.cardH;
        visibleTop = inspectorLayout.visibleTop;
        visibleBottom = inspectorLayout.visibleBottom;
        maxScroll = inspectorLayout.maxScroll;

        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            Widget control = entry.control;
            HudEditorLayout.row(inspectorLayout, i, entry.fullWidth, control.w, entry.stretchControl,
                    control.h, entry.bounds);
            control.x = entry.bounds.controlX;
            control.y = entry.bounds.controlY;
            control.w = entry.bounds.controlW;
            if (control instanceof ExactSliderWidget) {
                ((ExactSliderWidget) control).arrange();
            }
        }
    }

    private float hideX() {
        return cardX + cardW - CARD_PAD - 44f;
    }

    private float hideY() {
        return cardY + (HEAD_H - 16f) * 0.5f;
    }

    private boolean inScrollbar(float mx, float my) {
        return maxScroll > 0f && inside(mx, my, inspectorLayout.scrollbarX - 4f, visibleTop,
                inspectorLayout.scrollbarW + 8f, inspectorLayout.viewportH);
    }

    private void setScrollbarFromY(float my) {
        float travel = Math.max(0f, inspectorLayout.viewportH - inspectorLayout.scrollbarH);
        float thumbTop = Math.max(visibleTop, Math.min(visibleTop + travel, my - scrollbarGrabOffset));
        float fraction = travel <= 0f ? 0f : (thumbTop - visibleTop) / travel;
        scroll.setImmediate(fraction * maxScroll);
        layout();
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
                : target == EditorTarget.ITEM_ANIMATION ? "Item Animation — Renderer" : selected.name;
        title = fit(fr, title, Math.max(0f, hideX() - cardX - CARD_PAD - 8f));
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
                Theme.animations && AppleSettings.smoothScrolling, Theme.speed);
    }

    private float scrollSpeed() {
        return AppleSettings.guiScrollMultiplier();
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
        drawItemAnimationEditorPreview(mc);

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
        if (target == EditorTarget.ITEM_ANIMATION) {
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
        for (Entry entry : entries) {
            Widget control = entry.control;
            if (control.y + control.h < visibleTop || control.y > visibleBottom) {
                continue;
            }
            if (entry.label != null && entry.bounds.labelW > 0f) {
                String label = fit(fr, entry.label, entry.bounds.labelW);
                UIRender.text(fr, label, entry.bounds.labelX, entry.bounds.labelY,
                        Theme.textSecondary, 0.9f, false);
            }
            control.draw(pmx, pmy);
        }
        UIRender.scissorEnd();

        if (maxScroll > 0f) {
            UIRender.roundRect(inspectorLayout.scrollbarX, inspectorLayout.scrollbarY,
                    inspectorLayout.scrollbarW, inspectorLayout.scrollbarH, inspectorLayout.scrollbarW * 0.5f,
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

    /**
     * A real held-item icon preview keeps Item Animation inspectable while the editor covers the world/hand. It is
     * intentionally a renderer preview rather than a draggable HUD element; X/Y/size are applied live, and Z is
     * shown both numerically and as a subtle depth-scale cue. The authoritative hand still uses the ASM hook.
     */
    private boolean itemAnimationPreviewVisible() {
        float panelX = width - 160f;
        return target == EditorTarget.ITEM_ANIMATION && width >= 330 && panelX >= cardX + cardW + 8f;
    }

    private boolean inItemAnimationPreview(int mx, int my) {
        return itemAnimationPreviewVisible() && inside(mx, my, width - 160f, barY + BAR_H + 10f, 148f, 74f);
    }

    private void drawItemAnimationEditorPreview(Minecraft mc) {
        if (!itemAnimationPreviewVisible()) {
            return;
        }
        final float panelW = 148f;
        final float panelH = 74f;
        final float panelX = width - panelW - 12f;
        final float panelY = barY + BAR_H + 10f;
        final ItemStack held = mc.thePlayer == null ? null : mc.thePlayer.getHeldItem();
        final RenderItem itemRenderer = mc.getRenderItem();

        UIRender.beginFrame();
        try {
            UIRender.shadow(panelX, panelY, panelW, panelH, Theme.radius * 0.8f);
            UIRender.roundRectBorder(panelX, panelY, panelW, panelH, Theme.radius * 0.8f,
                    Theme.windowTop, Theme.windowBottom, Theme.border);
            UIRender.text(fr(), "LIVE HELD-ITEM PREVIEW", panelX + 9f, panelY + 8f,
                    Theme.textPrimary, 0.8f, false);
            UIRender.text(fr(), itemAnimationPreviewPositionText(), panelX + 9f, panelY + panelH - 13f,
                    Theme.textSecondary, 0.75f, false);
            UIRender.roundRect(panelX + 96f, panelY + 23f, 39f, 31f, 5f,
                    0x552D333D, 0x552D333D, 0.4f);
            if (held == null) {
                UIRender.text(fr(), "Empty hand", panelX + 14f, panelY + 37f,
                        Theme.textTertiary, 0.8f, false);
            }
        } finally {
            UIRender.endFrame();
        }

        if (held == null || itemRenderer == null) {
            return;
        }
        // Render Minecraft's real held ItemStack model, not a synthetic placeholder. The direct hook leaves map
        // paths fully vanilla, so this preview does too. Z has no practical depth in flat GUI projection.
        boolean map = held.getItem() == Items.filled_map || held.getItem() == Items.map;
        float previewScale = map ? 1.0F : ItemAnimation.getSize() * (1.0F + ItemAnimation.getZ() * 0.12F);
        previewScale = Math.max(0.35F, Math.min(2.25F, previewScale));
        float iconX = panelX + 106f + (map ? 0.0F : ItemAnimation.getX() * 24f);
        float iconY = panelY + 29f - (map ? 0.0F : ItemAnimation.getY() * 24f);
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(iconX, iconY, 200f);
            GlStateManager.scale(previewScale, previewScale, 1f);
            GlStateManager.enableRescaleNormal();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.enableDepth();
            RenderHelper.enableGUIStandardItemLighting();
            itemRenderer.renderItemAndEffectIntoGUI(held, 0, 0);
        } finally {
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
            GlStateManager.disableDepth();
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
        }
    }

    // ------------------------------------------------------------------ input

    @Override
    public void handleMouseInput() throws IOException {
        int wheel = Mouse.getEventDWheel();
        super.handleMouseInput();
        int mx = Mouse.getEventX() * width / Minecraft.getMinecraft().displayWidth;
        int my = height - Mouse.getEventY() * height / Minecraft.getMinecraft().displayHeight - 1;
        if (wheel != 0 && openDropdown == null && inCardView(mx, my)) {
            boolean fine = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
            scroll.wheel(wheel / 120f, fine, scrollSpeed());
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
            applyFocusedItemAnimationExact();
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
        if (inScrollbar(mx, my)) {
            draggingScrollbar = true;
            boolean thumb = my >= inspectorLayout.scrollbarY
                    && my < inspectorLayout.scrollbarY + inspectorLayout.scrollbarH;
            scrollbarGrabOffset = thumb ? my - inspectorLayout.scrollbarY : inspectorLayout.scrollbarH * 0.5f;
            setScrollbarFromY(my);
            return;
        }

        SearchField exactField = itemAnimationExactAt(mx, my);
        if (exactField != null) {
            focusItemAnimationExactField(exactField);
            return;
        }
        applyFocusedItemAnimationExact();
        if (target == EditorTarget.HUD_ELEMENT && manualField.contains(mx, my) && inCardView(mx, my)) {
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
            if (!(w instanceof AppleSlider) && !(w instanceof HorizontalDragWidget)) {
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

        // The Item Animation preview is informational and renderer-only; never let a click through it start
        // dragging a HUD element underneath it.
        if (inItemAnimationPreview(mx, my)) {
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
        if (draggingScrollbar) {
            setScrollbarFromY(my);
            return;
        }
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
        if (draggingScrollbar) {
            draggingScrollbar = false;
            return;
        }
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
            if ((active instanceof AppleSlider || active instanceof HorizontalDragWidget)
                    && gesture.kind == Gesture.PENDING) {
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
        SearchField exactField = focusedItemAnimationField();
        if (code == Keyboard.KEY_ESCAPE) {
            if (openDropdown != null) {
                openDropdown = null;
            } else if (exactField != null) {
                exactField.clear();
                exactField.setFocused(false);
            } else if (manualField.isFocused()) {
                manualField.setFocused(false);
            } else {
                close();
            }
            return;
        }
        if (exactField != null) {
            if (code == Keyboard.KEY_RETURN || code == Keyboard.KEY_NUMPADENTER) {
                applyItemAnimationExact(exactField);
                return;
            }
            if (acceptsItemAnimationKey(exactField, c, code)) {
                exactField.keyTyped(c, code);
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
            else selectItemAnimation();
        }
    }
}
