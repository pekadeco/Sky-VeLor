package com.arena.pekaclient.gui;

/** Geometry proof for compact/fullscreen reference-window modes across Minecraft GUI scales. */
public final class ReferenceSettingsWindowLayoutTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        normalWindowUsesRequestedDesktopFootprint();
        fullscreenAndLowResolutionBoundsStayUsable();
        smallWindowDragRetainsItsOffsetAfterFullscreenRoundTrip();
        titleBarDragOffsetStaysVisibleAndUsesSharedBounds();
        System.out.println("reference window layout: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void normalWindowUsesRequestedDesktopFootprint() {
        GuiLayoutToolkit.Rect normal = ReferenceSettingsWindowLayout.layout(new GuiLayoutToolkit.Rect(), 900f, 600f, false);
        float availableW = 900f - ReferenceSettingsWindowLayout.OUTER_MARGIN * 2f;
        float availableH = 600f - ReferenceSettingsWindowLayout.OUTER_MARGIN * 2f;
        check("normal width is 85 percent before the fixed maximum", close(normal.w, availableW * .85f));
        check("normal height is 82 percent before the fixed maximum", close(normal.h, availableH * .82f));
        check("normal window leaves a visible world border", normal.x >= ReferenceSettingsWindowLayout.OUTER_MARGIN - .01f
                && normal.y >= ReferenceSettingsWindowLayout.OUTER_MARGIN - .01f);
    }

    private static void fullscreenAndLowResolutionBoundsStayUsable() {
        GuiLayoutToolkit.Rect huge = ReferenceSettingsWindowLayout.layout(new GuiLayoutToolkit.Rect(), 1920f, 1080f, false);
        check("normal maximum width is capped", huge.w == ReferenceSettingsWindowLayout.NORMAL_MAX_WIDTH);
        check("normal maximum height is capped", huge.h == ReferenceSettingsWindowLayout.NORMAL_MAX_HEIGHT);
        GuiLayoutToolkit.Rect fullscreen = ReferenceSettingsWindowLayout.layout(new GuiLayoutToolkit.Rect(), 960f, 540f, true);
        check("fullscreen fills the safe width", close(fullscreen.w, 960f - ReferenceSettingsWindowLayout.OUTER_MARGIN * 2f));
        check("fullscreen fills the safe height", close(fullscreen.h, 540f - ReferenceSettingsWindowLayout.OUTER_MARGIN * 2f));
        check("fullscreen remains centered", centered(fullscreen, 960f, 540f));

        float[] widths = {1f, 80f, 160f, 320f, 640f};
        float[] heights = {1f, 70f, 140f, 240f, 480f};
        for (int i = 0; i < widths.length; i++) {
            GuiLayoutToolkit.Rect tiny = ReferenceSettingsWindowLayout.layout(new GuiLayoutToolkit.Rect(), widths[i], heights[i], false);
            check("small bounds stay on screen at scale " + i, tiny.x >= 0f && tiny.y >= 0f
                    && tiny.right() <= widths[i] + .01f && tiny.bottom() <= heights[i] + .01f);
        }
    }

    private static void smallWindowDragRetainsItsOffsetAfterFullscreenRoundTrip() {
        GuiLayoutToolkit.Rect small = ReferenceSettingsWindowLayout.layout(new GuiLayoutToolkit.Rect(), 960f, 540f, false, 90f, -40f);
        float offsetX = small.x - ReferenceSettingsWindowLayout.centeredX(960f, small.w);
        float offsetY = small.y - ReferenceSettingsWindowLayout.centeredY(540f, small.h);
        ReferenceSettingsWindowLayout.layout(new GuiLayoutToolkit.Rect(), 960f, 540f, true, offsetX, offsetY);
        GuiLayoutToolkit.Rect restored = ReferenceSettingsWindowLayout.layout(new GuiLayoutToolkit.Rect(), 960f, 540f, false, offsetX, offsetY);
        check("small window X drag survives fullscreen", close(small.x, restored.x));
        check("small window Y drag survives fullscreen", close(small.y, restored.y));
    }

    private static void titleBarDragOffsetStaysVisibleAndUsesSharedBounds() {
        GuiLayoutToolkit.Rect moved = ReferenceSettingsWindowLayout.layout(new GuiLayoutToolkit.Rect(),
                960f, 540f, false, 200f, -200f);
        check("title-bar drag translates the centered desktop window", moved.x > ReferenceSettingsWindowLayout.OUTER_MARGIN
                && moved.y >= ReferenceSettingsWindowLayout.OUTER_MARGIN - .01f);
        check("dragged window retains the visible world border", moved.right() <= 960f - ReferenceSettingsWindowLayout.OUTER_MARGIN + .01f
                && moved.bottom() <= 540f - ReferenceSettingsWindowLayout.OUTER_MARGIN + .01f);

        GuiLayoutToolkit.Rect constrained = ReferenceSettingsWindowLayout.layout(new GuiLayoutToolkit.Rect(),
                960f, 540f, false, 5000f, -5000f);
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
