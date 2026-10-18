package com.arena.pekaclient.chat;

/** Mouse-up UI delegates its copy action to this verified chain; this tests fall-through and clean text. */
public final class ClipboardChainTests {
    private static int passed, failed;

    public static void main(String[] args) {
        firstFailureFallsThrough();
        readbackMismatchFailsClosed();
        emptyAndFormatAreClean();
        mouseUpMenuState();
        cursorInsertionCapsAt256();
        messagePlayerInsertion();
        done();
    }

    private static void firstFailureFallsThrough() {
        Memory good = new Memory("AWT");
        ClipboardChain.Backend bad = new ClipboardChain.Backend() {
            public String name() { return "GuiScreen"; }
            public boolean write(String t) { return false; }
            public String read() { return ""; }
        };
        ClipboardChain.Result r = ClipboardChain.copy("A §aformatted\r message", new ClipboardChain.Backend[]{bad, good});
        check("fallback success", r.success && "AWT".equals(r.backend));
        check("no formatting copied", "A formatted message".equals(r.text));
    }

    private static void readbackMismatchFailsClosed() {
        ClipboardChain.Backend bad = new ClipboardChain.Backend() {
            public String name() { return "LWJGL"; }
            public boolean write(String t) { return true; }
            public String read() { return "other"; }
        };
        ClipboardChain.Result r = ClipboardChain.copy("same", new ClipboardChain.Backend[]{bad});
        check("readback failure", !r.success && "unavailable".equals(r.backend));
    }

    private static void emptyAndFormatAreClean() {
        check("empty refuses", !ClipboardChain.copy("§a", new ClipboardChain.Backend[0]).success);
        check("format cleaner", "hello".equals(ChatText.clean("§chello")));
    }

    private static void mouseUpMenuState() {
        ChatContextMenu menu = new ChatContextMenu();
        menu.setPlayerName("Player_42");
        check("exact first label", "Copy to clipboard".equals(menu.labelAt(0)));
        check("exact second label", "Copy to chat box".equals(menu.labelAt(1)));
        check("third action has the exact fixed message-player label", "Message player".equals(menu.labelAt(2))
                && menu.optionCount() == 3);
        menu.press(0);
        check("press does not execute", menu.armed());
        check("release same executes", menu.release(0) == 0);
        menu.press(1);
        check("release elsewhere refuses", menu.release(0) < 0);
        menu.setPlayerName("");
        check("invalid stored name removes third action", menu.optionCount() == 2 && !menu.press(2));
    }

    private static void cursorInsertionCapsAt256() {
        ChatInputInsertion.Result middle = ChatInputInsertion.insert("abc", 1, 1, "§aXY");
        check("clean text inserts at cursor", "aXYbc".equals(middle.text) && middle.cursor == 3);
        ChatInputInsertion.Result selected = ChatInputInsertion.insert("abcdef", 2, 5, "ZZ");
        check("selection is replaced", "abZZf".equals(selected.text) && selected.cursor == 4);
        StringBuilder full = new StringBuilder();
        for (int i = 0; i < 256; i++) full.append('x');
        ChatInputInsertion.Result capped = ChatInputInsertion.insert(full.toString(), 256, 256, "more");
        check("chat insertion never exceeds 256", capped.text.length() == 256 && !capped.inserted);
    }

    private static void messagePlayerInsertion() {
        ChatInputInsertion.Result empty = ChatInputInsertion.messagePlayer("", "Alex_99");
        check("empty box receives trailing-space msg command", empty.inserted && "/msg Alex_99 ".equals(empty.text)
                && empty.cursor == empty.text.length());
        ChatInputInsertion.Result text = ChatInputInsertion.messagePlayer("hello there", "Alex_99");
        check("existing text is retained after msg prefix", text.inserted && "/msg Alex_99 hello there".equals(text.text)
                && text.cursor == text.text.length());
        ChatInputInsertion.Result oldMessage = ChatInputInsertion.messagePlayer("/msg OldName keep this", "Alex_99");
        check("old slash-msg target is replaced", "/msg Alex_99 keep this".equals(oldMessage.text));
        ChatInputInsertion.Result whisper = ChatInputInsertion.messagePlayer("/w OtherName keep this", "Alex_99");
        ChatInputInsertion.Result tell = ChatInputInsertion.messagePlayer("/tell OtherName keep this", "Alex_99");
        check("old whisper and tell targets are replaced", "/msg Alex_99 keep this".equals(whisper.text)
                && "/msg Alex_99 keep this".equals(tell.text));
    }

    private static final class Memory implements ClipboardChain.Backend {
        private String value = "";
        private final String name;
        Memory(String n) { name = n; }
        public String name() { return name; }
        public boolean write(String t) { value = t; return true; }
        public String read() { return value; }
    }

    private static void check(String n, boolean ok) {
        if (ok) passed++; else { failed++; System.out.println("FAIL " + n); }
    }

    private static void done() {
        System.out.println("clipboard chain: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
