package com.chatapp.shared.model;

import java.io.Serializable;

/** Public-facing user info sent to clients. Never carries the password hash. */
public class UserDto implements Serializable {

    private int id;
    private String username;
    private String avatarColor;
    private PresenceStatus status;

    public UserDto() {
    }

    public UserDto(int id, String username, String avatarColor, PresenceStatus status) {
        this.id = id;
        this.username = username;
        this.avatarColor = avatarColor;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAvatarColor() {
        return avatarColor;
    }

    public void setAvatarColor(String avatarColor) {
        this.avatarColor = avatarColor;
    }

    public PresenceStatus getStatus() {
        return status;
    }

    public void setStatus(PresenceStatus status) {
        this.status = status;
    }
}
