package com.arena.pekaclient.gui;

import java.util.List;

/** Headless Hub/editor discoverability, ownership, compact-panel, and inspector-layout contract. */
public final class ItemAnimationReachabilityTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        List<ItemAnimationUiModel.Entry> hudTab = ItemAnimationUiModel.buildHudTabModel();
        List<ItemAnimationUiModel.Entry> itemAnimationDropdown =
                ItemAnimationUiModel.buildHudItemAnimationDropdownModel();

        check("HUD tab starts with the flat Item Animation dropdown", hasIdAt(hudTab, 0, "item-animation-dropdown"));
        check("HUD tab has no separate nested HUD-editor launcher", hudTab.size() == 1
                && !contains(hudTab, "hud-editor-open"));
        check("Item Animation dropdown contains exactly two rows", itemAnimationDropdown.size() == 2);
        check("dropdown starts with Item Animation ON/OFF", hasIdAt(itemAnimationDropdown, 0, "item-animation-toggle"));
        check("dropdown ends with Open Item Animation Editor",
                hasIdAt(itemAnimationDropdown, 1, "item-animation-open-editor"));
        requireInteractive(itemAnimationDropdown, "item-animation-toggle", "HUD-tab ON/OFF toggle is registered");
        requireInteractive(itemAnimationDropdown, "item-animation-open-editor",
                "HUD-tab Item Animation editor button is registered");
        check("old Open Item Animation Settings label is absent from HUD model",
                !hasLabel(hudTab, "Open Item Animation Settings")
                        && !hasLabel(itemAnimationDropdown, "Open Item Animation Settings"));

        check("Item Animation remains reachable through its direct editor route",
                contains(itemAnimationDropdown, "item-animation-open-editor"));

        ItemAnimationUiModel.PanelState panel = new ItemAnimationUiModel.PanelState();
        List<ItemAnimationUiModel.Entry> collapsed = ItemAnimationUiModel.buildPanelControlModel(panel);
        requireInteractive(collapsed, "position", "collapsed Position group is registered");
        check("Position starts collapsed", !contains(collapsed, "x") && !contains(collapsed, "y")
                && !contains(collapsed, "z"));
        check("Auto Fix option is absent from collapsed Item Animation model",
                !contains(collapsed, "auto-fix-hit-slash") && !hasLabel(collapsed, "Auto Fix Hit/Slash"));
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
                "smooth-swing", "smoothness", "remove-haste", "preview-swing", "reset-defaults"
        };
        for (String id : requiredControls) {
            requireInteractive(expanded, id, "Item Animation control registered: " + id);
        }
        check("Auto Fix option is absent from expanded Item Animation model",
                !contains(expanded, "auto-fix-hit-slash") && !hasLabel(expanded, "Auto Fix Hit/Slash"));
        ItemAnimationUiModel.Entry blurStatus = find(expanded, "swing-blur-status");
        check("blur is plainly reported as unavailable, not shipped as a control", blurStatus != null
                && !blurStatus.isControlRegistered() && blurStatus.getLabel().contains("unavailable"));
        check("no unsafe blur or intensity controls are registered", !contains(expanded, "swing-blur")
                && !contains(expanded, "swing-blur-intensity"));
        check("renderer panel has no background transparency control", !contains(expanded, "background-transparency")
                && !contains(expanded, "background-opacity") && !hasLabel(expanded, "Background transparency")
                && !hasLabel(expanded, "Background opacity"));
        check("HUD dropdown has no Item Animation background transparency duplicate",
                !contains(itemAnimationDropdown, "background-transparency")
                        && !hasLabel(itemAnimationDropdown, "Background transparency"));
        verifyNoForbiddenItemAnimationTerms(hudTab, itemAnimationDropdown);

        verifyNoCustomizationDuplicates();
        verifyFriendFeatureGuiOwnership();
        verifyLayoutAtAllGuiScales();

        System.out.println();
        System.out.println("item animation reachability: passed " + passed + ", failed " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    /**
     * Covers the HUD-tab dropdown/direct route and every expandable Item Animation panel state. No label or control
     * id may reintroduce generic HUD background/transparency/opacity controls into this renderer-only route.
     */
    private static void verifyNoForbiddenItemAnimationTerms(List<ItemAnimationUiModel.Entry> hudTab,
                                                             List<ItemAnimationUiModel.Entry> dropdown) {
        check("HUD-tab Item Animation routes contain no background/opacity/transparency terms",
                noForbiddenTerms(hudTab) && noForbiddenTerms(dropdown));
        for (int mask = 0; mask < 8; mask++) {
            ItemAnimationUiModel.PanelState state = new ItemAnimationUiModel.PanelState();
            if ((mask & 1) != 0) state.togglePosition();
            if ((mask & 2) != 0) state.toggleAnimation();
            if ((mask & 4) != 0) state.toggleOptions();
            check("Item Animation panel expansion " + mask + " contains no background/opacity/transparency terms",
                    noForbiddenTerms(ItemAnimationUiModel.buildPanelControlModel(state)));
        }
    }

    private static boolean noForbiddenTerms(List<ItemAnimationUiModel.Entry> entries) {
        for (ItemAnimationUiModel.Entry entry : entries) {
            String text = (entry.getId() + " " + entry.getLabel()).toLowerCase(java.util.Locale.ROOT);
            if (text.contains("transparency") || text.contains("opacity") || text.contains("background")) {
                return false;
            }
        }
        return true;
    }

    /** The Hub may expose visibility, but every visual customization has exactly one owner: the HUD editor. */
    private static void verifyNoCustomizationDuplicates() {
        for (GuiSettingsAuditModel.Feature feature : GuiSettingsAuditModel.buildFeatureAudit()) {
            check("tab keeps only visibility for " + feature.getTitle(), feature.hasOnlyVisibilityInTab());
            check("no tab/editor customization duplicate for " + feature.getTitle(),
                    !feature.hasDuplicatedCustomization());
        }
    }

    /** Proves the friend's new HUD option stays editor-owned and the chat options stay in the QoL tab model. */
    private static void verifyFriendFeatureGuiOwnership() {
        GuiSettingsAuditModel.Feature fishing = GuiSettingsAuditModel.find("fishing");
        check("AllNonLegendaryCC Fishing tab keeps only visibility", fishing != null && fishing.hasOnlyVisibilityInTab());
        check("AllNonLegendaryCC setting lives in the HUD editor", fishing != null
                && contains(fishing.getEditorCustomizationControls(), "common-creature"));
        check("AllNonLegendaryCC setting is not duplicated in a tab", fishing != null
                && !contains(fishing.getTabControls(), "common-creature"));

        List<ChatUiModel.Entry> chat = ChatUiModel.buildQolModel();
        check("Chat QoL master appears in the flat Hub model", chatEntry(chat, "chat-qol-master", true));
        check("Chat QoL leaves Chat Tabs, smooth messages and hover controls in its editor", findChat(chat, "chat-tabs") == null
                && findChat(chat, "smooth-messages") == null && findChat(chat, "hover-highlight") == null);
        check("Chat QoL keeps context-menu policy in its detailed editor", findChat(chat, "right-click-menu") == null);
        check("Chat QoL has exactly toggle plus editor route", chat.size() == 2
                && findChat(chat, "open-chat-editor") != null && findChat(chat, "open-chat-editor").isEditorRoute());

        List<FishingUiModel.Entry> fishingToggles = FishingUiModel.buildFeatureToggleModel();
        check("nearby encounter alert appears as one Fishing-tab feature toggle",
                fishingToggle(fishingToggles, "nearby-encounter-alert"));
        check("Fishing tab adds no HUD customization duplicate", fishingToggles.size() == 4
                && allFishingEntriesAreMasterToggles(fishingToggles));
    }

    private static boolean fishingToggle(List<FishingUiModel.Entry> entries, String id) {
        for (FishingUiModel.Entry entry : entries) {
            if (id.equals(entry.getId())) {
                return entry.isMasterToggle();
            }
        }
        return false;
    }

    private static boolean allFishingEntriesAreMasterToggles(List<FishingUiModel.Entry> entries) {
        for (FishingUiModel.Entry entry : entries) {
            if (!entry.isMasterToggle()) {
                return false;
            }
        }
        return true;
    }

    private static boolean chatEntry(List<ChatUiModel.Entry> entries, String id, boolean master) {
        for (ChatUiModel.Entry entry : entries) {
            if (id.equals(entry.getId())) {
                return entry.isToggle() && entry.isMasterToggle() == master;
            }
        }
        return false;
    }

    private static ChatUiModel.Entry findChat(List<ChatUiModel.Entry> entries, String id) {
        for (ChatUiModel.Entry entry : entries) {
            if (id.equals(entry.getId())) return entry;
        }
        return null;
    }

    private static boolean contains(String[] entries, String value) {
        for (String entry : entries) {
            if (value.equals(entry)) {
                return true;
            }
        }
        return false;
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

    private static boolean hasLabel(List<ItemAnimationUiModel.Entry> entries, String label) {
        for (ItemAnimationUiModel.Entry entry : entries) {
            if (label.equals(entry.getLabel())) {
                return true;
            }
        }
        return false;
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
