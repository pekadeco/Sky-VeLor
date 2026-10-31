package com.arena.pekaclient.dungeonesp;

/** Stable, allocation-free reason retained with a cached target for /lunaria espdump diagnostics. */
public enum DungeonEspMatchReason {
    NONE("not matched"),
    STAR_TAG("starred tag"),
    SPLIT_TAG("split tag stands"),
    REAL_ENDERMAN("real Enderman under tag"),
    ENDERMAN_HEAD("Enderman-head marker");

    private final String text;

    DungeonEspMatchReason(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }
}
