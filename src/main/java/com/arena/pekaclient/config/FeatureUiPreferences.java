package com.arena.pekaclient.config;

import com.arena.pekaclient.gui.FeatureUiState;

import java.util.Properties;

/** Presentation-only bridge for remembered selections in reusable settings editors. */
public final class FeatureUiPreferences {
    private static final FeatureUiState STATE = new FeatureUiState();

    private FeatureUiPreferences() { }

    public static String selection(String editorId, String fallback) {
        return STATE.selection(editorId, fallback);
    }

    public static void setSelection(String editorId, String selection) {
        STATE.setSelection(editorId, selection);
    }

    public static void load(Properties settings) { STATE.load(settings); }
    public static void save(Properties settings) { STATE.save(settings); }
}
