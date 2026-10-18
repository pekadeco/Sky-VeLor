package com.arena.pekaclient.chat;

/** Small shared state machine: menu commands never fire until matching mouse release. */
public final class ChatContextMenu {
    public static final int COPY_TO_CLIPBOARD = 0;
    public static final int COPY_TO_CHAT_BOX = 1;
    public static final int MESSAGE_PLAYER = 2;
    public static final String COPY_TO_CLIPBOARD_LABEL = "Copy to clipboard";
    public static final String COPY_TO_CHAT_BOX_LABEL = "Copy to chat box";

    private static final String[] COPY_OPTIONS = {COPY_TO_CLIPBOARD_LABEL, COPY_TO_CHAT_BOX_LABEL};
    private int pressed = -1;
    private String playerName = "";

    /** Uses the captured message's sender. Invalid/system messages deliberately retain only the two copy actions. */
    public void setPlayerName(String value) {
        String label = ChatPlayerName.messageLabel(value);
        playerName = label.length() == 0 ? "" : ChatPlayerName.extract("From " + value + ": preview");
        if (playerName.length() == 0) playerName = "";
    }

    public String playerName() { return playerName; }
    public int optionCount() { return playerName.length() == 0 ? COPY_OPTIONS.length : COPY_OPTIONS.length + 1; }

    public String labelAt(int option) {
        if (option == COPY_TO_CLIPBOARD) return COPY_TO_CLIPBOARD_LABEL;
        if (option == COPY_TO_CHAT_BOX) return COPY_TO_CHAT_BOX_LABEL;
        return option == MESSAGE_PLAYER && playerName.length() > 0 ? ChatPlayerName.messageLabel(playerName) : "";
    }

    public boolean press(int option) {
        if (option < 0 || option >= optionCount()) return false;
        pressed = option;
        return true;
    }

    /** Returns chosen option only when release is over the same option, otherwise -1. */
    public int release(int option) {
        int was = pressed;
        pressed = -1;
        return was >= 0 && was == option && option < optionCount() ? was : -1;
    }

    public void clear() { pressed = -1; }
    public boolean armed() { return pressed >= 0; }
}
