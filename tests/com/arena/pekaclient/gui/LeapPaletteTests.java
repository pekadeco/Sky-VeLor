package com.arena.pekaclient.gui;

/** Checks the card palette remains opaque and has a restrained but visible hover state. */
public final class LeapPaletteTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("main fill is the specified charcoal", LeapPalette.CARD_BACKGROUND == 0xFF252525);
        check("hover fill is lighter charcoal", LeapPalette.CARD_HOVER_BACKGROUND == 0xFF353535);
        check("main card fill is fully opaque", alpha(LeapPalette.CARD_BACKGROUND) == 255);
        check("hover card fill is fully opaque", alpha(LeapPalette.CARD_HOVER_BACKGROUND) == 255);
        check("normal border is fully opaque", alpha(LeapPalette.CARD_BORDER) == 255);
        check("hover border is fully opaque", alpha(LeapPalette.CARD_HOVER_BORDER) == 255);
        check("normal border contrasts with fill", LeapPalette.CARD_BORDER > LeapPalette.CARD_BACKGROUND);
        check("hover border is subtly brighter", LeapPalette.CARD_HOVER_BORDER > LeapPalette.CARD_BORDER
                && LeapPalette.CARD_HOVER_BORDER != 0xFFFFFFFF);

        System.out.println();
        System.out.println("leap palette: passed " + passed + ", failed " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static int alpha(int color) {
        return color >>> 24;
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL " + name);
        }
    }
}
