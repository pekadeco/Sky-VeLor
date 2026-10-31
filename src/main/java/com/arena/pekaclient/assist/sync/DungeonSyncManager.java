package com.arena.pekaclient.assist.sync;

import com.arena.pekaclient.config.AssistSettings;
import com.arena.pekaclient.dungeon.DungeonMapOverlay;
import com.arena.pekaclient.dungeon.PuzzleRoomMatcher;
import com.arena.pekaclient.feature.DungeonContext;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Owns the optional sync lifecycle. All player/roster parsing is local; the worker receives only a URL-path session
 * hash, a random connection ID, and allow-listed puzzle coordinates. No socket is created until the player enables
 * the setting, a strict dungeon snapshot is available, and the secure endpoint passes validation.
 */
public final class DungeonSyncManager {
    private static final int CAPTURE_INTERVAL_TICKS = 20;
    private static final int MAX_INBOUND = 128;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static DungeonSyncManager live;

    private final DungeonMapOverlay map;
    private final ConcurrentLinkedQueue<DungeonSyncMessages.Puzzle> inbound = new ConcurrentLinkedQueue<DungeonSyncMessages.Puzzle>();
    private DungeonSyncClient client;
    private String sessionId = "";
    private String sentKey = "";
    private double sentConfidence;
    private int ticksUntilCapture;
    private volatile boolean replayRequested;
    private volatile String status = "disabled";
    private long lastDebugAt;

    public DungeonSyncManager(DungeonMapOverlay map) {
        this.map = map;
        live = this;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!AssistSettings.dungeonSync || mc == null || mc.theWorld == null || mc.thePlayer == null
                || !DungeonContext.inDungeon()) {
            stop("disabled or outside dungeon");
            return;
        }
        if (++ticksUntilCapture >= CAPTURE_INTERVAL_TICKS || client == null) {
            ticksUntilCapture = 0;
            ensureSession(mc);
        }
        applyInbound();
        if (replayRequested) {
            replayRequested = false;
            sentKey = "";
            sentConfidence = 0D;
        }
        publishCurrentMatch();
        if (AssistSettings.dungeonSyncDebug && System.currentTimeMillis() - lastDebugAt >= 30000L) {
            lastDebugAt = System.currentTimeMillis();
            debug("status=" + safeStatus());
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        stop("world changed");
    }

    private void ensureSession(Minecraft mc) {
        DungeonSyncEnvironment.Snapshot snapshot = DungeonSyncEnvironment.capture(mc);
        if (snapshot == null) {
            if (client == null) status = "waiting for a stable dungeon tab/scoreboard snapshot";
            return;
        }
        if (snapshot.sessionId.equals(sessionId) && client != null) return;
        stop("new dungeon session");
        try {
            DungeonSyncClient next = new DungeonSyncClient(AssistSettings.dungeonSyncEndpoint, snapshot.sessionId,
                    randomMemberId(), new DungeonSyncClient.Listener() {
                        @Override public void onOpen() {
                            replayRequested = true;
                            status = "connected";
                        }

                        @Override public void onText(String text) {
                            receive(text);
                        }

                        @Override public void onProblem(String kind) {
                            status = "transport retry: " + (kind == null ? "unknown" : kind);
                        }
                    });
            sessionId = snapshot.sessionId;
            sentKey = "";
            sentConfidence = 0D;
            status = "connecting";
            client = next;
            next.start();
            debug("created encrypted puzzle-sync transport for a hashed party session; no usernames are uploaded");
        } catch (Exception unavailable) {
            sessionId = "";
            status = "secure transport unavailable";
            debug("sync transport was not started because the secure endpoint or local identity was rejected");
        }
    }

    private void receive(String raw) {
        java.util.List<DungeonSyncMessages.Puzzle> rooms = DungeonSyncMessages.welcomeRooms(raw);
        if (!rooms.isEmpty()) {
            for (DungeonSyncMessages.Puzzle room : rooms) enqueue(room);
            status = "connected; received worker room snapshot";
            return;
        }
        DungeonSyncMessages.Puzzle puzzle = DungeonSyncMessages.puzzleEvent(raw);
        if (puzzle != null) {
            enqueue(puzzle);
            status = "connected; received teammate puzzle label";
        }
        // Other bounded worker notices (ack/status/error) deliberately carry no user data and need no UI echo.
    }

    private void enqueue(DungeonSyncMessages.Puzzle puzzle) {
        if (puzzle == null || inbound.size() >= MAX_INBOUND) return;
        inbound.offer(puzzle);
    }

    private void applyInbound() {
        if (map == null) {
            inbound.clear();
            return;
        }
        DungeonSyncMessages.Puzzle next;
        while ((next = inbound.poll()) != null) {
            map.rememberSyncedPuzzle(next.name, next.x, next.y, next.z, next.confidence);
        }
    }

    private void publishCurrentMatch() {
        if (client == null || map == null || sessionId.length() != 64) return;
        PuzzleRoomMatcher.Match match = map.currentPuzzleMatchForSync();
        if (match == null || match.confidence < .65D) return;
        String payload = DungeonSyncProtocol.encodePuzzle(match.name, match.originX, match.originY, match.originZ,
                match.confidence);
        if (payload.length() == 0) return;
        String key = DungeonSyncProtocol.canonicalPuzzleName(match.name) + "@" + match.originX + ',' + match.originY
                + ',' + match.originZ;
        if (key.equals(sentKey) && sentConfidence >= match.confidence) return;
        if (client.send(payload)) {
            sentKey = key;
            sentConfidence = match.confidence;
            status = client.isConnected() ? "connected; local puzzle queued" : "connecting; local puzzle queued";
        }
    }

    private void stop(String reason) {
        DungeonSyncClient old = client;
        client = null;
        sessionId = "";
        sentKey = "";
        sentConfidence = 0D;
        ticksUntilCapture = 0;
        replayRequested = false;
        inbound.clear();
        status = reason == null ? "disabled" : reason;
        if (old != null) old.close();
    }

    private void debug(String line) {
        if (!AssistSettings.dungeonSyncDebug) return;
        System.out.println("[Lunaria][Dungeon Sync] " + line);
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText("\u00a78[\u00a7bLunaria Sync\u00a78] \u00a77" + line));
        }
    }

    private String safeStatus() {
        String current = status == null ? "unknown" : status.replace('\n', ' ').replace('\r', ' ');
        return current.length() > 140 ? current.substring(0, 140) : current;
    }

    private static String randomMemberId() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        StringBuilder value = new StringBuilder(32);
        for (byte part : bytes) value.append(String.format(Locale.ROOT, "%02x", part & 255));
        return value.toString();
    }

    public static String selfTestStatus() {
        DungeonSyncManager manager = live;
        return "enabled=" + AssistSettings.dungeonSync + ", " + (manager == null ? "controller unavailable" : manager.safeStatus());
    }
}
