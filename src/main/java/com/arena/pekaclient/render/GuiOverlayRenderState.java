package com.arena.pekaclient.render;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/** Restores the GUI render state touched by small textured overlays. */
public final class GuiOverlayRenderState {
    // GUI render snapshots are captured on Minecraft's render thread. Reuse these direct buffers so
    // opening/rendering a HUD overlay does not allocate native buffers every frame.
    private static final IntBuffer SCISSOR_BUFFER = BufferUtils.createIntBuffer(16);
    private static final FloatBuffer COLOR_BUFFER = BufferUtils.createFloatBuffer(16);

    private final boolean textureEnabled;
    private final boolean lightingEnabled;
    private final boolean blendEnabled;
    private final boolean depthEnabled;
    private final boolean alphaEnabled;
    private final boolean cullEnabled;
    private final boolean scissorEnabled;
    private final int scissorX;
    private final int scissorY;
    private final int scissorWidth;
    private final int scissorHeight;
    private final boolean depthMask;
    private final int shadeModel;
    private final int textureBinding;
    private final int blendSrcRgb;
    private final int blendDstRgb;
    private final int blendSrcAlpha;
    private final int blendDstAlpha;
    private final float red;
    private final float green;
    private final float blue;
    private final float alpha;

    public GuiOverlayRenderState() {
        textureEnabled = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        lightingEnabled = GL11.glIsEnabled(GL11.GL_LIGHTING);
        blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        alphaEnabled = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        // LWJGL 2's glGetInteger(IntBuffer) wrapper enforces a 16-element buffer even for
        // GL_SCISSOR_BOX (which writes only four integers). A 4-element buffer crashes the
        // first overlay frame on Forge 1.8.9 / LWJGL 2.9.4 before the guard can restore state.
        SCISSOR_BUFFER.clear();
        GL11.glGetInteger(GL11.GL_SCISSOR_BOX, SCISSOR_BUFFER);
        scissorX = SCISSOR_BUFFER.get(0);
        scissorY = SCISSOR_BUFFER.get(1);
        scissorWidth = SCISSOR_BUFFER.get(2);
        scissorHeight = SCISSOR_BUFFER.get(3);
        depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        shadeModel = GL11.glGetInteger(GL11.GL_SHADE_MODEL);
        textureBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        blendSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        blendDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        blendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        blendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        // LWJGL 2 checks glGetFloat buffers for room for 16 values regardless of the
        // queried state. GL_CURRENT_COLOR only writes four values, but a 4-float
        // buffer still throws IllegalArgumentException on Forge 1.8.9 / LWJGL 2.9.4.
        COLOR_BUFFER.clear();
        GL11.glGetFloat(GL11.GL_CURRENT_COLOR, COLOR_BUFFER);
        red = COLOR_BUFFER.get(0);
        green = COLOR_BUFFER.get(1);
        blue = COLOR_BUFFER.get(2);
        alpha = COLOR_BUFFER.get(3);
    }

    /** Prepares a texture/font overlay after container slots have already been drawn. */
    public void begin() {
        GlStateManager.enableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Restores all state captured by this guard, including the active atlas and texture blend factors. */
    public void restore() {
        GlStateManager.color(red, green, blue, alpha);
        GlStateManager.tryBlendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
        setEnabled(GL11.GL_TEXTURE_2D, textureEnabled);
        setEnabled(GL11.GL_LIGHTING, lightingEnabled);
        setEnabled(GL11.GL_BLEND, blendEnabled);
        setEnabled(GL11.GL_DEPTH_TEST, depthEnabled);
        setEnabled(GL11.GL_CULL_FACE, cullEnabled);
        GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);
        if (scissorEnabled) GL11.glEnable(GL11.GL_SCISSOR_TEST); else GL11.glDisable(GL11.GL_SCISSOR_TEST);
        if (alphaEnabled) GlStateManager.enableAlpha(); else GlStateManager.disableAlpha();
        GlStateManager.depthMask(depthMask);
        GlStateManager.shadeModel(shadeModel);
        GlStateManager.bindTexture(textureBinding);
    }

    private static void setEnabled(int capability, boolean enabled) {
        if (capability == GL11.GL_TEXTURE_2D) {
            if (enabled) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
        } else if (capability == GL11.GL_LIGHTING) {
            if (enabled) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
        } else if (capability == GL11.GL_BLEND) {
            if (enabled) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
        } else if (capability == GL11.GL_DEPTH_TEST) {
            if (enabled) GlStateManager.enableDepth(); else GlStateManager.disableDepth();
        } else if (capability == GL11.GL_CULL_FACE) {
            if (enabled) GlStateManager.enableCull(); else GlStateManager.disableCull();
        }
    }
}
