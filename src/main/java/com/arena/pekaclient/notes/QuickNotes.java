package com.arena.pekaclient.notes;

import com.arena.pekaclient.config.ConfigFiles;
import net.minecraft.client.Minecraft;

import java.io.File;

/** One shared quick-note store for the session. Legacy Lunaria pages load into a Lunaria save target. */
public final class QuickNotes {

    public static final String FILE_NAME = "lunaria-notes.txt";
    public static final String PRIOR_RELEASE_LEGACY_FILE_NAME = "skyvelor-notes.txt";
    private static QuickNoteStore store;

    private QuickNotes() {
    }

    public static synchronized QuickNoteStore store() {
        if (store == null) {
            File dir = Minecraft.getMinecraft().mcDataDir;
            File current = new File(dir, FILE_NAME);
            File source = ConfigFiles.resolveRead(dir, FILE_NAME, PRIOR_RELEASE_LEGACY_FILE_NAME);
            store = new QuickNoteStore(current, source);
        }
        return store;
    }
}
