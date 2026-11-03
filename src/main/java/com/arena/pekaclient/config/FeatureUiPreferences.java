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

    public static boolean dropdownOpen(String featureId) { return STATE.dropdownOpen(featureId); }
    public static void setDropdownOpen(String featureId, boolean open) { STATE.setDropdownOpen(featureId, open); }
    /** Clears stale persisted disclosure state when a new top-level settings screen is opened. */
    public static void closeAllDropdowns() { STATE.closeAllDropdowns(); }
    public static void openAccordion(String featureId, Iterable<String> peerIds) { STATE.openAccordion(featureId, peerIds); }

    public static void load(Properties settings) { STATE.load(settings); }
    public static void save(Properties settings) { STATE.save(settings); }
}
