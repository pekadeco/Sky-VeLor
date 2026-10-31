package com.arena.pekaclient.gui;

import com.arena.pekaclient.chat.ChatMenuPolicy;
import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.config.ChatQolSettings;
import com.arena.pekaclient.feature.ChatTabs;
import com.arena.pekaclient.render.GlobalFontManager;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import com.arena.pekaclient.widget.AppleButton;
import com.arena.pekaclient.widget.AppleDropdown;
import com.arena.pekaclient.widget.AppleSlider;
import com.arena.pekaclient.widget.AppleToggle;
import com.arena.pekaclient.widget.SearchField;
import com.arena.pekaclient.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Smaller active-font-measured editor for one Default Chat. There are no target selectors, Link all controls, split
 * windows, classifier rules, or pattern destinations: Background, Messages, Tabs, Menu and Highlight, and Reset
 * are the complete ownership surface.
 */
public final class ChatEditorScreen extends AppleScreen {

    private static final String[] SECTION_NAMES = {"Background", "Messages", "Tabs", "Menu and Highlight", "Reset"};

    private interface Label {
        String text();
    }

    private static final class Entry {
        final int section;
        final Label label;
        final Widget widget;

        Entry(int section, String label, Widget widget) {
            this(section, new FixedLabel(label), widget);
        }

        Entry(int section, Label label, Widget widget) {
            this.section = section;
            this.label = label;
            this.widget = widget;
        }
    }

    private static final class FixedLabel implements Label {
        private final String value;
        FixedLabel(String value) { this.value = value; }
        @Override public String text() { return value; }
    }

    private final GuiScreen parent;
    private final ChatEditorLayout.Layout layout = new ChatEditorLayout.Layout();
    private final GuiLayoutToolkit.Scroll editorScroll = new GuiLayoutToolkit.Scroll();
    private final boolean[] expanded = {true, true, true, true, true};
    private final List<Entry> entries = new ArrayList<Entry>();
    private final List<SearchField> fields = new ArrayList<SearchField>();

    private final SearchField backgroundColour = field("#000000");
    private final SearchField tabInactiveColour = field("#000000");
    private final SearchField tabBaseColour = field("#2F7DFF");
    private final SearchField tabName = field("Custom tab name");
    private final SearchField filterText = field("Filter text");
    private final SearchField sendPrefix = field("Optional send prefix, e.g. /pc");
    private final SearchField tabColour = field("#FF9F0A");
    private final SearchField sampleText = field("Sample line to test");
    private final SearchField hoverColour = field("#FFFFFF");
    private final SearchField compactCounterColour = field("#AAAAAA");

    private final AppleDropdown filterType;
    private final AppleDropdown menuMode;
    private final AppleToggle tabVisible;
    private final AppleToggle tabSound;
    private final List<AppleDropdown> dropdowns = new ArrayList<AppleDropdown>();

    private Widget active;
    private String editingTabId = ChatTabs.ALL;
    private String tabStatus = "Select a built-in tab or add a custom tab.";
    private boolean tabStatusError;
    private float x;
    private float y;
    private float w;
    private float h;
    private float viewX;
    private float viewY;
    private float viewW;
    private float viewH;
    private float fontHeight = 9f;
    private float rowHeight = ChatEditorLayout.ROW_H;
    private float headerHeight = ChatEditorLayout.HEADER_H;
    private final float[][] rowHeights = new float[ChatEditorLayout.SECTION_COUNT][];
    private final float[][] dropdownSpace = new float[ChatEditorLayout.SECTION_COUNT][];
    private long lastNs;

    public ChatEditorScreen(GuiScreen parent) {
        this.parent = parent;
        filterType = dropdown(new String[]{"Contains text", "Starts with", "Regex"}, () -> draftFilterType,
                value -> draftFilterType = ChatTabs.normalizeFilterType(value));
        menuMode = dropdown(new String[]{"Right click", "Left click", "Both"}, this::menuModeIndex,
                value -> ChatQolSettings.setMenuOpenMode(value == 1 ? ChatMenuPolicy.LEFT
                        : value == 2 ? ChatMenuPolicy.BOTH : ChatMenuPolicy.RIGHT));
        tabVisible = new AppleToggle(() -> currentTab() != null && currentTab().enabled(),
                value -> ChatQolSettings.setTabVisible(editingTabId, value));
        tabSound = new AppleToggle(() -> draftSound, value -> draftSound = value);
        buildEntries();
        syncFields(true);
    }

