package com.arena.pekaclient.widget;

import com.arena.pekaclient.render.Anim;
import com.arena.pekaclient.render.Icons;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.input.Keyboard;

/**
 * Single-line text field for filtering settings. Keyboard input arrives through the
 * screen's keyTyped(char, int), which forwards here. A version counter lets the
 * screen rebuild search results only when the text really changed.
 */
public final class SearchField extends Widget {

    public static final int MAX_LEN = 48;

    private final StringBuilder buf = new StringBuilder();
    private final Anim focus = new Anim(0f);
    private final String placeholder;

    private boolean focused;
    private int version;
    private String shown = "";
    private int shownAvail = -1;
    private boolean dirty = true;

    public SearchField(String placeholder) {
        this.placeholder = placeholder;
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean f) {
        focused = f;
        focus.to(f ? 1f : 0f);
    }

    public boolean isEmpty() {
        return buf.length() == 0;
    }

    public String text() {
        return buf.toString();
    }

    public int version() {
        return version;
    }

    /** Replaces the text. Used when the screen shows a different route. Bumps the version like typing does. */
    public void setText(String text) {
        String t = text == null ? "" : text;
        if (t.length() > MAX_LEN) {
            t = t.substring(0, MAX_LEN);
        }
        buf.setLength(0);
        buf.append(t);
        changed();
    }

    public void clear() {
        if (buf.length() > 0) {
            buf.setLength(0);
            changed();
        }
    }

    private void changed() {
        version++;
        dirty = true;
    }

    /** @return true when the key was consumed by the field. */
    public boolean keyTyped(char c, int code) {
        if (!focused) {
            return false;
        }
        if (code == Keyboard.KEY_BACK) {
            if (buf.length() > 0) {
                buf.setLength(buf.length() - 1);
                changed();
            }
            return true;
        }
        if (code == Keyboard.KEY_RETURN || code == Keyboard.KEY_NUMPADENTER) {
            setFocused(false);
            return true;
        }
        if (c >= 32 && c != 127 && buf.length() < MAX_LEN) {
            buf.append(c);
            changed();
            return true;
        }
        return false;
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        focus.update(dt, 18f);
    }

    @Override
    public void draw(int mx, int my) {
        hovered(mx, my);
        float f = focus.get();
        int border = UIRender.mix(Theme.border, Theme.accent, f);
        int fill = UIRender.mix(Theme.buttonFill, Theme.buttonHover, hover.get() * 0.5f);
        UIRender.roundRectBorder(x, y, w, h, h * 0.5f, fill, fill, border);

        float cy = y + h * 0.5f;
        Icons.draw(Icons.SEARCH, x + 11f, cy, 9f, Theme.textTertiary);

        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        float tx = x + 22f;
        float ty = y + (h - 9f) * 0.5f + 1f;
        float avail = w - 22f - 8f;
        float curX = tx;

        if (buf.length() == 0) {
            UIRender.text(fr, placeholder, tx, ty, Theme.textTertiary, 1f, false);
        } else {
            if (dirty || shownAvail != (int) avail) {
                rebuildShown(fr, (int) avail);
            }
            UIRender.text(fr, shown, tx, ty, Theme.textPrimary, 1f, false);
            curX = tx + fr.getStringWidth(shown);
        }

        if (focused && ((System.currentTimeMillis() / 500L) & 1L) == 0L) {
            UIRender.rect(curX + 1f, y + 5f, 1f, h - 10f, Theme.textPrimary);
        }
    }

    /** Keeps the tail of long queries visible. Runs only when the text changes. */
    private void rebuildShown(FontRenderer fr, int avail) {
        String s = buf.toString();
        int start = 0;
        while (start < s.length() && fr.getStringWidth(s.substring(start)) > avail) {
            start++;
        }
        shown = s.substring(start);
        shownAvail = avail;
        dirty = false;
    }
}
