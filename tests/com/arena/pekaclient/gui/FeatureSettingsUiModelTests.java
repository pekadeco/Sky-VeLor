package com.arena.pekaclient.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/** Headless proof for the restored single-menu expandable Hub and retained editor reachability. */
public final class FeatureSettingsUiModelTests {
    private static int passed, failed;

    public static void main(String[] args) {
        preservesOldConfigurationAndEditorState();
        categoryExpansionUsesHistoricalDirectChildren();
        oneLevelExpandableMenuHasDirectRows();
        everyOldSurfaceHasOneRoute();
        sectionNamesAndDungeonOrderRemainExact();
        everyDungeonEspControlStillHasARoute();
        editorRoutesTargetTheRightOwners();
        mobCategoriesKeepTogglesAndSharedThumbnails();
        rowsNeverOverlapAtGuiScales();
        done();
    }

    private static void preservesOldConfigurationAndEditorState() {
        Properties p = new Properties();
        p.setProperty("dungeonEsp.enabled", "true");
        p.setProperty("ui.feature.tree.dungeon.mob-esp", "false"); // accepted from an older presentation and ignored by the Hub
        p.setProperty("ui.feature.editor.mob-esp", "enderman");
        FeatureUiState restored = new FeatureUiState();
        restored.load(p);
        check("old editor selection loads", "enderman".equals(restored.selection("mob-esp", "global")));
        restored.save(p);
        check("old feature schema remains untouched", "true".equals(p.getProperty("dungeonEsp.enabled")));
        check("old presentation key does not prevent settings loading", "false".equals(p.getProperty("ui.feature.tree.dungeon.mob-esp")));
    }

    private static void categoryExpansionUsesHistoricalDirectChildren() {
        List<Row> rows = new ArrayList<Row>();
        Row quality = Row.header("Quality of Life");
        Row particles = Row.item("No Block Particles", null, null);
        Row animation = Row.item("Item Animation", null, null);
        Row hud = Row.header("HUD Hub Editor");
        Row editor = Row.item("HUD Editor", null, null);
        rows.add(quality);
        rows.add(particles);
        rows.add(animation);
        rows.add(hud);
        rows.add(editor);

        Set<String> expanded = new HashSet<String>();
        AppleHubScreen.applyGroupVisibility(rows, expanded);
        check("fresh Quality of Life category is the intended default", "Quality of Life".equals(AppleHubScreen.defaultExpandedSection()));
        check("closed header keeps only direct category headers visible", quality.visible && hud.visible
                && !particles.visible && !animation.visible && !editor.visible);
        check("header counts only direct rows", quality.children == 2 && hud.children == 1);

        expanded.add("Quality of Life");
        AppleHubScreen.applyGroupVisibility(rows, expanded);
        check("opening Quality of Life reveals its controls and Item Animation", particles.visible && animation.visible
                && !editor.visible && "Quality of Life".equals(animation.group));
        expanded.add("HUD Hub Editor");
        AppleHubScreen.applyGroupVisibility(rows, expanded);
        check("opening another category preserves independent expansion", particles.visible && animation.visible && editor.visible
                && "HUD Hub Editor".equals(editor.group));
    }

    private static void oneLevelExpandableMenuHasDirectRows() {
        check("main menu expands exactly one category level", AppleHubScreen.mainExpansionDepth() == 1);
        check("features create no nested dropdown depth", FeatureSettingsUiModel.maximumDropdownDepth() == 0);
        for (FeatureSettingsUiModel.Feature feature : FeatureSettingsUiModel.features()) {
            check("row has exactly one direct control: " + feature.id, feature.tabControls.size() == 1);
            String control = feature.tabControls.get(0);
            if (feature.isSimple()) {
                check("simple row is an existing toggle or slider: " + feature.id,
                        FeatureSettingsUiModel.TOGGLE.equals(control) || FeatureSettingsUiModel.SLIDER.equals(control));
                check("simple row has no second editor surface: " + feature.id,
                        feature.editorId.length() == 0 && feature.editorButtonLabel.length() == 0);
            } else {
                check("focused editor route uses one Open button: " + feature.id,
                        feature.isEditorRoute() && FeatureSettingsUiModel.EDITOR_BUTTON.equals(control)
                                && feature.editorId.length() > 0 && "Open".equals(feature.editorButtonLabel));
            }
        }
    }

