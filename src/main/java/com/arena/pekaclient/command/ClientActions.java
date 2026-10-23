package com.arena.pekaclient.command;

import com.arena.pekaclient.gui.AppleHubScreen;
import com.arena.pekaclient.gui.ChatEditorScreen;
import com.arena.pekaclient.gui.HudEditorScreen;
import com.arena.pekaclient.notes.QuickNoteScreen;
import com.arena.pekaclient.notes.QuickNotes;
import net.minecraft.client.Minecraft;

/**
 * Screens that chat commands ask for. A command only sets a flag. The client tick opens the screen,
 * because the chat box closes after the command is sent and would close a screen opened inside the command.
 * This is the same pattern RNG Carried Bald uses.
 */
public final class ClientActions {

    private static boolean hubPending;
    private static boolean notesPending;
    private static boolean itemAnimationPending;
    private static boolean chatEditorPending;

    private ClientActions() {
    }

    public static void requestHub() {
        hubPending = true;
    }

    public static void requestQuickNotes() {
        notesPending = true;
    }

    /** Opens the HUD editor directly on its non-draggable Item Animation renderer panel. */
    public static void requestItemAnimation() {
        itemAnimationPending = true;
    }

    /** Opens native Chat Settings after GuiChat has closed. */
    public static void requestChatEditor() {
        chatEditorPending = true;
    }

    /** Called once per client tick, after the chat screen has closed. */
    public static void run(Minecraft mc) {
        if (mc == null) {
            return;
        }
        if (itemAnimationPending) {
            itemAnimationPending = false;
            hubPending = false;
            chatEditorPending = false;
            mc.displayGuiScreen(HudEditorScreen.openItemAnimation(null));
        } else if (chatEditorPending) {
            chatEditorPending = false;
            hubPending = false;
            mc.displayGuiScreen(new ChatEditorScreen(null));
        } else if (hubPending) {
            hubPending = false;
            mc.displayGuiScreen(new AppleHubScreen(null));
        } else if (notesPending) {
            notesPending = false;
            mc.displayGuiScreen(new QuickNoteScreen(QuickNotes.store(), null));
        }
    }
}
