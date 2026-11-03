package com.arena.pekaclient.chat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minecraft-free text insertion used by the chat context menu. It preserves a cursor/selection insertion point for
 * copied messages and never produces more text than the active chat input limit, which keeps the real GuiTextField
 * deterministic.
 *
 * <p>The limit is a client-side input limit. It never raises a server-side limit: the server may still reject a message
 * longer than it accepts.</p>
 */
public final class ChatInputInsertion {

    /** Input limit with the Chat length option off. This is the vanilla GuiChat limit. */
    public static final int DEFAULT_LENGTH = 100;
    /** Input limit with the Chat length (256 characters) option on. */
    public static final int EXTENDED_LENGTH = 256;
    /** Largest limit this feature can use. Callers that do not pass a limit get this value. */
    public static final int MAX_LENGTH = EXTENDED_LENGTH;
    private static final Pattern PRIVATE_COMMAND = Pattern.compile("^\\s*/(?:msg|w|tell)(?:\\s+\\S+)?\\s*",
            Pattern.CASE_INSENSITIVE);

    public static final class Result {
        public final String text;
        public final int cursor;
        public final boolean inserted;
        /** True when the copied text was cut to fit the limit. The caller should say so rather than hide it. */
        public final boolean truncated;

        Result(String text, int cursor, boolean inserted) {
            this(text, cursor, inserted, false);
        }

        Result(String text, int cursor, boolean inserted, boolean truncated) {
            this.text = text;
            this.cursor = cursor;
            this.inserted = inserted;
            this.truncated = truncated;
        }
    }

    private ChatInputInsertion() {
    }

    /** The input limit for the given Chat length option state. */
    public static int limit(boolean extended) {
        return extended ? EXTENDED_LENGTH : DEFAULT_LENGTH;
    }

    /** True when the text can be sent or kept without exceeding the limit. Null counts as empty. */
    public static boolean withinLimit(String text, int limit) {
        return text == null || text.length() <= limit;
    }

    public static Result insert(String existing, int cursor, int selectionEnd, String source) {
        return insert(existing, cursor, selectionEnd, source, MAX_LENGTH);
    }

    /**
     * Inserts copied text at the selection. If the existing text is already over the limit, nothing is changed and the
     * result is not inserted. Pasted text that does not fit is cut at a whole code point and flagged as truncated.
     */
    public static Result insert(String existing, int cursor, int selectionEnd, String source, int limit) {
        int activeLimit = Math.max(0, limit);
        String before = existing == null ? "" : existing;
        if (before.length() > activeLimit) return new Result(before, clamp(cursor, 0, before.length()), false);
        int start = clamp(cursor, 0, before.length());
        int end = clamp(selectionEnd, 0, before.length());
        int low = Math.min(start, end);
        int high = Math.max(start, end);
        String clean = ChatText.cleanForChatInput(source == null ? "" : source);
        int room = Math.max(0, activeLimit - (before.length() - (high - low)));
        boolean truncated = clean.length() > room;
        if (truncated) clean = cutBeforeSurrogate(clean, room);
        String next = before.substring(0, low) + clean + before.substring(high);
        return new Result(next, low + clean.length(), clean.length() > 0, truncated);
    }

    /**
     * Inserts a stored-menu recipient command at the beginning of the real chat field and leaves focus/cursor work to
     * the caller. Existing ordinary text is retained after the prefix. Existing /msg, /w, or /tell recipient parts
     * are replaced while their typed message body is retained.
     */
    public static Result messagePlayer(String existing, String playerName) {
        return messagePlayer(existing, playerName, MAX_LENGTH);
    }

    /**
     * Like {@link #messagePlayer(String, String)} with an explicit limit. The typed body is never cut to make the
     * recipient fit: if the result would be over the limit, the field is left unchanged and nothing is inserted.
     */
    public static Result messagePlayer(String existing, String playerName, int limit) {
        int activeLimit = Math.max(0, limit);
        String before = existing == null ? "" : existing;
        String name = ChatPlayerName.extract("From " + (playerName == null ? "" : playerName) + ": preview");
        if (name.length() == 0) return new Result(before, before.length(), false);
        Matcher oldCommand = PRIVATE_COMMAND.matcher(before);
        String body = oldCommand.find() ? before.substring(oldCommand.end()) : before;
        String next = "/msg " + name + " " + body;
        if (next.length() > activeLimit) return new Result(before, before.length(), false);
        return new Result(next, next.length(), true);
    }

    /** Cuts to at most {@code max} chars without leaving a lone high surrogate at the end. */
    static String cutBeforeSurrogate(String value, int max) {
        if (max <= 0) return "";
        if (value.length() <= max) return value;
        int cut = max;
        if (Character.isHighSurrogate(value.charAt(cut - 1))) cut--;
        return value.substring(0, cut);
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : value > max ? max : value;
    }
}
