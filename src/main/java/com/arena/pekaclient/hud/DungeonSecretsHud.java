package com.arena.pekaclient.hud;

import com.arena.pekaclient.dungeon.DungeonProgressTracker;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;

/** Compact, movable Lunaria secrets counter; uses real observed values and never invents a denominator. */
public final class DungeonSecretsHud extends HudElement {
    public DungeonSecretsHud() {
        super("dungeonSecrets", "Dungeon Secrets", new HudPlacement(HudPlacement.Anchor.END, 8,
                HudPlacement.Anchor.START, 118), false);
    }

    private String countText() {
        DungeonProgressTracker t = HudManager.DUNGEON_PROGRESS;
        if (t.totalScore() < 0) return "Secrets: waiting for dungeon data";
        if (t.secretsPercent() >= 0 && t.secretTargetPercent() > 0) return t.secretDisplayText();
        if (t.secretsRequired() >= 0 && t.secretsFound() >= 0) return "Secrets: " + t.secretsFound() + "/" + t.secretsRequired();
        if (t.secretsFound() >= 0) return "Secrets: " + t.secretsFound() + " found";
        return "Secrets: waiting for dungeon data";
    }

    @Override public float width() { return Math.max(100f, textWidth(countText()) + 20f); }
    @Override public float height() { return 34f; }
    @Override protected void drawContent() {
        float w = width(), h = height();
        drawPanel(w, h);
        UIRender.roundRect(8f, 5f, w - 16f, 2f, 1f, accentColor(), accentColor(), 1f);
        text("SECRET TRACKER", 8f, 10f, accentColor());
        text(countText(), 8f, 21f, Theme.textPrimary);
    }
}
