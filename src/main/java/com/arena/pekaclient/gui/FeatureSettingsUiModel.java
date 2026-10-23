package com.arena.pekaclient.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Auditable, Minecraft-free inventory of settings presentation after the compact-tree rework.
 *
 * <p>This is deliberately a presentation registry, not a second configuration model. Its option ids name the
 * already-existing controls/configuration owners and prove that each old surface remains reachable through either
 * a simple inline row or a named editor route.</p>
 */
public final class FeatureSettingsUiModel {
    public static final String INLINE = "inline";
    public static final String EDITOR = "editor";
    public static final String EXTERNAL_EDITOR = "external-editor";

    public static final class Option {
        public final String id;
        public final String label;
        public final String route;
        private Option(String id, String label, String route) {
            this.id = id;
            this.label = label;
            this.route = route;
        }
    }

    public static final class Feature {
        public final String id;
        public final String tab;
        public final String parentId;
        public final String title;
        public final String description;
        public final String presentation;
        public final String editorId;
        public final List<String> optionIds;

        private Feature(String id, String tab, String parentId, String title, String description,
                        String presentation, String editorId, String... optionIds) {
            this.id = id;
            this.tab = tab;
            this.parentId = parentId;
            this.title = title;
            this.description = description;
            this.presentation = presentation;
            this.editorId = editorId;
            this.optionIds = Collections.unmodifiableList(Arrays.asList(optionIds));
        }

        public boolean usesEditor() { return !INLINE.equals(presentation); }
    }

    /** One concise entry suitable for the requested before/after reachability document. */
    public static final class AuditRow {
        public final String tab;
        public final String feature;
        public final String before;
        public final String after;
        private AuditRow(String tab, String feature, String before, String after) {
            this.tab = tab;
            this.feature = feature;
            this.before = before;
            this.after = after;
        }
    }

    private static final Map<String, Option> OPTIONS = new LinkedHashMap<String, Option>();
    private static final List<Feature> FEATURES = new ArrayList<Feature>();

