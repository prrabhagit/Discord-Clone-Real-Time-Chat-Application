package com.chatapp.client.net;

import com.chatapp.shared.protocol.Envelope;
import com.chatapp.shared.protocol.MessageType;
import com.chatapp.shared.util.JsonUtil;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Owns the client's single socket connection to the ChatApp server.
 * Sends are synchronous (write + flush); incoming envelopes are read on a
 * dedicated background thread and dispatched to registered listeners on the
 * JavaFX application thread via {@link Platform#runLater}.
 */
public class SocketClient {

    private final Map<MessageType, Consumer<Envelope>> listeners = new ConcurrentHashMap<>();
    private Runnable onDisconnected;

    private Socket socket;
    private PrintWriter out;
    private Thread readerThread;
    private volatile boolean connected;

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), false, StandardCharsets.UTF_8);
        connected = true;

        readerThread = new Thread(this::readLoop, "socket-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void readLoop() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while (connected && (line = in.readLine()) != null) {
                dispatch(line);
            }
        } catch (IOException e) {
            // connection dropped - fall through to disconnect handling below
        } finally {
            connected = false;
            if (onDisconnected != null) {
                Platform.runLater(onDisconnected);
            }
        }
    }

    private void dispatch(String line) {
        Envelope envelope;
        try {
            envelope = JsonUtil.fromLine(line);
        } catch (Exception e) {
            return;
        }
        if (envelope == null || envelope.getType() == null) {
            return;
        }
        Consumer<Envelope> listener = listeners.get(envelope.getType());
        if (listener != null) {
            Platform.runLater(() -> listener.accept(envelope));
        }
    }

    /** Registers (or replaces) the handler invoked whenever a message of this type arrives. */
    public void on(MessageType type, Consumer<Envelope> handler) {
        listeners.put(type, handler);
    }

    /** Registers the callback fired once when the connection is lost. */
    public void onDisconnected(Runnable callback) {
        this.onDisconnected = callback;
    }

    public synchronized void send(MessageType type, Object payload) {
        if (!connected || out == null) {
            return;
        }
        Envelope envelope = JsonUtil.envelope(type, payload);
        out.println(JsonUtil.toLine(envelope));
        out.flush();
    }

    public boolean isConnected() {
        return connected;
    }

    public void disconnect() {
        connected = false;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }
}
