package com.arena.pekaclient.gui;

import com.arena.pekaclient.config.DungeonEspSettings;

/**
 * Minecraft-free ownership and layout contract for the Dungeon tab's collapsible ESP controls. AppleHubScreen uses
 * the existing Row headers/dropdowns/sliders; this small model lets the full set remain regression-testable at every
 * GUI scale without constructing a Minecraft screen.
 */
public final class DungeonEspUiModel {

    public static final String SECTION_TITLE = "Dungeon ESP";
    public static final String STARRED_TITLE = "Starred Mobs";
    public static final String MINIBOSS_TITLE = "Minibosses";
    public static final String SPECIAL_TITLE = "Special Starred Mobs";
    public static final String GLOBAL_TITLE = "Global";
    public static final String DOORS_TITLE = "Wither & Blood Doors";
    public static final String PRESETS_TITLE = "Presets";

    private static final String[] GROUPS = {
            SECTION_TITLE, STARRED_TITLE, MINIBOSS_TITLE, SPECIAL_TITLE, GLOBAL_TITLE, DOORS_TITLE, PRESETS_TITLE
    };
    /** One master item, then style rows, global rows, door rows and preset actions in visual order. */
    private static final int[] ROW_COUNTS = {1, 7, 7, 7, 7, 11, 3};

    private DungeonEspUiModel() {
    }

    public static String[] groupTitles() {
        return GROUPS.clone();
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
     * their headers, so all content rows remain physically disjoint at 25%, 50%, 100% and large GUI scales.
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
}
