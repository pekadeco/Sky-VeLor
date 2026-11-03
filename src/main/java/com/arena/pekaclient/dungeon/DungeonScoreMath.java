package com.arena.pekaclient.dungeon;

import java.util.Locale;

/** Pure score math for the client-only dungeon HUD; no Minecraft or server state is read here. */
public final class DungeonScoreMath {
    private DungeonScoreMath() { }

    public static Result calculate(String floor, int timeSeconds, int secretsPercent, int clearPercent,
                                   int deaths, int failedPuzzles, int crypts, boolean paulMayor,
                                   boolean bloodDoorOpen) {
        return calculate(floor, timeSeconds, secretsPercent, clearPercent, deaths, failedPuzzles,
                crypts, paulMayor, bloodDoorOpen, 0);
    }

    /** Mirrors Better NEF 1.2.5 ScoreManager rules; a missing timer gives the same 100 speed points as NEF. */
    public static Result calculate(String floor, int timeSeconds, int secretsPercent, int clearPercent,
                                   int deaths, int failedPuzzles, int crypts, boolean paulMayor,
                                   boolean bloodDoorOpen, int reducedDeathPenalty) {
        int secretTarget = secretTarget(floor);
        int timeOffset = timeOffset(floor);
        if (secretTarget <= 0 || timeOffset == Integer.MIN_VALUE
                || secretsPercent < 0 || deaths < 0 || failedPuzzles < 0 || crypts < 0) {
            return null;
        }

        int secretPct = clamp(secretsPercent, 0, 100);
        // NEF applies the +9 Blood Door adjustment before clipping the final clear score;
        // an unknown clear percentage (-1) therefore becomes 8% (4 points) only after Blood opens.
        int effectiveClear = bloodDoorOpen ? Math.min(clearPercent + 9, 100) : clearPercent;
        int skill = clamp(100 - (deaths - Math.max(0, reducedDeathPenalty)) * 2 - failedPuzzles * 14, 0, 100);
        int explorationClear = clamp((int) Math.floor(60.0 * effectiveClear / 100.0), 0, 60);
        int explorationSecrets = clamp((int) Math.floor(40.0 * secretPct / secretTarget), 0, 40);
        int exploration = explorationClear + explorationSecrets;

        int speed;
        if (timeSeconds < 0) {
            // Better NEF's TablistParser returns an empty time as -1 and ScoreManager gives it 100 points.
            speed = 100;
        } else {
            int adjustedTime = timeSeconds + timeOffset;
            if (adjustedTime < 480) speed = 100;
            else if (adjustedTime < 600) speed = 140 - (int) Math.ceil(adjustedTime * (1f / 12f));
            else if (adjustedTime < 840) speed = 115 - (int) Math.ceil(adjustedTime * (1f / 24f));
            else if (adjustedTime < 1140) speed = 108 - (int) Math.ceil(adjustedTime * (1f / 30f));
            else if (adjustedTime < 3940) speed = (int) Math.ceil(98.5f - (int) Math.ceil(adjustedTime * (1f / 40f)));
            else speed = 0;
            speed = Math.max(0, speed);
        }
        int bonus = (paulMayor ? 10 : 0) + Math.min(5, crypts);
        return new Result(skill, exploration, explorationClear, explorationSecrets, speed, bonus, secretTarget);
    }

    public static int secretTarget(String floor) {
        if (floor == null) return -1;
        switch (floor.toUpperCase(Locale.ROOT)) {
            case "F1": return 30; case "F2": return 40; case "F3": return 50; case "F4": return 60;
            case "F5": return 70; case "F6": return 85; case "F7": return 100;
            case "M1": return 30; case "M2": return 40; case "M3": return 50; case "M4": return 60;
            case "M5": return 70; case "M6": return 85; case "M7": return 100;
            case "E0": case "F0": return 20;
            default: return -1;
        }
    }

    private static int timeOffset(String floor) {
        if (floor == null) return Integer.MIN_VALUE;
        switch (floor.toUpperCase(Locale.ROOT)) {
            case "E0": case "F0": case "F1": case "F2": case "F3": case "F5": case "M6": return -120;
            case "F4": case "F6": return -240;
            case "F7": case "M7": return -360;
            case "M1": case "M2": case "M3": case "M4": case "M5": return 0;
            default: return Integer.MIN_VALUE;
        }
    }

    /** Mirrors Better NEF's S+ secret requirement using 60 maximum clear points, not current clear points. */
    public static int requiredSecretPercent(int skill, int speed, int bonus, String floor) {
        int target = secretTarget(floor);
        if (target <= 0) return -1;
        int neededSecretScore = 300 - 60 - skill - speed - bonus;
        if (neededSecretScore > 40) return -1;
        return (int) Math.ceil(neededSecretScore * target / 40f);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static final class Result {
        public final int skill;
        public final int exploration;
        public final int explorationClear;
        public final int explorationSecrets;
        public final int speed;
        public final int bonus;
        public final int secretTarget;
        public final int total;

        private Result(int skill, int exploration, int explorationClear, int explorationSecrets,
                       int speed, int bonus, int secretTarget) {
            this.skill = skill;
            this.exploration = exploration;
            this.explorationClear = explorationClear;
            this.explorationSecrets = explorationSecrets;
            this.speed = speed;
            this.bonus = bonus;
            this.secretTarget = secretTarget;
            this.total = skill + exploration + speed + bonus;
        }
    }
}
