package com.arena.pekaclient.hud;

/** Formatting and fixed-layout checks for the restored three-card Stats Display. */
public final class StatsHudTextTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("frame rate uses original lower-case fps unit", "115 fps".equals(StatsHudText.frameRate(115)));
        check("unknown frame rate is explicit", "-- fps".equals(StatsHudText.frameRate(-1)));
        check("mining has two decimal places", "12.30 BPS".equals(StatsHudText.mining(12.3f)));
        check("invalid mining value is safe", "0.00 BPS".equals(StatsHudText.mining(Float.NaN)));
        check("memory is used heap in whole MB", "64 MB".equals(StatsHudText.memory(64L * 1024L * 1024L + 999L)));
        check("unknown memory is explicit", "-- MB".equals(StatsHudText.memory(-1L)));
        check("cards preserve Frame Rate, Mining, Memory order", StatsHud.FRAME_RATE_TITLE.equals("FRAME RATE")
                && StatsHud.MINING_TITLE.equals("MINING") && StatsHud.MEMORY_TITLE.equals("MEMORY"));
        check("cards are adjacent at their fixed gap", StatsHud.cardLeft(0) == 0f
                && StatsHud.cardLeft(1) == StatsHud.CARD_WIDTH + StatsHud.CARD_GAP
                && StatsHud.cardLeft(2) == (StatsHud.CARD_WIDTH + StatsHud.CARD_GAP) * 2f);
        check("total width accounts for exactly three cards", StatsHud.CARD_WIDTH * 3f + StatsHud.CARD_GAP * 2f == 534f);
        check("reference bevel keeps its 8px cut and 2px dark offset", StatsHud.CARD_CUT == 8f
                && StatsHud.CARD_SHADOW_OFFSET_X == 2f && StatsHud.CARD_SHADOW_OFFSET_Y == 2f);
        StatsHud stats = new StatsHud();
        check("reference group starts centered near the top", stats.place.anchorX == HudPlacement.Anchor.CENTER
                && stats.place.offsetX == 0 && stats.place.anchorY == HudPlacement.Anchor.START
                && stats.place.offsetY == 8);
        check("three-card Stats Display remains opt-in on a fresh configuration", !stats.enabled);
        System.out.println("stats HUD text: passed " + passed + ", failed " + failed);
        if (failed != 0) throw new AssertionError(failed + " stats HUD checks failed");
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++; else { failed++; System.err.println("FAIL " + name); }
    }
}
