package com.arena.pekaclient.widget;

import com.arena.pekaclient.render.Anim;
import com.arena.pekaclient.render.Icons;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Dropdown. The face is drawn inline; the popup is drawn by the screen in an overlay
 * pass so it is never clipped by the scrolling list. Only one popup is open at a time.
 */
public final class AppleDropdown extends Widget {

    public static final float ITEM_H = 22f;
    private static final float POP_MIN_W = 150f;

    private final String[] options;
    private final int[] dots;
    private final IntSupplier get;
    private final IntConsumer set;
    private final Anim open = new Anim(0f);

    private float popX, popY, popW, popH;

    /**
     * @param dots optional per-option colour dots (may be null)
     */
    public AppleDropdown(String[] options, int[] dots, float w, float h,
                         IntSupplier get, IntConsumer set) {
        this.options = options;
        this.dots = dots;
        this.w = w;
        this.h = h;
        this.get = get;
        this.set = set;
    }

    /** Called by the screen when this dropdown becomes the open one. */
    public void onExpand() {
        open.snap(0f);
        open.to(1f);
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        open.update(dt, 22f);
    }

    private int current() {
        return Math.max(0, Math.min(options.length - 1, get.getAsInt()));
    }

    @Override
    public void draw(int mx, int my) {
        hovered(mx, my);
        int cur = current();
        int fill = UIRender.mix(Theme.buttonFill, Theme.buttonHover, hover.get());
        float r = Math.min(Theme.radius, h * 0.5f);
        UIRender.roundRect(x, y, w, h, r, fill, fill, 1f);

        float cy = y + h * 0.5f;
        float tx = x + 10f;
        if (dots != null) {
            UIRender.circle(x + 14f, cy, 4f, dots[cur]);
            tx = x + 24f;
        }
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        UIRender.text(fr, options[cur], tx, y + (h - 9f) * 0.5f + 1f, Theme.textPrimary, 1f, false);
        Icons.draw(Icons.CHEVRON, x + w - 11f, cy, 8f, Theme.textSecondary);
    }

    /**
     * Draws the popup list. Call from the screen after the main content.
     * Opens upwards when there is no room below.
     */
    public void drawPopup(int mx, int my, float winTop, float winBottom, float winRight, float winLeft) {
        float a = open.get();
        if (a <= 0.01f) {
            return;
        }
        int n = options.length;
        float ph = n * ITEM_H + 8f;
        float pw = Math.max(w, POP_MIN_W);
        float px = x + w - pw;
        if (px < winLeft + 4f) {
            px = winLeft + 4f;
        }
        if (px + pw > winRight - 4f) {
            px = winRight - 4f - pw;
        }
        float py = y + h + 6f;
        boolean above = py + ph > winBottom - 4f;
        if (above) {
            py = y - 6f - ph;
        }
        if (py < winTop + 4f) {
            py = winTop + 4f;
        }
        popX = px;
        popY = py;
        popW = pw;
        popH = ph;

        float prev = UIRender.alphaMul;
        UIRender.alphaMul = prev * a;
        UIRender.shadow(px, py, pw, ph, Theme.radius);
        UIRender.roundRectBorder(px, py, pw, ph, Math.min(Theme.radius, 12f), Theme.popup, Theme.popup, Theme.border);

        int cur = current();
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        for (int i = 0; i < n; i++) {
            float iy = py + 4f + i * ITEM_H;
            boolean hi = mx >= px + 4f && mx < px + pw - 4f && my >= iy && my < iy + ITEM_H;
            if (hi) {
                UIRender.roundRect(px + 4f, iy, pw - 8f, ITEM_H, 6f, Theme.cardHover, Theme.cardHover, 1f);
            }
            float tx = px + 12f;
            if (dots != null) {
                UIRender.circle(px + 16f, iy + ITEM_H * 0.5f, 4f, dots[i]);
                tx = px + 28f;
            }
            UIRender.text(fr, options[i], tx, iy + (ITEM_H - 9f) * 0.5f + 1f, Theme.textPrimary, 1f, false);
            if (i == cur) {
                Icons.draw(Icons.CHECK, px + pw - 12f, iy + ITEM_H * 0.5f, 8f, Theme.accent);
            }
        }
        UIRender.alphaMul = prev;
    }

    /** Index of the option under the pointer in the open popup, or -1. */
    public int optionAt(int mx, int my) {
        if (!popupContains(mx, my)) {
            return -1;
        }
        float rel = my - popY - 4f;
        if (rel < 0f) {
            return -1;
        }
        int i = (int) (rel / ITEM_H);
        return i < options.length ? i : -1;
    }

    public boolean popupContains(int mx, int my) {
        return mx >= popX && mx < popX + popW && my >= popY && my < popY + popH;
    }

    public void choose(int index) {
        if (index != get.getAsInt()) {
            set.accept(index);
        }
    }

    @Override
    public void onRelease(int mx, int my, boolean activate) {
        super.onRelease(mx, my, activate);
        // Opening/closing the popup is handled by the screen (it owns the single open popup).
    }
}
