package com.arena.pekaclient.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Immutable player-facing presentation catalog for the native settings shell.
 *
 * <p>This catalog never owns a feature value. Every id resolves to an established configuration owner, HUD
 * element, or action. A feature may have one disclosure row; its expanded content is deliberately flat so the
 * page never turns into a tree of nested dropdowns.</p>
 */
public final class ReferenceSettingsCatalog {

    public enum Tab {
        HOME("Home", "Status, quick controls, and performance."),
        GAMEPLAY("Player", "Movement, inventory, chat, and Slayer."),
        DUNGEON("Dungeons", "Maps, terminals, party, and ESP."),
        FARMING("Farming", "No Farming Features Yet."),
        MINING("Mining", "Mining tools, readouts, and helpers."),
        FISHING("Fishing", "Fishing tracker, health, and alerts."),
        VISUALS("HUD & Visuals", "HUD layout and held-item visuals."),
        SYSTEM("Misc", "Appearance, performance, and commands.");

        public final String label;
        public final String caption;

        Tab(String label, String caption) {
            this.label = label;
            this.caption = caption;
        }
    }

    /** A caption is paint-only, not an interactive row. An editor row has a distinct Edit button. */
    public enum Control {
        TOGGLE, SLIDER, ACTION, CHOICE, INFO, DROPDOWN, TEXT, CAPTION, EDITOR
    }

    public static final class Setting {
        public final String id;
        public final String label;
        public final String description;
        public final String keywords;
        public final Control control;
        public final boolean advanced;
        public final String actionLabel;
        public final int min;
        public final int max;
        public final int step;
        public final String suffix;
        public final String[] choices;
        public final boolean editorHasMasterToggle;
        public final String editorActionId;
        public final List<Setting> children;

        private Setting(String id, String label, String description, String keywords, Control control,
                        boolean advanced, String actionLabel, int min, int max, int step, String suffix,
                        String[] choices, boolean editorHasMasterToggle, String editorActionId,
                        Setting... children) {
            this.id = id;
            this.label = label;
            this.description = description;
            this.keywords = keywords == null ? "" : keywords;
            this.control = control;
            this.advanced = advanced;
            this.actionLabel = actionLabel == null ? "" : actionLabel;
            this.min = min;
            this.max = max;
            this.step = step;
            this.suffix = suffix == null ? "" : suffix;
            this.choices = choices == null ? new String[0] : choices.clone();
            this.editorHasMasterToggle = editorHasMasterToggle;
            this.editorActionId = editorActionId == null ? "" : editorActionId;
            this.children = immutable(children);
        }

        public boolean hasChildren() { return !children.isEmpty(); }
        public boolean isDropdown() { return control == Control.DROPDOWN || (control == Control.TOGGLE && hasChildren()); }
        public boolean hasMasterToggle() { return control == Control.TOGGLE || (control == Control.EDITOR && editorHasMasterToggle); }
        public boolean isEditor() { return control == Control.EDITOR; }
        public boolean isCaption() { return control == Control.CAPTION; }
        public boolean isResetAction() { return control == Control.ACTION && "Reset to defaults".equals(actionLabel); }
        public int visibleChildCount() { return children.size(); }

        /** Compatibility for old model readers; presentation no longer has an Advanced visibility gate. */
        @Deprecated public boolean hasChildren(boolean ignored) { return hasChildren(); }
        @Deprecated public int visibleChildCount(boolean ignored) { return visibleChildCount(); }
    }

    public static final class Card {
        public final String id;
        public final String title;
        public final String subtitle;
        public final boolean advanced;
        public final String notice;
        public final boolean warningNotice;
        public final List<Setting> settings;

        private Card(String id, String title, String subtitle, boolean advanced, String notice, boolean warningNotice,
                     Setting... settings) {
            this.id = id;
            this.title = title;
            this.subtitle = subtitle;
            this.advanced = advanced;
            this.notice = notice == null ? "" : notice;
            this.warningNotice = warningNotice;
            this.settings = immutable(settings);
        }
    }

    /** A search result carries the one owning dropdown that must open before its option is visible. */
    public static final class Match {
        public final Tab tab;
        public final Card card;
        public final Setting setting;
        public final List<Setting> parents;

        private Match(Tab tab, Card card, Setting setting, List<Setting> parents) {
            this.tab = tab;
            this.card = card;
            this.setting = setting;
            this.parents = Collections.unmodifiableList(new ArrayList<Setting>(parents));
        }
    }

    /** Direct feature owners; ids are presentation ids, never replacement configuration keys. */
    public static final String[] REQUIRED_ROUTE_IDS = {
            "no-block-particles", "disable-hotbar-scrolling", "hurt-camera-shake", "auto-sprint",
            "item-lock-swap", "inventory-assist", "chat-qol", "movement-shortcuts", "slayer-tracker",
            "dungeon-map", "terminal-assist", "spirit-leap", "dungeon-boss-hud", "m7-overlay",
            "mob-esp-enabled", "door-esp-enabled", "dungeon-sync-enabled",
            "mining-bps", "mining-assistant-settings", "fishing-tracker", "hud-editor", "item-animation",
            "system-appearance", "system-performance", "system-general", "system-commands"
    };

    /** Native destination order. Farming is deliberately empty until a real owner exists. */
    public static final Tab[] TABS = Tab.values();

    private static final List<Card> HOME = Collections.emptyList();

