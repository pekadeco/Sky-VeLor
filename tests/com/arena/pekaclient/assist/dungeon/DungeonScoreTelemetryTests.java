package com.arena.pekaclient.assist.dungeon;

/** Parser regression tests for sidebar/tab/chat-only dungeon score observations. */
public final class DungeonScoreTelemetryTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        visibleRowsFeedTheCalculator();
        chatObservationsCoverFailuresSpiritAndMimic();
        finalVisibleScoreOverridesProjectionAndAllowsSPlus();
        unframedScoreTextCannotManufactureSPlus();
        timerRollbackStartsANewAttempt();
        done();
    }

    private static void visibleRowsFeedTheCalculator() {
        DungeonScoreTelemetry telemetry = new DungeonScoreTelemetry();
        telemetry.observeVisible("The Catacombs (F7)");
        telemetry.observeVisible("Cleared: 100%");
        telemetry.observeVisible("Secrets Found: 100%");
        telemetry.observeVisible("Deaths: 1 (2)");
        telemetry.observeVisible("Crypts: 5");
        telemetry.observeVisible("Time Elapsed: 5:00");
        DungeonScoreReadout readout = telemetry.readout();
        check("floor is observed from visible dungeon text", "F7".equals(readout.floor));
        check("tab deaths use the parenthesized total", readout.deaths == 2);
        check("sidebar/tab percentages are retained", readout.clearedPercent == 100 && readout.secretsPercent == 100);
        check("time and crypts are retained", readout.elapsedSeconds == 300 && readout.crypts == 5);
        check("readout calculates each complete component", readout.calculated.skill == 96
                && readout.calculated.exploration == 100 && readout.calculated.speed == 100
                && readout.calculated.bonus == 5 && readout.calculated.total == 301);
        check("live score line suppresses premature S+", "Score: 301 (S)".equals(readout.scoreLine()));
    }

    private static void chatObservationsCoverFailuresSpiritAndMimic() {
        DungeonScoreTelemetry telemetry = fullF7Telemetry();
        telemetry.observeChat("PUZZLE FAIL!");
        telemetry.observeChat("Your Spirit Pet reduced your death score penalty to 1!");
        telemetry.observeChat("The Mimic has been killed!");
        DungeonScoreReadout readout = telemetry.readout();
        check("puzzle failure is counted from visible chat", readout.failedPuzzles == 1);
        check("Spirit message records the visible one-penalty reduction", readout.spiritDeathPenaltyReductions == 1);
        check("mimic kill adds the native two-point bonus", readout.mimicKilled && readout.calculated.bonus == 7);
        check("skill combines one effective death and failed puzzle", readout.calculated.skill == 84);
    }

    private static void finalVisibleScoreOverridesProjectionAndAllowsSPlus() {
        DungeonScoreTelemetry telemetry = fullF7Telemetry();
        telemetry.observeChat("> EXTRA STATS <");
        telemetry.observeChat("Skill: 96");
        telemetry.observeChat("Exploration: 100");
        telemetry.observeChat("Speed: 100");
        telemetry.observeChat("Bonus: 5");
        telemetry.observeChat("Dungeon Score: 301");
        telemetry.observeChat("Rank: S+");
        DungeonScoreReadout readout = telemetry.readout();
        check("official visible end total is retained", readout.hasOfficialScore() && readout.officialTotal == 301);
        check("only the visible final score may display S+", "S+".equals(readout.displayRank())
                && "Score: 301 (S+)".equals(readout.scoreLine()));
        check("visible recap components are independently captured for mismatch diagnosis", telemetry.officialSkill() == 96
                && telemetry.officialExploration() == 100 && telemetry.officialSpeed() == 100
                && telemetry.officialBonus() == 5);
    }

    private static void unframedScoreTextCannotManufactureSPlus() {
        DungeonScoreTelemetry telemetry = fullF7Telemetry();
        telemetry.observeChat("Dungeon Score: 301");
        telemetry.observeChat("Rank: S+");
        DungeonScoreReadout readout = telemetry.readout();
        check("unframed score-looking chat never becomes an official result", !readout.hasOfficialScore());
        check("unframed score-looking chat retains the conservative live rank", "S".equals(readout.displayRank()));
    }

    private static void timerRollbackStartsANewAttempt() {
        DungeonScoreTelemetry telemetry = fullF7Telemetry();
        telemetry.observeChat("PUZZLE FAIL!");
        telemetry.observeVisible("Time: 0:10");
        DungeonScoreReadout readout = telemetry.readout();
        check("timer rollback retains the current floor", "F7".equals(readout.floor));
        check("timer rollback clears prior run-only puzzle failures", readout.failedPuzzles == 0);
        check("timer rollback records the fresh elapsed time", readout.elapsedSeconds == 10);
    }

    private static DungeonScoreTelemetry fullF7Telemetry() {
        DungeonScoreTelemetry telemetry = new DungeonScoreTelemetry();
        telemetry.observeVisible("The Catacombs (F7)");
        telemetry.observeVisible("Cleared: 100%");
        telemetry.observeVisible("Secrets Found: 100%");
        telemetry.observeVisible("Deaths: 1 (2)");
        telemetry.observeVisible("Crypts: 5");
        telemetry.observeVisible("Time: 5:00");
        return telemetry;
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++;
        else { failed++; System.out.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("dungeon score telemetry: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
