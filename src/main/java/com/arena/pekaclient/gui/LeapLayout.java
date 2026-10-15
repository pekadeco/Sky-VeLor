package com.arena.pekaclient.gui;

/**
 * Responsive geometry for the Spirit Leap card overlay.
 *
 * The proportions follow the supplied 2-by-2 reference: cards occupy about 36.5% of the viewport width,
 * the column gap about 12.5%, and the player head about a quarter of each card's width. All values are derived
 * from the scaled Minecraft GUI dimensions, not a screenshot-sized coordinate set.
 */
public final class LeapLayout {

    public static final int COLS = 2;

    private static final int MIN_SIDE_MARGIN = 6;
    private static final int MIN_COLUMN_GAP = 8;
    private static final int MIN_ROW_GAP = 6;
    private static final int MIN_VERTICAL_MARGIN = 6;
    private static final int HEAD_BOTTOM_TOP_SPACE = 12;

    private LeapLayout() {
    }

    /** Horizontal outside margin: approximately 7.1% of the scaled viewport. */
    public static int horizontalMargin(int screenWidth) {
        return Math.max(MIN_SIDE_MARGIN, Math.max(0, screenWidth) / 14);
    }

    /** Space between the two columns: approximately 12.5% of the scaled viewport. */
    public static int horizontalGap(int screenWidth) {
        return Math.max(MIN_COLUMN_GAP, Math.max(0, screenWidth) / 8);
    }

    /**
     * Card width for the current scaled viewport. The height-based cap preserves the reference's wide-card shape
     * on unusually wide, short windows while keeping the grid inside the screen.
     */
    public static int cardWidth(int screenWidth, int screenHeight) {
        int width = Math.max(0, screenWidth);
        int height = Math.max(0, screenHeight);
        int availableWidth = width - 2 * horizontalMargin(width) - horizontalGap(width);
        int widthLimited = Math.max(1, availableWidth / COLS);
        int heightLimited = Math.max(1, height * 72 / 100);
        return Math.min(widthLimited, heightLimited);
    }

    /** Vertical outside margin used when limiting a multi-row layout. */
    public static int verticalMargin(int screenHeight) {
        return Math.max(MIN_VERTICAL_MARGIN, Math.max(0, screenHeight) / 16);
    }

    /** Space between teammate rows: approximately one sixth of the scaled viewport height. */
    public static int verticalGap(int screenHeight, int rows) {
        if (rows <= 1) {
            return 0;
        }
        return Math.max(MIN_ROW_GAP, Math.max(0, screenHeight) / 6);
    }

    /**
     * Responsive card height. It follows the card width for a landscape rectangle, then shrinks as needed to fit
     * every row, the preview label, and safe margins on short screens.
     */
    public static int cardHeight(int screenWidth, int screenHeight, int rows, int topInset) {
        if (rows <= 0) {
            return 0;
        }
        int height = Math.max(0, screenHeight);
        int widthBased = Math.max(1, cardWidth(screenWidth, height) * 38 / 100);
        int gaps = Math.max(0, rows - 1) * verticalGap(height, rows);
        int reserved = 2 * verticalMargin(height) + Math.max(0, topInset) + gaps;
        int available = Math.max(1, height - reserved);
        int heightBased = Math.max(1, available / rows);
        return Math.min(widthBased, heightBased);
    }

    /** Head size scales with both card dimensions while remaining square and vertically inset. */
    public static int headSize(int cardWidth, int cardHeight) {
        if (cardWidth <= 0 || cardHeight <= 0) {
            return 0;
        }
        return Math.max(1, Math.min(cardWidth / 4, cardHeight - HEAD_BOTTOM_TOP_SPACE));
    }

    public static int headLeft(int cardWidth) {
        return Math.max(4, Math.max(0, cardWidth) / 16);
    }

    public static int headGap(int cardWidth) {
        return Math.max(4, Math.max(0, cardWidth) / 22);
    }

    public static int textRight(int cardWidth) {
        return Math.max(4, Math.max(0, cardWidth) / 32);
    }

    /** Width left for the name and class after the head and its padding. */
    public static int textWidth(int cardWidth, int headSize) {
        return Math.max(0, cardWidth - headLeft(cardWidth) - Math.max(0, headSize)
                - headGap(cardWidth) - textRight(cardWidth));
    }

    public static int gridWidth(int cardWidth, int columns, int gap) {
        return columns <= 0 ? 0 : columns * cardWidth + (columns - 1) * Math.max(0, gap);
    }

    public static int gridHeight(int cardHeight, int rows, int gap) {
        return rows <= 0 ? 0 : rows * cardHeight + (rows - 1) * Math.max(0, gap);
    }

    /** Number of rows needed for {@code cards} cards in the two-column grid. */
    public static int rows(int cards) {
        return cards <= 0 ? 0 : (cards + COLS - 1) / COLS;
    }
}
