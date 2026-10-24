package com.arena.pekaclient.widget;

import com.arena.pekaclient.render.GuiMotion;
import com.arena.pekaclient.render.GuiStyle;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class AppleToggle extends Widget {
    private final Supplier<Boolean> getter;
    private final Consumer<Boolean> setter;
    private float animatedOn;
    private boolean initialized;

    public AppleToggle(Supplier<Boolean> getter, Consumer<Boolean> setter) {
        super(38f, 20f);
        this.getter = getter;
        this.setter = setter;
    }

    private boolean value() {
        return getter != null && Boolean.TRUE.equals(getter.get());
    }

    @Override
    protected void updateWidget(float dt) {
        float target = value() ? 1f : 0f;
        if (!initialized) {
            animatedOn = target;
            initialized = true;
        } else {
            animatedOn = ease(animatedOn, target, dt, GuiMotion.TOGGLE_SETTLE_MS);
        }
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        beginAnimatedDraw(0.025f, 0.04f, 0.018f);
        float state = initialized ? animatedOn : (value() ? 1f : 0f);
        int fill = UIRender.mix(Theme.track, Theme.accent, state);
        if (contains(mouseX, mouseY) && enabled) {
            fill = UIRender.mix(fill, 0xFFFFFFFF, hoverAmount * 0.16f);
        }
        if (!enabled) {
            fill = UIRender.mix(fill, Theme.windowBottom, 0.45f);
        }
        float radius = GuiStyle.pillRadius(Theme.radius, w, h);
        UIRender.roundRect(x, y, w, h, radius, fill, fill, 0.5f);
        float pad = 2f;
        float knob = h - pad * 2f;
        float left = x + pad;
        float right = x + w - pad - knob;
        float kx = left + (right - left) * state;
        if (pressed && contains(mouseX, mouseY)) {
            kx = clamp(mouseX - knob * 0.5f, left, right);
        }
        if (animationsActive() && clickPulse > 0.015f && enabled) {
            float outer = knob * 0.5f + 2f + (1f - clickPulse) * 1.2f;
            UIRender.ring(kx + knob * 0.5f, y + h * 0.5f, outer, outer - 0.9f,
                    UIRender.withAlpha(Theme.accent, clickPulse * 0.25f));
        }
        UIRender.circle(kx + knob * 0.5f, y + h * 0.5f, knob * 0.5f,
                enabled ? 0xFFFFFFFF : 0xFFB4B4B8);
        endAnimatedDraw();
    }

    @Override
    public void onRelease(int mouseX, int mouseY, boolean activate) {
        boolean change = pressed && activate && enabled;
        super.onRelease(mouseX, mouseY, activate);
        if (change && setter != null) {
            setter.accept(!value());
        }
    }
}
