package com.arena.pekaclient.dungeon.sync;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.arena.pekaclient.dungeon.DungeonPuzzleSyncEnvironment;
import com.arena.pekaclient.dungeon.PuzzleRoomMatcher;
import com.arena.pekaclient.dungeon.PuzzleRoomMemory;
import com.arena.pekaclient.feature.DungeonContext;
import com.arena.pekaclient.feature.SkyblockContext;
import net.minecraft.client.Minecraft;
import net.minecraft.util.StringUtils;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/** Owns one temporary puzzle-sync session. Player identity fields are used locally and never serialized. */
public final class DungeonPuzzleSyncManager {
    private static final long SESSION_START_TIMEOUT_MS = 15000L;
    private static final long WORLD_CHANGE_GRACE_MS = 1800L;
    private static final long WORLD_CHANGE_MAX_WAIT_MS = 8000L;
    private static final long SERVER_ID_CHECK_INTERVAL_MS = 2500L;
    private static final long SERVER_ID_MISSING_TIMEOUT_MS = 8000L;
    private static final int SAMPLE_INTERVAL_TICKS = 10;
    private static final int REQUIRED_STABLE_SAMPLES = 3;
    private static final Set<String> PUZZLES = new HashSet<String>();
    private static final SecureRandom RANDOM = new SecureRandom();

    static {
        String[] names = {"silver fish", "warping", "3 wierdos", "creaper beams", "boulder", "water", "tic tac toe"};
        for (String name : names) PUZZLES.add(name);
    }

    private final ConcurrentLinkedQueue<SharedPuzzle> incoming = new ConcurrentLinkedQueue<SharedPuzzle>();
    private final ConcurrentLinkedQueue<DiagnosticNotice> networkNotices = new ConcurrentLinkedQueue<DiagnosticNotice>();
    private final AtomicBoolean workerWelcomeSignal = new AtomicBoolean(false);
    private final Map<String, Double> workerKnownPuzzles = new ConcurrentHashMap<String, Double>();
    private final Map<String, Double> sentPuzzles = new HashMap<String, Double>();
    private final Map<String, Long> debugThrottle = new HashMap<String, Long>();
    private final Set<String> localPuzzleTraceKeys = new HashSet<String>();
    private final Set<String> renderedPuzzleTraceKeys = new HashSet<String>();
    private final Set<String> mapAnchorTraceKeys = new HashSet<String>();
    private String lastCaptureDebug = "";
    private long lastNoPuzzleMatchDebugAt;
    private boolean noLocalMatchReported;
    private boolean countdownTracePending;
    private DungeonPuzzleSyncConfig config;
    private boolean pendingStart;
    private long pendingStartAt;
    private String pendingSkipReason = "waiting for the dungeon scoreboard";
    private int ticksUntilSample;
    private int stableSamples;
    private DungeonPuzzleSyncEnvironment.Snapshot lastSnapshot;
    private String activeSessionId;
    private String activeServerId;
    private String activeFloor;
    private World sessionWorld;
    private int missingDungeonTicks;
    private long worldChangeStartedAt;
    private boolean replayLocalMemory;
    private DungeonPuzzleSyncClient client;
    private boolean configWarningShown;
    private boolean identityWarningShown;
    private final DungeonSyncReminderPolicy reminderPolicy = new DungeonSyncReminderPolicy();
    private final AtomicBoolean connectionWarningShown = new AtomicBoolean(false);
    private long nextServerIdCheckAt;
    private long serverIdMissingSince;

