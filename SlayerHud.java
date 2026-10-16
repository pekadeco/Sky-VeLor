package com.arena.pekaclient.slayer;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.hud.HudElement;
import com.arena.pekaclient.hud.HudPlacement;
import com.arena.pekaclient.render.Theme;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/** Slayer tracker HUD: the active Slayer's drops, total profit, and the boss timer. */
public final class SlayerHud extends HudElement {

    private static final float LINE_HEIGHT = 10f;

    /** Per-element switches and the number of drop rows to show. */
    public boolean showProfit = true;
    public boolean showBossTimer = true;
    public int drops = SlayerTracker.MAX_TRACKED_DROPS;
    /** Off: show every tracked drop. The Drops slider only applies when this is on. */
    public boolean limitDrops;

    public SlayerHud() {
        super("slayer", "Slayer tracker",
                new HudPlacement(HudPlacement.Anchor.START, 6, HudPlacement.Anchor.START, 130), false);
    }

    private List<String> lines() {
        List<String> out = new ArrayList<String>();
        out.add("Slayer: " + SlayerTracker.displayName(SlayerTracker.getActiveSlayer()));
        List<SlayerTracker.DropEntry> drops = SlayerTracker.getDropHistory(SlayerTracker.getActiveSlayer());
        if (drops.isEmpty()) {
            out.add("Waiting for drops...");
        }
        int shown = Math.min(limitDrops ? this.drops : SlayerTracker.MAX_TRACKED_DROPS, drops.size());
        for (SlayerTracker.DropEntry drop : drops.subList(0, shown)) {
            String line = NumberFormat.getIntegerInstance(Locale.US).format(drop.quantity) + "x " + drop.item;
            if (AppleSettings.slayerPricing) {
                line += " (" + SlayerFormat.getDropValue(drop.quantity, SlayerTracker.getItemPrice(drop.item)) + ")";
            }
            out.add(line);
        }
        if (AppleSettings.slayerPricing && showProfit) {
            out.add("Total profit = " + SlayerFormat.formatCompactPrice(SlayerTracker.visibleTotalProfit()));
        }
        if (AppleSettings.slayerBossTimer && showBossTimer && SlayerTracker.hasBossTimer()) {
            out.add("Boss timer - " + SlayerFormat.formatTimerDuration(
                    SlayerTracker.bossTimerElapsedMillis(System.currentTimeMillis())));
        }
        return out;
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
        return 12f + lines().size() * LINE_HEIGHT * textScale();
    }

    @Override
    protected void drawContent() {
        List<String> lines = lines();
        drawPanel(width(), height());
        float y = 6f;
        for (int i = 0; i < lines.size(); i++) {
            boolean profit = lines.get(i).startsWith("Total profit");
            int color = profit ? 0xFFFFFF99 : Theme.textPrimary;
            text(lines.get(i), 10f, y, color);
            y += LINE_HEIGHT * textScale();
        }
    }

    @Override
    protected void resetExtra() {
        showProfit = true;
        showBossTimer = true;
        drops = SlayerTracker.MAX_TRACKED_DROPS;
        limitDrops = false;
    }

    @Override
    protected void loadExtra(Properties p, String k) {
        showProfit = bool(p, k + "profit", true);
        showBossTimer = bool(p, k + "timer", true);
        drops = clamp(intOf(p, k + "drops", SlayerTracker.MAX_TRACKED_DROPS), 1, SlayerTracker.MAX_TRACKED_DROPS);
        limitDrops = bool(p, k + "limitDrops", false);
    }

    @Override
    protected void saveExtra(Properties p, String k) {
        p.setProperty(k + "profit", String.valueOf(showProfit));
        p.setProperty(k + "timer", String.valueOf(showBossTimer));
        p.setProperty(k + "drops", String.valueOf(drops));
        p.setProperty(k + "limitDrops", String.valueOf(limitDrops));
    }
}
