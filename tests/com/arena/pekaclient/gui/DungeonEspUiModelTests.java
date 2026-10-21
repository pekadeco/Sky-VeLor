package com.arena.pekaclient.gui;

/** Regression proof that the full Dungeon ESP control set uses the shared collapsible flow without row overlap. */
public final class DungeonEspUiModelTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        ownsRequiredGroups();
        rowsNeverOverlapAtGuiScales();
        closedGroupsReserveNoBodyRows();
        done();
    }

    private static void ownsRequiredGroups() {
        String[] groups = DungeonEspUiModel.groupTitles();
        check("Dungeon ESP master group", contains(groups, "Dungeon ESP"));
        check("starred group", contains(groups, "Starred Mobs"));
        check("miniboss group", contains(groups, "Minibosses"));
        check("global group", contains(groups, "Global"));
        check("door group", contains(groups, "Wither & Blood Doors"));
        check("all controls represented", DungeonEspUiModel.totalControlRows() >= 40);
    }

    private static void rowsNeverOverlapAtGuiScales() {
        float[] widths = {160f, 214f, 427f, 854f, 1708f};
        boolean[] open = new boolean[DungeonEspUiModel.groupCount()];
        for (int i = 0; i < open.length; i++) open[i] = true;
        for (int widthIndex = 0; widthIndex < widths.length; widthIndex++) {
            GuiLayoutToolkit.Sections sections = DungeonEspUiModel.layout(widths[widthIndex], 120f, open);
            for (int i = 0; i < sections.headers.length; i++) {
                check("header/body disjoint " + widths[widthIndex] + " / " + i,
                        !sections.headers[i].intersects(sections.bodies[i]));
                if (i + 1 < sections.headers.length) {
                    check("group flow ordered " + widths[widthIndex] + " / " + i,
                            sections.headers[i + 1].y >= sections.bodies[i].bottom());
                }
            }
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
    private static void check(String name, boolean ok) { if (ok) passed++; else { failed++; System.out.println("FAIL " + name); } }
    private static void done() { System.out.println("dungeon ESP UI model: passed " + passed + ", failed " + failed); if (failed > 0) System.exit(1); }
}
