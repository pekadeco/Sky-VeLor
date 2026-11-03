package com.arena.pekaclient.dungeon.sync;

import java.io.File;
import java.nio.file.Files;
import java.util.Properties;

public final class DungeonPuzzleSyncConfigTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("bundled Worker endpoint is valid", DungeonPuzzleSyncConfig.isSecureWebSocketEndpoint(DungeonPuzzleSyncConfig.DEFAULT_ENDPOINT));
        check("blank config endpoint resolves to bundled Worker", DungeonPuzzleSyncConfig.resolveEndpoint("").equals(DungeonPuzzleSyncConfig.DEFAULT_ENDPOINT));
        check("missing config endpoint resolves to bundled Worker", DungeonPuzzleSyncConfig.resolveEndpoint(null).equals(DungeonPuzzleSyncConfig.DEFAULT_ENDPOINT));
        check("custom configured endpoint remains supported", DungeonPuzzleSyncConfig.resolveEndpoint("wss://custom.workers.dev/ws").equals("wss://custom.workers.dev/ws"));
        check("valid Worker endpoint is accepted", DungeonPuzzleSyncConfig.isSecureWebSocketEndpoint("wss://example.workers.dev/ws"));
        check("trailing slash is accepted", DungeonPuzzleSyncConfig.isSecureWebSocketEndpoint("wss://example.workers.dev/ws/"));
        check("custom valid port is accepted", DungeonPuzzleSyncConfig.isSecureWebSocketEndpoint("wss://example.workers.dev:8443/ws"));
        check("https is not accepted as a WebSocket endpoint", !DungeonPuzzleSyncConfig.isSecureWebSocketEndpoint("https://example.workers.dev/ws"));
        check("unencrypted ws is rejected", !DungeonPuzzleSyncConfig.isSecureWebSocketEndpoint("ws://example.workers.dev/ws"));
        check("endpoint without Worker route is rejected", !DungeonPuzzleSyncConfig.isSecureWebSocketEndpoint("wss://example.workers.dev/"));
        check("wrong route is rejected", !DungeonPuzzleSyncConfig.isSecureWebSocketEndpoint("wss://example.workers.dev/puzzles"));
        check("userinfo is rejected", !DungeonPuzzleSyncConfig.isSecureWebSocketEndpoint("wss://user@example.workers.dev/ws"));
        check("query strings are rejected", !DungeonPuzzleSyncConfig.isSecureWebSocketEndpoint("wss://example.workers.dev/ws?token=secret"));
        check("fragments are rejected", !DungeonPuzzleSyncConfig.isSecureWebSocketEndpoint("wss://example.workers.dev/ws#fragment"));
        check("malformed URL is rejected", !DungeonPuzzleSyncConfig.isSecureWebSocketEndpoint("not a URL"));
        testDiagnosticsPreferenceMigration();
        testPersistedToggle();
        System.out.println("DungeonPuzzleSyncConfigTests: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void testDiagnosticsPreferenceMigration() {
        Properties freshInstall = new Properties();
        check("fresh config defaults Dungeon Sync Debug to off", !DungeonPuzzleSyncConfig.resolveDiagnosticsPreference(freshInstall));
        check("fresh config records diagnostics as false", "false".equals(freshInstall.getProperty("diagnostics")));
        Properties legacy = new Properties();
        legacy.setProperty("diagnostics", "true");
        check("legacy debug-build default migrates to diagnostics off", !DungeonPuzzleSyncConfig.resolveDiagnosticsPreference(legacy));
        check("legacy diagnostics preference migration is marked", "1".equals(legacy.getProperty("diagnosticsPreferenceVersion")));
        check("legacy debug-build default is rewritten as false", "false".equals(legacy.getProperty("diagnostics")));

        Properties optedIn = new Properties();
        optedIn.setProperty("diagnosticsPreferenceVersion", "1");
        optedIn.setProperty("diagnostics", "true");
        check("explicit diagnostics opt-in is preserved", DungeonPuzzleSyncConfig.resolveDiagnosticsPreference(optedIn));

        Properties optedOut = new Properties();
        optedOut.setProperty("diagnosticsPreferenceVersion", "1");
        optedOut.setProperty("diagnostics", "false");
        check("explicit diagnostics opt-out is preserved", !DungeonPuzzleSyncConfig.resolveDiagnosticsPreference(optedOut));
    }

    private static void testPersistedToggle() {
        File dir = null;
        try {
            dir = Files.createTempDirectory("lunaria-sync-toggle").toFile();
            File config = new File(dir, "skyvelor-dungeon-sync.properties");
            check("toggle can enable synchronization in the local config", DungeonPuzzleSyncConfig.saveEnabled(
                    config, true, DungeonPuzzleSyncConfig.DEFAULT_ENDPOINT));
            Properties enabled = new Properties();
            try (java.io.FileInputStream in = new java.io.FileInputStream(config)) { enabled.load(in); }
            check("enabled state is persisted", "true".equals(enabled.getProperty("enabled")));
            check("bundled endpoint remains present when enabled", DungeonPuzzleSyncConfig.DEFAULT_ENDPOINT.equals(enabled.getProperty("endpoint")));
            check("diagnostics default off and persist with the settings", "false".equals(enabled.getProperty("diagnostics")));
            check("diagnostics preference version is saved", "1".equals(enabled.getProperty("diagnosticsPreferenceVersion")));
            check("toggle can disable synchronization again", DungeonPuzzleSyncConfig.saveEnabled(
                    config, false, DungeonPuzzleSyncConfig.DEFAULT_ENDPOINT));
            Properties disabled = new Properties();
            try (java.io.FileInputStream in = new java.io.FileInputStream(config)) { disabled.load(in); }
            check("disabled state is persisted", "false".equals(disabled.getProperty("enabled")));
            check("diagnostics-off state is preserved when sync is disabled", "false".equals(disabled.getProperty("diagnostics")));
            Properties customEndpoint = new Properties();
            customEndpoint.setProperty("endpoint", "wss://custom.workers.dev/ws");
            customEndpoint.setProperty("enabled", "false");
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(config)) {
                customEndpoint.store(out, "test");
            }
            check("toggle preserves a custom Worker endpoint", DungeonPuzzleSyncConfig.saveEnabled(
                    config, true, DungeonPuzzleSyncConfig.DEFAULT_ENDPOINT));
            Properties afterCustom = new Properties();
            try (java.io.FileInputStream in = new java.io.FileInputStream(config)) { afterCustom.load(in); }
            check("custom endpoint is not overwritten by UI toggle", "wss://custom.workers.dev/ws".equals(afterCustom.getProperty("endpoint")));
        } catch (Exception failure) {
            check("persisted settings toggle test completed", false);
        } finally {
            if (dir != null) {
                File[] files = dir.listFiles();
                if (files != null) for (File file : files) file.delete();
                dir.delete();
            }
        }
    }

    private static void check(String label, boolean okay) {
        if (okay) passed++;
        else { failed++; System.out.println("FAIL: " + label); }
    }
}
