package com.arena.pekaclient.gui;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Audits that flattening changed presentation only: every persisted option remains addressable. */
public final class InlineOptionPreservationTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        exactPersistedOptionInventoryRemains();
        requestedOwnersAndFlatCaptionsRemain();
        done();
    }

    private static void exactPersistedOptionInventoryRemains() {
        Set<String> expected = new HashSet<String>(Arrays.asList(
                "adaptive-quality", "animation-speed", "animation-speed-on", "animations", "appearance-corners",
                "appearance-corners-on", "appearance-opacity", "appearance-opacity-on", "auto-sprint", "catch-counter",
                "chat-background-closed-opacity", "chat-background-color", "chat-background-fit-width", "chat-background-open-area",
                "chat-background-open-opacity", "chat-background-radius", "chat-compact", "chat-compact-counter-color",
                "chat-fade-seconds", "chat-hover", "chat-hover-color", "chat-hover-opacity", "chat-menu-mode",
                "chat-message-animation", "chat-qol", "chat-smooth", "chat-tab-accent-color", "chat-tab-active-opacity",
                "chat-tab-filter-text", "chat-tab-filter-type", "chat-tab-inactive-color", "chat-tab-name", "chat-tab-opacity",
                "chat-tab-selected-color", "chat-tab-send-prefix", "chat-tab-sound", "chat-tab-visible", "chat-tabs",
                "curve-quality", "disable-hotbar-scrolling", "hide-dead-mobs", "smooth-aspect-teleport", "door-blood", "door-blood-color", "door-distance-text",
                "door-esp-enabled", "door-only-key", "door-scope", "door-wither-style", "dragon-alerts", "dungeon-boss-hud",
                "dungeon-map", "dungeon-map-player-names", "dungeon-map-player-heads", "dungeon-map-player-head-size", "hud-dungeon-run", "dungeon-map-puzzle-name", "dungeon-map-rotate", "dungeon-map-scale",
                "dungeon-map-scale-on", "dungeon-map-zoom", "dungeon-sync-debug", "dungeon-sync-enabled", "esp-distance-text",
                "esp-enderman", "esp-max-boxes", "esp-max-distance", "esp-max-mob-labels",
                "esp-miniboss", "esp-mob-labels", "esp-special", "esp-starred",
                "esp-update-interval", "fishing-festival",
                "global-scroll", "hide-potions", "hud-inventory", "hud-player-doll", "hurt-camera-shake", "item-animation",
                "item-lock-badge-color", "item-lock-badge-opacity", "item-lock-cooldown", "item-lock-hotbar", "item-lock-icons",
                "item-lock-line-color", "item-lock-line-mode", "item-lock-line-opacity", "item-lock-line-width", "item-lock-message",
                "item-lock-key", "item-lock-swap", "jawbus", "leap-key", "m7-areas-persist", "m7-boss-goldor", "m7-boss-maxor",
                "m7-boss-necron", "m7-boss-storm", "m7-dragon-apex", "m7-dragon-flame", "m7-dragon-ice", "m7-dragon-power",
                "m7-dragon-soul", "m7-overlay", "m7-relic-areas", "m7-terminal-solver", "mining-ability-alert",
                "mining-assistant-hud", "mining-bps", "mining-puzzler", "mob-esp-enabled", "nearby-encounters",
                "no-block-particles", "party-puzzle-announce", "performance-boost", "pets-shortcut", "qn-command",
                "rarity-highlight", "roman-numerals", "sea-hp-hud-route", "shadows", "slayer-boss-timer", "slayer-pricing",
                "slayer-tracker", "smooth-scrolling", "spirit-bow-locator", "spirit-leap", "sv-command", "terminal-auto-click",
                "terminal-hide-wrong", "terminal-hints", "terminal-prevent-clicks", "terminal-scale", "terminal-waypoints",
                "tooltip-scroll", "wardrobe-shortcut", "wardrobe-number-keys"));
        addStyleOptions(expected, "esp-starred");
        addStyleOptions(expected, "esp-miniboss");
        addStyleOptions(expected, "esp-special");
        addStyleOptions(expected, "esp-enderman");
        addStyleOptions(expected, "door-style");
        Set<String> actual = new HashSet<String>(ReferenceSettingsCatalog.configurableIds());
        check("persisted inventory includes Player Heads and omits the retired Command Keybind master", actual.equals(expected));
        check("the option inventory has no duplicate ids", actual.size() == ReferenceSettingsCatalog.configurableIds().size());
    }

    private static void requestedOwnersAndFlatCaptionsRemain() {
        assertPresent("chat-background-group", "chat-messages-group", "chat-tabs-group", "chat-menu-group",
                "m7-bosses", "m7-dragons", "esp-starred-caption", "door-wither-style-caption");
        assertPresent("item-lock-swap", "dungeon-map", "terminal-assist", "spirit-leap", "dungeon-boss-hud",
                "spirit-bow-locator", "m7-overlay", "mob-esp-enabled", "door-esp-enabled", "fishing-tracker",
                "hud-dungeon-run", "item-animation", "command-keybinds", "item-lock-key");
        check("Chat captions are not nested dropdowns", !ReferenceSettingsCatalog.find("chat-background-group").isDropdown()
                && !ReferenceSettingsCatalog.find("chat-tabs-group").isDropdown());
    }

    private static void addStyleOptions(Set<String> values, String prefix) {
        values.add(prefix + "-color");
        values.add(prefix + "-mode");
        values.add(prefix + "-walls");
        values.add(prefix + "-width");
        values.add(prefix + "-outline-opacity");
        values.add(prefix + "-fill-opacity");
    }

    private static void assertPresent(String... ids) {
        for (String id : ids) check("retained presentation id: " + id, ReferenceSettingsCatalog.find(id) != null);
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++; else { failed++; System.out.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("inline option preservation: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
