package com.arena.pekaclient.widget;

import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Horizontal slider. Value label sits to the left of the track. Tap or drag anywhere
 * on the track to set the value. Dragging never scrolls the list underneath.
 */
public final class AppleSlider extends Widget {

    private static final float VALUE_W = 36f;
    private static final float KNOB_R = 7f;

    private final int min;
    private final int max;
    private final int step;
    private final String suffix;
    private final IntSupplier get;
    private final IntConsumer set;

    public AppleSlider(int min, int max, int step, String suffix, float w,
                       IntSupplier get, IntConsumer set) {
        this.min = min;
        this.max = max;
        this.step = Math.max(1, step);
        this.suffix = suffix;
        this.get = get;
        this.set = set;
        this.w = w;
        this.h = 24f;
    }

    private float trackX0() {
        return x + VALUE_W + 8f;
    }

    private float trackX1() {
        return x + w - 8f;
    }

    @Override
    public void draw(int mx, int my) {
        hovered(mx, my);
        int v = get.getAsInt();
        float frac = max == min ? 0f : (v - min) / (float) (max - min);
        float tx0 = trackX0();
        float tx1 = trackX1();
        float cy = y + h * 0.5f;
        float kx = tx0 + frac * (tx1 - tx0);

        UIRender.roundRect(tx0, cy - 2f, tx1 - tx0, 4f, 2f, Theme.track, Theme.track, 1f);
        if (kx > tx0) {
            UIRender.roundRect(tx0, cy - 2f, kx - tx0, 4f, 2f, Theme.accent, Theme.accent, 1f);
        }

        float kr = KNOB_R + press.get() * 1.5f + hover.get() * 0.5f;
        UIRender.roundRect(kx - kr, cy - kr + 1f, kr * 2f, kr * 2f, kr, 0x40000000, 0x40000000, 1.5f);
        UIRender.roundRect(kx - kr, cy - kr, kr * 2f, kr * 2f, kr, 0xFFFFFFFF, 0xFFFFFFFF, 1f);

        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        String s = v + suffix;
        UIRender.text(fr, s, x + VALUE_W - fr.getStringWidth(s), cy - 4f, Theme.textSecondary, 1f, false);
    }

    private void setFromX(int mx) {
        float tx0 = trackX0();
        float tx1 = trackX1();
        float f = (mx - tx0) / (tx1 - tx0);
        if (f < 0f) {
            f = 0f;
        } else if (f > 1f) {
            f = 1f;
        }
        int steps = Math.round(f * (max - min) / (float) step);
        int v = Math.min(max, min + steps * step);
        if (v != get.getAsInt()) {
            set.accept(v);
        }
    }

    @Override
    public void onPress(int mx, int my) {
        super.onPress(mx, my);
        setFromX(mx);
    }

    @Override
    public void onDrag(int mx, int my) {
        setFromX(mx);
    }

    @Override
    public void onRelease(int mx, int my, boolean activate) {
        super.onRelease(mx, my, false);
    }
}
