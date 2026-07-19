package com.chatapp.server.service;

import com.chatapp.server.db.dao.ChannelDao;
import com.chatapp.server.db.dao.FriendDao;
import com.chatapp.server.db.dao.MessageDao;
import com.chatapp.server.db.dao.ServerDao;
import com.chatapp.server.db.dao.UserDao;
import com.chatapp.server.net.SessionManager;
import com.chatapp.shared.model.ChatMessageDto;
import com.chatapp.shared.protocol.MessageType;
import com.chatapp.shared.protocol.payload.MessagePayloads;
import com.chatapp.shared.util.JsonUtil;

import java.util.List;
import java.util.Optional;

public class MessageService {

    private static final int HISTORY_LIMIT = 200;

    private final MessageDao messageDao;
    private final UserDao userDao;
    private final FriendDao friendDao;
    private final ServerDao serverDao;
    private final ChannelDao channelDao;
    private final SessionManager sessionManager;

    public MessageService(MessageDao messageDao, UserDao userDao, FriendDao friendDao,
                           ServerDao serverDao, ChannelDao channelDao, SessionManager sessionManager) {
        this.messageDao = messageDao;
        this.userDao = userDao;
        this.friendDao = friendDao;
        this.serverDao = serverDao;
        this.channelDao = channelDao;
        this.sessionManager = sessionManager;
    }

    public static class MessageException extends RuntimeException {
        public MessageException(String message) {
            super(message);
        }
    }

    public void sendDirectMessage(int senderId, int receiverId, String content) {
        String trimmed = validateContent(content);
        if (!friendDao.areFriends(senderId, receiverId)) {
            throw new MessageException("You can only message friends");
        }
        UserDao.UserRecord sender = userDao.findById(senderId).orElseThrow();
        ChatMessageDto saved = messageDao.saveDirectMessage(senderId, sender.username(), sender.avatarColor(), receiverId, trimmed);

        var envelope = JsonUtil.envelope(MessageType.DIRECT_MESSAGE, saved);
        sessionManager.sendTo(receiverId, envelope);
        sessionManager.sendTo(senderId, envelope);
    }

    public List<ChatMessageDto> getDirectHistory(int userId, int friendId) {
        return messageDao.getDirectHistory(userId, friendId, HISTORY_LIMIT);
    }

    public void sendChannelMessage(int senderId, int channelId, String content) {
        String trimmed = validateContent(content);
        ChannelDao.ChannelRecord channel = channelDao.findById(channelId)
                .orElseThrow(() -> new MessageException("Channel not found"));
        if (!serverDao.isMember(channel.serverId(), senderId)) {
            throw new MessageException("You are not a member of this server");
        }
        UserDao.UserRecord sender = userDao.findById(senderId).orElseThrow();
        ChatMessageDto saved = messageDao.saveChannelMessage(senderId, sender.username(), sender.avatarColor(), channelId, trimmed);

        var envelope = JsonUtil.envelope(MessageType.CHANNEL_MESSAGE, saved);
        List<UserDao.UserRecord> members = serverDao.getMembers(channel.serverId());
        for (UserDao.UserRecord member : members) {
            sessionManager.sendTo(member.id(), envelope);
        }
    }

    public List<ChatMessageDto> getChannelHistory(int userId, int channelId) {
        ChannelDao.ChannelRecord channel = channelDao.findById(channelId)
                .orElseThrow(() -> new MessageException("Channel not found"));
        if (!serverDao.isMember(channel.serverId(), userId)) {
            throw new MessageException("You are not a member of this server");
        }
        return messageDao.getChannelHistory(channelId, HISTORY_LIMIT);
    }

    /** Relays a typing start/stop event to the relevant DM peer or channel members. */
    public void relayTyping(int userId, String username, MessagePayloads.TypingEvent event) {
        event.userId = userId;
        event.username = username;
        var envelope = JsonUtil.envelope(MessageType.TYPING_INDICATOR, event);

        if (event.receiverId != null) {
            sessionManager.sendTo(event.receiverId, envelope);
        } else if (event.channelId != null) {
            Optional<ChannelDao.ChannelRecord> channel = channelDao.findById(event.channelId);
            channel.ifPresent(c -> {
                for (UserDao.UserRecord member : serverDao.getMembers(c.serverId())) {
                    if (member.id() != userId) {
                        sessionManager.sendTo(member.id(), envelope);
                    }
                }
            });
        }
    }

    private String validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new MessageException("Message cannot be empty");
        }
        String trimmed = content.strip();
        if (trimmed.length() > 2000) {
            throw new MessageException("Message is too long (max 2000 characters)");
        }
        return trimmed;
    }
}
