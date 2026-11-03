/*
 * This modified version is maintained by ii8we. Review LICENSE for upstream attribution and applicable license terms before use or redistribution. Original NotEnoughFakepixel code and third-party components remain subject to their respective copyrights and license terms.
 */
package com.arena.pekaclient.dungeon.solver.terminals;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

/** Shared Lunaria terminal styling. All decorative rails are horizontal; no left-side vertical stripe is drawn. */
final class TerminalTheme {
    private static final int SHADOW = 0x8803070D;
    private static final int PANEL = 0xE814202D;
    private static final int HEADER = 0xFF1B2938;
    private static final int HEADER_LIGHT = 0xFF263748;
    private static final int EDGE = 0xFF35495A;
    private static final int BRAND = 0xFF101B26;
    private static final int ACCENT = 0xFF69C5B7;
    private static final int ACCENT_DIM = 0xFF477F8B;
    private static final int TEXT = 0xFFE5EDF2;
    private static final int MUTED = 0xFFACBBC8;
    private static final int SLOT_SHADOW = 0xFF101923;
    private static final int SLOT_BORDER = 0xFF35495B;
    private static final int SLOT = 0xFF1C2B3B;
    private static final int SLOT_HIGHLIGHT = 0xFF293B4D;

    private TerminalTheme() {
    }

    /** Compact title plate above the vanilla terminal grid. It deliberately avoids vertical frame lines. */
    static void drawOverlay(int width, int height, String title) {
        Gui.drawRect(1, -16, width + 1, 0, SHADOW);
        Gui.drawRect(0, -17, width, -1, HEADER);
        Gui.drawRect(0, -17, width, -15, ACCENT_DIM);
        Gui.drawRect(0, -15, width, -14, HEADER_LIGHT);
        Gui.drawRect(0, -2, width, -1, EDGE);

        drawBrandTitle(title, 3, -11);

        // A small, segmented signal underline gives the grid a custom identity without a side rail.
        Gui.drawRect(0, height + 1, width, height + 3, SHADOW);
        Gui.drawRect(0, height + 1, width, height + 2, ACCENT_DIM);
        Gui.drawRect(0, height + 1, Math.max(2, width / 4), height + 2, ACCENT);
        Gui.drawRect(Math.max(2, width / 4) + 3, height + 1,
                Math.min(width, Math.max(2, width / 4) + 10), height + 2, 0xFF4A7880);
    }

    /** Branded background for the standalone simulator. */
    static void drawWindow(int width, int height, String title) {
        Gui.drawRect(3, 3, width + 3, height + 3, SHADOW);
        Gui.drawRect(0, 0, width, height, PANEL);
        Gui.drawRect(0, 0, width, 19, HEADER);
        Gui.drawRect(0, 0, width, 2, ACCENT_DIM);
        Gui.drawRect(0, 0, Math.max(8, width / 3), 2, ACCENT);
        Gui.drawRect(0, 18, width, 19, EDGE);
        Gui.drawRect(0, height - 2, width, height - 1, ACCENT_DIM);
        Gui.drawRect(0, height - 2, Math.max(8, width / 4), height - 1, ACCENT);
        drawBrandTitle(title, 6, 7);
    }

    /** Recessed terminal cell with restrained horizontal highlights. */
    static void drawSlot(int x, int y, int width, int height) {
        Gui.drawRect(x + 1, y + 1, x + width + 1, y + height + 1, SLOT_SHADOW);
        Gui.drawRect(x, y, x + width, y + height, SLOT_BORDER);
        Gui.drawRect(x + 1, y + 1, x + width - 1, y + height - 1, SLOT);
        if (width > 8 && height > 4) {
            Gui.drawRect(x + 2, y + 1, x + width - 2, y + 2, SLOT_HIGHLIGHT);
            Gui.drawRect(x + 2, y + height - 2, x + width - 2, y + height - 1, SLOT_SHADOW);
        }
    }

    /** Focus fill and bright horizontal rails; never draws a full-height vertical outline. */
    static void drawTargetCell(int x, int y, int width, int height, int color) {
        if ((color >>> 24) == 0 || width < 3 || height < 3) return;
        Gui.drawRect(x + 1, y + 1, x + width - 1, y + height - 1, 0x9907101B);
        Gui.drawRect(x + 2, y + 2, x + width - 2, y + height - 2, withAlpha(color, 92));
        Gui.drawRect(x + 2, y + 1, x + width - 2, y + 3, withAlpha(color, 245));
        Gui.drawRect(x + 2, y + height - 3, x + width - 2, y + height - 1, withAlpha(color, 205));
        if (width >= 12) {
            Gui.drawRect(x + 3, y + 4, x + 7, y + 5, withAlpha(color, 155));
            Gui.drawRect(x + width - 7, y + 4, x + width - 3, y + 5, withAlpha(color, 155));
        }
    }

    private static void drawBrandTitle(String title, int x, int y) {
        FontRenderer font = Minecraft.getMinecraft().fontRendererObj;
        Gui.drawRect(x - 1, y - 3, x + 16, y + 8, BRAND);
        Gui.drawRect(x - 1, y - 3, x + 16, y - 2, ACCENT);
        Gui.drawRect(x - 1, y + 7, x + 16, y + 8, ACCENT_DIM);
        font.drawString("SV", x + 1, y - 1, ACCENT, false);
        font.drawString("/", x + 19, y - 1, MUTED, false);
        font.drawString(title, x + 26, y - 1, TEXT, false);
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }
}
