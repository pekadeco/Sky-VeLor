package com.arena.pekaclient.dungeon;

/** Visible secrets parsing used by DungeonProgressTracker and DungeonSecretsHud. */
public final class DungeonSecretsTests {
    public static void main(String[] args) {
        DungeonSecrets secrets = new DungeonSecrets();
        check("Secrets: waiting for dungeon data", secrets.display());
        secrets.observe("Secrets Found: 45%");
        check("Secrets: 45%", secrets.display());
        secrets.reset();
        secrets.observe("Secrets: 3/8");
        check("Secrets: 3/8", secrets.display());
        secrets.reset();
        secrets.foundSecret();
        check("Secrets: 1 found", secrets.display());
        secrets.reset();
        check("Secrets: waiting for dungeon data", secrets.display());
    }
    private static void check(String expected, String actual) {
        if (!expected.equals(actual)) throw new AssertionError(expected + " != " + actual);
    }
}
