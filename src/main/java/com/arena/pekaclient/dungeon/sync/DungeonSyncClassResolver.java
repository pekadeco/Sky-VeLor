package com.arena.pekaclient.dungeon.sync;

import com.arena.pekaclient.feature.LeapClass;
import com.arena.pekaclient.feature.LeapText;

import java.util.Collection;
import java.util.List;

/** Resolves a dungeon class using the same owner-bound text rules as the Spirit Leap selector. */
public final class DungeonSyncClassResolver {
    private DungeonSyncClassResolver() { }

    /** Returns the row that carries this exact player's badge, or null when none is recognized. */
    public static String findScoreboardRow(List<String> rows, String playerName, Collection<String> knownNames) {
        if (rows == null || playerName == null) return null;
        for (String row : rows) {
            if (LeapText.classFromScoreboardLine(row, playerName, knownNames) != null) return row;
        }
        return null;
    }

    /** Scoreboard/team rows are authoritative; tab rows are a fallback, as in Spirit Leap. */
    public static String resolve(List<String> scoreboardRows, List<String> tabRows,
                                 String playerName, Collection<String> knownNames) {
        LeapClass detected = fromScoreboardRows(scoreboardRows, playerName, knownNames);
        if (detected == null) detected = fromTabRows(tabRows, playerName, knownNames);
        return detected == null ? "UNKNOWN" : detected.label();
    }

    private static LeapClass fromScoreboardRows(List<String> rows, String playerName,
                                                Collection<String> knownNames) {
        if (rows == null || playerName == null) return null;
        for (String row : rows) {
            LeapClass detected = LeapText.classFromScoreboardLine(row, playerName, knownNames);
            if (detected != null) return detected;
        }
        return null;
    }

    private static LeapClass fromTabRows(List<String> rows, String playerName,
                                         Collection<String> knownNames) {
        if (rows == null || playerName == null) return null;
        for (String row : rows) {
            // Spirit Leap first accepts a named class such as "(Mage XXX)" in the tab entry,
            // then checks the same square-bracket marker syntax used by the scoreboard.
            LeapClass fromTabClass = LeapClass.parse(LeapText.classFromTab(row, playerName));
            if (fromTabClass != null) return fromTabClass;
            LeapClass fromBadge = LeapText.classFromScoreboardLine(row, playerName, knownNames);
            if (fromBadge != null) return fromBadge;
        }
        return null;
    }
}
