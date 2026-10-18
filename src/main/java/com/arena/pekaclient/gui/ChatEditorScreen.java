package com.arena.pekaclient.gui;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.config.ChatQolSettings;
import com.arena.pekaclient.feature.ChatFilterRules;
import com.arena.pekaclient.render.GlobalFontManager;
import com.arena.pekaclient.feature.ChatPatternRules;
import com.arena.pekaclient.feature.ChatWindowLayout;
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
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Native Apple-style editor for the compact Chat QoL feature. The hub exposes only feature switches; every
 * chat-specific visual, classification, window and interaction adjustment lives here. Font and scroll policy route
 * to their one top-level global settings home.
 */
public final class ChatEditorScreen extends AppleScreen {

    private static final String[] SECTION_NAMES = {
            "Windows", "Background", "Messages", "Global settings", "Tabs", "Hover / menu"
    };
    private static final int[] SECTION_ROWS = {5, 6, 14, 2, 5, 3};
    private static final String[] TARGET_OPTIONS = {"Default Chat", "Chat 1", "Chat 2"};
    private static final String[] APPEAR_OPTIONS = {"Vanilla", "Slide", "Fade"};
    private static final String[] ORDER_OPTIONS = {
            "All, Party, Reply", "All, Reply, Party", "Party, All, Reply",
            "Party, Reply, All", "Reply, All, Party", "Reply, Party, All"
    };
    private static final String[] ORDER_VALUES = {
            "all,party,reply", "all,reply,party", "party,all,reply",
            "party,reply,all", "reply,all,party", "reply,party,all"
    };

    private static final class Entry {
        final int section;
        final String label;
        final Widget widget;

        Entry(int section, String label, Widget widget) {
            this.section = section;
            this.label = label;
            this.widget = widget;
        }
    }

    private final GuiScreen parent;
    private final ChatEditorLayout.Layout layout = new ChatEditorLayout.Layout();
    private final GuiLayoutToolkit.Rect safePanel = new GuiLayoutToolkit.Rect();
    private final boolean[] expanded = {true, false, false, false, false, false};
    private final List<Entry> entries = new ArrayList<Entry>();
    private final List<SearchField> fields = new ArrayList<SearchField>();
    private final SearchField chat1Name = new SearchField("Chat 1 name");
    private final SearchField chat2Name = new SearchField("Chat 2 name");
    private final SearchField patternExpression = new SearchField("Regular expression, blank tests defaults");
    private final SearchField patternSample = new SearchField("Sample message to classify");
    private final SearchField textColour = new SearchField("#FFFFFF");
    private final SearchField backgroundColour = new SearchField("#15171B");
    private final SearchField borderColour = new SearchField("#4A4A50");
    private final SearchField hoverColour = new SearchField("#64D2FF");
    private final AppleDropdown patternTarget;
    private final AppleDropdown tabOrderChoice;
    private final AppleDropdown targetChoice;
    private final AppleDropdown copyFromChoice;
    private final AppleDropdown appearChoice;
    private final AppleToggle linkAllToggle;

    private final ChatWindowLayout.Rect chat1Rect = new ChatWindowLayout.Rect();
    private final ChatWindowLayout.Rect chat2Rect = new ChatWindowLayout.Rect();

    private Widget active;
    private float scroll;
    private float wx;
    private float wy;
    private float ww;
    private float wh;
    private float viewX;
    private float viewY;
    private float viewW;
    private float viewH;
    private float sf = 1f;
    private boolean arrangingWindows;
    private ChatQolSettings.Window draggedWindow;
    private boolean resizingWindow;
    private float grabX;
    private float grabY;
    private String patternStatus = "Add a rule, then use Test before saving it.";
    private long lastNs;
    private boolean draggingEditor;
    private float editorGrabX;
    private float editorGrabY;
    private long lastEditorTitleClick;
    private int draggedSection = -1;
    private float sectionGrabX;
    private float sectionGrabY;
    private final long[] lastSectionTitleClick = new long[ChatEditorLayout.SECTION_COUNT];


