package com.arena.pekaclient.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Immutable presentation catalog ported from the supplied DeepSeek GUI reference.
 *
 * <p>The catalog deliberately describes presentation only: ids resolve to the existing Sky VeLor configuration
 * owners and focused editors in {@link AppleSettingsScreen}. It never introduces a second persistence schema.
 * Home is an aggregate/preset surface; the four real destination sections are Gameplay, Dungeon, Visuals and
 * System. Cards, inline detail rows, the Advanced gate and search wording mirror the reference implementation.</p>
 */
public final class ReferenceSettingsCatalog {

    public enum Tab {
        HOME("Home", "Presets and the handful of switches you touch most."),
        GAMEPLAY("Gameplay", "Comfort, inventory, chat and skill trackers — the everyday surface."),
        DUNGEON("Dungeon", "Everything that should only be alive inside Catacombs."),
        VISUALS("Visuals", "Your HUD and first-person hand. All rendering, no server state."),
        SYSTEM("System", "Appearance, performance and the settings you rarely need.");

        public final String label;
        public final String caption;

        Tab(String label, String caption) {
            this.label = label;
            this.caption = caption;
        }
    }

    public enum Control {
        TOGGLE, SLIDER, ACTION, CHOICE, INFO
    }

    public static final class Setting {
        public final String id;
        public final String label;
        public final String description;
        public final String keywords;
        public final Control control;
        public final boolean advanced;
        /** A compact route label; non-empty means the existing focused owner is also reachable from this row. */
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

        public boolean hasChildren(boolean showAdvanced) {
            for (Setting child : children) if (showAdvanced || !child.advanced) return true;
            return false;
        }

        public int visibleChildCount(boolean showAdvanced) {
            int count = 0;
            for (Setting child : children) if (showAdvanced || !child.advanced) count++;
            return count;
        }
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

    public static final class Match {
        public final Tab tab;
        public final Card card;
        public final Setting setting;

        private Match(Tab tab, Card card, Setting setting) {
            this.tab = tab;
            this.card = card;
            this.setting = setting;
        }
    }

    /** Exact reference destination order: Home plus four intent sections. */
    public static final Tab[] TABS = Tab.values();

    /**
     * Existing routes that must remain in the reference shell. This explicitly excludes retired No Hurt Cam,
     * Player Dollars and Inventory Search entries from the stale prototype; current Sky VeLor owners remain intact.
     */
    public static final String[] REQUIRED_ROUTE_IDS = {
            "appearance", "mod-settings", "performance",
            "no-block-particles", "disable-hotbar-scrolling", "hurt-camera-shake", "auto-sprint",
            "inventory-assist", "movement-shortcuts", "item-lock-swap", "chat-qol", "commands",
            "item-animation", "hud-editor", "hud-stats", "hud-player-doll", "hud-inventory",
            "mining-bps", "mining-assistant-settings", "door-esp", "mob-esp", "dungeon-sync-enabled",
            "m7-overlay", "spirit-leap", "terminal-assist", "dungeon-boss-hud", "dungeon-map",
            "dungeon-sync-debug", "slayer-tracker", "slayer-hud-route", "jawbus", "nearby-encounters",
            "catch-counter", "fishing-hud-route", "sea-hp-hud-route"
    };

