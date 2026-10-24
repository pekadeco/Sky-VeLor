package com.arena.pekaclient.gui;

import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;

/** One explicit neutral Close icon used by focused child editors. */
final class FocusedHeaderCloseButton {
    static final float SIZE = SettingsShellLayout.HEADER_BUTTON_SIZE;
    private static final float HALF = SIZE * .5f;

    private FocusedHeaderCloseButton() {
    }

    static boolean contains(int mouseX, int mouseY, float centerX, float centerY) {
        float x = centerX - HALF;
        float y = centerY - HALF;
        return mouseX >= x && mouseX < x + SIZE && mouseY >= y && mouseY < y + SIZE;
    }

    static void draw(float centerX, float centerY, int mouseX, int mouseY) {
        float x = centerX - HALF;
        float y = centerY - HALF;
        boolean hovered = contains(mouseX, mouseY, centerX, centerY);
        int fill = hovered ? Theme.SURFACE_CONTROL_HOVER : Theme.SURFACE_CONTROL;
        int border = hovered ? Theme.ACCENT_BLUE_HOVER : Theme.BORDER;
        UIRender.roundRectBorder(x, y, SIZE, SIZE, Theme.RADIUS_SMALL, fill, fill, border);
        int ink = hovered ? Theme.TEXT_PRIMARY : Theme.ICON_INK;
        UIRender.line(centerX - 3.25f, centerY - 3.25f, centerX + 3.25f, centerY + 3.25f, 1.15f, ink);
        UIRender.line(centerX + 3.25f, centerY - 3.25f, centerX - 3.25f, centerY + 3.25f, 1.15f, ink);
    }
}
