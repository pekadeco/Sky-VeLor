package com.arena.pekaclient.gui;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Ensures the Chat Editor controls the one setting used by both native 1.8.9 chat-length mixins. */
public final class ChatEditorInputLengthOwnerTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        String editor = read("src/main/java/com/arena/pekaclient/gui/ChatEditorScreen.java");
        String catalog = read("src/main/java/com/arena/pekaclient/gui/ReferenceSettingsCatalog.java");
        String settingsScreen = read("src/main/java/com/arena/pekaclient/gui/AppleSettingsScreen.java");
        String inputMixin = read("src/main/java/com/arena/pekaclient/mixin/MixinGuiChat.java");
        String packetMixin = read("src/main/java/com/arena/pekaclient/mixin/MixinC01PacketChatMessage.java");

        check("Chat Editor uses the active persisted chat-length owner",
                editor.contains("AppleSettings.chatLengthEnabled"));
        check("Chat Editor visibly labels the optional 256-character switch",
                editor.contains("Chat length (256 characters)") && editor.contains("ChatEditorLayout.MESSAGES"));
        check("Chat length remains reachable through Misc to the retained Chat Editor",
                catalog.contains("editor(\"chat-editor\", \"Chat Editor\"")
                        && catalog.contains("\"open-chat-editor\"")
                        && settingsScreen.contains("\"open-chat-editor\"")
                        && settingsScreen.contains("new ChatEditorScreen(this)"));
        check("Chat Editor has no stale parallel chat-length owner",
                !editor.contains("ChatQolSettings.extendedChatLength"));
        check("input mixin reads the same owner", inputMixin.contains("AppleSettings.chatLengthEnabled"));
        check("packet mixin reads the same owner", packetMixin.contains("AppleSettings.chatLengthEnabled"));

        System.out.println("chat editor input-length owner: passed " + passed + ", failed " + failed);
        if (failed != 0) throw new AssertionError(failed + " chat-length ownership checks failed");
    }

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8);
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.err.println("FAIL " + name);
        }
    }
}
