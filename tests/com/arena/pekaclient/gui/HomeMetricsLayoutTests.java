package com.arena.pekaclient.gui;

/** Equal-card geometry proof for the Home FPS, Memory and Ping performance surface. */
public final class HomeMetricsLayoutTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        equalCardsRemainInsideBoundsAtEveryGuiScale();
        System.out.println("home metrics layout: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void equalCardsRemainInsideBoundsAtEveryGuiScale() {
        float[] widths = {1f, 120f, 299f, 300f, 480f, 900f};
        for (float width : widths) {
            HomeMetricsLayout.Layout layout = new HomeMetricsLayout.Layout();
            HomeMetricsLayout.update(layout, 7f, 11f, width);
            check("positive card group height " + width, layout.height > 0f && layout.height == HomeMetricsLayout.heightFor(width));
            for (int i = 0; i < HomeMetricsLayout.COUNT; i++) {
                GuiLayoutToolkit.Rect card = layout.cards[i];
                check("metric stays in bounds " + width + " #" + i,
                        card.x >= 7f && card.right() <= 7f + width + .01f);
                check("equal card height " + width + " #" + i,
                        card.h == (layout.stacked ? HomeMetricsLayout.STACKED_CARD_HEIGHT : HomeMetricsLayout.WIDE_CARD_HEIGHT));
                for (int other = i + 1; other < HomeMetricsLayout.COUNT; other++) {
                    check("metrics never overlap " + width + " #" + i + "/" + other,
                            !card.intersects(layout.cards[other]));
                }
            }
            check("small row stacks before compression " + width, width >= HomeMetricsLayout.STACK_AT_WIDTH || layout.stacked);
        }
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++; else { failed++; System.err.println("FAIL " + name); }
    }
}
