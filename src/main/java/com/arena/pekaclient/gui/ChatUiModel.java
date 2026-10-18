package com.arena.pekaclient.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Minecraft-free ownership contract for the one compact Chat QoL group in the General hub. */
public final class ChatUiModel {

    public static final int MASTER = 0;
    public static final int TOGGLE = 1;
    public static final int EDITOR = 2;

    /** One QoL-row contract shared by AppleHubScreen and headless tests. */
    public static final class Entry {
        private final String id;
        private final String label;
        private final int kind;

        private Entry(String id, String label, int kind) {
            this.id = id;
            this.label = label;
            this.kind = kind;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public boolean isMasterToggle() {
            return kind == MASTER;
        }

        public boolean isToggle() {
            return kind == MASTER || kind == TOGGLE;
        }

        public boolean isEditorRoute() {
            return kind == EDITOR;
        }
    }

    private ChatUiModel() {
    }

    /** Exactly the chat-specific controls allowed in the Hub; fonts and scrolling have their own global routes. */
    public static List<Entry> buildQolModel() {
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(new Entry("chat-qol-master", "Chat QoL", MASTER));
        entries.add(new Entry("chat-tabs", "Chat Tabs", TOGGLE));
        entries.add(new Entry("smooth-messages", "Smooth Messages", TOGGLE));
        entries.add(new Entry("hover-highlight", "Hover Highlight", TOGGLE));
        // Context-menu trigger and animation options live in Chat Editor with the rest of interaction behavior.
        // Fonts and scroll behavior are global mod settings, intentionally outside this chat-specific contract.
        entries.add(new Entry("open-chat-editor", "Open Chat Editor", EDITOR));
        return Collections.unmodifiableList(entries);
    }

    public static String label(String id) {
        for (Entry entry : buildQolModel()) {
            if (entry.getId().equals(id)) return entry.getLabel();
        }
        throw new IllegalArgumentException("Unknown chat UI control: " + id);
    }
}
