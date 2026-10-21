package com.arena.pekaclient.dungeonesp;

/**
 * Fixed-storage tick scanner for yellow-star dungeon targets. It deliberately owns preallocated candidate arrays and
 * an int-key map so a stable 500-entity room produces no scanner-side collection, iterator, boxing or lambda
 * allocation. The Minecraft adapter fills one {@link MutableEntity} repeatedly; tests can supply a mock world.
 */
public final class DungeonEspTracker {

    public static final int MAX_TRACKED = 256;
    private static final int MAP_CAPACITY = 512; // power of two, allowing open-addressed entity-id lookup.
    private static final int MAX_CANDIDATES = 512;
    private static final int MAX_TAGS = 256;
    private static final double TAG_HORIZONTAL_SQ = 2.25D;

    public interface EntitySource {
        int size();
        void read(int index, MutableEntity into);
    }

    /** Reused transport object. Sources must populate fields for each call to {@link EntitySource#read(int, MutableEntity)}. */
    public static final class MutableEntity {
        public int id;
        public double x;
        public double y;
        public double z;
        public double height = 1.8D;
        public float health = Float.NaN;
        public boolean armorStand;
        public boolean living;
        public boolean player;
        public boolean dead;
        public String name;
        /** The live game entity at runtime, or a test object. It is never examined by the pure scanner. */
        public Object reference;

        public void clear() {
            id = 0;
            x = y = z = 0D;
            height = 1.8D;
            health = Float.NaN;
            armorStand = false;
            living = false;
            player = false;
            dead = false;
            name = null;
            reference = null;
        }
    }

    /** Immutable-by-convention readout whose fields are overwritten only on an accepted tick scan. */
    public static final class TrackedMob {
        public int entityId;
        public DungeonEspCategory category = DungeonEspCategory.NONE;
        public Object reference;
        public double lastX;
        public double lastY;
        public double lastZ;
        public float lastHealth;
        public boolean dead;
        public int seenGeneration;

        private void set(int id, DungeonEspCategory newCategory, Candidate candidate, int generation) {
            entityId = id;
            category = newCategory;
            reference = candidate.reference;
            lastX = candidate.x;
            lastY = candidate.y;
            lastZ = candidate.z;
            lastHealth = candidate.health;
            dead = candidate.dead;
            seenGeneration = generation;
        }
    }

    private static final class Candidate {
        int id;
        double x;
        double y;
        double z;
        double height;
        float health;
        boolean dead;
        Object reference;

        void set(MutableEntity entity) {
            id = entity.id;
            x = entity.x;
            y = entity.y;
            z = entity.z;
            height = entity.height;
            health = entity.health;
            dead = entity.dead;
            reference = entity.reference;
        }
    }

    private static final class Tag {
        double x;
        double y;
        double z;
        DungeonEspCategory category;

        void set(MutableEntity entity, DungeonEspCategory newCategory) {
            x = entity.x;
            y = entity.y;
            z = entity.z;
            category = newCategory;
        }
    }

    private final MutableEntity scratch = new MutableEntity();
    private final Candidate[] candidates = new Candidate[MAX_CANDIDATES];
    private final Tag[] tags = new Tag[MAX_TAGS];
    private final int[] keys = new int[MAP_CAPACITY];
    /** 0 never used, 1 live, 2 tombstone. Tombstones preserve probes without allocating boxed Integer keys. */
    private final byte[] states = new byte[MAP_CAPACITY];
    private final TrackedMob[] values = new TrackedMob[MAP_CAPACITY];
    private int candidateCount;
    private int tagCount;
    private int trackedCount;
    private int generation;
    private int droppedCandidates;

    public DungeonEspTracker() {
        for (int i = 0; i < candidates.length; i++) candidates[i] = new Candidate();
        for (int i = 0; i < tags.length; i++) tags[i] = new Tag();
        for (int i = 0; i < values.length; i++) values[i] = new TrackedMob();
    }

    /**
     * Scans one loaded-entity snapshot. A target must be a non-player living entity beneath a matching armor stand;
     * ordinary level tags never enter the tag array. Existing entries are evicted at the end when their tag/body pair
     * no longer exists, so despawns, tag loss, and health-zero deaths cannot linger as ghost boxes.
     */
    public void scan(EntitySource source, double playerX, double playerY, double playerZ,
                     double maxDistance, boolean hideZeroHealth) {
        candidateCount = 0;
        tagCount = 0;
        droppedCandidates = 0;
        generation++;
        if (generation <= 0) {
            generation = 1;
            for (int i = 0; i < values.length; i++) values[i].seenGeneration = 0;
        }
        if (source == null || maxDistance <= 0D) {
            removeUnseen();
            return;
        }
        double maxDistanceSq = maxDistance * maxDistance;
        int size = source.size();
        for (int index = 0; index < size; index++) {
            scratch.clear();
            source.read(index, scratch);
            if (distanceSq(scratch.x, scratch.y, scratch.z, playerX, playerY, playerZ) > maxDistanceSq) {
                continue;
            }
            if (scratch.armorStand) {
                DungeonEspCategory category = DungeonEspMatcher.categoryForTag(scratch.name);
                if (category != DungeonEspCategory.NONE && tagCount < tags.length) {
                    tags[tagCount++].set(scratch, category);
                } else if (category != DungeonEspCategory.NONE) {
                    droppedCandidates++;
                }
                continue;
            }
            if (!scratch.living || scratch.player || scratch.dead
                    || (hideZeroHealth && !Float.isNaN(scratch.health) && scratch.health <= 0F)) {
                continue;
            }
            if (candidateCount < candidates.length) {
                candidates[candidateCount++].set(scratch);
            } else {
                droppedCandidates++;
            }
        }

        for (int tagIndex = 0; tagIndex < tagCount; tagIndex++) {
            Tag tag = tags[tagIndex];
            Candidate best = bestBodyFor(tag);
            if (best != null) {
                upsert(best.id, tag.category, best);
            }
        }
        removeUnseen();
    }

