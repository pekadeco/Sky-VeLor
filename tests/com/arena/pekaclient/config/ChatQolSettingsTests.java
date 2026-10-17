package com.arena.pekaclient.config;

import com.arena.pekaclient.feature.ChatFilterRules;
import com.arena.pekaclient.feature.ChatPatternRules;

import java.util.Properties;

/** Headless migration, clamping, per-GUI-scale position and native-only persistence proof for Chat QoL. */
public final class ChatQolSettingsTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        freshDefaults();
        legacyMigration();
        nativeClampingAndPersistence();

        System.out.println("chat QoL settings: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void freshDefaults() {
        ChatQolSettings.load(new Properties());
        check("fresh master is safely off", !ChatQolSettings.enabled);
        check("fresh tabs have three supported views", ChatQolSettings.tabsEnabled && ChatQolSettings.showAll
                && ChatQolSettings.showParty && ChatQolSettings.showReply);
        check("fresh separate windows are off", !ChatQolSettings.separateChats);
        check("fresh optional hover/menu/font are off", !ChatQolSettings.hoverHighlight
                && !ChatQolSettings.rightClickMenu && !ChatQolSettings.customFont);
        check("default patterns are present and compiled", ChatQolSettings.patternRuleCount() >= 2
                && ChatQolSettings.compiledPatterns().acceptedRules().size() >= 2);
    }

    private static void legacyMigration() {
        Properties old = new Properties();
        old.setProperty("chat.tabs.enabled", "true");
        old.setProperty("chat.tabMode", "3");
        old.setProperty("chat.animation.messages", "true");
        old.setProperty("chat.animation.messageSpeed", "85");
        ChatQolSettings.load(old);
        check("legacy profile enables native master", ChatQolSettings.enabled && ChatQolSettings.tabsEnabled);
        check("legacy direct-message selection becomes Reply", ChatQolSettings.selectedTab == ChatFilterRules.REPLY);
        check("legacy message animation becomes native smooth setting", ChatQolSettings.smoothMessages);
        check("legacy migration is flagged for rewrite", ChatQolSettings.isMigrationPending());

        Properties oldUnsupported = new Properties();
        oldUnsupported.setProperty("chat.tabMode", "2");
        ChatQolSettings.load(oldUnsupported);
        check("unsupported historical tab maps safely to All", ChatQolSettings.selectedTab == ChatFilterRules.ALL);

        ChatQolSettings.load(old);
        Properties rewritten = new Properties();
        ChatQolSettings.save(rewritten);
        check("migration writes native master", "true".equals(rewritten.getProperty("chat.qol.enabled")));
        check("migration writes Reply selection", "2".equals(rewritten.getProperty("chat.qol.tabs.selected")));
        check("retired tab key is not written", !rewritten.containsKey("chat.tabs.enabled")
                && !rewritten.containsKey("chat.tabMode"));
        check("retired animation bridge keys are not written", !rewritten.containsKey("chat.animation.enabled")
                && !rewritten.containsKey("chat.animation.messages"));
    }

    private static void nativeClampingAndPersistence() {
        Properties p = new Properties();
        p.setProperty("chat.qol.enabled", "true");
        p.setProperty("chat.qol.tabs.all", "false");
        p.setProperty("chat.qol.tabs.party", "false");
        p.setProperty("chat.qol.tabs.reply", "false");
        p.setProperty("chat.qol.tabs.selected", "99");
        p.setProperty("chat.qol.tabs.order", "reply,reply,all,bad,party");
        p.setProperty("chat.qol.window1.s3.x", "99999");
        p.setProperty("chat.qol.window1.s3.y", "-4");
        p.setProperty("chat.qol.window1.s3.w", "1");
        p.setProperty("chat.qol.window1.s3.h", "99999");
        p.setProperty("chat.qol.window1.limit", "1000");
        p.setProperty("chat.qol.window1.fade", "-5");
        p.setProperty("chat.qol.font.name", "Unknown font file");
        p.setProperty("chat.qol.patterns.count", "1");
        p.setProperty("chat.qol.patterns.0.target", ChatPatternRules.CHAT_1);
        p.setProperty("chat.qol.patterns.0.regex", "^Custom: .*$");
        ChatQolSettings.load(p);
        check("all tabs cannot be hidden at once", ChatQolSettings.showAll || ChatQolSettings.showParty || ChatQolSettings.showReply);
        check("selected tab is clamped", ChatQolSettings.selectedTab >= ChatQolSettings.TAB_ALL
                && ChatQolSettings.selectedTab <= ChatQolSettings.TAB_REPLY);
        check("tab order removes duplicates and unknown ids", "reply,all,party".equals(ChatQolSettings.tabOrder));
        check("per-scale geometry clamps width", ChatQolSettings.chat1().getW(3) >= 1000
                && ChatQolSettings.chat1().getW(3) <= 10000);
        check("per-scale geometry clamps x inside window", ChatQolSettings.chat1().getX(3)
                <= 10000 - ChatQolSettings.chat1().getW(3));
        check("per-scale geometry clamps y inside window", ChatQolSettings.chat1().getY(3)
                <= 10000 - ChatQolSettings.chat1().getH(3));
        check("message limits and fade clamp", ChatQolSettings.chat1().getMessageLimit() == 300
                && ChatQolSettings.chat1().getFadeSeconds() == 0);
        check("custom rule persists and compiles", ChatQolSettings.compiledPatterns().classify("Custom: one")
                == ChatFilterRules.PLAYER);

        ChatQolSettings.setTabVisible(ChatQolSettings.TAB_PARTY, true);
        ChatQolSettings.setTabVisible(ChatQolSettings.TAB_ALL, false);
        check("editor can hide an individual tab", !ChatQolSettings.showAll);
        ChatQolSettings.setSelectedTab(ChatQolSettings.TAB_ALL);
        check("hidden selected tab falls back to visible target", ChatQolSettings.selectedTab != ChatQolSettings.TAB_ALL);
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++;
        else {
            failed++;
            System.err.println("FAIL: " + name);
        }
    }
}
