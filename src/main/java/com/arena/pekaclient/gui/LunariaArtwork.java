package com.arena.pekaclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** Shared Lunaria artwork renderer. Loading art is cover-fit; pixel logo art deliberately uses nearest filtering. */
public final class LunariaArtwork {
    private static final ResourceLocation LOADING = new ResourceLocation("lunaria", "textures/gui/lunaria_loading_background.jpg");
    private static final ResourceLocation LOGO = new ResourceLocation("lunaria", "textures/gui/lunaria_logo.png");
    private static final ResourceLocation MOON = new ResourceLocation("lunaria", "textures/gui/lunaria_moon.png");
    private static final float LOADING_W = 1408F;
    private static final float LOADING_H = 768F;
    private static final float LOGO_W = 744F;
    private static final float LOGO_H = 744F;
    private static final float MOON_W = 360F;
    private static final float MOON_H = 289F;

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

    /** Draws the full supplied logo with crisp, nearest-neighbour pixels and its retained transparent glow. */
    public static void drawLogo(float centerX, float topY, float width, float alpha) {
        if (width <= 0F) return;
        drawNearest(LOGO, centerX - width * .5F, topY, width, width * LOGO_H / LOGO_W, alpha, LOGO_W, LOGO_H);
    }

    /** Draws the moon crop used instead of a text initial in the settings header. */
    public static void drawMoon(float centerX, float centerY, float size, float alpha) {
        if (size <= 0F) return;
        float h = size * MOON_H / MOON_W;
        drawNearest(MOON, centerX - size * .5F, centerY - h * .5F, size, h, alpha, MOON_W, MOON_H);
    }

    private static void drawNearest(ResourceLocation texture, float x, float y, float width, float height,
                                    float alpha, float textureWidth, float textureHeight) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GlStateManager.enableBlend();
        GlStateManager.color(1F, 1F, 1F, clamp(alpha));
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        Gui.drawModalRectWithCustomSizedTexture(Math.round(x), Math.round(y), 0F, 0F,
                Math.max(1, Math.round(width)), Math.max(1, Math.round(height)), textureWidth, textureHeight);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    private static float clamp(float value) {
        return Math.max(0F, Math.min(1F, value));
    }
}
