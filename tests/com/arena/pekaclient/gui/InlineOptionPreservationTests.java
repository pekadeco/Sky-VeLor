package com.arena.pekaclient.gui;

/** Audits the requested retained controls after their focused editors were folded inline. */
public final class InlineOptionPreservationTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        mapLeapBossAndM7ControlsRemain();
        chatGroupsAndCustomTabControlsRemain();
        itemLockSwapAndSystemControlsRemain();
        slayerAndChannelControlsRemain();
        done();
    }

    private static void mapLeapBossAndM7ControlsRemain() {
        assertPresent("dungeon-map", "dungeon-map-rotate", "dungeon-map-player-names", "dungeon-map-puzzle-name",
                "dungeon-map-scale", "dungeon-map-zoom", "spirit-leap", "leap-key", "dungeon-boss-hud",
                "m7-overlay", "m7-relic-areas", "m7-areas-persist", "dragon-alerts", "m7-terminal-solver",
                "m7-boss-maxor", "m7-boss-storm", "m7-boss-goldor", "m7-boss-necron", "m7-dragon-power",
                "m7-dragon-apex", "m7-dragon-soul", "m7-dragon-ice", "m7-dragon-flame", "m7-reset",
                "terminal-reset", "spirit-leap-reset");
    }

    private static void chatGroupsAndCustomTabControlsRemain() {
        assertPresent("chat-background-group", "chat-messages-group", "chat-tabs-group", "chat-menu-group", "chat-reset-group",
                "chat-background-color", "chat-message-animation", "chat-compact-counter-color", "chat-tab-inactive-color",
                "chat-tab-selected-color", "chat-tab-add", "chat-tab-name", "chat-tab-filter-type", "chat-tab-filter-text",
                "chat-tab-send-prefix", "chat-tab-accent-color", "chat-tab-sound", "chat-tab-save", "chat-tab-delete",
                "chat-hover-color", "chat-menu-mode", "chat-reset-all", "chat-reset-tabs");
    }

    private static void itemLockSwapAndSystemControlsRemain() {
        assertPresent("item-lock-swap", "item-lock-cooldown", "item-lock-icons", "item-lock-hotbar", "item-lock-message",
                "item-lock-badge-color", "item-lock-line-mode", "item-lock-line-width", "item-lock-clear-locks",
                "item-lock-clear-pairs", "item-lock-reset", "inventory-assist-reset", "movement-shortcuts-reset",
                "appearance-corners", "appearance-opacity", "appearance-reset", "curve-quality", "shadows",
                "animations", "animation-speed", "performance-reset", "global-scroll", "smooth-scrolling",
                "system-general-reset", "sv-command", "qn-command", "commands-reset");
    }

    private static void slayerAndChannelControlsRemain() {
        assertPresent("slayer-tracker", "slayer-boss-timer", "slayer-pricing", "slayer-clear-spider",
                "slayer-clear-revenant", "slayer-clear-blaze", "slayer-clear-wolf", "slayer-clear-enderman", "slayer-reset",
                "mining-bps", "mining-ability-alert", "mining-puzzler", "mining-assistant-reset", "catch-counter",
                "fishing-festival", "fishing-reset", "jawbus", "nearby-encounters");
    }

    private static void assertPresent(String... ids) {
        for (String id : ids) check("retained option: " + id, ReferenceSettingsCatalog.find(id) != null);
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++; else { failed++; System.out.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("inline option preservation: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
