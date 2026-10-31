package com.arena.pekaclient.feature.inventory;

/** Pure coordinate rules shared by inventory overlays and covered by regression tests. */
public final class InventoryLayoutPolicy {
    private static final int INVENTORY_WIDTH = 176;
    private static final int INVENTORY_HEIGHT = 166;
    private static final int POTION_PANEL_WIDTH = 60;
    private static final int CHEST_BASE_HEIGHT = 114;
    private static final int SLOT_SIZE = 18;

    private InventoryLayoutPolicy() { }

    public static int playerInventoryLeft(int screenWidth, boolean hasActiveEffects, boolean hideEffects) {
        int centered = (screenWidth - INVENTORY_WIDTH) / 2;
        return centered + (hasActiveEffects && !hideEffects ? POTION_PANEL_WIDTH : 0);
    }

    public static int playerInventoryTop(int screenHeight) {
        return (screenHeight - INVENTORY_HEIGHT) / 2;
    }

    public static int chestLeft(int screenWidth) {
        return (screenWidth - INVENTORY_WIDTH) / 2;
    }

    public static int chestTop(int screenHeight, int chestRows) {
        return (screenHeight - (CHEST_BASE_HEIGHT + Math.max(0, chestRows) * SLOT_SIZE)) / 2;
    }
}
