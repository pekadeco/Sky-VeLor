package com.arena.pekaclient.chat;

/**
 * Stateful, formatting-exact compact-message decision used before vanilla inserts a received chat line. Each eligible
 * run receives its own high chat-line id so deleting an updated compact group can never delete an unrelated older
 * group that happened to use a shared counter id.
 */
public final class ChatCompactMessages {

    /** High positive range reserved for compactable message groups; the id advances once for every new group. */
    public static final int GROUP_ID_BASE = 0x70000000;
    public static final int NO_GROUP_ID = 0;

    private String previousFormatted = "";
    private int count;
    private int activeGroupId = NO_GROUP_ID;
    private int nextGroupId = GROUP_ID_BASE;

    /** Result for one received server chat component. */
    public static final class Result {
        public final boolean compacted;
        public final int count;
        public final String formatted;
        /** The unique id of this compactable group, or {@link #NO_GROUP_ID} for blank/separator input. */
        public final int groupId;

        private Result(boolean compacted, int count, String formatted, int groupId) {
            this.compacted = compacted;
            this.count = count;
            this.formatted = formatted == null ? "" : formatted;
            this.groupId = groupId;
        }

        public boolean hasGroup() {
            return groupId != NO_GROUP_ID;
        }
    }

    /** Equivalent to {@link #accept(String, boolean)} when the prior group is still the newest cache entry. */
    public Result accept(String formatted) {
        return accept(formatted, true);
    }

    /**
     * Records a received formatted message. Equal means exactly equal legacy-formatted text, not merely equal clean
     * text, so different colours/styles never collapse into one message. A duplicate only compacts if its previous
     * group is still the last cache group; otherwise it starts a fresh group and leaves the older line untouched.
     */
    public Result accept(String formatted, boolean previousGroupIsLast) {
        String value = formatted == null ? "" : formatted;
        if (!isEligible(value)) {
            reset();
            return new Result(false, 0, value, NO_GROUP_ID);
        }
        if (value.equals(previousFormatted) && activeGroupId != NO_GROUP_ID && previousGroupIsLast) {
            count++;
            return new Result(true, count, value, activeGroupId);
        }
        previousFormatted = value;
        count = 1;
        activeGroupId = nextId();
        return new Result(false, count, value, activeGroupId);
    }

    public void reset() {
        previousFormatted = "";
        count = 0;
        activeGroupId = NO_GROUP_ID;
    }

    public boolean hasActiveMessage() {
        return activeGroupId != NO_GROUP_ID && count > 0 && previousFormatted.length() > 0;
    }

    public String activeFormatted() {
        return previousFormatted;
    }

    public int activeGroupId() {
        return activeGroupId;
    }

    /** Vanilla-compatible gray suffix appended as a sibling after the original component's final wrapped line. */
    public static String counterFormattedText(int count) {
        return "\u00a77 (x" + Math.max(2, count) + ")";
    }

    /** A blank or a cosmetic divider breaks a consecutive-message run instead of becoming a counter candidate. */
    public static boolean isEligible(String formatted) {
        String clean = ChatText.clean(formatted);
        return hasVisibleText(clean) && !isSeparator(clean);
    }

    private int nextId() {
        int id = nextGroupId;
        nextGroupId++;
        // Keep all ids positive/high even after an impractically long client session wraps the signed integer range.
        if (nextGroupId <= 0 || nextGroupId < GROUP_ID_BASE) {
            nextGroupId = GROUP_ID_BASE;
        }
        return id;
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
