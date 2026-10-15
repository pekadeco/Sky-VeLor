package com.arena.pekaclient.notes;

import net.minecraft.client.Minecraft;

import java.io.File;

/** One shared quick-note store for the session. The pages live in skyvelor-notes.txt in the game folder. */
public final class QuickNotes {

    public static final String FILE_NAME = "skyvelor-notes.txt";
    private static QuickNoteStore store;

    private QuickNotes() {
    }

    public static synchronized QuickNoteStore store() {
        if (store == null) {
            store = new QuickNoteStore(new File(Minecraft.getMinecraft().mcDataDir, FILE_NAME));
        }
        return store;
    }
}
