package com.chatapp.server.net;

import com.chatapp.shared.protocol.Envelope;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which users currently have a live socket connection, keyed by
 * user id. Used to route real-time pushes (messages, typing, presence)
 * to the right connected clients.
 */
public class SessionManager {

    private final ConcurrentHashMap<Integer, ClientHandler> sessions = new ConcurrentHashMap<>();

    public void register(int userId, ClientHandler handler) {
        sessions.put(userId, handler);
    }

    public void unregister(int userId) {
        sessions.remove(userId);
    }

    public boolean isOnline(int userId) {
        return sessions.containsKey(userId);
    }

    public ClientHandler get(int userId) {
        return sessions.get(userId);
    }

    public Collection<ClientHandler> allSessions() {
        return sessions.values();
    }

    /** Send an envelope to a single user, if they are currently connected. Silent no-op otherwise. */
    public void sendTo(int userId, Envelope envelope) {
        ClientHandler handler = sessions.get(userId);
        if (handler != null) {
            handler.send(envelope);
        }
    }

    /** Send an envelope to a batch of users, skipping anyone not connected. */
    public void sendToMany(Iterable<Integer> userIds, Envelope envelope) {
        for (int userId : userIds) {
            sendTo(userId, envelope);
        }
    }
}
