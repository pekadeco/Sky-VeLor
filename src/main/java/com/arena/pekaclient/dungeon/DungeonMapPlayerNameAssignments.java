package com.arena.pekaclient.dungeon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Name assignment utilities for player markers on the dungeon map. */
final class DungeonMapPlayerNameAssignments {
    private DungeonMapPlayerNameAssignments() { }

    static Map<String, String> fillUnassigned(List<String> markerKeys, List<String> orderedNames,
                                               Map<String, String> preferredAssignments) {
        if (markerKeys == null || markerKeys.isEmpty() || orderedNames == null || orderedNames.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<String> availableNames = new HashSet<String>();
        for (String name : orderedNames) {
            if (name != null && !name.trim().isEmpty()) availableNames.add(normalize(name));
        }

        Map<String, String> result = new LinkedHashMap<String, String>();
        Set<String> usedNames = new HashSet<String>();
        if (preferredAssignments != null) {
            for (String key : markerKeys) {
                String name = preferredAssignments.get(key);
                if (name == null || !availableNames.contains(normalize(name))) continue;
                if (usedNames.add(normalize(name))) result.put(key, name);
            }
        }

        List<String> remainingKeys = new ArrayList<String>();
        for (String key : markerKeys) {
            if (!result.containsKey(key)) remainingKeys.add(key);
        }
        List<String> remainingNames = new ArrayList<String>();
        Set<String> seenNames = new HashSet<String>();
        for (String name : orderedNames) {
            if (name == null || name.trim().isEmpty()) continue;
            String normalized = normalize(name);
            if (!usedNames.contains(normalized) && seenNames.add(normalized)) remainingNames.add(name);
        }

        // Use roster order only as a last resort when the remaining lists correspond one-to-one.
        // If counts differ, an unlabelled marker may be an NPC/map decoration: leave it unlabeled rather than lie.
        if (remainingKeys.size() != remainingNames.size()) return result;
        for (int i = 0; i < remainingKeys.size(); i++) {
            result.put(remainingKeys.get(i), remainingNames.get(i));
        }
        return result;
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
