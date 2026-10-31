package com.arena.pekaclient.assist.sync;

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

/**
 * Small Java-8 TLS WebSocket transport for the explicit Dungeon Sync opt-in. It verifies the HTTPS hostname and
 * WebSocket accept hash, bounds every header/frame/queue, masks client frames, and runs entirely off the Minecraft
 * thread. The caller receives only bounded text messages and failure class names.
 */
public final class DungeonSyncClient implements Closeable {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ACCEPT_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final int MAX_FRAME = 65536;
    private static final int MAX_OUTBOUND = 4096;

    private final URI uri;
    private final String memberId;
    private final Listener listener;
    private final ArrayBlockingQueue<String> outgoing = new ArrayBlockingQueue<String>(64);
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final Object outputLock = new Object();
    private volatile SSLSocket activeSocket;
    private volatile boolean connected;
    private Thread connectionThread;

    public DungeonSyncClient(String endpoint, String sessionId, String memberId, Listener listener) throws IOException {
        if (!validSession(sessionId) || !validMember(memberId)) throw new IOException("invalid local sync identity");
        this.uri = sessionUri(endpoint, sessionId);
        this.memberId = memberId;
        this.listener = listener;
    }

    public synchronized void start() {
        if (stopped.get() || (connectionThread != null && connectionThread.isAlive())) return;
        connectionThread = new Thread(new Runnable() {
            @Override public void run() { connectLoop(); }
        }, "Lunaria-DungeonSync");
        connectionThread.setDaemon(true);
        connectionThread.start();
    }

    public boolean send(String message) {
        return !stopped.get() && message != null && message.length() > 0 && message.length() <= MAX_OUTBOUND
                && outgoing.offer(message);
    }

    public boolean isConnected() {
        return connected;
    }

    private void connectLoop() {
        long delay = DungeonSyncReconnectPolicy.INITIAL_DELAY_MS;
        while (!stopped.get()) {
            long openedAt = 0L;
            SSLSocket socket = null;
            try {
                socket = open();
                activeSocket = socket;
                connected = true;
                openedAt = System.currentTimeMillis();
                sendFrame(socket.getOutputStream(), 1, "{\"type\":\"hello\",\"protocol\":1,\"memberId\":\""
                        + memberId + "\"}");
                if (listener != null) listener.onOpen();
                final SSLSocket writerSocket = socket;
                Thread writer = new Thread(new Runnable() {
                    @Override public void run() { writerLoop(writerSocket); }
                }, "Lunaria-DungeonSync-Send");
                writer.setDaemon(true);
                writer.start();
                readLoop(socket);
            } catch (Exception failure) {
                if (!stopped.get() && listener != null) listener.onProblem(failure.getClass().getSimpleName());
            } finally {
                connected = false;
                if (activeSocket == socket) activeSocket = null;
                closeQuietly(socket);
                delay = DungeonSyncReconnectPolicy.afterDisconnect(delay,
                        openedAt == 0L ? 0L : System.currentTimeMillis() - openedAt);
            }
            if (!stopped.get()) sleep(delay);
        }
    }

    private void readLoop(SSLSocket socket) throws IOException {
        InputStream input = socket.getInputStream();
        while (!stopped.get() && activeSocket == socket && !socket.isClosed()) {
            Frame frame = readFrame(input);
            if (frame.opcode == 1 && listener != null) listener.onText(new String(frame.payload, UTF8));
            else if (frame.opcode == 8) {
                sendFrame(socket.getOutputStream(), 8, frame.payload);
                return;
            } else if (frame.opcode == 9) {
                sendFrame(socket.getOutputStream(), 10, frame.payload);
            }
        }
    }

