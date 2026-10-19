package com.arena.pekaclient.chat;

import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;

/** Formatting-exact, unique-group compact-message coverage without starting a Minecraft client. */
public final class ChatCompactMessagesTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        fiveConsecutiveFormattedMessagesCompactIntoOneGroup();
        formattingSeparatorsAndInterruptedRunsBreakCompaction();
        uniqueHighGroupsAndGraySuffixContract();
        copiedClickableHoverableOriginalSurvivesSuffix();

        System.out.println();
        System.out.println("compact messages: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    /** Required x5 regression: every duplicate replaces the same current group, never allocates a counter line. */
    private static void fiveConsecutiveFormattedMessagesCompactIntoOneGroup() {
        ChatCompactMessages compact = new ChatCompactMessages();
        ChatCompactMessages.Result first = compact.accept("§aFrom Alex: hello", true);
        ChatCompactMessages.Result latest = first;
        for (int i = 2; i <= 5; i++) latest = compact.accept("§aFrom Alex: hello", true);
        check("first compactable message starts an original group", !first.compacted && first.count == 1 && first.hasGroup());
        check("x5 stays in the original unique group", latest.compacted && latest.count == 5
                && latest.groupId == first.groupId);
        check("active formatted original remains available for copy and msg", "§aFrom Alex: hello".equals(compact.activeFormatted()));
    }

    /** A-B-A must not delete A: only an immediately last group may be compacted. */
    private static void formattingSeparatorsAndInterruptedRunsBreakCompaction() {
        ChatCompactMessages compact = new ChatCompactMessages();
        ChatCompactMessages.Result a = compact.accept("§aHello", true);
        ChatCompactMessages.Result b = compact.accept("§bOther", true);
        ChatCompactMessages.Result aAgain = compact.accept("§aHello", false);
        check("A-B-A starts a fresh A group", !aAgain.compacted && aAgain.groupId != a.groupId
                && aAgain.groupId != b.groupId);

        compact.accept("§aHello", true);
        check("different legacy formatting never compacts", !compact.accept("§bHello", true).compacted);
        compact.accept("§aHello", true);
        check("blank message is never compacted", !compact.accept(" §7 ", true).compacted && !compact.hasActiveMessage());
        compact.accept("§aHello", true);
        check("unicode whitespace is never compacted", !compact.accept("\u2003\u2002", true).compacted && !compact.hasActiveMessage());
        compact.accept("§aHello", true);
        check("separator is never compacted", !compact.accept("§m----------------------", true).compacted
                && !compact.hasActiveMessage());
        check("ordinary punctuation remains eligible", ChatCompactMessages.isEligible("Wait... what?"));
    }

    private static void uniqueHighGroupsAndGraySuffixContract() {
        ChatCompactMessages compact = new ChatCompactMessages();
        ChatCompactMessages.Result first = compact.accept("one", true);
        ChatCompactMessages.Result second = compact.accept("two", true);
        check("new groups use high positive ids", first.groupId >= ChatCompactMessages.GROUP_ID_BASE
                && second.groupId > first.groupId);
        check("blank and separator inputs have no group id", compact.accept(" ", true).groupId
                == ChatCompactMessages.NO_GROUP_ID);
        check("suffix is legacy gray and begins at x2", "§7 (x4)".equals(ChatCompactMessages.counterFormattedText(4))
                && "§7 (x2)".equals(ChatCompactMessages.counterFormattedText(1)));
    }

    /** Mirrors the runtime replacement: clone the original rich component, then append the counter sibling. */
    private static void copiedClickableHoverableOriginalSurvivesSuffix() {
        ChatComponentText original = new ChatComponentText("From Alex: hello");
        original.setChatStyle(new ChatStyle()
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/msg Alex "))
                .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText("Alex"))));
        IChatComponent rendered = original.createCopy();
        rendered.appendSibling(new ChatComponentText(ChatCompactMessages.counterFormattedText(5)));
        ClickEvent click = rendered.getChatStyle().getChatClickEvent();
        HoverEvent hover = rendered.getChatStyle().getChatHoverEvent();
        check("click command survives rich component copy", click != null && "/msg Alex ".equals(click.getValue()));
        check("hover text survives rich component copy", hover != null && hover.getValue() != null
                && "Alex".equals(hover.getValue().getUnformattedTextForChat()));
        IChatComponent suffix = rendered.getSiblings().isEmpty() ? null : rendered.getSiblings().get(0);
        String suffixFormatted = suffix == null ? "" : suffix.getFormattedText();
        String suffixRaw = suffix == null ? "" : suffix.getUnformattedTextForChat();
        // Minecraft may prefix an inherited reset code when formatting a sibling; verify the actual gray suffix rather
        // than assuming that its serialized prefix is byte-for-byte identical to the standalone legacy string.
        check("gray suffix is appended after original component", rendered.getSiblings().size() == 1
                && (suffixFormatted.contains("§7") || suffixRaw.contains("§7"))
                && (suffixFormatted.endsWith("(x5)") || suffixRaw.endsWith("(x5)")));
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++; else { failed++; System.out.println("FAIL " + name); }
    }
}
