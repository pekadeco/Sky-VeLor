package com.arena.pekaclient.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Immutable presentation catalog for the native settings shell.
 *
 * <p>This catalog never owns a feature value. Every id is resolved by {@link AppleSettingsScreen} to an existing
 * configuration owner, HUD element, or established runtime action. The top level intentionally contains only
 * small toggle rows and collapsed dropdown rows; value controls live inside their owning dropdown.</p>
 */
public final class ReferenceSettingsCatalog {

    public enum Tab {
        HOME("Home", "Live performance and the handful of switches you touch most."),
        GAMEPLAY("Gameplay", "Comfort, inventory, chat and shortcuts."),
        DUNGEON("Dungeon", "Dungeon-only maps, helpers, overlays and ESP."),
        FARMING("Farming", "No farming features yet"),
        MINING("Mining", "Existing mining readouts and local helpers."),
        FISHING("Fishing", "Existing catch, encounter and sea-creature controls."),
        VISUALS("Visuals", "HUD layout and first-person item animation."),
        SYSTEM("System", "Appearance, performance, input and diagnostics.");

        public final String label;
        public final String caption;

        Tab(String label, String caption) {
            this.label = label;
            this.caption = caption;
        }
    }

    /** Child-control types. Top-level settings use only {@link #TOGGLE} or {@link #DROPDOWN}. */
    public enum Control {
        TOGGLE, SLIDER, ACTION, CHOICE, INFO, DROPDOWN, TEXT
    }

    public static final class Setting {
        public final String id;
        public final String label;
        public final String description;
        public final String keywords;
        public final Control control;
        public final boolean advanced;
        /** Label for a value action, never a focused-feature-editor route. */
        public final String actionLabel;
        public final int min;
        public final int max;
        public final int step;
        public final String suffix;
        public final String[] choices;
        public final List<Setting> children;

        private Setting(String id, String label, String description, String keywords, Control control,
                        boolean advanced, String actionLabel, int min, int max, int step, String suffix,
                        String[] choices, Setting... children) {
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
            this.children = immutable(children);
        }

        public boolean hasChildren() { return !children.isEmpty(); }
        public boolean isDropdown() { return control == Control.DROPDOWN || hasChildren(); }
        public boolean hasMasterToggle() { return control == Control.TOGGLE; }
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

    /** A search result carries every collapsed owner that must open before its option is visible. */
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

