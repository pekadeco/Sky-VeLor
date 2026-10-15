package com.arena.pekaclient.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/**
 * Draws boxes and lines in world space for overlays. It runs only from RenderWorldLastEvent, after the world is
 * drawn, so it never changes how entities or blocks are rendered. It uses only fixed-function line and quad
 * primitives, so it works with OpenGL 2.1 and gl4es.
 *
 * Caller contract: call begin() once, draw any number of boxes and lines, then end(). begin() and end() restore
 * the GL state they change.
 */
public final class WorldBox {

    private static WorldRenderer wr;
    private static boolean open;

    private WorldBox() {
    }

    /** Starts an overlay pass. {@code viewX/Y/Z} are the camera position from the render manager. */
    public static void begin(double viewX, double viewY, double viewZ, boolean throughWalls) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(-viewX, -viewY, -viewZ);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.depthMask(false);
        if (throughWalls) {
            GlStateManager.disableDepth();
        } else {
            GlStateManager.enableDepth();
        }
        GL11.glLineWidth(2f);
        wr = Tessellator.getInstance().getWorldRenderer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        open = true;
    }

    /** Ends the pass and restores the state set by begin(). */
    public static void end() {
        if (open) {
            Tessellator.getInstance().draw();
            open = false;
        }
        GL11.glLineWidth(1f);
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    /** Outline of an axis-aligned box, in world coordinates, as 12 edges. */
    public static void outline(double x0, double y0, double z0, double x1, double y1, double z1, int argb) {
        float a = ((argb >>> 24) & 0xFF) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        edge(x0, y0, z0, x1, y0, z0, r, g, b, a);
        edge(x1, y0, z0, x1, y0, z1, r, g, b, a);
        edge(x1, y0, z1, x0, y0, z1, r, g, b, a);
        edge(x0, y0, z1, x0, y0, z0, r, g, b, a);
        edge(x0, y1, z0, x1, y1, z0, r, g, b, a);
        edge(x1, y1, z0, x1, y1, z1, r, g, b, a);
        edge(x1, y1, z1, x0, y1, z1, r, g, b, a);
        edge(x0, y1, z1, x0, y1, z0, r, g, b, a);
        edge(x0, y0, z0, x0, y1, z0, r, g, b, a);
        edge(x1, y0, z0, x1, y1, z0, r, g, b, a);
        edge(x1, y0, z1, x1, y1, z1, r, g, b, a);
        edge(x0, y0, z1, x0, y1, z1, r, g, b, a);
    }

    /** One line segment in world coordinates. */
    public static void line(double x0, double y0, double z0, double x1, double y1, double z1, int argb) {
        float a = ((argb >>> 24) & 0xFF) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        edge(x0, y0, z0, x1, y1, z1, r, g, b, a);
    }

    private static void edge(double x0, double y0, double z0, double x1, double y1, double z1,
                             float r, float g, float b, float a) {
        wr.pos(x0, y0, z0).color(r, g, b, a).endVertex();
        wr.pos(x1, y1, z1).color(r, g, b, a).endVertex();
    }

    /** True when the game has a world and a player to draw for. */
    public static boolean ready() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.theWorld != null && mc.thePlayer != null && mc.getRenderManager() != null;
    }
}