    private static final List<Card> HOME = Collections.emptyList();
    private static final List<Card> GAMEPLAY = cards(
            card("comfort", "Comfort", "Tiny adjustments that remove friction without changing how the game plays.",
                    false, "",
                    toggle("no-block-particles", "Quiet block particles",
                            "Hide only digging and crack particles when a block breaks.", "particles digging cracks", false),
                    toggle("disable-hotbar-scrolling", "Lock hotbar to keys",
                            "Ignore the mouse wheel while playing so the selected slot never jumps.", "mouse wheel hotbar", false),
                    slider("hurt-camera-shake", "Hurt Camera Shake",
                            "Scale the local vanilla hurt-camera tilt from 0% to 100%.", "damage camera tilt", false,
                            0, 100, 1, "%"),
                    toggle("auto-sprint", "Sprint while holding forward",
                            "Uses only the normal sprint key while vanilla conditions allow it. No extra packets.",
                            "sprint movement", true)),
            card("inventory", "Inventory", "Locks, paired swaps and optional local tooltip helpers.", false, "",
                    routeToggle("item-lock-swap", "Item Lock and Swap",
                            "Lock a player-inventory position, or pair two slots for a safe swap.",
                            "lock pairing badges", false, "Open details",
                            toggle("item-lock-icons", "Show lock badges", "Draw a small padlock on locked positions.",
                                    "lock icons", false),
                            toggle("item-lock-hotbar", "Badges on the hotbar", "Keep lock badges visible while inventory is closed.",
                                    "hotbar icons", false),
                            action("item-swap-lines", "Swap connection lines", "Configure when paired-slot lines are drawn.",
                                    "swap lines", false, "Open details")),
                    routeAction("inventory-assist", "Inventory Assist",
                            "Potion-panel and tooltip helpers stay local to your inventory.",
                            "tooltips potion rarity", false, "Open details",
                            toggle("hide-potions", "Hide potion panel", "Keep the inventory layout clean when effects are active.",
                                    "potion effects", false),
                            toggle("roman-numerals", "Roman tooltip levels", "Render common enchanted-book levels as Roman numerals.",
                                    "enchant lore", false),
                            toggle("tooltip-scroll", "Scrollable tooltips", "Scroll long item lore instead of letting it run off screen.",
                                    "lore scroll", false),
                            toggle("rarity-highlight", "Rarity highlights", "A subtle tint on item slots based on rarity.",
                                    "rarity item", false))),
            card("chat", "Chat", "Readable messages, attached filters and one captured message menu.", false, "",
                    routeToggle("chat-qol", "Chat QoL",
                            "Rounded messages, smooth scrolling and attached All / Party / Reply filters.",
                            "chat tabs smooth hover", false, "Open chat settings",
                            toggle("chat-tabs", "Filter tabs", "Keep channel tabs attached to open chat instead of a second window.",
                                    "party reply", false),
                            toggle("chat-smooth", "Smooth message motion", "Fade and slide new messages in without moving old ones.",
                                    "animations messages", false),
                            toggle("chat-hover", "Highlight hovered line", "Use a quiet background on the message under the pointer.",
                                    "hover highlight", false))),
            card("skills", "Skills", "Mining, Slayer and fishing trackers, off until you enable them.", false, "",
                    routeToggle("mining-bps", "Mining speed", "Show the existing blocks-per-second readout while mining.",
                            "mining bps", false, "HUD settings"),
                    routeAction("mining-assistant-settings", "Mining assistant",
                            "Pickaxe-ready alert, read-only Puzzler marker and its HUD route.",
                            "pickaxe puzzler", false, "Open details",
                            toggle("mining-ability-alert", "Pickaxe ready alert", "A local cue when the ability is charged.",
                                    "ability alert", false),
                            toggle("mining-puzzler", "Puzzler route marker", "Show a suggested Dwarven Mines direction. Never clicks or walks.",
                                    "dwarven route", false),
                            toggle("mining-assistant-hud", "Mining assistant HUD", "Add the assistant readout to the movable HUD layer.",
                                    "hud", false)),
                    routeToggle("slayer-tracker", "Slayer tracker", "Local drop history, timer and price controls.",
                            "slayer boss timer", false, "Open details",
                            routeToggle("slayer-hud-route", "Slayer HUD", "Show the active tracker on the HUD layer.",
                                    "slayer hud", false, "HUD settings"),
                            toggle("slayer-boss-timer", "Boss timer", "Track spawn and kill time for each attempt.",
                                    "timer", false),
                            toggle("slayer-pricing", "Estimated item value", "Use only locally saved prices; nothing is fetched.",
                                    "prices", true)),
                    routeAction("fishing-hud-route", "Fishing tracker", "Catch counters, sea-creature health and alerts in one place.",
                            "fishing catches sea creature", false, "Open details",
                            routeAction("catch-counter", "Catch counter", "Count every rarity locally, including festival catches.",
                                    "fishing counter", false, "Open details"),
                            routeToggle("sea-hp-hud-route", "Sea creature health", "Show health for the current catch on the HUD layer.",
                                    "sea hp hud", false, "HUD settings"),
                            toggle("jawbus", "Lord Jawbus alert", "Show a local title when Lord Jawbus arrives.",
                                    "jawbus", false),
                            toggle("nearby-encounters", "Nearby encounters", "Show compatible nearby fishing title alerts. Useful, but busy.",
                                    "fishing nearby", true))),
            card("shortcuts", "Shortcuts", "Optional command keys. Nothing is bound on a fresh install.", true, "",
                    routeAction("movement-shortcuts", "Wardrobe and Pets", "Configure optional local shortcut routes.",
                            "wardrobe pets", false, "Open details",
                            toggle("wardrobe-shortcut", "Wardrobe shortcut", "Unbound by default. Choose a key in Controls.",
                                    "wardrobe key", false),
                            toggle("pets-shortcut", "Pets shortcut", "Unbound by default. Choose a key in Controls.",
                                    "pets key", false)))
    );

