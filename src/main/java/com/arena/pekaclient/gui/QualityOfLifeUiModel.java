package com.arena.pekaclient.gui;

import com.arena.pekaclient.itemlockswap.ItemLockSwapUiModel;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Minecraft-free ownership contract for the Quality of Life hub tab. Gameplay controls, Commands and Chat QoL are
 * separate collapsible groups on this one page; their detailed settings remain owned by their existing routes.
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

    /** The two gameplay toggles in the Quality of Life page's Gameplay group. */
    public static List<Entry> buildToggleModel() {
        return Collections.unmodifiableList(Arrays.asList(
                new Entry("disable-hotbar-scrolling", "Disable hotbar scrolling",
                        "Ignore mouse-wheel hotbar selection during gameplay"),
                new Entry("no-block-particles", "No block particles",
                        "Hide only vanilla block digging and crack particles")
        ));
    }

    /** Ordered collapsible groups; feature controls remain on this tab rather than becoming top-level tabs. */
    public static List<String> buildSectionTitles() {
        return Collections.unmodifiableList(Arrays.asList("Gameplay", ItemLockSwapUiModel.SECTION_TITLE,
                "Chat QoL", "Commands"));
    }
}