    /** Required direct owners; ids are presentation ids, not replacement configuration keys. */
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
            card("comfort", "Comfort", "Small local changes that do not alter ordinary play.", false, "",
                    toggle("no-block-particles", "Quiet block particles",
                            "Hide only digging and crack particles when a block breaks.", "particles digging cracks"),
                    toggle("disable-hotbar-scrolling", "Lock hotbar to keys",
                            "Ignore the mouse wheel while playing so the selected slot never jumps.", "mouse wheel hotbar"),
                    dropdown("hurt-camera", "Hurt camera", "Local vanilla hurt-camera tilt.", "damage camera tilt",
                            slider("hurt-camera-shake", "Hurt Camera Shake", "Scale the local vanilla tilt.",
                                    "damage camera tilt", 0, 100, 1, "%")),
                    toggle("auto-sprint", "Auto Sprint", "Sprints only while forward is held and vanilla allows it.",
                            "sprint movement")),
            card("inventory-chat", "Inventory and Chat", "All existing helpers stay local and configurable.", false, "",
                    masterDropdown("item-lock-swap", "Item Lock and Swap",
                            "Locks player-inventory positions and pairs two slots for a safe swap.", "lock pairing badges",
                            info("item-lock-key", "Item Lock & Swap key", "Configure the existing binding in Minecraft Controls, Sky VeLor.", "key controls"),
                            slider("item-lock-cooldown", "Swap Cooldown", "Time between manual pair swaps.", "swap cooldown", 5, 50, 1, ""),
                            toggle("item-lock-icons", "Show Lock Icons", "Padlock and numbered pair badges in player inventory slots.", "lock icons"),
                            toggle("item-lock-hotbar", "Show Icons on Hotbar", "Keep indicators visible in the in-game hotbar.", "hotbar icons"),
                            toggle("item-lock-message", "Blocked Message", "Chat feedback when an action is blocked or a pair changes.", "message"),
                            choice("item-lock-badge-color", "Badge Tint", "Badge colour.", "badge color",
                                    new String[]{"Gold", "Blue", "Purple", "Green", "Orange", "Teal"}),
                            slider("item-lock-badge-opacity", "Badge Transparency", "Badge opacity.", "badge opacity", 20, 100, 5, "%"),
                            choice("item-lock-line-mode", "Swap Line Mode", "Always visible, on hover, or hidden.", "swap lines",
                                    new String[]{"Always", "On hover", "Off"}),
                            choice("item-lock-line-color", "Swap Line Tint", "Tint for persistent pair-center lines.", "swap line color",
                                    new String[]{"Gold", "Blue", "Purple", "Green", "Orange", "Teal"}),
                            slider("item-lock-line-opacity", "Swap Line Transparency", "Persistent pair-line opacity.", "swap line opacity", 20, 100, 5, "%"),
                            slider("item-lock-line-width", "Swap Line Thickness", "Rounded persistent pair-line thickness.", "swap line width", 1, 4, 1, " px"),
                            action("item-lock-clear-locks", "Clear Locks", "Removes every saved position lock.", "clear locks", "Clear Locks"),
                            action("item-lock-clear-pairs", "Clear Pairs", "Removes every saved item-swap pair.", "clear pairs", "Clear Pairs"),
                            action("item-lock-reset", "Reset Item Lock and Swap", "Restore the existing lock, badge, line and key defaults.", "lock reset", "Reset")),
                    dropdown("inventory-assist", "Inventory Assist", "Potion-panel and tooltip helpers stay local.",
                            "tooltips potion rarity",
                            toggle("hide-potions", "Hide Potion Effects in Inventory", "Keep the inventory layout clean when effects are active.", "potion effects"),
                            toggle("roman-numerals", "Roman Numerals in Tooltips", "Render common enchanted-book levels as Roman numerals.", "enchant lore"),
                            toggle("tooltip-scroll", "Scrollable Item Tooltips", "Scroll long item lore instead of letting it run off screen.", "lore scroll"),
                            toggle("rarity-highlight", "Item Rarity Highlight", "A subtle tint on item slots based on rarity.", "rarity item"),
                            action("inventory-assist-reset", "Reset Inventory Assist", "Restore the existing inventory-helper defaults.", "inventory reset", "Reset")),
                    masterDropdown("chat-qol", "Chat QoL", "Readable messages and attached filter tabs.", "chat tabs smooth hover",
                            dropdown("chat-background-group", "Background", "Message background colour, opacity and shape.", "chat background",
                                    text("chat-background-color", "Background colour", "Hex colour, for example #000000.", "background color"),
                                    slider("chat-background-open-opacity", "Open opacity", "Opacity while chat is open.", "chat open opacity", 0, 100, 5, "%"),
                                    slider("chat-background-closed-opacity", "Closed opacity", "Opacity while chat is closed.", "chat closed opacity", 0, 100, 5, "%"),
                                    slider("chat-background-radius", "Corner radius", "Rounded message background radius.", "chat radius", 0, 12, 1, " px"),
                                    choice("chat-background-open-area", "Open chat background", "Fit to messages or fill the full open area.", "chat area",
                                            new String[]{"Fit to messages", "Full area"}),
                                    toggle("chat-background-fit-width", "Fit width to text", "Use the message width for the background.", "chat width")),
                            dropdown("chat-messages-group", "Messages", "Motion, fading and compact message details.", "chat messages",
                                    slider("chat-message-animation", "Message animation", "Animation duration; zero disables it.", "message motion", 0, 600, 20, " ms"),
                                    slider("chat-fade-seconds", "Fade-out", "Seconds until ordinary messages fade; zero disables it.", "message fade", 0, 60, 1, " s"),
                                    toggle("chat-smooth", "Use smooth messages", "Fade and slide new messages in without moving old ones.", "smooth messages"),
                                    toggle("chat-compact", "Compact messages", "Show compact message counters.", "compact messages"),
                                    text("chat-compact-counter-color", "Compact counter colour", "Hex colour for compact counters.", "counter color")),
                            dropdown("chat-tabs-group", "Tabs", "Attached All / Party / Reply tabs and custom tab editing.", "chat tabs custom",
                                    toggle("chat-tabs", "Use chat tabs", "Keep filters attached to open chat instead of a second window.", "party reply"),
                                    text("chat-tab-inactive-color", "Unselected tab colour", "Hex colour for inactive tabs.", "tab color"),
                                    text("chat-tab-selected-color", "Selected tab colour", "Hex colour for selected tabs.", "tab color"),
                                    slider("chat-tab-opacity", "Tab opacity", "Inactive tab opacity.", "tab opacity", 0, 100, 1, "%"),
                                    slider("chat-tab-active-opacity", "Active tab opacity", "Selected tab opacity.", "tab opacity", 0, 100, 1, "%"),
                                    action("chat-tab-previous", "Editing tab", "Select the previous existing tab.", "tab previous", "Previous"),
                                    action("chat-tab-next", "Next tab", "Select the next existing tab.", "tab next", "Next"),
                                    toggle("chat-tab-visible", "Visible", "Show the currently selected tab.", "tab visible"),
                                    action("chat-tab-add", "Add custom tab", "Add one custom tab using the values below.", "tab add", "Add"),
                                    text("chat-tab-name", "Name", "Custom tab name.", "tab name"),
                                    choice("chat-tab-filter-type", "Filter type", "Text matching rule for a custom tab.", "tab filter",
                                            new String[]{"Contains text", "Starts with", "Regex"}),
                                    text("chat-tab-filter-text", "Filter text", "Text or regular expression for the custom tab.", "tab filter"),
                                    text("chat-tab-send-prefix", "Send prefix", "Optional send prefix, for example /pc.", "tab prefix"),
                                    text("chat-tab-accent-color", "Custom accent colour", "Hex colour for this custom tab.", "tab accent"),
                                    toggle("chat-tab-sound", "Message sound", "Play the tab's existing message sound.", "tab sound"),
                                    action("chat-tab-save", "Save tab", "Save visibility and custom-tab values.", "tab save", "Save"),
                                    action("chat-tab-left", "Move left", "Move the selected tab earlier.", "tab move", "←"),
                                    action("chat-tab-right", "Move right", "Move the selected tab later.", "tab move", "→"),
                                    action("chat-tab-delete", "Delete custom", "Delete the selected custom tab; built-in tabs remain.", "tab delete", "Delete")),
                            dropdown("chat-menu-group", "Menu and Highlight", "Hover emphasis and captured-message menu input.", "chat menu highlight",
                                    toggle("chat-hover", "Use hover highlight", "Use a quiet background on the message under the pointer.", "hover highlight"),
                                    text("chat-hover-color", "Highlight colour", "Hex hover-highlight colour.", "hover color"),
                                    slider("chat-hover-opacity", "Highlight opacity", "Hover-highlight opacity.", "hover opacity", 0, 100, 5, "%"),
                                    choice("chat-menu-mode", "Open menu with", "Choose the existing captured-message menu input.", "chat menu",
                                            new String[]{"Right click", "Left click", "Both"})),
                            dropdown("chat-reset-group", "Reset", "Restore one existing group or all Chat QoL settings.", "chat reset",
                                    action("chat-reset-all", "Reset all Chat QoL", "Restore every existing Chat QoL setting and tab.", "reset all", "Reset All"),
                                    action("chat-reset-background", "Reset background", "Restore background settings.", "reset", "Reset"),
                                    action("chat-reset-messages", "Reset messages", "Restore message settings.", "reset", "Reset"),
                                    action("chat-reset-tabs", "Reset tabs", "Restore tabs and select All.", "reset", "Reset"),
                                    action("chat-reset-menu", "Reset menu/highlight", "Restore menu and hover settings.", "reset", "Reset"))),
                    dropdown("movement-shortcuts", "Movement and Shortcuts", "Optional command-key routes; unbound by default.", "wardrobe pets",
                            toggle("wardrobe-shortcut", "Wardrobe Shortcut", "Use the established optional Wardrobe route.", "wardrobe key"),
                            toggle("pets-shortcut", "Pets Shortcut", "Use the established optional Pets route.", "pets key"),
                            action("movement-shortcuts-reset", "Reset Movement Shortcuts", "Restore the existing shortcut defaults.", "shortcuts reset", "Reset")),
                    masterDropdown("slayer-tracker", "Slayer Tracker", "Track rare drops, price totals and the quest timer.", "slayer drops price timer",
                            toggle("slayer-boss-timer", "Boss Timer", "Count from boss spawn to Slayer quest completion.", "slayer timer"),
                            toggle("slayer-pricing", "Price Drops", "Show priced drops and allow the existing /setprice command.", "slayer price"),
                            info("slayer-set-price", "Set a Price", "Use /setprice for the held item, or /setprice <item name> <price>.", "setprice"),
                            dropdown("slayer-history", "Clear History", "Clear drops and profit for one tracked Slayer family.", "slayer clear history",
                                    action("slayer-clear-spider", "Spider", "Clear Spider drops and profit.", "spider clear", "Clear"),
                                    action("slayer-clear-revenant", "Revenant", "Clear Revenant drops and profit.", "revenant zombie clear", "Clear"),
                                    action("slayer-clear-blaze", "Blaze", "Clear Blaze drops and profit.", "blaze clear", "Clear"),
                                    action("slayer-clear-wolf", "Wolf", "Clear Wolf drops and profit.", "wolf sven clear", "Clear"),
                                    action("slayer-clear-enderman", "Enderman", "Clear Enderman drops and profit.", "enderman voidgloom clear", "Clear")),
                            action("slayer-reset", "Reset Slayer Tracker", "Restore the existing timer and pricing defaults.", "slayer reset", "Reset")))
    );

