package com.arena.pekaclient.notes;

import com.arena.pekaclient.gui.AppleGuiButton;
import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.gui.GuiLayoutToolkit;
import com.arena.pekaclient.render.UIRender;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * The /qn quick note pad: pages of text typed in game. Every edit marks the page dirty and a tick
 * timer writes the file a moment later, so notes save themselves while you type and on close.
 */
public final class QuickNoteScreen extends GuiScreen {
    private static final int BUTTON_PREV = 0;
    private static final int BUTTON_NEXT = 1;
    private static final int BUTTON_NEW_PAGE = 2;
    private static final int BUTTON_DELETE_PAGE = 3;
    private static final int BUTTON_DONE = 4;

    private static final int COLOR_PANEL = 0xF01B1B1D;
    private static final int COLOR_BORDER = 0xFF8E8E96;
    private static final int COLOR_BOX = 0xFF141416;
    private static final int COLOR_BOX_BORDER = 0xFF6E6E76;
    private static final int COLOR_TITLE = 0xFFF2F2F4;
    private static final int COLOR_TEXT = 0xFFE8E8EB;
    private static final int COLOR_HINT = 0xFF9A9AA2;
    private static final int COLOR_SAVED = 0xFF8FD68F;
    private static final int COLOR_PENDING = 0xFFE8C46A;
    private static final int COLOR_CURSOR = 0xFFFFFFFF;
    private static final int COLOR_SIDEBAR = 0xFF141416;
    private static final int COLOR_SIDEBAR_BORDER = 0xFF6E6E76;
    private static final int COLOR_ROW = 0xFF9A9AA2;
    private static final int COLOR_ROW_SELECTED = 0xFF35D3D3;
    private static final int COLOR_ROW_COUNT = 0xFF7F7F88;
    private static final long AUTOSAVE_DELAY_MS = 700L;

    private final QuickNoteStore store;
    private final GuiScreen parent;
    private GuiTextField headingField;
    private GuiTextField searchField;
    private final List<Integer> searchHits = new ArrayList<Integer>();
    private int searchScroll;
    private String lastQuery = null;
    private int page;
    private int cursor;
    private int scrollLine;
    private boolean dirty;
    private long lastEditAt;
    private String wrappedSource;
    private int wrappedWidth;
    private final List<int[]> lines = new ArrayList<int[]>();
    /** Real production geometry from the common clamped-panel system, never fixed pixel assumptions. */
    private final QuickNoteLayout.Metrics layout = new QuickNoteLayout.Metrics();

    public QuickNoteScreen(QuickNoteStore store, GuiScreen parent) {
        this.store = store;
        this.parent = parent;
        // Open on the last page with the cursor at the end, ready to keep writing.
        this.page = Math.max(0, store.getPageCount() - 1);
        this.cursor = store.getPage(this.page).length();
    }

    @Override
    public void initGui() {
        buttonList.clear();
        // Positions, widths and labels are updated below whenever the scaled viewport changes.
        buttonList.add(new AppleGuiButton(BUTTON_PREV, 0, 0, 1, 18, "< Prev"));
        buttonList.add(new AppleGuiButton(BUTTON_NEXT, 0, 0, 1, 18, "Next >"));
        buttonList.add(new AppleGuiButton(BUTTON_DELETE_PAGE, 0, 0, 1, 18, "Delete"));
        buttonList.add(new AppleGuiButton(BUTTON_DONE, 0, 0, 1, 18, "Done"));
        buttonList.add(new AppleGuiButton(BUTTON_NEW_PAGE, 0, 0, 1, 16, "+ New page"));
        headingField = new GuiTextField(0, fontRendererObj, headingFieldX(), headingLabelY(),
                headingFieldWidth(), 14);
        headingField.setMaxStringLength(QuickNoteStore.MAX_HEADING_LENGTH);
        headingField.setText(store.getHeading(page));
        searchField = new GuiTextField(1, fontRendererObj, sidebarX() + 4, panelY() + 25,
                Math.max(1, sidebarWidth() - 8), 14);
        searchField.setMaxStringLength(40);
        searchField.setText(lastQuery == null ? "" : lastQuery);
        refreshSearch();
        layoutButtons();
        updateButtonStates();
    }

