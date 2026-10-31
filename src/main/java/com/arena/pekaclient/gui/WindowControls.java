package com.arena.pekaclient.gui;

import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import net.minecraft.client.gui.FontRenderer;

/**
 * Minimal monochrome title-bar controls shared by the settings hub and HUD editor.
 * The controls use familiar line icons rather than coloured window-status lights.
 */
final class WindowControls {

    static final int CLOSE = 0;
    static final int COLLAPSE = 1;
    static final int ZOOM = 2;
    static final int NONE = -1;

    private static final float HIT = 7f;
    static final float STEP = 14f;
    private static final float ICON_HALF = 2.4f;
    private static final float HOVER_HALF = 5.5f;
    private static final float[] hoverAmount = new float[3];
    private static long lastFrameNs;

    private WindowControls() {
    }

    /** Centre x of button {@code index} when the first button's centre is {@code firstX}. */
    static float centreX(float firstX, int index) {
        return firstX + index * STEP;
    }

    /** Which enabled control is under the pointer, or {@link #NONE}. */
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

    /** Which disabled control is under the pointer, or {@link #NONE}. */
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

    /** Draws a tooltip explaining a disabled control when it is hovered. */
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

    static String hintFor(int index, String collapseReason, String zoomReason) {
        if (index == COLLAPSE) {
            return collapseReason;
        }
        if (index == ZOOM) {
            return zoomReason;
        }
        return null;
    }

    /** Draws neutral, monochrome close, collapse and zoom controls. */
    static void draw(float firstX, float cy, int mx, int my, int enabledMask, boolean zoomed) {
        long now = System.nanoTime();
        float dt = lastFrameNs == 0L ? 1f / 60f : Math.min(0.1f, (now - lastFrameNs) / 1.0e9f);
        lastFrameNs = now;

        for (int i = 0; i < 3; i++) {
            float x = centreX(firstX, i);
            boolean live = (enabledMask & (1 << i)) != 0;
            float dx = mx - x;
            float dy = my - cy;
            boolean hot = live && dx * dx + dy * dy <= HIT * HIT;
            float target = hot ? 1f : 0f;
            if (!Theme.animations) {
                hoverAmount[i] = target;
            } else {
                float ease = 1f - (float) Math.exp(-dt * 22f * Math.max(0.1f, Theme.speed));
                hoverAmount[i] += (target - hoverAmount[i]) * ease;
                if (Math.abs(target - hoverAmount[i]) < 0.0015f) {
                    hoverAmount[i] = target;
                }
            }

            float hv = hoverAmount[i];
            if (live && hv > 0.001f) {
                int hover = UIRender.mix(0x00000000, Theme.buttonHover, hv * 0.72f);
                UIRender.roundRect(x - HOVER_HALF, cy - HOVER_HALF, HOVER_HALF * 2f,
                        HOVER_HALF * 2f, 2.5f, hover, hover, 0f);
            }

            int ink = live
                    ? UIRender.mix(Theme.textSecondary, Theme.textPrimary, hot ? 0.65f : 0f)
                    : UIRender.withAlpha(Theme.textTertiary, 0.48f);
            drawGlyph(i, x, cy, zoomed, ink);
        }
    }

    private static void drawGlyph(int index, float x, float y, boolean zoomed, int ink) {
        switch (index) {
            case CLOSE:
                UIRender.line(x - 2.2f, y - 2.2f, x + 2.2f, y + 2.2f, 1.15f, ink);
                UIRender.line(x + 2.2f, y - 2.2f, x - 2.2f, y + 2.2f, 1.15f, ink);
                break;
            case COLLAPSE:
                UIRender.line(x - ICON_HALF, y, x + ICON_HALF, y, 1.2f, ink);
                break;
            default:
                if (zoomed) {
                    // Four short corner strokes indicate that clicking restores the normal window size.
                    UIRender.line(x - 2.5f, y - 1.0f, x - 2.5f, y - 2.5f, 1.0f, ink);
                    UIRender.line(x - 2.5f, y - 2.5f, x - 1.0f, y - 2.5f, 1.0f, ink);
                    UIRender.line(x + 1.0f, y - 2.5f, x + 2.5f, y - 2.5f, 1.0f, ink);
                    UIRender.line(x + 2.5f, y - 2.5f, x + 2.5f, y - 1.0f, 1.0f, ink);
                    UIRender.line(x - 2.5f, y + 1.0f, x - 2.5f, y + 2.5f, 1.0f, ink);
                    UIRender.line(x - 2.5f, y + 2.5f, x - 1.0f, y + 2.5f, 1.0f, ink);
                    UIRender.line(x + 1.0f, y + 2.5f, x + 2.5f, y + 2.5f, 1.0f, ink);
                    UIRender.line(x + 2.5f, y + 2.5f, x + 2.5f, y + 1.0f, 1.0f, ink);
                } else {
                    // Outlined square means enlarge.
                    UIRender.line(x - ICON_HALF, y - ICON_HALF, x + ICON_HALF, y - ICON_HALF, 1.0f, ink);
                    UIRender.line(x + ICON_HALF, y - ICON_HALF, x + ICON_HALF, y + ICON_HALF, 1.0f, ink);
                    UIRender.line(x + ICON_HALF, y + ICON_HALF, x - ICON_HALF, y + ICON_HALF, 1.0f, ink);
                    UIRender.line(x - ICON_HALF, y + ICON_HALF, x - ICON_HALF, y - ICON_HALF, 1.0f, ink);
                }
                break;
        }
    }
}