    private static void everyOldSurfaceHasOneRoute() {
        check("option inventory is complete", FeatureSettingsUiModel.options().size() > 90);
        check("all options have a reachable route", FeatureSettingsUiModel.isReachabilityValid());
        check("FPS has no separate display route", FeatureSettingsUiModel.feature("fps-display") == null);
        check("Ping has no separate display route", FeatureSettingsUiModel.feature("ping-display") == null);
        check("Memory has no separate display route", FeatureSettingsUiModel.feature("memory-display") == null);
        check("Hotbar customization is not a HUD row", FeatureSettingsUiModel.feature("hud-hotbar") == null);
        check("Item Protection is not a Quality of Life row", FeatureSettingsUiModel.feature("item-protection") == null);
        check("No Block Particles is a simple row", FeatureSettingsUiModel.feature("no-block-particles").isSimple());
        check("Hurt Camera Shake is a direct slider", FeatureSettingsUiModel.feature("hurt-camera-shake").isSlider());
        check("binary No Hurt Cam has no feature route", FeatureSettingsUiModel.feature("no-hurt-cam") == null);
        check("Inventory Search has no option route", FeatureSettingsUiModel.option("inventory-search") == null);
        check("Inventory Assist has one direct editor route", FeatureSettingsUiModel.feature("inventory-assist").isSettingsRoute());
        check("Stats Display is an HUD Hub Editor route", isEditorRoute("hud-stats", "hud"));
        check("HUD Editor remains separately named and routed", isEditorRoute("hud-editor", "hud"));
        check("Item Animation is a Quality of Life editor route", isEditorRoute("item-animation", "item-animation")
                && "Quality of Life".equals(FeatureSettingsUiModel.feature("item-animation").section));
        check("Auto Sprint is a simple row", FeatureSettingsUiModel.feature("auto-sprint").isSimple());
        check("renamed direct labels are plain", "No Hotbar Scrolling".equals(
                FeatureSettingsUiModel.feature("disable-hotbar-scrolling").title)
                && "Dungeon Sync".equals(FeatureSettingsUiModel.feature("dungeon-sync-enabled").title)
                && "Sync Details".equals(FeatureSettingsUiModel.feature("dungeon-sync-debug").title));
        check("Auto Sprint owns only its simple toggle", ownsOnlySimpleToggle("auto-sprint", "auto-sprint"));
        check("Hurt Camera Shake owns only its direct slider", ownsOnlySlider("hurt-camera-shake", "hurt-camera-shake"));
        check("Sync Details is a simple row", FeatureSettingsUiModel.feature("dungeon-sync-debug").isSimple());
    }

    private static void sectionNamesAndDungeonOrderRemainExact() {
        String[] sections = {"Quality of Life", "HUD Hub Editor", "General", "Farming", "Dungeon", "Slayer", "Fishing"};
        check("Hub uses one expandable section list", Arrays.equals(AppleHubScreen.sectionNames(), sections));
        for (FeatureSettingsUiModel.Feature feature : FeatureSettingsUiModel.features()) {
            check("feature uses an existing section: " + feature.id, Arrays.asList(sections).contains(feature.section));
        }
        for (AssistUiModel.Entry entry : AssistUiModel.entries()) {
            check("assist catalog uses a current section: " + entry.name,
                    Arrays.asList(sections).contains(entry.section));
        }
        List<String> actual = new ArrayList<String>();
        for (FeatureSettingsUiModel.Feature feature : FeatureSettingsUiModel.features()) {
            if ("Dungeon".equals(feature.section)) actual.add(feature.id);
        }
        List<String> expected = Arrays.asList("door-esp", "mob-esp", "dungeon-sync-enabled", "m7-overlay",
                "spirit-leap", "terminal-assist", "dungeon-boss-hud", "dungeon-map", "dungeon-sync-debug");
        check("Dungeon uses ESP, helpers, then information order", expected.equals(actual));
    }

    private static boolean isEditorRoute(String featureId, String editorId) {
        FeatureSettingsUiModel.Feature feature = FeatureSettingsUiModel.feature(featureId);
        return feature != null && feature.isEditorRoute() && editorId.equals(feature.editorId);
    }

