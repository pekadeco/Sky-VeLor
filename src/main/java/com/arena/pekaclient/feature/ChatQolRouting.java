package com.arena.pekaclient.feature;

/** Pure outgoing-chat routing: normal words use the selected channel; explicit commands always remain untouched. */
public final class ChatQolRouting {

    private ChatQolRouting() {
    }

    public static String applyPrefix(int tab, String typedText) {
        String text = typedText == null ? "" : typedText;
        if (text.length() == 0 || text.startsWith("/")) {
            return text;
        }
        if (tab == ChatFilterRules.PARTY) {
            return "/pc " + text;
        }
        if (tab == ChatFilterRules.REPLY) {
            return "/reply " + text;
        }
        return text;
    }

    public static String targetLabel(int tab) {
        return tab == ChatFilterRules.PARTY ? "Party" : tab == ChatFilterRules.REPLY ? "Reply" : "All";
    }
}
