package com.arena.pekaclient.config;

import java.util.Properties;

/** Persistence, migration, bounds and preset proof for the isolated dungeonEsp.* schema. */
public final class DungeonEspSettingsTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        defaultsAreUsable();
        legacyMobToggleMigrates();
        saveLoadRoundTripAndBounds();
        presetsRemainDocumented();
        done();
    }

    private static void defaultsAreUsable() {
        DungeonEspSettings.load(new Properties());
        check("master defaults on", DungeonEspSettings.enabled);
        check("five tick update default", DungeonEspSettings.updateIntervalTicks == 5);
        check("distance and box defaults", DungeonEspSettings.maxDistance == 60 && DungeonEspSettings.maxBoxes == 64);
        check("starred miniboss special categories on", DungeonEspSettings.starred.enabled
                && DungeonEspSettings.miniboss.enabled && DungeonEspSettings.special.enabled);
        check("new mob categories default safely", DungeonEspSettings.enderman.enabled
                && DungeonEspSettings.skeletor.enabled && DungeonEspSettings.otherMobs.enabled
                && !DungeonEspSettings.forceRenderAllMobs && !DungeonEspSettings.includeBats
                && !DungeonEspSettings.includeInvisibleMobs);
        check("Skeletor begins with starred style", DungeonEspSettings.skeletor.color == DungeonEspSettings.starred.color
                && DungeonEspSettings.skeletor.mode == DungeonEspSettings.starred.mode
                && DungeonEspSettings.skeletor.outlineWidth == DungeonEspSettings.starred.outlineWidth);
        check("door cache defaults on", DungeonEspSettings.doorsEnabled
                && DungeonEspSettings.doorScope == DungeonEspSettings.DOORS_NEAREST);
    }

    private static void legacyMobToggleMigrates() {
        Properties legacy = new Properties();
        legacy.setProperty("mobHighlights", "false");
        DungeonEspSettings.load(legacy);
        check("old off toggle becomes ESP master", !DungeonEspSettings.enabled);
        DungeonEspSettings.save(legacy);
        check("save retires replaced key", !legacy.containsKey("mobHighlights") && legacy.containsKey("dungeonEsp.enabled"));
    }

    private static void saveLoadRoundTripAndBounds() {
        DungeonEspSettings.resetDefaults();
        DungeonEspSettings.maxDistance = 133;
        DungeonEspSettings.updateIntervalTicks = 7;
        DungeonEspSettings.starred.color = 0xA6E22E;
        DungeonEspSettings.starred.mode = DungeonEspSettings.MODE_FILLED;
        DungeonEspSettings.miniboss.throughWalls = false;
        DungeonEspSettings.enderman.color = 0xFF9F0A;
        DungeonEspSettings.skeletor.color = 0xFF7EB6;
        DungeonEspSettings.skeletor.mode = DungeonEspSettings.MODE_OUTLINE;
        DungeonEspSettings.skeletor.outlineWidth = 5;
        DungeonEspSettings.otherMobs.throughWalls = false;
        DungeonEspSettings.forceRenderAllMobs = true;
        DungeonEspSettings.includeBats = true;
        DungeonEspSettings.includeInvisibleMobs = true;
        DungeonEspSettings.doors.fillOpacity = 37;
        DungeonEspSettings.doorScope = DungeonEspSettings.DOORS_ALL;
        Properties saved = new Properties();
        DungeonEspSettings.save(saved);
        check("Skeletor has its own persisted style keys", saved.containsKey("dungeonEsp.skeletor.enabled")
                && saved.containsKey("dungeonEsp.skeletor.color") && saved.containsKey("dungeonEsp.skeletor.mode")
                && saved.containsKey("dungeonEsp.skeletor.outlineWidth"));
        DungeonEspSettings.load(saved);
        check("styles round trip", DungeonEspSettings.maxDistance == 133 && DungeonEspSettings.updateIntervalTicks == 7
                && DungeonEspSettings.starred.color == 0xA6E22E && DungeonEspSettings.starred.mode == DungeonEspSettings.MODE_FILLED
                && !DungeonEspSettings.miniboss.throughWalls && DungeonEspSettings.enderman.color == 0xFF9F0A
                && DungeonEspSettings.skeletor.color == 0xFF7EB6
                && DungeonEspSettings.skeletor.mode == DungeonEspSettings.MODE_OUTLINE
                && DungeonEspSettings.skeletor.outlineWidth == 5 && !DungeonEspSettings.otherMobs.throughWalls
                && DungeonEspSettings.forceRenderAllMobs && DungeonEspSettings.includeBats
                && DungeonEspSettings.includeInvisibleMobs && DungeonEspSettings.doors.fillOpacity == 37
                && DungeonEspSettings.doorScope == DungeonEspSettings.DOORS_ALL);
        saved.setProperty("dungeonEsp.maxDistance", "9999");
        saved.setProperty("dungeonEsp.updateIntervalTicks", "0");
        saved.setProperty("dungeonEsp.starred.outlineWidth", "-10");
        saved.setProperty("dungeonEsp.doors.style.fillOpacity", "500");
        DungeonEspSettings.load(saved);
        check("hand-edited values clamp", DungeonEspSettings.maxDistance == DungeonEspSettings.MAX_DISTANCE
                && DungeonEspSettings.updateIntervalTicks == DungeonEspSettings.MIN_INTERVAL_TICKS
                && DungeonEspSettings.starred.outlineWidth == DungeonEspSettings.MIN_WIDTH
                && DungeonEspSettings.doors.fillOpacity == DungeonEspSettings.MAX_OPACITY);
    }

    private static void presetsRemainDocumented() {
        DungeonEspSettings.applySubtlePreset();
        check("subtle is depth-tested outline", DungeonEspSettings.starred.mode == DungeonEspSettings.MODE_OUTLINE
                && !DungeonEspSettings.starred.throughWalls && DungeonEspSettings.starred.fillOpacity == 0);
        DungeonEspSettings.applyDefaultPreset();
        check("default restores filled through-wall style", DungeonEspSettings.starred.mode == DungeonEspSettings.MODE_BOTH
                && DungeonEspSettings.starred.throughWalls && DungeonEspSettings.starred.fillOpacity > 0);
        check("palette lookup is bounded", DungeonEspSettings.colorForIndex(-2) == DungeonEspSettings.COLORS[0]
                && DungeonEspSettings.colorForIndex(999) == DungeonEspSettings.COLORS[DungeonEspSettings.COLORS.length - 1]);
    }

    private static void check(String name, boolean ok) { if (ok) passed++; else { failed++; System.out.println("FAIL " + name); } }
    private static void done() { System.out.println("dungeon ESP settings: passed " + passed + ", failed " + failed); if (failed > 0) System.exit(1); }
}