    public ChatEditorScreen(GuiScreen parent) {
        this.parent = parent;
        fields.add(chat1Name);
        fields.add(chat2Name);
        fields.add(patternExpression);
        fields.add(patternSample);
        fields.add(textColour);
        fields.add(backgroundColour);
        fields.add(borderColour);
        fields.add(hoverColour);
        patternTarget = new AppleDropdown(new String[]{"Chat 1 player", "Chat 2 system"}, null, 120f, 22f,
                () -> ChatPatternRules.CHAT_2.equals(patternTargetValue()) ? 1 : 0,
                value -> patternTargetValue(value == 1 ? ChatPatternRules.CHAT_2 : ChatPatternRules.CHAT_1));
        targetChoice = new AppleDropdown(TARGET_OPTIONS, null, 112f, 22f,
                () -> ChatQolSettings.editorTarget, ChatQolSettings::setEditorTarget);
        copyFromChoice = new AppleDropdown(TARGET_OPTIONS, null, 112f, 22f,
                () -> ChatQolSettings.editorTarget, value -> {
                    ChatQolSettings.copyAppearanceFrom(ChatQolSettings.editorTarget, value);
                    syncAppearanceFields();
                });
        appearChoice = new AppleDropdown(APPEAR_OPTIONS, null, 112f, 22f, this::appearChoiceIndex,
                value -> ChatQolSettings.editAppearance(ChatQolSettings.editorTarget,
                        appearance -> appearance.setAppearStyle(value == 1 ? "SLIDE" : value == 2 ? "FADE" : "VANILLA")));
        linkAllToggle = new AppleToggle(() -> ChatQolSettings.linkAll, value -> ChatQolSettings.linkAll = value);
        tabOrderChoice = new AppleDropdown(ORDER_OPTIONS, null, 148f, 22f, this::tabOrderIndex,
                value -> ChatQolSettings.setTabOrder(ORDER_VALUES[Math.max(0, Math.min(value, ORDER_VALUES.length - 1))]));
        buildEntries();
    }

    private String targetRule = ChatPatternRules.CHAT_1;

    private String patternTargetValue() {
        return targetRule;
    }

    private void patternTargetValue(String target) {
        targetRule = ChatPatternRules.normalizeTarget(target);
    }

