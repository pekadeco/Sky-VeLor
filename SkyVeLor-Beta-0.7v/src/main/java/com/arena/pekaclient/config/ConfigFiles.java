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
