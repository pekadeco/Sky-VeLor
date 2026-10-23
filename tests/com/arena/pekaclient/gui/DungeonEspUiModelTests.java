package com.arena.pekaclient.gui;

import java.util.HashSet;
import java.util.Set;

/** Regression proof that the pre-simplification Dungeon ESP option inventory remains editor-reachable. */
public final class DungeonEspUiModelTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        retainsTheLegacyOptionInventory();
        everyNamedOptionHasAnEditorRoute();
        editorsUseFlatCategories();
        done();
    }

    private static void retainsTheLegacyOptionInventory() {
        String[] groups = DungeonEspUiModel.groupTitles();
        check("legacy Mob ESP group remains inventory owner", groups.length > 0 && "Mob ESP".equals(groups[0]));
        check("legacy Door ESP group remains inventory owner", groups.length > 1 && "Door ESP".equals(groups[1]));
        check("all previous ESP controls plus readable label controls represented", DungeonEspUiModel.totalControlRows() == 69);
        check("mob control count", DungeonEspUiModel.rowCount(0) == 46);
        check("door control count", DungeonEspUiModel.rowCount(1) == 12);
        check("performance and label control count", DungeonEspUiModel.rowCount(2) == 8);
        check("preset control count", DungeonEspUiModel.rowCount(3) == 3);
    }

    private static void everyNamedOptionHasAnEditorRoute() {
        DungeonEspUiModel.Option[] options = DungeonEspUiModel.options();
        Set<String> names = new HashSet<String>();
        for (DungeonEspUiModel.Option option : options) {
            check("human-facing option: " + option.name, option.name != null && option.name.trim().length() > 0);
            check("flat editor route: " + option.name, FeatureSettingsUiModel.hasOptionLabel(option.name));
            names.add(option.name);
        }
        check("no duplicate old option rows", names.size() == options.length);
        check("Enderman remains in the editor", names.contains("Enderman ESP"));
        check("Skeletor remains in the editor", names.contains("Skeletor ESP"));
        check("force-render switch remains in the editor", names.contains("Force Render All Mobs in Room"));
        check("mob labels remain editor-reachable", names.contains("Mob Category Labels")
                && names.contains("Maximum Mob Labels"));
    }

    private static void editorsUseFlatCategories() {
        FeatureSettingsEditorScreen.Spec mob = DungeonEspEditorSpecs.mobSpec();
        FeatureSettingsEditorScreen.Spec door = DungeonEspEditorSpecs.doorSpec();
        check("Mob editor has a flat category list", hasFlatList(mob));
        check("Door editor has a flat category list", hasFlatList(door));
        check("Mob editor supplies one toggle per left-list category", allCategoriesHaveToggles(mob));
        check("Door editor supplies one toggle per left-list category", allCategoriesHaveToggles(door));
    }

    private static boolean hasFlatList(FeatureSettingsEditorScreen.Spec spec) {
        // The flat Category API has no parent/depth fields; this ordered list is the whole navigation surface.
        return spec != null && spec.categories != null && !spec.categories.isEmpty();
    }

    private static boolean allCategoriesHaveToggles(FeatureSettingsEditorScreen.Spec spec) {
        for (FeatureSettingsEditorScreen.Category category : spec.categories) if (!category.hasToggle()) return false;
        return true;
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++; else { failed++; System.out.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("dungeon ESP UI model: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
