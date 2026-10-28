package com.arena.pekaclient.assist;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/** Source audit: the shortcut stays a direct, screen-gated user input helper rather than an automation path. */
public final class WardrobeShortcutControllerSourceTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        File source = new File(repositoryRoot(), "src/main/java/com/arena/pekaclient/assist/WardrobeShortcutController.java");
        String text = source.isFile() ? read(source) : "";
        check("native Wardrobe controller source exists", source.isFile());
        check("controller requires physical keyboard edges and an actual GuiChest ContainerChest", text.contains("Keyboard.isKeyDown")
                && text.contains("Keyboard.getEventKeyState") && text.contains("Keyboard.isRepeatEvent")
                && text.contains("GuiChest") && text.contains("ContainerChest") && text.contains("digitForKey"));
        check("controller sends only user /wardrobe chat or one normal visible-menu click", text.contains("sendChatMessage(\"/wardrobe\")")
                && text.contains("windowClick") && text.contains("WardrobeShortcutPolicy") && text.contains("FIRST_WARDROBE_SLOT")
                && text.contains("LAST_WARDROBE_SLOT"));
        check("controller has no movement, packet, automation loop, or world mutation path", !text.contains("sendPacket")
                && !text.contains("setPosition") && !text.contains("setLocationAndAngles") && !text.contains("clickMouse")
                && !text.contains("Thread.sleep") && !text.contains("new Timer") && !text.contains(".schedule("));
        System.out.println("wardrobe shortcut controller source: passed " + passed + ", failed " + failed);
        if (failed > 0) throw new AssertionError("wardrobe shortcut controller source failures: " + failed);
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
        else {
            failed++;
            System.err.println("FAIL " + name);
        }
    }
}
