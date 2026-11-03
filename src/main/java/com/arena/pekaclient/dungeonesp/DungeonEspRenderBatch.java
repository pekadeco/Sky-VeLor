package com.arena.pekaclient.dungeonesp;

import com.arena.pekaclient.config.AssistSettings;
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
    /** Nearby labels are raised in small fixed lanes instead of fighting for the same nameplate position. */
    private static final double LABEL_HORIZONTAL_SQ = 3.24D;
    private static final double LABEL_VERTICAL_GAP = .48D;
    private static final double MAX_LABEL_LIFT = 2.40D;
    private static final int LABEL_LAYOUT_PASSES = 6;
    private final RenderBox[] boxes = new RenderBox[MAX_RENDER_BOXES];
    private final Frustum frustum = new Frustum();
    private int mobCandidateCount;
    private int boxCount;
    private int renderedMobCount;
    private int renderedMobLabelCount;
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
        DungeonEspCategory category;
        /** Existing optional numeric distance text, retained independently from the new category label. */
        boolean distanceText;
        /** True only for a bounded, collision-separated dungeon mob category badge. */
        boolean categoryLabel;
        /** Render-time result: preserves legacy distance-only labels when category labels are disabled. */
        boolean showDistance;
        boolean door;

        void set(double newMinX, double newMinY, double newMinZ, double newMaxX, double newMaxY, double newMaxZ,
                 double newDistanceSq, int newColor, DungeonEspSettings.Style newStyle, DungeonEspCategory newCategory,
                 boolean newDistanceText, boolean newDoor) {
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
            category = newCategory == null ? DungeonEspCategory.NONE : newCategory;
            distanceText = newDistanceText;
            categoryLabel = false;
            showDistance = newDistanceText;
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
        renderedMobLabelCount = 0;
        renderedDoorCount = 0;
        frustum.setPosition(cameraX, cameraY, cameraZ);
    }

    /**
     * Collects cached tick targets through their current real bounding boxes. Dead, zero-health, and death-animation
     * targets are removed at render time too, so the Comfort policy takes effect before the next configured scan.
     */
    public void collectMobs(DungeonEspTracker tracker, Entity localPlayer, double playerX, double playerY, double playerZ,
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
            // Scan-time identity filtering is authoritative; this live guard closes the interval before the next scan.
            if (isLocalPlayerEntity(entity, localPlayer)) {
                tracker.remove(tracked.entityId);
                continue;
            }
            if (entity.isDead || (AssistSettings.hideDeadMobs && entity instanceof EntityLivingBase
                    && (((EntityLivingBase) entity).deathTime > 0 || ((EntityLivingBase) entity).getHealth() <= 0F))) {
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
                // A player-head marker's vanilla armor-stand hitbox is tiny and misleading. Keep a tight stable
                // proxy centered on the marker, without the old oversized body-height outline.
                minX = DungeonEspMarkerBounds.minX(ix);
                maxX = DungeonEspMarkerBounds.maxX(ix);
                minY = DungeonEspMarkerBounds.minY(iy);
                maxY = DungeonEspMarkerBounds.maxY(iy);
                minZ = DungeonEspMarkerBounds.minZ(iz);
                maxZ = DungeonEspMarkerBounds.maxZ(iz);
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
                    tracked.category, DungeonEspSettings.distanceText, false);
        }
        sortMobCandidatesByDistance();
        boxCount = Math.min(mobCandidateCount, DungeonEspSettings.maxBoxes);
        renderedMobCount = boxCount;
        layoutMobLabels();
    }

    /** Kept explicit so the live render safety guard remains identity-based, not a broad player/NPC exclusion. */
    private static boolean isLocalPlayerEntity(Entity entity, Entity localPlayer) {
        return entity != null && entity == localPlayer;
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
                DungeonEspCategory.NONE, label, true);
        renderedDoorCount++;
    }

    /** Draws all collected boxes with one common matrix/state setup, grouped only where GL requires depth/width changes. */
    public void render(Minecraft minecraft, RenderManager renderManager,
                       double cameraX, double cameraY, double cameraZ) {
        if (boxCount <= 0 || minecraft == null || renderManager == null) return;
        GlStateManager.pushMatrix();
        try {
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
        } finally {
            restoreWorldRenderState();
            GlStateManager.popMatrix();
        }
        drawLabels(minecraft, renderManager, cameraX, cameraY, cameraZ);
    }

    public int renderedMobCount() {
        return renderedMobCount;
    }

    public int renderedDoorCount() {
        return renderedDoorCount;
    }

    /** Number of selected category badges, excluding legacy distance-only text. */
    public int renderedMobLabelCount() {
        return renderedMobLabelCount;
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

    /**
     * Selects a small hierarchy-first label set after the existing nearest-box cap is known. The display never asks
     * the world for a second entity list: category, geometry and distance all come from the cached collection pass.
     * Neighbouring labels receive a short world-space vertical lane; a crowded stack is skipped rather than covering
     * the view with unstable text.
     */
    private void layoutMobLabels() {
        renderedMobLabelCount = 0;
        for (int i = 0; i < renderedMobCount; i++) {
            RenderBox box = boxes[i];
            box.categoryLabel = false;
            // Disabling the redesign deliberately restores the pre-update distance-only behavior for every box.
            box.showDistance = box.distanceText && !DungeonEspSettings.mobLabels;
            box.labelY = box.maxY + .32D;
        }
        if (!DungeonEspSettings.mobLabels || DungeonEspSettings.maxMobLabels <= 0) return;
        int remaining = DungeonEspMobDisplay.clampLabelCount(DungeonEspSettings.maxMobLabels);
        for (int priority = 0; priority <= 3 && remaining > 0; priority++) {
            for (int i = 0; i < renderedMobCount && remaining > 0; i++) {
                RenderBox candidate = boxes[i];
                if (DungeonEspMobDisplay.priority(candidate.category) != priority
                        || !DungeonEspMobDisplay.hasTitle(candidate.category)) continue;
                double baseY = candidate.maxY + .32D;
                double labelY = separatedLabelY(candidate, baseY);
                if (labelY < baseY || labelY - baseY > MAX_LABEL_LIFT) continue;
                candidate.labelY = labelY;
                candidate.categoryLabel = true;
                candidate.showDistance = candidate.distanceText;
                remaining--;
            }
        }
        for (int i = 0; i < renderedMobCount; i++) if (boxes[i].categoryLabel) renderedMobLabelCount++;
    }

    /** Returns -1 for a dense stack whose extra lane would obstruct more than it helps. */
    private double separatedLabelY(RenderBox candidate, double baseY) {
        double result = baseY;
        for (int pass = 0; pass < LABEL_LAYOUT_PASSES; pass++) {
            boolean moved = false;
            for (int i = 0; i < renderedMobCount; i++) {
                RenderBox placed = boxes[i];
                if (!placed.categoryLabel) continue;
                double dx = candidate.labelX - placed.labelX;
                double dz = candidate.labelZ - placed.labelZ;
                if (dx * dx + dz * dz > LABEL_HORIZONTAL_SQ) continue;
                if (Math.abs(result - placed.labelY) < LABEL_VERTICAL_GAP) {
                    result = placed.labelY + LABEL_VERTICAL_GAP;
                    moved = true;
                }
            }
            if (!moved) break;
        }
        return result - baseY > MAX_LABEL_LIFT ? -1D : result;
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

    /**
     * Draws compact world-space badges after the box batches. Category headers are limited and collision-separated;
     * the legacy distance-only text remains available when category labels are disabled. Text uses each category's
     * existing through-walls setting instead of silently forcing every label through dungeon blocks.
     */
    private void drawLabels(Minecraft minecraft, RenderManager renderManager,
                            double cameraX, double cameraY, double cameraZ) {
        boolean any = false;
        for (int i = 0; i < boxCount; i++) {
            RenderBox box = boxes[i];
            if (box.categoryLabel || box.showDistance) {
                any = true;
                break;
            }
        }
        if (!any || minecraft.fontRendererObj == null) return;
        FontRenderer font = minecraft.fontRendererObj;
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.enableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        try {
            for (int i = 0; i < boxCount; i++) {
                RenderBox box = boxes[i];
                if (!box.categoryLabel && !box.showDistance) continue;
                drawLabel(font, renderManager, box, cameraX, cameraY, cameraZ);
            }
        } finally {
            restoreWorldRenderState();
        }
    }

    private void drawLabel(FontRenderer font, RenderManager renderManager, RenderBox box,
                           double cameraX, double cameraY, double cameraZ) {
        String title = box.categoryLabel ? DungeonEspMobDisplay.title(box.category) : "";
        String distance = box.showDistance ? DungeonEspMobDisplay.distance(box.distanceSq) : "";
        if (title.length() == 0 && distance.length() == 0) return;
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(box.labelX - cameraX, box.labelY - cameraY, box.labelZ - cameraZ);
            GlStateManager.rotate(-renderManager.playerViewY, 0F, 1F, 0F);
            GlStateManager.rotate(renderManager.playerViewX, 1F, 0F, 0F);
            GlStateManager.scale(-0.025F, -0.025F, 0.025F);
            if (box.style != null && box.style.throughWalls) GlStateManager.disableDepth();
            else GlStateManager.enableDepth();
            GlStateManager.depthMask(false);

            int titleWidth = title.length() == 0 ? 0 : font.getStringWidth(title);
            int distanceWidth = distance.length() == 0 ? 0 : font.getStringWidth(distance);
            int gap = titleWidth > 0 && distanceWidth > 0 ? 4 : 0;
            int contentWidth = titleWidth + gap + distanceWidth;
            float textX = -contentWidth * .5F;
            if (titleWidth > 0) {
                drawLabelBackdrop(textX - 4F, -3F, contentWidth + 8F, 13F, box.color);
                font.drawStringWithShadow(title, textX, 0F, 0xFF000000 | box.color);
            }
            if (distanceWidth > 0) {
                float distanceX = textX + titleWidth + gap;
                font.drawStringWithShadow(distance, distanceX, 0F,
                        titleWidth > 0 ? 0xFFF2F2F7 : 0xFFFFFFFF);
            }
        } finally {
            GlStateManager.popMatrix();
        }
    }

    /** A small opaque backing and one-pixel category accent keep the label readable without decorative clutter. */
    private static void drawLabelBackdrop(float x, float y, float width, float height, int color) {
        if (width <= 0F || height <= 0F) return;
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();
        WorldRenderer writer = Tessellator.getInstance().getWorldRenderer();
        writer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        labelVertex(writer, x, y, 0xB8121215);
        labelVertex(writer, x + width, y, 0xB8121215);
        labelVertex(writer, x + width, y + height, 0xB8121215);
        labelVertex(writer, x, y + height, 0xB8121215);
        int accent = 0xFF000000 | (color & 0xFFFFFF);
        labelVertex(writer, x + 2F, y, accent);
        labelVertex(writer, x + width - 2F, y, accent);
        labelVertex(writer, x + width - 2F, y + 1F, accent);
        labelVertex(writer, x + 2F, y + 1F, accent);
        Tessellator.getInstance().draw();
        GlStateManager.enableTexture2D();
    }

    private static void labelVertex(WorldRenderer writer, float x, float y, int color) {
        writer.pos(x, y, 0D).color(red(color), green(color), blue(color), ((color >>> 24) & 0xFF) / 255F).endVertex();
    }

    /** Returns the standard RenderWorldLast state after every normal or exceptional box/label pass. */
    private static void restoreWorldRenderState() {
        GL11.glLineWidth(1F);
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    private static DungeonEspSettings.Style styleFor(DungeonEspCategory category) {
        if (category == DungeonEspCategory.STARRED) return DungeonEspSettings.starred;
        if (category == DungeonEspCategory.MINIBOSS) return DungeonEspSettings.miniboss;
        if (category == DungeonEspCategory.SPECIAL) return DungeonEspSettings.special;
        if (category == DungeonEspCategory.ENDERMAN) return DungeonEspSettings.enderman;
        return null;
    }

    private static void setDepth(boolean throughWalls) {
        if (throughWalls) GlStateManager.disableDepth(); else GlStateManager.enableDepth();
    }

    /**
     * Emits either the fill or outline geometry for one box using the exact live style object. The batched renderer
     * calls this method for every cached target, so a mode, opacity or width is never approximated by a second
     * visual implementation.
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
