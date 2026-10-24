package com.arena.pekaclient.widget;

import com.arena.pekaclient.render.GuiStyle;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import org.lwjgl.input.Keyboard;

import java.util.function.IntSupplier;

/** A compact button whose next keyboard event is captured by its owning screen. */
public final class AppleKeybindButton extends Widget {

    private final IntSupplier keyCode;
    private boolean listening;

    public AppleKeybindButton(float width, float height, IntSupplier keyCode) {
        super(width, height);
        this.keyCode = keyCode;
    }

    public boolean isListening() {
        return listening;
    }

    public void cancelCapture() {
        listening = false;
    }

    /** Called after the screen has applied a valid key binding. */
    public void finishCapture() {
        listening = false;
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        beginAnimatedDraw(0.02f, 0.035f, 0.018f);
        int fill = UIRender.mix(Theme.buttonFill, Theme.buttonHover, hovered && enabled ? 0.55f : 0f);
        float radius = GuiStyle.radius(GuiStyle.RADIUS_FIELD, Theme.radius, w, h);
        UIRender.roundRect(x, y, w, h, radius, fill, fill, 0.4f);
        String label;
        int color;
        if (listening) {
            label = "Press a key…";
            color = Theme.accent;
        } else {
            int code = keyCode == null ? 0 : keyCode.getAsInt();
            String name = code > 0 ? Keyboard.getKeyName(code) : "NONE";
            label = name == null || name.length() == 0 ? "NONE" : name;
            color = Theme.textPrimary;
        }
        label = fitText(label, Math.max(0f, w - 16f));
        UIRender.text(font(), label, x + 8f, y + (h - textHeight()) * 0.5f, color, 1f, false);
        drawBorder(radius, listening ? Theme.accent : Theme.border);
        endAnimatedDraw();
    }

    @Override
    public void onRelease(int mouseX, int mouseY, boolean activate) {
        boolean arm = pressed && activate && enabled;
        super.onRelease(mouseX, mouseY, activate);
        if (arm) listening = true;
    }
}