    private static final List<Card> DUNGEON = cards(
            card("run-tools", "Run Tools", "Map, terminal, leap and boss information for a dungeon run.", false, "",
                    masterDropdown("dungeon-map", "Dungeon Map", "Room shapes, puzzle labels and party heads on a cached map.", "map rooms party",
                            toggle("dungeon-map-rotate", "Rotate to facing", "Keep the current room pointing up.", "map rotate"),
                            toggle("dungeon-map-player-names", "Show player names", "Show names for party heads on the map.", "map names"),
                            toggle("dungeon-map-puzzle-name", "Show puzzle name", "Show parsed puzzle names.", "map puzzle"),
                            toggle("dungeon-map-scale-on", "Use map scale", "Enable the saved map scale value.", "map scale"),
                            slider("dungeon-map-scale", "Map scale", "Map pixel scale.", "map scale", 70, 250, 5, "%"),
                            slider("dungeon-map-zoom", "Map zoom", "Cached map zoom.", "map zoom", 50, 200, 5, "%"),
                            toggle("party-puzzle-announce", "Party puzzle announce", "Use the existing optional party puzzle announcement.", "puzzle announce")),
                    dropdown("terminal-assist", "Terminal Helper", "Status dropdown: visual helpers and safeguards have no master switch.",
                            "terminal f7 m7 status",
                            toggle("terminal-hints", "Terminal Visual Helpers", "Outline parsed choices while preserving normal input.", "terminal hint"),
                            toggle("terminal-auto-click", "Terminal Auto Click", "Existing opt-in terminal automation setting; off by default.", "terminal auto click"),
                            toggle("terminal-hide-wrong", "Hide Incorrect Terminal Choices", "Dim known-wrong choices when parsing is unambiguous.", "terminal dim"),
                            toggle("terminal-prevent-clicks", "Prevent Wrong Terminal Clicks", "Cancel a click only for a known wrong parsed target.", "terminal prevent"),
                            toggle("terminal-waypoints", "Terminal Waypoints", "Show existing in-world route markers.", "terminal waypoint"),
                            slider("terminal-scale", "Terminal Scale", "Terminal helper overlay size.", "terminal scale", 70, 250, 5, "%"),
                            action("terminal-reset", "Reset Terminal Helper", "Restore the existing terminal-helper defaults.", "terminal reset", "Reset")),
                    masterDropdown("spirit-leap", "Spirit Leap Picker", "Replace only the server Spirit Leap chest with class-labelled heads.",
                            "leap teammate", toggle("leap-key", "Use Spirit Leap Key", "Keep the configured key route available.", "leap key"),
                            action("spirit-leap-reset", "Reset Spirit Leap", "Restore the existing Spirit Leap defaults.", "leap reset", "Reset")),
                    masterDropdown("dungeon-boss-hud", "Boss Health HUD", "Show the parsed boss readout for the current floor.", "boss hp hud",
                            info("dungeon-boss-hud-editor", "HUD placement", "Position and style stay in the permitted HUD Editor.", "hud editor"))),
            card("floor-seven", "Floor VII", "M7 overlays, dragon alerts and hitbox switches without a sample panel.", false, "",
                    masterDropdown("m7-overlay", "Floor VII Overlay", "Relic, device and dragon state at a glance.", "m7 floor seven dragons",
                            toggle("m7-relic-areas", "Relic Kill Areas", "Show five relic areas around dragon statues.", "relic area"),
                            toggle("m7-areas-persist", "Keep Areas After Dragons Spawn", "Keep relic areas after dragons appear.", "relic area"),
                            toggle("dragon-alerts", "Dragon Kill Alert", "Title when a dragon is close and sitting at its statue.", "dragon alert"),
                            toggle("m7-terminal-solver", "Click in Order Solver", "Outline only the next parsed chest slot; never clicks it.", "terminal solver"),
                            dropdown("m7-bosses", "Boss Hitboxes", "Choose each Floor VII boss outline.", "m7 boss",
                                    toggle("m7-boss-maxor", "Maxor", "Outline Maxor.", "maxor"),
                                    toggle("m7-boss-storm", "Storm", "Outline Storm.", "storm"),
                                    toggle("m7-boss-goldor", "Goldor", "Outline Goldor.", "goldor"),
                                    toggle("m7-boss-necron", "Necron", "Outline Necron.", "necron")),
                            dropdown("m7-dragons", "Dragon Hitboxes", "Choose each Floor VII dragon outline.", "m7 dragon",
                                    toggle("m7-dragon-power", "Power", "Outline the Power dragon.", "power dragon"),
                                    toggle("m7-dragon-apex", "Apex", "Outline the Apex dragon.", "apex dragon"),
                                    toggle("m7-dragon-soul", "Soul", "Outline the Soul dragon.", "soul dragon"),
                                    toggle("m7-dragon-ice", "Ice", "Outline the Ice dragon.", "ice dragon"),
                                    toggle("m7-dragon-flame", "Flame", "Outline the Flame dragon.", "flame dragon")),
                            action("m7-reset", "Reset Floor VII Overlay", "Restore the existing M7 overlay defaults.", "m7 reset", "Reset"))),
            card("esp", "Dungeon ESP", "Compact Mob and Door tables retain every cached style value.", false,
                    "Unknown room or entity data fails closed: no box and no state change.",
                    masterDropdown("mob-esp-enabled", "Mob ESP", "Cached mob scanning and compact per-category styles.", "mob esp starred miniboss",
                            slider("esp-max-distance", "Maximum ESP Distance", "Scan only entities and doors inside this range.", "esp distance", 8, 160, 1, " blocks"),
                            slider("esp-update-interval", "Update Interval", "Cached scan period.", "esp interval", 1, 20, 1, " ticks"),
                            toggle("esp-hide-zero-health", "Hide Zero-Health Mobs", "Drop cached targets at zero health.", "esp health"),
                            toggle("esp-tracers", "Tracers", "Draw a world-space line to tracked mobs.", "esp tracer"),
                            toggle("esp-mob-labels", "Mob Category Labels", "Show compact category badges.", "esp labels"),
                            slider("esp-max-mob-labels", "Maximum Mob Labels", "Cap category badges.", "esp labels", 1, 32, 1, ""),
                            toggle("esp-distance-text", "Distance Text", "Add rounded block distance to mob badges.", "esp distance text"),
                            slider("esp-max-boxes", "Maximum Boxes", "Cap displayed hitboxes without dropping the cache.", "esp boxes", 8, 128, 1, ""),
                            action("esp-default-preset", "Default Preset", "Restore high-visibility defaults.", "esp preset", "Apply Default"),
                            action("esp-subtle-preset", "Subtle Preset", "Use low-noise depth-tested outlines.", "esp preset", "Apply Subtle"),
                            action("esp-reset", "Reset Dungeon ESP", "Restore all Dungeon ESP values.", "esp reset", "Reset"),
                            mobStyle("esp-starred", "Starred Mob", "Standard starred dungeon mobs."),
                            mobStyle("esp-miniboss", "Miniboss", "Dungeon miniboss hitboxes."),
                            mobStyle("esp-special", "Special Starred Mob", "Named special starred enemies."),
                            mobStyle("esp-enderman", "Enderman", "Real and head-marker Endermen."),
                            mobStyle("esp-skeletor", "Skeletor", "Starred Skeletors."),
                            masterDropdown("esp-other", "Other Mobs", "Optional room-wide hostile coverage.", "other mobs",
                                    combine(styleChildren("esp-other", "Other Mobs"),
                                            toggle("esp-force-all", "Force Render All Mobs in Room", "Off by default; cache eligible room hostiles.", "esp force"),
                                            toggle("esp-include-bats", "Include Bats", "Applies while force-rendering room mobs.", "esp bats"),
                                            toggle("esp-include-invisible", "Include Invisible Mobs", "Applies while force-rendering room mobs.", "esp invisible")))),
                    masterDropdown("door-esp-enabled", "Door ESP", "Cached closed Wither and Blood door table.", "door esp wither blood",
                            choice("door-scope", "Door Scope", "Nearest closed door or every cached closed door.", "door scope",
                                    new String[]{"Nearest closed door", "All closed doors"}),
                            toggle("door-only-key", "Only With a Wither Key", "Hide cached doors until a Wither Key is in inventory.", "door key"),
                            toggle("door-distance-text", "Door Distance Text", "Show rounded distance above closed doors.", "door distance"),
                            action("door-default-preset", "Default Preset", "Restore Dungeon ESP defaults including doors.", "door preset", "Apply Default"),
                            action("door-subtle-preset", "Subtle Preset", "Use the low-noise Dungeon ESP preset.", "door preset", "Apply Subtle"),
                            action("door-reset", "Reset Door ESP", "Restore shared Dungeon ESP defaults.", "door reset", "Reset"),
                            masterDropdown("door-wither-style", "Wither Door Style", "Shared cached Wither-door style.", "wither style",
                                    doorStyleChildren("door-style", "Wither Door")),
                            masterDropdown("door-blood", "Blood Doors", "Cached red-clay Blood doors using the shared Door Style above.", "blood door",
                                    choice("door-blood-color", "Blood Door Color", "Colour used for Blood door boxes.", "blood color",
                                            new String[]{"Cyan", "Gold", "Purple", "Green", "Orange", "Teal", "Red", "Pink"})))),
            card("party-sync", "Party Sync", "Optional puzzle sharing; off by default and visible in diagnostics.", false,
                    "No transport starts until enabled and a valid dungeon context exists.", true,
                    masterDropdown("dungeon-sync-enabled", "Dungeon Sync", "Share allow-listed puzzle labels and room coordinates over TLS.",
                            "sync privacy network", toggle("dungeon-sync-debug", "Sync Diagnostics", "Session state and transport retries for support only.", "sync debug"),
                            info("dungeon-sync-endpoint", "Endpoint", "Uses the existing validated secure endpoint from configuration.", "sync endpoint")))
    );

