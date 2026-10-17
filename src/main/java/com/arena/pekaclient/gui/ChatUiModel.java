package com.arena.pekaclient.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Minecraft-free ownership model for the optional chat features in the QoL tab. */
public final class ChatUiModel {

    /** One QoL-row contract shared by AppleHubScreen and headless tests. */
    public static final class Entry {
        private final String id;
        private final String label;
        private final boolean masterToggle;

        private Entry(String id, String label, boolean masterToggle) {
            this.id = id;
            this.label = label;
            this.masterToggle = masterToggle;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public boolean isMasterToggle() {
            return masterToggle;
        }
    }

    private ChatUiModel() {
    }

    /** Chat options are QoL-only; neither feature is a movable HUD element or duplicates HUD-editor settings. */
    public static List<Entry> buildQolModel() {
        List<Entry> entries = new ArrayList<Entry>();
        // The Hub deliberately exposes only feature on/off toggles. Legacy animation tuning keys remain
        // persistence-compatible, but customization is not duplicated into a normal GUI tab.
        entries.add(new Entry("chat-tabs", "Chat channel tabs", true));
        entries.add(new Entry("chat-animation-bridge", "Use Chatting animation bridge", true));
        return Collections.unmodifiableList(entries);
    }

    public static String label(String id) {
        for (Entry entry : buildQolModel()) {
            if (entry.getId().equals(id)) {
                return entry.getLabel();
            }
        }
        throw new IllegalArgumentException("Unknown chat UI control: " + id);
    }
}
