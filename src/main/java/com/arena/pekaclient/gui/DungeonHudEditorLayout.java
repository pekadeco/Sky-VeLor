package com.arena.pekaclient.gui;

/**
 * Minecraft-free geometry for the direct-manipulation Dungeon HUD editor.
 *
 * <p>The canvas deliberately occupies the whole screen: HUD samples are never squeezed into a fake preview column.
 * The only fixed surface is a slim top-centre control strip. A requested options panel is docked beside/above/below
 * the selected element and is hidden rather than covering it when no safe slot exists. On narrow Pojav layouts the
 * same panel becomes a shallow bottom sheet so its toggles and sliders remain touchable.</p>
 */
public final class DungeonHudEditorLayout {
    private static final float EDGE = 6F;
    private static final float TOP_RIGHT_LAUNCHER_RESERVE = 78F;

    public final GuiLayoutToolkit.Rect topBar = new GuiLayoutToolkit.Rect();
    public final GuiLayoutToolkit.Rect inspector = new GuiLayoutToolkit.Rect();
    public final GuiLayoutToolkit.Rect canvas = new GuiLayoutToolkit.Rect();
    public final boolean compact;
    public boolean inspectorVisible;

    private final float width;
    private final float height;

    private DungeonHudEditorLayout(int screenWidth, int screenHeight) {
        width = Math.max(1F, screenWidth);
        height = Math.max(1F, screenHeight);
        compact = width < 560F || height < 300F;
        canvas.set(0F, 0F, width, height);

        float safeWidth = Math.max(1F, width - EDGE * 2F - TOP_RIGHT_LAUNCHER_RESERVE);
        float wanted = compact ? 232F : 430F;
        float barWidth = Math.min(wanted, safeWidth);
        // Centre inside the safe region left of launcher MOUSE/GUI controls rather than occupying the top-right.
        float barX = EDGE + Math.max(0F, (safeWidth - barWidth) * .5F);
        topBar.set(barX, EDGE, barWidth, compact ? 23F : 25F);
        inspector.set(0F, 0F, 0F, 0F);
    }

    public static DungeonHudEditorLayout create(int screenWidth, int screenHeight) {
        return new DungeonHudEditorLayout(screenWidth, screenHeight);
    }

    /**
     * Places the compact selected-element inspector without covering the supplied selection rectangle. A false
     * request, or an oversized target that leaves no non-overlapping slot, physically removes the panel from layout.
     */
    public void dockInspector(GuiLayoutToolkit.Rect selected, float wantedHeight, boolean requested) {
        inspectorVisible = false;
        inspector.set(0F, 0F, 0F, 0F);
        if (!requested || selected == null) return;

        if (compact) {
            // 320-pixel Pojav screens cannot fit usable switch/slider rows in 25% width. Use a shallow, collapsible
            // bottom sheet there; its height is bounded and it still yields to the selected element.
            float panelW = Math.max(1F, width - EDGE * 2F);
            float panelH = clamp(wantedHeight, 92F, Math.max(92F, Math.min(210F, height * .44F)));
            if (trySlot(EDGE, height - EDGE - panelH, panelW, panelH, selected)) return;
            if (trySlot(EDGE, topBar.bottom() + EDGE, panelW, panelH, selected)) return;
            return;
        }

        // Wide layouts cap the dock at one quarter of the screen width (and 240 GUI px), avoiding the former
        // obstructive half-screen inspector.
        float panelW = Math.min(240F, Math.max(120F, width * .25F));
        float panelH = clamp(wantedHeight, 118F, Math.max(118F, Math.min(300F, height - topBar.bottom() - EDGE * 2F)));
        float lowestY = topBar.bottom() + EDGE;
        float highestY = Math.max(lowestY, height - EDGE - panelH);
        float y = clamp(selected.y, lowestY, highestY);

        if (trySlot(width - EDGE - panelW, y, panelW, panelH, selected)) return;
        if (trySlot(EDGE, y, panelW, panelH, selected)) return;

        // A deliberately wide selected map can touch both side docks. Above/below it is still usable if a vertical
        // gap exists; otherwise the Options chip remains available while no panel obscures the direct canvas.
        float x = clamp(selected.x, EDGE, Math.max(EDGE, width - EDGE - panelW));
        if (trySlot(x, selected.bottom() + EDGE, panelW, panelH, selected)) return;
        trySlot(x, selected.y - EDGE - panelH, panelW, panelH, selected);
    }

    /** True when fixed editor surfaces are inside the screen, with no options panel covering the top controls. */
    public boolean validFor(int screenWidth, int screenHeight) {
        float sw = Math.max(1F, screenWidth);
        float sh = Math.max(1F, screenHeight);
        if (!inside(canvas, sw, sh) || !inside(topBar, sw, sh)) return false;
        return !inspectorVisible || inside(inspector, sw, sh) && !inspector.intersects(topBar);
    }

    /** Explicit contract hook for layout tests and future callers. */
    public boolean inspectorAvoids(GuiLayoutToolkit.Rect selected) {
        return !inspectorVisible || selected == null || !inspector.intersects(selected);
    }

    private boolean trySlot(float x, float y, float w, float h, GuiLayoutToolkit.Rect selected) {
        if (x < EDGE - .01F || y < topBar.bottom() + EDGE - .01F || x + w > width - EDGE + .01F
                || y + h > height - EDGE + .01F) return false;
        GuiLayoutToolkit.Rect candidate = new GuiLayoutToolkit.Rect().set(x, y, w, h);
        if (candidate.intersects(selected) || candidate.intersects(topBar)) return false;
        inspector.set(x, y, w, h);
        inspectorVisible = true;
        return true;
    }

    private static boolean inside(GuiLayoutToolkit.Rect rect, float width, float height) {
        return rect.x >= -0.01F && rect.y >= -0.01F && rect.right() <= width + .01F && rect.bottom() <= height + .01F;
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
