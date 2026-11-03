package com.arena.pekaclient.itemlockswap;

/** Pure safety and cooldown policy for the three-click internal pair transaction. */
public final class ItemLockSwapSwapPolicy {

    private ItemLockSwapSwapPolicy() {
    }

    public enum Result {
        ALLOW, CURSOR_NOT_EMPTY, SLOT_UNAVAILABLE, ITEM_DOES_NOT_FIT, COOLDOWN
    }

    /**
     * The two Slot.isItemValid outcomes are evaluated before packet clicks. This prevents a temporary cursor stack
     * from getting stranded when a main/hotbar item cannot occupy an armor slot (or vice versa).
     */
    public static Result evaluate(boolean cursorEmpty, boolean firstCanTake, boolean secondCanTake,
                                  boolean firstHasStack, boolean secondAcceptsFirst,
                                  boolean secondHasStack, boolean firstAcceptsSecond,
                                  long now, long lastSwapAt, int cooldownMillis) {
        if (!cursorEmpty) return Result.CURSOR_NOT_EMPTY;
        if (!firstCanTake || !secondCanTake) return Result.SLOT_UNAVAILABLE;
        if ((firstHasStack && !secondAcceptsFirst) || (secondHasStack && !firstAcceptsSecond)) {
            return Result.ITEM_DOES_NOT_FIT;
        }
        if (remainingMillis(now, lastSwapAt, cooldownMillis) > 0L) return Result.COOLDOWN;
        return Result.ALLOW;
    }

    public static long remainingMillis(long now, long lastSwapAt, int cooldownMillis) {
        if (lastSwapAt <= 0L || cooldownMillis <= 0) return 0L;
        long elapsed = now - lastSwapAt;
        if (elapsed < 0L) return 0L;
        long remaining = cooldownMillis - elapsed;
        return remaining > 0L ? remaining : 0L;
    }
}
