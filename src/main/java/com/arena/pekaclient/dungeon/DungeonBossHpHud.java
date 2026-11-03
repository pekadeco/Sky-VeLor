package com.arena.pekaclient.dungeon;

import com.arena.pekaclient.feature.SkyblockContext;
import com.arena.pekaclient.hud.HudElement;
import com.arena.pekaclient.hud.HudPlacement;
import com.arena.pekaclient.render.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.BossStatus;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Boss HP HUD for the four F7/M7 Withers and all five Master VII dragons. */
public final class DungeonBossHpHud extends HudElement {

    static final int RANGE = 160;
    static final long SCAN_INTERVAL_MS = 250L;
    static final long DETECTION_GRACE_MS = 1500L;
    private static final float ROW_HEIGHT = 10f;
    private static final double M7_DRAGON_MAX_HEALTH = 1000000000D;
    private static final String FAKEPIXEL_DRAGON_MAX_HEALTH = "900M";
    private static final Map<Integer, M7OverlayRenderer.M7DragonStatue> DRAGON_TYPES =
            new LinkedHashMap<Integer, M7OverlayRenderer.M7DragonStatue>();
    private static Object dragonCacheWorld;

    private List<DungeonBossParser.Entry> rows = Collections.emptyList();
    private long lastScanAt = Long.MIN_VALUE;
    private long lastDetectionAt = Long.MIN_VALUE;

    public DungeonBossHpHud() {
        super("dungeonbosshp", "Boss HP",
                new HudPlacement(HudPlacement.Anchor.START, 6,
                        HudPlacement.Anchor.START, 140), true);
    }

    private List<DungeonBossParser.Entry> rows() {
        if (!SkyblockContext.isInSkyblock()) {
            rows = Collections.emptyList();
            lastScanAt = Long.MIN_VALUE;
            lastDetectionAt = Long.MIN_VALUE;
            return rows;
        }
        long now = System.currentTimeMillis();
        if (lastScanAt == Long.MIN_VALUE || now < lastScanAt || now - lastScanAt >= SCAN_INTERVAL_MS) {
            lastScanAt = now;
            List<DungeonBossParser.Entry> detected = scan();
            if (!detected.isEmpty()) {
                rows = detected;
                lastDetectionAt = now;
            } else if (lastDetectionAt == Long.MIN_VALUE || now < lastDetectionAt
                    || now - lastDetectionAt > DETECTION_GRACE_MS) {
                rows = Collections.emptyList();
            }
        }
        return rows;
    }

