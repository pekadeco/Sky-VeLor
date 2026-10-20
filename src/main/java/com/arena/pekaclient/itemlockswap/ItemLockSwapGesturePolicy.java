package com.arena.pekaclient.itemlockswap;

/**
 * The one release decision used by the polled drag controller. Keeping it independent of Minecraft GUI classes lets
 * the same real state-machine path be regression-tested with a mock slot grid and keyboard.
 */
public final class ItemLockSwapGesturePolicy {

    private ItemLockSwapGesturePolicy() {
    }

    public enum Decision {
        LOCKED("released on start slot: position locked"),
        UNLOCKED("released on start slot: position unlocked"),
        PAIR_CREATED("released on valid target: pair created"),
        PAIR_REMOVED("released on paired endpoint: pair removed"),
        CANCELED("released over empty space: canceled"),
        REFUSED_LOCKED("released on locked position: refused"),
        REFUSED_ALREADY_PAIRED("Slot is already paired"),
        REFUSED_FULL("maximum of 8 pairs reached"),
        REFUSED_INVALID("invalid player-inventory slot: refused");

        private final String reason;

        Decision(String reason) {
            this.reason = reason;
        }

        public String reason() {
            return reason;
        }
    }

    /**
     * Applies one completed L-key drag release. Pair membership takes priority for an existing pair: a tap back onto
     * either endpoint, or a drag onto its partner, removes that pair. Other targets remain unchanged and refuse.
     */
    public static Decision release(int startSlot, int releaseSlot) {
        if (!ItemLockSwapSettings.validSlot(startSlot)) return Decision.REFUSED_INVALID;
        if (!ItemLockSwapSettings.validSlot(releaseSlot)) return Decision.CANCELED;

        int partner = ItemLockSwapSettings.partnerOf(startSlot);
        if (partner >= 0) {
            if ((releaseSlot == startSlot || releaseSlot == partner)
                    && ItemLockSwapSettings.removePairContaining(startSlot)) {
                return Decision.PAIR_REMOVED;
            }
            return Decision.REFUSED_ALREADY_PAIRED;
        }

        // A same-slot release is a lock toggle at every hold duration; there is deliberately no tap timer.
        if (releaseSlot == startSlot) {
            ItemLockSwapSettings.LockResult lock = ItemLockSwapSettings.toggleLock(startSlot);
            if (lock == ItemLockSwapSettings.LockResult.LOCKED) return Decision.LOCKED;
            if (lock == ItemLockSwapSettings.LockResult.UNLOCKED) return Decision.UNLOCKED;
            return lock == ItemLockSwapSettings.LockResult.PAIRED
                    ? Decision.REFUSED_ALREADY_PAIRED : Decision.REFUSED_INVALID;
        }

        if (ItemLockSwapSettings.isLocked(startSlot) || ItemLockSwapSettings.isLocked(releaseSlot)) {
            return Decision.REFUSED_LOCKED;
        }
        if (ItemLockSwapSettings.isPaired(releaseSlot)) return Decision.REFUSED_ALREADY_PAIRED;

        ItemLockSwapSettings.PairResult result = ItemLockSwapSettings.addPair(startSlot, releaseSlot);
        switch (result) {
            case ADDED:
                return Decision.PAIR_CREATED;
            case LOCKED:
                return Decision.REFUSED_LOCKED;
            case ALREADY_PAIRED:
                return Decision.REFUSED_ALREADY_PAIRED;
            case FULL:
                return Decision.REFUSED_FULL;
            default:
                return Decision.REFUSED_INVALID;
        }
    }

    /** Used by the foreground overlay before it draws the green/red target ring. */
    public static boolean isValidPairTarget(int startSlot, int hoveredSlot) {
        if (!ItemLockSwapSettings.validSlot(startSlot) || !ItemLockSwapSettings.validSlot(hoveredSlot)
                || startSlot == hoveredSlot) {
            return false;
        }
        int partner = ItemLockSwapSettings.partnerOf(startSlot);
        if (partner >= 0) return partner == hoveredSlot;
        return !ItemLockSwapSettings.isLocked(startSlot) && !ItemLockSwapSettings.isLocked(hoveredSlot)
                && !ItemLockSwapSettings.isPaired(hoveredSlot);
    }
}
