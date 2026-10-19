package com.arena.pekaclient.itemlockswap;

/** Small clock-driven L-key gesture state. It contains no Minecraft input or rendering dependencies. */
public final class ItemLockSwapInputState {

    public static final long TAP_LIMIT_MILLIS = 300L;

    private boolean down;
    private boolean slotClicked;
    private long pressedAt;

    /** Returns false for repeated key-down events so their timestamps never turn a hold into a tap. */
    public boolean press(long now) {
        if (down) return false;
        down = true;
        slotClicked = false;
        pressedAt = now;
        return true;
    }

    public boolean isDown() {
        return down;
    }

    public void markSlotClick() {
        if (down) slotClicked = true;
    }

    /** Releases the gesture and returns true only for a short press that never clicked an inventory position. */
    public boolean releaseIsTap(long now) {
        boolean tap = down && !slotClicked && now - pressedAt >= 0L && now - pressedAt < TAP_LIMIT_MILLIS;
        cancel();
        return tap;
    }

    public void cancel() {
        down = false;
        slotClicked = false;
        pressedAt = 0L;
    }
}
