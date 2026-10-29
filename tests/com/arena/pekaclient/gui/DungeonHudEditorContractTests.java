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
        done();
    }

    private static void dedicatedRouteUsesTheOneRowEditorPattern() {
        ReferenceSettingsCatalog.Setting row = ReferenceSettingsCatalog.find("dungeon-hud-editor");
        check("Dungeon HUD Editor is an explicit no-master editor row", row != null && row.isEditor()
                && !row.hasMasterToggle() && "open-dungeon-hud-editor".equals(row.editorActionId)
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

    private static void check(String name, boolean okay) {
        if (okay) passed++;
        else { failed++; System.out.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("dungeon HUD editor contract: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
