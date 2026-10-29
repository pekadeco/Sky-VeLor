package com.arena.pekaclient.gui;

import com.arena.pekaclient.hud.HudElement;
import com.arena.pekaclient.hud.HudManager;

/** Reachability and direct-canvas geometry contract for the dedicated Dungeon HUD editor. */
public final class DungeonHudEditorContractTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        dedicatedRouteUsesTheOneRowEditorPattern();
        pickerOwnsOnlyDungeonSpecificHudTargets();
        dungeonControlsExposeSafeDefaultsAndPlacementRoute();
        directCanvasEditorOwnsGroupedMapOptions();
        responsiveEditorLayoutAvoidsTheSelectedElement();
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
        check("Dungeon editor owns Score/Stats and Room Alert targets", score && alert
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

    private static void directCanvasEditorOwnsGroupedMapOptions() {
        String editor = classText(DungeonHudEditorScreen.class);
        check("direct editor keeps map zoom, heads, labels, secrets, background, colors and reset support",
                editor.contains("map-zoom") && editor.contains("map-heads") && editor.contains("map-head-size")
                        && editor.contains("map-room-names") && editor.contains("map-secrets")
                        && editor.contains("map-background") && editor.contains("map-background-opacity")
                        && editor.contains("map-background-color") && editor.contains("map-room-color")
                        && editor.contains("map-secrets-color") && editor.contains("resetMap"));
        check("map controls are grouped and use existing compact toggle and slider widgets",
                editor.contains("heading(\"Display\")") && editor.contains("heading(\"Players\")")
                        && editor.contains("heading(\"Colors\")") && editor.contains("heading(\"Background\")")
                        && editor.contains("AppleToggle") && editor.contains("AppleSlider"));
        check("direct editor keeps direct element selection rather than restoring a large background panel",
                editor.contains("drawSelection") && !editor.contains("drawDefaultBackground"));
    }

    private static void responsiveEditorLayoutAvoidsTheSelectedElement() {
        GuiLayoutToolkit.Rect selected = new GuiLayoutToolkit.Rect().set(10F, 80F, 120F, 72F);
        DungeonHudEditorLayout desktop = DungeonHudEditorLayout.create(960, 540);
        desktop.dockInspector(selected, 220F, true);
        check("wide direct editor keeps a slim top strip inside the launcher-safe area",
                !desktop.compact && desktop.topBar.right() <= 882F && desktop.validFor(960, 540));
        check("wide selected panel stays at about one quarter width and avoids the selected element",
                desktop.inspectorVisible && desktop.inspector.w <= 960F * .25F + .01F
                        && desktop.inspectorAvoids(selected));

        GuiLayoutToolkit.Rect pojavSelected = new GuiLayoutToolkit.Rect().set(18F, 34F, 72F, 72F);
        DungeonHudEditorLayout pojav = DungeonHudEditorLayout.create(320, 240);
        pojav.dockInspector(pojavSelected, 110F, true);
        check("Pojav-sized editor uses a shallow touchable dock without covering the selected element",
                pojav.compact && pojav.validFor(320, 240) && pojav.inspectorVisible
                        && pojav.inspectorAvoids(pojavSelected));
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
