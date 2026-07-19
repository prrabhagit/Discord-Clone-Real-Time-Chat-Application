package com.chatapp.shared.protocol;

import com.google.gson.JsonElement;

/**
 * The single wire format used for every message exchanged over the socket.
 * Each line sent on the wire is exactly one JSON-serialized Envelope,
 * terminated by a newline (newline-delimited JSON).
 */
public class Envelope {

    private MessageType type;
    private JsonElement payload;

    /** Optional client-generated id, echoed back so the client can correlate responses. */
    private String requestId;

    public Envelope() {
    }

    public Envelope(MessageType type, JsonElement payload) {
        this.type = type;
        this.payload = payload;
    }

    public Envelope(MessageType type, JsonElement payload, String requestId) {
        this.type = type;
        this.payload = payload;
        this.requestId = requestId;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public JsonElement getPayload() {
        return payload;
    }

    public void setPayload(JsonElement payload) {
        this.payload = payload;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    @Override
    public String toString() {
        return "Envelope{type=" + type + ", requestId=" + requestId + ", payload=" + payload + '}';
    }
}
