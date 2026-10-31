package com.arena.pekaclient.dungeon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Guards the embedded puzzle-room data and the expected ~2 second matcher cadence. */
public final class PuzzleRoomDataTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        InputStream stream = PuzzleRoomDataTests.class.getResourceAsStream("/assets/lunaria/dungeons/rooms.json");
        check("Lunaria rooms.json is bundled", stream != null);
        if (stream == null) { finish(); return; }
        JsonObject root;
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            root = new JsonParser().parse(reader).getAsJsonObject();
        }
        check("schema version 4", root.get("schemaVersion").getAsInt() == 4);
        JsonArray rooms = root.getAsJsonArray("rooms");
        check("seven puzzle room templates", rooms != null && rooms.size() == 7);
        Set<Integer> expected = new HashSet<Integer>(Arrays.asList(0, 90, 180, 270));
        int variants = 0;
        int baseBlocks = 0;
        int rotatedBlocks = 0;
        Set<String> names = new HashSet<String>();
        for (JsonElement roomElement : rooms) {
            JsonObject room = roomElement.getAsJsonObject();
            check("record is a puzzle", "PUZZLE".equalsIgnoreCase(room.get("roomType").getAsString()));
            check("room name unique", names.add(room.get("name").getAsString()));
            JsonArray blocks = room.getAsJsonArray("blocks");
            if ("Boulder".equalsIgnoreCase(room.get("name").getAsString())) {
                check("Boulder uses the new 33-point capture", blocks.size() == 33);
                check("Boulder capture has its original-coordinate metadata", room.has("origin")
                        && room.getAsJsonArray("origin").size() == 3);
            }
            baseBlocks += blocks.size();
            JsonArray variantsJson = room.getAsJsonArray("variants");
            check("four rotations per room", variantsJson != null && variantsJson.size() == 4);
            Set<Integer> rotations = new HashSet<Integer>();
            for (JsonElement variantElement : variantsJson) {
                JsonObject variant = variantElement.getAsJsonObject();
                variants++;
                rotations.add(variant.get("rotationY").getAsInt());
                rotatedBlocks += variant.getAsJsonArray("blocks").size();
                check("rotation has non-empty block template", variant.getAsJsonArray("blocks").size() > 0);
                if ("Boulder".equalsIgnoreCase(room.get("name").getAsString())) {
                    check("Boulder rotation has 33 samples", variant.getAsJsonArray("blocks").size() == 33);
                }
            }
            check("all rotations present", rotations.equals(expected));
        }
        check("28 rotations", variants == 28);
        check("215 base block samples after Boulder replacement", baseBlocks == 215);
        check("860 rotated block samples after Boulder replacement", rotatedBlocks == 860);
        check("re-check cadence is 40 client ticks", PuzzleRoomMatcher.CHECK_INTERVAL_TICKS == 40);
        finish();
    }

    private static void check(String name, boolean result) {
        if (result) passed++; else { failed++; System.out.println("FAIL " + name); }
    }
    private static void finish() {
        System.out.println("puzzle room data: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