    private SearchField field(String placeholder) {
        SearchField value = new SearchField(placeholder);
        fields.add(value);
        return value;
    }

    private AppleDropdown dropdown(String[] options, java.util.function.IntSupplier getter,
                                   java.util.function.IntConsumer setter) {
        AppleDropdown value = new AppleDropdown(options, null, 150f, 22f, getter, setter);
        dropdowns.add(value);
        return value;
    }

    private void buildEntries() {
        entries.add(new Entry(ChatEditorLayout.BACKGROUND, "Background colour", backgroundColour));
        entries.add(new Entry(ChatEditorLayout.BACKGROUND, "Open opacity", slider(0, 100, 5, "%",
                () -> ChatQolSettings.backgroundOpacityOpen, value -> ChatQolSettings.backgroundOpacityOpen = value)));
        entries.add(new Entry(ChatEditorLayout.BACKGROUND, "Closed opacity", slider(0, 100, 5, "%",
                () -> ChatQolSettings.backgroundOpacityClosed, value -> ChatQolSettings.backgroundOpacityClosed = value)));
        entries.add(new Entry(ChatEditorLayout.BACKGROUND, "Corner radius", slider(0, 12, 1, " px",
                () -> ChatQolSettings.backgroundRadius, value -> ChatQolSettings.backgroundRadius = value)));
        entries.add(new Entry(ChatEditorLayout.BACKGROUND, "Open chat background", dropdown(
                new String[]{"Fit to messages", "Full area"},
                () -> ChatQolSettings.backgroundOpenFullArea ? 1 : 0,
                value -> ChatQolSettings.backgroundOpenFullArea = value == 1)));
        entries.add(new Entry(ChatEditorLayout.BACKGROUND, "Fit width to text", new AppleToggle(
                () -> ChatQolSettings.backgroundFitWidth, value -> ChatQolSettings.backgroundFitWidth = value)));

        entries.add(new Entry(ChatEditorLayout.MESSAGES, "Message animation", slider(0, 600, 20, " ms",
                () -> ChatQolSettings.messageAnimationMillis, value -> ChatQolSettings.messageAnimationMillis = value)
                .valueLabel(value -> value == 0 ? "Off" : value + " ms")));
        entries.add(new Entry(ChatEditorLayout.MESSAGES, "Fade-out", slider(0, 60, 1, " s",
                () -> ChatQolSettings.fadeSeconds, value -> ChatQolSettings.fadeSeconds = value)
                .valueLabel(value -> value == 0 ? "Off" : value + " s")));
        entries.add(new Entry(ChatEditorLayout.MESSAGES, "Use smooth messages", new AppleToggle(
                () -> ChatQolSettings.smoothMessages, value -> ChatQolSettings.smoothMessages = value)));
        entries.add(new Entry(ChatEditorLayout.MESSAGES, "Compact messages", new AppleToggle(
                () -> ChatQolSettings.compactMessages, value -> ChatQolSettings.compactMessages = value)));
        entries.add(new Entry(ChatEditorLayout.MESSAGES, "Compact counter colour", compactCounterColour));
        entries.add(new Entry(ChatEditorLayout.MESSAGES, "Chat length (256 characters)", new AppleToggle(
                () -> ChatQolSettings.extendedChatLength, value -> ChatQolSettings.extendedChatLength = value)));

        entries.add(new Entry(ChatEditorLayout.TABS, "Unselected tab colour", tabInactiveColour));
        entries.add(new Entry(ChatEditorLayout.TABS, "Selected tab colour", tabBaseColour));
        entries.add(new Entry(ChatEditorLayout.TABS, "Tab opacity", slider(0, 100, 1, "%",
                () -> ChatQolSettings.tabOpacity, value -> ChatQolSettings.tabOpacity = value)));
        entries.add(new Entry(ChatEditorLayout.TABS, "Active tab opacity", slider(0, 100, 1, "%",
                () -> ChatQolSettings.tabActiveOpacity, value -> ChatQolSettings.tabActiveOpacity = value)));
        entries.add(new Entry(ChatEditorLayout.TABS, new Label() {
            @Override public String text() { return "Editing: " + (currentTab() == null ? "All" : currentTab().name()); }
        }, new AppleButton("Previous", 74f, 22f, false, () -> cycleEditingTab(-1))));
        entries.add(new Entry(ChatEditorLayout.TABS, "Next tab", new AppleButton("Next", 60f, 22f, false,
                () -> cycleEditingTab(1))));
        entries.add(new Entry(ChatEditorLayout.TABS, "Visible", tabVisible));
        entries.add(new Entry(ChatEditorLayout.TABS, "Add custom tab", new AppleButton("Add", 60f, 22f, true,
                this::addCustomTab)));
        entries.add(new Entry(ChatEditorLayout.TABS, "Name", tabName));
        entries.add(new Entry(ChatEditorLayout.TABS, "Filter type", filterType));
        entries.add(new Entry(ChatEditorLayout.TABS, "Filter text", filterText));
        entries.add(new Entry(ChatEditorLayout.TABS, "Send prefix", sendPrefix));
        entries.add(new Entry(ChatEditorLayout.TABS, "Custom accent colour", tabColour));
        entries.add(new Entry(ChatEditorLayout.TABS, "Message sound", tabSound));
        entries.add(new Entry(ChatEditorLayout.TABS, "Test filter", new AppleButton("Test", 60f, 22f, false,
                this::testFilter)));
        entries.add(new Entry(ChatEditorLayout.TABS, "Save tab", new AppleButton("Save", 60f, 22f, true,
                this::saveTab)));
        entries.add(new Entry(ChatEditorLayout.TABS, "Move left", new AppleButton("←", 44f, 22f, false,
                () -> ChatQolSettings.moveTab(editingTabId, -1))));
        entries.add(new Entry(ChatEditorLayout.TABS, "Move right", new AppleButton("→", 44f, 22f, false,
                () -> ChatQolSettings.moveTab(editingTabId, 1))));
        entries.add(new Entry(ChatEditorLayout.TABS, "Delete custom", new AppleButton("Delete", 66f, 22f, false,
                this::deleteCustomTab)));

        entries.add(new Entry(ChatEditorLayout.MENU_HIGHLIGHT, "Highlight colour", hoverColour));
        entries.add(new Entry(ChatEditorLayout.MENU_HIGHLIGHT, "Highlight opacity", slider(0, 100, 5, "%",
                () -> ChatQolSettings.hoverOpacity, value -> ChatQolSettings.hoverOpacity = value)));
        entries.add(new Entry(ChatEditorLayout.MENU_HIGHLIGHT, "Open menu with", menuMode));

        entries.add(new Entry(ChatEditorLayout.RESET, "Reset background", new AppleButton("Reset", 64f, 22f, false,
                () -> { ChatQolSettings.resetBackground(); backgroundColour.setFocused(false); syncFields(false); })));
        entries.add(new Entry(ChatEditorLayout.RESET, "Reset messages", new AppleButton("Reset", 64f, 22f, false,
                () -> { ChatQolSettings.resetMessages(); syncFields(false); })));
        entries.add(new Entry(ChatEditorLayout.RESET, "Reset tabs", new AppleButton("Reset", 64f, 22f, false,
                () -> { ChatQolSettings.resetTabs(); editingTabId = ChatTabs.ALL; syncFields(true); })));
        entries.add(new Entry(ChatEditorLayout.RESET, "Reset menu/highlight", new AppleButton("Reset", 64f, 22f, false,
                () -> { ChatQolSettings.resetMenuHighlight(); hoverColour.setFocused(false); syncFields(false); })));
    }

