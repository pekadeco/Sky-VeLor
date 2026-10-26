package com.arena.pekaclient.dungeonesp;

import com.arena.pekaclient.config.DungeonEspSettings;
import com.arena.pekaclient.feature.DungeonContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockColored;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Forge bridge for the clean-room starred-mob ESP implementation. Entity association happens only in END client
 * ticks; RenderWorldLast consumes fixed cached records and never walks loadedEntityList. Door blocks are sampled
 * only on a one-second timer, then their cache is rendered without world rescans.
 */
public final class DungeonEspController {

    private static final int DOOR_SCAN_INTERVAL_TICKS = 20;
    private static final int ESP_DUMP_LIMIT = 64;
    private static final Logger LOG = LogManager.getLogger("SkyVeLor");
    private static DungeonEspController activeInstance;

    private final DungeonEspTracker tracker = new DungeonEspTracker();
    private final WitherDoorCache doorCache = new WitherDoorCache();
    private final DungeonEspRenderBatch renderBatch = new DungeonEspRenderBatch();
    private final MinecraftEntitySource entitySource = new MinecraftEntitySource();
    private final MinecraftBlockSource blockSource = new MinecraftBlockSource();
    private int entityTicks;
    private int doorTicks;
    private long lastScanNanos;
    private long lastRenderNanos;
    private boolean activelyTracking;
    private World observedWorld;

