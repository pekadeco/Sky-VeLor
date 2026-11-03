package com.arena.pekaclient.hud;

import com.arena.pekaclient.assist.dungeon.DungeonBossTracker;

/** Optional cached boss-health HUD. It renders only timer-scanned tracker strings, never scans entities itself. */
public final class DungeonBossHud extends HudElement {
    public DungeonBossHud() {
        super("dungeonBossHp", "Boss HP", new HudPlacement(HudPlacement.Anchor.START, 6,
                HudPlacement.Anchor.START, 140), false);
    }

    @Override public float width() {
        float widest = textWidth("Boss HP");
        int rows = DungeonBossTracker.count();
        if (rows == 0) return Math.max(widest, textWidth("Maxor 100M/100M\u2764")) + 16F;
        for (int i = 0; i < rows; i++) widest = Math.max(widest, textWidth(DungeonBossTracker.lineAt(i)));
        return widest + 16F;
    }

    @Override public float height() {
        return (DungeonBossTracker.count() + 2) * 11F * textScale() + 6F;
    }

    @Override protected void drawContent() {
        int rows = DungeonBossTracker.count();
        if (rows == 0 && !backgroundOnHoverOnly) return;
        drawPanel(width(), height());
        float y = 5F;
        text("Boss HP", 8F, y, accentColor());
        y += 11F * textScale();
        if (rows == 0) {
            text("Maxor 100M/100M\u2764", 8F, y, accentColor());
            return;
        }
        for (int i = 0; i < rows; i++) {
            text(DungeonBossTracker.lineAt(i), 8F, y, accentColor());
            y += 11F * textScale();
        }
    }
}
