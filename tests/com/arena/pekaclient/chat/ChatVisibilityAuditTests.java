package com.arena.pekaclient.chat;

import java.util.List;

/** Closed/open visibility policy regression coverage independent of Minecraft's renderer. */
public final class ChatVisibilityAuditTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        thirtyMixedMessagesRemainVisibleWhileClosedBeforeFade();
        openOnlyTabFilteringAndClosedFadeHaveDistinctReasons();
        boundedHistoryKeepsLogicalCompactGroups();

        System.out.println();
        System.out.println("chat visibility audit: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    /** Required 30-message regression: active-tab mismatch cannot hide any closed non-compacted message before fade. */
    private static void thirtyMixedMessagesRemainVisibleWhileClosedBeforeFade() {
        final int fadeSeconds = 10;
        for (int index = 0; index < 30; index++) {
            // The boolean deliberately alternates as if Party/Reply/custom tabs would reject half the messages.
            boolean wouldMatchOpenTab = index % 2 == 0;
            ChatVisibilityAudit.Decision decision = ChatVisibilityAudit.decide(false, true, wouldMatchOpenTab,
                    fadeSeconds * 20 - 1, fadeSeconds, index, 30);
            check("closed mixed message " + index + " remains visible before fade", decision.visible
                    && decision.reason == ChatVisibilityAudit.Reason.CLOSED_ALL);
        }
    }

    private static void openOnlyTabFilteringAndClosedFadeHaveDistinctReasons() {
        ChatVisibilityAudit.Decision openFiltered = ChatVisibilityAudit.decide(true, true, false, 0, 10, 0, 30);
        ChatVisibilityAudit.Decision closedExpired = ChatVisibilityAudit.decide(false, true, false, 200, 10, 0, 30);
        ChatVisibilityAudit.Decision lineLimited = ChatVisibilityAudit.decide(false, true, true, 0, 10, 30, 30);
        check("active tab filters only open chat", !openFiltered.visible
                && openFiltered.reason == ChatVisibilityAudit.Reason.OPEN_TAB_FILTER);
        check("closed chat fades only at fade time", !closedExpired.visible
                && closedExpired.reason == ChatVisibilityAudit.Reason.FADE_EXPIRED);
        check("line capacity is reported separately", !lineLimited.visible
                && lineLimited.reason == ChatVisibilityAudit.Reason.LINE_LIMIT);
    }

    private static void boundedHistoryKeepsLogicalCompactGroups() {
        ChatVisibilityAudit.History history = new ChatVisibilityAudit.History();
        history.record(100, "same", 1);
        history.record(100, "same", 5);
        check("compact replacement updates its logical history entry", history.entries().size() == 1
                && history.entries().get(0).count == 5);
        for (int i = 0; i < 20; i++) history.record(200 + i, "message " + i, 1);
        List<ChatVisibilityAudit.Entry> entries = history.entries();
        check("debug history retains exactly the newest fifteen messages", entries.size() == ChatVisibilityAudit.HISTORY_LIMIT);
        check("debug history is newest first", "message 19".equals(entries.get(0).clean)
                && "message 5".equals(entries.get(entries.size() - 1).clean));
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++; else { failed++; System.out.println("FAIL " + name); }
    }
}
