package com.arena.pekaclient.widget;

import com.arena.pekaclient.render.Anim;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Segmented control: a pill with one selected segment that slides between options. */
public final class AppleSegmented extends Widget {

    private static final float PAD = 2f;

    private final String[] options;
    private final IntSupplier get;
    private final IntConsumer set;
    private final Anim sel;

    public AppleSegmented(String[] options, float w, float h, IntSupplier get, IntConsumer set) {
        this.options = options;
        this.w = w;
        this.h = h;
        this.get = get;
        this.set = set;
        this.sel = new Anim(get.getAsInt());
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        sel.update(dt, 20f);
    }

    @Override
    public void draw(int mx, int my) {
        hovered(mx, my);
        int n = options.length;
        int cur = get.getAsInt();
        sel.to(cur);

        float inner = h - PAD * 2f;
        UIRender.roundRect(x, y, w, h, h * 0.5f, Theme.track, Theme.track, 1f);
        float segW = (w - PAD * 2f) / n;
        float px = x + PAD + sel.get() * segW;
        UIRender.roundRect(px, y + PAD, segW, inner, inner * 0.5f, Theme.segSelected, Theme.segSelected, 1f);

        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        for (int i = 0; i < n; i++) {
            float tw = fr.getStringWidth(options[i]);
            float tx = x + PAD + i * segW + (segW - tw) * 0.5f;
            int c = i == cur ? Theme.textPrimary : Theme.textSecondary;
            UIRender.text(fr, options[i], tx, y + (h - 9f) * 0.5f + 1f, c, 1f, false);
        }
    }

    @Override
    public void onRelease(int mx, int my, boolean activate) {
        super.onRelease(mx, my, activate);
        if (!activate) {
            return;
        }
        int n = options.length;
        float segW = (w - PAD * 2f) / n;
        int idx = (int) ((mx - x - PAD) / segW);
        if (idx < 0) {
            idx = 0;
        } else if (idx >= n) {
            idx = n - 1;
        }
        if (idx != get.getAsInt()) {
            set.accept(idx);
        }
    }
}
