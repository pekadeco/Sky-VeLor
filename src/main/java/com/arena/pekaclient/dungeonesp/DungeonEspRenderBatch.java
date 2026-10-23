package com.arena.pekaclient.dungeonesp;

import com.arena.pekaclient.config.DungeonEspSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

/**
 * Reused render-side storage and batched fixed-function drawing for cached Dungeon ESP boxes. No entity list is read
 * here: tick data is supplied by {@link DungeonEspTracker}. Real interpolated entity bounding boxes are sampled only
 * to paint their current frame position; fills/outlines are grouped by depth and width rather than setting GL state
 * per entity. Door prisms enter the same batch.
 */
public final class DungeonEspRenderBatch {

    private static final int MAX_RENDER_BOXES = DungeonEspTracker.MAX_TRACKED + WitherDoorCache.MAX_DOORS;
    private final RenderBox[] boxes = new RenderBox[MAX_RENDER_BOXES];
    private final Frustum frustum = new Frustum();
    private int mobCandidateCount;
    private int boxCount;
    private int renderedMobCount;
    private int renderedDoorCount;

    private static final class RenderBox {
        double minX;
        double minY;
        double minZ;
        double maxX;
        double maxY;
        double maxZ;
        double distanceSq;
        double labelX;
        double labelY;
        double labelZ;
        int color;
        DungeonEspSettings.Style style;
        boolean tracer;
        boolean label;
        boolean door;

        void set(double newMinX, double newMinY, double newMinZ, double newMaxX, double newMaxY, double newMaxZ,
                 double newDistanceSq, int newColor, DungeonEspSettings.Style newStyle,
                 boolean newTracer, boolean newLabel, boolean newDoor) {
            minX = newMinX;
            minY = newMinY;
            minZ = newMinZ;
            maxX = newMaxX;
            maxY = newMaxY;
            maxZ = newMaxZ;
            distanceSq = newDistanceSq;
            labelX = (newMinX + newMaxX) * 0.5D;
            labelY = newMaxY + 0.32D;
            labelZ = (newMinZ + newMaxZ) * 0.5D;
            color = newColor & 0xFFFFFF;
            style = newStyle;
            tracer = newTracer;
            label = newLabel;
            door = newDoor;
        }
    }

    public DungeonEspRenderBatch() {
        for (int i = 0; i < boxes.length; i++) boxes[i] = new RenderBox();
    }

    /** Begins an allocation-free collection frame with the live camera position used by the render manager. */
    public void begin(double cameraX, double cameraY, double cameraZ) {
        mobCandidateCount = 0;
        boxCount = 0;
        renderedMobCount = 0;
        renderedDoorCount = 0;
        frustum.setPosition(cameraX, cameraY, cameraZ);
    }

    /**
     * Collects cached tick targets through their current real bounding boxes. Dead/zero-health live entities are
     * removed from the tracker at render time too, covering the few frames between an entity despawn and its next
     * configured tick scan.
     */
    public void collectMobs(DungeonEspTracker tracker, double playerX, double playerY, double playerZ,
                            double cameraX, double cameraY, double cameraZ, float partialTicks) {
        if (tracker == null) return;
        for (int slot = 0; slot < tracker.slotCapacity() && mobCandidateCount < DungeonEspTracker.MAX_TRACKED; slot++) {
            if (!tracker.isActiveSlot(slot)) continue;
            DungeonEspTracker.TrackedMob tracked = tracker.atSlot(slot);
            if (!(tracked.reference instanceof Entity)) {
                tracker.remove(tracked.entityId);
                continue;
            }
            Entity entity = (Entity) tracked.reference;
            if (entity.isDead || (DungeonEspSettings.hideZeroHealth && entity instanceof EntityLivingBase
                    && ((EntityLivingBase) entity).getHealth() <= 0F)) {
                tracker.remove(tracked.entityId);
                continue;
            }
            DungeonEspSettings.Style style = styleFor(tracked.category);
            if (style == null || !style.enabled) continue;
            double ix = interpolate(entity.lastTickPosX, entity.posX, partialTicks);
            double iy = interpolate(entity.lastTickPosY, entity.posY, partialTicks);
            double iz = interpolate(entity.lastTickPosZ, entity.posZ, partialTicks);
            double minX;
            double minY;
            double minZ;
            double maxX;
            double maxY;
            double maxZ;
            if (tracked.fixedArmorStandBox) {
                // A player-head marker's vanilla armor-stand hitbox is tiny and misleading. Keep its stable,
                // body-sized box centered on the marker rather than pretending the stand itself is the mob body.
                minX = ix - 0.55D;
                maxX = ix + 0.55D;
                minY = iy - 0.15D;
                maxY = minY + 2.70D;
                minZ = iz - 0.55D;
                maxZ = iz + 0.55D;
            } else {
                AxisAlignedBB bounds = entity.getEntityBoundingBox();
                if (bounds == null) continue;
                double offsetX = ix - entity.posX;
                double offsetY = iy - entity.posY;
                double offsetZ = iz - entity.posZ;
                minX = bounds.minX + offsetX;
                minY = bounds.minY + offsetY;
                minZ = bounds.minZ + offsetZ;
                maxX = bounds.maxX + offsetX;
                maxY = bounds.maxY + offsetY;
                maxZ = bounds.maxZ + offsetZ;
            }
            if (!frustum.isBoxInFrustum(minX, minY, minZ, maxX, maxY, maxZ)) continue;
            double centerX = (minX + maxX) * 0.5D;
            double centerY = (minY + maxY) * 0.5D;
            double centerZ = (minZ + maxZ) * 0.5D;
            double distanceSq = distanceSq(centerX, centerY, centerZ, playerX, playerY, playerZ);
            boxes[mobCandidateCount++].set(minX, minY, minZ, maxX, maxY, maxZ, distanceSq, style.color, style,
                    DungeonEspSettings.tracers, DungeonEspSettings.distanceText, false);
        }
        sortMobCandidatesByDistance();
        boxCount = Math.min(mobCandidateCount, DungeonEspSettings.maxBoxes);
        renderedMobCount = boxCount;
    }

