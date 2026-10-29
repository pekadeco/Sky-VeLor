package com.arena.pekaclient.dungeon;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.feature.DungeonContext;
import com.arena.pekaclient.feature.LeapText;
import com.arena.pekaclient.render.SkinHead;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraft.util.Vec4b;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.IntBuffer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Renders the live dungeon map, player-name labels, and the matched puzzle-room name. */
public final class DungeonMapOverlay {
    private static final Pattern ICON_KEY = Pattern.compile("^icon-(\\d+)$");
    private static final Pattern SECRET_COUNT = Pattern.compile("(?i)secrets\\s*:?\\s*(\\d+)(?:\\s*/\\s*\\d+)?");
    private static final ResourceLocation MAP_ICONS = new ResourceLocation("textures/map/map_icons.png");
    private static final Map<String, Vec4b> NO_DECORATIONS = Collections.emptyMap();
    private static final IntBuffer PREVIOUS_SCISSOR = BufferUtils.createIntBuffer(16);
    private final PuzzleRoomMatcher roomMatcher = new PuzzleRoomMatcher();
    private final PuzzleRoomMemory puzzleMemory = new PuzzleRoomMemory();
    private World puzzleMemoryWorld;
    private World rosterWorld;
    private long rosterUpdatedAt;
    private List<String> roster = Collections.emptyList();
    private MapData assignmentData;
    private long decorationHash;
    private long rosterHash;
    private Map<String, String> assignments = Collections.emptyMap();
    /** Per-marker interpolation state lives only for the current map/world and never changes server/player state. */
    private MapData playerHeadMotionData;
    private final Map<String, DungeonMapPlayerHeadMotion> playerHeadMotions =
            new HashMap<String, DungeonMapPlayerHeadMotion>();
    private String lastPartyPuzzleKey = "";

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!DungeonContext.inDungeon() || mc.theWorld == null || mc.thePlayer == null) {
            roomMatcher.reset();
            lastPartyPuzzleKey = "";
            clearPuzzleMemory();
            clearPlayerHeadMotions();
            if (!DungeonContext.inDungeon() || mc.theWorld == null) clearRoster();
            return;
        }
        if (puzzleMemoryWorld != mc.theWorld) {
            clearPuzzleMemory();
            puzzleMemoryWorld = mc.theWorld;
        }
        roomMatcher.tick(mc); // Re-checks templates every 40 client ticks (~2 seconds).
        rememberDetectedPuzzle();
        if (AppleSettings.partyPuzzleAnnounce) announcePuzzleToParty(mc);
        if (mc.theWorld != rosterWorld || System.currentTimeMillis() - rosterUpdatedAt >= 900L) refreshRoster(mc);
        advancePlayerHeadMotions();
    }

    private void rememberDetectedPuzzle() {
        PuzzleRoomMatcher.Match match = roomMatcher.current();
        if (match == null || match.confidence < 0.65D) return;
        puzzleMemory.remember(match.name, match.originX, match.originY, match.originZ, match.confidence);
    }

    /** Read-only current local match for the separately opt-in sync manager; it never exposes roster data. */
    public PuzzleRoomMatcher.Match currentPuzzleMatchForSync() {
        return roomMatcher.current();
    }

    /** Applies a worker-validated teammate label to the same current-run memory used by the local map renderer. */
    public void rememberSyncedPuzzle(String name, int originX, int originY, int originZ, double confidence) {
        puzzleMemory.remember(name, originX, originY, originZ, confidence);
    }

    private void clearPuzzleMemory() {
        puzzleMemory.clear();
        puzzleMemoryWorld = null;
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        roomMatcher.reset();
        lastPartyPuzzleKey = "";
        clearRoster();
        clearPuzzleMemory();
        clearPlayerHeadMotions();
        rotationZoomSource = null;
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || !AppleSettings.dungeonMap || !DungeonContext.inDungeon()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null || mc.entityRenderer == null) return;
        ItemStack stack = mc.thePlayer.inventory.getStackInSlot(8); // hotbar slot 9
        if (stack == null || !(stack.getItem() instanceof ItemMap)) return;
        MapData data = ((ItemMap) stack.getItem()).getMapData(stack, mc.theWorld);
        if (data == null || data.colors == null || data.colors.length < 128 * 128) return;
        drawMap(mc, data, event.partialTicks);
    }

    private void drawMap(Minecraft mc, MapData data, float partialTicks) {
        ScaledResolution sr = new ScaledResolution(mc);
        // Position and size use the same geometry as the HUD editor. Unlike ordinary HUD cards,
        // the dungeon map may extend beyond the viewport when the user deliberately places it there.
        int screenW = sr.getScaledWidth();
        int screenH = sr.getScaledHeight();
        int size = AppleSettings.dungeonMapPixelSize(screenW, screenH);
        // Derive the actual GL scale from the integer widget size, exactly matching the editor.
        float scale = size / 128.0F;
        int x = AppleSettings.dungeonMapLeft(screenW, screenH, size);
        int y = AppleSettings.dungeonMapTop(screenW, screenH, size);
        Vec4b self = findSelfMarker(data);
        boolean rotate = AppleSettings.dungeonMapRotate && self != null;
        float pivotX = self == null ? 64.0F : self.func_176112_b() / 2.0F + 64.0F;
        float pivotY = self == null ? 64.0F : self.func_176113_c() / 2.0F + 64.0F;
        float angle = rotate ? 180.0F - MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw) : 0.0F;
        // Content zoom is independent from the HUD widget scale. In rotating mode it multiplies
        // the stable fit zoom, so turning the player never changes the selected magnification.
        float contentZoom = AppleSettings.dungeonMapZoomFactor();
        float rotateZoom = rotate ? stableRotationZoom(data, self) * contentZoom : contentZoom;
        DungeonMapProjection projection = new DungeonMapProjection(rotate, pivotX, pivotY, angle, rotateZoom);
        List<PlayerHead> playerHeads = AppleSettings.dungeonMapPlayerHeads
                ? collectPlayerHeads(mc, data) : Collections.<PlayerHead>emptyList();
        if (!AppleSettings.dungeonMapPlayerHeads) clearPlayerHeadMotions();
        Set<String> replacedPlayerMarkerKeys = playerHeadMarkerKeys(playerHeads);

        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x, y, 0.0F);
            GlStateManager.scale(scale, scale, 1.0F);
            GlStateManager.enableTexture2D();
            GlStateManager.enableAlpha();
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

            if (rotate) {
                // Keep pixels not covered by the native map texture transparent. An opaque fill here
                // obscures the game behind unknown map pixels and makes the entire map look black.
                renderRotatingMap(mc, data, self, angle, rotateZoom, x, y, scale, replacedPlayerMarkerKeys);
            } else {
                renderStaticMap(mc, data, contentZoom, x, y, scale, replacedPlayerMarkerKeys);
            }

            // Text and faces are deliberately drawn after the map transform: their anchors follow the world map,
            // while labels remain level and player skin faces replace only their matching native arrows.
            drawDiscoveredPuzzleNames(mc, data, projection);
            drawPlayerHeads(playerHeads, projection, partialTicks);
            drawPlayerNames(mc, data, projection);
            drawSecretsCount(mc);
        } finally {
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.enableAlpha();
            GlStateManager.disableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }
    }

    /** Scales static-map contents around their center while keeping the HUD viewport fixed and clipped. */
    private void renderStaticMap(Minecraft mc, MapData data, float contentZoom,
                                 int screenX, int screenY, float scale, Set<String> replacedPlayerMarkerKeys) {
        ScaledResolution resolution = new ScaledResolution(mc);
        int factor = resolution.getScaleFactor();
        int scissorX = Math.round(screenX * factor);
        int scissorSize = Math.max(1, Math.round(128.0F * scale * factor));
        int scissorY = mc.displayHeight - Math.round(screenY * factor) - scissorSize;
        boolean wasScissored = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        PREVIOUS_SCISSOR.clear();
        GL11.glGetInteger(GL11.GL_SCISSOR_BOX, PREVIOUS_SCISSOR);
        int prevX = PREVIOUS_SCISSOR.get(0);
        int prevY = PREVIOUS_SCISSOR.get(1);
        int prevW = PREVIOUS_SCISSOR.get(2);
        int prevH = PREVIOUS_SCISSOR.get(3);

        Map<String, Vec4b> actualDecorations = data.mapDecorations;
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissorX, scissorY, scissorSize, scissorSize);
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(64.0F, 64.0F, 0.0F);
            GlStateManager.scale(contentZoom, contentZoom, 1.0F);
            GlStateManager.translate(-64.0F, -64.0F, 0.0F);
            // Vanilla draws decorations as part of renderMap. Temporarily omit only skins we can actually draw,
            // preserving every non-party map icon and avoiding a one-frame arrow/face overlap.
            data.mapDecorations = withoutPlayerHeadMarkers(actualDecorations, replacedPlayerMarkerKeys);
            try {
                mc.entityRenderer.getMapItemRenderer().renderMap(data, false);
            } finally {
                data.mapDecorations = actualDecorations;
            }
        } finally {
            data.mapDecorations = actualDecorations;
            GlStateManager.popMatrix();
            if (wasScissored) {
                GL11.glScissor(prevX, prevY, prevW, prevH);
            } else {
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
            }
        }
    }

    /** Rotates around the local-player decoration and clips to the map square. */
    private void renderRotatingMap(Minecraft mc, MapData data, Vec4b self, float angle, float rotateZoom,
                                   int screenX, int screenY, float scale, Set<String> replacedPlayerMarkerKeys) {
        float pivotX = self.func_176112_b() / 2.0F + 64.0F;
        float pivotY = self.func_176113_c() / 2.0F + 64.0F;
        ScaledResolution resolution = new ScaledResolution(mc);
        int factor = resolution.getScaleFactor();
        int scissorX = Math.round(screenX * factor);
        int scissorSize = Math.max(1, Math.round(128.0F * scale * factor));
        int scissorY = mc.displayHeight - Math.round(screenY * factor) - scissorSize;
        boolean wasScissored = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        PREVIOUS_SCISSOR.clear();
        GL11.glGetInteger(GL11.GL_SCISSOR_BOX, PREVIOUS_SCISSOR);
        int prevX = PREVIOUS_SCISSOR.get(0);
        int prevY = PREVIOUS_SCISSOR.get(1);
        int prevW = PREVIOUS_SCISSOR.get(2);
        int prevH = PREVIOUS_SCISSOR.get(3);

        Map<String, Vec4b> actualDecorations = data.mapDecorations;
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissorX, scissorY, scissorSize, scissorSize);
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(64.0F, 64.0F, 0.0F);
            GlStateManager.scale(rotateZoom, rotateZoom, 1.0F);
            GlStateManager.rotate(angle, 0.0F, 0.0F, 1.0F);
            GlStateManager.translate(-pivotX, -pivotY, 0.0F);
            data.mapDecorations = NO_DECORATIONS;
            try {
                mc.entityRenderer.getMapItemRenderer().renderMap(data, false);
            } finally {
                data.mapDecorations = actualDecorations;
            }
            drawNativeMapMarkers(mc, withoutPlayerHeadMarkers(actualDecorations, replacedPlayerMarkerKeys), angle);
        } finally {
            data.mapDecorations = actualDecorations;
            GlStateManager.popMatrix();
            if (wasScissored) {
                GL11.glScissor(prevX, prevY, prevW, prevH);
            } else {
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
            }
        }
    }

    /** Creates the tiny filtered decoration set only when an available skin is taking a matching arrow's place. */
    private static Map<String, Vec4b> withoutPlayerHeadMarkers(Map<String, Vec4b> decorations,
                                                                 Set<String> replacedPlayerMarkerKeys) {
        if (decorations == null || decorations.isEmpty() || replacedPlayerMarkerKeys == null
                || replacedPlayerMarkerKeys.isEmpty()) return decorations;
        Map<String, Vec4b> visible = new HashMap<String, Vec4b>();
        for (Map.Entry<String, Vec4b> entry : decorations.entrySet()) {
            if (!replacedPlayerMarkerKeys.contains(entry.getKey())) visible.put(entry.getKey(), entry.getValue());
        }
        return visible;
    }

    private static void drawNativeMapMarkers(Minecraft mc, Map<String, Vec4b> decorations, float angle) {
        if (decorations == null || decorations.isEmpty()) return;
        mc.getTextureManager().bindTexture(MAP_ICONS);
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        List<Map.Entry<String, Vec4b>> sorted = sortedDecorations(decorations);
        int depthIndex = 0;
        for (Map.Entry<String, Vec4b> entry : sorted) {
            Vec4b marker = entry.getValue();
            if (marker == null) continue;
            boolean local = marker.func_176110_a() == 1;
            float rotation = local ? (180.0F - angle) * 16.0F / 360.0F : marker.func_176111_d();
            drawNativeMarker(marker.func_176112_b() / 2.0F + 64.0F,
                    marker.func_176113_c() / 2.0F + 64.0F, rotation, marker.func_176110_a(), depthIndex++);
        }
    }

    private static void drawNativeMarker(float x, float y, float rotation, byte type, int index) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer buffer = tessellator.getWorldRenderer();
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x, y, -0.02F);
            GlStateManager.rotate(rotation * 360.0F / 16.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.scale(4.0F, 4.0F, 3.0F);
            GlStateManager.translate(-0.125F, 0.125F, 0.0F);
            int icon = type & 15;
            float u0 = (icon % 4) / 4.0F;
            float v0 = (icon / 4) / 4.0F;
            float u1 = (icon % 4 + 1) / 4.0F;
            float v1 = (icon / 4 + 1) / 4.0F;
            float depth = index * -0.001F;
            buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
            buffer.pos(-1.0D, 1.0D, depth).tex(u0, v0).endVertex();
            buffer.pos(1.0D, 1.0D, depth).tex(u1, v0).endVertex();
            buffer.pos(1.0D, -1.0D, depth).tex(u1, v1).endVertex();
            buffer.pos(-1.0D, -1.0D, depth).tex(u0, v1).endVertex();
            tessellator.draw();
        } finally {
            GlStateManager.popMatrix();
        }
    }

    private float stableRotationZoom(MapData data, Vec4b self) {
        // Freeze the fit for one map-data instance. This prevents marker updates during a run from
        // making the map pulse in size; yaw changes never enter this calculation.
        if (data != rotationZoomSource) {
            rotationZoomSource = data;
            float pivotX = self.func_176112_b() / 2.0F + 64.0F;
            float pivotY = self.func_176113_c() / 2.0F + 64.0F;
            cachedRotationZoom = DungeonMapProjection.fitZoom(pivotX, pivotY);
        }
        return cachedRotationZoom;
    }

    /** Previously discovered names stay attached to their room coordinates until the dungeon run ends. */
    private void drawDiscoveredPuzzleNames(Minecraft mc, MapData data, DungeonMapProjection projection) {
        if (!AppleSettings.dungeonMapShowPuzzleName || puzzleMemory.size() == 0 || mc.thePlayer == null) return;
        Vec4b self = findSelfMarker(data);
        if (self == null) return;
        float selfMapX = self.func_176112_b() / 2.0F + 64.0F;
        float selfMapY = self.func_176113_c() / 2.0F + 64.0F;
        // Use the cached scale detected from the current map; this keeps room centers aligned
        // with the base-map pixels without rescanning the map for every remembered room.
        float pixelsPerBlock = detectMapPixelsPerBlock(data);
        for (PuzzleRoomMemory.RoomLabel room : puzzleMemory.snapshot()) {
            // Dungeon room cells are 32x32 blocks: anchor the label at the exact cell center.
            float worldCenterX = room.originX + 16.0F;
            float worldCenterZ = room.originZ + 16.0F;
            float rawCenterX = selfMapX + (worldCenterX - (float) mc.thePlayer.posX) * pixelsPerBlock;
            float rawCenterY = selfMapY + (worldCenterZ - (float) mc.thePlayer.posZ) * pixelsPerBlock;
            float[] point = projection.project(rawCenterX, rawCenterY);
            if (!insideMap(point[0], point[1])) continue;

            PuzzleLabels.Style style = PuzzleLabels.of(room.name);
            String text = style.full;
            int textWidth = mc.fontRendererObj.getStringWidth(text);
            // Text is actually scaled below (not just used for positioning). Keep it restrained
            // and prefer the established short form when the full name would be too wide.
            float textScale = Math.min(0.46F, 26.0F / Math.max(1, textWidth));
            if (textScale < 0.34F && style.shortName != null && !style.shortName.isEmpty()) {
                text = style.shortName;
                textWidth = mc.fontRendererObj.getStringWidth(text);
                textScale = Math.min(0.46F, 26.0F / Math.max(1, textWidth));
            }

            float visualWidth = textWidth * textScale;
            float visualHeight = mc.fontRendererObj.FONT_HEIGHT * textScale;
            if (point[0] < visualWidth * 0.5F || point[0] > 128.0F - visualWidth * 0.5F
                    || point[1] < visualHeight * 0.5F || point[1] > 128.0F - visualHeight * 0.5F) continue;
            GlStateManager.pushMatrix();
            try {
                // The anchor follows the map projection, but glyphs remain horizontal and centered.
                GlStateManager.translate(point[0] - visualWidth * 0.5F, point[1] - visualHeight * 0.5F, 0.0F);
                GlStateManager.scale(textScale, textScale, 1.0F);
                mc.fontRendererObj.drawStringWithShadow(text, 0.0F, 0.0F, 0xFF55FF55);
            } finally {
                GlStateManager.popMatrix();
            }
        }
    }

    /** Collects only party/local markers with a loaded tab-list skin, so an unavailable texture keeps its normal arrow. */
    private List<PlayerHead> collectPlayerHeads(Minecraft mc, MapData data) {
        if (playerHeadMotionData != data) {
            clearPlayerHeadMotions();
            playerHeadMotionData = data;
        }
        Map<String, String> names = mappedNames(mc, data);
        if (names.isEmpty()) {
            playerHeadMotions.clear();
            return Collections.emptyList();
        }
        List<PlayerHead> heads = new ArrayList<PlayerHead>();
        Set<String> activeIdentities = new HashSet<String>();
        for (Map.Entry<String, Vec4b> entry : sortedDecorations(data.mapDecorations)) {
            Vec4b marker = entry.getValue();
            if (!isPlayerMarker(marker)) continue;
            String name = names.get(entry.getKey());
            ResourceLocation skin = loadedSkin(mc, name);
            if (skin == null) continue;
            String identity = String.valueOf(entry.getKey()) + "|" + name.toLowerCase(Locale.ROOT);
            DungeonMapPlayerHeadMotion motion = playerHeadMotions.get(identity);
            if (motion == null) {
                motion = new DungeonMapPlayerHeadMotion();
                playerHeadMotions.put(identity, motion);
            }
            motion.update(marker.func_176112_b() / 2.0F + 64.0F, marker.func_176113_c() / 2.0F + 64.0F);
            activeIdentities.add(identity);
            heads.add(new PlayerHead(entry.getKey(), skin, motion));
        }
        for (Iterator<Map.Entry<String, DungeonMapPlayerHeadMotion>> it = playerHeadMotions.entrySet().iterator();
             it.hasNext();) {
            if (!activeIdentities.contains(it.next().getKey())) it.remove();
        }
        return heads;
    }

    /** Vanilla starts a safe asynchronous skin fetch on getLocationSkin; draw only after the tab texture is ready. */
    private static ResourceLocation loadedSkin(Minecraft mc, String name) {
        if (mc == null || mc.getNetHandler() == null || name == null || name.length() == 0) return null;
        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(name);
        if (info == null) return null;
        ResourceLocation requested = info.getLocationSkin();
        return info.hasLocationSkin() ? requested : null;
    }

    private static Set<String> playerHeadMarkerKeys(List<PlayerHead> heads) {
        if (heads == null || heads.isEmpty()) return Collections.emptySet();
        Set<String> keys = new HashSet<String>();
        for (PlayerHead head : heads) keys.add(head.markerKey);
        return keys;
    }

    /** Faces are projected through exactly the same static/rotated/zoomed map transform as names and puzzle labels. */
    private static void drawPlayerHeads(List<PlayerHead> heads, DungeonMapProjection projection, float partialTicks) {
        if (heads == null || heads.isEmpty() || projection == null) return;
        float headSize = AppleSettings.effectiveDungeonMapPlayerHeadSize();
        float half = headSize * .5F;
        for (PlayerHead head : heads) {
            float[] point = projection.project(head.motion.x(partialTicks), head.motion.y(partialTicks));
            float centerX = DungeonMapPlayerHeadMotion.clampCenter(point[0], headSize);
            float centerY = DungeonMapPlayerHeadMotion.clampCenter(point[1], headSize);
            // SkinHead draws the 8x8 face plus the transparent hat layer using the Android-safe GUI texture path.
            SkinHead.draw(head.skin, centerX - half, centerY - half, headSize, 1.0F);
        }
    }

    private static boolean isPlayerMarker(Vec4b marker) {
        if (marker == null) return false;
        int type = marker.func_176110_a();
        return type == 0 || type == 1 || type == 3;
    }

    private void drawPlayerNames(Minecraft mc, MapData data, DungeonMapProjection projection) {
        if (!AppleSettings.dungeonMapShowPlayerNames || data.mapDecorations == null || data.mapDecorations.isEmpty()) return;
        Map<String, String> names = mappedNames(mc, data);
        for (Map.Entry<String, Vec4b> entry : sortedDecorations(data.mapDecorations)) {
            Vec4b marker = entry.getValue();
            String name = names.get(entry.getKey());
            if (marker == null || name == null) continue;
            if (!isPlayerMarker(marker)) continue;
            name = StringUtils.stripControlCodes(name);
            if (name == null || name.isEmpty()) continue;
            float rawX = marker.func_176112_b() / 2.0F + 64.0F;
            float rawY = marker.func_176113_c() / 2.0F + 64.0F;
            float[] point = projection.project(rawX, rawY);
            if (!insideMap(point[0], point[1])) continue;
            int width = mc.fontRendererObj.getStringWidth(name);
            float textScale = Math.min(0.55F, 36.0F / Math.max(1, width));
            float halfTextWidth = width * textScale * 0.5F;
            if (point[0] < halfTextWidth || point[0] > 128.0F - halfTextWidth || point[1] < 8.0F) continue;
            GlStateManager.pushMatrix();
            try {
                GlStateManager.translate(point[0] - width * textScale * 0.5F, point[1] - 8.0F, 0.0F);
                GlStateManager.scale(textScale, textScale, 1.0F);
                mc.fontRendererObj.drawStringWithShadow(name, 0.0F, 0.0F, 0xFFFFFFFF);
            } finally {
                GlStateManager.popMatrix();
            }
        }
    }

    private static boolean insideMap(float x, float y) {
        return x >= 0.0F && x < 128.0F && y >= 0.0F && y < 128.0F;
    }

    private static Vec4b findSelfMarker(MapData data) {
        if (data.mapDecorations == null) return null;
        for (Vec4b marker : data.mapDecorations.values()) {
            if (marker != null && marker.func_176110_a() == 1) return marker;
        }
        return null;
    }

    private static MapData mapScaleSource;
    private static long mapScaleCheckedAt;
    private static float cachedPixelsPerBlock = 20.0F / 32.0F;
    // Freeze zoom per map instance so movement/marker refreshes do not pulse the map's scale.
    private MapData rotationZoomSource;
    private float cachedRotationZoom = 0.66F;

    private static float detectMapPixelsPerBlock(MapData data) {
        long now = System.currentTimeMillis();
        if (data == mapScaleSource && now - mapScaleCheckedAt < 2000L) return cachedPixelsPerBlock;
        mapScaleSource = data;
        mapScaleCheckedAt = now;
        int longest = 0;
        for (int y = 0; y < 128; y++) {
            int run = 0;
            for (int x = 0; x < 128; x++) {
                if ((data.colors[y * 128 + x] & 0xFF) == 30) {
                    run++;
                    if (run > longest) longest = run;
                } else run = 0;
            }
        }
        cachedPixelsPerBlock = longest >= 18 ? 22.0F / 32.0F : 20.0F / 32.0F;
        return cachedPixelsPerBlock;
    }

    private void announcePuzzleToParty(Minecraft mc) {
        PuzzleRoomMatcher.Match match = roomMatcher.current();
        if (match == null) { lastPartyPuzzleKey = ""; return; }
        if (match.confidence < 0.65D || mc.thePlayer == null) return;
        String key = match.name + "@" + match.originX + "," + match.originY + "," + match.originZ;
        if (key.equals(lastPartyPuzzleKey)) return;
        lastPartyPuzzleKey = key;
        String code = stableRoomCode(match.originX, match.originZ);
        String puzzle = match.name.trim().replaceAll("\\s+", " ");
        mc.thePlayer.sendChatMessage("/pc " + puzzle + " puzzle [" + code + "]");
    }

    /** Grid code is derived only from the room's fixed 32-block dungeon cell, never player position. */
    static String stableRoomCode(int originX, int originZ) {
        int column = Math.floorDiv(originX, 32);
        int row = Math.floorDiv(originZ, 32);
        return gridLetters(column) + (row + 1);
    }

    private static String gridLetters(int index) {
        int value = Math.max(0, index);
        StringBuilder out = new StringBuilder();
        do {
            out.insert(0, (char) ('A' + value % 26));
            value = value / 26 - 1;
        } while (value >= 0);
        return out.toString();
    }


    private Map<String, String> mappedNames(Minecraft mc, MapData data) {
        List<Map.Entry<String, Vec4b>> icons = sortedDecorations(data.mapDecorations);
        // Marker positions change as players move. They must not invalidate name assignments: doing so
        // repeatedly made labels jump between players and appear to flicker.
        long hash = 1469598103934665603L;
        for (Map.Entry<String, Vec4b> entry : icons) {
            Vec4b marker = entry.getValue();
            hash = (hash ^ (entry.getKey() == null ? 0 : entry.getKey().hashCode())) * 1099511628211L;
            hash = (hash ^ (marker == null ? -1 : marker.func_176110_a())) * 1099511628211L;
        }
        long currentRosterHash = rosterSignature(roster);
        if (assignmentData == data && decorationHash == hash && rosterHash == currentRosterHash) return assignments;

        Map<String, String> previousAssignments = assignmentData == data ? assignments : Collections.<String, String>emptyMap();
        assignmentData = data;
        decorationHash = hash;
        rosterHash = currentRosterHash;
        Map<String, String> out = new HashMap<String, String>();
        if (mc.getNetHandler() == null || mc.thePlayer == null || mc.theWorld == null) {
            assignments = out;
            return out;
        }

        List<String> names = roster;
        Vec4b self = findSelfMarker(data);
        List<Integer> candidateIndexes = new ArrayList<Integer>();
        List<String> candidateKeys = new ArrayList<String>();
        for (int i = 0; i < icons.size(); i++) {
            Vec4b marker = icons.get(i).getValue();
            if (marker != null && (marker.func_176110_a() == 0 || marker.func_176110_a() == 1 || marker.func_176110_a() == 3)) {
                candidateIndexes.add(i);
                candidateKeys.add(icons.get(i).getKey());
            }
        }

        Set<String> used = new HashSet<String>();
        String selfName = mc.thePlayer.getName();
        String selfKey = null;
        if (self != null) {
            for (int idx : candidateIndexes) {
                if (icons.get(idx).getValue() == self) {
                    selfKey = icons.get(idx).getKey();
                    break;
                }
            }
            if (selfKey != null) {
                out.put(selfKey, selfName);
                used.add(selfName.toLowerCase(Locale.ROOT));
            }
        }

        // Reuse existing key-to-name pairs when the roster or marker set changes. Keys that remain
        // stable keep their labels even when a refresh changes marker positions or iteration order.
        for (String key : candidateKeys) {
            if (key.equals(selfKey)) continue;
            String oldName = previousAssignments.get(key);
            if (oldName == null || !containsIgnoreCase(names, oldName)) continue;
            String normalized = oldName.toLowerCase(Locale.ROOT);
            if (used.add(normalized)) out.put(key, oldName);
        }

        if (self != null) {
            double selfX = self.func_176112_b() / 2.0D + 64.0D;
            double selfZ = self.func_176113_c() / 2.0D + 64.0D;
            List<String> remainingNames = new ArrayList<String>();
            for (String name : names) if (name != null && !used.contains(name.toLowerCase(Locale.ROOT))) remainingNames.add(name);
            List<Integer> remainingIndexes = new ArrayList<Integer>();
            for (int idx : candidateIndexes) if (!out.containsKey(icons.get(idx).getKey())) remainingIndexes.add(idx);

            // Use live entity coordinates when available. Remote players may not be loaded as entities
            // on the client, so this pass is only an improvement and never a requirement for a label.
            for (String name : remainingNames) {
                EntityPlayer entity = mc.theWorld.getPlayerEntityByName(name);
                if (entity == null || entity == mc.thePlayer) continue;
                int bestIdx = -1;
                double bestDistance = Double.MAX_VALUE;
                for (double pxPerBlock : new double[]{20.0D / 32.0D, 22.0D / 32.0D}) {
                    double tx = selfX + (entity.posX - mc.thePlayer.posX) * pxPerBlock;
                    double tz = selfZ + (entity.posZ - mc.thePlayer.posZ) * pxPerBlock;
                    for (int idx : remainingIndexes) {
                        Vec4b marker = icons.get(idx).getValue();
                        if (marker == null) continue;
                        double dx = marker.func_176112_b() / 2.0D + 64.0D - tx;
                        double dz = marker.func_176113_c() / 2.0D + 64.0D - tz;
                        double distance = Math.sqrt(dx * dx + dz * dz);
                        if (distance < bestDistance) { bestDistance = distance; bestIdx = idx; }
                    }
                }
                if (bestIdx >= 0 && bestDistance <= 4.0D) {
                    String key = icons.get(bestIdx).getKey();
                    if (used.add(name.toLowerCase(Locale.ROOT))) {
                        out.put(key, name);
                        remainingIndexes.remove(Integer.valueOf(bestIdx));
                    }
                }
            }
        }

        // Always fill remaining player markers from the party roster, even when the roster and marker
        // counts differ. The old equal-count requirement silently dropped remote players whenever the
        // tab list contained extra entries or a remote entity was not loaded locally.
        assignments = DungeonMapPlayerNameAssignments.fillUnassigned(candidateKeys, names, out);
        return assignments;
    }

    private static long rosterSignature(List<String> names) {
        long hash = 1469598103934665603L;
        for (String name : names) {
            hash = (hash ^ (name == null ? 0 : name.toLowerCase(Locale.ROOT).hashCode())) * 1099511628211L;
        }
        return hash;
    }

    private static List<Map.Entry<String, Vec4b>> sortedDecorations(Map<String, Vec4b> decorations) {
        List<Map.Entry<String, Vec4b>> result = new ArrayList<Map.Entry<String, Vec4b>>();
        if (decorations != null) for (Map.Entry<String, Vec4b> entry : decorations.entrySet()) result.add(entry);
        Collections.sort(result, new Comparator<Map.Entry<String, Vec4b>>() {
            @Override public int compare(Map.Entry<String, Vec4b> a, Map.Entry<String, Vec4b> b) {
                int ai = keyIndex(a.getKey()), bi = keyIndex(b.getKey());
                if (ai != bi) return ai < bi ? -1 : 1;
                return a.getKey().compareTo(b.getKey());
            }
        });
        return result;
    }
    private static int keyIndex(String key) {
        Matcher m = ICON_KEY.matcher(key == null ? "" : key);
        if (!m.matches()) return Integer.MAX_VALUE;
        try { return Integer.parseInt(m.group(1)); } catch (NumberFormatException e) { return Integer.MAX_VALUE; }
    }

    private void refreshRoster(Minecraft mc) {
        rosterWorld = mc.theWorld; rosterUpdatedAt = System.currentTimeMillis();
        if (mc.getNetHandler() == null) { updateRoster(Collections.<String>emptyList()); return; }
        List<NetworkPlayerInfo> info = new ArrayList<NetworkPlayerInfo>(mc.getNetHandler().getPlayerInfoMap());
        Collections.sort(info, new Comparator<NetworkPlayerInfo>() {
            @Override public int compare(NetworkPlayerInfo a, NetworkPlayerInfo b) {
                if (a == b) return 0; if (a == null) return 1; if (b == null) return -1;
                boolean ap = a.getGameType() != WorldSettings.GameType.SPECTATOR;
                boolean bp = b.getGameType() != WorldSettings.GameType.SPECTATOR;
                if (ap != bp) return ap ? -1 : 1;
                String at = a.getPlayerTeam() == null ? "" : a.getPlayerTeam().getRegisteredName();
                String bt = b.getPlayerTeam() == null ? "" : b.getPlayerTeam().getRegisteredName();
                int c = at.compareToIgnoreCase(bt); if (c != 0) return c;
                return a.getGameProfile().getName().compareToIgnoreCase(b.getGameProfile().getName());
            }
        });
        List<String> known = new ArrayList<String>();
        for (NetworkPlayerInfo player : info) {
            if (player != null && player.getGameProfile() != null) known.add(player.getGameProfile().getName());
        }
        List<String> sidebar = sidebarLines(mc.theWorld.getScoreboard());
        List<String> orderedParty = DungeonMapPartyRoster.build(sidebar, known, mc.thePlayer.getName());
        updateRoster(orderedParty);
    }

    private void updateRoster(List<String> nextRoster) {
        if (sameRosterMembers(roster, nextRoster)) return;
        roster = Collections.unmodifiableList(new ArrayList<String>(nextRoster));
        // mappedNames() observes the changed roster signature and rebuilds only once.
    }

    private static boolean sameRosterMembers(List<String> current, List<String> next) {
        if (current.size() != next.size()) return false;
        Set<String> currentNames = new HashSet<String>();
        for (String name : current) if (name != null) currentNames.add(name.toLowerCase(Locale.ROOT));
        for (String name : next) {
            if (name == null || !currentNames.remove(name.toLowerCase(Locale.ROOT))) return false;
        }
        return currentNames.isEmpty();
    }

    private static List<String> sidebarLines(Scoreboard board) {
        List<String> lines = new ArrayList<String>();
        if (board == null) return lines;
        ScoreObjective objective = board.getObjectiveInDisplaySlot(1);
        if (objective == null) return lines;
        Collection<Score> scores = board.getSortedScores(objective);
        if (scores == null) return lines;
        for (Score score : scores) {
            if (score == null || score.getPlayerName() == null || score.getPlayerName().startsWith("#")) continue;
            String line = ScorePlayerTeam.formatPlayerName(board.getPlayersTeam(score.getPlayerName()), score.getPlayerName());
            lines.add(StringUtils.stripControlCodes(line));
        }
        return lines;
    }

    private static boolean containsIgnoreCase(List<String> values, String needle) {
        for (String value : values) if (value != null && needle != null && value.equalsIgnoreCase(needle)) return true;
        return false;
    }

    private void drawSecretsCount(Minecraft mc) {
        String count = currentSecrets(mc.theWorld.getScoreboard());
        if (count == null) return;
        String text = "Secrets: " + count;
        mc.fontRendererObj.drawStringWithShadow(text, 3.0F, 116.0F, 0xFFFFFFFF);
    }

    static String currentSecrets(Scoreboard scoreboard) {
        for (String line : sidebarLines(scoreboard)) {
            Matcher matcher = SECRET_COUNT.matcher(LeapText.strip(line));
            if (matcher.find()) return matcher.group(1);
        }
        return null;
    }

    private void advancePlayerHeadMotions() {
        for (DungeonMapPlayerHeadMotion motion : playerHeadMotions.values()) motion.advanceTick();
    }

    private void clearPlayerHeadMotions() {
        playerHeadMotionData = null;
        playerHeadMotions.clear();
    }

    private static final class PlayerHead {
        final String markerKey;
        final ResourceLocation skin;
        final DungeonMapPlayerHeadMotion motion;

        PlayerHead(String markerKey, ResourceLocation skin, DungeonMapPlayerHeadMotion motion) {
            this.markerKey = markerKey;
            this.skin = skin;
            this.motion = motion;
        }
    }

    private void clearRoster() {
        rosterWorld = null; rosterUpdatedAt = 0L; roster = Collections.emptyList();
        assignmentData = null; decorationHash = 0L; rosterHash = 0L; assignments = Collections.emptyMap();
    }
}
