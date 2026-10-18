package com.arena.pekaclient.config;

import com.arena.pekaclient.chat.ChatMenuPolicy;
import com.arena.pekaclient.feature.ChatTabs;

import java.util.Properties;

/** Headless persistence/migration proof for the simplified one-default-chat settings schema. */
public final class ChatQolSettingsTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        defaults();
        retiredSeparateKeysAreIgnored();
        customTabPersistence();
        invalidRegexAndBounds();
        done();
    }

    private static void defaults() {
        ChatQolSettings.load(new Properties());
        check("fresh master is opt-in", !ChatQolSettings.enabled);
        check("fresh settings use exactly built-in tabs", ChatQolSettings.tabs().size() == 3
                && ChatQolSettings.tab(ChatTabs.ALL) != null && ChatQolSettings.tab(ChatTabs.PARTY) != null
                && ChatQolSettings.tab(ChatTabs.REPLY) != null);
        check("default rounded background is vanilla-like", ChatQolSettings.backgroundRadius == 4
                && ChatQolSettings.backgroundOpacityOpen == 45 && ChatQolSettings.backgroundOpacityClosed == 25);
        check("default highlight is soft white", ChatQolSettings.hoverColor == 0xFFFFFF
                && ChatQolSettings.hoverOpacity == 25);
        check("default menu is Both at 100 ms", ChatQolSettings.menuOpenMode == ChatMenuPolicy.BOTH
                && ChatQolSettings.menuAnimationMillis == 100);
    }

    private static void retiredSeparateKeysAreIgnored() {
        Properties legacy = new Properties();
        legacy.setProperty("chat.qol.enabled", "true");
        legacy.setProperty("chat.qol.separate", "true");
        legacy.setProperty("chat.qol.window1.name", "Old Chat 1");
        legacy.setProperty("chat.qol.window2.name", "Old Chat 2");
        legacy.setProperty("chat.qol.patterns.count", "2");
        legacy.setProperty("chat.qol.patterns.0.regex", "[");
        legacy.setProperty("chat.qol.target.default.background.opacity", "99");
        legacy.setProperty("chat.qol.tabs.selected", "2");
        ChatQolSettings.load(legacy);
        check("old separate key does not create split tabs", ChatQolSettings.tabs().size() == 3);
        check("old selected tab key is ignored safely", ChatTabs.ALL.equals(ChatQolSettings.selectedTabId()));
        check("retired target visual key is ignored", ChatQolSettings.backgroundOpacityOpen == 45);
        check("old keys request rewrite", ChatQolSettings.isMigrationPending());
        ChatQolSettings.save(legacy);
        check("save drops separate key", !legacy.containsKey("chat.qol.separate")
                && !legacy.containsKey("chat.qol.window1.name") && !legacy.containsKey("chat.qol.patterns.count")
                && !legacy.containsKey("chat.qol.target.default.background.opacity"));
    }

    private static void customTabPersistence() {
        ChatQolSettings.resetDefaults();
        ChatQolSettings.Result created = ChatQolSettings.addCustomTab("Trade");
        check("custom tab added", created.success);
        ChatQolSettings.Result updated = ChatQolSettings.updateCustomTab(created.id, "Trade", ChatTabs.CONTAINS,
                "selling", "/pc", 0xFF9F0A, true, true);
        check("custom tab accepted", updated.success);
        ChatQolSettings.moveTab(created.id, -1);
        ChatQolSettings.setSelectedTab(created.id);
        Properties saved = new Properties();
        ChatQolSettings.save(saved);
        ChatQolSettings.load(saved);
        ChatTabs.Tab restored = ChatQolSettings.tab(created.id);
        check("custom tab survives save/load", restored != null && "Trade".equals(restored.name())
                && restored.matches("SELLING sword") && "/pc".equals(restored.sendPrefix()) && restored.sound());
        check("custom match remains in All", ChatTabs.appearsIn(ChatTabs.ALL, "SELLING sword", ChatQolSettings.tabs()));
        check("selected custom tab survives", created.id.equals(ChatQolSettings.selectedTabId()));
    }

    private static void invalidRegexAndBounds() {
        ChatQolSettings.resetDefaults();
        ChatQolSettings.setTabVisible(ChatTabs.PARTY, false);
        check("hidden built-in filter remains valid", !ChatQolSettings.tab(ChatTabs.PARTY).enabled()
                && ChatQolSettings.tab(ChatTabs.PARTY).matches("Party > Alex: hello"));
        ChatQolSettings.Result made = ChatQolSettings.addCustomTab("Regex");
        ChatQolSettings.Result invalid = ChatQolSettings.updateCustomTab(made.id, "Regex", ChatTabs.REGEX,
                "[", "", 0x123456, false, true);
        check("invalid regex is rejected", !invalid.success);
        for (int i = 0; i < ChatTabs.MAX_CUSTOM_TABS + 2; i++) ChatQolSettings.addCustomTab("More" + i);
        check("total tabs are capped at eight", ChatQolSettings.tabs().size() == ChatTabs.MAX_TABS);
        ChatQolSettings.backgroundRadius = 99;
        ChatQolSettings.messageAnimationMillis = 9999;
        ChatQolSettings.hoverOpacity = -4;
        Properties p = new Properties();
        ChatQolSettings.save(p);
        ChatQolSettings.load(p);
        check("visual values clamp", ChatQolSettings.backgroundRadius == 12
                && ChatQolSettings.messageAnimationMillis == 600 && ChatQolSettings.hoverOpacity == 0);
        for (ChatTabs.Tab tab : ChatQolSettings.tabs()) ChatQolSettings.setTabVisible(tab.id(), false);
        check("at least one tab remains visible", !ChatQolSettings.visibleTabs().isEmpty());
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++; else { failed++; System.err.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("chat QoL settings: passed " + passed + ", failed " + failed);
        if (failed > 0) throw new AssertionError("chat QoL settings failures: " + failed);
    }
}
