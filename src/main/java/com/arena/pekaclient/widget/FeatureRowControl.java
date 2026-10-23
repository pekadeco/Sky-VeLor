package com.arena.pekaclient.widget;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Compact hub-row action strip: optional existing toggle plus one Open Settings route, never extra inline controls. */
public final class FeatureRowControl extends Widget {
    private final AppleToggle toggle;
    private final AppleButton open;
    private Widget active;

    public FeatureRowControl(Runnable openAction) {
        this(null, null, openAction);
    }

    public FeatureRowControl(Supplier<Boolean> getter, Consumer<Boolean> setter, Runnable openAction) {
        super(getter == null ? 106f : 154f, 24f);
        toggle = getter == null ? null : new AppleToggle(getter, setter);
        open = new AppleButton("Open Settings", 104f, 24f, false, openAction);
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        if (toggle != null) toggle.update(dt);
        open.update(dt);
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        layoutChildren();
        if (toggle != null) toggle.draw(mouseX, mouseY);
        open.draw(mouseX, mouseY);
    }

    @Override
    public void onPress(int mouseX, int mouseY) {
        layoutChildren();
        active = toggle != null && toggle.contains(mouseX, mouseY) ? toggle : open.contains(mouseX, mouseY) ? open : null;
        if (active != null) active.onPress(mouseX, mouseY);
    }

    @Override
    public void onDrag(int mouseX, int mouseY) {
        if (active != null) active.onDrag(mouseX, mouseY);
    }

    @Override
    public void onRelease(int mouseX, int mouseY, boolean activate) {
        if (active == null) return;
        Widget released = active;
        active = null;
        released.onRelease(mouseX, mouseY, activate && released.contains(mouseX, mouseY));
    }

    @Override
    public void cancel() {
        if (active != null) active.cancel();
        active = null;
    }

    private void layoutChildren() {
        if (toggle == null) {
            open.x = x;
            open.y = y;
            open.w = w;
            open.h = h;
            return;
        }
        float toggleW = Math.min(38f, Math.max(24f, w * .28f));
        float gap = Math.min(6f, Math.max(2f, w * .06f));
        toggle.x = x;
        toggle.y = y + Math.max(0f, (h - 20f) * .5f);
        toggle.w = toggleW;
        toggle.h = Math.min(20f, h);
        open.x = x + toggleW + gap;
        open.y = y;
        open.w = Math.max(0f, w - toggleW - gap);
        open.h = h;
    }
}
