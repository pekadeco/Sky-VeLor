/*
 * This modified implementation follows the Better NEF / NotEnoughFakepixel behavior.
 * Maintained by ii8we; review LICENSE and NOTICE.md before use or redistribution.
 * Original code and third-party components remain subject to their applicable license terms.
 */

package com.arena.pekaclient.mixin;

import com.arena.pekaclient.feature.qol.ChatLineAccess;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ChatLine.class)
public abstract class MixinChatLine implements ChatLineAccess {
    @Unique private IChatComponent lunaria$fullMessage;

    @Override
    public IChatComponent lunaria$getFullMessage() {
        return lunaria$fullMessage;
    }

    @Override
    public void lunaria$setFullMessage(IChatComponent message) {
        this.lunaria$fullMessage = message;
    }
}
