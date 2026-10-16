package com.arena.pekaclient.widget;

import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;

public final class AppleButton extends Widget {
    private final String label;
    private final boolean primary;
    private final Runnable action;

    public AppleButton(String label, float width, float height, boolean primary, Runnable action) {
        super(width, height);
        this.label = label == null ? "" : label;
        this.primary = primary;
        this.action = action;
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        beginAnimatedDraw(0.045f, 0.065f, 0.028f);
        int fill = primary ? Theme.accent : Theme.buttonFill;
        int hover = primary ? UIRender.mix(Theme.accent, 0xFFFFFFFF, 0.12f) : Theme.buttonHover;
        fill = UIRender.mix(fill, hover, enabled ? hoverAmount : 0f);
        fill = UIRender.mix(fill, 0xFF000000, enabled ? pressAmount * 0.12f : 0f);
        if (!enabled) {
            fill = UIRender.mix(fill, Theme.windowBottom, 0.45f);
        }

        float radius = Math.min(Theme.radius * 0.65f, h * 0.5f);
        UIRender.roundRect(x, y, w, h, radius, fill, fill, 0.5f);
        if (!primary) {
            drawBorder(radius, Theme.border);
        }
        int textColor = primary ? 0xFFFFFFFF : Theme.textPrimary;
        if (!enabled) {
            textColor = Theme.textTertiary;
        }
        String shown = label;
        int max = Math.max(0, (int) (w - 12f));
        if (font().getStringWidth(shown) > max) {
            shown = font().trimStringToWidth(shown, Math.max(0, max - 5)) + "…";
        }
        float tx = x + (w - font().getStringWidth(shown)) * 0.5f;
        float ty = y + (h - 8f) * 0.5f + 1f;
        UIRender.text(font(), shown, tx, ty, textColor, 1f, false);
        endAnimatedDraw();
    }

    @Override
    public void onRelease(int mouseX, int mouseY, boolean activate) {
        boolean fire = pressed && activate && enabled;
        super.onRelease(mouseX, mouseY, activate);
        if (fire && action != null) {
            action.run();
        }
    }
}