    /** Adds a cached static closed-door prism to the same render batch after frustum culling. */
    public void collectDoor(WitherDoorCache.Door door, double playerX, double playerY, double playerZ,
                            int color, boolean label) {
        if (door == null || boxCount >= boxes.length || !DungeonEspSettings.doors.enabled) return;
        double minX = door.x;
        double minY = door.y;
        double minZ = door.z;
        double maxX = door.x + WitherDoorCache.DOOR_WIDTH;
        double maxY = door.y + WitherDoorCache.DOOR_HEIGHT;
        double maxZ = door.z + WitherDoorCache.DOOR_DEPTH;
        if (!frustum.isBoxInFrustum(minX, minY, minZ, maxX, maxY, maxZ)) return;
        double distanceSq = distanceSq((minX + maxX) * 0.5D, (minY + maxY) * 0.5D, (minZ + maxZ) * 0.5D,
                playerX, playerY, playerZ);
        boxes[boxCount++].set(minX, minY, minZ, maxX, maxY, maxZ, distanceSq, color, DungeonEspSettings.doors,
                false, label, true);
        renderedDoorCount++;
    }

    /** Draws all collected boxes with one common matrix/state setup, grouped only where GL requires depth/width changes. */
    public void render(Minecraft minecraft, RenderManager renderManager,
                       double cameraX, double cameraY, double cameraZ) {
        if (boxCount <= 0 || minecraft == null || renderManager == null) return;
        GlStateManager.pushMatrix();
        GlStateManager.translate(-cameraX, -cameraY, -cameraZ);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.depthMask(false);

        // Filled faces: one quad batch for depth-tested entries and one for see-through entries; never per entity.
        drawFills(false);
        drawFills(true);
        // Edge widths require separate GL batches, but category state is changed at most once per width/depth group.
        drawOutlines(false);
        drawOutlines(true);
        drawTracers(cameraX, cameraY, cameraZ);

        GL11.glLineWidth(1F);
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.popMatrix();
        drawLabels(minecraft, renderManager, cameraX, cameraY, cameraZ);
    }

    public int renderedMobCount() {
        return renderedMobCount;
    }

    public int renderedDoorCount() {
        return renderedDoorCount;
    }

    private void sortMobCandidatesByDistance() {
        // Insertion sort is stable, bounded by 256, and reorders object references only (no allocation/boxing).
        for (int i = 1; i < mobCandidateCount; i++) {
            RenderBox current = boxes[i];
            int j = i - 1;
            while (j >= 0 && boxes[j].distanceSq > current.distanceSq) {
                boxes[j + 1] = boxes[j];
                j--;
            }
            boxes[j + 1] = current;
        }
    }

