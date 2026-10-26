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
        check("fresh curve quality opens Smooth with a rounded fast floor", AppleSettings.curveQuality
                == AppleSettings.DEFAULT_CURVE_QUALITY && AppleSettings.CURVE_SEGMENTS[0] >= 16
                && AppleSettings.CURVE_SEGMENTS[2] >= 8);

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
        beta0108.setProperty("itemAnimation.smoothSwing", "false");
        beta0108.setProperty("itemAnimation.smoothness", "82");
        beta0108.setProperty("itemAnimation.swingBlur", "true");
        beta0108.setProperty("itemAnimation.swingBlurIntensity", "75");
        beta0108.setProperty("itemAnimation.backgroundTransparency", "12");
        beta0108.setProperty("itemAnimation.backgroundOpacity", "95");
        AppleSettings.loadItemAnimationSettings(beta0108);
        check("0.10.8v config with obsolete Auto Fix/background keys loads cleanly",
                !AppleSettings.itemAnimationEnabled && AppleSettings.itemAnimationSize == 145
                        && AppleSettings.itemAnimationSpeed == 175 && AppleSettings.itemAnimationX == -75
                        && AppleSettings.itemAnimationY == 25 && AppleSettings.itemAnimationZ == 50
                        && !AppleSettings.itemAnimationSmoothSwing && AppleSettings.itemAnimationSmoothness == 82);
        Properties migrated = new Properties();
        migrated.putAll(beta0108);
        AppleSettings.saveItemAnimationSettings(migrated);
        check("obsolete Item Animation keys are not written back",
                !migrated.containsKey("itemAnimation.autoFixHitSlash")
                        && !migrated.containsKey("itemAnimation.backgroundTransparency")
                        && !migrated.containsKey("itemAnimation.backgroundOpacity")
                        && !migrated.containsKey("itemAnimation.swingBlur")
                        && !migrated.containsKey("itemAnimation.swingBlurIntensity"));

        fishingSettingsCompatibility();
        chatSettingsCompatibility();
        globalScrollCompatibility();
        mouseSettingsCompatibility();
        particleSettingsCompatibility();
        settingsThemePersistence();
        settingsWindowLayoutPersistence();
        guiTransparencyLegacyCompatibility();
        configurationVersionMigration();

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
        check("new installs compact consecutive chat messages", ChatQolSettings.compactMessages);

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
        AppleSettings.noBlockParticles = false;
        AppleSettings.loadParticleSettings(new Properties());
        check("fresh config hides block particles", AppleSettings.noBlockParticles);

        Properties stored = new Properties();
        stored.setProperty("particles.noBlockParticles", "true");
        AppleSettings.loadParticleSettings(stored);
        check("no-block-particles choice loads", AppleSettings.noBlockParticles);
        Properties saved = new Properties();
        AppleSettings.saveParticleSettings(saved);
        check("no-block-particles has one stable key",
                "true".equals(saved.getProperty("particles.noBlockParticles")) && saved.size() == 1);
        Properties establishedWithoutParticleChoice = new Properties();
        establishedWithoutParticleChoice.setProperty("config.version", "2");
        AppleSettings.loadParticleSettings(establishedWithoutParticleChoice);
        check("missing visual key in established config keeps vanilla particles", !AppleSettings.noBlockParticles);
    }

    /** Additive palette choice must survive save/load while old configs retain the existing forest/cyan shell. */
    private static void settingsThemePersistence() {
        AppleSettings.settingsTheme = AppleSettings.SETTINGS_THEME_BLACK_BLUE;
        AppleSettings.loadSettingsTheme(new Properties());
        check("missing legacy theme key keeps the established Forest and Cyan shell",
                AppleSettings.settingsTheme == AppleSettings.DEFAULT_SETTINGS_THEME && !AppleSettings.isBlackBlueTheme());

        Properties stored = new Properties();
        stored.setProperty("appearance.settingsTheme", "1");
        AppleSettings.loadSettingsTheme(stored);
        check("Black and Blue selection loads", AppleSettings.settingsTheme == AppleSettings.SETTINGS_THEME_BLACK_BLUE
                && AppleSettings.isBlackBlueTheme());

        Properties written = new Properties();
        AppleSettings.saveSettingsTheme(written);
        check("Black and Blue selection has one stable persisted key",
                "1".equals(written.getProperty("appearance.settingsTheme")) && written.size() == 1);

        Properties invalid = new Properties();
        invalid.setProperty("appearance.settingsTheme", "99");
        AppleSettings.loadSettingsTheme(invalid);
        check("theme selection is bounded", AppleSettings.settingsTheme == AppleSettings.SETTINGS_THEME_BLACK_BLUE);
        AppleSettings.settingsTheme = AppleSettings.DEFAULT_SETTINGS_THEME;
    }

    /** The compact/fullscreen shell choice is additive and leaves old properties safe by default. */
    private static void settingsWindowLayoutPersistence() {
        AppleSettings.settingsWindowFullscreen = true;
        AppleSettings.loadSettingsWindowLayout(new Properties());
        check("missing shell layout key opens the compact responsive window", !AppleSettings.settingsWindowFullscreen);
        Properties stored = new Properties();
        stored.setProperty("settings.window.fullscreen", "true");
        AppleSettings.loadSettingsWindowLayout(stored);
        check("old fullscreen choice migrates once to compact", !AppleSettings.settingsWindowFullscreen);
        Properties saved = new Properties();
        AppleSettings.saveSettingsWindowLayout(saved);
        check("compact migration persists a versioned shell choice",
                "false".equals(saved.getProperty("settings.window.fullscreen"))
                        && Integer.toString(AppleSettings.CURRENT_SETTINGS_WINDOW_LAYOUT_VERSION).equals(
                                saved.getProperty(AppleSettings.SETTINGS_WINDOW_LAYOUT_VERSION_KEY)) && saved.size() == 2);
        Properties explicitFullscreen = new Properties();
        explicitFullscreen.setProperty(AppleSettings.SETTINGS_WINDOW_LAYOUT_VERSION_KEY,
                Integer.toString(AppleSettings.CURRENT_SETTINGS_WINDOW_LAYOUT_VERSION));
        explicitFullscreen.setProperty("settings.window.fullscreen", "true");
        AppleSettings.loadSettingsWindowLayout(explicitFullscreen);
        check("post-migration explicit fullscreen choice still loads", AppleSettings.settingsWindowFullscreen);
        AppleSettings.settingsWindowFullscreen = AppleSettings.DEFAULT_SETTINGS_WINDOW_FULLSCREEN;
    }

    /** The refreshed UI must retain the existing optional glass keys and honor old saved values. */
    private static void guiTransparencyLegacyCompatibility() {
        Properties legacy = new Properties();
        legacy.setProperty("glassOpacityOn", "false");
        legacy.setProperty("glassOpacity", "60");
        AppleSettings.glassOpacityOn = true;
        AppleSettings.glassOpacity = AppleSettings.DEFAULT_GLASS_OPACITY;
        AppleSettings.loadAppearanceSettings(legacy);
        check("legacy GUI opacity toggle and value load through their original keys",
                !AppleSettings.glassOpacityOn && AppleSettings.glassOpacity == 60);

        Properties refreshed = new Properties();
        refreshed.setProperty("glassOpacityOn", "true");
        refreshed.setProperty("glassOpacity", "45");
        AppleSettings.loadAppearanceSettings(refreshed);
        check("refreshed custom transparency accepts the useful lower glass bound",
                AppleSettings.glassOpacityOn && AppleSettings.glassOpacity == AppleSettings.MIN_GLASS_OPACITY);

        Properties written = new Properties();
        AppleSettings.saveAppearanceSettings(written);
        check("custom GUI transparency saves its established key pair",
                "true".equals(written.getProperty("glassOpacityOn"))
                        && "45".equals(written.getProperty("glassOpacity")));
        AppleSettings.glassOpacityOn = true;
        AppleSettings.glassOpacity = AppleSettings.DEFAULT_GLASS_OPACITY;
    }

    /** Exercises the same order used by AppleSettings.load without requiring a Minecraft data directory. */
    private static void configurationVersionMigration() {
        Properties oldSavedConfig = new Properties();
        oldSavedConfig.setProperty("chat.qol.messages.compact", "false");
        oldSavedConfig.setProperty("particles.noBlockParticles", "false");
        AppleSettings.loadParticleSettings(oldSavedConfig);
        AppleSettings.loadChatSettings(oldSavedConfig);
        check("old explicit values load before schema marker", !AppleSettings.noBlockParticles
                && !ChatQolSettings.compactMessages);
        check("older schema is marked without promoting saved choices",
                AppleSettings.applyConfigurationVersionUpgrade(oldSavedConfig)
                        && !AppleSettings.noBlockParticles && !ChatQolSettings.compactMessages);

        Properties migrated = new Properties();
        AppleSettings.saveConfigurationVersion(migrated);
        AppleSettings.saveParticleSettings(migrated);
        AppleSettings.saveChatSettings(migrated);
        check("migration save records schema and retains explicit values",
                Integer.toString(AppleSettings.CURRENT_CONFIG_VERSION).equals(
                        migrated.getProperty(AppleSettings.CONFIG_VERSION_KEY))
                        && "false".equals(migrated.getProperty("particles.noBlockParticles"))
                        && "false".equals(migrated.getProperty("chat.qol.messages.compact")));

        AppleSettings.loadParticleSettings(migrated);
        AppleSettings.loadChatSettings(migrated);
        check("current schema does not reapply the migration", !AppleSettings.applyConfigurationVersionUpgrade(migrated));
        check("saved OFF choices survive a restart", !AppleSettings.noBlockParticles
                && !ChatQolSettings.compactMessages);
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
