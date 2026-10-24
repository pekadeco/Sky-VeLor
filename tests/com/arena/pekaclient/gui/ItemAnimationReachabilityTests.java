package com.arena.pekaclient.gui;

import com.arena.pekaclient.feature.ItemAnimation;

/** Ensures the one permitted renderer editor stays reachable without reviving generic feature editors. */
public final class ItemAnimationReachabilityTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        itemAnimationIsTheOnlyDedicatedRendererEditorRoute();
        realPreviewSwingRemainsRendererOnly();
        done();
    }

    private static void itemAnimationIsTheOnlyDedicatedRendererEditorRoute() {
        ReferenceSettingsCatalog.Setting master = ReferenceSettingsCatalog.find("item-animation");
        ReferenceSettingsCatalog.Setting action = ReferenceSettingsCatalog.find("item-animation-open-editor");
        check("Item Animation retains an inline master dropdown", master != null && master.isDropdown()
                && master.control == ReferenceSettingsCatalog.Control.TOGGLE);
        check("Item Animation opens the permitted editor", action != null
                && action.control == ReferenceSettingsCatalog.Control.ACTION
                && "Item Animation Editor".equals(action.actionLabel));
        check("HUD Editor remains the other permitted editor action", ReferenceSettingsCatalog.find("open-hud-editor") != null
                && "HUD Editor".equals(ReferenceSettingsCatalog.find("open-hud-editor").actionLabel));
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
