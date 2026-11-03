package com.arena.pekaclient.feature.qol;

import net.minecraft.util.IChatComponent;

/** Stores the original message on each wrapped chat row. */
public interface ChatLineAccess {
    IChatComponent lunaria$getFullMessage();
    void lunaria$setFullMessage(IChatComponent message);
}
