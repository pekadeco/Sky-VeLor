package com.arena.pekaclient.gui;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Small, Minecraft-free persistence model for presentation state only.
 *
 * <p>The retired tree namespace is accepted when old properties load but is intentionally never interpreted or
 * created. Feature settings remain in their existing owners; this class remembers only selection compatibility and
 * collapsed inline dropdown disclosure.</p>
 */
public final class FeatureUiState {
    public static final String PREFIX = "ui.feature.";
    private static final int MAX_ENTRIES = 96;
    private static final int MAX_VALUE_LENGTH = 48;

    private final Map<String, String> selections = new HashMap<String, String>();
    /** Collapsed-by-default inline feature dropdown state; this never changes a feature configuration value. */
    private final Map<String, Boolean> dropdowns = new HashMap<String, Boolean>();

    public String selection(String editorId, String fallback) {
        String value = selections.get(cleanId(editorId));
        return value == null || value.length() == 0 ? fallback : value;
    }

    public void setSelection(String editorId, String selection) {
        String key = cleanId(editorId);
        String value = cleanValue(selection);
        if (key.length() == 0 || value.length() == 0) return;
        if (!selections.containsKey(key) && selections.size() >= MAX_ENTRIES) return;
        selections.put(key, value);
    }

    /** Returns false for unknown rows so every new feature starts collapsed. */
    public boolean dropdownOpen(String featureId) {
        Boolean value = dropdowns.get(cleanId(featureId));
        return Boolean.TRUE.equals(value);
    }

    public void setDropdownOpen(String featureId, boolean open) {
        String key = cleanId(featureId);
        if (key.length() == 0) return;
        if (!dropdowns.containsKey(key) && dropdowns.size() >= MAX_ENTRIES) return;
        dropdowns.put(key, open);
    }

    /**
     * Collapses every known disclosure for a fresh top-level settings screen.
     *
     * <p>Unknown ids already resolve to collapsed. Retaining explicit {@code false} values lets the next save
     * replace a legacy persisted {@code true}, rather than allowing it to reappear on a later load.</p>
     */
    public void closeAllDropdowns() {
        for (Map.Entry<String, Boolean> entry : dropdowns.entrySet()) entry.setValue(Boolean.FALSE);
    }

    /** Opens one page disclosure and closes its peers without touching a feature configuration value. */
    public void openAccordion(String featureId, Iterable<String> peers) {
        if (peers != null) {
            for (String peer : peers) {
                String key = cleanId(peer);
                if (key.length() > 0 && !key.equals(cleanId(featureId))) setDropdownOpen(key, false);
            }
        }
        setDropdownOpen(featureId, true);
    }

    /** Loads valid editor selections and ignores retired ui.feature.tree.* values without touching feature keys. */
    public void load(Properties source) {
        selections.clear();
        dropdowns.clear();
        if (source == null) return;
        for (String key : source.stringPropertyNames()) {
            if (key.startsWith(PREFIX + "editor.")) {
                String id = cleanId(key.substring((PREFIX + "editor.").length()));
                String selected = cleanValue(source.getProperty(key, ""));
                if (id.length() > 0 && selected.length() > 0 && selections.size() < MAX_ENTRIES) {
                    selections.put(id, selected);
                }
            } else if (key.startsWith(PREFIX + "dropdown.")) {
                String id = cleanId(key.substring((PREFIX + "dropdown.").length()));
                if (id.length() > 0 && dropdowns.size() < MAX_ENTRIES) {
                    dropdowns.put(id, Boolean.parseBoolean(source.getProperty(key, "false")));
                }
            }
        }
    }

    /** Saves only additive editor-selection keys, leaving every existing feature or retired presentation key alone. */
    public void save(Properties target) {
        if (target == null) return;
        for (Map.Entry<String, String> entry : selections.entrySet()) {
            target.setProperty(PREFIX + "editor." + entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Boolean> entry : dropdowns.entrySet()) {
            target.setProperty(PREFIX + "dropdown." + entry.getKey(), String.valueOf(Boolean.TRUE.equals(entry.getValue())));
        }
    }

    public Map<String, String> selectionsSnapshot() {
        return Collections.unmodifiableMap(new HashMap<String, String>(selections));
    }

    private static String cleanId(String raw) {
        if (raw == null) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < raw.length() && out.length() < MAX_VALUE_LENGTH; i++) {
            char c = raw.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' || c == '.') out.append(Character.toLowerCase(c));
        }
        return out.toString();
    }

    private static String cleanValue(String raw) {
        String value = cleanId(raw);
        return value.length() > MAX_VALUE_LENGTH ? value.substring(0, MAX_VALUE_LENGTH) : value;
    }
}
