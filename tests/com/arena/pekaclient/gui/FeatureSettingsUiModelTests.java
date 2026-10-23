package com.arena.pekaclient.gui;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/** Headless proof for compact-tree persistence, connector geometry and option reachability. */
public final class FeatureSettingsUiModelTests {
    private static int passed, failed;

    public static void main(String[] args) {
        remembersOnlyPresentationState();
        closedTreeConsumesNoRows();
        connectorsFollowIndentedChildren();
        everyOldSurfaceHasOneRoute();
        everyDungeonEspControlStillHasARoute();
        done();
    }

    private static void remembersOnlyPresentationState() {
        FeatureUiState source = new FeatureUiState();
        source.setExpanded("dungeon.mob-esp", false);
        source.setSelection("mob-esp", "enderman");
        Properties p = new Properties();
        p.setProperty("dungeon.esp.enabled", "true");
        source.save(p);
        FeatureUiState restored = new FeatureUiState();
        restored.load(p);
        check("tree state round trips", !restored.isExpanded("dungeon.mob-esp", true));
        check("editor selection round trips", "enderman".equals(restored.selection("mob-esp", "global")));
        check("feature schema remains untouched", "true".equals(p.getProperty("dungeon.esp.enabled")));
        check("presentation keys are namespaced", p.getProperty("ui.feature.tree.dungeon.mob-esp") != null);
    }

    private static void closedTreeConsumesNoRows() {
        List<FeatureTreeLayout.Node> nodes = Arrays.asList(
                new FeatureTreeLayout.Node("dungeon", null, 0, true, true, 40f),
                new FeatureTreeLayout.Node("mob", "dungeon", 1, true, true, 40f),
                new FeatureTreeLayout.Node("enderman", "mob", 2, false, false, 40f),
                new FeatureTreeLayout.Node("door", "dungeon", 1, false, false, 40f));
        FeatureTreeLayout.Result result = FeatureTreeLayout.layout(nodes,
                (id, fallback) -> !"mob".equals(id), 0f, 0f, 300f);
        FeatureTreeLayout.Row hidden = result.find("enderman");
        FeatureTreeLayout.Row door = result.find("door");
        check("closed descendant has zero geometry", hidden != null && !hidden.visible && hidden.bounds.h == 0f);
        check("later sibling packs immediately", door != null && door.bounds.y == 80f);
        check("visible rows do not overlap", door != null && door.bounds.y >= result.find("mob").bounds.bottom());
    }

    private static void connectorsFollowIndentedChildren() {
        List<FeatureTreeLayout.Node> nodes = Arrays.asList(
                new FeatureTreeLayout.Node("root", null, 0, true, true, 38f),
                new FeatureTreeLayout.Node("child", "root", 1, false, false, 38f));
        FeatureTreeLayout.Result result = FeatureTreeLayout.layout(nodes, (id, fallback) -> true, 12f, 4f, 240f);
        check("one child connector", result.connectors().size() == 1);
        FeatureTreeLayout.Connector connector = result.connectors().get(0);
        FeatureTreeLayout.Row child = result.find("child");
        check("connector stays inside child row", connector.y1 >= child.bounds.y && connector.y2 <= child.bounds.bottom());
        check("connector has visible tick", connector.tickEnd > connector.x);
        check("child arrow is indented", child.arrowX > result.find("root").arrowX);
    }

    private static void everyOldSurfaceHasOneRoute() {
        check("option inventory is complete", FeatureSettingsUiModel.options().size() > 90);
        check("all options have a reachable route", FeatureSettingsUiModel.isReachabilityValid());
        check("Mob ESP is an editor feature", FeatureSettingsUiModel.feature("mob-esp") != null
                && "mob-esp".equals(FeatureSettingsUiModel.feature("mob-esp").editorId));
        check("Door ESP is an editor feature", FeatureSettingsUiModel.feature("door-esp") != null
                && "door-esp".equals(FeatureSettingsUiModel.feature("door-esp").editorId));
        check("simple branches stay short", FeatureSettingsUiModel.feature("qol-gameplay").optionIds.size() <= 2);
    }

    private static void everyDungeonEspControlStillHasARoute() {
        for (DungeonEspUiModel.Option option : DungeonEspUiModel.options()) {
            check("Dungeon ESP route: " + option.name, FeatureSettingsUiModel.hasOptionLabel(option.name));
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
