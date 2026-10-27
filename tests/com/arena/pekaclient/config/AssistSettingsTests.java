package com.arena.pekaclient.config;

import java.util.Properties;

/** Safe defaults, retired-key compatibility, integer hurt-camera persistence and bounds coverage. */
public final class AssistSettingsTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        defaultsAreSafeAndVanillaCameraIsFullStrength();
        legacyInputLoadsWithoutRevivingRetiredFeatures();
        hurtCameraShakePersistsAtZeroIntermediateAndVanilla();
        roundTripAndBounds();
        done();
    }

    private static void defaultsAreSafeAndVanillaCameraIsFullStrength() {
        AssistSettings.load(new Properties());
        check("intrusive controls and opt-in locator default off", !AssistSettings.autoSprint && !AssistSettings.dungeonSync
                && !AssistSettings.terminalAutoClick && !AssistSettings.miningAbilityAlert && !AssistSettings.spiritBowLocator);
        check("fresh local inventory presentation is useful by default", AssistSettings.romanNumerals
                && AssistSettings.tooltipScroll && AssistSettings.rarityHighlight && !AssistSettings.hidePotionEffects);
        check("dead dungeon ESP targets are hidden by default", AssistSettings.hideDeadMobs);
        check("Aspect teleport visual transition defaults on", AssistSettings.smoothAspectTeleport);
        check("hurt camera defaults to vanilla 100 percent", AssistSettings.hurtCameraShake == 100
                && Math.abs(AssistSettings.hurtCameraShakeMultiplier() - 1f) < .00001f);
        check("endpoint default is usable", AssistSettings.validSecureEndpoint(AssistSettings.dungeonSyncEndpoint));
    }

    private static void legacyInputLoadsWithoutRevivingRetiredFeatures() {
        Properties old = new Properties();
        old.setProperty("miningAbilityNotifier", "true");
        old.setProperty("inventorySearchEnabled", "true");
        old.setProperty("assist.inventory.search", "true");
        old.setProperty("assist.visual.noHurtCamera", "true");
        old.setProperty("autoSprintEnabled", "true");
        old.setProperty("itemProtectionEnabled", "true");
        old.setProperty("assist.itemGuard.enabled", "true");
        old.setProperty("terminalGuiScale", "1.5");
        old.setProperty("dungeonEsp.hideZeroHealth", "false");
        AssistSettings.load(old);
        check("retained aliases load", AssistSettings.miningAbilityAlert && AssistSettings.autoSprint
                && AssistSettings.terminalScale == 150);
        check("old no-hurt toggle maps to zero percent", AssistSettings.hurtCameraShake == 0);
        check("old zero-health ESP choice migrates to Comfort", !AssistSettings.hideDeadMobs);
        check("established config does not acquire newly defaulted inventory visuals", !AssistSettings.romanNumerals
                && !AssistSettings.tooltipScroll && !AssistSettings.rarityHighlight);
        check("established config receives the safe render-only Aspect transition default", AssistSettings.smoothAspectTeleport);
        check("established config keeps the new Spirit Bow locator opt-in", !AssistSettings.spiritBowLocator);
        Properties rewritten = new Properties();
        rewritten.putAll(old);
        AssistSettings.save(rewritten);
        check("retired Search, binary camera, and ESP-local dead keys are dropped on save", !rewritten.containsKey("inventorySearchEnabled")
                && !rewritten.containsKey("assist.inventory.search")
                && !rewritten.containsKey("assist.visual.noHurtCamera") && !rewritten.containsKey("noHurtCamEnabled")
                && !rewritten.containsKey("dungeonEsp.hideZeroHealth")
                && "false".equals(rewritten.getProperty("assist.comfort.hideDeadMobs")));
        check("removed Item Protection keys are harmlessly ignored", old.containsKey("itemProtectionEnabled")
                && old.containsKey("assist.itemGuard.enabled"));
        Properties newOwnerWins = new Properties();
        newOwnerWins.setProperty("assist.comfort.hideDeadMobs", "true");
        newOwnerWins.setProperty("dungeonEsp.hideZeroHealth", "false");
        AssistSettings.load(newOwnerWins);
        check("new Comfort key wins over the retired ESP key", AssistSettings.hideDeadMobs);
    }

    private static void hurtCameraShakePersistsAtZeroIntermediateAndVanilla() {
        Properties p = new Properties();
        p.setProperty("assist.visual.hurtCameraShake", "0");
        AssistSettings.load(p);
        check("zero suppresses tilt amplitude only", AssistSettings.hurtCameraShake == 0
                && AssistSettings.hurtCameraShakeMultiplier() == 0f);
        p.setProperty("assist.visual.hurtCameraShake", "37");
        AssistSettings.load(p);
        check("intermediate intensity scales proportionally", AssistSettings.hurtCameraShake == 37
                && Math.abs(AssistSettings.hurtCameraShakeMultiplier() - .37f) < .00001f);
        p.setProperty("assist.visual.hurtCameraShake", "100");
        AssistSettings.load(p);
        check("one hundred is vanilla multiplier", AssistSettings.hurtCameraShake == 100
                && AssistSettings.hurtCameraShakeMultiplier() == 1f);
        p.setProperty("assist.visual.hurtCameraShake", "-2");
        AssistSettings.load(p);
        check("negative intensity clamps to zero", AssistSettings.hurtCameraShake == 0);
        p.setProperty("assist.visual.hurtCameraShake", "101");
        AssistSettings.load(p);
        check("over-max intensity clamps to one hundred", AssistSettings.hurtCameraShake == 100);
        p.setProperty("assist.visual.hurtCameraShake", "50.5");
        AssistSettings.load(p);
        check("non-whole intensity falls back to vanilla default", AssistSettings.hurtCameraShake == 100);

        int[] persistedValues = {0, 37, 100};
        for (int value : persistedValues) {
            AssistSettings.hurtCameraShake = value;
            Properties saved = new Properties();
            AssistSettings.save(saved);
            boolean stored = Integer.toString(value).equals(saved.getProperty("assist.visual.hurtCameraShake"));
            AssistSettings.load(saved);
            check("camera shake " + value + " persists and round trips", stored && AssistSettings.hurtCameraShake == value);
        }

        Properties newWins = new Properties();
        newWins.setProperty("assist.visual.hurtCameraShake", "55");
        newWins.setProperty("noHurtCamEnabled", "true");
        AssistSettings.load(newWins);
        check("new percentage wins over legacy binary key", AssistSettings.hurtCameraShake == 55);

        Properties legacyOff = new Properties();
        legacyOff.setProperty("noHurtCamEnabled", "false");
        AssistSettings.load(legacyOff);
        check("old false toggle maps to vanilla", AssistSettings.hurtCameraShake == 100);
    }

    private static void roundTripAndBounds() {
        AssistSettings.resetDefaults();
        AssistSettings.tooltipScroll = true;
        AssistSettings.hurtCameraShake = 44;
        AssistSettings.smoothAspectTeleport = false;
        AssistSettings.autoSprint = true;
        AssistSettings.hideDeadMobs = false;
        AssistSettings.dungeonSync = true;
        AssistSettings.dungeonSyncEndpoint = "wss://sync.example.org/ws";
        AssistSettings.spiritBowLocator = true;
        AssistSettings.terminalHints = true;
        AssistSettings.terminalAutoClick = true;
        AssistSettings.terminalScale = 222;
        Properties saved = new Properties();
        AssistSettings.save(saved);
        check("retained keys are namespaced", saved.containsKey("assist.inventory.tooltipScroll")
                && saved.containsKey("assist.visual.hurtCameraShake")
                && saved.containsKey("assist.visual.smoothAspectTeleport") && saved.containsKey("assist.movement.autoSprint")
                && saved.containsKey("assist.comfort.hideDeadMobs") && saved.containsKey("assist.dungeon.sync.enabled")
                && saved.containsKey("assist.dungeon.spiritBowLocator") && saved.containsKey("assist.dungeon.terminal.autoClick")
                && !saved.containsKey("assist.itemGuard.enabled") && !saved.containsKey("assist.itemGuard.notifications"));
        AssistSettings.load(saved);
        check("round trip retains settings", AssistSettings.tooltipScroll && AssistSettings.hurtCameraShake == 44
                && !AssistSettings.smoothAspectTeleport && AssistSettings.autoSprint && !AssistSettings.hideDeadMobs && AssistSettings.dungeonSync
                && "wss://sync.example.org/ws".equals(AssistSettings.dungeonSyncEndpoint) && AssistSettings.spiritBowLocator
                && AssistSettings.terminalAutoClickActive() && AssistSettings.terminalScale == 222);
        saved.setProperty("assist.visual.smoothAspectTeleport", "true");
        AssistSettings.load(saved);
        check("explicit Aspect transition preference loads", AssistSettings.smoothAspectTeleport);
        saved.setProperty("assist.dungeon.sync.endpoint", "http://not-secure.invalid");
        saved.setProperty("assist.dungeon.terminal.scale", "9999");
        AssistSettings.load(saved);
        check("invalid endpoint and scale clamp", AssistSettings.DEFAULT_SYNC_ENDPOINT.equals(AssistSettings.dungeonSyncEndpoint)
                && AssistSettings.terminalScale == AssistSettings.MAX_TERMINAL_SCALE);
        check("sync endpoint requires the fixed secure worker route", !AssistSettings.validSecureEndpoint("wss://sync.example.org/other")
                && !AssistSettings.validSecureEndpoint("wss://sync.example.org/ws?x=1"));
    }

    private static void check(String name, boolean ok) {
        if (ok) { passed++; System.out.println("ok   " + name); }
        else { failed++; System.err.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("assist settings: passed " + passed + ", failed " + failed);
        if (failed != 0) throw new AssertionError(failed + " assist-settings checks failed");
    }
}
