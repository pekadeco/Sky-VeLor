package com.arena.pekaclient.render;

import com.arena.pekaclient.config.AppleSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

/**
 * Custom ItemRenderer that applies first-person item customization.
 * Extends the vanilla ItemRenderer and overrides renderItemInFirstPerson to apply
 * scale transformations.
 */
public class CustomItemRenderer extends ItemRenderer {

    private final ItemRenderer original;
    private final Minecraft mc;

    public CustomItemRenderer(Minecraft mc, ItemRenderer original) {
        super(mc);
        this.mc = mc;
        this.original = original;
        
        // Copy fields from original using reflection
        try {
            java.lang.reflect.Field[] fields = ItemRenderer.class.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(original);
                field.set(this, value);
            }
        } catch (Exception e) {
            // Failed to copy fields - some customization may not work
        }
    }

    /**
     * Overrides first-person item rendering to apply scale transformations.
     */
    @Override
    public void renderItemInFirstPerson(float partialTicks) {
        // Apply scale transformation if enabled
        if (AppleSettings.firstPersonItemScaleOn) {
            float scale = AppleSettings.effectiveFirstPersonItemScale() / 100.0f;
            if (scale != 1.0f) {
                GlStateManager.pushMatrix();
                GlStateManager.scale(scale, scale, scale);
                
                // Call parent rendering with transformations
                super.renderItemInFirstPerson(partialTicks);
                
                GlStateManager.popMatrix();
                return;
            }
        }
        
        // Apply animation speed if enabled
        if (AppleSettings.firstPersonAnimSpeedOn) {
            float animSpeed = AppleSettings.effectiveFirstPersonAnimSpeed() / 100.0f;
            if (animSpeed != 1.0f) {
                try {
                    // Get the equipped progress fields
                    java.lang.reflect.Field equippedProgressField = ItemRenderer.class.getDeclaredField("equippedProgress");
                    equippedProgressField.setAccessible(true);
                    float equippedProgress = equippedProgressField.getFloat(this);
                    
                    java.lang.reflect.Field prevEquippedProgressField = ItemRenderer.class.getDeclaredField("prevEquippedProgress");
                    prevEquippedProgressField.setAccessible(true);
                    float prevEquippedProgress = prevEquippedProgressField.getFloat(this);
                    
                    // Apply animation speed multiplier
                    equippedProgressField.setFloat(this, equippedProgress * animSpeed);
                    prevEquippedProgressField.setFloat(this, prevEquippedProgress * animSpeed);
                    
                    super.renderItemInFirstPerson(partialTicks);
                    
                    // Reset the fields to original values
                    equippedProgressField.setFloat(this, equippedProgress);
                    prevEquippedProgressField.setFloat(this, prevEquippedProgress);
                    return;
                } catch (Exception e) {
                    // If reflection fails, just render normally
                }
            }
        }
        
        // No customization or both disabled - render normally
        super.renderItemInFirstPerson(partialTicks);
    }

    /**
     * Delegates all other methods to the original ItemRenderer.
     * These overrides ensure that our CustomItemRenderer behaves exactly like
     * the original for all other rendering.
     */
    @Override
    public void renderItem(EntityLivingBase entity, ItemStack heldStack, ItemCameraTransforms.TransformType transform) {
        original.renderItem(entity, heldStack, transform);
    }

    @Override
    public void renderFireInFirstPerson(float partialTicks) {
        original.renderFireInFirstPerson(partialTicks);
    }

    @Override
    public void renderOverlays(float partialTicks) {
        original.renderOverlays(partialTicks);
    }

    @Override
    public void renderMapFirstPerson(float pitch, float yaw, float partialTicks) {
        original.renderMapFirstPerson(pitch, yaw, partialTicks);
    }

    @Override
    public void updateEquippedItem() {
        original.updateEquippedItem();
    }
}
