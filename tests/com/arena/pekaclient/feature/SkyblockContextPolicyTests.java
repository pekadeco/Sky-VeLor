package com.arena.pekaclient.feature;

public final class SkyblockContextPolicyTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("uppercase SkyBlock title", SkyblockContextPolicy.isSkyblockScoreboardTitle("SKYBLOCK"));
        check("colored title", SkyblockContextPolicy.isSkyblockScoreboardTitle("\u00a76\u00a7lSKYBLOCK"));
        check("mixed-case title", SkyblockContextPolicy.isSkyblockScoreboardTitle("Hypixel SkyBlock"));
        check("punctuated title", SkyblockContextPolicy.isSkyblockScoreboardTitle("Sky-Block"));
        check("does not accept another mode", !SkyblockContextPolicy.isSkyblockScoreboardTitle("BED WARS"));
        check("does not accept empty title", !SkyblockContextPolicy.isSkyblockScoreboardTitle("  "));
        check("does not accept null", !SkyblockContextPolicy.isSkyblockScoreboardTitle(null));
        check("does not accept unrelated title mentioning island", !SkyblockContextPolicy.isSkyblockScoreboardTitle("Private Island"));
        System.out.println("SkyblockContextPolicyTests: " + passed + " passed, " + failed + " failed");
        if (failed != 0) throw new AssertionError("SkyBlock context tests failed");
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++;
        else { failed++; System.err.println("FAIL: " + name); }
    }
}
