package com.arena.pekaclient.hud;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.feature.MiningTracker;

import java.util.Locale;
import java.util.Properties;

/**
 * Mining BPS HUD: successfully mined blocks per second, from {@link MiningTracker}.
 *
 * Replaces the old walking-speed HUD. Shows "Mining BPS: 2.40" by default. The label, decimals and
 * rolling window (1, 5 or 10 s) can be changed. Shows 0 when nothing has been mined in the window,
 * never a fake value.
 */
public final class MiningHud extends HudElement {

    public static final String[] LABELS = {"Mining BPS", "Mining", "Blocks/s", "None"};
    public static final int LABEL_NONE = LABELS.length - 1;
    public static final int[] WINDOW_SECONDS = {1, 5, 10};
    public static final String[] WINDOW_NAMES = {"1 s", "5 s", "10 s"};
    public static final String[] DECIMAL_NAMES = {"0", "1", "2"};

    public int label;
    public int windowIndex = 1;   // default 5 s
    public int decimals = 2;      // default 2 -> "2.40"

    private String cachedNumber;
    private float cachedRate = Float.NaN;
    private int cachedDecimals = -1;
    private String cachedFull;
    private String cachedFullNumber;
    private int cachedFullLabel = -1;

    public MiningHud() {
        super("mining", "Mining BPS",
                new HudPlacement(HudPlacement.Anchor.START, 6, HudPlacement.Anchor.START, 6), true);
    }

    /** The rolling window in seconds for the current setting. */
    public int windowSeconds() {
        return WINDOW_SECONDS[clamp(windowIndex, 0, WINDOW_SECONDS.length - 1)];
    }

    private String numberText() {
        float rate = MiningTracker.rate();
        if (AppleSettings.experimentalPerformance && cachedNumber != null
                && rate == cachedRate && decimals == cachedDecimals) {
            return cachedNumber;
        }
        String pattern = "%." + clamp(decimals, 0, 2) + "f";
        cachedNumber = String.format(Locale.ROOT, pattern, rate);
        cachedRate = rate;
        cachedDecimals = decimals;
        return cachedNumber;
    }

    /**
     * The whole line. With the experimental performance mode on, the joined string is reused until the
     * number or the label changes, so nothing is allocated per frame.
     */
    private String fullText() {
        int l = clamp(label, 0, LABELS.length - 1);
        String number = numberText();
        if (AppleSettings.experimentalPerformance && cachedFull != null
                && number == cachedFullNumber && l == cachedFullLabel) {
            return cachedFull;
        }
        String out = (l == LABEL_NONE) ? number : LABELS[l] + ": " + number;
        cachedFull = out;
        cachedFullNumber = number;
        cachedFullLabel = l;
        return out;
    }

    @Override
    public float width() {
        return 10f + textWidth(fullText()) + 10f;
    }

    @Override
    public float height() {
        return 9f * textScale() + 12f;
    }

    @Override
    protected void drawContent() {
        float w = width();
        float h = height();
        drawPanel(w, h);
        float ty = h / 2f - 4.5f * textScale();
        text(fullText(), 10f, ty, accentColor());
    }

    @Override
    protected void resetExtra() {
        label = 0;
        windowIndex = 1;
        decimals = 2;
    }

    @Override
    protected void loadExtra(Properties p, String k) {
        label = clamp(intOf(p, k + "label", 0), 0, LABELS.length - 1);
        windowIndex = clamp(intOf(p, k + "window", 1), 0, WINDOW_SECONDS.length - 1);
        decimals = clamp(intOf(p, k + "decimals", 2), 0, 2);
    }

    @Override
    protected void saveExtra(Properties p, String k) {
        p.setProperty(k + "label", String.valueOf(label));
        p.setProperty(k + "window", String.valueOf(windowIndex));
        p.setProperty(k + "decimals", String.valueOf(decimals));
    }
}
