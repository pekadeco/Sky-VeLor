package com.arena.pekaclient.dungeon;

import com.arena.pekaclient.feature.DungeonContext;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.List;

/** Matches the player's current 32x32 room against the starred-block templates. Rechecks every 40 ticks (~2s). */
public final class PuzzleRoomMatcher {
    public static final int CHECK_INTERVAL_TICKS = 40;
    private static final int MAX_ROOM_HEIGHT = 55;
    private World worldRef;
    private int cellX = Integer.MIN_VALUE;
    private int cellZ = Integer.MIN_VALUE;
    private int ticksUntilCheck;
    private int consecutiveMisses;
    private Match current;

    public void tick(Minecraft mc) {
        if (mc == null || mc.thePlayer == null || mc.theWorld == null || !DungeonContext.inDungeon()) {
            reset();
            return;
        }
        if (worldRef != mc.theWorld) {
            reset();
            worldRef = mc.theWorld;
        }
        int bx = MathHelper.floor_double(mc.thePlayer.posX);
        int bz = MathHelper.floor_double(mc.thePlayer.posZ);
        int nextCellX = Math.floorDiv(bx, 32) * 32;
        int nextCellZ = Math.floorDiv(bz, 32) * 32;
        if (nextCellX != cellX || nextCellZ != cellZ) {
            cellX = nextCellX; cellZ = nextCellZ;
            current = null; consecutiveMisses = 0; ticksUntilCheck = CHECK_INTERVAL_TICKS - 1;
        }
        // Revalidate a matched room every interval too, so changes to loaded blocks are eventually noticed.
        // Ordinary rooms with repeated misses back off to avoid doing a full scan forever.
        int interval = consecutiveMisses >= 4 ? CHECK_INTERVAL_TICKS * 5 : CHECK_INTERVAL_TICKS;
        if (++ticksUntilCheck < interval) return;
        ticksUntilCheck = 0;
        if (!PuzzleRoomDatabase.isLoaded()) return;
        Match found = scan(mc.theWorld, cellX, cellZ, MathHelper.floor_double(mc.thePlayer.posY));
        if (found != null) {
            current = found;
            consecutiveMisses = 0;
        } else {
            consecutiveMisses++;
            if (consecutiveMisses >= 2) current = null;
        }
    }

    private Match scan(World world, int baseX, int baseZ, int playerY) {
        List<PuzzleRoomDatabase.Room> rooms = PuzzleRoomDatabase.rooms();
        Match best = null;
        int minY = Math.max(1, playerY - MAX_ROOM_HEIGHT - 3);
        int maxY = Math.min(250, playerY + 2);
        for (PuzzleRoomDatabase.Room room : rooms) {
            for (PuzzleRoomDatabase.Variant variant : room.variants) {
                if (variant.blocks.isEmpty() || variant.probes().isEmpty()) continue;
                int minimumProbeSamples = Math.min(variant.probes().size(),
                        Math.max(3, (int) Math.ceil(variant.probes().size() * 0.60D)));
                int minimumAvailableBlocks = Math.min(variant.blocks.size(),
                        Math.max(6, (int) Math.ceil(variant.blocks.size() * 0.60D)));
                for (int baseY = minY; baseY <= maxY; baseY++) {
                    SampleMatchCount probes = countMatches(world, baseX, baseY, baseZ, variant.probes());
                    if (probes.available < minimumProbeSamples) continue;
                    int probeRequired = Math.max(2, (int) Math.ceil(probes.available * (2.0D / 3.0D)));
                    if (probes.matched < probeRequired) continue;

                    SampleMatchCount sampleCount = countMatches(world, baseX, baseY, baseZ, variant.blocks);
                    if (sampleCount.available < minimumAvailableBlocks) continue;
                    int required = Math.max(5, (int) Math.ceil(sampleCount.available * 0.65D));
                    if (sampleCount.matched < required) continue;
                    double confidence = sampleCount.matched / (double) sampleCount.available;
                    if (best == null || confidence > best.confidence) {
                        best = new Match(room.name, variant.rotationY, baseX, baseY, baseZ,
                                sampleCount.matched, sampleCount.available, confidence);
                        if (confidence >= 0.98D) return best;
                    }
                }
            }
        }
        return best;
    }

    private SampleMatchCount countMatches(World world, int baseX, int baseY, int baseZ,
                                          List<PuzzleRoomDatabase.BlockSample> samples) {
        int matched = 0;
        int available = 0;
        for (PuzzleRoomDatabase.BlockSample sample : samples) {
            BlockPos pos = new BlockPos(baseX + sample.x, baseY + sample.y, baseZ + sample.z);
            // A sample outside the currently loaded chunks is unknown, not evidence that the room differs.
            if (!world.isBlockLoaded(pos)) continue;
            available++;
            if (matches(world, pos, sample)) matched++;
        }
        return new SampleMatchCount(matched, available);
    }

    private static final class SampleMatchCount {
        final int matched;
        final int available;

        SampleMatchCount(int matched, int available) {
            this.matched = matched;
            this.available = available;
        }
    }

    private boolean matches(World world, BlockPos pos, PuzzleRoomDatabase.BlockSample sample) {
        IBlockState state = world.getBlockState(pos);
        if (state == null || state.getBlock() != sample.block) return false;
        if (sample.meta < 0) return true;
        try { return sample.block.getMetaFromState(state) == sample.meta; }
        catch (RuntimeException ignored) { return true; }
    }

    public Match current() { return current; }
    public void reset() {
        worldRef = null; cellX = Integer.MIN_VALUE; cellZ = Integer.MIN_VALUE;
        ticksUntilCheck = 0; consecutiveMisses = 0; current = null;
    }

    public static final class Match {
        public final String name;
        public final int rotationY, originX, originY, originZ, matchedBlocks, totalBlocks;
        public final double confidence;
        private Match(String name, int rotationY, int originX, int originY, int originZ,
                      int matchedBlocks, int totalBlocks, double confidence) {
            this.name=name; this.rotationY=rotationY; this.originX=originX; this.originY=originY; this.originZ=originZ;
            this.matchedBlocks=matchedBlocks; this.totalBlocks=totalBlocks; this.confidence=confidence;
        }
    }
}