    private static final List<Card> GAMEPLAY = cards(
            card("comfort", "Comfort", "Small play preferences.", false, "",
                    toggle("no-block-particles", "Quiet Block Particles", "Hide block-break particles.", "particles digging cracks"),
                    toggle("disable-hotbar-scrolling", "Lock Hotbar Scroll", "Keep the mouse wheel from changing slots.", "mouse wheel hotbar"),
                    slider("hurt-camera-shake", "Hurt Camera Shake", "Adjust the camera tilt after damage.",
                            "damage camera tilt", 0, 100, 1, "%"),
                    toggle("hide-dead-mobs", "Hide Dead Mobs", "Hide dungeon ESP during the death animation.",
                            "dungeon esp dying defeated"),
                    toggle("auto-sprint", "Auto Sprint", "Sprint while moving forward when possible.", "sprint movement")),
            card("inventory-chat", "Inventory And Chat", "Inventory, messages, and shortcuts.", false, "",
                    masterDropdown("item-lock-swap", "Item Lock And Swap",
                            "Lock slots and swap saved pairs.", "lock pairing badges",
                            info("item-lock-key", "Item Lock And Swap Key", "Set the key in Minecraft Controls.", "key controls"),
                            slider("item-lock-cooldown", "Swap Cooldown", "Time between pair swaps.", "swap cooldown", 5, 50, 1, ""),
                            toggle("item-lock-icons", "Show Lock Icons", "Show lock and pair badges in inventory.", "lock icons"),
                            toggle("item-lock-hotbar", "Show Hotbar Icons", "Show badges in the hotbar.", "hotbar icons"),
                            toggle("item-lock-message", "Blocked Message", "Show a message when an action is blocked.", "message"),
                            choice("item-lock-badge-color", "Badge Tint", "Choose a badge color.", "badge color",
                                    new String[]{"Gold", "Blue", "Purple", "Green", "Orange", "Teal"}),
                            slider("item-lock-badge-opacity", "Badge Opacity", "Set badge opacity.", "badge opacity", 20, 100, 5, "%"),
                            choice("item-lock-line-mode", "Swap Line Mode", "Choose when pair lines appear.", "swap lines",
                                    new String[]{"Always", "On Hover", "Off"}),
                            choice("item-lock-line-color", "Swap Line Tint", "Choose a pair-line color.", "swap line color",
                                    new String[]{"Gold", "Blue", "Purple", "Green", "Orange", "Teal"}),
                            slider("item-lock-line-opacity", "Swap Line Opacity", "Set pair-line opacity.", "swap line opacity", 20, 100, 5, "%"),
                            slider("item-lock-line-width", "Swap Line Width", "Set pair-line thickness.", "swap line width", 1, 4, 1, " px"),
                            action("item-lock-clear-locks", "Clear Locks", "Remove saved slot locks.", "clear locks", "Clear Locks"),
                            action("item-lock-clear-pairs", "Clear Pairs", "Remove saved item pairs.", "clear pairs", "Clear Pairs"),
                            reset("item-lock-reset")),
                    dropdown("inventory-assist", "Inventory Assist", "Potion and tooltip helpers.", "tooltips potion rarity",
                            toggle("hide-potions", "Hide Potion Effects", "Hide effects while inventory is open.", "potion effects"),
                            toggle("roman-numerals", "Roman Numerals", "Show enchant levels as Roman numerals.", "enchant lore"),
                            toggle("tooltip-scroll", "Scrollable Tooltips", "Scroll long item descriptions.", "lore scroll"),
                            toggle("rarity-highlight", "Rarity Highlight", "Tint item slots by rarity.", "rarity item"),
                            reset("inventory-assist-reset")),
                    masterDropdown("chat-qol", "Chat QoL", "Message style and chat tabs.", "chat tabs smooth hover",
                            caption("chat-background-group", "Background"),
                            text("chat-background-color", "Background Color", "Choose a message background color.", "background color"),
                            slider("chat-background-open-opacity", "Open Opacity", "Set opacity while chat is open.", "chat open opacity", 0, 100, 5, "%"),
                            slider("chat-background-closed-opacity", "Closed Opacity", "Set opacity while chat is closed.", "chat closed opacity", 0, 100, 5, "%"),
                            slider("chat-background-radius", "Corner Radius", "Set message corner roundness.", "chat radius", 0, 12, 1, " px"),
                            choice("chat-background-open-area", "Open Chat Background", "Choose the open chat background area.", "chat area",
                                    new String[]{"Fit To Messages", "Full Area"}),
                            toggle("chat-background-fit-width", "Fit Width To Text", "Match the background to message width.", "chat width"),
                            caption("chat-messages-group", "Messages"),
                            slider("chat-message-animation", "Message Animation", "Set message animation time.", "message motion", 0, 600, 20, " ms"),
                            slider("chat-fade-seconds", "Fade Out", "Set when messages fade.", "message fade", 0, 60, 1, " s"),
                            toggle("chat-smooth", "Smooth Messages", "Fade and slide new messages.", "smooth messages"),
                            toggle("chat-compact", "Compact Messages", "Show repeated-message counters.", "compact messages"),
                            text("chat-compact-counter-color", "Counter Color", "Choose a counter color.", "counter color"),
                            caption("chat-tabs-group", "Tabs"),
                            toggle("chat-tabs", "Use Chat Tabs", "Show tabs in open chat.", "party reply"),
                            text("chat-tab-inactive-color", "Inactive Tab Color", "Choose an inactive tab color.", "tab color"),
                            text("chat-tab-selected-color", "Selected Tab Color", "Choose a selected tab color.", "tab color"),
                            slider("chat-tab-opacity", "Tab Opacity", "Set inactive tab opacity.", "tab opacity", 0, 100, 1, "%"),
                            slider("chat-tab-active-opacity", "Active Tab Opacity", "Set selected tab opacity.", "tab opacity", 0, 100, 1, "%"),
                            action("chat-tab-previous", "Editing Tab", "Choose the previous tab.", "tab previous", "Previous"),
                            action("chat-tab-next", "Next Tab", "Choose the next tab.", "tab next", "Next"),
                            toggle("chat-tab-visible", "Visible", "Show this tab.", "tab visible"),
                            action("chat-tab-add", "Add Custom Tab", "Create a custom tab.", "tab add", "Add"),
                            text("chat-tab-name", "Name", "Set the custom tab name.", "tab name"),
                            choice("chat-tab-filter-type", "Filter Type", "Choose how messages match.", "tab filter",
                                    new String[]{"Contains Text", "Starts With", "Regex"}),
                            text("chat-tab-filter-text", "Filter Text", "Set text for this tab.", "tab filter"),
                            text("chat-tab-send-prefix", "Send Prefix", "Set an optional chat prefix.", "tab prefix"),
                            text("chat-tab-accent-color", "Accent Color", "Choose this tab color.", "custom tab accent"),
                            toggle("chat-tab-sound", "Message Sound", "Play this tab sound.", "tab sound"),
                            action("chat-tab-save", "Save Tab", "Save tab changes.", "tab save", "Save"),
                            action("chat-tab-left", "Move Left", "Move this tab earlier.", "tab move", "Left"),
                            action("chat-tab-right", "Move Right", "Move this tab later.", "tab move", "Right"),
                            action("chat-tab-delete", "Delete Custom Tab", "Delete this custom tab.", "tab delete", "Delete"),
                            caption("chat-menu-group", "Click Menu"),
                            toggle("chat-hover", "Hover Highlight", "Highlight the message under the pointer.", "hover highlight"),
                            text("chat-hover-color", "Highlight Color", "Choose a highlight color.", "hover color"),
                            slider("chat-hover-opacity", "Highlight Opacity", "Set highlight opacity.", "hover opacity", 0, 100, 5, "%"),
                            choice("chat-menu-mode", "Open Menu With", "Choose the click for the message menu.", "chat menu",
                                    new String[]{"Right Click", "Left Click", "Both"}),
                            reset("chat-reset-all")),
                    dropdown("movement-shortcuts", "Movement And Shortcuts", "Key shortcuts for commands.", "wardrobe pets",
                            toggle("wardrobe-shortcut", "Wardrobe Shortcut", "Use the Wardrobe key shortcut.", "wardrobe key"),
                            toggle("pets-shortcut", "Pets Shortcut", "Use the Pets key shortcut.", "pets key"),
                            reset("movement-shortcuts-reset")),
                    masterDropdown("slayer-tracker", "Slayer Tracker", "Drops, prices, and quest timing.", "slayer drops price timer",
                            toggle("slayer-boss-timer", "Boss Timer", "Time each Slayer boss.", "slayer timer"),
                            toggle("slayer-pricing", "Price Drops", "Show priced drops.", "slayer price"),
                            info("slayer-set-price", "Set A Price", "Use /setprice for a held item.", "setprice"),
                            caption("slayer-history", "Clear History"),
                            action("slayer-clear-spider", "Spider", "Clear Spider drops and profit.", "spider clear", "Clear"),
                            action("slayer-clear-revenant", "Revenant", "Clear Revenant drops and profit.", "revenant zombie clear", "Clear"),
                            action("slayer-clear-blaze", "Blaze", "Clear Blaze drops and profit.", "blaze clear", "Clear"),
                            action("slayer-clear-wolf", "Wolf", "Clear Wolf drops and profit.", "wolf sven clear", "Clear"),
                            action("slayer-clear-enderman", "Enderman", "Clear Enderman drops and profit.", "enderman voidgloom clear", "Clear"),
                            reset("slayer-reset")))
    );

