/*
 * This modified implementation follows the Better NEF / NotEnoughFakepixel behavior.
 * Maintained by ii8we; review LICENSE and NOTICE.md before use or redistribution.
 * Original code and third-party components remain subject to their applicable license terms.
 */

package com.arena.pekaclient.mixin;

import com.arena.pekaclient.config.AppleSettings;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EffectRenderer.class)
public abstract class MixinEffectRenderer {
    @Inject(method = "addBlockDestroyEffects", at = @At("HEAD"), cancellable = true, require = 1)
    private void lunaria$hideBlockDestroyParticles(BlockPos pos, IBlockState state, CallbackInfo ci) {
        if (AppleSettings.hideBlockBreakingParticles) ci.cancel();
    }

    @Inject(method = "addBlockHitEffects", at = @At("HEAD"), cancellable = true, require = 1)
    private void lunaria$hideBlockHitParticles(BlockPos pos, EnumFacing side, CallbackInfo ci) {
        if (AppleSettings.hideBlockBreakingParticles) ci.cancel();
    }
}
