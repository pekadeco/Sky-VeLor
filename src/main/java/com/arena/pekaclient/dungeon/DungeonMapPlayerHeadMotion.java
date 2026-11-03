package com.arena.pekaclient.dungeon;

/**
 * Small render-thread state for one dungeon-map player face. Map decorations arrive as discrete coordinates; keeping
 * a previous and current value lets the overlay use Forge's partial tick between those updates without changing any
 * server/player state.
 */
final class DungeonMapPlayerHeadMotion {
    private float previousX;
    private float previousY;
    private float currentX;
    private float currentY;
    private boolean initialized;

    /** Starts at the marker position so a newly visible party member never slides in from an unrelated coordinate. */
    void update(float mapX, float mapY) {
        if (!initialized) {
            previousX = currentX = mapX;
            previousY = currentY = mapY;
            initialized = true;
            return;
        }
        if (currentX == mapX && currentY == mapY) return;
        // The next partial-tick sample moves from the last authoritative marker coordinate to this new one.
        previousX = currentX;
        previousY = currentY;
        currentX = mapX;
        currentY = mapY;
    }

    /** Called once per client tick so a completed interpolation becomes the next tick's stable start. */
    void advanceTick() {
        if (!initialized) return;
        previousX = currentX;
        previousY = currentY;
    }

    float x(float partialTicks) {
        return previousX + (currentX - previousX) * clampPartial(partialTicks);
    }

    float y(float partialTicks) {
        return previousY + (currentY - previousY) * clampPartial(partialTicks);
    }

    /** Keeps the center of a face fully inside the fixed 128x128 map viewport after projection/zoom. */
    static float clampCenter(float coordinate, float headSize) {
        float half = Math.max(0.0F, Math.min(64.0F, Math.max(0.0F, headSize) * 0.5F));
        return Math.max(half, Math.min(128.0F - half, coordinate));
    }

    private static float clampPartial(float partialTicks) {
        return Math.max(0.0F, Math.min(1.0F, partialTicks));
    }
}
