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
        addFeature("dungeon-sync", "Dungeon", null, "Dungeon Sync", "Party puzzle announcement and sync diagnostics.", INLINE,
                "", options("Party puzzle announce", "party-puzzle-announce", "Dungeon debug", "dungeon-debug"));
        addFeature("terminal-assist", "Dungeon", null, "Terminal Assist", "Terminal and Floor VII chest assistance.", EDITOR,
                "terminal-assist", options("Terminal hints", "terminal-hints", "Click in order solver", "terminal-solver",
                "Terminal debug", "terminal-debug"));
        addFeature("mob-esp", "Dungeon", null, "Mob ESP", "Per-mob hitbox styles, global scan controls and presets.", EDITOR,
                "mob-esp", options("Mob ESP master", "mob-esp-master", "Global Mob ESP", "mob-esp-global", "Starred Mob ESP", "mob-esp-starred",
                "Shadow Assassin ESP", "mob-esp-shadow-assassin", "Fels ESP", "mob-esp-fels", "Crypt Dreadlord ESP", "mob-esp-dreadlord",
                "Skeletor ESP", "mob-esp-skeletor", "Wither Skeleton ESP", "mob-esp-wither-skeleton", "Enderman ESP", "mob-esp-enderman",
                "Bat ESP", "mob-esp-bat", "Mimic ESP", "mob-esp-mimic", "Ice Spray ESP", "mob-esp-ice-spray",
                "Force Render All Mobs in Room", "mob-esp-force-room", "Mob ESP distance", "mob-esp-distance", "Mob ESP interval", "mob-esp-interval",
                "Hide zero-health mobs", "mob-esp-hide-zero", "Tracers", "mob-esp-tracers", "Distance text", "mob-esp-distance-text", "Maximum boxes", "mob-esp-max-boxes",
                "Default preset", "mob-esp-default-preset", "Subtle preset", "mob-esp-subtle-preset", "Reset Mob ESP", "mob-esp-reset"));
        addFeature("door-esp", "Dungeon", null, "Door ESP", "Wither and blood door hitbox styles, filters and preview.", EDITOR,
                "door-esp", options("Door ESP master", "door-esp-master", "Wither Doors", "door-esp-wither", "Blood Doors", "door-esp-blood",
                "Blood door color", "door-esp-blood-color", "Door render mode", "door-esp-mode", "Doors through walls", "door-esp-through-walls",
                "Door outline width", "door-esp-width", "Door outline opacity", "door-esp-outline-opacity", "Door fill opacity", "door-esp-fill-opacity",
                "Door distance text", "door-esp-distance-text", "Door ESP reset", "door-esp-reset"));
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
