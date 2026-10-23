package com.arena.pekaclient.gui;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.config.FeatureUiPreferences;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import com.arena.pekaclient.widget.AppleButton;
import com.arena.pekaclient.widget.AppleDropdown;
import com.arena.pekaclient.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Shared focused-settings surface.
 *
 * <p>The left side is always the one scrollable settings column.  When a feature has a meaningful
 * visual surface, the remaining right side is a clipped, non-interactive Live Preview; a feature
 * without one deliberately uses the whole width for settings rather than showing an empty mockup.
 * Widgets still write their live configuration owners directly, so there is no Apply/Save staging
 * state and no second preview copy of any setting.</p>
 */
public final class FeatureSettingsEditorScreen extends AppleScreen {
    public interface EntryFactory {
        void build(Category category, List<Entry> entries);
    }

    /** A preview receives the exact live settings owner and is drawn only while this editor is open. */
    public interface Preview {
        /** The object/class which owns the values used by the live renderer for this category. */
        Object settingsObject(String categoryId);
        /** Draw in the already clipped preview viewport. Implementations must not allocate per frame. */
        void draw(PreviewContext context);
    }

    /** Reused frame context; preview implementations must never retain it. */
    public static final class PreviewContext {
        public String categoryId = "";
        public float x, y, width, height;
        public int mouseX, mouseY;
        public float partialTicks;
        public long nowNanos;
        public FontRenderer font;

        private void set(String category, float px, float py, float pw, float ph, int mx, int my,
                         float partial, long now, FontRenderer renderer) {
            categoryId = category == null ? "" : category;
            x = px;
            y = py;
            width = pw;
            height = ph;
            mouseX = mx;
            mouseY = my;
            partialTicks = partial;
            nowNanos = now;
            font = renderer;
        }
    }

    public interface ResetAction {
        void reset();
    }

    /** Lightweight shared thumbnail contract for category headings such as Mob ESP. */
    public interface CategoryThumbnail {
        void draw(float x, float y, float width, float height);
    }

    /** A flat, one-level section heading and its existing feature switch. */
    public static final class Category {
        public final String id;
        public final String title;
        public final String description;
        private final java.util.function.Supplier<Boolean> enabled;
        private final java.util.function.Consumer<Boolean> enabledSetter;
        private final CategoryThumbnail thumbnail;

        public Category(String id, String title, String description) {
            this(id, title, description, null, null, null);
        }

        public Category(String id, String title, String description,
                        java.util.function.Supplier<Boolean> enabled,
                        java.util.function.Consumer<Boolean> enabledSetter,
                        CategoryThumbnail thumbnail) {
            this.id = id == null ? "" : id;
            this.title = title == null ? "" : title;
            this.description = description == null ? "" : description;
            this.enabled = enabled;
            this.enabledSetter = enabledSetter;
            this.thumbnail = thumbnail;
        }

        public boolean hasToggle() { return enabled != null && enabledSetter != null; }
        public boolean isEnabled() { return enabled != null && Boolean.TRUE.equals(enabled.get()); }
        public void setEnabled(boolean value) { if (enabledSetter != null) enabledSetter.accept(value); }
        public boolean hasThumbnail() { return thumbnail != null; }
        public void drawThumbnail(float x, float y, float width, float height) {
            if (thumbnail != null) thumbnail.draw(x, y, width, height);
        }
    }

    /** One live-setting row. The widget is the actual feature widget/configuration owner. */
    public static final class Entry {
        public final String label;
        public final String description;
        public final Widget widget;
        private float labelX;
        private float labelW;

        public Entry(String label, String description, Widget widget) {
            this.label = label == null ? "" : label;
            this.description = description;
            this.widget = widget;
        }

        public static Entry action(String label, String description, String buttonLabel, Runnable action) {
            return new Entry(label, description, new AppleButton(buttonLabel, 106f, 24f, false, action));
        }
    }

    public static final class Spec {
        public final String id;
        public final String title;
        public final String subtitle;
        public final List<Category> categories;
        public final EntryFactory entries;
        public final Preview preview;
        public final ResetAction reset;

