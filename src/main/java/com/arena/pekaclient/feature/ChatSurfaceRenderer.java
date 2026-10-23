package com.arena.pekaclient.feature;

import com.arena.pekaclient.config.ChatQolSettings;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import net.minecraft.client.gui.FontRenderer;

/**
 * Shared visual primitives for the live Default Chat replacement and its editor preview.  Keeping the
 * configured colours, opacities, rounded surfaces, tab accents, hover rows and context-menu drawing in
 * one renderer means the preview is a real rendering surface, not a separately styled mockup.
 */
public final class ChatSurfaceRenderer {
    private ChatSurfaceRenderer() { }

    public static void drawMessageBackground(float x, float y, float width, float height) {
        drawMessageBackground(x, y, width, height, ChatQolSettings.backgroundOpacityOpen);
    }

    public static void drawMessageBackground(float x, float y, float width, float height, int opacity) {
        int colour = withOpacity(ChatQolSettings.backgroundColor, opacity);
        UIRender.roundRect(x, y, width, height, ChatQolSettings.backgroundRadius, colour, colour, 1f);
    }

    public static void drawMessageLine(FontRenderer font, String formatted, float x, float y, int rgb,
                                       float alpha, float scale, boolean shadow) {
        UIRender.text(font, formatted, x, y, withAlpha(rgb, alpha), scale, shadow);
    }

    public static void drawHoverRow(float x, float y, float width, float height, float animationAlpha) {
        if (!ChatQolSettings.hoverHighlight || animationAlpha <= .003f) return;
        float alpha = ChatQolSettings.hoverOpacity / 100f * animationAlpha;
        int colour = withAlpha(ChatQolSettings.hoverColor, alpha);
        UIRender.roundRect(x, y, width, height, 0f, colour, colour, 0f);
    }

    public static void drawTab(FontRenderer font, ChatTabs.Tab tab, float x, float y, float width, float height,
                               boolean selected, boolean hot, boolean unread) {
        if (tab == null || width <= 0f || height <= 0f) return;
        float alpha = (selected ? ChatQolSettings.tabActiveOpacity : ChatQolSettings.tabOpacity) / 100f;
        int surface = selected ? ChatQolSettings.tabBackgroundColor : ChatQolSettings.tabInactiveColor;
        if (hot) {
            surface = lighter(surface, .12f);
            alpha = Math.min(1f, alpha + .05f);
        }
        int fill = withAlpha(surface, alpha);
        int border = withAlpha(lighter(surface, .18f), Math.min(1f, alpha + .18f));
        UIRender.roundRectBorder(x, y, width, height, 3f, fill, fill, border);
        String label = fit(font, tab.name(), Math.max(1f, width - 12f), .70f);
        UIRender.text(font, label, x + 5f, y + 5f, 0xFFFFFFFF, .70f, false);
        if (!tab.builtIn() && width > 8f) {
            UIRender.rect(x + 3f, y + height - 2f, width - 6f, 1.2f, withAlpha(tab.colour(), .95f));
        }
        if (unread) UIRender.circle(x + width - 5f, y + 5f, 2.2f, tab.builtIn() ? 0xFFFFFFFF : tab.colour());
    }

    public static void drawMenu(FontRenderer font, float x, float y, float width, int optionCount,
                                String first, String second, String third, int hotIndex, float hotAlpha) {
        float height = Math.max(0, optionCount) * 20f + 4f;
        UIRender.roundRectBorder(x, y, width, height, 4f, Theme.popup, Theme.popup, Theme.border);
        for (int i = 0; i < optionCount; i++) {
            float rowY = y + 2f + i * 20f;
            if (i == hotIndex && hotAlpha > .002f) UIRender.roundRect(x + 2f, rowY, width - 4f, 18f,
                    3f, UIRender.withAlpha(Theme.cardHover, hotAlpha), UIRender.withAlpha(Theme.cardHover, hotAlpha), 0f);
            String label = i == 0 ? first : i == 1 ? second : third;
            UIRender.text(font, fit(font, label, width - 14f, .78f), x + 7f, rowY + 5f,
                    i == hotIndex && hotAlpha > .5f ? Theme.textPrimary : Theme.textSecondary, .78f, false);
        }
    }

    public static int withOpacity(int rgb, int percent) {
        int alpha = Math.max(0, Math.min(255, Math.round(Math.max(0, Math.min(100, percent)) * 2.55f)));
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    public static int withAlpha(int rgb, float alpha) {
        int value = Math.max(0, Math.min(255, Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f)));
        return (value << 24) | (rgb & 0xFFFFFF);
    }

    public static int lighter(int rgb, float amount) {
        float mix = Math.max(0f, Math.min(1f, amount));
        int red = (rgb >>> 16) & 0xFF;
        int green = (rgb >>> 8) & 0xFF;
        int blue = rgb & 0xFF;
        red = Math.round(red + (255 - red) * mix);
        green = Math.round(green + (255 - green) * mix);
        blue = Math.round(blue + (255 - blue) * mix);
        return (red << 16) | (green << 8) | blue;
    }

    public static String fit(FontRenderer font, String value, float width, float scale) {
        if (font == null || value == null || width <= 0f) return "";
        float unscaled = width / Math.max(.01f, scale);
        if (font.getStringWidth(value) <= unscaled) return value;
        return font.trimStringToWidth(value, Math.max(1, (int) unscaled - 7)) + "...";
    }
}
