package com.arena.pekaclient.gui;

import com.arena.pekaclient.config.DungeonEspSettings;
import com.arena.pekaclient.widget.AppleDropdown;
import com.arena.pekaclient.widget.Widget;

import java.util.ArrayList;
import java.util.List;

/** Exercises live Dungeon ESP editor widgets without opening a Minecraft screen. */
public final class DungeonEspEditorSpecsTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        globalControlsWriteTheLiveSettingsOwner();
        categoryControlsWriteTheExactStyleOwner();
        doorControlsWriteTheDoorSettingsOwner();
        done();
    }

    private static void globalControlsWriteTheLiveSettingsOwner() {
        DungeonEspSettings.resetDefaults();
        List<FeatureSettingsEditorScreen.Entry> entries = entries("global", DungeonEspEditorSpecs.mobSpec());
        click(find(entries, "Mob Category Labels").widget);
        check("label toggle writes live DungeonEspSettings", !DungeonEspSettings.mobLabels);
        setSliderToEnd(find(entries, "Maximum Mob Labels").widget);
        check("label cap slider writes live DungeonEspSettings", DungeonEspSettings.maxMobLabels
                == DungeonEspSettings.MAX_MOB_LABELS);
        click(find(entries, "Distance Text").widget);
        check("distance toggle remains independent", DungeonEspSettings.distanceText);
    }

    private static void categoryControlsWriteTheExactStyleOwner() {
        DungeonEspSettings.resetDefaults();
        List<FeatureSettingsEditorScreen.Entry> entries = entries("starred", DungeonEspEditorSpecs.mobSpec());
        click(find(entries, "Enable Starred Mob ESP").widget);
        check("category enable toggle writes starred style", !DungeonEspSettings.starred.enabled);
        FeatureSettingsEditorScreen.Entry color = find(entries, "Starred Mob Color");
        check("color control is a dropdown", color.widget instanceof AppleDropdown);
        ((AppleDropdown) color.widget).choose(1);
        check("color dropdown writes starred style", DungeonEspSettings.starred.color == DungeonEspSettings.COLORS[1]);
    }

    private static void doorControlsWriteTheDoorSettingsOwner() {
        DungeonEspSettings.resetDefaults();
        List<FeatureSettingsEditorScreen.Entry> entries = entries("global", DungeonEspEditorSpecs.doorSpec());
        click(find(entries, "Show Closed Doors").widget);
        check("door master toggle writes DungeonEspSettings", !DungeonEspSettings.doorsEnabled);
        FeatureSettingsEditorScreen.Entry scope = find(entries, "Door Scope");
        check("door scope is a dropdown", scope.widget instanceof AppleDropdown);
        ((AppleDropdown) scope.widget).choose(DungeonEspSettings.DOORS_ALL);
        check("door scope dropdown writes live setting", DungeonEspSettings.doorScope == DungeonEspSettings.DOORS_ALL);
    }

    private static List<FeatureSettingsEditorScreen.Entry> entries(String categoryId,
                                                                     FeatureSettingsEditorScreen.Spec spec) {
        FeatureSettingsEditorScreen.Category category = null;
        for (FeatureSettingsEditorScreen.Category item : spec.categories) {
            if (categoryId.equals(item.id)) {
                category = item;
                break;
            }
        }
        List<FeatureSettingsEditorScreen.Entry> entries = new ArrayList<FeatureSettingsEditorScreen.Entry>();
        if (category != null) spec.entries.build(category, entries);
        return entries;
    }

    private static FeatureSettingsEditorScreen.Entry find(List<FeatureSettingsEditorScreen.Entry> entries,
                                                           String label) {
        for (FeatureSettingsEditorScreen.Entry entry : entries) if (label.equals(entry.label)) return entry;
        failed++;
        System.out.println("FAIL missing editor entry " + label);
        return new FeatureSettingsEditorScreen.Entry(label, "", null);
    }

    private static void click(Widget widget) {
        if (widget == null) return;
        widget.x = 0F;
        widget.y = 0F;
        widget.w = Math.max(40F, widget.w);
        widget.h = Math.max(20F, widget.h);
        widget.onPress(1, 1);
        widget.onRelease(1, 1, true);
    }

    private static void setSliderToEnd(Widget widget) {
        if (widget == null) return;
        widget.x = 0F;
        widget.y = 0F;
        widget.w = Math.max(100F, widget.w);
        widget.h = Math.max(20F, widget.h);
        int x = Math.max(0, (int) widget.w - 1);
        widget.onPress(x, 1);
        widget.onRelease(x, 1, true);
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++;
        else {
            failed++;
            System.out.println("FAIL " + name);
        }
    }

    private static void done() {
        System.out.println("dungeon ESP editor specs: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