    private static final List<Card> DUNGEON = cards(
            card("run-tools", "Run tools", "The utilities you want during a run, not buried in a menu.", false, "",
                    routeToggle("dungeon-map", "Dungeon map", "Room shapes, puzzle labels and party heads on a cached map.",
                            "map rooms party", false, "Open map settings",
                            toggle("dungeon-map-rotate", "Rotate to facing", "Keep the current room pointing up.",
                                    "map rotate", false)),
                    routeAction("terminal-assist", "Terminal helper", "Visual helpers and explicit safeguards for parsed terminals.",
                            "terminal f7 m7", false, "Open details",
                            toggle("terminal-hints", "Highlight the next slot", "Only verified layouts are outlined. Unknown layouts stay vanilla.",
                                    "terminal hint", false),
                            toggle("terminal-hide-wrong", "Dim known wrong slots", "Dim known-wrong choices when the parsed state is unambiguous.",
                                    "terminal dim", false),
                            toggle("terminal-prevent-clicks", "Block known wrong clicks", "Cancel your click only when layout and target are unambiguous.",
                                    "terminal prevent", false),
                            toggle("terminal-auto-click", "Terminal auto click", "Existing opt-in automation control; kept Advanced and off by default.",
                                    "terminal automation", true),
                            toggle("terminal-waypoints", "Terminal waypoints", "Show existing in-world route markers when enabled.",
                                    "terminal waypoint", true)),
                    routeToggle("spirit-leap", "Spirit Leap picker", "Replace only the server leap chest with class-labelled party heads.",
                            "leap teammate", false, "Open details",
                            toggle("leap-key", "Leap hotkey", "Use the configured key route; left click remains vanilla.",
                                    "leap key", false)),
                    routeToggle("dungeon-boss-hud", "Boss health", "Show the parsed boss readout for the current floor.",
                            "boss hp hud", false, "HUD settings")),
            card("floor-seven", "Floor VII", "M7 overlays, dragon alerts and the small things that save a run.", false, "",
                    routeToggle("m7-overlay", "Floor VII overlay", "Relic, device and dragon state at a glance.",
                            "m7 floor seven dragons", false, "Open details",
                            toggle("dragon-alerts", "Dragon alerts", "Keep the existing local dragon-kill alert enabled.",
                                    "dragon alert", false),
                            toggle("m7-areas", "Relic kill areas", "Show the existing relic-area boxes.",
                                    "relic area", false))),
            card("highlights", "Highlights", "Cached boxes for the things that matter. Styles stay in one editor.", false,
                    "Unknown room or entity data fails closed: no box, no state change.",
                    routeAction("mob-esp", "Mob highlights", "Star mobs, minibosses and special mobs from the cached scan.",
                            "mob esp starred miniboss", false, "Open styles"),
                    routeAction("door-esp", "Door highlights", "Wither and blood doors with shared style controls.",
                            "door esp wither blood", false, "Open styles")),
            card("party-sync", "Party sync", "Optional puzzle sharing. Off by default and visible in diagnostics.", false,
                    "No transport starts until it is enabled and a valid dungeon context exists.", true,
                    routeToggle("dungeon-sync-enabled", "Dungeon Sync", "Share allow-listed puzzle labels and room coordinates over TLS.",
                            "sync privacy network", false, "Review privacy",
                            toggle("dungeon-sync-debug", "Sync diagnostics", "Session state and transport retries for support only.",
                                    "sync debug", true)))
    );

