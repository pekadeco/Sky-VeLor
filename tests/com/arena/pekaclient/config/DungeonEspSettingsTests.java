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
        doorColorDefaultsMigrateOnlyFormerValues();
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
        DungeonEspSettings.otherMobs.color = 0xA6E22E;
        DungeonEspSettings.otherMobs.mode = DungeonEspSettings.MODE_FILLED;
        DungeonEspSettings.otherMobs.outlineWidth = 4;
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
        check("force and Other Mobs have persisted controls", saved.containsKey("dungeonEsp.forceRenderAllMobs")
                && saved.containsKey("dungeonEsp.includeBats") && saved.containsKey("dungeonEsp.includeInvisibleMobs")
                && saved.containsKey("dungeonEsp.otherMobs.color")
                && saved.containsKey("dungeonEsp.otherMobs.outlineWidth"));
        DungeonEspSettings.load(saved);
        check("styles round trip", DungeonEspSettings.maxDistance == 133 && DungeonEspSettings.updateIntervalTicks == 7
                && DungeonEspSettings.starred.color == 0xA6E22E && DungeonEspSettings.starred.mode == DungeonEspSettings.MODE_FILLED
                && !DungeonEspSettings.miniboss.throughWalls && DungeonEspSettings.enderman.color == 0xFF9F0A
                && DungeonEspSettings.skeletor.color == 0xFF7EB6
                && DungeonEspSettings.skeletor.mode == DungeonEspSettings.MODE_OUTLINE
                && DungeonEspSettings.skeletor.outlineWidth == 5
                && DungeonEspSettings.otherMobs.color == 0xA6E22E
                && DungeonEspSettings.otherMobs.mode == DungeonEspSettings.MODE_FILLED
                && DungeonEspSettings.otherMobs.outlineWidth == 4 && !DungeonEspSettings.otherMobs.throughWalls
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

    private static void doorColorDefaultsMigrateOnlyFormerValues() {
        DungeonEspSettings.load(new Properties());
        check("new door defaults are red and dark red", DungeonEspSettings.doors.color == DungeonEspSettings.DEFAULT_WITHER_DOOR_COLOR
                && DungeonEspSettings.bloodDoorColor == DungeonEspSettings.DEFAULT_BLOOD_DOOR_COLOR);
        check("dark red is selectable", DungeonEspSettings.colorForIndex(DungeonEspSettings.colorIndex(
                DungeonEspSettings.DEFAULT_BLOOD_DOOR_COLOR)) == DungeonEspSettings.DEFAULT_BLOOD_DOOR_COLOR);

        Properties oldDefaults = new Properties();
        oldDefaults.setProperty("dungeonEsp.doors.style.color", Integer.toString(DungeonEspSettings.FORMER_WITHER_DOOR_COLOR));
        oldDefaults.setProperty("dungeonEsp.doors.bloodColor", Integer.toString(DungeonEspSettings.FORMER_BLOOD_DOOR_COLOR));
        DungeonEspSettings.load(oldDefaults);
        check("former stored defaults migrate", DungeonEspSettings.doors.color == DungeonEspSettings.DEFAULT_WITHER_DOOR_COLOR
                && DungeonEspSettings.bloodDoorColor == DungeonEspSettings.DEFAULT_BLOOD_DOOR_COLOR);
        DungeonEspSettings.save(oldDefaults);
        check("migration persists new values", Integer.toString(DungeonEspSettings.DEFAULT_WITHER_DOOR_COLOR)
                .equals(oldDefaults.getProperty("dungeonEsp.doors.style.color"))
                && Integer.toString(DungeonEspSettings.DEFAULT_BLOOD_DOOR_COLOR)
                .equals(oldDefaults.getProperty("dungeonEsp.doors.bloodColor")));

        Properties custom = new Properties();
        custom.setProperty("dungeonEsp.doors.style.color", Integer.toString(0x0A84FF));
        custom.setProperty("dungeonEsp.doors.bloodColor", Integer.toString(0xBF5AF2));
        DungeonEspSettings.load(custom);
        check("custom door colors remain untouched", DungeonEspSettings.doors.color == 0x0A84FF
                && DungeonEspSettings.bloodDoorColor == 0xBF5AF2);
    }

    private static void check(String name, boolean ok) { if (ok) passed++; else { failed++; System.out.println("FAIL " + name); } }
    private static void done() { System.out.println("dungeon ESP settings: passed " + passed + ", failed " + failed); if (failed > 0) System.exit(1); }
}
