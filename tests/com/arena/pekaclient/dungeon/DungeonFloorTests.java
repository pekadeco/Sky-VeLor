package com.arena.pekaclient.dungeon;

import java.util.Arrays;

/** Tests the local parser used by DungeonProgressTracker for sidebar/tab floor text. */
public final class DungeonFloorTests {
    public static void main(String[] args) {
        if (!"M3".equals(DungeonFloor.detect(Arrays.asList("§aMaster Mode 3")))) throw new AssertionError("master mode");
        if (!"M6".equals(DungeonFloor.detect(Arrays.asList("Dungeon (M6)")))) throw new AssertionError("master code");
        if (!"F7".equals(DungeonFloor.detect(Arrays.asList("Catacombs (F7)")))) throw new AssertionError("floor code");
        if (!"E0".equals(DungeonFloor.detect(Arrays.asList("Entrance")))) throw new AssertionError("entrance");
        if (!"--".equals(DungeonFloor.detect(Arrays.asList("lobby")))) throw new AssertionError("unknown");
        System.out.println("dungeon floor parser passed");
    }
}
