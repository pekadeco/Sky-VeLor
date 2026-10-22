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
}
