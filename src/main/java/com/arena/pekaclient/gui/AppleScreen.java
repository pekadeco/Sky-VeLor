package com.arena.pekaclient.gui;

import com.arena.pekaclient.widget.AppleButton;
import com.arena.pekaclient.widget.AppleDropdown;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;

/**
 * Shared base for every Apple-style screen (the settings hub and the HUD editor). The window buttons
 * are in {@link WindowDots}.
 * It holds the helpers both screens need, so the design system lives in one place.
 */
public abstract class AppleScreen extends GuiScreen {

    /** The dropdown whose popup is open, or null. Its popup is drawn last, above everything. */
    protected AppleDropdown openDropdown;

    protected static FontRenderer fr() {
        return Minecraft.getMinecraft().fontRendererObj;
    }

    protected static boolean inside(float px, float py, float x, float y, float w, float h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    protected static String fit(FontRenderer fr, String s, float maxW) {
        if (s == null) {
            return "";
        }
        return GuiLayoutToolkit.ellipsize(s, maxW, new GuiLayoutToolkit.TextMetrics() {
            public float width(String value) { return fr.getStringWidth(value); }
            public float height() { return fr.FONT_HEIGHT; }
        });
    }

    /** The standard secondary button used by settings rows. */
    protected static AppleButton button(String label, Runnable action) {
        return new AppleButton(label, 64f, 22f, false, action);
    }

    /** Draws the open dropdown popup (if any) above the screen content. */
    protected void drawOpenDropdown(int mx, int my, float top, float bottom, float right, float left) {
        if (openDropdown != null) {
            openDropdown.drawPopup(mx, my, top, bottom, right, left);
        }
    }

    /**
     * Handles a click while a dropdown popup is open. Picks an option when one is under the pointer,
     * and closes the popup either way.
     *
     * @return true when the click was consumed by the popup
     */
    protected boolean handleOpenDropdownClick(int mx, int my) {
        if (openDropdown == null) {
            return false;
        }
        AppleDropdown dd = openDropdown;
        int opt = dd.optionAt(mx, my);
        openDropdown = null;
        if (opt >= 0) {
            dd.choose(opt);
        }
        return true;
    }
}