    private AppleSlider slider(int min, int max, int step, String suffix, java.util.function.IntSupplier getter,
                               java.util.function.IntConsumer setter) {
        return new AppleSlider(min, max, step, suffix, 150f, getter, setter);
    }

    private ChatTabs.Tab currentTab() {
        ChatTabs.Tab tab = ChatQolSettings.tab(editingTabId);
        if (tab == null) {
            editingTabId = ChatTabs.ALL;
            tab = ChatQolSettings.tab(editingTabId);
        }
        return tab;
    }

    private void cycleEditingTab(int delta) {
        List<ChatTabs.Tab> tabs = ChatQolSettings.tabs();
        if (tabs.isEmpty()) return;
        int at = 0;
        for (int i = 0; i < tabs.size(); i++) if (tabs.get(i).id().equals(editingTabId)) { at = i; break; }
        int next = (at + delta) % tabs.size();
        if (next < 0) next += tabs.size();
        editingTabId = tabs.get(next).id();
        tabStatus = "Editing " + tabs.get(next).name() + ".";
        tabStatusError = false;
        syncFields(true);
    }

    private void addCustomTab() {
        ChatQolSettings.Result result = ChatQolSettings.addCustomTab("Custom tab");
        tabStatus = result.message;
        tabStatusError = !result.success;
        if (result.success) {
            editingTabId = result.id;
            syncFields(true);
        }
    }

