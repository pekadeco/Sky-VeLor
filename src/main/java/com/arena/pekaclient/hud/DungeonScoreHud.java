package com.arena.pekaclient.hud;

import com.arena.pekaclient.assist.dungeon.DungeonScoreReadout;
import com.arena.pekaclient.assist.dungeon.DungeonScoreTracker;
import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.feature.DungeonContext;
import com.arena.pekaclient.render.Theme;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Borderless, movable Dungeon Score/Stats HUD populated only from visible dungeon observations. */
public final class DungeonScoreHud extends HudElement {
    /** A one-time layout migration distinguishes the old accidental default-off key from a later explicit choice. */
    private static final String DEFAULT_ON_MIGRATION_KEY = "visibleDefaultV2";

    public DungeonScoreHud() {
        // A fresh HUD layout must not silently hide the score readout. A player can still explicitly disable it.
        super("dungeonScore", "Dungeon Score/Stats HUD", new HudPlacement(HudPlacement.Anchor.END, 8,
                HudPlacement.Anchor.START, 8), true);
        forceBorderless();
    }

    @Override
    public float width() {
        float widest = 1F;
        for (String line : lines(DungeonScoreTracker.INSTANCE.readout())) widest = Math.max(widest, textWidth(line));
        return widest;
    }

    @Override
    public float height() {
        int count = lines(DungeonScoreTracker.INSTANCE.readout()).size();
        return Math.max(1F, count * 10F * textScale());
    }

    @Override
    protected void drawContent() {
        if (!DungeonContext.inDungeon() && !backgroundOnHoverOnly) return;
        // Inside a confirmed dungeon retain the layout while values are still arriving. The readout methods render
        // unavailable inputs as --, rather than making a newly detected run look like the HUD vanished.
        DungeonScoreReadout readout = DungeonScoreTracker.INSTANCE.readout();
        List<String> lines = lines(readout);
        float y = 0F;
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            text(line, 0F, y, index == 0 ? accentColor() : Theme.textPrimary);
            y += 10F * textScale();
        }
    }

    @Override
    protected void loadExtra(Properties p, String key) {
        // Beta-0.10.9.3 wrote this new HUD as disabled even for a player who had never made a choice. Upgrade that
        // one historical default once; subsequent saved false values are always an explicit user preference.
        if (!"true".equalsIgnoreCase(p.getProperty(key + DEFAULT_ON_MIGRATION_KEY))) enabled = true;
        forceBorderless();
    }

    @Override
    protected void saveExtra(Properties p, String key) {
        // Preserve a strict borderless contract even when an older generic HUD editor wrote style fields.
        p.setProperty(key + "bg", "false");
        p.setProperty(key + "border", "false");
        p.setProperty(key + DEFAULT_ON_MIGRATION_KEY, "true");
    }

    @Override
    protected void resetExtra() {
        forceBorderless();
    }

    private void forceBorderless() {
        background = false;
        border = false;
    }

    private static List<String> lines(DungeonScoreReadout readout) {
        List<String> result = new ArrayList<String>();
        // The editor must communicate useful size/line placement outside a dungeon rather than showing four blanks.
        boolean sample = backgroundOnHoverOnly && (readout == null || !readout.hasData());
        if (sample) {
            if (AppleSettings.dungeonScoreShowScore) result.add("Score: 287 (S)");
            if (AppleSettings.dungeonScoreShowCrypts) result.add("Crypts: 4");
            if (AppleSettings.dungeonScoreShowDeaths) result.add("Deaths: 1");
            if (AppleSettings.dungeonScoreShowRunTime) result.add("Run time: 6:32");
        } else if (readout != null) {
            if (AppleSettings.dungeonScoreShowScore) result.add(readout.scoreLine());
            if (AppleSettings.dungeonScoreShowCrypts) result.add(readout.cryptsLine());
            if (AppleSettings.dungeonScoreShowDeaths) result.add(readout.deathsLine());
            if (AppleSettings.dungeonScoreShowRunTime) result.add(readout.timeLine());
        }
        // A disabled group remains selectable in the dedicated editor without drawing a stray in-game placeholder.
        if (result.isEmpty() && backgroundOnHoverOnly) result.add("Dungeon Score HUD");
        return result;
    }
}