    private static final List<Card> DUNGEON = cards(
            card("run-tools", "Run Tools", "Map, terminals, leaps, and boss health.", false, "",
                    masterDropdown("dungeon-map", "Dungeon Map", "Room, puzzle, and party information.", "map rooms party",
                            toggle("dungeon-map-rotate", "Rotate To Facing", "Keep your room facing up.", "map rotate"),
                            toggle("dungeon-map-player-names", "Show Player Names", "Show party names on the map.", "map names"),
                            toggle("dungeon-map-puzzle-name", "Show Puzzle Name", "Show puzzle names on the map.", "map puzzle"),
                            toggle("dungeon-map-scale-on", "Use Map Scale", "Use the saved map scale.", "map scale"),
                            slider("dungeon-map-scale", "Map Scale", "Set map size.", "map scale", 70, 250, 5, "%"),
                            slider("dungeon-map-zoom", "Map Zoom", "Set map zoom.", "map zoom", 50, 200, 5, "%"),
                            toggle("party-puzzle-announce", "Party Puzzle Announce", "Send puzzle calls to party chat.", "puzzle announce")),
                    dropdown("terminal-assist", "Terminal Helper", "Helpers and click safeguards for terminals.",
                            "terminal f7 m7 status",
                            toggle("terminal-hints", "Visual Helpers", "Show helpful terminal choices.", "terminal hint"),
                            toggle("terminal-auto-click", "Auto Click", "Click clear terminal choices automatically.", "terminal auto click"),
                            toggle("terminal-hide-wrong", "Hide Wrong Choices", "Dim wrong choices when known.", "terminal dim"),
                            toggle("terminal-prevent-clicks", "Prevent Wrong Clicks", "Block wrong choices when known.", "terminal prevent"),
                            toggle("terminal-waypoints", "Terminal Waypoints", "Show terminal direction markers.", "terminal waypoint"),
                            slider("terminal-scale", "Terminal Scale", "Set terminal helper size.", "terminal scale", 70, 250, 5, "%"),
                            reset("terminal-reset")),
                    masterDropdown("spirit-leap", "Spirit Leap Picker", "Show class labels on Spirit Leap heads.",
                            "leap teammate", toggle("leap-key", "Spirit Leap Key", "Use the Spirit Leap key.", "leap key"),
                            reset("spirit-leap-reset")),
                    editor("dungeon-boss-hud", "Boss Health HUD", "Show boss health on screen.", "boss hp hud", true, "open-hud-editor")),
            card("floor-seven", "Floor VII", "Relics, dragons, and boss outlines.", false, "",
                    masterDropdown("m7-overlay", "Floor VII Overlay", "Track relics, devices, and dragons.", "m7 floor seven dragons",
                            toggle("m7-relic-areas", "Relic Kill Areas", "Show relic areas by statues.", "relic area"),
                            toggle("m7-areas-persist", "Keep Areas After Dragons", "Keep relic areas after dragons appear.", "relic area"),
                            toggle("dragon-alerts", "Dragon Kill Alert", "Alert when a dragon is vulnerable.", "dragon alert"),
                            toggle("m7-terminal-solver", "Click In Order Solver", "Show the next chest slot.", "terminal solver"),
                            caption("m7-bosses", "Boss Hitboxes"),
                            toggle("m7-boss-maxor", "Maxor", "Show Maxor outline.", "maxor"),
                            toggle("m7-boss-storm", "Storm", "Show Storm outline.", "storm"),
                            toggle("m7-boss-goldor", "Goldor", "Show Goldor outline.", "goldor"),
                            toggle("m7-boss-necron", "Necron", "Show Necron outline.", "necron"),
                            caption("m7-dragons", "Dragon Hitboxes"),
                            toggle("m7-dragon-power", "Power", "Show Power dragon outline.", "power dragon"),
                            toggle("m7-dragon-apex", "Apex", "Show Apex dragon outline.", "apex dragon"),
                            toggle("m7-dragon-soul", "Soul", "Show Soul dragon outline.", "soul dragon"),
                            toggle("m7-dragon-ice", "Ice", "Show Ice dragon outline.", "ice dragon"),
                            toggle("m7-dragon-flame", "Flame", "Show Flame dragon outline.", "flame dragon"),
                            reset("m7-reset"))),
            card("esp", "Dungeon ESP", "Starred-mob and door outlines.", false, "",
                    masterDropdown("mob-esp-enabled", "Starred Mobs", "Show starred and miniboss dungeon outlines.",
                            "starred mobs miniboss dungeon esp",
                            combine(new Setting[]{
                                    slider("esp-max-distance", "Maximum Distance", "Set scan distance.", "esp distance", 8, 160, 1, " blocks"),
                                    slider("esp-update-interval", "Update Interval", "Set scan frequency.", "esp interval", 1, 20, 1, " ticks"),
                                    toggle("esp-mob-labels", "Starred Mob Labels", "Show marked-target category labels.", "esp labels"),
                                    slider("esp-max-mob-labels", "Maximum Labels", "Limit visible marked-target labels.", "esp labels", 1, 32, 1, ""),
                                    toggle("esp-distance-text", "Distance Text", "Show distance on labels.", "esp distance text"),
                                    slider("esp-max-boxes", "Maximum Boxes", "Limit visible outlines.", "esp boxes", 8, 128, 1, ""),
                                    caption("esp-starred-caption", "Starred Mob Styles")},
                                    mobStyleRows("esp-starred", "Starred Mob", "Show starred mob outlines."),
                                    mobStyleRows("esp-miniboss", "Miniboss", "Show miniboss outlines."),
                                    mobStyleRows("esp-special", "Special Starred Mob", "Show special mob outlines."),
                                    mobStyleRows("esp-enderman", "Enderman", "Show Enderman outlines."),
                                    new Setting[]{reset("esp-reset")})),
                    masterDropdown("door-esp-enabled", "Door ESP", "Show closed dungeon doors.", "door esp wither blood",
                            combine(new Setting[]{
                                    choice("door-scope", "Door Scope", "Choose nearby or all doors.", "door scope",
                                            new String[]{"Nearest Closed Door", "All Closed Doors"}),
                                    toggle("door-only-key", "Only With A Wither Key", "Hide doors until you have a key.", "door key"),
                                    toggle("door-distance-text", "Door Distance Text", "Show distance above doors.", "door distance"),
                                    action("door-default-preset", "Default Preset", "Use high-visibility colors.", "door preset", "Apply Default"),
                                    action("door-subtle-preset", "Subtle Preset", "Use lower-noise colors.", "door preset", "Apply Subtle"),
                                    caption("door-wither-style-caption", "Door Style")},
                                    doorStyleRows("door-wither-style", "door-style", "Wither Door Style", "Style for Wither doors."),
                                    new Setting[]{
                                            toggle("door-blood", "Blood Doors", "Show Blood doors.", "blood door"),
                                            choice("door-blood-color", "Blood Door Color", "Choose a Blood door color.", "blood color",
                                                    new String[]{"Cyan", "Gold", "Purple", "Green", "Orange", "Teal", "Red", "Pink"}),
                                            reset("door-reset")}))),
            card("party-sync", "Party Sync", "Share puzzle information with your party.", false, "",
                    masterDropdown("dungeon-sync-enabled", "Dungeon Sync", "Share puzzle labels and room positions.",
                            "sync privacy network", toggle("dungeon-sync-debug", "Sync Details", "Show connection details.", "sync debug"),
                            info("dungeon-sync-endpoint", "Server", "Use the configured secure server.", "sync endpoint")))
    );

