package com.arena.pekaclient.render;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Proves external-font migration is read-only and a created current folder cannot hide an older selection. */
public final class ExternalFontLocationsTests {
    private ExternalFontLocationsTests() {
    }

    public static void main(String[] args) throws Exception {
        File root = Files.createTempDirectory("lunaria-font-locations").toFile();
        File config = new File(root, "config");
        File legacy = new File(config, "skyvelor/fonts");
        File legacyFont = new File(legacy, "kept.ttf");
        legacy.mkdirs();
        write(legacyFont, "legacy-font");

        File selected = ExternalFontLocations.findReadableFile(config, "kept.ttf");
        check("legacy font resolves before a current folder exists", legacyFont.equals(selected));
        check("read lookup does not create the current folder",
                !new File(config, "lunaria/fonts").exists());

        File current = ExternalFontLocations.currentDirectory(config, true);
        check("current folder can be created for new user files", current != null && current.isDirectory());
        check("creating current folder does not hide selected legacy font",
                legacyFont.equals(ExternalFontLocations.findReadableFile(config, "kept.ttf")));
        check("legacy file is untouched", "legacy-font".equals(read(legacyFont)));

        File currentFont = new File(current, "kept.ttf");
        write(currentFont, "current-font");
        check("current file wins only when the same selected name exists in both locations",
                currentFont.equals(ExternalFontLocations.findReadableFile(config, "kept.ttf")));
        check("legacy duplicate remains untouched", "legacy-font".equals(read(legacyFont)));

        File[] directories = ExternalFontLocations.readableDirectories(config, false);
        check("discovery includes current then legacy directories", directories.length == 2
                && current.equals(directories[0]) && legacy.equals(directories[1]));
        delete(root);
        if (failed > 0) throw new AssertionError("ExternalFontLocationsTests failed: " + failed);
        System.out.println("ExternalFontLocationsTests: all tests passed");
    }

    private static int failed;

    private static void check(String name, boolean value) {
        if (!value) {
            failed++;
            System.out.println("FAIL " + name);
        }
    }

    private static void write(File file, String text) throws Exception {
        file.getParentFile().mkdirs();
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String read(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private static void delete(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) delete(child);
        }
        file.delete();
    }
}