        public Spec(String id, String title, String subtitle, List<Category> categories,
                    EntryFactory entries, Preview preview, ResetAction reset) {
            this.id = id == null ? "settings" : id;
            this.title = title == null ? "Feature Settings" : title;
            this.subtitle = subtitle == null ? "" : subtitle;
            this.categories = categories == null ? Collections.<Category>emptyList()
                    : Collections.unmodifiableList(new ArrayList<Category>(categories));
            this.entries = entries;
            this.preview = preview;
            this.reset = reset;
        }
    }

    private static final float TITLE_H = 34f;
    private static final float PAD = 12f;
    private static final float BOTTOM_H = 28f;
    private static final float CATEGORY_H = 42f;
    private static final float ENTRY_H = 42f;
    private static final float GAP = 6f;
    private static final float SETTINGS_RATIO = .35f;

    private final GuiScreen parent;
    private final Spec spec;
    private final List<Entry> entries = new ArrayList<Entry>();
    private final GuiLayoutToolkit.Scroll scroll = new GuiLayoutToolkit.Scroll();
    private final GuiLayoutToolkit.Scroll navigationScroll = new GuiLayoutToolkit.Scroll();
    private final GuiLayoutToolkit.Rect panel = new GuiLayoutToolkit.Rect();
    private final GuiLayoutToolkit.Rect content = new GuiLayoutToolkit.Rect();
    /** Category section headings. Kept separate from the scrollable setting rows. */
    private final GuiLayoutToolkit.Rect left = new GuiLayoutToolkit.Rect();
    /** Right-hand preview only; never a duplicate settings pane. */
    private final GuiLayoutToolkit.Rect right = new GuiLayoutToolkit.Rect();
    private final GuiLayoutToolkit.Rect previewBounds = new GuiLayoutToolkit.Rect();
    private final GuiLayoutToolkit.Rect entriesView = new GuiLayoutToolkit.Rect();
    private final GuiLayoutToolkit.Rect row = new GuiLayoutToolkit.Rect();
    private final GuiLayoutToolkit.Rect label = new GuiLayoutToolkit.Rect();
    private final GuiLayoutToolkit.Rect control = new GuiLayoutToolkit.Rect();
    private final PreviewContext previewContext = new PreviewContext();
    private final AppleButton back;
    private final AppleButton reset;
    private String selectedId;
    private Widget active;
    private long lastNs;
    private float scaleFactor = 1f;
    private boolean dirtyEntries = true;
    private boolean previewAvailable = true;
    private String status = "";

    public FeatureSettingsEditorScreen(GuiScreen parent, Spec spec) {
        this(parent, spec, null);
    }

    /** Opens a routed child category while retaining the remembered category for ordinary openings. */
    public FeatureSettingsEditorScreen(GuiScreen parent, Spec spec, String requestedCategory) {
        this.parent = parent;
        this.spec = spec == null ? new Spec("settings", "Feature Settings", "", null, null, null, null) : spec;
        String fallback = this.spec.categories.isEmpty() ? "" : this.spec.categories.get(0).id;
        selectedId = requestedCategory == null || requestedCategory.length() == 0
                ? FeatureUiPreferences.selection(this.spec.id, fallback) : requestedCategory;
        if (category(selectedId) == null) selectedId = fallback;
        FeatureUiPreferences.setSelection(this.spec.id, selectedId);
        back = new AppleButton("Back", 64f, 22f, false, this::backToParent);
        reset = new AppleButton("Reset", 66f, 22f, false, this::resetCurrent);
    }

    public String selectedCategoryId() { return selectedId; }

    /** Test and diagnostics hook proving a preview and live renderer share the same settings owner. */
    Object previewSettingsIdentity() {
        return spec.preview == null ? null : spec.preview.settingsObject(selectedId);
    }

    private Category category(String id) {
        for (Category category : spec.categories) if (category.id.equals(id)) return category;
        return null;
    }

    private void select(Category category) {
        if (category == null || category.id.equals(selectedId)) return;
        selectedId = category.id;
        FeatureUiPreferences.setSelection(spec.id, selectedId);
        scroll.reset();
        dirtyEntries = true;
        openDropdown = null;
        status = "";
        previewAvailable = true;
    }

    private void rebuildEntries() {
        if (!dirtyEntries) return;
        entries.clear();
        Category category = category(selectedId);
        if (category != null && spec.entries != null) spec.entries.build(category, entries);
        dirtyEntries = false;
    }