    private void drawFills(boolean throughWalls) {
        boolean any = false;
        for (int i = 0; i < boxCount; i++) {
            RenderBox box = boxes[i];
            if (box.style != null && box.style.throughWalls == throughWalls
                    && (box.style.mode == DungeonEspSettings.MODE_FILLED || box.style.mode == DungeonEspSettings.MODE_BOTH)
                    && box.style.fillOpacity > 0) {
                any = true;
                break;
            }
        }
        if (!any) return;
        setDepth(throughWalls);
        WorldRenderer writer = Tessellator.getInstance().getWorldRenderer();
        writer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < boxCount; i++) {
            RenderBox box = boxes[i];
            if (box.style == null || box.style.throughWalls != throughWalls
                    || (box.style.mode != DungeonEspSettings.MODE_FILLED && box.style.mode != DungeonEspSettings.MODE_BOTH)
                    || box.style.fillOpacity <= 0) continue;
            drawStyledBox(writer, box.style, box.color, box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ, true);
        }
        Tessellator.getInstance().draw();
    }

    private void drawOutlines(boolean throughWalls) {
        for (int width = DungeonEspSettings.MIN_WIDTH; width <= DungeonEspSettings.MAX_WIDTH; width++) {
            boolean any = false;
            for (int i = 0; i < boxCount; i++) {
                RenderBox box = boxes[i];
                if (box.style != null && box.style.throughWalls == throughWalls && box.style.outlineWidth == width
                        && (box.style.mode == DungeonEspSettings.MODE_OUTLINE || box.style.mode == DungeonEspSettings.MODE_BOTH)
                        && box.style.outlineOpacity > 0) {
                    any = true;
                    break;
                }
            }
            if (!any) continue;
            setDepth(throughWalls);
            GL11.glLineWidth(width);
            WorldRenderer writer = Tessellator.getInstance().getWorldRenderer();
            writer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i < boxCount; i++) {
                RenderBox box = boxes[i];
                if (box.style == null || box.style.throughWalls != throughWalls || box.style.outlineWidth != width
                        || (box.style.mode != DungeonEspSettings.MODE_OUTLINE && box.style.mode != DungeonEspSettings.MODE_BOTH)
                        || box.style.outlineOpacity <= 0) continue;
                drawStyledBox(writer, box.style, box.color, box.minX, box.minY, box.minZ,
                        box.maxX, box.maxY, box.maxZ, false);
            }
            Tessellator.getInstance().draw();
        }
    }

    private void drawTracers(double cameraX, double cameraY, double cameraZ) {
        if (!DungeonEspSettings.tracers || renderedMobCount <= 0) return;
        boolean any = false;
        for (int i = 0; i < renderedMobCount; i++) if (boxes[i].tracer) { any = true; break; }
        if (!any) return;
        setDepth(true);
        GL11.glLineWidth(1F);
        WorldRenderer writer = Tessellator.getInstance().getWorldRenderer();
        writer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < renderedMobCount; i++) {
            RenderBox box = boxes[i];
            if (!box.tracer) continue;
            writer.pos(cameraX, cameraY, cameraZ).color(red(box.color), green(box.color), blue(box.color), 0.8F).endVertex();
            writer.pos(box.labelX, box.labelY - 0.32D, box.labelZ).color(red(box.color), green(box.color), blue(box.color), 0.8F).endVertex();
        }
        Tessellator.getInstance().draw();
    }

    private void drawLabels(Minecraft minecraft, RenderManager renderManager,
                            double cameraX, double cameraY, double cameraZ) {
        boolean any = false;
        for (int i = 0; i < boxCount; i++) if (boxes[i].label) { any = true; break; }
        if (!any || minecraft.fontRendererObj == null) return;
        FontRenderer font = minecraft.fontRendererObj;
        for (int i = 0; i < boxCount; i++) {
            RenderBox box = boxes[i];
            if (!box.label) continue;
            String text = Integer.toString((int) Math.ceil(Math.sqrt(box.distanceSq))) + "m";
            GlStateManager.pushMatrix();
            GlStateManager.translate(box.labelX - cameraX, box.labelY - cameraY, box.labelZ - cameraZ);
            GlStateManager.rotate(-renderManager.playerViewY, 0F, 1F, 0F);
            GlStateManager.rotate(renderManager.playerViewX, 1F, 0F, 0F);
            GlStateManager.scale(-0.025F, -0.025F, 0.025F);
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
            int width = font.getStringWidth(text);
            font.drawStringWithShadow(text, -width / 2, 0, 0xFFFFFFFF);
            GlStateManager.depthMask(true);
            GlStateManager.enableDepth();
            GlStateManager.popMatrix();
        }
    }

    private static DungeonEspSettings.Style styleFor(DungeonEspCategory category) {
        if (category == DungeonEspCategory.STARRED) return DungeonEspSettings.starred;
        if (category == DungeonEspCategory.MINIBOSS) return DungeonEspSettings.miniboss;
        if (category == DungeonEspCategory.SPECIAL) return DungeonEspSettings.special;
        if (category == DungeonEspCategory.ENDERMAN) return DungeonEspSettings.enderman;
        if (category == DungeonEspCategory.SKELETOR) return DungeonEspSettings.skeletor;
        if (category == DungeonEspCategory.OTHER) return DungeonEspSettings.otherMobs;
        return null;
    }

    private static void setDepth(boolean throughWalls) {
        if (throughWalls) GlStateManager.disableDepth(); else GlStateManager.enableDepth();
    }

    /**
     * Emits either the fill or outline geometry for one box using the exact live style object. The batched renderer
     * calls this method for every cached target; the settings preview calls it too, so a mode, opacity or width is
     * never approximated by a second visual implementation.
     *
     * <p>The caller owns GL state and Tessellator begin/draw calls. Passing {@code fill=true} requires a quad
     * writer; passing false requires a line writer.</p>
     */
    public static void drawStyledBox(WorldRenderer writer, DungeonEspSettings.Style style, int color,
                                     double minX, double minY, double minZ,
                                     double maxX, double maxY, double maxZ, boolean fill) {
        if (writer == null || style == null) return;
        if (fill) {
            if ((style.mode != DungeonEspSettings.MODE_FILLED && style.mode != DungeonEspSettings.MODE_BOTH)
                    || style.fillOpacity <= 0) return;
            int opacity = style.fillOpacity;
            quad(writer, color, opacity, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ);
            quad(writer, color, opacity, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ);
            quad(writer, color, opacity, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ);
            quad(writer, color, opacity, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ);
            quad(writer, color, opacity, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ);
            quad(writer, color, opacity, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ);
            return;
        }
        if ((style.mode != DungeonEspSettings.MODE_OUTLINE && style.mode != DungeonEspSettings.MODE_BOTH)
                || style.outlineOpacity <= 0) return;
        int opacity = style.outlineOpacity;
        edge(writer, color, opacity, minX, minY, minZ, maxX, minY, minZ);
        edge(writer, color, opacity, maxX, minY, minZ, maxX, minY, maxZ);
        edge(writer, color, opacity, maxX, minY, maxZ, minX, minY, maxZ);
        edge(writer, color, opacity, minX, minY, maxZ, minX, minY, minZ);
        edge(writer, color, opacity, minX, maxY, minZ, maxX, maxY, minZ);
        edge(writer, color, opacity, maxX, maxY, minZ, maxX, maxY, maxZ);
        edge(writer, color, opacity, maxX, maxY, maxZ, minX, maxY, maxZ);
        edge(writer, color, opacity, minX, maxY, maxZ, minX, maxY, minZ);
        edge(writer, color, opacity, minX, minY, minZ, minX, maxY, minZ);
        edge(writer, color, opacity, maxX, minY, minZ, maxX, maxY, minZ);
        edge(writer, color, opacity, maxX, minY, maxZ, maxX, maxY, maxZ);
        edge(writer, color, opacity, minX, minY, maxZ, minX, maxY, maxZ);
    }

    private static void edge(WorldRenderer writer, int color, int opacity,
                             double x0, double y0, double z0, double x1, double y1, double z1) {
        vertex(writer, color, opacity, x0, y0, z0);
        vertex(writer, color, opacity, x1, y1, z1);
    }

    private static void quad(WorldRenderer writer, int color, int opacity,
                             double x0, double y0, double z0, double x1, double y1, double z1,
                             double x2, double y2, double z2, double x3, double y3, double z3) {
        vertex(writer, color, opacity, x0, y0, z0);
        vertex(writer, color, opacity, x1, y1, z1);
        vertex(writer, color, opacity, x2, y2, z2);
        vertex(writer, color, opacity, x3, y3, z3);
    }

    private static void vertex(WorldRenderer writer, int color, int opacity, double x, double y, double z) {
        writer.pos(x, y, z).color(red(color), green(color), blue(color),
                Math.max(0, Math.min(100, opacity)) / 100F).endVertex();
    }

    private static float red(int color) { return ((color >> 16) & 0xFF) / 255F; }
    private static float green(int color) { return ((color >> 8) & 0xFF) / 255F; }
    private static float blue(int color) { return (color & 0xFF) / 255F; }
    private static double interpolate(double previous, double current, float partialTicks) { return previous + (current - previous) * partialTicks; }
    private static double distanceSq(double x, double y, double z, double otherX, double otherY, double otherZ) {
        double dx = x - otherX, dy = y - otherY, dz = z - otherZ;
        return dx * dx + dy * dy + dz * dz;
    }
}
