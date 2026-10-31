package com.arena.pekaclient.mixin;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.feature.qol.ChatLengthPolicy;
import com.arena.pekaclient.feature.ChatChannelTabs;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Extends only the chat text-field limit; the text field's position and size stay unchanged. */
@Mixin(GuiChat.class)
public abstract class MixinGuiChat {
    @ModifyArg(
            method = "initGui",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiTextField;setMaxStringLength(I)V"),
            index = 0,
            require = 1
    )
    private int lunaria$extendChatInputLimit(int vanillaLimit) {
        return ChatLengthPolicy.maximumLength(AppleSettings.chatLengthEnabled);
    }

    /**
     * When our replacement chat owns the overlay, vanilla's hit test still targets its hidden row cache and can
     * resolve the wrong hovered component. Let the custom logical rows own hover events in that mode only.
     */
    @Redirect(
            method = "drawScreen",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiNewChat;getChatComponent(II)Lnet/minecraft/util/IChatComponent;"),
            require = 1
    )
    private IChatComponent lunaria$skipStaleVanillaChatHover(GuiNewChat chat, int mouseX, int mouseY) {
        if (ChatChannelTabs.isNativeRendererActive()) return null;
        return chat.getChatComponent(mouseX, mouseY);
    }

    /** Restores native SHOW_ITEM / SHOW_TEXT tooltips after our custom chat has drawn its rows. */
    @Inject(method = "drawScreen", at = @At("TAIL"), require = 1)
    private void lunaria$renderNativeChatHover(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (ChatChannelTabs.isNativeRendererActive()) {
            ChatChannelTabs.renderHoverAtMouse((GuiChat) (Object) this, mouseX, mouseY);
        }
    }
}
