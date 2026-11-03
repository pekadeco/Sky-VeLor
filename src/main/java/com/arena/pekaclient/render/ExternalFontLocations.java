package com.arena.pekaclient.render;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only compatibility lookup for user-provided font files.
 *
 * <p>New files belong in {@code config/lunaria/fonts}. The prior-release directory remains a fallback forever:
 * discovering or creating the current directory never hides a selected legacy file, and no file is copied, renamed,
 * or deleted. When the same file name exists in both locations, the current Lunaria file deliberately wins.</p>
 */
public final class ExternalFontLocations {

    private ExternalFontLocations() {
    }

    /** Returns the current write/discovery directory, creating it only when the caller explicitly requests it. */
    public static File currentDirectory(File configDirectory, boolean create) {
        if (configDirectory == null) return null;
        File current = new File(configDirectory, "lunaria/fonts");
        if (current.isDirectory()) return current;
        if (!create || current.exists()) return null;
        return current.mkdirs() || current.isDirectory() ? current : null;
    }

    /** The previous release's font directory is never created or changed by Lunaria. */
    public static File legacyDirectory(File configDirectory) {
        if (configDirectory == null) return null;
        File legacy = new File(configDirectory, "skyvelor/fonts");
        return legacy.isDirectory() ? legacy : null;
    }

    /**
     * Current directory first, legacy directory second. Creating the current directory is opt-in so render-time
     * resolution can remain side-effect free while the editor can expose a stable folder for new files.
     */
    public static File[] readableDirectories(File configDirectory, boolean createCurrent) {
        List<File> result = new ArrayList<File>();
        File current = currentDirectory(configDirectory, createCurrent);
        if (current != null) result.add(current);
        File legacy = legacyDirectory(configDirectory);
        if (legacy != null && !legacy.equals(current)) result.add(legacy);
        return result.toArray(new File[result.size()]);
    }

    /** Finds a selected external font without creating any directory or changing either source. */
    public static File findReadableFile(File configDirectory, String safeFileName) {
        if (safeFileName == null || safeFileName.length() == 0) return null;
        File[] directories = readableDirectories(configDirectory, false);
        for (File directory : directories) {
            File candidate = new File(directory, safeFileName);
            if (candidate.isFile()) return candidate;
        }
        return null;
    }
}
