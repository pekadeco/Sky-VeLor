package com.arena.pekaclient.slayer;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.config.ConfigFiles;
import com.arena.pekaclient.fishing.SeaCreatureParser;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Slayer drop tracker: rare drops per Slayer type from chat, a boss timer from the boss spawn to the
 * quest completion, and manual prices for profit. Drop history lasts for the session. Prices are saved
 * to {@code skyvelor-slayer-prices.properties}. Ported from RNG Carried Bald; nothing is sent to the server.
 */
public final class SlayerTracker {

    private static final Logger LOG = LogManager.getLogger("SkyVeLor");
    static final int MAX_TRACKED_DROPS = 4;
    /** The same drop seen twice within this window counts once (chat can repeat). */
    static final long DUPLICATE_DROP_WINDOW_MS = 750L;
    static final String PRICE_FILE = "skyvelor-slayer-prices.properties";
    private static final String PRICE_PREFIX = "price.";

    /** One tracked drop: the item name and how many. */
    public static final class DropEntry {
        public final String item;
        public final long quantity;

        DropEntry(String item, long quantity) {
            this.item = item;
            this.quantity = Math.max(1L, quantity);
        }
    }

    private static final Map<SlayerType, List<DropEntry>> HISTORY =
            new EnumMap<SlayerType, List<DropEntry>>(SlayerType.class);
    private static final Map<String, Long> PRICES = new HashMap<String, Long>();
    private static SlayerType activeSlayer = SlayerType.SPIDER;
    private static String lastDropKey = "";
    private static long lastDropAt;
    private static boolean bossTimerActive;
    private static boolean bossTimerHasValue;
    private static SlayerType bossTimerType;
    private static long bossTimerStartedAt;
    private static long lastBossDurationMillis;
    private static boolean pricesLoaded;

    public SlayerTracker() {
    }

