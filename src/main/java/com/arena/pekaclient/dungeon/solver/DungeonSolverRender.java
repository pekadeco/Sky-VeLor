package com.arena.pekaclient.dungeon.solver;

import net.minecraft.block.BlockLever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

/** Shared, fixed-function rendering helpers required by the dungeon solvers. */
public final class DungeonSolverRender {
    private DungeonSolverRender() { }

    public static void drawOnSlot(int containerSize, int slotX, int slotY, int color) {
        drawOnSlot(containerSize, slotX, slotY, color, -1);
    }

    public static void drawOnSlot(int containerSize, int slotX, int slotY, int color, int number) {
        Minecraft mc = Minecraft.getMinecraft();
        int width = mc.currentScreen == null ? mc.displayWidth : mc.currentScreen.width;
        int height = mc.currentScreen == null ? mc.displayHeight : mc.currentScreen.height;
        net.minecraft.client.gui.ScaledResolution sr = new net.minecraft.client.gui.ScaledResolution(mc);
        int guiLeft = (sr.getScaledWidth() - 176) / 2;
        int guiTop = (sr.getScaledHeight() - 222) / 2;
        int x = guiLeft + slotX;
        int y = guiTop + slotY;
        if (containerSize != 90) y += (6 - (containerSize - 36) / 9) * 9;
        GL11.glTranslated(0, 0, 1);
        Gui.drawRect(x, y, x + 16, y + 16, color);
        GL11.glTranslated(0, 0, -1);
        if (number >= 0) {
            String text = Integer.toString(number);
            GlStateManager.pushMatrix();
            GlStateManager.translate(0, 0, 300);
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            mc.fontRendererObj.drawStringWithShadow(text, x + 8 - mc.fontRendererObj.getStringWidth(text) / 2,
                    y + 4, 0xFFFFFFFF);
            GlStateManager.enableDepth();
            GlStateManager.enableLighting();
            GlStateManager.popMatrix();
        }
    }

    public static void highlightBlock(BlockPos pos, Color color, boolean disableDepth, float partialTicks) {
        highlightBlock(pos, color, disableDepth, false, partialTicks);
    }

    public static void highlightBlock(BlockPos pos, Color color, boolean disableDepth, boolean isButton, float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        if (pos == null || mc.getRenderViewEntity() == null) return;
        Entity viewer = mc.getRenderViewEntity();
        double vx = interp(viewer.lastTickPosX, viewer.posX, partialTicks);
        double vy = interp(viewer.lastTickPosY, viewer.posY, partialTicks);
        double vz = interp(viewer.lastTickPosZ, viewer.posZ, partialTicks);
        AxisAlignedBB box;
        if (isButton) {
            box = new AxisAlignedBB(pos.getX() - vx + 0.36, pos.getY() - vy + 0.38, pos.getZ() - vz + 0.36,
                    pos.getX() - vx + 0.64, pos.getY() - vy + 0.64, pos.getZ() - vz + 0.64);
        } else {
            box = new AxisAlignedBB(pos.getX() - vx, pos.getY() - vy, pos.getZ() - vz,
                    pos.getX() - vx + 1, pos.getY() - vy + 1, pos.getZ() - vz + 1);
        }
        if (disableDepth) GlStateManager.disableDepth();
        GlStateManager.disableCull();
        GlStateManager.disableLighting();
        drawFilledBoundingBox(box, color, 0.35f);
        if (disableDepth) GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.enableCull();
    }

