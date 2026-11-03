package com.arena.pekaclient.itemlockswap;

/** Small shared geometry contract for pair-center and live-drag line endpoints. */
public final class ItemLockSwapLineGeometry {

    public static final float SLOT_CENTER_OFFSET = 8f;

    private ItemLockSwapLineGeometry() {
    }

    public static float slotCenterX(int guiLeft, int slotDisplayX) {
        return guiLeft + slotDisplayX + SLOT_CENTER_OFFSET;
    }

    public static float slotCenterY(int guiTop, int slotDisplayY) {
        return guiTop + slotDisplayY + SLOT_CENTER_OFFSET;
    }

    /** The live endpoint is the scaled GUI cursor position, not a stale Mouse event coordinate. */
    public static float cursorX(int mouseX) {
        return mouseX;
    }

    public static float cursorY(int mouseY) {
        return mouseY;
    }
}
