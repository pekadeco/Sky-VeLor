package com.arena.pekaclient.feature;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Small, defensive classifier for Chat QoL.
 *
 * The three tab values are intentionally the only selectable channels: All always keeps unknown lines visible,
 * Party is limited to party traffic, and Reply groups direct messages. Window routing has two additional internal
 * values: PLAYER belongs to Chat 1 and SYSTEM/unknown belongs to Chat 2.
 */
public final class ChatFilterRules {

    public static final int ALL = 0;
    public static final int PARTY = 1;
    public static final int REPLY = 2;
    public static final int PLAYER = 3;
    public static final int SYSTEM = 4;

    private static final Pattern PLAYER_LINE = Pattern.compile(
            "^(?:\\[[^\\]]{1,32}\\]\\s*)?[A-Za-z0-9_]{1,16}:\\s+.+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern LEADING_TAGS = Pattern.compile("^(?:\\[[^\\]]{1,32}\\]\\s*)+");

    private ChatFilterRules() {
    }

    /** All is fail-open. A non-All view contains only its exact known channel. */
    public static boolean visible(int tab, String formattedText) {
        return tab == ALL || channel(formattedText) == tab;
    }

    /** Returns only a selectable tab channel; normal player and system lines safely become All. */
    public static int channel(String formattedText) {
        int kind = classify(formattedText, null);
        return kind == PARTY || kind == REPLY ? kind : ALL;
    }

    /** Immutable one-pass classification result retained by the router and surfaced by /velor chatdebug. */
    public static final class Classification {
        public final int kind;
        public final String reason;

        private Classification(int kind, String reason) {
            this.kind = kind;
            this.reason = reason;
        }
    }

    /** Classifies an incoming line once for tab filtering, window routing, unread markers and debug output. */
    public static int classify(String formattedText, ChatPatternRules patterns) {
        return classifyDetailed(formattedText, patterns).kind;
    }

    /** Same decision as {@link #classify}, with a stable human-readable reason for debug history. */
    public static Classification classifyDetailed(String formattedText, ChatPatternRules patterns) {
        String clean = stripFormatting(formattedText).trim();
        String text = clean.toLowerCase(Locale.ROOT);
        // Servers commonly put a rank, channel tag or both before an actual Party/From/To prefix. Classify that
        // envelope first without weakening normal `Player: text` matching.
        String traffic = LEADING_TAGS.matcher(clean).replaceFirst("").trim();
        String trafficLower = traffic.toLowerCase(Locale.ROOT);
        if (starts(text, "party >", "p >", "[party]", "[p]")
                || starts(trafficLower, "party >", "p >")) {
            return new Classification(PARTY, "party prefix");
        }
        if (starts(text, "to ", "from ", "[pm]", "[private]")
                || starts(trafficLower, "to ", "from ")) {
            return new Classification(REPLY, "reply/private prefix");
        }
        if (isPartyNotice(text) || isPartyNotice(trafficLower)) {
            return new Classification(PARTY, "party system notice");
        }

        // User patterns are evaluated only after unambiguous Party/Reply prefixes. They make normal player/server
        // classification editable without allowing a broad expression to accidentally swallow direct messages.
        int override = patterns == null ? ChatPatternRules.NO_MATCH : patterns.classify(clean);
        if (override != ChatPatternRules.NO_MATCH) {
            return new Classification(override, "editable pattern → "
                    + (belongsToChatOne(override) ? "Chat 1" : "Chat 2"));
        }
        if (PLAYER_LINE.matcher(clean).matches() || PLAYER_LINE.matcher(traffic).matches()) {
            return new Classification(PLAYER, "player-message shape");
        }
        return new Classification(SYSTEM, "system/unknown fallback");
    }

    /** Convenience for old diagnostics; real routing should retain a {@link Classification} result instead. */
    public static String reason(String formattedText, ChatPatternRules patterns) {
        return classifyDetailed(formattedText, patterns).reason;
    }

    public static boolean belongsToChatOne(int kind) {
        return kind == PARTY || kind == REPLY || kind == PLAYER;
    }

    public static String displayName(int kind) {
        switch (kind) {
            case PARTY:
                return "Party";
            case REPLY:
                return "Reply";
            case PLAYER:
                return "Player";
            default:
                return "System";
        }
    }

    private static boolean isPartyNotice(String text) {
        return text.contains("joined the party") || text.contains("left the party")
                || text.contains("removed from the party") || text.contains("disbanded the party")
                || text.contains("party was disbanded") || text.contains("party was transferred")
                || text.contains("invited you to join their party") || text.contains("to the party!")
                || (text.startsWith("kicked ") && text.contains("because they were offline"));
    }

    private static boolean starts(String text, String... prefixes) {
        for (String prefix : prefixes) {
            if (text.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /** Removes legacy Minecraft formatting pairs while preserving line breaks for copy-to-clipboard. */
    public static String stripFormatting(String text) {
        if (text == null || text.length() == 0) {
            return "";
        }
        StringBuilder clean = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00a7' && i + 1 < text.length()) {
                i++;
            } else {
                clean.append(c);
            }
        }
        return clean.toString();
    }
}
