package com.arena.pekaclient.gui;

import com.arena.pekaclient.config.ChatQolSettings;
import com.arena.pekaclient.feature.ChatSurfaceRenderer;
import com.arena.pekaclient.feature.ChatTabLayout;
import com.arena.pekaclient.feature.ChatTabs;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import net.minecraft.client.gui.FontRenderer;

import java.util.List;

/**
 * Interactive editor-only sample for the Default Chat renderer. It has no connection to the live
 * message-history/capture path: test messages are local preview data, while every visual setting is
 * read directly from the same {@link ChatQolSettings} fields used by {@code ChatChannelTabs}.
 */
final class ChatEditorPreview {
    private static final String LINE_ONE = "[Party] Alex: test message from Sky VeLor";
    private static final String LINE_TWO = "[Guild] Mina: hover this row for the highlight";
    private static final String LINE_THREE = "Welcome back! Right-click-like preview menu is available.";
    private static final String MENU_COPY = "Copy to clipboard";
    private static final String MENU_CHAT = "Copy to chat box";
    private static final String MENU_MESSAGE = "Message player";
    private static final long AUTO_INTERVAL_NANOS = 4000000000L;

    /** Mutable UI-only state, constructed once by the editor and never persisted. */
    static final class State {
        private final ChatTabLayout.Layout tabs = new ChatTabLayout.Layout();
        private boolean autoMessages;
        private boolean menuOpen;
        private int menuHot = -1;
        private long testStartedNanos = -1L;
        private long nextAutoNanos;
        private float lastLineX, lastLineY, lastLineW, lastLineH;
        private float menuX, menuY, menuW;

        boolean autoMessages() { return autoMessages; }
        void setAutoMessages(boolean value) {
            autoMessages = value;
            nextAutoNanos = System.nanoTime() + AUTO_INTERVAL_NANOS;
            if (value && testStartedNanos < 0L) sendTest();
        }
        void sendTest() { testStartedNanos = System.nanoTime(); menuOpen = false; }
        boolean menuOpen() { return menuOpen; }

        void update(long now) {
            if (autoMessages && now >= nextAutoNanos) {
                testStartedNanos = now;
                nextAutoNanos = now + AUTO_INTERVAL_NANOS;
                menuOpen = false;
            }
        }

        boolean click(float mouseX, float mouseY) {
            if (menuOpen) {
                int row = (int) ((mouseY - menuY - 2f) / 20f);
                if (mouseX >= menuX && mouseX <= menuX + menuW && row >= 0 && row < 3) {
                    menuOpen = false;
                    return true;
                }
                menuOpen = false;
                return true;
            }
            for (int i = 0; i < tabs.count; i++) {
                ChatTabLayout.Rect tab = tabs.tabs[i];
                if (tab.contains(mouseX, mouseY)) {
                    ChatQolSettings.setSelectedTab(tab.id);
                    return true;
                }
            }
            if (mouseX >= lastLineX && mouseX <= lastLineX + lastLineW
                    && mouseY >= lastLineY && mouseY <= lastLineY + lastLineH) {
                menuOpen = true;
                menuX = Math.min(lastLineX + 20f, lastLineX + lastLineW - 118f);
                menuY = Math.max(lastLineY - 51f, lastLineY + 8f);
                menuW = 118f;
                return true;
            }
            return false;
        }
    }

    private ChatEditorPreview() { }

    /** Testable identity proof: preview rendering reads the live static Chat QoL owner directly. */
    static Object settingsIdentity() { return ChatQolSettings.class; }

    /** Same-frame animation calculation used by the drawn test message. */
    static float testMessageAlpha(long elapsedNanos) {
        long elapsed = Math.max(0L, elapsedNanos);
        float alpha = 1f;
        if (ChatQolSettings.smoothMessages && ChatQolSettings.messageAnimationMillis > 0) {
            float p = Math.min(1f, elapsed / (ChatQolSettings.messageAnimationMillis * 1000000f));
            float eased = 1f - (1f - p) * (1f - p);
            alpha = .18f + .82f * eased;
        }
        if (ChatQolSettings.fadeSeconds > 0) {
            float fadeAge = elapsed / 1000000000f;
            float duration = ChatQolSettings.fadeSeconds;
            if (fadeAge >= duration) alpha = 0f;
            else if (fadeAge > duration * .70f) alpha *= Math.max(0f, (duration - fadeAge) / (duration * .30f));
        }
        return alpha;
    }

