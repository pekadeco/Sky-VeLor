package com.arena.pekaclient.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Small, Minecraft-free description of every Item Animation entry that must be reachable in the hub and HUD
 * editor. The real screens consume these labels; the standalone reachability test consumes the same model without
 * constructing a game window.
 */
public final class ItemAnimationUiModel {

    private static final String HUD_SECTION = "Item Animation";
    private static final String HUD_TOGGLE = "Item Animation ON/OFF";
    private static final String HUD_OPEN = "Open Item Animation Settings";
    private static final String EDITOR_ENTRY = "Item Animation";

    /** One discoverability or panel-control entry. */
    public static final class Entry {
        private final String id;
        private final String label;
        private final boolean visibleWithoutScrolling;
        private final boolean controlRegistered;

        private Entry(String id, String label, boolean visibleWithoutScrolling, boolean controlRegistered) {
            this.id = id;
            this.label = label;
            this.visibleWithoutScrolling = visibleWithoutScrolling;
            this.controlRegistered = controlRegistered;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        /** True only for entries placed before any scroll-only content in their owning view. */
        public boolean isVisibleWithoutScrolling() {
            return visibleWithoutScrolling;
        }

        /** True when the entry has a live button, toggle, slider, or dropdown in the real screen. */
        public boolean isControlRegistered() {
            return controlRegistered;
        }
    }

    private ItemAnimationUiModel() {
    }

    public static String hudSectionTitle() {
        return HUD_SECTION;
    }

    public static String hudToggleLabel() {
        return HUD_TOGGLE;
    }

    public static String hudOpenLabel() {
        return HUD_OPEN;
    }

    public static String editorEntryLabel() {
        return EDITOR_ENTRY;
    }

    /** The top of the HUD tab. Both interactive entries are intentionally above the generic HUD editor group. */
    public static List<Entry> buildHudTabModel() {
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(new Entry("item-animation-section", hudSectionTitle(), true, false));
        entries.add(new Entry("item-animation-toggle", hudToggleLabel(), true, true));
        entries.add(new Entry("item-animation-open", hudOpenLabel(), true, true));
        return Collections.unmodifiableList(entries);
    }

    /** The direct, first HUD-editor card entry. It is not inside the capped chooser popup. */
    public static List<Entry> buildHudEditorTargetList() {
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(new Entry("item-animation", editorEntryLabel(), true, true));
        return Collections.unmodifiableList(entries);
    }

    /**
     * Item Animation inspector controls. These identifiers are also the contract checked by the headless CI test;
     * labels are resolved by {@link #panelControlLabel(String)} in the actual editor.
     */
    public static List<Entry> buildPanelControlModel() {
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(new Entry("enable", "Enable Item Animation", true, true));
        entries.add(new Entry("scale", "Scale", true, true));
        entries.add(new Entry("x", "X offset (1/100)", true, true));
        entries.add(new Entry("y", "Y offset (1/100)", true, true));
        entries.add(new Entry("z", "Z offset (1/100)", true, true));
        entries.add(new Entry("remove-haste", "Remove Haste Effect", true, true));
        entries.add(new Entry("auto-fix-hit-slash", "Auto Fix Hit/Slash", true, true));
        entries.add(new Entry("hit-slash-style", "Custom Hit/Slash Style", true, true));
        entries.add(new Entry("hit-slash-speed", "Custom Hit/Slash Speed", true, true));
        entries.add(new Entry("preview-swing", "Preview Swing", true, true));
        entries.add(new Entry("reset-defaults", "Reset to Defaults", true, true));
        return Collections.unmodifiableList(entries);
    }

    /** Returns the visible editor label for a required Item Animation control id. */
    public static String panelControlLabel(String id) {
        for (Entry entry : buildPanelControlModel()) {
            if (entry.getId().equals(id)) {
                return entry.getLabel();
            }
        }
        throw new IllegalArgumentException("Unknown Item Animation control: " + id);
    }
}
