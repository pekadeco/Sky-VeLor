package com.arena.pekaclient.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Minecraft-free discoverability and compact-panel model shared by the real screens and headless tests. */
public final class ItemAnimationUiModel {

    private static final String QUALITY_OF_LIFE_ITEM_ANIMATION = "Item Animation";

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

    /**
     * The main expandable Quality of Life section exposes one clearly named direct route. The renderer's enable
     * switch and all renderer-only controls remain together in the established Item Animation editor; duplicating
     * them in the Hub would create a second settings surface.
     */
    public static List<Entry> buildQualityOfLifeModel() {
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(new Entry("item-animation-open-editor", QUALITY_OF_LIFE_ITEM_ANIMATION, true, true));
        return Collections.unmodifiableList(entries);
    }

    public static List<Entry> buildPanelControlModel() {
        return buildPanelControlModel(new PanelState());
    }

    /**
     * Required controls for the current compact-panel expansion state. Item Animation is a renderer-only surface:
     * it deliberately owns no HUD background/transparency control in either this panel or the Quality of Life route.
     */
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
            entries.add(new Entry("smooth-swing", "Smooth swing", true, true));
            entries.add(new Entry("smoothness", "Smoothness", true, true));
            // This is information, not a hidden control: duplicating first-person renders with transparent ghost
            // passes is not safe in the narrow ItemRenderer hook, so no blur or intensity option is shipped.
            entries.add(new Entry("swing-blur-status", "Swing blur unavailable (no safe translucent ghosts)", true,
                    false));
        }
        entries.add(new Entry("options", "Options", true, true));
        if (panel.isOptionsExpanded()) {
            entries.add(new Entry("remove-haste", "Remove Haste Effect", true, true));
        }
        entries.add(new Entry("preview-swing-loop", "Loop Preview Swing", true, true));
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
