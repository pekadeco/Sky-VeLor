package com.arena.pekaclient.hud;

import com.arena.pekaclient.dungeon.TerminalSolver;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;

/**
 * The Click in order panel: the numbers 1 to 14 in two balanced rows of seven, as on the terminal. While a Floor VII or
 * M7 Click in order terminal is open it mirrors that terminal: green is the number to click now, yellow is the
 * one after it, and clicked numbers are dimmed. Elsewhere it shows a sample (1 green, 2 yellow) in the HUD
 * editor. It is drawn only over an open terminal, never in the normal game HUD. It can be moved and resized
 * in the HUD editor.
 */
public final class ClickInOrderHud extends HudElement {

    private static final int COUNT = 14;
    private static final int COLS = 7;
    private static final float PAD = 8f;
    private static final float HEADER = 18f;
    private static final float CELL_W = 22f;
    private static final float CELL_H = 16f;
    private static final float GAP = 3f;

    private static final int COLOR_GREEN = 0xFF2E9E3E;
    private static final int COLOR_YELLOW = 0xFFE6C229;
    private static final int COLOR_BLACK = 0xFF000000;
    private static final int COLOR_TEXT_DIM = 0xFF5A5A62;
    private static final int COLOR_TEXT_DARK = 0xFF1A1A1A;
    private static final int COLOR_TEXT_LIGHT = 0xFFF2F2F4;

    public ClickInOrderHud() {
        super("clickInOrder", "Click in order",
                new HudPlacement(HudPlacement.Anchor.START, 12, HudPlacement.Anchor.START, 40), true);
    }

    @Override
    public float width() {
        return PAD * 2 + COLS * CELL_W + (COLS - 1) * GAP;
    }

    @Override
    public float height() {
        return HEADER + 2 * CELL_H + GAP + PAD;
    }

    @Override
    protected void drawContent() {
        drawPanel(width(), height());
        text("Click in order", PAD, 4f, Theme.textPrimary);

        boolean live = TerminalSolver.hasLiveTerminal();
        int next = live ? TerminalSolver.snapNext() : 1;
        int after = live ? TerminalSolver.snapAfter() : 2;
        for (int n = 1; n <= COUNT; n++) {
            int col = (n - 1) % COLS;
            int row = (n - 1) / COLS;
            float x = PAD + col * (CELL_W + GAP);
            float y = HEADER + row * (CELL_H + GAP);
            boolean clicked = live && TerminalSolver.snapClicked(n);

            int fill = COLOR_BLACK;
            int color = COLOR_TEXT_LIGHT;
            if (clicked) {
                color = COLOR_TEXT_DIM;
            } else if (n == next) {
                fill = COLOR_GREEN;
            } else if (n == after) {
                fill = COLOR_YELLOW;
                color = COLOR_TEXT_DARK;
            }
            UIRender.roundRect(x, y, CELL_W, CELL_H, 4f, fill, fill, 1f);

            String label = String.valueOf(n);
            float tx = x + (CELL_W - textWidth(label)) / 2f;
            float ty = y + (CELL_H - 8f * textScale()) / 2f;
            text(label, tx, ty, color);
        }
    }
}
