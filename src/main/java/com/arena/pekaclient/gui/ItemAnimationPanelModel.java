package com.arena.pekaclient.gui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Pure contract for the standalone Item Animation panel. */
public final class ItemAnimationPanelModel {
    private static final List<String> RENDER_LAYERS = Collections.unmodifiableList(
            Arrays.asList("item-animation-panel"));
    private static final List<String> CONTROL_LABELS = Collections.unmodifiableList(Arrays.asList(
            "Hook", "Enable", "Scale", "Position", "X", "Y", "Z", "Animation",
            "Custom Hit/Slash Style", "Custom Hit/Slash Speed", "Smooth Swing", "Smoothness",
            "Options", "Remove Haste Effect", "Loop Preview Swing", "Preview Swing", "Reset to Defaults"));

    private ItemAnimationPanelModel() {
    }

    /** The panel deliberately paints no HUD canvas, toolbar, veil, or item sample surface; its one panel is opaque. */
    public static List<String> renderLayers() {
        return RENDER_LAYERS;
    }

    /** All player-visible controls; retired background controls intentionally do not belong here. */
    public static List<String> controlLabels() {
        return CONTROL_LABELS;
    }

    public static boolean hasRetiredBackgroundControl() {
        for (String label : CONTROL_LABELS) {
            String lower = label.toLowerCase(java.util.Locale.ROOT);
            if (lower.contains("transparency") || lower.contains("opacity") || lower.contains("background")) return true;
        }
        return false;
    }
}
