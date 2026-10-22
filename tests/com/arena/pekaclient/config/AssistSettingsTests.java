package com.arena.pekaclient.config;

import java.util.Properties;

/** Safe-default, old-key compatibility, persistence and bounds coverage for the new isolated assist schema. */
public final class AssistSettingsTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        defaultsAreSafe();
        legacyInputLoadsWithoutOverwritingOldFeatures();
        roundTripAndBounds();
        done();
    }

    private static void defaultsAreSafe() {
        AssistSettings.load(new Properties());
        check("all intrusive controls default off", !AssistSettings.autoSprint && !AssistSettings.dungeonSync
                && !AssistSettings.terminalAutoClick && !AssistSettings.itemGuard && !AssistSettings.inventorySearch
                && !AssistSettings.noHurtCamera && !AssistSettings.miningAbilityAlert);
        check("notification and endpoint defaults are usable", AssistSettings.itemGuardNotifications
                && AssistSettings.validSecureEndpoint(AssistSettings.dungeonSyncEndpoint));
    }

    private static void legacyInputLoadsWithoutOverwritingOldFeatures() {
        Properties old = new Properties();
        old.setProperty("miningAbilityNotifier", "true");
        old.setProperty("inventorySearchEnabled", "true");
        old.setProperty("itemProtectionEnabled", "true");
        old.setProperty("terminalGuiScale", "1.5");
        AssistSettings.load(old);
        check("old friend aliases load", AssistSettings.miningAbilityAlert && AssistSettings.inventorySearch
                && AssistSettings.itemGuard && AssistSettings.terminalScale == 150);
        check("old shared keys remain untouched", old.containsKey("itemProtectionEnabled")
                && old.containsKey("inventorySearchEnabled"));
    }

    private static void roundTripAndBounds() {
        AssistSettings.resetDefaults();
        AssistSettings.tooltipScroll = true;
        AssistSettings.dungeonSync = true;
        AssistSettings.dungeonSyncEndpoint = "wss://sync.example.org/ws";
        AssistSettings.terminalHints = true;
        AssistSettings.terminalAutoClick = true;
        AssistSettings.terminalScale = 222;
        Properties saved = new Properties();
        AssistSettings.save(saved);
        check("new keys are namespaced", saved.containsKey("assist.inventory.tooltipScroll")
                && saved.containsKey("assist.dungeon.sync.enabled") && saved.containsKey("assist.dungeon.terminal.autoClick"));
        AssistSettings.load(saved);
        check("round trip retains settings", AssistSettings.tooltipScroll && AssistSettings.dungeonSync
                && "wss://sync.example.org/ws".equals(AssistSettings.dungeonSyncEndpoint)
                && AssistSettings.terminalAutoClickActive() && AssistSettings.terminalScale == 222);
        saved.setProperty("assist.dungeon.sync.endpoint", "http://not-secure.invalid");
        saved.setProperty("assist.dungeon.terminal.scale", "9999");
        AssistSettings.load(saved);
        check("invalid endpoint and scale clamp", AssistSettings.DEFAULT_SYNC_ENDPOINT.equals(AssistSettings.dungeonSyncEndpoint)
                && AssistSettings.terminalScale == AssistSettings.MAX_TERMINAL_SCALE);
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
