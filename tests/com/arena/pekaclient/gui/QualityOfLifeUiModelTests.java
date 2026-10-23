package com.arena.pekaclient.gui;

import com.arena.pekaclient.itemlockswap.ItemLockSwapUiModel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Headless ownership proof for the flat Quality of Life tab and focused editors. */
public final class QualityOfLifeUiModelTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        List<QualityOfLifeUiModel.Entry> quality = QualityOfLifeUiModel.buildControlModel();
        List<ModSettingsUiModel.Entry> mod = ModSettingsUiModel.buildScrollModel();
        List<ChatUiModel.Entry> chat = ChatUiModel.buildQolModel();
        List<ItemAnimationUiModel.Entry> hud = ItemAnimationUiModel.buildHudItemAnimationDropdownModel();

        check("Quality of Life has its dedicated tab label", "Quality of Life".equals(QualityOfLifeUiModel.TAB_LABEL));
        check("Quality of Life starts with direct toggle/slider controls", quality.size() == 4
                && "no-block-particles".equals(quality.get(0).getId()) && quality.get(0).isToggle()
                && "disable-hotbar-scrolling".equals(quality.get(1).getId())
                && "No Hotbar Scrolling".equals(quality.get(1).getLabel())
                && "hurt-camera-shake".equals(quality.get(2).getId()) && quality.get(2).isSlider()
                && "Hurt Camera Shake".equals(quality.get(2).getLabel())
                && "auto-sprint".equals(quality.get(3).getId()) && quality.get(3).isToggle()
                && "Sprints automatically while you hold forward.".equals(quality.get(3).getDescription()));
        check("Quality of Life direct control ids are not duplicated", uniqueQualityIds(quality));
        check("General is the first hub tab", AppleHubScreen.PAGE_NAMES.length == 7
                && "General".equals(AppleHubScreen.PAGE_NAMES[0])
                && "Quality of Life".equals(AppleHubScreen.PAGE_NAMES[1]));
        check("Quality of Life has only simple/features headings",
                QualityOfLifeUiModel.buildSectionTitles().size() == 2
                        && "Simple features".equals(QualityOfLifeUiModel.buildSectionTitles().get(0))
                        && "Feature editors".equals(QualityOfLifeUiModel.buildSectionTitles().get(1)));

        FeatureSettingsUiModel.Feature lock = FeatureSettingsUiModel.feature("item-lock-swap");
        check("Item Lock/Swap Hub dropdown has exactly toggle plus editor", lock != null && lock.isDropdown()
                && lock.tabControls.size() == 2 && "item-lock-swap".equals(lock.editorId));
        List<ItemLockSwapUiModel.Entry> itemLockSwap = ItemLockSwapUiModel.buildControlModel();
        check("Item Lock/Swap editor retains fourteen non-duplicated controls", itemLockSwap.size() == 14
                && uniqueItemLockSwapIds(itemLockSwap)
                && "item-lock-swap-master".equals(itemLockSwap.get(0).getId())
                && "item-lock-swap-clear-pairs".equals(itemLockSwap.get(13).getId()));
        check("Item Lock/Swap editor rows never overlap", hasNonOverlappingEditorRows(itemLockSwap));

        check("Mod Settings retains exactly the scroll controls", mod.size() == 2
                && "scroll-speed".equals(mod.get(0).getId()) && "smooth-scrolling".equals(mod.get(1).getId()));
        check("Mod Settings has neither Item Animation nor Chat QoL master",
                !containsMod(mod, "item-animation") && !containsMod(mod, "chat-qol-master"));
        check("simple QoL and Mod Settings do not duplicate controls", noCrossSurfaceDuplicates(quality, mod));

        check("Chat QoL Hub dropdown has On/Off plus Settings only", chat.size() == 2
                && containsChatMaster(chat, "chat-qol-master") && containsEditor(chat, "open-chat-editor")
                && "On/Off".equals(chat.get(0).getLabel()) && "Settings".equals(chat.get(1).getLabel()));
        check("Item Animation Hub dropdown has its two controls", hud.size() == 2 && containsItemAnimationToggle(hud)
                && containsItemAnimationEditor(hud));

        System.out.println("quality of life UI model: passed " + passed + ", failed " + failed);
        if (failed > 0) throw new AssertionError("quality of life UI model failures: " + failed);
    }

    private static boolean uniqueQualityIds(List<QualityOfLifeUiModel.Entry> entries) {
        Set<String> ids = new HashSet<String>();
        for (QualityOfLifeUiModel.Entry entry : entries) if (!ids.add(entry.getId())) return false;
        return true;
    }

    private static boolean hasNonOverlappingEditorRows(List<ItemLockSwapUiModel.Entry> entries) {
        float top = 0f;
        float previousBottom = -1f;
        for (ItemLockSwapUiModel.Entry ignored : entries) {
            if (top < previousBottom) return false;
            previousBottom = top + 42f;
            top = AppleHubScreen.nextRowTop(top, 42f);
        }
        return true;
    }

    private static boolean uniqueItemLockSwapIds(List<ItemLockSwapUiModel.Entry> entries) {
        Set<String> ids = new HashSet<String>();
        for (ItemLockSwapUiModel.Entry entry : entries) if (!ids.add(entry.getId())) return false;
        return true;
    }

    private static boolean containsMod(List<ModSettingsUiModel.Entry> entries, String id) {
        for (ModSettingsUiModel.Entry entry : entries) if (id.equals(entry.getId())) return true;
        return false;
    }

    private static boolean noCrossSurfaceDuplicates(List<QualityOfLifeUiModel.Entry> quality,
                                                    List<ModSettingsUiModel.Entry> mod) {
        Set<String> ids = new HashSet<String>();
        for (QualityOfLifeUiModel.Entry entry : quality) ids.add(entry.getId());
        for (ModSettingsUiModel.Entry entry : mod) if (!ids.add(entry.getId())) return false;
        return true;
    }

    private static boolean containsChatMaster(List<ChatUiModel.Entry> entries, String id) {
        for (ChatUiModel.Entry entry : entries) if (id.equals(entry.getId())) return entry.isMasterToggle() && entry.isToggle();
        return false;
    }

    private static boolean containsEditor(List<ChatUiModel.Entry> entries, String id) {
        for (ChatUiModel.Entry entry : entries) if (id.equals(entry.getId())) return entry.isEditorRoute();
        return false;
    }

    private static boolean containsItemAnimationToggle(List<ItemAnimationUiModel.Entry> entries) {
        for (ItemAnimationUiModel.Entry entry : entries) {
            if ("item-animation-toggle".equals(entry.getId())) return entry.isControlRegistered();
        }
        return false;
    }

    private static boolean containsItemAnimationEditor(List<ItemAnimationUiModel.Entry> entries) {
        for (ItemAnimationUiModel.Entry entry : entries) {
            if ("item-animation-open-editor".equals(entry.getId())) return entry.isControlRegistered();
        }
        return false;
    }

    private static void check(String name, boolean okay) {
        if (okay) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL " + name);
        }
    }
}