    private static final List<Card> MINING = cards(
            card("mining", "Mining", "Mining readouts and helpers.", false, "",
                    toggle("mining-bps", "Mining Speed", "Show blocks per second while mining.", "mining bps"),
                    dropdown("mining-assistant-settings", "Mining Assistant", "Alerts, Puzzler help, and HUD display.", "pickaxe puzzler",
                            toggle("mining-ability-alert", "Pickaxe Ability Alert", "Alert when your ability is ready.", "ability alert"),
                            toggle("mining-puzzler", "Puzzler Direction Helper", "Show a suggested Puzzler direction.", "dwarven direction"),
                            toggle("mining-assistant-hud", "Show Mining Assistant", "Show the mining helper on screen.", "mining hud"),
                            reset("mining-assistant-reset")))
    );

    private static final List<Card> FISHING = cards(
            card("fishing", "Fishing", "Catch, creature, and alert controls.", false, "",
                    dropdown("fishing-tracker", "Fishing Tracker", "Catch counts, health, and alerts.", "fishing catches sea creature",
                            toggle("catch-counter", "Catch Counter", "Count every fishing catch.", "fishing counter"),
                            toggle("fishing-festival", "Festival Best", "Track Festival catches and your best.", "festival"),
                            action("fishing-reset", "Clear Counters", "Clear catch counts and Festival best.", "catch reset", "Clear"),
                            toggle("sea-hp-hud-route", "Sea Creature Health", "Show catch health on screen.", "sea hp hud"),
                            toggle("jawbus", "Lord Jawbus Alert", "Alert when Lord Jawbus appears.", "jawbus"),
                            toggle("nearby-encounters", "Nearby Encounters", "Alert for nearby fishing encounters.", "fishing nearby")))
    );

