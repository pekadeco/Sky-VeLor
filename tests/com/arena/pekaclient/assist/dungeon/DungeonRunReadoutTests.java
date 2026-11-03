package com.arena.pekaclient.assist.dungeon;

/** Pure sidebar parsing contract for the read-only Dungeon Run HUD. */
public final class DungeonRunReadoutTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        parsesFormattedVisibleSidebarValues();
        keepsFractionsBoundedAndDoesNotInventData();
        resetDropsRunOnlyState();
        done();
    }

    private static void parsesFormattedVisibleSidebarValues() {
        DungeonRunReadout readout = new DungeonRunReadout();
        check("format codes are stripped before parsing", readout.observe("\u00a7aThe Catacombs (F7)"));
        readout.observe("\u00a7eTime Elapsed: 04:07");
        readout.observe("\u00a7bCleared: 63%");
        readout.observe("\u00a7dSecrets Found: 12/20");
        readout.observe("\u00a7cDeaths: 2");
        check("floor uses an explicitly visible F token", "Floor: F7".equals(readout.floorLine()));
        check("time keeps a zero-padded visible sidebar value", "Time: 4:07".equals(readout.timeLine()));
        check("clear percentage is retained", "Cleared: 63%".equals(readout.clearedLine()));
        check("secret fraction is retained", "Secrets: 12/20".equals(readout.secretsLine()));
        check("death count is retained", "Deaths: 2".equals(readout.deathsLine()));
        check("readout reports observed run data", readout.hasData());
    }

    private static void keepsFractionsBoundedAndDoesNotInventData() {
        DungeonRunReadout readout = new DungeonRunReadout();
        check("invalid fraction is ignored", !readout.observe("Secrets: 30/20"));
        check("out-of-range clear is ignored", !readout.observe("Cleared: 180%"));
        check("out-of-range time seconds is ignored", !readout.observe("Time: 03:99"));
        check("unknown values remain placeholders", "Secrets: --".equals(readout.secretsLine())
                && "Cleared: --".equals(readout.clearedLine()) && "Time: --".equals(readout.timeLine()));
        check("Master Mode wording maps to a visible M token", readout.observe("Master Mode 6")
                && "Floor: M6".equals(readout.floorLine()));
        check("percent-only secrets remain explicit rather than inferred", readout.observe("Secrets Found: 81%")
                && "Secrets: 81%".equals(readout.secretsLine()));
    }

    private static void resetDropsRunOnlyState() {
        DungeonRunReadout readout = new DungeonRunReadout();
        readout.observe("Floor 4");
        readout.observe("Secrets: 3/9");
        readout.reset();
        check("reset clears every observed line", !readout.hasData() && "Floor: --".equals(readout.floorLine())
                && "Secrets: --".equals(readout.secretsLine()) && "Deaths: --".equals(readout.deathsLine()));
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++; else { failed++; System.out.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("dungeon run readout: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
