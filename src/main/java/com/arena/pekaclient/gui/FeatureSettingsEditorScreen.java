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
 * Reusable two-pane settings surface for detailed features.
 *
 * <p>It owns only editor chrome, selection persistence, scrolling and widget routing. A feature supplies its
 * categories, existing setting widgets, reset action and optional preview; it never has to duplicate a modal or
 * layout implementation. This keeps the hub compact without changing the feature/configuration owners.</p>
 */
public final class FeatureSettingsEditorScreen extends AppleScreen {
    public interface EntryFactory {
        void build(Category category, List<Entry> entries);
    }

    public interface Preview {
        void draw(String categoryId, float x, float y, float width, float height, FontRenderer font);
    }

    public interface ResetAction {
        void reset();
    }

    /** Navigation category. Parent/depth optionally make this a visual tree instead of a flat list. */
    public static final class Category {
        public final String id;
        public final String parentId;
        public final int depth;
        public final String title;
        public final String description;

        public Category(String id, String title, String description) {
            this(id, null, 0, title, description);
        }

        public Category(String id, String parentId, int depth, String title, String description) {
            this.id = id == null ? "" : id;
            this.parentId = parentId;
            this.depth = Math.max(0, depth);
            this.title = title == null ? "" : title;
            this.description = description == null ? "" : description;
        }
    }

    /** One right-pane setting row. Widgets remain the existing source of behavior and visual language. */
    public static final class Entry {
        public final String label;
        public final String description;
        public final Widget widget;

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
    private static final float LEFT_W = 166f;
    private static final float CAT_H = 42f;
    private static final float ENTRY_H = 42f;
    private static final float GAP = 6f;

    private final GuiScreen parent;
    private final Spec spec;
    private final List<Entry> entries = new ArrayList<Entry>();
    private final GuiLayoutToolkit.Scroll scroll = new GuiLayoutToolkit.Scroll();
    private final GuiLayoutToolkit.Rect panel = new GuiLayoutToolkit.Rect();
    private final GuiLayoutToolkit.Rect content = new GuiLayoutToolkit.Rect();
    private final GuiLayoutToolkit.Rect left = new GuiLayoutToolkit.Rect();
    private final GuiLayoutToolkit.Rect right = new GuiLayoutToolkit.Rect();
    private final GuiLayoutToolkit.Rect previewBounds = new GuiLayoutToolkit.Rect();
    private final GuiLayoutToolkit.Rect entriesView = new GuiLayoutToolkit.Rect();
    private final GuiLayoutToolkit.Rect row = new GuiLayoutToolkit.Rect();
    private final GuiLayoutToolkit.Rect label = new GuiLayoutToolkit.Rect();
    private final GuiLayoutToolkit.Rect control = new GuiLayoutToolkit.Rect();
    private final AppleButton back;
    private final AppleButton reset;
    private String selectedId;
    private Widget active;
    private long lastNs;
    private float scaleFactor = 1f;
    private boolean dirtyEntries = true;
    private String status = "";

    public FeatureSettingsEditorScreen(GuiScreen parent, Spec spec) {
        this(parent, spec, null);
    }

    /** Opens a routed child category while retaining the remembered selection for ordinary editor openings. */
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
        String version = "Settings";
        UIRender.text(font, version, panel.x + panel.w - PAD - font.getStringWidth(version) * .85f,
                panel.y + 13.5f, Theme.textTertiary, .85f, false);
        drawToolbar(mouseX, mouseY, font);
        drawNavigation(mouseX, mouseY, font);
        drawSettings(mouseX, mouseY, font);
        if (openDropdown != null) {
            openDropdown.drawPopup(mouseX, mouseY, panel.y, panel.bottom(), panel.right(), panel.x);
        }
        UIRender.endFrame();
    }

