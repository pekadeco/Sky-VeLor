package com.arena.pekaclient.chat;

import com.arena.pekaclient.feature.ChatTabs;
import net.minecraft.util.ChatComponentText;

import java.util.ArrayList;
import java.util.List;

/** Headless proof that channel tabs are backed by their own 500-message logical history, never vanilla's draw cache. */
public final class ChatMessageHistoryTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        keepsMixedPartyMessagesInArrivalOrder();
        compactRefreshUpdatesOneStoredEntry();
        ringIsBoundedAndClearsExplicitly();
        done();
    }

    private static void keepsMixedPartyMessagesInArrivalOrder() {
        ChatMessageHistory history = new ChatMessageHistory();
        for (int i = 1; i <= 150; i++) {
            // Exactly forty Party messages are interleaved through the first 120 arrivals; none is a tail-only case.
            boolean party = i <= 120 && i % 3 == 1;
            String text = party ? "Party > Member: party-" + ((i + 2) / 3)
                    : i % 2 == 0 ? "Guild > Member: guild-" + i : "All > Member: all-" + i;
            history.record(0, new ChatComponentText(text), text, 1, i * 1000L);
        }
        ChatTabs.Tab all = ChatTabs.all();
        ChatTabs.Tab partyTab = ChatTabs.party();
        List<String> party = matching(history.oldestFirst(), partyTab);
        check("150 mixed messages remain in All before selecting Party", history.size() == 150
                && all.matches(history.oldestFirst().get(149).clean()));
        check("opening Party can recover all 40 matching messages beyond vanilla 100 lines", party.size() == 40);
        boolean ordered = true;
        for (int i = 0; i < 40; i++) {
            String expected = "party-" + (i + 1);
            if (!party.get(i).endsWith(expected)) {
                ordered = false;
                break;
            }
        }
        check("Party history is ordered oldest to newest after tab filtering", ordered);
    }

    private static void compactRefreshUpdatesOneStoredEntry() {
        ChatMessageHistory history = new ChatMessageHistory(8);
        history.record(731, new ChatComponentText("§aFrom Alex: hello"), "From Alex: hello", 1, 10L);
        history.record(731, new ChatComponentText("§aFrom Alex: hello"), "From Alex: hello", 4, 90L);
        ChatMessageHistory.Entry entry = history.oldestFirst().get(0);
        check("compact replacement does not append a duplicate history message", history.size() == 1);
        check("compact replacement refreshes stored counter and timestamp", entry.compactCount() == 4
                && entry.timestamp() == 90L && "From Alex: hello".equals(entry.clean()));
        check("stored formatted component remains available for rebuild", entry.originalCopy() != null
                && entry.originalCopy().getFormattedText().contains("hello"));
    }

    private static void ringIsBoundedAndClearsExplicitly() {
        ChatMessageHistory history = new ChatMessageHistory(3);
        for (int i = 0; i < 5; i++) {
            history.record(0, new ChatComponentText("m" + i), "m" + i, 1, i);
        }
        List<ChatMessageHistory.Entry> entries = history.oldestFirst();
        check("history ring evicts only oldest messages at its cap", history.size() == 3
                && "m2".equals(entries.get(0).clean()) && "m4".equals(entries.get(2).clean()));
        history.clear();
        check("history clears only when explicitly requested", history.size() == 0);
    }

    private static List<String> matching(List<ChatMessageHistory.Entry> entries, ChatTabs.Tab tab) {
        List<String> result = new ArrayList<String>();
        for (ChatMessageHistory.Entry entry : entries) {
            if (tab.matches(entry.clean())) result.add(entry.clean());
        }
        return result;
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++;
        else { failed++; System.err.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("chat message history: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
