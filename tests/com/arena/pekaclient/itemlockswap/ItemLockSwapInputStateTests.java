package com.arena.pekaclient.itemlockswap;

import java.util.Properties;

/**
 * Drives the exact poll/release APIs used by ItemLockSwapController's DrawScreenEvent.Post handler with a mock
 * keyboard and a real-slot-index mock grid. No old keyTyped/timer/click pairing path is simulated here.
 */
public final class ItemLockSwapInputStateTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        createsPairByKeyDrag();
        sameSlotTogglesRegardlessOfHoldLength();
        cancellationAndRepeatedKeyDownAreSafe();
        pairedAndLockedReleaseRules();
        validatesLiveTargetHighlightRules();
        supportsHotbarArmorAndMultiplePairs();
        closeFocusAndClickSwallowRules();
        System.out.println("item lock/swap input state: passed " + passed + ", failed " + failed);
        if (failed > 0) throw new AssertionError("item lock/swap input failures: " + failed);
    }

    private static void createsPairByKeyDrag() {
        reset();
        MockGuiDrawHandler handler = new MockGuiDrawHandler();
        check("key down over A enters held", handler.frame(true, 4, 4, 100L) == null && handler.input.isHeld()
                && handler.input.startSlot() == 0);
        check("moving to B updates hover without click", handler.frame(true, 24, 4, 180L) == null
                && handler.input.hoveredSlot() == 1);
        check("release over B creates pair", handler.frame(false, 24, 4, 220L)
                == ItemLockSwapGesturePolicy.Decision.PAIR_CREATED
                && ItemLockSwapSettings.partnerOf(0) == 1);
    }

    private static void sameSlotTogglesRegardlessOfHoldLength() {
        reset();
        MockGuiDrawHandler handler = new MockGuiDrawHandler();
        handler.frame(true, 4, 4, 100L);
        check("unpaired L tap locks", handler.frame(false, 4, 4, 110L)
                == ItemLockSwapGesturePolicy.Decision.LOCKED && ItemLockSwapSettings.isLocked(0));
        handler.frame(true, 4, 4, 200L);
        check("locked L tap unlocks", handler.frame(false, 4, 4, 210L)
                == ItemLockSwapGesturePolicy.Decision.UNLOCKED && !ItemLockSwapSettings.isLocked(0));
        handler.frame(true, 4, 4, 300L);
        check("long same-slot release still locks without a timer", handler.frame(false, 4, 4, 2300L)
                == ItemLockSwapGesturePolicy.Decision.LOCKED && ItemLockSwapSettings.isLocked(0));
    }

    private static void cancellationAndRepeatedKeyDownAreSafe() {
        reset();
        MockGuiDrawHandler handler = new MockGuiDrawHandler();
        handler.frame(true, 4, 4, 100L);
        check("repeat down does not restart held gesture", handler.frame(true, 4, 4, 500L) == null
                && handler.input.isHeld() && handler.input.heldAt() == 100L);
        check("empty-space release cancels unchanged", handler.frame(false, 200, 200, 510L)
                == ItemLockSwapGesturePolicy.Decision.CANCELED && ItemLockSwapSettings.pairCount() == 0
                && !ItemLockSwapSettings.isLocked(0));

        handler.frame(true, 200, 200, 600L);
        check("pressing outside grid cannot arm after moving onto slot", handler.frame(true, 4, 4, 610L) == null
                && !handler.input.isHeld());
        check("outside press release is ignored", handler.frame(false, 4, 4, 620L) == null);
    }

    private static void pairedAndLockedReleaseRules() {
        reset();
        check("fixture pair creates", ItemLockSwapSettings.addPair(0, 1) == ItemLockSwapSettings.PairResult.ADDED);
        MockGuiDrawHandler handler = new MockGuiDrawHandler();
        handler.frame(true, 4, 4, 10L);
        check("tap on first paired endpoint removes both endpoints", handler.frame(false, 4, 4, 20L)
                == ItemLockSwapGesturePolicy.Decision.PAIR_REMOVED && !ItemLockSwapSettings.isPaired(0)
                && !ItemLockSwapSettings.isPaired(1));
        Properties saved = new Properties();
        ItemLockSwapSettings.save(saved);
        ItemLockSwapSettings.load(saved);
        check("tap removal persists without a stale pair", ItemLockSwapSettings.pairCount() == 0
                && ItemLockSwapSettings.partnerOf(0) < 0 && ItemLockSwapSettings.partnerOf(1) < 0);

        check("fixture pair re-creates", ItemLockSwapSettings.addPair(0, 1) == ItemLockSwapSettings.PairResult.ADDED);
        handler = new MockGuiDrawHandler();
        handler.frame(true, 24, 4, 30L);
        check("tap on partner paired endpoint removes pair", handler.frame(false, 24, 4, 40L)
                == ItemLockSwapGesturePolicy.Decision.PAIR_REMOVED && ItemLockSwapSettings.pairCount() == 0);

        check("fixture pair re-creates for drag removal", ItemLockSwapSettings.addPair(0, 1)
                == ItemLockSwapSettings.PairResult.ADDED);
        handler = new MockGuiDrawHandler();
        handler.frame(true, 4, 4, 50L);
        check("paired start dragged onto partner still removes pair", handler.frame(false, 24, 4, 60L)
                == ItemLockSwapGesturePolicy.Decision.PAIR_REMOVED && !ItemLockSwapSettings.isPaired(0));

        check("fixture pair re-creates for a second target", ItemLockSwapSettings.addPair(0, 1)
                == ItemLockSwapSettings.PairResult.ADDED);
        handler = new MockGuiDrawHandler();
        handler.frame(true, 4, 4, 70L);
        check("paired start can create another legal pair", handler.frame(false, 44, 4, 80L)
                == ItemLockSwapGesturePolicy.Decision.PAIR_CREATED
                && ItemLockSwapSettings.pairCountForSlot(0) == 2);

        reset();
        ItemLockSwapSettings.toggleLock(1);
        handler = new MockGuiDrawHandler();
        handler.frame(true, 4, 4, 90L);
        check("locked target refuses", handler.frame(false, 24, 4, 100L)
                == ItemLockSwapGesturePolicy.Decision.REFUSED_LOCKED && !ItemLockSwapSettings.isPaired(0));

        reset();
        ItemLockSwapSettings.addPair(1, 2);
        handler = new MockGuiDrawHandler();
        handler.frame(true, 4, 4, 110L);
        check("an already paired target can share a legal hotbar pair", handler.frame(false, 24, 4, 120L)
                == ItemLockSwapGesturePolicy.Decision.PAIR_CREATED && ItemLockSwapSettings.isPaired(0));
    }

    private static void validatesLiveTargetHighlightRules() {
        reset();
        check("new unlocked target is green-valid", ItemLockSwapGesturePolicy.isValidPairTarget(0, 1));
        check("same slot is red-invalid", !ItemLockSwapGesturePolicy.isValidPairTarget(0, 0));
        ItemLockSwapSettings.toggleLock(1);
        check("locked target is red-invalid", !ItemLockSwapGesturePolicy.isValidPairTarget(0, 1));
        ItemLockSwapSettings.toggleLock(1);
        ItemLockSwapSettings.addPair(1, 2);
        check("already paired target is a valid shared hotbar target", ItemLockSwapGesturePolicy.isValidPairTarget(0, 1));
        check("main-to-main target is red-invalid", !ItemLockSwapGesturePolicy.isValidPairTarget(9, 10));
        ItemLockSwapSettings.clearPairs();
        ItemLockSwapSettings.addPair(0, 1);
        check("paired partner is a valid green removal target", ItemLockSwapGesturePolicy.isValidPairTarget(0, 1));
    }

    private static void supportsHotbarArmorAndMultiplePairs() {
        reset();
        MockGuiDrawHandler handler = new MockGuiDrawHandler();
        handler.frame(true, 4, 4, 100L);
        check("hotbar to armor L-drag pairs", handler.frame(false, MockSlotGrid.armorX(39), 4, 110L)
                == ItemLockSwapGesturePolicy.Decision.PAIR_CREATED && ItemLockSwapSettings.partnerOf(0) == 39);

        reset();
        ItemLockSwapSettings.addPair(0, 9);
        ItemLockSwapSettings.addPair(0, 10);
        handler = new MockGuiDrawHandler();
        handler.frame(true, 4, 4, 120L);
        check("L tap on a shared endpoint still removes one pair", handler.frame(false, 4, 4, 130L)
                == ItemLockSwapGesturePolicy.Decision.PAIR_REMOVED && ItemLockSwapSettings.pairCountForSlot(0) == 1
                && ItemLockSwapSettings.partnerOf(0) == 10);

        reset();
        handler = new MockGuiDrawHandler();
        handler.frame(true, MockSlotGrid.slotX(9), 4, 140L);
        check("inventory to inventory L-drag is refused", handler.frame(false, MockSlotGrid.slotX(10), 4, 150L)
                == ItemLockSwapGesturePolicy.Decision.REFUSED_ILLEGAL && ItemLockSwapSettings.pairCount() == 0);
    }

    private static void closeFocusAndClickSwallowRules() {
        reset();
        MockGuiDrawHandler handler = new MockGuiDrawHandler();
        handler.frame(true, 4, 4, 100L);
        check("held state asks controller to swallow container click", handler.shouldSwallowContainerClick());
        handler.input.ignore(true); // Same call used by the controller for a focused text field.
        check("focus loss resets held state", !handler.input.isHeld() && !handler.shouldSwallowContainerClick());
        check("focused key tail cannot release an action", handler.frame(false, 4, 4, 110L) == null);
        handler.input.cancel(); // Same reset used when GuiOpen closes or replaces a container.
        check("GUI close reset permits a later fresh down edge", handler.frame(true, 4, 4, 120L) == null
                && handler.input.isHeld());
    }

    private static void reset() {
        ItemLockSwapSettings.load(new Properties());
    }

    /** Tiny same-real-handler stand-in: keyboard boolean + slot hit-test go straight into InputState.poll. */
    private static final class MockGuiDrawHandler {
        final ItemLockSwapInputState input = new ItemLockSwapInputState();
        final MockKeyboard keyboard = new MockKeyboard();
        final MockSlotGrid grid = new MockSlotGrid();

        ItemLockSwapGesturePolicy.Decision frame(boolean down, int x, int y, long now) {
            keyboard.down = down;
            int transition = input.poll(keyboard.down, grid.slotAt(x, y), now);
            return transition == ItemLockSwapInputState.RELEASED
                    ? ItemLockSwapGesturePolicy.release(input.releasedStartSlot(), input.releasedSlot()) : null;
        }

        boolean shouldSwallowContainerClick() {
            return input.isHeld();
        }
    }

    private static final class MockKeyboard {
        boolean down;
    }

    /** Slots 0-35 occupy a 20px strip; armor positions deliberately live at a distinct visual x coordinate. */
    private static final class MockSlotGrid {
        int slotAt(int x, int y) {
            if (y < 0 || y >= 16) return -1;
            if (x >= 0 && x < 36 * 20) return x / 20;
            if (x >= armorX(36) && x < armorX(40)) return 36 + (x - armorX(36)) / 20;
            return -1;
        }

        static int slotX(int slot) {
            return slot * 20 + 4;
        }

        static int armorX(int slot) {
            return 800 + (slot - 36) * 20 + 4;
        }
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++;
        else {
            failed++;
            System.err.println("FAIL " + name);
        }
    }
}