    /** Keeps every action reachable at compact GUI scales instead of letting the old fixed button row overlap. */
    private void layoutButtons() {
        measurePanel();
        if (buttonList == null || buttonList.isEmpty()) {
            return;
        }
        GuiButton prev = button(BUTTON_PREV);
        GuiButton next = button(BUTTON_NEXT);
        GuiButton delete = button(BUTTON_DELETE_PAGE);
        GuiButton done = button(BUTTON_DONE);
        GuiButton add = button(BUTTON_NEW_PAGE);
        if (layout.compact) {
            int gap = 2;
            int x = Math.round(layout.footer.x);
            int y = Math.round(layout.footer.y + 2f);
            int w = Math.max(22, (Math.round(layout.footer.w) - gap * 3) / 4);
            placeButton(prev, x, y, w, 18, "<");
            placeButton(next, x + w + gap, y, w, 18, ">");
            placeButton(delete, x + (w + gap) * 2, y, w, 18, "Del");
            placeButton(done, Math.round(layout.footer.right()) - w, y, w, 18, "Done");
            placeButton(add, Math.round(layout.panel.right()) - 20, Math.round(layout.panel.y + 4f), 14, 16, "+");
            return;
        }
        int buttonWidth = 62;
        int gap = 4;
        int y = Math.round(layout.footer.y + 20f);
        int x = Math.round(layout.footer.x);
        placeButton(prev, x, y, buttonWidth, 18, "< Prev");
        placeButton(next, x + buttonWidth + gap, y, buttonWidth, 18, "Next >");
        placeButton(delete, x + (buttonWidth + gap) * 2, y, buttonWidth, 18, "Delete");
        placeButton(done, Math.round(layout.panel.right()) - 10 - buttonWidth, y, buttonWidth, 18, "Done");
        placeButton(add, Math.round(layout.panel.right()) - 90, Math.round(layout.panel.y + 5f), 78, 16, "+ New page");
    }

    private GuiButton button(int id) {
        for (Object object : buttonList) {
            GuiButton candidate = (GuiButton) object;
            if (candidate.id == id) return candidate;
        }
        return null;
    }

    private void placeButton(GuiButton button, int x, int y, int width, int height, String label) {
        if (button == null) return;
        button.xPosition = x;
        button.yPosition = y;
        button.width = Math.max(1, width);
        button.height = Math.max(1, height);
        button.displayString = label;
    }

    private void updateButtonStates() {
        if (buttonList == null || buttonList.isEmpty()) {
            return;
        }
        for (Object object : buttonList) {
            GuiButton button = (GuiButton) object;
            if (button.id == BUTTON_PREV) {
                button.enabled = page > 0;
            } else if (button.id == BUTTON_NEXT) {
                button.enabled = page < store.getPageCount() - 1;
            } else if (button.id == BUTTON_NEW_PAGE) {
                button.enabled = store.getPageCount() < QuickNoteStore.MAX_PAGES;
            } else if (button.id == BUTTON_DELETE_PAGE) {
                button.enabled = !store.getPage(page).isEmpty() || store.getPageCount() > 1;
            }
        }
    }

    // ---------------------------------------------------------------- geometry
    private net.minecraft.client.gui.FontRenderer font() { return fontRendererObj; }

    private void measurePanel() {
        QuickNoteLayout.update(layout, width, height);
    }