    private void deleteCustomTab() {
        ChatTabs.Tab tab = currentTab();
        if (tab == null || tab.builtIn()) {
            tabStatus = "Built-in tabs cannot be deleted.";
            tabStatusError = true;
            return;
        }
        ChatQolSettings.deleteCustomTab(tab.id());
        editingTabId = ChatTabs.ALL;
        tabStatus = "Custom tab deleted.";
        tabStatusError = false;
        syncFields(true);
    }

    private void saveTab() {
        ChatTabs.Tab tab = currentTab();
        if (tab == null) return;
        if (tab.builtIn()) {
            ChatQolSettings.setTabVisible(tab.id(), tabVisibleState());
            tabStatus = tab.name() + " filter/prefix is built in; visibility was saved.";
            tabStatusError = false;
            return;
        }
        Integer colour = parseColour(tabColour.text());
        if (colour == null) {
            tabStatus = "Enter a valid tab colour such as #FF9F0A.";
            tabStatusError = true;
            return;
        }
        ChatQolSettings.Result result = ChatQolSettings.updateCustomTab(tab.id(), tabName.text(), filterTypeIndex(),
                filterText.text(), sendPrefix.text(), colour.intValue(), tabSoundState(), tabVisibleState());
        tabStatus = result.message;
        tabStatusError = !result.success;
        if (result.success) syncFields(true);
    }

    private void testFilter() {
        ChatTabs.Tab tab = currentTab();
        if (tab == null) return;
        ChatTabs.Tab draft = tab.copy();
        if (!tab.builtIn()) {
            if (!draft.setFilter(filterTypeIndex(), filterText.text())) {
                tabStatus = "Invalid regex: " + draft.validationError();
                tabStatusError = true;
                return;
            }
        }
        String sample = sampleText.text();
        if (sample.trim().length() == 0) {
            tabStatus = "Enter a sample line first.";
            tabStatusError = true;
            return;
        }
        boolean match = draft.matches(sample);
        tabStatus = match ? "Matches this tab; it also remains in All." : "Does not match this tab.";
        tabStatusError = false;
    }

    private boolean tabVisibleState() { return currentTab() != null && currentTab().enabled(); }
    private boolean tabSoundState() { return draftSound; }

    private int editingFilterIndex() {
        ChatTabs.Tab tab = currentTab();
        return tab == null ? ChatTabs.CONTAINS : tab.filterType();
    }

    private int filterTypeIndex() { return draftFilterType; }

    private int draftFilterType = ChatTabs.CONTAINS;
    private boolean draftSound;

    private int menuModeIndex() {
        return ChatQolSettings.menuOpenMode == ChatMenuPolicy.LEFT ? 1
                : ChatQolSettings.menuOpenMode == ChatMenuPolicy.BOTH ? 2 : 0;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        syncFields(true);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        commitColourFields();
        AppleSettings.save();
        super.onGuiClosed();
    }

