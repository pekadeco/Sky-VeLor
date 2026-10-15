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
    private static final int OFF_RING = 0xFF8A8A92;
    private static final float RING_WIDTH = 1.5f;
    private static final int KNOB_RING = 0xFFC8C8D0;

    private final int min;
    private final int max;
    private final int step;
    private final String suffix;
    private final IntSupplier get;
    private final IntConsumer set;
    private java.util.function.Supplier<Boolean> gate;

    /** When the gate returns false the slider is drawn dimmed and ignores input. */
    public AppleSlider gate(java.util.function.Supplier<Boolean> gate) {
        this.gate = gate;
        return this;
    }

    private boolean active() {
        return gate == null || gate.get();
    }

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

        // Outline style, matching the switches: grey track ring, accent ring over the filled part.
        boolean on = active();
        UIRender.roundRectRing(tx0, cy - 3f, tx1 - tx0, 6f, 3f, RING_WIDTH, OFF_RING);
        if (on && kx - tx0 > 6f) {
            UIRender.roundRectRing(tx0, cy - 3f, kx - tx0, 6f, 3f, RING_WIDTH, Theme.accent);
        }

        float kr = KNOB_R + press.get() * 1.5f + hover.get() * 0.5f;
        // Knob is a light grey ring, as in the reference screenshot. The track fill carries the accent.
        UIRender.roundRectRing(kx - kr, cy - kr, kr * 2f, kr * 2f, kr, RING_WIDTH, on ? KNOB_RING : OFF_RING);

        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        String s = v + suffix;
        UIRender.text(fr, s, x + VALUE_W - fr.getStringWidth(s), cy - 4f, Theme.textSecondary, 1f, false);
    }

    private void setFromX(int mx) {
        if (!active()) {
            return;
        }
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
