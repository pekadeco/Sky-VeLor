package com.arena.pekaclient.gui;

/**
 * Size rules for the Spirit Leap grid: always two columns, with the card width taken from the screen width.
 *
 * Pure integer maths with no Minecraft types, so it is unit-tested. The screen keeps two columns at every width.
 * Only the card width changes, within fixed limits, so the head and the text inside a card never overlap.
 */
public final class LeapLayout {

    /** Always two cards per row. Four teammates therefore make two rows. */
    public static final int COLS = 2;
    public static final int GAP = 6;
    public static final int PAD = 10;
    /** Space kept free on each side of the panel. */
    public static final int MARGIN = 10;
    public static final int MIN_CARD_W = 96;
    public static final int MAX_CARD_W = 180;
    /** Space from the card's left edge to the head, from the head to the text, and the right inset of the text. */
    public static final int HEAD_LEFT = 8;
    public static final int HEAD_GAP = 8;
    public static final int TEXT_RIGHT = 6;

    private LeapLayout() {
    }

    /** Card width for a screen this wide. Never below {@link #MIN_CARD_W} or above {@link #MAX_CARD_W}. */
    public static int cardWidth(int screenWidth, int headSize) {
        int inner = screenWidth - 2 * MARGIN - 2 * PAD;
        int w = (inner - GAP) / COLS;
        // The head and its margins must fit. Shrinking below this would let the head run into the text.
        int minForHead = HEAD_LEFT + headSize + HEAD_GAP + TEXT_RIGHT + 1;
        if (w > MAX_CARD_W) {
            w = MAX_CARD_W;
        }
        int floor = Math.max(MIN_CARD_W, minForHead);
        if (w < floor) {
            w = floor;
        }
        return w;
    }

    /** Panel width for the given card width and two columns. */
    public static int panelWidth(int cardWidth) {
        return COLS * cardWidth + (COLS - 1) * GAP + 2 * PAD;
    }

    /** Width left for the name and class text inside a card, never negative. */
    public static int textWidth(int cardWidth, int headSize) {
        return Math.max(0, cardWidth - HEAD_LEFT - headSize - HEAD_GAP - TEXT_RIGHT);
    }

    /** Number of rows needed for {@code cards} cards in two columns. */
    public static int rows(int cards) {
        return cards <= 0 ? 0 : (cards + COLS - 1) / COLS;
    }
}