    private int panelWidth() { measurePanel(); return Math.round(layout.panel.w); }
    private int panelHeight() { measurePanel(); return Math.round(layout.panel.h); }
    private int panelX() { measurePanel(); return Math.round(layout.panel.x); }
    private int panelY() { measurePanel(); return Math.round(layout.panel.y); }
    private int sidebarX() { measurePanel(); return Math.round(layout.sidebar.x); }
    private int sidebarY() { measurePanel(); return Math.round(layout.sidebar.y); }
    private int sidebarBottom() { measurePanel(); return Math.round(layout.sidebar.bottom()); }
    private int sidebarWidth() { measurePanel(); return Math.round(layout.sidebar.w); }
    private int contentLeft() { measurePanel(); return Math.round(layout.note.x); }
    private int boxX() { measurePanel(); return Math.round(layout.note.x); }
    private int boxWidth() { measurePanel(); return Math.max(1, Math.round(layout.note.w)); }
    private int headingLabelY() { measurePanel(); return Math.round(layout.heading.y); }
    private int headingFieldX() { measurePanel(); return Math.round(layout.heading.x); }
    private int headingFieldWidth() { measurePanel(); return Math.max(1, Math.round(layout.heading.w)); }
    private int boxY() { measurePanel(); return Math.round(layout.note.y); }
    private int boxHeight() { measurePanel(); return Math.max(1, Math.round(layout.note.h)); }
    private boolean compactLayout() { measurePanel(); return layout.compact; }
    private boolean showSidebar() { measurePanel(); return layout.showSidebar; }

    private int lineHeight() {
        return font().FONT_HEIGHT + 2;
    }

    private int visibleLines() {
        return Math.max(1, (boxHeight() - 6) / lineHeight());
    }

    private String pageText() {
        return store.getPage(page);
    }

    // ---------------------------------------------------------------- drawing
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        measurePanel();
        layoutButtons();
        drawDefaultBackground();
        Gui.drawRect(panelX(), panelY(), panelX() + panelWidth(), panelY() + panelHeight(),
                COLOR_PANEL);
        drawBorder(panelX(), panelY(), panelWidth(), panelHeight(), COLOR_BORDER);
        String title = compactLayout() ? "Note " + (page + 1) + "/" + store.getPageCount() : "Quick Note";
        font().drawStringWithShadow(font().trimStringToWidth(title, Math.max(1, panelWidth() - 38)),
                panelX() + 8, panelY() + 10, COLOR_TITLE);
        if (showSidebar()) {
            drawSidebar();
        }

        // In the compact arrangement the heading field spans its own row; in the normal arrangement it follows
        // the label in the note column. Both forms are derived from QuickNoteLayout, not fixed sidebar offsets.
        if (!compactLayout()) {
            font().drawStringWithShadow("Heading", contentLeft(), headingLabelY() + 3, COLOR_HINT);
        }
        if (headingField != null) {
            headingField.xPosition = headingFieldX();
            headingField.yPosition = headingLabelY();
            headingField.width = headingFieldWidth();
            headingField.drawTextBox();
            if (!headingField.isFocused() && headingField.getText().isEmpty()) {
                font().drawStringWithShadow(compactLayout() ? "Title (optional)" : "(optional)",
                        headingFieldX() + 5, headingLabelY() + 3, COLOR_HINT);
            }
        }

        Gui.drawRect(boxX(), boxY(), boxX() + boxWidth(), boxY() + boxHeight(), COLOR_BOX);
        drawBorder(boxX(), boxY(), boxWidth(), boxHeight(), COLOR_BOX_BORDER);

        String text = pageText();
        rebuildLines(text);
        int visible = visibleLines();
        int maxScroll = Math.max(0, lines.size() - visible);
        scrollLine = Math.max(0, Math.min(maxScroll, scrollLine));
        beginClip(layout.note);
        try {
            for (int i = 0; i < visible; i++) {
                int lineIndex = scrollLine + i;
                if (lineIndex >= lines.size()) {
                    break;
                }
                int[] line = lines.get(lineIndex);
                String label = text.substring(line[0], line[1]);
                font().drawStringWithShadow(label, boxX() + 4,
                        boxY() + 4 + i * lineHeight(), COLOR_TEXT);
            }
            drawCursor(text);
        } finally {
            UIRender.scissorEnd();
        }

