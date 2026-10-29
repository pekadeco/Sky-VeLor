package com.arena.pekaclient.gui;

import com.arena.pekaclient.hud.HudElement;
import com.arena.pekaclient.hud.HudManager;

/** Reachability contract for the dedicated, separate Dungeon HUD editing surface. */
public final class DungeonHudEditorContractTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        dedicatedRouteUsesTheOneRowEditorPattern();
        pickerOwnsOnlyDungeonSpecificHudTargets();
        dungeonControlsExposeSafeDefaultsAndPlacementRoute();
        dedicatedMapOptionsAreNotDuplicatedInDungeonsCatalog();
        responsiveEditorLayoutKeepsSurfacesSeparate();
        done();
    }

    private static void dedicatedRouteUsesTheOneRowEditorPattern() {
        ReferenceSettingsCatalog.Setting row = ReferenceSettingsCatalog.find("dungeon-map");
        check("Dungeon Map keeps its master toggle with an explicit Dungeon HUD Editor route", row != null && row.isEditor()
                && row.hasMasterToggle() && "open-dungeon-hud-editor".equals(row.editorActionId)
                && "Open Dungeon HUD Editor".equals(row.actionLabel));
        check("dedicated editor class is available", DungeonHudEditorScreen.class != null);
    }

    private static void pickerOwnsOnlyDungeonSpecificHudTargets() {
        boolean score = false;
        boolean alert = false;
        for (HudElement element : HudManager.DUNGEON_EDITOR_ELEMENTS) {
            if (element == HudManager.DUNGEON_SCORE) score = true;
            if (element == HudManager.ROOM_CLEAR_ALERT) alert = true;
        }
        check("Dungeon picker has Score/Stats and Room Alert targets", score && alert
                && HudManager.DUNGEON_EDITOR_ELEMENTS.length == 2);
    }

    private static void dungeonControlsExposeSafeDefaultsAndPlacementRoute() {
        ReferenceSettingsCatalog.Setting score = ReferenceSettingsCatalog.find("dungeon-score-hud");
        ReferenceSettingsCatalog.Setting alert = ReferenceSettingsCatalog.find("room-clear-alert");
        ReferenceSettingsCatalog.Setting close = ReferenceSettingsCatalog.find("dungeon-auto-chest-close");
        check("score HUD exposes independently configurable score, crypts, deaths, and run time lines",
                score != null && score.children.size() == 4 && ReferenceSettingsCatalog.find("dungeon-score-line") != null
                        && ReferenceSettingsCatalog.find("dungeon-crypts-line") != null
                        && ReferenceSettingsCatalog.find("dungeon-deaths-line") != null
                        && ReferenceSettingsCatalog.find("dungeon-run-time-line") != null);
        check("Room Alert exposes duration, optional sound, and an out-of-dungeon Test Alert", alert != null
                && alert.children.size() == 3 && ReferenceSettingsCatalog.find("room-clear-duration") != null
                && ReferenceSettingsCatalog.find("room-clear-sound") != null
                && ReferenceSettingsCatalog.find("room-clear-test") != null);
        check("Auto Chest Close text makes its default-off exact-title safety clear", close != null
                && close.description.toLowerCase(java.util.Locale.ROOT).contains("off by default")
                && close.description.contains("Chest"));
    }

    private static void dedicatedMapOptionsAreNotDuplicatedInDungeonsCatalog() {
        ReferenceSettingsCatalog.Setting map = ReferenceSettingsCatalog.find("dungeon-map");
        check("Dungeons catalog keeps only the map toggle plus its editor opener", map != null
                && map.isEditor() && map.hasMasterToggle() && map.children.isEmpty()
                && "open-dungeon-hud-editor".equals(map.editorActionId));
        String editor = classText(DungeonHudEditorScreen.class);
        check("Dungeon Map element owns zoom, heads, room names, secrets, background, opacity, colors and reset actions",
                editor.contains("map-zoom") && editor.contains("map-heads") && editor.contains("map-head-size")
                        && editor.contains("map-room-names") && editor.contains("map-secrets")
                        && editor.contains("map-background") && editor.contains("map-background-opacity")
                        && editor.contains("map-background-color") && editor.contains("map-room-color")
                        && editor.contains("map-secrets-color") && editor.contains("reset-map-position"));
    }

    private static void responsiveEditorLayoutKeepsSurfacesSeparate() {
        DungeonHudEditorLayout desktop = DungeonHudEditorLayout.create(960, 540);
        DungeonHudEditorLayout pojav = DungeonHudEditorLayout.create(320, 240);
        check("wide Dungeon HUD editor uses separated solid side panels", !desktop.compact && desktop.validFor(960, 540));
        check("Pojav-sized Dungeon HUD editor moves controls above and below the canvas without overlap",
                pojav.compact && pojav.validFor(320, 240));
    }

    private static String classText(Class<?> type) {
        try {
            java.io.InputStream stream = type.getResourceAsStream('/' + type.getName().replace('.', '/') + ".class");
            if (stream == null) return "";
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[2048];
            for (int n; (n = stream.read(buffer)) >= 0;) out.write(buffer, 0, n);
            return new String(out.toByteArray(), "ISO-8859-1");
        } catch (Exception ignored) {
            return "";
        }
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++;
        else { failed++; System.out.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("dungeon HUD editor contract: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
