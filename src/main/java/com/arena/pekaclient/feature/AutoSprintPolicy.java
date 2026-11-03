package com.arena.pekaclient.feature;

/** Pure vanilla-compatible checks for when forward auto-sprint may begin. */
public final class AutoSprintPolicy {
    private AutoSprintPolicy() { }

    public static boolean shouldStart(float forwardInput, boolean sneaking, boolean usingItem, boolean riding,
                                      boolean collidedHorizontally, boolean hasFoodOrFlight, boolean blinded) {
        return forwardInput >= 0.8F && !sneaking && !usingItem && !riding
                && !collidedHorizontally && hasFoodOrFlight && !blinded;
    }
}
