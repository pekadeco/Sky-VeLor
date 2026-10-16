package net.minecraft.client.multiplayer;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.entity.EntityPlayerSP;

/** Records the window clicks that the terminal panel sends. */
public class PlayerControllerMP {
    public static final List<String> CLICKS = new ArrayList<String>();
    public static List<Runnable> HOOKS = new ArrayList<Runnable>();

    public void windowClick(int windowId, int slotId, int mouseButton, int mode, EntityPlayerSP player) {
        CLICKS.add(windowId + ":" + slotId + ":" + mouseButton + ":" + mode);
        for (int i = 0; i < HOOKS.size(); i++) {
            HOOKS.get(i).run();
        }
    }
}