    private void resetCurrent() {
        if (spec.reset != null) {
            spec.reset.reset();
            status = "Defaults restored.";
            dirtyEntries = true;
        }
    }

    private void backToParent() {
        AppleSettings.save();
        Minecraft.getMinecraft().displayGuiScreen(parent);
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        Theme.refresh();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        AppleSettings.save();
        super.onGuiClosed();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long now = System.nanoTime();
        float dt = lastNs == 0L ? 1f / 60f : Math.min(.1f, (now - lastNs) / 1.0e9f);
        lastNs = now;
        rebuildEntries();
        layout();
        scroll.update(dt, Theme.animations && AppleSettings.smoothScrolling);
        navigationScroll.update(dt, Theme.animations && AppleSettings.smoothScrolling);
        for (Entry entry : entries) if (entry.widget != null) entry.widget.update(dt);
        back.update(dt);
        reset.update(dt);

        drawDefaultBackground();
        UIRender.beginFrame();
        UIRender.rect(0f, 0f, width, height, Theme.veil);
        UIRender.shadow(panel.x, panel.y, panel.w, panel.h, Theme.radius);
        UIRender.roundRectBorder(panel.x, panel.y, panel.w, panel.h, Theme.radius,
                Theme.windowTop, Theme.windowBottom, Theme.border);
        UIRender.rect(panel.x + 1f, panel.y + TITLE_H, panel.w - 2f, 1f, Theme.divider);
        WindowDots.draw(panel.x + 16f, panel.y + 17f, mouseX, mouseY, 0b001, false);

        FontRenderer font = fr();
        UIRender.text(font, spec.title, panel.x + 42f, panel.y + 12.5f, Theme.textPrimary, 1.05f, false);
        String heading = spec.preview == null ? "Settings" : "Settings + Live Preview";
        UIRender.text(font, heading, panel.right() - PAD - font.getStringWidth(heading) * .78f,
                panel.y + 13.5f, Theme.textTertiary, .78f, false);
        drawSubtitle(font);
        drawNavigation(mouseX, mouseY, font);
        drawSettings(mouseX, mouseY, partialTicks, now, font);
        drawBottomBar(mouseX, mouseY, font);
        if (openDropdown != null) {
            openDropdown.drawPopup(mouseX, mouseY, panel.y, panel.bottom(), panel.right(), panel.x);
        }
        UIRender.endFrame();
    }

    private void layout() {
        float availW = Math.max(1f, width - 8f);
        float availH = Math.max(1f, height - 8f);
        float desiredW = Math.min(availW, 920f);
        float desiredH = Math.min(availH, 560f);
        desiredW = Math.max(Math.min(300f, availW), desiredW);
        desiredH = Math.max(Math.min(200f, availH), desiredH);
        GuiLayoutToolkit.clampPanel(panel, (width - desiredW) * .5f, (height - desiredH) * .5f,
                desiredW, desiredH, width, height);

        float contentTop = panel.y + TITLE_H + PAD + 18f;
        content.set(panel.x + PAD, contentTop, panel.w - PAD * 2f,
                Math.max(1f, panel.bottom() - PAD - BOTTOM_H - GAP - contentTop));
        boolean hasPreview = spec.preview != null;
        GuiLayoutToolkit.splitLivePreview(content, SETTINGS_RATIO, GAP, 110f, hasPreview, left, right);
        float settingsW = left.w;
        float categoryH = Math.min(Math.max(42f, content.h * .30f), Math.max(42f, spec.categories.size() * CATEGORY_H + 8f));
        if (content.h < 115f) categoryH = Math.min(categoryH, Math.max(26f, content.h * .38f));
        left.set(left.x, left.y, settingsW, Math.min(categoryH, content.h));
        entriesView.set(content.x, Math.min(content.bottom(), left.bottom() + GAP), settingsW,
                Math.max(1f, content.bottom() - Math.min(content.bottom(), left.bottom() + GAP)));
        previewBounds.set(right.x, right.y, right.w, right.h);
        navigationScroll.setMaximum(Math.max(0f, spec.categories.size() * CATEGORY_H + 8f - left.h));

        back.x = panel.x + PAD;
        back.y = panel.bottom() - PAD - 22f;
        back.w = Math.min(back.preferredWidth(), Math.max(38f, panel.w * .13f));
        reset.x = back.x + back.w + GAP;
        reset.y = back.y;
        reset.w = Math.min(reset.preferredWidth(), Math.max(40f, panel.w * .13f));

        float total = 0f;
        for (Entry ignored : entries) total += ENTRY_H + GAP;
        scroll.setMaximum(Math.max(0f, total - entriesView.h));
        float cursor = entriesView.y - scroll.value();
        for (Entry entry : entries) {
            row.set(entriesView.x, cursor, entriesView.w, ENTRY_H);
            if (entry.widget != null) {
                GuiLayoutToolkit.splitRow(row, 9f, 7f, entry.widget.preferredWidth(), entry.widget.h, label, control);
                entry.labelX = label.x;
                entry.labelW = label.w;
                entry.widget.x = control.x;
                entry.widget.y = control.y;
                entry.widget.w = control.w;
                entry.widget.h = control.h;
            } else {
                entry.labelX = entriesView.x + 9f;
                entry.labelW = Math.max(1f, entriesView.w - 18f);
            }
            cursor += ENTRY_H + GAP;
        }
        scaleFactor = new ScaledResolution(Minecraft.getMinecraft()).getScaleFactor();
    }

