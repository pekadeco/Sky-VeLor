package com.arena.pekaclient.dungeon;

/** Keeps current-run local/synced puzzle labels bounded even if a worker repeatedly sends new coordinates. */
public final class PuzzleRoomMemoryTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        boundedMemoryEvictsOldestRoom();
        higherConfidenceUpdatesSameRoom();
        done();
    }

    private static void boundedMemoryEvictsOldestRoom() {
        PuzzleRoomMemory memory = new PuzzleRoomMemory();
        for (int index = 0; index < 65; index++) {
            memory.remember("water", index * 32, 70, 0, .9D);
        }
        check("puzzle memory caps current-run labels", memory.size() == 64 && !contains(memory, 0)
                && contains(memory, 64 * 32));
    }

    private static void higherConfidenceUpdatesSameRoom() {
        PuzzleRoomMemory memory = new PuzzleRoomMemory();
        memory.remember("water", 0, 70, 0, .70D);
        memory.remember("box", 0, 70, 0, .90D);
        memory.remember("silverfish", 0, 70, 0, .80D);
        check("puzzle memory keeps the highest-confidence room label", memory.size() == 1
                && "box".equals(memory.snapshot().get(0).name));
    }

    private static boolean contains(PuzzleRoomMemory memory, int x) {
        for (PuzzleRoomMemory.RoomLabel label : memory.snapshot()) {
            if (label.originX == x) return true;
        }
        return false;
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("ok   " + name);
        } else {
            failed++;
            System.err.println("FAIL " + name);
        }
    }

    private static void done() {
        System.out.println("puzzle memory: passed " + passed + ", failed " + failed);
        if (failed != 0) throw new AssertionError(failed + " puzzle-memory checks failed");
    }
}