    static List<DungeonBossParser.Entry> scan() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null || mc.thePlayer == null) {
            return Collections.emptyList();
        }
        if (dragonCacheWorld != mc.theWorld) {
            DRAGON_TYPES.clear();
            dragonCacheWorld = mc.theWorld;
        }
        String scoreboardText = getSidebarScoreboardText(mc);
        boolean m7Run = M7OverlayRenderer.isM7ScoreboardText(scoreboardText);
        boolean fakepixelDragonSidebar =
                M7OverlayRenderer.isFakepixelM7DragonScoreboardText(scoreboardText);
        double maxDistance = (double) RANGE * RANGE;
        Map<String, DungeonBossParser.Entry> found = new LinkedHashMap<String, DungeonBossParser.Entry>();

        // Hypixel sends the real SkyBlock HP in the vanilla boss-bar title. The Wither entity's
        // own health is often only a scaled transport value such as 250/250, so the bar is the
        // authoritative source for the supported Withers and M7 dragons.
        if (BossStatus.statusBarTime > 0) {
            DungeonBossParser.Entry bossBar = DungeonBossParser.parseLabel(
                    BossStatus.bossName, -1, -1D);
            if (bossBar != null && bossBar.hasHealth()) {
                found.put(bossBar.bossName, bossBar);
            }
        }

        // Fakepixel's M7 sidebar exposes one current-HP value per active dragon rather than a
        // current/max fraction. Its dragons use a 900M maximum, so these rows are authoritative.
        if (fakepixelDragonSidebar) {
            for (String dragonName : DungeonBossParser.DRAGON_NAMES) {
                DungeonBossParser.Entry sidebar = DungeonBossParser.parseCurrentHealthLabel(
                        scoreboardText, dragonName, FAKEPIXEL_DRAGON_MAX_HEALTH, -2, -2D);
                if (sidebar != null) {
                    found.put(sidebar.bossName, sidebar);
                }
            }
        }

        for (Object object : mc.theWorld.loadedEntityList) {
            if (!(object instanceof Entity)) {
                continue;
            }
            Entity entity = (Entity) object;
            double distance = mc.thePlayer.getDistanceSqToEntity(entity);
            if (distance > maxDistance) {
                continue;
            }
            DungeonBossParser.Entry entry = parseEntity(entity, distance, m7Run);
            if (entry == null) {
                continue;
            }
            DungeonBossParser.Entry old = found.get(entry.bossName);
            if (old == null || (entry.hasHealth() && !old.hasHealth())
                    || (entry.hasHealth() == old.hasHealth() && entry.distanceSq < old.distanceSq)) {
                found.put(entry.bossName, entry);
            }
        }
        List<DungeonBossParser.Entry> ordered = new ArrayList<DungeonBossParser.Entry>();
        for (String boss : DungeonBossParser.TARGET_NAMES) {
            DungeonBossParser.Entry entry = found.get(boss);
            if (entry != null) {
                ordered.add(entry);
            }
        }
        return ordered;
    }

    private static String getSidebarScoreboardText(Minecraft mc) {
        if (mc == null || mc.theWorld == null) {
            return "";
        }
        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        if (scoreboard == null) {
            return "";
        }
        ScoreObjective sidebar = scoreboard.getObjectiveInDisplaySlot(1);
        if (sidebar == null) {
            return "";
        }
        StringBuilder text = new StringBuilder(sidebar.getDisplayName());
        for (Score score : scoreboard.getSortedScores(sidebar)) {
            String playerName = score.getPlayerName();
            if (playerName == null || playerName.startsWith("#")) {
                continue;
            }
            ScorePlayerTeam team = scoreboard.getPlayersTeam(playerName);
            text.append(' ').append(ScorePlayerTeam.formatPlayerName(team, playerName));
        }
        return text.toString();
    }

    private static DungeonBossParser.Entry parseEntity(Entity entity, double distanceSq,
                                                         boolean m7Run) {
        int id = entity.getEntityId();
        DungeonBossParser.Entry best = null;
        best = better(best, DungeonBossParser.parseLabel(entity.getCustomNameTag(), id, distanceSq));
        if (entity.getDisplayName() != null) {
            best = better(best, DungeonBossParser.parseLabel(
                    entity.getDisplayName().getFormattedText(), id, distanceSq));
            best = better(best, DungeonBossParser.parseLabel(
                    entity.getDisplayName().getUnformattedText(), id, distanceSq));
        }
        best = better(best, DungeonBossParser.parseLabel(entity.getName(), id, distanceSq));

        if (entity instanceof EntityDragon && entity instanceof EntityLivingBase && m7Run) {
            M7OverlayRenderer.M7DragonStatue statue = DRAGON_TYPES.get(id);
            if (best != null) {
                M7OverlayRenderer.M7DragonStatue named =
                        M7OverlayRenderer.dragonStatueForName(best.bossName);
                if (named != null) {
                    statue = named;
                    DRAGON_TYPES.put(id, named);
                }
            }
            if (statue == null) {
                // M7's dragon entities can be named only "Ender Dragon". Assign their type at
                // first sight from the spawn statue and retain it while they fly around the arena.
                statue = M7OverlayRenderer.nearestM7DragonStatue(entity.posX, entity.posZ);
                if (statue != null) {
                    DRAGON_TYPES.put(id, statue);
                }
            }
            if (statue != null && (best == null || !best.hasHealth())) {
                EntityLivingBase dragon = (EntityLivingBase) entity;
                double current = scaledDragonHealth(dragon.getHealth(), dragon.getMaxHealth());
                if (current >= 0D) {
                    return new DungeonBossParser.Entry(statue.dragonName,
                            DungeonBossParser.compactHealth(current),
                            DungeonBossParser.compactHealth(M7_DRAGON_MAX_HEALTH), id, distanceSq);
                }
            }
        }

        if (best == null) {
            return null;
        }
        if (!best.hasHealth() && (entity instanceof EntityWither || entity instanceof EntityDragon)
                && entity instanceof EntityLivingBase) {
            EntityLivingBase living = (EntityLivingBase) entity;
            // Values such as Wither 250/250 or Dragon 200/200 are vanilla-scaled transport
            // health, not SkyBlock boss HP. Only use fallback when it carries an unscaled value.
            if (isUsableEntityHealth(living.getMaxHealth())) {
                return new DungeonBossParser.Entry(best.bossName,
                        DungeonBossParser.compactHealth(living.getHealth()),
                        DungeonBossParser.compactHealth(living.getMaxHealth()), id, distanceSq);
            }
        }
        // A name-only entity is not enough: wait for its numeric nameplate or boss-bar title.
        return best.hasHealth() ? best : null;
    }

    static boolean isUsableEntityHealth(double maxHealth) {
        return maxHealth > 1000D && !Double.isNaN(maxHealth) && !Double.isInfinite(maxHealth);
    }

    static double scaledDragonHealth(double entityHealth, double entityMaxHealth) {
        if (entityHealth < 0D || entityMaxHealth <= 0D || Double.isNaN(entityHealth)
                || Double.isNaN(entityMaxHealth) || Double.isInfinite(entityHealth)
                || Double.isInfinite(entityMaxHealth)) {
            return -1D;
        }
        double ratio = Math.max(0D, Math.min(1D, entityHealth / entityMaxHealth));
        return ratio * M7_DRAGON_MAX_HEALTH;
    }

    static boolean shouldDraw(boolean inEditor, int detectedRows) {
        return inEditor || detectedRows > 0;
    }

    static DungeonBossParser.Entry better(DungeonBossParser.Entry current,
                                           DungeonBossParser.Entry candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null || (candidate.hasHealth() && !current.hasHealth())) {
            return candidate;
        }
        return current;
    }

    static String styledRow(DungeonBossParser.Entry entry) {
        StringBuilder text = new StringBuilder("\u00A76\u226A \u00A7c")
                .append(entry.bossName);
        if (entry.hasHealth()) {
            text.append(" \u00A7a").append(entry.currentHealth).append('/')
                    .append(entry.maxHealth).append("\u00A7c\u2764");
        }
        return text.append(" \u00A76\u226B").toString();
    }

    private String[] lines() {
        List<DungeonBossParser.Entry> current = rows();
        if (current.isEmpty()) {
            return new String[0];
        }
        String[] out = new String[current.size()];
        for (int i = 0; i < current.size(); i++) {
            out[i] = styledRow(current.get(i));
        }
        return out;
    }

    @Override
    public float width() {
        float max = textWidth("\u00A7e\u00A7lBoss HP");
        String[] current = lines();
        if (current.length == 0) {
            // Keep a useful drag target in the HUD editor while remaining invisible in game.
            max = Math.max(max, textWidth("\u00A76\u226A \u00A7cMaxor \u00A7a100M/100M\u00A7c\u2764 \u00A76\u226B"));
        }
        for (String line : current) {
            max = Math.max(max, textWidth(line));
        }
        return 20f + max;
    }

    @Override
    public float height() {
        int rowCount = Math.max(1, lines().length);
        return 12f + (rowCount + 1) * ROW_HEIGHT * textScale();
    }

    @Override
    protected void drawContent() {
        String[] current = lines();
        if (!shouldDraw(backgroundOnHoverOnly, current.length)) {
            return;
        }
        if (current.length == 0) {
            current = new String[] {"\u00A76\u226A \u00A7cMaxor \u00A7a100M/100M\u00A7c\u2764 \u00A76\u226B"};
        }
        drawPanel(width(), height());
        float y = 6f;
        text("\u00A7e\u00A7lBoss HP", 10f, y, Theme.textPrimary);
        y += ROW_HEIGHT * textScale();
        for (String line : current) {
            text(line, 10f, y, Theme.textPrimary);
            y += ROW_HEIGHT * textScale();
        }
    }
}
