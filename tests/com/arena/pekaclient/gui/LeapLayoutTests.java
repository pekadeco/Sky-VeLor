package com.arena.pekaclient.gui;

/** Checks the Spirit Leap grid rules: two columns at every width, and a card width that keeps head and text apart. */
public final class LeapLayoutTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        alwaysTwoColumns();
        cardWidthStaysInLimits();
        headAndTextDoNotOverlap();
        rowsForFourTeammates();

        System.out.println();
        System.out.println("leap layout: passed " + passed + ", failed " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void check(String name, boolean ok, String detail) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL " + name + " " + detail);
        }
    }

    private static void alwaysTwoColumns() {
        check("cols", LeapLayout.COLS == 2, "");
        for (int w = 320; w <= 2560; w += 40) {
            int cw = LeapLayout.cardWidth(w, 32);
            int panel = LeapLayout.panelWidth(cw);
            check("panel fits screen at width " + w, panel <= w - 2 * LeapLayout.MARGIN,
                    "card=" + cw + " panel=" + panel);
        }
    }

    private static void cardWidthStaysInLimits() {
        for (int w = 100; w <= 2560; w += 20) {
            int cw = LeapLayout.cardWidth(w, 32);
            check("card min at " + w, cw >= LeapLayout.MIN_CARD_W, "card=" + cw);
            check("card max at " + w, cw <= LeapLayout.MAX_CARD_W, "card=" + cw);
        }
        check("wide screen uses max", LeapLayout.cardWidth(2560, 32) == LeapLayout.MAX_CARD_W, "");
        check("small screen uses min", LeapLayout.cardWidth(320, 32) >= LeapLayout.MIN_CARD_W
                && LeapLayout.cardWidth(320, 32) < LeapLayout.MAX_CARD_W, "");
    }

    private static void headAndTextDoNotOverlap() {
        int head = 32;
        for (int w = 100; w <= 2560; w += 20) {
            int cw = LeapLayout.cardWidth(w, head);
            int textX = LeapLayout.HEAD_LEFT + head + LeapLayout.HEAD_GAP;
            int textW = LeapLayout.textWidth(cw, head);
            check("text inside card at " + w, textX + textW + LeapLayout.TEXT_RIGHT <= cw, "card=" + cw);
            check("text width never negative at " + w, textW >= 0, "");
        }
        check("head fits at smallest width", LeapLayout.cardWidth(0, 32) >= LeapLayout.HEAD_LEFT + 32
                + LeapLayout.HEAD_GAP + LeapLayout.TEXT_RIGHT, "");
    }

    private static void rowsForFourTeammates() {
        check("four teammates two rows", LeapLayout.rows(4) == 2, "");
        check("three teammates two rows", LeapLayout.rows(3) == 2, "");
        check("one teammate one row", LeapLayout.rows(1) == 1, "");
        check("none", LeapLayout.rows(0) == 0, "");
    }
}
