package com.arena.pekaclient.gui;

import com.arena.pekaclient.render.GuiStyle;

/**
 * Pure, reusable geometry for the settings-channel rail.
 *
 * <p>The normal rail uses 30 px entries separated by the shared 4 px rhythm. On small GUI surfaces it changes to a
 * four-column compact rail rather than shrinking, overlapping, or hiding a destination. There is no profile
 * footer or preset card: every rectangle represents one of the current settings pages, and rendering/hit testing share
 * one {@link Rail} instance.</p>
 */
public final class SettingsRailLayout {
    public static final float ENTRY_HEIGHT = 30f;
    /** Nine destinations retain a 26 px compact target rather than becoming clipped at GUI scale four. */
    public static final float COMPACT_ENTRY_HEIGHT = 26f;
    public static final float ENTRY_GAP = GuiStyle.SPACE_4;
    public static final float RAIL_PADDING = GuiStyle.SPACE_8;
    public static final float COMPACT_PADDING = GuiStyle.SPACE_4;
    /** A four-column compact rail grows to the required number of rows without shrinking destinations. */
    public static final int COMPACT_COLUMNS = 4;

    private SettingsRailLayout() {
    }

    /** Reusable bounds: created once by a screen, so layout refreshes introduce no box allocations. */
    public static final class Rail {
        public final GuiLayoutToolkit.Rect[] entries;
        public boolean compact;
        public float entryHeight;
        public int columns;

        public Rail(int tabCount) {
            entries = new GuiLayoutToolkit.Rect[Math.max(0, tabCount)];
            for (int i = 0; i < entries.length; i++) entries[i] = new GuiLayoutToolkit.Rect();
        }
    }

    public static float verticalMinimumHeight(int tabCount) {
        int count = Math.max(0, tabCount);
        float navigation = count * ENTRY_HEIGHT + Math.max(0, count - 1) * ENTRY_GAP;
        return RAIL_PADDING + navigation + RAIL_PADDING;
    }

    public static float compactHeight(int tabCount) {
        int rows = compactRows(tabCount);
        return COMPACT_PADDING + rows * COMPACT_ENTRY_HEIGHT + Math.max(0, rows - 1) * ENTRY_GAP + COMPACT_PADDING;
    }

    public static boolean shouldCompact(float availableWidth, float availableHeight, int tabCount) {
        return availableWidth < 540f || availableHeight < verticalMinimumHeight(tabCount);
    }

    public static void layout(Rail out, float x, float y, float width, float height, boolean compact) {
        if (out == null) return;
        float w = Math.max(0f, width);
        float h = Math.max(0f, height);
        out.compact = compact;
        if (compact) {
            layoutCompact(out, x, y, w, h);
        } else {
            layoutVertical(out, x, y, w, h);
        }
    }

    private static void layoutVertical(Rail out, float x, float y, float width, float height) {
        out.columns = 1;
        out.entryHeight = ENTRY_HEIGHT;
        float navX = x + Math.min(RAIL_PADDING, width * .5f);
        float navW = Math.max(0f, width - Math.min(RAIL_PADDING * 2f, width));
        float cursor = y + Math.min(RAIL_PADDING, height);
        for (GuiLayoutToolkit.Rect entry : out.entries) {
            entry.set(navX, cursor, navW, ENTRY_HEIGHT);
            cursor += ENTRY_HEIGHT + ENTRY_GAP;
        }
    }

    private static void layoutCompact(Rail out, float x, float y, float width, float height) {
        int count = out.entries.length;
        int columns = Math.max(1, Math.min(COMPACT_COLUMNS, Math.max(1, count)));
        int rows = compactRows(count);
        out.columns = columns;
        float fixed = COMPACT_PADDING * 2f + Math.max(0, rows - 1) * ENTRY_GAP;
        float entryH = rows == 0 ? 0f : Math.max(1f, Math.min(COMPACT_ENTRY_HEIGHT, (height - fixed) / rows));
        out.entryHeight = entryH;
        float innerX = x + Math.min(COMPACT_PADDING, width * .5f);
        float innerW = Math.max(0f, width - Math.min(COMPACT_PADDING * 2f, width));
        float entryW = Math.max(0f, (innerW - Math.max(0, columns - 1) * ENTRY_GAP) / columns);
        for (int i = 0; i < count; i++) {
            int row = i / columns;
            int column = i % columns;
            out.entries[i].set(innerX + column * (entryW + ENTRY_GAP),
                    y + COMPACT_PADDING + row * (entryH + ENTRY_GAP), entryW, entryH);
        }
    }

    private static int compactRows(int tabCount) {
        int count = Math.max(0, tabCount);
        return count == 0 ? 0 : (count + COMPACT_COLUMNS - 1) / COMPACT_COLUMNS;
    }
}