    private void buildEntries() {
        // Window placement stays separate from visual target settings so the legacy Chat 1/Chat 2 geometry survives.
        entries.add(new Entry(ChatEditorLayout.WINDOWS, "Snap to edges", new AppleToggle(
                () -> ChatQolSettings.snapWindows, value -> ChatQolSettings.snapWindows = value)));
        entries.add(new Entry(ChatEditorLayout.WINDOWS, "Chat 1 name", chat1Name));
        entries.add(new Entry(ChatEditorLayout.WINDOWS, "Chat 2 name", chat2Name));
        entries.add(new Entry(ChatEditorLayout.WINDOWS, "Arrange windows", new AppleButton("Arrange", 82f, 22f,
                true, () -> arrangingWindows = !arrangingWindows)));
        entries.add(new Entry(ChatEditorLayout.WINDOWS, "Reset windows", new AppleButton("Reset", 64f, 22f,
                false, ChatQolSettings::resetWindows)));

        entries.add(new Entry(ChatEditorLayout.BACKGROUND, "Background colour", backgroundColour));
        entries.add(new Entry(ChatEditorLayout.BACKGROUND, "Opacity", new AppleSlider(0, 100, 5, "%", 142f,
                () -> appearance().backgroundOpacity(), value -> editAppearance(a -> a.setBackgroundOpacity(value)))));
        entries.add(new Entry(ChatEditorLayout.BACKGROUND, "Corner radius", new AppleSlider(0, 16, 1, " px", 142f,
                () -> appearance().radius(), value -> editAppearance(a -> a.setRadius(value)))));
        entries.add(new Entry(ChatEditorLayout.BACKGROUND, "Border colour", borderColour));
        entries.add(new Entry(ChatEditorLayout.BACKGROUND, "Border opacity", new AppleSlider(0, 100, 5, "%", 142f,
                () -> appearance().borderOpacity(), value -> editAppearance(a -> a.setBorderOpacity(value)))));
        entries.add(new Entry(ChatEditorLayout.BACKGROUND, "Reset background", new AppleButton("Reset", 64f, 22f,
                false, ChatQolSettings::resetBackground)));

        entries.add(new Entry(ChatEditorLayout.MESSAGES, "Text colour", textColour));
        entries.add(new Entry(ChatEditorLayout.MESSAGES, "Text scale", new AppleSlider(50, 200, 5, "%", 142f,
                () -> appearance().textScale(), value -> editAppearance(a -> a.setTextScale(value)))));
        entries.add(new Entry(ChatEditorLayout.MESSAGES, "Line spacing", new AppleSlider(-2, 12, 1, " px", 142f,
                () -> appearance().lineSpacing(), value -> editAppearance(a -> a.setLineSpacing(value)))));
        entries.add(new Entry(ChatEditorLayout.MESSAGES, "Text shadow", new AppleToggle(
                () -> appearance().shadow(), value -> editAppearance(a -> a.setShadow(value)))));
        entries.add(new Entry(ChatEditorLayout.MESSAGES, "Appear style", appearChoice));
        entries.add(new Entry(ChatEditorLayout.MESSAGES, "Appear speed", new AppleSlider(0, 1000, 20, " ms", 142f,
                () -> appearance().appearSpeed(), value -> editAppearance(a -> a.setAppearSpeed(value)))));
        entries.add(new Entry(ChatEditorLayout.MESSAGES, "Fade-out", new AppleSlider(0, 60, 1, " s", 142f,
                () -> appearance().fadeSeconds(), value -> editAppearance(a -> a.setFadeSeconds(value)))));
        entries.add(new Entry(ChatEditorLayout.MESSAGES, "Chat 1 lines", new AppleSlider(10, 300, 5, "", 142f,
                () -> ChatQolSettings.chat1().getMessageLimit(), value -> ChatQolSettings.chat1().setMessageLimit(value))));
        entries.add(new Entry(ChatEditorLayout.MESSAGES, "Chat 2 lines", new AppleSlider(10, 300, 5, "", 142f,
                () -> ChatQolSettings.chat2().getMessageLimit(), value -> ChatQolSettings.chat2().setMessageLimit(value))));
        entries.add(new Entry(ChatEditorLayout.MESSAGES, "Rule target", patternTarget));
        entries.add(new Entry(ChatEditorLayout.MESSAGES, "Rule expression", patternExpression));
        entries.add(new Entry(ChatEditorLayout.MESSAGES, "Rule sample", patternSample));
        entries.add(new Entry(ChatEditorLayout.MESSAGES, "Test / add", new AppleButton("Test + add", 82f, 22f,
                true, this::testAndAddPattern)));

        // Global controls are linked, never duplicated, so one wheel/font policy governs every editor and chat.
        entries.add(new Entry(ChatEditorLayout.FONT, "Mod Settings", new AppleButton("Open", 74f, 22f, false,
                () -> Minecraft.getMinecraft().displayGuiScreen(new ModSettingsScreen(this)))));
        entries.add(new Entry(ChatEditorLayout.FONT, "Fonts", new AppleButton("Open", 74f, 22f, false,
                () -> Minecraft.getMinecraft().displayGuiScreen(new FontsScreen(this)))));

        entries.add(new Entry(ChatEditorLayout.TABS, "Show All", new AppleToggle(
                () -> ChatQolSettings.showAll, value -> ChatQolSettings.setTabVisible(ChatQolSettings.TAB_ALL, value))));
        entries.add(new Entry(ChatEditorLayout.TABS, "Show Party", new AppleToggle(
                () -> ChatQolSettings.showParty, value -> ChatQolSettings.setTabVisible(ChatQolSettings.TAB_PARTY, value))));
        entries.add(new Entry(ChatEditorLayout.TABS, "Show Reply", new AppleToggle(
                () -> ChatQolSettings.showReply, value -> ChatQolSettings.setTabVisible(ChatQolSettings.TAB_REPLY, value))));
        entries.add(new Entry(ChatEditorLayout.TABS, "Tab order", tabOrderChoice));
        entries.add(new Entry(ChatEditorLayout.TABS, "Reset tabs", new AppleButton("Reset", 64f, 22f,
                false, ChatQolSettings::resetTabs)));

        entries.add(new Entry(ChatEditorLayout.HOVER_MENU, "Hover colour", hoverColour));
        entries.add(new Entry(ChatEditorLayout.HOVER_MENU, "Highlight opacity", new AppleSlider(0, 100, 5, "%", 142f,
                () -> appearance().hoverOpacity(), value -> editAppearance(a -> a.setHoverOpacity(value)))));
        entries.add(new Entry(ChatEditorLayout.HOVER_MENU, "Reset hover/menu", new AppleButton("Reset", 64f, 22f,
                false, ChatQolSettings::resetHoverMenu)));
    }

    private ChatQolSettings.Appearance appearance() {
        return ChatQolSettings.appearance(ChatQolSettings.editorTarget);
    }

    private void editAppearance(ChatQolSettings.AppearanceEdit edit) {
        ChatQolSettings.editAppearance(ChatQolSettings.editorTarget, edit);
    }

    private void testAndAddPattern() {
        String expression = patternExpression.text().trim();
        String sample = patternSample.text().trim();
        if (sample.length() == 0) {
            patternStatus = "Enter a sample to test the current default rules or your new rule.";
            return;
        }
        if (expression.length() == 0) {
            patternStatus = "Default result: " + ChatFilterRules.displayName(
                    ChatFilterRules.classify(sample, ChatQolSettings.compiledPatterns())) + ".";
            return;
        }
        if (!ChatPatternRules.isValid(expression)) {
            patternStatus = "Invalid rule: keep it under 256 characters.";
            return;
        }
        List<ChatPatternRules.Rule> rules = new ArrayList<ChatPatternRules.Rule>(ChatQolSettings.patternRules());
        if (rules.size() >= 16) {
            patternStatus = "Rule limit reached (16). Reset Messages to restore defaults.";
            return;
        }
        rules.add(new ChatPatternRules.Rule(targetRule, expression));
        ChatPatternRules tested = new ChatPatternRules(rules);
        int result = ChatFilterRules.classify(sample, tested);
        ChatQolSettings.setPatternRules(rules);
        patternStatus = "Test: " + ChatFilterRules.displayName(result) + "; rule added to "
                + (ChatPatternRules.CHAT_1.equals(targetRule) ? "Chat 1" : "Chat 2") + ".";
        patternExpression.clear();
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        syncFields();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        commitFields();
        AppleSettings.save();
        super.onGuiClosed();
    }

