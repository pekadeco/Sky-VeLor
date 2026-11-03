package com.arena.pekaclient.feature;

import java.util.Locale;

/** Message classification used by the Skytils-style chat tabs. */
public final class ChatFilterRules {

    public static final int ALL = 0;
    public static final int PARTY = 1;
    public static final int GUILD = 2;
    public static final int PRIVATE = 3;
    public static final int COOP = 4;

    private ChatFilterRules() {
    }

    public static boolean visible(int tab, String formattedText) {
        return tab == ALL || channel(formattedText) == tab;
    }

    /**
     * Classifies Hypixel-style prefixes while tolerating formatting changes and suffixes inserted by
     * chat mods (for example Atheria's COPY control). The rules are also useful on compatible servers.
     */
    public static int channel(String formattedText) {
        String text = stripFormatting(formattedText).trim().toLowerCase(Locale.ROOT);

        if (starts(text, "party >", "p >", "[party]", "[p]")) {
            return PARTY;
        }
        if (starts(text, "guild >", "g >", "[guild]", "[g]")) {
            return GUILD;
        }
        if (starts(text, "to ", "from ", "[pm]", "[private]")) {
            return PRIVATE;
        }
        if (starts(text, "co-op >", "coop >", "cc >", "[co-op]", "[coop]", "[cc]")) {
            return COOP;
        }

        // Party lifecycle notices belong in the Party tab as well as direct party conversation.
        if (text.contains("joined the party") || text.contains("left the party")
                || text.contains("removed from the party") || text.contains("disbanded the party")
                || text.contains("party was disbanded") || text.contains("party was transferred")
                || text.contains("invited you to join their party") || text.contains("to the party!")
                || (text.startsWith("kicked ") && text.contains("because they were offline"))) {
            return PARTY;
        }
        return ALL;
    }

    private static boolean starts(String text, String... prefixes) {
        for (String prefix : prefixes) {
            if (text.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    static String stripFormatting(String text) {
        if (text == null || text.isEmpty()) {
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
