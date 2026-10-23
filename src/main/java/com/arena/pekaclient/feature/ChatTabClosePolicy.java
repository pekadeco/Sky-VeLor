package com.arena.pekaclient.feature;

/** Pure lifecycle rule: changing the selected tab is allowed only on a real GuiChat open-to-closed transition. */
public final class ChatTabClosePolicy {
    private ChatTabClosePolicy() {
    }

    public static boolean shouldResetToAll(boolean wasGuiChatOpen, boolean isGuiChatOpen) {
        return wasGuiChatOpen && !isGuiChatOpen;
    }
}
