package com.arena.pekaclient.chat;

/** Headless regression coverage for combining consecutive identical chat lines without Minecraft component classes. */
public final class ChatCompactMessagesPolicyTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        consecutiveDuplicatesShareOneGroup();
        formattingAndInterveningLinesBreakRuns();
        noLongerNewestGroupCannotBeReplaced();
        blankAndDividerLinesResetTheRun();
        System.out.println("chat compact-message policy: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void consecutiveDuplicatesShareOneGroup() {
        ChatCompactMessages compact = new ChatCompactMessages();
        ChatCompactMessages.Result one = compact.accept("§aFrom Alex: hello", true);
        ChatCompactMessages.Result two = compact.accept("§aFrom Alex: hello", true);
        compact.accept("§aFrom Alex: hello", true);
        compact.accept("§aFrom Alex: hello", true);
        ChatCompactMessages.Result five = compact.accept("§aFrom Alex: hello", true);
        check("first message starts a count-one group", !one.compacted && one.count == 1 && one.hasGroup());
        check("consecutive identical messages retain group ID", two.compacted && two.groupId == one.groupId
                && five.compacted && five.groupId == one.groupId);
        check("counter suffix starts at x2 and reflects combined count", "§7 (x2)".equals(
                ChatCompactMessages.counterFormattedText(two.count)) && "§7 (x5)".equals(
                ChatCompactMessages.counterFormattedText(five.count)));
    }

    private static void formattingAndInterveningLinesBreakRuns() {
        ChatCompactMessages compact = new ChatCompactMessages();
        ChatCompactMessages.Result first = compact.accept("§aHello", true);
        ChatCompactMessages.Result otherColour = compact.accept("§bHello", true);
        ChatCompactMessages.Result again = compact.accept("§aHello", false);
        check("formatting is part of message identity", !otherColour.compacted && otherColour.groupId != first.groupId);
        check("A-B-A creates a new group instead of deleting the first A",
                !again.compacted && again.groupId != first.groupId && again.groupId != otherColour.groupId);
    }

    private static void noLongerNewestGroupCannotBeReplaced() {
        ChatCompactMessages compact = new ChatCompactMessages();
        ChatCompactMessages.Result first = compact.accept("same", true);
        ChatCompactMessages.Result stale = compact.accept("same", false);
        check("stale cache group receives a fresh identity", !stale.compacted && stale.groupId != first.groupId);
        check("new group IDs remain positive and monotonic", first.groupId >= ChatCompactMessages.GROUP_ID_BASE
                && stale.groupId > first.groupId);
    }

    private static void blankAndDividerLinesResetTheRun() {
        ChatCompactMessages compact = new ChatCompactMessages();
        compact.accept("message", true);
        ChatCompactMessages.Result blank = compact.accept(" \u2003 ", true);
        boolean activeAfterBlank = compact.hasActiveMessage();
        ChatCompactMessages.Result divider = compact.accept("§m--------------------", true);
        ChatCompactMessages.Result afterDivider = compact.accept("message", true);
        check("blank lines do not enter compact groups", !blank.hasGroup() && !activeAfterBlank);
        check("decorative dividers are not compact candidates", !divider.hasGroup());
        check("message after divider starts a fresh group", !afterDivider.compacted && afterDivider.count == 1
                && afterDivider.hasGroup());
        check("ordinary punctuation is retained", ChatCompactMessages.isEligible("Wait... what?"));
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++;
        else { failed++; System.err.println("FAIL " + name); }
    }
}