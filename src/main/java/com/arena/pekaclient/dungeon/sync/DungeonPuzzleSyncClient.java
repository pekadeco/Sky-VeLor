package com.arena.pekaclient.dungeon.sync;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Minimal Java-8 TLS WebSocket client; uses the runtime's SSL stack and no bundled networking library. */
public final class DungeonPuzzleSyncClient implements Closeable {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final String WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final int MAX_FRAME_BYTES = 1024 * 1024;
    private static final long INITIAL_RECONNECT_DELAY_MS = DungeonSyncReconnectPolicy.INITIAL_DELAY_MS;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final URI endpoint;
    private final String sessionId;
    private final String memberId;
    private final Listener listener;
    private final ArrayBlockingQueue<String> outbound = new ArrayBlockingQueue<String>(64);
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private volatile boolean connected;
    private volatile SSLSocket socket;
    private Thread connectionThread;

    public DungeonPuzzleSyncClient(String endpointBase, String sessionId, String memberId, Listener listener) throws IOException {
        if (!DungeonPuzzleSyncConfig.isSecureWebSocketEndpoint(endpointBase)) {
            throw new IOException("Dungeon sync endpoint must use wss:// and have no query string");
        }
        if (sessionId == null || !sessionId.matches("[0-9a-f]{64}")) throw new IOException("Invalid session ID");
        if (memberId == null || !memberId.matches("[0-9a-f]{32}")) throw new IOException("Invalid ephemeral member ID");
        String base = endpointBase.trim().replaceAll("/+$", "");
        try {
            this.endpoint = new URI(base + "/" + sessionId);
        } catch (URISyntaxException invalidEndpoint) {
            throw new IOException("Invalid WebSocket endpoint", invalidEndpoint);
        }
        if (!"wss".equalsIgnoreCase(endpoint.getScheme())) throw new IOException("TLS is required for puzzle sync");
        this.sessionId = sessionId;
        this.memberId = memberId;
        this.listener = listener;
    }

    public synchronized void connectAsync() {
        if (stopped.get() || (connectionThread != null && connectionThread.isAlive())) return;
        connectionThread = new Thread(new Runnable() {
            @Override public void run() { connectionLoop(); }
        }, "Lunaria-PuzzleSync");
        connectionThread.setDaemon(true);
        connectionThread.start();
    }

    public boolean isConnected() { return connected; }

    public boolean send(JsonObject message) {
        if (message == null || stopped.get()) return false;
        String encoded = message.toString();
        if (encoded.length() > 4096) return false;
        // Per-run puzzle memory is also retained locally, so a full queue can be replayed after reconnect.
        return outbound.offer(encoded);
    }

    private void connectionLoop() {
        long reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS;
        while (!stopped.get()) {
            SSLSocket active = null;
            long connectedAt = 0L;
            try {
                active = openWebSocket();
                socket = active;
                connected = true;
                connectedAt = System.currentTimeMillis();
                JsonObject hello = new JsonObject();
                hello.addProperty("type", "hello");
                hello.addProperty("protocol", 1);
                hello.addProperty("memberId", memberId);
                writeFrame(active.getOutputStream(), 0x1, hello.toString().getBytes(UTF8));
                if (listener != null) listener.onConnected();

                final SSLSocket writerSocket = active;
                Thread writer = new Thread(new Runnable() {
                    @Override public void run() { writeLoop(writerSocket); }
                }, "Lunaria-PuzzleSync-Send");
                writer.setDaemon(true);
                writer.start();

                InputStream input = active.getInputStream();
                while (!stopped.get() && !active.isClosed()) {
                    Frame frame = readFrame(input);
                    if (frame == null) break;
                    if (frame.opcode == 0x1) {
                        if (listener != null) listener.onMessage(new String(frame.payload, UTF8));
                    } else if (frame.opcode == 0x8) {
                        try { writeFrame(active.getOutputStream(), 0x8, frame.payload); } catch (IOException ignored) { }
                        break;
                    } else if (frame.opcode == 0x9) {
                        writeFrame(active.getOutputStream(), 0xA, frame.payload);
                    }
                }
            } catch (Exception failure) {
                if (!stopped.get() && listener != null) listener.onFailure(failure);
            } finally {
                connected = false;
                if (socket == active) socket = null;
                closeQuietly(active);
                long connectionDurationMs = connectedAt == 0L ? 0L : System.currentTimeMillis() - connectedAt;
                reconnectDelayMs = DungeonSyncReconnectPolicy.afterDisconnect(reconnectDelayMs, connectionDurationMs);
            }
            if (!stopped.get()) {
                try { Thread.sleep(reconnectDelayMs); }
                catch (InterruptedException ignored) { Thread.currentThread().interrupt(); return; }
            }
        }
    }

