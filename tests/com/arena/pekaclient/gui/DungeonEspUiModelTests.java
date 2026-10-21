package com.arena.pekaclient.gui;

import java.util.HashSet;
import java.util.Set;

/** Regression proof that the full Dungeon ESP set stays on one four-group collapsible surface without overlap. */
public final class DungeonEspUiModelTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        ownsExactlyRequiredGroups();
        everyNamedOptionBelongsOnce();
        rowsNeverOverlapAtGuiScales();
        responsiveColumnsNeverOverlapAtGuiScales();
        closedGroupsReserveNoBodyRows();
        done();
    }

    private static void ownsExactlyRequiredGroups() {
        String[] groups = DungeonEspUiModel.groupTitles();
        check("exactly four ESP groups", groups.length == 4);
        check("Mob ESP is first", groups.length > 0 && "Mob ESP".equals(groups[0]));
        check("Door ESP is second", groups.length > 1 && "Door ESP".equals(groups[1]));
        check("Performance is third", groups.length > 2 && "Performance".equals(groups[2]));
        check("Presets is fourth", groups.length > 3 && "Presets".equals(groups[3]));
        check("all ESP controls represented", DungeonEspUiModel.totalControlRows() == 67);
        check("mob control count", DungeonEspUiModel.rowCount(0) == 46);
        check("door control count", DungeonEspUiModel.rowCount(1) == 12);
        check("performance control count", DungeonEspUiModel.rowCount(2) == 6);
        check("preset control count", DungeonEspUiModel.rowCount(3) == 3);
    }

    private static void everyNamedOptionBelongsOnce() {
        String[] groups = DungeonEspUiModel.groupTitles();
        DungeonEspUiModel.Option[] options = DungeonEspUiModel.options();
        Set<String> names = new HashSet<String>();
        boolean groupKnown = true;
        boolean nonBlank = true;
        for (int i = 0; i < options.length; i++) {
            DungeonEspUiModel.Option option = options[i];
            nonBlank &= option.name != null && option.name.trim().length() > 0;
            groupKnown &= contains(groups, option.group);
            names.add(option.name);
        }
        check("all control names are human-facing", nonBlank);
        check("all controls have one of four owners", groupKnown);
        check("no duplicate option rows", names.size() == options.length);
        check("Enderman category is listed", names.contains("Enderman ESP"));
        check("Skeletor category is listed", names.contains("Skeletor ESP"));
        check("force-render switch is listed", names.contains("Force Render All Mobs in Room"));
    }

    private static void rowsNeverOverlapAtGuiScales() {
        float[] widths = {160f, 214f, 427f, 854f, 1708f};
        float[] heights = {80f, 120f, 240f, 480f};
        boolean[] open = new boolean[DungeonEspUiModel.groupCount()];
        for (int i = 0; i < open.length; i++) open[i] = true;
        for (int widthIndex = 0; widthIndex < widths.length; widthIndex++) {
            for (int heightIndex = 0; heightIndex < heights.length; heightIndex++) {
                GuiLayoutToolkit.Sections sections = DungeonEspUiModel.layout(widths[widthIndex], heights[heightIndex], open);
                for (int i = 0; i < sections.headers.length; i++) {
                    check("header/body disjoint " + widths[widthIndex] + " / " + heights[heightIndex] + " / " + i,
                            !sections.headers[i].intersects(sections.bodies[i]));
                    if (i + 1 < sections.headers.length) {
                        check("group flow ordered " + widths[widthIndex] + " / " + heights[heightIndex] + " / " + i,
                                sections.headers[i + 1].y >= sections.bodies[i].bottom());
                    }
                }
            }
        }
    }

    private static void responsiveColumnsNeverOverlapAtGuiScales() {
        float[] widths = {1f, 80f, 160f, 214f, 427f, 854f, 1708f};
        for (int i = 0; i < widths.length; i++) {
            GuiLayoutToolkit.Rect row = new GuiLayoutToolkit.Rect().set(0f, 0f, widths[i], 40f);
            GuiLayoutToolkit.Rect label = new GuiLayoutToolkit.Rect();
            GuiLayoutToolkit.Rect control = new GuiLayoutToolkit.Rect();
            GuiLayoutToolkit.splitRow(row, 12f, 12f, 150f, 24f, label, control);
            check("responsive row is in bounds " + widths[i], label.x >= row.x && control.right() <= row.right());
            check("responsive label/control do not overlap " + widths[i], !label.intersects(control));
        }
    }

    private static void closedGroupsReserveNoBodyRows() {
        GuiLayoutToolkit.Sections sections = DungeonEspUiModel.layout(214f, 120f, new boolean[DungeonEspUiModel.groupCount()]);
        boolean allClosed = true;
        for (int i = 0; i < sections.bodies.length; i++) allClosed &= sections.bodies[i].h == 0f;
        check("collapsed groups have zero body geometry", allClosed);
    }

    private static boolean contains(String[] values, String value) {
        for (int i = 0; i < values.length; i++) if (value.equals(values[i])) return true;
        return false;
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++; else { failed++; System.out.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("dungeon ESP UI model: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
