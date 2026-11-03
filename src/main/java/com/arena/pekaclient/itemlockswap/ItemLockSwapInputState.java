package com.arena.pekaclient.itemlockswap;

/**
 * Allocation-free physical-key polling state for the Item Lock/Swap drag gesture.
 *
 * <p>The controller calls {@link #poll(boolean, int, long)} once per GUI draw frame. A gesture can begin only on
 * the physical down edge while a player-inventory slot is under the pointer; moving onto a slot after the key was
 * already pressed never arms a gesture. A release is reported exactly once on the following down-to-up edge.</p>
 */
public final class ItemLockSwapInputState {

    public static final int IDLE = 0;
    public static final int HELD = 1;

    public static final int NO_CHANGE = 0;
    public static final int STARTED = 1;
    public static final int RELEASED = 2;

    private int state = IDLE;
    private boolean physicalDown;
    private int startSlot = -1;
    private int hoveredSlot = -1;
    private int releasedStartSlot = -1;
    private int releasedSlot = -1;
    private long heldAt;

    /**
     * Polls the physical bound-key state and current player-slot hit-test result. {@code hoveredSlot} is -1 outside
     * the player inventory. The return value is one of {@link #NO_CHANGE}, {@link #STARTED}, or {@link #RELEASED}.
     */
    public int poll(boolean keyDown, int hoveredSlot, long now) {
        if (keyDown) {
            if (!physicalDown && hoveredSlot >= 0) {
                state = HELD;
                startSlot = hoveredSlot;
                this.hoveredSlot = hoveredSlot;
                heldAt = now;
                physicalDown = true;
                return STARTED;
            }
            physicalDown = true;
            if (state == HELD) this.hoveredSlot = hoveredSlot;
            return NO_CHANGE;
        }

        boolean wasDown = physicalDown;
        physicalDown = false;
        if (state != HELD || !wasDown) return NO_CHANGE;
        releasedStartSlot = startSlot;
        releasedSlot = hoveredSlot;
        state = IDLE;
        startSlot = -1;
        this.hoveredSlot = -1;
        heldAt = 0L;
        return RELEASED;
    }

    /**
     * Stops an incomplete gesture while remembering an already-held physical key. This prevents focused text fields
     * and newly opened screens from treating the tail of that key press as a fresh drag.
     */
    public void ignore(boolean keyDown) {
        state = IDLE;
        physicalDown = keyDown;
        startSlot = -1;
        hoveredSlot = -1;
        releasedStartSlot = -1;
        releasedSlot = -1;
        heldAt = 0L;
    }

    /** Resets all state; used for GUI close/focus transitions where the next press may begin a fresh gesture. */
    public void cancel() {
        ignore(false);
    }

    public boolean isHeld() {
        return state == HELD;
    }

    public boolean isPhysicalDown() {
        return physicalDown;
    }

    public int state() {
        return state;
    }

    public String stateName() {
        return state == HELD ? "HELD" : "IDLE";
    }

    public int startSlot() {
        return startSlot;
    }

    public int hoveredSlot() {
        return hoveredSlot;
    }

    public int releasedStartSlot() {
        return releasedStartSlot;
    }

    public int releasedSlot() {
        return releasedSlot;
    }

    public long heldAt() {
        return heldAt;
    }

    public boolean hasHeldFor(long now, long millis) {
        return state == HELD && now - heldAt >= millis;
    }
}
