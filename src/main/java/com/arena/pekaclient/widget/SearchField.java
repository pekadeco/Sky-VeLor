package com.arena.pekaclient.widget;

import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import org.lwjgl.input.Keyboard;

public final class SearchField extends Widget {
    private static final int MAX_LENGTH = 160;
    private final String placeholder;
    private final StringBuilder value = new StringBuilder();
    private boolean focused;
    private int version;
    private float focusAmount;

    public SearchField(String placeholder) {
        super(150f, 22f);
        this.placeholder = placeholder == null ? "" : placeholder;
    }

    public String text() {
        return value.toString();
    }

    public void setText(String text) {
        String next = text == null ? "" : text.substring(0, Math.min(text.length(), MAX_LENGTH));
        if (!value.toString().equals(next)) {
            value.setLength(0);
            value.append(next);
            version++;
        }
    }

    public void clear() {
        if (value.length() != 0) {
            value.setLength(0);
            version++;
        }
    }

    public boolean isEmpty() {
        return value.length() == 0;
    }

    public int version() {
        return version;
    }

    public void setFocused(boolean focused) {
        if (focused && !this.focused) {
            clickPulse = 0.55f;
        }
        this.focused = focused;
        if (!focused) {
            pressed = false;
        }
    }

    public boolean isFocused() {
        return focused;
    }

    @Override
    protected void updateWidget(float dt) {
        focusAmount = approach(focusAmount, focused ? 1f : 0f, dt, 16f);
    }

    public boolean keyTyped(char typedChar, int keyCode) {
        if (!focused) {
            return false;
        }
        if (keyCode == Keyboard.KEY_BACK) {
            if (value.length() > 0) {
                value.deleteCharAt(value.length() - 1);
                version++;
            }
            return true;
        }
        if (keyCode == Keyboard.KEY_DELETE) {
            clear();
            return true;
        }
        if (keyCode == Keyboard.KEY_HOME) {
            return true;
        }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER || keyCode == Keyboard.KEY_ESCAPE) {
            return false;
        }
        if (typedChar >= 32 && typedChar != 127 && value.length() < MAX_LENGTH) {
            value.append(typedChar);
            version++;
            return true;
        }
        return false;
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        beginAnimatedDraw(0.012f, 0.02f, 0.008f);
        float hover = !focused && enabled ? hoverAmount : 0f;
        int fill = UIRender.mix(Theme.buttonFill, Theme.buttonHover, hover);
        float radius = Math.min(Theme.radius * 0.55f, h * 0.5f);
        UIRender.roundRect(x, y, w, h, radius, fill, fill, 0.3f);
        int border = UIRender.mix(Theme.border, Theme.accent, focusAmount);
        UIRender.roundRectRing(x, y, w, h, radius, 1f, border);

        String shown = value.length() == 0 && !focused ? placeholder : value.toString();
        int available = Math.max(0, (int) (w - 16f));
        if (font().getStringWidth(shown) > available) {
            while (shown.length() > 0 && font().getStringWidth(shown) > available) {
                shown = shown.substring(1);
            }
        }
        int color = value.length() == 0 && !focused ? Theme.textTertiary : Theme.textPrimary;
        UIRender.text(font(), shown, x + 8f, y + (h - 8f) * 0.5f + 1f, color, 1f, false);
        if (focused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            float caretX = x + 8f + font().getStringWidth(shown);
            if (caretX < x + w - 5f) {
                UIRender.rect(caretX, y + 5f, 1f, h - 10f, Theme.textPrimary);
            }
        }
        endAnimatedDraw();
    }
}
