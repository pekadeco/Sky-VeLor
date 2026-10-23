package com.arena.pekaclient.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/** Headless proof for the flat, one-level Hub contract and retained editor reachability. */
public final class FeatureSettingsUiModelTests {
    private static int passed, failed;

    public static void main(String[] args) {
        preservesOldConfigurationAndEditorState();
        flatRowsUseOnlyTheTwoAllowedForms();
        everyOldSurfaceHasOneRoute();
        dungeonsUseRequestedUsageOrder();
        everyDungeonEspControlStillHasARoute();
        editorButtonsTargetTheRightEditors();
        mobCategoriesKeepTogglesAndSharedThumbnails();
        statusDotsReflectTheirToggleState();
        rowsNeverOverlapAtGuiScales();
        done();
    }

    private static void preservesOldConfigurationAndEditorState() {
        Properties p = new Properties();
        p.setProperty("dungeonEsp.enabled", "true");
        p.setProperty("ui.feature.tree.dungeon.mob-esp", "false"); // accepted from the pre-flat layout and ignored by the Hub
        p.setProperty("ui.feature.editor.mob-esp", "enderman");
        FeatureUiState restored = new FeatureUiState();
        restored.load(p);
        check("old editor selection loads", "enderman".equals(restored.selection("mob-esp", "global")));
        restored.save(p);
        check("old feature schema remains untouched", "true".equals(p.getProperty("dungeonEsp.enabled")));
        check("old presentation key does not prevent settings loading", "false".equals(p.getProperty("ui.feature.tree.dungeon.mob-esp")));
    }

    private static void flatRowsUseOnlyTheTwoAllowedForms() {
        check("dropdown depth is at most one", FeatureSettingsUiModel.maximumDropdownDepth() <= 1);
        for (FeatureSettingsUiModel.Feature feature : FeatureSettingsUiModel.features()) {
            if (feature.isSimple()) {
                check("simple row has only a toggle: " + feature.id,
                        feature.tabControls.size() == 1 && FeatureSettingsUiModel.TOGGLE.equals(feature.tabControls.get(0)));
                check("simple row has no editor: " + feature.id, feature.editorId.length() == 0
                        && feature.editorButtonLabel.length() == 0);
            } else {
                check("dropdown has exactly toggle plus editor button: " + feature.id,
                        feature.tabControls.size() == 2 && FeatureSettingsUiModel.TOGGLE.equals(feature.tabControls.get(0))
                                && FeatureSettingsUiModel.EDITOR_BUTTON.equals(feature.tabControls.get(1)));
                check("dropdown is one level: " + feature.id, feature.dropdownDepth() == 1);
                check("dropdown editor button is Settings: " + feature.id,
                        "Settings".equals(feature.editorButtonLabel));
            }
        }
    }

    private static void everyOldSurfaceHasOneRoute() {
        check("option inventory is complete", FeatureSettingsUiModel.options().size() > 90);
        check("all options have a reachable route", FeatureSettingsUiModel.isReachabilityValid());
        check("no per-child tab rows are modeled", FeatureSettingsUiModel.maximumDropdownDepth() == 1);
        check("Mob ESP is a dropdown feature", isDropdownEditor("mob-esp", "mob-esp"));
        check("Door ESP is a dropdown feature", isDropdownEditor("door-esp", "door-esp"));
        check("FPS Display is a simple General row", FeatureSettingsUiModel.feature("fps-display").isSimple());
        check("Ping Display is a simple General row", FeatureSettingsUiModel.feature("ping-display").isSimple());
        check("Memory Display is a simple General row", FeatureSettingsUiModel.feature("memory-display").isSimple());
        check("Hotbar customization is not a HUD row", FeatureSettingsUiModel.feature("hud-hotbar") == null);
        check("Item Protection is not a Quality of Life row", FeatureSettingsUiModel.feature("item-protection") == null);
        check("No Block Particles is a simple row",  FeatureSettingsUiModel.feature("no-block-particles").isSimple());
        check("No Hurt Cam is a simple row", FeatureSettingsUiModel.feature("no-hurt-cam").isSimple());
        check("Auto Sprint is a simple row", FeatureSettingsUiModel.feature("auto-sprint").isSimple());
        check("renamed simple labels are plain", "No Hotbar Scrolling".equals(
                FeatureSettingsUiModel.feature("disable-hotbar-scrolling").title)
                && "Dungeon Sync".equals(FeatureSettingsUiModel.feature("dungeon-sync-enabled").title)
                && "Sync Details".equals(FeatureSettingsUiModel.feature("dungeon-sync-debug").title));
        check("Auto Sprint is not nested in Movement and Shortcuts",  FeatureSettingsUiModel.option("auto-sprint") == null);
        check("No Hurt Cam is not nested in Inventory Assist", FeatureSettingsUiModel.option("no-hurt-camera") == null);
        check("Sync Details is a simple row", FeatureSettingsUiModel.feature("dungeon-sync-debug").isSimple());
    }

