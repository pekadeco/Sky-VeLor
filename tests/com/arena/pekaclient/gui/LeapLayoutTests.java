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
        check("head remains prominent", head >= 90 && head <= 100, "head=" + head);
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
        float requestedScale = 1.6f;
        for (int textWidth = 40; textWidth <= 240; textWidth += 10) {
            float scaleX = LeapLayout.fittedScale(widestUsername, textWidth, requestedScale);
            check("positive name scale at text width " + textWidth,
                    scaleX > 0f && scaleX <= requestedScale, "scale=" + scaleX);
            check("full name stays within text slot at " + textWidth,
                    widestUsername * scaleX <= textWidth + 0.01f,
                    "scaled=" + widestUsername * scaleX);
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

    private static void rowsForFourTeammates() {
        check("four teammates two rows", LeapLayout.rows(4) == 2, "");
        check("three teammates two rows", LeapLayout.rows(3) == 2, "");
        check("one teammate one row", LeapLayout.rows(1) == 1, "");
        check("none", LeapLayout.rows(0) == 0, "");
        check("grid height includes row gap", LeapLayout.gridHeight(80, 2, 40) == 200, "");
        check("single row has no gap", LeapLayout.verticalGap(400, 1) == 0, "");
    }
}
