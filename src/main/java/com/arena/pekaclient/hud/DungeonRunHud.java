package com.arena.pekaclient.hud;

import com.arena.pekaclient.assist.dungeon.DungeonRunReadout;
import com.arena.pekaclient.assist.dungeon.DungeonRunTracker;

/** Optional movable, read-only summary of values already visible in the dungeon sidebar. */
public final class DungeonRunHud extends HudElement {
    public DungeonRunHud() {
        super("dungeonRun", "Dungeon Run", new HudPlacement(HudPlacement.Anchor.END, 8,
                HudPlacement.Anchor.START, 72), false);
    }

    @Override
    public float width() {
        DungeonRunReadout readout = DungeonRunTracker.INSTANCE.readout();
        float widest = textWidth("DUNGEON RUN");
        widest = Math.max(widest, textWidth(readout.floorLine()));
        widest = Math.max(widest, textWidth(readout.timeLine()));
        widest = Math.max(widest, textWidth(readout.clearedLine()));
        widest = Math.max(widest, textWidth(readout.secretsLine()));
        widest = Math.max(widest, textWidth(readout.deathsLine()));
        return Math.max(112F, widest + 16F);
    }

    @Override
    public float height() {
        return 10F + 6F * (10F * textScale());
    }

    @Override
    protected void drawContent() {
        DungeonRunReadout readout = DungeonRunTracker.INSTANCE.readout();
        if (!readout.hasData() && !backgroundOnHoverOnly) return;
        float width = width();
        drawPanel(width, height());
        float y = 5F;
        text("DUNGEON RUN", 8F, y, accentColor());
        y += 10F * textScale();
        text(readout.floorLine(), 8F, y, accentColor());
        y += 10F * textScale();
        text(readout.timeLine(), 8F, y, accentColor());
        y += 10F * textScale();
        text(readout.clearedLine(), 8F, y, accentColor());
        y += 10F * textScale();
        text(readout.secretsLine(), 8F, y, accentColor());
        y += 10F * textScale();
        text(readout.deathsLine(), 8F, y, accentColor());
    }
}
