package com.arena.pekaclient.dungeon;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Remembers puzzle rooms discovered during the current dungeon run.
 * Room identity is based on world block coordinates, not the player's current cell or map pixel.
 */
public final class PuzzleRoomMemory {
    /** A full dungeon has far fewer puzzle rooms; this also bounds hostile/buggy sync input for one world session. */
    private static final int MAX_ROOMS = 64;
    private final Map<RoomKey, RoomLabel> rooms = new LinkedHashMap<RoomKey, RoomLabel>();

    public void remember(String name, int originX, int originY, int originZ, double confidence) {
        if (name == null || name.trim().isEmpty() || confidence < 0.65D) return;
        RoomKey key = new RoomKey(originX, originY, originZ);
        RoomLabel existing = rooms.get(key);
        if (existing != null && existing.confidence > confidence) return;
        if (existing == null && rooms.size() >= MAX_ROOMS) {
            java.util.Iterator<RoomKey> oldest = rooms.keySet().iterator();
            if (oldest.hasNext()) {
                oldest.next();
                oldest.remove();
            }
        }
        rooms.put(key, new RoomLabel(name.trim(), originX, originY, originZ, confidence));
    }

    /** Snapshot keeps rendering independent from mutations made by the client tick. */
    public List<RoomLabel> snapshot() {
        return new ArrayList<RoomLabel>(rooms.values());
    }

    public int size() { return rooms.size(); }

    public void clear() { rooms.clear(); }

    public static final class RoomLabel {
        public final String name;
        public final int originX;
        public final int originY;
        public final int originZ;
        public final double confidence;

        private RoomLabel(String name, int originX, int originY, int originZ, double confidence) {
            this.name = name;
            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;
            this.confidence = confidence;
        }
    }

    private static final class RoomKey {
        private final int x;
        private final int y;
        private final int z;

        private RoomKey(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof RoomKey)) return false;
            RoomKey key = (RoomKey) other;
            return x == key.x && y == key.y && z == key.z;
        }

        @Override public int hashCode() {
            int result = x;
            result = 31 * result + y;
            result = 31 * result + z;
            return result;
        }
    }
}
