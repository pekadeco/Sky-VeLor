package com.arena.pekaclient.dungeon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.block.Block;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Loads the user-provided, four-rotation puzzle templates bundled with Sky VeLor. */
public final class PuzzleRoomDatabase {
    private static final Logger LOG = LogManager.getLogger("SkyVeLor");
    private static final String RESOURCE = "/assets/pekaclient/dungeons/rooms.json";
    private static volatile List<Room> rooms = Collections.emptyList();
    private static volatile boolean loaded;

    private PuzzleRoomDatabase() { }

    public static synchronized void load() {
        List<Room> parsed = new ArrayList<Room>();
        try (InputStream stream = PuzzleRoomDatabase.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IllegalStateException("Missing " + RESOURCE);
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
                if (readInt(root, "schemaVersion", -1) != 4) {
                    throw new IllegalStateException("Unsupported room data schema");
                }
                JsonArray array = root.getAsJsonArray("rooms");
                if (array == null) throw new IllegalStateException("rooms.json has no rooms array");
                for (JsonElement element : array) {
                    if (!element.isJsonObject()) continue;
                    JsonObject object = element.getAsJsonObject();
                    if (!"PUZZLE".equalsIgnoreCase(readString(object, "roomType", ""))) continue;
                    List<Variant> variants = new ArrayList<Variant>();
                    JsonArray variantsJson = object.getAsJsonArray("variants");
                    if (variantsJson != null) {
                        for (JsonElement variantElement : variantsJson) {
                            if (!variantElement.isJsonObject()) continue;
                            JsonObject variantObject = variantElement.getAsJsonObject();
                            JsonArray blocksJson = variantObject.getAsJsonArray("blocks");
                            if (blocksJson == null) continue;
                            List<BlockSample> blocks = parseBlocks(blocksJson);
                            if (!blocks.isEmpty()) {
                                variants.add(new Variant(readInt(variantObject, "rotationY", 0),
                                        readInt(variantObject, "sizeX", 31),
                                        readInt(variantObject, "sizeY", 40),
                                        readInt(variantObject, "sizeZ", 31), blocks));
                            }
                        }
                    }
                    if (!variants.isEmpty()) parsed.add(new Room(readString(object, "name", "Puzzle"), variants));
                }
            }
            if (parsed.isEmpty()) throw new IllegalStateException("No puzzle templates were loaded");
            chooseProbes(parsed);
            rooms = Collections.unmodifiableList(parsed);
            loaded = true;
            int variantCount = 0;
            for (Room room : parsed) variantCount += room.variants.size();
            LOG.info("Loaded {} puzzle rooms ({} variants)", parsed.size(), variantCount);
        } catch (Exception error) {
            rooms = Collections.emptyList();
            loaded = false;
            LOG.warn("Could not load the bundled puzzle room database", error);
        }
    }

    private static List<BlockSample> parseBlocks(JsonArray array) {
        List<BlockSample> result = new ArrayList<BlockSample>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            JsonObject object = element.getAsJsonObject();
            String blockName = readString(object, "block", "");
            Block block = Block.getBlockFromName(blockName);
            if (block == null) continue;
            result.add(new BlockSample(readInt(object, "x", 0), readInt(object, "y", 0),
                    readInt(object, "z", 0), readInt(object, "meta", -1), blockName, block));
        }
        return result;
    }

    private static void chooseProbes(List<Room> roomList) {
        final Map<String, Integer> frequencies = new HashMap<String, Integer>();
        for (Room room : roomList) for (Variant variant : room.variants) for (BlockSample block : variant.blocks) {
            Integer count = frequencies.get(block.signature());
            frequencies.put(block.signature(), count == null ? 1 : count + 1);
        }
        for (Room room : roomList) for (Variant variant : room.variants) {
            List<BlockSample> candidates = new ArrayList<BlockSample>(variant.blocks);
            Collections.sort(candidates, new Comparator<BlockSample>() {
                @Override public int compare(BlockSample a, BlockSample b) {
                    int fa = frequencies.containsKey(a.signature()) ? frequencies.get(a.signature()) : Integer.MAX_VALUE;
                    int fb = frequencies.containsKey(b.signature()) ? frequencies.get(b.signature()) : Integer.MAX_VALUE;
                    if (fa != fb) return fa < fb ? -1 : 1;
                    int signatureOrder = a.signature().compareTo(b.signature());
                    if (signatureOrder != 0) return signatureOrder;
                    if (a.x != b.x) return a.x < b.x ? -1 : 1;
                    if (a.y != b.y) return a.y < b.y ? -1 : 1;
                    if (a.z != b.z) return a.z < b.z ? -1 : 1;
                    return 0;
                }
            });
            List<BlockSample> probes = new ArrayList<BlockSample>(4);
            while (!candidates.isEmpty() && probes.size() < 4) {
                Set<String> usedSignatures = new HashSet<String>();
                for (BlockSample chosen : probes) usedSignatures.add(chosen.signature());
                boolean unusedSignatureAvailable = false;
                if (!probes.isEmpty()) {
                    for (BlockSample candidate : candidates) {
                        if (!usedSignatures.contains(candidate.signature())) {
                            unusedSignatureAvailable = true;
                            break;
                        }
                    }
                }

                BlockSample best = null;
                int bestDistance = Integer.MIN_VALUE;
                int bestFrequency = Integer.MAX_VALUE;
                for (BlockSample candidate : candidates) {
                    if (unusedSignatureAvailable && usedSignatures.contains(candidate.signature())) continue;
                    int frequency = frequencies.containsKey(candidate.signature())
                            ? frequencies.get(candidate.signature()) : Integer.MAX_VALUE;
                    int nearest = 0;
                    if (!probes.isEmpty()) {
                        nearest = Integer.MAX_VALUE;
                        for (BlockSample chosen : probes) {
                            nearest = Math.min(nearest, Math.abs(candidate.x - chosen.x)
                                    + Math.abs(candidate.y - chosen.y) + Math.abs(candidate.z - chosen.z));
                        }
                    }
                    boolean better;
                    if (probes.isEmpty()) {
                        better = best == null || frequency < bestFrequency;
                    } else {
                        better = best == null || nearest > bestDistance
                                || (nearest == bestDistance && frequency < bestFrequency);
                    }
                    if (better) {
                        best = candidate;
                        bestDistance = nearest;
                        bestFrequency = frequency;
                    }
                }
                if (best == null) break;
                probes.add(best);
                candidates.remove(best);
            }
            variant.probes = Collections.unmodifiableList(probes);
        }
    }

    private static int readInt(JsonObject object, String key, int fallback) {
        try { return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsInt() : fallback; }
        catch (RuntimeException ignored) { return fallback; }
    }
    private static String readString(JsonObject object, String key, String fallback) {
        try { return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : fallback; }
        catch (RuntimeException ignored) { return fallback; }
    }

    public static List<Room> rooms() { return rooms; }
    public static boolean isLoaded() { return loaded; }

    public static final class Room {
        public final String name;
        public final List<Variant> variants;
        private Room(String name, List<Variant> variants) {
            this.name = name;
            this.variants = Collections.unmodifiableList(new ArrayList<Variant>(variants));
        }
    }
    public static final class Variant {
        public final int rotationY, sizeX, sizeY, sizeZ;
        public final List<BlockSample> blocks;
        private List<BlockSample> probes = Collections.emptyList();
        private Variant(int rotationY, int sizeX, int sizeY, int sizeZ, List<BlockSample> blocks) {
            this.rotationY = rotationY; this.sizeX = sizeX; this.sizeY = sizeY; this.sizeZ = sizeZ;
            this.blocks = Collections.unmodifiableList(new ArrayList<BlockSample>(blocks));
        }
        public List<BlockSample> probes() { return probes; }
    }
    public static final class BlockSample {
        public final int x, y, z, meta;
        public final String blockName;
        public final Block block;
        private BlockSample(int x, int y, int z, int meta, String blockName, Block block) {
            this.x=x; this.y=y; this.z=z; this.meta=meta; this.blockName=blockName; this.block=block;
        }
        private String signature() { return blockName + "#" + meta; }
    }
}