    private static final List<Card> VISUALS = cards(
            card("hud", "HUD", "Movable elements. Drag them once in the editor, then forget this page.", false, "",
                    action("hud-editor", "HUD Editor", "Select, move, resize and style existing HUD elements.",
                            "hud hub editor", false, "Open editor"),
                    routeToggle("hud-stats", "Stats Display", "Frame rate, mining BPS and memory cards matching the classic HUD.",
                            "stats fps mining memory", false, "HUD settings"),
                    routeToggle("hud-player-doll", "Player doll", "A live model of you, with armour and held item.",
                            "doll armour", false, "HUD settings"),
                    routeToggle("hud-inventory", "Inventory HUD", "The 27 main inventory slots and optional armour.",
                            "inventory hud", false, "HUD settings")),
            card("item-animation", "Item Animation", "First-person position, size and equip tempo. Vanilla at the defaults.", false, "",
                    routeToggle("item-animation", "Enable Item Animation", "Wrap only Minecraft's first-person hand call. Third person stays vanilla.",
                            "hand first person", false, "Open Item Animation",
                            slider("item-animation-size", "Item size", "Uniform first-person scale.", "size scale", true,
                                    30, 200, 5, "%"),
                            slider("item-animation-speed", "Equip tempo", "Visual interpolation speed only.", "speed equip", false,
                                    25, 400, 5, "%"),
                            slider("item-animation-x", "Horizontal offset", "Nudge the held item left or right.", "offset x", true,
                                    -150, 150, 1, ""),
                            slider("item-animation-y", "Vertical offset", "Nudge the held item up or down.", "offset y", true,
                                    -150, 150, 1, ""),
                            slider("item-animation-z", "Depth offset", "Nudge the held item closer or farther.", "offset z", true,
                                    -150, 150, 1, ""),
                            action("item-animation-reset", "Reset to vanilla", "Restore size, tempo and offsets to their saved defaults.",
                                    "reset hand", true, "Reset")))
    );

    private static final List<Card> SYSTEM = cards(
            card("appearance", "Appearance", "The Apple surfaces: glass, corners, accent and density.", false, "",
                    choice("appearance", "Theme", "Graphite for late-night dungeons, Frost for daylight.", "dark light graphite frost", false,
                            new String[]{"Graphite", "Frost"}),
                    choice("appearance-accent", "Accent", "The highlight used for selection and focus.", "accent color blue purple green", false,
                            new String[]{"Blue", "Purple", "Pink", "Green", "Orange", "Teal"}),
                    slider("appearance-corners", "Corner radius", "Soft Apple-style corners. Lower values feel more utilitarian.",
                            "radius corners", true, 4, 18, 1, " px"),
                    slider("appearance-opacity", "Panel opacity", "Higher is more readable; lower reveals more game behind it.",
                            "glass opacity", true, 60, 100, 1, "%")),
            card("performance", "Performance", "The safe knobs. The GUI is designed to stay smooth without them.", false, "",
                    toggle("performance-boost", "Performance Boost Beta", "Cached geometry and reduced redraw work. Leave off if anything looks wrong.",
                            "performance beta", false),
                    toggle("animations", "Interface animations", "Short eased transitions across Sky VeLor screens.",
                            "animations motion", false),
                    toggle("adaptive-quality", "Adaptive GUI quality", "Lower curve work and shadows when frame time rises.",
                            "adaptive quality", true),
                    toggle("shadows", "Soft shadows", "Panel depth without a full-screen blur pass.",
                            "shadows", true),
                    routeAction("performance", "Performance settings", "Curve quality and animation tempo remain in the focused editor.",
                            "curve animation speed", true, "Open details")),
            card("general", "General", "Input behaviour and the settings you actually understand.", false, "",
                    toggle("smooth-scrolling", "Smooth scrolling", "Ease list movement instead of jumping.",
                            "scroll smooth", false),
                    slider("global-scroll", "Scroll speed", "One speed for Hub lists, editors and chat.",
                            "scroll wheel", false, 50, 300, 10, "%"),
                    routeAction("mod-settings", "More interface settings", "Open the focused Mod Settings editor.",
                            "mod settings", false, "Open details")),
            card("diagnostics", "Diagnostics", "Developer and command surfaces, intentionally out of the everyday path.", true, "",
                    info("gui-metrics", "Client metrics", "Read-only frame rate, ping and memory telemetry. This never creates another HUD.",
                            "fps ping memory telemetry"),
                    routeAction("commands", "Client command routes", "Configure /sv, /qn and diagnostics.",
                            "commands selftest debug", false, "Open details"),
                    routeAction("sync-diagnostics", "Sync details", "View existing sync diagnostics without changing transport state.",
                            "sync debug", true, "Open details")),
            card("about", "About", "Version and client-side limits.", false, "",
                    info("about-version", "Sky VeLor settings", "Minecraft 1.8.9 + Forge client-side settings surface.",
                            "version forge"),
                    info("about-rules", "Use within server rules", "Visual helpers never guarantee server-rule compliance.",
                            "rules safety"))
    );

