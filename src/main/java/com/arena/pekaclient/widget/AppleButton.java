package com.arena.pekaclient.widget;

import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

/** Rounded, animated button. Fires on release so touch-scrolling never triggers it. */
public final class AppleButton extends Widget {

    private final String label;
    private final boolean primary;
    private final Runnable action;

    public AppleButton(String label, float w, float h, boolean primary, Runnable action) {
        this.label = label;
        this.w = w;
        this.h = h;
        this.primary = primary;
        this.action = action;
    }

    @Override
    public void draw(int mx, int my) {
        hovered(mx, my);
        float hv = hover.get();
        float inset = press.get() * 1f;
        float r = Math.min(Theme.radius, h * 0.5f);

        int fill;
        int textCol;
        if (primary) {
            fill = UIRender.mix(Theme.accent, 0xFFFFFFFF, hv * 0.10f);
            textCol = 0xFFFFFFFF;
        } else {
            fill = UIRender.mix(Theme.buttonFill, Theme.buttonHover, hv);
            textCol = Theme.accent;
        }
        if (!enabled) {
            fill = Theme.track;
            textCol = Theme.textTertiary;
        }
        UIRender.roundRect(x + inset, y + inset, w - inset * 2f, h - inset * 2f, r, fill, fill, 1f);

        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        float tx = x + (w - fr.getStringWidth(label)) * 0.5f;
        UIRender.text(fr, label, tx, y + (h - 9f) * 0.5f + 1f, textCol, 1f, false);
    }

    @Override
    public void onRelease(int mx, int my, boolean activate) {
        super.onRelease(mx, my, activate);
        if (activate && enabled && action != null) {
            action.run();
        }
    }
}
