package com.arena.pekaclient.config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

/**
 * Checks Lunaria's ordered config migration: lunaria.properties wins, then Lunaria, then Peka Client.
 * Legacy input is read-only; an explicit new-file save is the only migration step.
 */
public final class ConfigFilesTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) throws IOException {
        File dir = Files.createTempDirectory("lunaria-config-test").toFile();
        try {
            noFilesGivesCurrentName(dir);
            pekaOnlyIsReadAndPreserved(dir);
            priorReleaseWinsOverEarliestLegacy(dir);
            lunariaWinsOverEveryLegacyFile(dir);
            migrationOnSaveLeavesLegacyUntouched(dir);
            unreadableFileGivesEmptySet(dir);
        } finally {
            deleteTree(dir);
        }

        System.out.println();
        System.out.println("config files: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void check(String name, boolean ok, String detail) {
        if (ok) passed++;
        else {
            failed++;
            System.out.println("FAIL " + name + " " + detail);
        }
    }

    private static void write(File f, String text) throws IOException {
        Files.write(f.toPath(), text.getBytes(StandardCharsets.ISO_8859_1));
    }

    private static File resolve(File dir) {
        return ConfigFiles.resolveRead(dir, "lunaria.properties", "skyvelor.properties", "pekaclient.properties");
    }

    private static void noFilesGivesCurrentName(File dir) {
        File f = resolve(dir);
        check("no files: Lunaria name", "lunaria.properties".equals(f.getName()), f.getName());
        check("no files: empty set", ConfigFiles.read(f).isEmpty(), "");
    }

    private static void pekaOnlyIsReadAndPreserved(File dir) throws IOException {
        File peka = new File(dir, "pekaclient.properties");
        write(peka, "scrollSpeed=150\ndarkMode=false\n");
        long before = peka.lastModified();
        Properties p = ConfigFiles.read(resolve(dir));
        check("Peka fallback read", "150".equals(p.getProperty("scrollSpeed")), String.valueOf(p.getProperty("scrollSpeed")));
        check("Peka fallback preserves value", "false".equals(p.getProperty("darkMode")), "");
        check("Peka source untouched by read", peka.isFile() && peka.lastModified() == before, "");
        peka.delete();
    }

    private static void priorReleaseWinsOverEarliestLegacy(File dir) throws IOException {
        write(new File(dir, "pekaclient.properties"), "scrollSpeed=150\n");
        write(new File(dir, "skyvelor.properties"), "scrollSpeed=80\n");
        Properties p = ConfigFiles.read(resolve(dir));
        check("Lunaria is first legacy fallback", "80".equals(p.getProperty("scrollSpeed")), String.valueOf(p));
        new File(dir, "pekaclient.properties").delete();
        new File(dir, "skyvelor.properties").delete();
    }

    private static void lunariaWinsOverEveryLegacyFile(File dir) throws IOException {
        write(new File(dir, "pekaclient.properties"), "scrollSpeed=150\n");
        write(new File(dir, "skyvelor.properties"), "scrollSpeed=80\n");
        write(new File(dir, "lunaria.properties"), "scrollSpeed=42\n");
        Properties p = ConfigFiles.read(resolve(dir));
        check("Lunaria current file wins", "42".equals(p.getProperty("scrollSpeed")), String.valueOf(p));
        new File(dir, "pekaclient.properties").delete();
        new File(dir, "skyvelor.properties").delete();
        new File(dir, "lunaria.properties").delete();
    }

    private static void migrationOnSaveLeavesLegacyUntouched(File dir) throws IOException {
        File legacy = new File(dir, "skyvelor.properties");
        write(legacy, "scrollSpeed=137\ndarkMode=false\n");
        byte[] before = Files.readAllBytes(legacy.toPath());
        Properties normalized = ConfigFiles.read(resolve(dir));
        check("migration reads legacy state", "137".equals(normalized.getProperty("scrollSpeed")), String.valueOf(normalized));
        boolean wrote = ConfigFiles.writeAtomic(new File(dir, "lunaria.properties"), normalized, "Lunaria settings");
        check("migration writes new Lunaria file", wrote && new File(dir, "lunaria.properties").isFile(), "");
        check("migration preserves legacy file bytes", java.util.Arrays.equals(before, Files.readAllBytes(legacy.toPath())), "");
        check("migration new file has inherited value", "137".equals(ConfigFiles.read(new File(dir, "lunaria.properties"))
                .getProperty("scrollSpeed")), "");
        legacy.delete();
        new File(dir, "lunaria.properties").delete();
    }

    private static void unreadableFileGivesEmptySet(File dir) throws IOException {
        File current = new File(dir, "lunaria.properties");
        write(current, "bad=\\uZZZZ\n");
        Properties p = ConfigFiles.read(current);
        check("corrupt file gives empty set", p.isEmpty(), String.valueOf(p));
        current.delete();
    }

    private static void deleteTree(File f) {
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) deleteTree(k);
        f.delete();
    }
}