    static {
        // General
        addFeature("appearance", "General", null, "Appearance", "Theme, accent, corners and panel opacity.", EDITOR,
                "appearance", options("dark-mode", "Accent color", "accent-color", "Corner radius switch", "corner-radius-on",
                "Corner radius", "corner-radius", "Panel opacity switch", "panel-opacity-on", "Panel opacity", "panel-opacity"));
        addFeature("mod-settings", "General", null, "Mod Settings", "Global scroll speed and smoothing.", EXTERNAL_EDITOR,
                "mod-settings", options("Global scroll speed", "global-scroll", "Smooth scrolling", "smooth-scroll"));
        addFeature("fonts", "General", null, "Fonts", "Font selection, preview, licenses and safe fallback.", EXTERNAL_EDITOR,
                "fonts", options("Font selection", "font-selection", "Font preview", "font-preview", "Font licenses", "font-licenses"));
        addFeature("performance", "General", null, "Performance", "Curve, shadows, animation and adaptive GUI quality.", EDITOR,
                "performance", options("Curve quality", "curve-quality", "Soft shadows", "soft-shadows", "Animations", "animations",
                "Animation speed switch", "animation-speed-on", "Animation speed", "animation-speed", "Adaptive quality", "adaptive-quality",
                "Performance Boost Beta", "performance-boost"));

        // QoL
        addFeature("qol-gameplay", "Quality of Life", null, "Gameplay", "Two small client-side gameplay switches.", INLINE,
                "", options("Disable hotbar scrolling", "disable-hotbar-scrolling", "No block particles", "no-block-particles"));
        addFeature("inventory-assist", "Quality of Life", null, "Inventory Assist", "Search, tooltip and inventory visual helpers.", EDITOR,
                "inventory-assist", options("Inventory search and filter", "inventory-search", "Hide potion effects in inventory", "hide-potions",
                "Roman numerals in tooltips", "roman-numerals", "Scrollable item tooltips", "tooltip-scroll", "Item rarity highlight", "rarity-highlight"));
        addFeature("item-protection", "Quality of Life", null, "Item Protection", "Protect selected UUID items and show status.", EDITOR,
                "item-protection", options("Item Protection", "item-guard", "Item Protection notifications", "item-guard-notifications",
                "Item Protection status", "item-guard-status"));
        addFeature("movement-shortcuts", "Quality of Life", null, "Movement and Shortcuts", "Sprint, wardrobe and pets helpers.", EDITOR,
                "movement-shortcuts", options("Auto sprint", "auto-sprint", "Wardrobe shortcut", "wardrobe-shortcut", "Pets shortcut", "pets-shortcut"));
        addFeature("item-lock-swap", "Quality of Life", null, "Item Lock & Swap", "Pair locks, indicators and swap-line controls.", EDITOR,
                "item-lock-swap", options("Item Lock & Swap", "item-lock-swap-enabled", "Item Lock & Swap key", "item-lock-swap-key",
                "Swap cooldown", "item-lock-swap-cooldown", "Show lock icons", "item-lock-show-icons", "Show icons on hotbar", "item-lock-hotbar-icons",
                "Blocked message", "item-lock-message", "Badge tint", "item-lock-color", "Badge transparency", "item-lock-opacity",
                "Swap line mode", "item-swap-line-mode", "Swap line tint", "item-swap-line-color", "Swap line transparency", "item-swap-line-opacity",
                "Swap line thickness", "item-swap-line-width", "Clear locks", "item-lock-clear", "Clear pairs", "item-swap-clear"));
        addFeature("chat-qol", "Quality of Life", null, "Chat QoL", "Attached tabs, message animation and highlighting.", EXTERNAL_EDITOR,
                "chat", options("Chat QoL master", "chat-master", "Chat tabs", "chat-tabs", "Smooth messages", "chat-smooth",
                "Hover highlight", "chat-hover", "Chat editor", "chat-editor"));
        addFeature("commands", "Quality of Life", null, "Commands", "Optional /sv and /qn command routes plus diagnostics.", EDITOR,
                "commands", options("/sv", "command-sv", "/qn", "command-qn", "/velor itemdebug", "command-itemdebug",
                "/velor lockdebug", "command-lockdebug", "/velor selftest", "command-selftest"));

        // HUD and farming
        addFeature("hud-editor", "HUD", null, "HUD Editor", "Drag, resize and style HUD elements on screen.", EXTERNAL_EDITOR,
                "hud", options("Open HUD Editor", "hud-editor"));
        addFeature("item-animation", "HUD", null, "Item Animation", "Visual first-person transform controls.", EXTERNAL_EDITOR,
                "item-animation", options("Item Animation", "item-animation-enabled", "Open Item Animation Editor", "item-animation-editor"));
        addFeature("hud-elements", "HUD", null, "HUD Elements", "Every HUD element keeps visibility and its editor route.", EXTERNAL_EDITOR,
                "hud", options("HUD element visibility", "hud-element-visible", "HUD element editor", "hud-element-editor"));
        addFeature("mining-bps", "Farming", null, "Mining BPS", "Mining block-break-speed HUD.", EXTERNAL_EDITOR,
                "hud", options("Mining BPS visibility", "mining-bps-visible", "Mining BPS editor", "mining-bps-editor"));
        addFeature("mining-assistant", "Farming", null, "Mining Assistant", "Alerts, puzzler marker and assistant HUD.", EDITOR,
                "mining-assistant", options("Pickaxe ability ready alert", "mining-ability-alert", "Puzzler direction helper", "mining-puzzler",
                "Mining Assistant HUD", "mining-assistant-hud", "Mining Assistant editor", "mining-assistant-editor"));

        // Dungeon
        addFeature("spirit-leap", "Dungeon", null, "Spirit Leap", "Spirit Leap menu, key shortcut and preview.", EDITOR,
                "spirit-leap", options("Spirit Leap menu", "spirit-leap-menu", "Spirit Leap key", "spirit-leap-key", "Spirit Leap preview", "spirit-leap-preview"));
        addFeature("dungeon-map", "Dungeon", null, "Dungeon Map", "Dungeon map visibility and in-world editor.", EXTERNAL_EDITOR,
                "hud", options("Dungeon map", "dungeon-map", "Dungeon map editor", "dungeon-map-editor"));
        addFeature("dungeon-sync", "Dungeon", null, "Dungeon Sync", "Two small sync switches.", INLINE,
                "", options("Enable Dungeon Sync", "dungeon-sync-enabled", "Dungeon Sync Debug", "dungeon-sync-debug"));
        addFeature("terminal-assist", "Dungeon", null, "Terminal Assist", "Terminal and Floor VII chest assistance.", EDITOR,
                "terminal-assist", options("Terminal visual helpers", "terminal-hints", "Terminal auto click", "terminal-auto-click",
                "Hide incorrect terminal choices", "terminal-hide-incorrect", "Prevent wrong terminal clicks", "terminal-prevent-wrong",
                "Terminal waypoints", "terminal-waypoints", "Terminal scale", "terminal-scale"));
        addFeature("mob-esp", "Dungeon", null, "Mob ESP", "Per-mob hitbox styles, global scan controls and presets.", EDITOR,
                "mob-esp", options("Enable Dungeon ESP", "mob-esp-master",
                "Starred Mob ESP", "mob-starred-enabled", "Starred Mob Color", "mob-starred-color", "Starred Mob Render Mode", "mob-starred-mode", "Starred Mob Through Walls", "mob-starred-walls", "Starred Mob Outline Width", "mob-starred-width", "Starred Mob Outline Opacity", "mob-starred-outline", "Starred Mob Fill Opacity", "mob-starred-fill",
                "Miniboss ESP", "mob-miniboss-enabled", "Miniboss Color", "mob-miniboss-color", "Miniboss Render Mode", "mob-miniboss-mode", "Miniboss Through Walls", "mob-miniboss-walls", "Miniboss Outline Width", "mob-miniboss-width", "Miniboss Outline Opacity", "mob-miniboss-outline", "Miniboss Fill Opacity", "mob-miniboss-fill",
                "Special Starred Mob ESP", "mob-special-enabled", "Special Starred Mob Color", "mob-special-color", "Special Starred Mob Render Mode", "mob-special-mode", "Special Starred Mob Through Walls", "mob-special-walls", "Special Starred Mob Outline Width", "mob-special-width", "Special Starred Mob Outline Opacity", "mob-special-outline", "Special Starred Mob Fill Opacity", "mob-special-fill",
                "Enderman ESP", "mob-enderman-enabled", "Enderman Color", "mob-enderman-color", "Enderman Render Mode", "mob-enderman-mode", "Enderman Through Walls", "mob-enderman-walls", "Enderman Outline Width", "mob-enderman-width", "Enderman Outline Opacity", "mob-enderman-outline", "Enderman Fill Opacity", "mob-enderman-fill",
                "Skeletor ESP", "mob-skeletor-enabled", "Skeletor Color", "mob-skeletor-color", "Skeletor Render Mode", "mob-skeletor-mode", "Skeletor Through Walls", "mob-skeletor-walls", "Skeletor Outline Width", "mob-skeletor-width", "Skeletor Outline Opacity", "mob-skeletor-outline", "Skeletor Fill Opacity", "mob-skeletor-fill",
                "Force Render All Mobs in Room", "mob-force-room", "Other Mobs ESP", "mob-other-enabled", "Other Mobs Color", "mob-other-color", "Other Mobs Render Mode", "mob-other-mode", "Other Mobs Through Walls", "mob-other-walls", "Other Mobs Outline Width", "mob-other-width", "Other Mobs Outline Opacity", "mob-other-outline", "Other Mobs Fill Opacity", "mob-other-fill", "Include Bats", "mob-include-bats", "Include Invisible Mobs", "mob-include-invisible",
                "Maximum ESP Distance", "mob-distance", "Update Interval", "mob-interval", "Hide Zero-Health Mobs", "mob-hide-zero", "Tracers", "mob-tracers", "Distance Text", "mob-distance-text", "Maximum Boxes", "mob-max-boxes", "Default Preset", "mob-default-preset", "Subtle Preset", "mob-subtle-preset", "Reset Dungeon ESP", "mob-reset", "Copy Style", "mob-copy-style", "Apply Copied Style", "mob-apply-style"));
        addFeature("door-esp", "Dungeon", null, "Door ESP", "Wither and blood door hitbox styles, filters and preview.", EDITOR,
                "door-esp", options("Show Closed Doors", "door-esp-master", "Door Scope", "door-esp-scope", "Only With a Wither Key", "door-esp-key", "Blood Doors", "door-esp-blood", "Blood Door Color", "door-esp-blood-color", "Enable Door Style", "door-esp-style-enabled", "Wither Door Color", "door-esp-wither-color", "Door Render Mode", "door-esp-mode", "Doors Through Walls", "door-esp-through-walls", "Door Outline Width", "door-esp-width", "Door Outline Opacity", "door-esp-outline-opacity", "Door Fill Opacity", "door-esp-fill-opacity", "Door Distance Text", "door-esp-distance-text", "Default Preset", "door-esp-default-preset", "Subtle Preset", "door-esp-subtle-preset", "Reset Door ESP", "door-esp-reset", "Copy Style", "door-esp-copy", "Apply Copied Style", "door-esp-apply"));
        addFeature("m7-overlay", "Dungeon", null, "Floor VII Overlay", "Areas, alerts, boss and dragon hitboxes.", EDITOR,
                "m7-overlay", options("M7 overlay", "m7-overlay", "Relic kill areas", "m7-relic-areas", "Keep areas after dragons", "m7-areas-persist",
                "Dragon kill alert", "m7-dragon-alert", "Boss hitboxes", "m7-boss-hitboxes", "Dragon hitboxes", "m7-dragon-hitboxes"));

        // Slayer and fishing
        addFeature("slayer-tracker", "Slayer", null, "Slayer Tracker", "Drop history tracker and HUD route.", EXTERNAL_EDITOR,
                "hud", options("Track slayer drops", "slayer-tracker", "Slayer HUD", "slayer-hud", "Slayer HUD editor", "slayer-hud-editor"));
        addFeature("slayer-simple", "Slayer", null, "Boss Timer and Prices", "Timer, price switch and price command help.", EDITOR,
                "slayer-simple", options("Boss timer", "slayer-boss-timer", "Price drops", "slayer-prices", "Set a price", "slayer-set-price"));
        addFeature("slayer-history", "Slayer", null, "Clear Slayer History", "Clear individual slayer drop histories.", EDITOR,
                "slayer-history", options("Clear Revenant history", "slayer-clear-revenant", "Clear Tarantula history", "slayer-clear-tarantula",
                "Clear Sven history", "slayer-clear-sven", "Clear Voidgloom history", "slayer-clear-voidgloom", "Clear Blaze history", "slayer-clear-blaze"));
        addFeature("catch-counter", "Fishing", null, "Catch Counter", "Catch counts, Festival best and reset.", EDITOR,
                "catch-counter", options("Catch counter", "fishing-counter", "Festival best", "fishing-festival", "Reset counters", "fishing-reset"));
        addFeature("jawbus", "Fishing", null, "Lord Jawbus", "Arrival title alert.", INLINE,
                "", options("Jawbus arrival alert", "jawbus-alert"));
        addFeature("nearby-encounters", "Fishing", null, "Nearby Encounters", "Local compatible encounter title alert.", INLINE,
                "", options("Nearby encounter alert", "nearby-encounter-alert"));
        addFeature("fishing-huds", "Fishing", null, "Fishing HUDs", "Catch counter and sea-creature HP HUD routes.", EXTERNAL_EDITOR,
                "hud", options("Fishing HUD", "fishing-hud", "Fishing HUD editor", "fishing-hud-editor", "Sea HP HUD", "sea-hp-hud", "Sea HP editor", "sea-hp-editor"));
    }