    /** Starts only on the exact dungeon countdown line, whether delivered as chat or system text. */
    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (!SkyblockContext.isInSkyblock() || event == null || event.message == null) return;
        String text = StringUtils.stripControlCodes(event.message.getUnformattedText());
        if (!DungeonSyncIdentity.isDungeonStartMessage(event.type, text)) return;
        if (!DungeonContext.inDungeon()) reminderPolicy.onDungeonStartCountdown();
        beginPendingSession();
        countdownTracePending = true;
    }

    private void beginPendingSession() {
        closeActiveSession();
        incoming.clear();
        networkNotices.clear();
        pendingStart = true;
        pendingStartAt = System.currentTimeMillis();
        pendingSkipReason = "waiting for the dungeon scoreboard";
        ticksUntilSample = 0;
        stableSamples = 0;
        lastSnapshot = null;
        sentPuzzles.clear();
        localPuzzleTraceKeys.clear();
        renderedPuzzleTraceKeys.clear();
        mapAnchorTraceKeys.clear();
        debugThrottle.clear();
        lastCaptureDebug = "";
        lastNoPuzzleMatchDebugAt = 0L;
        noLocalMatchReported = false;
    }

    public void tick(Minecraft mc) {
        if (mc == null) {
            verifyExitAfterMissingWorld(null);
            return;
        }

        DungeonPuzzleSyncConfig latestConfig = DungeonPuzzleSyncConfig.load(mc);
        boolean wasUsable = config != null && config.isUsable();
        config = latestConfig;
        processNetworkNotices(mc);
        boolean hasPlayerWorldForReminder = mc.thePlayer != null && mc.theWorld != null;
        boolean inDungeonForReminder = hasPlayerWorldForReminder && DungeonContext.inDungeon();
        boolean confirmedOutsideDungeon = hasPlayerWorldForReminder && SkyblockContext.isInSkyblock() && !inDungeonForReminder;
        if (reminderPolicy.shouldShowReminder(inDungeonForReminder, confirmedOutsideDungeon, config.isUsable())) {
            showDisabledSyncReminder(mc);
        }
        if (countdownTracePending) {
            countdownTracePending = false;
            debug(mc, "countdown", "Dungeon start countdown detected; waiting for stable server, floor and player roster data");
        }
        boolean isUsable = config.isUsable();
        if (wasUsable && !isUsable) {
            // The player disabled sync in the in-game settings. Close the socket promptly and
            // discard pending local sync work; local puzzle detection/map drawing remain intact.
            closeActiveSession();
            resetPendingStart();
            incoming.clear();
            configWarningShown = false;
        } else if (!wasUsable && isUsable) {
            configWarningShown = false;
            identityWarningShown = false;
            // If enabled midway through a run, don't make the player wait for the start chat again.
            if (mc.thePlayer != null && mc.theWorld != null && DungeonContext.inDungeon()) {
                beginPendingSession();
            }
        }

        if (mc.thePlayer == null || mc.theWorld == null) {
            verifyExitAfterMissingWorld(mc);
            return;
        }
        verifyActiveSession(mc);
        if (!pendingStart || activeSessionId != null) return;
        if (!config.isUsable()) {
            if (!configWarningShown) {
                configWarningShown = true;
                if (diagnosticsEnabled()) debug(mc, "sync_disabled", "Dungeon Sync is disabled or the secure endpoint is invalid; local puzzle detection and map rendering remain active");
            }
            pendingStart = false;
            return;
        }
        if (System.currentTimeMillis() - pendingStartAt > SESSION_START_TIMEOUT_MS) {
            if (!identityWarningShown) {
                identityWarningShown = true;
                if (diagnosticsEnabled()) debug(mc, "session_setup_failed", pendingSkipReason);
                else System.out.println("[Lunaria] Puzzle sync skipped: " + pendingSkipReason + ".");
            }
            resetPendingStart();
            return;
        }
        if (++ticksUntilSample < SAMPLE_INTERVAL_TICKS) return;
        ticksUntilSample = 0;
        if (!DungeonContext.inDungeon()) {
            pendingSkipReason = "the local scoreboard is not recognized as a dungeon";
            logCaptureState(mc, pendingSkipReason);
            stableSamples = 0;
            lastSnapshot = null;
            return;
        }
        DungeonPuzzleSyncEnvironment.Snapshot snapshot = DungeonPuzzleSyncEnvironment.capture(mc);
        if (snapshot == null) {
            pendingSkipReason = DungeonPuzzleSyncEnvironment.getLastCaptureFailure();
            logCaptureState(mc, pendingSkipReason);
            stableSamples = 0;
            lastSnapshot = null;
            return;
        }
        logCaptureState(mc, "snapshot ready: floor=" + snapshot.floor + ", party=" + snapshot.partySize
                + ", declaredPartySize=" + snapshot.declaredPartySize + ", rosterSource=" + snapshot.rosterSource
                + ", tabProfiles=" + snapshot.tabProfileCount + ", validProfiles=" + snapshot.validProfileCount
                + ", visiblePlayerRows=" + snapshot.visiblePlayerCount + ", classMarked=" + snapshot.classMarkedCount);
        if (lastSnapshot != null && lastSnapshot.sameAs(snapshot)) stableSamples++;
        else {
            stableSamples = 1;
            pendingSkipReason = "the tab-list server ID, scoreboard floor, or exact player-name roster is still changing";
        }
        lastSnapshot = snapshot;
        if (stableSamples >= REQUIRED_STABLE_SAMPLES) establishSession(mc, snapshot);
    }

    private void establishSession(Minecraft mc, DungeonPuzzleSyncEnvironment.Snapshot snapshot) {
        pendingStart = false;
        activeSessionId = snapshot.sessionId;
        activeServerId = snapshot.serverId;
        activeFloor = snapshot.floor;
        sessionWorld = mc.theWorld;
        missingDungeonTicks = 0;
        worldChangeStartedAt = 0L;
        sentPuzzles.clear();
        workerKnownPuzzles.clear();
        workerWelcomeSignal.set(false);
        connectionWarningShown.set(false);
        nextServerIdCheckAt = System.currentTimeMillis() + SERVER_ID_CHECK_INTERVAL_MS;
        serverIdMissingSince = 0L;
        String memberId = randomHex(16);
        debug(mc, "session_ready", "Local session fingerprint prepared for " + snapshot.floor
                + " on " + snapshot.serverId + "; selected party size=" + snapshot.partySize
                + ". Raw usernames and the session hash are not printed or uploaded as names.");
        try {
            client = new DungeonPuzzleSyncClient(config.endpoint, activeSessionId, memberId,
                    new DungeonPuzzleSyncClient.Listener() {
                        @Override public void onConnected() {
                            connectionWarningShown.set(false);
                            networkNotices.offer(new DiagnosticNotice("websocket_open", "TLS/WebSocket handshake completed; waiting for the Worker welcome response"));
                        }
                        @Override public void onMessage(String message) { receiveMessage(message); }
                        @Override public void onFailure(Exception failure) {
                            if (connectionWarningShown.compareAndSet(false, true)) {
                                networkNotices.offer(new DiagnosticNotice("connection_failure", safeFailure(failure)));
                            }
                        }
                    });
            client.connectAsync();
            replayLocalMemory = false;
            debug(mc, "connection_start", "Connecting to the configured secure sync endpoint; the URL and session hash are hidden from diagnostics");
        } catch (Exception failure) {
            closeActiveSession();
            debug(mc, "connection_start_failed", safeFailure(failure));
            if (!diagnosticsEnabled()) System.out.println("[Lunaria] Puzzle sync could not start; local map remains active.");
        }
    }

    private void verifyActiveSession(Minecraft mc) {
        if (activeSessionId == null) return;
        if (client != null && workerWelcomeSignal.getAndSet(false)) {
            // Wait for the welcome snapshot before replaying. This lets the client skip puzzle rooms
            // the Worker already has, avoiding duplicate WebSocket messages/storage reads on reconnect.
            sentPuzzles.clear();
            replayLocalMemory = true;
        }

        long now = System.currentTimeMillis();
        if (mc.theWorld != sessionWorld) {
            if (worldChangeStartedAt == 0L) worldChangeStartedAt = now;
            long elapsed = now - worldChangeStartedAt;
            if (elapsed >= WORLD_CHANGE_GRACE_MS && now >= nextServerIdCheckAt) {
                nextServerIdCheckAt = now + 1000L;
                String currentServerId = DungeonPuzzleSyncEnvironment.currentServerId(mc);
                String currentFloor = DungeonPuzzleSyncEnvironment.currentFloor(mc);
                if (DungeonContext.inDungeon() && activeServerId.equals(currentServerId)
                        && activeFloor.equals(currentFloor)) {
                    sessionWorld = mc.theWorld;
                    worldChangeStartedAt = 0L;
                    missingDungeonTicks = 0;
                    serverIdMissingSince = 0L;
                    nextServerIdCheckAt = now + SERVER_ID_CHECK_INTERVAL_MS;
                } else if ((currentServerId != null && !activeServerId.equals(currentServerId))
                        || (currentFloor != null && !activeFloor.equals(currentFloor))) {
                    closeActiveSession();
                } else if (elapsed >= WORLD_CHANGE_MAX_WAIT_MS) {
                    // The world/scoreboard may take a few seconds to load. Only remove the member
                    // after the transition remains unverifiable for the full timeout.
                    closeActiveSession();
                }
            }
            return;
        }

        if (!DungeonContext.inDungeon()) {
            if (++missingDungeonTicks >= 40) closeActiveSession();
            return;
        }
        missingDungeonTicks = 0;

        // WorldEvent.Unload can be raised slightly before a world reference changes. If the same
        // world is still active, verify it instead of leaving the transition flag stuck forever.
        if (worldChangeStartedAt != 0L) {
            long elapsed = now - worldChangeStartedAt;
            if (elapsed < WORLD_CHANGE_GRACE_MS || now < nextServerIdCheckAt) return;
            nextServerIdCheckAt = now + 1000L;
            String currentServerId = DungeonPuzzleSyncEnvironment.currentServerId(mc);
            String currentFloor = DungeonPuzzleSyncEnvironment.currentFloor(mc);
            if (activeServerId.equals(currentServerId) && activeFloor.equals(currentFloor)) {
                worldChangeStartedAt = 0L;
                serverIdMissingSince = 0L;
                nextServerIdCheckAt = now + SERVER_ID_CHECK_INTERVAL_MS;
            } else if (currentServerId != null) {
                closeActiveSession();
            } else if (elapsed >= WORLD_CHANGE_MAX_WAIT_MS) {
                closeActiveSession();
            }
            return;
        }

        // Avoid scanning the tab list every client tick; the server instance rarely changes while
        // the player stays in one world, so check at a low fixed interval instead.
        if (now >= nextServerIdCheckAt) {
            nextServerIdCheckAt = now + SERVER_ID_CHECK_INTERVAL_MS;
            String currentServerId = DungeonPuzzleSyncEnvironment.currentServerId(mc);
            String currentFloor = DungeonPuzzleSyncEnvironment.currentFloor(mc);
            if (currentServerId != null && currentFloor != null) {
                serverIdMissingSince = 0L;
                if (!activeServerId.equals(currentServerId) || !activeFloor.equals(currentFloor)) {
                    closeActiveSession();
                }
            } else if (serverIdMissingSince == 0L) {
                serverIdMissingSince = now;
            } else if (now - serverIdMissingSince >= SERVER_ID_MISSING_TIMEOUT_MS) {
                closeActiveSession();
            }
        }
    }

    private void verifyExitAfterMissingWorld(Minecraft mc) {
        if (activeSessionId == null) return;
        if (worldChangeStartedAt == 0L) worldChangeStartedAt = System.currentTimeMillis();
        if (System.currentTimeMillis() - worldChangeStartedAt >= WORLD_CHANGE_MAX_WAIT_MS) closeActiveSession();
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (activeSessionId != null && worldChangeStartedAt == 0L) worldChangeStartedAt = System.currentTimeMillis();
    }

    @SubscribeEvent
    public void onClientDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        closeActiveSession();
        resetPendingStart();
        incoming.clear();
        networkNotices.clear();
    }

    public void publishLocalPuzzle(PuzzleRoomMatcher.Match match) {
        if (match == null) return;
        publishPuzzle(match.name, match.originX, match.originY, match.originZ, match.confidence);
    }

    public void publishRememberedPuzzle(PuzzleRoomMemory.RoomLabel room) {
        if (room == null) return;
        publishPuzzle(room.name, room.originX, room.originY, room.originZ, room.confidence);
    }

    public void onLocalPuzzleDetected(PuzzleRoomMatcher.Match match) {
        if (match == null) return;
        noLocalMatchReported = false;
        String key = normalizePuzzle(match.name) + "@" + match.originX + "," + match.originY + "," + match.originZ;
        if (localPuzzleTraceKeys.add(key)) {
            debug(Minecraft.getMinecraft(), "local_puzzle_detected", normalizePuzzle(match.name)
                    + " at room origin " + match.originX + "," + match.originY + "," + match.originZ
                    + " (confidence=" + String.format(Locale.ROOT, "%.2f", match.confidence) + ")");
        }
    }

    public void reportNoLocalPuzzleMatch() {
        if (!diagnosticsEnabled() || !DungeonContext.inDungeon() || noLocalMatchReported) return;
        long now = System.currentTimeMillis();
        if (pendingStartAt == 0L || now - pendingStartAt < 25000L) return;
        noLocalMatchReported = true;
        lastNoPuzzleMatchDebugAt = now;
        debug(Minecraft.getMinecraft(), "local_detection_waiting", "No recognized puzzle room match has been found yet; this is a local room-detection state, not a network failure");
    }

    public void onSharedPuzzleApplied(Minecraft mc, SharedPuzzle shared, boolean wasNewRoom) {
        if (shared == null) return;
        debug(mc, "puzzle_applied_to_memory", shared.name + " at room origin " + shared.x + "," + shared.y + "," + shared.z
                + "; newRoom=" + wasNewRoom + "; confidence=" + String.format(Locale.ROOT, "%.2f", shared.confidence));
        if (shared.notifyPlayer && wasNewRoom && mc != null && mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText("§8[§bLunaria§8] §rA teammate discovered §a"
                    + PuzzleDisplayName.of(shared.name) + "§r. Added it to your dungeon map."));
        }
    }

    public void reportMapState(Minecraft mc, String state, int rememberedRooms) {
        debug(mc, "map_state", state + "; rememberedPuzzleRooms=" + rememberedRooms);
    }

    public void reportMapAnchor(Minecraft mc, int x, int y, int z) {
        String key = x + "," + y + "," + z;
        if (mapAnchorTraceKeys.add(key)) debug(mc, "map_anchor_captured", "room " + key + " was assigned a map-space anchor");
    }

    public void reportMapLabelRendered(Minecraft mc, String name, int x, int y, int z) {
        String key = normalizePuzzle(name) + "@" + x + "," + y + "," + z;
        if (renderedPuzzleTraceKeys.add(key)) debug(mc, "map_label_rendered", name + " at room " + x + "," + y + "," + z);
    }

    public void reportMapLabelClipped(Minecraft mc, String name, int x, int y, int z) {
        String key = "clipped:" + normalizePuzzle(name) + "@" + x + "," + y + "," + z;
        if (renderedPuzzleTraceKeys.add(key)) {
            debug(mc, "map_label_clipped", name + " at room " + x + "," + y + "," + z + " is outside the map text viewport at the current zoom/rotation");
        }
    }

    public void reportMapLabelOverlap(Minecraft mc, String name, int x, int y, int z) {
        String key = "overlap:" + normalizePuzzle(name) + "@" + x + "," + y + "," + z;
        if (renderedPuzzleTraceKeys.add(key)) {
            debug(mc, "map_label_overlap", name + " at room " + x + "," + y + "," + z
                    + " was hidden because no clear label position was available at the current zoom/rotation");
        }
    }

    private void publishPuzzle(String rawName, int x, int y, int z, double confidence) {
        if (config == null || !config.isUsable()) {
            debug(Minecraft.getMinecraft(), "puzzle_upload_skipped", "sync is disabled or the secure endpoint is unavailable; local map memory remains active");
            return;
        }
        if (activeSessionId == null || client == null) {
            debug(Minecraft.getMinecraft(), "puzzle_upload_waiting", normalizePuzzle(rawName) + " at room " + x + "," + y + "," + z
                    + " was detected locally, but no shared session is established yet");
            return;
        }
        if (!isAllowedPuzzle(rawName)) {
            debug(Minecraft.getMinecraft(), "puzzle_upload_rejected_locally", "puzzle name is not in the synchronized allow-list");
            return;
        }
        if (confidence < 0.65D || confidence > 1.0D) {
            debug(Minecraft.getMinecraft(), "puzzle_upload_rejected_locally", "confidence is outside the accepted 0.65–1.00 range");
            return;
        }
        if (Math.abs((long) x) > 30000000L || Math.abs((long) z) > 30000000L
                || y < 1 || y > 250 || Math.floorMod(x, 32) != 0 || Math.floorMod(z, 32) != 0) {
            debug(Minecraft.getMinecraft(), "puzzle_upload_rejected_locally", "room coordinates failed bounds or 32-block alignment validation: " + x + "," + y + "," + z);
            return;
        }
        String name = normalizePuzzle(rawName);
        String key = x + "," + y + "," + z;
        Double workerConfidence = workerKnownPuzzles.get(key);
        if (workerConfidence != null && workerConfidence.doubleValue() >= confidence) return;
        Double sentConfidence = sentPuzzles.get(key);
        if (sentConfidence != null && sentConfidence.doubleValue() >= confidence) return;
        JsonObject message = new JsonObject();
        message.addProperty("type", "puzzle_discovered");
        message.addProperty("name", name);
        message.addProperty("x", x);
        message.addProperty("y", y);
        message.addProperty("z", z);
        message.addProperty("confidence", confidence);
        if (client.send(message)) {
            sentPuzzles.put(key, confidence);
            debug(Minecraft.getMinecraft(), "puzzle_upload_queued", name + " at room " + key + "; awaiting server acceptance acknowledgement");
        } else {
            debug(Minecraft.getMinecraft(), "puzzle_upload_queue_full", "the local network send queue could not accept " + name + " at room " + key);
        }
    }

    private void receiveMessage(String raw) {
        if (raw == null || raw.length() > 32768) {
            networkNotices.offer(new DiagnosticNotice("protocol_input_rejected", "incoming message was empty or exceeded the client-side size limit"));
            return;
        }
        try {
            JsonObject root = new JsonParser().parse(raw).getAsJsonObject();
            String type = root.has("type") ? root.get("type").getAsString() : "";
            if ("welcome".equals(type) && root.has("rooms") && root.get("rooms").isJsonArray()) {
                JsonArray rooms = root.getAsJsonArray("rooms");
                workerKnownPuzzles.clear();
                int queued = 0;
                for (JsonElement element : rooms) {
                    if (element != null && element.isJsonObject() && queueRoom(element.getAsJsonObject(), false)) {
                        rememberWorkerKnownRoom(element.getAsJsonObject());
                        queued++;
                    }
                }
                int connectedMembers = root.has("connectedMembers") ? safeInt(root.get("connectedMembers"), 1) : 1;
                networkNotices.offer(new DiagnosticNotice("worker_welcome", "Worker accepted the session handshake; connectedMembers="
                        + connectedMembers + ", storedRooms=" + rooms.size() + ", validRoomsQueued=" + queued));
                networkNotices.offer(new DiagnosticNotice("session_members", "currently connected clients sharing this exact session=" + connectedMembers));
                workerWelcomeSignal.set(true);
            } else if ("puzzle_discovered".equals(type)) {
                if (queueRoom(root, true)) {
                    rememberWorkerKnownRoom(root);
                    // Applied to puzzle memory on the Minecraft client thread, where the user notification is safe.
                } else {
                    networkNotices.offer(new DiagnosticNotice("remote_puzzle_rejected", "Worker sent a puzzle event that failed client-side validation"));
                }
            } else if ("puzzle_ack".equals(type)) {
                String name = root.has("name") ? normalizePuzzle(root.get("name").getAsString()) : "unknown";
                int x = root.has("x") ? root.get("x").getAsInt() : 0;
                int y = root.has("y") ? root.get("y").getAsInt() : 0;
                int z = root.has("z") ? root.get("z").getAsInt() : 0;
                boolean duplicate = root.has("duplicate") && root.get("duplicate").getAsBoolean();
                int stored = root.has("roomCount") ? safeInt(root.get("roomCount"), -1) : -1;
                int members = root.has("connectedMembers") ? safeInt(root.get("connectedMembers"), -1) : -1;
                String ackKey = x + "," + y + "," + z;
                Double previouslyKnown = workerKnownPuzzles.get(ackKey);
                double ackConfidence = root.has("confidence")
                        ? safeDouble(root.get("confidence"), previouslyKnown == null ? 0.0D : previouslyKnown)
                        : (previouslyKnown == null ? 0.0D : previouslyKnown);
                rememberWorkerKnown(x, y, z, ackConfidence);
                networkNotices.offer(new DiagnosticNotice("puzzle_upload_ack", name + " at " + x + "," + y + "," + z
                        + " accepted by Worker; duplicate=" + duplicate + ", storedRooms=" + stored
                        + ", connectedMembers=" + members));
            } else if ("sync_error".equals(type)) {
                String code = root.has("code") ? root.get("code").getAsString() : "unspecified";
                networkNotices.offer(new DiagnosticNotice("worker_rejected_message", "Worker rejected a sync message (code=" + code + ")"));
            } else if ("session_status".equals(type)) {
                int members = root.has("connectedMembers") ? safeInt(root.get("connectedMembers"), -1) : -1;
                networkNotices.offer(new DiagnosticNotice("session_members", "currently connected clients sharing this exact session=" + members));
            } else {
                networkNotices.offer(new DiagnosticNotice("unknown_worker_message", "received an unrecognized protocol message type; payload was discarded"));
            }
        } catch (RuntimeException ignored) {
            networkNotices.offer(new DiagnosticNotice("protocol_parse_error", "Worker message was not valid expected JSON; payload was discarded"));
        }
    }

    private boolean queueRoom(JsonObject room, boolean notifyPlayer) {
        try {
            String name = room.get("name").getAsString();
            int x = room.get("x").getAsInt();
            int y = room.get("y").getAsInt();
            int z = room.get("z").getAsInt();
            double confidence = room.has("confidence") ? room.get("confidence").getAsDouble() : 0.8D;
            if (!isAllowedPuzzle(name) || Math.abs((long) x) > 30000000L || Math.abs((long) z) > 30000000L
                    || y < 1 || y > 250 || Math.floorMod(x, 32) != 0 || Math.floorMod(z, 32) != 0
                    || confidence < 0.65D || confidence > 1.0D) return false;
            if (incoming.size() >= 128) {
                networkNotices.offer(new DiagnosticNotice("incoming_queue_full", "client puzzle queue is full; a remote event was dropped"));
                return false;
            }
            incoming.offer(new SharedPuzzle(normalizePuzzle(name), x, y, z, confidence, notifyPlayer));
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static int safeInt(JsonElement value, int fallback) {
        try { return value.getAsInt(); } catch (RuntimeException ignored) { return fallback; }
    }

    private static double safeDouble(JsonElement value, double fallback) {
        try {
            double parsed = value.getAsDouble();
            return Double.isFinite(parsed) ? parsed : fallback;
        } catch (RuntimeException ignored) { return fallback; }
    }

    private void rememberWorkerKnownRoom(JsonObject room) {
        try {
            rememberWorkerKnown(room.get("x").getAsInt(), room.get("y").getAsInt(), room.get("z").getAsInt(),
                    room.has("confidence") ? safeDouble(room.get("confidence"), 0.8D) : 0.8D);
        } catch (RuntimeException ignored) { }
    }

    private void rememberWorkerKnown(int x, int y, int z, double confidence) {
        if (!Double.isFinite(confidence) || confidence < 0.0D || confidence > 1.0D) return;
        String key = x + "," + y + "," + z;
        workerKnownPuzzles.merge(key, confidence, (previous, next) -> Math.max(previous, next));
    }

    private void processNetworkNotices(Minecraft mc) {
        DiagnosticNotice notice;
        while ((notice = networkNotices.poll()) != null) {
            if ("session_members".equals(notice.stage) && notice.detail.endsWith("=1")) {
                debug(mc, notice.stage, notice.detail + "; no teammate is connected to this session yet");
            } else {
                debug(mc, notice.stage, notice.detail);
            }
        }
    }

    private void logCaptureState(Minecraft mc, String detail) {
        if (detail == null || detail.equals(lastCaptureDebug)) return;
        lastCaptureDebug = detail;
        debug(mc, "roster_capture", detail);
    }

    /** Shows a client-local reminder. This never sends a message to the party or the server. */
    private static void showDisabledSyncReminder(Minecraft mc) {
        if (mc == null || mc.thePlayer == null) return;
        mc.thePlayer.addChatMessage(new ChatComponentText("\u00a7b[Lunaria] \u00a7fFor a better dungeon experience, enable Dungeon Sync in /lunaria > Dungeon > Dungeon Sync to share detected puzzle names with teammates who also enable it. Local puzzle solvers still work when Sync is off."));
    }

    private boolean diagnosticsEnabled() { return config != null && config.diagnosticsEnabled; }

    private void debug(Minecraft mc, String stage, String detail) {
        if (!diagnosticsEnabled()) return;
        String safeDetail = detail == null ? "(no detail)" : detail.replace('\n', ' ').replace('\r', ' ');
        if (safeDetail.length() > 240) safeDetail = safeDetail.substring(0, 240);
        String key = stage + "|" + safeDetail;
        long now = System.currentTimeMillis();
        Long previous = debugThrottle.get(key);
        if (previous != null) return;
        debugThrottle.put(key, now);
        if (debugThrottle.size() > 500) debugThrottle.clear();
        String line = "[Lunaria][DungeonSync][" + stage + "] " + safeDetail;
        System.out.println(line);
        if (mc != null && mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText("§8[§bLunaria Sync§8] §7" + stage + ": §f" + safeDetail));
        }
    }

    private String safeFailure(Exception failure) {
        if (failure == null) return "unknown network failure";
        String message = failure.getMessage();
        if (message == null || message.trim().isEmpty()) return failure.getClass().getSimpleName();
        message = message.replace('\n', ' ').replace('\r', ' ');
        if (config != null) {
            try {
                String host = new java.net.URI(config.endpoint).getHost();
                if (host != null && !host.isEmpty()) message = message.replace(host, "<sync-host>");
            } catch (Exception ignored) { }
        }
        message = message.replaceAll("(?i)[0-9a-f]{64}", "<session-hash>");
        if (message.length() > 140) message = message.substring(0, 140);
        return failure.getClass().getSimpleName() + ": " + message;
    }

    public SharedPuzzle pollSharedPuzzle() { return incoming.poll(); }

    public boolean consumeReplayRequest() {
        boolean request = replayLocalMemory;
        replayLocalMemory = false;
        return request;
    }

    public boolean isSessionActive() { return activeSessionId != null; }

    /** True while a world/dungeon transition is being verified instead of immediately removing the client. */
    public boolean isVerifyingExit() {
        return activeSessionId != null && (worldChangeStartedAt != 0L || missingDungeonTicks > 0);
    }

    private static String normalizePuzzle(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static boolean isAllowedPuzzle(String name) {
        return name != null && PUZZLES.contains(normalizePuzzle(name));
    }

    private static String randomHex(int bytes) {
        byte[] values = new byte[bytes];
        RANDOM.nextBytes(values);
        StringBuilder out = new StringBuilder(bytes * 2);
        for (byte value : values) out.append(String.format(Locale.ROOT, "%02x", value & 0xff));
        return out.toString();
    }

    private void resetPendingStart() {
        pendingStart = false;
        pendingStartAt = 0L;
        ticksUntilSample = 0;
        stableSamples = 0;
        lastSnapshot = null;
    }

    private void closeActiveSession() {
        DungeonPuzzleSyncClient oldClient = client;
        client = null;
        activeSessionId = null;
        activeServerId = null;
        activeFloor = null;
        sessionWorld = null;
        missingDungeonTicks = 0;
        worldChangeStartedAt = 0L;
        nextServerIdCheckAt = 0L;
        serverIdMissingSince = 0L;
        sentPuzzles.clear();
        workerKnownPuzzles.clear();
        workerWelcomeSignal.set(false);
        incoming.clear();
        replayLocalMemory = false;
        if (oldClient != null) oldClient.close();
    }

    private static final class DiagnosticNotice {
        final String stage;
        final String detail;
        DiagnosticNotice(String stage, String detail) { this.stage = stage; this.detail = detail; }
    }

    public static final class SharedPuzzle {
        public final String name;
        public final int x;
        public final int y;
        public final int z;
        public final double confidence;
        public final boolean notifyPlayer;
        SharedPuzzle(String name, int x, int y, int z, double confidence, boolean notifyPlayer) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.confidence = confidence;
            this.notifyPlayer = notifyPlayer;
        }
    }
}
