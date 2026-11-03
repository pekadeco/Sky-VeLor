package com.arena.pekaclient.dungeonesp;

/**
 * Cached, timer-driven detector for closed dungeon doors. A closed wither door is a 3 x 4 x 3 coal-block prism; a
 * blood door uses the same prism in red stained clay. The scanner checks a bounded area around the player only on a
 * caller-controlled tick timer, and cached doors are revalidated before each refresh so opened doors disappear.
 */
public final class WitherDoorCache {

    public static final int DOOR_WIDTH = 3;
    public static final int DOOR_HEIGHT = 4;
    public static final int DOOR_DEPTH = 3;
    public static final int DEFAULT_RADIUS = 48;
    public static final int MAX_DOORS = 128;

    public enum Type {
        WITHER,
        BLOOD
    }

    public interface BlockSource {
        DoorBlockKind blockAt(int x, int y, int z);
    }

    /** Preallocated cache entry. Coordinates are the inclusive lower/north-west corner of the 3 x 4 x 3 prism. */
    public static final class Door {
        public int x;
        public int y;
        public int z;
        public Type type = Type.WITHER;
        int seenGeneration;

        private void set(int newX, int newY, int newZ, Type newType, int generation) {
            x = newX;
            y = newY;
            z = newZ;
            type = newType;
            seenGeneration = generation;
        }
    }

    private final Door[] doors = new Door[MAX_DOORS];
    private int count;
    private int generation;

    public WitherDoorCache() {
        for (int i = 0; i < doors.length; i++) doors[i] = new Door();
    }

    /**
     * Searches a 97 x 13 x 97 box centered on the player by default. The broad vertical window handles stairs and
     * different room elevations while avoiding a full-world block traversal. Existing cached doors are checked even
     * when outside this local window, so a remotely opened door cannot stay stale after the next timer scan.
     */
    public void scanAround(BlockSource source, int centerX, int centerY, int centerZ) {
        scan(source, centerX - DEFAULT_RADIUS, centerY - 8, centerZ - DEFAULT_RADIUS,
                centerX + DEFAULT_RADIUS, centerY + 4, centerZ + DEFAULT_RADIUS);
    }

    /** Visible for deterministic tests and future room-bounded callers. Bounds are inclusive lower/upper base positions. */
    public void scan(BlockSource source, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        generation++;
        if (generation <= 0) generation = 1;
        if (source == null) {
            clear();
            return;
        }
        invalidateOpened(source);
        if (minX > maxX || minY > maxY || minZ > maxZ) return;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    DoorBlockKind kind = source.blockAt(x, y, z);
                    Type type = typeFor(kind);
                    if (type == null || !closedPrism(source, x, y, z, kind)) continue;
                    touch(x, y, z, type);
                }
            }
        }
    }

    public void clear() {
        count = 0;
        generation = 0;
    }

    public int size() {
        return count;
    }

    public Door at(int index) {
        return index >= 0 && index < count ? doors[index] : null;
    }

    /** Returns the closest cached door allowed by the caller, without creating a temporary collection. */
    public Door nearest(double x, double y, double z, boolean includeBlood) {
        Door best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            Door door = doors[i];
            if (!includeBlood && door.type == Type.BLOOD) continue;
            double dx = door.x + 1.5D - x;
            double dy = door.y + 2D - y;
            double dz = door.z + 1.5D - z;
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = door;
            }
        }
        return best;
    }

    /** Exact closed-state check used by the render adapter before drawing cached positions. */
    public static boolean isClosed(BlockSource source, Door door) {
        if (source == null || door == null) return false;
        return closedPrism(source, door.x, door.y, door.z,
                door.type == Type.WITHER ? DoorBlockKind.COAL : DoorBlockKind.RED_CLAY);
    }

    private void invalidateOpened(BlockSource source) {
        int i = 0;
        while (i < count) {
            Door door = doors[i];
            if (isClosed(source, door)) {
                door.seenGeneration = generation;
                i++;
            } else {
                removeAt(i);
            }
        }
    }

    private void touch(int x, int y, int z, Type type) {
        for (int i = 0; i < count; i++) {
            Door existing = doors[i];
            if (existing.x == x && existing.y == y && existing.z == z) {
                existing.set(x, y, z, type, generation);
                return;
            }
        }
        if (count < doors.length) {
            doors[count++].set(x, y, z, type, generation);
        }
    }

    private void removeAt(int index) {
        int last = count - 1;
        if (index != last) {
            Door from = doors[last];
            doors[index].set(from.x, from.y, from.z, from.type, from.seenGeneration);
        }
        count--;
    }

    private static Type typeFor(DoorBlockKind kind) {
        if (kind == DoorBlockKind.COAL) return Type.WITHER;
        if (kind == DoorBlockKind.RED_CLAY) return Type.BLOOD;
        return null;
    }

    private static boolean closedPrism(BlockSource source, int x, int y, int z, DoorBlockKind expected) {
        for (int yy = 0; yy < DOOR_HEIGHT; yy++) {
            for (int xx = 0; xx < DOOR_WIDTH; xx++) {
                for (int zz = 0; zz < DOOR_DEPTH; zz++) {
                    if (source.blockAt(x + xx, y + yy, z + zz) != expected) return false;
                }
            }
        }
        return true;
    }
}
