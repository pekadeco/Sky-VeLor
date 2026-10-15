package com.arena.pekaclient.gui;

import com.arena.pekaclient.render.UIRender;
import net.minecraft.client.gui.FontRenderer;

/**
 * The three window-control buttons in the title bar: close (red), collapse (yellow) and zoom (green).
 *
 * Each button is a solid filled disc with a darker rim. On hover the disc brightens and shows its glyph.
 * The hit area is a 14 px circle, so touch and mouse both work. A button is drawn in a disabled state
 * (dimmed, no glyph, no action) when a screen has no meaningful action for it.
 */
final class WindowDots {

    static final int CLOSE = 0;
    static final int COLLAPSE = 1;
    static final int ZOOM = 2;
    static final int NONE = -1;

    /** Disc radius. */
    private static final float R = 6f;
    /** Hit radius, a little larger than the disc for easier touch. */
    private static final float HIT = 7f;
    /** Horizontal spacing between disc centres. */
    static final float STEP = 14f;

    private static final int[] TOP = {0xFFFF7A70, 0xFFFFD35C, 0xFF55DD66};
    private static final int[] BOTTOM = {0xFFFF5F57, 0xFFFEBC2E, 0xFF28C840};
    private static final int[] RIM = {0xFFD9423C, 0xFFD9A020, 0xFF1EA534};
    private static final int DISABLED = 0x59808088;

    private WindowDots() {
    }

    /** Centre x of button {@code index} when the first button's centre is {@code firstX}. */
    static float centreX(float firstX, int index) {
        return firstX + index * STEP;
    }

    /**
     * Which button is under the pointer, or {@link #NONE}. Only buttons in {@code enabledMask} count.
     *
     * @param enabledMask bit i set means button i can be pressed
     */
    static int hit(int mx, int my, float firstX, float cy, int enabledMask) {
        for (int i = 0; i < 3; i++) {
            if ((enabledMask & (1 << i)) == 0) {
                continue;
            }
            float dx = mx - centreX(firstX, i);
            float dy = my - cy;
            if (dx * dx + dy * dy <= HIT * HIT) {
                return i;
            }
        }
        return NONE;
    }

    /**
     * The button under the pointer that is NOT live, or {@link #NONE}. Used to explain why a dimmed button does
     * nothing, so a disabled control never looks broken.
     */
    static int disabledAt(int mx, int my, float firstX, float cy, int enabledMask) {
        for (int i = 0; i < 3; i++) {
            if ((enabledMask & (1 << i)) != 0) {
                continue;
            }
            float dx = mx - centreX(firstX, i);
            float dy = my - cy;
            if (dx * dx + dy * dy <= HIT * HIT) {
                return i;
            }
        }
        return NONE;
    }

    /**
     * Shows why a dimmed button does nothing: a small tooltip next to the pointer. Screens that cannot collapse or
     * zoom call this after they draw their buttons, so the reason is always visible on hover.
     */
    static void drawHint(FontRenderer fr, int mx, int my, float firstX, float cy, int enabledMask,
                         String collapseReason, String zoomReason, int screenWidth) {
        String tip = hintFor(disabledAt(mx, my, firstX, cy, enabledMask), collapseReason, zoomReason);
        if (tip == null) {
            return;
        }
        float w = fr.getStringWidth(tip) + 12f;
        float x = Math.min(mx + 10f, screenWidth - w - 4f);
        float y = my + 14f;
        UIRender.beginFrame();
        UIRender.roundRect(x, y, w, 16f, 4f, 0xE6101820, 0xE6101820, 1f);
        UIRender.text(fr, tip, x + 6f, y + 4f, 0xFFFFFFFF, 1f, false);
        UIRender.endFrame();
    }

    /** The hint shown when a dimmed button is hovered. Each screen passes its own reason. */
    static String hintFor(int index, String collapseReason, String zoomReason) {
        if (index == COLLAPSE) {
            return collapseReason;
        }
        if (index == ZOOM) {
            return zoomReason;
        }
        return null;
    }

    /**
     * Draws the three buttons.
     *
     * @param zoomed    true when the window is zoomed, which changes the zoom glyph
     * @param enabledMask bit i set means button i is live. Other buttons are drawn dimmed
     */
    static void draw(float firstX, float cy, int mx, int my, int enabledMask, boolean zoomed) {
        boolean group = false;
        for (int i = 0; i < 3; i++) {
            if (hit(mx, my, firstX, cy, enabledMask) == i) {
                group = true;
            }
        }
        for (int i = 0; i < 3; i++) {
            float x = centreX(firstX, i);
            boolean live = (enabledMask & (1 << i)) != 0;
            if (!live) {
                UIRender.circle(x, cy, R, DISABLED);
                continue;
            }
            UIRender.circle(x, cy, R, RIM[i]);
            UIRender.roundRect(x - R + 0.8f, cy - R + 0.8f, (R - 0.8f) * 2f, (R - 0.8f) * 2f,
                    R - 0.8f, TOP[i], BOTTOM[i], 0.6f);
            if (group) {
                drawGlyph(i, x, cy, zoomed);
            }
        }
    }

    private static void drawGlyph(int index, float x, float cy, boolean zoomed) {
        int ink = 0xB3000000;
        switch (index) {
            case CLOSE:
                UIRender.line(x - 2.2f, cy - 2.2f, x + 2.2f, cy + 2.2f, 1.1f, ink);
                UIRender.line(x + 2.2f, cy - 2.2f, x - 2.2f, cy + 2.2f, 1.1f, ink);
                break;
            case COLLAPSE:
                UIRender.line(x - 2.6f, cy, x + 2.6f, cy, 1.2f, ink);
                break;
            default:
                // Zoom: two diagonal arrows pointing outwards when normal, inwards when zoomed.
                float s = zoomed ? -1f : 1f;
                UIRender.line(x - 1.6f * s, cy - 1.6f * s, x - 2.8f * s, cy - 2.8f * s, 1.0f, ink);
                UIRender.line(x + 1.6f * s, cy + 1.6f * s, x + 2.8f * s, cy + 2.8f * s, 1.0f, ink);
                break;
        }
    }
}