    /** Clears all cached relationships immediately when leaving a dungeon or disabling the master switch. */
    public void clear() {
        for (int i = 0; i < states.length; i++) states[i] = 0;
        trackedCount = 0;
        candidateCount = 0;
        tagCount = 0;
        droppedCandidates = 0;
    }

    /** Removes a record found dead by the render-side live-entity guard without waiting for the next configured scan. */
    public void remove(int entityId) {
        int slot = findSlot(entityId);
        if (slot >= 0 && states[slot] == 1) {
            states[slot] = 2;
            values[slot].reference = null;
            values[slot].category = DungeonEspCategory.NONE;
            trackedCount--;
        }
    }

    public int trackedCount() {
        return trackedCount;
    }

    public int droppedCandidates() {
        return droppedCandidates;
    }

    public int slotCapacity() {
        return MAP_CAPACITY;
    }

    public boolean isActiveSlot(int slot) {
        return slot >= 0 && slot < MAP_CAPACITY && states[slot] == 1;
    }

    public TrackedMob atSlot(int slot) {
        return slot >= 0 && slot < MAP_CAPACITY ? values[slot] : null;
    }

    private Candidate bestBodyFor(Tag tag) {
        Candidate best = null;
        double bestScore = Double.MAX_VALUE;
        for (int i = 0; i < candidateCount; i++) {
            Candidate candidate = candidates[i];
            double dx = candidate.x - tag.x;
            double dz = candidate.z - tag.z;
            double horizontalSq = dx * dx + dz * dz;
            if (horizontalSq > TAG_HORIZONTAL_SQ) continue;
            // Name plates normally float just above a hitbox. Allow a generous window for tall, tiny and teleporting mobs.
            double vertical = tag.y - candidate.y;
            if (vertical < -1.25D || vertical > candidate.height + 3.5D) continue;
            double expected = candidate.height + 0.65D;
            double score = horizontalSq * 4D + Math.abs(vertical - expected) * 0.15D;
            if (score < bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    private void upsert(int entityId, DungeonEspCategory category, Candidate candidate) {
        int slot = findOrCreateSlot(entityId);
        if (slot < 0) {
            droppedCandidates++;
            return;
        }
        if (states[slot] != 1) {
            states[slot] = 1;
            keys[slot] = entityId;
            trackedCount++;
        }
        values[slot].set(entityId, category, candidate, generation);
    }

    private void removeUnseen() {
        for (int i = 0; i < states.length; i++) {
            if (states[i] == 1 && values[i].seenGeneration != generation) {
                states[i] = 2;
                values[i].reference = null;
                values[i].category = DungeonEspCategory.NONE;
                trackedCount--;
            }
        }
    }

    private int findSlot(int entityId) {
        int mask = MAP_CAPACITY - 1;
        int slot = mix(entityId) & mask;
        for (int probe = 0; probe < MAP_CAPACITY; probe++) {
            byte state = states[slot];
            if (state == 0) return -1;
            if (state == 1 && keys[slot] == entityId) return slot;
            slot = (slot + 1) & mask;
        }
        return -1;
    }

    private int findOrCreateSlot(int entityId) {
        int mask = MAP_CAPACITY - 1;
        int slot = mix(entityId) & mask;
        int tombstone = -1;
        for (int probe = 0; probe < MAP_CAPACITY; probe++) {
            byte state = states[slot];
            if (state == 0) return tombstone >= 0 ? tombstone : slot;
            if (state == 1 && keys[slot] == entityId) return slot;
            if (state == 2 && tombstone < 0) tombstone = slot;
            slot = (slot + 1) & mask;
        }
        return tombstone;
    }

    private static int mix(int value) {
        value ^= value >>> 16;
        value *= 0x7feb352d;
        value ^= value >>> 15;
        value *= 0x846ca68b;
        return value ^ value >>> 16;
    }

    private static double distanceSq(double x, double y, double z, double otherX, double otherY, double otherZ) {
        double dx = x - otherX;
        double dy = y - otherY;
        double dz = z - otherZ;
        return dx * dx + dy * dy + dz * dz;
    }
}
