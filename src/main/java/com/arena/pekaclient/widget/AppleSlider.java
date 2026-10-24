package com.arena.pekaclient.widget;

import com.arena.pekaclient.render.GuiMotion;
import com.arena.pekaclient.render.GuiStyle;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import org.lwjgl.input.Keyboard;

import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class AppleSlider extends Widget {
    private final int min;
    private final int max;
    private final int step;
    private final String suffix;
    private final IntSupplier getter;
    private final IntConsumer setter;
    private IntFunction<String> labelFormatter;
    private Supplier<Boolean> gate;
    /** Holding Shift uses this smaller step; unchanged sliders retain their ordinary step. */
    private int fineStep;
    /** Optional per-control reset action, invoked by a double-click/tap. */
    private Runnable doubleClickAction;
    /** Compact composite rows supply their own number box, so they can suppress this slider's value label. */
    private boolean showValue = true;
    private long lastPressNanos;
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
        this.fineStep = this.step;
    }

    /** Lets a fast slider become one-unit precise while either Shift key is held. */
    public AppleSlider fineStep(int value) {
        this.fineStep = Math.max(1, value);
        return this;
    }

    /** Lets a setting return to its own default without opening a separate reset dialog. */
    public AppleSlider onDoubleClick(Runnable action) {
        this.doubleClickAction = action;
        return this;
    }

    /** Formats the visible value without changing the integer setting; useful for the explicit `Off` zero state. */
    public AppleSlider valueLabel(IntFunction<String> formatter) {
        this.labelFormatter = formatter;
        return this;
    }

    /** Hides the built-in value label when a compact row provides a separate exact-value field. */
    public AppleSlider hideValue() {
        this.showValue = false;
        return this;
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
            displayedFraction = ease(displayedFraction, target, dt,
                    pressed ? GuiMotion.PRESS_SETTLE_MS : GuiMotion.SLIDER_SETTLE_MS);
        }
        valuePulse = ease(valuePulse, 0f, dt, GuiMotion.PULSE_SETTLE_MS);
    }

    private void setFromX(float px) {
        if (!allowed() || setter == null || max <= min) {
            return;
        }
        float trackLeft = x + 7f;
        float trackWidth = Math.max(1f, w - 14f);
        float f = clamp((px - trackLeft) / trackWidth, 0f, 1f);
        int raw = min + Math.round(f * (max - min));
        boolean shiftFine = Keyboard.isCreated() && (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)
                || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
        int activeStep = shiftFine ? fineStep : step;
        int stepped = min + Math.round((raw - min) / (float) activeStep) * activeStep;
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
        float animatedHover = animationsActive() && active ? hoverAmount : 0f;
        float animatedValue = animationsActive() ? valuePulse : 0f;
        float knobR = 4f + animatedHover * 0.35f + animatedValue * 0.45f;
        if (active && animationsActive() && valuePulse > 0.015f) {
            float outer = knobR + 1.1f + (1f - valuePulse) * 1.1f;
            UIRender.ring(end, cy, outer, Math.max(knobR + 0.4f, outer - 0.9f),
                    UIRender.withAlpha(Theme.accent, valuePulse * 0.24f));
        }
        UIRender.circle(end, cy, knobR, active ? 0xFFFFFFFF : Theme.textTertiary);
        UIRender.circle(end, cy, knobR - 1.2f, active ? Theme.accent : Theme.track);
        if (showValue) {
            String text = labelFormatter == null ? value() + suffix : labelFormatter.apply(value());
            if (text == null) text = "";
            int color = active ? UIRender.mix(Theme.textSecondary, Theme.accent, valuePulse * 0.5f)
                    : Theme.textTertiary;
            String shown = fitText(text, Math.max(0f, (w - 4f) / .8f));
            UIRender.text(font(), shown, x + Math.max(0f, w - textWidth(shown) * .8f),
                    y + Math.max(0f, (h - textHeight() * .8f) * .15f), color, 0.8f, false);
        }
        if (!active) {
            UIRender.roundRect(x, y, w, h,
                    GuiStyle.radius(GuiStyle.RADIUS_SMALL, Theme.radius, w, h), 0x24000000, 0x24000000, 0f);
        }
        endAnimatedDraw();
    }

    @Override
    public void onPress(int mouseX, int mouseY) {
        super.onPress(mouseX, mouseY);
        if (pressed && allowed()) {
            long now = System.nanoTime();
            if (doubleClickAction != null && lastPressNanos != 0L && now - lastPressNanos <= 340000000L) {
                lastPressNanos = 0L;
                doubleClickAction.run();
                valuePulse = 1f;
                return;
            }
            lastPressNanos = now;
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