        if (!compactLayout()) {
            String hint = dirty ? "saving..." : "saved automatically";
            font().drawStringWithShadow(hint, panelX() + 12,
                    panelY() + panelHeight() - 37, dirty ? COLOR_PENDING : COLOR_SAVED);
            String keys = font().trimStringToWidth("Enter new line  -  search on the left  -  Esc closes",
                    Math.max(1, panelWidth() - 24 - font().getStringWidth(hint)));
            font().drawStringWithShadow(keys,
                    panelX() + panelWidth() - 12 - font().getStringWidth(keys),
                    panelY() + panelHeight() - 37, COLOR_HINT);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
        updateButtonStates();
    }

    /** The page list on the left, with the search field that filters it. */
    private void drawSidebar() {
        if (!showSidebar()) return;
        int sidebarWidth = sidebarWidth();
        Gui.drawRect(sidebarX(), sidebarY(), sidebarX() + sidebarWidth, sidebarBottom(),
                COLOR_SIDEBAR);
        drawBorder(sidebarX(), sidebarY(), sidebarWidth, sidebarBottom() - sidebarY(),
                COLOR_SIDEBAR_BORDER);
        if (searchField != null) {
            searchField.xPosition = sidebarX() + 4;
            searchField.yPosition = panelY() + 25;
            searchField.width = Math.max(1, sidebarWidth - 8);
            searchField.drawTextBox();
            if (!searchField.isFocused() && searchField.getText().isEmpty()) {
                font().drawStringWithShadow("Search notes",
                        sidebarX() + 9, panelY() + 29, COLOR_ROW_COUNT);
            }
        }

        int rowHeight = lineHeight();
        int visible = Math.max(1, (sidebarBottom() - sidebarY() - 16) / rowHeight);
        int maxScroll = Math.max(0, searchHits.size() - visible);
        searchScroll = Math.max(0, Math.min(maxScroll, searchScroll));
        for (int i = 0; i < visible; i++) {
            int slot = searchScroll + i;
            if (slot >= searchHits.size()) {
                break;
            }
            int pageIndex = searchHits.get(slot);
            int rowY = sidebarY() + 3 + i * rowHeight;
            boolean selected = pageIndex == page;
            String label = (pageIndex + 1) + ". " + store.getPreview(pageIndex);
            String clipped = font().trimStringToWidth(label, Math.max(1, sidebarWidth - 12));
            if (selected) {
                Gui.drawRect(sidebarX() + 1, rowY - 1, sidebarX() + sidebarWidth - 1,
                        rowY + rowHeight - 1, 0x5535D3D3);
            }
            font().drawStringWithShadow(clipped, sidebarX() + 5, rowY,
                    selected ? COLOR_ROW_SELECTED : COLOR_ROW);
        }

        String count = searchField != null && !searchField.getText().trim().isEmpty()
                ? searchHits.size() + " of " + store.getPageCount() + " pages"
                : store.getPageCount() + " pages";
        font().drawStringWithShadow(font().trimStringToWidth(count,
                Math.max(1, sidebarWidth - 10)), sidebarX() + 5, sidebarBottom() - 10, COLOR_ROW_COUNT);
    }

    private void refreshSearch() {
        String query = searchField == null ? "" : searchField.getText();
        if (query.equals(lastQuery)) {
            return;
        }
        lastQuery = query;
        searchHits.clear();
        searchHits.addAll(store.findPages(query));
        searchScroll = 0;
    }

    private void jumpToPage(int pageIndex) {
        saveNow();
        page = Math.max(0, Math.min(store.getPageCount() - 1, pageIndex));
        cursor = pageText().length();
        scrollLine = 0;
        wrappedSource = null;
        if (headingField != null) {
            headingField.setText(store.getHeading(page));
            headingField.setFocused(false);
        }
        ensureCursorVisible();
        updateButtonStates();
    }

