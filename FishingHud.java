package com.arena.pekaclient.fishing;

import com.arena.pekaclient.hud.HudElement;
import com.arena.pekaclient.hud.HudPlacement;
import com.arena.pekaclient.render.Theme;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Fishing counter HUD: total legendary catches, plus Festival current and best Great White counts. */
public final class FishingHud extends HudElement {

    private static final float LINE_HEIGHT = 10f;

    /** Shown when on. */
    public boolean showCatches = true;
    public boolean showFestival = true;

    public FishingHud() {
        super("fishing", "Fishing counter",
                new HudPlacement(HudPlacement.Anchor.START, 6, HudPlacement.Anchor.CENTER, 0), false);
    }

    private String[] lines() {
        long now = System.currentTimeMillis();
        List<String> out = new ArrayList<String>();
        if (showCatches) {
            out.add("Legendary catches: " + FishingStats.totalCount());
        }
        if (showFestival) {
            out.add("Festival: " + FishingStats.festivalCurrent(now));
            out.add("Festival best: " + FishingStats.festivalBest());
        }
        return out.toArray(new String[0]);
    }

    @Override
    public float width() {
        float max = 0f;
        for (String s : lines()) {
            max = Math.max(max, textWidth(s));
        }
        return 20f + max;
    }

    @Override
    public float height() {
        return 12f + lines().length * LINE_HEIGHT * textScale();
    }

    @Override
    protected void drawContent() {
        String[] lines = lines();
        drawPanel(width(), height());
        float y = 6f;
        for (String s : lines) {
            text(s, 10f, y, Theme.textPrimary);
            y += LINE_HEIGHT * textScale();
        }
    }

    @Override
    protected void resetExtra() {
        showCatches = true;
        showFestival = true;
    }

    @Override
    protected void loadExtra(Properties p, String k) {
        showCatches = bool(p, k + "catches", true);
        showFestival = bool(p, k + "festival", true);
    }

    @Override
    protected void saveExtra(Properties p, String k) {
        p.setProperty(k + "catches", String.valueOf(showCatches));
        p.setProperty(k + "festival", String.valueOf(showFestival));
    }
}
