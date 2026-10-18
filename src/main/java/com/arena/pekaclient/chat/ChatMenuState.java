package com.arena.pekaclient.chat;

/**
 * One immutable-at-open chat-menu payload plus its matching mouse press/release state.
 *
 * The renderer can relayout and the pointer can leave the original row after opening, but every menu action must use
 * this exact group, clean text, and parsed recipient until the action has completed and the owner discards the state.
 */
public final class ChatMenuState {
    public final String groupId;
    public final String clean;
    public final String formatted;
    public final String playerName;

    private final ChatContextMenu actions = new ChatContextMenu();

    public ChatMenuState(String groupId, String clean, String formatted) {
        this.groupId = groupId == null ? "" : groupId;
        this.formatted = formatted == null ? "" : formatted;
        String primary = ChatText.clean(clean == null ? "" : clean);
        String fallback = ChatText.clean(this.formatted);
        // Some composite vanilla components expose an empty unformatted root while retaining visible formatted text.
        this.clean = hasText(primary) ? primary : fallback;
        String parsed = ChatPlayerName.extract(this.clean);
        this.playerName = parsed.length() > 0 ? parsed : ChatPlayerName.extract(fallback);
        actions.setPlayerName(this.playerName);
    }

    public boolean hasCopyText() {
        return hasText(clean);
    }

    public int optionCount() {
        return actions.optionCount();
    }

    public String labelAt(int option) {
        return actions.labelAt(option);
    }

    /** Called by the same menu mouse-down path used in game. */
    public boolean press(int option) {
        return actions.press(option);
    }

    /** Called by the same menu mouse-up path used in game. */
    public int release(int option) {
        return actions.release(option);
    }

    public boolean armed() {
        return actions.armed();
    }

    /** Shared production/test insertion path for the two actions that edit the real chat field. */
    public ChatInputInsertion.Result insertInto(String existing, int cursor, int selectionEnd, int action) {
        if (action == ChatContextMenu.MESSAGE_PLAYER) return ChatInputInsertion.messagePlayer(existing, playerName);
        if (action == ChatContextMenu.COPY_TO_CHAT_BOX) return ChatInputInsertion.insert(existing, cursor, selectionEnd, clean);
        return ChatInputInsertion.insert(existing, cursor, selectionEnd, "");
    }

    public String describe() {
        String text = clean.length() > 42 ? clean.substring(0, 39) + "..." : clean;
        return "open group=#" + groupId + " clean=" + (hasCopyText() ? "[" + text + "]" : "empty")
                + " player=" + (playerName.length() == 0 ? "none" : playerName)
                + " armed=" + (armed() ? "yes" : "no");
    }

    private static boolean hasText(String value) {
        if (value == null) return false;
        for (int i = 0; i < value.length(); i++) if (!Character.isWhitespace(value.charAt(i))) return true;
        return false;
    }
}
