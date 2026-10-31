/*
 * Lunaria client hook for the model-outline pass.
 * See LICENSE and NOTICE.md for project attribution and applicable source notices.
 */
package com.arena.pekaclient.mixin;

import com.arena.pekaclient.render.MobHighlight;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RendererLivingEntity.class)
public abstract class MixinRendererLivingEntity {
    @Shadow
    protected ModelBase mainModel;

    @Inject(
            method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/RendererLivingEntity;renderLayers(Lnet/minecraft/entity/EntityLivingBase;FFFFFFF)V",
                    shift = At.Shift.AFTER
            ),
            require = 1
    )
    private void lunaria$renderStarredMobModelOutline(EntityLivingBase entity,
                                                       double x, double y, double z,
                                                       float entityYaw, float partialTicks,
                                                       CallbackInfo ci) {
        if (entity == null || mainModel == null) return;
        float limbSwing = entity.limbSwing - entity.limbSwingAmount * (1.0F - partialTicks);
        float limbSwingAmount = entity.prevLimbSwingAmount
                + (entity.limbSwingAmount - entity.prevLimbSwingAmount) * partialTicks;
        float ageInTicks = entity.ticksExisted + partialTicks;
        float headYaw = entity.prevRotationYawHead
                + (entity.rotationYawHead - entity.prevRotationYawHead) * partialTicks;
        float headPitch = entity.prevRotationPitch
                + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
        MobHighlight.renderModelOutline(entity, mainModel, limbSwing, limbSwingAmount,
                ageInTicks, headYaw, headPitch, 0.0625F);
    }
}
