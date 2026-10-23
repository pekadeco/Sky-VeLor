package com.arena.pekaclient.gui;

/** Geometry regression for General-category FPS/Ping/Memory cards at every practical GUI width. */
public final class GeneralMetricsLayoutTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        float[] widths = {1f, 80f, 159f, 249f, 250f, 427f, 854f, 1708f};
        GeneralMetricsLayout.Layout layout = new GeneralMetricsLayout.Layout();
        for (float width : widths) {
            GeneralMetricsLayout.update(layout, 4f, 7f, width);
            check("positive metric height " + width, layout.height > 0f);
            for (int i = 0; i < GeneralMetricsLayout.COUNT; i++) {
                GuiLayoutToolkit.Rect card = layout.cards[i];
                check("metric card in horizontal bounds " + width + " #" + i,
                        card.x >= 4f && card.right() <= 4f + width + .01f);
                for (int other = i + 1; other < GeneralMetricsLayout.COUNT; other++) {
                    check("metric cards do not overlap " + width + " #" + i + "/" + other,
                            !card.intersects(layout.cards[other]));
                }
            }
            check("small cards stack " + width, width >= 250f || layout.stacked);
            check("wide cards share a row " + width, width < 250f || !layout.stacked);
        }
        System.out.println("general metrics layout: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++; else { failed++; System.err.println("FAIL " + name); }
    }
}
