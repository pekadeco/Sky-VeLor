package com.arena.pekaclient.gui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Pure ownership map for options shared between the Hub and HUD editor. A feature may expose its visibility switch
 * in a tab, but visual/customization controls have one owner: the HUD editor. The Hub consumes the titles and
 * descriptions below, and headless tests use the same map to prevent duplicate settings from returning.
 */
public final class GuiSettingsAuditModel {

    private static final String[] COMMON_EDITOR_STYLE = {
            "scale", "opacity", "radius", "text-size", "background", "border", "accent", "reset"
    };

    public static final class Feature {
        private final String id;
        private final String title;
        private final String description;
        private final String[] tabControls;
        private final String[] editorCustomizationControls;

        private Feature(String id, String title, String description, String[] tabControls,
                        String[] editorCustomizationControls) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.tabControls = tabControls.clone();
            this.editorCustomizationControls = editorCustomizationControls.clone();
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        /** The tab may repeat only visibility; customization belongs to the editor. */
        public String[] getTabControls() {
            return tabControls.clone();
        }

        public String[] getEditorCustomizationControls() {
            return editorCustomizationControls.clone();
        }

        public boolean hasDuplicatedCustomization() {
            for (String tab : tabControls) {
                if ("enabled".equals(tab)) {
                    continue;
                }
                for (String editor : editorCustomizationControls) {
                    if (tab.equals(editor)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public boolean hasOnlyVisibilityInTab() {
            return tabControls.length == 1 && "enabled".equals(tabControls[0]);
        }
    }

    private static final List<Feature> FEATURES = Collections.unmodifiableList(Arrays.asList(
            feature("doll", "Player Doll", "A live model of you, with your armour and held item",
                    "spin", "angle"),
            feature("inventory", "Inventory HUD", "The 27 main inventory slots, optional armour",
                    "armour-slots", "transparent", "transparency"),
            feature("mining", "Mining BPS", "Blocks you finish mining per second. Not walking speed",
                    "label", "window", "decimals"),
            feature("dollars", "Player Dollars", "Purse from the scoreboard, or a typed value",
                    "label", "format", "coin-icon", "source", "amount"),
            feature("hotbar", "Hotbar", "The 9 hotbar slots as a separate element"),
            feature("fishing", "Fishing counter", "Legendary catches, Festival and Festival best",
                    "catches", "festival"),
            feature("seahp", "Sea Creature HP", "Nearby sea creatures: name, level and health",
                    "limit-range", "range", "limit-rows", "rows"),
            feature("slayer", "Slayer tracker", "Active Slayer drops, total profit and boss timer",
                    "limit-drops", "drops", "profit", "boss-timer"),
            new Feature("dungeon-map", "Dungeon Map", "Live map overlay with party and puzzle labels",
                    new String[]{"enabled"}, new String[]{"placement", "scale", "zoom", "player-names",
                    "puzzle-names", "party-announce"}),
            new Feature("item-animation", "Item Animation", "Visual first-person transforms only",
                    new String[]{"enabled"}, new String[]{"scale", "position", "hit-slash-style",
                    "hit-slash-speed", "remove-haste", "preview", "reset"})
    ));

    private GuiSettingsAuditModel() {
    }

    private static Feature feature(String id, String title, String description, String... specificControls) {
        String[] editor = new String[specificControls.length + COMMON_EDITOR_STYLE.length];
        System.arraycopy(specificControls, 0, editor, 0, specificControls.length);
        System.arraycopy(COMMON_EDITOR_STYLE, 0, editor, specificControls.length, COMMON_EDITOR_STYLE.length);
        return new Feature(id, title, description, new String[]{"enabled"}, editor);
    }

    public static List<Feature> buildFeatureAudit() {
        return FEATURES;
    }

    public static Feature find(String id) {
        for (Feature feature : FEATURES) {
            if (feature.id.equals(id)) {
                return feature;
            }
        }
        return null;
    }
}
