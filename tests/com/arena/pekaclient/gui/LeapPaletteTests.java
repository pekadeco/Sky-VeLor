package com.arena.pekaclient.gui;

import com.arena.pekaclient.render.Theme;

/** Checks the Spirit Leap card facade consumes the shared neutral-dark Theme tokens. */
public final class LeapPaletteTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("main fill is sourced from Theme", LeapPalette.CARD_BACKGROUND == Theme.LEAP_CARD);
        check("hover fill is sourced from Theme", LeapPalette.CARD_HOVER_BACKGROUND == Theme.LEAP_CARD_HOVER);
        check("border tokens are sourced from Theme", LeapPalette.CARD_BORDER == Theme.LEAP_BORDER
                && LeapPalette.CARD_HOVER_BORDER == Theme.LEAP_BORDER_HOVER);
        check("main card fill is fully opaque", alpha(LeapPalette.CARD_BACKGROUND) == 255);
        check("hover card fill is fully opaque", alpha(LeapPalette.CARD_HOVER_BACKGROUND) == 255);
        check("normal border is fully opaque", alpha(LeapPalette.CARD_BORDER) == 255);
        check("hover border is fully opaque", alpha(LeapPalette.CARD_HOVER_BORDER) == 255);
        check("normal border contrasts with fill", LeapPalette.CARD_BORDER > LeapPalette.CARD_BACKGROUND);
        check("hover border is subtly brighter", LeapPalette.CARD_HOVER_BORDER > LeapPalette.CARD_BORDER
                && LeapPalette.CARD_HOVER_BORDER != Theme.OPAQUE_WHITE);
        check("card radius is visibly rounded but restrained", LeapPalette.CARD_RADIUS == Theme.RADIUS_CARD
                && LeapPalette.CARD_RADIUS >= 6f && LeapPalette.CARD_RADIUS <= 10f);

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
