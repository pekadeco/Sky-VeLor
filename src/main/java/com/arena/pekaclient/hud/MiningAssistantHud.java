package com.arena.pekaclient.hud;

import com.arena.pekaclient.assist.mining.MiningAssistController;

/** Optional compact mining HUD. Its state strings are cached on client ticks by MiningAssistController. */
public final class MiningAssistantHud extends HudElement {
    private final MiningAssistController controller = new MiningAssistController();

    public MiningAssistantHud() {
        super("miningAssistant", "Mining Assistant", new HudPlacement(HudPlacement.Anchor.START, 6,
                HudPlacement.Anchor.START, 42), false);
    }

    /** The mod entry point registers this sole controller on Forge's event bus. */
    public MiningAssistController controller() {
        return controller;
    }

    @Override public float width() {
        return Math.max(textWidth(controller.miningLine()), Math.max(textWidth(controller.sessionLine()),
                textWidth(controller.abilityLine()))) + 16F;
    }

    @Override public float height() { return 42F * textScale(); }

    @Override protected void drawContent() {
        drawPanel(width(), height());
        float step = 11F * textScale();
        text(controller.miningLine(), 8F, 5F, accentColor());
        text(controller.sessionLine(), 8F, 5F + step, accentColor());
        text(controller.abilityLine(), 8F, 5F + step * 2F, accentColor());
    }
}
