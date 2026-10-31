package com.arena.pekaclient.dungeon.sync;

import com.arena.pekaclient.config.ConfigFiles;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Properties;

/** Local opt-in settings for the bundled Lunaria Cloudflare sync service. No API secrets are stored here. */
public final class DungeonPuzzleSyncConfig {
    /** Existing deployed Worker route retained so prior opt-in settings continue to connect without a reset. */
    public static final String DEFAULT_ENDPOINT = "wss://skyvelor-dungeon-sync.bombadzalge.workers.dev/ws";
    public static final String FILE_NAME = "lunaria-dungeon-sync.properties";
    public static final String PRIOR_RELEASE_LEGACY_FILE_NAME = "skyvelor-dungeon-sync.properties";

    private static final String DIAGNOSTICS_PREFERENCE_VERSION = "1";
    private static DungeonPuzzleSyncConfig cached;
    public final boolean enabled;
    public final boolean diagnosticsEnabled;
    public final String endpoint;
    public final File file;

    private DungeonPuzzleSyncConfig(boolean enabled, boolean diagnosticsEnabled, String endpoint, File file) {
        this.enabled = enabled;
        this.diagnosticsEnabled = diagnosticsEnabled;
        this.endpoint = endpoint;
        this.file = file;
    }

    public static synchronized DungeonPuzzleSyncConfig load(Minecraft mc) {
        if (cached != null) return cached;
        File configDir = new File(mc.mcDataDir, "config");
        if (!configDir.isDirectory() && !configDir.mkdirs()) {
            cached = new DungeonPuzzleSyncConfig(false, false, DEFAULT_ENDPOINT, new File(configDir, FILE_NAME));
            return cached;
        }
        File file = new File(configDir, FILE_NAME);
        File source = ConfigFiles.resolveRead(configDir, FILE_NAME, PRIOR_RELEASE_LEGACY_FILE_NAME);
        Properties properties = new Properties();
        boolean enabled = false;
        boolean diagnosticsEnabled = false;
        boolean migrateDiagnosticsPreference = false;
        String endpoint = DEFAULT_ENDPOINT;
        if (source.isFile()) {
            try (FileInputStream input = new FileInputStream(source)) {
                properties.load(input);
                enabled = Boolean.parseBoolean(properties.getProperty("enabled", "false").trim());
                migrateDiagnosticsPreference = !DIAGNOSTICS_PREFERENCE_VERSION.equals(
                        properties.getProperty("diagnosticsPreferenceVersion", ""));
                diagnosticsEnabled = resolveDiagnosticsPreference(properties);
                endpoint = resolveEndpoint(properties.getProperty("endpoint", DEFAULT_ENDPOINT));
            } catch (IOException ignored) {
                enabled = false;
                diagnosticsEnabled = false;
                endpoint = DEFAULT_ENDPOINT;
            }
        } else {
            // The endpoint is built into the mod so users do not need to copy it manually.
            // Keep external synchronization and verbose diagnostics disabled until the user opts in.
            properties.setProperty("enabled", "false");
            properties.setProperty("diagnostics", "false");
            properties.setProperty("diagnosticsPreferenceVersion", DIAGNOSTICS_PREFERENCE_VERSION);
            properties.setProperty("endpoint", DEFAULT_ENDPOINT);
            try (OutputStream output = new FileOutputStream(file)) {
                properties.store(output,
                        "Lunaria dungeon puzzle sync. Enable or disable from /lunaria > Dungeon > Dungeon Sync. The Worker URL is supplied by the mod. Cloudflare receives a temporary session hash, puzzle labels/room coordinates, and normal connection metadata such as IP address.");
            } catch (IOException ignored) {
                // No connection is attempted when the opt-in file cannot be created.
            }
        }
        if (!isSecureWebSocketEndpoint(endpoint)) {
            enabled = false;
            endpoint = DEFAULT_ENDPOINT;
        }
        if (migrateDiagnosticsPreference) {
            properties.setProperty("enabled", Boolean.toString(enabled));
            properties.setProperty("diagnostics", Boolean.toString(diagnosticsEnabled));
            properties.setProperty("diagnosticsPreferenceVersion", DIAGNOSTICS_PREFERENCE_VERSION);
            properties.setProperty("endpoint", endpoint);
            ConfigFiles.writeAtomic(file, properties,
                    "Lunaria dungeon puzzle sync settings; verbose diagnostics are opt-in.");
        }
        cached = new DungeonPuzzleSyncConfig(enabled, diagnosticsEnabled, endpoint, file);
        return cached;
    }

