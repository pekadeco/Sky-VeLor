package com.arena.pekaclient.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Minecraft-free catalog of every Beta-0.10.8.7 control. The Hub uses these exact labels/descriptions so tests can
 * prove each option has a one-line explanation and belongs to exactly one matching tab/channel.
 */
public final class AssistUiModel {

    public static final String TAB_QUALITY_OF_LIFE = "Quality of Life";
    public static final String TAB_MINING = "Mining";
    public static final String TAB_DUNGEONS = "Dungeons";

    public static final class Entry {
        public final String tab;
        public final String group;
        public final String name;
        public final String description;

        private Entry(String tab, String group, String name, String description) {
            this.tab = tab;
            this.group = group;
            this.name = name;
            this.description = description;
        }
    }

    private static final List<Entry> ENTRIES = build();

    private AssistUiModel() {
    }

    public static List<Entry> entries() {
        return ENTRIES;
    }

    public static Entry find(String name) {
        for (Entry entry : ENTRIES) if (entry.name.equals(name)) return entry;
        return null;
    }

    private static List<Entry> build() {
        List<Entry> entries = new ArrayList<Entry>();
        add(entries, TAB_QUALITY_OF_LIFE, "Inventory assist", "Inventory search and filter",
                "Search item names and lore; non-matches are only dimmed");
        add(entries, TAB_QUALITY_OF_LIFE, "Inventory assist", "Hide potion effects in inventory",
                "Hide only the potion panel and recenter the player inventory");
        add(entries, TAB_QUALITY_OF_LIFE, "Inventory assist", "Roman numerals in tooltips",
                "Convert unambiguous tooltip level tokens while keeping colors");
        add(entries, TAB_QUALITY_OF_LIFE, "Inventory assist", "Scrollable item tooltips",
                "Use the wheel to read long lore without changing the hotbar");
        add(entries, TAB_QUALITY_OF_LIFE, "Inventory assist", "Item rarity highlight",
                "Draw a static rarity tint behind supported inventory items");
        add(entries, TAB_QUALITY_OF_LIFE, "Visual safety", "No hurt camera",
                "Disable only the local damage camera roll and shake");
        add(entries, TAB_QUALITY_OF_LIFE, "Item safety", "Item Protection",
                "Guard UUID-tagged SkyBlock items against destructive drops");
        add(entries, TAB_QUALITY_OF_LIFE, "Item safety", "Item Protection notifications",
                "Show local feedback when the item guard blocks an action");
        add(entries, TAB_QUALITY_OF_LIFE, "Movement and shortcuts", "Auto sprint",
                "Opt-in forward sprint assistance using vanilla movement conditions");
        add(entries, TAB_QUALITY_OF_LIFE, "Movement and shortcuts", "Wardrobe shortcut",
                "Allow the unbound Wardrobe key to send its command on Fakepixel");
        add(entries, TAB_QUALITY_OF_LIFE, "Movement and shortcuts", "Pets shortcut",
                "Allow the unbound Pets key to send its command on Fakepixel");

        add(entries, TAB_MINING, "Mining assistant", "Pickaxe ability ready alert",
                "Show a local title for supported Fakepixel pickaxe cooldowns");
        add(entries, TAB_MINING, "Mining assistant", "Puzzler direction helper",
                "Outline the decoded Dwarven Mines target block; never clicks it");
        add(entries, TAB_MINING, "Mining assistant", "Mining Assistant HUD",
                "Show mining rate, session blocks, and ability status in the HUD editor");

        add(entries, TAB_DUNGEONS, "Boss HP HUD", "Boss HP HUD",
                "Show detected F7/M7 boss and dragon health in the HUD editor");
        add(entries, TAB_DUNGEONS, "Dungeon Sync", "Enable Dungeon Sync",
                "Opt in to encrypted puzzle sync; player names are hashed locally and never uploaded");
        add(entries, TAB_DUNGEONS, "Dungeon Sync", "Dungeon Sync Debug",
                "Show local diagnostics without printing player names or session hashes");
        add(entries, TAB_DUNGEONS, "Terminal assist", "Terminal visual helpers",
                "Highlight parsed terminal choices while preserving normal input");
        add(entries, TAB_DUNGEONS, "Terminal assist", "Terminal auto click",
                "Explicitly allow automatic clicks for unambiguous parsed terminal choices");
        add(entries, TAB_DUNGEONS, "Terminal assist", "Hide incorrect terminal choices",
                "Dim known wrong choices in supported parsed terminal menus");
        add(entries, TAB_DUNGEONS, "Terminal assist", "Prevent wrong terminal clicks",
                "Block clicks on known wrong parsed choices without guessing");
        add(entries, TAB_DUNGEONS, "Terminal assist", "Terminal waypoints",
                "Show local terminal/device waypoints during supported dungeon phases");
        return Collections.unmodifiableList(entries);
    }

    private static void add(List<Entry> entries, String tab, String group, String name, String description) {
        entries.add(new Entry(tab, group, name, description));
    }
}