    private void drawCursor(String text) {
        if ((System.currentTimeMillis() / 500L) % 2L != 0L) {
            return;
        }
        int lineIndex = cursorLine(text);
        if (lineIndex < scrollLine || lineIndex >= scrollLine + visibleLines()) {
            return;
        }
        int[] line = lines.get(lineIndex);
        int offset = font().getStringWidth(text.substring(line[0],
                Math.max(line[0], Math.min(cursor, line[1]))));
        int cursorX = boxX() + 4 + offset;
        int cursorY = boxY() + 4 + (lineIndex - scrollLine) * lineHeight();
        Gui.drawRect(cursorX, cursorY - 1, cursorX + 1, cursorY + font().FONT_HEIGHT,
                COLOR_CURSOR);
    }

    /** Scissor clipping shares the same resolved note rectangle used for wrap, hit test and scrolling. */
    private void beginClip(GuiLayoutToolkit.Rect rect) {
        UIRender.scissorBegin(rect.x, rect.y, rect.w, rect.h,
                new ScaledResolution(mc).getScaleFactor());
    }

    private void drawBorder(int x, int y, int w, int h, int color) {
        Gui.drawRect(x, y, x + w, y + 1, color);
        Gui.drawRect(x, y + h - 1, x + w, y + h, color);
        Gui.drawRect(x, y, x + 1, y + h, color);
        Gui.drawRect(x + w - 1, y, x + w, y + h, color);
    }

