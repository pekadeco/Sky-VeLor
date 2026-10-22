package com.arena.pekaclient.assist.dungeon;

import com.arena.pekaclient.feature.DungeonContext;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.BossStatus;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Fixed-size, timer-scanned F7/M7 boss cache. It never allocates during HUD rendering and does no world scan per
 * frame; incomplete/ambiguous names are ignored rather than guessed.
 */
public final class DungeonBossTracker {
    private static final int SCAN_INTERVAL_TICKS = 5;
    private static final int RANGE_SQ = 160 * 160;
    private static final int MAX_ROWS = 9;
    private static final BossHealthParser.Entry[] ENTRIES = new BossHealthParser.Entry[MAX_ROWS];
    private static final String[] LINES = new String[MAX_ROWS];
    private static int count;
    private int untilScan;
    private Object knownWorld;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null || mc.thePlayer == null || !DungeonContext.inF7OrM7()) {
            clear();
            knownWorld = mc == null ? null : mc.theWorld;
            return;
        }
        if (knownWorld != mc.theWorld) {
            knownWorld = mc.theWorld;
            untilScan = 0;
            clear();
        }
        if (--untilScan > 0) return;
        untilScan = SCAN_INTERVAL_TICKS;
        scan(mc);
    }

    public static int count() {
        return count;
    }

    public static String lineAt(int index) {
        return index >= 0 && index < count ? LINES[index] : "";
    }

    private static void scan(Minecraft mc) {
        clear();
        if (BossStatus.statusBarTime > 0) add(BossHealthParser.parse(BossStatus.bossName));
        for (int i = 0; i < mc.theWorld.loadedEntityList.size(); i++) {
            Object raw = mc.theWorld.loadedEntityList.get(i);
            if (!(raw instanceof Entity)) continue;
            Entity entity = (Entity) raw;
            if (mc.thePlayer.getDistanceSqToEntity(entity) > RANGE_SQ) continue;
            BossHealthParser.Entry parsed = BossHealthParser.parse(entity.getCustomNameTag());
            if (parsed == null && entity.getDisplayName() != null) {
                parsed = BossHealthParser.parse(entity.getDisplayName().getUnformattedText());
            }
            if (parsed == null) continue;
            if (!parsed.hasHealth() && entity instanceof EntityLivingBase) {
                EntityLivingBase living = (EntityLivingBase) entity;
                if (living.getMaxHealth() > 1000F) {
                    parsed = new BossHealthParser.Entry(parsed.name, BossHealthParser.compact(living.getHealth()),
                            BossHealthParser.compact(living.getMaxHealth()));
                }
            }
            add(parsed);
        }
    }

    private static void add(BossHealthParser.Entry entry) {
        if (entry == null) return;
        for (int i = 0; i < count; i++) {
            if (!ENTRIES[i].name.equals(entry.name)) continue;
            if (entry.hasHealth() && !ENTRIES[i].hasHealth()) {
                ENTRIES[i] = entry;
                LINES[i] = line(entry);
            }
            return;
        }
        if (count >= MAX_ROWS) return;
        ENTRIES[count] = entry;
        LINES[count] = line(entry);
        count++;
    }

    private static String line(BossHealthParser.Entry entry) {
        return entry.hasHealth() ? entry.name + " " + entry.current + "/" + entry.maximum + "\u2764" : entry.name;
    }

    private static void clear() {
        for (int i = 0; i < count; i++) {
            ENTRIES[i] = null;
            LINES[i] = null;
        }
        count = 0;
    }
}
