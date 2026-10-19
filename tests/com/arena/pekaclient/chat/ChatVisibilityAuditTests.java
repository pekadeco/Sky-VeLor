package com.arena.pekaclient.chat;

import java.util.List;

/** Closed/open visibility policy regression coverage independent of Minecraft's renderer. */
public final class ChatVisibilityAuditTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        closedFilteringMatchesOpenFiltering();
        fadeAndLineCapacityHaveDistinctReasons();
        boundedHistoryKeepsLogicalCompactGroups();

        System.out.println();
        System.out.println("chat visibility audit: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    /** Required closed-HUD contract: remembered Party/custom selection hides only nonmatching rows, never history. */
    private static void closedFilteringMatchesOpenFiltering() {
        final int fadeSeconds = 10;
        for (int index = 0; index < 30; index++) {
            boolean matchesSelected = index % 2 == 0;
            ChatVisibilityAudit.Decision closed = ChatVisibilityAudit.decide(false, true, matchesSelected,
                    fadeSeconds * 20 - 1, fadeSeconds, index / 2, 30);
            ChatVisibilityAudit.Decision open = ChatVisibilityAudit.decide(true, true, matchesSelected,
                    0, fadeSeconds, index / 2, 30);
            check("closed selection matches open filter for mixed message " + index,
                    closed.visible == matchesSelected && open.visible == matchesSelected
                            && closed.reason == (matchesSelected ? ChatVisibilityAudit.Reason.CLOSED_ACTIVE_TAB
                            : ChatVisibilityAudit.Reason.CLOSED_TAB_FILTER)
                            && open.reason == (matchesSelected ? ChatVisibilityAudit.Reason.OPEN_ACTIVE_TAB
                            : ChatVisibilityAudit.Reason.OPEN_TAB_FILTER));
        }
    }

    private static void fadeAndLineCapacityHaveDistinctReasons() {
        ChatVisibilityAudit.Decision closedExpired = ChatVisibilityAudit.decide(false, true, true, 200, 10, 0, 30);
        ChatVisibilityAudit.Decision lineLimited = ChatVisibilityAudit.decide(false, true, true, 0, 10, 30, 30);
        check("closed matching message fades only at fade time", !closedExpired.visible
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
        history.clear();
        check("debug history has explicit disconnect-clear support", history.entries().isEmpty());
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++; else { failed++; System.out.println("FAIL " + name); }
    }
}
