package com.arena.pekaclient.assist.dungeon;

import java.util.Locale;

/**
 * Native, data-only Catacombs score calculation.
 *
 * <p>The values are calculated solely from visible dungeon observations supplied by
 * {@link DungeonScoreTelemetry}. Floor secret targets and speed bands were independently reimplemented from
 * observed Fakepixel scoring behaviour; this class intentionally has no API, packet, entity, or inventory access.
 * A projected result is deliberately conservative: callers must use {@link Result#projectedRank()} rather than
 * promoting an unverified projection to S+.</p>
 */
public final class DungeonScoreCalculator {
    /** Represents a value that has not been observed on the visible sidebar/tab/chat yet. */
    public static final int UNKNOWN = -1;

    private DungeonScoreCalculator() {
    }

    /** Calculates each score component when its visible inputs are available. Unknown components remain -1. */
    public static Result calculate(Input input) {
        Input safe = input == null ? Input.builder().build() : input;
        FloorRule rule = floorRule(safe.floor);

        int effectiveDeaths = safe.deaths < 0 ? UNKNOWN
                : Math.max(0, safe.deaths - Math.max(0, safe.spiritDeathPenaltyReductions));
        int skill = effectiveDeaths < 0 ? UNKNOWN : clamp(100 - effectiveDeaths * 2
                - Math.max(0, safe.failedPuzzles) * 14, 0, 100);

        int clear = percentScore(safe.clearedPercent, 100, 60);
        int secret = rule == null ? UNKNOWN : percentScore(safe.secretsPercent, rule.secretPercent, 40);
        int exploration = clear < 0 || secret < 0 ? UNKNOWN : clear + secret;
        int speed = rule == null || safe.elapsedSeconds < 0 ? UNKNOWN : speedScore(safe.elapsedSeconds, rule.timeOffset);
        int bonus = safe.crypts < 0 ? UNKNOWN : Math.max(0, Math.min(5, safe.crypts))
                + (safe.mimicKilled ? 2 : 0) + (safe.paulActive ? 10 : 0);
        int total = skill < 0 || exploration < 0 || speed < 0 || bonus < 0 ? UNKNOWN
                : skill + exploration + speed + bonus;
        return new Result(rule, effectiveDeaths, skill, clear, secret, exploration, speed, bonus, total);
    }

    /** Normalizes the displayed Catacombs floor identifier, or returns an empty value when it is unsupported. */
    public static String canonicalFloor(String raw) {
        if (raw == null) return "";
        String value = raw.trim().toUpperCase(Locale.ROOT);
        return floorRule(value) == null ? "" : value;
    }

    /** Actual rank thresholds. Use only for server-confirmed/end-of-run scores in player-facing UI. */
    public static String rankForScore(int score) {
        if (score < 0) return "--";
        if (score >= 300) return "S+";
        if (score >= 270) return "S";
        if (score >= 230) return "A";
        if (score >= 160) return "B";
        if (score >= 100) return "C";
        return "D";
    }

    private static int percentScore(int percent, int required, int max) {
        if (percent < 0 || required <= 0) return UNKNOWN;
        int bounded = Math.max(0, Math.min(percent, required));
        return Math.max(0, Math.min(max, (int) Math.floor(max * (double) bounded / required)));
    }

    /** Applies Fakepixel's floor-adjusted speed bands with integer ceiling arithmetic. */
    private static int speedScore(int elapsedSeconds, int floorOffset) {
        int adjusted = Math.max(0, elapsedSeconds) + floorOffset;
        if (adjusted < 480) return 100;
        if (adjusted < 600) return clamp(140 - ceilDiv(adjusted, 12), 0, 100);
        if (adjusted < 840) return clamp(115 - ceilDiv(adjusted, 24), 0, 100);
        if (adjusted < 1140) return clamp(108 - ceilDiv(adjusted, 30), 0, 100);
        if (adjusted < 3940) return clamp((int) Math.ceil(98.5D - ceilDiv(adjusted, 40)), 0, 100);
        return 0;
    }

