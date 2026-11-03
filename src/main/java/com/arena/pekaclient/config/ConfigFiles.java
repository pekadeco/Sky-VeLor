package com.arena.pekaclient.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Finds and reads Lunaria configuration files without destroying older installations.
 *
 * <p>The current name is always preferred. When it does not exist, callers may supply any number of historic
 * names in priority order (for Lunaria: the prior release, then the earliest legacy release). Reads never rename or delete an older file;
 * a caller deliberately migrates by writing its normalized state to the current name via {@link #writeAtomic}.</p>
 */
public final class ConfigFiles {

    private static final Logger LOG = LogManager.getLogger("Lunaria");

    private ConfigFiles() {
    }

    /**
     * Resolves the file to read: current name first, then historic names in the supplied order, else current name.
     * Blank historic names are ignored so feature-specific callers can build their fallback list safely.
     */
    public static File resolveRead(File dir, String currentName, String... historicNames) {
        File current = new File(dir, currentName);
        if (current.isFile()) {
            return current;
        }
        if (historicNames != null) {
            for (String historicName : historicNames) {
                if (historicName == null || historicName.trim().length() == 0) continue;
                File historic = new File(dir, historicName);
                if (historic.isFile()) {
                    LOG.info("Reading legacy settings from " + historic.getName() + "; next save writes " + currentName);
                    return historic;
                }
            }
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