    private static final List<Card> VISUALS = cards(
            card("hud", "HUD", "Move and resize on-screen elements.", false, "",
                    editor("hud-editor", "HUD Editor", "Move and resize on-screen elements.", "hud editor", false,
                            "open-hud-editor"),
                    toggle("hud-player-doll", "Player Doll", "Show your player model on screen.", "doll armour"),
                    toggle("hud-inventory", "Inventory HUD", "Show inventory slots on screen.", "inventory hud")),
            card("item-animation", "Item Animation", "Change how your held item looks.", false, "",
                    editor("item-animation", "Item Animation", "Change how your held item looks.", "hand first person",
                            true, "item-animation-open-editor"))
    );

    private static final List<Card> SYSTEM = cards(
            card("appearance", "Appearance", "Panel shape and optional transparency.", false, "",
                    dropdown("system-appearance", "Appearance", "Adjust panel shape and GUI transparency.", "appearance transparency glass",
                            toggle("appearance-corners-on", "Corner Radius", "Use custom panel corners.", "radius corners"),
                            slider("appearance-corners", "Corner Radius Value", "Set panel roundness.", "radius corners", 4, 18, 1, " px"),
                            toggle("appearance-opacity-on", "Custom GUI Transparency", "Optionally use a custom transparent glass amount.", "gui transparency"),
                            slider("appearance-opacity", "GUI Transparency", "Lower values show more of the game behind the GUI.", "glass opacity transparency", 45, 100, 5, "% opacity"),
                            reset("appearance-reset"))),
            card("performance", "Performance", "Interface quality and motion.", false, "",
                    dropdown("system-performance", "Performance", "Adjust quality, shadows, and motion.", "performance gui",
                            choice("curve-quality", "Curve Quality", "Choose corner smoothness.", "curve quality",
                                    new String[]{"Smooth", "Balanced", "Fast"}),
                            toggle("shadows", "Soft Shadows", "Show shadows behind panels.", "shadows"),
                            toggle("animations", "Animations", "Show smooth transitions.", "animations"),
                            toggle("animation-speed-on", "Animation Speed", "Use custom animation speed.", "animation speed"),
                            slider("animation-speed", "Animation Speed Value", "Set transition speed.", "animation speed", 40, 200, 10, "%"),
                            toggle("adaptive-quality", "Adaptive Quality", "Lower detail when FPS drops.", "adaptive quality"),
                            toggle("performance-boost", "Performance Boost", "Use lighter interface effects.", "performance beta"),
                            reset("performance-reset"))),
            card("general", "General", "Scrolling and commands.", false, "",
                    dropdown("system-general", "Interface", "Adjust scrolling preferences.", "scroll settings",
                            slider("global-scroll", "Scroll Speed", "Set settings scroll speed.", "scroll speed", 50, 300, 25, "%"),
                            toggle("smooth-scrolling", "Smooth Scrolling", "Use smooth settings scrolling.", "smooth scrolling"),
                            reset("system-general-reset")),
                    dropdown("system-commands", "Commands", "Chat commands and diagnostics.", "commands selftest debug",
                            toggle("sv-command", "/sv", "Open settings from chat.", "sv command"),
                            toggle("qn-command", "/qn", "Open notes from chat.", "qn command"),
                            info("command-itemdebug", "/velor itemdebug", "Show held-item values.", "item debug"),
                            info("command-lockdebug", "/velor lockdebug", "Show lock and swap details.", "lock debug"),
                            info("command-selftest", "/velor selftest", "Show feature details.", "selftest"),
                            reset("commands-reset")))
    );

