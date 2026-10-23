package com.arena.pekaclient.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minecraft-free audit of the single expandable Hub menu.
 *
 * <p>This is a presentation inventory, never a second settings schema. It records every existing option's route:
 * direct rows own one existing toggle or slider, while a focused editor route owns the detailed controls. Main Hub
 * sections expand one level only; features themselves do not create nested dropdowns or status-dot navigation.</p>
 */
public final class FeatureSettingsUiModel {
    public static final String SIMPLE = "simple";
    public static final String EDITOR_ROUTE = "editor-route";
    /** @deprecated The Hub no longer presents feature dropdowns. Use {@link #EDITOR_ROUTE}. */
    @Deprecated
    public static final String DROPDOWN = EDITOR_ROUTE;
    public static final String TOGGLE = "toggle";
    public static final String SLIDER = "slider";
    public static final String EDITOR_BUTTON = "editor-button";

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

    /** One direct feature row in an expandable section. Detailed controls live only in their focused editor. */
    public static final class Feature {
        public final String id;
        /** Logical section name retained for configuration/audit compatibility. */
        public final String tab;
        /** Alias for new callers; this is an expandable section, not a tab. */
        public final String section;
        public final String title;
        public final String description;
        public final String rowType;
        public final String editorId;
        public final String toggleOptionId;
        public final String editorButtonLabel;
        public final List<String> optionIds;
        /** @deprecated Kept as a one-control row list for existing audit consumers. */
        @Deprecated
        public final List<String> tabControls;

        private Feature(String id, String section, String title, String description, String rowType,
                        String editorId, String toggleOptionId, String editorButtonLabel, String directControl,
                        String... optionIds) {
            this.id = id;
            this.tab = section;
            this.section = section;
            this.title = title;
            this.description = description;
            this.rowType = rowType;
            this.editorId = editorId;
            this.toggleOptionId = toggleOptionId;
            this.editorButtonLabel = editorButtonLabel;
            this.optionIds = Collections.unmodifiableList(Arrays.asList(optionIds));
            this.tabControls = Collections.singletonList(directControl);
        }

        public boolean isSimple() { return SIMPLE.equals(rowType); }
        public boolean isEditorRoute() { return EDITOR_ROUTE.equals(rowType); }
        /** @deprecated Feature dropdowns were removed; all routes are direct rows. */
        @Deprecated
        public boolean isDropdown() { return false; }
        public boolean isSlider() { return isSimple() && tabControls.equals(Collections.singletonList(SLIDER)); }
        public boolean isSettingsRoute() { return isEditorRoute(); }
        /** @deprecated The main Hub has category expansion only, never a feature dropdown. */
        @Deprecated
        public int dropdownDepth() { return 0; }
    }

    /** One concise row for the requested release table. */
    public static final class AuditRow {
        /** @deprecated Logical section name; the Hub has no tab strip. */
        @Deprecated
        public final String tab;
        public final String section;
        public final String feature;
        public final String rowType;
        public final String whereOptionsLive;
        private AuditRow(String section, String feature, String rowType, String whereOptionsLive) {
            this.tab = section;
            this.section = section;
            this.feature = feature;
            this.rowType = rowType;
            this.whereOptionsLive = whereOptionsLive;
        }
    }

    private static final Map<String, Option> OPTIONS = new LinkedHashMap<String, Option>();
    private static final List<Feature> FEATURES = new ArrayList<Feature>();
    private static boolean duplicateOptionId;

