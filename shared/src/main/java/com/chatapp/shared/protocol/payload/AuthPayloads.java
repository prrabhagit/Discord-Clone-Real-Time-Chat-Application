package com.chatapp.shared.protocol.payload;

import com.chatapp.shared.model.UserDto;

/** Groups all payload classes related to registration / login. */
public final class AuthPayloads {

    private AuthPayloads() {
    }

    public static class RegisterRequest {
        public String username;
        public String password;

        public RegisterRequest() {
        }

        public RegisterRequest(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    public static class LoginRequest {
        public String username;
        public String password;

        public LoginRequest() {
        }

        public LoginRequest(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    public static class AuthResponse {
        public boolean success;
        public String message;
        public UserDto user;

        public AuthResponse() {
        }

        public AuthResponse(boolean success, String message, UserDto user) {
            this.success = success;
            this.message = message;
            this.user = user;
        }
    }
}
