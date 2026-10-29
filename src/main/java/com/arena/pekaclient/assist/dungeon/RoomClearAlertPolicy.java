package com.arena.pekaclient.assist.dungeon;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Conservative room-clear gate with two independent visible signals: a server chat confirmation and a sidebar
 * Cleared-percent increase. Both pass through the same room/run de-duplication so a server that emits both produces
 * one alert, not two.
 */
public final class RoomClearAlertPolicy {
    private static final long UNKNOWN_ROOM_DEDUPLICATION_MS = 15000L;
    private static final long CROSS_SOURCE_DEDUPLICATION_MS = 1200L;
    private static final int MAX_REMEMBERED_ROOMS = 64;
    private static final int MAX_PROGRESS_JUMP = 25;
    private static final Pattern CLEAR_MESSAGE = Pattern.compile(
            "(?i)\\b(?:room\\s+cleared|(?:(?:you\\s+)?(?:have\\s+)?)cleared\\s+(?:the|this)\\s+room)\\b"
                    + "|\\b(?:(?:you\\s+)?(?:have\\s+)?)cleared\\s+\\d{1,3}\\s*%\\s+"
                    + "(?:of\\s+)?(?:the\\s+)?dungeon\\b");

    private final Set<String> clearedRooms = new LinkedHashSet<String>();
    private final Set<Integer> clearedProgress = new LinkedHashSet<Integer>();
    private long lastUnknownRoomAlertAt = Long.MIN_VALUE;
    private long lastAlertAt = Long.MIN_VALUE;

    /** Clears one dungeon run's de-duplication state. */
    public void reset() {
        clearedRooms.clear();
        clearedProgress.clear();
        lastUnknownRoomAlertAt = Long.MIN_VALUE;
        lastAlertAt = Long.MIN_VALUE;
    }

    /**
     * Tests a received chat line. A false result is deliberately the safe default for an ambiguous context, named
     * special room, repeated/"already" clear, or text that is not a room/progress confirmation.
     */
    public boolean shouldAlert(boolean inDungeon, String rawMessage, String roomKey, long nowMillis) {
        if (!inDungeon) return false;
        String message = DungeonRunReadout.normalize(rawMessage).toLowerCase(Locale.ROOT);
        if (message.length() == 0 || isExcluded(message) || !CLEAR_MESSAGE.matcher(message).find()) return false;
        return acceptRoom(roomKey, nowMillis);
    }

    /**
     * Tests a visible sidebar progression. The first seen percent is only a baseline. Later increases are accepted
     * only for a plausible normal-room-sized jump; this intentionally rejects stale/replayed/invalid values and
     * cannot be triggered by merely entering Blood, Fairy, Entrance, or a final dungeon-clear message.
     */
    public boolean shouldAlertClearedProgress(boolean inDungeon, int previousPercent, int currentPercent,
                                              String roomKey, long nowMillis) {
        if (!inDungeon || previousPercent < 0 || currentPercent < 0 || previousPercent >= 100
                || currentPercent > 100 || currentPercent <= previousPercent
                || currentPercent - previousPercent > MAX_PROGRESS_JUMP) return false;
        if (clearedProgress.contains(Integer.valueOf(currentPercent))) return false;
        if (!acceptRoom(roomKey, nowMillis)) return false;
        clearedProgress.add(Integer.valueOf(currentPercent));
        while (clearedProgress.size() > MAX_REMEMBERED_ROOMS) {
            Iterator<Integer> iterator = clearedProgress.iterator();
            if (!iterator.hasNext()) break;
            iterator.next();
            iterator.remove();
        }
        return true;
    }

    private boolean acceptRoom(String roomKey, long nowMillis) {
        // A chat and sidebar update often arrive in adjacent client ticks. They describe one clear even if movement
        // made the coarse cell key change between the two observations.
        if (lastAlertAt != Long.MIN_VALUE && nowMillis - lastAlertAt < CROSS_SOURCE_DEDUPLICATION_MS) return false;
        String key = roomKey == null ? "" : roomKey.trim();
        if (key.length() == 0) {
            if (lastUnknownRoomAlertAt != Long.MIN_VALUE
                    && nowMillis - lastUnknownRoomAlertAt < UNKNOWN_ROOM_DEDUPLICATION_MS) return false;
            lastUnknownRoomAlertAt = nowMillis;
            lastAlertAt = nowMillis;
            return true;
        }
        if (clearedRooms.contains(key)) return false;
        clearedRooms.add(key);
        lastAlertAt = nowMillis;
        while (clearedRooms.size() > MAX_REMEMBERED_ROOMS) {
            Iterator<String> iterator = clearedRooms.iterator();
            if (!iterator.hasNext()) break;
            iterator.next();
            iterator.remove();
        }
        return true;
    }

    private static boolean isExcluded(String message) {
        return message.indexOf("blood") >= 0 || message.indexOf("fairy") >= 0 || message.indexOf("entrance") >= 0
                || message.indexOf("already") >= 0 || message.indexOf("boss") >= 0
                || message.indexOf("dungeon cleared") >= 0 || message.indexOf("cleared the dungeon") >= 0;
    }
}
