package com.arena.pekaclient.dungeonesp;

import com.sun.management.ThreadMXBean;

import java.lang.management.ManagementFactory;

/** Headless regression and 500-entity reuse proof for timer-only Dungeon ESP association. */
public final class DungeonEspTrackerTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        classificationAcceptsStarsNotLevels();
        delayedTagAndBodyAssociateOnNextScan();
        tallAndSplitTagsAssociateOnce();
        nearestUnmatchedBodiesAndDeathsAreHandled();
        realAndHeadEndermenAreHandled();
        mockFiveHundredEntitySteadyStateReusesStorage();
        done();
    }

    private static void classificationAcceptsStarsNotLevels() {
        check("plain level tag is never selected",
                DungeonEspMatcher.categoryForTag("\u00a78[\u00a77Lv100\u00a78] \u00a7aZombie") == DungeonEspCategory.NONE);
        check("formatted yellow star is selected",
                DungeonEspMatcher.categoryForTag("\u00a76\u272e \u00a78[Lv100] Zombie") == DungeonEspCategory.STARRED);
        check("alternate star variant is selected",
                DungeonEspMatcher.categoryForTag("\u00a76\u2727 [Lv100] Zombie") == DungeonEspCategory.STARRED);
        check("miniboss needs no star during delayed tag update",
                DungeonEspMatcher.categoryForTag("\u00a7cShadow Assassin \u00a7a12M\u2764") == DungeonEspCategory.MINIBOSS);
        check("Crypt Souleater gets special starred category",
                DungeonEspMatcher.categoryForTag("\u00a76\u272f \u00a7cCrypt Souleater") == DungeonEspCategory.SPECIAL);
        check("known Enderman skull data is recognized", DungeonEspMatcher.isKnownEndermanHeadTexture(
                "e3RleHR1cmVzOntTS0lOOnt1cmw6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2E1OWJiMGE3YTMyOTY1YjNkOTBkOGVhZmE4OTlkMTgzNWY0MjQ1MDllYWRkNGU2YjcwOWFkYTUwYjljZiJ9fX0="));
    }

    private static void delayedTagAndBodyAssociateOnNextScan() {
        DungeonEspTracker tracker = new DungeonEspTracker();
        MockSource world = new MockSource(2);
        world.setTag(0, 10, 0D, 66.7D, 0D, "\u272e [Lv50] Skeleton");
        world.setUnused(1);
        tracker.scan(world, 0D, 64D, 0D, 60D, true);
        check("tag before body has no ghost", tracker.trackedCount() == 0);
        world.setLiving(1, 42, 0.2D, 64D, 0.1D, 1.8D, 100F, new Object());
        tracker.scan(world, 0D, 64D, 0D, 60D, true);
        check("body arriving later associates", tracker.trackedCount() == 1 && find(tracker, 42) != null);
    }

    private static void tallAndSplitTagsAssociateOnce() {
        DungeonEspTracker tracker = new DungeonEspTracker();
        MockSource world = new MockSource(3);
        // The star and Crypt Souleater text occupy separate stands; the tall body intersects the documented
        // tag-relative vertical window even though its origin is over 3.5 blocks below the top line.
        world.setTag(0, 10, 8D, 69D, 8D, "\u272e [Lv100]");
        world.setTag(1, 11, 8.1D, 68.4D, 8D, "\u00a7cCrypt Souleater");
        Object body = new Object();
        world.setLiving(2, 12, 8.08D, 64D, 8.02D, 4.2D, 100F, body);
        tracker.scan(world, 0D, 64D, 0D, 60D, true);
        DungeonEspTracker.TrackedMob record = find(tracker, 12);
        check("tall split tag body is selected", record != null && record.reference == body
                && record.category == DungeonEspCategory.SPECIAL);
        check("split stands consume body once", tracker.trackedCount() == 1
                && record.matchReason == DungeonEspMatchReason.SPLIT_TAG);
    }

    private static void nearestUnmatchedBodiesAndDeathsAreHandled() {
        DungeonEspTracker tracker = new DungeonEspTracker();
        MockSource world = new MockSource(4);
        world.setTag(0, 11, 3D, 66.7D, 3D, "\u272e [Lv50] Zombie");
        world.setTag(1, 12, 3.55D, 66.7D, 3D, "\u272e [Lv50] Zombie");
        Object near = new Object();
        Object second = new Object();
        world.setLiving(2, 101, 3.05D, 64D, 3.05D, 0.6D, 100F, near);
        world.setLiving(3, 102, 4.0D, 64D, 3D, 1.8D, 100F, second);
        tracker.scan(world, 0D, 64D, 0D, 60D, true);
        DungeonEspTracker.TrackedMob chosen = find(tracker, 101);
        check("closest body beneath first tag is selected", chosen != null && chosen.reference == near);
        check("second tag uses next unmatched body", tracker.trackedCount() == 2 && find(tracker, 102) != null);
        // Remove the alternative candidate too: a health-zero target must not retain a stale box when no living body
        // remains beneath the tag (a different living body would correctly take ownership instead).
        world.setUnused(3);
        world.setLiving(2, 101, 3.05D, 64D, 3.05D, 0.6D, 0F, near);
        tracker.scan(world, 0D, 64D, 0D, 60D, true);
        check("health zero removes immediately on tick scan", tracker.trackedCount() == 0);
        world.setLiving(2, 101, 3.05D, 64D, 3.05D, 0.6D, 100F, near);
        tracker.scan(world, 0D, 64D, 0D, 60D, true);
        tracker.remove(101); // render-side live guard for a post-scan despawn.
        check("render guard removes despawn without next interval", tracker.trackedCount() == 0);
    }

    private static void realAndHeadEndermenAreHandled() {
        DungeonEspTracker realTracker = new DungeonEspTracker();
        MockSource realWorld = new MockSource(2);
        realWorld.setTag(0, 20, 0D, 68.5D, 0D, "\u272f [Lv100] Enderman");
        Object realBody = new Object();
        realWorld.setEnderman(1, 21, 0D, 64D, 0D, 3D, 100F, realBody);
        realTracker.scan(realWorld, 0D, 64D, 0D, 60D, true);
        DungeonEspTracker.TrackedMob real = find(realTracker, 21);
        check("real Enderman gets own category", real != null && real.category == DungeonEspCategory.ENDERMAN
                && real.matchReason == DungeonEspMatchReason.REAL_ENDERMAN && !real.fixedArmorStandBox);

        DungeonEspTracker headTracker = new DungeonEspTracker();
        MockSource headWorld = new MockSource(1);
        Object head = new Object();
        headWorld.setEndermanHead(0, 31, 4D, 65D, 4D, "Fel head marker", head);
        headTracker.scan(headWorld, 0D, 64D, 0D, 60D, true);
        DungeonEspTracker.TrackedMob marker = find(headTracker, 31);
        check("Enderman-head armor stand gets fixed target", marker != null
                && marker.category == DungeonEspCategory.ENDERMAN && marker.fixedArmorStandBox
                && marker.matchReason == DungeonEspMatchReason.ENDERMAN_HEAD);
    }

    /** A stable mock room must reuse the same fixed arrays/map instead of manufacturing per-scan collections. */
    private static void mockFiveHundredEntitySteadyStateReusesStorage() {
        DungeonEspTracker tracker = new DungeonEspTracker();
        MockSource world = new MockSource(500);
        Object body = new Object();
        world.setTag(0, 900, 1D, 66.7D, 1D, "\u272e [Lv100] Teleporting Fels");
        world.setLiving(1, 901, 1D, 64D, 1D, 2D, 100F, body);
        for (int i = 2; i < 500; i++) world.setPlayer(i, 1000 + i, 100D + i, 70D, 100D);
        for (int i = 0; i < 40; i++) tracker.scan(world, 0D, 64D, 0D, 60D, true); // JIT warm-up.
        DungeonEspTracker.TrackedMob record = find(tracker, 901);
        long started = System.nanoTime();
        long allocated = allocatedForScans(tracker, world);
        long elapsed = System.nanoTime() - started;
        check("500 entity scan tracks special body", tracker.trackedCount() == 1 && record != null
                && record.category == DungeonEspCategory.SPECIAL);
        check("500 entity scan reuses tracked record", find(tracker, 901) == record);
        check("500 entity scan remains bounded", elapsed < 1500000000L);
        check("steady scanner allocation stays near zero", allocated < 512L);
    }

    private static long allocatedForScans(DungeonEspTracker tracker, MockSource world) {
        java.lang.management.ThreadMXBean raw = ManagementFactory.getThreadMXBean();
        if (!(raw instanceof ThreadMXBean)) return 0L;
        ThreadMXBean bean = (ThreadMXBean) raw;
        if (!bean.isThreadAllocatedMemorySupported()) return 0L;
        if (!bean.isThreadAllocatedMemoryEnabled()) bean.setThreadAllocatedMemoryEnabled(true);
        long id = Thread.currentThread().getId();
        long before = bean.getThreadAllocatedBytes(id);
        for (int i = 0; i < 120; i++) tracker.scan(world, 0D, 64D, 0D, 60D, true);
        return bean.getThreadAllocatedBytes(id) - before;
    }

    private static DungeonEspTracker.TrackedMob find(DungeonEspTracker tracker, int id) {
        for (int i = 0; i < tracker.slotCapacity(); i++) {
            if (tracker.isActiveSlot(i)) {
                DungeonEspTracker.TrackedMob mob = tracker.atSlot(i);
                if (mob.entityId == id) return mob;
            }
        }
        return null;
    }

    private static final class MockSource implements DungeonEspTracker.EntitySource {
        private final Mock[] entities;

        MockSource(int count) {
            entities = new Mock[count];
            for (int i = 0; i < entities.length; i++) entities[i] = new Mock();
        }

        void setUnused(int index) { entities[index].clear(); }
        void setTag(int index, int id, double x, double y, double z, String name) {
            Mock m = entities[index];
            m.clear();
            m.id = id;
            m.armorStand = true;
            m.x = x;
            m.y = y;
            m.z = z;
            m.name = name;
        }
        void setEndermanHead(int index, int id, double x, double y, double z, String name, Object ref) {
            setTag(index, id, x, y, z, name);
            Mock m = entities[index];
            m.endermanHead = true;
            m.reference = ref;
        }
        void setLiving(int index, int id, double x, double y, double z, double height, float health, Object ref) {
            Mock m = entities[index];
            m.clear();
            m.id = id;
            m.living = true;
            m.x = x;
            m.y = y;
            m.z = z;
            m.height = height;
            m.health = health;
            m.reference = ref;
        }
        void setEnderman(int index, int id, double x, double y, double z, double height, float health, Object ref) {
            setLiving(index, id, x, y, z, height, health, ref);
            entities[index].enderman = true;
        }
        void setPlayer(int index, int id, double x, double y, double z) {
            Mock m = entities[index];
            m.clear();
            m.id = id;
            m.living = true;
            m.player = true;
            m.x = x;
            m.y = y;
            m.z = z;
        }
        @Override public int size() { return entities.length; }
        @Override public void read(int index, DungeonEspTracker.MutableEntity into) {
            Mock m = entities[index];
            into.id = m.id;
            into.x = m.x;
            into.y = m.y;
            into.z = m.z;
            into.height = m.height;
            into.health = m.health;
            into.armorStand = m.armorStand;
            into.living = m.living;
            into.player = m.player;
            into.dead = m.dead;
            into.enderman = m.enderman;
            into.endermanHead = m.endermanHead;
            into.headTexture = m.headTexture;
            into.name = m.name;
            into.reference = m.reference;
        }
    }

    private static final class Mock {
        int id;
        double x, y, z, height = 1.8D;
        float health = Float.NaN;
        boolean armorStand, living, player, dead, enderman, endermanHead;
        String headTexture, name;
        Object reference;
        void clear() {
            id = 0;
            x = y = z = 0D;
            height = 1.8D;
            health = Float.NaN;
            armorStand = living = player = dead = enderman = endermanHead = false;
            headTexture = null;
            name = null;
            reference = null;
        }
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++; else { failed++; System.out.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("dungeon ESP tracker: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
