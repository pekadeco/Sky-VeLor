package com.arena.pekaclient.gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Enforces the Stage-B GUI colour boundary.
 *
 * <p>The renderer-facing gui, widget, notes, hud, and render packages may declare hexadecimal colours only in the
 * single {@code render/Theme.java} token authority. Configuration-owned legacy palette arrays are deliberately out
 * of this scan: they remain readable for old files, but Theme does not render from them.</p>
 */
public final class HardcodedGuiColorTests {
    private static final Pattern ARGB_OR_RGB = Pattern.compile("(?i)0x[0-9a-f]{6,8}\\b");
    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        File root = repositoryRoot();
        File source = new File(root, "src/main/java/com/arena/pekaclient");
        check("source root is available to the standalone acceptance suite", source.isDirectory());
        if (source.isDirectory()) {
            String[] scoped = {"gui", "widget", "notes", "hud", "render"};
            List<String> violations = new ArrayList<String>();
            for (String name : scoped) scan(new File(source, name), violations, root);
            check("only Theme.java contains direct GUI colour literals", violations.isEmpty());
            if (!violations.isEmpty()) {
                for (String violation : violations) System.out.println("  " + violation);
            }
            File theme = new File(source, "render/Theme.java");
            check("Theme is the declared colour-token authority", theme.isFile() && text(theme).contains("ACCENT_BLUE")
                    && text(theme).contains("SURFACE_CARD") && text(theme).contains("MOTION_HOVER_MS"));
        }
        System.out.println("hardcoded GUI colors: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static File repositoryRoot() {
        File current = new File(System.getProperty("user.dir", ".")).getAbsoluteFile();
        while (current != null) {
            if (new File(current, "src/main/java").isDirectory()) return current;
            current = current.getParentFile();
        }
        return new File(System.getProperty("user.dir", ".")).getAbsoluteFile();
    }

    private static void scan(File file, List<String> violations, File root) throws IOException {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) scan(child, violations, root);
            return;
        }
        if (!file.getName().endsWith(".java") || "Theme.java".equals(file.getName())) return;
        BufferedReader reader = new BufferedReader(new FileReader(file));
        try {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (ARGB_OR_RGB.matcher(line).find()) {
                    violations.add(relative(root, file) + ":" + lineNumber + " " + line.trim());
                }
            }
        } finally {
            reader.close();
        }
    }

    private static String text(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuilder out = new StringBuilder();
        try {
            String line;
            while ((line = reader.readLine()) != null) out.append(line).append('\n');
        } finally {
            reader.close();
        }
        return out.toString();
    }

    private static String relative(File root, File file) {
        String base = root.getAbsolutePath();
        String value = file.getAbsolutePath();
        return value.startsWith(base) ? value.substring(base.length() + 1) : value;
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++;
        else {
            failed++;
            System.out.println("FAIL " + name);
        }
    }
}
