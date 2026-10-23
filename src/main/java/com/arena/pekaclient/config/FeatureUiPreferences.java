package com.arena.pekaclient.config;

import com.arena.pekaclient.gui.FeatureUiState;

import java.util.Properties;

/**
 * Presentation-only bridge for the hub tree and reusable settings editors.
 * Feature settings continue to be owned by their existing schemas; these keys only remember where the user was.
 */
public final class FeatureUiPreferences {
    private static final FeatureUiState STATE = new FeatureUiState();

    private FeatureUiPreferences() { }

    public static boolean isExpanded(String id, boolean fallback) {
        return STATE.isExpanded(id, fallback);
    }

    public static void setExpanded(String id, boolean open) {
        STATE.setExpanded(id, open);
    }

    public static void toggleExpanded(String id, boolean fallback) {
        STATE.toggleExpanded(id, fallback);
    }

    public static String selection(String editorId, String fallback) {
        return STATE.selection(editorId, fallback);
    }

    public static void setSelection(String editorId, String selection) {
        STATE.setSelection(editorId, selection);
    }

    public static void load(Properties settings) {
        STATE.load(settings);
    }

    public static void save(Properties settings) {
        STATE.save(settings);
    }
}
