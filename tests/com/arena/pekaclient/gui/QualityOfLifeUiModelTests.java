package com.arena.pekaclient.gui;

import com.arena.pekaclient.itemlockswap.ItemLockSwapUiModel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Headless ownership proof for the dedicated Quality of Life and scroll-only Mod Settings surfaces. */
public final class QualityOfLifeUiModelTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        List<QualityOfLifeUiModel.Entry> quality = QualityOfLifeUiModel.buildToggleModel();
        List<ModSettingsUiModel.Entry> mod = ModSettingsUiModel.buildScrollModel();
        List<ChatUiModel.Entry> chat = ChatUiModel.buildQolModel();
        List<ItemAnimationUiModel.Entry> hud = ItemAnimationUiModel.buildHudItemAnimationDropdownModel();

        check("Quality of Life has its dedicated tab label", "Quality of Life".equals(QualityOfLifeUiModel.TAB_LABEL));
        check("Quality of Life has exactly its two toggles", quality.size() == 2
                && "disable-hotbar-scrolling".equals(quality.get(0).getId())
                && "no-block-particles".equals(quality.get(1).getId()));
        check("Quality of Life toggles are not duplicated", uniqueQualityIds(quality));
        check("General is the first hub tab", AppleHubScreen.PAGE_NAMES.length == 7
                && "General".equals(AppleHubScreen.PAGE_NAMES[0])
                && "Quality of Life".equals(AppleHubScreen.PAGE_NAMES[1]));
        check("Quality of Life keeps Gameplay, Item Lock/Swap, Chat QoL and Commands as dropdown groups",
                QualityOfLifeUiModel.buildSectionTitles().size() == 4
                        && "Gameplay".equals(QualityOfLifeUiModel.buildSectionTitles().get(0))
                        && ItemLockSwapUiModel.SECTION_TITLE.equals(QualityOfLifeUiModel.buildSectionTitles().get(1))
                        && "Chat QoL".equals(QualityOfLifeUiModel.buildSectionTitles().get(2))
                        && "Commands".equals(QualityOfLifeUiModel.buildSectionTitles().get(3)));
        List<ItemLockSwapUiModel.Entry> itemLockSwap = ItemLockSwapUiModel.buildControlModel();
        check("Item Lock/Swap dropdown owns ten non-duplicated shared-row controls", itemLockSwap.size() == 10
                && uniqueItemLockSwapIds(itemLockSwap)
                && "item-lock-swap-master".equals(itemLockSwap.get(0).getId())
                && "item-lock-swap-clear-pairs".equals(itemLockSwap.get(9).getId()));

        check("Mod Settings retains exactly the scroll controls", mod.size() == 2
                && "scroll-speed".equals(mod.get(0).getId())
                && "smooth-scrolling".equals(mod.get(1).getId()));
        check("Mod Settings has neither Item Animation nor Chat QoL master",
                !containsMod(mod, "item-animation") && !containsMod(mod, "chat-qol-master"));
        check("quality and Mod Settings do not duplicate controls", noCrossSurfaceDuplicates(quality, mod));

        check("Chat QoL retains its own master toggle", containsChatMaster(chat, "chat-qol-master"));
        check("Item Animation remains in the HUD dropdown", containsItemAnimationToggle(hud));

        System.out.println("quality of life UI model: passed " + passed + ", failed " + failed);
        if (failed > 0) {
            throw new AssertionError("quality of life UI model failures: " + failed);
        }
    }

    private static boolean uniqueQualityIds(List<QualityOfLifeUiModel.Entry> entries) {
        Set<String> ids = new HashSet<String>();
        for (QualityOfLifeUiModel.Entry entry : entries) {
            if (!ids.add(entry.getId())) return false;
        }
        return true;
    }

    private static boolean uniqueItemLockSwapIds(List<ItemLockSwapUiModel.Entry> entries) {
        Set<String> ids = new HashSet<String>();
        for (ItemLockSwapUiModel.Entry entry : entries) {
            if (!ids.add(entry.getId())) return false;
        }
        return true;
    }

    private static boolean containsMod(List<ModSettingsUiModel.Entry> entries, String id) {
        for (ModSettingsUiModel.Entry entry : entries) {
            if (id.equals(entry.getId())) return true;
        }
        return false;
    }

    private static boolean noCrossSurfaceDuplicates(List<QualityOfLifeUiModel.Entry> quality,
                                                    List<ModSettingsUiModel.Entry> mod) {
        Set<String> ids = new HashSet<String>();
        for (QualityOfLifeUiModel.Entry entry : quality) ids.add(entry.getId());
        for (ModSettingsUiModel.Entry entry : mod) {
            if (!ids.add(entry.getId())) return false;
        }
        return true;
    }

    private static boolean containsChatMaster(List<ChatUiModel.Entry> entries, String id) {
        for (ChatUiModel.Entry entry : entries) {
            if (id.equals(entry.getId())) return entry.isMasterToggle() && entry.isToggle();
        }
        return false;
    }

    private static boolean containsItemAnimationToggle(List<ItemAnimationUiModel.Entry> entries) {
        for (ItemAnimationUiModel.Entry entry : entries) {
            if ("item-animation-toggle".equals(entry.getId())) return entry.isControlRegistered();
        }
        return false;
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.err.println("FAIL " + name);
        }
    }
}
