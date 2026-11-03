package com.arena.pekaclient.feature.inventory;

/** Resolves Better NEF's slot-lock gesture: hold the key over an inventory/armor slot and move to a hotbar slot. */
public final class SlotBindingGesturePolicy {
    private SlotBindingGesturePolicy() { }

    public static Pair resolve(int sourceIndex, int clickedIndex) {
        if (sourceIndex == clickedIndex) return null;
        if (!isBindableSourceIndex(sourceIndex) || !isHotbarIndex(clickedIndex)) return null;
        return new Pair(sourceIndex, clickedIndex);
    }

    /** Defer a source lock-toggle while an eligible binding drag is in progress. */
    public static boolean shouldDeferLockToggle(int sourceIndex, boolean bindingEnabled) {
        return bindingEnabled && isBindableSourceIndex(sourceIndex);
    }

    /** Bound/locked slots must not fall through to vanilla's ordinary move/drop click path. */
    public static boolean shouldBlockVanillaSlotClick(boolean slotLocked, boolean permittedBoundSwap) {
        return slotLocked && !permittedBoundSwap;
    }

    /** Resolves a bound pair regardless of which endpoint the user clicks. */
    public static Pair resolveSwap(int clickedIndex, int boundTo, int sourceForHotbar) {
        if (isBindableSourceIndex(clickedIndex) && isHotbarIndex(boundTo)) {
            return new Pair(clickedIndex, boundTo);
        }
        if (isHotbarIndex(clickedIndex) && isBindableSourceIndex(sourceForHotbar)) {
            return new Pair(sourceForHotbar, clickedIndex);
        }
        return null;
    }

    /** Right-click, or shift-left-click, on a bound endpoint swaps it with its linked hotbar slot. */
    public static boolean shouldActivateSwap(int clickedButton, int clickType,
                                             boolean bindingEnabled, Pair swap) {
        boolean ordinaryRightClick = clickedButton == 1 && clickType == 0;
        boolean shiftLeftClick = clickedButton == 0 && clickType == 1;
        return bindingEnabled && swap != null && (ordinaryRightClick || shiftLeftClick);
    }

    public static boolean isHotbarIndex(int index) {
        return index >= 0 && index < SlotLockStore.HOTBAR_SIZE;
    }

    /** Player inventory indices 9-35 are the main inventory; 36-39 are the four armor slots. */
    public static boolean isBindableSourceIndex(int index) {
        return index >= SlotLockStore.MAIN_INVENTORY_START && index < SlotLockStore.SLOT_COUNT;
    }

    public static final class Pair {
        public final int inventoryIndex;
        public final int hotbarIndex;

        private Pair(int inventoryIndex, int hotbarIndex) {
            this.inventoryIndex = inventoryIndex;
            this.hotbarIndex = hotbarIndex;
        }
    }
}
