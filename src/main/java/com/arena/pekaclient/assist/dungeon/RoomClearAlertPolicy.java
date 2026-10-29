package com.arena.pekaclient.assist.dungeon;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Conservative chat-only room-clear gate. It accepts a narrow positive message and remembers cleared room cells so
 * the alert cannot retrigger when a player revisits a room.
 */
public final class RoomClearAlertPolicy {
    private static final long UNKNOWN_ROOM_DEDUPLICATION_MS = 15000L;
    private static final int MAX_REMEMBERED_ROOMS = 64;
    private static final Pattern CLEAR_MESSAGE = Pattern.compile(
            "(?i)\\b(?:(?:you\\s+)?(?:have\\s+)?(?:successfully\\s+)?)cleared\\s+(?:the|this)\\s+room!?(?=\\s|$)"
                    + "|\\broom\\s+cleared!?(?=\\s|$)");

    private final Set<String> clearedRooms = new LinkedHashSet<String>();
    private long lastUnknownRoomAlertAt = Long.MIN_VALUE;

    /** Clears one dungeon run's deduplication state. */
    public void reset() {
        clearedRooms.clear();
        lastUnknownRoomAlertAt = Long.MIN_VALUE;
    }

    /**
     * Tests a received chat line. A false result is deliberately the safe default for an ambiguous context, named
     * special room, repeated/"already" clear, or any text that is not the narrow room-clear confirmation.
     */
    public boolean shouldAlert(boolean inDungeon, String rawMessage, String roomKey, long nowMillis) {
        if (!inDungeon) return false;
        String message = DungeonRunReadout.normalize(rawMessage).toLowerCase(Locale.ROOT);
        if (message.length() == 0 || isExcluded(message) || !CLEAR_MESSAGE.matcher(message).find()) return false;

        String key = roomKey == null ? "" : roomKey.trim();
        if (key.length() == 0) {
            if (lastUnknownRoomAlertAt != Long.MIN_VALUE
                    && nowMillis - lastUnknownRoomAlertAt < UNKNOWN_ROOM_DEDUPLICATION_MS) return false;
            lastUnknownRoomAlertAt = nowMillis;
            return true;
        }
        if (clearedRooms.contains(key)) return false;
        clearedRooms.add(key);
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