    private static final List<Card> MINING = cards(
            card("mining", "Mining", "Existing readouts and read-only local helpers.", false, "",
                    masterDropdown("mining-bps", "Mining Speed", "Show the existing blocks-per-second readout while mining.", "mining bps",
                            info("mining-bps-hud", "HUD placement", "Position and style stay in the permitted HUD Editor.", "hud editor")),
                    dropdown("mining-assistant-settings", "Mining Assistant", "Alerts, Puzzler marker and HUD route.", "pickaxe puzzler",
                            toggle("mining-ability-alert", "Pickaxe Ability Ready Alert", "A local cue when the ability is charged.", "ability alert"),
                            toggle("mining-puzzler", "Puzzler Direction Helper", "Show a suggested Dwarven Mines direction; never clicks or walks.", "dwarven route"),
                            toggle("mining-assistant-hud", "Show Mining Assistant", "Add the existing readout to the HUD layer.", "mining hud"),
                            action("mining-assistant-reset", "Reset Mining Assistant", "Restore the existing assistant defaults.", "mining reset", "Reset")))
    );

    private static final List<Card> FISHING = cards(
            card("fishing", "Fishing", "Existing catch counters, creature health and encounter alerts.", false, "",
                    dropdown("fishing-tracker", "Fishing Tracker", "Catch counters, sea-creature health and alerts.", "fishing catches sea creature",
                            masterDropdown("catch-counter", "Catch Counter", "Count every rarity locally, including festival catches.", "fishing counter",
                                    toggle("fishing-festival", "Festival Best", "Great White Sharks in the current Festival and your best.", "festival"),
                                    action("fishing-reset", "Reset Counters", "Clear every catch count and Festival best.", "catch reset", "Reset")),
                            masterDropdown("sea-hp-hud-route", "Sea Creature Health", "Show health for the current catch on the HUD layer.", "sea hp hud",
                                    info("sea-hp-placement", "HUD placement", "Position and style stay in the permitted HUD Editor.", "hud editor")),
                            toggle("jawbus", "Lord Jawbus Alert", "Show a local title when Lord Jawbus arrives.", "jawbus"),
                            toggle("nearby-encounters", "Nearby Encounters", "Show compatible nearby fishing title alerts.", "fishing nearby")))
    );

