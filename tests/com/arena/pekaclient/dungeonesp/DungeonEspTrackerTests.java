package com.arena.pekaclient.dungeonesp;

import com.sun.management.ThreadMXBean;

import java.lang.management.ManagementFactory;

/** Headless regression and 500-entity reuse proof for the tick-only starred-mob tracker. */
public final class DungeonEspTrackerTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        classificationAcceptsStarsNotLevels();
        delayedTagAndBodyAssociateOnNextScan();
        nearestBodyAndDeathsAreHandled();
        mockFiveHundredEntitySteadyStateReusesStorage();
        done();
    }

    private static void classificationAcceptsStarsNotLevels() {
        check("plain level tag is never selected",
                DungeonEspMatcher.categoryForTag("\u00a78[\u00a77Lv100\u00a78] \u00a7aZombie") == DungeonEspCategory.NONE);
        check("formatted yellow star is selected",
                DungeonEspMatcher.categoryForTag("\u00a76\u272e \u00a78[Lv100] Zombie") == DungeonEspCategory.STARRED);
        check("miniboss needs no star during delayed tag update",
                DungeonEspMatcher.categoryForTag("\u00a7cShadow Assassin \u00a7a12M\u2764") == DungeonEspCategory.MINIBOSS);
        check("named special starred enemy gets category",
                DungeonEspMatcher.categoryForTag("\u00a76\u272f \u00a7cWithermancer") == DungeonEspCategory.SPECIAL);
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

    private static void nearestBodyAndDeathsAreHandled() {
        DungeonEspTracker tracker = new DungeonEspTracker();
        MockSource world = new MockSource(3);
        world.setTag(0, 11, 3D, 66.7D, 3D, "\u272e [Lv50] Zombie");
        Object far = new Object();
        Object near = new Object();
        world.setLiving(1, 101, 4.2D, 64D, 3D, 1.8D, 100F, far);
        world.setLiving(2, 102, 3.05D, 64D, 3.05D, 0.6D, 100F, near); // small/invisible bodies retain their true object.
        tracker.scan(world, 0D, 64D, 0D, 60D, true);
        DungeonEspTracker.TrackedMob chosen = find(tracker, 102);
        check("closest body beneath tag is selected", chosen != null && chosen.reference == near && tracker.trackedCount() == 1);
        world.setLiving(2, 102, 3.05D, 64D, 3.05D, 0.6D, 0F, near);
        tracker.scan(world, 0D, 64D, 0D, 60D, true);
        check("health zero removes immediately on tick scan", tracker.trackedCount() == 0);
        world.setLiving(2, 102, 3.05D, 64D, 3.05D, 0.6D, 100F, near);
        tracker.scan(world, 0D, 64D, 0D, 60D, true);
        tracker.remove(102); // render-side live guard for a post-scan despawn.
        check("render guard removes despawn without next interval", tracker.trackedCount() == 0);
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
            Mock m = entities[index]; m.clear(); m.id = id; m.armorStand = true; m.x = x; m.y = y; m.z = z; m.name = name;
        }
        void setLiving(int index, int id, double x, double y, double z, double height, float health, Object ref) {
            Mock m = entities[index]; m.clear(); m.id = id; m.living = true; m.x = x; m.y = y; m.z = z;
            m.height = height; m.health = health; m.reference = ref;
        }
        void setPlayer(int index, int id, double x, double y, double z) {
            Mock m = entities[index]; m.clear(); m.id = id; m.living = true; m.player = true; m.x = x; m.y = y; m.z = z;
        }
        @Override public int size() { return entities.length; }
        @Override public void read(int index, DungeonEspTracker.MutableEntity into) {
            Mock m = entities[index];
            into.id = m.id; into.x = m.x; into.y = m.y; into.z = m.z; into.height = m.height; into.health = m.health;
            into.armorStand = m.armorStand; into.living = m.living; into.player = m.player; into.dead = m.dead;
            into.name = m.name; into.reference = m.reference;
        }
    }

    private static final class Mock {
        int id; double x, y, z, height = 1.8D; float health = Float.NaN;
        boolean armorStand, living, player, dead; String name; Object reference;
        void clear() { id = 0; x = y = z = 0D; height = 1.8D; health = Float.NaN; armorStand = living = player = dead = false; name = null; reference = null; }
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++; else { failed++; System.out.println("FAIL " + name); }
    }
    private static void done() {
        System.out.println("dungeon ESP tracker: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
