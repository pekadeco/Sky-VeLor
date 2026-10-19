package com.arena.pekaclient.itemlockswap;

/** Pure selected-hotbar guard shared by Q and Ctrl+Q; both route through EntityPlayerSP.dropOneItem(boolean). */
public final class ItemLockSwapDropPolicy {

    private ItemLockSwapDropPolicy() {
    }

    /** dropAll distinguishes Q from Ctrl+Q for test/documentation; protection is identical for both. */
    public static boolean shouldBlock(boolean enabled, boolean selectedSlotProtected, boolean internalSwap,
                                      boolean dropAll) {
        return enabled && selectedSlotProtected && !internalSwap;
    }
}
