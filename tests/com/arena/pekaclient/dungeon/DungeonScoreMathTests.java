package com.arena.pekaclient.dungeon;

public final class DungeonScoreMathTests {
    public static void main(String[] args) {
        DungeonScoreMath.Result f7 = DungeonScoreMath.calculate("F7", 300, 100, 100, 0, 0, 5, false, false);
        check(f7 != null, "complete F7 stats should calculate");
        equal(100, f7.skill, "skill score");
        equal(100, f7.exploration, "full clear and secrets");
        equal(100, f7.speed, "fast F7 time after floor offset");
        equal(5, f7.bonus, "crypt bonus capped at five");
        equal(305, f7.total, "F7 total score");

        DungeonScoreMath.Result partial = DungeonScoreMath.calculate("F7", 300, 50, 50, 1, 0, 2, false, true);
        check(partial != null, "partial but complete stats should calculate");
        equal(98, partial.skill, "one death penalty");
        equal(55, partial.exploration, "blood bonus contributes nine clear percentage points");
        equal(2, partial.bonus, "crypt bonus");
        equal(255, partial.total, "partial F7 score");

        DungeonScoreMath.Result paul = DungeonScoreMath.calculate("M1", 500, 100, 100, 0, 0, 5, true, false);
        check(paul != null, "M1 stats should calculate");
        equal(15, paul.bonus, "Paul perk bonus plus capped crypts");

        check(DungeonScoreMath.calculate("--", 300, 100, 100, 0, 0, 5, false, false) == null,
                "unknown floor must not invent score");
        DungeonScoreMath.Result missingTimer = DungeonScoreMath.calculate("F7", -1, 100, 100, 0, 0, 5, false, false);
        check(missingTimer != null, "missing run timer uses Better NEF's 100 speed points");
        equal(100, missingTimer.speed, "missing timer default speed");
        check(DungeonScoreMath.calculate("F7", 300, -1, 100, 0, 0, 5, false, false) == null,
                "unknown secret percentage must not invent score");
        DungeonScoreMath.Result spiritPet = DungeonScoreMath.calculate("F7", 300, 100, 100, 1, 0, 5, false, false, 1);
        check(spiritPet != null, "Spirit Pet modifier score calculates");
        equal(100, spiritPet.skill, "Spirit Pet reduces death penalty to one point equivalent");
        equal(88, DungeonScoreMath.requiredSecretPercent(100, 100, 5, "F7"), "required secret percent follows NEF formula");
        equal(-1, DungeonScoreMath.requiredSecretPercent(0, 0, 0, "F7"), "S+ can be unreachable by secrets alone");
        equal(20, DungeonScoreMath.secretTarget("E0"), "entrance secret target matches NEF");
        DungeonScoreMath.Result unknownClearBlood = DungeonScoreMath.calculate("F7", -1, 0, -1, 0, 0, 0, false, true);
        check(unknownClearBlood != null, "unknown clear remains calculable like NEF");
        equal(4, unknownClearBlood.explorationClear, "Blood Door adds nine before clear score clamping");
        System.out.println("DungeonScoreMathTests passed.");
    }

    private static void equal(int expected, int actual, String label) {
        if (expected != actual) throw new AssertionError(label + ": expected " + expected + ", got " + actual);
    }
    private static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }
}
