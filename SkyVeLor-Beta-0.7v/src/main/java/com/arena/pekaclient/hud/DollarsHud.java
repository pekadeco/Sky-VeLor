package com.arena.pekaclient.hud;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;

import java.util.Locale;
import java.util.Properties;

/**
 * Balance (purse) HUD. The value comes from {@link BalanceSource}: the scoreboard, or a manual number.
 * An unknown balance is drawn as "--". It is never shown as 0.
 */
public final class DollarsHud extends HudElement {

    public static final String UNKNOWN_TEXT = "--";

    public static final String[] LABELS = {"Balance", "Purse", "Coins", "None"};
    public static final String[] FORMATS = {"Full", "Short", "Plain"};
    public static final int FORMAT_FULL = 0;
    public static final int FORMAT_SHORT = 1;
    public static final int FORMAT_PLAIN = 2;

    public int label;             // index into LABELS
    public int format = FORMAT_FULL;
    public boolean coinIcon = true;

    private long cachedValue = Long.MIN_VALUE;
    private int cachedFormat = -1;
    private String cachedText = UNKNOWN_TEXT;
    private final String[] labelPrefix = new String[LABELS.length];
    private String shownCache;
    private String shownValue;
    private int shownLabel = -1;

    public DollarsHud() {
        super("dollars", "Player Dollars",
                new HudPlacement(HudPlacement.Anchor.END, 6, HudPlacement.Anchor.START, 6), true);
    }

    /** Formats a balance. Returns "--" when the balance is unknown. */
    public static String format(long value, int mode) {
        if (value == BalanceSource.UNKNOWN) {
            return UNKNOWN_TEXT;
        }
        if (mode == FORMAT_PLAIN) {
            return Long.toString(value);
        }
        if (mode == FORMAT_SHORT) {
            if (value >= 1000000000L) {
                return String.format(Locale.ROOT, "%.2fB", value / 1.0e9);
            }
            if (value >= 1000000L) {
                return String.format(Locale.ROOT, "%.2fM", value / 1.0e6);
            }
            if (value >= 1000L) {
                return String.format(Locale.ROOT, "%.1fK", value / 1.0e3);
            }
            return Long.toString(value);
        }
        return String.format(Locale.ROOT, "%,d", value);
    }

    private String valueText() {
        long v = BalanceSource.value();
        if (v != cachedValue || format != cachedFormat) {
            cachedValue = v;
            cachedFormat = format;
            cachedText = format(v, format);
        }
        return cachedText;
    }

    /** "Balance " for the label, reused from a table when the experimental performance mode is on. */
    private String labelText(int l) {
        if (!AppleSettings.experimentalPerformance) {
            return LABELS[l] + " ";
        }
        if (labelPrefix[l] == null) {
            labelPrefix[l] = LABELS[l] + " ";
        }
        return labelPrefix[l];
    }

    /** The line used to measure the element: label and value. Reused while nothing changes in experimental mode. */
    private String shownText() {
        String v = valueText();
        if (!showsLabel()) {
            return v;
        }
        if (AppleSettings.experimentalPerformance && shownCache != null && v == shownValue && label == shownLabel) {
            return shownCache;
        }
        String out = labelText(label) + v;
        shownCache = out;
        shownValue = v;
        shownLabel = label;
        return out;
    }

    private boolean showsLabel() {
        return label != LABELS.length - 1;
    }

    private float iconSpace() {
        return coinIcon ? 14f : 0f;
    }

    @Override
    public float width() {
        return 10f + iconSpace() + textWidth(shownText()) + 10f;
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
        float ts = textScale();
        float ty = h / 2f - 4.5f * ts;
        float x = 10f;
        if (coinIcon) {
            UIRender.circle(x + 5f, h / 2f, 5f, 0xFFFFD60A);
            UIRender.circle(x + 5f, h / 2f, 3f, 0xFFC79A00);
            x += 14f;
        }
        if (showsLabel()) {
            x += text(labelText(label), x, ty, Theme.textSecondary);
        }
        int color = BalanceSource.value() == BalanceSource.UNKNOWN ? Theme.textTertiary : accentColor();
        text(valueText(), x, ty, color);
    }

    @Override
    protected void resetExtra() {
        label = 0;
        format = FORMAT_FULL;
        coinIcon = true;
    }

    @Override
    protected void loadExtra(Properties p, String k) {
        label = clamp(intOf(p, k + "label", 0), 0, LABELS.length - 1);
        format = clamp(intOf(p, k + "format", FORMAT_FULL), 0, FORMATS.length - 1);
        coinIcon = bool(p, k + "coin", true);
    }

    @Override
    protected void saveExtra(Properties p, String k) {
        p.setProperty(k + "label", String.valueOf(label));
        p.setProperty(k + "format", String.valueOf(format));
        p.setProperty(k + "coin", String.valueOf(coinIcon));
    }
}
