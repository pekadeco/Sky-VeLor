package com.arena.pekaclient.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.EXTPackedDepthStencil;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

/** Stencil-based model silhouette outline used by the Starred Mobs Outline display mode. */
public final class StarredMobOutlineRenderer {
    private StarredMobOutlineRenderer() {
    }

    public static boolean isAvailable() {
        try {
            return GLContext.getCapabilities().GL_EXT_framebuffer_object
                    && GLContext.getCapabilities().GL_EXT_packed_depth_stencil;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public static void render(EntityLivingBase entity, ModelBase model, float limbSwing,
                              float limbSwingAmount, float ageInTicks, float headYaw,
                              float headPitch, float scaleFactor, int argb) {
        if (entity == null || model == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.getFramebuffer() == null) return;
        if (!isAvailable() || !ensureStencilBuffer(mc)) {
            renderFallback(entity, model, limbSwing, limbSwingAmount, ageInTicks,
                    headYaw, headPitch, scaleFactor, argb);
            return;
        }

        int oldHurtTime = entity.hurtTime;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            // Keep the outline from inheriting the red flash of the standard hurt renderer.
            entity.hurtTime = 0;
            float alpha = ((argb >>> 24) & 0xFF) / 255.0F;
            float red = ((argb >>> 16) & 0xFF) / 255.0F;
            float green = ((argb >>> 8) & 0xFF) / 255.0F;
            float blue = (argb & 0xFF) / 255.0F;

            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glLineWidth(5.0F);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
            GL11.glEnable(GL11.GL_STENCIL_TEST);
            GL11.glClearStencil(0xF);
            GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
            GL11.glStencilMask(0xF);
            GL11.glStencilFunc(GL11.GL_NEVER, 1, 0xF);
            GL11.glStencilOp(GL11.GL_REPLACE, GL11.GL_REPLACE, GL11.GL_REPLACE);
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
            GL11.glColor4f(red, green, blue, alpha);
            renderModel(model, entity, limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch, scaleFactor);

            GL11.glStencilFunc(GL11.GL_NEVER, 0, 0xF);
            GL11.glStencilOp(GL11.GL_REPLACE, GL11.GL_REPLACE, GL11.GL_REPLACE);
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
            GL11.glColor4f(red, green, blue, alpha);
            renderModel(model, entity, limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch, scaleFactor);

            GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xF);
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
            GL11.glColor4f(red, green, blue, alpha);
            renderModel(model, entity, limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch, scaleFactor);

            // The final silhouette pass is depth-independent, matching the optional ESP mode.
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_LINE);
            GL11.glPolygonOffset(1.0F, -2000000.0F);
            GL11.glColor4f(red, green, blue, alpha);
            renderModel(model, entity, limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch, scaleFactor);
        } finally {
            entity.hurtTime = oldHurtTime;
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
            GL11.glPolygonOffset(1.0F, 2000000.0F);
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_LINE);
            GL11.glDisable(GL11.GL_STENCIL_TEST);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(true);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glLineWidth(1.0F);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
            net.minecraft.client.renderer.GlStateManager.resetColor();
        }
    }

    /** Fallback equivalent to Better NEF's Pojav path when stencil outlines are unavailable. */
    private static void renderFallback(EntityLivingBase entity, ModelBase model, float limbSwing,
                                       float limbSwingAmount, float ageInTicks, float headYaw,
                                       float headPitch, float scaleFactor, int argb) {
        float alpha = ((argb >>> 24) & 0xFF) / 255.0F;
        float red = ((argb >>> 16) & 0xFF) / 255.0F;
        float green = ((argb >>> 8) & 0xFF) / 255.0F;
        float blue = (argb & 0xFF) / 255.0F;
        int oldHurtTime = entity.hurtTime;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            entity.hurtTime = 0;
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(red, green, blue, alpha);
            GL11.glScalef(1.03F, 1.03F, 1.03F);
            renderModel(model, entity, limbSwing, limbSwingAmount, ageInTicks,
                    headYaw, headPitch, scaleFactor);
        } finally {
            entity.hurtTime = oldHurtTime;
            GL11.glPopMatrix();
            GL11.glPopAttrib();
            net.minecraft.client.renderer.GlStateManager.resetColor();
        }
    }

    private static void renderModel(ModelBase model, EntityLivingBase entity, float limbSwing,
                                    float limbSwingAmount, float ageInTicks, float headYaw,
                                    float headPitch, float scaleFactor) {
        model.render(entity, limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch, scaleFactor);
    }

    private static boolean ensureStencilBuffer(Minecraft mc) {
        Framebuffer framebuffer = mc.getFramebuffer();
        if (framebuffer == null || framebuffer.depthBuffer <= -1) return framebuffer != null;
        try {
            EXTFramebufferObject.glDeleteRenderbuffersEXT(framebuffer.depthBuffer);
            int stencilDepthBufferId = EXTFramebufferObject.glGenRenderbuffersEXT();
            EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, stencilDepthBufferId);
            EXTFramebufferObject.glRenderbufferStorageEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT,
                    EXTPackedDepthStencil.GL_DEPTH_STENCIL_EXT, mc.displayWidth, mc.displayHeight);
            EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                    EXTFramebufferObject.GL_STENCIL_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT,
                    stencilDepthBufferId);
            EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                    EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT,
                    stencilDepthBufferId);
            framebuffer.depthBuffer = -1;
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
