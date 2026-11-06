package com.arena.pekaclient.gui;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/** Source wiring checks for GUI callbacks; actual disk writes have separate ConfigFilesTests. */
public final class AutoSaveContractTests {
    private static String source(String name) throws Exception {
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/arena/pekaclient/gui/" + name + ".java")), StandardCharsets.UTF_8);
    }
    public static void main(String[] args) throws Exception {
        String main = source("AppleSettingsScreen");
        if (main.contains("save-settings") || main.contains("drawSaveSettings") || main.contains("showToast(\"Saved\")"))
            throw new AssertionError("manual save UI remains");
        if (!main.contains("InlineSettingsBindings.setBoolean(id, value)") || !main.contains("InlineSettingsBindings.setInt(id, value)")
                || !main.contains("InlineSettingsBindings.setChoice(id, value)") || !main.contains("AppleSettings.save()"))
            throw new AssertionError("main control/save wiring missing");
        for (String editor : new String[]{"HudEditorScreen", "DungeonHudEditorScreen", "ItemAnimationEditorScreen",
                "CommandKeybindEditorScreen", "ChatEditorScreen"}) {
            String text = source(editor);
            if (!text.contains("onGuiClosed()") || !text.contains("AppleSettings.save()"))
                throw new AssertionError(editor + " does not save on close");
        }
        System.out.println("autosave source contracts passed");
    }
}
