package com.arena.pekaclient.feature;

/** Pure transition coverage for resetting chat tabs only after a real GuiChat close. */
public final class ChatTabClosePolicyTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("opening chat does not reset selection", !ChatTabClosePolicy.shouldResetToAll(false, true));
        check("chat staying open does not reset selection", !ChatTabClosePolicy.shouldResetToAll(true, true));
        check("other screens staying closed do not reset selection", !ChatTabClosePolicy.shouldResetToAll(false, false));
        check("Escape close transition resets selection", ChatTabClosePolicy.shouldResetToAll(true, false));
        check("send close transition resets selection", ChatTabClosePolicy.shouldResetToAll(true, false));
        check("command close transition resets selection", ChatTabClosePolicy.shouldResetToAll(true, false));
        System.out.println("chat tab close policy: passed " + passed + ", failed " + failed);
        if (failed != 0) throw new AssertionError(failed + " chat-tab close checks failed");
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++; else { failed++; System.err.println("FAIL " + name); }
    }
}
