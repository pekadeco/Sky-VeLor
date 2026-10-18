package com.arena.pekaclient.config;

import java.util.Properties;

/** Checks fresh-install performance defaults and explicit saved choices. */
public final class AppleSettingsTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("fresh adaptive quality defaults off", !AppleSettings.adaptive);
        check("fresh Performance Boost Beta defaults on", AppleSettings.experimentalPerformance);
        check("fresh panel opacity is slightly transparent", AppleSettings.DEFAULT_GLASS_OPACITY == 88);

        AppleSettings.adaptive = AppleSettings.DEFAULT_ADAPTIVE_QUALITY;
        AppleSettings.experimentalPerformance = AppleSettings.DEFAULT_PERFORMANCE_BOOST_BETA;
        AppleSettings.loadPerformanceSettings(new Properties());
        check("empty fresh config keeps adaptive disabled", !AppleSettings.adaptive);
        check("empty fresh config keeps Performance Boost Beta enabled",
                AppleSettings.experimentalPerformance);

        Properties saved = new Properties();
        saved.setProperty("adaptive", "true");
        saved.setProperty("experimentalPerformance", "false");
        AppleSettings.loadPerformanceSettings(saved);
        check("saved adaptive choice is preserved", AppleSettings.adaptive);
        check("saved Performance Boost Beta choice is preserved", !AppleSettings.experimentalPerformance);

        AppleSettings.loadPerformanceSettings(new Properties());
        check("missing keys do not overwrite effective adaptive choice", AppleSettings.adaptive);
        check("missing keys do not overwrite effective performance choice",
                !AppleSettings.experimentalPerformance);

        Properties beta0108 = new Properties();
        beta0108.setProperty("itemAnimation.enabled", "false");
        beta0108.setProperty("itemAnimation.size", "145");
        beta0108.setProperty("itemAnimation.speed", "175");
        beta0108.setProperty("itemAnimation.x", "-75");
        beta0108.setProperty("itemAnimation.y", "25");
        beta0108.setProperty("itemAnimation.z", "50");
        beta0108.setProperty("itemAnimation.autoFixHitSlash", "false");
        beta0108.setProperty("itemAnimation.backgroundTransparency", "12");
        beta0108.setProperty("itemAnimation.backgroundOpacity", "95");
        AppleSettings.loadItemAnimationSettings(beta0108);
        check("0.10.8v config with obsolete Auto Fix/background keys loads cleanly",
                !AppleSettings.itemAnimationEnabled && AppleSettings.itemAnimationSize == 145
                        && AppleSettings.itemAnimationSpeed == 175 && AppleSettings.itemAnimationX == -75
                        && AppleSettings.itemAnimationY == 25 && AppleSettings.itemAnimationZ == 50);
        Properties migrated = new Properties();
        migrated.putAll(beta0108);
        AppleSettings.saveItemAnimationSettings(migrated);
        check("obsolete Item Animation keys are not written back",
                !migrated.containsKey("itemAnimation.autoFixHitSlash")
                        && !migrated.containsKey("itemAnimation.backgroundTransparency")
                        && !migrated.containsKey("itemAnimation.backgroundOpacity"));

        fishingSettingsCompatibility();
        chatSettingsCompatibility();
        globalScrollCompatibility();
        mouseSettingsCompatibility();
        particleSettingsCompatibility();

        System.out.println();
        System.out.println("apple settings: passed " + passed + ", failed " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void fishingSettingsCompatibility() {
        AppleSettings.fishingNearbyEncounterAlert = true;
        AppleSettings.loadFishingSettings(new Properties());
        check("old fishing config keeps nearby encounter titles opt-in", !AppleSettings.fishingNearbyEncounterAlert);

        Properties saved = new Properties();
        saved.setProperty("fishing.counter", "false");
        saved.setProperty("fishing.festival", "false");
        saved.setProperty("fishing.jawbusAlert", "true");
        saved.setProperty("fishing.nearbyEncounterAlert", "true");
        AppleSettings.loadFishingSettings(saved);
        check("nearby encounter title choice loads", !AppleSettings.fishingCounter
                && !AppleSettings.fishingFestival && AppleSettings.fishingJawbusAlert
                && AppleSettings.fishingNearbyEncounterAlert);

        Properties rewritten = new Properties();
        AppleSettings.saveFishingSettings(rewritten);
        check("nearby encounter title has one stable config key",
                "true".equals(rewritten.getProperty("fishing.nearbyEncounterAlert"))
                        && rewritten.size() == 4);
    }

    private static void chatSettingsCompatibility() {
        AppleSettings.loadChatSettings(new Properties());
        check("new installs keep Chat QoL master opt-in", !ChatQolSettings.enabled);

        Properties oldFriendChat = new Properties();
        oldFriendChat.setProperty("chat.tabMode", "3");
        oldFriendChat.setProperty("chat.animation.messages", "true");
        AppleSettings.loadChatSettings(oldFriendChat);
        check("legacy tab key is ignored instead of enabling a renderer", !ChatQolSettings.enabled
                && "all".equals(ChatQolSettings.selectedTabId()));
        check("legacy animation profile is ignored safely", ChatQolSettings.smoothMessages);

        AppleSettings.saveChatSettings(oldFriendChat);
        check("chat rewrite keeps only stable one-default keys", "false".equals(oldFriendChat.getProperty("chat.qol.enabled"))
                && "all".equals(oldFriendChat.getProperty("chat.qol.tabs.selectedId"))
                && !oldFriendChat.containsKey("chat.animation.enabled") && !oldFriendChat.containsKey("chat.tabMode"));
    }

    private static void globalScrollCompatibility() {
        Properties retired = new Properties();
        retired.setProperty("chat.qol.target.default.scroll.speed", "225");
        retired.setProperty("chat.qol.target.default.scroll.smooth", "false");
        retired.setProperty("chat.qol.target.chat2.scroll.speed", "175");
        AppleSettings.loadGlobalScrollSettings(retired);
        check("retired split-chat scroll keys are ignored", AppleSettings.scrollSpeed == AppleSettings.DEFAULT_SCROLL_SPEED
                && AppleSettings.smoothScrolling == AppleSettings.DEFAULT_SMOOTH_SCROLLING);

        Properties nativeGlobal = new Properties();
        nativeGlobal.setProperty("scrollSpeed", "150");
        nativeGlobal.setProperty("smoothScrolling", "true");
        nativeGlobal.setProperty("chat.qol.target.default.scroll.speed", "300");
        nativeGlobal.setProperty("chat.qol.target.default.scroll.smooth", "false");
        AppleSettings.loadGlobalScrollSettings(nativeGlobal);
        check("native global scroll values remain authoritative", AppleSettings.scrollSpeed == 150
                && AppleSettings.smoothScrolling);
    }

    private static void mouseSettingsCompatibility() {
        AppleSettings.disableHotbarScrolling = true;
        AppleSettings.loadMouseSettings(new Properties());
        check("old config keeps hotbar wheel scrolling enabled", !AppleSettings.disableHotbarScrolling);

        Properties stored = new Properties();
        stored.setProperty("mouse.disableHotbarScrolling", "true");
        AppleSettings.loadMouseSettings(stored);
        check("hotbar wheel choice loads", AppleSettings.disableHotbarScrolling);
        Properties saved = new Properties();
        AppleSettings.saveMouseSettings(saved);
        check("hotbar wheel choice has one stable key",
                "true".equals(saved.getProperty("mouse.disableHotbarScrolling")) && saved.size() == 1);
    }

    private static void particleSettingsCompatibility() {
        AppleSettings.noBlockParticles = true;
        AppleSettings.loadParticleSettings(new Properties());
        check("old config keeps block particles visible", !AppleSettings.noBlockParticles);

        Properties stored = new Properties();
        stored.setProperty("particles.noBlockParticles", "true");
        AppleSettings.loadParticleSettings(stored);
        check("no-block-particles choice loads", AppleSettings.noBlockParticles);
        Properties saved = new Properties();
        AppleSettings.saveParticleSettings(saved);
        check("no-block-particles has one stable key",
                "true".equals(saved.getProperty("particles.noBlockParticles")) && saved.size() == 1);
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL " + name);
        }
    }
}
