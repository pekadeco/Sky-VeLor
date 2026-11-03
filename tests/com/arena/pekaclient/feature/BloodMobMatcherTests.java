package com.arena.pekaclient.feature;

/** Guards against overly broad blood-mob name matching. */
public final class BloodMobMatcherTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("exact name", BloodMobMatcher.isBloodMob("Shadow Assassin"));
        check("case insensitive", BloodMobMatcher.isBloodMob("KING MIDAS"));
        check("formatting removed", BloodMobMatcher.isBloodMob("\u00a7cLost Adventurer"));
        check("level prefix", BloodMobMatcher.isBloodMob("[Lv100] Frozen Adventurer 12,345❤"));
        check("health suffix", BloodMobMatcher.isBloodMob("Diamond Guy 1,234❤"));
        check("valid suffix boundary", BloodMobMatcher.isBloodMob("Angry Archaeologist (Blood Room)"));
        reject("null", null);
        reject("empty", "");
        reject("unrelated mob", "Zombie");
        reject("name prefix", "The Shadow Assassin");
        reject("similar suffix", "Shadow Assassin Clone");
        reject("inside another word", "NotKing Midas");
        System.out.println();
        System.out.println("blood mob matcher: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++; else { failed++; System.out.println("FAIL " + name); }
    }

    private static void reject(String name, String value) {
        check(name, !BloodMobMatcher.isBloodMob(value));
    }
}