    static {
        // General keeps its existing read-only GUI telemetry; configurable in-game metrics belong only to Stats Display.
        editorRoute("appearance", "General", "Appearance", "Theme, accent, corners and panel opacity.",
                "appearance", "dark-mode", "Appearance", options("Dark mode", "dark-mode", "Accent color", "accent-color",
                "Corner radius switch", "corner-radius-on", "Corner radius", "corner-radius", "Panel opacity switch",
                "panel-opacity-on", "Panel opacity", "panel-opacity"));
        editorRoute("mod-settings", "General", "Mod Settings", "Global scroll speed and smoothing.",
                "mod-settings", "smooth-scroll", "Mod Settings", options("Global scroll speed", "global-scroll",
                "Smooth scrolling", "smooth-scroll"));
        editorRoute("performance", "General", "Performance", "Curve, shadows, animation and adaptive GUI quality.",
                "performance", "performance-boost", "Performance", options("Curve quality", "curve-quality", "Soft shadows", "soft-shadows",
                "Animations", "animations", "Animation speed switch", "animation-speed-on", "Animation speed", "animation-speed",
                "Adaptive quality", "adaptive-quality", "Performance Boost Beta", "performance-boost"));

        // Quality of Life: direct daily controls first, followed by one named route to each retained tool.
        simple("no-block-particles", "Quality of Life", "No Block Particles", "Hide only vanilla block digging and crack particles.",
                "no-block-particles", "No block particles");
        simple("disable-hotbar-scrolling", "Quality of Life", "No Hotbar Scrolling", "Ignore mouse-wheel hotbar selection during gameplay.",
                "disable-hotbar-scrolling", "No Hotbar Scrolling");
        slider("hurt-camera-shake", "Quality of Life", "Hurt Camera Shake",
                "Scale only the local vanilla hurt-camera tilt from 0% to 100%.",
                "hurt-camera-shake", "Hurt Camera Shake");
        simple("auto-sprint", "Quality of Life", "Auto Sprint", "Sprints automatically while you hold forward.",
                "auto-sprint", "Auto Sprint");
        action("inventory-assist", "Quality of Life", "Inventory Assist", "Tooltip and inventory visual helpers.",
                "inventory-assist", "Inventory Assist", options("Inventory Assist Settings", "inventory-assist-settings",
                "Hide potion effects in inventory", "hide-potions", "Roman numerals in tooltips", "roman-numerals",
                "Scrollable item tooltips", "tooltip-scroll", "Item rarity highlight", "rarity-highlight"));
        editorRoute("movement-shortcuts", "Quality of Life", "Movement and Shortcuts", "Wardrobe and pets shortcuts.",
                "movement-shortcuts", "wardrobe-shortcut", "Movement and Shortcuts", options("Wardrobe shortcut", "wardrobe-shortcut",
                "Pets shortcut", "pets-shortcut"));
        editorRoute("item-lock-swap", "Quality of Life", "Item Lock and Swap", "Pair locks, indicators and swap-line controls.",
                "item-lock-swap", "item-lock-swap-enabled", "Item Lock and Swap", options("Item Lock and Swap", "item-lock-swap-enabled",
                "Item Lock and Swap key", "item-lock-swap-key", "Swap cooldown", "item-lock-swap-cooldown", "Show lock icons", "item-lock-show-icons",
                "Show icons on hotbar", "item-lock-hotbar-icons", "Blocked message", "item-lock-message", "Badge tint", "item-lock-color",
                "Badge transparency", "item-lock-opacity", "Swap line mode", "item-swap-line-mode", "Swap line tint", "item-swap-line-color",
                "Swap line transparency", "item-swap-line-opacity", "Swap line thickness", "item-swap-line-width", "Clear locks", "item-lock-clear",
                "Clear pairs", "item-swap-clear"));
        editorRoute("chat-qol", "Quality of Life", "Chat QoL", "Attached tabs, message animation and highlighting.",
                "chat", "chat-master", "Chat", options("Chat QoL master", "chat-master", "Chat tabs", "chat-tabs",
                "Smooth messages", "chat-smooth", "Hover highlight", "chat-hover", "Chat Settings", "chat-editor"));
        editorRoute("commands", "Quality of Life", "Commands", "Optional /sv and /qn command routes plus diagnostics.",
                "commands", "command-sv", "Commands", options("/sv", "command-sv", "/qn", "command-qn",
                "/velor itemdebug", "command-itemdebug", "/velor lockdebug", "command-lockdebug", "/velor selftest", "command-selftest"));

        // Item Animation is a Quality of Life tool; its renderer-only details remain in the established editor.
        editorRoute("item-animation", "Quality of Life", "Item Animation", "Visual first-person transform controls.",
                "item-animation", "item-animation-editor", "Item Animation", options("Item Animation editor", "item-animation-editor"));

        // The HUD Hub Editor section is the only generic HUD editor entry. Element rows below retain their direct
        // visibility switches and the same editor route, not a duplicate renderer or nested feature menu.
        action("hud-editor", "HUD Hub Editor", "HUD Editor", "Select, move, resize and style existing HUD elements.",
                "hud", "HUD Editor", options("HUD Editor", "hud-editor-open"));
        hud("hud-player-doll", "Player Doll", "A live model of you, with your armour and held item");
        hud("hud-inventory", "Inventory HUD", "The 27 main inventory slots, optional armour");
        hud("hud-stats", "Stats Display", "Frame rate, mining BPS and memory cards matching the classic HUD.");
        editorRoute("mining-bps", "Farming", "Mining BPS", "Mining block-break-speed HUD.",
                "hud", "mining-bps-visible", "Mining BPS", options("Mining BPS visibility", "mining-bps-visible",
                "Mining BPS Settings", "mining-bps-editor"));
        editorRoute("mining-assistant-settings", "Farming", "Mining Assistant", "Alerts, puzzler marker and assistant HUD.",
                "mining-assistant", "mining-assistant-hud", "Mining Assistant", options("Pickaxe ability ready alert", "mining-ability-alert",
                "Puzzler direction helper", "mining-puzzler", "Mining Assistant HUD", "mining-assistant-hud",
                "Mining Assistant Settings", "mining-assistant-editor"));

        // Dungeon usage order: ESP first, solvers/helpers next, information/other last; each group is alphabetical.
        editorRoute("door-esp", "Dungeon", "Door ESP", "Wither and blood door hitbox styles, filters and preview.",
                "door-esp", "door-esp-master", "Door ESP", doorEspOptions());
        editorRoute("mob-esp", "Dungeon", "Mob ESP", "Per-mob hitbox styles, readable category labels, scan controls and presets.",
                "mob-esp", "mob-esp-master", "Mob ESP", mobEspOptions());
        simple("dungeon-sync-enabled", "Dungeon", "Dungeon Sync", "Opt into the existing dungeon-sync connection.",
                "dungeon-sync-enabled", "Dungeon Sync");
        editorRoute("m7-overlay", "Dungeon", "Floor VII (M7) Overlay", "Areas, alerts, boss and dragon hitboxes.",
                "m7-overlay", "m7-overlay", "Floor VII (M7) Overlay", options("M7 overlay", "m7-overlay", "Relic kill areas", "m7-relic-areas",
                "Keep areas after dragons", "m7-areas-persist", "Dragon kill alert", "m7-dragon-alert", "Boss hitboxes", "m7-boss-hitboxes",
                "Dragon hitboxes", "m7-dragon-hitboxes"));
        editorRoute("spirit-leap", "Dungeon", "Spirit Leap GUI", "Spirit Leap menu, key shortcut and preview.",
                "spirit-leap", "spirit-leap-menu", "Spirit Leap GUI", options("Spirit Leap menu", "spirit-leap-menu",
                "Spirit Leap key", "spirit-leap-key", "Spirit Leap preview", "spirit-leap-preview"));
        editorRoute("terminal-assist", "Dungeon", "Terminal Assist", "Terminal and Floor VII chest assistance.",
                "terminal-assist", "terminal-hints", "Terminal Assist", options("Terminal visual helpers", "terminal-hints",
                "Terminal auto click", "terminal-auto-click", "Hide incorrect terminal choices", "terminal-hide-incorrect",
                "Prevent wrong terminal clicks", "terminal-prevent-wrong", "Terminal waypoints", "terminal-waypoints",
                "Terminal scale", "terminal-scale"));
        editorRoute("dungeon-boss-hud", "Dungeon", "Boss HP", "Dungeon boss-health HUD and settings route.",
                "hud", "dungeon-boss-hp-visible", "Boss HP", options("Boss HP HUD", "dungeon-boss-hp-visible",
                "Boss HP Settings", "dungeon-boss-hp-editor"));
        editorRoute("dungeon-map", "Dungeon", "Dungeon Map", "Dungeon map visibility and in-world settings.",
                "hud", "dungeon-map", "Dungeon Map", options("Dungeon map", "dungeon-map", "Dungeon Map Settings", "dungeon-map-editor"));
        simple("dungeon-sync-debug", "Dungeon", "Sync Details", "Show the existing local Dungeon Sync diagnostics.",
                "dungeon-sync-debug", "Sync Details");

        // Slayer and fishing remain fully reachable without a third Hub row type.
        editorRoute("slayer-tracker", "Slayer", "Slayer Tracker", "Drop history, timer, prices and clear-history controls.",
                "slayer-simple", "slayer-tracker", "Slayer Tracker", options("Track slayer drops", "slayer-tracker", "Boss timer", "slayer-boss-timer",
                "Price drops", "slayer-prices", "Set a price", "slayer-set-price", "Clear Revenant history", "slayer-clear-revenant",
                "Clear Tarantula history", "slayer-clear-tarantula", "Clear Sven history", "slayer-clear-sven",
                "Clear Voidgloom history", "slayer-clear-voidgloom", "Clear Blaze history", "slayer-clear-blaze"));
        editorRoute("slayer-hud-route", "Slayer", "Slayer HUD", "Active Slayer tracker HUD and settings route.",
                "hud", "slayer-hud-visible", "Slayer HUD", options("Slayer HUD", "slayer-hud-visible",
                "Slayer HUD Settings", "slayer-hud-editor"));
        simple("jawbus", "Fishing", "Lord Jawbus", "Arrival title alert.", "jawbus-alert", "Jawbus arrival alert");
        simple("nearby-encounters", "Fishing", "Nearby Encounters", "Local compatible encounter title alert.",
                "nearby-encounter-alert", "Nearby encounter alert");
        editorRoute("catch-counter", "Fishing", "Catch Counter", "Catch counts, Festival best and reset.",
                "catch-counter", "fishing-counter", "Catch Counter", options("Catch counter", "fishing-counter",
                "Festival best", "fishing-festival", "Reset counters", "fishing-reset"));
        editorRoute("fishing-hud-route", "Fishing", "Fishing HUD", "Catch counter HUD and settings route.",
                "hud", "fishing-hud-visible", "Fishing HUD", options("Fishing HUD", "fishing-hud-visible",
                "Fishing HUD Settings", "fishing-hud-editor"));
        editorRoute("sea-hp-hud-route", "Fishing", "Sea Creature HP", "Sea creature HP HUD and settings route.",
                "hud", "sea-hp-hud-visible", "Sea Creature HP", options("Sea HP HUD", "sea-hp-hud-visible",
                "Sea Creature HP Settings", "sea-hp-editor"));
    }

