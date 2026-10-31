package com.arena.pekaclient.dungeon;

/** Projects a 32x32 dungeon room center from world coordinates into the live map texture. */
final class DungeonMapLabelAnchor {
    private static final float ROOM_SIZE = 32.0F;
    private static final float ROOM_CENTER = ROOM_SIZE * 0.5F;
    private static final int MAP_SIZE = 128;
    private static final int MAP_PIXELS = MAP_SIZE * MAP_SIZE;
    // Minecraft 1.8.9 MapColor.colorPurple has palette index 24 (four brightness variants).
    private static final int PURPLE_MAP_COLOR = 24;
    private static final int MIN_PURPLE_COMPONENT_PIXELS = 12;
    private static final int MAX_PURPLE_COMPONENT_SIZE = 32;
    private static final float MAX_PURPLE_CENTER_DISTANCE = 18.0F;

    private DungeonMapLabelAnchor() { }

    /**
     * Projects a room center relative to a local-player marker sample.
     * Positive world X moves right on the map; positive world Z moves down.
     * The resulting texture-space anchor should be captured once per map-data instance: map-marker
     * updates arrive in quantized steps, so recomputing from live player movement every frame makes
     * a room label slide and snap even though its room tile is fixed on the map.
     */
    static boolean projectRoomCenter(int originX, int originZ, double playerWorldX, double playerWorldZ,
                                     float playerMapX, float playerMapY, float pixelsPerBlock, float[] output) {
        if (output == null || output.length < 2 || !finite(playerWorldX) || !finite(playerWorldZ)
                || !finite(playerMapX) || !finite(playerMapY) || !finite(pixelsPerBlock)
                || pixelsPerBlock <= 0.0F) {
            return false;
        }

        float roomCenterX = originX + ROOM_CENTER;
        float roomCenterZ = originZ + ROOM_CENTER;
        double mapX = playerMapX + (roomCenterX - playerWorldX) * pixelsPerBlock;
        double mapY = playerMapY + (roomCenterZ - playerWorldZ) * pixelsPerBlock;
        if (!finite(mapX) || !finite(mapY) || Math.abs(mapX) > 4096.0D || Math.abs(mapY) > 4096.0D) {
            return false;
        }
        output[0] = (float) mapX;
        output[1] = (float) mapY;
        return true;
    }

    /**
     * Snaps a projected room-center estimate to the actual purple room box in the map texture.
     * The world-to-map scale is inferred from the dungeon map and can be off by a few pixels;
     * centering the label on the map's own purple component avoids that accumulated drift.
     *
     * @return true if a nearby purple room component was found and output was updated
     */
    static boolean snapToPurpleRoomCenter(byte[] colors, float expectedX, float expectedY, float[] output) {
        if (colors == null || colors.length < MAP_PIXELS || output == null || output.length < 2
                || !finite(expectedX) || !finite(expectedY)) {
            return false;
        }

        boolean[] visited = new boolean[MAP_PIXELS];
        int[] queue = new int[MAP_PIXELS];
        float bestDistanceSquared = MAX_PURPLE_CENTER_DISTANCE * MAX_PURPLE_CENTER_DISTANCE;
        float bestCenterX = 0.0F;
        float bestCenterY = 0.0F;
        boolean found = false;

        for (int start = 0; start < MAP_PIXELS; start++) {
            if (visited[start] || !isPurpleMapPixel(colors[start])) continue;

            int head = 0;
            int tail = 0;
            queue[tail++] = start;
            visited[start] = true;
            int minX = MAP_SIZE;
            int minY = MAP_SIZE;
            int maxX = -1;
            int maxY = -1;
            int count = 0;

            while (head < tail) {
                int index = queue[head++];
                int x = index % MAP_SIZE;
                int y = index / MAP_SIZE;
                count++;
                if (x < minX) minX = x;
                if (x > maxX) maxX = x;
                if (y < minY) minY = y;
                if (y > maxY) maxY = y;

                if (x > 0) tail = enqueuePurple(colors, visited, queue, tail, index - 1);
                if (x + 1 < MAP_SIZE) tail = enqueuePurple(colors, visited, queue, tail, index + 1);
                if (y > 0) tail = enqueuePurple(colors, visited, queue, tail, index - MAP_SIZE);
                if (y + 1 < MAP_SIZE) tail = enqueuePurple(colors, visited, queue, tail, index + MAP_SIZE);
            }

            int width = maxX - minX + 1;
            int height = maxY - minY + 1;
            if (count < MIN_PURPLE_COMPONENT_PIXELS || width < 4 || height < 4
                    || width > MAX_PURPLE_COMPONENT_SIZE || height > MAX_PURPLE_COMPONENT_SIZE) {
                continue;
            }

            // Pixel coordinates describe pixel origins, so add one before halving the bounds.
            float centerX = (minX + maxX + 1.0F) * 0.5F;
            float centerY = (minY + maxY + 1.0F) * 0.5F;
            float dx = centerX - expectedX;
            float dy = centerY - expectedY;
            float distanceSquared = dx * dx + dy * dy;
            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
                bestCenterX = centerX;
                bestCenterY = centerY;
                found = true;
            }
        }

        if (!found) return false;
        output[0] = bestCenterX;
        output[1] = bestCenterY;
        return true;
    }

    private static int enqueuePurple(byte[] colors, boolean[] visited, int[] queue, int tail, int index) {
        if (!visited[index] && isPurpleMapPixel(colors[index])) {
            visited[index] = true;
            queue[tail++] = index;
        }
        return tail;
    }

    private static boolean isPurpleMapPixel(byte color) {
        return ((color & 0xFF) >> 2) == PURPLE_MAP_COLOR;
    }

    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}
