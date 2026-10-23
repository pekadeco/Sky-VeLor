package com.arena.pekaclient.gui;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Small, Minecraft-free persistence model for the presentation state of the feature tree and settings editors.
 *
 * <p>Only navigation state belongs here: no gameplay setting is copied, renamed, or interpreted. Keeping this
 * separate from feature schemas makes opening or closing a settings branch safe across old configurations.</p>
 */
public final class FeatureUiState {
    public static final String PREFIX = "ui.feature.";
    private static final int MAX_ENTRIES = 96;
    private static final int MAX_VALUE_LENGTH = 48;

    private final Map<String, Boolean> expanded = new HashMap<String, Boolean>();
    private final Map<String, String> selections = new HashMap<String, String>();

    public boolean isExpanded(String id, boolean fallback) {
        Boolean value = expanded.get(cleanId(id));
        return value == null ? fallback : value.booleanValue();
    }

    public void setExpanded(String id, boolean open) {
        String key = cleanId(id);
        if (key.length() == 0) return;
        if (!expanded.containsKey(key) && expanded.size() >= MAX_ENTRIES) return;
        expanded.put(key, Boolean.valueOf(open));
    }

    public void toggleExpanded(String id, boolean fallback) {
        setExpanded(id, !isExpanded(id, fallback));
    }

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

    /** Loads only valid UI keys and deliberately ignores malformed/user-added values. */
    public void load(Properties source) {
        expanded.clear();
        selections.clear();
        if (source == null) return;
        for (String key : source.stringPropertyNames()) {
            if (!key.startsWith(PREFIX)) continue;
            String suffix = key.substring(PREFIX.length());
            String value = source.getProperty(key, "").trim();
            if (suffix.startsWith("tree.")) {
                String id = cleanId(suffix.substring("tree.".length()));
                if (id.length() > 0 && expanded.size() < MAX_ENTRIES
                        && ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value))) {
                    expanded.put(id, Boolean.valueOf(Boolean.parseBoolean(value)));
                }
            } else if (suffix.startsWith("editor.")) {
                String id = cleanId(suffix.substring("editor.".length()));
                String selected = cleanValue(value);
                if (id.length() > 0 && selected.length() > 0 && selections.size() < MAX_ENTRIES) {
                    selections.put(id, selected);
                }
            }
        }
    }

    /** Saves UI state using an additive namespace, leaving every existing feature key untouched. */
    public void save(Properties target) {
        if (target == null) return;
        for (Map.Entry<String, Boolean> entry : expanded.entrySet()) {
            target.setProperty(PREFIX + "tree." + entry.getKey(), Boolean.toString(entry.getValue().booleanValue()));
        }
        for (Map.Entry<String, String> entry : selections.entrySet()) {
            target.setProperty(PREFIX + "editor." + entry.getKey(), entry.getValue());
        }
    }

    public Map<String, Boolean> expandedSnapshot() {
        return Collections.unmodifiableMap(new HashMap<String, Boolean>(expanded));
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
                    || c == '-' || c == '_' || c == '.') {
                out.append(Character.toLowerCase(c));
            }
        }
        return out.toString();
    }

    private static String cleanValue(String raw) {
        String value = cleanId(raw);
        return value.length() > MAX_VALUE_LENGTH ? value.substring(0, MAX_VALUE_LENGTH) : value;
    }
}
