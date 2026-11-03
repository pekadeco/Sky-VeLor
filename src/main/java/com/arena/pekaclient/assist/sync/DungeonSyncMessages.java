package com.arena.pekaclient.assist.sync;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Validates inbound worker JSON before it reaches the Minecraft/render thread. */
public final class DungeonSyncMessages {
    private static final int MAX_MESSAGE_CHARS = 32768;
    private static final int MAX_ROOMS = 64;

    private DungeonSyncMessages() {
    }

    public static List<Puzzle> welcomeRooms(String raw) {
        JsonObject root = object(raw);
        if (root == null || !"welcome".equals(string(root, "type")) || !root.has("rooms")
                || !root.get("rooms").isJsonArray()) return Collections.emptyList();
        JsonArray array = root.getAsJsonArray("rooms");
        if (array.size() > MAX_ROOMS) return Collections.emptyList();
        ArrayList<Puzzle> rooms = new ArrayList<Puzzle>(array.size());
        for (JsonElement value : array) {
            if (value == null || !value.isJsonObject()) return Collections.emptyList();
            Puzzle puzzle = puzzle(value.getAsJsonObject());
            if (puzzle == null) return Collections.emptyList();
            rooms.add(puzzle);
        }
        return rooms;
    }

    public static Puzzle puzzleEvent(String raw) {
        JsonObject root = object(raw);
        return root == null || !"puzzle_discovered".equals(string(root, "type")) ? null : puzzle(root);
    }

    private static JsonObject object(String raw) {
        if (raw == null || raw.length() == 0 || raw.length() > MAX_MESSAGE_CHARS) return null;
        try {
            JsonElement parsed = new JsonParser().parse(raw);
            return parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static Puzzle puzzle(JsonObject value) {
        try {
            String name = DungeonSyncProtocol.canonicalPuzzleName(string(value, "name"));
            if (name.length() == 0) return null;
            int x = value.get("x").getAsInt();
            int y = value.get("y").getAsInt();
            int z = value.get("z").getAsInt();
            double confidence = value.get("confidence").getAsDouble();
            if (Math.abs((long) x) > 30000000L || Math.abs((long) z) > 30000000L || y < 1 || y > 250
                    || Math.floorMod(x, 32) != 0 || Math.floorMod(z, 32) != 0 || Double.isNaN(confidence)
                    || Double.isInfinite(confidence) || confidence < .65D || confidence > 1D) return null;
            return new Puzzle(name, x, y, z, confidence);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String string(JsonObject object, String key) {
        try {
            return object != null && object.has(key) ? object.get(key).getAsString() : "";
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    public static final class Puzzle {
        public final String name;
        public final int x;
        public final int y;
        public final int z;
        public final double confidence;

        Puzzle(String name, int x, int y, int z, double confidence) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.confidence = confidence;
        }
    }
}