    private void drawSubtitle(FontRenderer font) {
        String subtitle = status.length() == 0 ? spec.subtitle : status;
        int color = status.length() == 0 ? Theme.textSecondary : Theme.accent;
        UIRender.text(font, fit(font, subtitle, panel.w - PAD * 2f), panel.x + PAD, panel.y + TITLE_H + 6f,
                color, .83f, false);
    }

    private void drawBottomBar(int mouseX, int mouseY, FontRenderer font) {
        UIRender.rect(panel.x + 1f, back.y - 7f, panel.w - 2f, 1f, Theme.divider);
        back.draw(mouseX, mouseY);
        reset.draw(mouseX, mouseY);
        String hint = spec.preview == null ? "Changes apply instantly." : "Live Preview — changes apply instantly.";
        float hintX = reset.x + reset.w + 10f;
        UIRender.text(font, fit(font, hint, panel.right() - PAD - hintX), hintX, back.y + 7f,
                Theme.textTertiary, .80f, false);
    }

    private float categoryToggleWidth() {
        return Math.min(34f, Math.max(26f, left.w * .26f));
    }

    private float categoryToggleX() {
        return left.right() - 8f - categoryToggleWidth();
    }

    private boolean categoryToggleContains(Category category, int mouseX, int mouseY, float rowY) {
        return category != null && category.hasToggle() && inside(mouseX, mouseY, categoryToggleX(),
                rowY + (CATEGORY_H - 20f) * .5f, categoryToggleWidth(), 20f);
    }

    private void drawCategoryToggle(Category category, float rowY) {
        if (!category.hasToggle()) return;
        float tw = categoryToggleWidth();
        float tx = categoryToggleX();
        float ty = rowY + (CATEGORY_H - 20f) * .5f;
        boolean enabled = category.isEnabled();
        int fill = enabled ? Theme.accent : Theme.track;
        UIRender.roundRect(tx, ty, tw, 20f, 10f, fill, fill, .4f);
        float knob = 16f;
        float knobX = enabled ? tx + tw - knob - 2f : tx + 2f;
        UIRender.circle(knobX + knob * .5f, ty + 10f, knob * .5f, 0xFFFFFFFF);
    }

