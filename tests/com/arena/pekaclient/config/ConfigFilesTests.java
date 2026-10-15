package com.arena.pekaclient.config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

/**
 * Checks the config file migration: the new file is used when it exists, the old file is read when only
 * the old one exists, and nothing is ever written to the old file.
 */
public final class ConfigFilesTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) throws IOException {
        File dir = Files.createTempDirectory("skyvelor-test").toFile();
        try {
            noFilesGivesCurrentName(dir);
            legacyOnlyIsMigratedAndNotWritten(dir);
            currentWinsOverLegacy(dir);
            unreadableFileGivesEmptySet(dir);
        } finally {
            deleteTree(dir);
        }

        System.out.println();
        System.out.println("config files: passed " + passed + ", failed " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void check(String name, boolean ok, String detail) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL " + name + " " + detail);
        }
    }

    private static void write(File f, String text) throws IOException {
        Files.write(f.toPath(), text.getBytes(StandardCharsets.ISO_8859_1));
    }

    private static void noFilesGivesCurrentName(File dir) {
        File f = ConfigFiles.resolveRead(dir, "skyvelor.properties", "pekaclient.properties");
        check("no files: current name", f.getName().equals("skyvelor.properties"), f.getName());
        check("no files: empty set", ConfigFiles.read(f).isEmpty(), "");
    }

    private static void legacyOnlyIsMigratedAndNotWritten(File dir) throws IOException {
        File legacy = new File(dir, "pekaclient.properties");
        write(legacy, "scrollSpeed=150\ndarkMode=false\n");
        long before = legacy.lastModified();
        File f = ConfigFiles.resolveRead(dir, "skyvelor.properties", "pekaclient.properties");
        Properties p = ConfigFiles.read(f);
        check("legacy read", "150".equals(p.getProperty("scrollSpeed")), String.valueOf(p.getProperty("scrollSpeed")));
        check("legacy read bool", "false".equals(p.getProperty("darkMode")), "");
        check("legacy untouched", legacy.isFile() && legacy.lastModified() == before, "");
        legacy.delete();
    }

    private static void currentWinsOverLegacy(File dir) throws IOException {
        write(new File(dir, "pekaclient.properties"), "scrollSpeed=150\n");
        write(new File(dir, "skyvelor.properties"), "scrollSpeed=80\n");
        File f = ConfigFiles.resolveRead(dir, "skyvelor.properties", "pekaclient.properties");
        check("current wins", "80".equals(ConfigFiles.read(f).getProperty("scrollSpeed")), "");
        new File(dir, "pekaclient.properties").delete();
        new File(dir, "skyvelor.properties").delete();
    }

    private static void unreadableFileGivesEmptySet(File dir) throws IOException {
        // A bad unicode escape makes Properties.load throw IllegalArgumentException. It must not stop the load.
        write(new File(dir, "skyvelor.properties"), "bad=\\uZZZZ\n");
        Properties p = ConfigFiles.read(new File(dir, "skyvelor.properties"));
        check("corrupt file gives empty set", p.isEmpty(), String.valueOf(p));
        new File(dir, "skyvelor.properties").delete();
    }

    private static void deleteTree(File f) {
        File[] kids = f.listFiles();
        if (kids != null) {
            for (File k : kids) {
                deleteTree(k);
            }
        }
        f.delete();
    }
}
