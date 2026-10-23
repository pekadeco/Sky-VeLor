package com.arena.pekaclient.gui;

import com.arena.pekaclient.config.DungeonEspSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * Minecraft-free legacy-option inventory for Dungeon ESP. The flat Hub never renders these four former groups;
 * Mob ESP and Door ESP now own the same labels through their focused editors. Keeping this list lets tests prove
 * that every pre-simplification option remains reachable without restoring child rows to the tab.
 */
public final class DungeonEspUiModel {

    public static final String MOB_ESP_TITLE = "Mob ESP";
    public static final String DOOR_ESP_TITLE = "Door ESP";
    public static final String PERFORMANCE_TITLE = "Performance";
    public static final String PRESETS_TITLE = "Presets";

    private static final String[] GROUPS = {
            MOB_ESP_TITLE, DOOR_ESP_TITLE, PERFORMANCE_TITLE, PRESETS_TITLE
    };

    /** Human-facing options in the same order as the real Dungeon page. */
    private static final Option[] OPTIONS = buildOptions();
    private static final int[] ROW_COUNTS = countRows();

    private DungeonEspUiModel() {
    }

    /** Immutable name/group pair so headless tests can audit ownership without Minecraft widgets. */
    public static final class Option {
        public final String group;
        public final String name;

        private Option(String group, String name) {
            this.group = group;
            this.name = name;
        }
    }

    public static String[] groupTitles() {
        return GROUPS.clone();
    }

    public static Option[] options() {
        return OPTIONS.clone();
    }

    public static int groupCount() {
        return GROUPS.length;
    }

    public static int rowCount(int group) {
        return group >= 0 && group < ROW_COUNTS.length ? ROW_COUNTS[group] : 0;
    }

    public static int totalControlRows() {
        int total = 0;
        for (int i = 0; i < ROW_COUNTS.length; i++) total += ROW_COUNTS[i];
        return total;
    }

    /** Existing shared dropdown palette; colors are RGB because each style separately exposes opacity sliders. */
    public static String[] colorNames() {
        return DungeonEspSettings.COLOR_NAMES.clone();
    }

    /**
     * Computes group geometry with GuiLayoutToolkit rather than a bespoke ESP layout. Closed groups reserve only
     * their headers, so content rows remain physically disjoint at every supported GUI scale and viewport height.
     */
    public static GuiLayoutToolkit.Sections layout(float width, float height, boolean[] open) {
        float[] bodyHeights = new float[GROUPS.length];
        float[] openness = new float[GROUPS.length];
        for (int i = 0; i < GROUPS.length; i++) {
            bodyHeights[i] = ROW_COUNTS[i] * 40f + Math.max(0, ROW_COUNTS[i] - 1) * GuiLayoutToolkit.GAP;
            openness[i] = open != null && i < open.length && open[i] ? 1f : 0f;
        }
        GuiLayoutToolkit.Sections sections = new GuiLayoutToolkit.Sections(GROUPS.length);
        sections.layout(0f, 0f, Math.max(1f, width), Math.max(1f, height), 32f,
                GuiLayoutToolkit.GAP, bodyHeights, openness, 0f);
        return sections;
    }

    private static Option[] buildOptions() {
        List<Option> values = new ArrayList<Option>();
        add(values, MOB_ESP_TITLE, "Enable Dungeon ESP");
        addStyle(values, MOB_ESP_TITLE, "Starred Mob");
        addStyle(values, MOB_ESP_TITLE, "Miniboss");
        addStyle(values, MOB_ESP_TITLE, "Special Starred Mob");
        addStyle(values, MOB_ESP_TITLE, "Enderman");
        addStyle(values, MOB_ESP_TITLE, "Skeletor");
        add(values, MOB_ESP_TITLE, "Force Render All Mobs in Room");
        addStyle(values, MOB_ESP_TITLE, "Other Mobs");
        add(values, MOB_ESP_TITLE, "Include Bats");
        add(values, MOB_ESP_TITLE, "Include Invisible Mobs");

        add(values, DOOR_ESP_TITLE, "Show Closed Doors");
        add(values, DOOR_ESP_TITLE, "Door Scope");
        add(values, DOOR_ESP_TITLE, "Only With a Wither Key");
        add(values, DOOR_ESP_TITLE, "Blood Doors");
        add(values, DOOR_ESP_TITLE, "Blood Door Color");
        add(values, DOOR_ESP_TITLE, "Wither Door Color");
        add(values, DOOR_ESP_TITLE, "Door Render Mode");
        add(values, DOOR_ESP_TITLE, "Doors Through Walls");
        add(values, DOOR_ESP_TITLE, "Door Outline Width");
        add(values, DOOR_ESP_TITLE, "Door Outline Opacity");
        add(values, DOOR_ESP_TITLE, "Door Fill Opacity");
        add(values, DOOR_ESP_TITLE, "Door Distance Text");

        add(values, PERFORMANCE_TITLE, "Maximum ESP Distance");
        add(values, PERFORMANCE_TITLE, "Update Interval");
        add(values, PERFORMANCE_TITLE, "Hide Zero-Health Mobs");
        add(values, PERFORMANCE_TITLE, "Tracers");
        add(values, PERFORMANCE_TITLE, "Distance Text");
        add(values, PERFORMANCE_TITLE, "Maximum Boxes");

        add(values, PRESETS_TITLE, "Default Preset");
        add(values, PRESETS_TITLE, "Subtle Preset");
        add(values, PRESETS_TITLE, "Reset Dungeon ESP");
        return values.toArray(new Option[values.size()]);
    }

    private static void add(List<Option> values, String group, String name) {
        values.add(new Option(group, name));
    }

    private static void addStyle(List<Option> values, String group, String prefix) {
        add(values, group, prefix + " ESP");
        add(values, group, prefix + " Color");
        add(values, group, prefix + " Render Mode");
        add(values, group, prefix + " Through Walls");
        add(values, group, prefix + " Outline Width");
        add(values, group, prefix + " Outline Opacity");
        add(values, group, prefix + " Fill Opacity");
    }

    private static int[] countRows() {
        int[] counts = new int[GROUPS.length];
        for (int i = 0; i < OPTIONS.length; i++) {
            for (int group = 0; group < GROUPS.length; group++) {
                if (GROUPS[group].equals(OPTIONS[i].group)) counts[group]++;
            }
        }
        return counts;
    }
}
