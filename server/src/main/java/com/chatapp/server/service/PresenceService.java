package com.chatapp.server.service;

import com.chatapp.server.db.dao.FriendDao;
import com.chatapp.server.db.dao.UserDao;
import com.chatapp.server.net.SessionManager;
import com.chatapp.shared.model.PresenceStatus;
import com.chatapp.shared.protocol.MessageType;
import com.chatapp.shared.protocol.payload.PresencePayloads;
import com.chatapp.shared.util.JsonUtil;

import java.util.List;
import java.util.stream.Collectors;

/** Updates a user's presence in the database and notifies their friends in real time. */
public class PresenceService {

    private final UserDao userDao;
    private final FriendDao friendDao;
    private final SessionManager sessionManager;

    public PresenceService(UserDao userDao, FriendDao friendDao, SessionManager sessionManager) {
        this.userDao = userDao;
        this.friendDao = friendDao;
        this.sessionManager = sessionManager;
    }

    public void setPresence(int userId, PresenceStatus status) {
        userDao.updatePresence(userId, status);
        broadcastToFriends(userId, status);
    }

    private void broadcastToFriends(int userId, PresenceStatus status) {
        List<Integer> friendIds = friendDao.getFriends(userId).stream()
                .map(UserDao.UserRecord::id)
                .collect(Collectors.toList());
        var envelope = JsonUtil.envelope(MessageType.PRESENCE_UPDATE,
                new PresencePayloads.PresenceUpdate(userId, status));
        sessionManager.sendToMany(friendIds, envelope);
    }
}
