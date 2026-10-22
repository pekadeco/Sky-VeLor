package com.arena.pekaclient.assist.guard;

import com.arena.pekaclient.config.ConfigFiles;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Atomic, local-only store for explicitly protected SkyBlock UUIDs. A malformed/unreadable existing file is never
 * silently replaced: the guard reports unhealthy and destructive actions fail closed while it is enabled.
 */
public final class ItemGuardStore {

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final String VERSION = "version=1";
    private final Set<String> ids = new TreeSet<String>();
    private File file;
    private boolean loaded;
    private boolean healthy = true;
    private String problem = "not loaded";

    public synchronized void load(File target) {
        if (loaded) return;
        loaded = true;
        file = target;
        if (target == null) {
            fail("protected-item file location unavailable");
            return;
        }
        File parent = target.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            fail("could not create protected-item folder");
            return;
        }
        if (!target.isFile()) {
            problem = "ready";
            return;
        }
        TreeSet<String> parsed = new TreeSet<String>();
        boolean version = false;
        try {
            BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(target), UTF8));
            try {
                String line;
                while ((line = input.readLine()) != null) {
                    String value = line.trim();
                    if (value.length() == 0 || value.charAt(0) == '#') continue;
                    if (VERSION.equals(value) && !version) {
                        version = true;
                        continue;
                    }
                    int separator = value.indexOf('=');
                    if (separator < 4 || !value.startsWith("id.")) {
                        fail("protected-item file contains invalid data; it was not changed");
                        return;
                    }
                    try {
                        Integer.parseInt(value.substring(3, separator));
                    } catch (NumberFormatException invalidIndex) {
                        fail("protected-item file contains invalid data; it was not changed");
                        return;
                    }
                    UUID uuid = ItemIdentity.parse(value.substring(separator + 1));
                    if (uuid == null) {
                        fail("protected-item file contains invalid data; it was not changed");
                        return;
                    }
                    parsed.add(uuid.toString());
                }
            } finally {
                input.close();
            }
        } catch (IOException | SecurityException failure) {
            fail("could not read protected-item file");
            return;
        }
        if (!version) {
            fail("protected-item file has no supported version marker");
            return;
        }
        ids.addAll(parsed);
        problem = "ready";
    }

    public synchronized boolean contains(UUID uuid) {
        return uuid != null && ids.contains(uuid.toString());
    }

    public synchronized Toggle toggle(UUID uuid) {
        if (uuid == null) return Toggle.INVALID;
        if (!loaded || !healthy) return Toggle.UNAVAILABLE;
        String value = uuid.toString();
        boolean present = ids.remove(value);
        if (!present) ids.add(value);
        if (write()) return present ? Toggle.REMOVED : Toggle.ADDED;
        if (present) ids.add(value); else ids.remove(value);
        fail("could not save protected-item file; change rolled back");
        return Toggle.UNAVAILABLE;
    }

    public synchronized boolean isHealthy() {
        return loaded && healthy;
    }

    public synchronized boolean isLoaded() {
        return loaded;
    }

    public synchronized int size() {
        return ids.size();
    }

    public synchronized String status() {
        return healthy ? "ready, " + ids.size() + " protected UUID" + (ids.size() == 1 ? "" : "s")
                : "unavailable: " + problem;
    }

    public synchronized Set<String> snapshot() {
        return Collections.unmodifiableSet(new TreeSet<String>(ids));
    }

    private boolean write() {
        if (file == null) return false;
        java.util.Properties data = new java.util.Properties();
        // ConfigFiles owns the temp-and-replace lifecycle. Values are numbered to retain deterministic ordering.
        data.setProperty("version", "1");
        int index = 0;
        for (String id : ids) data.setProperty("id." + index++, id);
        return ConfigFiles.writeAtomic(file, data, "Sky VeLor local item-guard UUIDs. Do not edit while Minecraft is running.");
    }

    private void fail(String reason) {
        healthy = false;
        problem = reason == null ? "protected-item storage unavailable" : reason;
    }

    public enum Toggle {
        ADDED, REMOVED, INVALID, UNAVAILABLE
    }
}
