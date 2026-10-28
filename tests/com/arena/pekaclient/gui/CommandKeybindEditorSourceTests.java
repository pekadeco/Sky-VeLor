package com.arena.pekaclient.gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/** Source-level contract for the deliberately custom, opaque Command Keybind editor surface. */
public final class CommandKeybindEditorSourceTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        File source = new File(repositoryRoot(), "src/main/java/com/arena/pekaclient/gui/CommandKeybindEditorScreen.java");
        String text = source.isFile() ? read(source) : "";
        check("Command Keybind editor source exists", source.isFile());
        check("editor is solid and undimmed like Item Animation", text.contains("Theme.ITEM_ANIMATION_PANEL")
                && text.contains("roundRectBorder") && !text.contains("drawDefaultBackground"));
        check("editor exposes add edit delete capture clear and reset paths", text.contains("new AppleButton(\"Add\"")
                && text.contains("new AppleButton(\"Edit\"") && text.contains("new AppleButton(\"Delete\"")
                && text.contains("new AppleButton(\"Reset\"") && text.contains("AppleKeybindButton")
                && text.contains("Keyboard.KEY_BACK") && text.contains("Keyboard.KEY_ESCAPE"));
        check("editor visibly warns command, Sky VeLor and Minecraft collisions", text.contains("hasOtherEntryWithKey")
                && text.contains("Sky VeLor") && text.contains("Minecraft ") && text.contains("collisionWarning"));
        check("editor persists through established settings save", text.contains("AppleSettings.save()")
                && text.contains("doesGuiPauseGame"));
        System.out.println("command keybind editor source: passed " + passed + ", failed " + failed);
        if (failed > 0) throw new AssertionError("command keybind editor source failures: " + failed);
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

    private static void check(String name, boolean ok) {
        if (ok) passed++;
        else { failed++; System.err.println("FAIL " + name); }
    }
}
