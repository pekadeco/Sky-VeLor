package com.arena.pekaclient.assist.movement;

/** Pure vanilla-condition predicate for the explicitly opt-in movement assist. */
public final class AutoSprintPolicy {
    private AutoSprintPolicy() {
    }

    public static boolean mayStart(float forwardInput, boolean sneaking, boolean usingItem, boolean riding,
                                   boolean collidedHorizontally, boolean hasFoodOrFlight, boolean blinded) {
        return forwardInput >= 0.8F && !sneaking && !usingItem && !riding && !collidedHorizontally
                && hasFoodOrFlight && !blinded;
    }

    /** A held forward key plus vanilla-safe movement conditions is the only path that may hold sprint. */
    public static boolean shouldHoldSprintKey(boolean forwardKeyHeld, float forwardInput, boolean sneaking,
                                              boolean usingItem, boolean riding, boolean collidedHorizontally,
                                              boolean hasFoodOrFlight, boolean blinded) {
        return forwardKeyHeld && mayStart(forwardInput, sneaking, usingItem, riding, collidedHorizontally,
                hasFoodOrFlight, blinded);
    }
}
