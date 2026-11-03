package com.arena.pekaclient.hud;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.feature.DungeonContext;
import com.arena.pekaclient.render.Theme;

import java.util.Properties;

/** Borderless transient room-clear alert with a persistent HUD placement and scale. */
public final class RoomClearAlertHud extends HudElement {
    private static final String LABEL = "ROOM CLEARED";
    private long visibleUntilMillis;
    /** A settings-only local preview may render briefly even when the user intentionally has the alert disabled. */
    private long testVisibleUntilMillis;

    public RoomClearAlertHud() {
        super("roomClearAlert", "Room Clear Alert", new HudPlacement(HudPlacement.Anchor.CENTER, 0,
                HudPlacement.Anchor.START, 34), false);
        forceBorderless();
    }

    /** Shows one local alert for the configured bounded duration. */
    public void trigger(long nowMillis) {
        visibleUntilMillis = Math.max(visibleUntilMillis,
                nowMillis + AppleSettings.effectiveDungeonRoomClearDurationMillis());
    }

    /** Starts a settings-only preview without persisting or changing the master enable choice. */
    public void triggerTest(long nowMillis) {
        long until = nowMillis + AppleSettings.effectiveDungeonRoomClearDurationMillis();
        visibleUntilMillis = Math.max(visibleUntilMillis, until);
        testVisibleUntilMillis = Math.max(testVisibleUntilMillis, until);
    }

    /** Drops an expired/old-world alert immediately. */
    public void clear() {
        visibleUntilMillis = 0L;
        testVisibleUntilMillis = 0L;
    }

    public boolean active(long nowMillis) {
        return visibleUntilMillis > nowMillis;
    }

    /** Lets HudManager draw a one-shot Test Alert even if the regular enabled flag is off. */
    public boolean testPreviewActive(long nowMillis) {
        return testVisibleUntilMillis > nowMillis;
    }

    @Override
    public float width() {
        return textWidth(LABEL);
    }

    @Override
    public float height() {
        return 10F * textScale();
    }

    @Override
    protected void drawContent() {
        long now = System.currentTimeMillis();
        boolean test = testPreviewActive(now);
        if (!DungeonContext.inDungeon() && !backgroundOnHoverOnly && !test) return;
        if (!active(now) && !backgroundOnHoverOnly) return;
        text(LABEL, 0F, 0F, backgroundOnHoverOnly && !active(now)
                ? Theme.textSecondary : accentColor());
    }

    @Override
    protected void loadExtra(Properties p, String key) {
        forceBorderless();
    }

    @Override
    protected void saveExtra(Properties p, String key) {
        p.setProperty(key + "bg", "false");
        p.setProperty(key + "border", "false");
    }

    @Override
    protected void resetExtra() {
        clear();
        forceBorderless();
    }

    private void forceBorderless() {
        background = false;
        border = false;
    }
}
