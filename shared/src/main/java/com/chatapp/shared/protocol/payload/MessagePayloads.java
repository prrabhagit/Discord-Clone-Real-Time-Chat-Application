package com.chatapp.shared.protocol.payload;

import com.chatapp.shared.model.ChatMessageDto;

import java.util.List;

public final class MessagePayloads {

    private MessagePayloads() {
    }

    public static class SendDirectMessage {
        public int receiverId;
        public String content;

        public SendDirectMessage() {
        }

        public SendDirectMessage(int receiverId, String content) {
            this.receiverId = receiverId;
            this.content = content;
        }
    }

    public static class GetDmHistory {
        public int friendId;

        public GetDmHistory() {
        }

        public GetDmHistory(int friendId) {
            this.friendId = friendId;
        }
    }

    public static class DmHistory {
        public int friendId;
        public List<ChatMessageDto> messages;

        public DmHistory() {
        }

        public DmHistory(int friendId, List<ChatMessageDto> messages) {
            this.friendId = friendId;
            this.messages = messages;
        }
    }

    public static class SendChannelMessage {
        public int channelId;
        public String content;

        public SendChannelMessage() {
        }

        public SendChannelMessage(int channelId, String content) {
            this.channelId = channelId;
            this.content = content;
        }
    }

    public static class GetChannelHistory {
        public int channelId;

        public GetChannelHistory() {
        }

        public GetChannelHistory(int channelId) {
            this.channelId = channelId;
        }
    }

    public static class ChannelHistory {
        public int channelId;
        public List<ChatMessageDto> messages;

        public ChannelHistory() {
        }

        public ChannelHistory(int channelId, List<ChatMessageDto> messages) {
            this.channelId = channelId;
            this.messages = messages;
        }
    }

    /** Sent by client to start/stop typing, and relayed by server to the peer(s). */
    public static class TypingEvent {
        public Integer receiverId; // set for DM typing
        public Integer channelId;  // set for channel typing
        public int userId;         // filled by server before relaying
        public String username;    // filled by server before relaying
        public boolean typing;

        public TypingEvent() {
        }

        public TypingEvent(Integer receiverId, Integer channelId, int userId, String username, boolean typing) {
            this.receiverId = receiverId;
            this.channelId = channelId;
            this.userId = userId;
            this.username = username;
            this.typing = typing;
        }
    }
}