    /** A box whose coordinates are already relative to the render-view position. */
    public static void drawFilledBoundingBox(AxisAlignedBB box, Color color, float alpha) {
        if (box == null || color == null) return;
        float a = MathHelper.clamp_float((color.getAlpha() / 255.0f) * alpha, 0.0f, 1.0f);
        float r = color.getRed() / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue() / 255.0f;
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        face(wr, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, r, g, b, a);
        face(wr, box.minX, box.maxY, box.minZ, box.minX, box.maxY, box.maxZ, box.maxX, box.maxY, box.maxZ, box.maxX, box.maxY, box.minZ, r, g, b, a);
        face(wr, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, box.maxX, box.minY, box.minZ, r, g, b, a);
        face(wr, box.minX, box.minY, box.maxZ, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, r, g, b, a);
        face(wr, box.minX, box.minY, box.minZ, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, r, g, b, a);
        face(wr, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, box.maxX, box.minY, box.maxZ, r, g, b, a);
        Tessellator.getInstance().draw();
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    public static void drawFilledBoundingBox(AxisAlignedBB box, float alpha, Color color) {
        drawFilledBoundingBox(box, color, alpha);
    }

    private static void face(WorldRenderer wr,
                             double x1, double y1, double z1, double x2, double y2, double z2,
                             double x3, double y3, double z3, double x4, double y4, double z4,
                             float r, float g, float b, float a) {
        wr.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        wr.pos(x2, y2, z2).color(r, g, b, a).endVertex();
        wr.pos(x3, y3, z3).color(r, g, b, a).endVertex();
        wr.pos(x4, y4, z4).color(r, g, b, a).endVertex();
    }

    public static void draw3DLine(Vec3 start, Vec3 end, Color color, int lineWidth, boolean depth, float partialTicks) {
        draw3DLine(start, end, color, lineWidth, depth, partialTicks, false, false, null);
    }

    public static void draw3DLine(Vec3 start, Vec3 end, Color color, int lineWidth, boolean depth,
                                  float partialTicks, boolean fromHead, boolean isLever,
                                  BlockLever.EnumOrientation orientation) {
        Minecraft mc = Minecraft.getMinecraft();
        Entity viewer = mc.getRenderViewEntity();
        if (viewer == null || start == null || end == null) return;
        Vec3 lineStart = isLever ? leverCenter(start, orientation) : start;
        Vec3 lineEnd = end;
        double vx = interp(viewer.lastTickPosX, viewer.posX, partialTicks);
        double vy = interp(viewer.lastTickPosY, viewer.posY, partialTicks);
        double vz = interp(viewer.lastTickPosZ, viewer.posZ, partialTicks);
        GlStateManager.pushMatrix();
        GlStateManager.translate(-vx, -vy, -vz);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        if (!depth) GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GL11.glLineWidth(Math.max(1, Math.min(8, lineWidth)));
        WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        float a = color.getAlpha() / 255.0f;
        wr.pos(lineStart.xCoord, lineStart.yCoord, lineStart.zCoord).color(color.getRed()/255f,color.getGreen()/255f,color.getBlue()/255f,a).endVertex();
        wr.pos(lineEnd.xCoord, lineEnd.yCoord, lineEnd.zCoord).color(color.getRed()/255f,color.getGreen()/255f,color.getBlue()/255f,a).endVertex();
        Tessellator.getInstance().draw();
        GL11.glLineWidth(1f);
        GlStateManager.depthMask(true);
        if (!depth) GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private static Vec3 leverCenter(Vec3 pos, BlockLever.EnumOrientation orientation) {
        if (orientation == null) return new Vec3(pos.xCoord + 0.5, pos.yCoord + 0.5, pos.zCoord + 0.5);
        switch (orientation) {
            case UP_X: case UP_Z: return new Vec3(pos.xCoord + 0.5, pos.yCoord + 0.1, pos.zCoord + 0.5);
            case NORTH: return new Vec3(pos.xCoord + 0.5, pos.yCoord + 0.5, pos.zCoord + 0.875);
            case SOUTH: return new Vec3(pos.xCoord + 0.5, pos.yCoord + 0.5, pos.zCoord + 0.125);
            case WEST: return new Vec3(pos.xCoord + 0.875, pos.yCoord + 0.5, pos.zCoord + 0.5);
            case EAST: return new Vec3(pos.xCoord + 0.125, pos.yCoord + 0.5, pos.zCoord + 0.5);
            default: return new Vec3(pos.xCoord + 0.5, pos.yCoord + 0.5, pos.zCoord + 0.5);
        }
    }

    public static void drawLeverBoundingBox(BlockPos pos, EnumFacing facing, Color color, float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        Entity viewer = mc.getRenderViewEntity();
        if (viewer == null || pos == null) return;
        double vx = interp(viewer.lastTickPosX, viewer.posX, partialTicks);
        double vy = interp(viewer.lastTickPosY, viewer.posY, partialTicks);
        double vz = interp(viewer.lastTickPosZ, viewer.posZ, partialTicks);
        double x = pos.getX() - vx, y = pos.getY() - vy, z = pos.getZ() - vz;
        AxisAlignedBB bb;
        switch (facing) {
            case NORTH: bb = new AxisAlignedBB(x + .25, y + .1875, z + .75, x + .75, y + .8125, z + 1); break;
            case SOUTH: bb = new AxisAlignedBB(x + .25, y + .1875, z, x + .75, y + .8125, z + .25); break;
            case WEST: bb = new AxisAlignedBB(x + .75, y + .1875, z + .25, x + 1, y + .8125, z + .75); break;
            case EAST: bb = new AxisAlignedBB(x, y + .1875, z + .25, x + .25, y + .8125, z + .75); break;
            default: bb = new AxisAlignedBB(x + .25, y + .1875, z + .25, x + .75, y + .8125, z + .75); break;
        }
        drawFilledBoundingBox(bb, color, 0.75f);
    }

    public static void drawTag(String text, double[] pos, Color color, float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        Entity player = mc.thePlayer;
        if (player == null || mc.getRenderViewEntity() == null || pos == null || pos.length < 3) return;
        Entity viewer = mc.getRenderViewEntity();
        double vx = interp(viewer.lastTickPosX, viewer.posX, partialTicks);
        double vy = interp(viewer.lastTickPosY, viewer.posY, partialTicks);
        double vz = interp(viewer.lastTickPosZ, viewer.posZ, partialTicks);
        net.minecraft.client.renderer.entity.RenderManager rm = mc.getRenderManager();
        double x = pos[0] - vx + 0.5, y = pos[1] - vy + 0.5, z = pos[2] - vz + 0.5;
        float scale = Math.max(2.0F, (float) player.getDistance(pos[0], pos[1], pos[2]) / 5.0F) * 0.016666668F;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y + 2.5, z);
        GlStateManager.rotate(-rm.playerViewY, 0, 1, 0);
        GlStateManager.rotate(rm.playerViewX, 1, 0, 0);
        GlStateManager.scale(-scale, -scale, scale);
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        int width = mc.fontRendererObj.getStringWidth(text) / 2;
        Gui.drawRect(-width - 2, -1, width + 2, 9, 0x40000000);
        mc.fontRendererObj.drawString(text, -width, 0, color.getRGB());
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    private static double interp(double old, double now, float partial) { return old + (now - old) * partial; }
}
