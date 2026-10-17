package com.arena.pekaclient.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Minecraft-free discoverability and compact-panel model shared by the real screens and headless tests. */
public final class ItemAnimationUiModel {

    private static final String HUD_EDITOR_SECTION = "HUD editor";
    private static final String HUD_EDITOR_OPEN = "Open the HUD editor";
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

        public boolean isVisibleWithoutScrolling() {
            return visibleWithoutScrolling;
        }

        public boolean isControlRegistered() {
            return controlRegistered;
        }
    }

    /** Mutable compact-panel expansion state; it has no Minecraft dependency and can be tested headlessly. */
    public static final class PanelState {
        private boolean positionExpanded;
        private boolean animationExpanded;
        private boolean optionsExpanded;

        public boolean isPositionExpanded() {
            return positionExpanded;
        }

        public boolean isAnimationExpanded() {
            return animationExpanded;
        }

        public boolean isOptionsExpanded() {
            return optionsExpanded;
        }

        public void togglePosition() {
            positionExpanded = !positionExpanded;
        }

        public void toggleAnimation() {
            animationExpanded = !animationExpanded;
        }

        public void toggleOptions() {
            optionsExpanded = !optionsExpanded;
        }
    }

    private ItemAnimationUiModel() {
    }

    public static String editorEntryLabel() {
        return EDITOR_ENTRY;
    }

    /** HUD order is intentional: the editor launcher is first, then Item Animation immediately below it. */
    public static List<Entry> buildHudTabModel() {
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(new Entry("hud-editor-section", HUD_EDITOR_SECTION, true, false));
        entries.add(new Entry("hud-editor-open", HUD_EDITOR_OPEN, true, true));
        entries.add(new Entry("item-animation-section", HUD_SECTION, true, false));
        entries.add(new Entry("item-animation-toggle", HUD_TOGGLE, true, true));
        entries.add(new Entry("item-animation-open", HUD_OPEN, true, true));
        entries.add(new Entry("hud-editor-reset", "Reset positions", true, true));
        return Collections.unmodifiableList(entries);
    }

    /** The direct, first HUD-editor target. It never depends on popup scrolling. */
    public static List<Entry> buildHudEditorTargetList() {
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(new Entry("item-animation", editorEntryLabel(), true, true));
        return Collections.unmodifiableList(entries);
    }

    public static List<Entry> buildPanelControlModel() {
        return buildPanelControlModel(new PanelState());
    }

    /** Required controls for the current compact-panel expansion state. */
    public static List<Entry> buildPanelControlModel(PanelState state) {
        PanelState panel = state == null ? new PanelState() : state;
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(new Entry("enable", "Enable Item Animation", true, true));
        entries.add(new Entry("scale", "Scale", true, true));
        entries.add(new Entry("position", "Position", true, true));
        if (panel.isPositionExpanded()) {
            entries.add(new Entry("x", "X", true, true));
            entries.add(new Entry("y", "Y", true, true));
            entries.add(new Entry("z", "Z", true, true));
            entries.add(new Entry("position-reset", "Reset Position", true, true));
        }
        entries.add(new Entry("animation", "Animation", true, true));
        if (panel.isAnimationExpanded()) {
            entries.add(new Entry("hit-slash-style", "Custom Hit/Slash Style", true, true));
            entries.add(new Entry("hit-slash-speed", "Custom Hit/Slash Speed", true, true));
        }
        entries.add(new Entry("options", "Options", true, true));
        if (panel.isOptionsExpanded()) {
            entries.add(new Entry("remove-haste", "Remove Haste Effect", true, true));
            entries.add(new Entry("auto-fix-hit-slash", "Auto Fix Hit/Slash", true, true));
        }
        entries.add(new Entry("preview-swing", "Preview Swing", true, true));
        entries.add(new Entry("reset-defaults", "Reset to Defaults", true, true));
        return Collections.unmodifiableList(entries);
    }

    /** Returns a label from the current compact-panel model. */
    public static String panelControlLabel(String id, PanelState state) {
        for (Entry entry : buildPanelControlModel(state)) {
            if (entry.getId().equals(id)) {
                return entry.getLabel();
            }
        }
        throw new IllegalArgumentException("Unknown Item Animation control: " + id);
    }
}
