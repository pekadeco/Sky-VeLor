package com.arena.pekaclient.dungeon;

import java.util.Properties;

/** Fresh/default and saved-value contract for specialized Floor VII helpers. */
public final class M7SettingsTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        freshConfigKeepsSpecializedToolsOptIn();
        oldConfigKeepsItsHistoricFallback();
        explicitSavedChoicesWin();
        System.out.println("m7 settings: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void freshConfigKeepsSpecializedToolsOptIn() {
        M7Settings.load(new Properties());
        check("fresh M7 overlay is off", !M7Settings.isOverlayEnabled());
        check("fresh M7 alerts and terminal solver are off", !M7Settings.isDragonKillAlertEnabled()
                && !M7Settings.isTerminalSolverEnabled() && !M7Settings.isStatueZonesEnabled());
    }

    private static void oldConfigKeepsItsHistoricFallback() {
        Properties old = new Properties();
        old.setProperty("config.version", "2");
        M7Settings.load(old);
        check("old file without M7 keys preserves historic overlay default", M7Settings.isOverlayEnabled()
                && M7Settings.isDragonKillAlertEnabled() && M7Settings.isTerminalSolverEnabled());
    }

    private static void explicitSavedChoicesWin() {
        Properties saved = new Properties();
        saved.setProperty("dungeon.m7.overlay", "false");
        saved.setProperty("dungeon.m7.dragonKillAlert", "false");
        saved.setProperty("dungeon.m7.terminalSolver", "true");
        M7Settings.load(saved);
        check("explicit saved values override either fallback", !M7Settings.isOverlayEnabled()
                && !M7Settings.isDragonKillAlertEnabled() && M7Settings.isTerminalSolverEnabled());
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++; else { failed++; System.out.println("FAIL " + name); }
    }
}
