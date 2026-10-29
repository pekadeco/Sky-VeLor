package com.arena.pekaclient.gui;

/**
 * Responsive, Minecraft-free geometry for the dedicated Dungeon HUD editor. Wide screens keep a picker and inspector
 * at opposite edges; Pojav/narrow screens move the picker above and inspector below the preview canvas, so solid UI
 * surfaces never overlap each other or run off screen.
 */
public final class DungeonHudEditorLayout {
    public final GuiLayoutToolkit.Rect header = new GuiLayoutToolkit.Rect();
    public final GuiLayoutToolkit.Rect picker = new GuiLayoutToolkit.Rect();
    public final GuiLayoutToolkit.Rect inspector = new GuiLayoutToolkit.Rect();
    public final GuiLayoutToolkit.Rect canvas = new GuiLayoutToolkit.Rect();
    public final boolean compact;

    private DungeonHudEditorLayout(boolean compact) {
        this.compact = compact;
    }

    public static DungeonHudEditorLayout create(int screenWidth, int screenHeight) {
        float width = Math.max(1, screenWidth);
        float height = Math.max(1, screenHeight);
        boolean compact = width < 600F || height < 300F;
        DungeonHudEditorLayout result = new DungeonHudEditorLayout(compact);
        result.header.set(0F, 0F, width, Math.min(28F, height));
        if (!compact) {
            float pickerW = clamp(width * .20F, 116F, 156F);
            float inspectorW = clamp(width * .29F, 178F, 250F);
            float top = result.header.bottom() + 6F;
            float bottom = Math.max(top + 1F, height - 6F);
            result.picker.set(6F, top, pickerW, bottom - top);
            result.inspector.set(Math.max(result.picker.right() + 8F, width - inspectorW - 6F), top,
                    Math.min(inspectorW, Math.max(1F, width - result.picker.right() - 14F)), bottom - top);
            result.canvas.set(result.picker.right() + 8F, top,
                    Math.max(1F, result.inspector.x - result.picker.right() - 16F), bottom - top);
        } else {
            float top = result.header.bottom() + 4F;
            float pickerH = Math.min(34F, Math.max(24F, height * .15F));
            float inspectorH = Math.min(166F, Math.max(94F, height * .42F));
            float inspectorY = Math.max(top + pickerH + 8F, height - inspectorH - 4F);
            result.picker.set(4F, top, Math.max(1F, width - 8F), pickerH);
            result.inspector.set(4F, inspectorY, Math.max(1F, width - 8F), Math.max(1F, height - inspectorY - 4F));
            result.canvas.set(0F, result.picker.bottom() + 4F, width,
                    Math.max(1F, result.inspector.y - result.picker.bottom() - 8F));
        }
        return result;
    }

    /** True when the editor's fixed surfaces are inside the screen and do not collide. */
    public boolean validFor(int screenWidth, int screenHeight) {
        float width = Math.max(1, screenWidth);
        float height = Math.max(1, screenHeight);
        return inside(header, width, height) && inside(picker, width, height) && inside(inspector, width, height)
                && inside(canvas, width, height) && !picker.intersects(inspector) && !picker.intersects(canvas)
                && !inspector.intersects(canvas);
    }

    private static boolean inside(GuiLayoutToolkit.Rect rect, float width, float height) {
        return rect.x >= 0F && rect.y >= 0F && rect.right() <= width + .01F && rect.bottom() <= height + .01F;
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
