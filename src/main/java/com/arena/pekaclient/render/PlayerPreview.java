package com.arena.pekaclient.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;

/**
 * Draws a living entity (the local player, for the doll HUD) inside a GUI, with an explicit yaw.
 *
 * This follows the vanilla {@link GuiInventory#drawEntityOnScreen} sequence: the same matrix steps, the same
 * lighting, and the same temporary shadow and view-yaw settings. The difference is that the yaw is given
 * directly instead of being derived from the mouse. It uses only the public {@link RenderManager} calls that
 * vanilla GUIs use. No renderer, event, model or layer is changed, and no entity is added or moved in the world.
 *
 * Every field this class changes on the entity or the render manager is restored afterwards, and the GL
 * state is put back to the custom-drawing state that {@link UIRender} expects.
 */
public final class PlayerPreview {

    /** Vanilla's view yaw for GUI entity drawing. Applied the same way vanilla applies it. */
    private static final float GUI_VIEW_YAW = 180f;

    private PlayerPreview() {
    }

    /**
     * Draws {@code entity} centred on {@code cx}, with its feet on {@code feetY}, in the current GUI coordinate space.
     *
     * @param size    pixels per block (about 26 gives a figure a little under 50 px tall)
     * @param yawDeg  horizontal facing in degrees. 0 is the vanilla default facing
     */
    public static void draw(float cx, float feetY, float size, float yawDeg, EntityLivingBase entity) {
        if (entity == null) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        RenderManager rm = mc.getRenderManager();
        if (rm == null) {
            return;
        }

        // Hand the GL state to vanilla entity rendering: flush our batch, turn on texturing and alpha test.
        // The model needs depth testing to hide its own limbs correctly. The depth buffer is cleared first so
        // the world's depth cannot hide the figure. Depth is turned off again afterwards, as UI drawing expects.
        UIRender.flush();
        GlStateManager.pushMatrix();
        GlStateManager.enableDepth();
        GlStateManager.clear(256);
        GlStateManager.enableColorMaterial();
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.color(1f, 1f, 1f, 1f);

        // Save the entity fields that the draw changes, so they can be restored in the finally block.
        float savedBodyYaw = entity.renderYawOffset;
        float savedYaw = entity.rotationYaw;
        float savedPitch = entity.rotationPitch;
        float savedPrevHead = entity.prevRotationYawHead;
        float savedHead = entity.rotationYawHead;
        float savedViewYaw = rm.playerViewY;
        boolean savedShadow = rm.isRenderShadow();

        try {
            GlStateManager.translate(cx, feetY, 50f);
            GlStateManager.scale(-size, size, size);
            GlStateManager.rotate(180f, 0f, 0f, 1f);

            GlStateManager.rotate(135f, 0f, 1f, 0f);
            RenderHelper.enableStandardItemLighting();
            GlStateManager.rotate(-135f, 0f, 1f, 0f);

            // The yaw is set on the entity, so the body, head and limbs all face the requested angle.
            float yaw = normalise(yawDeg);
            entity.renderYawOffset = yaw;
            entity.rotationYaw = yaw;
            entity.rotationPitch = 0f;
            entity.rotationYawHead = yaw;
            entity.prevRotationYawHead = yaw;

            rm.setPlayerViewY(GUI_VIEW_YAW);
            rm.setRenderShadow(false);
            rm.renderEntityWithPosYaw(entity, 0.0D, 0.0D, 0.0D, 0f, 1f);
        } finally {
            rm.setRenderShadow(savedShadow);
            rm.setPlayerViewY(savedViewYaw);
            entity.renderYawOffset = savedBodyYaw;
            entity.rotationYaw = savedYaw;
            entity.rotationPitch = savedPitch;
            entity.prevRotationYawHead = savedPrevHead;
            entity.rotationYawHead = savedHead;

            GlStateManager.popMatrix();
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.disableTexture2D();
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);

            // Back to the custom-drawing state that UIRender.beginFrame sets.
            GlStateManager.disableColorMaterial();
            // UI drawing is 2D and runs with depth testing off (see UIRender.beginFrame).
            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.disableTexture2D();
            GlStateManager.disableAlpha();
            GlStateManager.color(1f, 1f, 1f, 1f);
        }
    }

    /** Wraps an angle into the range -180 .. 180. */
    public static float normalise(float deg) {
        float d = deg % 360f;
        if (d > 180f) {
            d -= 360f;
        } else if (d < -180f) {
            d += 360f;
        }
        return d;
    }
}
