package com.arena.pekaclient.feature.qol;

public final class ChatLengthPolicyTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("vanilla mode keeps the 100-character limit",
                ChatLengthPolicy.maximumLength(false) == 100);
        check("extended mode raises the limit to 256 characters",
                ChatLengthPolicy.maximumLength(true) == 256);
        check("vanilla maximum constant remains compatible", ChatLengthPolicy.VANILLA_MAX_LENGTH == 100);
        check("extended maximum constant matches modern chat length", ChatLengthPolicy.EXTENDED_MAX_LENGTH == 256);
        System.out.println("chat length policy: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++;
        else {
            failed++;
            System.out.println("FAIL " + name);
        }
    }
}
