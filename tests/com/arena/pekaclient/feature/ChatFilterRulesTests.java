package com.arena.pekaclient.feature;

/** Headless classification proof for the optional client-only chat tabs. */
public final class ChatFilterRulesTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        channel("\u00a7r\u00a79Party \u00a78> \u00a7b[MVP+] Alex\u00a7f: hello", ChatFilterRules.PARTY);
        channel("P > Alex: hello [COPY] (2)", ChatFilterRules.PARTY);
        channel("Alex joined the party.", ChatFilterRules.PARTY);
        channel("\u00a7r\u00a72Guild > Alex: hello", ChatFilterRules.GUILD);
        channel("G > Alex: hello [COPY]", ChatFilterRules.GUILD);
        channel("\u00a7dFrom Alex: hello", ChatFilterRules.PRIVATE);
        channel("To Alex: hello", ChatFilterRules.PRIVATE);
        channel("\u00a7r\u00a7bCo-op > Alex: hello", ChatFilterRules.COOP);
        channel("CC > Alex: hello [COPY]", ChatFilterRules.COOP);
        channel("Alex: party > is only text in a message", ChatFilterRules.ALL);

        check(ChatFilterRules.visible(ChatFilterRules.ALL, "anything"), "all shows everything");
        check(ChatFilterRules.visible(ChatFilterRules.PARTY, "Party > Alex: hi"), "party shows party");
        check(!ChatFilterRules.visible(ChatFilterRules.PARTY, "Guild > Alex: hi"), "party hides guild");
        check(ChatFilterRules.visible(ChatFilterRules.GUILD, "Guild > Alex: hi"), "guild shows guild");
        check(ChatFilterRules.visible(ChatFilterRules.PRIVATE, "From Alex: hi"), "pm shows private");
        check(ChatFilterRules.visible(ChatFilterRules.COOP, "Co-op > Alex: hi"), "coop shows coop");
        check(!ChatFilterRules.visible(ChatFilterRules.COOP, "Alex: hi"), "coop hides public");
        check("hello".equals(ChatFilterRules.stripFormatting("\u00a7ahe\u00a7bllo")), "format stripping");

        System.out.println("chat tabs: passed " + passed + ", failed " + failed);
        if (failed != 0) {
            throw new AssertionError(failed + " chat-tab tests failed");
        }
    }

    private static void channel(String text, int expected) {
        check(ChatFilterRules.channel(text) == expected, "channel: " + text);
    }

    private static void check(boolean condition, String name) {
        if (condition) {
            passed++;
        } else {
            failed++;
            System.err.println("FAIL: " + name);
        }
    }
}