    static void draw(State state, float x, float y, float width, float height, int mouseX, int mouseY,
                     FontRenderer font, long nowNanos) {
        if (state == null || font == null || width < 20f || height < 20f) return;
        state.update(nowNanos);
        float pad = 9f;
        float windowX = x + pad;
        float windowY = y + 15f;
        float windowW = Math.max(12f, width - pad * 2f);
        float windowH = Math.max(12f, height - 24f);
        UIRender.roundRectBorder(windowX, windowY, windowW, windowH, 4f,
                Theme.CHAT_PREVIEW_SURFACE, Theme.CHAT_PREVIEW_SURFACE, Theme.border);
        float inputY = windowY + windowH - 15f;
        float messageBottom = inputY - 4f;
        float lineH = Math.max(9f, font.FONT_HEIGHT * .82f + 1f);
        float lineW = Math.max(8f, windowW - 12f);
        float firstY = messageBottom - lineH * 3f;
        float testY = messageBottom - lineH * 4f;

        // The real background primitive uses direct ChatQolSettings colour/opacity/radius values.
        if (ChatQolSettings.backgroundOpacityOpen > 0) {
            ChatSurfaceRenderer.drawMessageBackground(windowX + 4f, Math.max(windowY + 4f, testY - 2f),
                    Math.min(lineW, windowW - 8f), Math.max(lineH * 4f + 4f, messageBottom - testY + 4f));
        }
        boolean hoveringSecond = contains(mouseX, mouseY, windowX + 6f, firstY + lineH, lineW, lineH);
        if (hoveringSecond) ChatSurfaceRenderer.drawHoverRow(windowX + 5f, firstY + lineH, lineW, lineH, 1f);
        ChatSurfaceRenderer.drawMessageLine(font, LINE_ONE, windowX + 7f, firstY, (Theme.TEXT_PRIMARY & Theme.RGB_MASK), 1f, .82f, true);
        ChatSurfaceRenderer.drawMessageLine(font, LINE_TWO, windowX + 7f, firstY + lineH, (Theme.TEXT_PRIMARY & Theme.RGB_MASK), 1f, .82f, true);
        ChatSurfaceRenderer.drawMessageLine(font, LINE_THREE, windowX + 7f, firstY + lineH * 2f, (Theme.TEXT_PRIMARY & Theme.RGB_MASK), 1f, .82f, true);

        drawTestMessage(state, font, windowX + 7f, testY, nowNanos);
        state.lastLineX = windowX + 5f;
        state.lastLineY = firstY + lineH;
        state.lastLineW = lineW;
        state.lastLineH = lineH;

        UIRender.roundRect(windowX + 4f, inputY, windowW - 8f, 10f, 2f, Theme.CHAT_PREVIEW_INPUT, Theme.CHAT_PREVIEW_INPUT, .3f);
        UIRender.text(font, "Type a chat message…", windowX + 8f, inputY + 2f, Theme.CHAT_PREVIEW_HINT, .68f, false);
        drawTabs(state, font, windowX, inputY - ChatTabLayout.HEIGHT - 2f, windowW, mouseX, mouseY);
        if (state.menuOpen) {
            state.menuHot = (int) ((mouseY - state.menuY - 2f) / 20f);
            if (state.menuHot < 0 || state.menuHot > 2 || !contains(mouseX, mouseY, state.menuX, state.menuY,
                    state.menuW, 64f)) state.menuHot = -1;
            ChatSurfaceRenderer.drawMenu(font, state.menuX, state.menuY, state.menuW, 3,
                    MENU_COPY, MENU_CHAT, MENU_MESSAGE, state.menuHot, state.menuHot < 0 ? 0f : 1f);
        }
    }

    private static void drawTestMessage(State state, FontRenderer font, float x, float y, long nowNanos) {
        if (state.testStartedNanos < 0L) return;
        long elapsed = Math.max(0L, nowNanos - state.testStartedNanos);
        float alpha = testMessageAlpha(elapsed);
        float slide = 0f;
        if (ChatQolSettings.smoothMessages && ChatQolSettings.messageAnimationMillis > 0) {
            float p = Math.min(1f, elapsed / (ChatQolSettings.messageAnimationMillis * 1000000f));
            float eased = 1f - (1f - p) * (1f - p);
            slide = (1f - eased) * 5f;
        }
        if (alpha > .002f) ChatSurfaceRenderer.drawMessageLine(font, "[Preview] New test message", x, y + slide,
                Theme.CHAT_PREVIEW_TAB, alpha, .82f, true);
    }

    private static void drawTabs(State state, FontRenderer font, float x, float y, float width, int mouseX, int mouseY) {
        List<ChatTabs.Tab> visible = ChatQolSettings.visibleTabs();
        ChatTabLayout.update(state.tabs, width, y + ChatTabLayout.HEIGHT, visible);
        // ChatTabLayout uses the game's left inset. Rebase it into this preview window without making new data.
        float offset = x - ChatTabLayout.LEFT;
        for (int i = 0; i < state.tabs.count; i++) {
            ChatTabLayout.Rect rect = state.tabs.tabs[i];
            ChatTabs.Tab tab = ChatQolSettings.tab(rect.id);
            if (tab == null) continue;
            boolean selected = rect.id.equals(ChatQolSettings.selectedTabId());
            ChatSurfaceRenderer.drawTab(font, tab, rect.x + offset, rect.y, rect.w, rect.h, selected,
                    contains(mouseX, mouseY, rect.x + offset, rect.y, rect.w, rect.h), false);
            // click() needs preview-space rectangles, so keep the cached layout rebased after drawing.
            rect.x += offset;
        }
    }

    private static boolean contains(float px, float py, float x, float y, float width, float height) {
        return px >= x && px < x + width && py >= y && py < y + height;
    }
}
