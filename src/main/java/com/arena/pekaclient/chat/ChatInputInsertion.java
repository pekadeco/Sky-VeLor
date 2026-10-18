package com.arena.pekaclient.chat;

/**
 * Minecraft-free text insertion used by Copy to chat box.  It preserves a cursor/selection insertion point and
 * never produces more than 256 clean characters, which keeps the real GuiTextField path deterministic.
 */
public final class ChatInputInsertion {

    public static final int MAX_LENGTH = 256;

    public static final class Result {
        public final String text;
        public final int cursor;
        public final boolean inserted;

        Result(String text, int cursor, boolean inserted) {
            this.text = text;
            this.cursor = cursor;
            this.inserted = inserted;
        }
    }

    private ChatInputInsertion() {
    }

    public static Result insert(String existing, int cursor, int selectionEnd, String source) {
        String before = existing == null ? "" : existing;
        if (before.length() > MAX_LENGTH) before = before.substring(0, MAX_LENGTH);
        int start = clamp(cursor, 0, before.length());
        int end = clamp(selectionEnd, 0, before.length());
        int low = Math.min(start, end);
        int high = Math.max(start, end);
        String clean = ChatText.clean(source == null ? "" : source);
        int room = Math.max(0, MAX_LENGTH - (before.length() - (high - low)));
        if (clean.length() > room) clean = clean.substring(0, room);
        String next = before.substring(0, low) + clean + before.substring(high);
        return new Result(next, low + clean.length(), clean.length() > 0);
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : value > max ? max : value;
    }
}
