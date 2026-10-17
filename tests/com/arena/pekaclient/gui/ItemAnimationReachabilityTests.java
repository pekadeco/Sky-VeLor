package com.arena.pekaclient.gui;

import java.util.List;

/** Headless discoverability, compact-panel, and inspector-layout contract. No game window is constructed. */
public final class ItemAnimationReachabilityTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        List<ItemAnimationUiModel.Entry> hudTab = ItemAnimationUiModel.buildHudTabModel();
        List<ItemAnimationUiModel.Entry> editorTargets = ItemAnimationUiModel.buildHudEditorTargetList();

        check("HUD editor section is first in HUD tab", hasIdAt(hudTab, 0, "hud-editor-section"));
        check("HUD editor button is the first HUD-tab entry", hasIdAt(hudTab, 1, "hud-editor-open"));
        requireInteractive(hudTab, "hud-editor-open", "HUD editor button is visible and registered");
        check("Item Animation section is directly below HUD editor button", hasIdAt(hudTab, 2, "item-animation-section"));
        requireInteractive(hudTab, "item-animation-toggle", "HUD-tab ON/OFF toggle is visible and registered");
        requireInteractive(hudTab, "item-animation-open", "HUD-tab direct settings button is visible and registered");

        check("HUD editor starts with direct Item Animation target",
                !editorTargets.isEmpty() && "item-animation".equals(editorTargets.get(0).getId())
                        && editorTargets.get(0).isVisibleWithoutScrolling()
                        && editorTargets.get(0).isControlRegistered());

        ItemAnimationUiModel.PanelState panel = new ItemAnimationUiModel.PanelState();
        List<ItemAnimationUiModel.Entry> collapsed = ItemAnimationUiModel.buildPanelControlModel(panel);
        requireInteractive(collapsed, "position", "collapsed Position group is registered");
        check("Position starts collapsed", !contains(collapsed, "x") && !contains(collapsed, "y")
                && !contains(collapsed, "z"));
        panel.togglePosition();
        List<ItemAnimationUiModel.Entry> positionExpanded = ItemAnimationUiModel.buildPanelControlModel(panel);
        requireInteractive(positionExpanded, "x", "expanded Position X row is registered");
        requireInteractive(positionExpanded, "y", "expanded Position Y row is registered");
        requireInteractive(positionExpanded, "z", "expanded Position Z row is registered");
        requireInteractive(positionExpanded, "position-reset", "expanded Position reset is registered");
        panel.togglePosition();
        check("Position collapses again", !contains(ItemAnimationUiModel.buildPanelControlModel(panel), "x"));

        panel.toggleAnimation();
        panel.toggleOptions();
        List<ItemAnimationUiModel.Entry> expanded = ItemAnimationUiModel.buildPanelControlModel(panel);
        String[] requiredControls = {
                "enable", "scale", "position", "animation", "options", "hit-slash-style", "hit-slash-speed",
                "remove-haste", "auto-fix-hit-slash", "preview-swing", "reset-defaults"
        };
        for (String id : requiredControls) {
            requireInteractive(expanded, id, "Item Animation control registered: " + id);
        }

        verifyLayoutAtAllGuiScales();

        System.out.println();
        System.out.println("item animation reachability: passed " + passed + ", failed " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    /** Covers Small, Normal, Large, and the 854x480 small-window geometry after Minecraft GUI scaling. */
    private static void verifyLayoutAtAllGuiScales() {
        int[][] scales = {
                {854, 480}, {427, 240}, {284, 160}, {213, 120}
        };
        for (int[] scale : scales) {
            HudEditorLayout.Metrics metrics = new HudEditorLayout.Metrics();
            float windowW = Math.max(120f, scale[0] - 12f);
            float windowH = Math.max(120f, scale[1] - 12f);
            HudEditorLayout.update(metrics, 6f, 6f, windowW, windowH, 44f, 20, 0f, false, 0f, 0f);
            check("card remains on-screen at " + scale[0] + "x" + scale[1],
                    metrics.cardX >= 6f && metrics.cardY >= 6f
                            && metrics.cardX + metrics.cardW <= 6f + windowW + 0.01f
                            && metrics.cardY + metrics.cardH <= 6f + windowH + 0.01f);
            check("scrollbar exists for tall panel at " + scale[0] + "x" + scale[1], metrics.maxScroll > 0f);

            HudEditorLayout.Bounds previous = null;
            for (int index = 0; index < 20; index++) {
                HudEditorLayout.Bounds row = new HudEditorLayout.Bounds();
                boolean fullWidth = index % 4 == 0;
                HudEditorLayout.row(metrics, index, fullWidth, 150f, !fullWidth, 22f, row);
                check("row control stays inside panel at " + scale[0] + "x" + scale[1] + " #" + index,
                        row.controlX >= metrics.cardX && row.controlX + row.controlW <= metrics.cardX + metrics.cardW);
                if (!fullWidth) {
                    check("label and control do not overlap at " + scale[0] + "x" + scale[1] + " #" + index,
                            row.labelX + row.labelW <= row.controlX);
                }
                if (previous != null) {
                    check("rows do not overlap at " + scale[0] + "x" + scale[1] + " #" + index,
                            !HudEditorLayout.intersects(previous, row));
                }
                previous = row;
            }

            HudEditorLayout.update(metrics, 6f, 6f, windowW, windowH, 44f, 20, metrics.maxScroll,
                    false, 0f, 0f);
            check("max-scroll remains clamped at " + scale[0] + "x" + scale[1],
                    metrics.scroll >= 0f && metrics.scroll <= metrics.maxScroll);
        }
    }

    private static boolean hasIdAt(List<ItemAnimationUiModel.Entry> entries, int index, String id) {
        return entries.size() > index && id.equals(entries.get(index).getId())
                && entries.get(index).isVisibleWithoutScrolling();
    }

    private static void requireInteractive(List<ItemAnimationUiModel.Entry> entries, String id, String name) {
        ItemAnimationUiModel.Entry entry = find(entries, id);
        check(name, entry != null && entry.isControlRegistered());
    }

    private static boolean contains(List<ItemAnimationUiModel.Entry> entries, String id) {
        return find(entries, id) != null;
    }

    private static ItemAnimationUiModel.Entry find(List<ItemAnimationUiModel.Entry> entries, String id) {
        for (ItemAnimationUiModel.Entry entry : entries) {
            if (id.equals(entry.getId())) {
                return entry;
            }
        }
        return null;
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL " + name);
        }
    }
}
