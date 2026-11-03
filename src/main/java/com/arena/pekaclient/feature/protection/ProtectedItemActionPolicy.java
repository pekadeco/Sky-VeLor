package com.arena.pekaclient.feature.protection;

/** Pure policy for the two vanilla 1.8.9 container-click forms that can throw an item. */
public final class ProtectedItemActionPolicy {
    private static final int OUTSIDE_SLOT = -999;
    private static final int THROW_MODE = 4;

    private ProtectedItemActionPolicy() { }

    /** Vanilla sends mode 0 with slot -999 when a cursor stack is released outside the GUI. */
    public static boolean shouldBlockOutsideDrop(int slotId, boolean cursorProtected, boolean packetStackProtected,
                                                  boolean storageUnavailable, boolean cursorOrPacketHasStack) {
        return slotId == OUTSIDE_SLOT && (cursorProtected || packetStackProtected
                || (storageUnavailable && cursorOrPacketHasStack));
    }

    /** Mode 4 is the explicit Q/control-Q throw action for a container slot. */
    public static boolean shouldBlockThrowAction(int mode, boolean clickedProtected, boolean storageUnavailable) {
        return mode == THROW_MODE && (clickedProtected || storageUnavailable);
    }
}
