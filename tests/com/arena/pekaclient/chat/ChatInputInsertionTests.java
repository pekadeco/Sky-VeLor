package com.arena.pekaclient.chat;

public final class ChatInputInsertionTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("strips formatting codes", "Sword".equals(ChatText.cleanForChatInput("§6Sword")));
        check("joins multi-line copied chat with spaces", "first second third".equals(
                ChatText.cleanForChatInput("first\nsecond\r\nthird")));
        check("standalone carriage return becomes a safe separator", "first second".equals(
                ChatText.cleanForChatInput("first\rsecond")));
        check("removes control characters and formatting pairs", "abcef".equals(
                ChatText.cleanForChatInput("abc\u0001§def\u007f")));
        check("removes an unmatched section sign", "abc".equals(ChatText.cleanForChatInput("abc§")));
        ChatInputInsertion.Result inserted = ChatInputInsertion.insert("say ", 4, 4, "hello\nworld§a", 100);
        check("inserted text has no illegal controls or formatting codes", "say hello world".equals(inserted.text));
        check("cursor follows sanitized inserted text", inserted.cursor == inserted.text.length());
        ChatInputInsertion.Result selected = ChatInputInsertion.insert("old text", 0, 8, "clean\nline", 100);
        check("selection is replaced by one chat-safe line", "clean line".equals(selected.text));
        System.out.println("chat input insertion: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++;
        else { failed++; System.out.println("FAIL " + name); }
    }
}
