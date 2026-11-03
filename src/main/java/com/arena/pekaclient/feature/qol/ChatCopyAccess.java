package com.arena.pekaclient.feature.qol;

import net.minecraft.client.gui.ChatLine;

/** Access to wrapped chat rows so click-copy acts on the original full message. */
public interface ChatCopyAccess {
    ChatLine lunaria$getChatLineAt(int mouseX, int mouseY);
    int lunaria$getVisibleRowAt(int mouseX, int mouseY);
    int lunaria$getMessageFirstRow(int row);
    int lunaria$getMessageLastRow(int row);
}
