package com.arena.pekaclient.feature.inventory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class SlotLockStoreTests {
    private SlotLockStoreTests() { }

    public static void main(String[] args) throws Exception {
        testMissingFileStartsEmpty();
        testLegacyFileMigratesOnlyOnCurrentWrite();
        testCurrentFileWinsOverLegacyFallback();
        testLocksSurviveReload();
        testMultipleItemsCanShareHotbarBinding();
        testMultipleIndependentBindingsPersistTogether();
        testAllInventoryAndArmorSourcesCanBeBound();
        testReciprocalBindingMatchesBetterNef();
        testLegacyOneWayBindingIsNormalizedOnLoad();
        testBoundSourcesAreAutomaticallyLocked();
        testArmorBindingsPersist();
        testLockedSlotsCannotBeBound();
        testBindingGestureAutomaticallyLocksItsSource();
        testBindingGestureRejectsLockedTarget();
        testLockingABindingEndpointClearsTheBinding();
        testMalformedFileFailsClosedAndIsPreserved();
        testValidRecoveryFileIsUsed();
        testMalformedRecoveryFileFailsClosed();
        System.out.println("SlotLockStoreTests: all tests passed");
    }

    private static void testMissingFileStartsEmpty() throws Exception {
        File root = Files.createTempDirectory("lunaria-slot-empty").toFile();
        SlotLockStore store = new SlotLockStore();
        store.load(new File(root, "slot-locks.properties"));
        assertTrue(!store.hasFailed(), "missing settings file should use safe defaults");
        assertEquals(0, store.lockedCount(), "fresh store should have no locked slots");
        assertEquals(0, store.bindingCount(), "fresh store should have no bindings");
        delete(root);
    }

    /** Legacy slot data is a read-only input: state is retained, then a user mutation writes only Lunaria's file. */
    private static void testLegacyFileMigratesOnlyOnCurrentWrite() throws Exception {
        File root = Files.createTempDirectory("lunaria-slot-legacy-migration").toFile();
        File current = new File(root, "config/lunaria/slot-locks.properties");
        File legacy = new File(root, "config/skyvelor/slot-locks.properties");
        legacy.getParentFile().mkdirs();
        String before = "version=1\nlocked.3=true\nbound.12=4\n";
        write(legacy, before);

        SlotLockStore store = new SlotLockStore();
        store.load(current, legacy);
        assertTrue(!store.hasFailed(), "valid legacy state should remain usable");
        assertTrue(store.isLocked(3), "legacy manual lock should load");
        assertEquals(4, store.getBoundTo(12), "legacy binding should load");
        assertTrue(!current.exists(), "reading legacy data must not copy or rewrite it at startup");
        assertEquals(before, new String(Files.readAllBytes(legacy.toPath()), StandardCharsets.UTF_8),
                "legacy source must remain byte-for-byte unchanged");

        assertEquals(SlotLockStore.ChangeResult.LOCKED, store.toggleLock(5),
                "a new mutation should save the normalized current target");
        assertTrue(current.isFile(), "the first mutation should create the current Lunaria target");
        assertEquals(before, new String(Files.readAllBytes(legacy.toPath()), StandardCharsets.UTF_8),
                "writing the current target must never alter the legacy source");
        SlotLockStore reloaded = new SlotLockStore();
        reloaded.load(current, legacy);
        assertTrue(reloaded.isLocked(3) && reloaded.isLocked(5), "migrated state should survive reload");
        assertEquals(4, reloaded.getBoundTo(12), "migrated binding should survive reload");
        delete(root);
    }

    private static void testCurrentFileWinsOverLegacyFallback() throws Exception {
        File root = Files.createTempDirectory("lunaria-slot-current-wins").toFile();
        File current = new File(root, "config/lunaria/slot-locks.properties");
        File legacy = new File(root, "config/skyvelor/slot-locks.properties");
        current.getParentFile().mkdirs();
        legacy.getParentFile().mkdirs();
        write(current, "version=1\nlocked.1=true\n");
        write(legacy, "version=1\nlocked.2=true\n");
        SlotLockStore store = new SlotLockStore();
        store.load(current, legacy);
        assertTrue(store.isLocked(1), "current target must be authoritative when it exists");
        assertTrue(!store.isLocked(2), "legacy fallback must not merge over a current target");
        delete(root);
    }

    private static void testLocksSurviveReload() throws Exception {
        File root = Files.createTempDirectory("lunaria-slot-roundtrip").toFile();
        File file = new File(root, "slot-locks.properties");
        SlotLockStore store = new SlotLockStore();
        store.load(file);
        assertEquals(SlotLockStore.ChangeResult.LOCKED, store.toggleLock(0), "hotbar slot should lock");
        assertEquals(SlotLockStore.ChangeResult.LOCKED, store.toggleLock(39), "armor slot should lock");
        assertTrue(file.isFile(), "settings should be saved to disk");
        String saved = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        assertTrue(saved.contains("version=1"), "saved file should include a schema version");
        assertTrue(saved.contains("locked.0=true"), "saved file should be human-readable");

        SlotLockStore loaded = new SlotLockStore();
        loaded.load(file);
        assertTrue(loaded.isLocked(0), "hotbar lock should survive restart");
        assertTrue(loaded.isLocked(39), "armor lock should survive restart");
        assertEquals(SlotLockStore.ChangeResult.UNLOCKED, loaded.toggleLock(0), "hotbar slot should unlock");
        delete(root);
    }

    private static void testMultipleItemsCanShareHotbarBinding() throws Exception {
        File root = Files.createTempDirectory("lunaria-slot-multibind").toFile();
        File file = new File(root, "slot-locks.properties");
        SlotLockStore store = new SlotLockStore();
        store.load(file);
        assertEquals(SlotLockStore.ChangeResult.BOUND, store.bind(9, 2), "first source should bind to hotbar");
        assertEquals(SlotLockStore.ChangeResult.BOUND, store.bind(10, 2), "second source should coexist on the same hotbar");
        assertEquals(2, store.getBoundTo(9), "first source should keep its target");
        assertEquals(2, store.getBoundTo(10), "second source should keep the same target");
        assertEquals(10, store.getBoundTo(2), "the hotbar should prefer the most recently bound source");
        assertEquals(2, store.bindingCount(), "each source should count as a separate binding");
        assertTrue(store.isLocked(9) && store.isLocked(10) && store.isLocked(2),
                "all linked sources and their hotbar target should be automatically locked");
        assertTrue(!store.isExplicitlyLocked(9) && !store.isExplicitlyLocked(10) && !store.isExplicitlyLocked(2),
                "binding must not masquerade as a manual lock setting");
        assertEquals(10, store.getSourceForHotbar(2), "target click should select the preferred source");
        assertEquals(2, store.getSourcesForHotbar(2).length, "all sources should be discoverable from the target");

        SlotLockStore loaded = new SlotLockStore();
        loaded.load(file);
        assertEquals(2, loaded.bindingCount(), "all bindings should survive restart");
        assertEquals(2, loaded.getBoundTo(9), "first binding should persist");
        assertEquals(2, loaded.getBoundTo(10), "second binding should persist");
        assertEquals(10, loaded.getSourceForHotbar(2), "preferred source should persist");
        assertEquals(SlotLockStore.ChangeResult.UNBOUND, loaded.bind(10, 2), "unbinding one source should succeed");
        assertEquals(-1, loaded.getBoundTo(10), "unbound source should be cleared");
        assertEquals(2, loaded.getBoundTo(9), "unbinding one source must preserve the other one");
        assertEquals(9, loaded.getSourceForHotbar(2), "preferred source should fall back to the remaining item");
        assertEquals(1, loaded.bindingCount(), "only the remaining link should count");
        assertEquals(SlotLockStore.ChangeResult.UNBOUND, loaded.bind(9, 2), "last source should unbind cleanly");
        assertEquals(0, loaded.bindingCount(), "no bindings should remain");
        delete(root);
    }



    private static void testMultipleIndependentBindingsPersistTogether() throws Exception {
        File root = Files.createTempDirectory("lunaria-slot-multiple-bindings").toFile();
        File file = new File(root, "slot-locks.properties");
        SlotLockStore store = new SlotLockStore();
        store.load(file);
        assertEquals(SlotLockStore.ChangeResult.BOUND, store.bindFromLockGesture(9, 0),
                "first independent item should bind to hotbar slot 1");
        assertEquals(SlotLockStore.ChangeResult.BOUND, store.bindFromLockGesture(10, 1),
                "second independent item should bind to hotbar slot 2");
        assertEquals(SlotLockStore.ChangeResult.BOUND, store.bindFromLockGesture(11, 2),
                "third independent item should bind to hotbar slot 3");
        assertEquals(3, store.bindingCount(), "independent binding pairs should coexist");
        assertEquals(0, store.getBoundTo(9), "first source binding should be retained");
        assertEquals(9, store.getBoundTo(0), "first hotbar slot should reciprocally remember its source");
        assertEquals(1, store.getBoundTo(10), "second source binding should be retained");
        assertEquals(10, store.getBoundTo(1), "second hotbar slot should reciprocally remember its source");
        assertEquals(2, store.getBoundTo(11), "third source binding should be retained");
        assertEquals(11, store.getBoundTo(2), "third hotbar slot should reciprocally remember its source");
        assertEquals(9, store.getSourceForHotbar(0), "first hotbar target should resolve its source");
        assertEquals(10, store.getSourceForHotbar(1), "second hotbar target should resolve its source");
        assertEquals(11, store.getSourceForHotbar(2), "third hotbar target should resolve its source");

        SlotLockStore loaded = new SlotLockStore();
        loaded.load(file);
        assertEquals(3, loaded.bindingCount(), "all pairs should survive reload");
        assertTrue(loaded.isLocked(9) && loaded.isLocked(0), "first pair should lock both endpoints");
        assertTrue(loaded.isLocked(10) && loaded.isLocked(1), "second pair should lock both endpoints");
        assertTrue(loaded.isLocked(11) && loaded.isLocked(2), "third pair should lock both endpoints");
        delete(root);
    }

    private static void testAllInventoryAndArmorSourcesCanBeBound() throws Exception {
        File root = Files.createTempDirectory("lunaria-slot-full-capacity").toFile();
        File file = new File(root, "slot-locks.properties");
        SlotLockStore store = new SlotLockStore();
        store.load(file);
        for (int source = SlotLockStore.MAIN_INVENTORY_START; source < SlotLockStore.SLOT_COUNT; source++) {
            int target = (source - SlotLockStore.MAIN_INVENTORY_START) % SlotLockStore.HOTBAR_SIZE;
            assertEquals(SlotLockStore.ChangeResult.BOUND, store.bindFromLockGesture(source, target),
                    "every inventory/armor source should bind (source=" + source + ")");
        }
        assertEquals(SlotLockStore.SLOT_COUNT - SlotLockStore.MAIN_INVENTORY_START, store.bindingCount(),
                "binding capacity should cover every main-inventory and armor slot");
        for (int target = 0; target < SlotLockStore.HOTBAR_SIZE; target++) {
            assertTrue(store.getSourcesForHotbar(target).length >= 3,
                    "each hotbar should retain multiple item sources");
        }
        SlotLockStore loaded = new SlotLockStore();
        loaded.load(file);
        assertEquals(SlotLockStore.SLOT_COUNT - SlotLockStore.MAIN_INVENTORY_START, loaded.bindingCount(),
                "all full-capacity bindings should persist");
        delete(root);
    }

    private static void testReciprocalBindingMatchesBetterNef() throws Exception {
        File root = Files.createTempDirectory("lunaria-slot-reciprocal").toFile();
        File file = new File(root, "slot-locks.properties");
        SlotLockStore store = new SlotLockStore();
        store.load(file);
        assertEquals(SlotLockStore.ChangeResult.BOUND, store.bindFromLockGesture(21, 7),
                "binding gesture should create a pair");
        assertEquals(7, store.getBoundTo(21), "inventory source should point to hotbar");
        assertEquals(21, store.getBoundTo(7), "hotbar should point back to inventory source");
        assertEquals(21, store.getSourceForHotbar(7), "reverse lookup should use the reciprocal association");
        String saved = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        assertTrue(saved.contains("bound.21=7"), "source relation should be persisted");
        assertTrue(saved.contains("bound.7=21"), "reciprocal target relation should be persisted");
        SlotLockStore loaded = new SlotLockStore();
        loaded.load(file);
        assertEquals(7, loaded.getBoundTo(21), "source link should survive reload");
        assertEquals(21, loaded.getBoundTo(7), "reciprocal target link should survive reload");
        assertEquals(SlotLockStore.ChangeResult.UNBOUND, loaded.bind(21, 7), "repeating the same pair should unbind it");
        assertEquals(-1, loaded.getBoundTo(21), "source end should be cleared on unbind");
        assertEquals(-1, loaded.getBoundTo(7), "target end should be cleared on unbind");
        delete(root);
    }

    private static void testLegacyOneWayBindingIsNormalizedOnLoad() throws Exception {
        File root = Files.createTempDirectory("lunaria-slot-legacy-binding").toFile();
        File file = new File(root, "slot-locks.properties");
        write(file, "version=1\nbound.17=4\n");
        SlotLockStore store = new SlotLockStore();
        store.load(file);
        assertTrue(!store.hasFailed(), "older one-way settings should remain readable");
        assertEquals(4, store.getBoundTo(17), "legacy source link should be retained");
        assertEquals(17, store.getBoundTo(4), "legacy link should be normalized to a reciprocal pair");
        assertEquals(17, store.getSourceForHotbar(4), "normalized target should find the source");
        delete(root);
    }

    private static void testBoundSourcesAreAutomaticallyLocked() throws Exception {
        File root = Files.createTempDirectory("lunaria-bound-auto-lock").toFile();
        File file = new File(root, "slot-locks.properties");
        SlotLockStore store = new SlotLockStore();
        store.load(file);
        assertEquals(SlotLockStore.ChangeResult.BOUND, store.bindFromLockGesture(18, 6),
                "binding gesture should save a source-to-hotbar relation");
        assertTrue(store.isLocked(18), "bound source must be protected like a locked slot");
        assertTrue(!store.isExplicitlyLocked(18), "automatic lock must be derived from the binding");
        assertTrue(store.isLocked(6), "the hotbar endpoint must be protected too");
        assertEquals(2, store.lockedCount(), "effective lock count must include both bound endpoints");

        SlotLockStore loaded = new SlotLockStore();
        loaded.load(file);
        assertTrue(loaded.isLocked(18), "source automatic lock must survive reload through the saved binding");
        assertTrue(loaded.isLocked(6), "target automatic lock must survive reload through the saved binding");
        assertEquals(6, loaded.getBoundTo(18), "binding should survive reload with automatic lock");
        assertEquals(SlotLockStore.ChangeResult.LOCKED, loaded.toggleLock(18),
                "pressing the lock action on a bound source should replace the binding with an explicit lock");
        assertEquals(-1, loaded.getBoundTo(18), "explicitly locking the source should clear its binding");
        assertEquals(-1, loaded.getBoundTo(6), "explicitly locking the source should clear the target reference too");
        assertTrue(!loaded.isLocked(6), "removing the binding should release its automatic target lock");
        delete(root);
    }

    private static void testArmorBindingsPersist() throws Exception {
        File root = Files.createTempDirectory("lunaria-armor-binding").toFile();
        File file = new File(root, "slot-locks.properties");
        SlotLockStore store = new SlotLockStore();
        store.load(file);
        assertEquals(SlotLockStore.ChangeResult.BOUND, store.bindFromLockGesture(39, 4),
                "armor slot should bind to a hotbar slot");
        assertEquals(4, store.getBoundTo(39), "armor slot should persist its target");
        assertEquals(39, store.getSourceForHotbar(4), "hotbar lookup should find the armor source");
        assertEquals(1, store.bindingCount(), "armor binding should be included in the status count");
        assertEquals(SlotLockStore.ChangeResult.BOUND, store.bind(10, 4),
                "a main-inventory source should share a hotbar target with an armor source");
        assertEquals(4, store.getBoundTo(39), "the original armor binding should remain");
        assertEquals(10, store.getSourceForHotbar(4), "the target should prefer the newest source");
        assertEquals(2, store.bindingCount(), "both armor and main-inventory bindings should be counted");
        SlotLockStore loaded = new SlotLockStore();
        loaded.load(file);
        assertEquals(4, loaded.getBoundTo(10), "the main-inventory binding should survive reload");
        assertEquals(4, loaded.getBoundTo(39), "the armor binding should survive reload");
        assertEquals(10, loaded.getSourceForHotbar(4), "the preferred source should survive reload");
        assertEquals(SlotLockStore.ChangeResult.UNBOUND, loaded.bind(10, 4), "unbind should remove only the selected source");
        assertEquals(39, loaded.getSourceForHotbar(4), "the armor source should remain active after unbinding the main source");
        delete(root);
    }

    private static void testLockedSlotsCannotBeBound() throws Exception {
        File root = Files.createTempDirectory("lunaria-slot-lock-bind").toFile();
        File file = new File(root, "slot-locks.properties");
        SlotLockStore store = new SlotLockStore();
        store.load(file);
        store.toggleLock(9);
        assertEquals(SlotLockStore.ChangeResult.LOCKED_SLOT, store.bind(9, 2), "locked source cannot be bound");
        store.toggleLock(9);
        store.toggleLock(39);
        assertEquals(SlotLockStore.ChangeResult.LOCKED_SLOT, store.bind(39, 2),
                "locked armor source cannot be bound through the ordinary bind path");
        store.toggleLock(39);
        store.toggleLock(2);
        assertEquals(SlotLockStore.ChangeResult.LOCKED_SLOT, store.bind(9, 2), "locked target cannot be bound");
        delete(root);
    }

    private static void testBindingGestureAutomaticallyLocksItsSource() throws Exception {
        File root = Files.createTempDirectory("lunaria-slot-bind-gesture").toFile();
        File file = new File(root, "slot-locks.properties");
        SlotLockStore store = new SlotLockStore();
        store.load(file);
        assertEquals(SlotLockStore.ChangeResult.LOCKED, store.toggleLock(14), "key-down toggles source first");
        assertTrue(store.isLocked(14), "source should reflect the initial key-down toggle");
        assertEquals(SlotLockStore.ChangeResult.BOUND, store.bindFromLockGesture(14, 5),
                "binding gesture should complete even when key-down temporarily locked its source");
        assertTrue(store.isLocked(14), "successfully bound source should be automatically locked");
        assertTrue(!store.isExplicitlyLocked(14), "the automatic lock should come from the binding");
        assertEquals(5, store.getBoundTo(14), "source should retain the hotbar target");
        assertEquals(14, store.getSourceForHotbar(5), "reverse target lookup should resolve the source");
        assertEquals(SlotLockStore.ChangeResult.BOUND, store.bindFromLockGesture(16, 5),
                "a second source should share the target instead of replacing the first");
        assertEquals(5, store.getBoundTo(14), "the previous source must remain bound after adding another item");
        assertEquals(5, store.getBoundTo(16), "the second source should keep the same target");
        assertEquals(16, store.getSourceForHotbar(5), "the most recent source should be preferred for target-side swap");
        SlotLockStore loaded = new SlotLockStore();
        loaded.load(file);
        assertEquals(5, loaded.getBoundTo(14), "first gesture binding must persist through reload");
        assertEquals(5, loaded.getBoundTo(16), "second gesture binding must persist through reload");
        assertTrue(loaded.isLocked(14) && loaded.isLocked(16), "all persisted sources should be automatically locked");
        assertTrue(!loaded.isExplicitlyLocked(16), "persisted automatic lock must not become a manual lock");
        delete(root);
    }

    private static void testBindingGestureRejectsLockedTarget() throws Exception {
        File root = Files.createTempDirectory("lunaria-slot-bind-locked-target").toFile();
        File file = new File(root, "slot-locks.properties");
        SlotLockStore store = new SlotLockStore();
        store.load(file);
        store.toggleLock(5);
        store.toggleLock(14);
        assertEquals(SlotLockStore.ChangeResult.LOCKED_SLOT, store.bindFromLockGesture(14, 5),
                "binding gesture must not override a locked hotbar target");
        assertTrue(store.isLocked(14), "failed binding should preserve the source's key-down toggle");
        assertTrue(store.isLocked(5), "failed binding should preserve target lock");
        delete(root);
    }

    private static void testLockingABindingEndpointClearsTheBinding() throws Exception {
        File root = Files.createTempDirectory("lunaria-slot-lock-bound-endpoint").toFile();
        File file = new File(root, "slot-locks.properties");
        SlotLockStore store = new SlotLockStore();
        store.load(file);
        store.bind(9, 2);
        store.bind(10, 2);
        assertEquals(SlotLockStore.ChangeResult.LOCKED, store.toggleLock(2), "hotbar endpoint should lock");
        assertEquals(-1, store.getBoundTo(9), "locking a hotbar endpoint must clear the first source binding");
        assertEquals(-1, store.getBoundTo(10), "locking a hotbar endpoint must clear every source binding");
        assertEquals(-1, store.getBoundTo(2), "locking a hotbar endpoint must clear the reciprocal side too");
        store.toggleLock(2);
        store.bind(9, 2);
        assertEquals(SlotLockStore.ChangeResult.LOCKED, store.toggleLock(9), "source endpoint should lock");
        assertEquals(-1, store.getBoundTo(9), "locking a source endpoint must clear its binding");
        assertEquals(-1, store.getBoundTo(2), "locking a source endpoint must clear the reciprocal side too");
        store.toggleLock(9);
        store.bindFromLockGesture(39, 2);
        assertEquals(SlotLockStore.ChangeResult.LOCKED, store.toggleLock(2), "hotbar target paired to armor should lock");
        assertEquals(-1, store.getBoundTo(39), "locking a hotbar target must clear its armor-source binding");
        store.toggleLock(2);
        store.bindFromLockGesture(39, 2);
        assertEquals(SlotLockStore.ChangeResult.LOCKED, store.toggleLock(39), "armor source endpoint should lock");
        assertEquals(-1, store.getBoundTo(39), "locking an armor source endpoint must clear its binding");
        delete(root);
    }

    private static void testMalformedFileFailsClosedAndIsPreserved() throws Exception {
        File root = Files.createTempDirectory("lunaria-slot-corrupt").toFile();
        File file = new File(root, "slot-locks.properties");
        String bad = "version=99\nlocked.999=true\n";
        write(file, bad);
        SlotLockStore store = new SlotLockStore();
        store.load(file);
        assertTrue(store.hasFailed(), "unsupported schema must fail closed");
        assertEquals(SlotLockStore.ChangeResult.STORAGE_UNAVAILABLE, store.toggleLock(1), "failed storage must reject changes");
        assertEquals(bad, new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8), "invalid file must be preserved");
        delete(root);
    }

    private static void testValidRecoveryFileIsUsed() throws Exception {
        File root = Files.createTempDirectory("lunaria-slot-recovery").toFile();
        File file = new File(root, "slot-locks.properties");
        File temp = new File(file.getPath() + ".tmp");
        write(file, "version=1\nlocked.0=true\n");
        write(temp, "version=1\nlocked.1=true\nbound.9=2\n");
        SlotLockStore recovered = new SlotLockStore();
        recovered.load(file);
        assertTrue(!recovered.hasFailed(), "valid recovery file should be recoverable");
        assertTrue(recovered.isLocked(1), "latest valid recovery contents should be used");
        assertEquals(2, recovered.getBoundTo(9), "recovery should preserve saved bindings");
        assertTrue(!temp.exists(), "successful recovery should remove temp file");
        delete(root);
    }

    private static void testMalformedRecoveryFileFailsClosed() throws Exception {
        File root = Files.createTempDirectory("lunaria-slot-bad-recovery").toFile();
        File file = new File(root, "slot-locks.properties");
        File temp = new File(file.getPath() + ".tmp");
        write(file, "version=1\nlocked.3=true\n");
        write(temp, "partial write\n");
        SlotLockStore store = new SlotLockStore();
        store.load(file);
        assertTrue(store.hasFailed(), "malformed recovery must fail closed");
        assertTrue(store.isLocked(3), "valid existing locks should remain in memory");
        assertTrue(temp.isFile(), "malformed recovery file should be preserved for inspection");
        delete(root);
    }

    private static void write(File file, String value) throws Exception {
        try (FileOutputStream stream = new FileOutputStream(file);
             OutputStreamWriter writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8)) {
            writer.write(value);
        }
    }

    private static void delete(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) delete(child);
        }
        file.delete();
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " (expected=" + expected + ", actual=" + actual + ")");
        }
    }
}