    private static final List<Card> VISUALS = cards(
            card("hud", "HUD", "Only the HUD Editor remains as a dedicated editor screen.", false, "",
                    dropdown("hud-editor", "HUD Editor", "Select, move, resize and style existing HUD elements.", "hud editor",
                            action("open-hud-editor", "Open HUD Editor", "Open the permitted HUD Editor screen.", "hud editor", "HUD Editor"),
                            masterDropdown("hud-player-doll", "Player Doll", "A live model with armour and held item.", "doll armour",
                                    info("hud-player-doll-placement", "Placement and style", "Edit in the HUD Editor.", "hud editor")),
                            masterDropdown("hud-inventory", "Inventory HUD", "The 27 main inventory slots and optional armour.", "inventory hud",
                                    info("hud-inventory-placement", "Placement and style", "Edit in the HUD Editor.", "hud editor")))),
            card("item-animation", "Item Animation", "First-person position, size and equip tempo; vanilla at defaults.", false, "",
                    masterDropdown("item-animation", "Enable Item Animation", "Wrap only Minecraft's first-person hand call; third person stays vanilla.",
                            "hand first person", action("item-animation-open-editor", "Open Item Animation Editor",
                                    "Open the permitted editor with real hand rendering and Preview Swing.", "item animation editor", "Item Animation Editor")))
    );