    private FeatureSettingsUiModel() { }

    private static String[] options(String... labelIdPairs) { return labelIdPairs; }

    private static void addFeature(String id, String tab, String parentId, String title, String description,
                                   String presentation, String editorId, String... labelIdPairs) {
        List<String> ids = new ArrayList<String>();
        for (int i = 0; i + 1 < labelIdPairs.length; i += 2) {
            String label = labelIdPairs[i];
            String optionId = labelIdPairs[i + 1];
            OPTIONS.put(optionId, new Option(optionId, label, presentation));
            ids.add(optionId);
        }
        FEATURES.add(new Feature(id, tab, parentId, title, description, presentation, editorId,
                ids.toArray(new String[ids.size()])));
    }

    public static List<Feature> features() { return Collections.unmodifiableList(FEATURES); }
    public static Option option(String id) { return OPTIONS.get(id); }
    public static List<Option> options() { return Collections.unmodifiableList(new ArrayList<Option>(OPTIONS.values())); }

    /** True when a former human-facing control label is still owned by a compact row/editor route. */
    public static boolean hasOptionLabel(String label) {
        if (label == null) return false;
        for (Option option : OPTIONS.values()) if (label.equals(option.label)) return true;
        return false;
    }

    public static Feature feature(String id) {
        for (Feature feature : FEATURES) if (feature.id.equals(id)) return feature;
        return null;
    }

