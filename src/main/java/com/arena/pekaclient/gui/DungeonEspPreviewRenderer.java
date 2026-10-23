package com.arena.pekaclient.gui;

import com.arena.pekaclient.config.DungeonEspSettings;
import com.arena.pekaclient.dungeonesp.DungeonEspCategory;
import com.arena.pekaclient.dungeonesp.DungeonEspMobDisplay;
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
 * Editor-only Dungeon ESP surface. The hitbox is emitted through
 * {@link DungeonEspRenderBatch#drawStyledBox}, exactly the live cached-ESP style path.  The rotating
 * biped is intentionally a renderer-safe geometric stand-in rather than a fake world/entity, so this
 * editor remains available at the title screen and on a disconnected client.
 */
public final class DungeonEspPreviewRenderer {
    private DungeonEspPreviewRenderer() { }

    /** Compatibility entry point retained for category thumbnails/tests. */
    public static void drawMob(String category, DungeonEspSettings.Style style,
                               float x, float y, float width, float height, FontRenderer font) {
        drawMob(category, style, x, y, width, height, font, System.nanoTime());
    }

    /** Draws the selected rotating model and the exact live styled hitbox in one clipped editor frame. */
    public static void drawMob(String category, DungeonEspSettings.Style style,
                               float x, float y, float width, float height, FontRenderer font, long nowNanos) {
        String name = mobName(category);
        UIRender.text(font, name, x + 10f, y + 8f, Theme.textSecondary, .84f, false);
        UIRender.text(font, "Rotating model + live ESP hitbox", x + 10f, y + height - 16f,
                Theme.textTertiary, .72f, false);
        drawMobLabelPreview(category, style, x, y, width, font);
        float entityHeight = mobHeight(category);
        float modelScale = Math.max(8f, Math.min(Math.min(width, height) * .20f, 28f));
        float centerX = x + width * .43f;
        float baseline = y + height * .72f;
        drawRotatingMobModel(category, centerX, baseline, modelScale, nowNanos,
                style == null ? 0xFFFFFFFF : opaque(style.color));
        drawBox(style, style == null ? 0xFFFFFF : style.color, x + width * .72f, y + height - 27f,
                Math.max(13f, Math.min(width * .12f, 26f)), entityHeight, 1.0f);
    }

    /** Uses the same compact title/distance hierarchy as the live renderer without constructing a dummy entity. */
    private static void drawMobLabelPreview(String category, DungeonEspSettings.Style style,
                                            float x, float y, float width, FontRenderer font) {
        if (!DungeonEspSettings.mobLabels || style == null) return;
        String title = DungeonEspMobDisplay.title(displayCategory(category));
        if (title.length() == 0) return;
        String distance = DungeonEspSettings.distanceText ? DungeonEspMobDisplay.distance(24D * 24D) : "";
        int titleWidth = font.getStringWidth(title);
        int distanceWidth = distance.length() == 0 ? 0 : font.getStringWidth(distance);
        float contentWidth = titleWidth + (distanceWidth == 0 ? 0f : 4f + distanceWidth);
        float badgeW = contentWidth + 8f;
        float badgeX = x + Math.max(10f, Math.min(width - badgeW - 8f, width * .51f));
        float badgeY = y + 25f;
        UIRender.roundRect(badgeX, badgeY, badgeW, 13f, 3f, 0xD8121215, 0xD8121215, 0f);
        UIRender.rect(badgeX + 2f, badgeY, Math.max(1f, badgeW - 4f), 1f, opaque(style.color));
        UIRender.text(font, title, badgeX + 4f, badgeY + 3f, opaque(style.color), .72f, true);
        if (distanceWidth > 0) {
            UIRender.text(font, distance, badgeX + 4f + titleWidth + 4f, badgeY + 3f, 0xFFF2F2F7, .72f, true);
        }
    }

    /** Door frame plus the exact cached closed-door hitbox path used by live Wither/Blood Door ESP. */
    public static void drawDoor(String category, DungeonEspSettings.Style style, int color,
                                float x, float y, float width, float height, FontRenderer font) {
        String name = "blood".equals(category) ? "Blood Door" : "Wither Door";
        UIRender.text(font, name + " frame", x + 10f, y + 8f, Theme.textSecondary, .84f, false);
        UIRender.text(font, "Cached closed-door ESP", x + 10f, y + height - 16f, Theme.textTertiary, .72f, false);
        float frameW = Math.max(24f, Math.min(width * .23f, 62f));
        float frameH = Math.max(44f, Math.min(height * .52f, 104f));
        float frameX = x + width * .27f - frameW * .5f;
        float frameY = y + height * .52f - frameH * .5f;
        int frame = UIRender.withAlpha(color, .68f);
        UIRender.roundRectRing(frameX, frameY, frameW, frameH, 3f, 2f, frame);
        UIRender.rect(frameX + frameW * .5f - 1f, frameY + 3f, 2f, frameH - 6f, UIRender.withAlpha(color, .35f));
        drawBox(style, color, x + width * .72f, y + height - 27f,
                Math.max(13f, Math.min(width * .12f, 26f)), 2.0f, .22f);
    }

    /** Small category-heading preview sharing the actual ESP styled-box renderer. */
    public static void drawMobThumbnail(String category, DungeonEspSettings.Style style,
                                        float x, float y, float width, float height) {
        drawBox(style, style == null ? 0xFFFFFF : style.color, x + width * .5f, y + height - 1f,
                Math.max(3f, Math.min(width * .48f, 8f)), mobHeight(category), 1.0f);
    }

    /** Small category-heading preview for a cached closed-door prism. */
    public static void drawDoorThumbnail(String category, DungeonEspSettings.Style style, int color,
                                         float x, float y, float width, float height) {
        drawBox(style, color, x + width * .5f, y + height - 1f,
                Math.max(3f, Math.min(width * .48f, 8f)), 2.0f, .22f);
    }

    /** A compact biped/Enderman geometry built without a world, entity allocation, texture, or item stack. */
    private static void drawRotatingMobModel(String category, float centerX, float baseline, float scale,
                                             long nowNanos, int color) {
        float tall = "enderman".equals(category) ? 1.42f : "miniboss".equals(category) ? 1.18f : 1f;
        float angle = (nowNanos / 1000000L % 8000L) * .045f;
        GlStateManager.pushMatrix();
        GlStateManager.translate(centerX, baseline, 0f);
        GlStateManager.scale(scale, -scale, scale);
        GlStateManager.rotate(angle, 0f, 1f, 0f);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glColor4f(((color >>> 16) & 0xFF) / 255f, ((color >>> 8) & 0xFF) / 255f,
                (color & 0xFF) / 255f, .85f);
        // Legs, torso, arms and head match the recognisable dimensions of a Minecraft biped.
        cuboid(-.28f, 0f, -.12f, .16f, .72f * tall, .24f);
        cuboid(.12f, 0f, -.12f, .16f, .72f * tall, .24f);
        cuboid(-.32f, .65f * tall, -.16f, .64f, .72f * tall, .32f);
        cuboid(-.58f, .70f * tall, -.12f, .20f, .64f * tall, .24f);
        cuboid(.38f, .70f * tall, -.12f, .20f, .64f * tall, .24f);
        cuboid(-.30f, 1.35f * tall, -.25f, .60f, .55f * tall, .50f);
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.popMatrix();
    }

    private static void cuboid(float x, float y, float z, float w, float h, float d) {
        float r = x + w, t = y + h, f = z + d;
        GL11.glBegin(GL11.GL_QUADS);
        // front/back
        GL11.glVertex3f(x, y, f); GL11.glVertex3f(r, y, f); GL11.glVertex3f(r, t, f); GL11.glVertex3f(x, t, f);
        GL11.glVertex3f(r, y, z); GL11.glVertex3f(x, y, z); GL11.glVertex3f(x, t, z); GL11.glVertex3f(r, t, z);
        // left/right
        GL11.glVertex3f(x, y, z); GL11.glVertex3f(x, y, f); GL11.glVertex3f(x, t, f); GL11.glVertex3f(x, t, z);
        GL11.glVertex3f(r, y, f); GL11.glVertex3f(r, y, z); GL11.glVertex3f(r, t, z); GL11.glVertex3f(r, t, f);
        // bottom/top
        GL11.glVertex3f(x, y, z); GL11.glVertex3f(r, y, z); GL11.glVertex3f(r, y, f); GL11.glVertex3f(x, y, f);
        GL11.glVertex3f(x, t, f); GL11.glVertex3f(r, t, f); GL11.glVertex3f(r, t, z); GL11.glVertex3f(x, t, z);
        GL11.glEnd();
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

    private static int opaque(int color) { return 0xFF000000 | (color & 0xFFFFFF); }

    private static DungeonEspCategory displayCategory(String id) {
        if ("starred".equals(id) || "global".equals(id)) return DungeonEspCategory.STARRED;
        if ("miniboss".equals(id)) return DungeonEspCategory.MINIBOSS;
        if ("special".equals(id)) return DungeonEspCategory.SPECIAL;
        if ("enderman".equals(id)) return DungeonEspCategory.ENDERMAN;
        if ("skeletor".equals(id)) return DungeonEspCategory.SKELETOR;
        if ("other".equals(id)) return DungeonEspCategory.OTHER;
        return DungeonEspCategory.NONE;
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