    /**
     * Diagnostics were enabled by default in early debugging builds. Treat an unversioned
     * setting as that legacy default, switch it off once, and preserve choices made in this version.
     */
    static boolean resolveDiagnosticsPreference(Properties properties) {
        if (properties == null) return false;
        if (!DIAGNOSTICS_PREFERENCE_VERSION.equals(properties.getProperty("diagnosticsPreferenceVersion", ""))) {
            properties.setProperty("diagnostics", "false");
            properties.setProperty("diagnosticsPreferenceVersion", DIAGNOSTICS_PREFERENCE_VERSION);
            return false;
        }
        return Boolean.parseBoolean(properties.getProperty("diagnostics", "false").trim());
    }

    public static synchronized void clearCacheForTests() {
        cached = null;
    }

    /** Current UI value. Invalid or unavailable endpoints always read as disabled. */
    public static synchronized boolean isEnabled(Minecraft mc) {
        return load(mc).isUsable();
    }

    /**
     * Saves the user's choice immediately. Replaces the cached immutable config only after the
     * atomic disk write succeeds, so the settings toggle cannot claim success if saving failed.
     */
    public static synchronized boolean setEnabled(Minecraft mc, boolean enabled) {
        if (mc == null) return false;
        DungeonPuzzleSyncConfig current = load(mc);
        boolean nextEnabled = enabled && isSecureWebSocketEndpoint(current.endpoint);
        if (!saveEnabled(current.file, nextEnabled, current.endpoint)) return false;
        cached = new DungeonPuzzleSyncConfig(nextEnabled, current.diagnosticsEnabled, current.endpoint, current.file);
        return true;
    }

    public static synchronized boolean isDiagnosticsEnabled(Minecraft mc) {
        return load(mc).diagnosticsEnabled;
    }

    public static synchronized boolean setDiagnosticsEnabled(Minecraft mc, boolean enabled) {
        if (mc == null) return false;
        DungeonPuzzleSyncConfig current = load(mc);
        if (!saveOptions(current.file, current.enabled, enabled, current.endpoint)) return false;
        cached = new DungeonPuzzleSyncConfig(current.enabled, enabled, current.endpoint, current.file);
        return true;
    }

    static boolean saveEnabled(File file, boolean enabled, String configuredEndpoint) {
        Properties properties = ConfigFiles.read(file);
        boolean diagnostics = resolveDiagnosticsPreference(properties);
        return saveOptions(file, enabled, diagnostics, configuredEndpoint);
    }

    private static boolean saveOptions(File file, boolean enabled, boolean diagnostics, String configuredEndpoint) {
        if (file == null) return false;
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory()) return false;
        Properties properties = ConfigFiles.read(file);
        String endpoint = resolveEndpoint(properties.getProperty("endpoint", configuredEndpoint));
        if (!isSecureWebSocketEndpoint(endpoint)) endpoint = DEFAULT_ENDPOINT;
        properties.setProperty("enabled", Boolean.toString(enabled));
        properties.setProperty("diagnostics", Boolean.toString(diagnostics));
        properties.setProperty("diagnosticsPreferenceVersion", DIAGNOSTICS_PREFERENCE_VERSION);
        properties.setProperty("endpoint", endpoint);
        return ConfigFiles.writeAtomic(file, properties,
                "Lunaria dungeon puzzle sync. Settings are available from /lunaria > Dungeon > Dungeon Sync.");
    }

    public boolean isUsable() {
        return enabled && isSecureWebSocketEndpoint(endpoint);
    }

    /** Uses the bundled service endpoint when an older config has no endpoint or a blank value. */
    public static String resolveEndpoint(String configuredEndpoint) {
        if (configuredEndpoint == null || configuredEndpoint.trim().isEmpty()) return DEFAULT_ENDPOINT;
        return configuredEndpoint.trim();
    }

    public static boolean isSecureWebSocketEndpoint(String endpoint) {
        if (endpoint == null || endpoint.trim().isEmpty()) return false;
        try {
            // java.net.URL has no built-in handler for the wss scheme on Java 8.
            // URI parses wss:// normally and lets the socket client perform TLS itself.
            URI uri = new URI(endpoint.trim());
            String path = uri.getRawPath() == null ? "" : uri.getRawPath().replaceAll("/+$", "");
            return "wss".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null && !uri.getHost().trim().isEmpty()
                    && (uri.getPort() == -1 || (uri.getPort() >= 1 && uri.getPort() <= 65535))
                    && path.endsWith("/ws")
                    && (uri.getUserInfo() == null || uri.getUserInfo().trim().isEmpty())
                    && (uri.getQuery() == null || uri.getQuery().trim().isEmpty())
                    && (uri.getFragment() == null || uri.getFragment().trim().isEmpty());
        } catch (Exception ignored) {
            return false;
        }
    }
}
