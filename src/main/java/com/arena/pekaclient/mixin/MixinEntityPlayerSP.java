/*
 * Lunaria client-chat integration. Maintained by ii8we, whiteshadow, and pekadeco.
 * See the root LICENSE and NOTICE.md for applicable licensing and attribution.
 */
package com.arena.pekaclient.mixin;

import com.arena.pekaclient.feature.ChatChannelTabs;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures mod-generated/local feedback for the native chat's independent history. */
@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP {
    @Inject(method = "addChatMessage", at = @At("HEAD"), require = 1)
    private void lunaria$captureClientSideMessage(IChatComponent component, CallbackInfo ci) {
        ChatChannelTabs.captureClientSideMessage(component);
    }
}