    /** Wraps the page into display lines, each an inclusive-start / exclusive-end index pair. */
    private void rebuildLines(String text) {
        if (text.equals(wrappedSource) && boxWidth() == wrappedWidth && !lines.isEmpty()) {
            return;
        }
        wrappedSource = text;
        wrappedWidth = boxWidth();
        lines.clear();
        int maxWidth = Math.max(20, boxWidth() - 8);
        StringBuilder current = new StringBuilder();
        int lineStart = 0;
        int lastSpace = -1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                lines.add(new int[] {lineStart, i});
                current.setLength(0);
                lineStart = i + 1;
                lastSpace = -1;
                continue;
            }
            current.append(c);
            if (c == ' ') {
                lastSpace = i;
            }
            if (current.length() > 1
                    && font().getStringWidth(current.toString()) > maxWidth) {
                if (lastSpace > lineStart) {
                    lines.add(new int[] {lineStart, lastSpace});
                    lineStart = lastSpace + 1;
                } else {
                    lines.add(new int[] {lineStart, i});
                    lineStart = i;
                }
                current.setLength(0);
                lastSpace = -1;
                i = lineStart - 1;
            }
        }
        lines.add(new int[] {lineStart, text.length()});
    }

    private int cursorLine(String text) {
        for (int i = 0; i < lines.size(); i++) {
            if (cursor <= lines.get(i)[1]) {
                return i;
            }
        }
        return Math.max(0, lines.size() - 1);
    }

    private void ensureCursorVisible() {
        int line = cursorLine(pageText());
        if (line < scrollLine) {
            scrollLine = line;
        } else if (line >= scrollLine + visibleLines()) {
            scrollLine = line - visibleLines() + 1;
        }
    }

    // ---------------------------------------------------------------- editing
    private void insert(String addition) {
        String text = pageText();
        if (text.length() + addition.length() > QuickNoteStore.MAX_PAGE_LENGTH) {
            return;
        }
        setText(text.substring(0, cursor) + addition + text.substring(cursor));
        cursor += addition.length();
    }

    private void setText(String text) {
        store.setPage(page, text);
        if (cursor > text.length()) {
            cursor = text.length();
        }
        wrappedSource = null;
        dirty = true;
        lastEditAt = System.currentTimeMillis();
        ensureCursorVisible();
    }

    private void saveNow() {
        syncHeading();
        store.save();
        dirty = false;
    }

    /** Copies the heading text into the store when it differs. */
    private void syncHeading() {
        if (headingField == null) {
            return;
        }
        if (!headingField.getText().equals(store.getHeading(page))) {
            store.setHeading(page, headingField.getText());
            dirty = true;
        }
    }

    private void markDirty() {
        dirty = true;
        lastEditAt = System.currentTimeMillis();
    }

    private void goToPage(int target) {
        saveNow();
        page = Math.max(0, Math.min(store.getPageCount() - 1, target));
        cursor = pageText().length();
        scrollLine = 0;
        wrappedSource = null;
        if (headingField != null) {
            headingField.setText(store.getHeading(page));
            headingField.setFocused(false);
        }
        ensureCursorVisible();
        updateButtonStates();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        String text = pageText();
        if (searchField != null && searchField.isFocused()) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                searchField.setFocused(false);
                return;
            }
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_TAB) {
                searchField.setFocused(false);
                return;
            }
            searchField.textboxKeyTyped(typedChar, keyCode);
            refreshSearch();
            return;
        }
        if (headingField != null && headingField.isFocused() && keyCode != Keyboard.KEY_ESCAPE) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_TAB) {
                // Enter or Tab drops out of the heading and into the note itself.
                store.setHeading(page, headingField.getText());
                markDirty();
                headingField.setFocused(false);
                return;
            }
            headingField.textboxKeyTyped(typedChar, keyCode);
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            saveNow();
            mc.displayGuiScreen(parent);
            return;
        }
        if (keyCode == Keyboard.KEY_BACK) {
            if (cursor > 0) {
                setText(text.substring(0, cursor - 1) + text.substring(cursor));
                cursor--;
                ensureCursorVisible();
            }
            return;
        }
        if (keyCode == Keyboard.KEY_DELETE) {
            if (cursor < text.length()) {
                setText(text.substring(0, cursor) + text.substring(cursor + 1));
            }
            return;
        }
        if (keyCode == Keyboard.KEY_RETURN) {
            insert("\n");
            return;
        }
        if (keyCode == Keyboard.KEY_TAB) {
            insert("  ");
            return;
        }
        if (keyCode == Keyboard.KEY_LEFT) {
            if (cursor > 0) {
                cursor--;
                ensureCursorVisible();
            }
            return;
        }
        if (keyCode == Keyboard.KEY_RIGHT) {
            if (cursor < text.length()) {
                cursor++;
                ensureCursorVisible();
            }
            return;
        }
        if (keyCode == Keyboard.KEY_UP || keyCode == Keyboard.KEY_DOWN) {
            moveCursorVertically(keyCode == Keyboard.KEY_UP ? -1 : 1);
            return;
        }
        if (typedChar >= ' ' && typedChar != 127 && typedChar != 167) {
            insert(String.valueOf(typedChar));
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private void moveCursorVertically(int delta) {
        String text = pageText();
        rebuildLines(text);
        int line = cursorLine(text);
        int target = Math.max(0, Math.min(lines.size() - 1, line + delta));
        if (target == line) {
            return;
        }
        int[] source = lines.get(line);
        int desired = font().getStringWidth(text.substring(source[0],
                Math.max(source[0], Math.min(cursor, source[1]))));
        int[] destination = lines.get(target);
        int best = destination[0];
        for (int i = destination[0]; i <= destination[1]; i++) {
            best = i;
            if (font().getStringWidth(text.substring(destination[0], i)) >= desired) {
                break;
            }
        }
        cursor = best;
        ensureCursorVisible();
    }

    // ---------------------------------------------------------------- mouse
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0 && showSidebar() && searchField != null
                && mouseX >= searchField.xPosition
                && mouseX <= searchField.xPosition + searchField.width
                && mouseY >= searchField.yPosition
                && mouseY <= searchField.yPosition + searchField.height) {
            searchField.setFocused(true);
            if (headingField != null) {
                headingField.setFocused(false);
            }
            searchField.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }
        if (searchField != null) {
            searchField.setFocused(false);
        }
        if (mouseButton == 0 && mouseX >= sidebarX() && showSidebar() && mouseX <= sidebarX() + sidebarWidth()
                && mouseY >= sidebarY() && mouseY <= sidebarBottom()) {
            int rowHeight = lineHeight();
            int slot = searchScroll + Math.max(0, (mouseY - sidebarY() - 3) / rowHeight);
            if (slot >= 0 && slot < searchHits.size()) {
                jumpToPage(searchHits.get(slot));
            }
            return;
        }
        if (mouseButton == 0 && headingField != null
                && mouseX >= headingField.xPosition
                && mouseX <= headingField.xPosition + headingField.width
                && mouseY >= headingField.yPosition
                && mouseY <= headingField.yPosition + headingField.height) {
            headingField.setFocused(true);
            headingField.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }
        if (headingField != null) {
            headingField.setFocused(false);
        }
        if (mouseButton == 0 && mouseX >= boxX() && mouseX <= boxX() + boxWidth()
                && mouseY >= boxY() && mouseY <= boxY() + boxHeight()) {
            String text = pageText();
            rebuildLines(text);
            int lineIndex = scrollLine + Math.max(0, (mouseY - boxY() - 4) / lineHeight());
            if (lineIndex < lines.size()) {
                int[] line = lines.get(lineIndex);
                int target = line[0];
                for (int i = line[0]; i <= line[1]; i++) {
                    target = i;
                    if (boxX() + 4 + font().getStringWidth(
                            text.substring(line[0], i)) >= mouseX) {
                        break;
                    }
                }
                cursor = Math.max(line[0], Math.min(line[1], target));
                ensureCursorVisible();
            }
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        boolean fine = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        int step = Math.max(1, Math.round(2f * AppleSettings.guiScrollMultiplier() * (fine ? .25f : 1f)));
        if (showSidebar() && mouseX >= sidebarX() && mouseX <= sidebarX() + sidebarWidth()) {
            int visible = Math.max(1, (sidebarBottom() - sidebarY() - 16) / lineHeight());
            int maxListScroll = Math.max(0, searchHits.size() - visible);
            searchScroll = Math.max(0, Math.min(maxListScroll, searchScroll - wheel / 120 * step));
            return;
        }
        if (mouseX < boxX() || mouseX > boxX() + boxWidth()
                || mouseY < boxY() || mouseY > boxY() + boxHeight()) {
            return;
        }
        int maxScroll = Math.max(0, lines.size() - visibleLines());
        scrollLine = Math.max(0, Math.min(maxScroll, scrollLine - wheel / 120 * step));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case BUTTON_PREV:
                goToPage(page - 1);
                break;
            case BUTTON_NEXT:
                goToPage(page + 1);
                break;
            case BUTTON_NEW_PAGE:
                saveNow();
                page = store.appendPage();
                lastQuery = null;
                cursor = 0;
                scrollLine = 0;
                wrappedSource = null;
                if (headingField != null) {
                    headingField.setText(store.getHeading(page));
                    headingField.setFocused(false);
                }
                markDirty();
                updateButtonStates();
                break;
            case BUTTON_DELETE_PAGE:
                saveNow();
                store.deletePage(page);
                page = Math.max(0, Math.min(store.getPageCount() - 1, page));
                cursor = pageText().length();
                scrollLine = 0;
                wrappedSource = null;
                if (headingField != null) {
                    headingField.setText(store.getHeading(page));
                    headingField.setFocused(false);
                }
                markDirty();
                updateButtonStates();
                break;
            case BUTTON_DONE:
                saveNow();
                mc.displayGuiScreen(parent);
                break;
            default:
                break;
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (searchField != null) {
            searchField.updateCursorCounter();
            refreshSearch();
        }
        if (headingField != null) {
            headingField.updateCursorCounter();
            if (headingField.isFocused()
                    && !headingField.getText().equals(store.getHeading(page))) {
                store.setHeading(page, headingField.getText());
                markDirty();
            }
        }
        if (dirty && System.currentTimeMillis() - lastEditAt >= AUTOSAVE_DELAY_MS) {
            saveNow();
        }
    }

    @Override
    public void onGuiClosed() {
        saveNow();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
