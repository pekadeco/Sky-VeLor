package com.arena.pekaclient.render;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.feature.DungeonContext;
import com.arena.pekaclient.feature.MobMarkers;
import com.arena.pekaclient.feature.StarredMobPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dungeon mob markers. Starred mobs are discovered from the exact ✮ floating armor-stand name tag,
 * associated with the living entity below the tag, and refreshed on a throttled 50 ms (20 Hz) cadence.
 * Box mode draws the marker-stand bounds; Outline mode re-renders the associated mob model.
 */
public final class MobHighlight {
    private static final int NORMAL_COLOR = 0xFF7CFC8A;
    private static final int DEFAULT_STAR_COLOR = 0x5C9AFFFF;
    private static final int DEFAULT_STORMY_COLOR = 0xADD8E6FF;
    private static final int DEFAULT_WITHERMANCER_COLOR = 0xA9A9A9FF;
    private static final int DEFAULT_ZOMBIE_COMMANDER_COLOR = 0xFF0000FF;
    private static final int DEFAULT_SKELETON_MASTER_COLOR = 0xFF6400FF;
    /** Refresh entity/name-tag associations at 20 Hz to reduce per-frame world scanning. */
    private static final long CACHE_REFRESH_INTERVAL_MS = 50L;

    private static MobHighlight activeInstance;

    private final Set<EntityLivingBase> normalMobs = Collections.newSetFromMap(
            new IdentityHashMap<EntityLivingBase, Boolean>());
    /** Living models outlined by Outline mode, populated from the floating name-tag armor stands. */
    private final Map<EntityLivingBase, StarredMobInfo> starredMobs =
            new IdentityHashMap<EntityLivingBase, StarredMobInfo>();
    /** Armor stands boxed in Box mode, exactly as in Better NEF. */
    private final Map<EntityArmorStand, StarredMobInfo> starredNameTags =
            new IdentityHashMap<EntityArmorStand, StarredMobInfo>();
    /** Cached target per floating star tag; checked every render so zero-health mobs vanish immediately. */
    private final Map<EntityArmorStand, EntityLivingBase> starredTargetsByTag =
            new IdentityHashMap<EntityArmorStand, EntityLivingBase>();
    /** Fallback for server variants that put ✮ directly on the mob instead of an armor stand. */
    private final Map<EntityLivingBase, StarredMobInfo> directStarredMobs =
            new IdentityHashMap<EntityLivingBase, StarredMobInfo>();
    private final Set<EntityLivingBase> linkedStarredTargets = Collections.newSetFromMap(
            new IdentityHashMap<EntityLivingBase, Boolean>());
    /** Fel armor stands are scanned for head texture/NBT at cache refresh, never on every rendered frame. */
    private final List<EntityArmorStand> felStands = new ArrayList<EntityArmorStand>();
    private World cachedWorld;
    private long lastCacheRefreshTime;

    public MobHighlight() {
        activeInstance = this;
    }

