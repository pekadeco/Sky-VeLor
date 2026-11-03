package com.arena.pekaclient.feature.qol;

public final class ChatCopyTextPolicyTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("plain mode returns unformatted message", "Hello world".equals(
                ChatCopyTextPolicy.selectText("Hello world", "§aHello world§r", false)));
        check("plain mode strips embedded Minecraft formatting", "Hello world".equals(
                ChatCopyTextPolicy.selectText("§aHello §bworld", "§aHello §bworld§r", false)));
        check("formatted mode preserves color codes", "§aHello §bworld".equals(
                ChatCopyTextPolicy.selectText("Hello world", "§aHello §bworld§r", true)));
        check("formatted mode strips trailing reset", "§aHello".equals(
                ChatCopyTextPolicy.selectText("Hello", "§aHello§r§r", true)));
        check("null messages safely become empty", "".equals(
                ChatCopyTextPolicy.selectText(null, null, false)));
        check("empty messages stay empty", "".equals(
                ChatCopyTextPolicy.selectText("   ", "§a   §r", false)));
        System.out.println("ChatCopyTextPolicyTests: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static void check(String name, boolean success) {
        if (success) passed++; else { failed++; System.out.println("FAIL " + name); }
    }
}