    /** Plain section-heading navigation; no nested tree/dropdowns are created here. */
    private void drawNavigation(int mouseX, int mouseY, FontRenderer font) {
        UIRender.roundRectBorder(left.x, left.y, left.w, left.h, Math.max(4f, Theme.radius - 4f),
                Theme.card, Theme.card, Theme.border);
        UIRender.scissorBegin(left.x, left.y, left.w, left.h, scaleFactor);
        float cursor = left.y + 4f - navigationScroll.value();
        for (Category category : spec.categories) {
            if (cursor + CATEGORY_H >= left.y && cursor <= left.bottom()) {
                boolean chosen = category.id.equals(selectedId);
                boolean hover = inside(mouseX, mouseY, left.x + 4f, cursor, left.w - 8f, CATEGORY_H);
                int fill = chosen ? UIRender.withAlpha(Theme.accent, .18f) : hover ? Theme.cardHover : Theme.card;
                UIRender.roundRect(left.x + 4f, cursor, Math.max(1f, left.w - 8f), CATEGORY_H - 2f,
                        5f, fill, fill, 1f);
                float toggleX = category.hasToggle() ? categoryToggleX() : left.right() - 8f;
                float thumbnailW = category.hasThumbnail() ? 18f : 0f;
                float thumbnailX = toggleX - (thumbnailW > 0f ? thumbnailW + 7f : 0f);
                if (category.hasThumbnail()) category.drawThumbnail(thumbnailX, cursor + 10f, thumbnailW, 22f);
                float titleX = left.x + 10f;
                float titleW = Math.max(1f, thumbnailX - titleX - 6f);
                UIRender.text(font, fit(font, category.title, titleW), titleX, cursor + 7f,
                        chosen ? Theme.textPrimary : Theme.textSecondary, .92f, false);
                if (category.description.length() > 0) {
                    UIRender.text(font, fit(font, category.description, titleW), titleX, cursor + 21f,
                            Theme.textTertiary, .73f, false);
                }
                drawCategoryToggle(category, cursor);
            }
            cursor += CATEGORY_H;
        }
        UIRender.scissorEnd();
    }

    private void drawSettings(int mouseX, int mouseY, float partialTicks, long now, FontRenderer font) {
        if (spec.preview != null) drawPreview(mouseX, mouseY, partialTicks, now, font);
        UIRender.scissorBegin(entriesView.x, entriesView.y, entriesView.w, entriesView.h, scaleFactor);
        float cursor = entriesView.y - scroll.value();
        for (Entry entry : entries) {
            if (cursor + ENTRY_H >= entriesView.y && cursor <= entriesView.bottom()) {
                UIRender.roundRectBorder(entriesView.x, cursor, entriesView.w, ENTRY_H,
                        Math.max(4f, Theme.radius - 4f), Theme.card, Theme.card, Theme.border);
                UIRender.text(font, fit(font, entry.label, entry.labelW), entry.labelX, cursor + 7f,
                        Theme.textPrimary, .90f, false);
                if (entry.description != null && entry.description.length() > 0) {
                    UIRender.text(font, fit(font, entry.description, entry.labelW), entry.labelX, cursor + 21f,
                            Theme.textSecondary, .78f, false);
                }
                if (entry.widget != null) entry.widget.draw(mouseX, mouseY);
            }
            cursor += ENTRY_H + GAP;
        }
        UIRender.scissorEnd();
        if (entries.isEmpty()) {
            String empty = "No settings in this section.";
            UIRender.text(font, empty, entriesView.x + 8f, entriesView.y + 18f, Theme.textTertiary, .85f, false);
        }
    }

    private void drawPreview(int mouseX, int mouseY, float partialTicks, long now, FontRenderer font) {
        UIRender.roundRectBorder(previewBounds.x, previewBounds.y, previewBounds.w, previewBounds.h,
                Math.max(4f, Theme.radius - 4f), Theme.card, Theme.card, Theme.border);
        UIRender.text(font, "LIVE PREVIEW", previewBounds.x + 10f, previewBounds.y + 9f,
                Theme.textTertiary, .72f, false);
        if (!previewAvailable) {
            String unavailable = "Preview unavailable";
            UIRender.text(font, unavailable, previewBounds.x + (previewBounds.w - font.getStringWidth(unavailable) * .9f) * .5f,
                    previewBounds.y + previewBounds.h * .5f, Theme.textTertiary, .9f, false);
            return;
        }
        try {
            UIRender.scissorBegin(previewBounds.x + 1f, previewBounds.y + 18f,
                    Math.max(1f, previewBounds.w - 2f), Math.max(1f, previewBounds.h - 19f), scaleFactor);
            previewContext.set(selectedId, previewBounds.x + 1f, previewBounds.y + 19f,
                    Math.max(1f, previewBounds.w - 2f), Math.max(1f, previewBounds.h - 20f),
                    mouseX, mouseY, partialTicks, now, font);
            spec.preview.draw(previewContext);
            UIRender.scissorEnd();
        } catch (RuntimeException | LinkageError unavailable) {
            // A missing renderer/world/mod hook must never block access to the real settings column.
            previewAvailable = false;
            try { UIRender.scissorEnd(); } catch (RuntimeException ignored) { }
        }
    }

