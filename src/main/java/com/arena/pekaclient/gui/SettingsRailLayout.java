package com.arena.pekaclient.gui;

import com.arena.pekaclient.render.GuiStyle;

/**
 * Pure, reusable geometry for the six-channel settings rail.
 *
 * <p>The normal rail uses 30 px entries separated by the shared 4 px rhythm. On small GUI surfaces it changes to a
 * two-row, three-column compact rail rather than shrinking, overlapping, or hiding a destination. The profile footer
 * is always docked at the rail's bottom edge. Rendering and hit testing share one {@link Rail} instance.</p>
 */
public final class SettingsRailLayout {
    public static final float ENTRY_HEIGHT = 30f;
    public static final float COMPACT_ENTRY_HEIGHT = 28f;
    public static final float ENTRY_GAP = GuiStyle.SPACE_4;
    public static final float RAIL_PADDING = GuiStyle.SPACE_8;
    public static final float COMPACT_PADDING = GuiStyle.SPACE_4;
    public static final float PROFILE_HEIGHT = 36f;
    public static final float COMPACT_PROFILE_HEIGHT = GuiStyle.SPACE_16;
    public static final int COMPACT_COLUMNS = 3;

    private SettingsRailLayout() {
    }

    /** Reusable bounds: created once by a screen, so layout refreshes introduce no box allocations. */
    public static final class Rail {
        public final GuiLayoutToolkit.Rect[] entries;
        public final GuiLayoutToolkit.Rect profile = new GuiLayoutToolkit.Rect();
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
        return RAIL_PADDING + navigation + GuiStyle.SPACE_8 + PROFILE_HEIGHT + RAIL_PADDING;
    }

    public static float compactHeight(int tabCount) {
        int rows = compactRows(tabCount);
        return COMPACT_PADDING + rows * COMPACT_ENTRY_HEIGHT + Math.max(0, rows - 1) * ENTRY_GAP
                + ENTRY_GAP + COMPACT_PROFILE_HEIGHT + COMPACT_PADDING;
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
        float profileH = Math.min(PROFILE_HEIGHT, Math.max(0f, height));
        float profileY = Math.max(cursor, y + Math.max(0f, height - RAIL_PADDING - profileH));
        // The caller selects compact mode before this point when the normal rail cannot fit; the max keeps a
        // pathological one-pixel screen bounded without moving a footer over a navigation item.
        if (profileY + profileH > y + height) profileY = Math.max(cursor, y + height - profileH);
        out.profile.set(navX, profileY, navW, profileH);
    }

    private static void layoutCompact(Rail out, float x, float y, float width, float height) {
        int count = out.entries.length;
        int columns = Math.max(1, Math.min(COMPACT_COLUMNS, Math.max(1, count)));
        int rows = compactRows(count);
        out.columns = columns;
        float footerH = Math.min(COMPACT_PROFILE_HEIGHT, Math.max(0f, height));
        float fixed = COMPACT_PADDING * 2f + Math.max(0, rows - 1) * ENTRY_GAP + ENTRY_GAP + footerH;
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
        float navBottom = y + COMPACT_PADDING + rows * entryH + Math.max(0, rows - 1) * ENTRY_GAP;
        float profileY = Math.max(navBottom + ENTRY_GAP, y + height - COMPACT_PADDING - footerH);
        if (profileY + footerH > y + height) profileY = Math.max(navBottom, y + height - footerH);
        out.profile.set(innerX, profileY, innerW, footerH);
    }

    private static int compactRows(int tabCount) {
        int count = Math.max(0, tabCount);
        return count == 0 ? 0 : (count + COMPACT_COLUMNS - 1) / COMPACT_COLUMNS;
    }
}
