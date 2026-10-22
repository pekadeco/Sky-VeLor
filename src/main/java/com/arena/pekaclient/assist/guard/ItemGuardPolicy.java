package com.arena.pekaclient.assist.guard;

/** Pure decision table for the UUID item guard's destructive-action hooks. */
public final class ItemGuardPolicy {
    private ItemGuardPolicy() {
    }

    /** Mode 4 is vanilla throw; slot -999 is the outside-container drop sentinel. */
    public static boolean shouldBlockWindowAction(boolean guardEnabled, boolean storageHealthy, boolean protectedItem,
                                                  int slotId, int mode) {
        if (!guardEnabled) return false;
        if (!storageHealthy) return mode == 4 || slotId == -999;
        return protectedItem && (mode == 4 || slotId == -999);
    }

    public static boolean shouldBlockHeldDrop(boolean guardEnabled, boolean storageHealthy, boolean protectedHeld) {
        return guardEnabled && (!storageHealthy || protectedHeld);
    }
}
