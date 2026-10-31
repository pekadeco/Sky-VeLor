package com.arena.pekaclient.chat;

/**
 * Guards Shift-left-click copy so the release must belong to the same logical chat message as the press.
 * This deliberately contains no Minecraft types so the gesture can be regression-tested headlessly.
 */
public final class ChatShiftClickGesture {
    public static final int PASS = -1;
    public static final int CONSUME = -2;
    public static final int COPY = -3;

    private String pendingGroupId;

    /** Arms a copy gesture for a non-empty logical message ID. */
    public boolean begin(String groupId) {
        if (groupId == null || groupId.length() == 0) {
            pendingGroupId = null;
            return false;
        }
        pendingGroupId = groupId;
        return true;
    }

    /**
     * Ends the armed gesture on left-button release. A mismatched/outside release is consumed but cannot copy a
     * different message. Unrelated mouse buttons pass through without cancelling the pending left-button release.
     */
    public int release(int button, String groupId) {
        if (pendingGroupId == null || button != 0) return PASS;
        String expected = pendingGroupId;
        pendingGroupId = null;
        return expected.equals(groupId) ? COPY : CONSUME;
    }

    public void reset() {
        pendingGroupId = null;
    }

    public boolean pending() {
        return pendingGroupId != null;
    }
}