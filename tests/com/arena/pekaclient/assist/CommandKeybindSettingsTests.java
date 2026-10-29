package com.arena.pekaclient.assist;

import java.util.Properties;

/** Pure persistence, migration, collision and direct-edge policy coverage for Shortcut Command Keybinds. */
public final class CommandKeybindSettingsTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        defaultsAndEditing();
        persistenceAndLegacyActiveMigration();
        directPhysicalInputPolicy();
        System.out.println("command keybind settings: passed " + passed + ", failed " + failed);
        if (failed > 0) throw new AssertionError("command keybind settings failures: " + failed);
    }

    private static void defaultsAndEditing() {
        CommandKeybindSettings.reset();
        check("master defaults off and list starts empty", !CommandKeybindSettings.enabled && CommandKeybindSettings.count() == 0);
        check("add creates an editable row", CommandKeybindSettings.add() && CommandKeybindSettings.count() == 1);
        CommandKeybindSettings.Entry fresh = CommandKeybindSettings.entryAt(0);
        check("fresh row has a clearable key and slash placeholder", fresh.keyCode() == 0 && "/".equals(fresh.command()));
        CommandKeybindSettings.setKeyCode(0, 21);
        CommandKeybindSettings.setCommand(0, "  warp  hub  ");
        CommandKeybindSettings.Entry edited = CommandKeybindSettings.entryAt(0);
        check("edit outer-trims slash commands while preserving inner spaces", edited.keyCode() == 21
                && "/warp  hub".equals(edited.command()) && CommandKeybindSettings.isExecutable(edited));
        CommandKeybindSettings.setKeyCode(0, 999);
        check("invalid key clears rather than remaining executable", CommandKeybindSettings.entryAt(0).keyCode() == 0
                && !CommandKeybindSettings.isExecutable(CommandKeybindSettings.entryAt(0)));
        CommandKeybindSettings.setKeyCode(0, 21);
        check("valid key range is bounded", CommandKeybindSettings.validKeyCode(1)
                && CommandKeybindSettings.validKeyCode(255) && !CommandKeybindSettings.validKeyCode(0)
                && !CommandKeybindSettings.validKeyCode(256));
    }

    private static void persistenceAndLegacyActiveMigration() {
        CommandKeybindSettings.reset();
        CommandKeybindSettings.enabled = true;
        CommandKeybindSettings.add();
        CommandKeybindSettings.setKeyCode(0, 21);
        CommandKeybindSettings.setCommand(0, "/warp alpha;beta, gamma");
        CommandKeybindSettings.add();
        CommandKeybindSettings.setKeyCode(1, 22);
        CommandKeybindSettings.setCommand(1, "/pets");
        Properties saved = new Properties();
        CommandKeybindSettings.save(saved);
        String records = saved.getProperty(CommandKeybindSettings.ENTRIES_KEY);
        check("new Shortcut config keys are written with no per-entry off state", "true".equals(saved.getProperty(CommandKeybindSettings.PREFIX))
                && records != null && records.indexOf(",true,") >= 0 && !saved.containsKey("assist.keybindCut.commandKeybinds"));
        Properties priorRelease = new Properties();
        priorRelease.setProperty("assist.keybindCut.commandKeybinds", "true");
        // The second .1.9 field was the old On/Off flag. False records must now migrate as active entries.
        priorRelease.setProperty("assist.keybindCut.commandKeybinds.entries", records.replaceFirst(",true,", ",false,"));
        CommandKeybindSettings.load(priorRelease);
        check("previous Keybind Cut command records migrate and treat old false records as active", CommandKeybindSettings.enabled
                && CommandKeybindSettings.count() == 2 && "/warp alpha;beta, gamma".equals(CommandKeybindSettings.entryAt(0).command())
                && CommandKeybindSettings.isExecutable(CommandKeybindSettings.entryAt(0)));
        CommandKeybindSettings.load(saved);
        check("commands with separators round trip", CommandKeybindSettings.enabled && CommandKeybindSettings.count() == 2
                && "/warp alpha;beta, gamma".equals(CommandKeybindSettings.entryAt(0).command())
                && CommandKeybindSettings.entryAt(0).keyCode() == 21 && CommandKeybindSettings.isExecutable(CommandKeybindSettings.entryAt(1)));

        Properties malformed = new Properties();
        malformed.setProperty(CommandKeybindSettings.PREFIX, "true");
        malformed.setProperty(CommandKeybindSettings.ENTRIES_KEY,
                records + ";broken;999,true,002f007800;20,true,nope");
        CommandKeybindSettings.load(malformed);
        check("malformed entries are dropped without losing valid saved siblings", CommandKeybindSettings.count() == 2
                && "/warp alpha;beta, gamma".equals(CommandKeybindSettings.entryAt(0).command()));
        CommandKeybindSettings.reset();
        check("reset clears persisted model and master", !CommandKeybindSettings.enabled && CommandKeybindSettings.count() == 0);
    }

    private static void directPhysicalInputPolicy() {
        CommandKeybindSettings.reset();
        CommandKeybindSettings.enabled = true;
        CommandKeybindSettings.add();
        CommandKeybindSettings.setKeyCode(0, 33);
        CommandKeybindSettings.setCommand(0, "/wardrobe");
        CommandKeybindSettings.add();
        CommandKeybindSettings.setKeyCode(1, 33);
        CommandKeybindSettings.setCommand(1, "/pets");
        check("duplicate command key is reported", CommandKeybindSettings.hasOtherEntryWithKey(0, 33)
                && CommandKeybindPolicy.collisionWith(33, new int[]{21, 33}));
        check("first creation-order command wins duplicate dispatch", "/wardrobe".equals(
                CommandKeybindSettings.firstExecutableForKey(33).command()));
        check("direct down edge with no screen sends", CommandKeybindPolicy.shouldSend(true, true, true,
                false, true, false, 1000L, 0L));
        check("held repeat never sends", !CommandKeybindPolicy.shouldSend(true, true, true,
                false, true, true, 1000L, 0L));
        check("chat GUI/editor open never sends", !CommandKeybindPolicy.shouldSend(true, true, true,
                true, true, false, 1000L, 0L));
        check("missing player or master never sends", !CommandKeybindPolicy.shouldSend(false, true, true,
                false, true, false, 1000L, 0L) && !CommandKeybindPolicy.shouldSend(true, true, false,
                false, true, false, 1000L, 0L));
        check("short anti-spam cooldown blocks then expires", !CommandKeybindPolicy.shouldSend(true, true, true,
                false, true, false, 1100L, 1000L) && CommandKeybindPolicy.shouldSend(true, true, true,
                false, true, false, 1300L, 1000L));
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++;
        else {
            failed++;
            System.err.println("FAIL " + name);
        }
    }
}
