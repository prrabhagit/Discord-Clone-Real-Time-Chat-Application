package com.chatapp.shared.model;

import java.io.Serializable;

/**
 * A single chat message. Used both for direct messages (receiverId set,
 * channelId 0) and channel messages (channelId set, receiverId 0).
 */
public class ChatMessageDto implements Serializable {

    private long id;
    private int senderId;
    private String senderUsername;
    private String senderAvatarColor;
    private Integer receiverId;   // null/0 for channel messages
    private Integer channelId;    // null/0 for direct messages
    private String content;
    private long timestamp;

    public ChatMessageDto() {
    }

    public ChatMessageDto(long id, int senderId, String senderUsername, String senderAvatarColor,
                           Integer receiverId, Integer channelId, String content, long timestamp) {
        this.id = id;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.senderAvatarColor = senderAvatarColor;
        this.receiverId = receiverId;
        this.channelId = channelId;
        this.content = content;
        this.timestamp = timestamp;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public Integer getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Integer receiverId) {
        this.receiverId = receiverId;
    }

    public Integer getChannelId() {
        return channelId;
    }

    public void setChannelId(Integer channelId) {
        this.channelId = channelId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
