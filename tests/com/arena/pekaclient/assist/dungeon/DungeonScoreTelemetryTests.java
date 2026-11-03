package com.arena.pekaclient.assist.dungeon;

/** Parser regression tests for source-aware sidebar/tab/chat dungeon score observations. */
public final class DungeonScoreTelemetryTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        fakepixelVisibleRowsFeedTheCalculator();
        chatObservationsCoverFailuresSpiritBloodAndDiagnostics();
        finalVisibleScoreOverridesProjectionAndAllowsSPlus();
        unframedScoreTextCannotManufactureSPlus();
        timerRollbackStartsANewAttempt();
        sourceBoundariesRejectStatLookingChat();
        cryptFractionDoesNotInventCompletedCrypts();
        done();
    }

    private static void fakepixelVisibleRowsFeedTheCalculator() {
        DungeonScoreTelemetry telemetry = new DungeonScoreTelemetry();
        telemetry.observeSidebar("The Catacombs (F7)");
        telemetry.observeSidebar("Dungeon Cleared: 100%");
        telemetry.observeTab("Dungeon: The Catacombs (F7)");
        telemetry.observeTab("Secrets Found: 100%");
        telemetry.observeTab("Deaths: 1 (2)");
        telemetry.observeTab("Crypts: 5");
        telemetry.observeTab("Time: 5:00");
        DungeonScoreReadout readout = telemetry.readout();
        check("floor is observed from the Fakepixel sidebar/tab labels", "F7".equals(readout.floor));
        check("tab deaths use the parenthesized total", readout.deaths == 2);
        check("sidebar and tab percentages are retained", readout.clearedPercent == 100 && readout.secretsPercent == 100);
        check("tab time and crypts are retained", readout.elapsedSeconds == 300 && readout.crypts == 5);
        check("readout calculates each complete component", readout.calculated.skill == 96
                && readout.calculated.exploration == 100 && readout.calculated.speed == 100
                && readout.calculated.bonus == 5 && readout.calculated.total == 301);
        check("live score line suppresses premature S+", "Score: 301 (S)".equals(readout.scoreLine()));
    }

    private static void chatObservationsCoverFailuresSpiritBloodAndDiagnostics() {
        DungeonScoreTelemetry telemetry = fullF7Telemetry();
        telemetry.observeSidebar("Dungeon Cleared: 91%");
        telemetry.observeChat("PUZZLE FAIL!");
        telemetry.observeChat("Your Spirit Pet reduced your death score penalty to 1!");
        telemetry.observeChat("The BLOOD DOOR has been opened!");
        telemetry.observeChat("The Mimic has been killed!");
        DungeonScoreReadout readout = telemetry.readout();
        check("puzzle failure is counted from visible chat", readout.failedPuzzles == 1);
        check("Spirit message records the visible one-penalty reduction", readout.spiritDeathPenaltyReductions == 1);
        check("Blood Door chat adjusts only the score clear input", readout.bloodDoorOpened
                && readout.clearedPercent == 91 && readout.calculated.adjustedClearedPercent == 100);
        check("Mimic remains a diagnostic rather than an invented two-point bonus", readout.mimicKilled
                && readout.calculated.bonus == 5);
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
        check("visible recap components are independently captured for mismatch diagnosis", readout.officialSkill == 96
                && readout.officialExploration == 100 && readout.officialSpeed == 100 && readout.officialBonus == 5);
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
        telemetry.observeTab("Time: 0:10");
        DungeonScoreReadout readout = telemetry.readout();
        check("timer rollback retains the current floor", "F7".equals(readout.floor));
        check("timer rollback clears prior run-only puzzle failures", readout.failedPuzzles == 0);
        check("timer rollback records the fresh elapsed time", readout.elapsedSeconds == 10);
    }

    private static void sourceBoundariesRejectStatLookingChat() {
        DungeonScoreTelemetry telemetry = fullF7Telemetry();
        telemetry.observeChat("Deaths: 99");
        telemetry.observeChat("Time: 99:59");
        DungeonScoreReadout readout = telemetry.readout();
        check("chat that looks like a tab stat cannot replace deaths", readout.deaths == 2);
        check("chat that looks like a tab stat cannot replace time", readout.elapsedSeconds == 300);
    }

    private static void cryptFractionDoesNotInventCompletedCrypts() {
        DungeonScoreTelemetry telemetry = new DungeonScoreTelemetry();
        telemetry.observeTab("Crypts: 3/5");
        check("fraction-style Crypts tab row is explicitly zero completed crypts", telemetry.readout().crypts == 0);
    }

    private static DungeonScoreTelemetry fullF7Telemetry() {
        DungeonScoreTelemetry telemetry = new DungeonScoreTelemetry();
        telemetry.observeSidebar("The Catacombs (F7)");
        telemetry.observeSidebar("Dungeon Cleared: 100%");
        telemetry.observeTab("Dungeon: The Catacombs (F7)");
        telemetry.observeTab("Secrets Found: 100%");
        telemetry.observeTab("Deaths: 1 (2)");
        telemetry.observeTab("Crypts: 5");
        telemetry.observeTab("Time: 5:00");
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