    private static int ceilDiv(int value, int divisor) {
        if (divisor <= 0) return 0;
        return (value + divisor - 1) / divisor;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static FloorRule floorRule(String raw) {
        if (raw == null) return null;
        if ("E0".equals(raw) || "F0".equals(raw)) return new FloorRule(raw, 20, -120);
        if ("F1".equals(raw)) return new FloorRule(raw, 30, -120);
        if ("F2".equals(raw)) return new FloorRule(raw, 40, -120);
        if ("F3".equals(raw)) return new FloorRule(raw, 50, -120);
        if ("F4".equals(raw)) return new FloorRule(raw, 60, -240);
        if ("F5".equals(raw)) return new FloorRule(raw, 70, -120);
        if ("F6".equals(raw)) return new FloorRule(raw, 85, -240);
        if ("F7".equals(raw)) return new FloorRule(raw, 100, -360);
        if ("M1".equals(raw)) return new FloorRule(raw, 30, 0);
        if ("M2".equals(raw)) return new FloorRule(raw, 40, 0);
        if ("M3".equals(raw)) return new FloorRule(raw, 50, 0);
        if ("M4".equals(raw)) return new FloorRule(raw, 60, 0);
        if ("M5".equals(raw)) return new FloorRule(raw, 70, 0);
        if ("M6".equals(raw)) return new FloorRule(raw, 85, -120);
        if ("M7".equals(raw)) return new FloorRule(raw, 100, -360);
        return null;
    }

    /** Immutable visible-data input. Values default to {@link #UNKNOWN}; booleans default to false until observed. */
    public static final class Input {
        public final String floor;
        public final int clearedPercent;
        public final int secretsPercent;
        public final int deaths;
        public final int failedPuzzles;
        public final int spiritDeathPenaltyReductions;
        public final int crypts;
        public final int elapsedSeconds;
        public final boolean mimicKilled;
        public final boolean paulActive;

        private Input(Builder builder) {
            floor = canonicalFloor(builder.floor);
            clearedPercent = builder.clearedPercent;
            secretsPercent = builder.secretsPercent;
            deaths = builder.deaths;
            failedPuzzles = builder.failedPuzzles;
            spiritDeathPenaltyReductions = builder.spiritDeathPenaltyReductions;
            crypts = builder.crypts;
            elapsedSeconds = builder.elapsedSeconds;
            mimicKilled = builder.mimicKilled;
            paulActive = builder.paulActive;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String floor = "";
            private int clearedPercent = UNKNOWN;
            private int secretsPercent = UNKNOWN;
            private int deaths = UNKNOWN;
            private int failedPuzzles;
            private int spiritDeathPenaltyReductions;
            private int crypts = UNKNOWN;
            private int elapsedSeconds = UNKNOWN;
            private boolean mimicKilled;
            private boolean paulActive;

            public Builder floor(String value) { floor = value == null ? "" : value; return this; }
            public Builder clearedPercent(int value) { clearedPercent = value; return this; }
            public Builder secretsPercent(int value) { secretsPercent = value; return this; }
            public Builder deaths(int value) { deaths = value; return this; }
            public Builder failedPuzzles(int value) { failedPuzzles = value; return this; }
            public Builder spiritDeathPenaltyReductions(int value) { spiritDeathPenaltyReductions = value; return this; }
            public Builder crypts(int value) { crypts = value; return this; }
            public Builder elapsedSeconds(int value) { elapsedSeconds = value; return this; }
            public Builder mimicKilled(boolean value) { mimicKilled = value; return this; }
            public Builder paulActive(boolean value) { paulActive = value; return this; }
            public Input build() { return new Input(this); }
        }
    }

    /** Immutable breakdown for HUD and end-score comparison. */
    public static final class Result {
        public final String floor;
        public final int requiredSecretsPercent;
        public final int effectiveDeaths;
        public final int skill;
        public final int clear;
        public final int secret;
        public final int exploration;
        public final int speed;
        public final int bonus;
        public final int total;

        private Result(FloorRule rule, int effectiveDeaths, int skill, int clear, int secret, int exploration,
                       int speed, int bonus, int total) {
            this.floor = rule == null ? "" : rule.id;
            this.requiredSecretsPercent = rule == null ? UNKNOWN : rule.secretPercent;
            this.effectiveDeaths = effectiveDeaths;
            this.skill = skill;
            this.clear = clear;
            this.secret = secret;
            this.exploration = exploration;
            this.speed = speed;
            this.bonus = bonus;
            this.total = total;
        }

        /** True only when every component was derived from a visible input. */
        public boolean complete() {
            return skill >= 0 && exploration >= 0 && speed >= 0 && bonus >= 0 && total >= 0;
        }

        /**
         * Safe live rank. An estimate never renders S+; that label is reserved for a server-confirmed end score.
         */
        public String projectedRank() {
            String rank = rankForScore(total);
            return "S+".equals(rank) ? "S" : rank;
        }
    }

    private static final class FloorRule {
        final String id;
        final int secretPercent;
        final int timeOffset;

        FloorRule(String id, int secretPercent, int timeOffset) {
            this.id = id;
            this.secretPercent = secretPercent;
            this.timeOffset = timeOffset;
        }
    }
}