    private static boolean ownsOnlySimpleToggle(String featureId, String optionId) {
        FeatureSettingsUiModel.Feature feature = FeatureSettingsUiModel.feature(featureId);
        return feature != null && feature.isSimple() && feature.tabControls.equals(
                java.util.Collections.singletonList(FeatureSettingsUiModel.TOGGLE)) && optionId.equals(feature.toggleOptionId)
                && feature.optionIds.size() == 1 && optionId.equals(feature.optionIds.get(0))
                && FeatureSettingsUiModel.option(optionId) != null;
    }

    private static boolean ownsOnlySlider(String featureId, String optionId) {
        FeatureSettingsUiModel.Feature feature = FeatureSettingsUiModel.feature(featureId);
        return feature != null && feature.isSlider() && optionId.equals(feature.toggleOptionId)
                && feature.optionIds.size() == 1 && optionId.equals(feature.optionIds.get(0))
                && FeatureSettingsUiModel.option(optionId) != null;
    }

    private static void everyDungeonEspControlStillHasARoute() {
        for (DungeonEspUiModel.Option option : DungeonEspUiModel.options()) {
            check("Dungeon ESP route: " + option.name, FeatureSettingsUiModel.hasOptionLabel(option.name));
        }
    }

    private static void editorRoutesTargetTheRightOwners() {
        check("Mob ESP button is Open", "Open".equals(FeatureSettingsUiModel.feature("mob-esp").editorButtonLabel));
        check("Door ESP button is Open", "Open".equals(FeatureSettingsUiModel.feature("door-esp").editorButtonLabel));
        check("Item Lock keeps its editor", isEditorRoute("item-lock-swap", "item-lock-swap"));
        check("Chat keeps Chat settings route", isEditorRoute("chat-qol", "chat"));
        check("Item Animation keeps its existing editor", isEditorRoute("item-animation", "item-animation"));
        check("M7 keeps its focused editor", isEditorRoute("m7-overlay", "m7-overlay"));
    }

    private static void mobCategoriesKeepTogglesAndSharedThumbnails() {
        FeatureSettingsEditorScreen.Spec spec = DungeonEspEditorSpecs.mobSpec();
        String[] ids = {"global", "starred", "miniboss", "special", "enderman", "skeletor", "other"};
        check("Mob ESP editor lists Global plus every category", spec.categories.size() == ids.length);
        for (String id : ids) {
            FeatureSettingsEditorScreen.Category category = category(spec, id);
            check("Mob ESP category exists: " + id, category != null);
            check("Mob ESP left-list toggle exists: " + id, category != null && category.hasToggle());
            check("Mob ESP left-list hitbox preview exists: " + id, category != null && category.hasThumbnail());
        }
    }

    private static FeatureSettingsEditorScreen.Category category(FeatureSettingsEditorScreen.Spec spec, String id) {
        for (FeatureSettingsEditorScreen.Category category : spec.categories) if (id.equals(category.id)) return category;
        return null;
    }

    private static void rowsNeverOverlapAtGuiScales() {
        float[] widths = {1f, 80f, 160f, 214f, 427f, 854f, 1708f};
        for (float width : widths) {
            GuiLayoutToolkit.Rect row = new GuiLayoutToolkit.Rect().set(0f, 0f, width, 42f);
            GuiLayoutToolkit.Rect label = new GuiLayoutToolkit.Rect();
            GuiLayoutToolkit.Rect control = new GuiLayoutToolkit.Rect();
            GuiLayoutToolkit.splitRow(row, 12f, 8f, 172f, 24f, label, control);
            check("label/control columns do not overlap at " + width, !label.intersects(control));
            check("control stays in bounds at " + width, control.right() <= row.right() && control.x >= row.x);
        }
        float top = 0f;
        float previousBottom = -1f;
        for (FeatureSettingsUiModel.Feature ignored : FeatureSettingsUiModel.features()) {
            check("direct row follows previous row", top >= previousBottom);
            previousBottom = top + 40f;
            top = AppleHubScreen.nextRowTop(top, 40f);
        }
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++; else { failed++; System.out.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("feature settings UI model: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