    private void writeLoop(SSLSocket active) {
        OutputStream output;
        try { output = active.getOutputStream(); }
        catch (IOException failure) { return; }
        long lastPing = System.currentTimeMillis();
        while (!stopped.get() && socket == active && !active.isClosed()) {
            try {
                String message = outbound.poll(500L, TimeUnit.MILLISECONDS);
                if (socket != active) {
                    if (message != null) outbound.offer(message);
                    return;
                }
                if (message != null) {
                    writeFrame(output, 0x1, message.getBytes(UTF8));
                } else if (System.currentTimeMillis() - lastPing >= 20000L) {
                    writeFrame(output, 0x9, new byte[0]);
                    lastPing = System.currentTimeMillis();
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return;
            } catch (IOException failure) {
                closeQuietly(active);
                return;
            }
        }
    }

    private SSLSocket openWebSocket() throws Exception {
        String host = endpoint.getHost();
        int port = endpoint.getPort() < 0 ? 443 : endpoint.getPort();
        SSLSocket active = (SSLSocket) SSLSocketFactory.getDefault().createSocket(host, port);
        try {
            active.setSoTimeout(12000);
            SSLParameters parameters = active.getSSLParameters();
            parameters.setEndpointIdentificationAlgorithm("HTTPS");
            active.setSSLParameters(parameters);
            active.startHandshake();

            byte[] nonce = new byte[16];
            RANDOM.nextBytes(nonce);
            String key = Base64.getEncoder().encodeToString(nonce);
            String path = endpoint.getRawPath();
            if (path == null || path.isEmpty()) path = "/";
            String hostHeader = host + (port == 443 ? "" : ":" + port);
            String request = "GET " + path + " HTTP/1.1\r\n"
                    + "Host: " + hostHeader + "\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Sec-WebSocket-Key: " + key + "\r\n"
                    + "Sec-WebSocket-Version: 13\r\n\r\n";
            OutputStream output = active.getOutputStream();
            output.write(request.getBytes("US-ASCII"));
            output.flush();

            InputStream input = active.getInputStream();
            String status = readHttpLine(input, 4096);
            if (status == null || !status.matches("HTTP/1\\.[01] 101(?: .*)?")) {
                throw new IOException("WebSocket upgrade was rejected: " + status);
            }
            String accept = null;
            int bytes = status.length() + 2;
            while (true) {
                String line = readHttpLine(input, 4096);
                if (line == null) throw new EOFException("Unexpected end of WebSocket handshake");
                bytes += line.length() + 2;
                if (bytes > 16384) throw new IOException("WebSocket handshake headers are too large");
                if (line.isEmpty()) break;
                int separator = line.indexOf(':');
                if (separator > 0 && "sec-websocket-accept".equalsIgnoreCase(line.substring(0, separator).trim())) {
                    accept = line.substring(separator + 1).trim();
                }
            }
            String expected = Base64.getEncoder().encodeToString(
                    MessageDigest.getInstance("SHA-1").digest((key + WEBSOCKET_GUID).getBytes("US-ASCII")));
            if (accept == null || !expected.equals(accept)) throw new IOException("WebSocket accept verification failed");
            active.setSoTimeout(0);
            return active;
        } catch (Exception failure) {
            closeQuietly(active);
            throw failure;
        }
    }

    private static String readHttpLine(InputStream input, int limit) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        int previous = -1;
        while (line.size() < limit) {
            int value = input.read();
            if (value < 0) return line.size() == 0 ? null : new String(line.toByteArray(), "US-ASCII");
            if (previous == '\r' && value == '\n') {
                byte[] bytes = line.toByteArray();
                return new String(bytes, 0, Math.max(0, bytes.length - 1), "US-ASCII");
            }
            line.write(value);
            previous = value;
        }
        throw new IOException("WebSocket handshake line is too large");
    }

