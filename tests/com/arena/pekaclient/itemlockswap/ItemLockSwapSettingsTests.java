package com.arena.pekaclient.itemlockswap;

import java.util.Properties;

/** Standalone regression tests for persisted position state, hotbar-only pair safety, and multi-pair cycling. */
public final class ItemLockSwapSettingsTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        freshInstallDefaults();
        lockAndHotbarPairRules();
        multiplePairsCycleInCreationOrder();
        everyLegalPairFitsAndPersists();
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
        check("legal graph has room for every hotbar edge", ItemLockSwapSettings.MAX_PAIRS == 315);
    }

    private static void lockAndHotbarPairRules() {
        ItemLockSwapSettings.load(new Properties());
        check("empty position locks", ItemLockSwapSettings.toggleLock(0) == ItemLockSwapSettings.LockResult.LOCKED
                && ItemLockSwapSettings.isLocked(0));
        check("locked position cannot pair", ItemLockSwapSettings.addPair(0, 39)
                == ItemLockSwapSettings.PairResult.LOCKED);
        check("unlock works", ItemLockSwapSettings.toggleLock(0) == ItemLockSwapSettings.LockResult.UNLOCKED);
        check("hotbar to armor pair is allowed", ItemLockSwapSettings.addPair(0, 39)
                == ItemLockSwapSettings.PairResult.ADDED);
        check("pair protects both endpoints", ItemLockSwapSettings.isProtected(0)
                && ItemLockSwapSettings.isProtected(39) && ItemLockSwapSettings.partnerOf(0) == 39);
        check("paired position cannot become lock", ItemLockSwapSettings.toggleLock(0)
                == ItemLockSwapSettings.LockResult.PAIRED);
        check("pair number is stable before mutation", ItemLockSwapSettings.pairNumber(0) == 1
                && ItemLockSwapSettings.pairNumber(39) == 1);
        check("remove existing pair", ItemLockSwapSettings.removePairContaining(39)
                && !ItemLockSwapSettings.isPaired(0));
        check("self pair is invalid", ItemLockSwapSettings.addPair(2, 2) == ItemLockSwapSettings.PairResult.INVALID);
        check("hotbar to inventory is legal", ItemLockSwapSettings.addPair(0, 9)
                == ItemLockSwapSettings.PairResult.ADDED);
        check("hotbar to armor is legal", ItemLockSwapSettings.addPair(1, 36)
                == ItemLockSwapSettings.PairResult.ADDED);
        check("hotbar to hotbar remains legal", ItemLockSwapSettings.addPair(2, 3)
                == ItemLockSwapSettings.PairResult.ADDED);
        check("inventory to inventory is illegal", ItemLockSwapSettings.addPair(9, 10)
                == ItemLockSwapSettings.PairResult.ILLEGAL);
        check("armor to inventory is illegal", ItemLockSwapSettings.addPair(36, 10)
                == ItemLockSwapSettings.PairResult.ILLEGAL);
        check("inventory to armor is illegal", ItemLockSwapSettings.addPair(11, 36)
                == ItemLockSwapSettings.PairResult.ILLEGAL);
        check("armor to armor is illegal", ItemLockSwapSettings.addPair(36, 37)
                == ItemLockSwapSettings.PairResult.ILLEGAL);
        check("illegal inputs never create a pair", ItemLockSwapSettings.pairCount() == 3);
    }

    private static void multiplePairsCycleInCreationOrder() {
        ItemLockSwapSettings.load(new Properties());
        check("first shared endpoint pair saves", ItemLockSwapSettings.addPair(0, 9)
                == ItemLockSwapSettings.PairResult.ADDED);
        check("second shared endpoint pair saves", ItemLockSwapSettings.addPair(0, 10)
                == ItemLockSwapSettings.PairResult.ADDED);
        check("third shared endpoint pair saves", ItemLockSwapSettings.addPair(0, 39)
                == ItemLockSwapSettings.PairResult.ADDED);
        check("a duplicate exact pair is not recreated", ItemLockSwapSettings.addPair(10, 0)
                == ItemLockSwapSettings.PairResult.ALREADY_PAIRED);
        check("shared endpoint reports every saved target", ItemLockSwapSettings.pairCountForSlot(0) == 3
                && ItemLockSwapSettings.isPaired(0));
        check("first manual target follows creation order", ItemLockSwapSettings.partnerForSwap(0) == 9);
        ItemLockSwapSettings.advanceSwapPartner(0, 9);
        check("second manual target follows creation order", ItemLockSwapSettings.partnerForSwap(0) == 10);
        ItemLockSwapSettings.advanceSwapPartner(0, 10);
        check("third manual target follows creation order", ItemLockSwapSettings.partnerForSwap(0) == 39);
        ItemLockSwapSettings.advanceSwapPartner(0, 39);
        check("manual target loops back after last pair", ItemLockSwapSettings.partnerForSwap(0) == 9);
        check("tap removal removes one pair and keeps the others", ItemLockSwapSettings.removePairContaining(0)
                && ItemLockSwapSettings.pairCountForSlot(0) == 2 && ItemLockSwapSettings.partnerOf(0) == 10);
        check("exact removal preserves unrelated shared pair", ItemLockSwapSettings.removePair(0, 39)
                && ItemLockSwapSettings.pairCountForSlot(0) == 1 && ItemLockSwapSettings.partnerOf(0) == 10);
        ItemLockSwapSettings.clearPairs();
        check("clear pairs removes all pairs", ItemLockSwapSettings.pairCount() == 0);
    }

    private static void everyLegalPairFitsAndPersists() {
        ItemLockSwapSettings.load(new Properties());
        boolean allAdded = true;
        // Every legal edge has a hotbar endpoint; visiting first hotbar 0..8 and later slots visits each once.
        for (int first = 0; first <= 8; first++) {
            for (int second = first + 1; second < ItemLockSwapSettings.SLOT_COUNT; second++) {
                if (ItemLockSwapSettings.addPair(first, second) != ItemLockSwapSettings.PairResult.ADDED) allAdded = false;
            }
        }
        check("every distinct legal hotbar pair fits", allAdded && ItemLockSwapSettings.pairCount()
                == ItemLockSwapSettings.MAX_PAIRS);
        Properties saved = new Properties();
        ItemLockSwapSettings.save(saved);
        ItemLockSwapSettings.load(saved);
        check("every saved legal pair survives a full load", ItemLockSwapSettings.pairCount()
                == ItemLockSwapSettings.MAX_PAIRS && ItemLockSwapSettings.pairCountForSlot(0) == 39);
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
        ItemLockSwapSettings.addPair(0, 9);
        ItemLockSwapSettings.addPair(0, 39);
        Properties p = new Properties();
        ItemLockSwapSettings.save(p);
        ItemLockSwapSettings.load(p);
        check("settings round trip", !ItemLockSwapSettings.enabled && ItemLockSwapSettings.keyCode == 21
                && ItemLockSwapSettings.cooldownMillis() == 3500 && ItemLockSwapSettings.iconColor == 4
                && ItemLockSwapSettings.iconOpacity == 65 && ItemLockSwapSettings.swapLineMode
                == ItemLockSwapSettings.SWAP_LINES_ON_HOVER && ItemLockSwapSettings.swapLineColor == 2
                && ItemLockSwapSettings.swapLineOpacity == 55 && ItemLockSwapSettings.swapLineWidth == 3);
        check("locks round trip", ItemLockSwapSettings.isLocked(8) && ItemLockSwapSettings.isLocked(36));
        check("multiple legal pairs round trip in order", ItemLockSwapSettings.pairCount() == 2
                && ItemLockSwapSettings.partnerForSwap(0) == 9);
        ItemLockSwapSettings.advanceSwapPartner(0, 9);
        check("loaded pair order cycles", ItemLockSwapSettings.partnerForSwap(0) == 39);

        Properties malformed = new Properties();
        malformed.setProperty(ItemLockSwapSettings.PREFIX + "locks", "-1,0,nope,40,35");
        // Legacy legal 8-39 survives; every pair without a hotbar endpoint is safely discarded.
        malformed.setProperty(ItemLockSwapSettings.PREFIX + "pairs", "7-1;bad;2-2;37-x;9-10;36-9;8-39;36-37");
        malformed.setProperty(ItemLockSwapSettings.PREFIX + "cooldownTenths", "999");
        ItemLockSwapSettings.load(malformed);
        check("bad slot data fails closed", ItemLockSwapSettings.isLocked(0) && ItemLockSwapSettings.isLocked(35)
                && !ItemLockSwapSettings.isLocked(39));
        check("legacy illegal pairs are dropped safely", ItemLockSwapSettings.pairCount() == 2
                && ItemLockSwapSettings.partnerOf(7) == 1 && ItemLockSwapSettings.partnerOf(8) == 39);
        check("legacy pairs keep new line defaults", ItemLockSwapSettings.swapLineMode
                == ItemLockSwapSettings.SWAP_LINES_ALWAYS && ItemLockSwapSettings.swapLineWidth == 2);
        check("ranges clamp", ItemLockSwapSettings.cooldownMillis() == 5000);
        ItemLockSwapSettings.swapLineMode = ItemLockSwapSettings.SWAP_LINES_ON_HOVER;
        check("line visibility modes honor hover and off", ItemLockSwapSettings.showPersistentSwapLines(8)
                && !ItemLockSwapSettings.showPersistentSwapLines(4));
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
