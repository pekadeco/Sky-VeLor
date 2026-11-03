package com.arena.pekaclient.fishing;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.hud.HudElement;
import com.arena.pekaclient.hud.HudPlacement;
import com.arena.pekaclient.render.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IChatComponent;

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
    /** Keeps the last valid rows briefly while a compatible server replaces its nameplate entities. */
    static final long DETECTION_GRACE_MS = 1500L;
    static final int MAX_ROWS = 8;

    /** Scan radius in blocks, and how many rows to list. Both are set in the HUD editor. */
    public int range = DEFAULT_RANGE;
    public int maxRows = MAX_ROWS;
    /** Off: scan the default 64 blocks. The Range slider only applies when this is on. */
    public boolean limitRange;
    /** Off: list every row up to the maximum. The Rows slider only applies when this is on. */
    public boolean limitRows;
    private static final float ROW_HEIGHT = 10f;

    private List<SeaCreatureParser.Entry> rows = Collections.emptyList();
    private long lastScanAt = Long.MIN_VALUE;
    private long lastDetectionAt = Long.MIN_VALUE;
    /** Allocated once per HUD; update itself performs no allocation. */
    private final EncounterState encounters = new EncounterState();

    public SeaCreatureHud() {
        super("seahp", "Sea Creature HP",
                new HudPlacement(HudPlacement.Anchor.START, 6, HudPlacement.Anchor.START, 90), false);
    }

    /** The nearby rows, rescanned when the cached scan is older than the interval. */
    List<SeaCreatureParser.Entry> rows() {
        long now = System.currentTimeMillis();
        if (shouldScan(now, lastScanAt)) {
            lastScanAt = now;
            List<SeaCreatureParser.Entry> detected =
                    scan(limitRange ? range : DEFAULT_RANGE, limitRows ? maxRows : MAX_ROWS);
            if (!detected.isEmpty()) {
                rows = detected;
                lastDetectionAt = now;
            } else if (!keepPreviousDetection(now, lastDetectionAt, rows)) {
                rows = Collections.emptyList();
            }
            String enteredTitle = encounters.update(rows);
            if (enteredTitle != null && AppleSettings.fishingNearbyEncounterAlert) {
                FishingStats.showCreatureTitle(enteredTitle);
            }
        }
        return rows;
    }

    /** Prevents a one-scan nameplate replacement from making the HP HUD flicker or disappear. */
    static boolean keepPreviousDetection(long now, long previousDetection,
                                         List<SeaCreatureParser.Entry> previousRows) {
        return previousRows != null && !previousRows.isEmpty()
                && previousDetection != Long.MIN_VALUE && now >= previousDetection
                && now - previousDetection <= DETECTION_GRACE_MS;
    }

    /** Tracks entry transitions so a stable nearby label produces one visual alert rather than a scan-rate title. */
    static final class EncounterState {
        private boolean yetiNearby;
        private boolean jawbusNearby;

        String update(List<SeaCreatureParser.Entry> detected) {
            boolean hasYeti = containsCreature(detected, "Yeti");
            boolean hasJawbus = containsCreature(detected, "Lord Jawbus");
            boolean yetiEntered = hasYeti && !yetiNearby;
            boolean jawbusEntered = hasJawbus && !jawbusNearby;
            yetiNearby = hasYeti;
            jawbusNearby = hasJawbus;
            // Jawbus remains the higher-priority title when both labels arrive on the same scan.
            if (jawbusEntered) {
                return FishingStats.LORD_JAWBUS_TITLE;
            }
            return yetiEntered ? FishingStats.YETI_TITLE : null;
        }
    }

    static boolean containsCreature(List<SeaCreatureParser.Entry> detected, String name) {
        if (detected == null || name == null) {
            return false;
        }
        for (SeaCreatureParser.Entry entry : detected) {
            if (entry != null && name.equalsIgnoreCase(entry.creatureName)) {
                return true;
            }
        }
        return false;
    }

    /** Handles the uninitialised sentinel without overflowing {@code now - Long.MIN_VALUE}. */
    static boolean shouldScan(long now, long previousScan) {
        return previousScan == Long.MIN_VALUE || now < previousScan
                || now - previousScan >= SCAN_INTERVAL_MS;
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
            // Most servers use a custom-name armor stand. Some compatible servers expose that label through a
            // scoreboard/display component instead, so permit armor stands through the fallback without scanning
            // arbitrary mobs' display names every interval.
            if (!entity.hasCustomName() && !(entity instanceof EntityArmorStand)) {
                continue;
            }
            double distSq = player.getDistanceSqToEntity(entity);
            if (distSq > maxSq) {
                continue;
            }
            SeaCreatureParser.Entry entry = parseEntityLabel(entity, distSq);
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

    /**
     * Reads custom, formatted, plain and scoreboard-composed text without assuming one server's nameplate
     * implementation. A health-bearing candidate wins over a name-only custom tag.
     */
    private static SeaCreatureParser.Entry parseEntityLabel(Entity entity, double distanceSq) {
        int id = entity.getEntityId();
        SeaCreatureParser.Entry best = null;
        best = betterEntry(best, SeaCreatureParser.parseLabel(entity.getCustomNameTag(), id, distanceSq));
        IChatComponent displayName = entity.getDisplayName();
        if (displayName != null) {
            best = betterEntry(best, SeaCreatureParser.parseLabel(displayName.getFormattedText(), id, distanceSq));
            best = betterEntry(best, SeaCreatureParser.parseLabel(displayName.getUnformattedText(), id, distanceSq));
        }
        // Some compatible Fakepixel nameplates expose their scoreboard-composed value only through getName().
        return betterEntry(best, SeaCreatureParser.parseLabel(entity.getName(), id, distanceSq));
    }

    /** Prefers the candidate carrying both health values instead of returning an incomplete custom tag. */
    static SeaCreatureParser.Entry betterEntry(SeaCreatureParser.Entry current,
                                                SeaCreatureParser.Entry candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null) {
            return candidate;
        }
        boolean currentHasHealth = !current.currentHealth.isEmpty() && !current.maxHealth.isEmpty();
        boolean candidateHasHealth = !candidate.currentHealth.isEmpty() && !candidate.maxHealth.isEmpty();
        return candidateHasHealth && !currentHasHealth ? candidate : current;
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
