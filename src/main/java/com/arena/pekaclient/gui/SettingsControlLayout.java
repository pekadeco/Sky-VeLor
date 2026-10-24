package com.arena.pekaclient.gui;

/**
 * One right-side control policy for settings rows.
 *
 * <p>Labels own the left column and every toggle, slider, segmented choice, or route action ends on this shared
 * trailing edge. The pure helpers are deliberately used by the screen and scale-1–4 checks so a narrow page cannot
 * drift controls into text or leave sibling controls misaligned.</p>
 */
public final class SettingsControlLayout {
    private SettingsControlLayout() {
    }

    /** Right edge for a control inside a row with the declared trailing padding. */
    public static float rightX(float rowX, float rowWidth, float trailingPadding, float controlWidth) {
        float width = Math.max(0f, rowWidth);
        float control = Math.max(0f, Math.min(width, controlWidth));
        float pad = Math.max(0f, Math.min(width - control, trailingPadding));
        return rowX + width - pad - control;
    }

    /** Centers a right-side control in the primary (non-child) section of a row. */
    public static float centeredY(float rowY, float primaryHeight, float controlHeight) {
        return rowY + Math.max(0f, (Math.max(0f, primaryHeight) - Math.max(0f, controlHeight)) * .5f);
    }

    /** Child/narrow rows keep their direct control below text with an explicit bottom inset. */
    public static float stackedY(float rowY, float rowHeight, float controlHeight, float bottomInset) {
        return rowY + Math.max(0f, rowHeight - Math.max(0f, controlHeight) - Math.max(0f, bottomInset));
    }

    /** A label column must leave a positive gap before a right-side control. */
    public static boolean doesNotOverlap(float labelX, float labelWidth, float controlX, float gap) {
        return labelX + Math.max(0f, labelWidth) + Math.max(0f, gap) <= controlX + .01f;
    }
}
