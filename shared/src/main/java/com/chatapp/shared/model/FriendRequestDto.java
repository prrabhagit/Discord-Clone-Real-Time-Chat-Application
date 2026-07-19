package com.chatapp.shared.model;

import java.io.Serializable;

public class FriendRequestDto implements Serializable {

    private int requestId;
    private int senderId;
    private String senderUsername;
    private String senderAvatarColor;
    private long createdAt;

    public FriendRequestDto() {
    }

    public FriendRequestDto(int requestId, int senderId, String senderUsername,
                             String senderAvatarColor, long createdAt) {
        this.requestId = requestId;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.senderAvatarColor = senderAvatarColor;
        this.createdAt = createdAt;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getSenderAvatarColor() {
        return senderAvatarColor;
    }

    public void setSenderAvatarColor(String senderAvatarColor) {
        this.senderAvatarColor = senderAvatarColor;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