    private static Frame readFrame(InputStream input) throws IOException {
        int first = input.read();
        if (first < 0) throw new EOFException("WebSocket connection closed");
        int second = input.read();
        if (second < 0) throw new EOFException("Truncated WebSocket frame");
        boolean masked = (second & 0x80) != 0;
        long length = second & 0x7f;
        if (length == 126) {
            length = ((long) readRequired(input) << 8) | readRequired(input);
        } else if (length == 127) {
            length = 0;
            for (int i = 0; i < 8; i++) length = (length << 8) | readRequired(input);
        }
        if (length < 0 || length > MAX_FRAME_BYTES) throw new IOException("WebSocket frame exceeded the local size limit");
        byte[] mask = masked ? readExact(input, 4) : null;
        byte[] payload = readExact(input, (int) length);
        if (masked) for (int i = 0; i < payload.length; i++) payload[i] = (byte) (payload[i] ^ mask[i & 3]);
        return new Frame(first & 0x0f, payload);
    }

    private static int readRequired(InputStream input) throws IOException {
        int value = input.read();
        if (value < 0) throw new EOFException("Truncated WebSocket frame header");
        return value;
    }

    private static byte[] readExact(InputStream input, int count) throws IOException {
        byte[] bytes = new byte[count];
        int offset = 0;
        while (offset < count) {
            int read = input.read(bytes, offset, count - offset);
            if (read < 0) throw new EOFException("Truncated WebSocket frame payload");
            offset += read;
        }
        return bytes;
    }

    private static void writeFrame(OutputStream output, int opcode, byte[] payload) throws IOException {
        if (payload.length > MAX_FRAME_BYTES) throw new IOException("Outgoing WebSocket message is too large");
        byte[] mask = new byte[4];
        RANDOM.nextBytes(mask);
        ByteArrayOutputStream frame = new ByteArrayOutputStream(payload.length + 14);
        frame.write(0x80 | (opcode & 0x0f));
        if (payload.length <= 125) {
            frame.write(0x80 | payload.length);
        } else if (payload.length <= 65535) {
            frame.write(0x80 | 126);
            frame.write((payload.length >>> 8) & 0xff);
            frame.write(payload.length & 0xff);
        } else {
            frame.write(0x80 | 127);
            long length = payload.length;
            for (int i = 7; i >= 0; i--) frame.write((int) ((length >>> (i * 8)) & 0xff));
        }
        frame.write(mask);
        byte[] maskedPayload = new byte[payload.length];
        for (int i = 0; i < payload.length; i++) maskedPayload[i] = (byte) (payload[i] ^ mask[i & 3]);
        frame.write(maskedPayload);
        synchronized (output) {
            output.write(frame.toByteArray());
            output.flush();
        }
    }

    @Override public void close() {
        if (!stopped.compareAndSet(false, true)) return;
        connected = false;
        closeQuietly(socket);
        Thread thread = connectionThread;
        if (thread != null) thread.interrupt();
        outbound.clear();
    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try { closeable.close(); } catch (IOException ignored) { }
        }
    }

    public interface Listener {
        void onConnected();
        void onMessage(String message);
        void onFailure(Exception failure);
    }

    private static final class Frame {
        final int opcode;
        final byte[] payload;
        Frame(int opcode, byte[] payload) { this.opcode = opcode; this.payload = payload; }
    }
}
