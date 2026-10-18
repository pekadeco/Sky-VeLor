package com.arena.pekaclient.chat;

/**
 * Immutable-at-open context-menu payload. The renderer may relayout or the pointer may leave the original message,
 * but a menu action must always operate on the exact full message group captured when the menu opened.
 */
public final class CapturedChatMenu {
    public static final class Message {
        public final String groupId;
        public final String clean;
        public final String formatted;

        private Message(String groupId, String clean, String formatted) {
            this.groupId = groupId == null ? "" : groupId;
            this.clean = ChatText.clean(clean == null ? "" : clean);
            this.formatted = formatted == null ? "" : formatted;
        }
    }

    private Message stored;

    public void open(String groupId, String clean, String formatted) {
        stored = new Message(groupId, clean, formatted);
    }

    public void clear() {
        stored = null;
    }

    public boolean hasMessage() {
        return stored != null && stored.clean.length() > 0;
    }

    public Message message() {
        return stored;
    }
}
