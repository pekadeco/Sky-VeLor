package com.arena.pekaclient.dungeonesp;

/** Sample closed/open 3 x 4 x 3 layouts for the cached wither and blood door detector. */
public final class WitherDoorCacheTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        findsBothClosedDoorMaterials();
        openedDoorInvalidatesOnNextTimerScan();
        nearestAndAllCacheSemantics();
        done();
    }

    private static void findsBothClosedDoorMaterials() {
        Grid grid = new Grid(-8, 12, 60, 80, -8, 12);
        fillDoor(grid, 0, 68, 0, DoorBlockKind.COAL);
        fillDoor(grid, 6, 68, 0, DoorBlockKind.RED_CLAY);
        WitherDoorCache cache = new WitherDoorCache();
        cache.scan(grid, -2, 65, -2, 8, 72, 4);
        check("coal prism is wither door", cache.size() == 2 && has(cache, 0, 68, 0, WitherDoorCache.Type.WITHER));
        check("red clay prism is blood door", has(cache, 6, 68, 0, WitherDoorCache.Type.BLOOD));
    }

    private static void openedDoorInvalidatesOnNextTimerScan() {
        Grid grid = new Grid(-8, 12, 60, 80, -8, 12);
        fillDoor(grid, 0, 68, 0, DoorBlockKind.COAL);
        WitherDoorCache cache = new WitherDoorCache();
        cache.scan(grid, -2, 65, -2, 4, 72, 4);
        grid.set(1, 69, 1, DoorBlockKind.AIR);
        cache.scan(grid, -2, 65, -2, 4, 72, 4);
        check("opened block invalidates cached door", cache.size() == 0);
    }

    private static void nearestAndAllCacheSemantics() {
        Grid grid = new Grid(-8, 24, 60, 80, -8, 8);
        fillDoor(grid, 0, 68, 0, DoorBlockKind.COAL);
        fillDoor(grid, 12, 68, 0, DoorBlockKind.COAL);
        fillDoor(grid, 18, 68, 0, DoorBlockKind.RED_CLAY);
        WitherDoorCache cache = new WitherDoorCache();
        cache.scan(grid, -2, 65, -2, 20, 72, 4);
        WitherDoorCache.Door nearest = cache.nearest(0D, 68D, 0D, true);
        WitherDoorCache.Door withoutBlood = cache.nearest(19D, 68D, 0D, false);
        check("all scope retains every closed door", cache.size() == 3);
        check("nearest scope selects closest closed door", nearest != null && nearest.x == 0);
        check("blood visibility filter honors key/scope consumer", withoutBlood != null && withoutBlood.type == WitherDoorCache.Type.WITHER);
    }

    private static boolean has(WitherDoorCache cache, int x, int y, int z, WitherDoorCache.Type type) {
        for (int i = 0; i < cache.size(); i++) {
            WitherDoorCache.Door door = cache.at(i);
            if (door.x == x && door.y == y && door.z == z && door.type == type) return true;
        }
        return false;
    }

    private static void fillDoor(Grid grid, int x, int y, int z, DoorBlockKind kind) {
        for (int yy = 0; yy < WitherDoorCache.DOOR_HEIGHT; yy++)
            for (int xx = 0; xx < WitherDoorCache.DOOR_WIDTH; xx++)
                for (int zz = 0; zz < WitherDoorCache.DOOR_DEPTH; zz++) grid.set(x + xx, y + yy, z + zz, kind);
    }

    private static final class Grid implements WitherDoorCache.BlockSource {
        private final int minX, minY, minZ, sizeX, sizeY, sizeZ;
        private final DoorBlockKind[] blocks;
        Grid(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
            this.minX = minX; this.minY = minY; this.minZ = minZ;
            sizeX = maxX - minX + 1; sizeY = maxY - minY + 1; sizeZ = maxZ - minZ + 1;
            blocks = new DoorBlockKind[sizeX * sizeY * sizeZ];
            for (int i = 0; i < blocks.length; i++) blocks[i] = DoorBlockKind.AIR;
        }
        void set(int x, int y, int z, DoorBlockKind kind) { int i = index(x, y, z); if (i >= 0) blocks[i] = kind; }
        @Override public DoorBlockKind blockAt(int x, int y, int z) { int i = index(x, y, z); return i < 0 ? DoorBlockKind.AIR : blocks[i]; }
        private int index(int x, int y, int z) {
            int xx = x - minX, yy = y - minY, zz = z - minZ;
            return xx < 0 || yy < 0 || zz < 0 || xx >= sizeX || yy >= sizeY || zz >= sizeZ ? -1 : (yy * sizeZ + zz) * sizeX + xx;
        }
    }

    private static void check(String name, boolean ok) { if (ok) passed++; else { failed++; System.out.println("FAIL " + name); } }
    private static void done() { System.out.println("wither door cache: passed " + passed + ", failed " + failed); if (failed > 0) System.exit(1); }
}
