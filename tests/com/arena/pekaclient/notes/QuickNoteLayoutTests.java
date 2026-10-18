package com.arena.pekaclient.notes;

import com.arena.pekaclient.gui.GuiLayoutToolkit;

/** Proves the production quick-note layout stays usable at the scaled 854x480 GUI resolutions. */
public final class QuickNoteLayoutTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        int[][] viewports = {{854, 480}, {427, 240}, {284, 160}, {213, 120}};
        for (int[] viewport : viewports) {
            QuickNoteLayout.Metrics metrics = new QuickNoteLayout.Metrics();
            QuickNoteLayout.update(metrics, viewport[0], viewport[1]);
            check("panel stays in " + label(viewport), inside(metrics.panel, viewport));
            check("heading remains positive in " + label(viewport), positive(metrics.heading));
            check("note remains positive in " + label(viewport), positive(metrics.note));
            check("note is on-screen in " + label(viewport), inside(metrics.note, viewport));
            check("footer does not cover note in " + label(viewport),
                    metrics.note.bottom() <= metrics.footer.y + .01f);
            if (metrics.showSidebar) {
                check("wide sidebar does not overlap note in " + label(viewport),
                        metrics.sidebar.right() <= metrics.note.x + .01f);
                check("sidebar stays in panel in " + label(viewport), contains(metrics.panel, metrics.sidebar));
            } else {
                check("compact form retires the sidebar rather than overlaps in " + label(viewport),
                        metrics.sidebar.w == 0f && metrics.sidebar.h == 0f);
            }
        }
        System.out.println("quick note layout: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static boolean positive(GuiLayoutToolkit.Rect rect) {
        return rect.w >= 1f && rect.h >= 1f;
    }

    private static boolean inside(GuiLayoutToolkit.Rect rect, int[] viewport) {
        return rect.x >= -.01f && rect.y >= -.01f && rect.right() <= viewport[0] + .01f
                && rect.bottom() <= viewport[1] + .01f;
    }

    private static boolean contains(GuiLayoutToolkit.Rect parent, GuiLayoutToolkit.Rect child) {
        return child.x >= parent.x - .01f && child.y >= parent.y - .01f
                && child.right() <= parent.right() + .01f && child.bottom() <= parent.bottom() + .01f;
    }

    private static String label(int[] viewport) {
        return viewport[0] + "x" + viewport[1];
    }

    private static void check(String title, boolean condition) {
        if (condition) passed++;
        else {
            failed++;
            System.out.println("FAIL " + title);
        }
    }
}