    private static final List<Card> SYSTEM = cards(
            card("appearance", "Appearance", "Neutral dark surfaces and a fixed blue focus.", false, "",
                    dropdown("system-appearance", "Appearance", "Existing shell radius and opacity preferences.", "appearance",
                            toggle("appearance-corners-on", "Corner Radius", "Use the saved corner-radius value.", "radius corners"),
                            slider("appearance-corners", "Corner Radius Value", "Roundness of panels and controls.", "radius corners", 4, 18, 1, " px"),
                            toggle("appearance-opacity-on", "Panel Opacity", "Use the saved panel-opacity value.", "panel opacity"),
                            slider("appearance-opacity", "Panel Opacity Value", "Higher is more solid.", "glass opacity", 60, 100, 5, "%"),
                            action("appearance-reset", "Reset Appearance", "Restore the existing appearance defaults.", "appearance reset", "Reset"))),
            card("performance", "Performance", "Safe GUI-quality controls only; world rendering is unchanged.", false, "",
                    dropdown("system-performance", "Performance", "Curve quality, shadows and interface motion.", "performance gui",
                            choice("curve-quality", "Curve Quality", "Smoothness of rounded corners.", "curve quality",
                                    new String[]{"Smooth", "Balanced", "Fast"}),
                            toggle("shadows", "Soft Shadows", "Shadows behind panels and menus.", "shadows"),
                            toggle("animations", "Animations", "Smooth transitions and motion.", "animations"),
                            toggle("animation-speed-on", "Animation Speed", "Use the saved animation-speed value.", "animation speed"),
                            slider("animation-speed", "Animation Speed Value", "Tempo of transitions.", "animation speed", 40, 200, 10, "%"),
                            toggle("adaptive-quality", "Adaptive Quality", "Lower detail automatically when FPS drops.", "adaptive quality"),
                            toggle("performance-boost", "Performance Boost Beta", "Lower-cost Sky VeLor GUI/HUD work only.", "performance beta"),
                            action("performance-reset", "Reset Performance", "Restore the existing GUI-performance defaults.", "performance reset", "Reset"))),
            card("general", "General", "Input behavior and command routes.", false, "",
                    dropdown("system-general", "Interface", "Scrolling and other general settings.", "scroll settings",
                            slider("global-scroll", "Scroll Speed", "Mouse/touch scroll multiplier; Shift makes it fine.", "scroll speed", 50, 300, 25, "%"),
                            toggle("smooth-scrolling", "Smooth Scrolling", "Ease every Sky VeLor settings viewport.", "smooth scrolling"),
                            action("system-general-reset", "Reset Interface", "Restore the existing scrolling defaults.", "interface reset", "Reset")),
                    dropdown("system-commands", "Commands", "Optional chat command routes and diagnostics.", "commands selftest debug",
                            toggle("sv-command", "/sv", "Open the settings hub from chat.", "sv command"),
                            toggle("qn-command", "/qn", "Open the quick-note pad from chat.", "qn command"),
                            info("command-itemdebug", "/velor itemdebug", "Shows live Item Animation values used by the renderer.", "item debug"),
                            info("command-lockdebug", "/velor lockdebug", "Shows Item Lock/Swap settings and hook state.", "lock debug"),
                            info("command-selftest", "/velor selftest", "Shows active core-hook, tooltip, camera, terminal and sync status.", "selftest"),
                            action("commands-reset", "Reset Commands", "Restore the existing command defaults.", "commands reset", "Reset")))
    );