    public DungeonEspController() {
        activeInstance = this;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayerSP player = minecraft == null ? null : minecraft.thePlayer;
        World world = minecraft == null ? null : minecraft.theWorld;
        boolean dungeon = world != null && player != null && DungeonContext.inDungeon();
        if (!DungeonEspSettings.enabled || !dungeon) {
            if (activelyTracking || tracker.trackedCount() != 0 || doorCache.size() != 0) clearCaches();
            activelyTracking = false;
            observedWorld = world;
            return;
        }
        if (world != observedWorld) {
            clearCaches();
            observedWorld = world;
        }
        boolean entering = !activelyTracking;
        activelyTracking = true;
        int interval = Math.max(DungeonEspSettings.MIN_INTERVAL_TICKS,
                Math.min(DungeonEspSettings.MAX_INTERVAL_TICKS, DungeonEspSettings.updateIntervalTicks));
        long scanNanosThisTick = 0L;
        boolean scanned = false;
        if (entering || ++entityTicks >= interval) {
            entityTicks = 0;
            entitySource.setWorld(world);
            long started = System.nanoTime();
            tracker.scan(entitySource, player.posX, player.posY, player.posZ,
                    DungeonEspSettings.maxDistance, DungeonEspSettings.hideZeroHealth);
            scanNanosThisTick += System.nanoTime() - started;
            scanned = true;
        }
        if (DungeonEspSettings.doorsEnabled && (entering || ++doorTicks >= DOOR_SCAN_INTERVAL_TICKS)) {
            doorTicks = 0;
            blockSource.setWorld(world);
            long started = System.nanoTime();
            doorCache.scanAround(blockSource, floor(player.posX), floor(player.posY), floor(player.posZ));
            scanNanosThisTick += System.nanoTime() - started;
            scanned = true;
        }
        if (scanned) lastScanNanos = scanNanosThisTick;
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (event == null || !DungeonEspSettings.enabled || !activelyTracking || !DungeonContext.inDungeon()) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.theWorld == null || minecraft.thePlayer == null) return;
        RenderManager renderManager = minecraft.getRenderManager();
        if (renderManager == null) return;
        EntityPlayerSP player = minecraft.thePlayer;
        long started = System.nanoTime();
        double cameraX = renderManager.viewerPosX;
        double cameraY = renderManager.viewerPosY;
        double cameraZ = renderManager.viewerPosZ;
        renderBatch.begin(cameraX, cameraY, cameraZ);
        renderBatch.collectMobs(tracker, player.posX, player.posY, player.posZ,
                cameraX, cameraY, cameraZ, event.partialTicks);
        collectDoors(player);
        renderBatch.render(minecraft, renderManager, cameraX, cameraY, cameraZ);
        lastRenderNanos = System.nanoTime() - started;
    }

    /** Exact command surface for /velor espdebug; all figures come from the live tick/render cache, not config guesses. */
    public static String debugStatus() {
        DungeonEspController controller = activeInstance;
        if (controller == null) {
            return "Dungeon ESP: controller not initialized";
        }
        String dungeon = DungeonContext.inDungeon() ? "IN_DUNGEON" : "OUTSIDE_DUNGEON";
        return "Dungeon ESP " + dungeon + ", master=" + (DungeonEspSettings.enabled ? "ON" : "OFF")
                + ", tracked=" + controller.tracker.trackedCount()
                + ", rendered=" + controller.renderBatch.renderedMobCount()
                + ", labels=" + controller.renderBatch.renderedMobLabelCount()
                + ", doors=" + controller.doorCache.size()
                + ", scan=" + micros(controller.lastScanNanos) + "us"
                + ", render=" + micros(controller.lastRenderNanos) + "us"
                + ", interval=" + DungeonEspSettings.updateIntervalTicks + "t"
                + ", dropped=" + controller.tracker.droppedCandidates();
    }

    /**
     * Command-only nearby-entity diagnostic. It intentionally walks the live list once when requested, never from
     * RenderWorldLast, and logs the same bounded lines it returns for chat.
     */
    public static String[] espDump() {
        DungeonEspController controller = activeInstance;
        if (controller == null) return new String[] {"ESP dump: controller not initialized"};
        return controller.buildEspDump();
    }

    private String[] buildEspDump() {
        Minecraft minecraft = Minecraft.getMinecraft();
        World world = minecraft == null ? null : minecraft.theWorld;
        EntityPlayerSP player = minecraft == null ? null : minecraft.thePlayer;
        if (world == null || player == null || world.loadedEntityList == null) {
            return new String[] {"ESP dump: no client world/player"};
        }
        List<String> lines = new ArrayList<String>();
        double range = Math.max(12D, DungeonEspSettings.maxDistance);
        double rangeSq = range * range;
        lines.add("ESP dump: range=" + (int) range + "m, loaded=" + world.loadedEntityList.size()
                + ", tracked=" + tracker.trackedCount());
        int nearby = 0;
        for (int i = 0; i < world.loadedEntityList.size(); i++) {
            Entity entity = world.loadedEntityList.get(i);
            if (entity == null || distanceSq(entity.posX, entity.posY, entity.posZ,
                    player.posX, player.posY, player.posZ) > rangeSq) continue;
            nearby++;
            if (lines.size() > ESP_DUMP_LIMIT) continue;
            boolean armorStand = entity instanceof EntityArmorStand;
            String name = armorStand ? entity.getCustomNameTag() : entity.getName();
            String texture = armorStand ? skullTexture((EntityArmorStand) entity) : null;
            DungeonEspCategory tagCategory = armorStand ? DungeonEspMatcher.categoryForTag(name) : DungeonEspCategory.NONE;
            boolean star = armorStand && DungeonEspMatcher.hasStar(name);
            boolean knownHead = DungeonEspMatcher.isKnownEndermanHeadTexture(texture);
            boolean namedHead = texture != null && DungeonEspMatcher.isEndermanMarkerName(name);
            boolean hostile = entity instanceof IMob;
            boolean bat = entity instanceof EntityBat;
            boolean invisible = entity.isInvisible();
            boolean npc = isNpcName(entity.getCustomNameTag());
            boolean pet = entity instanceof EntityTameable && ((EntityTameable) entity).isTamed();
            float health = entity instanceof EntityLivingBase ? ((EntityLivingBase) entity).getHealth() : Float.NaN;
            AxisAlignedBB box = entity.getEntityBoundingBox();
            DungeonEspCategory selected = tracker.categoryFor(entity.getEntityId());
            DungeonEspMatchReason reason = tracker.matchReasonFor(entity.getEntityId());
            lines.add("#" + entity.getEntityId() + " " + entity.getClass().getSimpleName()
                    + " name=" + brief(name)
                    + " tag=" + tagCategory + "/star=" + star
                    + " headTexture=" + (knownHead ? "known-enderman" : (namedHead ? "named-skull" : textureState(texture)))
                    + " health=" + healthText(health)
                    + " force=" + (hostile ? "hostile" : (bat ? "bat" : "non-hostile"))
                    + (invisible ? "/invisible" : "") + (npc ? "/npc" : "") + (pet ? "/pet" : "")
                    + " geometry=" + (reason == DungeonEspMatchReason.ENDERMAN_HEAD
                            ? "fixed-1.1x2.7/" + boxText(box) : boxText(box))
                    + " espMatch=" + selected + "/" + reason.text());
        }
        if (nearby == 0) lines.add("No nearby entities inside the ESP range.");
        if (nearby > ESP_DUMP_LIMIT) lines.add("Dump limited to " + ESP_DUMP_LIMIT + " of " + nearby + " nearby entities.");
        String[] output = lines.toArray(new String[lines.size()]);
        for (int i = 0; i < output.length; i++) LOG.info("[Dungeon ESP dump] {}", output[i]);
        return output;
    }

    private void collectDoors(EntityPlayerSP player) {
        if (!DungeonEspSettings.doorsEnabled || !DungeonEspSettings.doors.enabled || doorCache.size() == 0) return;
        if (DungeonEspSettings.doorOnlyWhenKey && !hasWitherKey(player)) return;
        boolean includeBlood = DungeonEspSettings.showBloodDoors;
        if (DungeonEspSettings.doorScope == DungeonEspSettings.DOORS_NEAREST) {
            WitherDoorCache.Door nearest = doorCache.nearest(player.posX, player.posY, player.posZ, includeBlood);
            if (nearest != null) addDoor(nearest, player);
            return;
        }
        for (int i = 0; i < doorCache.size(); i++) {
            WitherDoorCache.Door door = doorCache.at(i);
            if (door.type == WitherDoorCache.Type.BLOOD && !includeBlood) continue;
            addDoor(door, player);
        }
    }

    private void addDoor(WitherDoorCache.Door door, EntityPlayerSP player) {
        int color = door.type == WitherDoorCache.Type.BLOOD ? DungeonEspSettings.bloodDoorColor
                : DungeonEspSettings.doors.color;
        renderBatch.collectDoor(door, player.posX, player.posY, player.posZ, color,
                DungeonEspSettings.doorDistanceText);
    }

    private void clearCaches() {
        tracker.clear();
        doorCache.clear();
        entityTicks = 0;
        doorTicks = 0;
        lastScanNanos = 0L;
        lastRenderNanos = 0L;
    }

    private static boolean hasWitherKey(EntityPlayerSP player) {
        if (player == null || player.inventory == null || player.inventory.mainInventory == null) return false;
        ItemStack[] inventory = player.inventory.mainInventory;
        for (int i = 0; i < inventory.length; i++) {
            ItemStack stack = inventory[i];
            if (stack != null && DungeonEspMatcher.containsToken(stack.getDisplayName(), "wither key")) return true;
        }
        return false;
    }

    private static int floor(double value) {
        int whole = (int) value;
        return value < whole ? whole - 1 : whole;
    }

    private static long micros(long nanos) {
        return nanos <= 0L ? 0L : nanos / 1000L;
    }

    private static String skullTexture(EntityArmorStand stand) {
        if (stand == null) return null;
        ItemStack head = stand.getEquipmentInSlot(4);
        if (head == null || head.getTagCompound() == null) return null;
        NBTTagCompound root = head.getTagCompound();
        if (!root.hasKey("SkullOwner", 10)) return null;
        NBTTagCompound owner = root.getCompoundTag("SkullOwner");
        if (!owner.hasKey("Properties", 10)) return null;
        NBTTagCompound properties = owner.getCompoundTag("Properties");
        if (!properties.hasKey("textures", 9)) return null;
        NBTTagList textures = properties.getTagList("textures", 10);
        if (textures == null || textures.tagCount() <= 0) return null;
        NBTTagCompound first = textures.getCompoundTagAt(0);
        return first.hasKey("Value", 8) ? first.getString("Value") : null;
    }

    private static String brief(String value) {
        if (value == null || value.length() == 0) return "-";
        String compact = value.replace('§', '&');
        return compact.length() <= 38 ? compact : compact.substring(0, 35) + "...";
    }

    private static String textureState(String value) {
        if (value == null || value.length() == 0) return "none";
        return "other:" + (value.length() <= 18 ? value : value.substring(0, 18) + "...");
    }

    private static boolean isNpcName(String name) {
        return DungeonEspMatcher.containsToken(name, "[npc]") || DungeonEspMatcher.containsToken(name, " npc ");
    }

    private static String healthText(float health) {
        return Float.isNaN(health) ? "n/a" : String.format(Locale.ROOT, "%.1f", health);
    }

    private static String boxText(AxisAlignedBB box) {
        if (box == null) return "none";
        return String.format(Locale.ROOT, "%.1f,%.1f,%.1f..%.1f,%.1f,%.1f",
                box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    private static double distanceSq(double x, double y, double z, double otherX, double otherY, double otherZ) {
        double dx = x - otherX;
        double dy = y - otherY;
        double dz = z - otherZ;
        return dx * dx + dy * dy + dz * dz;
    }

    /** Thin adapter that fills the tracker scratch object in-place and never creates a per-entity snapshot. */
    private static final class MinecraftEntitySource implements DungeonEspTracker.EntitySource {
        private List<Entity> entities;

        void setWorld(World world) {
            entities = world == null ? null : world.loadedEntityList;
        }

        @Override
        public int size() {
            return entities == null ? 0 : entities.size();
        }

        @Override
        public void read(int index, DungeonEspTracker.MutableEntity into) {
            if (entities == null || index < 0 || index >= entities.size()) return;
            Entity entity = entities.get(index);
            if (entity == null) return;
            into.id = entity.getEntityId();
            into.x = entity.posX;
            into.y = entity.posY;
            into.z = entity.posZ;
            into.height = entity.height;
            into.dead = entity.isDead;
            into.reference = entity;
            if (entity instanceof EntityArmorStand) {
                into.armorStand = true;
                into.name = entity.getCustomNameTag();
                into.headTexture = skullTexture((EntityArmorStand) entity);
                into.endermanHead = DungeonEspMatcher.isKnownEndermanHeadTexture(into.headTexture);
                return;
            }
            if (entity instanceof EntityLivingBase) {
                into.living = true;
                into.player = entity instanceof EntityPlayer;
                into.enderman = entity instanceof EntityEnderman;
                into.hostile = entity instanceof IMob;
                into.bat = entity instanceof EntityBat;
                into.invisible = entity.isInvisible();
                into.name = entity.getCustomNameTag();
                into.npc = isNpcName(into.name);
                into.pet = entity instanceof EntityTameable && ((EntityTameable) entity).isTamed();
                into.health = ((EntityLivingBase) entity).getHealth();
            }
        }
    }

    /** Reuses one mutable BlockPos across the periodic cache scan rather than allocating one per sampled block. */
    private static final class MinecraftBlockSource implements WitherDoorCache.BlockSource {
        private final BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        private World world;

        void setWorld(World newWorld) {
            world = newWorld;
        }

        @Override
        public DoorBlockKind blockAt(int x, int y, int z) {
            if (world == null) return DoorBlockKind.AIR;
            IBlockState state = world.getBlockState(position.set(x, y, z));
            Block block = state.getBlock();
            if (block == Blocks.coal_block) return DoorBlockKind.COAL;
            if (block == Blocks.stained_hardened_clay
                    && state.getValue(BlockColored.COLOR) == EnumDyeColor.RED) return DoorBlockKind.RED_CLAY;
            return block == Blocks.air ? DoorBlockKind.AIR : DoorBlockKind.OTHER;
        }
    }
}
