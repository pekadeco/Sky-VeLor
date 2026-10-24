package com.arena.pekaclient.chat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minecraft-free text insertion used by the chat context menu. It preserves a cursor/selection insertion point for
 * copied messages and never produces more than 256 clean characters, which keeps the real GuiTextField deterministic.
 */
public final class ChatInputInsertion {

    public static final int MAX_LENGTH = 256;
    private static final Pattern PRIVATE_COMMAND = Pattern.compile("^\\s*/(?:msg|w|tell)(?:\\s+\\S+)?\\s*",
            Pattern.CASE_INSENSITIVE);

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
        String before = bounded(existing);
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

    /**
     * Inserts a stored-menu recipient command at the beginning of the real chat field and leaves focus/cursor work to
     * the caller. Existing ordinary text is retained after the prefix. Existing /msg, /w, or /tell recipient parts
     * are replaced while their typed message body is retained.
     */
    public static Result messagePlayer(String existing, String playerName) {
        String name = ChatPlayerName.extract("From " + (playerName == null ? "" : playerName) + ": example");
        if (name.length() == 0) return new Result(bounded(existing), bounded(existing).length(), false);
        String before = bounded(existing);
        Matcher oldCommand = PRIVATE_COMMAND.matcher(before);
        String body = oldCommand.find() ? before.substring(oldCommand.end()) : before;
        String next = "/msg " + name + " " + body;
        if (next.length() > MAX_LENGTH) next = next.substring(0, MAX_LENGTH);
        return new Result(next, next.length(), true);
    }

    private static String bounded(String source) {
        String value = source == null ? "" : source;
        return value.length() > MAX_LENGTH ? value.substring(0, MAX_LENGTH) : value;
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : value > max ? max : value;
    }
}