    private void syncFields(boolean tabFields) {
        if (!backgroundColour.isFocused()) backgroundColour.setText(colourText(ChatQolSettings.backgroundColor));
        if (!tabInactiveColour.isFocused()) tabInactiveColour.setText(colourText(ChatQolSettings.tabInactiveColor));
        if (!tabBaseColour.isFocused()) tabBaseColour.setText(colourText(ChatQolSettings.tabBackgroundColor));
        if (!hoverColour.isFocused()) hoverColour.setText(colourText(ChatQolSettings.hoverColor));
        if (!compactCounterColour.isFocused()) compactCounterColour.setText(colourText(ChatQolSettings.compactCounterColor));
        if (!tabFields) return;
        ChatTabs.Tab tab = currentTab();
        if (tab == null) return;
        tabName.setText(tab.name());
        filterText.setText(tab.filterText());
        sendPrefix.setText(tab.sendPrefix());
        tabColour.setText(colourText(tab.colour()));
        draftFilterType = tab.filterType();
        draftSound = tab.sound();
    }

    private void commitColourFields() {
        Integer background = parseColour(backgroundColour.text());
        Integer tabInactive = parseColour(tabInactiveColour.text());
        Integer tabBase = parseColour(tabBaseColour.text());
        Integer hover = parseColour(hoverColour.text());
        Integer compactCounter = parseColour(compactCounterColour.text());
        if (background != null) ChatQolSettings.backgroundColor = background.intValue();
        if (tabInactive != null) ChatQolSettings.tabInactiveColor = tabInactive.intValue();
        if (tabBase != null) ChatQolSettings.tabBackgroundColor = tabBase.intValue();
        if (hover != null) ChatQolSettings.hoverColor = hover.intValue();
        if (compactCounter != null) ChatQolSettings.compactCounterColor = compactCounter.intValue();
    }

    private void updateLayout(float dt) {
        w = Math.max(156f, Math.min(width - 12f, 500f));
        h = Math.max(120f, Math.min(height - 12f, 390f));
        x = Math.max(6f, (width - w) * .5f);
        y = Math.max(6f, (height - h) * .5f);
        float active = GlobalFontManager.height(GlobalFontManager.Area.EDITOR);
        if (active < 0f) active = fr().FONT_HEIGHT;
        fontHeight = Math.max(1f, active);
        rowHeight = GuiLayoutToolkit.rowHeight(fontHeight * .82f, 1, Math.max(22f, fontHeight + 7f), 5f);
        headerHeight = Math.max(ChatEditorLayout.HEADER_H, fontHeight + 12f);
        int[] counts = sectionCounts();
        for (int section = 0; section < ChatEditorLayout.SECTION_COUNT; section++) {
            if (rowHeights[section] == null || rowHeights[section].length != counts[section]) {
                rowHeights[section] = new float[counts[section]];
                dropdownSpace[section] = new float[counts[section]];
            }
            for (int row = 0; row < counts[section]; row++) {
                rowHeights[section][row] = rowHeight;
                dropdownSpace[section][row] = 0f;
            }
        }
        int[] positions = new int[ChatEditorLayout.SECTION_COUNT];
        for (Entry entry : entries) {
            int row = positions[entry.section]++;
            if (entry.widget == openDropdown && entry.widget instanceof AppleDropdown) {
                dropdownSpace[entry.section][row] = ((AppleDropdown) entry.widget).reservedPopupHeight();
            }
        }
        viewX = x + 9f;
        viewY = y + 35f;
        viewW = Math.max(1f, w - 18f);
        viewH = Math.max(1f, h - 44f);
        ChatEditorLayout.update(layout, viewX, viewY, viewW, viewH, expanded, headerHeight, rowHeights,
                dropdownSpace, editorScroll.value());
        editorScroll.setMaximum(layout.maxScroll);
        editorScroll.update(dt, AppleSettings.smoothScrolling);
        ChatEditorLayout.update(layout, viewX, viewY, viewW, viewH, expanded, headerHeight, rowHeights,
                dropdownSpace, editorScroll.value());
        positionWidgets();
    }

