package com.arena.pekaclient.config;

import com.arena.pekaclient.chat.ChatMenuPolicy;
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
        targetSettingsLinkCopyAndFontMigration();
        separateAndMenuPersistence();
        editorPanelPersistence();

        System.out.println("chat QoL settings: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void freshDefaults() {
        ChatQolSettings.load(new Properties());
        check("fresh master is safely off", !ChatQolSettings.enabled);
        check("fresh tabs have three supported views", ChatQolSettings.tabsEnabled && ChatQolSettings.showAll
                && ChatQolSettings.showParty && ChatQolSettings.showReply);
        check("fresh separate windows are off", !ChatQolSettings.separateChats);
        check("fresh optional hover/font are off", !ChatQolSettings.hoverHighlight
                && !ChatQolSettings.customFont);
        check("fresh context menu defaults to Both with a 100 ms animation", ChatQolSettings.menuOpenMode == ChatMenuPolicy.BOTH
                && ChatQolSettings.menuAnimationMillis == 100);
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

    private static void targetSettingsLinkCopyAndFontMigration() {
        Properties legacy = new Properties();
        legacy.setProperty("chat.qol.font.enabled", "true");
        legacy.setProperty("chat.qol.font.name", "Compact");
        legacy.setProperty("chat.qol.background.opacity", "61");
        legacy.setProperty("chat.qol.background.radius", "7");
        ChatQolSettings.load(legacy);
        check("chat-only Compact migrates to global Smooth", ChatQolSettings.globalFontEnabled
                && ChatQolSettings.FONT_SMOOTH.equals(ChatQolSettings.globalFontName));
        check("legacy shared background migrates to both native targets", ChatQolSettings.appearance(ChatQolSettings.TARGET_CHAT_1).backgroundOpacity() == 61
                && ChatQolSettings.appearance(ChatQolSettings.TARGET_CHAT_2).radius() == 7);
        check("default target stays vanilla-shaped after migration", ChatQolSettings.appearance(ChatQolSettings.TARGET_DEFAULT).backgroundOpacity() == 0
                && ChatQolSettings.appearance(ChatQolSettings.TARGET_DEFAULT).radius() == 0);

        ChatQolSettings.linkAll = false;
        ChatQolSettings.editAppearance(ChatQolSettings.TARGET_CHAT_1, appearance -> appearance.setTextScale(135));
        check("target edits stay independent", ChatQolSettings.appearance(ChatQolSettings.TARGET_CHAT_1).textScale() == 135
                && ChatQolSettings.appearance(ChatQolSettings.TARGET_CHAT_2).textScale() != 135);
        ChatQolSettings.copyAppearanceFrom(ChatQolSettings.TARGET_CHAT_2, ChatQolSettings.TARGET_CHAT_1);
        check("copy settings from clones target policy", ChatQolSettings.appearance(ChatQolSettings.TARGET_CHAT_2).textScale() == 135);
        ChatQolSettings.linkAll = true;
        ChatQolSettings.editAppearance(ChatQolSettings.TARGET_DEFAULT, appearance -> appearance.setHoverOpacity(71));
        check("Link all fans an edit to all targets", ChatQolSettings.appearance(ChatQolSettings.TARGET_DEFAULT).hoverOpacity() == 71
                && ChatQolSettings.appearance(ChatQolSettings.TARGET_CHAT_1).hoverOpacity() == 71
                && ChatQolSettings.appearance(ChatQolSettings.TARGET_CHAT_2).hoverOpacity() == 71);

        Properties saved = new Properties();
        ChatQolSettings.save(saved);
        ChatQolSettings.load(saved);
        check("per-target appearance survives save/load", ChatQolSettings.appearance(ChatQolSettings.TARGET_CHAT_2).textScale() == 135
                && ChatQolSettings.appearance(ChatQolSettings.TARGET_DEFAULT).hoverOpacity() == 71);
        Properties badFont = new Properties();
        badFont.setProperty("chat.qol.globalFont.name", "External:../../unsafe.ttf");
        badFont.setProperty("chat.qol.globalFont.enabled", "true");
        ChatQolSettings.load(badFont);
        check("unknown global font fails closed", ChatQolSettings.FONT_MINECRAFT_DEFAULT.equals(ChatQolSettings.globalFontName)
                && ChatQolSettings.globalFontEnabled);
    }

    private static void separateAndMenuPersistence() {
        ChatQolSettings.resetDefaults();
        ChatQolSettings.setSeparateChats(true);
        ChatQolSettings.setMenuOpenMode(ChatMenuPolicy.LEFT);
        ChatQolSettings.setMenuAnimationMillis(0);
        Properties saved = new Properties();
        ChatQolSettings.save(saved);
        ChatQolSettings.load(saved);
        check("separate chats uses one persisted native key", "true".equals(saved.getProperty("chat.qol.separate"))
                && ChatQolSettings.isSeparateChats());
        check("new menu trigger and Off animation survive restart", ChatQolSettings.menuOpenMode == ChatMenuPolicy.LEFT
                && ChatQolSettings.menuAnimationMillis == 0);

        Properties legacyOff = new Properties();
        legacyOff.setProperty("chat.qol.menu", "false");
        ChatQolSettings.load(legacyOff);
        check("old disabled menu profile remains disabled after migration", ChatQolSettings.menuOpenMode == ChatMenuPolicy.DISABLED);
        Properties legacyRight = new Properties();
        legacyRight.setProperty("chat.qol.menu", "true");
        ChatQolSettings.load(legacyRight);
        check("old right-click profile migrates to Right", ChatQolSettings.menuOpenMode == ChatMenuPolicy.RIGHT);
    }

    private static void editorPanelPersistence() {
        ChatQolSettings.resetDefaults();
        ChatQolSettings.editorPanel(0).set(2, 4200, 3300, true);
        Properties saved = new Properties();
        ChatQolSettings.save(saved);
        ChatQolSettings.load(saved);
        check("editor panel position persists by scale", ChatQolSettings.editorPanel(0).x(2) == 4200
                && ChatQolSettings.editorPanel(0).y(2) == 3300);
        check("editor panel collapse persists by scale", ChatQolSettings.editorPanel(0).collapsed(2));

        // Each named settings section owns a separate scale-keyed title-card state, rather than sharing the main card.
        for (int section = 1; section <= 6; section++) {
            ChatQolSettings.editorPanel(section).set(3, 1000 + section * 500, 2000 + section * 300,
                    (section & 1) == 0);
        }
        saved.clear();
        ChatQolSettings.save(saved);
        ChatQolSettings.load(saved);
        boolean sectionStatesIndependent = true;
        for (int section = 1; section <= 6; section++) {
            sectionStatesIndependent &= ChatQolSettings.editorPanel(section).x(3) == 1000 + section * 500
                    && ChatQolSettings.editorPanel(section).y(3) == 2000 + section * 300
                    && ChatQolSettings.editorPanel(section).collapsed(3) == ((section & 1) == 0);
        }
        check("every editor section persists an independent GUI-scale panel state", sectionStatesIndependent);
        ChatQolSettings.editorPanel(4).resetScale(3);
        check("section title reset restores its own default position", ChatQolSettings.editorPanel(4).x(3) == 5000
                && ChatQolSettings.editorPanel(4).y(3) == 5000);
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++;
        else {
            failed++;
            System.err.println("FAIL: " + name);
        }
    }
}
