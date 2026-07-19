package com.chatapp.shared.protocol.payload;

import com.chatapp.shared.model.PresenceStatus;

public final class PresencePayloads {

    private PresencePayloads() {
    }

    public static class SetPresenceRequest {
        public PresenceStatus status;

        public SetPresenceRequest() {
        }

        public SetPresenceRequest(PresenceStatus status) {
            this.status = status;
        }
    }

    public static class PresenceUpdate {
        public int userId;
        public PresenceStatus status;

        public PresenceUpdate() {
        }

        public PresenceUpdate(int userId, PresenceStatus status) {
            this.userId = userId;
            this.status = status;
        }
    }
}
