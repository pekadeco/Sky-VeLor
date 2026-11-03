package com.arena.pekaclient.gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/** Verifies that the visual-review renderer stays tools-only and consumes real production layout models. */
public final class AppleLayoutRendererSourceTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        File root = repositoryRoot();
        File source = new File(root, "tools/apple-layout-renderer/AppleSettingsLayoutRenderer.java");
        File runner = new File(root, "tools/run_apple_layout_renderer.sh");
        check("Java2D renderer lives under tools", source.isFile());
        check("renderer has a tools-only runner", runner.isFile());
        String text = source.isFile() ? read(source) : "";
        String runnerText = runner.isFile() ? read(runner) : "";
        check("runner enables Java2D headless mode and keeps previews outside dist", runnerText.contains("-Djava.awt.headless=true")
                && runnerText.contains("build/apple-layout-previews") && !runnerText.contains("OUTPUT=\"$ROOT/dist"));
        check("renderer uses real window, header, rail, control and catalog models",
                text.contains("ReferenceSettingsWindowLayout.layout") && text.contains("SettingsShellLayout.layoutHeader")
                        && text.contains("SettingsRailLayout.layout") && text.contains("SettingsControlLayout.toggleX")
                        && text.contains("ReferenceSettingsCatalog.cards"));
        check("renderer checks the 24 px header actions and current nine destinations",
                text.contains("isHeaderTargetValid") && text.contains("ReferenceSettingsCatalog.TABS.length == 9"));
        check("renderer produces Home, Dungeon, and scale-four evidence", text.contains("home-scale1.png")
                && text.contains("dungeon-scale1.png") && text.contains("home-scale4.png"));
        check("renderer excludes retired traffic-light actions", !text.contains("window-red") && !text.contains("window-yellow")
                && !text.contains("window-green"));
        done();
    }

    private static File repositoryRoot() {
        File current = new File(System.getProperty("user.dir", ".")).getAbsoluteFile();
        while (current != null) {
            if (new File(current, "src/main/java").isDirectory()) return current;
            current = current.getParentFile();
        }
        return new File(System.getProperty("user.dir", ".")).getAbsoluteFile();
    }

    private static String read(File file) throws IOException {
        StringBuilder out = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        try {
            String line;
            while ((line = reader.readLine()) != null) out.append(line).append('\n');
        } finally {
            reader.close();
        }
        return out.toString();
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++;
        else { failed++; System.out.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("Apple layout renderer source: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