    private int[] sectionCounts() {
        int[] counts = new int[ChatEditorLayout.SECTION_COUNT];
        for (Entry entry : entries) counts[entry.section]++;
        return counts;
    }

    private void positionWidgets() {
        int[] next = new int[ChatEditorLayout.SECTION_COUNT];
        for (Entry entry : entries) {
            int row = next[entry.section]++;
            ChatEditorLayout.Rect bounds = layout.rows[entry.section][row];
            float controlW = Math.max(56f, Math.min(Math.max(72f, bounds.w * .57f), bounds.w - 50f));
            entry.widget.w = Math.max(1f, controlW);
            entry.widget.h = Math.min(Math.max(1f, rowHeight - 6f), 24f);
            entry.widget.x = bounds.x + bounds.w - entry.widget.w;
            entry.widget.y = bounds.y + (bounds.h - entry.widget.h) * .5f;
        }
    }

    @Override
    public void drawScreen(int mx, int my, float partialTicks) {
        long now = System.nanoTime();
        float dt = lastNs == 0L ? 1f / 60f : Math.min(.1f, (now - lastNs) / 1.0e9f);
        lastNs = now;
        updateLayout(dt);
        for (Entry entry : entries) if (entryVisible(entry)) entry.widget.update(dt);
        drawDefaultBackground();
        UIRender.beginFrame();
        UIRender.rect(0f, 0f, width, height, Theme.veil);
        UIRender.shadow(x, y, w, h, Math.min(Theme.radius, 12f));
        UIRender.roundRect(x, y, w, h, Theme.radius, Theme.windowTop, Theme.windowBottom, .6f);
        UIRender.roundRectRing(x, y, w, h, Theme.radius, 1f, Theme.border);
        UIRender.text(fr(), "Chat Editor", x + 9f, y + 10f, Theme.textPrimary, 1f, false,
                GlobalFontManager.Area.EDITOR);
        UIRender.text(fr(), "Done", x + w - 37f, y + 10f, Theme.accent, .82f, false,
                GlobalFontManager.Area.EDITOR);
        if (tabStatus.length() > 0) {
            UIRender.text(fr(), fitActive(tabStatus, Math.max(1f, w - 90f), .64f), x + 9f, y + 23f,
                    tabStatusError ? 0xFFFF453A : Theme.textTertiary, .64f, false, GlobalFontManager.Area.EDITOR);
        }
        UIRender.rect(x + 1f, y + 34f, w - 2f, 1f, Theme.divider);
        UIRender.scissorBegin(viewX, viewY, viewW, viewH, Minecraft.getMinecraft().gameSettings.guiScale == 0
                ? 1f : new net.minecraft.client.gui.ScaledResolution(Minecraft.getMinecraft()).getScaleFactor());
        drawSectionsAndEntries(mx, my);
        UIRender.scissorEnd();
        if (openDropdown != null) {
            UIRender.scissorBegin(viewX, viewY, viewW, viewH,
                    new net.minecraft.client.gui.ScaledResolution(Minecraft.getMinecraft()).getScaleFactor());
            openDropdown.drawPopup(mx, my, viewY, viewY + viewH, x + w, x);
            UIRender.scissorEnd();
        }
        UIRender.endFrame();
    }

    private void drawSectionsAndEntries(int mx, int my) {
        for (int section = 0; section < ChatEditorLayout.SECTION_COUNT; section++) {
            ChatEditorLayout.Rect header = layout.headers[section];
            boolean hot = inside(mx, my, header.x, header.y, header.w, header.h);
            UIRender.roundRectBorder(header.x, header.y, header.w, header.h, 5f,
                    hot ? Theme.cardHover : Theme.card, hot ? Theme.cardHover : Theme.card, Theme.border);
            UIRender.text(fr(), (expanded[section] ? "▾  " : "▸  ") + SECTION_NAMES[section], header.x + 8f,
                    header.y + (header.h - fontHeight * .85f) * .5f, expanded[section] ? Theme.textPrimary : Theme.textSecondary,
                    .85f, false, GlobalFontManager.Area.EDITOR);
        }
        int[] next = new int[ChatEditorLayout.SECTION_COUNT];
        for (Entry entry : entries) {
            int row = next[entry.section]++;
            if (!entryVisible(entry)) continue;
            ChatEditorLayout.Rect bounds = layout.rows[entry.section][row];
            float labelW = Math.max(1f, entry.widget.x - bounds.x - 7f);
            UIRender.text(fr(), fitActive(entry.label.text(), labelW, .76f), bounds.x,
                    entry.widget.y + (entry.widget.h - fontHeight * .76f) * .5f, Theme.textSecondary, .76f, false,
                    GlobalFontManager.Area.EDITOR);
            entry.widget.draw(mx, my);
        }
    }