    private void syncFields() {
        if (!chat1Name.isFocused()) chat1Name.setText(ChatQolSettings.chat1().getName());
        if (!chat2Name.isFocused()) chat2Name.setText(ChatQolSettings.chat2().getName());
        syncAppearanceFields();
    }

    private void syncAppearanceFields() {
        ChatQolSettings.Appearance current = appearance();
        if (!textColour.isFocused()) textColour.setText(colourText(current.textColor()));
        if (!backgroundColour.isFocused()) backgroundColour.setText(colourText(current.backgroundColor()));
        if (!borderColour.isFocused()) borderColour.setText(colourText(current.borderColor()));
        if (!hoverColour.isFocused()) hoverColour.setText(colourText(current.hoverColor()));
    }

    private void commitFields() {
        ChatQolSettings.chat1().setName(chat1Name.text());
        ChatQolSettings.chat2().setName(chat2Name.text());
        Integer text = parseColour(textColour.text());
        Integer background = parseColour(backgroundColour.text());
        Integer border = parseColour(borderColour.text());
        Integer hover = parseColour(hoverColour.text());
        if (text != null) editAppearance(a -> a.setTextColor(text));
        if (background != null) editAppearance(a -> a.setBackgroundColor(background));
        if (border != null) editAppearance(a -> a.setBorderColor(border));
        if (hover != null) editAppearance(a -> a.setHoverColor(hover));
        syncAppearanceFields();
    }

