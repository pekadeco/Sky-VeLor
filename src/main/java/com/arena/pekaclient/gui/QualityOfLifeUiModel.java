package com.arena.pekaclient.gui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Minecraft-free ownership contract for the flat Quality of Life hub tab. */
public final class QualityOfLifeUiModel {

    public static final String TAB_LABEL = "Quality of Life";
    public static final String TOGGLE = "toggle";
    public static final String SLIDER = "slider";

    private QualityOfLifeUiModel() { }

    public static final class Entry {
        private final String id;
        private final String label;
        private final String description;
        private final String control;

        private Entry(String id, String label, String description, String control) {
            this.id = id;
            this.label = label;
            this.description = description;
            this.control = control;
        }

        public String getId() { return id; }
        public String getLabel() { return label; }
        public String getDescription() { return description; }
        public boolean isToggle() { return TOGGLE.equals(control); }
        public boolean isSlider() { return SLIDER.equals(control); }
    }

    /** The first rows in the tab: direct controls only, with no nested feature tree. */
    public static List<Entry> buildControlModel() {
        return Collections.unmodifiableList(Arrays.asList(
                new Entry("no-block-particles", "No Block Particles",
                        "Hide only vanilla block digging and crack particles", TOGGLE),
                new Entry("disable-hotbar-scrolling", "No Hotbar Scrolling",
                        "Ignore mouse-wheel hotbar selection during gameplay", TOGGLE),
                new Entry("hurt-camera-shake", "Hurt Camera Shake",
                        "Scale only the local vanilla hurt-camera tilt from 0% to 100%", SLIDER),
                new Entry("auto-sprint", "Auto Sprint",
                        "Sprints automatically while you hold forward.", TOGGLE)
        ));
    }

    /** Kept for source compatibility with the prior model name; the list now also contains one direct slider. */
    public static List<Entry> buildToggleModel() {
        return buildControlModel();
    }

    /** Display-only headings; feature rows themselves remain a single flat list. */
    public static List<String> buildSectionTitles() {
        return Collections.unmodifiableList(Arrays.asList("Simple features", "Feature editors"));
    }
}
