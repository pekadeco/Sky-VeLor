package com.arena.pekaclient.itemlockswap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Headless ownership model for the Item Lock and Item Swap dropdown in Quality of Life. */
public final class ItemLockSwapUiModel {

    public static final String SECTION_TITLE = "Item Lock and Item Swap";

    private ItemLockSwapUiModel() {
    }

    public static List<Entry> buildControlModel() {
        ArrayList<Entry> entries = new ArrayList<Entry>();
        entries.add(new Entry("item-lock-swap-master", "Enable Item Lock and Item Swap", true));
        entries.add(new Entry("item-lock-swap-key", "Lock / swap key", false));
        entries.add(new Entry("item-lock-swap-cooldown", "Swap cooldown", false));
        entries.add(new Entry("item-lock-swap-icons", "Show lock and pair icons", true));
        entries.add(new Entry("item-lock-swap-hotbar-icons", "Show icons on hotbar", true));
        entries.add(new Entry("item-lock-swap-messages", "Show blocked-action messages", true));
        entries.add(new Entry("item-lock-swap-icon-color", "Icon color", false));
        entries.add(new Entry("item-lock-swap-icon-opacity", "Icon opacity", false));
        entries.add(new Entry("item-lock-swap-clear-locks", "Clear all locks", false));
        entries.add(new Entry("item-lock-swap-clear-pairs", "Clear all pairs", false));
        return Collections.unmodifiableList(entries);
    }

    public static final class Entry {
        private final String id;
        private final String label;
        private final boolean toggle;

        Entry(String id, String label, boolean toggle) {
            this.id = id;
            this.label = label;
            this.toggle = toggle;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public boolean isToggle() {
            return toggle;
        }
    }
}
