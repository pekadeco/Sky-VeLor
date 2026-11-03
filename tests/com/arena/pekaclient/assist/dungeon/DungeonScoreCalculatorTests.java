package com.arena.pekaclient.assist.dungeon;

/** Headless score-band and safe-rank checks for the native visible-data calculator. */
public final class DungeonScoreCalculatorTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        floorAwareScoresDoNotInventMimicBonus();
        bloodDoorAdjustmentUsesOnlyVisibleChatState();
        failuresAndSpiritReductionAffectSkill();
        speedBandsRespectFloorOffsets();
        incompleteObservationsDoNotInventATotal();
        done();
    }

    private static void floorAwareScoresDoNotInventMimicBonus() {
        DungeonScoreCalculator.Result result = DungeonScoreCalculator.calculate(DungeonScoreCalculator.Input.builder()
                .floor("F7").clearedPercent(100).secretsPercent(100).deaths(0).crypts(5)
                .elapsedSeconds(300).mimicKilled(true).build());
        check("F7 has its visible 100 percent secret target", result.requiredSecretsPercent == 100);
        check("full clear and secrets produce 100 exploration", result.clear == 60 && result.secret == 40
                && result.exploration == 100);
        check("F7 offset preserves a 100 speed score for a quick run", result.speed == 100);
        check("Mimic observation does not invent a score bonus", result.bonus == 5);
        check("complete live score sums only supported components", result.total == 305 && result.complete());
        check("unverified 300-plus projection is never labelled S+", "S".equals(result.projectedRank()));
        check("actual score threshold still identifies server-confirmed S+", "S+".equals(DungeonScoreCalculator.rankForScore(305)));
    }

    private static void bloodDoorAdjustmentUsesOnlyVisibleChatState() {
        DungeonScoreCalculator.Result unopened = DungeonScoreCalculator.calculate(DungeonScoreCalculator.Input.builder()
                .floor("F7").clearedPercent(91).secretsPercent(100).deaths(0).crypts(0)
                .elapsedSeconds(300).build());
        DungeonScoreCalculator.Result opened = DungeonScoreCalculator.calculate(DungeonScoreCalculator.Input.builder()
                .floor("F7").clearedPercent(91).secretsPercent(100).deaths(0).crypts(0)
                .elapsedSeconds(300).bloodDoorOpened(true).build());
        check("raw sidebar clear remains unadjusted until Blood Door chat", unopened.adjustedClearedPercent == 91
                && unopened.clear == 54);
        check("visible Blood Door chat adds the capped nine clear percent", opened.adjustedClearedPercent == 100
                && opened.clear == 60 && opened.exploration == 100);
    }

    private static void failuresAndSpiritReductionAffectSkill() {
        DungeonScoreCalculator.Result result = DungeonScoreCalculator.calculate(DungeonScoreCalculator.Input.builder()
                .floor("F1").clearedPercent(100).secretsPercent(15).deaths(2).failedPuzzles(1)
                .spiritDeathPenaltyReductions(1).crypts(5).elapsedSeconds(600).build());
        check("F1 secret target is floor-specific", result.requiredSecretsPercent == 30 && result.secret == 20);
        check("one Spirit reduction removes one two-point death penalty", result.effectiveDeaths == 1);
        check("failed puzzle and effective death reduce skill", result.skill == 84);
        check("exploration combines rooms and floor-aware secrets", result.exploration == 80);
        check("F1 time offset is applied before speed bands", result.speed == 100);
        check("all values sum into the observed estimate", result.total == 269);
    }

    private static void speedBandsRespectFloorOffsets() {
        DungeonScoreCalculator.Result master = DungeonScoreCalculator.calculate(DungeonScoreCalculator.Input.builder()
                .floor("M1").clearedPercent(0).secretsPercent(0).deaths(0).crypts(0).elapsedSeconds(600).build());
        DungeonScoreCalculator.Result normal = DungeonScoreCalculator.calculate(DungeonScoreCalculator.Input.builder()
                .floor("F1").clearedPercent(0).secretsPercent(0).deaths(0).crypts(0).elapsedSeconds(600).build());
        check("master floor has no F1 offset in its visible speed band", master.speed == 90);
        check("normal F1 offset moves the same elapsed time into 100 speed", normal.speed == 100);
        check("canonical floor parser rejects arbitrary labels", "".equals(DungeonScoreCalculator.canonicalFloor("F8")));
    }

    private static void incompleteObservationsDoNotInventATotal() {
        DungeonScoreCalculator.Result result = DungeonScoreCalculator.calculate(DungeonScoreCalculator.Input.builder()
                .floor("F6").clearedPercent(80).deaths(0).crypts(2).elapsedSeconds(400).build());
        check("missing secrets leave exploration unavailable", result.secret == DungeonScoreCalculator.UNKNOWN
                && result.exploration == DungeonScoreCalculator.UNKNOWN);
        check("missing component leaves total unavailable instead of assuming points", result.total == DungeonScoreCalculator.UNKNOWN
                && !result.complete() && "--".equals(result.projectedRank()));
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++;
        else { failed++; System.out.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("dungeon score calculator: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
