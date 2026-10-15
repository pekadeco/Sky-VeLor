package com.arena.pekaclient.widget;

import com.arena.pekaclient.render.Anim;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** iOS-style switch. The knob slides and the track cross-fades to the accent colour. */
public final class AppleToggle extends Widget {

    private final Supplier<Boolean> get;
    private final Consumer<Boolean> set;
    private final Anim knob = new Anim(0f);
    private static final int OFF_RING = 0xFF8A8A92;
    private static final float RING_WIDTH = 1.5f;

    public AppleToggle(Supplier<Boolean> get, Consumer<Boolean> set) {
        this.get = get;
        this.set = set;
        this.w = 40f;
        this.h = 22f;
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        knob.update(dt, 18f);
    }

    @Override
    public void draw(int mx, int my) {
        hovered(mx, my);
        boolean on = get.get();
        knob.to(on ? 1f : 0f);
        float k = knob.get();
        float p = press.get();

        // Outline style: a thin ring around the pill, and a ring knob. Accent when on, grey when off.
        int ring = UIRender.mix(OFF_RING, Theme.accent, k);
        UIRender.roundRectRing(x, y, w, h, h * 0.5f, RING_WIDTH, ring);

        float kd = h - 8f + p * 2f;
        float travel = w - h;
        float kx = x + 4f + k * travel - p * 1f;
        float ky = y + (h - kd) * 0.5f;
        UIRender.roundRectRing(kx, ky, kd, kd, kd * 0.5f, RING_WIDTH, ring);
    }

    @Override
    public void onRelease(int mx, int my, boolean activate) {
        super.onRelease(mx, my, activate);
        if (activate && enabled) {
            set.accept(!get.get());
        }
    }
}