    private void writerLoop(SSLSocket socket) {
        long lastPing = System.currentTimeMillis();
        try {
            OutputStream output = socket.getOutputStream();
            while (!stopped.get() && activeSocket == socket && !socket.isClosed()) {
                String message = outgoing.poll(500L, TimeUnit.MILLISECONDS);
                if (activeSocket != socket) {
                    if (message != null) outgoing.offer(message);
                    return;
                }
                if (message != null) {
                    sendFrame(output, 1, message);
                } else if (System.currentTimeMillis() - lastPing >= 20000L) {
                    sendFrame(output, 9, new byte[0]);
                    lastPing = System.currentTimeMillis();
                }
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (IOException ignored) {
            closeQuietly(socket);
        }
    }

    private SSLSocket open() throws Exception {
        String host = uri.getHost();
        int port = uri.getPort() < 0 ? 443 : uri.getPort();
        SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(host, port);
        try {
            socket.setSoTimeout(12000);
            SSLParameters ssl = socket.getSSLParameters();
            ssl.setEndpointIdentificationAlgorithm("HTTPS");
            socket.setSSLParameters(ssl);
            socket.startHandshake();
            byte[] nonce = new byte[16];
            RANDOM.nextBytes(nonce);
            String key = Base64.getEncoder().encodeToString(nonce);
            String path = uri.getRawPath();
            if (path == null || path.length() == 0) path = "/";
            String hostHeader = host + (port == 443 ? "" : ":" + port);
            String request = "GET " + path + " HTTP/1.1\r\nHost: " + hostHeader + "\r\nUpgrade: websocket\r\n"
                    + "Connection: Upgrade\r\nSec-WebSocket-Version: 13\r\nSec-WebSocket-Key: " + key + "\r\n\r\n";
            OutputStream output = socket.getOutputStream();
            output.write(request.getBytes("US-ASCII"));
            output.flush();
            InputStream input = socket.getInputStream();
            String status = line(input, 4096);
            if (status == null || !status.matches("HTTP/1\\.[01] 101(?: .*)?")) throw new IOException("upgrade rejected");
            String accepted = "";
            int consumed = status.length() + 2;
            while (true) {
                String header = line(input, 4096);
                if (header == null) throw new EOFException("truncated upgrade response");
                consumed += header.length() + 2;
                if (consumed > 16384) throw new IOException("oversized upgrade response");
                if (header.length() == 0) break;
                int colon = header.indexOf(':');
                if (colon > 0 && "sec-websocket-accept".equalsIgnoreCase(header.substring(0, colon).trim())) {
                    accepted = header.substring(colon + 1).trim();
                }
            }
            String expected = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1")
                    .digest((key + ACCEPT_GUID).getBytes("US-ASCII")));
            if (!expected.equals(accepted)) throw new IOException("upgrade accept mismatch");
            socket.setSoTimeout(0);
            return socket;
        } catch (Exception failure) {
            closeQuietly(socket);
            throw failure;
        }
    }

    private void sendFrame(OutputStream output, int opcode, String text) throws IOException {
        sendFrame(output, opcode, text.getBytes(UTF8));
    }

    private void sendFrame(OutputStream output, int opcode, byte[] payload) throws IOException {
        if (payload == null || payload.length > MAX_FRAME) throw new IOException("outbound frame limit");
        byte[] mask = new byte[4];
        RANDOM.nextBytes(mask);
        ByteArrayOutputStream frame = new ByteArrayOutputStream(payload.length + 14);
        frame.write(0x80 | (opcode & 15));
        if (payload.length <= 125) {
            frame.write(0x80 | payload.length);
        } else {
            frame.write(0x80 | 126);
            frame.write((payload.length >>> 8) & 255);
            frame.write(payload.length & 255);
        }
        frame.write(mask);
        for (int index = 0; index < payload.length; index++) frame.write(payload[index] ^ mask[index & 3]);
        synchronized (outputLock) {
            output.write(frame.toByteArray());
            output.flush();
        }
    }

    private static Frame readFrame(InputStream input) throws IOException {
        int first = required(input);
        int second = required(input);
        if ((first & 0x80) == 0) throw new IOException("fragmented frame rejected");
        long length = second & 127;
        if (length == 126) length = ((long) required(input) << 8) | required(input);
        else if (length == 127) throw new IOException("large frame rejected");
        if (length > MAX_FRAME) throw new IOException("inbound frame limit");
        byte[] mask = (second & 0x80) == 0 ? null : bytes(input, 4);
        byte[] payload = bytes(input, (int) length);
        if (mask != null) for (int index = 0; index < payload.length; index++) payload[index] ^= mask[index & 3];
        return new Frame(first & 15, payload);
    }

    private static int required(InputStream input) throws IOException {
        int value = input.read();
        if (value < 0) throw new EOFException("socket closed");
        return value;
    }

    private static byte[] bytes(InputStream input, int length) throws IOException {
        byte[] data = new byte[length];
        int offset = 0;
        while (offset < length) {
            int count = input.read(data, offset, length - offset);
            if (count < 0) throw new EOFException("truncated frame");
            offset += count;
        }
        return data;
    }

    private static String line(InputStream input, int limit) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int previous = -1;
        while (output.size() < limit) {
            int value = input.read();
            if (value < 0) return output.size() == 0 ? null : new String(output.toByteArray(), "US-ASCII");
            if (previous == '\r' && value == '\n') {
                byte[] bytes = output.toByteArray();
                return new String(bytes, 0, Math.max(0, bytes.length - 1), "US-ASCII");
            }
            output.write(value);
            previous = value;
        }
        throw new IOException("header line limit");
    }

    private static URI sessionUri(String endpoint, String session) throws IOException {
        try {
            URI base = new URI(endpoint == null ? "" : endpoint.trim());
            if (!"wss".equalsIgnoreCase(base.getScheme()) || base.getHost() == null || base.getHost().length() == 0
                    || base.getUserInfo() != null || base.getQuery() != null || base.getFragment() != null
                    || (base.getPort() != -1 && (base.getPort() < 1 || base.getPort() > 65535))) {
                throw new IOException("invalid secure sync endpoint");
            }
            String path = base.getRawPath();
            if (path == null || path.length() == 0) path = "/ws";
            while (path.endsWith("/")) path = path.substring(0, path.length() - 1);
            if (!path.endsWith("/ws")) throw new IOException("sync endpoint path rejected");
            return new URI("wss", null, base.getHost(), base.getPort(), path + "/" + session, null, null);
        } catch (URISyntaxException invalid) {
            throw new IOException("invalid secure sync endpoint", invalid);
        }
    }

    private static boolean validSession(String value) {
        return value != null && value.matches("[0-9a-f]{64}");
    }

    private static boolean validMember(String value) {
        return value != null && value.matches("[0-9a-f]{32}");
    }

    private static void sleep(long delay) {
        try { Thread.sleep(delay); }
        catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }

    @Override
    public void close() {
        if (!stopped.compareAndSet(false, true)) return;
        connected = false;
        closeQuietly(activeSocket);
        Thread thread = connectionThread;
        if (thread != null) thread.interrupt();
        outgoing.clear();
    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable == null) return;
        try { closeable.close(); } catch (IOException ignored) { }
    }

    public interface Listener {
        void onOpen();
        void onText(String text);
        void onProblem(String kind);
    }

    private static final class Frame {
        final int opcode;
        final byte[] payload;
        Frame(int opcode, byte[] payload) {
            this.opcode = opcode;
            this.payload = payload;
        }
    }
}