    private static final List<Card> FARMING = Collections.emptyList();
    private static final Setting FARMING_SEARCH_RESULT = info("farming-empty", "No farming features yet", "", "farming crop wheat sprout");
    private static final Card FARMING_SEARCH_CARD = card("farming-empty", "Farming", "", false, "", FARMING_SEARCH_RESULT);

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

    /** Search every actual option, including options hidden by a collapsed owning dropdown. */
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
            next.add(setting);
            search(setting.children, tab, card, query, next, out);
        }
    }

    private static boolean matchesOwn(Setting setting, String query) {
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

    /** Returns all ids in deterministic catalog order for option-preservation audits. */
    public static List<String> allIds() {
        List<String> values = new ArrayList<String>();
        for (Tab tab : TABS) for (Card card : cards(tab)) collectIds(card.settings, values);
        return Collections.unmodifiableList(values);
    }

    private static void collectIds(List<Setting> settings, List<String> values) {
        for (Setting setting : settings) {
            values.add(setting.id);
            collectIds(setting.children, values);
        }
    }

    private static Card card(String id, String title, String subtitle, boolean advanced, String notice, Setting... settings) {
        return new Card(id, title, subtitle, advanced, notice, false, settings);
    }

    private static Card card(String id, String title, String subtitle, boolean advanced, String notice,
                             boolean warningNotice, Setting... settings) {
        return new Card(id, title, subtitle, advanced, notice, warningNotice, settings);
    }

    private static Setting toggle(String id, String label, String description, String keywords) {
        return new Setting(id, label, description, keywords, Control.TOGGLE, false, "", 0, 1, 1, "", null);
    }

    private static Setting masterDropdown(String id, String label, String description, String keywords, Setting... children) {
        return new Setting(id, label, description, keywords, Control.TOGGLE, false, "", 0, 1, 1, "", null, children);
    }

    private static Setting dropdown(String id, String label, String description, String keywords, Setting... children) {
        return new Setting(id, label, description, keywords, Control.DROPDOWN, false, "", 0, 0, 0, "", null, children);
    }

    private static Setting slider(String id, String label, String description, String keywords,
                                  int min, int max, int step, String suffix) {
        return new Setting(id, label, description, keywords, Control.SLIDER, false, "", min, max, step, suffix, null);
    }

    private static Setting action(String id, String label, String description, String keywords, String actionLabel) {
        return new Setting(id, label, description, keywords, Control.ACTION, false, actionLabel, 0, 0, 0, "", null);
    }

    private static Setting choice(String id, String label, String description, String keywords, String[] choices) {
        return new Setting(id, label, description, keywords, Control.CHOICE, false, "", 0, 0, 0, "", choices);
    }

    private static Setting text(String id, String label, String description, String keywords) {
        return new Setting(id, label, description, keywords, Control.TEXT, false, "", 0, 0, 0, "", null);
    }

    private static Setting info(String id, String label, String description, String keywords) {
        return new Setting(id, label, description, keywords, Control.INFO, false, "", 0, 0, 0, "", null);
    }

    private static Setting mobStyle(String id, String label, String description) {
        return masterDropdown(id, label, description, "mob esp style", styleChildren(id, label));
    }

    private static Setting[] styleChildren(String id, String label) {
        return new Setting[]{
                choice(id + "-color", label + " Color", "Choose the existing ESP colour.", "esp color",
                        new String[]{"Cyan", "Gold", "Purple", "Green", "Orange", "Teal", "Red", "Pink"}),
                choice(id + "-mode", label + " Render Mode", "Draw outline, filled hitbox, or both.", "esp render mode",
                        new String[]{"Outline", "Filled", "Outline + Filled"}),
                toggle(id + "-walls", label + " Through Walls", "Keep this category visible behind dungeon blocks.", "esp walls"),
                slider(id + "-width", label + " Outline Width", "World-space outline thickness.", "esp width", 1, 6, 1, " px"),
                slider(id + "-outline-opacity", label + " Outline Opacity", "Opacity for hitbox edges.", "esp opacity", 0, 100, 5, "%"),
                slider(id + "-fill-opacity", label + " Fill Opacity", "Opacity for hitbox faces.", "esp opacity", 0, 100, 5, "%"),
                action(id + "-copy", "Copy " + label + " Style", "Copy this existing style for this session.", "esp copy", "Copy"),
                action(id + "-apply", "Apply Copied Style", "Apply the last copied style to this category.", "esp apply", "Apply"),
                action(id + "-apply-all", "Apply This Style to All", "Apply this style to every Mob ESP category.", "esp apply all", "Apply to All")
        };
    }

    /** Door styles share the same persisted Style values but intentionally have no Mob-only clipboard actions. */
    private static Setting[] doorStyleChildren(String id, String label) {
        return new Setting[]{
                choice(id + "-color", label + " Color", "Choose the existing ESP colour.", "door color",
                        new String[]{"Cyan", "Gold", "Purple", "Green", "Orange", "Teal", "Red", "Pink"}),
                choice(id + "-mode", label + " Render Mode", "Draw outline, filled hitbox, or both.", "door render mode",
                        new String[]{"Outline", "Filled", "Outline + Filled"}),
                toggle(id + "-walls", label + " Through Walls", "Keep cached doors visible behind dungeon blocks.", "door walls"),
                slider(id + "-width", label + " Outline Width", "World-space outline thickness.", "door width", 1, 6, 1, " px"),
                slider(id + "-outline-opacity", label + " Outline Opacity", "Opacity for hitbox edges.", "door opacity", 0, 100, 5, "%"),
                slider(id + "-fill-opacity", label + " Fill Opacity", "Opacity for hitbox faces.", "door opacity", 0, 100, 5, "%")
        };
    }

    private static Setting[] combine(Setting[] first, Setting... more) {
        int firstLength = first == null ? 0 : first.length;
        int moreLength = more == null ? 0 : more.length;
        Setting[] values = new Setting[firstLength + moreLength];
        if (firstLength > 0) System.arraycopy(first, 0, values, 0, firstLength);
        if (moreLength > 0) System.arraycopy(more, 0, values, firstLength, moreLength);
        return values;
    }

    private static <T> List<T> immutable(T... values) {
        if (values == null || values.length == 0) return Collections.emptyList();
        return Collections.unmodifiableList(Arrays.asList(values.clone()));
    }

    private static List<Card> cards(Card... values) { return immutable(values); }
}
