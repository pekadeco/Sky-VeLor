package com.arena.pekaclient.itemlockswap;

/**
 * Pure pre-packet decision for PlayerControllerMP.windowClick. Each branch is intentionally explicit so newly
 * introduced click modes cannot accidentally inherit a permissive default.
 */
public final class ItemLockSwapClickPolicy {

    public static final int PICKUP = 0;
    public static final int QUICK_MOVE = 1;
    public static final int SWAP = 2;
    public static final int CLONE = 3;
    public static final int THROW = 4;
    public static final int QUICK_CRAFT = 5;
    public static final int PICKUP_ALL = 6;

    private ItemLockSwapClickPolicy() {
    }

    /**
     * @param clickedProtected true when the addressed container slot maps to a locked or paired player position
     * @param hotbarSourceProtected true for mode 2 when its 0-8 number-key source is protected
     * @param collectWouldTouchProtected true for mode 6 when the cursor's stack matches a protected player slot
     */
    public static boolean shouldBlock(int mode, boolean clickedProtected, boolean hotbarSourceProtected,
                                      boolean collectWouldTouchProtected) {
        switch (mode) {
            case PICKUP:
            case QUICK_MOVE:
            case CLONE:
            case THROW:
            case QUICK_CRAFT:
                return clickedProtected;
            case SWAP:
                return clickedProtected || hotbarSourceProtected;
            case PICKUP_ALL:
                return clickedProtected || collectWouldTouchProtected;
            default:
                // Fail open for a future/unrecognised vanilla mode rather than cancelling unrelated mod actions.
                return false;
        }
    }
}