    private static void dungeonsUseRequestedUsageOrder() {
        List<String> actual = new ArrayList<String>();
        for (FeatureSettingsUiModel.Feature feature : FeatureSettingsUiModel.features()) {
            if ("Dungeon".equals(feature.tab)) actual.add(feature.id);
        }
        List<String> expected = Arrays.asList("door-esp", "mob-esp", "dungeon-sync-enabled", "m7-overlay",
                "spirit-leap", "terminal-assist", "dungeon-boss-hud", "dungeon-map", "dungeon-sync-debug");
        check("Dungeons use ESP, helpers, then information order", expected.equals(actual));
    }

    private static boolean isDropdownEditor(String featureId, String editorId) {
        FeatureSettingsUiModel.Feature feature = FeatureSettingsUiModel.feature(featureId);
        return feature != null && feature.isDropdown() && editorId.equals(feature.editorId);
    }

    private static void everyDungeonEspControlStillHasARoute() {
        for (DungeonEspUiModel.Option option : DungeonEspUiModel.options()) {
            check("Dungeon ESP route: " + option.name, FeatureSettingsUiModel.hasOptionLabel(option.name));
        }
    }

    private static void editorButtonsTargetTheRightEditors() {
        check("Mob ESP button is Settings", "Settings".equals(FeatureSettingsUiModel.feature("mob-esp").editorButtonLabel));
        check("Door ESP button is Settings", "Settings".equals(FeatureSettingsUiModel.feature("door-esp").editorButtonLabel));
        check("Item Lock button keeps its editor", isDropdownEditor("item-lock-swap", "item-lock-swap"));
        check("Chat button keeps Chat settings route", isDropdownEditor("chat-qol", "chat"));
        check("Item Animation keeps its existing editor", isDropdownEditor("item-animation", "item-animation"));
        check("M7 keeps its focused editor", isDropdownEditor("m7-overlay", "m7-overlay"));
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

    private static void statusDotsReflectTheirToggleState() {
        check("enabled status dot is green", FeatureSettingsUiModel.statusDotColor(true) == FeatureSettingsUiModel.STATUS_DOT_ON);
        check("disabled status dot is red", FeatureSettingsUiModel.statusDotColor(false) == FeatureSettingsUiModel.STATUS_DOT_OFF);
        check("status dots are distinct", FeatureSettingsUiModel.STATUS_DOT_ON != FeatureSettingsUiModel.STATUS_DOT_OFF);
    }

    private static void rowsNeverOverlapAtGuiScales() {
        float[] widths = {1f, 80f, 160f, 214f, 427f, 854f, 1708f};
        for (float width : widths) {
            GuiLayoutToolkit.Rect row = new GuiLayoutToolkit.Rect().set(0f, 0f, width, 42f);
            GuiLayoutToolkit.Rect label = new GuiLayoutToolkit.Rect();
            GuiLayoutToolkit.Rect control = new GuiLayoutToolkit.Rect();
            GuiLayoutToolkit.splitRow(row, 12f, 8f, 172f, 24f, label, control);
            check("flat label/control columns do not overlap at " + width, !label.intersects(control));
            check("flat control stays in bounds at " + width, control.right() <= row.right() && control.x >= row.x);
        }
        float top = 0f;
        float previousBottom = -1f;
        for (FeatureSettingsUiModel.Feature feature : FeatureSettingsUiModel.features()) {
            float height = feature.isSimple() ? 40f : 42f;
            check("feature header follows previous row: " + feature.id, top >= previousBottom);
            previousBottom = top + height;
            top = AppleHubScreen.nextRowTop(top, height);
            if (feature.isDropdown()) {
                for (int body = 0; body < 2; body++) {
                    check("dropdown body follows header: " + feature.id + " / " + body, top >= previousBottom);
                    previousBottom = top + 40f;
                    top = AppleHubScreen.nextRowTop(top, 40f);
                }
            }
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
