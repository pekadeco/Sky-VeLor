package com.arena.pekaclient.widget;

import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class AppleSlider extends Widget {
    private final int min;
    private final int max;
    private final int step;
    private final String suffix;
    private final IntSupplier getter;
    private final IntConsumer setter;
    private Supplier<Boolean> gate;
    private float displayedFraction;
    private boolean fractionInitialized;
    /** Brief knob/value acknowledgement while the player changes the setting. */
    private float valuePulse;

    public AppleSlider(int min, int max, int step, String suffix, float width,
                       IntSupplier getter, IntConsumer setter) {
        super(width, 22f);
        this.min = Math.min(min, max);
        this.max = Math.max(min, max);
        this.step = Math.max(1, step);
        this.suffix = suffix == null ? "" : suffix;
        this.getter = getter;
        this.setter = setter;
    }

    public AppleSlider gate(Supplier<Boolean> enabledWhen) {
        this.gate = enabledWhen;
        return this;
    }

    private boolean allowed() {
        return enabled && (gate == null || Boolean.TRUE.equals(gate.get()));
    }

    private int value() {
        int current = getter == null ? min : getter.getAsInt();
        return Math.max(min, Math.min(max, current));
    }

    private float fraction() {
        return max == min ? 0f : (value() - min) / (float) (max - min);
    }

    @Override
    protected void updateWidget(float dt) {
        float target = fraction();
        if (!fractionInitialized) {
            displayedFraction = target;
            fractionInitialized = true;
        } else {
            displayedFraction = approach(displayedFraction, target, dt, pressed ? 42f : 18f);
        }
        valuePulse = approach(valuePulse, 0f, dt, 9f);
    }

    private void setFromX(float px) {
        if (!allowed() || setter == null || max <= min) {
            return;
        }
        float trackLeft = x + 7f;
        float trackWidth = Math.max(1f, w - 14f);
        float f = clamp((px - trackLeft) / trackWidth, 0f, 1f);
        int raw = min + Math.round(f * (max - min));
        int stepped = min + Math.round((raw - min) / (float) step) * step;
        stepped = Math.max(min, Math.min(max, stepped));
        if (stepped != value()) {
            setter.accept(stepped);
            valuePulse = 1f;
        }
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        beginAnimatedDraw(0.022f, 0.028f, 0.009f);
        boolean active = allowed();
        float trackLeft = x + 7f;
        float trackW = Math.max(1f, w - 14f);
        float cy = y + h * 0.62f;
        float radius = 2f;
        float frac = fractionInitialized ? displayedFraction : fraction();
        float end = trackLeft + trackW * frac;
        UIRender.roundRect(trackLeft, cy - radius, trackW, radius * 2f, radius,
                Theme.track, Theme.track, 0.4f);
        UIRender.roundRect(trackLeft, cy - radius, Math.max(0f, end - trackLeft), radius * 2f, radius,
                active ? Theme.accent : Theme.textTertiary,
                active ? Theme.accent : Theme.textTertiary, 0.4f);
        float animatedHover = Theme.animations && active ? hoverAmount : 0f;
        float animatedValue = Theme.animations ? valuePulse : 0f;
        float knobR = 4f + animatedHover * 0.35f + animatedValue * 0.45f;
        if (active && Theme.animations && valuePulse > 0.015f) {
            float outer = knobR + 1.1f + (1f - valuePulse) * 1.1f;
            UIRender.ring(end, cy, outer, Math.max(knobR + 0.4f, outer - 0.9f),
                    UIRender.withAlpha(Theme.accent, valuePulse * 0.24f));
        }
        UIRender.circle(end, cy, knobR, active ? 0xFFFFFFFF : Theme.textTertiary);
        UIRender.circle(end, cy, knobR - 1.2f, active ? Theme.accent : Theme.track);
        String text = value() + suffix;
        int color = active ? UIRender.mix(Theme.textSecondary, Theme.accent, valuePulse * 0.5f)
                : Theme.textTertiary;
        UIRender.text(font(), text, x + w - font().getStringWidth(text), y + 1f, color, 0.8f, false);
        if (!active) {
            UIRender.roundRect(x, y, w, h, 4f, 0x24000000, 0x24000000, 0f);
        }
        endAnimatedDraw();
    }

    @Override
    public void onPress(int mouseX, int mouseY) {
        super.onPress(mouseX, mouseY);
        if (pressed && allowed()) {
            setFromX(mouseX);
        }
    }

    @Override
    public void onDrag(int mouseX, int mouseY) {
        if (pressed && allowed()) {
            setFromX(mouseX);
        }
    }
}
