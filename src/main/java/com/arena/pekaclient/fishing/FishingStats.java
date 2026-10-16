package com.arena.pekaclient.fishing;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.config.ConfigFiles;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

/**
 * Fishing counters from local catch chat: how many of each sea creature you caught, the Great White
 * Festival count and personal best, and the Lord Jawbus arrival title. Counts are saved to
 * {@code skyvelor-fishing.properties}. Nothing is sent to the server.
 */
public final class FishingStats {

    static final String FILE_NAME = "skyvelor-fishing.properties";
    /** The same creature announced twice within this window counts once (chat can repeat). */
    static final long DUPLICATE_WINDOW_MS = 750L;
    /** The Jawbus arrival line can repeat; the title shows once per window. */
    static final long JAWBUS_WINDOW_MS = 3000L;

    public static final FishingSessionTracker FESTIVAL = new FishingSessionTracker();

    private static final Map<LegendaryCreature, Long> COUNTS =
            new EnumMap<LegendaryCreature, Long>(LegendaryCreature.class);
    private static LegendaryCreature lastCreature;
    private static long lastCreatureAt;
    private static long lastJawbusAt;
    private static boolean loaded;

    public FishingStats() {
    }

    // ---------------------------------------------------------------- queries
    public static long count(LegendaryCreature creature) {
        ensureLoaded();
        Long value = COUNTS.get(creature);
        return value == null ? 0L : value;
    }

    public static long totalCount() {
        ensureLoaded();
        long total = 0L;
        for (LegendaryCreature creature : LegendaryCreature.values()) {
            total += count(creature);
        }
        return total;
    }

    public static long festivalCurrent(long now) {
        return FESTIVAL.getCurrentFestivalGreatWhiteCount(now);
    }

    public static long festivalBest() {
        ensureLoaded();
        return FESTIVAL.getBestFestivalGreatWhiteCount();
    }

    // ---------------------------------------------------------------- recording
    /**
     * Handles one chat line. Returns the creature that was counted, or null. Kept separate from the
     * event so the rules can be tested without a running game.
     */
    public static LegendaryCreature record(String message, boolean systemMessage, long now) {
        if (message == null) {
            return null;
        }
        ensureLoaded();
        if (systemMessage && SeaCreatureParser.isFestivalEnd(message)) {
            FESTIVAL.endFestival();
        }
        if (systemMessage && SeaCreatureParser.isLordJawbusArrival(message)) {
            if (AppleSettings.fishingJawbusAlert && (lastJawbusAt == 0L || now - lastJawbusAt >= JAWBUS_WINDOW_MS)) {
                lastJawbusAt = now;
                showJawbusTitle();
            }
        }
        LegendaryCreature creature = SeaCreatureParser.detectLegendaryCreature(message);
        if (creature == null) {
            return null;
        }
        if (creature == lastCreature && now - lastCreatureAt >= 0L
                && now - lastCreatureAt <= DUPLICATE_WINDOW_MS) {
            return null;
        }
        lastCreature = creature;
        lastCreatureAt = now;
        if (AppleSettings.fishingCounter) {
            COUNTS.put(creature, count(creature) + 1L);
        }
        if (AppleSettings.fishingFestival && creature == LegendaryCreature.GREAT_WHITE_SHARK) {
            FESTIVAL.recordGreatWhiteCatch(now);
        }
        save();
        return creature;
    }

    private static void showJawbusTitle() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null && minecraft.ingameGUI != null) {
            minecraft.ingameGUI.displayTitle("\u00A7c\u00A7lLORD JAWBUS", "", 1, 45, 1);
        }
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (event == null || event.message == null) {
            return;
        }
        record(event.message.getUnformattedText(), event.type == 1, System.currentTimeMillis());
    }

    // ---------------------------------------------------------------- persistence
    /**
     * Loads the saved counts once. Every read and write calls this first, so a save can never run
     * before the file has been read. Otherwise an empty save could overwrite the real counts.
     */
    public static void ensureLoaded() {
        if (!loaded) {
            load();
        }
    }

    public static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        Properties p = ConfigFiles.read(new File(dir(), FILE_NAME));
        COUNTS.clear();
        for (LegendaryCreature creature : LegendaryCreature.values()) {
            COUNTS.put(creature, parseLong(p.getProperty("count." + creature.name()), 0L));
        }
        FESTIVAL.loadBestFestivalGreatWhiteCount(parseLong(p.getProperty("festival.best"), 0L));
    }

    public static void save() {
        ensureLoaded();
        Properties p = new Properties();
        for (LegendaryCreature creature : LegendaryCreature.values()) {
            p.setProperty("count." + creature.name(), Long.toString(count(creature)));
        }
        p.setProperty("festival.best", Long.toString(festivalBest()));
        ConfigFiles.writeAtomic(new File(dir(), FILE_NAME), p, "Sky VeLor fishing counters");
    }

    /** Clears every counter and the festival best. */
    public static void reset() {
        ensureLoaded();
        COUNTS.clear();
        FESTIVAL.clear();
        save();
    }

    private static long parseLong(String value, long fallback) {
        try {
            return Math.max(0L, Long.parseLong(value == null ? "" : value.trim()));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static File dir() {
        return Minecraft.getMinecraft().mcDataDir;
    }
}
