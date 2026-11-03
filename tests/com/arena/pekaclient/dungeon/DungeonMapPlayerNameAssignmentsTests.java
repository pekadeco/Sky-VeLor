package com.arena.pekaclient.dungeon;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DungeonMapPlayerNameAssignmentsTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        List<String> markers = Arrays.asList("icon-0", "icon-1", "icon-2", "icon-3", "icon-4");
        List<String> roster = Arrays.asList("LocalPlayer", "NearPlayer", "FarPlayer", "FartherPlayer", "LastPlayer");
        Map<String, String> partial = new HashMap<String, String>();
        partial.put("icon-0", "LocalPlayer");
        partial.put("icon-2", "NearPlayer");
        Map<String, String> completed = DungeonMapPlayerNameAssignments.fillUnassigned(markers, roster, partial);
        check("unloaded/far players receive names even with partial live matches", "FarPlayer".equals(completed.get("icon-1"))
                || "FarPlayer".equals(completed.get("icon-3")) || "FarPlayer".equals(completed.get("icon-4")));
        check("all five markers receive a name when five roster members exist", completed.size() == 5);
        check("existing local assignment stays on its marker", "LocalPlayer".equals(completed.get("icon-0")));
        check("existing nearby assignment stays on its marker", "NearPlayer".equals(completed.get("icon-2")));
        Map<String, String> afterMarkerRefresh = DungeonMapPlayerNameAssignments.fillUnassigned(
                Arrays.asList("icon-4", "icon-3", "icon-2", "icon-1", "icon-0"), roster, completed);
        check("changing marker iteration order does not change key-to-name assignments",
                completed.equals(afterMarkerRefresh));

        Map<String, String> mismatched = DungeonMapPlayerNameAssignments.fillUnassigned(
                Arrays.asList("icon-0", "icon-1", "icon-2"), roster, Collections.<String, String>emptyMap());
        check("mismatched marker and roster counts do not receive guessed labels", mismatched.isEmpty());
        Map<String, String> partialMismatch = DungeonMapPlayerNameAssignments.fillUnassigned(
                Arrays.asList("icon-0", "icon-1", "icon-2"), roster, Collections.singletonMap("icon-0", "LocalPlayer"));
        check("confirmed assignment is kept even if the remaining counts differ",
                partialMismatch.size() == 1 && "LocalPlayer".equals(partialMismatch.get("icon-0")));
        Map<String, String> equalRemaining = DungeonMapPlayerNameAssignments.fillUnassigned(
                Arrays.asList("icon-0", "icon-1", "icon-2"), Arrays.asList("LocalPlayer", "FarPlayer", "NearPlayer"),
                Collections.singletonMap("icon-0", "LocalPlayer"));
        check("remaining labels use roster order only when remaining counts match",
                equalRemaining.size() == 3 && "FarPlayer".equals(equalRemaining.get("icon-1"))
                        && "NearPlayer".equals(equalRemaining.get("icon-2")));

        Map<String, String> duplicates = new HashMap<String, String>();
        duplicates.put("icon-0", "LocalPlayer");
        duplicates.put("icon-1", "localplayer");
        Map<String, String> unique = DungeonMapPlayerNameAssignments.fillUnassigned(markers, roster, duplicates);
        check("same username with different capitalization is not assigned twice", countName(unique, "localplayer") == 1);
        check("unknown/stale assignments are not retained", !DungeonMapPlayerNameAssignments.fillUnassigned(markers, roster,
                Collections.singletonMap("icon-0", "DisconnectedPlayer")).containsValue("DisconnectedPlayer"));

        System.out.println("DungeonMapPlayerNameAssignmentsTests: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static int countName(Map<String, String> map, String name) {
        int count = 0;
        for (String value : map.values()) if (name.equalsIgnoreCase(value)) count++;
        return count;
    }

    private static void check(String label, boolean condition) {
        if (condition) passed++;
        else { failed++; System.out.println("FAIL: " + label); }
    }
}
