package com.arena.pekaclient.chat;

/**
 * Per-menu holder for exactly one {@link ChatMenuState}. A fresh state is created when either mouse path opens the
 * menu and is retained until after the selected action has run.
 */
public final class CapturedChatMenu {
    private ChatMenuState stored;

    public void open(String groupId, String clean, String formatted) {
        stored = new ChatMenuState(groupId, clean, formatted);
    }

    public void clear() {
        stored = null;
    }

    public boolean hasMessage() {
        return stored != null && stored.hasCopyText();
    }

    public ChatMenuState state() {
        return stored;
    }
}