    // ------------------------------------------------------------------ events
    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (event == null || event.message == null) {
            return;
        }
        onChat(event.message.getUnformattedText(), System.currentTimeMillis());
    }

    /** Starts the boss timer when a supported boss entity appears in the world. */
    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event == null || event.world == null || !event.world.isRemote || event.entity == null
                || !AppleSettings.slayerBossTimer) {
            return;
        }
        String name = event.entity.getDisplayName().getUnformattedText();
        SlayerType boss = DropMessageParser.detectSlayerBossName(name);
        if (boss != null) {
            startBossTimer(boss, System.currentTimeMillis());
        }
    }

    /** Handles one chat line. Kept separate from the event so the rules are easy to follow. */
    static void onChat(String message, long now) {
        if (message == null) {
            return;
        }
        if (AppleSettings.slayerBossTimer) {
            SlayerType spawned = DropMessageParser.detectSlayerBossSpawnMessage(message);
            if (spawned != null) {
                startBossTimer(spawned, now);
            }
            if (DropMessageParser.isSlayerQuestCompleteMessage(message)) {
                completeBossTimer(now);
            }
        }
        if (!AppleSettings.slayerTracker) {
            return;
        }
        SlayerType detected = DropMessageParser.detectSlayer(message);
        if (detected == null) {
            return;
        }
        String item = DropMessageParser.extractDropItem(message);
        if (item.length() == 0) {
            item = "Unknown item";
        }
        long quantity = DropMessageParser.extractDropQuantity(message);
        String key = detected.name() + "|" + DropMessageParser.normalizeItemName(item) + "|" + quantity;
        long elapsed = now - lastDropAt;
        if (key.equals(lastDropKey) && elapsed >= 0L && elapsed <= DUPLICATE_DROP_WINDOW_MS) {
            return;
        }
        lastDropKey = key;
        lastDropAt = now;
        recordDrop(detected, item, quantity);
    }

    // ------------------------------------------------------------------ drops
    static void recordDrop(SlayerType slayer, String item, long quantity) {
        if (slayer == null) {
            return;
        }
        activeSlayer = slayer;
        List<DropEntry> history = historyFor(slayer);
        String key = DropMessageParser.normalizeItemName(item);
        for (int i = 0; i < history.size(); i++) {
            DropEntry existing = history.get(i);
            if (DropMessageParser.normalizeItemName(existing.item).equals(key)) {
                long combined = SlayerFormat.addDropQuantities(existing.quantity, quantity);
                history.remove(i);
                history.add(0, new DropEntry(item, combined));
                return;
            }
        }
        history.add(0, new DropEntry(item, quantity));
        while (history.size() > MAX_TRACKED_DROPS) {
            history.remove(history.size() - 1);
        }
    }

    private static List<DropEntry> historyFor(SlayerType slayer) {
        List<DropEntry> history = HISTORY.get(slayer);
        if (history == null) {
            history = new ArrayList<DropEntry>();
            HISTORY.put(slayer, history);
        }
        return history;
    }

    public static List<DropEntry> getDropHistory(SlayerType slayer) {
        return Collections.unmodifiableList(historyFor(slayer));
    }

    public static SlayerType getActiveSlayer() {
        return activeSlayer;
    }

    /** Clears one Slayer's drops. The boss timer is cleared too if it belongs to that Slayer. */
    public static void clearHistory(SlayerType slayer) {
        if (slayer == null) {
            return;
        }
        historyFor(slayer).clear();
        if (slayer.name().equals(lastDropKey.split("\\|", 2)[0])) {
            lastDropKey = "";
            lastDropAt = 0L;
        }
        if (bossTimerType == slayer) {
            resetBossTimer();
        }
    }

    // ------------------------------------------------------------------ boss timer
    private static void startBossTimer(SlayerType boss, long now) {
        if (bossTimerActive && bossTimerType == boss) {
            return;
        }
        bossTimerType = boss;
        bossTimerStartedAt = now;
        bossTimerActive = true;
        bossTimerHasValue = true;
    }

    private static void completeBossTimer(long now) {
        if (!bossTimerActive) {
            return;
        }
        lastBossDurationMillis = Math.max(0L, now - bossTimerStartedAt);
        bossTimerActive = false;
    }

    public static boolean hasBossTimer() {
        return bossTimerHasValue;
    }

    public static boolean isBossTimerActive() {
        return bossTimerActive;
    }

    /** Running time while the boss is alive, or the last finished time. */
    public static long bossTimerElapsedMillis(long now) {
        if (!bossTimerHasValue) {
            return 0L;
        }
        return bossTimerActive ? Math.max(0L, now - bossTimerStartedAt) : lastBossDurationMillis;
    }

    private static void resetBossTimer() {
        bossTimerActive = false;
        bossTimerHasValue = false;
        bossTimerType = null;
        bossTimerStartedAt = 0L;
        lastBossDurationMillis = 0L;
    }

    // ------------------------------------------------------------------ prices
    /** Sets the manual price of an item. The item name is cleaned and normalised first. */
    public static void setItemPrice(String item, long price) {
        String clean = DropMessageParser.extractDropItem(SeaCreatureParser.stripFormatting(item).trim());
        String key = DropMessageParser.normalizeItemName(clean);
        if (key.length() == 0 || price < 0L) {
            return;
        }
        ensurePricesLoaded();
        PRICES.put(key, price);
        savePrices();
    }

    public static Long getItemPrice(String item) {
        ensurePricesLoaded();
        return PRICES.get(DropMessageParser.normalizeItemName(item));
    }

    /** Total value of the active Slayer's drops that have a price. */
    public static long visibleTotalProfit() {
        long total = 0L;
        for (DropEntry entry : historyFor(activeSlayer)) {
            total = SlayerFormat.addPricedDropValue(total, getItemPrice(entry.item), entry.quantity);
        }
        return total;
    }

    private static void ensurePricesLoaded() {
        if (pricesLoaded) {
            return;
        }
        pricesLoaded = true;
        Properties p = ConfigFiles.read(new File(dir(), PRICE_FILE));
        for (String name : p.stringPropertyNames()) {
            if (!name.startsWith(PRICE_PREFIX)) {
                continue;
            }
            try {
                long price = Long.parseLong(p.getProperty(name).trim());
                String item = DropMessageParser.normalizeItemName(name.substring(PRICE_PREFIX.length()));
                if (item.length() > 0 && price >= 0L) {
                    PRICES.put(item, price);
                }
            } catch (NumberFormatException ignored) {
                // A malformed price is skipped. The other prices still load.
            }
        }
    }

    private static void savePrices() {
        Properties p = new Properties();
        for (Map.Entry<String, Long> e : PRICES.entrySet()) {
            p.setProperty(PRICE_PREFIX + e.getKey(), Long.toString(e.getValue()));
        }
        try (OutputStream out = new FileOutputStream(new File(dir(), PRICE_FILE))) {
            p.store(out, "Sky VeLor Slayer prices");
        } catch (IOException e) {
            LOG.warn("Could not save Slayer prices", e);
        }
    }

    private static File dir() {
        return Minecraft.getMinecraft().mcDataDir;
    }

    /** The lowercase display name for a Slayer, e.g. "Spider". */
    public static String displayName(SlayerType type) {
        String lower = type.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
