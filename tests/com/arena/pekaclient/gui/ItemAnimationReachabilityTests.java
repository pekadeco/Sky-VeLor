package com.arena.pekaclient.gui;

import java.util.List;

/**
 * Headless discoverability contract for Item Animation. This deliberately uses the Minecraft-free UI model rather
 * than constructing a GuiScreen, so CI can prove the hub and editor registration without opening a game window.
 */
public final class ItemAnimationReachabilityTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        List<ItemAnimationUiModel.Entry> hudTab = ItemAnimationUiModel.buildHudTabModel();
        List<ItemAnimationUiModel.Entry> editorTargets = ItemAnimationUiModel.buildHudEditorTargetList();
        List<ItemAnimationUiModel.Entry> panelControls = ItemAnimationUiModel.buildPanelControlModel();

        check("HUD tab starts with Item Animation section",
                !hudTab.isEmpty() && "item-animation-section".equals(hudTab.get(0).getId())
                        && hudTab.get(0).isVisibleWithoutScrolling());
        requireInteractive(hudTab, "item-animation-toggle", "HUD-tab ON/OFF toggle is visible and registered");
        requireInteractive(hudTab, "item-animation-open", "HUD-tab direct settings button is visible and registered");

        check("HUD editor starts with direct Item Animation target",
                !editorTargets.isEmpty() && "item-animation".equals(editorTargets.get(0).getId())
                        && editorTargets.get(0).isVisibleWithoutScrolling()
                        && editorTargets.get(0).isControlRegistered());

        String[] requiredControls = {
                "enable", "scale", "x", "y", "z", "remove-haste", "auto-fix-hit-slash", "hit-slash-style",
                "hit-slash-speed", "preview-swing", "reset-defaults"
        };
        for (String id : requiredControls) {
            requireInteractive(panelControls, id, "Item Animation control registered: " + id);
        }

        System.out.println();
        System.out.println("item animation reachability: passed " + passed + ", failed " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void requireInteractive(List<ItemAnimationUiModel.Entry> entries, String id, String name) {
        ItemAnimationUiModel.Entry entry = find(entries, id);
        check(name, entry != null && entry.isControlRegistered());
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
