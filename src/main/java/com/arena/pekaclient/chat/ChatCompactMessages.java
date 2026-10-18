package com.arena.pekaclient.chat;

/**
 * Stateful, formatting-exact compact-message decision used before vanilla inserts a received chat line. The state is
 * intentionally tiny and event-owned: rendering only reads the already-compacted vanilla cache and allocates no
 * message history of its own.
 */
public final class ChatCompactMessages {

    /** Reserved fixed high line id passed to GuiNewChat.printChatMessageWithOptionalDeletion. */
    public static final int COUNTER_LINE_ID = 0x7F564C52;

    private String previousFormatted = "";
    private int count;

    /** Result for one received server chat component. */
    public static final class Result {
        public final boolean compacted;
        public final int count;
        public final String formatted;

        private Result(boolean compacted, int count, String formatted) {
            this.compacted = compacted;
            this.count = count;
            this.formatted = formatted == null ? "" : formatted;
        }
    }

    /**
     * Records a received formatted message. Equal means exactly equal legacy-formatted text, not merely equal clean
     * text, so different colours/styles never collapse into one message.
     */
    public Result accept(String formatted) {
        String value = formatted == null ? "" : formatted;
        if (!isEligible(value)) {
            reset();
            return new Result(false, 0, value);
        }
        if (value.equals(previousFormatted)) {
            count++;
            return new Result(true, count, value);
        }
        previousFormatted = value;
        count = 1;
        return new Result(false, count, value);
    }

    public void reset() {
        previousFormatted = "";
        count = 0;
    }

    public boolean hasActiveMessage() {
        return count > 0 && previousFormatted.length() > 0;
    }

    public String activeFormatted() {
        return previousFormatted;
    }

    /** Vanilla-compatible gray legacy text for the fixed high-id counter line. */
    public static String counterFormattedText(int count) {
        return "\u00a77 (x" + Math.max(2, count) + ")";
    }

    /** A blank or a cosmetic divider breaks a consecutive-message run instead of becoming a counter candidate. */
    public static boolean isEligible(String formatted) {
        String clean = ChatText.clean(formatted);
        return hasVisibleText(clean) && !isSeparator(clean);
    }

    private static boolean hasVisibleText(String value) {
        if (value == null) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!Character.isWhitespace(c) && !Character.isSpaceChar(c)) return true;
        }
        return false;
    }

    /** Detects conventional purely decorative chat dividers while preserving normal punctuation-bearing messages. */
    public static boolean isSeparator(String clean) {
        if (clean == null) return false;
        int marks = 0;
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            if (Character.isWhitespace(c)) continue;
            if (!isSeparatorMark(c)) return false;
            marks++;
        }
        return marks >= 3;
    }

    private static boolean isSeparatorMark(char c) {
        return c == '-' || c == '_' || c == '=' || c == '*' || c == '~' || c == '.' || c == ':'
                || c == '\u2500' || c == '\u2501' || c == '\u2013' || c == '\u2014' || c == '\u2022';
    }
}