    public static List<Feature> tab(String name) {
        List<Feature> out = new ArrayList<Feature>();
        for (Feature feature : FEATURES) if (feature.tab.equals(name)) out.add(feature);
        return out;
    }

    public static List<AuditRow> auditRows() {
        List<AuditRow> rows = new ArrayList<AuditRow>();
        for (Feature feature : FEATURES) {
            String before = feature.optionIds.size() + " inline option" + (feature.optionIds.size() == 1 ? "" : "s");
            String after = INLINE.equals(feature.presentation) ? "short inline tree branch"
                    : (EXTERNAL_EDITOR.equals(feature.presentation) ? "existing editor route" : "Feature Settings Editor: " + feature.editorId);
            rows.add(new AuditRow(feature.tab, feature.title, before, after));
        }
        return Collections.unmodifiableList(rows);
    }

    /** Every registry option has exactly one route and every feature obeys the simple-inline cap. */
    public static boolean isReachabilityValid() {
        for (Feature feature : FEATURES) {
            if (feature.optionIds.isEmpty()) return false;
            if (INLINE.equals(feature.presentation) && feature.optionIds.size() > 2) return false;
            if (!INLINE.equals(feature.presentation) && (feature.editorId == null || feature.editorId.length() == 0)) return false;
            for (String id : feature.optionIds) if (!OPTIONS.containsKey(id)) return false;
        }
        return OPTIONS.size() > 80;
    }
}
