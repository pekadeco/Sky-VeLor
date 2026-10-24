package com.arena.pekaclient.gui;

/** Scale-1–4 geometry proof for inline disclosure rows and their reflowed child controls. */
public final class InlineDropdownLayoutTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        rightAlignedRowsDoNotOverlapAtAllGuiScales();
        narrowRowsStackControlsBelowTheirLabelsAtAllGuiScales();
        nestedCatalogRowsHaveARealOwnerPath();
        done();
    }

    private static void rightAlignedRowsDoNotOverlapAtAllGuiScales() {
        for (int scale = 1; scale <= 4; scale++) {
            float logicalWidth = 1280f / scale;
            float rowX = 12f;
            float rowW = Math.max(92f, Math.min(520f, logicalWidth - 48f));
            float toggleW = 38f;
            float controlX = SettingsControlLayout.rightX(rowX, rowW, 12f, toggleW);
            float labelW = Math.max(0f, controlX - rowX - 8f);
            check("standard toggle stays in bounds at scale " + scale,
                    controlX >= rowX && controlX + toggleW <= rowX + rowW);
            check("standard toggle leaves a label gap at scale " + scale,
                    SettingsControlLayout.doesNotOverlap(rowX, labelW, controlX, 8f));
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

    private static void nestedCatalogRowsHaveARealOwnerPath() {
        ReferenceSettingsCatalog.Match match = null;
        for (ReferenceSettingsCatalog.Match candidate : ReferenceSettingsCatalog.search("custom accent colour")) {
            if ("chat-tab-accent-color".equals(candidate.setting.id)) { match = candidate; break; }
        }
        check("nested Chat text row is searchable", match != null);
        check("nested Chat text row has two collapsed owner rows", match != null && match.parents.size() == 2
                && "chat-qol".equals(match.parents.get(0).id)
                && "chat-tabs-group".equals(match.parents.get(1).id));
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++; else { failed++; System.out.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("inline dropdown layout: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