    /** Keep costly world/name-tag association scans to 20 Hz; entity health is checked every render. */
    @SubscribeEvent
    public void onRenderLiving(RenderLivingEvent.Pre<EntityLivingBase> event) {
        long now = System.currentTimeMillis();
        if (lastCacheRefreshTime != 0L && now >= lastCacheRefreshTime
                && now - lastCacheRefreshTime < CACHE_REFRESH_INTERVAL_MS) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null || mc.thePlayer == null) {
            clear();
            return;
        }
        if (cachedWorld != mc.theWorld) {
            clear();
            cachedWorld = mc.theWorld;
        }
        lastCacheRefreshTime = now;
        rebuildCache(mc);
    }

    private void rebuildCache(Minecraft mc) {
        normalMobs.clear();
        starredMobs.clear();
        starredNameTags.clear();
        starredTargetsByTag.clear();
        directStarredMobs.clear();
        linkedStarredTargets.clear();
        felStands.clear();
        World world = mc.theWorld;
        if (world == null || mc.thePlayer == null) {
            return;
        }

        boolean wantsStars = AppleSettings.starredMobDisplayMode != StarredMobPolicy.DISPLAY_DISABLED;
        boolean wantsLevelTags = AppleSettings.mobHighlights;
        boolean wantsFel = AppleSettings.felEsp && DungeonContext.inDungeon();
        if (!wantsStars && !wantsLevelTags && !wantsFel) {
            return;
        }

        List<Entity> loaded = world.loadedEntityList;
        // Entity-owned labels are a fallback for server variants that do not spawn a separate name-tag stand.
        if (wantsStars || wantsLevelTags) {
            for (int i = 0; i < loaded.size(); i++) {
                Entity entity = loaded.get(i);
                if (!(entity instanceof EntityLivingBase) || entity instanceof EntityPlayer
                        || entity instanceof EntityArmorStand || entity.isDead) {
                    continue;
                }
                EntityLivingBase mob = (EntityLivingBase) entity;
                String rawName = getNameText(entity);
                MobMarkers.Kind kind = MobMarkers.classify(rawName);
                if (wantsStars && kind == MobMarkers.Kind.ELITE && !isDying(mob)) {
                    StarredMobInfo info = new StarredMobInfo(rawName);
                    starredMobs.put(mob, info);
                    directStarredMobs.put(mob, info);
                } else if (wantsLevelTags && kind == MobMarkers.Kind.NORMAL) {
                    normalMobs.add(mob);
                }
            }
        }

        for (int i = 0; i < loaded.size(); i++) {
            Entity entity = loaded.get(i);
            if (!(entity instanceof EntityArmorStand) || entity.isDead) {
                continue;
            }
            EntityArmorStand tag = (EntityArmorStand) entity;
            if (wantsFel && hasFelHeadTexture(tag)) felStands.add(tag);
            String tagName = tag.getName();
            if (wantsStars && StarredMobPolicy.hasStarMarker(tagName)) {
                StarredMobInfo info = new StarredMobInfo(tagName);
                // Better NEF boxes the marker stand in Box mode, even if no living target is found.
                if (!isDying(tag)) {
                    starredNameTags.put(tag, info);
                    EntityLivingBase mob = findAssociatedMob(mc, tag);
                    if (mob != null) {
                        starredTargetsByTag.put(tag, mob);
                        if (!isDying(mob)) {
                            linkedStarredTargets.add(mob);
                            if (!starredMobs.containsKey(mob)) {
                                starredMobs.put(mob, info);
                            }
                        }
                    }
                }
                continue;
            }
            if (wantsLevelTags && MobMarkers.classify(tagName) == MobMarkers.Kind.NORMAL) {
                EntityLivingBase mob = findAssociatedMob(mc, tag);
                if (mob != null && !mob.isDead) {
                    normalMobs.add(mob);
                }
            }
        }

        // A starred name tag wins over the optional ordinary [LvNN] outline for the same mob.
        if (!starredMobs.isEmpty()) {
            normalMobs.removeAll(starredMobs.keySet());
        }
    }

    /**
     * Same association volume used by Better NEF: extend the armor-stand bounds by 0.5 blocks sideways
     * and 3 blocks vertically, then choose the first nearby living non-stand target other than a player.
     */
    private static EntityLivingBase findAssociatedMob(Minecraft mc, EntityArmorStand tag) {
        if (mc == null || mc.theWorld == null || tag == null || tag.getEntityBoundingBox() == null) {
            return null;
        }
        AxisAlignedBB query = tag.getEntityBoundingBox().expand(0.5D, 3.0D, 0.5D);
        List<EntityLivingBase> nearby = mc.theWorld.getEntitiesWithinAABB(EntityLivingBase.class, query);
        for (int i = 0; i < nearby.size(); i++) {
            EntityLivingBase candidate = nearby.get(i);
            if (candidate == null || candidate == tag || candidate.isDead
                    || candidate instanceof EntityArmorStand || candidate instanceof EntityPlayer
                    || candidate == mc.thePlayer) {
                continue;
            }
            return candidate;
        }
        return null;
    }

    private static String getNameText(Entity entity) {
        if (entity == null) return "";
        String custom = entity.getCustomNameTag();
        if (custom != null && !custom.isEmpty()) return custom;
        if (entity.getDisplayName() != null) {
            String formatted = entity.getDisplayName().getFormattedText();
            if (formatted != null && !formatted.isEmpty()) return formatted;
        }
        return entity.getName();
    }

    private static boolean isDying(EntityLivingBase entity) {
        if (entity == null || StarredMobPolicy.isDeadOrZeroHealth(entity.isDead, entity.getHealth())) {
            return true;
        }
        return entity.getDisplayName() != null
                && StarredMobPolicy.isDyingName(entity.getDisplayName().getFormattedText());
    }

    @SubscribeEvent
    public void onWorldRender(RenderWorldLastEvent event) {
        if (event == null || !WorldBox.ready()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null || cachedWorld != mc.theWorld) {
            return;
        }
        RenderManager rm = mc.getRenderManager();
        if (rm == null) return;

        if (AppleSettings.mobHighlights && !normalMobs.isEmpty()) {
            drawNormalMobBoxes(event, rm);
        }

        int mode = StarredMobPolicy.clampDisplayMode(AppleSettings.starredMobDisplayMode);
        if (AppleSettings.felEsp && DungeonContext.inDungeon() && !felStands.isEmpty()) {
            drawFelBoxes(event, rm);
        }

        if (mode == StarredMobPolicy.DISPLAY_BOX
                && (!starredNameTags.isEmpty() || !directStarredMobs.isEmpty())) {
            drawStarredMobBoxes(event, rm);
        }
    }

    /** Better NEF-style Fel ESP: draw only the stands whose exact head texture was matched in the 20 Hz cache. */
    private void drawFelBoxes(RenderWorldLastEvent event, RenderManager rm) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null || felStands.isEmpty()) return;
        int color = StarredMobPolicy.parseColor(AppleSettings.felEspColor, 0xFF00FF00);
        WorldBox.begin(rm.viewerPosX, rm.viewerPosY, rm.viewerPosZ, true);
        try {
            for (int i = 0; i < felStands.size(); i++) {
                EntityArmorStand stand = felStands.get(i);
                if (stand == null || stand.isDead || stand.worldObj != mc.theWorld) continue;
                double x = stand.lastTickPosX + (stand.posX - stand.lastTickPosX) * event.partialTicks;
                double y = stand.lastTickPosY + (stand.posY - stand.lastTickPosY) * event.partialTicks;
                double z = stand.lastTickPosZ + (stand.posZ - stand.lastTickPosZ) * event.partialTicks;
                WorldBox.outline(x - 0.5D, y - 0.5D, z - 0.5D,
                        x + 0.5D, y + 3.0D, z + 0.5D, color);
            }
        } finally {
            WorldBox.end();
        }
    }

    /** Texture/NBT matching is deliberately kept out of the per-frame render path. */
    private static boolean hasFelHeadTexture(EntityArmorStand stand) {
        if (stand == null || stand.isDead) return false;
        ItemStack helmet = stand.getEquipmentInSlot(4);
        if (helmet == null || helmet.getItem() != Items.skull || !helmet.hasTagCompound()) return false;
        NBTTagCompound root = helmet.getTagCompound();
        if (!root.hasKey("SkullOwner", 10)) return false;
        NBTTagCompound owner = root.getCompoundTag("SkullOwner");
        if (!owner.hasKey("Properties", 10)) return false;
        NBTTagCompound properties = owner.getCompoundTag("Properties");
        if (!properties.hasKey("textures", 9)) return false;
        NBTTagList textures = properties.getTagList("textures", 10);
        if (textures.tagCount() == 0) return false;
        NBTTagCompound texture = textures.getCompoundTagAt(0);
        return texture.hasKey("Value", 8)
                && StarredMobPolicy.isFelHeadTexture(texture.getString("Value"));
    }

    private void drawNormalMobBoxes(RenderWorldLastEvent event, RenderManager rm) {
        Minecraft mc = Minecraft.getMinecraft();
        WorldBox.begin(rm.viewerPosX, rm.viewerPosY, rm.viewerPosZ, true);
        try {
            // Render on the client thread; the cache only changes in RenderLivingEvent.Pre.
            // Iterate directly to avoid allocating a defensive copy every rendered frame.
            for (EntityLivingBase mob : normalMobs) {
                if (mob == null || mob.isDead || mob.worldObj != mc.theWorld
                        || starredMobs.containsKey(mob)) {
                    continue;
                }
                outlineEntityBox(mob, event.partialTicks, NORMAL_COLOR);
            }
        } finally {
            WorldBox.end();
        }
    }

    private void drawStarredMobBoxes(RenderWorldLastEvent event, RenderManager rm) {
        Minecraft mc = Minecraft.getMinecraft();
        // Better NEF's ESP toggle controls depth testing for the whole starred-box pass.
        WorldBox.begin(rm.viewerPosX, rm.viewerPosY, rm.viewerPosZ, AppleSettings.starredMobEsp);
        try {
            // The maps are updated on the same client thread. Avoid cloning both maps every frame.
            for (Map.Entry<EntityArmorStand, StarredMobInfo> entry : starredNameTags.entrySet()) {
                EntityArmorStand tag = entry.getKey();
                StarredMobInfo info = entry.getValue();
                if (tag == null || tag.isDead || tag.worldObj != mc.theWorld || isDying(tag)) continue;
                // A tag stand can outlive its mob for a short time. Check the cached living target
                // on every frame, not only on the slower association refresh, so HP=0 hides it at once.
                EntityLivingBase target = starredTargetsByTag.get(tag);
                if (target != null && isDying(target)) continue;
                drawStarredNameTagBox(tag, event.partialTicks, info.tagName);
            }

            // Preserve the existing server-variant fallback, without double-boxing mobs that have a tag stand.
            for (Map.Entry<EntityLivingBase, StarredMobInfo> entry : directStarredMobs.entrySet()) {
                EntityLivingBase mob = entry.getKey();
                if (mob == null || mob.isDead || mob.worldObj != mc.theWorld || isDying(mob)
                        || linkedStarredTargets.contains(mob)) continue;
                outlineEntityBox(mob, event.partialTicks, getStarredColor(entry.getValue().tagName));
            }
        } finally {
            WorldBox.end();
        }
    }

    /** Reproduces Better NEF's Box-mode bounds around the floating star name-tag armor stand. */
    private static void drawStarredNameTagBox(EntityArmorStand tag, float partialTicks, String tagName) {
        double x = tag.lastTickPosX + (tag.posX - tag.lastTickPosX) * partialTicks;
        double y = tag.lastTickPosY + (tag.posY - tag.lastTickPosY) * partialTicks;
        double z = tag.lastTickPosZ + (tag.posZ - tag.lastTickPosZ) * partialTicks;
        double minY = y + StarredMobPolicy.starredBoxMinYOffset(tagName);
        double maxY = y + StarredMobPolicy.starredBoxMaxYOffset(tagName);
        WorldBox.outline(x - 0.5D, minY, z - 0.5D, x + 0.5D, maxY, z + 0.5D,
                getStarredColor(tagName));
    }

    private static void outlineEntityBox(EntityLivingBase mob, float partialTicks, int color) {
        AxisAlignedBB box = mob.getEntityBoundingBox();
        if (box == null) return;
        double ox = mob.lastTickPosX + (mob.posX - mob.lastTickPosX) * partialTicks - mob.posX;
        double oy = mob.lastTickPosY + (mob.posY - mob.lastTickPosY) * partialTicks - mob.posY;
        double oz = mob.lastTickPosZ + (mob.posZ - mob.lastTickPosZ) * partialTicks - mob.posZ;
        WorldBox.outline(box.minX + ox, box.minY + oy, box.minZ + oz,
                box.maxX + ox, box.maxY + oy, box.maxZ + oz, color);
    }

    /** Called by the RendererLivingEntity mixin after the original model and layers have rendered. */
    public static void renderModelOutline(EntityLivingBase entity, ModelBase model,
                                          float limbSwing, float limbSwingAmount, float ageInTicks,
                                          float headYaw, float headPitch, float scaleFactor) {
        MobHighlight active = activeInstance;
        if (active == null || entity == null || model == null
                || StarredMobPolicy.clampDisplayMode(AppleSettings.starredMobDisplayMode)
                != StarredMobPolicy.DISPLAY_OUTLINE) {
            return;
        }
        StarredMobInfo info = active.starredMobs.get(entity);
        if (info == null || entity.isInvisible() || active.isDying(entity)) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || entity.worldObj != mc.theWorld) {
            return;
        }
        if (!AppleSettings.starredMobEsp && !mc.thePlayer.canEntityBeSeen(entity)) {
            return;
        }
        StarredMobOutlineRenderer.render(entity, model, limbSwing, limbSwingAmount,
                ageInTicks, headYaw, headPitch, scaleFactor, getStarredColor(info.tagName));
    }

    private static int getStarredColor(String tagName) {
        return StarredMobPolicy.colorForName(tagName,
                getBaseStarredColor(),
                StarredMobPolicy.parseColor(AppleSettings.starredMobStormyColor, DEFAULT_STORMY_COLOR),
                StarredMobPolicy.parseColor(AppleSettings.starredMobWithermancerColor, DEFAULT_WITHERMANCER_COLOR),
                StarredMobPolicy.parseColor(AppleSettings.starredMobZombieCommanderColor, DEFAULT_ZOMBIE_COMMANDER_COLOR),
                StarredMobPolicy.parseColor(AppleSettings.starredMobSkeletonMasterColor, DEFAULT_SKELETON_MASTER_COLOR));
    }

    private static int getBaseStarredColor() {
        return StarredMobPolicy.parseColor(AppleSettings.starredMobColor, DEFAULT_STAR_COLOR);
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event != null && event.world != null && event.world.isRemote) {
            clear();
        }
    }

    private void clear() {
        normalMobs.clear();
        starredMobs.clear();
        starredNameTags.clear();
        starredTargetsByTag.clear();
        directStarredMobs.clear();
        linkedStarredTargets.clear();
        felStands.clear();
        cachedWorld = null;
        lastCacheRefreshTime = 0L;
    }

    private static final class StarredMobInfo {
        private final String tagName;

        private StarredMobInfo(String tagName) {
            this.tagName = tagName == null ? "" : tagName;
        }
    }
}
