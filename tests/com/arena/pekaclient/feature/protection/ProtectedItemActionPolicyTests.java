package com.arena.pekaclient.feature.protection;

public final class ProtectedItemActionPolicyTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("protected cursor drop outside is blocked", ProtectedItemActionPolicy.shouldBlockOutsideDrop(-999, true, false, false, true));
        check("normal cursor stack outside is allowed", !ProtectedItemActionPolicy.shouldBlockOutsideDrop(-999, false, false, false, true));
        check("unavailable storage fails closed for held stack", ProtectedItemActionPolicy.shouldBlockOutsideDrop(-999, false, false, true, true));
        check("unavailable storage without a stack does not invent a drop", !ProtectedItemActionPolicy.shouldBlockOutsideDrop(-999, false, false, true, false));
        check("other slot is not an outside drop", !ProtectedItemActionPolicy.shouldBlockOutsideDrop(5, true, false, false, true));
        check("protected packet stack is blocked even if local cursor already cleared", ProtectedItemActionPolicy.shouldBlockOutsideDrop(-999, false, true, false, true));
        check("unprotected packet stack does not count as a protected drop", !ProtectedItemActionPolicy.shouldBlockOutsideDrop(-999, false, false, false, true));
        check("protected explicit throw is blocked", ProtectedItemActionPolicy.shouldBlockThrowAction(4, true, false));
        check("unavailable protection storage blocks explicit throw", ProtectedItemActionPolicy.shouldBlockThrowAction(4, false, true));
        check("ordinary click is not treated as a throw", !ProtectedItemActionPolicy.shouldBlockThrowAction(0, true, false));
        System.out.println("ProtectedItemActionPolicyTests: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void check(String name, boolean condition) {
        if (condition) passed++;
        else { failed++; System.out.println("FAIL: " + name); }
    }
}
