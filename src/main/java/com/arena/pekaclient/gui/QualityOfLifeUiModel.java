package com.arena.pekaclient.gui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Minecraft-free ownership contract for the flat Quality of Life hub tab. */
public final class QualityOfLifeUiModel {

    public static final String TAB_LABEL = "Quality of Life";

    private QualityOfLifeUiModel() { }

    public static final class Entry {
        private final String id;
        private final String label;
        private final String description;

        private Entry(String id, String label, String description) {
            this.id = id;
            this.label = label;
            this.description = description;
        }

        public String getId() { return id; }
        public String getLabel() { return label; }
        public String getDescription() { return description; }
    }

    /** The first rows in the tab: no dropdowns, no children, only their established feature toggle. */
    public static List<Entry> buildToggleModel() {
        return Collections.unmodifiableList(Arrays.asList(
                new Entry("no-block-particles", "No block particles",
                        "Hide only vanilla block digging and crack particles"),
                new Entry("disable-hotbar-scrolling", "Disable hotbar scrolling",
                        "Ignore mouse-wheel hotbar selection during gameplay"),
                new Entry("no-hurt-cam", "No Hurt Cam",
                        "Removes only the local hurt-camera tilt; damage, knockback, and packets are unchanged"),
                new Entry("auto-sprint", "Auto Sprint",
                        "Sprints automatically while you hold forward.")
        ));
    }

    /** Display-only headings; feature rows themselves remain a single flat list. */
    public static List<String> buildSectionTitles() {
        return Collections.unmodifiableList(Arrays.asList("Simple features", "Feature editors"));
    }
}
