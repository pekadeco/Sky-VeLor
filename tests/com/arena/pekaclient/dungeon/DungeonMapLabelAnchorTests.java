package com.arena.pekaclient.dungeon;

public final class DungeonMapLabelAnchorTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        float[] point = new float[2];
        check("room center includes the 16-block room-center offset",
                DungeonMapLabelAnchor.projectRoomCenter(32, 64, 32.0D, 64.0D,
                        64.0F, 64.0F, 0.5F, point)
                        && close(point[0], 72.0F) && close(point[1], 72.0F));

        check("rooms east of the player project to the right",
                DungeonMapLabelAnchor.projectRoomCenter(32, 64, 0.0D, 64.0D,
                        64.0F, 64.0F, 0.5F, point) && point[0] > 64.0F);
        check("rooms west of the player project to the left",
                DungeonMapLabelAnchor.projectRoomCenter(-32, 64, 0.0D, 64.0D,
                        64.0F, 64.0F, 0.5F, point) && point[0] < 64.0F);
        check("rooms south of the player project downward",
                DungeonMapLabelAnchor.projectRoomCenter(32, 64, 32.0D, 0.0D,
                        64.0F, 64.0F, 0.5F, point) && point[1] > 64.0F);
        check("rooms north of the player project upward",
                DungeonMapLabelAnchor.projectRoomCenter(32, -32, 32.0D, 0.0D,
                        64.0F, 64.0F, 0.5F, point) && point[1] < 64.0F);

        float initialX = projectX(32, 0.0D, 64.0F);
        float movedX = projectX(32, 8.0D, 68.0F);
        check("anchor remains stable when player and marker move together", close(initialX, movedX));
        float freshX = projectX(32, 8.0D, 72.0F);
        check("anchor corrects when a stale marker catches up", !close(movedX, freshX));
        check("non-finite values are rejected",
                !DungeonMapLabelAnchor.projectRoomCenter(0, 0, Double.NaN, 0.0D,
                        64.0F, 64.0F, 0.5F, point));
        check("zero scale is rejected",
                !DungeonMapLabelAnchor.projectRoomCenter(0, 0, 0.0D, 0.0D,
                        64.0F, 64.0F, 0.0F, point));

        byte[] mapColors = new byte[128 * 128];
        // A 20x18 purple room, with different brightness variants from Minecraft's map palette.
        for (int y = 50; y < 68; y++) {
            for (int x = 40; x < 60; x++) {
                mapColors[y * 128 + x] = (byte) (96 + ((x + y) & 3));
            }
        }
        // A tiny purple decoration must not be mistaken for an entire room.
        mapColors[5 * 128 + 5] = (byte) 98;
        mapColors[5 * 128 + 6] = (byte) 98;
        mapColors[6 * 128 + 5] = (byte) 98;
        mapColors[6 * 128 + 6] = (byte) 98;
        point[0] = -1.0F;
        point[1] = -1.0F;
        check("purple room label snaps to actual map-box center",
                DungeonMapLabelAnchor.snapToPurpleRoomCenter(mapColors, 53.0F, 62.0F, point)
                        && close(point[0], 50.0F) && close(point[1], 59.0F));
        point[0] = 17.0F;
        point[1] = 19.0F;
        check("purple room snap rejects unrelated distant components",
                !DungeonMapLabelAnchor.snapToPurpleRoomCenter(mapColors, 100.0F, 100.0F, point)
                        && close(point[0], 17.0F) && close(point[1], 19.0F));
        check("purple room snap rejects incomplete map data",
                !DungeonMapLabelAnchor.snapToPurpleRoomCenter(new byte[32], 50.0F, 59.0F, point));
        check("purple room snap rejects invalid coordinates",
                !DungeonMapLabelAnchor.snapToPurpleRoomCenter(mapColors, Float.NaN, 59.0F, point));

        System.out.println("DungeonMapLabelAnchorTests: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static float projectX(int originX, double playerX, float markerX) {
        float[] point = new float[2];
        if (!DungeonMapLabelAnchor.projectRoomCenter(originX, 0, playerX, 0.0D,
                markerX, 64.0F, 0.5F, point)) throw new AssertionError("Expected valid anchor");
        return point[0];
    }

    private static boolean close(float a, float b) { return Math.abs(a - b) < 0.001F; }
    private static void check(String name, boolean condition) {
        if (condition) passed++;
        else { failed++; System.out.println("FAIL: " + name); }
    }
}
