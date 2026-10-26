package com.arena.pekaclient.gui;

import com.arena.pekaclient.feature.ItemAnimation;

import java.util.Arrays;
import java.util.List;

/** Contract for the standalone renderer-only Item Animation editor. */
public final class ItemAnimationReachabilityTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        itemAnimationUsesOneDistinctEditorRow();
        standalonePanelContainsOnlyItsOwnSurfaceAndControls();
        realPreviewSwingRemainsRendererOnly();
        done();
    }

    private static void itemAnimationUsesOneDistinctEditorRow() {
        ReferenceSettingsCatalog.Setting row = ReferenceSettingsCatalog.find("item-animation");
        check("Item Animation is a toggle plus Edit editor row", row != null && row.isEditor()
                && row.hasMasterToggle() && "item-animation-open-editor".equals(row.editorActionId));
        check("Item Animation no longer owns a dropdown or a separate action row", row != null && !row.isDropdown()
                && ReferenceSettingsCatalog.find("item-animation-open-editor") == null);
        check("standalone panel stays near twenty-eight percent wide", ItemAnimationEditorScreen.PANEL_WIDTH_FRACTION >= .27f
                && ItemAnimationEditorScreen.PANEL_WIDTH_FRACTION <= .29f);
    }

    private static void standalonePanelContainsOnlyItsOwnSurfaceAndControls() {
        check("renderer paints only the Item Animation panel layer", ItemAnimationPanelModel.renderLayers().equals(
                Arrays.asList("item-animation-panel")));
        List<String> controls = ItemAnimationPanelModel.controlLabels();
        String[] required = {"Hook", "Enable", "Scale", "Position", "X", "Y", "Z", "Animation",
                "Custom Hit/Slash Style", "Custom Hit/Slash Speed", "Smooth Swing", "Smoothness", "Options",
                "Remove Haste Effect", "Loop Preview Swing", "Preview Swing", "Reset to Defaults"};
        for (String label : required) check("standalone control retained: " + label, controls.contains(label));
        check("standalone panel excludes old background controls", !ItemAnimationPanelModel.hasRetiredBackgroundControl());
        check("active Hook state has a dedicated green semantic color",
                com.arena.pekaclient.render.Theme.STATUS_SUCCESS != com.arena.pekaclient.render.Theme.ACCENT_BLUE
                        && (com.arena.pekaclient.render.Theme.STATUS_SUCCESS & 0x00FF00) > 0);
        check("failed Hook state has a distinct critical semantic color",
                com.arena.pekaclient.render.Theme.STATUS_CRITICAL
                        != com.arena.pekaclient.render.Theme.STATUS_SUCCESS);
    }

    private static void realPreviewSwingRemainsRendererOnly() {
        ItemAnimation.stopPreviewSwingLoop();
        check("Preview Swing begins only a local renderer timestamp", ItemAnimation.getPreviewSwingStartedNanos() < 0L);
        ItemAnimation.previewSwing();
        check("Preview Swing is available for the real hand renderer", ItemAnimation.getPreviewSwingStartedNanos() > 0L);
        ItemAnimation.stopPreviewSwingLoop();
        check("closing the editor clears the temporary swing", ItemAnimation.getPreviewSwingStartedNanos() < 0L
                && !ItemAnimation.isPreviewSwingLoop());
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++; else { failed++; System.out.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("item animation reachability: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