    private static Integer parseColour(String source) {
        if (source == null) return null;
        String value = source.trim();
        if (value.startsWith("#")) value = value.substring(1);
        if (value.startsWith("0x") || value.startsWith("0X")) value = value.substring(2);
        try {
            int result = (int) Long.parseLong(value, 16);
            return result >= 0 && result <= 0xFFFFFF ? Integer.valueOf(result) : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String colourText(int value) {
        String hex = Integer.toHexString(value & 0xFFFFFF).toUpperCase(java.util.Locale.ROOT);
        StringBuilder out = new StringBuilder("#");
        for (int i = hex.length(); i < 6; i++) out.append('0');
        return out.append(hex).toString();
    }

    private int appearChoiceIndex() {
        String style = appearance().appearStyle();
        return "SLIDE".equals(style) ? 1 : "FADE".equals(style) ? 2 : 0;
    }

    private int tabOrderIndex() {
        String value = ChatQolSettings.tabOrder;
        for (int i = 0; i < ORDER_VALUES.length; i++) if (ORDER_VALUES[i].equals(value)) return i;
        return 0;
    }

    private void updateLayout() {
        ww = Math.max(150f, Math.min(width - 12f, 470f));
        wh = Math.max(116f, Math.min(height - 12f, 372f));
        int guiScale = Math.max(1, Math.min(4, Math.round(sf)));
        ChatEditorPanelLayout.Rect panel = new ChatEditorPanelLayout.Rect();
        ChatEditorPanelLayout.resolve(panel, ChatQolSettings.editorPanel(0), guiScale, ww, wh, width, height);
        GuiLayoutToolkit.clampPanel(safePanel, panel.x, panel.y, panel.w, panel.h, width, height);
        wx = safePanel.x;
        wy = safePanel.y;
        ww = safePanel.w;
        wh = safePanel.h;
        viewX = wx + 10f;
        viewY = wy + 66f;
        viewW = Math.max(1f, ww - 20f);
        viewH = Math.max(1f, wh - 76f);
        for (int i = 0; i < expanded.length; i++) {
            expanded[i] = !ChatQolSettings.editorPanel(i + 1).collapsed(guiScale);
        }
        ChatEditorLayout.update(layout, viewX, viewY, viewW, viewH, expanded, SECTION_ROWS, scroll);
        applySectionPanelOffsets(guiScale);
        scroll = layout.scroll;
        positionControls();
        targetChoice.x = wx + 56f;
        targetChoice.y = wy + 38f;
        linkAllToggle.x = wx + 212f;
        linkAllToggle.y = wy + 39f;
        copyFromChoice.x = Math.min(wx + ww - copyFromChoice.w - 9f, wx + 334f);
        copyFromChoice.y = wy + 38f;
    }

    /** Applies each persisted panel offset after the safe stacked baseline is calculated. */
    private void applySectionPanelOffsets(int guiScale) {
        for (int section = 0; section < ChatEditorLayout.SECTION_COUNT; section++) {
            ChatEditorLayout.Rect header = layout.headers[section];
            ChatEditorLayout.Rect body = layout.bodies[section];
            ChatEditorPanelLayout.State state = ChatQolSettings.editorPanel(section + 1);
            float cardW = Math.max(1f, layout.viewport.w - 12f);
            float cardH = header.h + (body.h > 0f ? ChatEditorLayout.GAP + body.h : 0f);
            float baseX = layout.viewport.x + (layout.viewport.w - cardW) * 0.5f;
            float dx = (state.x(guiScale) - 5000) * layout.viewport.w / 10000f;
            float dy = (state.y(guiScale) - 5000) * layout.viewport.h / 10000f;
            float x = clampPanel(baseX + dx, layout.viewport.x, layout.viewport.x + layout.viewport.w - cardW);
            // A tall expanded card starts at the viewport edge; collapsing restores a compact draggable title card.
            float effectiveH = Math.min(cardH, layout.viewport.h);
            float y = clampPanel(header.y + dy, layout.viewport.y, layout.viewport.y + layout.viewport.h - effectiveH);
            header.x = x;
            header.y = y;
            header.w = cardW;
            if (body.h > 0f) {
                body.x = x + ChatEditorLayout.PAD;
                body.y = y + header.h + ChatEditorLayout.GAP;
                body.w = Math.max(1f, cardW - ChatEditorLayout.PAD * 2f);
            } else {
                body.x = x + ChatEditorLayout.PAD;
                body.y = y + header.h;
                body.w = Math.max(1f, cardW - ChatEditorLayout.PAD * 2f);
            }
        }
    }

    private static float clampPanel(float value, float min, float max) {
        return value < min ? min : (value > max ? max : value);
    }

    private void positionControls() {
        int[] nextRow = new int[ChatEditorLayout.SECTION_COUNT];
        for (Entry entry : entries) {
            ChatEditorLayout.Rect body = layout.bodies[entry.section];
            int row = nextRow[entry.section]++;
            float controlW = entry.widget instanceof AppleSlider || entry.widget instanceof SearchField
                    || entry.widget instanceof AppleDropdown ? Math.max(72f, body.w * 0.58f) : entry.widget.w;
            entry.widget.w = Math.min(controlW, Math.max(1f, body.w));
            entry.widget.x = body.x + body.w - entry.widget.w;
            entry.widget.y = body.y + row * ChatEditorLayout.ROW_H + (ChatEditorLayout.ROW_H - entry.widget.h) * 0.5f;
        }
    }

    private boolean entryVisible(Entry entry) {
        ChatEditorLayout.Rect body = layout.bodies[entry.section];
        return expanded[entry.section] && body.h > 0f && entry.widget.y + entry.widget.h > viewY
                && entry.widget.y < viewY + viewH;
    }

    private void updateWidgets(float dt) {
        syncFields();
        targetChoice.update(dt);
        linkAllToggle.update(dt);
        copyFromChoice.update(dt);
        for (Entry entry : entries) {
            if (entryVisible(entry)) entry.widget.update(dt);
        }
    }

    @Override
    public void drawScreen(int mx, int my, float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        long now = System.nanoTime();
        float dt = lastNs == 0L ? 1f / 60f : Math.min(0.1f, (now - lastNs) / 1.0e9f);
        lastNs = now;
        sf = new ScaledResolution(mc).getScaleFactor();
        updateLayout();
        updateWidgets(dt);
        drawDefaultBackground();

        int q = AppleSettings.experimentalPerformance ? 2 : AppleSettings.curveQuality;
        UIRender.segCap = AppleSettings.CURVE_SEGMENTS[q];
        UIRender.shadows = AppleSettings.shadows && !AppleSettings.experimentalPerformance;
        UIRender.beginFrame();
        UIRender.rect(0f, 0f, width, height, Theme.veil);
        drawArrangePreview(mx, my);
        drawWindow(mx, my);
        UIRender.endFrame();
    }

    private void drawArrangePreview(int mx, int my) {
        int scale = new ScaledResolution(Minecraft.getMinecraft()).getScaleFactor();
        ChatWindowLayout.update(chat1Rect, ChatQolSettings.chat1(), scale, width, height);
        ChatWindowLayout.update(chat2Rect, ChatQolSettings.chat2(), scale, width, height);
        if (!arrangingWindows) return;
        drawPreviewWindow(chat1Rect, ChatQolSettings.chat1().getName(), ChatQolSettings.TARGET_CHAT_1, mx, my);
        drawPreviewWindow(chat2Rect, ChatQolSettings.chat2().getName(), ChatQolSettings.TARGET_CHAT_2, mx, my);
        UIRender.text(fr(), "Arrange: drag a window; drag its lower-right corner to resize; edge snap is optional.",
                8f, 8f, Theme.textPrimary, 0.85f, true);
    }

    private void drawPreviewWindow(ChatWindowLayout.Rect rect, String name, int target, int mx, int my) {
        ChatQolSettings.Appearance appearance = ChatQolSettings.appearance(target);
        boolean hot = rect.contains(mx, my);
        int alpha = Math.round(appearance.backgroundOpacity() * (hot ? 1.25f : 1f) * 2.55f);
        alpha = Math.max(0, Math.min(255, alpha));
        int fill = (alpha << 24) | appearance.backgroundColor();
        int border = (Math.max(0, Math.min(255, Math.round(appearance.borderOpacity() * 2.55f))) << 24)
                | appearance.borderColor();
        UIRender.roundRectBorder(rect.x, rect.y, rect.w, rect.h, Math.min(appearance.radius(), 6f),
                fill, fill, border);
        UIRender.text(fr(), fit(fr(), name, rect.w - 12f), rect.x + 6f, rect.y + 6f,
                0xFF000000 | appearance.textColor(), appearance.textScale() / 100f, appearance.shadow(),
                GlobalFontManager.Area.EDITOR);
        UIRender.line(rect.x + rect.w - 8f, rect.y + rect.h - 2f, rect.x + rect.w - 2f, rect.y + rect.h - 8f,
                1f, border);
    }

    private void drawWindow(int mx, int my) {
        UIRender.shadow(wx, wy, ww, wh, Math.min(Theme.radius, 12f));
        UIRender.roundRect(wx, wy, ww, wh, Theme.radius, Theme.windowTop, Theme.windowBottom, 0.6f);
        UIRender.roundRectRing(wx, wy, ww, wh, Theme.radius, 1f, Theme.border);
        UIRender.text(fr(), "Chat Editor", wx + 14f, wy + 12f, Theme.textPrimary, 1.0f, false,
                GlobalFontManager.Area.EDITOR);
        String mode = arrangingWindows ? "Arranging windows" : "Drag title to move · double-click title to reset";
        UIRender.text(fr(), mode, wx + 14f, wy + 25f, arrangingWindows ? Theme.accent : Theme.textSecondary, 0.67f, false,
                GlobalFontManager.Area.EDITOR);
        UIRender.text(fr(), "Done", wx + ww - 40f, wy + 13f, Theme.accent, 0.85f, false,
                GlobalFontManager.Area.EDITOR);
        UIRender.rect(wx + 1f, wy + 34f, ww - 2f, 1f, Theme.divider);
        UIRender.text(fr(), "Target", wx + 12f, wy + 46f, Theme.textSecondary, 0.72f, false,
                GlobalFontManager.Area.EDITOR);
        targetChoice.draw(mx, my);
        UIRender.text(fr(), "Link all", wx + 258f, wy + 46f, Theme.textSecondary, 0.72f, false,
                GlobalFontManager.Area.EDITOR);
        linkAllToggle.draw(mx, my);
        UIRender.text(fr(), "Copy from", copyFromChoice.x - 57f, wy + 46f, Theme.textSecondary, 0.66f, false,
                GlobalFontManager.Area.EDITOR);
        copyFromChoice.draw(mx, my);
        UIRender.rect(wx + 1f, wy + 62f, ww - 2f, 1f, Theme.divider);

        UIRender.scissorBegin(viewX, viewY, viewW, viewH, sf);
        for (int section = 0; section < ChatEditorLayout.SECTION_COUNT; section++) {
            drawSection(section, mx, my);
        }
        for (Entry entry : entries) {
            if (!entryVisible(entry)) continue;
            float labelW = Math.max(0f, entry.widget.x - 8f - (layout.bodies[entry.section].x));
            String label = fit(fr(), entry.label, labelW);
            UIRender.text(fr(), label, layout.bodies[entry.section].x,
                    entry.widget.y + (entry.widget.h - 8f) * 0.5f + 1f, Theme.textSecondary, 0.82f, false);
            entry.widget.draw(mx, my);
        }
        if (expanded[ChatEditorLayout.MESSAGES]) {
            ChatEditorLayout.Rect body = layout.bodies[ChatEditorLayout.MESSAGES];
            float infoY = body.y + 13 * ChatEditorLayout.ROW_H + 8f;
            UIRender.text(fr(), fit(fr(), "Rules: " + ChatQolSettings.patternRuleCount() + " — " + patternStatus,
                    body.w), body.x, infoY, Theme.textTertiary, 0.68f, false);
        }
        UIRender.scissorEnd();
        if (openDropdown != null) {
            openDropdown.drawPopup(mx, my, wy, wy + wh, wx + ww, wx);
        }
    }

    private void drawSection(int index, int mx, int my) {
        ChatEditorLayout.Rect header = layout.headers[index];
        boolean hot = inside(mx, my, header.x, header.y, header.w, header.h);
        int fill = hot ? Theme.cardHover : Theme.card;
        UIRender.roundRectBorder(header.x, header.y, header.w, header.h, 6f, fill, fill, Theme.border);
        String arrow = expanded[index] ? "▾" : "▸";
        UIRender.text(fr(), arrow + "  " + SECTION_NAMES[index], header.x + 9f, header.y + 9f,
                expanded[index] ? Theme.textPrimary : Theme.textSecondary, 0.9f, false);
        if (index != ChatEditorLayout.FONT) {
            UIRender.text(fr(), "Reset", header.x + header.w - 38f, header.y + 9f,
                    hot ? Theme.accent : Theme.textTertiary, 0.7f, false);
        }
    }

    private Entry entryAt(int mx, int my) {
        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry entry = entries.get(i);
            if (entryVisible(entry) && entry.widget.contains(mx, my)) return entry;
        }
        return null;
    }

    private int headerAt(int mx, int my) {
        for (int i = 0; i < ChatEditorLayout.SECTION_COUNT; i++) {
            ChatEditorLayout.Rect header = layout.headers[i];
            if (inside(mx, my, header.x, header.y, header.w, header.h)
                    && header.y + header.h > viewY && header.y < viewY + viewH) return i;
        }
        return -1;
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        if (button != 0) return;
        if (handleOpenDropdownClick(mx, my)) return;
        if (inside(mx, my, wx + ww - 52f, wy, 52f, 34f)) {
            close();
            return;
        }
        // The main settings card is a real draggable/clamped panel. A double-click title reset is scale-specific.
        if (inside(mx, my, wx, wy, ww - 52f, 34f)) {
            long now = System.currentTimeMillis();
            if (now - lastEditorTitleClick < 300L) {
                ChatQolSettings.editorPanel(0).resetScale(guiScale());
                draggingEditor = false;
            } else {
                draggingEditor = true;
                editorGrabX = mx - wx;
                editorGrabY = my - wy;
            }
            lastEditorTitleClick = now;
            return;
        }
        Widget toolbar = toolbarAt(mx, my);
        if (toolbar != null) {
            for (SearchField field : fields) field.setFocused(false);
            active = toolbar;
            active.onPress(mx, my);
            return;
        }
        if (arrangingWindows && !inside(mx, my, wx, wy, ww, wh)) {
            beginWindowDrag(mx, my);
            if (draggedWindow != null) return;
        }
        int header = headerAt(mx, my);
        if (header >= 0) {
            ChatEditorLayout.Rect bounds = layout.headers[header];
            if (header != ChatEditorLayout.FONT && mx >= bounds.x + bounds.w - 48f) {
                resetSection(header);
            } else if (mx <= bounds.x + 34f) {
                // The arrow has its own click target; dragging anywhere else on the title bar cannot accidentally
                // collapse a panel while the user is positioning it.
                ChatQolSettings.editorPanel(header + 1).toggleCollapsed(guiScale());
                expanded[header] = !ChatQolSettings.editorPanel(header + 1).collapsed(guiScale());
            } else {
                long now = System.currentTimeMillis();
                if (now - lastSectionTitleClick[header] < 300L) {
                    ChatQolSettings.editorPanel(header + 1).resetScale(guiScale());
                    draggedSection = -1;
                } else {
                    draggedSection = header;
                    sectionGrabX = mx - bounds.x;
                    sectionGrabY = my - bounds.y;
                }
                lastSectionTitleClick[header] = now;
            }
            return;
        }
        Entry entry = entryAt(mx, my);
        for (SearchField field : fields) field.setFocused(entry != null && entry.widget == field);
        active = entry == null ? null : entry.widget;
        if (active != null) active.onPress(mx, my);
    }

    private Widget toolbarAt(int mx, int my) {
        if (targetChoice.contains(mx, my)) return targetChoice;
        if (linkAllToggle.contains(mx, my)) return linkAllToggle;
        if (copyFromChoice.contains(mx, my)) return copyFromChoice;
        return null;
    }

    private int guiScale() {
        return Math.max(1, Math.min(4, Math.round(sf)));
    }

    /** Stores a section-card drag as a GUI-scale-specific offset around its safe stacked baseline. */
    private void moveSectionPanel(int section, float desiredX, float desiredY) {
        if (section < 0 || section >= ChatEditorLayout.SECTION_COUNT) return;
        ChatEditorLayout.Rect header = layout.headers[section];
        ChatEditorLayout.Rect body = layout.bodies[section];
        float cardH = header.h + (body.h > 0f ? ChatEditorLayout.GAP + body.h : 0f);
        float minX = viewX;
        float maxX = viewX + viewW - header.w;
        float minY = viewY;
        float maxY = viewY + viewH - Math.min(cardH, viewH);
        float clampedX = clampPanel(desiredX, minX, Math.max(minX, maxX));
        float clampedY = clampPanel(desiredY, minY, Math.max(minY, maxY));
        float dx = clampedX - header.x;
        float dy = clampedY - header.y;
        ChatEditorPanelLayout.State state = ChatQolSettings.editorPanel(section + 1);
        int nextX = state.x(guiScale()) + Math.round(dx * 10000f / Math.max(1f, viewW));
        int nextY = state.y(guiScale()) + Math.round(dy * 10000f / Math.max(1f, viewH));
        state.set(guiScale(), nextX, nextY, state.collapsed(guiScale()));
    }

    private void beginWindowDrag(int mx, int my) {
        draggedWindow = null;
        resizingWindow = false;
        ChatWindowLayout.Rect[] rects = {chat2Rect, chat1Rect};
        ChatQolSettings.Window[] windows = {ChatQolSettings.chat2(), ChatQolSettings.chat1()};
        for (int i = 0; i < rects.length; i++) {
            if (!rects[i].contains(mx, my)) continue;
            draggedWindow = windows[i];
            resizingWindow = rects[i].inResizeGrip(mx, my);
            grabX = mx - rects[i].x;
            grabY = my - rects[i].y;
            return;
        }
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long timeSinceLastClick) {
        if (draggingEditor) {
            ChatQolSettings.editorPanel(0).move(guiScale(), mx - editorGrabX, my - editorGrabY, ww, wh, width, height);
            return;
        }
        if (draggedSection >= 0) {
            moveSectionPanel(draggedSection, mx - sectionGrabX, my - sectionGrabY);
            return;
        }
        if (draggedWindow != null) {
            int scale = new ScaledResolution(Minecraft.getMinecraft()).getScaleFactor();
            ChatWindowLayout.Rect current = draggedWindow == ChatQolSettings.chat1() ? chat1Rect : chat2Rect;
            if (resizingWindow) {
                ChatWindowLayout.resize(draggedWindow, scale, width, height, current.x, current.y,
                        Math.round(mx - current.x), Math.round(my - current.y), ChatQolSettings.snapWindows);
            } else {
                ChatWindowLayout.move(draggedWindow, scale, width, height,
                        Math.round(mx - grabX), Math.round(my - grabY), ChatQolSettings.snapWindows);
            }
            return;
        }
        if (active != null) active.onDrag(mx, my);
    }

    @Override
    protected void mouseReleased(int mx, int my, int button) {
        if (draggingEditor) {
            draggingEditor = false;
            return;
        }
        if (draggedSection >= 0) {
            draggedSection = -1;
            return;
        }
        if (draggedWindow != null) {
            draggedWindow = null;
            return;
        }
        if (active != null) {
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
        commitFields();
    }

    @Override
    public void handleMouseInput() throws IOException {
        int wheel = Mouse.getEventDWheel();
        super.handleMouseInput();
        if (wheel != 0 && openDropdown == null) {
            scroll = Math.max(0f, Math.min(layout.maxScroll, scroll - wheel / 120f * 28f));
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        for (SearchField field : fields) {
            if (field.isFocused() && field.keyTyped(typedChar, keyCode)) {
                commitFields();
                return;
            }
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (openDropdown != null) {
                openDropdown = null;
            } else if (arrangingWindows) {
                arrangingWindows = false;
            } else {
                close();
            }
            return;
        }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            commitFields();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private void resetSection(int section) {
        switch (section) {
            case ChatEditorLayout.WINDOWS:
                ChatQolSettings.resetWindows();
                break;
            case ChatEditorLayout.BACKGROUND:
                ChatQolSettings.resetBackground();
                break;
            case ChatEditorLayout.MESSAGES:
                ChatQolSettings.resetMessages();
                patternStatus = "Default patterns restored.";
                break;
            case ChatEditorLayout.FONT:
                // Routes only: font reset belongs to the dedicated global Fonts screen.
                break;
            case ChatEditorLayout.TABS:
                ChatQolSettings.resetTabs();
                break;
            default:
                ChatQolSettings.resetHoverMenu();
                break;
        }
        syncFields();
    }

    private void close() {
        commitFields();
        Minecraft.getMinecraft().displayGuiScreen(parent);
    }
}
