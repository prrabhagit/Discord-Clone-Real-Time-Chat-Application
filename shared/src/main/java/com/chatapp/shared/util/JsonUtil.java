package com.chatapp.shared.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.chatapp.shared.protocol.Envelope;
import com.chatapp.shared.protocol.MessageType;

/**
 * Central Gson instance and small helpers for building/parsing {@link Envelope}s.
 * Kept as a single utility so every module (client/server) serializes the
 * exact same way.
 */
public final class JsonUtil {

    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    private JsonUtil() {
    }

    public static Gson gson() {
        return GSON;
    }

    /** Build an envelope wrapping any payload object (or null for type-only messages). */
    public static Envelope envelope(MessageType type, Object payload) {
        JsonElement element = payload == null ? null : GSON.toJsonTree(payload);
        return new Envelope(type, element);
    }

    public static Envelope envelope(MessageType type, Object payload, String requestId) {
        JsonElement element = payload == null ? null : GSON.toJsonTree(payload);
        return new Envelope(type, element, requestId);
    }

    /** Serialize an envelope to a single line (no embedded newlines). */
    public static String toLine(Envelope envelope) {
        return GSON.toJson(envelope);
    }

    /** Parse a line of text received from the socket back into an envelope. */
    public static Envelope fromLine(String line) {
        return GSON.fromJson(line, Envelope.class);
    }

    /** Convert an envelope's payload into a concrete payload class. */
    public static <T> T payload(Envelope envelope, Class<T> clazz) {
        if (envelope.getPayload() == null) {
            return null;
        }
        return GSON.fromJson(envelope.getPayload(), clazz);
    }
}