    private FeatureSettingsUiModel() { }

    private static void hud(String id, String title, String description) {
        String visible = id + "-visible";
        String editor = id + "-editor";
        editorRoute(id, "HUD Hub Editor", title, description, "hud", visible, title,
                options(title + " visibility", visible, title + " Settings", editor));
    }

    private static String[] options(String... labelIdPairs) { return labelIdPairs; }

    private static void simple(String id, String tab, String title, String description, String optionId, String optionLabel) {
        addFeature(id, tab, title, description, SIMPLE, "", optionId, "", TOGGLE, options(optionLabel, optionId));
    }

    private static void slider(String id, String tab, String title, String description, String optionId, String optionLabel) {
        addFeature(id, tab, title, description, SIMPLE, "", optionId, "", SLIDER, options(optionLabel, optionId));
    }

    private static void action(String id, String section, String title, String description, String editorId,
                               String editorName, String... labelIdPairs) {
        String primary = labelIdPairs.length > 1 ? labelIdPairs[1] : id + "-settings";
        addFeature(id, section, title, description, EDITOR_ROUTE, editorId, primary, "Open", EDITOR_BUTTON, labelIdPairs);
    }

    private static void editorRoute(String id, String section, String title, String description, String editorId,
                                    String primaryOptionId, String editorName, String... labelIdPairs) {
        addFeature(id, section, title, description, EDITOR_ROUTE, editorId, primaryOptionId, "Open", EDITOR_BUTTON,
                labelIdPairs);
    }

