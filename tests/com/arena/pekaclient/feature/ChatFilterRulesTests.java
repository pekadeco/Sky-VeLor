package com.arena.pekaclient.feature;

import java.util.ArrayList;
import java.util.List;

/** Headless proof for native Chat QoL classification, tab filtering, routing, clean copying and safe fallbacks. */
public final class ChatFilterRulesTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        classificationSamples();
        filteringAndRouting();
        editableRulesAndHistory();
        colorStripping();

        System.out.println("chat QoL classifier: passed " + passed + ", failed " + failed);
        if (failed != 0) throw new AssertionError(failed + " chat QoL tests failed");
    }

    /** At least fifteen representative player, party, reply, system and unknown lines. */
    private static void classificationSamples() {
        sample("§r§9Party §8> §b[MVP+] Alex§f: hello", ChatFilterRules.PARTY);
        sample("P > Alex: hello [COPY] (2)", ChatFilterRules.PARTY);
        sample("[PARTY] Alex: hello", ChatFilterRules.PARTY);
        sample("Alex joined the party.", ChatFilterRules.PARTY);
        sample("Alex left the party.", ChatFilterRules.PARTY);
        sample("A player invited you to join their party!", ChatFilterRules.PARTY);
        sample("§dFrom Alex: hello", ChatFilterRules.REPLY);
        sample("To Alex: hello", ChatFilterRules.REPLY);
        sample("[PM] From Alex: hello", ChatFilterRules.REPLY);
        sample("§7[MVP+] Alex: normal player text", ChatFilterRules.PLAYER);
        sample("Alex: plain player text", ChatFilterRules.PLAYER);
        sample("Some_One: words", ChatFilterRules.PLAYER);
        sample("Welcome to Hypixel SkyBlock!", ChatFilterRules.SYSTEM);
        sample("You are now in the Hub.", ChatFilterRules.SYSTEM);
        sample("Alex: party > appears only in player text", ChatFilterRules.PLAYER);
        sample("A line with no known prefix", ChatFilterRules.SYSTEM);
        sample("[SYSTEM] Scheduled restart", ChatFilterRules.SYSTEM);

        check("classification sample count is at least fifteen", passed >= 17);
        check("only All/Party/Reply are selectable tab constants",
                ChatFilterRules.ALL == 0 && ChatFilterRules.PARTY == 1 && ChatFilterRules.REPLY == 2);
        check("unknown uses All tab channel", ChatFilterRules.channel("unknown server text") == ChatFilterRules.ALL);
        check("system routes to Chat 2", !ChatFilterRules.belongsToChatOne(ChatFilterRules.SYSTEM));
        check("player routes to Chat 1", ChatFilterRules.belongsToChatOne(ChatFilterRules.PLAYER));
    }

    private static void filteringAndRouting() {
        check("All shows unknown", ChatFilterRules.visible(ChatFilterRules.ALL, "unknown server text"));
        check("Party shows party", ChatFilterRules.visible(ChatFilterRules.PARTY, "Party > Alex: hi"));
        check("Party hides reply", !ChatFilterRules.visible(ChatFilterRules.PARTY, "From Alex: hi"));
        check("Reply shows direct messages", ChatFilterRules.visible(ChatFilterRules.REPLY, "To Alex: hi"));
        check("Reply hides party", !ChatFilterRules.visible(ChatFilterRules.REPLY, "P > Alex: hi"));
        check("Party send prefix", "/pc hello".equals(ChatQolRouting.applyPrefix(ChatFilterRules.PARTY, "hello")));
        check("Reply sends /reply", "/reply hello".equals(ChatQolRouting.applyPrefix(ChatFilterRules.REPLY, "hello")));
        check("All does not prefix", "hello".equals(ChatQolRouting.applyPrefix(ChatFilterRules.ALL, "hello")));
        check("explicit command remains untouched", "/warp home".equals(ChatQolRouting.applyPrefix(ChatFilterRules.REPLY, "/warp home")));
        check("empty text remains untouched", "".equals(ChatQolRouting.applyPrefix(ChatFilterRules.PARTY, "")));
        check("Reply target label is clear", "Reply".equals(ChatQolRouting.targetLabel(ChatFilterRules.REPLY)));
    }

    private static void editableRulesAndHistory() {
        List<ChatPatternRules.Rule> rules = new ArrayList<ChatPatternRules.Rule>();
        rules.add(new ChatPatternRules.Rule(ChatPatternRules.CHAT_1, "^Trade: .*$"));
        rules.add(new ChatPatternRules.Rule(ChatPatternRules.CHAT_2, "^Server: .*$"));
        rules.add(new ChatPatternRules.Rule(ChatPatternRules.CHAT_1, "[")); // invalid syntax must fail closed
        ChatPatternRules compiled = new ChatPatternRules(rules);
        check("editable Chat 1 rule works", ChatFilterRules.classify("Trade: offer", compiled) == ChatFilterRules.PLAYER);
        check("editable Chat 2 rule works", ChatFilterRules.classify("Server: restart", compiled) == ChatFilterRules.SYSTEM);
        check("invalid editable rule is ignored", compiled.acceptedRules().size() == 2);
        check("pattern test validates legal expression", ChatPatternRules.isValid("^Player: .*$"));
        check("pattern test rejects invalid expression", !ChatPatternRules.isValid("["));

        ChatQolHistory history = new ChatQolHistory();
        history.add("P > Alex: one", ChatFilterRules.PARTY, null, 1L, ChatFilterRules.ALL);
        history.add("From Alex: two", ChatFilterRules.REPLY, null, 2L, ChatFilterRules.PARTY);
        history.add("Server: three", ChatFilterRules.SYSTEM, null, 3L, ChatFilterRules.REPLY);
        check("party unread marker is retained", history.unread(ChatFilterRules.PARTY));
        check("reply unread marker is retained", history.unread(ChatFilterRules.REPLY));
        history.markRead(ChatFilterRules.PARTY);
        check("selected tab clears only its unread marker", !history.unread(ChatFilterRules.PARTY)
                && history.unread(ChatFilterRules.REPLY));
        check("history keeps newest full message", "Server: three".equals(history.newest(0).clean));
    }

    private static void colorStripping() {
        check("format stripping removes colour codes", "hello".equals(ChatFilterRules.stripFormatting("§ahe§bllo")));
        check("format stripping preserves message lines", "one\ntwo".equals(ChatFilterRules.stripFormatting("§cone\n§dtwo")));
    }

    private static void sample(String text, int expected) {
        check("classify: " + text, ChatFilterRules.classify(text, null) == expected);
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.err.println("FAIL: " + name);
        }
    }
}
