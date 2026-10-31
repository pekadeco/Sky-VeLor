/*
 * This modified implementation follows the Better NEF / NotEnoughFakepixel behavior.
 * Maintained by ii8we; review LICENSE and NOTICE.md before use or redistribution.
 * Original code and third-party components remain subject to their applicable license terms.
 */

package com.arena.pekaclient.mixin;

import com.arena.pekaclient.config.AppleSettings;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {
    @Inject(method = "hurtCameraEffect", at = @At("HEAD"), cancellable = true, require = 1)
    private void lunaria$disableHurtCamera(float partialTicks, CallbackInfo ci) {
        if (AppleSettings.noHurtCamEnabled) ci.cancel();
    }
}