    private ReferenceSettingsCatalog() { }

    public static List<Card> cards(Tab tab) {
        if (tab == null) return HOME;
        switch (tab) {
            case GAMEPLAY: return GAMEPLAY;
            case DUNGEON: return DUNGEON;
            case VISUALS: return VISUALS;
            case SYSTEM: return SYSTEM;
            default: return HOME;
        }
    }

    public static boolean hasRoute(String id) {
        return find(id) != null;
    }

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

    public static List<Match> search(String value, boolean showAdvanced) {
        String query = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (query.length() == 0) return Collections.emptyList();
        List<Match> matches = new ArrayList<Match>();
        for (Tab tab : TABS) {
            if (tab == Tab.HOME) continue;
            for (Card card : cards(tab)) {
                if (card.advanced && !showAdvanced) continue;
                for (Setting setting : card.settings) {
                    if ((!setting.advanced || showAdvanced) && matches(setting, query)) {
                        matches.add(new Match(tab, card, setting));
                    }
                }
            }
        }
        return Collections.unmodifiableList(matches);
    }

    public static boolean routesAreComplete() {
        for (String id : REQUIRED_ROUTE_IDS) if (!hasRoute(id)) return false;
        return true;
    }

    private static boolean matches(Setting setting, String query) {
        StringBuilder text = new StringBuilder();
        append(text, setting.id); append(text, setting.label); append(text, setting.description); append(text, setting.keywords);
        for (Setting child : setting.children) {
            append(text, child.id); append(text, child.label); append(text, child.description); append(text, child.keywords);
        }
        return text.toString().toLowerCase(Locale.ROOT).contains(query);
    }

    private static void append(StringBuilder out, String value) {
        if (value != null) out.append(value).append(' ');
    }

    private static Card card(String id, String title, String subtitle, boolean advanced, String notice, Setting... settings) {
        return new Card(id, title, subtitle, advanced, notice, false, settings);
    }

    private static Card card(String id, String title, String subtitle, boolean advanced, String notice,
                             boolean warningNotice, Setting... settings) {
        return new Card(id, title, subtitle, advanced, notice, warningNotice, settings);
    }

    private static Setting toggle(String id, String label, String description, String keywords, boolean advanced,
                                  Setting... children) {
        return new Setting(id, label, description, keywords, Control.TOGGLE, advanced, "", 0, 1, 1, "", null, children);
    }

    private static Setting routeToggle(String id, String label, String description, String keywords, boolean advanced,
                                       String actionLabel, Setting... children) {
        return new Setting(id, label, description, keywords, Control.TOGGLE, advanced, actionLabel,
                0, 1, 1, "", null, children);
    }

    private static Setting slider(String id, String label, String description, String keywords, boolean advanced,
                                  int min, int max, int step, String suffix, Setting... children) {
        return new Setting(id, label, description, keywords, Control.SLIDER, advanced, "", min, max, step, suffix,
                null, children);
    }

    private static Setting action(String id, String label, String description, String keywords, boolean advanced,
                                  String actionLabel, Setting... children) {
        return new Setting(id, label, description, keywords, Control.ACTION, advanced, actionLabel,
                0, 0, 0, "", null, children);
    }

    private static Setting routeAction(String id, String label, String description, String keywords, boolean advanced,
                                       String actionLabel, Setting... children) {
        return action(id, label, description, keywords, advanced, actionLabel, children);
    }

    private static Setting choice(String id, String label, String description, String keywords, boolean advanced,
                                  String[] choices, Setting... children) {
        return new Setting(id, label, description, keywords, Control.CHOICE, advanced, "", 0, 0, 0, "", choices, children);
    }

    private static Setting info(String id, String label, String description, String keywords) {
        return new Setting(id, label, description, keywords, Control.INFO, false, "", 0, 0, 0, "", null);
    }

    private static List<Card> cards(Card... cards) {
        return immutable(cards);
    }

    private static <T> List<T> immutable(T... values) {
        if (values == null || values.length == 0) return Collections.emptyList();
        return Collections.unmodifiableList(Arrays.asList(values.clone()));
    }
}
