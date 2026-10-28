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
        REFUSED_ALREADY_PAIRED("pair already exists"),
        REFUSED_ILLEGAL("one endpoint must be a hotbar slot"),
        REFUSED_FULL("all legal pairs are already saved"),
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
     * Applies one completed L-key drag release. A paired start can create another unique legal pair. Tapping L back
     * on that start removes its oldest remaining pair, while dragging to a known partner removes that exact pair.
     */
    public static Decision release(int startSlot, int releaseSlot) {
        if (!ItemLockSwapSettings.validSlot(startSlot)) return Decision.REFUSED_INVALID;
        if (!ItemLockSwapSettings.validSlot(releaseSlot)) return Decision.CANCELED;

        if (releaseSlot == startSlot) {
            if (ItemLockSwapSettings.isPaired(startSlot)) {
                return ItemLockSwapSettings.removePairContaining(startSlot)
                        ? Decision.PAIR_REMOVED : Decision.REFUSED_INVALID;
            }
            // A same-slot release is a lock toggle at every hold duration; there is deliberately no tap timer.
            ItemLockSwapSettings.LockResult lock = ItemLockSwapSettings.toggleLock(startSlot);
            if (lock == ItemLockSwapSettings.LockResult.LOCKED) return Decision.LOCKED;
            if (lock == ItemLockSwapSettings.LockResult.UNLOCKED) return Decision.UNLOCKED;
            return lock == ItemLockSwapSettings.LockResult.PAIRED
                    ? Decision.REFUSED_ALREADY_PAIRED : Decision.REFUSED_INVALID;
        }

        if (ItemLockSwapSettings.isLocked(startSlot) || ItemLockSwapSettings.isLocked(releaseSlot)) {
            return Decision.REFUSED_LOCKED;
        }

        // Dragging an existing endpoint to its existing partner remains the precise removal gesture.
        if (ItemLockSwapSettings.removePair(startSlot, releaseSlot)) return Decision.PAIR_REMOVED;

        ItemLockSwapSettings.PairResult result = ItemLockSwapSettings.addPair(startSlot, releaseSlot);
        switch (result) {
            case ADDED:
                return Decision.PAIR_CREATED;
            case LOCKED:
                return Decision.REFUSED_LOCKED;
            case ALREADY_PAIRED:
                return Decision.REFUSED_ALREADY_PAIRED;
            case ILLEGAL:
                return Decision.REFUSED_ILLEGAL;
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
        if (ItemLockSwapSettings.isLocked(startSlot) || ItemLockSwapSettings.isLocked(hoveredSlot)) return false;
        // An existing endpoint is a valid removal target. A new target must pass the hotbar legality rule.
        return ItemLockSwapSettings.partnerOf(startSlot) == hoveredSlot
                || (ItemLockSwapSettings.isLegalPair(startSlot, hoveredSlot)
                && ItemLockSwapSettings.partnerOf(startSlot) != hoveredSlot);
    }
}