    private Category navigationAt(int mouseX, int mouseY) {
        if (!left.contains(mouseX, mouseY)) return null;
        float cursor = left.y + 4f - navigationScroll.value();
        for (Category category : spec.categories) {
            if (inside(mouseX, mouseY, left.x + 4f, cursor, left.w - 8f, CATEGORY_H)) return category;
            cursor += CATEGORY_H;
        }
        return null;
    }

    private boolean categoryToggleAt(Category target, int mouseX, int mouseY) {
        float cursor = left.y + 4f - navigationScroll.value();
        for (Category category : spec.categories) {
            if (category == target) return categoryToggleContains(category, mouseX, mouseY, cursor);
            cursor += CATEGORY_H;
        }
        return false;
    }

    private Widget widgetAt(int mouseX, int mouseY) {
        if (!entriesView.contains(mouseX, mouseY)) return null;
        for (Entry entry : entries) if (entry.widget != null && entry.widget.contains(mouseX, mouseY)) return entry.widget;
        return null;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        if (button != 0) return;
        if (handleOpenDropdownClick(mouseX, mouseY)) return;
        if (!panel.contains(mouseX, mouseY)) { backToParent(); return; }
        if (WindowDots.hit(mouseX, mouseY, panel.x + 16f, panel.y + 17f, 0b001) == WindowDots.CLOSE) {
            backToParent();
            return;
        }
        if (back.contains(mouseX, mouseY)) { active = back; active.onPress(mouseX, mouseY); return; }
        if (reset.contains(mouseX, mouseY)) { active = reset; active.onPress(mouseX, mouseY); return; }
        Category category = navigationAt(mouseX, mouseY);
        if (category != null) {
            if (categoryToggleAt(category, mouseX, mouseY)) {
                category.setEnabled(!category.isEnabled());
                if (category.id.equals(selectedId)) dirtyEntries = true;
                return;
            }
            select(category);
            return;
        }
        active = widgetAt(mouseX, mouseY);
        if (active != null) active.onPress(mouseX, mouseY);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long sinceClick) {
        if (active != null) active.onDrag(mouseX, mouseY);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int button) {
        if (active == null) return;
        Widget released = active;
        active = null;
        boolean activate = released.contains(mouseX, mouseY);
        released.onRelease(mouseX, mouseY, activate);
        if (activate && released instanceof AppleDropdown) {
            AppleDropdown dropdown = (AppleDropdown) released;
            openDropdown = openDropdown == dropdown ? null : dropdown;
            if (openDropdown != null) openDropdown.onExpand();
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        int wheel = Mouse.getEventDWheel();
        super.handleMouseInput();
        int mouseX = Mouse.getEventX() * width / Math.max(1, Minecraft.getMinecraft().displayWidth);
        int mouseY = height - Mouse.getEventY() * height / Math.max(1, Minecraft.getMinecraft().displayHeight) - 1;
        if (wheel != 0 && openDropdown == null) {
            boolean fine = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
            if (left.contains(mouseX, mouseY)) navigationScroll.wheel(wheel / 120f, fine,
                    AppleSettings.guiScrollMultiplier(), 30f);
            else if (entriesView.contains(mouseX, mouseY)) scroll.wheel(wheel / 120f, fine,
                    AppleSettings.guiScrollMultiplier(), 30f);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (openDropdown != null) { openDropdown = null; return; }
            backToParent();
            return;
        }
        if (keyCode == Keyboard.KEY_UP) { scroll.nudge(-ENTRY_H); return; }
        if (keyCode == Keyboard.KEY_DOWN) { scroll.nudge(ENTRY_H); return; }
        if (keyCode == Keyboard.KEY_PRIOR) { scroll.nudge(-entriesView.h * .8f); return; }
        if (keyCode == Keyboard.KEY_NEXT) scroll.nudge(entriesView.h * .8f);
    }
}
