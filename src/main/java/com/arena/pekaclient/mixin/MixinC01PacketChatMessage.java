package com.arena.pekaclient.mixin;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.feature.qol.ChatLengthPolicy;
import net.minecraft.network.play.client.C01PacketChatMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Keeps the outbound packet constructor from truncating enabled extended messages at 100 chars. */
@Mixin(C01PacketChatMessage.class)
public abstract class MixinC01PacketChatMessage {
    @ModifyConstant(
            method = "<init>(Ljava/lang/String;)V",
            constant = @Constant(intValue = 100),
            require = 1
    )
    private int lunaria$extendOutgoingChatLimit(int vanillaLimit) {
        return ChatLengthPolicy.maximumLength(AppleSettings.chatLengthEnabled);
    }
}
