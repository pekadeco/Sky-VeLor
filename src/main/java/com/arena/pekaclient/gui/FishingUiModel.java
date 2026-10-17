package com.arena.pekaclient.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Minecraft-free ownership model for ordinary Fishing-tab feature toggles. */
public final class FishingUiModel {

    /** A Fishing-tab feature may expose its one on/off switch here, but no HUD customization. */
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

        public boolean isMasterToggle() {
            return true;
        }
    }

    private static final List<Entry> FEATURE_TOGGLES = build();

    private FishingUiModel() {
    }

    private static List<Entry> build() {
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(new Entry("catch-counter", "Count sea-creature pulls"));
        entries.add(new Entry("festival-best", "Festival best"));
        entries.add(new Entry("jawbus-arrival-alert", "Arrival alert"));
        entries.add(new Entry("nearby-encounter-alert", "Nearby encounter titles"));
        return Collections.unmodifiableList(entries);
    }

    public static List<Entry> buildFeatureToggleModel() {
        return FEATURE_TOGGLES;
    }

    public static String label(String id) {
        for (Entry entry : FEATURE_TOGGLES) {
            if (entry.getId().equals(id)) {
                return entry.getLabel();
            }
        }
        throw new IllegalArgumentException("Unknown Fishing UI control: " + id);
    }
}
