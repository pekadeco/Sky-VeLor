package com.arena.pekaclient.gui;

/** Responsive geometry tests for the reference-matched Spirit Leap overlay. */
public final class LeapLayoutTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        referenceProportions();
        twoColumnGridFitsAcrossResolutions();
        headAndTextStayInsideCards();
        rowsFitWithPreviewInset();
        completeNamesFitWithoutClipping();
        roundedCardHitTesting();
        textSizesAreProminentAndOrdered();
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

    private static void referenceProportions() {
        int width = 1169;
        int height = 597;
        int cardWidth = LeapLayout.cardWidth(width, height);
        int cardHeight = LeapLayout.cardHeight(width, height, 2, 0);
        int head = LeapLayout.headSize(cardWidth, cardHeight);
        int gridWidth = LeapLayout.gridWidth(cardWidth, 2, LeapLayout.horizontalGap(width));
        int gridHeight = LeapLayout.gridHeight(cardHeight, 2, LeapLayout.verticalGap(height, 2));
        check("compact reference card width", cardWidth >= 375 && cardWidth <= 390, "card=" + cardWidth);
        check("compact reference card height", cardHeight >= 139 && cardHeight <= 149, "card=" + cardHeight);
        check("head remains clear but leaves room for larger text", head >= 60 && head <= 66, "head=" + head);
        check("text slot reserves most of the card for names",
                LeapLayout.textWidth(cardWidth, head) >= cardWidth * 72 / 100,
                "text=" + LeapLayout.textWidth(cardWidth, head) + " card=" + cardWidth);
        check("card group is compact and centered", gridWidth >= width * 67 / 100
                && gridWidth <= width * 71 / 100, "grid=" + gridWidth);
        check("two-row group is compact", gridHeight >= height * 52 / 100
                && gridHeight <= height * 56 / 100, "grid=" + gridHeight);
        check("column gap is substantially reduced", LeapLayout.horizontalGap(width) <= width / 20,
                "gap=" + LeapLayout.horizontalGap(width));
        check("row gap is substantially reduced", LeapLayout.verticalGap(height, 2) <= height / 12,
                "gap=" + LeapLayout.verticalGap(height, 2));
    }

    private static void twoColumnGridFitsAcrossResolutions() {
        check("two columns", LeapLayout.COLS == 2, "");
        for (int width = 320; width <= 2560; width += 40) {
            int cardWidth = LeapLayout.cardWidth(width, 480);
            int gap = LeapLayout.horizontalGap(width);
            int gridWidth = LeapLayout.gridWidth(cardWidth, LeapLayout.COLS, gap);
            int left = (width - gridWidth) / 2;
            check("positive card width at " + width, cardWidth > 0, "card=" + cardWidth);
            check("grid inside width at " + width, left >= 0 && left + gridWidth <= width,
                    "left=" + left + " grid=" + gridWidth + " screen=" + width);
            check("grid centered at " + width, Math.abs(left - (width - gridWidth - left)) <= 1,
                    "left=" + left + " right=" + (width - gridWidth - left));
        }
    }

    private static void headAndTextStayInsideCards() {
        for (int width = 320; width <= 2560; width += 80) {
            for (int height = 180; height <= 1080; height += 60) {
                int cardWidth = LeapLayout.cardWidth(width, height);
                int cardHeight = LeapLayout.cardHeight(width, height, 2, 18);
                int head = LeapLayout.headSize(cardWidth, cardHeight);
                int textLeft = LeapLayout.headLeft(cardWidth) + head + LeapLayout.headGap(cardWidth);
                int textWidth = LeapLayout.textWidth(cardWidth, head);
                check("head fits card at " + width + "x" + height,
                        head > 0 && head + 2 * LeapLayout.headLeft(cardWidth) <= cardWidth,
                        "card=" + cardWidth + " head=" + head);
                check("text starts after head at " + width + "x" + height,
                        textLeft <= cardWidth - LeapLayout.textRight(cardWidth), "");
                check("text stays in card at " + width + "x" + height,
                        textLeft + textWidth + LeapLayout.textRight(cardWidth) <= cardWidth,
                        "card=" + cardWidth + " text=" + textWidth);
                check("text width non-negative at " + width + "x" + height, textWidth >= 0, "");
            }
        }
    }

    private static void rowsFitWithPreviewInset() {
        for (int width = 320; width <= 1920; width += 160) {
            for (int height = 180; height <= 1080; height += 60) {
                for (int rows = 1; rows <= 4; rows++) {
                    int inset = 18;
                    int cardHeight = LeapLayout.cardHeight(width, height, rows, inset);
                    int gap = LeapLayout.verticalGap(height, rows);
                    int gridHeight = LeapLayout.gridHeight(cardHeight, rows, gap);
                    int available = height - inset;
                    int top = inset + Math.max(0, (available - gridHeight) / 2);
                    check("rows fit " + width + "x" + height + " rows=" + rows,
                            cardHeight > 0 && top >= 0 && top + gridHeight <= height,
                            "card=" + cardHeight + " top=" + top + " grid=" + gridHeight);
                }
            }
        }
    }

    private static void completeNamesFitWithoutClipping() {
        int widestUsername = 16 * 8;
        float requestedScale = LeapScreen.NAME_SCALE;
        for (int textWidth = 40; textWidth <= 360; textWidth += 10) {
            float scaleX = LeapLayout.fittedScale(widestUsername, textWidth, requestedScale);
            float classScale = LeapLayout.fittedScale(56, textWidth, LeapScreen.CLASS_SCALE);
            check("positive name scale at text width " + textWidth,
                    scaleX > 0f && scaleX <= requestedScale, "scale=" + scaleX);
            check("full name stays within text slot at " + textWidth,
                    widestUsername * scaleX <= textWidth + 0.01f,
                    "scaled=" + widestUsername * scaleX);
            check("class label stays within text slot at " + textWidth,
                    56 * classScale <= textWidth + 0.01f, "scaled=" + 56 * classScale);
            if (textWidth < widestUsername * requestedScale) {
                check("long name fits horizontally at " + textWidth, scaleX < requestedScale,
                        "scale=" + scaleX);
            } else {
                check("name keeps the requested larger scale at " + textWidth,
                        scaleX == requestedScale, "scale=" + scaleX);
            }
        }
        check("empty text has no scale", LeapLayout.fittedScale(0, 100, requestedScale) == 0f, "");
        check("zero-width slot has no scale", LeapLayout.fittedScale(100, 0, requestedScale) == 0f, "");
    }

    private static void roundedCardHitTesting() {
        check("rounded card center is clickable",
                LeapLayout.containsRoundedRect(50, 30, 0, 0, 100, 60, LeapPalette.CARD_RADIUS), "");
        check("straight edge is clickable",
                LeapLayout.containsRoundedRect(50, 1, 0, 0, 100, 60, LeapPalette.CARD_RADIUS), "");
        check("rounded corner pixel is not clickable",
                !LeapLayout.containsRoundedRect(0, 0, 0, 0, 100, 60, LeapPalette.CARD_RADIUS), "");
        check("pixel inside the curved edge is clickable",
                LeapLayout.containsRoundedRect(5, 5, 0, 0, 100, 60, LeapPalette.CARD_RADIUS), "");
        check("point beyond the card is not clickable",
                !LeapLayout.containsRoundedRect(100, 30, 0, 0, 100, 60, LeapPalette.CARD_RADIUS), "");
        check("zero radius is rectangular",
                LeapLayout.containsRoundedRect(0, 0, 0, 0, 100, 60, 0f), "");
    }

    private static void textSizesAreProminentAndOrdered() {
        check("player names use a visibly enlarged scale", LeapScreen.NAME_SCALE >= 2.5f,
                "scale=" + LeapScreen.NAME_SCALE);
        check("class labels are enlarged but smaller than names",
                LeapScreen.CLASS_SCALE >= 1.8f && LeapScreen.CLASS_SCALE < LeapScreen.NAME_SCALE,
                "name=" + LeapScreen.NAME_SCALE + " class=" + LeapScreen.CLASS_SCALE);
        float normalName = LeapLayout.fittedScale(64, 200, LeapScreen.NAME_SCALE);
        float normalClass = LeapLayout.fittedScale(40, 200, LeapScreen.CLASS_SCALE);
        check("normal player name keeps large requested scale", normalName == LeapScreen.NAME_SCALE,
                "scale=" + normalName);
        check("normal class label keeps clear requested scale", normalClass == LeapScreen.CLASS_SCALE,
                "scale=" + normalClass);
    }

    private static void rowsForFourTeammates() {
        check("four teammates two rows", LeapLayout.rows(4) == 2, "");
        check("three teammates two rows", LeapLayout.rows(3) == 2, "");
        check("one teammate one row", LeapLayout.rows(1) == 1, "");
        check("none", LeapLayout.rows(0) == 0, "");
        check("grid height includes row gap", LeapLayout.gridHeight(80, 2, 40) == 200, "");
        check("single row has no gap", LeapLayout.verticalGap(400, 1) == 0, "");
    }
}
