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

        int track = UIRender.mix(Theme.track, Theme.accent, k);
        UIRender.roundRect(x, y, w, h, h * 0.5f, track, track, 1f);

        float kd = h - 4f + p * 3f;
        float travel = w - h;
        float kx = x + 2f + k * travel - p * 1.5f;
        float ky = y + (h - kd) * 0.5f;
        UIRender.roundRect(kx, ky + 1f, kd, kd, kd * 0.5f, 0x40000000, 0x40000000, 1.5f);
        UIRender.roundRect(kx, ky, kd, kd, kd * 0.5f, 0xFFFFFFFF, 0xFFFFFFFF, 1f);
    }

    @Override
    public void onRelease(int mx, int my, boolean activate) {
        super.onRelease(mx, my, activate);
        if (activate && enabled) {
            set.accept(!get.get());
        }
    }
}
