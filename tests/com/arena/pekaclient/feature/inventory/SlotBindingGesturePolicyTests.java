package com.arena.pekaclient.feature.inventory;

public final class SlotBindingGesturePolicyTests {
    private SlotBindingGesturePolicyTests() { }

    public static void main(String[] args) {
        testLockToggleIsDeferredForBindingSources();
        testLockedSlotsBlockVanillaActions();
        testMainInventoryToHotbar();
        testArmorToHotbar();
        testSwapCanStartFromInventorySource();
        testSwapCanStartFromHotbarTarget();
        testSwapRejectsUnboundSlots();
        testSwapActivatesOnRightClickAndShiftLeftClick();
        testRejectsSameSlot();
        testRejectsTwoMainInventorySlots();
        testRejectsTwoHotbarSlots();
        testRejectsArmorAndCraftingSlots();
        System.out.println("SlotBindingGesturePolicyTests: 12 tests passed, 0 failed");
    }

    private static void testLockToggleIsDeferredForBindingSources() {
        assertTrue(SlotBindingGesturePolicy.shouldDeferLockToggle(9, true),
                "main-inventory source should defer its lock toggle while binding is available");
        assertTrue(SlotBindingGesturePolicy.shouldDeferLockToggle(39, true),
                "armor source should defer its lock toggle while binding is available");
        assertFalse(SlotBindingGesturePolicy.shouldDeferLockToggle(2, true),
                "hotbar target should keep ordinary lock-toggle behavior");
        assertFalse(SlotBindingGesturePolicy.shouldDeferLockToggle(9, false),
                "disabled binding should keep ordinary lock-toggle behavior");
    }

    private static void testLockedSlotsBlockVanillaActions() {
        assertTrue(SlotBindingGesturePolicy.shouldBlockVanillaSlotClick(true, false),
                "locked/bound slot must not fall through to vanilla move/drop behavior");
        assertFalse(SlotBindingGesturePolicy.shouldBlockVanillaSlotClick(false, false),
                "unlocked slot should preserve vanilla behavior");
        assertFalse(SlotBindingGesturePolicy.shouldBlockVanillaSlotClick(true, true),
                "the dedicated validated bound-swap path is the only exception");
    }

    private static void testMainInventoryToHotbar() {
        SlotBindingGesturePolicy.Pair pair = SlotBindingGesturePolicy.resolve(20, 3);
        assertPair(pair, 20, 3, "main-inventory source should bind to clicked hotbar slot");
    }

    private static void testArmorToHotbar() {
        SlotBindingGesturePolicy.Pair pair = SlotBindingGesturePolicy.resolve(39, 3);
        assertPair(pair, 39, 3, "armor source should bind to a hotbar slot like Better NEF");
    }

    private static void testSwapCanStartFromInventorySource() {
        SlotBindingGesturePolicy.Pair pair = SlotBindingGesturePolicy.resolveSwap(20, 3, -1);
        assertPair(pair, 20, 3, "right-clicking the bound inventory source should swap with its hotbar target");
    }

    private static void testSwapCanStartFromHotbarTarget() {
        SlotBindingGesturePolicy.Pair pair = SlotBindingGesturePolicy.resolveSwap(3, -1, 20);
        assertPair(pair, 20, 3, "right-clicking the bound hotbar target should swap with its source");
    }

    private static void testSwapRejectsUnboundSlots() {
        assertNull(SlotBindingGesturePolicy.resolveSwap(3, -1, -1), "unbound hotbar slot must not trigger a swap");
        assertNull(SlotBindingGesturePolicy.resolveSwap(20, -1, -1), "unbound inventory slot must not trigger a swap");
        assertNull(SlotBindingGesturePolicy.resolveSwap(40, 3, -1), "invalid source must not trigger a swap");
    }

    private static void testSwapActivatesOnRightClickAndShiftLeftClick() {
        SlotBindingGesturePolicy.Pair pair = SlotBindingGesturePolicy.resolveSwap(20, 3, -1);
        assertTrue(SlotBindingGesturePolicy.shouldActivateSwap(1, 0, true, pair),
                "ordinary enabled right-click on a bound endpoint should be intercepted");
        assertTrue(SlotBindingGesturePolicy.shouldActivateSwap(0, 1, true, pair),
                "shift-left-click on a bound endpoint should swap the item into its linked hotbar slot");
        assertFalse(SlotBindingGesturePolicy.shouldActivateSwap(0, 0, true, pair),
                "ordinary left-click should keep vanilla behavior");
        assertFalse(SlotBindingGesturePolicy.shouldActivateSwap(1, 1, true, pair),
                "shift-right-click should keep vanilla behavior");
        assertFalse(SlotBindingGesturePolicy.shouldActivateSwap(1, 0, false, pair),
                "disabled binding should keep vanilla behavior");
        assertFalse(SlotBindingGesturePolicy.shouldActivateSwap(0, 1, false, pair),
                "shift-click should keep vanilla behavior when binding is disabled");
        assertFalse(SlotBindingGesturePolicy.shouldActivateSwap(1, 0, true, null),
                "an unbound slot should keep vanilla behavior");
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void assertFalse(boolean value, String message) {
        if (value) throw new AssertionError(message);
    }

    private static void testRejectsSameSlot() {
        assertNull(SlotBindingGesturePolicy.resolve(9, 9), "same slot cannot bind to itself");
    }

    private static void testRejectsTwoMainInventorySlots() {
        assertNull(SlotBindingGesturePolicy.resolve(9, 35), "main-inventory slots cannot bind to each other");
    }

    private static void testRejectsTwoHotbarSlots() {
        assertNull(SlotBindingGesturePolicy.resolve(0, 8), "hotbar slots cannot bind to each other");
    }

    private static void testRejectsArmorAndCraftingSlots() {
        assertNull(SlotBindingGesturePolicy.resolve(39, 36), "armor slot cannot be a hotbar target");
        assertNull(SlotBindingGesturePolicy.resolve(1, 36), "armor slot cannot be a hotbar target");
        assertNull(SlotBindingGesturePolicy.resolve(7, 31), "binding must start on the main inventory and target the hotbar");
        assertNull(SlotBindingGesturePolicy.resolve(-1, 4), "invalid slot cannot be a binding endpoint");
    }

    private static void assertPair(SlotBindingGesturePolicy.Pair pair, int inventoryIndex,
                                   int hotbarIndex, String message) {
        if (pair == null) throw new AssertionError(message + " (pair was null)");
        if (pair.inventoryIndex != inventoryIndex || pair.hotbarIndex != hotbarIndex) {
            throw new AssertionError(message + " (expected=" + inventoryIndex + "->" + hotbarIndex
                    + ", actual=" + pair.inventoryIndex + "->" + pair.hotbarIndex + ")");
        }
    }

    private static void assertNull(Object value, String message) {
        if (value != null) throw new AssertionError(message + " (unexpected result)");
    }
}
