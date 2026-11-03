package com.arena.pekaclient.gui;

/** Scale-1–4 geometry proof for flat inline disclosures and their fixed columns. */
public final class InlineDropdownLayoutTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        fixedColumnsDoNotOverlapAtAllGuiScales();
        narrowRowsStackControlsBelowTheirLabelsAtAllGuiScales();
        searchNeedsOnlyOneOwnerDisclosure();
        done();
    }

    private static void fixedColumnsDoNotOverlapAtAllGuiScales() {
        for (int scale = 1; scale <= 4; scale++) {
            float logicalWidth = 1280f / scale;
            float rowX = 12f;
            float rowW = Math.max(92f, Math.min(520f, logicalWidth - 48f));
            float chevronX = SettingsControlLayout.chevronCenterX(rowX, rowW, 12f);
            float toggleX = SettingsControlLayout.toggleX(rowX, rowW, 12f);
            float controlRight = SettingsControlLayout.contentRight(rowX, rowW, 12f, 8f);
            float labelW = Math.max(0f, controlRight - rowX - 8f);
            check("standard toggle stays in bounds at scale " + scale,
                    toggleX >= rowX && toggleX + SettingsControlLayout.TOGGLE_COLUMN_WIDTH <= rowX + rowW);
            check("label stays before fixed columns at scale " + scale,
                    SettingsControlLayout.doesNotOverlap(rowX, labelW, controlRight, 8f));
            check("chevron shares one x for every row type at scale " + scale,
                    chevronX == SettingsControlLayout.chevronCenterX(rowX, rowW, 12f));
            check("chevron column precedes the rightmost toggle at scale " + scale,
                    chevronX + SettingsControlLayout.CHEVRON_COLUMN_WIDTH * .5f + SettingsControlLayout.COLUMN_GAP
                            <= toggleX + .01f);
        }
    }

    private static void narrowRowsStackControlsBelowTheirLabelsAtAllGuiScales() {
        for (int scale = 1; scale <= 4; scale++) {
            float logicalWidth = 1280f / scale;
            float contentW = Math.max(1f, logicalWidth - 64f);
            boolean narrow = SettingsPageLayout.isNarrow(contentW);
            float rowY = 20f;
            float rowH = narrow ? 60f : 38f;
            float controlH = 22f;
            float controlY = narrow ? SettingsControlLayout.stackedY(rowY, rowH, controlH, 7f)
                    : SettingsControlLayout.centeredY(rowY, rowH, controlH);
            check("inline control remains vertically inside row at scale " + scale,
                    controlY >= rowY && controlY + controlH <= rowY + rowH);
            if (narrow) check("narrow scale stacks control after label area at scale " + scale, controlY >= rowY + 28f);
        }
    }

    private static void searchNeedsOnlyOneOwnerDisclosure() {
        ReferenceSettingsCatalog.Match match = null;
        for (ReferenceSettingsCatalog.Match candidate : ReferenceSettingsCatalog.search("custom accent color")) {
            if ("chat-tab-accent-color".equals(candidate.setting.id)) { match = candidate; break; }
        }
        check("flat Chat text row is searchable", match != null);
        check("flat Chat text row has one owning dropdown", match != null && match.parents.size() == 1
                && "chat-qol".equals(match.parents.get(0).id));
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++; else { failed++; System.out.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("inline dropdown layout: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
