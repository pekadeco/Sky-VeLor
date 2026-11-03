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
        sameMillisecondArrivalSequenceDefinesDisplayOrder();
        compactRefreshPreservesSequenceAndPosition();
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
        List<String> party = matching(history.arrivalOrder(), partyTab);
        check("150 mixed messages remain in All before selecting Party", history.size() == 150
                && all.matches(history.arrivalOrder().get(149).clean()));
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

    /** A and B can share a wall-clock millisecond; immutable arrival sequence still places A above B. */
    private static void sameMillisecondArrivalSequenceDefinesDisplayOrder() {
        ChatMessageHistory history = new ChatMessageHistory(8);
        ChatMessageHistory.Entry a = history.record(0, new ChatComponentText("From Alex: A"), "From Alex: A", 1, 77L);
        ChatMessageHistory.Entry b = history.record(0, new ChatComponentText("From Alex: B"), "From Alex: B", 1, 77L);
        List<ChatMessageHistory.Entry> display = history.arrivalOrder();
        List<ChatMessageHistory.Entry> debug = history.newestArrivalFirst();
        check("same-millisecond entries receive strictly increasing sequence", a.sequence() < b.sequence()
                && a.timestamp() == b.timestamp());
        check("All display source keeps A above B", display.size() == 2 && display.get(0) == a && display.get(1) == b);
        check("Reply tab display source keeps A above B", matching(history.arrivalOrder(), ChatTabs.reply()).size() == 2
                && "From Alex: A".equals(matching(history.arrivalOrder(), ChatTabs.reply()).get(0))
                && "From Alex: B".equals(matching(history.arrivalOrder(), ChatTabs.reply()).get(1)));
        check("diagnostic newest order is sequence reverse, not timestamp tie order", debug.get(0) == b && debug.get(1) == a);
    }

    /** A compact reprint changes the count/fade time only; it cannot become a second local/server echo or move later. */
    private static void compactRefreshPreservesSequenceAndPosition() {
        ChatMessageHistory history = new ChatMessageHistory(8);
        ChatMessageHistory.Entry compact = history.record(731, new ChatComponentText("§aFrom Alex: hello"),
                "From Alex: hello", 1, 10L);
        ChatMessageHistory.Entry following = history.record(0, new ChatComponentText("From Alex: after"),
                "From Alex: after", 1, 11L);
        ChatMessageHistory.Entry refreshed = history.record(731, new ChatComponentText("§aFrom Alex: hello"),
                "From Alex: hello", 4, 90L);
        List<ChatMessageHistory.Entry> display = history.arrivalOrder();
        List<ChatMessageHistory.Entry> newest = history.newestArrivalFirst();
        check("compact reprint does not append a duplicate history message", history.size() == 2 && refreshed == compact);
        check("compact reprint refreshes count/fade timestamp but retains sequence", compact.compactCount() == 4
                && compact.timestamp() == 90L && compact.sequence() < following.sequence());
        check("compacted line stays at its original display position", display.get(0) == compact && display.get(1) == following
                && newest.get(0) == following && newest.get(1) == compact);
        check("stored formatted component remains available for rebuild", compact.originalCopy() != null
                && compact.originalCopy().getFormattedText().contains("hello"));
    }

    private static void ringIsBoundedAndClearsExplicitly() {
        ChatMessageHistory history = new ChatMessageHistory(3);
        ChatMessageHistory.Entry first = null;
        for (int i = 0; i < 5; i++) {
            ChatMessageHistory.Entry next = history.record(0, new ChatComponentText("m" + i), "m" + i, 1, i);
            if (i == 0) first = next;
        }
        List<ChatMessageHistory.Entry> entries = history.arrivalOrder();
        check("history ring evicts only oldest messages at its cap", history.size() == 3
                && "m2".equals(entries.get(0).clean()) && "m4".equals(entries.get(2).clean()));
        history.clear();
        ChatMessageHistory.Entry afterClear = history.record(0, new ChatComponentText("after"), "after", 1, 5L);
        check("history clear keeps sequence monotonic for a later server arrival", afterClear.sequence() > first.sequence());
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
        if (ok) passed++; else { failed++; System.err.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("chat message history: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