    private void layout() {
        float availW = Math.max(1f, width - 8f);
        float availH = Math.max(1f, height - 8f);
        float desiredW = Math.min(availW, 760f);
        float desiredH = Math.min(availH, 470f);
        desiredW = Math.max(Math.min(260f, availW), desiredW);
        desiredH = Math.max(Math.min(180f, availH), desiredH);
        GuiLayoutToolkit.clampPanel(panel, (width - desiredW) * .5f, (height - desiredH) * .5f,
                desiredW, desiredH, width, height);
        float top = panel.y + TITLE_H + PAD;
        float toolbarH = 24f;
        content.set(panel.x + PAD, top + toolbarH + PAD, panel.w - PAD * 2f,
                Math.max(1f, panel.bottom() - PAD - (top + toolbarH + PAD)));
        float actualLeft = Math.min(LEFT_W, Math.max(94f, content.w * .32f));
        left.set(content.x, content.y, actualLeft, content.h);
        right.set(left.right() + GAP, content.y, Math.max(1f, content.w - actualLeft - GAP), content.h);
        float previewH = spec.preview == null ? 0f : Math.min(92f, Math.max(46f, right.h * .28f));
        previewBounds.set(right.x, right.y, right.w, previewH);
        entriesView.set(right.x, right.y + previewH + (previewH > 0 ? GAP : 0), right.w,
                Math.max(1f, right.h - previewH - (previewH > 0 ? GAP : 0)));
        back.x = panel.x + PAD;
        back.y = top;
        back.w = Math.min(back.preferredWidth(), Math.max(38f, panel.w * .13f));
        reset.x = panel.right() - PAD - Math.min(reset.preferredWidth(), Math.max(38f, panel.w * .13f));
        reset.y = top;
        reset.w = Math.min(reset.preferredWidth(), Math.max(38f, panel.w * .13f));

        float total = 0f;
        for (Entry ignored : entries) total += ENTRY_H + GAP;
        scroll.setMaximum(Math.max(0f, total - entriesView.h));
        float cursor = entriesView.y - scroll.value();
        for (Entry entry : entries) {
            row.set(entriesView.x, cursor, entriesView.w, ENTRY_H);
            if (entry.widget != null) {
                GuiLayoutToolkit.splitRow(row, 10f, 8f, entry.widget.preferredWidth(), entry.widget.h, label, control);
                entry.widget.x = control.x;
                entry.widget.y = control.y;
                entry.widget.w = control.w;
            }
            cursor += ENTRY_H + GAP;
        }
        scaleFactor = new ScaledResolution(Minecraft.getMinecraft()).getScaleFactor();
    }

    private void drawToolbar(int mouseX, int mouseY, FontRenderer font) {
        back.draw(mouseX, mouseY);
        reset.draw(mouseX, mouseY);
        float textX = back.x + back.w + 10f;
        float textW = Math.max(1f, reset.x - textX - 10f);
        String subtitle = status.length() == 0 ? spec.subtitle : status;
        int color = status.length() == 0 ? Theme.textSecondary : Theme.accent;
        UIRender.text(font, fit(font, subtitle, textW), textX, back.y + 7f, color, .92f, false);
    }

    private void drawNavigation(int mouseX, int mouseY, FontRenderer font) {
        UIRender.roundRectBorder(left.x, left.y, left.w, left.h, Math.max(4f, Theme.radius - 4f),
                Theme.card, Theme.card, Theme.border);
        float cursor = left.y + 4f;
        for (Category category : spec.categories) {
            if (cursor + CAT_H > left.bottom()) break;
            boolean chosen = category.id.equals(selectedId);
            boolean hover = inside(mouseX, mouseY, left.x + 4f, cursor, left.w - 8f, CAT_H);
            int fill = chosen ? UIRender.withAlpha(Theme.accent, .18f)
                    : hover ? Theme.cardHover : Theme.card;
            float indent = category.depth * FeatureTreeLayout.INDENT;
            UIRender.roundRect(left.x + 4f + indent, cursor, Math.max(1f, left.w - 8f - indent), CAT_H - 2f,
                    5f, fill, fill, 1f);
            if (category.depth > 0) {
                float lineX = left.x + 10f + (category.depth - 1) * FeatureTreeLayout.INDENT;
                UIRender.rect(lineX, cursor - 2f, 1f, CAT_H * .5f + 1f, Theme.divider);
                UIRender.rect(lineX, cursor + CAT_H * .5f, FeatureTreeLayout.CONNECTOR_TICK, 1f, Theme.divider);
            }
            float titleX = left.x + 10f + indent;
            UIRender.text(font, fit(font, category.title, Math.max(1f, left.right() - titleX - 8f)), titleX,
                    cursor + 8f, chosen ? Theme.textPrimary : Theme.textSecondary, .95f, false);
            if (category.description.length() > 0) {
                UIRender.text(font, fit(font, category.description, Math.max(1f, left.right() - titleX - 8f)),
                        titleX, cursor + 21f, Theme.textTertiary, .78f, false);
            }
            cursor += CAT_H;
        }
    }

