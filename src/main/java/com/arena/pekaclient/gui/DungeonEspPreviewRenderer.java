package com.arena.pekaclient.gui;

import com.arena.pekaclient.config.DungeonEspSettings;
import com.arena.pekaclient.dungeonesp.DungeonEspRenderBatch;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/**
 * Editor-only ESP preview. It deliberately uses a named, real-dimension fallback hitbox instead of constructing a
 * fake entity/world: the editor can safely open from a menu or a disconnected client. Fill and outline vertices are
 * emitted by {@link DungeonEspRenderBatch#drawStyledBox}, the same style path used for live dungeon ESP.
 */
public final class DungeonEspPreviewRenderer {
    private DungeonEspPreviewRenderer() { }

    public static void drawMob(String category, DungeonEspSettings.Style style,
                               float x, float y, float width, float height, FontRenderer font) {
        String name = mobName(category);
        UIRender.text(font, "Preview · " + name, x + 10f, y + 8f, Theme.textSecondary, .84f, false);
        UIRender.text(font, "Named fallback hitbox", x + 10f, y + height - 16f, Theme.textTertiary, .74f, false);
        float entityHeight = mobHeight(category);
        drawBox(style, style == null ? 0xFFFFFF : style.color, x + width * .68f, y + height - 10f,
                Math.max(13f, Math.min(width * .13f, 28f)), entityHeight, 1.0f);
    }

    public static void drawDoor(String category, DungeonEspSettings.Style style, int color,
                                float x, float y, float width, float height, FontRenderer font) {
        String name = "blood".equals(category) ? "Blood Door" : "Wither Door";
        UIRender.text(font, "Preview · " + name, x + 10f, y + 8f, Theme.textSecondary, .84f, false);
        UIRender.text(font, "Cached closed-door prism", x + 10f, y + height - 16f, Theme.textTertiary, .74f, false);
        drawBox(style, color, x + width * .68f, y + height - 10f,
                Math.max(13f, Math.min(width * .13f, 28f)), 2.0f, .22f);
    }

    private static void drawBox(DungeonEspSettings.Style style, int color,
                                float centerX, float baselineY, float scale, float modelHeight, float modelDepth) {
        if (style == null) return;
        float worldWidth = 1.0f;
        float worldHeight = Math.max(.5f, modelHeight);
        float worldDepth = Math.max(.12f, modelDepth);
        float fit = Math.min(scale, Math.max(5f, scale * 2.2f / worldHeight));
        GlStateManager.pushMatrix();
        GlStateManager.translate(centerX, baselineY, 0f);
        // GUI y grows downward; flip y so a hitbox grows upward from its baseline like the live entity box.
        GlStateManager.scale(fit, -fit, fit);
        GlStateManager.translate(-worldWidth * .5f, 0f, -worldDepth * .5f);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.depthMask(false);
        if (style.throughWalls) GlStateManager.disableDepth(); else GlStateManager.enableDepth();

        WorldRenderer writer = Tessellator.getInstance().getWorldRenderer();
        writer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        DungeonEspRenderBatch.drawStyledBox(writer, style, color, 0d, 0d, 0d,
                worldWidth, worldHeight, worldDepth, true);
        Tessellator.getInstance().draw();
        GL11.glLineWidth(Math.max(1, Math.min(DungeonEspSettings.MAX_WIDTH, style.outlineWidth)));
        writer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        DungeonEspRenderBatch.drawStyledBox(writer, style, color, 0d, 0d, 0d,
                worldWidth, worldHeight, worldDepth, false);
        Tessellator.getInstance().draw();

        GL11.glLineWidth(1f);
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.popMatrix();
    }

    private static String mobName(String id) {
        if ("starred".equals(id)) return "Starred Mob";
        if ("miniboss".equals(id)) return "Miniboss";
        if ("special".equals(id)) return "Special Starred Mob";
        if ("enderman".equals(id)) return "Enderman";
        if ("skeletor".equals(id)) return "Skeletor";
        if ("other".equals(id)) return "Other Mob";
        return "Dungeon Mob";
    }

    private static float mobHeight(String id) {
        if ("enderman".equals(id)) return 2.7f;
        if ("miniboss".equals(id)) return 2.35f;
        if ("special".equals(id)) return 2.15f;
        if ("skeletor".equals(id)) return 1.95f;
        return 1.8f;
    }
}
