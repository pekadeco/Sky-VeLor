package com.arena.pekaclient.fishing;

import com.arena.pekaclient.hud.HudElement;
import com.arena.pekaclient.hud.HudPlacement;
import com.arena.pekaclient.render.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

/**
 * Sea Creature HP HUD: lists nearby sea-creature nameplates (name, level and current / max health).
 * Scans the loaded entities at most every 500 ms, within 64 blocks, and shows at most 8 rows. Nothing
 * is sent to the server.
 */
public final class SeaCreatureHud extends HudElement {

    static final int DEFAULT_RANGE = 64;
    static final int MAX_RANGE = 64;
    static final long SCAN_INTERVAL_MS = 500L;
    static final int MAX_ROWS = 8;

    /** Scan radius in blocks, and how many rows to list. Both are set in the hub. */
    public int range = DEFAULT_RANGE;
    public int maxRows = MAX_ROWS;
    /** Off: scan the default 64 blocks. The Range slider only applies when this is on. */
    public boolean limitRange;
    /** Off: list every row up to the maximum. The Rows slider only applies when this is on. */
    public boolean limitRows;
    private static final float ROW_HEIGHT = 10f;

    private List<SeaCreatureParser.Entry> rows = Collections.emptyList();
    private long lastScanAt = Long.MIN_VALUE;

    public SeaCreatureHud() {
        super("seahp", "Sea Creature HP",
                new HudPlacement(HudPlacement.Anchor.START, 6, HudPlacement.Anchor.START, 90), false);
    }

    /** The nearby rows, rescanned when the cached scan is older than the interval. */
    List<SeaCreatureParser.Entry> rows() {
        long now = System.currentTimeMillis();
        if (now - lastScanAt >= SCAN_INTERVAL_MS || now < lastScanAt) {
            lastScanAt = now;
            rows = scan(limitRange ? range : DEFAULT_RANGE, limitRows ? maxRows : MAX_ROWS);
        }
        return rows;
    }

    /** Scans the loaded entities once. Returns at most {@link #MAX_ROWS} nearest creatures. */
    static List<SeaCreatureParser.Entry> scan(int range, int maxRows) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null || mc.thePlayer == null) {
            return Collections.emptyList();
        }
        EntityPlayer player = mc.thePlayer;
        double maxSq = (double) range * range;
        List<SeaCreatureParser.Entry> found = new ArrayList<SeaCreatureParser.Entry>();
        for (Object o : mc.theWorld.loadedEntityList) {
            if (!(o instanceof Entity)) {
                continue;
            }
            Entity entity = (Entity) o;
            if (!entity.hasCustomName()) {
                continue;
            }
            double distSq = player.getDistanceSqToEntity(entity);
            if (distSq > maxSq) {
                continue;
            }
            SeaCreatureParser.Entry entry = SeaCreatureParser.parseLabel(
                    entity.getDisplayName().getUnformattedText(), entity.getEntityId(), distSq);
            if (entry != null) {
                found.add(entry);
            }
        }
        Collections.sort(found, new Comparator<SeaCreatureParser.Entry>() {
            @Override
            public int compare(SeaCreatureParser.Entry a, SeaCreatureParser.Entry b) {
                return Double.compare(a.distanceSq, b.distanceSq);
            }
        });
        if (found.size() > maxRows) {
            return new ArrayList<SeaCreatureParser.Entry>(found.subList(0, maxRows));
        }
        return found;
    }

    static String rowText(SeaCreatureParser.Entry e) {
        StringBuilder sb = new StringBuilder();
        if (e.level >= 0) {
            sb.append("[Lvl ").append(e.level).append("] ");
        }
        sb.append(e.creatureName);
        if (!e.currentHealth.isEmpty() && !e.maxHealth.isEmpty()) {
            sb.append("  ").append(e.currentHealth).append('/').append(e.maxHealth);
        }
        return sb.toString();
    }

    private String[] lines() {
        List<SeaCreatureParser.Entry> list = rows();
        if (list.isEmpty()) {
            return new String[] {"Sea creatures: none nearby"};
        }
        String[] out = new String[list.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = rowText(list.get(i));
        }
        return out;
    }

    @Override
    public float width() {
        float max = textWidth("Sea creatures");
        for (String s : lines()) {
            max = Math.max(max, textWidth(s));
        }
        return 20f + max;
    }

    @Override
    public float height() {
        return 12f + lines().length * ROW_HEIGHT * textScale();
    }

    @Override
    protected void drawContent() {
        String[] lines = lines();
        drawPanel(width(), height());
        float y = 6f;
        for (String s : lines) {
            text(s, 10f, y, Theme.textPrimary);
            y += ROW_HEIGHT * textScale();
        }
    }

    @Override
    protected void resetExtra() {
        range = DEFAULT_RANGE;
        maxRows = MAX_ROWS;
        limitRange = false;
        limitRows = false;
    }

    @Override
    protected void loadExtra(Properties p, String k) {
        range = clamp(intOf(p, k + "range", DEFAULT_RANGE), 16, MAX_RANGE);
        maxRows = clamp(intOf(p, k + "rows", MAX_ROWS), 1, MAX_ROWS);
        limitRange = bool(p, k + "limitRange", false);
        limitRows = bool(p, k + "limitRows", false);
    }

    @Override
    protected void saveExtra(Properties p, String k) {
        p.setProperty(k + "range", String.valueOf(range));
        p.setProperty(k + "rows", String.valueOf(maxRows));
        p.setProperty(k + "limitRange", String.valueOf(limitRange));
        p.setProperty(k + "limitRows", String.valueOf(limitRows));
    }
}
