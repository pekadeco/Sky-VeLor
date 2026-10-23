package com.arena.pekaclient.gui;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Small, Minecraft-free persistence model for remembered editor selection only.
 *
 * <p>The retired tree namespace is accepted when old properties load but is intentionally never interpreted or
 * created. Feature settings remain in their existing owners; this class only remembers the selected editor pane.</p>
 */
public final class FeatureUiState {
    public static final String PREFIX = "ui.feature.";
    private static final int MAX_ENTRIES = 96;
    private static final int MAX_VALUE_LENGTH = 48;

    private final Map<String, String> selections = new HashMap<String, String>();

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

    /** Loads valid editor selections and ignores retired ui.feature.tree.* values without touching feature keys. */
    public void load(Properties source) {
        selections.clear();
        if (source == null) return;
        for (String key : source.stringPropertyNames()) {
            if (!key.startsWith(PREFIX + "editor.")) continue;
            String id = cleanId(key.substring((PREFIX + "editor.").length()));
            String selected = cleanValue(source.getProperty(key, ""));
            if (id.length() > 0 && selected.length() > 0 && selections.size() < MAX_ENTRIES) {
                selections.put(id, selected);
            }
        }
    }

    /** Saves only additive editor-selection keys, leaving every existing feature or retired presentation key alone. */
    public void save(Properties target) {
        if (target == null) return;
        for (Map.Entry<String, String> entry : selections.entrySet()) {
            target.setProperty(PREFIX + "editor." + entry.getKey(), entry.getValue());
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