    private boolean entryVisible(Entry entry) {
        ChatEditorLayout.Rect body = layout.bodies[entry.section];
        return expanded[entry.section] && body.h > 0f && entry.widget.y + entry.widget.h > viewY
                && entry.widget.y < viewY + viewH;
    }

    private String fitActive(String value, float max, final float scale) {
        return GuiLayoutToolkit.ellipsize(value, max, new GuiLayoutToolkit.TextMetrics() {
            @Override public float width(String text) {
                float width = GlobalFontManager.width(text, GlobalFontManager.Area.EDITOR);
                return (width < 0f ? fr().getStringWidth(text) : width) * scale;
            }
            @Override public float height() { return fontHeight * scale; }
        });
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        if (button != 0) return;
        if (handleOpenDropdownClick(mx, my)) return;
        if (inside(mx, my, x + w - 52f, y, 52f, 34f)) {
            close();
            return;
        }
        for (int section = 0; section < ChatEditorLayout.SECTION_COUNT; section++) {
            ChatEditorLayout.Rect header = layout.headers[section];
            if (inside(mx, my, header.x, header.y, header.w, header.h)) {
                expanded[section] = !expanded[section];
                return;
            }
        }
        Entry picked = null;
        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry entry = entries.get(i);
            if (entryVisible(entry) && entry.widget.contains(mx, my)) { picked = entry; break; }
        }
        for (SearchField field : fields) field.setFocused(picked != null && picked.widget == field);
        active = picked == null ? null : picked.widget;
        if (active != null) active.onPress(mx, my);
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
        commitColourFields();
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long elapsed) {
        if (active != null) active.onDrag(mx, my);
        super.mouseClickMove(mx, my, button, elapsed);
    }

    @Override
    public void handleMouseInput() throws IOException {
        int wheel = Mouse.getEventDWheel();
        super.handleMouseInput();
        int mx = Mouse.getEventX() * width / Minecraft.getMinecraft().displayWidth;
        int my = height - Mouse.getEventY() * height / Minecraft.getMinecraft().displayHeight - 1;
        if (wheel != 0 && openDropdown == null && inside(mx, my, viewX, viewY, viewW, viewH)) {
            boolean fine = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
            editorScroll.wheel(wheel / 120f, fine, AppleSettings.guiScrollMultiplier(), rowHeight);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        for (SearchField field : fields) {
            if (field.isFocused() && field.keyTyped(typedChar, keyCode)) return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (openDropdown != null) openDropdown = null; else close();
            return;
        }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            commitColourFields();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private static Integer parseColour(String source) {
        if (source == null) return null;
        String value = source.trim();
        if (value.startsWith("#")) value = value.substring(1);
        if (value.startsWith("0x") || value.startsWith("0X")) value = value.substring(2);
        try {
            int parsed = (int) Long.parseLong(value, 16);
            return parsed >= 0 && parsed <= 0xFFFFFF ? Integer.valueOf(parsed) : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String colourText(int colour) {
        String value = Integer.toHexString(colour & 0xFFFFFF).toUpperCase(java.util.Locale.ROOT);
        StringBuilder out = new StringBuilder("#");
        for (int i = value.length(); i < 6; i++) out.append('0');
        return out.append(value).toString();
    }

    private void close() {
        commitColourFields();
        Minecraft.getMinecraft().displayGuiScreen(parent);
    }
}