    private static final List<Card> FARMING = Collections.emptyList();
    private static final Setting FARMING_SEARCH_RESULT = info("farming-empty", "No Farming Features Yet", "", "farming crop wheat sprout");
    private static final Card FARMING_SEARCH_CARD = card("farming-empty", "Farming", "", false, "", FARMING_SEARCH_RESULT);
    private static final String[] BANNED_PLAYER_WORDS = {
            "parsed", "cached", "srg", "hook", "wrap", "route", "status dropdown", "master switch", "stays local", "existing"
    };

    private ReferenceSettingsCatalog() { }

    public static List<Card> cards(Tab tab) {
        if (tab == null) return HOME;
        switch (tab) {
            case GAMEPLAY: return GAMEPLAY;
            case DUNGEON: return DUNGEON;
            case FARMING: return FARMING;
            case MINING: return MINING;
            case FISHING: return FISHING;
            case VISUALS: return VISUALS;
            case SYSTEM: return SYSTEM;
            default: return HOME;
        }
    }

    public static boolean hasRoute(String id) { return find(id) != null; }

    public static Setting find(String id) {
        if (id == null) return null;
        for (Tab tab : TABS) {
            for (Card card : cards(tab)) {
                Setting result = find(card.settings, id);
                if (result != null) return result;
            }
        }
        return null;
    }

    private static Setting find(List<Setting> settings, String id) {
        for (Setting setting : settings) {
            if (id.equals(setting.id)) return setting;
            Setting result = find(setting.children, id);
            if (result != null) return result;
        }
        return null;
    }

    /** Search every actual option, including options inside a collapsed owning dropdown. */
    public static List<Match> search(String value) {
        String query = normalizedSearchText(value);
        if (query.length() == 0) return Collections.emptyList();
        List<Match> matches = new ArrayList<Match>();
        for (Tab tab : TABS) {
            if (tab == Tab.HOME) continue;
            if (tab == Tab.FARMING) {
                if (matchesOwn(FARMING_SEARCH_RESULT, query)) matches.add(new Match(tab, FARMING_SEARCH_CARD,
                        FARMING_SEARCH_RESULT, Collections.<Setting>emptyList()));
                continue;
            }
            for (Card card : cards(tab)) search(card.settings, tab, card, query, new ArrayList<Setting>(), matches);
        }
        return Collections.unmodifiableList(matches);
    }

    private static void search(List<Setting> settings, Tab tab, Card card, String query, List<Setting> parents,
                               List<Match> out) {
        for (Setting setting : settings) {
            if (matchesOwn(setting, query)) out.add(new Match(tab, card, setting, parents));
            List<Setting> next = new ArrayList<Setting>(parents);
            if (setting.isDropdown()) next.add(setting);
            search(setting.children, tab, card, query, next, out);
        }
    }

    private static boolean matchesOwn(Setting setting, String query) {
        if (setting.isCaption()) return false;
        String text = normalizedSearchText(setting.id + " " + setting.label + " " + setting.description + " " + setting.keywords);
        if (text.contains(query)) return true;
        String[] words = query.split(" ");
        for (String word : words) {
            if (word.length() > 0 && !text.contains(word)) return false;
        }
        return true;
    }