    private static void addFeature(String id, String tab, String title, String description, String rowType,
                                   String editorId, String toggleOptionId, String editorButtonLabel, String directControl,
                                   String... labelIdPairs) {
        List<String> ids = new ArrayList<String>();
        for (int i = 0; i + 1 < labelIdPairs.length; i += 2) {
            String label = labelIdPairs[i];
            String optionId = labelIdPairs[i + 1];
            if (OPTIONS.containsKey(optionId)) duplicateOptionId = true;
            OPTIONS.put(optionId, new Option(optionId, label, rowType));
            ids.add(optionId);
        }
        FEATURES.add(new Feature(id, tab, title, description, rowType, editorId, toggleOptionId, editorButtonLabel,
                directControl, ids.toArray(new String[ids.size()])));
    }

    private static String[] mobEspOptions() {
        return options("Enable Dungeon ESP", "mob-esp-master",
                "Starred Mob ESP", "mob-starred-enabled", "Starred Mob Color", "mob-starred-color", "Starred Mob Render Mode", "mob-starred-mode", "Starred Mob Through Walls", "mob-starred-walls", "Starred Mob Outline Width", "mob-starred-width", "Starred Mob Outline Opacity", "mob-starred-outline", "Starred Mob Fill Opacity", "mob-starred-fill",
                "Miniboss ESP", "mob-miniboss-enabled", "Miniboss Color", "mob-miniboss-color", "Miniboss Render Mode", "mob-miniboss-mode", "Miniboss Through Walls", "mob-miniboss-walls", "Miniboss Outline Width", "mob-miniboss-width", "Miniboss Outline Opacity", "mob-miniboss-outline", "Miniboss Fill Opacity", "mob-miniboss-fill",
                "Special Starred Mob ESP", "mob-special-enabled", "Special Starred Mob Color", "mob-special-color", "Special Starred Mob Render Mode", "mob-special-mode", "Special Starred Mob Through Walls", "mob-special-walls", "Special Starred Mob Outline Width", "mob-special-width", "Special Starred Mob Outline Opacity", "mob-special-outline", "Special Starred Mob Fill Opacity", "mob-special-fill",
                "Enderman ESP", "mob-enderman-enabled", "Enderman Color", "mob-enderman-color", "Enderman Render Mode", "mob-enderman-mode", "Enderman Through Walls", "mob-enderman-walls", "Enderman Outline Width", "mob-enderman-width", "Enderman Outline Opacity", "mob-enderman-outline", "Enderman Fill Opacity", "mob-enderman-fill",
                "Skeletor ESP", "mob-skeletor-enabled", "Skeletor Color", "mob-skeletor-color", "Skeletor Render Mode", "mob-skeletor-mode", "Skeletor Through Walls", "mob-skeletor-walls", "Skeletor Outline Width", "mob-skeletor-width", "Skeletor Outline Opacity", "mob-skeletor-outline", "Skeletor Fill Opacity", "mob-skeletor-fill",
                "Force Render All Mobs in Room", "mob-force-room", "Other Mobs ESP", "mob-other-enabled", "Other Mobs Color", "mob-other-color", "Other Mobs Render Mode", "mob-other-mode", "Other Mobs Through Walls", "mob-other-walls", "Other Mobs Outline Width", "mob-other-width", "Other Mobs Outline Opacity", "mob-other-outline", "Other Mobs Fill Opacity", "mob-other-fill", "Include Bats", "mob-include-bats", "Include Invisible Mobs", "mob-include-invisible",
                "Maximum ESP Distance", "mob-distance", "Update Interval", "mob-interval", "Hide Zero-Health Mobs", "mob-hide-zero", "Tracers", "mob-tracers", "Mob Category Labels", "mob-category-labels", "Maximum Mob Labels", "mob-max-labels", "Distance Text", "mob-distance-text", "Maximum Boxes", "mob-max-boxes", "Default Preset", "mob-default-preset", "Subtle Preset", "mob-subtle-preset", "Reset Dungeon ESP", "mob-reset", "Copy Style", "mob-copy-style", "Apply Copied Style", "mob-apply-style", "Apply This Style to All", "mob-apply-all");
    }

