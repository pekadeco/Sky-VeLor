package com.arena.pekaclient.gui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Minecraft-free ownership contract for the dedicated Quality of Life hub tab.
 * Keep this list limited to gameplay quality-of-life toggles so those controls cannot be duplicated elsewhere.
 */
public final class QualityOfLifeUiModel {

    public static final String TAB_LABEL = "Quality of Life";

    private QualityOfLifeUiModel() {
    }

    public static final class Entry {
        private final String id;
        private final String label;
        private final String description;

        private Entry(String id, String label, String description) {
            this.id = id;
            this.label = label;
            this.description = description;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public String getDescription() {
            return description;
        }
    }

    /** Exactly the two gameplay toggles owned by the Quality of Life tab. */
    public static List<Entry> buildToggleModel() {
        return Collections.unmodifiableList(Arrays.asList(
                new Entry("disable-hotbar-scrolling", "Disable hotbar scrolling",
                        "Ignore mouse-wheel hotbar selection during gameplay"),
                new Entry("no-block-particles", "No block particles",
                        "Hide only vanilla block digging and crack particles")
        ));
    }
}
