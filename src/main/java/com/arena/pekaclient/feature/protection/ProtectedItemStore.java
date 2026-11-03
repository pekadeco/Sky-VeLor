package com.arena.pekaclient.feature.protection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Local, human-readable list of unique SkyBlock item UUIDs. */
public final class ProtectedItemStore {
    private static final Logger LOGGER = Logger.getLogger("Lunaria");
    private static final String HEADER = "# Lunaria protected items";
    private static final String VERSION_LINE = "version=1";

    private final Set<String> ids = new TreeSet<String>();
    private File file;
    private boolean loaded;
    private boolean failed;
    private String failureReason = "";

    public synchronized void load(File target) {
        load(target, target);
    }

    /**
     * Loads a compatibility source while retaining {@code target} as the only future write destination.
     * A legacy file is therefore never altered merely by launching Lunaria.
     */
    public synchronized void load(File target, File source) {
        if (loaded) return;
        if (target == null) {
            fail("Protected item file path is unavailable.");
            loaded = true;
            return;
        }
        file = target;
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory()) {
            fail("Could not create the protected item folder.");
            loaded = true;
            return;
        }

        File readSource = source == null ? file : source;
        File temp = new File(readSource.getPath() + ".tmp");
        boolean hasMain = readSource.isFile();
        boolean hasTemp = temp.isFile();
        if (!hasMain && !hasTemp) {
            loaded = true;
            return;
        }

        Set<String> mainIds = null;
        Set<String> tempIds = null;
        if (hasMain) {
            mainIds = readStrict(readSource);
            if (mainIds == null) {
                fail("The protected item file is unreadable or malformed. No protected entries were discarded.");
                loaded = true;
                return;
            }
        }
        if (hasTemp) {
            tempIds = readStrict(temp);
            if (tempIds == null) {
                if (!hasMain) {
                    fail("The recovery file is unreadable or malformed. No protected entries were discarded.");
                    loaded = true;
                    return;
                }
                ids.addAll(mainIds);
                fail("A recovery file is malformed. The saved protected list was preserved; fix the .tmp file before using protection.");
                loaded = true;
                return;
            }
        }

        if (mainIds != null) ids.addAll(mainIds);
        if (tempIds != null) ids.addAll(tempIds);
        loaded = true;

        // When a crash left a valid temp file, union it with the main file so a failed
        // unprotect write can never silently remove protection on the next launch.
        if (hasTemp && !failed) {
            if (!saveCurrentSet()) {
                fail("Could not recover the protected item list. Destructive actions remain blocked.");
            }
        }
    }

    public synchronized boolean contains(UUID uuid) {
        ensureLoaded();
        return uuid != null && ids.contains(uuid.toString().toLowerCase(Locale.ROOT));
    }

    public synchronized boolean isLoaded() {
        return loaded;
    }

    public synchronized boolean hasFailed() {
        ensureLoaded();
        return failed;
    }

    public synchronized String getFailureReason() {
        ensureLoaded();
        return failureReason;
    }

    public synchronized int size() {
        ensureLoaded();
        return ids.size();
    }

    public synchronized Set<String> snapshot() {
        ensureLoaded();
        return Collections.unmodifiableSet(new TreeSet<String>(ids));
    }

    public synchronized ToggleResult toggle(UUID uuid) {
        ensureLoaded();
        if (uuid == null) return ToggleResult.INVALID_UUID;
        if (failed) return ToggleResult.STORAGE_UNAVAILABLE;

        String id = uuid.toString().toLowerCase(Locale.ROOT);
        boolean wasPresent = ids.contains(id);
        if (wasPresent) ids.remove(id); else ids.add(id);
        if (!saveCurrentSet()) {
            if (wasPresent) ids.add(id); else ids.remove(id);
            fail("Could not save the protected item list. Changes were rolled back; destructive actions are blocked until restart or repair.");
            return ToggleResult.STORAGE_ERROR;
        }
        return wasPresent ? ToggleResult.UNPROTECTED : ToggleResult.PROTECTED;
    }

    private void ensureLoaded() {
        if (!loaded) {
            File root = new File(System.getProperty("user.dir", "."), "config");
            File current = new File(root, "lunaria/protected-items.txt");
            File legacy = new File(root, "skyvelor/protected-items.txt");
            load(current, current.isFile() || new File(current.getPath() + ".tmp").isFile() ? current : legacy);
        }
    }

    private Set<String> readStrict(File source) {
        Set<String> loadedIds = new TreeSet<String>();
        boolean versionFound = false;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Files.newInputStream(source.toPath()), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String value = line.trim();
                if (value.isEmpty() || value.startsWith("#")) continue;
                if (VERSION_LINE.equals(value)) {
                    if (versionFound) return null;
                    versionFound = true;
                    continue;
                }
                if (value.startsWith("version=")) return null;
                if (!versionFound) return null;
                try {
                    UUID uuid = UUID.fromString(value);
                    String canonical = uuid.toString();
                    if (!canonical.equalsIgnoreCase(value)) return null;
                    loadedIds.add(canonical.toLowerCase(Locale.ROOT));
                } catch (IllegalArgumentException badUuid) {
                    return null;
                }
            }
        } catch (IOException | SecurityException ex) {
            LOGGER.log(Level.WARNING, "Could not read protected item list " + source, ex);
            return null;
        }
        return versionFound ? loadedIds : null;
    }

    private boolean saveCurrentSet() {
        if (file == null) return false;
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory()) return false;
        File temp = new File(file.getPath() + ".tmp");
        try (FileOutputStream stream = new FileOutputStream(temp);
             Writer writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8)) {
            writer.write(HEADER);
            writer.write(System.lineSeparator());
            writer.write(VERSION_LINE);
            writer.write(System.lineSeparator());
            for (String id : ids) {
                writer.write(id);
                writer.write(System.lineSeparator());
            }
            writer.flush();
            stream.getFD().sync();
        } catch (IOException | SecurityException ex) {
            LOGGER.log(Level.WARNING, "Could not write protected item temp file " + temp, ex);
            try { Files.deleteIfExists(temp.toPath()); } catch (IOException ignored) { }
            return false;
        }

        try {
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException | SecurityException ex) {
            LOGGER.log(Level.WARNING, "Could not replace protected item file " + file, ex);
            try { Files.deleteIfExists(temp.toPath()); } catch (IOException ignored) { }
            return false;
        }
    }

    private void fail(String reason) {
        failed = true;
        failureReason = reason == null ? "Protected item storage failed." : reason;
        LOGGER.warning("[Lunaria] " + failureReason);
    }

    public enum ToggleResult {
        PROTECTED,
        UNPROTECTED,
        INVALID_UUID,
        STORAGE_ERROR,
        STORAGE_UNAVAILABLE
    }
}
