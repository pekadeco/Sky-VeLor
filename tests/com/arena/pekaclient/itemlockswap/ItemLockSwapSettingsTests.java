package com.arena.pekaclient.itemlockswap;

import java.util.Properties;

/** Standalone regression tests for persisted position state, pair constraints and fresh defaults. */
public final class ItemLockSwapSettingsTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        freshInstallDefaults();
        lockAndPairRules();
        persistenceAndMalformedInput();
        keybindPolicy();
        System.out.println("item lock/swap settings: passed " + passed + ", failed " + failed);
        if (failed > 0) throw new AssertionError("item lock/swap settings failures: " + failed);
    }

    private static void freshInstallDefaults() {
        ItemLockSwapSettings.load(new Properties());
        check("master defaults on", ItemLockSwapSettings.enabled);
        check("L key defaults", ItemLockSwapSettings.keyCode == ItemLockSwapSettings.DEFAULT_KEY_CODE);
        check("2 second cooldown defaults", ItemLockSwapSettings.cooldownMillis() == 2000);
        check("lock icons and hotbar icons default on", ItemLockSwapSettings.showLockIcons
                && ItemLockSwapSettings.showIconsOnHotbar);
        check("swap lines default always, gold, opaque and 2px", ItemLockSwapSettings.swapLineMode
                == ItemLockSwapSettings.SWAP_LINES_ALWAYS && ItemLockSwapSettings.swapLineColor == 0
                && ItemLockSwapSettings.swapLineOpacity == 100
                && ItemLockSwapSettings.swapLineWidth == ItemLockSwapSettings.DEFAULT_LINE_WIDTH);
        check("empty positions start unlocked", !ItemLockSwapSettings.isLocked(0) && !ItemLockSwapSettings.isLocked(39));
    }

    private static void lockAndPairRules() {
        ItemLockSwapSettings.load(new Properties());
        check("empty position locks", ItemLockSwapSettings.toggleLock(0) == ItemLockSwapSettings.LockResult.LOCKED
                && ItemLockSwapSettings.isLocked(0));
        check("locked position cannot pair", ItemLockSwapSettings.addPair(0, 1)
                == ItemLockSwapSettings.PairResult.LOCKED);
        check("unlock works", ItemLockSwapSettings.toggleLock(0) == ItemLockSwapSettings.LockResult.UNLOCKED);
        check("hotbar to armor pair is allowed", ItemLockSwapSettings.addPair(0, 39)
                == ItemLockSwapSettings.PairResult.ADDED);
        check("pair protects both endpoints", ItemLockSwapSettings.isProtected(0)
                && ItemLockSwapSettings.isProtected(39) && ItemLockSwapSettings.partnerOf(0) == 39);
        check("paired position cannot become lock", ItemLockSwapSettings.toggleLock(0)
                == ItemLockSwapSettings.LockResult.PAIRED);
        check("pair number is stable", ItemLockSwapSettings.pairNumber(0) == 1
                && ItemLockSwapSettings.pairNumber(39) == 1);
        check("remove existing pair", ItemLockSwapSettings.removePairContaining(39)
                && !ItemLockSwapSettings.isPaired(0));
        check("self pair is invalid", ItemLockSwapSettings.addPair(2, 2) == ItemLockSwapSettings.PairResult.INVALID);

        for (int i = 0; i < ItemLockSwapSettings.MAX_PAIRS; i++) {
            check("pair " + i + " can be made", ItemLockSwapSettings.addPair(i * 2, i * 2 + 1)
                    == ItemLockSwapSettings.PairResult.ADDED);
        }
        check("ninth pair is rejected", ItemLockSwapSettings.addPair(20, 21) == ItemLockSwapSettings.PairResult.FULL);
        ItemLockSwapSettings.clearPairs();
        check("clear pairs removes all pairs", ItemLockSwapSettings.pairCount() == 0);
    }

    private static void persistenceAndMalformedInput() {
        ItemLockSwapSettings.load(new Properties());
        ItemLockSwapSettings.enabled = false;
        ItemLockSwapSettings.keyCode = 21;
        ItemLockSwapSettings.cooldownTenths = 35;
        ItemLockSwapSettings.iconColor = 4;
        ItemLockSwapSettings.iconOpacity = 65;
        ItemLockSwapSettings.swapLineMode = ItemLockSwapSettings.SWAP_LINES_ON_HOVER;
        ItemLockSwapSettings.swapLineColor = 2;
        ItemLockSwapSettings.swapLineOpacity = 55;
        ItemLockSwapSettings.swapLineWidth = 3;
        ItemLockSwapSettings.toggleLock(8);
        ItemLockSwapSettings.toggleLock(36);
        ItemLockSwapSettings.addPair(9, 39);
        Properties p = new Properties();
        ItemLockSwapSettings.save(p);
        ItemLockSwapSettings.load(p);
        check("settings round trip", !ItemLockSwapSettings.enabled && ItemLockSwapSettings.keyCode == 21
                && ItemLockSwapSettings.cooldownMillis() == 3500 && ItemLockSwapSettings.iconColor == 4
                && ItemLockSwapSettings.iconOpacity == 65 && ItemLockSwapSettings.swapLineMode
                == ItemLockSwapSettings.SWAP_LINES_ON_HOVER && ItemLockSwapSettings.swapLineColor == 2
                && ItemLockSwapSettings.swapLineOpacity == 55 && ItemLockSwapSettings.swapLineWidth == 3);
        check("locks round trip", ItemLockSwapSettings.isLocked(8) && ItemLockSwapSettings.isLocked(36));
        check("pair round trip", ItemLockSwapSettings.partnerOf(9) == 39 && ItemLockSwapSettings.pairCount() == 1);

        Properties malformed = new Properties();
        malformed.setProperty(ItemLockSwapSettings.PREFIX + "locks", "-1,0,nope,40,35");
        malformed.setProperty(ItemLockSwapSettings.PREFIX + "pairs", "0-1;bad;2-2;37-x;8-39");
        malformed.setProperty(ItemLockSwapSettings.PREFIX + "cooldownTenths", "999");
        ItemLockSwapSettings.load(malformed);
        check("bad slot data fails closed", ItemLockSwapSettings.isLocked(0) && ItemLockSwapSettings.isLocked(35)
                && !ItemLockSwapSettings.isLocked(39));
        check("legacy pairs survive without new line keys", ItemLockSwapSettings.pairCount() == 1
                && ItemLockSwapSettings.partnerOf(8) == 39 && ItemLockSwapSettings.swapLineMode
                == ItemLockSwapSettings.SWAP_LINES_ALWAYS && ItemLockSwapSettings.swapLineWidth == 2);
        check("ranges clamp", ItemLockSwapSettings.cooldownMillis() == 5000);
        check("line visibility modes honor hover and off", ItemLockSwapSettings.showPersistentSwapLines(8)
                && !ItemLockSwapSettings.showPersistentSwapLines(0));
        ItemLockSwapSettings.swapLineMode = ItemLockSwapSettings.SWAP_LINES_OFF;
        check("off mode suppresses every persistent line", !ItemLockSwapSettings.showPersistentSwapLines(8));
    }

    private static void keybindPolicy() {
        check("Escape never binds", !ItemLockSwapKeybind.canBind(ItemLockSwapKeybind.ESCAPE));
        check("zero never binds", !ItemLockSwapKeybind.canBind(0));
        check("L can bind", ItemLockSwapKeybind.canBind(ItemLockSwapSettings.DEFAULT_KEY_CODE));
        check("collision sees another binding", ItemLockSwapKeybind.conflicts(38, 21, new int[]{21, 38, 44}));
        check("own binding does not self-conflict", !ItemLockSwapKeybind.conflicts(38, 38, new int[]{38}));
        ItemLockSwapSettings.keyCode = 38;
        check("rebind writes config state", ItemLockSwapKeybind.rebind(21) && ItemLockSwapSettings.keyCode == 21);
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++;
        else {
            failed++;
            System.err.println("FAIL " + name);
        }
    }
}
