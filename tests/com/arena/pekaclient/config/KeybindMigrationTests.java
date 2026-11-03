package com.arena.pekaclient.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Pure options.txt parser checks for renamed Controls IDs; no Minecraft client is started. */
public final class KeybindMigrationTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        currentRecordWinsOverHistoricRecord();
        historicalRecordIsFoundWhenCurrentIsAbsent();
        malformedCurrentFallsBackToHistoric();
        lastValidOptionsLineWins();
        check("record key uses Minecraft's key_ prefix", "key_key.lunaria.open".equals(
                KeybindMigration.recordKey("key.lunaria.open")));
        check("already-prefixed record key stays unchanged", "key_key.lunaria.open".equals(
                KeybindMigration.recordKey("key_key.lunaria.open")));
        System.out.println("keybind migration: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void currentRecordWinsOverHistoricRecord() {
        List<String> lines = lines("key_key.pekaclient.open:54", "key_key.lunaria.open:42");
        check("explicit current Controls record wins", value(lines, "key.lunaria.open", "key.pekaclient.open") == 42);
    }

    private static void historicalRecordIsFoundWhenCurrentIsAbsent() {
        List<String> lines = lines("music:0.5", "key_key.lunaria.wardrobe:17");
        check("friend wardrobe record migrates to Shortcut", value(lines, "key.lunaria.wardrobeShortcut",
                "key.lunaria.wardrobe", "key.pekaclient.wardrobeShortcut") == 17);
    }

    private static void malformedCurrentFallsBackToHistoric() {
        List<String> lines = lines("key_key.lunaria.petsShortcut:not-a-key", "key_key.lunaria.pets:25");
        check("malformed current record does not erase a valid legacy choice", value(lines, "key.lunaria.petsShortcut",
                "key.lunaria.pets") == 25);
    }

    private static void lastValidOptionsLineWins() {
        List<String> lines = lines("key_key.pekaclient.leap:30", "key_key.pekaclient.leap:31");
        check("last valid options line wins", KeybindMigration.valueFor(lines, "key.pekaclient.leap") == 31);
    }

    private static Integer value(List<String> lines, String current, String... historic) {
        return KeybindMigration.resolveValue(lines, current, historic);
    }

    private static List<String> lines(String... values) {
        return new ArrayList<String>(Arrays.asList(values));
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++;
        else {
            failed++;
            System.out.println("FAIL " + name);
        }
    }
}