    private void drawSettings(int mouseX, int mouseY, FontRenderer font) {
        if (spec.preview != null) {
            UIRender.roundRectBorder(previewBounds.x, previewBounds.y, previewBounds.w, previewBounds.h,
                    Math.max(4f, Theme.radius - 4f), Theme.card, Theme.card, Theme.border);
            spec.preview.draw(selectedId, previewBounds.x, previewBounds.y, previewBounds.w, previewBounds.h, font);
        }
        UIRender.scissorBegin(entriesView.x, entriesView.y, entriesView.w, entriesView.h, scaleFactor);
        float cursor = entriesView.y - scroll.value();
        for (Entry entry : entries) {
            if (cursor + ENTRY_H >= entriesView.y && cursor <= entriesView.bottom()) {
                UIRender.roundRectBorder(entriesView.x, cursor, entriesView.w, ENTRY_H,
                        Math.max(4f, Theme.radius - 4f), Theme.card, Theme.card, Theme.border);
                float labelW = entry.widget == null ? entriesView.w - 20f : label.w;
                UIRender.text(font, fit(font, entry.label, labelW), entriesView.x + 10f, cursor + 7f,
                        Theme.textPrimary, .95f, false);
                if (entry.description != null && entry.description.length() > 0) {
                    UIRender.text(font, fit(font, entry.description, labelW), entriesView.x + 10f, cursor + 21f,
                            Theme.textSecondary, .85f, false);
                }
                if (entry.widget != null) entry.widget.draw(mouseX, mouseY);
            }
            cursor += ENTRY_H + GAP;
        }
        UIRender.scissorEnd();
        if (entries.isEmpty()) {
            String empty = "No settings in this category.";
            UIRender.text(font, empty, entriesView.x + (entriesView.w - font.getStringWidth(empty)) * .5f,
                    entriesView.y + 18f, Theme.textTertiary, .9f, false);
        }
    }

    private Category navigationAt(int mouseX, int mouseY) {
        if (!left.contains(mouseX, mouseY)) return null;
        float cursor = left.y + 4f;
        for (Category category : spec.categories) {
            if (cursor + CAT_H > left.bottom()) return null;
            if (inside(mouseX, mouseY, left.x + 4f, cursor, left.w - 8f, CAT_H)) return category;
            cursor += CAT_H;
        }
        return null;
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
        if (!panel.contains(mouseX, mouseY)) {
            backToParent();
            return;
        }
        if (WindowDots.hit(mouseX, mouseY, panel.x + 16f, panel.y + 17f, 0b001) == WindowDots.CLOSE) {
            backToParent();
            return;
        }
        if (back.contains(mouseX, mouseY)) { active = back; active.onPress(mouseX, mouseY); return; }
        if (reset.contains(mouseX, mouseY)) { active = reset; active.onPress(mouseX, mouseY); return; }
        Category category = navigationAt(mouseX, mouseY);
        if (category != null) { select(category); return; }
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
        if (wheel != 0 && openDropdown == null && entriesView.contains(mouseX, mouseY)) {
            boolean fine = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
            scroll.wheel(wheel / 120f, fine, AppleSettings.guiScrollMultiplier(), 30f);
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
        if (keyCode == Keyboard.KEY_NEXT) { scroll.nudge(entriesView.h * .8f); }
    }
}
