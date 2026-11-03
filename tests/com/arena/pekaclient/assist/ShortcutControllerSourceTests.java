package com.arena.pekaclient.assist;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/** Source-level boundary for the direct physical Pets command shortcut. */
public final class ShortcutControllerSourceTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        File source = new File(repositoryRoot(), "src/main/java/com/arena/pekaclient/assist/ShortcutController.java");
        String text = source.isFile() ? read(source) : "";
        check("Pets shortcut controller source exists", source.isFile());
        check("Pets uses its configurable key and one physical down edge", text.contains("PETS_SHORTCUT_KEY")
                && text.contains("Keyboard.isKeyDown") && text.contains("petsWasDown") && text.contains("/pets"));
        check("Pets is enabled, player, screen, and retained server gated", text.contains("AssistSettings.petsShortcut")
                && text.contains("mc.thePlayer == null") && text.contains("mc.currentScreen != null") && text.contains("isFakepixel"));
        check("Pets has no packet, movement, scheduler, or repeated KeyBinding loop", !text.contains("sendPacket")
                && !text.contains("setPosition") && !text.contains("Thread.sleep") && !text.contains(".schedule(")
                && !text.contains("isPressed()"));
        System.out.println("shortcut controller source: passed " + passed + ", failed " + failed);
        if (failed > 0) throw new AssertionError("shortcut controller source failures: " + failed);
    }

    private static File repositoryRoot() {
        File current = new File(System.getProperty("user.dir", ".")).getAbsoluteFile();
        while (current != null) {
            if (new File(current, "src/main/java").isDirectory()) return current;
            current = current.getParentFile();
        }
        return new File(System.getProperty("user.dir", ".")).getAbsoluteFile();
    }

    private static String read(File file) throws Exception {
        StringBuilder text = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        try {
            String line;
            while ((line = reader.readLine()) != null) text.append(line).append('\n');
        } finally {
            reader.close();
        }
        return text.toString();
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++;
        else { failed++; System.err.println("FAIL " + name); }
    }
}
