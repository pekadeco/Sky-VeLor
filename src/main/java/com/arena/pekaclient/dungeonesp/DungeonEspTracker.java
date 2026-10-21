package com.arena.pekaclient.dungeonesp;

/**
 * Fixed-storage tick scanner for dungeon ESP. It owns preallocated candidate/tag arrays and an int-key map so a
 * stable room produces no scanner-side collection, iterator, boxing, or lambda allocation. The Minecraft adapter
 * fills one {@link MutableEntity} repeatedly; tests can supply a mock world.
 */
public final class DungeonEspTracker {

    public static final int MAX_TRACKED = 256;
    private static final int MAP_CAPACITY = 512; // power of two, allowing open-addressed entity-id lookup.
    private static final int MAX_CANDIDATES = 512;
    private static final int MAX_TAGS = 256;
    private static final double TAG_HORIZONTAL_SQ = 2.25D; // about 1.5 blocks.
    private static final double TAG_BODY_BELOW = 3.5D;
    private static final double TAG_BODY_ABOVE = 0.5D;
    private static final double TAG_GROUP_VERTICAL = 3.5D;

    public interface EntitySource {
        int size();
        void read(int index, MutableEntity into);
    }

    /** Reused transport object. Sources must populate fields for every {@link EntitySource#read(int, MutableEntity)}. */
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
        /** True only for a real EntityEnderman body. */
        public boolean enderman;
        /** True for an armor stand carrying the recognized Dungeon Enderman-head marker. */
        public boolean endermanHead;
        /** Raw player-head texture property retained only for command diagnostics. */
        public String headTexture;
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
            enderman = false;
            endermanHead = false;
            headTexture = null;
            name = null;
            reference = null;
        }
    }

    /** Immutable-by-convention readout overwritten only on an accepted tick scan. */
    public static final class TrackedMob {
        public int entityId;
        public DungeonEspCategory category = DungeonEspCategory.NONE;
        public DungeonEspMatchReason matchReason = DungeonEspMatchReason.NONE;
        public Object reference;
        public double lastX;
        public double lastY;
        public double lastZ;
        public float lastHealth;
        public boolean dead;
        /** Head-marker stands use a fixed readable target box rather than their tiny model hitbox. */
        public boolean fixedArmorStandBox;
        public int seenGeneration;

        private void set(int id, DungeonEspCategory newCategory, DungeonEspMatchReason newReason,
                         Candidate candidate, int newGeneration) {
            entityId = id;
            category = newCategory;
            matchReason = newReason;
            reference = candidate.reference;
            lastX = candidate.x;
            lastY = candidate.y;
            lastZ = candidate.z;
            lastHealth = candidate.health;
            dead = candidate.dead;
            fixedArmorStandBox = candidate.directEndermanMarker;
            seenGeneration = newGeneration;
        }

        private void clear() {
            reference = null;
            category = DungeonEspCategory.NONE;
            matchReason = DungeonEspMatchReason.NONE;
            fixedArmorStandBox = false;
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
        boolean enderman;
        boolean directEndermanMarker;
        boolean matched;
        Object reference;

        void set(MutableEntity entity, boolean directMarker) {
            id = entity.id;
            x = entity.x;
            y = entity.y;
            z = entity.z;
            height = entity.height;
            health = entity.health;
            dead = entity.dead;
            enderman = entity.enderman;
            directEndermanMarker = directMarker;
            matched = false;
            reference = entity.reference;
        }
    }

    private static final class Tag {
        int id;
        double x;
        double y;
        double z;
        boolean hasStar;
        DungeonEspCategory namedCategory;
        DungeonEspCategory category;
        boolean split;

        void set(MutableEntity entity) {
            id = entity.id;
            x = entity.x;
            y = entity.y;
            z = entity.z;
            hasStar = DungeonEspMatcher.hasStar(entity.name);
            namedCategory = DungeonEspMatcher.categoryForMobName(entity.name);
            category = DungeonEspMatcher.categoryForTag(entity.name);
            split = false;
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
     * Scans one loaded-entity snapshot. A normal target must be a non-player, non-armor-stand living body near a
     * relevant armor-stand tag; each body is consumed at most once. An unambiguous Enderman-head armor stand is a
     * direct marker target and deliberately receives a fixed render box. Existing entries are evicted if their body,
     * marker, or tag relation is absent on the next configured scan.
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
            if (distanceSq(scratch.x, scratch.y, scratch.z, playerX, playerY, playerZ) > maxDistanceSq) continue;
            if (scratch.armorStand) {
                if (scratch.dead) continue;
                boolean headMarker = scratch.endermanHead || (scratch.headTexture != null
                        && DungeonEspMatcher.isEndermanMarkerName(scratch.name));
                if (headMarker) addCandidate(scratch, true);
                if (DungeonEspMatcher.isTagCandidate(scratch.name)) addTag(scratch);
                continue;
            }
            if (!scratch.living || scratch.player || scratch.dead
                    || (hideZeroHealth && !Float.isNaN(scratch.health) && scratch.health <= 0F)) {
                continue;
            }
            addCandidate(scratch, false);
        }

        prepareSplitTagCategories();
        for (int tagIndex = 0; tagIndex < tagCount; tagIndex++) {
            Tag tag = tags[tagIndex];
            if (tag.category == DungeonEspCategory.NONE) continue;
            Candidate best = bestBodyFor(tag);
            if (best == null) continue;
            best.matched = true;
            DungeonEspCategory category = best.enderman ? DungeonEspCategory.ENDERMAN : tag.category;
            DungeonEspMatchReason reason = best.enderman ? DungeonEspMatchReason.REAL_ENDERMAN
                    : (tag.split ? DungeonEspMatchReason.SPLIT_TAG : DungeonEspMatchReason.STAR_TAG);
            upsert(best.id, category, reason, best);
        }
        // Known Enderman-head armor stands represent their own entity target and need no nearby living body/tag.
        for (int candidateIndex = 0; candidateIndex < candidateCount; candidateIndex++) {
            Candidate candidate = candidates[candidateIndex];
            if (candidate.directEndermanMarker) {
                upsert(candidate.id, DungeonEspCategory.ENDERMAN, DungeonEspMatchReason.ENDERMAN_HEAD, candidate);
            }
        }
        removeUnseen();
    }

    /** Clears all cached relationships immediately when leaving a dungeon or disabling the master switch. */
    public void clear() {
        for (int i = 0; i < states.length; i++) {
            states[i] = 0;
            values[i].clear();
        }
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
            values[slot].clear();
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

    /** Command-safe lookup of the retained reason; returns NONE for an untracked entity. */
    public DungeonEspMatchReason matchReasonFor(int entityId) {
        int slot = findSlot(entityId);
        return slot >= 0 && states[slot] == 1 ? values[slot].matchReason : DungeonEspMatchReason.NONE;
    }

    /** Command-safe lookup of the selected renderer category; returns NONE for an untracked entity. */
    public DungeonEspCategory categoryFor(int entityId) {
        int slot = findSlot(entityId);
        return slot >= 0 && states[slot] == 1 ? values[slot].category : DungeonEspCategory.NONE;
    }

    private void addCandidate(MutableEntity entity, boolean directMarker) {
        if (candidateCount < candidates.length) {
            candidates[candidateCount++].set(entity, directMarker);
        } else {
            droppedCandidates++;
        }
    }

    private void addTag(MutableEntity entity) {
        if (tagCount < tags.length) {
            tags[tagCount++].set(entity);
        } else {
            droppedCandidates++;
        }
    }

    /** Merges neighboring star/name armor stands without building strings or temporary tag collections. */
    private void prepareSplitTagCategories() {
        for (int i = 0; i < tagCount; i++) {
            Tag tag = tags[i];
            boolean groupStar = tag.hasStar;
            DungeonEspCategory named = tag.namedCategory;
            boolean split = false;
            for (int otherIndex = 0; otherIndex < tagCount; otherIndex++) {
                if (i == otherIndex) continue;
                Tag other = tags[otherIndex];
                if (!relatedTags(tag, other)) continue;
                if (other.hasStar && !tag.hasStar) {
                    groupStar = true;
                    split = true;
                }
                if (other.namedCategory != DungeonEspCategory.NONE) {
                    if (named != other.namedCategory) split = true;
                    named = strongerCategory(named, other.namedCategory);
                }
            }
            if (named == DungeonEspCategory.MINIBOSS) {
                tag.category = DungeonEspCategory.MINIBOSS;
            } else if (groupStar) {
                tag.category = named == DungeonEspCategory.NONE ? DungeonEspCategory.STARRED : named;
            } else {
                tag.category = DungeonEspCategory.NONE;
            }
            tag.split = split;
        }
    }

    private Candidate bestBodyFor(Tag tag) {
        Candidate best = null;
        double bestHorizontal = Double.MAX_VALUE;
        double bestVertical = Double.MAX_VALUE;
        for (int i = 0; i < candidateCount; i++) {
            Candidate candidate = candidates[i];
            if (candidate.matched || candidate.directEndermanMarker) continue;
            double dx = candidate.x - tag.x;
            double dz = candidate.z - tag.z;
            double horizontalSq = dx * dx + dz * dz;
            if (horizontalSq > TAG_HORIZONTAL_SQ) continue;
            double minBodyY = candidate.y;
            double maxBodyY = candidate.y + Math.max(0.1D, candidate.height);
            double minAllowedY = tag.y - TAG_BODY_BELOW;
            double maxAllowedY = tag.y + TAG_BODY_ABOVE;
            // A body box must intersect the documented tag-relative vertical window. This handles tall mobs without
            // letting a distant floor entity steal the tag.
            if (maxBodyY < minAllowedY || minBodyY > maxAllowedY) continue;
            double vertical = distanceToRange(tag.y, minBodyY, maxBodyY);
            if (horizontalSq < bestHorizontal
                    || (horizontalSq == bestHorizontal && vertical < bestVertical)) {
                best = candidate;
                bestHorizontal = horizontalSq;
                bestVertical = vertical;
            }
        }
        return best;
    }

    private void upsert(int entityId, DungeonEspCategory category, DungeonEspMatchReason reason, Candidate candidate) {
        int slot = findOrCreateSlot(entityId);
        if (slot < 0 || (states[slot] != 1 && trackedCount >= MAX_TRACKED)) {
            droppedCandidates++;
            return;
        }
        if (states[slot] != 1) {
            states[slot] = 1;
            keys[slot] = entityId;
            trackedCount++;
        }
        values[slot].set(entityId, category, reason, candidate, generation);
    }

    private void removeUnseen() {
        for (int i = 0; i < states.length; i++) {
            if (states[i] == 1 && values[i].seenGeneration != generation) {
                states[i] = 2;
                values[i].clear();
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

    private static boolean relatedTags(Tag first, Tag second) {
        double dx = first.x - second.x;
        double dz = first.z - second.z;
        return dx * dx + dz * dz <= TAG_HORIZONTAL_SQ && Math.abs(first.y - second.y) <= TAG_GROUP_VERTICAL;
    }

    private static DungeonEspCategory strongerCategory(DungeonEspCategory first, DungeonEspCategory second) {
        if (first == DungeonEspCategory.MINIBOSS || second == DungeonEspCategory.MINIBOSS) return DungeonEspCategory.MINIBOSS;
        if (first == DungeonEspCategory.SPECIAL || second == DungeonEspCategory.SPECIAL) return DungeonEspCategory.SPECIAL;
        return first != DungeonEspCategory.NONE ? first : second;
    }

    private static double distanceToRange(double value, double minimum, double maximum) {
        return value < minimum ? minimum - value : (value > maximum ? value - maximum : 0D);
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
