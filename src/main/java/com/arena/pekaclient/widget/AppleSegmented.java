package com.arena.pekaclient.widget;

import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public final class AppleSegmented extends Widget {
    private final String[] options;
    private final IntSupplier getter;
    private final IntConsumer setter;
    private float displayedIndex;
    private boolean indexInitialized;

    public AppleSegmented(String[] options, float width, float height, IntSupplier getter, IntConsumer setter) {
        super(width, height);
        this.options = options == null ? new String[0] : options.clone();
        this.getter = getter;
        this.setter = setter;
    }

    private int selected() {
        if (options.length == 0 || getter == null) {
            return 0;
        }
        return Math.max(0, Math.min(options.length - 1, getter.getAsInt()));
    }

    @Override
    protected void updateWidget(float dt) {
        if (options.length == 0) {
            displayedIndex = 0f;
            indexInitialized = true;
            return;
        }
        float target = selected();
        if (!indexInitialized) {
            displayedIndex = target;
            indexInitialized = true;
        } else {
            displayedIndex = approach(displayedIndex, target, dt, 20f);
        }
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        beginAnimatedDraw(0.02f, 0.035f, 0.018f);
        float radius = Math.min(Theme.radius * 0.6f, h * 0.5f);
        UIRender.roundRect(x, y, w, h, radius, Theme.buttonFill, Theme.buttonFill, 0.4f);
        if (options.length == 0) {
            drawBorder(radius, Theme.border);
            endAnimatedDraw();
            return;
        }
        float segmentW = w / options.length;
        float index = indexInitialized ? displayedIndex : selected();
        UIRender.roundRect(x + segmentW * index + 1f, y + 1f,
                Math.max(0f, segmentW - 2f), h - 2f,
                Math.max(1f, radius - 1f), Theme.segSelected, Theme.segSelected, 0.3f);

        for (int i = 0; i < options.length; i++) {
            String label = options[i] == null ? "" : options[i];
            float maxTextW = Math.max(0f, segmentW - 8f);
            label = fitText(label, maxTextW);
            float selectedWeight = Math.max(0f, 1f - Math.min(1f, Math.abs(index - i)));
            int color = UIRender.mix(Theme.textSecondary, Theme.textPrimary, selectedWeight);
            UIRender.text(font(), label, x + segmentW * i + (segmentW - textWidth(label)) * 0.5f,
                    y + (h - textHeight()) * 0.5f, color, 1f, false);
        }
        drawBorder(radius, Theme.border);
        endAnimatedDraw();
    }

    @Override
    public void onRelease(int mouseX, int mouseY, boolean activate) {
        boolean choose = pressed && activate && enabled && setter != null && options.length > 0;
        super.onRelease(mouseX, mouseY, activate);
        if (choose) {
            int index = (int) ((mouseX - x) / Math.max(1f, w) * options.length);
            setter.accept(Math.max(0, Math.min(options.length - 1, index)));
        }
    }
}
