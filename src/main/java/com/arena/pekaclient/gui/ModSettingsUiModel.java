package com.arena.pekaclient.gui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Minecraft-free ownership contract for the scroll-only Mod Settings screen. */
public final class ModSettingsUiModel {

    private ModSettingsUiModel() {
    }

    public static final class Entry {
        private final String id;
        private final String label;

        private Entry(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }
    }

    /** Scroll settings remain here; feature masters belong to their owning pages. */
    public static List<Entry> buildScrollModel() {
        return Collections.unmodifiableList(Arrays.asList(
                new Entry("scroll-speed", "Scroll speed"),
                new Entry("smooth-scrolling", "Smooth scrolling")
        ));
    }
}
