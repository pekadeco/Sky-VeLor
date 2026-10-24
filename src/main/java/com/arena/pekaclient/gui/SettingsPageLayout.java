package com.arena.pekaclient.gui;

import com.arena.pekaclient.render.GuiStyle;

/** Pure responsive measurements shared by the settings page's card flows. */
public final class SettingsPageLayout {
    /** Below this usable content width, labels and controls reflow into a single readable column. */
    public static final float NARROW_CONTENT_WIDTH = 405f;

    private SettingsPageLayout() {
    }

    public static boolean isNarrow(float contentWidth) {
        return contentWidth < NARROW_CONTENT_WIDTH;
    }

    public static int columnsFor(float contentWidth) {
        return isNarrow(contentWidth) ? 1 : 2;
    }

    public static int rowsFor(int itemCount, int columns) {
        int safeColumns = Math.max(1, columns);
        return Math.max(0, (Math.max(0, itemCount) + safeColumns - 1) / safeColumns);
    }

    public static float itemWidth(float availableWidth, int columns) {
        int safeColumns = Math.max(1, columns);
        return Math.max(1f, (Math.max(1f, availableWidth) - GuiStyle.SPACE_8 * (safeColumns - 1)) / safeColumns);
    }

    public static float gridHeight(int itemCount, int columns, float itemHeight) {
        int rows = rowsFor(itemCount, columns);
        return rows == 0 ? 0f : rows * Math.max(0f, itemHeight) + (rows - 1) * GuiStyle.SPACE_8;
    }
}
