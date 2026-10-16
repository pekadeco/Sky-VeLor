package com.arena.pekaclient.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Finds and reads Sky VeLor config files, with a safe migration from the Peka Client names.
 *
 * Beta-0.3v (and 0.2v) write {@code skyvelor*.properties}. If that file does not exist yet but the older
 * {@code pekaclient*.properties} file does, the old file is read once. Nothing is ever written to the
 * old file, so a downgrade or a failed save leaves the previous settings untouched.
 */
public final class ConfigFiles {

    private static final Logger LOG = LogManager.getLogger("SkyVeLor");

    private ConfigFiles() {
    }

    /** The file to read: the current name if it exists, else the legacy name if it exists, else the current name. */
    public static File resolveRead(File dir, String currentName, String legacyName) {
        File current = new File(dir, currentName);
        if (current.isFile()) {
            return current;
        }
        File legacy = new File(dir, legacyName);
        if (legacy.isFile()) {
            LOG.info("Migrating settings from " + legacy.getName() + " to " + currentName);
            return legacy;
        }
        return current;
    }

    /**
     * Writes properties to a temporary sibling and moves it over the target. A crash or full disk while writing
     * therefore leaves the previous settings file intact instead of truncating it. Returns whether replacement
     * succeeded; on a replacement failure the complete new file remains next to the target as {@code .tmp}.
     */
    public static boolean writeAtomic(File target, Properties properties, String comment) {
        if (target == null || properties == null) {
            LOG.warn("Could not write settings: target or properties was null");
            return false;
        }
        File parent = target.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            LOG.warn("Could not create settings directory " + parent);
            return false;
        }
        File temporary = new File(parent == null ? new File(".") : parent, target.getName() + ".tmp");
        try (java.io.OutputStream out = new java.io.FileOutputStream(temporary)) {
            properties.store(out, comment);
        } catch (IOException e) {
            LOG.warn("Could not write " + temporary, e);
            return false;
        }
        try {
            try {
                java.nio.file.Files.move(temporary.toPath(), target.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                return true;
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                // Filesystems without atomic rename still receive a complete replacement below.
            } catch (java.nio.file.FileAlreadyExistsException ignored) {
                // Some providers reject REPLACE_EXISTING together with ATOMIC_MOVE; retry non-atomically.
            }
            java.nio.file.Files.move(temporary.toPath(), target.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            LOG.warn("Could not replace " + target + "; the new data is in " + temporary, e);
            return false;
        }
    }

    /** Reads a properties file. A missing file gives an empty set; an unreadable one is logged and also gives an empty set. */
    public static Properties read(File f) {
        Properties p = new Properties();
        if (!f.isFile()) {
            return p;
        }
        try (InputStream in = new FileInputStream(f)) {
            p.load(in);
        } catch (IOException | IllegalArgumentException e) {
            // IllegalArgumentException: a malformed unicode escape in a corrupt file.
            LOG.warn("Could not read " + f + "; using default settings", e);
            return new Properties();
        }
        return p;
    }
}