    /** Normalizes presentation ids such as chat-tab-filter-text into the same searchable words users type. */
    private static String normalizedSearchText(String value) {
        if (value == null) return "";
        String lower = value.toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(lower.length());
        boolean spaced = true;
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                out.append(c);
                spaced = false;
            } else if (!spaced) {
                out.append(' ');
                spaced = true;
            }
        }
        int length = out.length();
        if (length > 0 && out.charAt(length - 1) == ' ') out.setLength(length - 1);
        return out.toString();
    }

    /** Compatibility overload; there is no advanced visibility gate. */
    @Deprecated public static List<Match> search(String value, boolean ignored) { return search(value); }

    public static boolean routesAreComplete() {
        for (String id : REQUIRED_ROUTE_IDS) if (!hasRoute(id)) return false;
        return true;
    }

    /** Returns all presentation ids in deterministic catalog order. */
    public static List<String> allIds() {
        List<String> values = new ArrayList<String>();
        for (Tab tab : TABS) for (Card card : cards(tab)) collectIds(card.settings, values);
        return Collections.unmodifiableList(values);
    }

    /** Returns the persisted feature-value ids, excluding captions, information, and one-shot actions. */
    public static List<String> configurableIds() {
        List<String> values = new ArrayList<String>();
        for (Tab tab : TABS) for (Card card : cards(tab)) collectConfigurableIds(card.settings, values);
        return Collections.unmodifiableList(values);
    }

    private static void collectIds(List<Setting> settings, List<String> values) {
        for (Setting setting : settings) {
            values.add(setting.id);
            collectIds(setting.children, values);
        }
    }

    private static void collectConfigurableIds(List<Setting> settings, List<String> values) {
        for (Setting setting : settings) {
            if (setting.control == Control.TOGGLE || setting.control == Control.SLIDER
                    || setting.control == Control.CHOICE || setting.control == Control.TEXT
                    || (setting.control == Control.EDITOR && setting.editorHasMasterToggle)) values.add(setting.id);
            collectConfigurableIds(setting.children, values);
        }
    }

    /** The rendered catalog has exactly one dropdown level. */
    public static int maxDropdownDepth() {
        int depth = 0;
        for (Tab tab : TABS) for (Card card : cards(tab)) {
            for (Setting setting : card.settings) depth = Math.max(depth, dropdownDepth(setting, 0));
        }
        return depth;
    }

    private static int dropdownDepth(Setting setting, int parentDepth) {
        int current = setting.isDropdown() ? parentDepth + 1 : parentDepth;
        int depth = current;
        for (Setting child : setting.children) depth = Math.max(depth, dropdownDepth(child, current));
        return depth;
    }

    public static boolean everyDropdownHasAtLeastTwoOptions() {
        for (Tab tab : TABS) for (Card card : cards(tab)) {
            for (Setting setting : card.settings) if (!dropdownsHaveAtLeastTwoOptions(setting)) return false;
        }
        return true;
    }

    private static boolean dropdownsHaveAtLeastTwoOptions(Setting setting) {
        if (setting.isDropdown() && setting.children.size() < 2) return false;
        for (Setting child : setting.children) if (!dropdownsHaveAtLeastTwoOptions(child)) return false;
        return true;
    }

    /** Every inline value selector is meaningful: a choice popup never opens for fewer than two values. */
    public static boolean everyChoiceHasAtLeastTwoOptions() {
        for (Tab tab : TABS) for (Card card : cards(tab)) {
            for (Setting setting : card.settings) if (!choicesHaveAtLeastTwoOptions(setting)) return false;
        }
        return true;
    }

    private static boolean choicesHaveAtLeastTwoOptions(Setting setting) {
        if (setting.control == Control.CHOICE && setting.choices.length < 2) return false;
        for (Setting child : setting.children) if (!choicesHaveAtLeastTwoOptions(child)) return false;
        return true;
    }

    /** Direct disclosure ids on a page, used by the persisted one-open accordion policy. */
    public static List<String> dropdownIds(Tab tab) {
        List<String> ids = new ArrayList<String>();
        for (Card card : cards(tab)) for (Setting setting : card.settings) {
            if (setting.isDropdown()) ids.add(setting.id);
        }
        return Collections.unmodifiableList(ids);
    }

    /** Finds the direct owner of a presentation id. */
    public static Tab ownerOf(String id) {
        if (id == null) return null;
        for (Tab tab : TABS) for (Card card : cards(tab)) if (find(card.settings, id) != null) return tab;
        return null;
    }

    /** Validates player-facing labels and descriptions without coupling the test to a GUI runtime. */
    public static List<String> playerTextViolations() {
        List<String> violations = new ArrayList<String>();
        for (Tab tab : TABS) {
            auditText("tab " + tab.name(), tab.label, true, violations);
            auditText("tab caption " + tab.name(), tab.caption, false, violations);
            for (Card card : cards(tab)) {
                auditText("section " + card.id, card.title, true, violations);
                auditText("section description " + card.id, card.subtitle, false, violations);
                auditText("section notice " + card.id, card.notice, false, violations);
                auditSettingsText(card.settings, violations);
            }
        }
        auditText("farming message", FARMING_SEARCH_RESULT.label, true, violations);
        return Collections.unmodifiableList(violations);
    }

    private static void auditSettingsText(List<Setting> settings, List<String> violations) {
        for (Setting setting : settings) {
            // Reset links are rendered from one shared player-facing label rather than a row label in the catalog.
            if (!setting.isResetAction()) {
                auditText("label " + setting.id, setting.label, true, violations);
                auditText("description " + setting.id, setting.description, false, violations);
            }
            auditSettingsText(setting.children, violations);
        }
    }

    private static void auditText(String id, String value, boolean required, List<String> violations) {
        String text = value == null ? "" : value.trim();
        if (required && text.length() == 0) violations.add(id + " is empty");
        if (text.length() > 60) violations.add(id + " is longer than 60 characters");
        String lower = text.toLowerCase(Locale.ROOT);
        for (String banned : BANNED_PLAYER_WORDS) {
            if (lower.contains(banned)) violations.add(id + " contains banned text: " + banned);
        }
    }

    /** All ids must remain unique, including noninteractive captions used for search-free grouping. */
    public static boolean idsAreUnique() {
        List<String> ids = allIds();
        return ids.size() == new HashSet<String>(ids).size();
    }

    private static Card card(String id, String title, String subtitle, boolean advanced, String notice, Setting... settings) {
        return new Card(id, title, subtitle, advanced, notice, false, settings);
    }

    private static Setting toggle(String id, String label, String description, String keywords) {
        return new Setting(id, label, description, keywords, Control.TOGGLE, false, "", 0, 1, 1, "", null,
                false, "");
    }

    private static Setting masterDropdown(String id, String label, String description, String keywords, Setting... children) {
        return new Setting(id, label, description, keywords, Control.TOGGLE, false, "", 0, 1, 1, "", null,
                false, "", children);
    }

    private static Setting dropdown(String id, String label, String description, String keywords, Setting... children) {
        return new Setting(id, label, description, keywords, Control.DROPDOWN, false, "", 0, 0, 0, "", null,
                false, "", children);
    }

    private static Setting slider(String id, String label, String description, String keywords,
                                  int min, int max, int step, String suffix) {
        return new Setting(id, label, description, keywords, Control.SLIDER, false, "", min, max, step, suffix, null,
                false, "");
    }

    private static Setting action(String id, String label, String description, String keywords, String actionLabel) {
        return new Setting(id, label, description, keywords, Control.ACTION, false, actionLabel, 0, 0, 0, "", null,
                false, "");
    }

    private static Setting reset(String id) {
        return action(id, "", "", "reset defaults", "Reset to defaults");
    }

    private static Setting choice(String id, String label, String description, String keywords, String[] choices) {
        return new Setting(id, label, description, keywords, Control.CHOICE, false, "", 0, 0, 0, "", choices,
                false, "");
    }

    private static Setting text(String id, String label, String description, String keywords) {
        return new Setting(id, label, description, keywords, Control.TEXT, false, "", 0, 0, 0, "", null,
                false, "");
    }

    private static Setting info(String id, String label, String description, String keywords) {
        return new Setting(id, label, description, keywords, Control.INFO, false, "", 0, 0, 0, "", null,
                false, "");
    }

    private static Setting caption(String id, String label) {
        return new Setting(id, label, "", label, Control.CAPTION, false, "", 0, 0, 0, "", null,
                false, "");
    }

    private static Setting editor(String id, String label, String description, String keywords,
                                  boolean hasToggle, String actionId) {
        return new Setting(id, label, description, keywords, Control.EDITOR, false, "Edit", 0, 1, 1, "", null,
                hasToggle, actionId);
    }

    /** A flat Starred Mobs group: category toggle followed by all of its style controls. */
    private static Setting[] mobStyleRows(String id, String label, String description) {
        return combine(new Setting[]{toggle(id, label, description, "starred mobs style")}, mobStyleControls(id, label));
    }

    /** Flat style controls are inserted immediately after each starred-target category. */
    private static Setting[] mobStyleControls(String id, String label) {
        return new Setting[]{
                choice(id + "-color", label + " Color", "Choose an outline color.", "esp color",
                        new String[]{"Cyan", "Gold", "Purple", "Green", "Orange", "Teal", "Red", "Pink"}),
                choice(id + "-mode", label + " Render Mode", "Choose outline, fill, or both.", "esp render mode",
                        new String[]{"Outline", "Filled", "Outline And Filled"}),
                toggle(id + "-walls", label + " Through Walls", "Show this category through blocks.", "esp walls"),
                slider(id + "-width", label + " Outline Width", "Set outline thickness.", "esp width", 1, 6, 1, " px"),
                slider(id + "-outline-opacity", label + " Outline Opacity", "Set outline opacity.", "esp opacity", 0, 100, 5, "%"),
                slider(id + "-fill-opacity", label + " Fill Opacity", "Set fill opacity.", "esp opacity", 0, 100, 5, "%")
        };
    }

    private static Setting[] doorStyleRows(String toggleId, String styleId, String label, String description) {
        return combine(new Setting[]{toggle(toggleId, label, description, "door style")}, doorStyleControls(styleId, label));
    }

    private static Setting[] doorStyleControls(String id, String label) {
        return new Setting[]{
                choice(id + "-color", label + " Color", "Choose an outline color.", "door color",
                        new String[]{"Cyan", "Gold", "Purple", "Green", "Orange", "Teal", "Red", "Pink"}),
                choice(id + "-mode", label + " Render Mode", "Choose outline, fill, or both.", "door render mode",
                        new String[]{"Outline", "Filled", "Outline And Filled"}),
                toggle(id + "-walls", label + " Through Walls", "Show doors through blocks.", "door walls"),
                slider(id + "-width", label + " Outline Width", "Set outline thickness.", "door width", 1, 6, 1, " px"),
                slider(id + "-outline-opacity", label + " Outline Opacity", "Set outline opacity.", "door opacity", 0, 100, 5, "%"),
                slider(id + "-fill-opacity", label + " Fill Opacity", "Set fill opacity.", "door opacity", 0, 100, 5, "%")
        };
    }

    private static Setting[] combine(Setting[] first, Setting[]... more) {
        int length = first == null ? 0 : first.length;
        if (more != null) for (Setting[] values : more) length += values == null ? 0 : values.length;
        Setting[] combined = new Setting[length];
        int at = 0;
        if (first != null) {
            System.arraycopy(first, 0, combined, 0, first.length);
            at += first.length;
        }
        if (more != null) for (Setting[] values : more) {
            if (values == null) continue;
            System.arraycopy(values, 0, combined, at, values.length);
            at += values.length;
        }
        return combined;
    }

    private static <T> List<T> immutable(T... values) {
        if (values == null || values.length == 0) return Collections.emptyList();
        return Collections.unmodifiableList(Arrays.asList(values.clone()));
    }

    private static List<Card> cards(Card... values) { return immutable(values); }
}