    private static String[] doorEspOptions() {
        return options("Show Closed Doors", "door-esp-master", "Door Scope", "door-esp-scope", "Only With a Wither Key", "door-esp-key", "Blood Doors", "door-esp-blood", "Blood Door Color", "door-esp-blood-color", "Enable Door Style", "door-esp-style-enabled", "Wither Door Color", "door-esp-wither-color", "Door Render Mode", "door-esp-mode", "Doors Through Walls", "door-esp-through-walls", "Door Outline Width", "door-esp-width", "Door Outline Opacity", "door-esp-outline-opacity", "Door Fill Opacity", "door-esp-fill-opacity", "Door Distance Text", "door-esp-distance-text", "Default Preset", "door-esp-default-preset", "Subtle Preset", "door-esp-subtle-preset", "Reset Door ESP", "door-esp-reset", "Copy Style", "door-esp-copy", "Apply Copied Style", "door-esp-apply");
    }

    public static List<Feature> features() { return Collections.unmodifiableList(FEATURES); }
    public static Option option(String id) { return OPTIONS.get(id); }
    public static List<Option> options() { return Collections.unmodifiableList(new ArrayList<Option>(OPTIONS.values())); }

    /** True when a former human-facing control label is still owned by a simple row or an editor route. */
    public static boolean hasOptionLabel(String label) {
        if (label == null) return false;
        for (Option option : OPTIONS.values()) if (label.equals(option.label)) return true;
        return false;
    }

