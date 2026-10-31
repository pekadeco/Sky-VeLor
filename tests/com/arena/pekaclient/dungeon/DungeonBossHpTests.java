package com.arena.pekaclient.dungeon;

public final class DungeonBossHpTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        boss("Maxor");
        boss("Storm");
        boss("Goldor");
        boss("Necron");
        boss("Power Dragon");
        boss("Apex Dragon");
        boss("Soul Dragon");
        boss("Ice Dragon");
        boss("Flame Dragon");

        entry("styled M7 plate",
                "\u00A7c\u00A7l[BOSS] Storm \u00A7a400M/400M\u2764",
                "Storm", "400M", "400M");
        entry("commas and decimal suffix",
                "\u00A74Necron \u00A7e1,250.5M / 2B \u2665",
                "Necron", "1,250.5M", "2B");
        entry("spaces around suffix",
                "Goldor 750 M / 1 B\u2764",
                "Goldor", "750M", "1B");
        check("ordinary nameplate", DungeonBossParser.parseLabel("[BOSS] Maxor", 1, 4D) != null);
        check("word boundary", DungeonBossParser.parseLabel("Stormy Player 20/20\u2764", 1, 4D) == null);
        check("Wither King excluded", DungeonBossParser.parseLabel("Wither King 5B/5B\u2764", 1, 4D) == null);
        check("generic Ender Dragon excluded", DungeonBossParser.parseLabel("Ender Dragon 1B/1B\u2764", 1, 4D) == null);
        check("unrelated dragon excluded", DungeonBossParser.parseLabel("Young Dragon 1B/1B\u2764", 1, 4D) == null);
        currentOnly("Fakepixel Apex sidebar", "Apex Dragon 864M\u2764",
                "Apex Dragon", "864M", "900M");
        currentOnly("Fakepixel Soul decimal sidebar", "Soul Dragon 286.5M\u2764",
                "Soul Dragon", "286.5M", "900M");
        currentOnly("Fakepixel flattened sidebar",
                "Dungeon Apex Dragon 864M\u2764 Soul Dragon 286.5M\u2764 Power Dragon 900M\u2764",
                "Power Dragon", "900M", "900M");

        eq("entity fallback whole", "400M", DungeonBossParser.compactHealth(400000000D));
        eq("entity fallback decimal", "1.3M", DungeonBossParser.compactHealth(1250000D));
        eq("entity fallback zero", "0", DungeonBossParser.compactHealth(0D));
        check("scaled Wither 250 is rejected", !DungeonBossHpHud.isUsableEntityHealth(250D));
        check("scaled Dragon 200 is rejected as direct HP", !DungeonBossHpHud.isUsableEntityHealth(200D));
        check("real numeric entity HP is accepted", DungeonBossHpHud.isUsableEntityHealth(100000000D));
        eq("scaled dragon full", "1B", DungeonBossParser.compactHealth(
                DungeonBossHpHud.scaledDragonHealth(200D, 200D)));
        eq("scaled dragon half", "500M", DungeonBossParser.compactHealth(
                DungeonBossHpHud.scaledDragonHealth(100D, 200D)));
        eq("scaled dragon clamps", "1B", DungeonBossParser.compactHealth(
                DungeonBossHpHud.scaledDragonHealth(250D, 200D)));
        check("scaled dragon invalid rejected", DungeonBossHpHud.scaledDragonHealth(20D, 0D) < 0D);
        check("hidden without boss", !DungeonBossHpHud.shouldDraw(false, 0));
        check("shown after detection", DungeonBossHpHud.shouldDraw(false, 1));
        check("preview shown in editor", DungeonBossHpHud.shouldDraw(true, 0));
        eq("styled compact panel", "\u00A76\u226A \u00A7cStorm \u00A7a400M/400M\u00A7c\u2764 \u00A76\u226B",
                DungeonBossHpHud.styledRow(new DungeonBossParser.Entry(
                        "Storm", "400M", "400M", 1, 4D)));

        System.out.println("Dungeon boss HP tests: " + passed + " passed, " + failed + " failed");
        if (failed != 0) {
            System.exit(1);
        }
    }

    private static void boss(String name) {
        entry(name + " numeric HP", name + " 123M/456M\u2764", name, "123M", "456M");
        entry(name + " boss bar", name + " - 100M/100M\u2764", name, "100M", "100M");
    }

    private static void currentOnly(String label, String input, String boss, String hp, String max) {
        DungeonBossParser.Entry entry = DungeonBossParser.parseCurrentHealthLabel(
                input, boss, max, 7, 9D);
        check(label + " parsed", entry != null);
        if (entry != null) {
            eq(label + " boss", boss, entry.bossName);
            eq(label + " hp", hp, entry.currentHealth);
            eq(label + " max", max, entry.maxHealth);
        }
    }

    private static void entry(String label, String input, String boss, String hp, String max) {
        DungeonBossParser.Entry entry = DungeonBossParser.parseLabel(input, 7, 9D);
        check(label + " parsed", entry != null);
        if (entry != null) {
            eq(label + " boss", boss, entry.bossName);
            eq(label + " hp", hp, entry.currentHealth);
            eq(label + " max", max, entry.maxHealth);
        }
    }

    private static void eq(String label, Object expected, Object actual) {
        check(label + " expected=" + expected + " actual=" + actual,
                expected == null ? actual == null : expected.equals(actual));
    }

    private static void check(String label, boolean ok) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.err.println("FAIL: " + label);
        }
    }
}
