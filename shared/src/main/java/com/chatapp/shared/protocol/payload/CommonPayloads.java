package com.chatapp.shared.protocol.payload;

public final class CommonPayloads {

    private CommonPayloads() {
    }

    public static class ErrorPayload {
        public String message;

        public ErrorPayload() {
        }

        public ErrorPayload(String message) {
            this.message = message;
        }
    }

    public static class AckPayload {
        public String message;

        public AckPayload() {
        }

        public AckPayload(String message) {
            this.message = message;
        }
    }
}
