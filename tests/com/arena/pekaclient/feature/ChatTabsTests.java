package com.arena.pekaclient.feature;

import java.util.ArrayList;
import java.util.List;

/** Headless proof for built-in/custom default-chat tab filtering. */
public final class ChatTabsTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        builtIns();
        customFiltersAndAll();
        invalidRegexFailsClosed();
        prefixes();
        done();
    }

    private static void builtIns() {
        List<ChatTabs.Tab> tabs = ChatTabs.defaults();
        check("All matches ordinary text", ChatTabs.appearsIn(ChatTabs.ALL, "Server result", tabs));
        check("Party matches Party prefix", ChatTabs.appearsIn(ChatTabs.PARTY, "Party > Alex: hello", tabs));
        check("Party rejects reply", !ChatTabs.appearsIn(ChatTabs.PARTY, "From Alex: hello", tabs));
        check("Reply matches inbound private", ChatTabs.appearsIn(ChatTabs.REPLY, "From Alex: hello", tabs));
        check("Reply matches outbound private", ChatTabs.appearsIn(ChatTabs.REPLY, "To Alex: hello", tabs));
    }

    private static void customFiltersAndAll() {
        List<ChatTabs.Tab> tabs = new ArrayList<ChatTabs.Tab>(ChatTabs.defaults());
        ChatTabs.Tab contains = ChatTabs.custom("custom-trade", "Trade");
        check("contains filter accepted", contains.setFilter(ChatTabs.CONTAINS, "selling"));
        tabs.add(contains);
        ChatTabs.Tab starts = ChatTabs.custom("custom-alert", "Alert");
        check("starts-with filter accepted", starts.setFilter(ChatTabs.STARTS_WITH, "ALERT:"));
        tabs.add(starts);
        check("custom contains matches case insensitively", ChatTabs.appearsIn("custom-trade", "SELLING sword", tabs));
        check("custom starts matches", ChatTabs.appearsIn("custom-alert", "alert: restart", tabs));
        check("custom non-match stays out of custom tab", !ChatTabs.appearsIn("custom-trade", "party > Alex", tabs));
        check("custom matched message remains in All", ChatTabs.appearsIn(ChatTabs.ALL, "SELLING sword", tabs));
    }

    private static void invalidRegexFailsClosed() {
        ChatTabs.Tab regex = ChatTabs.custom("custom-regex", "Regex");
        check("legal regex accepted", regex.setFilter(ChatTabs.REGEX, "^Guild: .*$"));
        check("legal regex matches", regex.matches("guild: hello"));
        check("invalid regex rejected", !regex.setFilter(ChatTabs.REGEX, "["));
        check("rejected regex does not replace the last safe rule", regex.matches("Guild: hello"));
        regex.loadFilter(ChatTabs.REGEX, "[");
        check("invalid config regex is nonmatching", !regex.matches("anything") && !regex.valid());
    }

    private static void prefixes() {
        ChatTabs.Tab party = ChatTabs.party();
        check("party prefix", "/pc hello".equals(ChatTabs.outgoingPrefix(party, "hello")));
        check("command stays command", "/warp home".equals(ChatTabs.outgoingPrefix(party, "/warp home")));
        ChatTabs.Tab custom = ChatTabs.custom("custom-send", "Send");
        custom.setSendPrefix("/msg Bob");
        check("custom prefix", "/msg Bob hello".equals(ChatTabs.outgoingPrefix(custom, "hello")));
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++; else { failed++; System.err.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("chat tabs: passed " + passed + ", failed " + failed);
        if (failed > 0) throw new AssertionError("chat tabs failures: " + failed);
    }
}