    public static Feature feature(String id) {
        for (Feature feature : FEATURES) if (feature.id.equals(id)) return feature;
        return null;
    }

    /** Returns features in one expandable Hub section. */
    public static List<Feature> section(String name) {
        List<Feature> out = new ArrayList<Feature>();
        for (Feature feature : FEATURES) if (feature.section.equals(name)) out.add(feature);
        return Collections.unmodifiableList(out);
    }

    /** @deprecated The Hub has no tabs; use {@link #section(String)}. */
    @Deprecated
    public static List<Feature> tab(String name) {
        return section(name);
    }

    public static List<AuditRow> auditRows() {
        List<AuditRow> rows = new ArrayList<AuditRow>();
        for (Feature feature : FEATURES) {
            String direct = feature.tabControls.get(0);
            String type = feature.isEditorRoute() ? "Editor route" : SLIDER.equals(direct) ? "Slider" : "Simple";
            String where = feature.isEditorRoute() ? feature.editorButtonLabel + " -> " + feature.editorId
                    : SLIDER.equals(direct) ? "This section's percentage slider" : "This section's toggle";
            rows.add(new AuditRow(feature.section, feature.title, type, where));
        }
        return Collections.unmodifiableList(rows);
    }

    /** The only expandable layer is AppleHubScreen's top-level category header. */
    public static int maximumDropdownDepth() {
        return 0;
    }

    /** Static proof of direct-row/editor-route ownership and complete option reachability. */
    public static boolean isReachabilityValid() {
        if (duplicateOptionId || FEATURES.isEmpty() || OPTIONS.size() <= 90) return false;
        for (Feature feature : FEATURES) {
            if (feature.id == null || feature.id.length() == 0 || feature.optionIds.isEmpty()
                    || feature.toggleOptionId == null || !OPTIONS.containsKey(feature.toggleOptionId)
                    || feature.tabControls.size() != 1) return false;
            String control = feature.tabControls.get(0);
            if (feature.isSimple()) {
                if ((TOGGLE.equals(control) || SLIDER.equals(control))
                        && (feature.editorId.length() != 0 || feature.editorButtonLabel.length() != 0)) return false;
                if (!TOGGLE.equals(control) && !SLIDER.equals(control)) return false;
            } else if (feature.isEditorRoute()) {
                if (!EDITOR_BUTTON.equals(control) || feature.editorId.length() == 0
                        || !"Open".equals(feature.editorButtonLabel)) return false;
            } else return false;
            for (String id : feature.optionIds) if (!OPTIONS.containsKey(id)) return false;
        }
        return maximumDropdownDepth() == 0;
    }
}
