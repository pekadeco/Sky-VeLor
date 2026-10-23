package com.arena.pekaclient.gui;

/** Geometry proof for normal, larger and minimized reference-window modes across Minecraft GUI scales. */
public final class ReferenceSettingsWindowLayoutTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        normalWindowUsesRequestedDesktopFootprint();
        maximumAndLowResolutionBoundsStayUsable();
        desktopModesRemainCenteredAndRestorable();
        titleBarDragOffsetStaysVisibleAndUsesSharedBounds();
        System.out.println("reference window layout: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void normalWindowUsesRequestedDesktopFootprint() {
        GuiLayoutToolkit.Rect normal = ReferenceSettingsWindowLayout.layout(new GuiLayoutToolkit.Rect(), 900f, 600f, false, false);
        float availableW = 900f - ReferenceSettingsWindowLayout.OUTER_MARGIN * 2f;
        float availableH = 600f - ReferenceSettingsWindowLayout.OUTER_MARGIN * 2f;
        check("normal width is 85 percent before the fixed maximum", close(normal.w, availableW * .85f));
        check("normal height is 82 percent before the fixed maximum", close(normal.h, availableH * .82f));
        check("normal window leaves a visible world border", normal.x >= ReferenceSettingsWindowLayout.OUTER_MARGIN - .01f
                && normal.y >= ReferenceSettingsWindowLayout.OUTER_MARGIN - .01f);
    }

    private static void maximumAndLowResolutionBoundsStayUsable() {
        GuiLayoutToolkit.Rect huge = ReferenceSettingsWindowLayout.layout(new GuiLayoutToolkit.Rect(), 1920f, 1080f, false, false);
        check("normal maximum width is capped", huge.w == ReferenceSettingsWindowLayout.NORMAL_MAX_WIDTH);
        check("normal maximum height is capped", huge.h == ReferenceSettingsWindowLayout.NORMAL_MAX_HEIGHT);

        float[] widths = {1f, 80f, 160f, 320f, 640f};
        float[] heights = {1f, 70f, 140f, 240f, 480f};
        for (int i = 0; i < widths.length; i++) {
            GuiLayoutToolkit.Rect tiny = ReferenceSettingsWindowLayout.layout(new GuiLayoutToolkit.Rect(), widths[i], heights[i], false, false);
            check("normal bounds stay on screen at scale " + i, tiny.x >= 0f && tiny.y >= 0f
                    && tiny.right() <= widths[i] + .01f && tiny.bottom() <= heights[i] + .01f);
        }
    }

    private static void desktopModesRemainCenteredAndRestorable() {
        GuiLayoutToolkit.Rect normal = ReferenceSettingsWindowLayout.layout(new GuiLayoutToolkit.Rect(), 960f, 540f, false, false);
        GuiLayoutToolkit.Rect larger = ReferenceSettingsWindowLayout.layout(new GuiLayoutToolkit.Rect(), 960f, 540f, true, false);
        GuiLayoutToolkit.Rect minimized = ReferenceSettingsWindowLayout.layout(new GuiLayoutToolkit.Rect(), 960f, 540f, false, true);
        check("green larger mode grows the normal window", larger.w > normal.w && larger.h > normal.h);
        check("all desktop modes remain centered", centered(normal, 960f, 540f)
                && centered(larger, 960f, 540f) && centered(minimized, 960f, 540f));
        check("yellow minimized strip exposes controls", minimized.h == ReferenceSettingsWindowLayout.MINIMIZED_HEIGHT
                && ReferenceSettingsWindowLayout.canShowDesktopControls(minimized));
    }

    private static void titleBarDragOffsetStaysVisibleAndUsesSharedBounds() {
        GuiLayoutToolkit.Rect moved = ReferenceSettingsWindowLayout.layout(new GuiLayoutToolkit.Rect(),
                960f, 540f, false, false, 200f, -200f);
        check("title-bar drag translates the centered desktop window", moved.x > ReferenceSettingsWindowLayout.OUTER_MARGIN
                && moved.y >= ReferenceSettingsWindowLayout.OUTER_MARGIN - .01f);
        check("dragged window retains the visible world border", moved.right() <= 960f - ReferenceSettingsWindowLayout.OUTER_MARGIN + .01f
                && moved.bottom() <= 540f - ReferenceSettingsWindowLayout.OUTER_MARGIN + .01f);

        GuiLayoutToolkit.Rect constrained = ReferenceSettingsWindowLayout.layout(new GuiLayoutToolkit.Rect(),
                960f, 540f, false, false, 5000f, -5000f);
        check("extreme drag is constrained instead of losing the title bar", constrained.x >= ReferenceSettingsWindowLayout.OUTER_MARGIN - .01f
                && constrained.y >= ReferenceSettingsWindowLayout.OUTER_MARGIN - .01f
                && constrained.right() <= 960f - ReferenceSettingsWindowLayout.OUTER_MARGIN + .01f
                && constrained.bottom() <= 540f - ReferenceSettingsWindowLayout.OUTER_MARGIN + .01f);
        check("shared layout reports the same centered origin used to derive drag offsets",
                close(ReferenceSettingsWindowLayout.centeredX(960f, moved.w), (960f - moved.w) * .5f)
                        && close(ReferenceSettingsWindowLayout.centeredY(540f, moved.h), (540f - moved.h) * .5f));
    }

    private static boolean centered(GuiLayoutToolkit.Rect bounds, float w, float h) {
        return close(bounds.x, (w - bounds.w) * .5f) && close(bounds.y, (h - bounds.h) * .5f);
    }

    private static boolean close(float one, float two) {
        return Math.abs(one - two) < .02f;
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++; else { failed++; System.out.println("FAIL " + name); }
    }
}
