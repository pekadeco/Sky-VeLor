package com.arena.pekaclient.chat;

/** Formatting-exact compact-message state coverage without a Minecraft client. */
public final class ChatCompactMessagesTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        equalFormattedMessagesCompact();
        formattingAndSeparatorsBreakRuns();
        counterUsesFixedHighLineAndGrayText();

        System.out.println();
        System.out.println("compact messages: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void equalFormattedMessagesCompact() {
        ChatCompactMessages compact = new ChatCompactMessages();
        ChatCompactMessages.Result first = compact.accept("§aFrom Alex: hello");
        ChatCompactMessages.Result second = compact.accept("§aFrom Alex: hello");
        ChatCompactMessages.Result third = compact.accept("§aFrom Alex: hello");
        check("first original remains unmodified", !first.compacted && first.count == 1);
        check("second equal formatted message becomes x2", second.compacted && second.count == 2);
        check("third equal formatted message replaces counter with x3", third.compacted && third.count == 3);
        check("active formatted original remains available for copy and msg", "§aFrom Alex: hello".equals(compact.activeFormatted()));
    }

    private static void formattingAndSeparatorsBreakRuns() {
        ChatCompactMessages compact = new ChatCompactMessages();
        compact.accept("§aHello");
        check("different legacy formatting never compacts", !compact.accept("§bHello").compacted);
        compact.accept("§aHello");
        check("blank message is never compacted", !compact.accept(" §7 ").compacted && !compact.hasActiveMessage());
        check("unicode whitespace is never compacted", !compact.accept("\u2003\u2002").compacted && !compact.hasActiveMessage());
        compact.accept("§aHello");
        check("separator is never compacted", !compact.accept("§m----------------------").compacted
                && !compact.hasActiveMessage());
        check("ordinary punctuation remains eligible", ChatCompactMessages.isEligible("Wait... what?"));
    }

    private static void counterUsesFixedHighLineAndGrayText() {
        check("counter id is a fixed positive high id", ChatCompactMessages.COUNTER_LINE_ID > 0x70000000);
        check("counter is legacy gray with the required suffix", "§7 (x4)".equals(ChatCompactMessages.counterFormattedText(4)));
        check("counter never emits x1", "§7 (x2)".equals(ChatCompactMessages.counterFormattedText(1)));
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++; else { failed++; System.out.println("FAIL " + name); }
    }
}
