package com.arena.pekaclient.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Draws the 2D face of a player skin: the 8x8 face with its hat layer on top. Uses fixed-function textured quads
 * only (POSITION_TEX), so it works on Android GL 2.1 / gl4es as well as desktop GL.
 *
 * Caller contract: call inside a UIRender frame. This method flushes any batched UI first and restores texture
 * and alpha state to the UI defaults (texture off, alpha test off) before it returns.
 */
public final class SkinHead {

    /** Face and hat positions on a 64x64 skin texture. */
    private static final float TEX = 64f;

    private SkinHead() {
    }

    /** Draws the head with its top-left corner at (x, y), {@code size} pixels square. */
    public static void draw(ResourceLocation skin, float x, float y, float size, float alpha) {
        if (skin == null) {
            return;
        }
        UIRender.flush();
        Minecraft.getMinecraft().getTextureManager().bindTexture(skin);
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1f, 1f, 1f, UIRender.clamp01(alpha));
        quad(x, y, size, 8f, 8f);
        quad(x, y, size, 40f, 8f);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.disableAlpha();
        GlStateManager.disableTexture2D();
    }

    /** One 8x8 texel block starting at (u, v) on the skin texture, drawn into the square at (x, y). */
    private static void quad(float x, float y, float size, float u, float v) {
        float u0 = u / TEX;
        float v0 = v / TEX;
        float u1 = (u + 8f) / TEX;
        float v1 = (v + 8f) / TEX;
        WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        wr.pos(x, y + size, 0.0D).tex(u0, v1).endVertex();
        wr.pos(x + size, y + size, 0.0D).tex(u1, v1).endVertex();
        wr.pos(x + size, y, 0.0D).tex(u1, v0).endVertex();
        wr.pos(x, y, 0.0D).tex(u0, v0).endVertex();
        Tessellator.getInstance().draw();
    }
}
