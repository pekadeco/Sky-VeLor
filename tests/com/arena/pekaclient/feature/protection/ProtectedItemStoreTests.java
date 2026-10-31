package com.arena.pekaclient.feature.protection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

public final class ProtectedItemStoreTests {
    private ProtectedItemStoreTests() { }

    public static void main(String[] args) throws Exception {
        testMissingFileStartsEmpty();
        testProtectAndUnprotectSurviveReload();
        testRecoveryUsesUnionAndNeverDropsPreviousIds();
        testMalformedFileFailsClosedAndIsPreserved();
        testMalformedRecoveryFileFailsClosed();
        testStrictUuidParsing();
        System.out.println("ProtectedItemStoreTests: all tests passed");
    }

    private static void testMissingFileStartsEmpty() throws Exception {
        File root = Files.createTempDirectory("lunaria-protection-empty").toFile();
        File target = new File(root, "protected-items.txt");
        ProtectedItemStore store = new ProtectedItemStore();
        store.load(target);
        assertTrue(!store.hasFailed(), "missing file should not be a storage error");
        assertEquals(0, store.size(), "fresh store should be empty");
        delete(root);
    }

    private static void testProtectAndUnprotectSurviveReload() throws Exception {
        File root = Files.createTempDirectory("lunaria-protection-roundtrip").toFile();
        File target = new File(root, "protected-items.txt");
        UUID id = UUID.fromString("f12e4567-89ab-4cde-8fab-0123456789ab");
        ProtectedItemStore store = new ProtectedItemStore();
        store.load(target);
        assertEquals(ProtectedItemStore.ToggleResult.PROTECTED, store.toggle(id), "first toggle should protect");
        assertTrue(target.isFile(), "protection file should be saved");
        String saved = new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8);
        assertTrue(saved.contains("version=1"), "saved file should contain a version header");
        assertTrue(saved.contains(id.toString()), "saved file should contain the UUID in readable text");

        ProtectedItemStore reloaded = new ProtectedItemStore();
        reloaded.load(target);
        assertTrue(reloaded.contains(id), "protected UUID should survive reload");
        assertEquals(ProtectedItemStore.ToggleResult.UNPROTECTED, reloaded.toggle(id), "second toggle should unprotect");
        ProtectedItemStore afterUnprotect = new ProtectedItemStore();
        afterUnprotect.load(target);
        assertTrue(!afterUnprotect.contains(id), "unprotected UUID should be absent after reload");
        delete(root);
    }

    private static void testRecoveryUsesUnionAndNeverDropsPreviousIds() throws Exception {
        File root = Files.createTempDirectory("lunaria-protection-recovery").toFile();
        File target = new File(root, "protected-items.txt");
        File temp = new File(target.getPath() + ".tmp");
        UUID committed = UUID.fromString("11111111-2222-4333-8444-555555555555");
        UUID pending = UUID.fromString("aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee");
        write(target, "# Lunaria protected items\nversion=1\n" + committed + "\n");
        write(temp, "# Lunaria protected items\nversion=1\n" + pending + "\n");

        ProtectedItemStore recovered = new ProtectedItemStore();
        recovered.load(target);
        assertTrue(!recovered.hasFailed(), "valid recovery file should be recoverable");
        assertTrue(recovered.contains(committed), "recovery must preserve the committed UUID");
        assertTrue(recovered.contains(pending), "recovery must preserve the pending UUID");
        assertTrue(!temp.exists(), "successful recovery should atomically replace and remove the temp file");
        delete(root);
    }

    private static void testMalformedFileFailsClosedAndIsPreserved() throws Exception {
        File root = Files.createTempDirectory("lunaria-protection-corrupt").toFile();
        File target = new File(root, "protected-items.txt");
        String bad = "# Lunaria protected items\nversion=99\nnot-a-uuid\n";
        write(target, bad);
        ProtectedItemStore store = new ProtectedItemStore();
        store.load(target);
        assertTrue(store.hasFailed(), "unsupported format must fail closed");
        assertEquals(ProtectedItemStore.ToggleResult.STORAGE_UNAVAILABLE,
                store.toggle(UUID.fromString("f12e4567-89ab-4cde-8fab-0123456789ab")),
                "failed store must not accept changes");
        assertEquals(bad, new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8),
                "malformed source file must remain available for manual recovery");
        delete(root);
    }

    private static void testMalformedRecoveryFileFailsClosed() throws Exception {
        File root = Files.createTempDirectory("lunaria-protection-bad-temp").toFile();
        File target = new File(root, "protected-items.txt");
        File temp = new File(target.getPath() + ".tmp");
        write(target, "# Lunaria protected items\nversion=1\n11111111-2222-4333-8444-555555555555\n");
        write(temp, "partial write without a version header\n");
        ProtectedItemStore store = new ProtectedItemStore();
        store.load(target);
        assertTrue(store.hasFailed(), "malformed recovery state must fail closed");
        assertTrue(store.contains(UUID.fromString("11111111-2222-4333-8444-555555555555")),
                "already saved UUIDs should still be remembered in memory");
        assertTrue(temp.isFile(), "the malformed recovery file should remain inspectable");
        delete(root);
    }

    private static void testStrictUuidParsing() {
        UUID expected = UUID.fromString("f12e4567-89ab-4cde-8fab-0123456789ab");
        assertEquals(expected, ItemProtectionIdentity.parseUuid(expected.toString()), "canonical UUID should parse");
        assertEquals(expected, ItemProtectionIdentity.parseUuid(expected.toString().toUpperCase()), "uppercase UUID should parse");
        assertEquals(null, ItemProtectionIdentity.parseUuid("f12e4567-89ab-4cde-8fab-0123456789a"), "short UUID should be rejected");
        assertEquals(null, ItemProtectionIdentity.parseUuid("not-a-uuid"), "malformed UUID should be rejected");
    }

    private static void write(File file, String content) throws Exception {
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory()) parent.mkdirs();
        try (FileOutputStream out = new FileOutputStream(file);
             OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            writer.write(content);
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

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " (expected=" + expected + ", actual=" + actual + ")");
        }
    }
}
