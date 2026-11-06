package com.arena.pekaclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** Shared Lunaria artwork renderer. Loading art is cover-fit; pixel logo art deliberately uses nearest filtering. */
public final class LunariaArtwork {
    private static final ResourceLocation LOADING = new ResourceLocation("lunaria", "textures/gui/lunaria_loading_background.jpg");
    private static ResourceLocation moonTexture;
    private static final float LOADING_W = 1408F;
    private static final float LOADING_H = 768F;

    private LunariaArtwork() {
    }

    /** Draws the supplied wide illustration aspect-fill, without stretching it at desktop or Pojav aspect ratios. */
    public static void drawLoadingCover(int x, int y, int width, int height, float alpha) {
        if (width <= 0 || height <= 0) return;
        Minecraft.getMinecraft().getTextureManager().bindTexture(LOADING);
        GlStateManager.enableBlend();
        GlStateManager.color(1F, 1F, 1F, clamp(alpha));
        float sourceAspect = LOADING_W / LOADING_H;
        float targetAspect = width / (float) height;
        float u = 0F;
        float v = 0F;
        float cropW = LOADING_W;
        float cropH = LOADING_H;
        if (targetAspect > sourceAspect) {
            cropH = LOADING_W / targetAspect;
            v = (LOADING_H - cropH) * .5F;
        } else {
            cropW = LOADING_H * targetAspect;
            u = (LOADING_W - cropW) * .5F;
        }
        // Unlike drawModalRectWithCustomSizedTexture, this supplies the cropped source extent as well as the
        // destination extent. That preserves cover-fit behaviour on 16:9 desktops and narrow Pojav displays.
        Gui.drawScaledCustomSizeModalRect(x, y, u, v, Math.max(1, Math.round(cropW)), Math.max(1, Math.round(cropH)),
                width, height, LOADING_W, LOADING_H);
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    /** Draws the same baked transparent pixel moon at Home and on loading screens. */
    public static void drawLogo(float centerX, float topY, float width, float alpha) {
        drawPixelMoon(centerX - width * .5F, topY, width, alpha);
    }

    /** Draws the same baked moon centred in the header. */
    public static void drawMoon(float centerX, float centerY, float size, float alpha) {
        drawPixelMoon(centerX - size * .5F, centerY - size * .5F, size, alpha);
    }

    private static void drawPixelMoon(float x, float y, float size, float alpha) {
        if (size <= 0F) return;
        boolean textureWasEnabled = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        GlStateManager.enableTexture2D();
        if (moonTexture == null) {
            DynamicTexture texture = new DynamicTexture(PixelMoon.SIZE, PixelMoon.SIZE);
            int[] baked = PixelMoon.bake();
            System.arraycopy(baked, 0, texture.getTextureData(), 0, baked.length);
            texture.updateDynamicTexture();
            moonTexture = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("lunaria_pixel_moon", texture);
            Minecraft.getMinecraft().getTextureManager().bindTexture(moonTexture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        } else {
            Minecraft.getMinecraft().getTextureManager().bindTexture(moonTexture);
        }
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1F, 1F, 1F, clamp(alpha));
        Gui.drawScaledCustomSizeModalRect(Math.round(x), Math.round(y), 0F, 0F,
                PixelMoon.SIZE, PixelMoon.SIZE, Math.max(1, Math.round(size)), Math.max(1, Math.round(size)),
                PixelMoon.SIZE, PixelMoon.SIZE);
        GlStateManager.color(1F, 1F, 1F, 1F);
        if (!textureWasEnabled) GlStateManager.disableTexture2D();
    }

    private static float clamp(float value) {
        return Math.max(0F, Math.min(1F, value));
    }
}
