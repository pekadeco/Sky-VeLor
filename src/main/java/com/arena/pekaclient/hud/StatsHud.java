package com.arena.pekaclient.hud;

import com.arena.pekaclient.feature.MiningTracker;
import com.arena.pekaclient.render.UIRender;

/**
 * Restored three-card status HUD. Its fixed Frame Rate / Mining / Memory order and cut-corner treatment follow the
 * supplied reference: muted pixel headings, cyan frame-rate and BPS values, and a white memory value.
 *
 * <p>It deliberately reuses {@link ClientMetrics}' bounded FPS/heap reader and {@link MiningTracker}'s existing BPS
 * calculation. The renderer changes presentation only; it does not create a second metric sampler.</p>
 */
public final class StatsHud extends HudElement {

    public static final String FRAME_RATE_TITLE = "FRAME RATE";
    public static final String MINING_TITLE = "MINING";
    public static final String MEMORY_TITLE = "MEMORY";
    public static final float CARD_WIDTH = 172f;
    public static final float CARD_HEIGHT = 102f;
    public static final float CARD_GAP = 9f;
    public static final float CARD_CUT = 8f;
    /** The reference cards have one shared, crisp offset edge rather than a generic rounded drop shadow. */
    public static final float CARD_SHADOW_OFFSET_X = 2f;
    public static final float CARD_SHADOW_OFFSET_Y = 2f;

    private static final int CARD_SHADOW = 0xC0101012;
    private static final int CARD_FILL = 0xFF404044;
    private static final int TITLE_COLOR = 0xFF89898E;
    private static final int CYAN = 0xFF5FC8F3;
    private static final int MEMORY_COLOR = 0xFFF4F4F6;

    private final ClientMetrics.Reader metrics = ClientMetrics.liveReader();
    private float lastMiningRate = Float.NaN;
    private String cachedMining = StatsHudText.mining(0f);

    public StatsHud() {
        super("stats", "Stats Display",
                new HudPlacement(HudPlacement.Anchor.CENTER, 0, HudPlacement.Anchor.START, 8), false);
    }

    @Override
    public float width() {
        return CARD_WIDTH * 3f + CARD_GAP * 2f;
    }

    @Override
    public float height() {
        return CARD_HEIGHT;
    }

    /** Card positions are intentionally fixed so the three items stay adjacent as shown in the reference. */
    static float cardLeft(int index) {
        return Math.max(0, Math.min(2, index)) * (CARD_WIDTH + CARD_GAP);
    }

    @Override
    protected void drawContent() {
        ClientMetrics.Snapshot snapshot = metrics.snapshot(System.currentTimeMillis());
        drawCard(cardLeft(0), FRAME_RATE_TITLE, StatsHudText.frameRate(snapshot.fps()), CYAN);
        drawCard(cardLeft(1), MINING_TITLE, miningText(), CYAN);
        drawCard(cardLeft(2), MEMORY_TITLE, StatsHudText.memory(snapshot.usedBytes()), MEMORY_COLOR);
    }

    private String miningText() {
        float current = MiningTracker.rate();
        if (Float.compare(current, lastMiningRate) != 0) {
            lastMiningRate = current;
            cachedMining = StatsHudText.mining(current);
        }
        return cachedMining;
    }

    private void drawCard(float x, String title, String value, int valueColor) {
        // The two-pixel dark offset provides the visible separation/shadow without a rounded generic panel.
        UIRender.bevelRect(x + CARD_SHADOW_OFFSET_X, CARD_SHADOW_OFFSET_Y,
                CARD_WIDTH, CARD_HEIGHT, CARD_CUT, CARD_SHADOW);
        UIRender.bevelRect(x, 0f, CARD_WIDTH, CARD_HEIGHT, CARD_CUT, CARD_FILL);
        float textSize = textScale();
        UIRender.text(fr(), title, x + 12f, 13f, TITLE_COLOR, .84f * textSize, false);
        UIRender.text(fr(), value, x + 12f, CARD_HEIGHT - 34f, valueColor, 1.45f * textSize, false);
    }
}
