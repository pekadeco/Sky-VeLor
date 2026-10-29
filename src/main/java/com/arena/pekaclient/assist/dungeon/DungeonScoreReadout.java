package com.arena.pekaclient.assist.dungeon;

/** Immutable snapshot of visible dungeon-score observations and their native calculation. */
public final class DungeonScoreReadout {
    public final String floor;
    public final int clearedPercent;
    public final int secretsPercent;
    public final int deaths;
    public final int crypts;
    public final int elapsedSeconds;
    public final int failedPuzzles;
    public final int spiritDeathPenaltyReductions;
    public final boolean mimicKilled;
    public final boolean paulActive;
    public final int officialTotal;
    public final String officialRank;
    public final DungeonScoreCalculator.Result calculated;

    DungeonScoreReadout(String floor, int clearedPercent, int secretsPercent, int deaths, int crypts,
                        int elapsedSeconds, int failedPuzzles, int spiritDeathPenaltyReductions,
                        boolean mimicKilled, boolean paulActive, int officialTotal, String officialRank) {
        this.floor = floor == null ? "" : floor;
        this.clearedPercent = clearedPercent;
        this.secretsPercent = secretsPercent;
        this.deaths = deaths;
        this.crypts = crypts;
        this.elapsedSeconds = elapsedSeconds;
        this.failedPuzzles = failedPuzzles;
        this.spiritDeathPenaltyReductions = spiritDeathPenaltyReductions;
        this.mimicKilled = mimicKilled;
        this.paulActive = paulActive;
        this.officialTotal = officialTotal;
        this.officialRank = officialRank == null ? "" : officialRank;
        calculated = DungeonScoreCalculator.calculate(DungeonScoreCalculator.Input.builder()
                .floor(this.floor).clearedPercent(clearedPercent).secretsPercent(secretsPercent).deaths(deaths)
                .crypts(crypts).elapsedSeconds(elapsedSeconds).failedPuzzles(failedPuzzles)
                .spiritDeathPenaltyReductions(spiritDeathPenaltyReductions).mimicKilled(mimicKilled)
                .paulActive(paulActive).build());
    }

    /** True when at least one current-run value came from a visible sidebar, tab row, or dungeon chat line. */
    public boolean hasData() {
        return floor.length() > 0 || clearedPercent >= 0 || secretsPercent >= 0 || deaths >= 0 || crypts >= 0
                || elapsedSeconds >= 0 || officialTotal >= 0;
    }

    /** True only after a score total appeared in a final visible score recap. */
    public boolean hasOfficialScore() {
        return officialTotal >= 0;
    }

    /** Uses the final visible total when available, otherwise the complete live estimate. */
    public int displayScore() {
        return officialTotal >= 0 ? officialTotal : calculated.total;
    }

    /** Live estimates deliberately cap at S; only a visible final total can display S+. */
    public String displayRank() {
        if (officialTotal >= 0) {
            String derived = DungeonScoreCalculator.rankForScore(officialTotal);
            // Treat malformed/unexpected rank text as presentation only; the observed total remains authoritative.
            if ("S+".equals(officialRank) && "S+".equals(derived)) return "S+";
            if ("S".equals(officialRank) && "S".equals(derived)) return "S";
            if ("A".equals(officialRank) && "A".equals(derived)) return "A";
            if ("B".equals(officialRank) && "B".equals(derived)) return "B";
            if ("C".equals(officialRank) && "C".equals(derived)) return "C";
            if ("D".equals(officialRank) && "D".equals(derived)) return "D";
            return derived;
        }
        return calculated.projectedRank();
    }

    public String scoreLine() {
        int score = displayScore();
        return score < 0 ? "Score: --" : "Score: " + score + " (" + displayRank() + ")";
    }

    public String cryptsLine() {
        return crypts < 0 ? "Crypts: --" : "Crypts: " + crypts;
    }

    public String deathsLine() {
        return deaths < 0 ? "Deaths: --" : "Deaths: " + deaths;
    }

    public String timeLine() {
        return elapsedSeconds < 0 ? "Run time: --" : "Run time: " + timeText(elapsedSeconds);
    }

    public String calculationStatus() {
        if (hasOfficialScore()) return "verified visible end score";
        return calculated.complete() ? "visible-data estimate" : "waiting for visible dungeon data";
    }

    static String timeText(int seconds) {
        int bounded = Math.max(0, seconds);
        int minutes = bounded / 60;
        int remainder = bounded % 60;
        return minutes + ":" + (remainder < 10 ? "0" : "") + remainder;
    }
}
